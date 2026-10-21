# Fugaz SMS — proyecto Android

> **Estado:** prototipo funcional para SMS de texto en Android 11–16. No es una
> sustitución completa de Google Mensajes para MMS o RCS.

## Novedades de la versión 0.28.8 (v0.28.8)

- **Barra superior bajada a la tarjeta de búsqueda:** los cuatro iconos
  (⚙ Configuración, ❓ Ayuda, 📦 Archivados, ⌛ Eliminados recientemente) ya no
  están en las esquinas de arriba, que con letra grande en pantallas altas eran
  lo más difícil de tocar. Ahora están un solo toque más abajo, detrás del botón
  ⋯ junto al buscador, a la altura del pulgar. La cabecera se queda solo con el
  título. El botón ✕ de limpiar búsqueda aparece únicamente cuando hay algo
  escrito, dejando sitio al ⋯.
- **La bandeja se actualiza sola y avisa en la línea de subtítulo:** la bandeja
  ya se redibuja cada segundo, así que ahora también se detectan y anuncian dos
  cambios que antes pasaban desapercibidos: que cambie la **duración del borrado**
  (por ejemplo al pasar de 5 a 10 minutos en Configuración) y que un mensaje sea
  **re-etiquetado** (por ejemplo de Normal a Posible spam) al añadir palabras
  clave nuevas. El aviso aparece appended al subtítulo («♻️ duración ahora 10
  minutos», «🔄 etiquetas actualizadas») durante un instante, sin avisos
  emergentes ni notificaciones: el texto se actualiza solo, sin que tengas que
  pedirlo.
- **Resaltado de palabras y fechas dentro de los mensajes:** en Configuración →
  Apariencia → RESALTAR EN LOS MENSAJES puedes (a) encender o apagar el
  resaltado de tus propias palabras, (b) encender o apagar el resaltado de las
  fechas de los mensajes entrantes y (c) elegir el estilo: Solo color, Color y
  negrita o Fondo marcado. Añades las palabras en PALABRAS QUE SE RESALTAN. El
  resaltado aparece en la bandeja y en las conversaciones, ignora mayúsculas y
  tildes, y **no modifica nunca el texto real del SMS** (solo se decora una
  copia para mostrar). Las palabras cortas se resaltan solo completas («PIN» no
  marca «opino») y las frases largas ganan sobre las cortas.
- Ayuda: la ventanita 5 de la guía explica el nuevo botón ⋯ y dónde está el
  resaltado. La guía sigue teniendo 10 ventanitas.

## Novedades de la versión 0.28.7 (v0.28.7)

- **Ventana de reinicio grande y obligatoria:** sustituye la tarjeta pegada a la
  bandeja. No se cierra con Atrás ni tocando fuera; un único botón REINICIAR
  aplica el nuevo plazo desde cero, sin segunda confirmación. El contenido se
  desplaza para que el botón siga accesible con letra grande. Si sales de la
  pantalla (Inicio, bloqueo, cambio de app o rotación), el aviso se cierra y se
  desactiva sin reiniciar los plazos. No reaparece al volver ni tras cerrar el
  proceso; un nuevo cambio de duración genera otro aviso. Esto sustituye la
  persistencia del aviso una vez mostrado de v0.28.6.
- **Letra más limpia y resaltado opcional:** tipografía sans-serif con más aire
  entre líneas. Configuración → Apariencia → LETRA permite elegir Normal o
  Resaltada (negrita), sin cambiar el tamaño ni seguir la escala del sistema.
- **Mensajes ordenados por bloques:** remitente, categoría, cuerpo, fecha y
  cuenta atrás separados, con jerarquía de tamaños y márgenes en bandeja,
  conversación, archivados e historial. No se modifica el texto real del SMS.
  En conversaciones, las burbujas de enviados/recibidos tienen margen lateral
  distinto y las acciones Compartir / Conservar / Eliminar van en una fila
  compacta. La cabecera también separa el contacto de la explicación de borrado.
