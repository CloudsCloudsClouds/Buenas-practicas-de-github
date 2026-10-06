# EyePilot

Una silla de ruedas que se maneja desde el celular. Sin cables, sin joystick físico. La app se llama **EyePilot** y ofrece dos formas de conducir: un joystick en pantalla, o la mirada.

Suena ambicioso. En realidad son cuatro piezas hablando entre sí.

## Las cuatro piezas

**La app Android.** Está escrita en Kotlin y vive en `app/`. Usa la cámara del teléfono y MediaPipe para mirarte la cara, y Bluetooth para mandar órdenes a la silla.

**El firmware.** Un sketch de Arduino (`Proyecto_Silla_de_ruedas_inalambrico.ino`) que recibe dos bytes por puerto serie y los traduce en cuatro salidas PWM para los motores. Un módulo Bluetooth serie hace de puente con el teléfono.

**La cámara.** Un módulo tipo ESP32-CAM levanta su propio punto de acceso y transmite vídeo MJPEG en `http://192.168.4.1:81/stream`. La app lo muestra dentro de la pantalla de navegación ocular.

**El legado.** Un prototipo hecho en MIT App Inventor y el diseño del circuito en Proteus. Se conservan por historia; la app Kotlin los reemplaza. Detalles en [Decisiones](decisiones.md).

## Cómo se conectan

El teléfono y el firmware hablan por **Bluetooth serie clásico (SPP)**. La app manda tramas de dos bytes —posición X y posición Y— cada 100 ms como latido. El Arduino las convierte en movimiento y, si deja de escuchar, frena solo. Ese protocolo es el corazón de todo el sistema y está descrito en [Protocolo](protocolo.md).

La cámara va por su lado, por WiFi. No participa del control: solo te deja ver hacia dónde vas.

!!! warning "Antes de mover la silla"
    Es un dispositivo en movimiento. Lee primero el [modelo de seguridad](seguridad.md): hay varias paradas automáticas y conviene entender cuándo actúan.
