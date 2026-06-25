# Feature Specification: Diagnóstico Técnico del Servicio Legacy

**Feature Branch**: `001-legacy-diagnostic`

**Created**: 2026-06-21

**Status**: Draft

**Input**: Auditoría de código legacy bajo `src/` sin modificar ningún archivo fuente.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Revisión de calidad de código (Priority: P1)

Un desarrollador o arquitecto necesita conocer el estado actual del código en
términos de legibilidad, complejidad y buenas prácticas, para tener una base
objetiva antes de planificar cualquier refactorización.

**Why this priority**: Sin esta revisión no es posible dimensionar el esfuerzo ni
priorizar los features de reconstrucción. Es el punto de entrada de toda la cadena.

**Independent Test**: Dado el directorio `src/`, cuando se ejecuta el análisis de
Clean Code, entonces se produce un inventario de hallazgos con id, categoría
"Clean Code", severidad, ubicación `archivo:línea`, evidencia y descripción — sin
ningún archivo de `src/` modificado.

**Acceptance Scenarios**:

1. **Given** el código fuente bajo `src/main/java`, **When** se analiza la dimensión
   Clean Code, **Then** cada hallazgo incluye id único, severidad, ubicación exacta
   y un snippet de evidencia.
2. **Given** un hallazgo identificado, **When** se consulta su ubicación, **Then**
   apunta a un archivo y número de línea existentes en el repositorio.

---

### User Story 2 — Mapa de arquitectura actual vs. hexagonal (Priority: P2)

Un arquitecto necesita visualizar cómo está estructurado el código hoy versus
cómo debería estar bajo Arquitectura Hexagonal, para identificar las brechas y
planificar la reconstrucción.

**Why this priority**: El mapa arquitectónico es el insumo clave del feature 003
(recomendaciones) y del feature 004 (specs de reconstrucción).

**Independent Test**: Dado el código existente, cuando se analiza la dimensión
Arquitectura Hexagonal, entonces el informe incluye un mapa que contrasta la
estructura de paquetes actual con la separación dominio/aplicación/infraestructura
esperada — sin ningún archivo modificado.

**Acceptance Scenarios**:

1. **Given** la estructura de paquetes en `com.bcp.cnf.services`, **When** se
   analiza la adherencia a Arquitectura Hexagonal, **Then** el informe identifica
   qué clases tienen dependencias mal orientadas y cita el import o anotación
   específica que lo evidencia.
2. **Given** el mapa generado, **When** se revisa la sección de arquitectura del
   informe, **Then** existe una tabla o diagrama que muestra el estado actual vs.
   el estado hexagonal objetivo.

---

### User Story 3 — Inventario completo de deuda técnica (Priority: P3)

Un líder técnico necesita un inventario exhaustivo de errores de lógica, malas
prácticas, código muerto, duplicado e inerte, para priorizar qué corregir primero
en los features de reconstrucción.

**Why this priority**: Complementa las dos historias anteriores con hallazgos de
las cinco dimensiones restantes, completando el diagnóstico integral.

**Independent Test**: Dado el código fuente completo, cuando se analizan las
dimensiones 3 a 7, entonces el informe incluye hallazgos para cada dimensión con
la misma estructura (id, categoría, severidad, ubicación, evidencia, impacto) —
sin ningún archivo modificado.

**Acceptance Scenarios**:

1. **Given** el código fuente, **When** se analiza la dimensión Código Muerto,
   **Then** se listan métodos, clases o imports no referenciados con su ubicación
   exacta.
2. **Given** el informe final, **When** se revisa la matriz de hallazgos,
   **Then** existe un conteo por severidad (crítica/alta/media/baja) y por
   categoría (las 7 dimensiones).

---

### Edge Cases

- ¿Qué pasa si un archivo fuente no puede ser leído (permisos, encoding)?
  → Se registra como hallazgo de cobertura con severidad alta y se continúa.
- ¿Qué pasa si una clase pertenece a más de una categoría de hallazgo?
  → Se registra un hallazgo independiente por cada categoría aplicable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El análisis DEBE cubrir el 100% de los archivos bajo `src/main/java`
  y los archivos de configuración (`application.yml`, `pom.xml`, migraciones).
- **FR-002**: Cada hallazgo DEBE tener: id único, categoría (una de las 7
  dimensiones), severidad (crítica/alta/media/baja), ubicación exacta
  (`archivo:línea`), descripción del problema, evidencia/snippet e impacto.
- **FR-003**: El informe DEBE incluir un resumen ejecutivo con el nivel de salud
  general del servicio.
- **FR-004**: El informe DEBE incluir una matriz de hallazgos agrupados por
  severidad y por categoría.
- **FR-005**: El informe DEBE incluir un mapa de la arquitectura actual vs. la
  arquitectura hexagonal objetivo.
- **FR-006**: El informe DEBE incluir una lista priorizada de los hallazgos más
  críticos.
- **FR-007**: El análisis NO DEBE modificar ningún archivo bajo `src/`.
- **FR-008**: El análisis DEBE evaluar las 7 dimensiones: Clean Code, Arquitectura
  Hexagonal, Errores de lógica, Malas prácticas, Código muerto, Código duplicado
  y Código inerte.

### Key Entities

- **Hallazgo**: unidad atómica del diagnóstico. Atributos: id, categoría,
  severidad, ubicación (`archivo:línea`), descripción, evidencia, impacto.
- **Informe de diagnóstico**: agregado de todos los hallazgos más resumen
  ejecutivo, matriz y mapa arquitectónico.
- **Dimensión**: una de las 7 categorías de análisis definidas en el alcance.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los archivos fuente bajo `src/main/java` y archivos de
  configuración quedan registrados como revisados en el informe.
- **SC-002**: Cada hallazgo del inventario incluye los 7 campos obligatorios
  (id, categoría, severidad, ubicación, descripción, evidencia, impacto) — sin
  excepción.
- **SC-003**: El informe es accionable: cada hallazgo crítico o alto puede ser
  trasladado directamente como insumo del feature 003 (recomendaciones) sin
  necesidad de recolectar información adicional del código.
- **SC-004**: Ningún archivo bajo `src/` presenta diferencias antes y después
  de ejecutar el análisis (verificable con diff).
- **SC-005**: El mapa arquitectónico identifica al menos una brecha concreta entre
  el estado actual y la Arquitectura Hexagonal objetivo, con evidencia citada.

## Assumptions

- El código fuente bajo `src/` es el estado actual y definitivo del servicio legacy
  que será diagnosticado.
- No existen features de reconstrucción activos en paralelo que modifiquen `src/`
  durante el análisis.
- El informe se entrega como documentación Markdown en el directorio del feature
  (`specs/001-legacy-diagnostic/`).
- La cobertura se define sobre `src/main/java`; los archivos de test bajo
  `src/test/` son revisados como contexto pero no son el objeto principal del
  diagnóstico.
- No se requiere acceso a sistemas externos (base de datos, Redis, Azure) para
  realizar el análisis estático del código.
