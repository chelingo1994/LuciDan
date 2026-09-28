# LuciDan App — Contexto del Proyecto

> Este archivo se carga automáticamente al abrir Claude Code en esta carpeta.
> Resume todo lo definido en la documentación RUP/Scrum del proyecto para que
> el desarrollo de código continúe sin perder las decisiones ya tomadas.

## 1. Qué es el proyecto

**LuciDan App**: aplicación móvil centrada en el **comercio local de Bermejo,
Tarija, Bolivia** (ciudad fronteriza con Argentina, paso de Aguas Blancas),
con un complemento de turismo. Bermejo es un punto de paso comercial, no un
destino turístico como Tarija capital — por eso el eje central de la app es
que turistas, comerciantes y viajeros que cruzan la frontera encuentren
fácilmente qué negocios hay (qué venden, dónde están, cómo contactarlos),
con los lugares turísticos como complemento. Proyecto académico y personal
de **Marcelo Justiniano**, quien asume TODOS los roles: Patrocinador,
Product Owner, Scrum Master y único desarrollador.

Dos componentes:
- **App móvil** (Android, Kotlin): mapa interactivo con lugares turísticos
  Y negocios locales explorables por radio configurable y por
  categoría/rubro, fichas de detalle (incluye qué vende y teléfono de
  contacto si es un negocio), rutas, reseñas, favoritos, notificaciones de
  cercanía.
- **Panel web** (React + Node.js): gestión de perfil de negocio, planes de
  publicidad destacada, estadísticas de interacción.

Base de datos única en la nube: **PostgreSQL + PostGIS** (soporte
geoespacial). En el diseño de la app móvil se usa **Room** como capa local.

Presupuesto total estimado: **Bs 76 375** (ver `docs/03_Acta_de_Constitucion_LuciDan.pdf`
para el desglose completo). Duración estimada: 9 meses, 3 sprints.

## 2. Pila tecnológica

| Capa | Tecnología |
|---|---|
| App móvil | Kotlin, arquitectura por capas (Screen → ViewModel → Repository → DAO) |
| Persistencia local | Room (SQLite) |
| Backend / Panel web | Node.js + React |
| Base de datos central | PostgreSQL + extensión PostGIS |
| Mapas y geolocalización | **osmdroid** (tiles de OpenStreetMap) para el mapa + **OSRM** (servidor demo público `router.project-osrm.org`) para calcular rutas — sin API key, sin facturación, sin tarjeta |
| Notificaciones | Notificaciones locales del sistema (`NotificationService` + `GeofenceManager`) — sin servidor externo |
| Autenticación | PBKDF2 + Room local (`AuthService`) — sin servidor externo |
| Diseño UX/UI | Figma |
| Gestión de tareas | Trello |

**Nota:** no se usa Firebase para Sprint 1/2 (ni Cloud Messaging ni
Authentication) — login y notificaciones de cercanía son 100% locales. Si
más adelante se agrega push remoto real, ahí recién se evaluaría Firebase.

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
(cálculo de ruta). Los nombres de clases, el modelo de datos (`Lugar`,
`Negocio`, etc.) y el resto del backlog **no cambian**.

### 2.2 Decisión de producto: comercio local como eje central

A pedido de Marcelo (validado con un compañero y coherente con las
entrevistas de `docs/05_Epicas_LuciDan.docx`, donde una dueña de
restaurante y la observación de campo en el paso fronterizo de Aguas
Blancas ya señalaban la falta de un directorio de negocios), se reposicionó
la app: **los negocios locales pasan a ser un punto explorable de igual
jerarquía que los lugares turísticos desde el Sprint 1**, no algo que
aparece recién al registrarse en Sprint 2. En concreto:

- HU01 (explorar mapa) y HU02 (filtrar categoría) ahora muestran y filtran
  **lugares turísticos Y negocios publicados** juntos, diferenciados por
  ícono, con el mismo radio configurable.
- HU04 (ficha de detalle) y HU05 (cómo llegar) aplican igual a un lugar que
  a un negocio.
- La clase `Negocio` (sección 4) suma dos campos: `descripcion` (qué vende
  u ofrece) y `telefono` (contacto), para que un visitante pueda saber qué
  hay y cómo comunicarse, no solo que el negocio existe.
- El registro de negocio (HU08, Sprint 2) sigue siendo donde el dueño carga
  y mantiene esos datos — eso no cambia — pero ahora la ficha completa
  (incluida descripción y teléfono) se pensó desde el día uno para que
  cualquier visitante la vea, no solo el dueño.

Ver `docs/08_Backlog_Optimizado_LuciDan.xlsx` (HU01/HU02/HU08 actualizadas)
y `docs/11_Backlog_del_Sprint_1_LuciDan.xlsx` /
`docs/12_Backlog_del_Sprint_2_LuciDan.xlsx` (tareas técnicas ajustadas) para
el detalle exacto.

