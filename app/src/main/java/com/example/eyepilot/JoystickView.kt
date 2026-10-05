package com.example.eyepilot

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.eyepilot.bluetooth.BluetoothManager
import kotlin.math.*

class JoystickView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f
    private var knobX = 0f
    private var knobY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f
        knobRadius = baseRadius * 0.25f
        knobX = centerX
        knobY = centerY

        basePaint.shader = RadialGradient(centerX, centerY, baseRadius,
            Color.parseColor("#E1BEE7"), Color.parseColor("#6A1B9A"), Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        knobPaint.shader = RadialGradient(knobX, knobY, knobRadius,
            Color.parseColor("#BA68C8"), Color.parseColor("#4A148C"), Shader.TileMode.CLAMP)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !BluetoothManager.isConnected) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                val maxDistance = baseRadius - knobRadius

                if (distance < maxDistance) {
                    knobX = event.x
                    knobY = event.y
                } else {
                    val angle = atan2(dy, dx)
                    knobX = centerX + maxDistance * cos(angle)
                    knobY = centerY + maxDistance * sin(angle)
                }
                enviarDatosArduino(knobX, knobY)
            }
            MotionEvent.ACTION_UP -> {
                knobX = centerX
                knobY = centerY

                BluetoothManager.sendJoystickData(
                    BluetoothManager.AXIS_CENTER,
                    BluetoothManager.AXIS_CENTER
                )
            }
        }
        invalidate()
        return true
    }

    private fun enviarDatosArduino(x: Float, y: Float) {
        val maxDist = baseRadius - knobRadius


        val valorX = (BluetoothManager.AXIS_CENTER + ((x - centerX) / maxDist * BluetoothManager.AXIS_CENTER))
            .toInt().coerceIn(0, BluetoothManager.AXIS_MAX)
        val valorY = (BluetoothManager.AXIS_CENTER + ((y - centerY) / maxDist * BluetoothManager.AXIS_CENTER))
            .toInt().coerceIn(0, BluetoothManager.AXIS_MAX)

        BluetoothManager.sendJoystickData(valorX, valorY)
    }
}