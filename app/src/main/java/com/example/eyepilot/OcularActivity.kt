package com.example.eyepilot

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.eyepilot.bluetooth.BluetoothManager
import com.example.eyepilot.databinding.ActivityOcularBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt

class OcularActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcularBinding
    private lateinit var cameraExecutor: ExecutorService
    private var faceLandmarker: FaceLandmarker? = null

    private var navegacionIniciada = false
    private var calibradoYListo = false
    private var esperandoReanudar = false
    private var perdidaBluetoothGestionada = false
    private var multiplicadorVelocidad = 0.5f
    private var zonaFijada = "STOP"

    private var ojosCerradosDesde: Long = 0
    private var mirandoCentroDesde: Long = 0

    // Suavizado
    private var bloqueoParpadeo = false
    private var smoothX = 0.5f
    private var smoothY = 0.5f
    private val factorSuavizado = 0.03f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        if (!BluetoothManager.isConnected) {
            Toast.makeText(this, "Conecta la silla antes de iniciar el control ocular", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivityOcularBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupControls()
        setupVideoStream()
        setupFaceLandmarker()
        startCamera()
    }

    private fun setupControls() {
        binding.btnVel1.setOnClickListener { multiplicadorVelocidad = 0.85f; iniciarCalibracion() }
        binding.btnVel2.setOnClickListener { multiplicadorVelocidad = 0.55f; iniciarCalibracion() }
        binding.btnContinuar.setOnClickListener {
            esperandoReanudar = false
            binding.btnContinuar.visibility = View.GONE
        }
    }

    private fun iniciarCalibracion() {
        navegacionIniciada = true
        binding.layoutVelocidad.visibility = View.GONE
        binding.txtCalibracion.visibility = View.VISIBLE
        binding.puntoRojoFijo.visibility = View.VISIBLE
        binding.puntoGuia.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light))
    }

    private fun processFaceLandmarks(result: FaceLandmarkerResult) {
        if (!BluetoothManager.isConnected) {
            if (!perdidaBluetoothGestionada) {
                perdidaBluetoothGestionada = true
                runOnUiThread {
                    Toast.makeText(this, "Bluetooth desconectado. El movimiento se detuvo.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            return
        }

        if (!navegacionIniciada || esperandoReanudar) return

        if (result.faceLandmarks().isNotEmpty()) {
            val landmarks = result.faceLandmarks()[0]

            val distOjo = abs(landmarks[159].y() - landmarks[145].y())
            if (distOjo < 0.011f) {
                bloqueoParpadeo = true
                if (ojosCerradosDesde == 0L) ojosCerradosDesde = System.currentTimeMillis()
                else if (System.currentTimeMillis() - ojosCerradosDesde > 1000) {
                    runOnUiThread { detenerPorSeguridad() }
                }
                return
            } else {
                ojosCerradosDesde = 0L
                if (bloqueoParpadeo) {
                    bloqueoParpadeo = false
                    return
                }
            }


            val iris = landmarks[468]
            val hRatio = (iris.x() - landmarks[33].x()) / (landmarks[133].x() - landmarks[33].x())
            val vRatio = (iris.y() - landmarks[159].y()) / (landmarks[145].y() - landmarks[159].y())


            var hAjustado = ((hRatio - 0.5f) * 6.5f) + 0.5f
            var vAjustado = ((vRatio - 0.45f) * -12.0f) + 0.5f


            if (abs(hAjustado - 0.5f) < 0.10f) hAjustado = 0.5f
            if (abs(vAjustado - 0.5f) < 0.10f) vAjustado = 0.5f


            smoothX = (factorSuavizado * hAjustado.coerceIn(0f, 1f)) + (1f - factorSuavizado) * smoothX
            smoothY = (factorSuavizado * vAjustado.coerceIn(0f, 1f)) + (1f - factorSuavizado) * smoothY

            runOnUiThread {
                val xFinal = 1.0f - smoothX
                val yFinal = smoothY

                binding.puntoGuia.x = (xFinal * binding.root.width) - (binding.puntoGuia.width / 2)
                binding.puntoGuia.y = (yFinal * binding.root.height) - (binding.puntoGuia.height / 2)

                if (!calibradoYListo) {

                    if (abs(xFinal - 0.5f) < 0.18f && abs(yFinal - 0.5f) < 0.18f) {
                        if (mirandoCentroDesde == 0L) mirandoCentroDesde = System.currentTimeMillis()
                        else if (System.currentTimeMillis() - mirandoCentroDesde > 1500) {
                            finalizarCalibracion()
                        }
                        binding.puntoGuia.scaleX = 1.5f
                        binding.puntoGuia.scaleY = 1.5f
                    } else {
                        mirandoCentroDesde = 0L
                        binding.puntoGuia.scaleX = 1.0f
                        binding.puntoGuia.scaleY = 1.0f
                    }
                } else {
                    gestionarComandosFijos(xFinal, yFinal)
                }
            }
        }
    }

    private fun finalizarCalibracion() {
        calibradoYListo = true
        binding.txtCalibracion.visibility = View.GONE
        binding.puntoRojoFijo.visibility = View.GONE
        binding.puntoGuia.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        binding.statusText.text = "SISTEMA ACTIVO"
    }

    private fun gestionarComandosFijos(x: Float, y: Float) {
        val zonaActual = obtenerNombreZona((x * 250).toInt(), (y * 250).toInt())
        if (zonaFijada == "STOP") {
            if (zonaActual != "STOP") {
                zonaFijada = zonaActual
                enviarComandoZonal(zonaActual)
            }
        } else if (zonaActual == "STOP") {
            zonaFijada = "STOP"
            enviarComandoZonal("STOP")
        }
    }

    private fun enviarComandoZonal(zona: String) {
        var xPWM = BluetoothManager.AXIS_CENTER
        var yPWM = BluetoothManager.AXIS_CENTER
        when (zona) {
            "ADELANTE" -> yPWM = coordenadaParaVelocidad(multiplicadorVelocidad, true)
            "ATRÁS" -> yPWM = coordenadaParaVelocidad(multiplicadorVelocidad, false)
            "IZQUIERDA" -> xPWM = coordenadaParaVelocidad(multiplicadorVelocidad, true)
            "DERECHA" -> xPWM = coordenadaParaVelocidad(multiplicadorVelocidad, false)
        }
        BluetoothManager.sendJoystickData(xPWM, yPWM)
        binding.statusText.text = "RUMBO: $zona"
    }

    private fun coordenadaParaVelocidad(velocidad: Float, adelanteOIzquierda: Boolean): Int {
        val velocidadLimitada = velocidad.coerceIn(0f, 1f)
        if (velocidadLimitada == 0f) return BluetoothManager.AXIS_CENTER

        return if (adelanteOIzquierda) {
            (83 * (1f - velocidadLimitada)).roundToInt().coerceIn(0, 82)
        } else {
            (166 + 84 * velocidadLimitada).roundToInt().coerceIn(167, BluetoothManager.AXIS_MAX)
        }
    }

    private fun detenerPorSeguridad() {
        esperandoReanudar = true
        BluetoothManager.sendJoystickData(BluetoothManager.AXIS_CENTER, BluetoothManager.AXIS_CENTER)
        binding.btnContinuar.visibility = View.VISIBLE
    }

    private fun obtenerNombreZona(x: Int, y: Int): String {
        return when {
            y < 85 -> "ADELANTE"
            y > 165 -> "ATRÁS"
            x < 85 -> "IZQUIERDA"
            x > 165 -> "DERECHA"
            else -> "STOP"
        }
    }

    private fun setupVideoStream() {
        binding.videoStream.settings.javaScriptEnabled = true
        val html = "<html><body style='margin:0;background:black;'><img src='http://192.168.4.1:81/stream' style='width:100vw;height:100vh;object-fit:cover;'></body></html>"
        binding.videoStream.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun setupFaceLandmarker() {
        val baseOptions = com.google.mediapipe.tasks.core.BaseOptions.builder().setModelAssetPath("face_landmarker.task").build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> processFaceLandmarks(result) }
            .build()
        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { proxy ->
                        faceLandmarker?.detectAsync(com.google.mediapipe.framework.image.BitmapImageBuilder(proxy.toBitmap()).build(), System.currentTimeMillis())
                        proxy.close()
                    }
                }
            cameraProvider.bindToLifecycle(this, androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA, preview, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothManager.sendJoystickData(BluetoothManager.AXIS_CENTER, BluetoothManager.AXIS_CENTER)
        cameraExecutor.shutdown()
    }
}