- **Cuenta atrás sin reconstruir la conversación cada segundo:** mientras no
  cambien sus datos, solo se actualiza el texto del temporizador, evitando
  interrumpir los toques sobre acciones o el desplazamiento.

## Novedades de la versión 0.28.6 (v0.28.6)

- **↻ REINICIAR en cada bandeja:** al cambiar la duración de los entrantes o
  de eliminados recientemente aparece una tarjeta superpuesta sobre los mensajes,
  con un texto específico para esa bandeja y el nuevo tiempo. El aviso queda
  guardado aunque cierres la app. Cambiar el ajuste no modifica los plazos de
  mensajes existentes: al pulsar REINICIAR y confirmar empiezan desde cero.
- **Historial con plazos independientes:** acortar o alargar la duración solo
  afecta a nuevas entradas hasta reiniciar. Se conserva la fecha real de eliminación;
  no se recupera nada ya vencido. En entrantes solo se reinician cuentas pendientes;
  «Nunca» las cancela, sin tocar enviados ni conservados.
- **Iconos superiores sin recortes al reducir tamaño:** se eliminan los rellenos
  y mínimos heredados de los botones de Android en los iconos de la bandeja y
  en Volver. El texto sigue calculándose en píxeles con Ui.text, sin seguir
  el tamaño de letra del sistema. Pendiente de validación física en Samsung A16.

## Novedades de la versión 0.28.5 (v28.5)

- **Atajo para volver al tamaño 100 % con toques al fondo:** si la bandeja se
  queda muy grande (por ejemplo en 150 %), ya no hace falta buscar el botón
  «Aa TAMAÑO»: toca el **fondo de la pantalla inicial 8 veces seguidas** (en
  unos segundos, sin tocar mensajes ni botones) y la app pregunta «¿La
  restablezco al 100 % (el tamaño con el que empiezan todos los teléfonos)?».
  Al confirmar, la interfaz vuelve al tamaño de fábrica al instante. Solo
  cambia el tamaño: no se borran mensajes ni se toca ninguna otra
  configuración. Si ya estás en el 100 %, el atajo solo te lo confirma con un
  aviso.
- **Guía ampliada a 10 ventanitas:** el mensaje 10 («🔁 Volver al tamaño
  100 % (atajo)») explica el gesto y la otra vía (el botón «Aa TAMAÑO» de la
  bandeja, o «TAMAÑO DE INTERFAZ» en Configuración → Apariencia, con «Usar
  100 % (predeterminado)»); la bienvenida ya dice 10 ventanitas.

## Novedades de la versión 0.28.4 (v28.4)

- **Deslizador de tamaño con rango seguro (70 %–150 %):** el botón «Aa TAMAÑO»
  (bandeja y Configuración → Apariencia) ya no permite bajar al 40 % ni subir
  al 200 %. Los extremos anteriores dejaban la interfaz inservible en algunos
  teléfonos (letras cortadas, botones amontonados o todo fuera de la
  pantalla), así que ahora el deslizador se mueve solo entre **70 % y 150 %**,
  que es donde la app se ve bien de verdad. El diálogo lo explica como «rango
  seguro» y conserva los botones de −1 % / +1 %, la vista previa en vivo y el
  atajo «Usar 100 % (predeterminado)».
- **Auto-reparación de un tamaño extremo guardado:** si el teléfono quedó con
  un tamaño guardado fuera de 70 %–150 % (por ejemplo, elegido con la versión
  anterior), al abrir la app vuelve **solo al 100 %** y lo corrige en los
  ajustes. No se borra ninguna otra configuración ni ningún mensaje: solo se
  repara ese valor.

## Novedades de la versión 0.28.3 (v28.3)

