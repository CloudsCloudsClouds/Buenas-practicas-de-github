package com.example.eyepilot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CalibracionActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var faceLandmarker: FaceLandmarker? = null


    private var progreso = 0
    private var puntosIzquierdo = 0
    private var puntosDerecho = 0
    private var ojoFinal = "AMBOS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibracion)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupMediaPipe()
        iniciarCamara()

        findViewById<Button>(R.id.btnIrANavegacion).setOnClickListener {

            val intent = Intent(this, OcularActivity::class.java)
            intent.putExtra("OJO_DOMINANTE", ojoFinal)
            startActivity(intent)
            finish()
        }
    }

    private fun setupMediaPipe() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()


        val optionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result: FaceLandmarkerResult, _: MPImage ->
                procesarResultado(result)
            }

        val options = optionsBuilder.build()
        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
    }

    private fun procesarResultado(result: FaceLandmarkerResult) {
        if (result.faceLandmarks().isNotEmpty() && progreso < 100) {
            val landmarks = result.faceLandmarks()[0]

            // Medición apertura de cada ojo uso de landmarks de párpados
            val distIzq = Math.abs(landmarks[159].y() - landmarks[145].y())
            val distDer = Math.abs(landmarks[386].y() - landmarks[374].y())

            if (distIzq > 0.02) puntosIzquierdo++
            if (distDer > 0.02) puntosDerecho++

            progreso++

            runOnUiThread {
                findViewById<ProgressBar>(R.id.pbarCalibracion).progress = progreso
                if (progreso >= 100) finalizarCalibracion()
            }
        }
    }

    private fun finalizarCalibracion() {
        ojoFinal = when {
            puntosIzquierdo > puntosDerecho + 15 -> "IZQUIERDO"
            puntosDerecho > puntosIzquierdo + 15 -> "DERECHO"
            else -> "AMBOS"
        }

        findViewById<TextView>(R.id.txtInstruccion).text = "¡Calibración Completa!"
        findViewById<TextView>(R.id.txtResultadoOjo).apply {
            text = "Ojo detectado: $ojoFinal"
            visibility = View.VISIBLE
        }
        findViewById<Button>(R.id.btnIrANavegacion).visibility = View.VISIBLE
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinderCalibracion).surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {

                    it.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                        val frameTime = System.currentTimeMillis()
                        val bitmap = imageProxy.toBitmap()


                        val mpImage = BitmapImageBuilder(bitmap).build()

                        faceLandmarker?.detectAsync(mpImage, frameTime)
                        imageProxy.close()
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun ImageProxy.toBitmap(): android.graphics.Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()

        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceLandmarker?.close()
    }
}