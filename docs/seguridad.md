# Seguridad

Esto mueve una silla de ruedas. Las paradas automáticas no son un detalle: son la parte más importante de la app. Vale la pena conocerlas antes de tocar un botón.

## Qué frena la silla, y cuándo

| Situación | Qué hace el sistema |
|---|---|
| El Bluetooth deja de entregar órdenes por más de 350 ms | El firmware apaga los motores y vuelve al centro |
| El usuario cierra los ojos más de 1 s durante la navegación ocular | Se manda la parada, se bloquea el movimiento y aparece el botón **Continuar** |
| Se pierde la conexión Bluetooth mientras se navega | La app avisa y cierra la pantalla de navegación |
| Se suelta el joystick en pantalla | La posición vuelve al centro |
| Se sale de cualquiera de las pantallas de control | Se envía la trama de parada `(125, 125)` |
| Se pulsa atrás en el joystick | Se envía la trama de parada antes de salir |
| Se abre la navegación ocular sin silla conectada | La pantalla se cierra y avisa |

La parada por ojos cerrados es a propósito, y no se recupera sola: hay que tocar **Continuar**. Es intencional que cueste reanudar; así un parpadeo largo no se convierte en un accidente.

## El cinturón de seguridad del firmware

De todos los frenos, el más confiable es el del Arduino. No depende de la app ni del teléfono. Si el puerto serie se queda callado 350 ms —porque se cayó el enlace, porque se cerró la app, porque el celular se apagó— los motores quedan en cero.

Por eso la app insiste con un latido cada 100 ms. No es ruido: es la señal de "sigo aquí". Sin ese latido, la silla entiende que perdió el control y se detiene.

!!! danger "No confíes solo en el software"
    Ninguna parada automática reemplaza una parada física. Antes de operar la silla con una persona encima, ten siempre a mano un corte de energía del motor y prueba cada escenario de la tabla por separado.