## 3. Sistema de diseño (branding, para el panel web y cualquier UI)

- Maroon principal: `#632423`
- Gris claro de fondo: `#F2F2F2`
- Crema/acento: `#FFF6F0`
- Tipografía: Arial (documentos); en el prototipo de UI se usó Fraunces +
  Work Sans (paleta coral/teal/gold) — ver `prototype/`.

## 4. Modelo de dominio (Diagrama de Clases)

Fuente completa en `diagrams/00_Diagrama_de_Clases.puml`. Clases principales:

- **Usuario**: id, correo, contrasenaHash, fechaRegistro, rol (enum
  `RolUsuario`: TURISTA, NEGOCIO, ADMINISTRADOR), notificacionesActivas.
- **Lugar**: id, nombre, categoria, latitud, longitud, descripcion, horario,
  rangoPrecio, fotos, promedioCalificacion.
- **Resena**: id, calificacion (1-5), comentario, fecha. Relacionada a
  Usuario y Lugar.
- **Favorito**: id, fechaGuardado. Relaciona Usuario ↔ Lugar.
- **Negocio**: id, nombre, categoria, **descripcion** (qué vende/ofrece),
  **telefono** (contacto), ubicacion, horario, fotos, estado (enum
  `EstadoNegocio`: PENDIENTE, PUBLICADO, SUSPENDIDO), destacado.
  Relacionado 1 a 1 con el Usuario dueño.
- **PlanPublicidad**: id, nombre, duracionDias, precio, beneficios.
- **ContratacionPublicidad**: id, fechaInicio, fechaVencimiento, activo.
  Relaciona Negocio ↔ PlanPublicidad.
- **EstadisticaNegocio**: id, tipo (enum `TipoEvento`: VISTA,
  CLIC_COMO_LLEGAR), fecha.

Esquema de base de datos equivalente (ER) en
`diagrams/00b_Diagrama_Entidad_Relacion.puml`: tablas `usuarios`, `lugares`,
`resenas`, `favoritos`, `negocios` (con `descripcion` y `telefono`),
`planes_publicidad`, `contrataciones_publicidad`, `estadisticas_negocio`.

## 5. Backlog del Producto (14 historias de usuario, 6 épicas)

Fuente: `docs/08_Backlog_Optimizado_LuciDan.xlsx`.

| Épica | Historias | Sprint |
|---|---|---|
| EPIC01 — Exploración de Lugares y Negocios | HU01, HU02, HU03 | 1 |
| EPIC02 — Detalle y Rutas (lugares y negocios) | HU04, HU05 | 1 |
| EPIC03 — Reseñas y Favoritos | HU06, HU07 | 1 |
| EPIC04 — Registro y Gestión de Negocios (perfil, qué vende, contacto) | HU08, HU09 | 2 |
| EPIC05 — Publicidad y Estadísticas | HU10, HU11 | 3 |
| EPIC06 — Registro/Seguridad/Notificaciones | HU12 (Sprint 1), HU13 (Sprint 2), HU14 (Sprint 3) | — |

## 6. Sprints y tareas técnicas (Backlogs de Sprint)

Fuente completa con horas estimadas por tarea:
`docs/11_Backlog_del_Sprint_1_LuciDan.xlsx`,
`docs/12_Backlog_del_Sprint_2_LuciDan.xlsx`,
`docs/13_Backlog_del_Sprint_3_LuciDan.xlsx`.

### Sprint 1 (01/10/2026–20/01/2027, 39 SP, 140 h, 35 tareas)
HU01 Explorar mapa (lugares y negocios) · HU02 Filtrar categoría/rubro ·
HU03 Buscar desde punto · HU04 Ficha de detalle · HU05 Trazar ruta ·
HU06 Calificar/reseñar · HU07 Favoritos · HU12 Registro/login.

### Sprint 2 (21/01/2027–17/03/2027, 13 SP, 57 h, 14 tareas)
HU08 Registrar negocio (incluye descripción y teléfono) ·
HU09 Editar perfil de negocio · HU13 Notificaciones de cercanía.

### Sprint 3 (18/03/2027–09/06/2027, 18 SP, 66 h, 17 tareas)
HU10 Contratar plan de publicidad · HU11 Ver estadísticas ·
HU14 Gestión de roles y permisos (admin).

Cada tarea tiene ID (`TA-<HU>-<n>`), descripción, historia asociada y
estimación en horas — usar esos IDs como referencia en commits / nombres de
rama (ej. `feature/TA-01-03-mapa-marcadores`).

## 7. Diagramas de secuencia (11, uno por tarea "diagrama de secuencia" del backlog)