- **Tamaño fijo predeterminado en todos los teléfonos:** la interfaz ya no
  sigue el tamaño de letra ni el tamaño de pantalla configurados en Android.
  Todos los teléfonos arrancan igual en **100 %** (el diseño mide 360 dp de
  ancho y se adapta proporcional a cada pantalla), así que ya no queda «muy
  grande» en unos y «diminuta» en otros por la configuración del sistema.
  También se quitaron los aumentos automáticos de antes (85 %–150 % y el
  empuje en pantallas angostas) que cambiaban el tamaño sin avisar.
- **Deslizador de tamaño del 40 % al 200 %:** el botón «Aa TAMAÑO» (bandeja y
  Configuración → Apariencia) ahora abre un **deslizador** con 161 tamaños
  posibles (de 1 % en 1 %), botones de −1 % / +1 % para ajustar fino, vista
  previa en vivo de un mensaje mientras lo mueves y el botón «Usar 100 %
  (predeterminado)» para volver al tamaño de fábrica de la app. Se aplica al
  instante en todas las pantallas al tocar APLICAR.
- **Todo respeta el tamaño elegido:** se pasó a escala propia (píxeles
  calculados por la app) en textos, botones, campos, diálogos y burbujas que
  antes seguían al sistema por separado (borrar búsqueda, escribir SMS,
  conservar/eliminar, accesos rápidos del temporizador, pies de pantalla…),
  para que el deslizador mueva la interfaz completa al unísono.

## Novedades de la versión 0.28.2 (v28.2)

- **Sección «Batería e internet» en Configuración** con los dos mensajes:
  (1) la app NO usa internet (sin permiso de red; los SMS llegan por la señal
  del operador, los datos en segundo plano no le hacen falta) y (2) para
  algunas funciones es necesario poner la batería en «Sin restricciones» /
  «No optimizar», con un botón que abre directo esos ajustes de Android.
- **Ventanita 9 de la guía:** «🔋 Batería e internet» al final del recorrido,
  que lleva a esa sección y la resalta; la bienvenida ya dice 9 ventanitas.
- **Diagnóstico de recepción** añade dos líneas: internet no necesario y el
  consejo de batería sin optimizar.

## Novedades de la versión 0.28.1 (v28.1)

- **Elegir contacto al escribir un SMS:** la pantalla «✏️ SMS nuevo» trae el
  botón **👤 ELEGIR CONTACTO**, que abre el selector de contactos de Android
  (los del teléfono y los de la cuenta de Google, con buscador) **sin pedir
  permisos nuevos**: al elegir, el número se carga limpio y se muestra el
  nombre. Si además ya diste el permiso opcional de contactos, el nombre
  aparece solo al escribir cualquier número conocido.

## Novedades de la versión 0.28.0 (v28)

- **✏️ ESCRIBIR SMS:** botón nuevo en la bandeja para redactar y enviar un SMS
  a cualquier número (con validación y aviso si el número está bloqueado). Lo
  enviado queda en la conversación y no se borra solo; sale por la línea
  elegida en Configuración.
- **Tamaño de la interfaz ajustable:** botón «Aa TAMAÑO» en la bandeja y en
  Configuración → Apariencia con 5 tamaños (85 % a 150 %). Escala textos,
  botones, iconos y espaciados de toda la app al instante: útil en teléfonos
  donde todo se ve diminuto (como el Motorola de la familia).
- **Un sonido por categoría de mensaje:** las notificaciones entrantes usan
  tres canales de Android —«SMS importantes», «SMS entrantes (normales)» y
  «Posible spam»— y cada uno tiene su propio sonido elegible en los ajustes
  de notificaciones del sistema (importantes suena más fuerte, el spam nace
  silencioso). Nota nueva en Configuración y en la guía que lo explica.

## Novedades de la versión 0.27.9 (v27.9)

- **El temporizador se detiene si reactivas el sonido por fuera:** con la
  cuenta atrás en marcha, si el sonido vuelve por fuera de la app (botones,
  ajustes u otra app), el temporizador se **cancela** en vez de quedar
  «esperando» como si siguiera en silencio, y llega el aviso «Parece que
  activaste el sonido de notificación. El temporizador se detuvo y estoy
  atento para cuando lo vuelvas a desactivar».
