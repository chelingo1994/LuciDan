# LuciDan App — Contexto del Proyecto

> Este archivo se carga automáticamente al abrir Claude Code en esta carpeta.
> Resume todo lo definido en la documentación RUP/Scrum del proyecto para que
> el desarrollo de código continúe sin perder las decisiones ya tomadas.

## 1. Qué es el proyecto

**LuciDan App**: **guía de negocios geolocalizada de Bermejo, Tarija,
Bolivia** (ciudad fronteriza con Argentina, paso de Aguas Blancas). La app
responde una sola pregunta central: **"¿qué negocios tengo a mi disposición
alrededor de mi ubicación?"** — qué venden, dónde están, en qué horario y
cómo contactarlos. Referencia de producto: **uruku.bo** (directorio web de
negocios de Bermejo), llevado a app Android nativa. Ya **no es una app de
turismo**: la entidad `Lugar` (lugares turísticos) fue eliminada del modelo
(ver 2.2). Proyecto académico y personal de **Marcelo Justiniano**, quien
asume TODOS los roles: Patrocinador, Product Owner, Scrum Master y único
desarrollador.

Dos componentes:
- **App móvil** (Android, Kotlin): mapa interactivo con los negocios de la
  ciudad explorables por radio configurable y por rubro, ficha de detalle
  (qué vende, fotos, horario, teléfono, dirección), cómo llegar, reseñas,
  favoritos, notificaciones de cercanía.
- **Panel web** (React + Node.js): gestión de perfiles de negocio, planes de
  publicidad destacada, estadísticas de interacción.

Presupuesto total estimado: **Bs 76 375** (ver `docs/03_Acta_de_Constitucion_LuciDan.pdf`
para el desglose completo). Duración estimada: 9 meses, 3 sprints.

## 2. Pila tecnológica

| Capa | Tecnología |
|---|---|
| App móvil | Kotlin, arquitectura por capas (Screen → ViewModel → Repository → DAO) |
| Interfaz | Jetpack Compose + Material 3 (tema claro/oscuro, ver sección 3) |
| Imágenes | Coil 3 (fotos de negocios desde recursos, archivos o URL) |
| Persistencia local | Room (SQLite), versión 14 |
| Backend / Panel web | Node.js + React |
| Base de datos central | PostgreSQL + extensión PostGIS |
| Mapas y geolocalización | **osmdroid** (tiles de OpenStreetMap) para el mapa + **OSRM** (servidor demo público `router.project-osrm.org`) para calcular rutas — sin API key, sin facturación, sin tarjeta |
| Notificaciones | Notificaciones locales del sistema (`NotificationService` + `GeofenceManager`) — sin servidor externo |
| Autenticación | PBKDF2 + Room local (`AuthService`) — sin servidor externo |
| Diseño UX/UI | Figma |
| Gestión de tareas | Trello |
| Pruebas | JUnit (51 pruebas) + pruebas manuales en teléfono físico (Infinix HOT 11S NFC, ADB inalámbrico) |

**Nota (actualizada 2026-10-02):** se usa **Cloud Firestore** (plan Spark,
sin tarjeta) como base compartida de `Negocio`, con Room como caché local
(`NubeService` + `sincronizar()` en `NegocioRepository`). Esto reemplaza por
ahora a PostgreSQL + PostGIS y al backend Node. Siguen locales: login
(PBKDF2 + Room, sin Firebase Authentication), favoritos,
estadísticas, contrataciones y notificaciones de cercanía. **Las reseñas también viajan a Firestore** (colección `resenas`, autor = hash del correo; Room v12; promedio calculado en el teléfono). Las reglas están
en `firestore.rules` (solo validan forma: sin Authentication el servidor no
sabe quién escribe). `app/google-services.json` no se commitea.

**Sección «Info útil» (2026-11-19, EPIC07: HU16 y HU17 + clima) — DESACTIVADA por decisión de Marcelo: se aplicará en una fase posterior.** El código sigue en el repo pero sin ruta ni acceso desde Home (para reactivarla: volver a registrar `INFO_UTIL`/`EDITAR_FRONTERA` en `LuciDanNavHost` y el ícono ⓘ + ítem de menú en `HomeScreen`). Diseño: ícono ⓘ en la barra de Home (y ítem en su menú) →
`InfoUtilScreen` con tres tarjetas: cambio Bs↔ARS (calculadora en ambos sentidos), clima en Bermejo y paso fronterizo.
- Firestore `info_util/cambio` = `{compraArsPorBs, ventaArsPorBs, actualizado (ms), adminId}`; tasas en **ARS por 1 Bs**
  (compra = lo que recibes al cambiar Bs→ARS, venta = lo que cuesta 1 Bs al cambiar ARS→Bs; `compra <= venta`).
