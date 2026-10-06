# Contribuir a EyePilot

¡Gracias por tu interés en colaborar! Cualquier ayuda es bienvenida: código, documentación, reporte de errores o ideas.

## Cómo reportar un error

Abre un [issue](../../issues) e incluye:

- Descripción breve del problema
- Pasos para reproducirlo
- Comportamiento esperado vs. obtenido
- Versión de Android del dispositivo (si aplica)

## Configuración del entorno

1. Clona el repositorio
2. Ábrelo en Android Studio (con JDK 17)
3. Verifica que compila:

```bash
./gradlew assembleDebug
```

## Proceso para pull requests

1. Crea una rama descriptiva, por ejemplo `feature/control-bluetooth` o `fix/botón-panico`
2. Haz cambios pequeños y enfocados; un PR por funcionalidad o corrección
3. Mensajes de commit en español, breves y descriptivos
4. Describe en el PR qué cambias y por qué

## Convenciones

- Código en Kotlin siguiendo el estilo estándar de Android Studio (Ktlint como referencia)
- Comentarios e identificadores en inglés, documentación de usuario en español

## Licencia

Al contribuir, aceptas que tus cambios se distribuyan bajo la misma licencia del proyecto ([BOLA v1.1](LICENSE)).