- **Desde el nuevo aviso se reactiva todo:** trae las acciones **SILENCIAR Y
  ACTIVAR (tu tiempo)** —silencia el teléfono y arma el temporizador con el
  tiempo configurado, de un toque— y **ELEGIR TIEMPO** (abre la pantalla del
  temporizador). Deslizar el aviso fuera descarta el estado «atento».
- **Sin notificaciones innecesarias:** con el temporizador cancelado ya no
  llega el aviso de «sonido restaurado» al terminar con el sonido ya activo,
  y si la vigilancia opcional está apagada el servicio de vigilancia se
  detiene (su aviso permanente desaparece).
- **Atento de verdad:** mientras el aviso está presente la vigilancia sigue
  viva aunque el interruptor opcional esté apagado; si vuelves a silenciar el
  teléfono, llega el aviso de activación de un toque como siempre.

## Novedades de la versión 0.27.8 (v27.8)

- **Color de resaltado por tema:** en tema claro la guía sigue señalando con
  el fondo ámbar; en tema oscuro vuelve al **celeste** de antes de v27.4
  (el bronce apenas se distinguía sobre el fondo oscuro). Cada tema tiene su
  propio color de señal.
- **Copia completa de todo (Datos):** EXPORTAR/IMPORTAR COPIA COMPLETA
  (antes «ajustes») ahora guarda también los ARCHIVADOS, los ELIMINADOS
  RECIENTEMENTE y los temporizadores pendientes (borrado automático y
  temporizador de silencio), además de ajustes, tema, bloqueados, palabras
  clave, PIN y huella. Al importar, los borrados pendientes se reprograman
  solos. Los archivados/eliminados viajan como referencias a los SMS del
  sistema: vuelven a su sitio en el mismo teléfono o tras restaurar también
  esos SMS.
- **Detalles:** textos y avisos de la copia más explícitos, nombre de archivo
  sugerido `fugaz-sms-copia-completa.json`, y limpieza de una clave muerta
  (`help_last_step`) en el exportador.

## Novedades de la versión 0.27.7 (v27.7)

- **Arreglo: «← Atrás» de la guía ya retrocede.** El botón solo cerraba la
  ventanita sin llamar al paso anterior; ahora vuelve de verdad a la
  ventanita previa (con su sección resaltada detrás).
- **La guía ❓ abre siempre desde la primera ventanita.** Ya no reanuda en la
  sección donde la dejaste: al cerrar en la última quedaba atascada ahí y
  había que retroceder una por una hasta la primera.
- **La huella ahora se encuentra aunque aún no haya PIN:** el botón
  DESBLOQUEO CON HUELLA está siempre visible en Privacidad; sin PIN creado
  dice «NO (PRIMERO CREA TU PIN)» y al tocarlo explica que la huella es una
  forma alternativa de desbloqueo y ofrece crear el PIN en el momento.
  Sigue siendo opcional y apagada por defecto; el PIN continúa siendo el
  respaldo.

## Novedades de la versión 0.27.6 (v27.6)

- **Desbloqueo con huella (opcional):** en Privacidad aparece el botón
  **DESBLOQUEO CON HUELLA**, una opción **apagada por defecto** que solo se
  enciende cuando tú la confirmas con tu huella (Android 9+). Se puede apagar
  cuando quieras; si el teléfono no tiene huellas o no hay sensor, se pide el
  PIN como siempre. El bloqueo al abrir sigue **desactivado por defecto**:
  nada se activa solo.
- **La guía (❓) lo menciona y lo señala:** nueva ventana «🔒 PIN o huella
  (opcional)» que explica dónde está la opción de bloquear, con el botón
  BLOQUEO AL ABRIR **resaltado con el fondo parpadeante** mientras la ventana
  está abierta (bronce en tema oscuro, ámbar en claro).