- Firestore `info_util/frontera` = ocho textos (`horarioPaso`, `horarioChalanas`, `tarifasChalanas`, `recomendaciones`,
  `telPolicia`, `telBomberos`, `telAmbulancia`, `telMigracion`) + `actualizado` + `adminId`; por defecto «Por confirmar».
- Solo el ADMINISTRADOR ve «Actualizar tasa» y «Editar información» (`EditarFronteraScreen`); lo exige la app, no las reglas.
- Fuentes externas sin clave: **open.er-api.com** (cruce `rates.ARS / rates.BOB`, solo referencia; atribución «ExchangeRate-API»)
  y **Open-Meteo** (clima, 3 días; atribución «Open-Meteo.com»). Caché de todo en SharedPreferences `info_util_cache`
  (no toca Room); el clima se vuelve a bajar si tiene más de 30 min.
- Código: `ui/infoutil/*`, `data/remote/InfoUtilService.kt`, `data/repository/InfoUtilRepository.kt`,
  `data/model/InfoUtil.kt`, `util/Cambio.kt`, `util/ClimaCodigos.kt`, `util/Tiempo.kt`.
- `firestore.rules`: `info_util/{doc}` lectura pública; create/update solo con forma válida (`cambioValido`, `fronteraValida`);
  delete prohibido. También cubre `resenas` (solo create).

### Planes de publicidad con beneficios (HU10 ampliada, 2026-11-25)

- Niveles definidos SOLO en `util/BeneficiosPlan.kt`: 1 Semanal (insignia «Destacado», pin con estrella, primero en la
  lista, visible fuera del radio), 2 Mensual (+ aviso de cercanía y estadísticas por período), 3 Trimestral (+ carrusel
  «Destacados» y etiqueta «Más conveniente»). `VISIBLE_SIN_FILTRO_RUBRO_NIVEL3 = false`.
- Reglas puras en `util/Destacados.kt`. La vigencia se calcula al leer (`planNivel` y `destacadoHasta`); el booleano
  `destacado` solo no basta (los «SI» de la semilla ya no destacan sin plan).
- `negocios` suma `planNivel` y `destacadoHasta` (ms), y `planes_publicidad` suma `nivel` (Room 13). Los tres campos del
  negocio viajan a Firestore; `firestore.rules` no cambió.
- Las contrataciones siguen locales: `ContratacionRepository.aplicarPlanes()` pone en cada negocio el mejor plan vigente
  de este teléfono y nunca baja uno vigente que venga de la nube. Sin pagos reales.
- Sin plan o con nivel 1, las estadísticas muestran solo totales.
- Fotos por nivel 0/1/2/3: 2/3/5/6. El límite solo impide agregar; al vencer no se borra nada, se muestran las
  primeras N (`LimiteFotos`) y «Mis negocios» avisa. En Firestore las fotos comparten el documento (900 000
  caracteres repartidos).
- Nivel 2+: redes sociales (`util/RedesSociales.kt`) y «Comparación con tu rubro» (referencial, datos del teléfono).
- Nivel 3: oferta del día (60 caracteres), sello «Negocio verificado» (lo marca el administrador;
  `SELLO_SOLO_NIVEL3`) y aviso 2 días antes de vencer (`AvisadorVencimiento`, sin WorkManager: se revisa al abrir
  Home y «Mis negocios»). Reglas puras en `util/ReglasPlan.kt`.
- El WhatsApp de todos los negocios abre con «Hola, vi su negocio en LuciDan».
- Room 14 (`data/local/Migraciones.kt`): `facebook`, `instagram`, `tiktok`, `ofertaTexto`, `ofertaHasta`,
  `verificado`. Todos viajan a Firestore. Editar el perfil escribe con *merge* sin plan, estado ni `verificado`.

### 2.1 Decisión de arquitectura: osmdroid en vez de Google Maps Platform

El plan original era usar Google Maps Platform (Maps SDK for Android +
Directions API). Se cambió a **osmdroid + OSRM** porque:

- El Maps SDK for Android exige facturación activa (tarjeta) sí o sí — no
  existe una "demo key" que lo cubra (esa demo key solo cubre APIs
  web/JS, no SDKs nativos de Android).
