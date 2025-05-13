package com.example.absencestest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
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
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {
    private var cameraOpenRetries = 0
    private val maxRetries = 3
    private val cameraPermissionRequestCode = 123
    private var isStreaming = false
    private var lastFrameSentTime = 0L
    private val frameInterval = 100L // 10 FPS (1000ms/10)
    private var lastToastTime = 0L
    private var sessionActive = false

    private lateinit var streamButton: FloatingActionButton
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webSocket: WebSocket
    private var cameraProvider: ProcessCameraProvider? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var faceOverlay: FaceOverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        faceOverlay = findViewById(R.id.faceOverlay)
        streamButton = findViewById(R.id.btnScan)
        previewView = findViewById(R.id.previewView)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CameraActivity", "Exception non gérée : ${throwable.message}", throwable)
            runOnUiThread {
                Toast.makeText(this, "Erreur : ${throwable.message}", Toast.LENGTH_LONG).show()
            }
        }

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

    private fun initWebSocket() {
        val client = OkHttpClient.Builder()
            .pingInterval(10, TimeUnit.SECONDS)
            .build()

        val seanceId = intent.getIntExtra("SEANCE_ID", -1).takeIf { it != -1 }
            ?: run {
                Toast.makeText(this, "ID de séance invalide", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        val request = Request.Builder()
            .url("ws://192.168.43.18:8765")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("CameraActivity", "WebSocket connecté")
                sessionActive = true

                // Envoyer le message d'initialisation avec le seance_id
                val initMessage = JSONObject().apply {
                    put("seance_id", seanceId)
                    put("action", "start_stream")
                }
                webSocket.send(initMessage.toString())

                runOnUiThread {
                    Toast.makeText(this@CameraActivity, "Streaming démarré", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WS_DEBUG", "Réponse reçue: $text") // Ajoutez ce log

                try {
                    val jsonResponse = JSONObject(text)
                    val detections = jsonResponse.getJSONArray("detections")

                    // DEBUG: Vérifiez le contenu des détections
                    Log.d("WS_DEBUG", "Nombre de détections: ${detections.length()}")

                    val faces = mutableListOf<FaceOverlayView.FaceDetection>()
                    for (i in 0 until detections.length()) {
                        val obj = detections.getJSONObject(i)
                        Log.d("WS_DEBUG", "Détection $i: $obj") // Log chaque détection

                        faces.add(FaceOverlayView.FaceDetection(
                            obj.getString("id"),
                            obj.getDouble("left").toFloat(),
                            obj.getDouble("top").toFloat(),
                            obj.getDouble("right").toFloat(),
                            obj.getDouble("bottom").toFloat()
                        ))
                    }

                    runOnUiThread {
                        val previewWidth = previewView.width.toFloat()
                        val previewHeight = previewView.height.toFloat()

                        val scaledFaces = faces.map { face ->
                            // Ajustez selon la résolution de votre caméra (ex: 640x480)
                            val scaleX = previewWidth / 640f
                            val scaleY = previewHeight / 480f

                            FaceOverlayView.FaceDetection(
                                face.id,
                                face.left * scaleX,
                                face.top * scaleY,
                                face.right * scaleX,
                                face.bottom * scaleY
                            )
                        }

                        faceOverlay.updateDetections(scaledFaces)
                    }
                } catch (e: Exception) {
                    Log.e("WS_ERROR", "Erreur parsing: ${e.message}")
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                sessionActive = false
                Log.d("CameraActivity", "WebSocket fermé: $reason")
                runOnUiThread {
                    Toast.makeText(this@CameraActivity, "Streaming arrêté", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                sessionActive = false
                Log.e("CameraActivity", "Erreur WS: ${t.message}")
                runOnUiThread {
                    Toast.makeText(this@CameraActivity, "Erreur streaming: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        try {
            cameraProvider.unbindAll()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val preview = Preview.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 480)) // Résolution réduite pour performance
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            if (isStreaming && sessionActive) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastFrameSentTime >= frameInterval) {
                                    val jpegBytes = convertToJpeg(imageProxy)
                                    webSocket.send(ByteString.of(*jpegBytes))
                                    lastFrameSentTime = currentTime
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CameraActivity", "Erreur envoi frame: ${e.message}")
                        } finally {
                            imageProxy.close()
                        }
                    }
                }

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erreur liaison caméra: ${e.message}")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun convertToJpeg(imageProxy: ImageProxy): ByteArray {
        val image = imageProxy.image ?: return ByteArray(0)
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 70, out) // Qualité 70%
        return out.toByteArray()
    }

    fun onStreamClick(view: View) {
        isStreaming = !isStreaming
        updateStreamUI()

        if (isStreaming) {
            if (!sessionActive) {
                initWebSocket() // Reconnecter si nécessaire
            }
            Toast.makeText(this, "Streaming démarré", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Streaming arrêté", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStreamUI() {
        streamButton.setImageResource(if (isStreaming) R.drawable.ic_stop else R.drawable.ic_scan)
        streamButton.backgroundTintList = ContextCompat.getColorStateList(
            this, if (isStreaming) R.color.red else R.color.design_default_color_primary
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        webSocket.close(1000, "Activité terminée")
    }

    override fun onPause() {
        super.onPause()
        isStreaming = false
        cameraProvider?.unbindAll()
    }

    override fun onResume() {
        super.onResume()
        if (checkCameraPermission()) {
            setupCamera()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeComponents()
        } else {
            Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}