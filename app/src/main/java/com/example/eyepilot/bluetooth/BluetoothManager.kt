package com.example.eyepilot.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.OutputStream
import java.util.*

@SuppressLint("StaticFieldLeak")
object BluetoothManager {

    const val AXIS_CENTER = 125
    const val AXIS_MAX = 250

    private const val STOP_MIN = 83
    private const val STOP_MAX = 166
    private const val HEARTBEAT_INTERVAL_MS = 100L
    private val stopFrame = byteArrayOf(AXIS_CENTER.toByte(), AXIS_CENTER.toByte())

    @Volatile private var bluetoothSocket: BluetoothSocket? = null
    @Volatile private var outputStream: OutputStream? = null
    private var writerJob: Job? = null
    @Volatile private var outgoingChannel: Channel<ByteArray>? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    val isConnected: Boolean
        get() = bluetoothSocket?.isConnected == true

    // Función permisos Android
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, onSuccess: () -> Unit, onError: (String) -> Unit) {
        serviceScope.launch {
            val previousSocket = bluetoothSocket
            outputStream = null
            bluetoothSocket = null
            writerJob?.cancel()
            outgoingChannel?.close()
            outgoingChannel = null

            withContext(Dispatchers.IO) {
                runCatching { previousSocket?.close() }
            }

            var newSocket: BluetoothSocket? = null
            try {
                newSocket = withContext(Dispatchers.IO) {
                    val candidate = device.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    )
                    try {
                        candidate.connect()
                        candidate.outputStream.write(stopFrame)
                        candidate.outputStream.flush()
                        candidate
                    } catch (error: Exception) {
                        runCatching { candidate.close() }
                        throw error
                    }
                }

                val socket = checkNotNull(newSocket)
                val stream = socket.outputStream
                val channel = Channel<ByteArray>(Channel.CONFLATED)
                bluetoothSocket = socket
                outputStream = stream
                outgoingChannel = channel
                writerJob = serviceScope.launch(Dispatchers.IO) {
                    var currentFrame = stopFrame
                    try {
                        while (isActive && outputStream === stream) {
                            val nextFrame = withTimeoutOrNull(HEARTBEAT_INTERVAL_MS) {
                                channel.receive()
                            }
                            if (nextFrame != null) {
                                currentFrame = nextFrame
                                stream.write(currentFrame)
                                stream.flush()
                            } else if (!isStopFrame(currentFrame)) {
                                stream.write(currentFrame)
                                stream.flush()
                            }
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        if (bluetoothSocket === socket) {
                            outputStream = null
                            bluetoothSocket = null
                            outgoingChannel = null
                            channel.close()
                            runCatching { socket.close() }
                        }
                    } finally {
                        channel.close()
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                runCatching { newSocket?.close() }
                onError("Error: ${e.message}")
            }
        }
    }

    fun sendJoystickData(x: Int, y: Int) {
        val channel = outgoingChannel ?: return
        val frame = byteArrayOf(
            x.coerceIn(0, AXIS_MAX).toByte(),
            y.coerceIn(0, AXIS_MAX).toByte()
        )
        channel.trySend(frame)
    }

    @SuppressLint("MissingPermission")
    fun getConnectedDeviceName(): String? = if (isConnected) bluetoothSocket?.remoteDevice?.name else null

    private fun isStopFrame(frame: ByteArray): Boolean {
        val x = frame[0].toInt() and 0xFF
        val y = frame[1].toInt() and 0xFF
        return x in STOP_MIN..STOP_MAX && y in STOP_MIN..STOP_MAX
    }
}