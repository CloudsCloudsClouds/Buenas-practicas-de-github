package com.example.eyepilot

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.eyepilot.bluetooth.BluetoothManager

class JoystickActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var joystickView: JoystickView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_joystick)

        tvStatus = findViewById(R.id.tvStatus)
        joystickView = findViewById(R.id.myJoystick)
        val btnBack = findViewById<Button>(R.id.btnBack)

        actualizarEstado()

        btnBack.setOnClickListener {
            BluetoothManager.sendJoystickData(
                BluetoothManager.AXIS_CENTER,
                BluetoothManager.AXIS_CENTER
            )
            finish()
        }
    }

    private fun actualizarEstado() {
        val nombre = BluetoothManager.getConnectedDeviceName()
        if (BluetoothManager.isConnected) {
            tvStatus.text = "Conectado con: ${nombre ?: "SILLA"}"
        } else {
            tvStatus.text = "Desconectado. Conecta la silla desde el menú."
        }
        joystickView.isEnabled = BluetoothManager.isConnected
    }

    override fun onResume() {
        super.onResume()
        actualizarEstado()
        if (BluetoothManager.isConnected) {
            BluetoothManager.sendJoystickData(
                BluetoothManager.AXIS_CENTER,
                BluetoothManager.AXIS_CENTER
            )
        }
    }
}