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
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {
    private var cameraOpenRetries = 0
    private val maxRetries = 3
    private val cameraPermissionRequestCode = 123
    private var isScanning = false
    private var lastImageSentTime = 0L
    private val minIntervalBetweenImages = 150L
    private var lastToastTime = 0L
    private var isProcessing = false
    private var seanceId: Int = -1
    private val detectedStudents = mutableSetOf<String>()

    private lateinit var scanButton: FloatingActionButton
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webSocket: WebSocket
    private var cameraProvider: ProcessCameraProvider? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var faceOverlay: FaceOverlayView
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS) // Increased for multi-face processing
        .build()

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
        faceOverlay = findViewById(R.id.faceOverlay)

        // Retrieve seance_id
        seanceId = intent.getIntExtra("SEANCE_ID", -1)
        if (seanceId == -1) {
            Log.e("CameraActivity", "Invalid seance ID")
            Toast.makeText(this, "Erreur : ID de séance manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d("CameraActivity", "Seance ID: $seanceId")

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
        val request = Request.Builder().url("ws://100.70.32.157:8765").build()
        try {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("CameraActivity", "WebSocket connecté")
                    runOnUiThread { Toast.makeText(this@CameraActivity, "Connecté au backend", Toast.LENGTH_SHORT).show() }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        Log.d("CameraActivity", "Received WebSocket response: $text")
                        val jsonResponse = JSONObject(text)
                        val detectionsArray = jsonResponse.getJSONArray("detections")
                        val imageSize = jsonResponse.getJSONObject("image_size")

                        val rawDetections = mutableListOf<FaceOverlayView.FaceDetection>()
                        for (i in 0 until detectionsArray.length()) {
                            val obj = detectionsArray.getJSONObject(i)
                            val studentId = obj.getString("id")
                            rawDetections.add(
                                FaceOverlayView.FaceDetection(
                                    studentId,
                                    obj.getDouble("left").toFloat(),
                                    obj.getDouble("top").toFloat(),
                                    obj.getDouble("right").toFloat(),
                                    obj.getDouble("bottom").toFloat()
                                )
                            )
                            detectedStudents.add(studentId)
                            Log.d("CameraActivity", "Detected student: $studentId")
                        }
                        Log.d("CameraActivity", "Received ${rawDetections.size} detections")

                        runOnUiThread {
                            var originalWidth = 1280f // Updated default
                            var originalHeight = 720f
                            try {
                                originalWidth = imageSize.getInt("width").toFloat()
                                originalHeight = imageSize.getInt("height").toFloat()
                            } catch (e: Exception) {
                                Log.e("CameraActivity", "Error parsing image_size: ${e.message}, using default 1280x720")
                            }

                            val scaleX = previewView.width / originalWidth
                            val scaleY = previewView.height / originalHeight

                            val scaledDetections = rawDetections.map { detection ->
                                FaceOverlayView.FaceDetection(
                                    detection.id,
                                    detection.left * scaleX,
                                    detection.top * scaleY,
                                    detection.right * scaleX,
                                    detection.bottom * scaleY
                                )
                            }
                            Log.d("CameraActivity", "Updating detections: ${scaledDetections.size}")
                            faceOverlay.updateDetections(scaledDetections)
                            faceOverlay.invalidate()
                            isProcessing = false
                        }
                    } catch (e: Exception) {
                        Log.e("CameraActivity", "Erreur de traitement: ${e.message}")
                        isProcessing = false
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("CameraActivity", "WebSocket fermé : $reason")
                    runOnUiThread { Toast.makeText(this@CameraActivity, "Déconnecté", Toast.LENGTH_SHORT).show() }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("CameraActivity", "Erreur WebSocket : ${t.message}")
                    runOnUiThread { Toast.makeText(this@CameraActivity, "Erreur WebSocket : ${t.message}", Toast.LENGTH_LONG).show() }
                    isProcessing = false
                }
            })
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erreur lors de l'initialisation WebSocket : ${e.message}")
            runOnUiThread { Toast.makeText(this, "Erreur WebSocket", Toast.LENGTH_SHORT).show() }
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
            Toast.makeText(this, "Erreur caméra", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: run {
            Log.e("CameraActivity", "CameraProvider non initialisé")
            return
        }
        try {
            cameraProvider.unbindAll()
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            val preview = Preview.Builder().setTargetRotation(previewView.display.rotation).build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 480)) // Increased resolution
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            if (isScanning && !isProcessing) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastImageSentTime >= minIntervalBetweenImages) {
                                    val jpegBytes = convertToJpeg(imageProxy)
                                    Log.d("CameraActivity", "Envoi de l'image, taille : ${jpegBytes.size} bytes")
                                    val success = webSocket.send(ByteString.of(*jpegBytes))
                                    if (success) {
                                        isProcessing = true
                                        lastImageSentTime = currentTime
                                    } else {
                                        Log.e("CameraActivity", "Échec de l'envoi de l'image")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CameraActivity", "Erreur lors de l'envoi de l'image : ${e.message}")
                            isProcessing = false
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
        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        Log.d("CameraActivity", "Rotation de l'image : $rotationDegrees degrés")

        val finalBitmap = if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, outputStream) // Increased quality
        val jpegBytes = outputStream.toByteArray()

        if (finalBitmap != bitmap) finalBitmap.recycle()
        bitmap.recycle()

        Log.d("CameraActivity", "Image convertie en JPEG, taille : ${jpegBytes.size} bytes")
        return jpegBytes
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
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
            detectedStudents.clear()
            bindCameraUseCases()
            Toast.makeText(this, "Scan démarré pour séance $seanceId", Toast.LENGTH_SHORT).show()
        } else {
            cameraProvider?.unbindAll()
            faceOverlay.updateDetections(emptyList())
            faceOverlay.invalidate()
            isProcessing = false
            sendPresenceToSeance()
            savePresenceToJson()
            Toast.makeText(this, "Scan arrêté", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateScanUI() {
        scanButton.setImageResource(if (isScanning) R.drawable.ic_stop else R.drawable.ic_scan)
        scanButton.backgroundTintList = ContextCompat.getColorStateList(
            this, if (isScanning) R.color.red else R.color.design_default_color_primary
        )
    }

    private fun sendPresenceToSeance() {
        if (detectedStudents.isEmpty()) {
            Log.d("CameraActivity", "Aucun étudiant détecté, sauvegarde ignorée")
            runOnUiThread {
                Toast.makeText(this, "Aucun étudiant détecté", Toast.LENGTH_SHORT).show()
            }
            return
        }

        Thread {
            try {
                val jsonArray = JSONArray()
                detectedStudents.forEach { studentId ->
                    val jsonObject = JSONObject().apply {
                        put("etudiant_nom", studentId)
                        put("timestamp", System.currentTimeMillis())
                    }
                    jsonArray.put(jsonObject)
                }
                val jsonBody = JSONObject().apply {
                    put("seance_id", seanceId)
                    put("presences", jsonArray)
                }

                val requestBody = RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    jsonBody.toString()
                )
                val request = Request.Builder()
                    .url("http://100.70.32.157:5000/seance/$seanceId/presence1") // Corrected endpoint
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("CameraActivity", "Présence envoyée à seance.py: $jsonBody")
                        runOnUiThread {
                            Toast.makeText(this, "Présence sauvegardée", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val error = response.body?.string() ?: "Erreur inconnue"
                        Log.e("CameraActivity", "Échec de l'envoi de la présence : $error")
                        runOnUiThread {
                            Toast.makeText(this, "Échec de la sauvegarde : $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraActivity", "Erreur lors de l'envoi de la présence : ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Erreur lors de l'envoi : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun savePresenceToJson() {
        val file = File(getExternalFilesDir(null), "presence_seance_$seanceId.json")
        val jsonArray = JSONArray()
        detectedStudents.forEach { studentId ->
            val jsonObject = JSONObject().apply {
                put("seance_id", seanceId)
                put("etudiant_nom", studentId)
                put("timestamp", System.currentTimeMillis())
            }
            jsonArray.put(jsonObject)
        }
        try {
            FileOutputStream(file).use { fos ->
                fos.write(jsonArray.toString(2).toByteArray())
                fos.flush()
            }
            Log.d("CameraActivity", "JSON sauvegardé à ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erreur lors de la sauvegarde JSON : ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
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
        if (checkCameraPermission() && !::cameraExecutor.isInitialized) {
            setupCamera()
        }
    }
}