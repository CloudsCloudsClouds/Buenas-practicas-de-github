# Puesta en marcha

De cero a mover la silla. Si algo falla, al final hay una tabla de problemas comunes.

## Compilar

Necesitas Android Studio con JDK 17. La app pide Android 7.0 (API 24) o superior.

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`. Para instalarlo en un dispositivo conectado:

```bash
./gradlew installDebug
```

## Emparejar la silla

El emparejamiento Bluetooth se hace en los ajustes del teléfono, no dentro de la app.

1. Enciende el módulo Bluetooth de la silla y el Arduino.
2. Desde los ajustes del teléfono, empareja el dispositivo (suele llamarse `HC-05` o similar).
3. Abre EyePilot y concede los permisos de cámara y Bluetooth.
4. En el menú, toca **CONEXIÓN BLUETOOTH** y elige el módulo de la silla.

Cuando conecta, el botón cambia a **CONECTADO** y se habilitan los dos modos de conducción. Sin conexión, ambos quedan bloqueados.

## Conducir en modo manual

Entra a **Control Manual**. El joystick aparece en pantalla; arrástralo y la silla responde. Al soltarlo, se detiene. El botón de atrás frena antes de salir.

## Conducir con la mirada

Este modo son dos pantallas.

Primero la **calibración**. Mira a la cámara unos segundos: la app junta 100 muestras y decide cuál es tu ojo dominante.

Después la **navegación**. Elige velocidad —rápida o lenta— y mira al centro del punto rojo durante un segundo y medio. Eso activa el sistema. A partir de ahí, mira hacia la dirección que quieras: el borde de la pantalla marca las cuatro zonas. Para detenerte, cierra los ojos un segundo; aparecerá **Continuar** cuando quieras reanudar.

El vídeo de a bordo llega por WiFi, así que el teléfono debe estar conectado a la red de la cámara (`192.168.4.1`). El control Bluetooth funciona en paralelo.

## Si algo no anda

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| "Conecta la silla antes de iniciar" | No hay enlace Bluetooth | Empareja y conecta desde el menú |
| "No hay dispositivos vinculados" | Módulo aún no emparejado | Toca **Ir a Ajustes** y vincúlalo |
| Los botones de conducción están grises | Sin conexión activa | Vuelve al menú y conecta la silla |
| El vídeo no carga | El teléfono no está en la red de la cámara | Únete a la WiFi del módulo de cámara |
| La silla se frena sola al poco de avanzar | La app dejó de mandar el latido | Revisa la conexión; es la parada de seguridad |

Si nada de esto resuelve, el [Protocolo](protocolo.md) explica qué debería estar pasando por debajo.
