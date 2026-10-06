# Protocolo

Todo el control de la silla viaja en **dos bytes**. Nada más. Si entiendes esta página, entiendes cómo respira el sistema.

## La trama

Cada orden es un par `(X, Y)`:

| Campo | Rango | Centro | Significado |
|---|---|---|---|
| X | 0–255 | 125 | 0 = izquierda, 250 = derecha |
| Y | 0–255 | 125 | 0 = adelante, 250 = atrás |

El firmware descarta la trama y vuelve al centro si algún valor supera 250. El centro `(125, 125)` es la parada.

Para el vídeo, en cambio, no hay protocolo propio: un WebView carga el stream MJPEG del módulo de cámara. Por eso puede fallar sin que se caiga el control.

## Zonas de conducción

El firmware no calcula direcciones continuas: divide el plano en nueve zonas y enciende motores en consecuencia. Las diagonales giran reduciendo a la mitad la rueda interior.

| Zona | X | Y | Movimiento |
|---|---|---|---|
| Adelante | 84–165 | 0–82 | ambos motores adelante |
| Atrás | 84–165 | 167–250 | ambos motores atrás |
| Izquierda | 0–82 | 84–165 | gira a la izquierda |
| Derecha | 167–250 | 84–165 | gira a la derecha |
| Adelante + izquierda | 0–82 | 0–82 | avanza, rueda izquierda a la mitad |
| Adelante + derecha | 167–250 | 0–82 | avanza, rueda derecha a la mitad |
| Atrás + izquierda | 0–82 | 167–250 | retrocede, rueda izquierda a la mitad |
| Atrás + derecha | 167–250 | 167–250 | retrocede, rueda derecha a la mitad |
| Centro | 83–166 | 83–166 | parada |

## Latido y parada de emergencia

La app no manda una orden y se olvida. Mantiene un canal *conflated*: si no hay trama nueva en 100 ms, reenvía la última. Así el movimiento no depende de que cada paquete llegue.

Del otro lado, el Arduino cronometra. Si pasan más de **350 ms** sin recibir nada, los motores se apagan y la trama vuelve al centro. Es un cinturón de seguridad: si se cae el Bluetooth, la silla se detiene sola.

## Cómo el control ocular produce estas tramas

El modo por mirada no envía posiciones continuas. Detecta en qué zona está mirando el usuario y fija la orden, cambiándola solo cuando la mirada sale de esa zona o vuelve al centro. Primero escala la posición del iris, luego suaviza el resultado para que no tiemble.

La calibración de ojo dominante suma 100 frames y decide entre izquierdo, derecho o ambos según cuál se mantuvo más abierto. Ese dato ya se calcula, aunque hoy la navegación no lo aprovecha.

## Números clave

Estos valores gobiernan el comportamiento. Casi todos están en `BluetoothManager.kt`, `OcularActivity.kt`, `CalibracionActivity.kt` y el `.ino`.

| Valor | Dónde | Para qué sirve |
|---|---|---|
| `125` | Android y firmware | centro / parada de cada eje |
| `250` | Android y firmware | máximo de cada eje |
| `83` / `166` | ambos | frontera de la zona muerta central |
| `350 ms` | firmware | sin órdenes durante este tiempo, frena |
| `100 ms` | Android | intervalo del latido |
| `9600` baudios | firmware | velocidad del puerto serie |
| pines `3, 5, 6, 9` | firmware | motores: der/adelante, der/atrás, izq/adelante, izq/atrás |
| `0.03` | OcularActivity | suavizado de la mirada (más alto, más nervioso) |
| `6.5` / `-12.0` | OcularActivity | escala horizontal y vertical del iris |
| `0.10` | OcularActivity | zona muerta: movimientos menores se ignoran |
| `0.011` | OcularActivity | apertura de ojo por debajo de la cual se considera cerrado |
| `1000 ms` | OcularActivity | parpadeo mantenido que dispara la parada |
| `1500 ms` | OcularActivity | tiempo mirando al centro para terminar la calibración |
| `0.85` / `0.55` | OcularActivity | multiplicadores de velocidad (rápido / lento) |
| `100` frames | CalibracionActivity | muestras para decidir el ojo dominante |
| `15` puntos | CalibracionActivity | diferencia mínima para declarar un ojo dominante |
| `192.168.4.1:81` | OcularActivity | stream MJPEG de la cámara |

Ajustar la sensibilidad de la mirada casi siempre significa tocar `0.03`, `0.10` y las escalas `6.5` / `-12.0`. La velocidad de marcha se cambia con los multiplicadores `0.85` y `0.55`.