- **Arreglo del resaltado:** al terminar de parpadear se restaura el fondo
  original de la sección apuntada (los botones, como ① CONFIGURACIÓN
  AUTOMÁTICA, ya no quedan sin su fondo).

## Novedades de la versión 0.27.5 (v27.5)

- **Bloqueo con PIN (Privacidad):** crea un PIN de 4–10 dígitos en
  Configuración → Privacidad; se pide al abrir la bandeja (y al abrir una
  conversación por notificación), con pantalla tapada mientras tanto, y
  vuelve a pedirse si la app estuvo más de 5 segundos en segundo plano.
- **No leídos:** contador 🟡 en el subtítulo de la bandeja; tócalo para ver
  solo lo sin leer y toca de nuevo para volver. Al abrir la conversación se
  marca como leída.
- **Exportar/importar ajustes (Datos):** guarda tus preferencias, lista de
  bloqueados, palabras clave y PIN (hash) en un archivo JSON y restáuralas en
  otro teléfono.
- **«¿POR QUÉ?» en la barra amarilla:** un toque abre el diagnóstico de
  recepción directamente (junto a REPARAR).
- Pendiente: versión en inglés (requiere extraer todos los textos a
  resources; se hará en una versión dedicada para no dejar traducciones a
  medias).

## Novedades de la versión 0.27.4 (v27.4)

- **Ayuda que señala:** el resaltado de la sección ahora **parpadea de forma
  continua** mientras dura su ventana; al cambiar de paso (Siguiente/Atrás) se
  detiene el anterior y arranca el nuevo sin duplicarse, y al cerrar todo se
  apaga. Color aparte del azul: **ámbar** en tema claro (más oscuro que el
  fondo, más claro que el texto) y **bronce** en tema oscuro.
- **Sonido restaurado a mano:** si el temporizador está corriendo y vuelves a
  poner el sonido por fuera de la app, aparece «Parece que activaste solo el
  sonido. Estoy a la espera de cuando lo desactives» (aviso + estado en la
  pantalla del temporizador), el conteo sigue esperando y **al terminar no se
  muestra el mensaje de "Sonido restaurado" si todo ya está activo**.
- La vigilancia se mantiene viva mientras haya un temporizador en curso (aunque
  tengas apagado el interruptor de vigilancia opcional).

## Novedades de la versión 0.27.3 (v27.3)

- **Arreglo: vigilancia de silencio reparada** tras actualizar (Android mata
  el servicio al actualizar la app; ahora se reinicia solo al abrir la app,
  con el reinstalado (`MY_PACKAGE_REPLACED`) y en el arranque).
- **Arreglo: el receptor de cambio de sonido** usaba `RECEIVER_NOT_EXPORTED`,
  con el que Android **no entrega** muchos broadcasts del sistema (como
  `RINGER_MODE_CHANGED`) — ahora usa `RECEIVER_EXPORTED`, que es el correcto
  para broadcasts del sistema (protegidos: solo el sistema puede enviarlos).
- **Ahora detecta No Molestar 🤫** además de silencio y vibración: observa
  `zen_mode` y muestra el mismo aviso para activar el temporizador de un toque.
- **Aviso en canal propio de alta prioridad** (banners) para que el aviso de
  activar el temporizador nunca se pierda, con permiso para saltarse el
  No Molestar si diste el acceso.
- **«ACTIVAR» también funciona con No Molestar puro** (modo de timbre normal):
  solo programa el retorno sin forzar el silencio.

## Novedades de la versión 0.27.2 (v27.2)

- **Ayuda que señala:** el ❓ ya no solo informa: cada ventana de la guía
  **lleva la pantalla a la sección de Configuración que describe y la resalta**,
  con **← Atrás / Siguiente →** para moverse entre mensajes; al reabrirla
  continúa donde la dejaste. Desde la bandeja, ❓ abre Configuración con la guía.
- **Barra amarilla clicable:** tocar la barra (o REPARAR) va directo al paso
  que falta.