- Google Cloud Billing rechazó las tarjetas disponibles para habilitar la
  facturación (problema recurrente con tarjetas emitidas en Bolivia).
- osmdroid (librería `org.osmdroid`) usa tiles de OpenStreetMap y no
  requiere ninguna API key ni cuenta de facturación, nunca.
- OSRM (Open Source Routing Machine) tiene un servidor demo público
  (`router.project-osrm.org`) gratuito y sin key, suficiente para el
  cálculo de rutas de HU05 ("Cómo llegar") en un proyecto académico. Si
  más adelante el volumen de uso lo justifica, se puede migrar a un
  servidor OSRM propio o a GraphHopper sin cambiar el resto del código
  (mismo contrato en `RutaService`).

**Impacto en el código:** solo afecta la implementación interna de
`HomeScreen`/`DetalleScreen` (renderizado del mapa) y `RutaService`
(cálculo de ruta). Los nombres de clases, el modelo de datos (`Negocio`,
etc.) y el resto del backlog **no cambian**.

### 2.2 Decisión de producto: guía 100% de negocios (actualizado 2026-11-14)

A pedido de Marcelo, la app dejó de ser de turismo y pasó a ser una **guía
pura de negocios cercanos**, tomando como referencia el sitio **uruku.bo**
("toda tu ciudad en un solo lugar", directorio de Bermejo). Cambios de
modelo respecto a la versión anterior:

- **Se eliminó la clase `Lugar`** (lugares turísticos) y la tabla `lugares`.
  La única entidad explorable en el mapa es `Negocio`.
- `Negocio` ahora lleva lo que antes estaba repartido con `Lugar`:
  `descripcion` (qué vende u ofrece), `telefono` (contacto), `latitud`,
  `longitud`, `direccion`, `horario`, `fotos`, `promedioCalificacion` y el
  método `calcularDistancia(origen)`.
- `Resena` y `Favorito` ahora se relacionan con `Negocio` (antes con
  `Lugar`): se reseñan negocios y se guardan negocios favoritos.
- El enum `RolUsuario` cambió `TURISTA` → **`VISITANTE`** (usuario común
  que explora la guía).
- Un usuario puede tener **0 o más negocios** (0..*): la pantalla de
  HU08/HU09 es "Mis negocios" (lista + agregar), no un perfil único.
- Solo los negocios con `estado = PUBLICADO` aparecen en la exploración
  del mapa (HU01/HU02/HU03).
- Se agregó la **EPIC07 (Futuro, post-Sprint 3)** inspirada en uruku.bo:
  HU15 ofertas del día, HU16 tipo de cambio Bs↔ARS con calculadora, HU17
  información del paso fronterizo. No entra en los sprints actuales.

## 3. Sistema de diseño (branding, para el panel web y cualquier UI)

- Maroon principal: `#632423`
- Gris claro de fondo: `#F2F2F2`
- Crema/acento: `#FFF6F0`
- **Tema de la app (Material 3, 2026-11-14):** esquemas claro/oscuro generados con material-color-utilities desde
  `#632423` (`ui/theme/Color.kt` + `Theme.kt`; fuente en `design/theme/`). Color dinámico desactivado a propósito.
  Todo color sale de `MaterialTheme.colorScheme` (tarjetas y fondos elevados: roles `surfaceContainer*`); la única
  excepción son los colores por rubro (`util/RubroVisual.kt`), armonizados con la marca y fijos para el mapa.
- Tipografía: Arial (documentos); en el prototipo de UI se usó Fraunces +
  Work Sans (paleta coral/teal/gold) — ver `prototype/`.

## 4. Modelo de dominio (Diagrama de Clases)

Fuente completa en `diagrams/00_Diagrama_de_Clases.puml`. Clases principales:

- **Usuario**: id, correo, contrasenaHash, fechaRegistro, rol (enum
  `RolUsuario`: VISITANTE, NEGOCIO, ADMINISTRADOR), notificacionesActivas.
- **Negocio**: id, nombre, categoria (enum `Rubro`: GASTRONOMIA, FARMACIA_SALUD, MODA_ROPA, CAMBIO_MONEDA, TRANSPORTE, ALOJAMIENTO, ABARROTES, TECNOLOGIA, SERVICIOS, OTRO), descripcion (qué vende),
  telefono, latitud, longitud, direccion, horario, fotos,
  promedioCalificacion, estado (enum `EstadoNegocio`: PENDIENTE, PUBLICADO,
  SUSPENDIDO), destacado. Relacionado **1 a muchos** con el Usuario dueño
  (un usuario puede registrar varios negocios). Método
  `calcularDistancia(origen)`.
