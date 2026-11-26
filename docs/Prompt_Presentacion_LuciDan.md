Actúa como diseñador de presentaciones. Crea una presentación en PowerPoint (.pptx, 16:9), en español, para defender mi proyecto "LuciDan App". Soy Marcelo Justiniano, único desarrollador.

## Contexto de la exposición
- Duración total: 15 a 20 minutos: unos 10-11 de diapositivas y 6-8 de demostración en vivo en mi teléfono.
- Objetivo: explicar qué se construyó, qué hace la app y qué tareas se hicieron en cada sprint, y luego mostrarlo en la app.
- Lenguaje claro, entendible para alguien no técnico, con una parte técnica breve.

## Reglas de contenido
- Usa SOLO la información de este documento. No inventes cifras, funciones, clientes ni resultados. Si falta un dato, deja [completar].
- Poco texto por diapositiva (máximo 5 viñetas cortas). La explicación va en las notas del orador.
- Cada diapositiva lleva notas del orador con lo que debo decir y el tiempo estimado.

## Diseño
- Colores: guindo #632423 (principal), gris claro #F2F2F2 (fondo), crema #FFF6F0 (acento).
- Estilo limpio y moderno, con íconos, tarjetas, líneas de tiempo y diagramas simples.
- Deja marcos "[captura: ...]" donde deba pegar capturas de la app.

## Estructura (unas 14 diapositivas)
1. Portada: LuciDan App, guía de negocios geolocalizada de Bermejo, Tarija, Bolivia.
2. Problema: ciudad fronteriza y comercial; no es fácil saber qué negocios hay cerca, qué venden, su horario ni cómo contactarlos.
3. Solución: responde "¿qué negocios tengo alrededor de mi ubicación?". Referencia de producto: uruku.bo (directorio web de Bermejo), llevado a app Android nativa.
4. Usuarios: Visitante (explora), Dueño de negocio (registra y promociona), Administrador (revisa y publica).
5. Metodología: RUP + Scrum, 3 sprints, 14 historias de usuario, 70 puntos de historia, 66 tareas, 256 horas estimadas.
6. Sprint 1 - Explorar negocios.
7. Sprint 1 - Ficha, ruta, reseñas, favoritos y cuenta.
8. Sprint 2 - Gestión de negocios y avisos.
9. Sprint 3 - Publicidad, estadísticas y administración.
10. Tecnologías y arquitectura.
11. Cambios de visión y mejoras posteriores.
12. Pruebas, limitaciones y trabajo futuro.
13. Demostración en vivo (recorrido).
14. Conclusiones y preguntas.

## Información del proyecto

### Sprint 1 (planificado 01/10/2026 - 20/01/2027; 39 SP, 135 h, 35 tareas)
- HU01 Explorar el mapa: ubicación GPS con permiso, marcadores por rubro con agrupación, radio de 500 m, 1, 2 o 5 km. Solo aparecen negocios publicados.
- HU02 Filtrar por rubro: 10 rubros (gastronomía, farmacia y salud, moda y ropa, cambio de moneda, transporte, alojamiento, abarrotes, tecnología, servicios, otro). Incluye buscador de negocios y opción de buscar en todo Bermejo.
- HU03 Buscar desde un punto elegido en el mapa, dentro de la zona de Bermejo.
- HU04 Ficha del negocio: qué vende, foto, horario, dirección, calificación y contacto por WhatsApp o llamada.
- HU05 Cómo llegar: ruta en el mapa con distancia y tiempo estimado (OSRM).
- HU06 Reseñas: calificación de 1 a 5 estrellas con comentario y promedio del negocio.
- HU07 Favoritos: guardar y consultar negocios preferidos.
- HU12 Registro, inicio de sesión y recuperación de contraseña; contraseña cifrada con PBKDF2.

### Sprint 2 (planificado 21/01/2027 - 17/03/2027; 13 SP, 55 h, 14 tareas)
- HU08 Registrar negocio: nombre, rubro, qué vende, teléfono (opcional), ubicación y foto. Queda PENDIENTE hasta que el administrador lo publique.
- HU09 Editar negocio: pantalla "Mis negocios"; un usuario puede tener varios.
- HU13 Notificación local al acercarse a un negocio destacado (geocerca de unos 300 m), sin servidor externo.

### Sprint 3 (planificado 18/03/2027 - 09/06/2027; 18 SP, 66 h, 17 tareas)
- HU10 Planes de publicidad destacada: Semanal Bs 30 (7 días), Mensual Bs 100 (30 días), Trimestral Bs 250 (90 días). El negocio destacado aparece primero y deja de estarlo al vencer. La contratación se registra en la app; no hay cobro en línea.
- HU11 Estadísticas del negocio: vistas de la ficha y clics en "Cómo llegar".
- HU14 Panel de administración: gestionar usuarios y negocios (activar, publicar, suspender, eliminar), solo para el administrador.

### Tecnologías
- Kotlin, Jetpack Compose y Material 3 (tema claro y oscuro).
- Arquitectura por capas: Pantalla -> ViewModel -> Repositorio -> DAO (dibujar diagrama).
- Room (SQLite, versión 12) como base local.
- Cloud Firestore para compartir negocios y reseñas entre teléfonos.
- osmdroid + OpenStreetMap para el mapa y OSRM para las rutas: sin API key ni tarjeta. Se eligió en lugar de Google Maps porque la facturación de Google Cloud rechazó las tarjetas disponibles.
- Coil para imágenes. Autenticación y notificaciones locales.
- Entidades principales: Usuario, Negocio, Reseña, Favorito, Plan de publicidad, Contratación, Estadística.

### Documentación entregada
Caso de negocio, visión, acta de constitución, interesados, épicas, backlogs de producto y de sprint, casos de uso, diagrama de clases, entidad-relación, secuencia y actividad, y prototipo de UI.

### Cambios de visión y mejoras
- De guía turística a guía 100% de negocios: se eliminó la entidad de lugares turísticos y se migró la base de datos.
- 12 negocios reales de Bermejo como datos iniciales, con fotos.
- Rediseño Material 3 con revisión de accesibilidad, logo e ícono nuevos.

### Pruebas
- 29 pruebas unitarias con JUnit y pruebas manuales en teléfono físico (Infinix HOT 11S).

### Limitaciones (presentarlas con honestidad)
- El inicio de sesión es local; no hay autenticación en la nube.
- Favoritos, estadísticas y contrataciones se guardan solo en el teléfono.
- Las fotos tomadas de la galería no se comparten entre teléfonos.
- La ruta usa un servidor público de demostración.

### Trabajo futuro
- Panel web (React + Node.js) y base central PostgreSQL + PostGIS (planificados, todavía no desarrollados).
- Ofertas del día, tipo de cambio boliviano-peso argentino e información del paso fronterizo.
- Seguimiento de la ruta en tiempo real.
- Autenticación en la nube.

## Diapositiva de demostración (en este orden)
1. Iniciar sesión.
2. Mapa: radio, filtro por rubro, buscador y punto elegido.
3. Ficha: contacto, cómo llegar, reseña y favorito.
4. Mis negocios: registrar un negocio (queda pendiente).
5. Administrador: publicarlo y verlo en el mapa.
6. Contratar un plan y ver que queda destacado.
7. Ver estadísticas y cambiar a tema oscuro.

## Además de las diapositivas
- Entrega una tabla con el tiempo por diapositiva.
- Entrega 8 preguntas probables del tribunal con respuestas cortas, basadas solo en este documento.