- **Sugerencia en la bandeja vacía:** «puedes configurar el sonido al
  desactivarlo, o configurarlo manualmente» (abre el temporizador).
- **Recordatorio a los 3 días** si la configuración sigue incompleta (una vez,
  con «Configurar ahora»).
- **🎉 Todo listo** al completar la configuración (una vez, con opción de
  probar el sonido) y **mensajes de Configuración ocultables** (💬 toggle).

## Novedades de la versión 0.27.1 (v27.1)

- **Ayuda integrada:** icono ❓ en la bandeja y en Configuración abre una
  guía de 7 ventanitas flotantes (se cierran con ✕, avanza con «Siguiente»)
  con todas las definiciones y mensajes largos, que ahora van cortos en
  Configuración. En Ayuda también: mensaje de bienvenida para verlo otra vez.
- **Bienvenida única:** al abrir la app por primera vez o tras una
  actualización aparece un mensaje corto con los 2 pasos iniciales; no vuelve
  a aparecer hasta la próxima versión (mi hermano ya la abrió antes, al
  actualizar verá el mensaje).
- **Checklist de estado** en Configuración: ✅/❌ de app predeterminada,
  permisos, notificaciones y alarmas en una sola vista (o «Todo listo»).

## Novedades de la versión 0.27.0 (v27)

- **Configuración automática primero:** el botón de app predeterminada va
  separado y debajo. El automático abre directo la info de la app para
  «Permitir ajustes restringidos» (necesario en Android 15/16 con APK),
  luego permisos, notificaciones, alarmas y al final la app predeterminada.
  Al volver a la app el flujo continúa solo.
- **Interfaz que escala:** textos y botones respetan el tamaño de letra del
  sistema y se agrandan un poco en pantallas chicas (Motorola y similares).
- **Android 16:** `targetSdk 36`, servicio de vigilancia con tipo
  `specialUse` exigido desde Android 14+.

## Novedades de la versión 0.26.0 (v26)

- **Vigilar silencio de otras apps:** si Volume Styles, el sistema u otra
  app pone el teléfono en silencio o vibración, llega una notificación
  con Activar (tu tiempo automático) de un toque, sin abrir la app.
  Nada se arma solo: tú decides. Opcional, con aviso permanente
  discreto; no detecta No molestar puro (Android no lo avisa).

## Novedades de la versión 0.25.0 (v25)

- **Arreglo urgente:** el Temporizador de silencio se cerraba al abrirlo
  porque faltaba registrar la pantalla en el manifiesto. Ya abre normal
  (no era un permiso).