- **Resena**: id, calificacion (1-5), comentario, fecha. Relacionada a
  Usuario y Negocio.
- **Favorito**: id, fechaGuardado. Relaciona Usuario ↔ Negocio.
- **PlanPublicidad**: id, nombre, duracionDias, precio, beneficios.
- **ContratacionPublicidad**: id, fechaInicio, fechaVencimiento, activo.
  Relaciona Negocio ↔ PlanPublicidad.
- **EstadisticaNegocio**: id, tipo (enum `TipoEvento`: VISTA,
  CLIC_COMO_LLEGAR), fecha.

Esquema de base de datos equivalente (ER) en
`diagrams/00b_Diagrama_Entidad_Relacion.puml`: tablas `usuarios`,
`negocios` (con descripcion, telefono, latitud, longitud, direccion),
`resenas`, `favoritos`, `planes_publicidad`, `contrataciones_publicidad`,
`estadisticas_negocio`. **Ya no existe la tabla `lugares`.**

## 5. Backlog del Producto (14 HU en sprints + 3 HU futuras, 7 épicas)

Fuente: `docs/08_Backlog_Optimizado_LuciDan.xlsx`.

| Épica | Historias | Sprint |
|---|---|---|
| EPIC01 — Exploración de Negocios | HU01, HU02, HU03 | 1 |
| EPIC02 — Detalle y Rutas | HU04, HU05 | 1 |
| EPIC03 — Reseñas y Favoritos | HU06, HU07 | 1 |
| EPIC04 — Registro y Gestión de Negocios (qué vende, contacto) | HU08, HU09 | 2 |
| EPIC05 — Publicidad y Estadísticas | HU10, HU11 | 3 |
| EPIC06 — Registro/Seguridad/Notificaciones | HU12 (Sprint 1), HU13 (Sprint 2), HU14 (Sprint 3) | — |
| EPIC07 — Información útil de la ciudad (ofertas, cambio, frontera) | HU15, HU16, HU17 | Futuro |

## 6. Sprints y tareas técnicas (Backlogs de Sprint)

Fuente completa con horas estimadas por tarea:
`docs/11_Backlog_del_Sprint_1_LuciDan.xlsx`,
`docs/12_Backlog_del_Sprint_2_LuciDan.xlsx`,
`docs/13_Backlog_del_Sprint_3_LuciDan.xlsx`.

### Sprint 1 (01/10/2026–20/01/2027, 39 SP, 135 h, 35 tareas)
HU01 Explorar mapa de negocios · HU02 Filtrar por rubro ·
HU03 Buscar desde punto · HU04 Ficha de detalle del negocio ·
HU05 Trazar ruta · HU06 Calificar/reseñar · HU07 Favoritos ·
HU12 Registro/login.

### Sprint 2 (21/01/2027–17/03/2027, 13 SP, 55 h, 14 tareas)
HU08 Registrar negocio (qué vende, teléfono, ubicación) ·
HU09 Editar perfil de negocio · HU13 Notificaciones de cercanía.

### Sprint 3 (18/03/2027–09/06/2027, 18 SP, 66 h, 17 tareas)
HU10 Contratar plan de publicidad · HU11 Ver estadísticas ·
HU14 Gestión de roles y permisos (admin).

Cada tarea tiene ID (`TA-<HU>-<n>`), descripción, historia asociada y
estimación en horas — usar esos IDs como referencia en commits / nombres de
rama (ej. `feature/TA-01-03-mapa-marcadores`).

## 7. Diagramas de secuencia (11, uno por tarea "diagrama de secuencia" del backlog)

Todos en `diagrams/*.puml` (formato PlantUML, texto plano — léelos
directamente para replicar la lógica exacta en código). **Los nombres de
archivo se mantienen por trazabilidad histórica, pero el contenido ya está
actualizado al modelo de negocios** (donde el nombre del archivo dice
"Lugares", el diagrama por dentro trabaja con negocios):

