package com.example.eyepilot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.eyepilot.bluetooth.BluetoothManager
import android.content.Context

class MenuActivity : AppCompatActivity() {

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            mostrarDialogoDispositivos()
        } else {
            Toast.makeText(this, "Se necesitan permisos para conectar la silla", Toast.LENGTH_LONG).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) mostrarDialogoDispositivos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val btnBluetooth = findViewById<Button>(R.id.btnBluetooth)
        val btnJoystick = findViewById<Button>(R.id.btnJoystick)
        val btnOcular = findViewById<Button>(R.id.btnOcular)

        btnBluetooth.setOnClickListener {
            verificarPermisosYMostrarLista()
        }

        btnJoystick.setOnClickListener {
            startActivity(Intent(this, JoystickActivity::class.java))
        }

        btnOcular.setOnClickListener {
            startActivity(Intent(this, CalibracionActivity::class.java))
        }

        actualizarEstadoConexion()
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoConexion()
    }

    private fun actualizarEstadoConexion() {
        val conectado = BluetoothManager.isConnected
        findViewById<Button>(R.id.btnJoystick).isEnabled = conectado
        findViewById<Button>(R.id.btnOcular).isEnabled = conectado
        findViewById<Button>(R.id.btnBluetooth).text = if (conectado) {
            "CONECTADO: ${BluetoothManager.getConnectedDeviceName() ?: "SILLA"}"
        } else {
            "CONEXIÓN BLUETOOTH"
        }
    }

    private fun verificarPermisosYMostrarLista() {
        val permissions = BluetoothManager.getRequiredPermissions()
        val todosConcedidos = permissions.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

        if (!todosConcedidos) {
            bluetoothPermissionLauncher.launch(permissions)
        } else {
            mostrarDialogoDispositivos()
        }
    }

    @SuppressLint("MissingPermission")
    private fun mostrarDialogoDispositivos() {
        val bluetoothService = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val adapter = bluetoothService.adapter

        if (adapter == null) {
            Toast.makeText(this, "Este dispositivo no admite Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        val dispositivosVinculados = adapter.bondedDevices.toList()
        val builder = AlertDialog.Builder(this)

        if (dispositivosVinculados.isEmpty()) {
            builder.setTitle("Dispositivo no encontrado")
            builder.setMessage("No hay dispositivos vinculados. Vincula el módulo Bluetooth de la silla desde los ajustes del teléfono y vuelve a intentarlo.")
            builder.setPositiveButton("Ir a Ajustes") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            builder.setNegativeButton("Cancelar", null)
        } else {
            builder.setTitle("Selecciona el módulo de la silla")
            val nombresDispositivos = dispositivosVinculados.map { it.name ?: "Desconocido" }.toTypedArray()

            builder.setItems(nombresDispositivos) { _, which ->
                val seleccionado = dispositivosVinculados[which]
                conectarADispositivoEspecifico(seleccionado)
            }

            builder.setNeutralButton("¿No aparece?") { _, _ ->
                Toast.makeText(this, "Vincula el módulo de la silla desde los ajustes Bluetooth", Toast.LENGTH_LONG).show()
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            builder.setNegativeButton("Cerrar", null)
        }

        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun conectarADispositivoEspecifico(dispositivo: BluetoothDevice) {
        val nombre = dispositivo.name ?: "Dispositivo Bluetooth"
        Toast.makeText(this, "Conectando a $nombre...", Toast.LENGTH_SHORT).show()

        BluetoothManager.connectToDevice(dispositivo,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Conectado con: $nombre ✅", Toast.LENGTH_LONG).show()
                    actualizarEstadoConexion()
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}