# SMS 5 minutos — proyecto Android

> **Estado:** prototipo funcional para SMS de texto en Android 11. No es una
> sustitución completa de Google Mensajes para MMS o RCS.

## Qué hace

Cuando la aplicación es la **aplicación SMS predeterminada** del teléfono:

1. Recibe cada SMS de texto entrante.
2. Lo guarda en la bandeja de entrada propia y muestra una notificación.
3. Programa la eliminación de **ese SMS concreto** 300 segundos después.
4. Permite leer el SMS y responder desde la conversación mientras exista.
5. Borra únicamente los SMS entrantes; los SMS enviados se conservan.

No hay filtro por número, contenido, contacto ni tipo de SMS. La eliminación
se programa localmente y no requiere Wi-Fi ni datos móviles. Android puede
entregar la alarma algo tarde si el teléfono está en reposo profundo, pero la
aplicación no la programa antes de los cinco minutos.

## Lo que no puede hacerse con Google Mensajes como predeterminada

No existe un permiso normal que se pueda conceder a otra aplicación para
borrar selectivamente los SMS de Google Mensajes. Android reserva la escritura
(incluido borrar) de la base de SMS para la aplicación SMS predeterminada.
Google Mensajes tampoco publica una API para que otra app gestione o borre sus
conversaciones. Automatizar toques por accesibilidad sería una alternativa
frágil y podría borrar un hilo completo.

Por ello, al activar este proyecto como app SMS predeterminada, Google Mensajes
deja de administrar los SMS entrantes.

## Limitaciones importantes de esta primera versión

- **SMS de texto sí; MMS no.** El manifiesto declara el receptor MMS que Android
  exige para la función SMS, pero esta versión no descarga, muestra ni envía
  MMS. No la uses como app predeterminada si recibes MMS importantes.
- **RCS no está incluido.** Los chats de Google Mensajes no pertenecen al
  proveedor de SMS estándar ni se pueden incorporar a una app SMS básica.
- La interfaz es deliberadamente simple: lista de mensajes, conversación y
  respuesta por SMS. No hay contactos, búsqueda, adjuntos, bloqueo ni copia de
  seguridad dentro de la app.
- Una copia de seguridad de SMS de Google hecha antes de eliminar un mensaje no
  se borra por esta app. Si el objetivo es privacidad, revisa la configuración
  de copias de seguridad por separado.
- Borrar del proveedor SMS elimina el mensaje de la bandeja local; no borra
  copias del remitente, del operador ni copias de seguridad previas.

## Antes de instalar

1. Haz una copia de los SMS que quieras conservar.
2. Prueba primero con un SMS normal enviado desde otro teléfono. **No uses un
   código bancario o un mensaje importante como primera prueba.**
3. Asegúrate de poder usar Android Studio o pide a alguien de confianza que
   compile el proyecto. No instales APK de páginas desconocidas: una app SMS
   predeterminada necesita permisos muy sensibles.

## Compilar e instalar

1. Copia la carpeta `AutoSMS5Min` a un ordenador con Android Studio actualizado.
2. Abre la carpeta como proyecto de Gradle. Android Studio descargará los
   componentes necesarios (compile SDK 35) si no los tiene.
3. Conecta el Motorola One Fusion por USB, activa **Opciones de desarrollador →
   Depuración USB**, o genera un APK firmado desde `Build → Generate Signed
   Bundle / APK`.
4. Instala el APK en el teléfono y abre **SMS 5 minutos**.
5. Pulsa **Configurar como app SMS predeterminada** y acepta el diálogo de
   Android.
6. Acepta los permisos de recibir, leer y enviar SMS.
7. En `Ajustes → Apps → SMS 5 minutos → Batería`, elige **Sin restricciones**
   / **No optimizar** para reducir retrasos de la alarma.
8. Envía un SMS de prueba desde otro número. Debe aparecer; a los cinco minutos
   debe desaparecer sin borrar los mensajes enviados ni el resto del hilo.

Para volver a Google Mensajes:

`Ajustes → Apps y notificaciones → Apps predeterminadas → Aplicación SMS →
Mensajes`.

Al cambiar de aplicación predeterminada, los SMS que no se hayan eliminado
seguirán normalmente en la base de datos del dispositivo. Los SMS que esta app
ya haya borrado no reaparecerán por cambiar de aplicación.

## Compilar desde el teléfono con GitHub Actions

El proyecto incluye `.github/workflows/build-apk.yml`. GitHub ejecutará la
compilación en un servidor Linux y guardará `app-debug.apk` como un artefacto.
No hace falta Android Studio ni Shizuku para esa compilación.

1. Crea un repositorio **privado** vacío en GitHub, por ejemplo `sms-5-minutos`.
2. Sube el contenido de esta carpeta al repositorio (incluida la carpeta oculta
   `.github`). Desde Android es práctico usar Termux con `git` o la interfaz
   web de GitHub en modo escritorio.
3. Al enviar la rama `main`, abre la pestaña **Actions** del repositorio. El flujo
   `Compilar APK de SMS 5 minutos` debe iniciarse automáticamente.
4. Cuando termine con una marca verde, abre la ejecución y descarga el artefacto
   `SMS-5-minutos-debug-APK`. Descomprime el archivo descargado y tendrás
   `app-debug.apk`.
5. Permite a Archivos/Chrome instalar apps desconocidas, instala el APK y sigue
   los pasos de configuración anteriores.

El APK de depuración se firma para pruebas. Para actualizar una instalación sin
problemas de firma o distribuir la app, debe configurarse una clave de firma de
lanzamiento propia; no publiques una clave privada ni SMS en GitHub.

## Archivos principales

- `IncomingSmsReceiver.java`: recibe y guarda el SMS entrante.
- `DeleteRegistry.java` y `DeleteScheduler.java`: conservan el ID y programan
  la eliminación a 300 000 ms.
- `DeleteAlarmReceiver.java`: borra solo el ID programado.
- `BootReceiver.java`: restaura alarmas pendientes después de reiniciar.
- `MainActivity.java` y `ConversationActivity.java`: bandeja básica y respuesta
  por SMS.

## Pruebas recomendadas

- Un SMS de un número cualquiera: debe desaparecer cerca de los cinco minutos.
- Dos SMS del mismo número separados por un minuto: cada uno debe eliminarse a
  sus propios cinco minutos, sin eliminar el otro antes.
- Responder al mensaje: el SMS enviado debe permanecer cuando el entrante se
  borre.
- Reiniciar el teléfono durante los cinco minutos: tras el reinicio, el SMS debe
  eliminarse al llegar (o superar) el momento pendiente.