1. `S1_DS01_Carga_Lugares_Cercanos.puml` — HU01 (carga de negocios cercanos)
2. `S1_DS02_Busqueda_Punto_Mapa.puml` — HU03
3. `S1_DS03_Ficha_Detalle.puml` — HU04 (ficha del negocio)
4. `S1_DS04_Trazado_Ruta.puml` — HU05 (ruta hacia un negocio, OSRM)
5. `S1_DS05_Registro_Resena.puml` — HU06 (reseña sobre un negocio)
6. `S1_DS06_Registro_Login.puml` — HU12
7. `S2_DS07_Registro_Negocio.puml` — HU08 (incluye descripción y teléfono)
8. `S2_DS08_Notificacion_Cercania.puml` — HU13 (notificación local, sin Firebase)
9. `S3_DS09_Contratacion_Plan.puml` — HU10
10. `S3_DS10_Estadisticas_Negocio.puml` — HU11
11. `S3_DS11_Gestion_Roles.puml` — HU14

Diagramas adicionales (post-migración, 15/11/2026), en `diagrams/` y reunidos en
`docs/16_Diagramas_Actividad_y_Secuencia_Adicionales_LuciDan.pdf`:

- `AD01_Explorar_y_Contactar_Negocio.puml` — actividad: permiso de ubicación, radio/rubro/búsqueda, ficha, WhatsApp o llamada, cómo llegar, favorito/reseña.
- `AD02_Registrar_y_Publicar_Negocio.puml` — actividad: formulario (teléfono opcional, GPS, foto), estado PENDIENTE y publicación o suspensión por el administrador.
- `PM_DS12_Carga_Inicial_y_Sincronizacion.puml` — secuencia: semilla (INSERT OR IGNORE), `sincronizar()` con Firestore y política de no borrar los ejemplos.
- `PM_DS13_Foto_y_Registro_de_Negocio.puml` — secuencia: elegir foto de la galería, validar, guardar en Room como PENDIENTE y subir a Firestore.

Cada uno define los nombres exactos de Screen / ViewModel / Repository /
Service que debe usar el código (por ejemplo `HomeScreen`, `HomeViewModel`,
`NegocioRepository`, `LocationService`) — mantener esos nombres para que el
código coincida con la documentación entregada. El repositorio central de
datos de la app es `NegocioRepository` (ya no existe `LugarRepository`).

## 8. Estado actual del proyecto

- [x] Documentación RUP/Scrum completa (Caso de Negocio, Visión, Acta de
      Constitución, Registro de Interesados, Épicas, Equipo Scrum, Plan de
      Capacitación, Backlog Optimizado, Planificación de Lanzamiento,
      Requisitos de Recursos).
- [x] Diagrama de Casos de Uso (17 casos), Diagrama de Clases, Diagrama ER, 11
      Diagramas de Secuencia.
- [x] Backlogs de Sprint 1, 2 y 3 con tareas y estimaciones.
- [x] Prototipo de UI/UX (9 pantallas, ver `prototype/`).
- [x] Decisión de arquitectura: mapas/rutas con osmdroid + OSRM (ver 2.1),
      por bloqueo de facturación de Google Cloud.
- [x] Decisión de producto (2026-11-14): guía 100% de negocios estilo
      uruku.bo, sin lugares turísticos (ver 2.2) — diagramas y backlogs ya
      actualizados.
- [x] Código migrado al modelo solo-negocios (2026-11-14): sin `Lugar`/`LugarRepository`/tabla `lugares`;
      `Rubro` reemplaza a `CategoriaLugar`; `Negocio` lleva `direccion` y `promedioCalificacion`; `Resena` y
      `Favorito` usan `negocioId`; `RolUsuario.VISITANTE`; Room **v11** (migraciones 9→10 y 10→11). La semilla sale de
      `docs/negocios_bermejo.txt` (12 negocios reales de Bermejo; la copia a assets la hace la tarea Gradle `copiarSemilla`).
- [x] Post-migración (2026-11-14), detalle en `docs/Modificaciones_Post_Cambio_de_Vision_LuciDan.txt` y
      `docs/15_Registro_de_Cambios_Post_Migracion_LuciDan.pdf`: fotos de los 12 negocios (`res/drawable-nodpi`, Coil 3)
      con placeholder por rubro; contacto por WhatsApp o llamada (`util/Contacto.kt`); teléfono opcional al registrar;
      login con ojo de contraseña y logo; rediseño Material 3 con auditoría de accesibilidad; selector de tema
      claro/oscuro (menú de Home → Tema); reglas de Firestore actualizadas; 12 pruebas JUnit en verde.