- **Saldo de mis líneas:** consulta el saldo por SIM con los canales
  oficiales (Kolbi: *888# o SMS SALDO al 8888; Liberty/Movistar: SMS
  SALDO al 606; Claro: *611#). El SMS se envía desde la SIM que elijas
  y la respuesta llega como SMS normal; también abre el menú de la SIM
  del operador. Android no permite a las apps leer el saldo directo.

## Novedades de la versión 0.24.0 (v24)

- **Temporizador de silencio:** Configuración → Sonido. Silencia o vibra el
  teléfono AHORA y el sonido vuelve solo después de los minutos que escribas
  (accesos: 30 min, 1 h, 2 h, 4 h o manual 1–720). Si usas No molestar,
  dale el permiso extra una sola vez. Sobrevive reinicios.
- **Cuenta atrás con segundos:** Eliminados recientemente ya no parece
  atascado en horas; ahora muestra segundos siempre (7 h 23 min 45 s).

## Novedades de la versión 0.23.0 (v23)

- **Aplicar tiempo a pendientes:** al cambiar el tiempo de borrado con
  cuentas en curso, se ofrece reiniciarlas con el nuevo tiempo (o
  cancelarlas si cambias a Nunca). Antes solo aplicaba a mensajes nuevos y
  parecía no actualizarse.
- **Mis palabras clave:** añade tus propias palabras de spam o importantes;
  las tuyas tienen prioridad sobre las listas internas. Ideal para tiendas
  que te spamean o avisos que se marcan mal.
- **Historial con etiquetas vivas:** Eliminados recientemente reclasifica
  con las reglas actuales (y tus palabras) en vez de mostrar la etiqueta
  vieja guardada al borrar.

## Novedades de la versión 0.22.0 (v22)

- **Clasificador en 3 niveles:** los códigos, seguridad, salud y alertas de
  fraude siempre ganan como importantes; luego se detecta spam/promos
  (nuevas palabras: cupón, adelanto, recargue, canje, puntos, off,
  publicidad); palabras como saldo o banco solo marcan importante si no hay
  señal de promo. Las promos de operadora ya no se quedan conservadas.
- **Bloquear + borrar de una vez:** al bloquear un número se ofrece eliminar
  sus mensajes existentes (van a Eliminados recientemente por seguridad).
- **La conversación vacía se cierra sola** al eliminar su último mensaje, y
  ahora tiene botón atrás (‹) propio.
- La ayuda de clasificación explica los 3 niveles y el orden de prioridad.

## Novedades de la versión 0.21.0 (v21)

- **Nombres y fotos de contactos:** la bandeja, conversaciones,
  notificaciones, archivados, eliminados y bloqueados muestran el nombre
  (permiso opcional READ_CONTACTS, pedido junto al resto). Sin permiso, se ven
  los números como siempre. La bandeja usa avatares de letra estilo Gmail.
- **Buscador:** en la bandeja (por nombre, número o texto) y dentro de cada
  conversación (por texto), con contador de resultados.
- **Probar sonido de notificación:** nuevo botón que envía una notificación de
  prueba para distinguir un problema de la app de uno del volumen/DND/otra
  app. Además, las notificaciones ahora piden sonido+vibración explícitos
  (audibles también en Android 6/7).
- El diagnóstico muestra el estado del permiso de contactos.

## Novedades de la versión 0.20.1 (v20.1)

- **Corrección:** el botón Abrir Google Mensajes decía "no está instalado"
  aunque sí lo estaba. Causa: desde Android 11 las apps no pueden ver qué
  otras apps hay instaladas salvo que lo declaren. Se añadió `<queries>` para
  Google Mensajes en el manifiesto (permiso de visibilidad, no da acceso a
  datos). Sin cambios de funciones.

## Novedades de la versión 0.20.0 (v20)

- **Google Mensajes y RCS:** nueva sección con un botón que abre Google
  Mensajes directamente y guía paso a paso para pasar los chats a solo SMS
  (global o por contacto). Si no está instalado, ofrece abrir Play Store.
- **Aviso en lenguaje simple:** la app explica que solo recibe SMS de texto
  (señal del celular, sin internet) y no chats por internet (Wi-Fi o datos)
  como los de Google Mensajes o iPhone. Ideal para quien no sabe qué es RCS.
- **Nuevo nombre:** la app se renombra con una marca más original y
  profesional (el paquete técnico no cambia, las actualizaciones siguen
  funcionando).

## Novedades de la versión 0.19.0 (v19)

- **Apariencia automática:** nuevo modo "Automático (según el teléfono)" que
  sigue el modo claro/oscuro del celular. Tu ajuste anterior se respeta; los
  nuevos usan automático.
- **Recuperar eliminados:** toca un registro en Eliminados recientemente para
  devolver el mensaje COMPLETO a la bandeja o a Archivados (queda conservado,
  sin borrado automático). El historial ahora guarda el texto completo hasta
  que venza; sigue sin ser una copia permanente.
- **Diagnóstico de recepción:** nuevo botón que muestra estado real (rol,
  permisos, último SMS recibido, borrados pendientes) y explica por qué los
  chats RCS nunca llegan a esta app ni a ninguna tercera (Google no da acceso).
- **Aclaración RCS:** no es posible recibir RCS ni combinar RCS+datos en esta
  app. Para recibirlo todo como SMS, desactiva Chats RCS en Google Mensajes.

## Novedades de la versión 0.18.0 (v18)

- **Deslizar estilo Gmail:** desliza un mensaje en la bandeja para eliminarlo
  (rojo) o archivarlo (verde). Cada lado se configura por separado en
  Configuración → Deslizar en bandeja (Nada / Eliminar / Archivar).
- **Archivados:** los mensajes archivados se ocultan de la bandeja y no se
  borran solos. Nueva pantalla Archivados (botón 📦) para verlos, devolverlos
  a la bandeja o eliminarlos definitivamente.
- **Borrado manual:** mantén presionado cualquier mensaje de la bandeja para
  abrirlo, archivarlo o eliminarlo. Los SMS enviados ahora también tienen
  botón Eliminar en la conversación (se conservan por diseño, pero ya se
  pueden quitar a mano).
- **Actualizar estado de mensajes:** nuevo botón que programa los entrantes
  que no tenían cuenta atrás (por ejemplo, de antes de una actualización),
  conserva los que tus ajustes digan y limpia programas viejos.

## Novedades de la versión 0.17.0 (v17)

- **Cuenta atrás en vivo:** la bandeja muestra "Se elimina en X min Y s"
  actualizada cada segundo, y "Eliminados recientemente" muestra cuánto falta
  para que cada vista previa se borre del historial.
- **Modo oscuro completo:** todos los diálogos y botones usan la tarjeta gris
  redondeada; ya no aparecen paneles blancos en modo oscuro.
- **Fuentes grandes:** todos los selectores (tiempo de borrado, tiempo de
  historial) se desplazan para alcanzar cada opción.
- **Configuración automática:** al abrir la app pide los permisos que falten y
  el botón Reparar / Configuración automática guía paso a paso (rol SMS,
  notificaciones, alarmas exactas). Corrige el estado de predeterminada en
  Android 12+ (RoleManager) y el bloqueo de notificaciones en Samsung/Android 13+.
- **Borrado por tipo:** en Configuración se elige si se borran solos los
  normales, los posible spam y los importantes (por defecto los importantes se
  conservan). Listas de palabras clave ampliadas en ambos tipos.
- **Alarmas exactas con plan B:** si Android 12+ deniega las alarmas exactas,
  el borrado sigue funcionando (puede retrasarse un poco) en vez de no ocurrir.
- **Nuevo icono:** tu diseño azul (reloj de arena + temporizador 5 min +
  burbuja de mensaje) sobre fondo blanco estilo Google, con tamaño y margen
  correctos para que el launcher no lo recorte. APK más liviano (recursos sin
  uso eliminados).

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
4. Instala el APK en el teléfono y abre **Fugaz SMS**.
5. Pulsa **Configurar como app SMS predeterminada** y acepta el diálogo de
   Android.
6. Acepta los permisos de recibir, leer y enviar SMS.
7. En `Ajustes → Apps → Fugaz SMS → Batería`, elige **Sin restricciones**
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
   `Compilar APK de Fugaz SMS` debe iniciarse automáticamente.
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
- Ventana de reinicio: cambia la duración de eliminación en Ajustes y vuelve a
  la bandeja. Debe abrirse una ventana grande con un solo botón REINICIAR; no
  se cierra con Atrás ni tocando fuera. Al pulsar REINICIAR, las cuentas
  pendientes empiezan desde cero y la fecha real de borrado no cambia. Si sales
  de la app con la ventana abierta, debe cerrarse sin reiniciar los plazos y no
  volver a aparecer hasta que cambies la duración otra vez.
- Letra resaltada: en Ajustes → Apariencia → LETRA elige «Resaltada (negrita)».
  El texto debe verse más grueso sin cambiar de tamaño, y los mensajes deben
  seguir mostrándose completos, con remitente, categoría, cuerpo, fecha y
  cuenta atrás separados.
