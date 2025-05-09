package com.example.absencestest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private var cameraOpenRetries = 0
    private val maxRetries = 3
    private val cameraPermissionRequestCode = 123
    private var isScanning = false
    private var lastImageSentTime = 0L
    private val minIntervalBetweenImages = 500L
    private var lastToastTime = 0L

    private lateinit var scanButton: FloatingActionButton
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webSocket: WebSocket
    private var cameraProvider: ProcessCameraProvider? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CameraActivity", "Exception non gérée : ${throwable.message}", throwable)
            runOnUiThread {
                Toast.makeText(this, "Erreur : ${throwable.message}", Toast.LENGTH_LONG).show()
            }
        }
        setContentView(R.layout.activity_camera)

        scanButton = findViewById(R.id.btnScan)
        previewView = findViewById(R.id.previewView)

        if (!checkCameraPermission()) {
            requestCameraPermission()
        } else {
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        initWebSocket()
        setupCamera()
    }

    private fun checkCameraPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        Log.d("CameraActivity", "Permission caméra : $granted")
        return granted
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("CameraActivity", "Permission caméra accordée")
            initializeComponents()
        } else {
            Log.d("CameraActivity", "Permission caméra refusée")
            Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://192.168.228.90:8765").build()
        try {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("CameraActivity", "WebSocket connecté")
                    runOnUiThread { Toast.makeText(this@CameraActivity, "Connecté au backend", Toast.LENGTH_SHORT).show() }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("CameraActivity", "Message reçu du backend : $text")
                    try {
                        val ids = text.trim('[', ']').split(",").map { it.trim().trim('"') }.filter { it.isNotEmpty() }
                        Log.d("CameraActivity", "IDs détectés : $ids")

                        // Limiter les Toasts
                        val currentTime = System.currentTimeMillis()
                        val minIntervalBetweenToasts = 2000L
                        if (currentTime - lastToastTime >= minIntervalBetweenToasts) {
                            if (ids.isNotEmpty()) {
                                runOnUiThread { Toast.makeText(this@CameraActivity, "Présences détectées : $ids", Toast.LENGTH_SHORT).show() }
                            } else {
                                runOnUiThread { Toast.makeText(this@CameraActivity, "Aucun visage détecté", Toast.LENGTH_SHORT).show() }
                            }
                            lastToastTime = currentTime
                        }
                    } catch (e: Exception) {
                        Log.e("CameraActivity", "Erreur lors du traitement du message : ${e.message}")
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("CameraActivity", "WebSocket fermé : $reason")
                    runOnUiThread { Toast.makeText(this@CameraActivity, "Déconnecté", Toast.LENGTH_SHORT).show() }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("CameraActivity", "Erreur WebSocket : ${t.message}")
                    runOnUiThread { Toast.makeText(this@CameraActivity, "Erreur WebSocket : ${t.message}", Toast.LENGTH_LONG).show() }
                }
            })
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erreur lors de l'initialisation WebSocket : ${e.message}")
        }
    }

    private fun setupCamera() {
        Log.d("CameraActivity", "Initialisation de la caméra")
        try {
            if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) cameraExecutor.shutdown()
            cameraExecutor = Executors.newSingleThreadExecutor()

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                Log.d("CameraActivity", "CameraProvider obtenu")
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erreur lors de l'initialisation de la caméra : ${e.message}")
            Toast.makeText(this, "Erreur caméra : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        try {
            cameraProvider.unbindAll()
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            val preview = Preview.Builder().setTargetRotation(previewView.display.rotation).build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            if (isScanning) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastImageSentTime >= minIntervalBetweenImages) {
                                    val jpegBytes = convertToJpeg(imageProxy)
                                    Log.d("CameraActivity", "Envoi de l'image, taille : ${jpegBytes.size} bytes")

                                    // Sauvegarder l'image localement pour inspection
                                    try {
                                        val file = java.io.File(getExternalFilesDir(null), "test_image.jpg")
                                        java.io.FileOutputStream(file).use { fos ->
                                            fos.write(jpegBytes)
                                            fos.flush()
                                        }
                                        Log.d("CameraActivity", "Image sauvegardée localement : ${file.absolutePath}")
                                    } catch (e: Exception) {
                                        Log.e("CameraActivity", "Erreur lors de la sauvegarde locale : ${e.message}")
                                    }

                                    val success = webSocket.send(ByteString.of(*jpegBytes))
                                    Log.d("CameraActivity", "Image envoyée : $success")
                                    lastImageSentTime = currentTime
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CameraActivity", "Erreur lors de l'envoi de l'image : ${e.message}")
                        } finally {
                            imageProxy.close()
                        }
                    }
                }

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
            cameraOpenRetries = 0
            Log.d("CameraActivity", "Camera bindée avec succès")
        } catch (ex: Exception) {
            Log.e("CameraActivity", "Erreur lors de la liaison de la caméra : ${ex.message}")
            Toast.makeText(this, "Erreur caméra : ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertToJpeg(imageProxy: ImageProxy): ByteArray {
        // Convertir l'ImageProxy en Bitmap
        val bitmap = imageProxy.toBitmap()

        // Vérifier l'orientation de l'image
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        Log.d("CameraActivity", "Rotation de l'image : $rotationDegrees degrés")

        // Appliquer la rotation si nécessaire
        val finalBitmap = if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        // Convertir le Bitmap en JPEG
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
        val jpegBytes = outputStream.toByteArray()

        // Nettoyer les ressources
        if (finalBitmap != bitmap) {
            finalBitmap.recycle()
        }
        bitmap.recycle()

        Log.d("CameraActivity", "Image convertie en JPEG, taille : ${jpegBytes.size} bytes")
        return jpegBytes
    }

    @OptIn(ExperimentalGetImage::class)
    private fun ImageProxy.toBitmap(): Bitmap {
        val image = this.image ?: throw IllegalStateException("ImageProxy image is null")
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val data = ByteArray(ySize + uSize + vSize)
        yBuffer.get(data, 0, ySize)
        uBuffer.get(data, ySize, uSize)
        vBuffer.get(data, ySize + uSize, vSize)

        // Utiliser YuvImage pour convertir en JPEG, puis en Bitmap
        val yuvImage = YuvImage(data, ImageFormat.YUV_420_888, width, height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
        val jpegBytes = outputStream.toByteArray()

        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    fun onScanClick(view: View) {
        isScanning = !isScanning
        updateScanUI()
        if (isScanning) {
            bindCameraUseCases()
            Toast.makeText(this, "Scan démarré", Toast.LENGTH_SHORT).show()
        } else {
            cameraProvider?.unbindAll()
            Toast.makeText(this, "Scan arrêté", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateScanUI() {
        scanButton.setImageResource(if (isScanning) R.drawable.ic_stop else R.drawable.ic_scan)
        scanButton.backgroundTintList = ContextCompat.getColorStateList(
            this, if (isScanning) R.color.red else R.color.design_default_color_primary
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        try {
            webSocket.close(1000, "Activité terminée")
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erreur lors de la fermeture du WebSocket : ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
    }

    override fun onResume() {
        super.onResume()
        if (checkCameraPermission() && ::cameraExecutor.isInitialized) setupCamera()
    }
}