- [x] Logo e ícono nuevos (2026-11-17, `icon/nuevo/`): lanzador adaptativo + monocromo + PNG de respaldo (minSdk 24), logo
      horizontal en login/registro/recuperar (tarjeta clara fija), `ic_notificacion` vectorial y Play Store 512 en
      `icon/nuevo/exportados/`. Marcadores del mapa estilo A con agrupación (osmbonuspack, JitPack).
- [ ] Pendientes conocidos: commit en git sin hacer; re-probar «Cómo llegar», panel de administración, planes y
      estadísticas; probar físicamente la notificación de cercanía (HU13); publicar el negocio PENDIENTE desde el
      panel de administración; reemplazar las fotos por propias/autorizadas antes de cualquier publicación real;
      probar la notificación de cercanía con el ícono nuevo (`ic_notificacion`); las fotos de galería no se comparten entre teléfonos.
- [x] Documentación RUP actualizada a la visión de guía de negocios
      (14/11/2026): Caso de Negocio, Visión, Acta de Constitución, Registro
      de Interesados, Épicas, Requisitos de Recursos, Equipo Scrum, Plan de
      Capacitación, Planificación de Lanzamiento y Diagrama de Caso de Uso
      (fuente editable en `diagrams/10_Diagrama_Caso_de_Uso.puml`). La
      semilla de datos de ejemplo es `docs/negocios_bermejo.txt`
      (reemplaza a `lugares_bermejo.txt`, que puede eliminarse). Actualizados el 15/11/2026: Acta de Constitución, Requisitos de
      Recursos y Plan de Capacitación (v2.1); Backlog Optimizado (hoja «Cambios Post-Migración»); Backlogs de Sprint 1 y 2
      (tareas de ficha y registro); diagramas de clases, ER, secuencia DS03 y DS07, y Casos de Uso con UC16 (contactar
      por WhatsApp) y UC17 (tema). Sin cambios: Caso de Negocio, Visión, Interesados, Épicas, Equipo Scrum y Lanzamiento.
- [ ] Backend / panel web (Node.js + React): sin empezar.

## 9. Convenciones al programar

- Idioma del código: nombres de clases/variables en español cuando ya están
  fijados por los diagramas (`Negocio`, `Resena`, `Favorito`...); nombres de
  paquete/carpeta en inglés estándar de Android (paquete real: `dev.justinianomarcelo.lucidanapp`).
- Seguir estrictamente los nombres de clases y relaciones del diagrama de
  clases (sección 4) al crear las entidades de Room.
- Seguir el flujo exacto de cada diagrama de secuencia (sección 7) al
  implementar cada caso de uso — están numerados y listos para transcribir.
- Mapas y rutas: osmdroid + OSRM (sección 2.1) — no requieren API key.
  Notificaciones y autenticación: locales, sin Firebase (sección 2). Si en
  el futuro se suma algún servicio con API key, va en `local.properties` /
  variables de entorno, nunca commiteada.
- La exploración (HU01/HU02/HU03) muestra **solo negocios con estado
  PUBLICADO**; los pendientes o suspendidos no aparecen en el mapa.
- **Un usuario puede tener varios negocios (0..\*, sección 4):** la pantalla
  de HU08/HU09 debe ser "Mis negocios" (lista con botón de agregar y cada
  negocio editable por separado), no un formulario de perfil único. La
  tabla `negocios` ya soporta esto porque `usuario_id` es una FK simple sin
  restricción de unicidad — no requiere cambios de esquema, solo de UI y de
  las consultas en `NegocioRepository` (traer lista por `usuario_id` en vez
  de un solo registro).
- Ir por historias de usuario completas (no por capas sueltas), en el orden
  del Sprint 1 listado en la sección 6.

## 10. Dónde está cada archivo en esta carpeta

```
LuciDanApp/
├── CLAUDE.md                         (este archivo)
├── docs/                             documentación RUP/Scrum completa (PDF/DOCX/XLSX/PPTX),
│                                     negocios_bermejo.txt (semilla) y registro de cambios post-migración
├── diagrams/                         diagramas UML en PlantUML (.puml, texto plano)
├── design/theme/                     paleta Material 3 generada desde #632423
├── imagenes_negocio/                 fotos originales de los 12 negocios (la app usa copias en res/drawable-nodpi)
├── firestore.rules                   reglas de Cloud Firestore (publicar en la consola de Firebase)
└── prototype/                        exportación del prototipo de UI/UX (PDF + capturas PNG)
```
