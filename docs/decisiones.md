# Decisiones

Por qué el proyecto es como es. Estas notas están reconstruidas a partir del código; si algún autor recuerda un matiz distinto, este es el lugar para corregirlo.

## Bluetooth serie clásico, no BLE

La silla usa el perfil SPP, que emula un puerto serie. La razón práctica: con él, mandar datos es tan simple como escribir bytes en un stream. Con BLE habría que montar servicios y características GATT para transportar lo mismo.

**Consecuencia:** compatible con módulos baratos tipo HC-05 y con el `Serial.read()` del Arduino. A cambio, el emparejamiento se hace en los ajustes del teléfono, no desde la app. Por eso la app manda a Ajustes cuando no encuentra dispositivos.

## Tramas binarias de dos bytes

Nada de JSON ni de comandos de texto. Dos bytes, y listo.

**Consecuencia:** el firmware lee con `Serial.read()` sin parsear nada, y el coste por paquete es mínimo. El precio es que el protocolo no se explica solo: hay que leer esta documentación para entenderlo.

## Vídeo MJPEG en un WebView

La cámara transmite MJPEG. En vez de escribir un reproductor nativo, la app carga el stream en un `WebView` con una etiqueta `<img>`.

**Consecuencia:** unas pocas líneas de código y funciona con cualquier stream MJPEG. A cambio, la IP está fija (`192.168.4.1`) y el vídeo no trae autenticación.

## Zonas discretas en el modo ocular

La mirada no produce movimiento continuo. Se traduce a una de cuatro direcciones y se fija hasta que la mirada cambia de zona o vuelve al centro.

**Consecuencia:** el movimiento es predecible y no oscila con el temblor natural del ojo. Se pierde precisión fina, que se compensa con el suavizado y la zona muerta descritos en [Protocolo](protocolo.md).

## Dejó de usarse App Inventor

El primer prototipo se hizo en MIT App Inventor (`.aia`). La versión actual es una app Kotlin completa, que además incorpora MediaPipe para el control por mirada, algo que App Inventor no permitía.

## Licencia BOLA

El proyecto se publica bajo la Buena Onda License Agreement (BOLA), en el archivo `LICENSE` de la raíz. En la práctica es dominio público, con una lista de buenas costumbres como pedido, no como obligación.

## Legado

Los archivos de `Proyecto_Silla_de_ruedas_inalambrico/` son históricos. El `.aia` y el `.apk` son el prototipo en App Inventor; los `.pdsprj` y los documentos de impresión pertenecen al diseño del circuito en Proteus. No forman parte de la app actual y se conservan solo como referencia.

## Limitaciones conocidas

- `BluetoothActivity` es una actividad vacía que figura en el manifiesto pero no se abre desde ningún lado. La selección de dispositivos ocurre dentro de `MenuActivity`. Es código muerto; se puede eliminar.
- Hay dos copias anidadas del proyecto de App Inventor, con respaldos y binarios pesados. No afectan el funcionamiento, pero ensucian el repositorio y conviene limpiarlas.