Todos en `diagrams/*.puml` (formato PlantUML, texto plano — léelos
directamente para replicar la lógica exacta en código):

1. `S1_DS01_Carga_Lugares_Cercanos.puml` — HU01 (carga lugares Y negocios)
2. `S1_DS02_Busqueda_Punto_Mapa.puml` — HU03
3. `S1_DS03_Ficha_Detalle.puml` — HU04
4. `S1_DS04_Trazado_Ruta.puml` — HU05
5. `S1_DS05_Registro_Resena.puml` — HU06
6. `S1_DS06_Registro_Login.puml` — HU12
7. `S2_DS07_Registro_Negocio.puml` — HU08 (incluye descripción y teléfono)
8. `S2_DS08_Notificacion_Cercania.puml` — HU13 (notificación local, sin Firebase)
9. `S3_DS09_Contratacion_Plan.puml` — HU10
10. `S3_DS10_Estadisticas_Negocio.puml` — HU11
11. `S3_DS11_Gestion_Roles.puml` — HU14

Cada uno define los nombres exactos de Screen / ViewModel / Repository /
Service que debe usar el código (por ejemplo `HomeScreen`, `HomeViewModel`,
`LugarRepository`, `NegocioRepository`, `LocationService`) — mantener esos
nombres para que el código coincida con la documentación entregada.
**Nota:** donde el diagrama menciona "Google Maps SDK" o "Directions API"
en el flujo de `RutaService`, usar en su lugar osmdroid / OSRM según la
sección 2.1 — la lógica y los nombres de los pasos no cambian, solo el
proveedor de mapas/rutas.

## 8. Estado actual del proyecto

- [x] Documentación RUP/Scrum completa (Caso de Negocio, Visión, Acta de
      Constitución, Registro de Interesados, Épicas, Equipo Scrum, Plan de
      Capacitación, Backlog Optimizado, Planificación de Lanzamiento,
      Requisitos de Recursos).
- [x] Diagrama de Casos de Uso, Diagrama de Clases, Diagrama ER, 11
      Diagramas de Secuencia.
- [x] Backlogs de Sprint 1, 2 y 3 con tareas y estimaciones.
- [x] Prototipo de UI/UX (9 pantallas, ver `prototype/`).
- [x] Decisión de arquitectura: mapas/rutas con osmdroid + OSRM (ver 2.1),
      por bloqueo de facturación de Google Cloud.
- [x] Decisión de producto: comercio local como eje central, negocios
      explorables desde Sprint 1 (ver 2.2).
- [ ] **Falta actualizar con el nuevo enfoque:** `docs/01_Caso_de_Negocio_LuciDan.pdf`
      y `docs/02_Vision_del_Proyecto_LuciDan.pdf` (solo existen en PDF, sin
      fuente editable) — pendiente de reescritura si Marcelo lo pide.
- [ ] **Código del proyecto Android Studio: sin empezar.** Este es el
      siguiente paso — crear el proyecto, el modelo de datos (Room) según
      la sección 4, y las pantallas del Sprint 1 en orden de dependencia
      (Registro/Login → Home/Mapa con lugares y negocios → Detalle →
      Reseñas → Favoritos).
- [ ] Backend / panel web (Node.js + React): sin empezar.

## 9. Convenciones al programar

- Idioma del código: nombres de clases/variables en español cuando ya están
  fijados por los diagramas (`Lugar`, `Negocio`, `Resena`...); nombres de
  paquete/carpeta en inglés estándar de Android (`com.lucidan.app...`).
- Seguir estrictamente los nombres de clases y relaciones del diagrama de
  clases (sección 4) al crear las entidades de Room.
- Seguir el flujo exacto de cada diagrama de secuencia (sección 7) al
  implementar cada caso de uso — están numerados y listos para transcribir.
- Mapas y rutas: osmdroid + OSRM (sección 2.1) — no requieren API key.
  Notificaciones y autenticación: locales, sin Firebase (sección 2). Si en
  el futuro se suma algún servicio con API key, va en `local.properties` /
  variables de entorno, nunca commiteada.
- La exploración (HU01/HU02), la ficha de detalle (HU04) y "cómo llegar"
  (HU05) deben tratar `Lugar` y `Negocio` como el mismo tipo de punto en el
  mapa, no como features separadas (sección 2.2).
- Ir por historias de usuario completas (no por capas sueltas), en el orden
  del Sprint 1 listado en la sección 6.

## 10. Dónde está cada archivo en esta carpeta

```
LuciDanApp/
├── CLAUDE.md                         (este archivo)
├── docs/                             documentación RUP/Scrum completa (PDF/DOCX/XLSX/PPTX)
├── diagrams/                         diagramas UML en PlantUML (.puml, texto plano)
└── prototype/                        exportación del prototipo de UI/UX (PDF + capturas PNG)
```
