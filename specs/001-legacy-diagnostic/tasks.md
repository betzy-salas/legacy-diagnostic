---
description: "Lista de tareas de auditoría para el diagnóstico técnico del servicio legacy"
---

# Tasks: Diagnóstico Técnico del Servicio Legacy

**Input**: Artefactos de diseño de `specs/001-legacy-diagnostic/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Naturaleza**: Tareas de AUDITORÍA y PRODUCCIÓN DEL INFORME. Ninguna tarea modifica `src/`.

**Entregable final**: `specs/001-legacy-diagnostic/informe-diagnostico.md`

## Format: `[ID] [P?] [Story?] Descripción`

- **[P]**: Puede ejecutarse en paralelo (sin dependencias entre sí)
- **[Story]**: A qué user story del spec corresponde (US1, US2, US3)
- Cada tarea indica el artefacto que produce o actualiza

## Path Conventions

- **Artefactos del diagnóstico**: `specs/001-legacy-diagnostic/`
- **Código auditado (solo lectura)**: `src/main/java/com/bcp/cnf/services/`
- **Informe de salida**: `specs/001-legacy-diagnostic/informe-diagnostico.md`

---

## Phase 1: Setup — Preparación e Inventario

**Purpose**: Construir la base del análisis: inventario de archivos y confirmación del stack.
No produce hallazgos aún; alimenta `research.md` y habilita todas las fases siguientes.

- [x] T001 Listar todos los archivos bajo `src/main/java/` y `src/main/resources/`, contar el total y registrar el inventario completo en `specs/001-legacy-diagnostic/research.md` (habilita SC-001)
- [x] T002 Verificar y confirmar el stack detectado (Quarkus, Java, Maven, SingleStore, Redis, Azure Table Storage, BCP ATLA FWK) inspeccionando imports y `application.yml`; documentar versiones y dependencias en `specs/001-legacy-diagnostic/research.md`
- [x] T003 [P] Identificar y documentar los puntos de entrada del servicio: endpoints JAX-RS en `expose/web/`, schedulers en `scheduler/`, y la ruta raíz `/business-position-keeping/v1`; registrar en `specs/001-legacy-diagnostic/research.md`
- [x] T004 [P] Construir el mapa de dependencias entre componentes: qué paquete importa a cuál (ej. `service/` → `proxy/`); registrar en `specs/001-legacy-diagnostic/research.md`

**Checkpoint**: Inventario completo y stack confirmado → las fases 2 y 3 pueden comenzar.

---

## Phase 2: Foundational — Análisis Estático y Métricas

**Purpose**: Capturar métricas cuantitativas que serán evidencia objetiva de los hallazgos.
Esta fase DEBE completarse antes de cualquier evaluación por dimensión.

**⚠️ CRÍTICO**: Las fases de evaluación (3, 4, 5) no pueden comenzar hasta completar esta fase.

- [x] T005 Calcular la complejidad ciclomática de cada método en `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java` (contar ramas: if/else/for/while/catch/&&/||); registrar tabla de métricas en `specs/001-legacy-diagnostic/research.md`
- [x] T006 [P] Calcular métricas de tamaño (líneas de código por método y por clase) para todos los archivos Java bajo `src/main/java/`; identificar métodos > 20 líneas y clases > 200 líneas; registrar en `specs/001-legacy-diagnostic/research.md`
- [x] T007 [P] Inventariar la cobertura de pruebas existente: cruzar clases en `src/main/java/` contra clases de test en `src/test/unit/java/`; calcular porcentaje de cobertura de clases; registrar en `specs/001-legacy-diagnostic/research.md`

**Checkpoint**: Métricas disponibles → evaluación por dimensión puede comenzar en paralelo.

---

## Phase 3: User Story 1 — Revisión de Calidad de Código (Priority: P1) 🎯 MVP

**Goal**: Producir el inventario completo de hallazgos de la dimensión Clean Code (D1),
que es el insumo más directo para el equipo de desarrollo en el feature 003.

**Independent Test**: Dado el análisis completado, cuando se revisa la sección D1 del informe,
entonces cada hallazgo tiene id D1-XXX, severidad, `archivo:línea` y snippet de evidencia —
y `git diff src/` no muestra cambios.

### Implementación de User Story 1

- [x] T008 [US1] Evaluar dimensión Clean Code (D1) en `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java`: nombres, complejidad, tamaño de métodos, manejo de errores, números mágicos; registrar hallazgos D1-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T009 [P] [US1] Evaluar dimensión Clean Code (D1) en `src/main/java/com/bcp/cnf/services/proxy/singlestore/ItfTxnRepository.java`: responsabilidades, uso de `Object` como parámetro, casting, lógica mezclada; registrar hallazgos D1-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T010 [P] [US1] Evaluar dimensión Clean Code (D1) en archivos restantes: `proxy/redis/`, `proxy/tablestorage/`, `expose/web/`, `scheduler/`, `model/entities/`, `dto/`, `config/`; registrar hallazgos D1-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T031 [US1] Analizar sistemáticamente todos los identificadores del código fuente (clases, interfaces, enums, métodos, variables, parámetros, constantes, paquetes) contra las reglas de `.specify/memory/naming-conventions.md`; registrar cada violación como hallazgo D1-XXX con: tipo de identificador, nombre actual, regla violada y nombre sugerido en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T011 [US1] Consolidar todos los hallazgos D1 en la sección "Hallazgos D1 — Clean Code" del informe, con tabla de resumen y fichas completas (7 campos c/u) en `specs/001-legacy-diagnostic/informe-diagnostico.md` (depende de T008, T009, T010, T031)

**Checkpoint**: User Story 1 completa y verificable de forma independiente.

---

## Phase 4: User Story 2 — Mapa de Arquitectura Actual vs. Hexagonal (Priority: P2)

**Goal**: Producir el mapa arquitectónico completo (estado actual vs. Ports & Adapters objetivo)
con todas las violaciones de la regla de dependencias identificadas y citadas.

**Independent Test**: Dado el análisis completado, cuando se revisa la Sección 5 del informe,
entonces existe una tabla de mapeo paquete → capa hexagonal, una lista de violaciones de
dependencias con `archivo:línea`, y un inventario de puertos (interfaces) por adaptador.

### Implementación de User Story 2

- [x] T012 [US2] Evaluar dependencias entre capas: verificar en `EvaluateItfServiceImpl.java` que sus imports no incluyan clases de `proxy.*`, `expose.*` ni frameworks; registrar violaciones como hallazgos D2-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T013 [P] [US2] Verificar existencia de puertos (interfaces) para cada adaptador: `proxy/redis/`, `proxy/singlestore/`, `proxy/tablestorage/`, `scheduler/`; registrar ausencias como hallazgos D2-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T014 [P] [US2] Evaluar si las entidades JPA en `model/entities/` se usan directamente como objetos de dominio en `service/`; registrar hallazgos D2-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T015 [US2] Redactar el mapa arquitectónico completo (Sección 5 del informe) siguiendo el esquema de `specs/001-legacy-diagnostic/contracts/diagnostic-report-schema.md`, incluyendo tabla de paquetes, violaciones de dependencias y estado de puertos; en `specs/001-legacy-diagnostic/informe-diagnostico.md` (depende de T012, T013, T014)

**Checkpoint**: User Stories 1 y 2 completas y verificables independientemente.

---

## Phase 5: User Story 3 — Inventario Completo de Deuda Técnica (Priority: P3)

**Goal**: Completar el diagnóstico con las 5 dimensiones restantes (D3–D7), produciendo
el inventario exhaustivo de deuda técnica del servicio.

**Independent Test**: Dado el análisis completado, cuando se revisa el informe, entonces
existen secciones de hallazgos para D3, D4, D5, D6 y D7, cada una con hallazgos que
citan `archivo:línea` y evidencia concreta.

### Implementación de User Story 3 (dimensiones paralelizables)

- [x] T016 [P] [US3] Evaluar dimensión Errores de lógica (D3) en todos los archivos bajo `src/main/java/`: nulos sin protección, `MAX(id)+1` en `ItfTxnRepository.java`, variables mutables en bucles, transacciones amplias; registrar hallazgos D3-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T017 [P] [US3] Evaluar dimensión Malas prácticas (D4) en todos los archivos bajo `src/main/java/`: `keys.keys("pattern")` en Redis, casting inseguro, violaciones SOLID, logging con concatenación; registrar hallazgos D4-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T018 [P] [US3] Evaluar dimensión Código muerto (D5) en todos los archivos bajo `src/main/java/`: métodos sin llamantes, imports sin uso, variables locales no leídas, ramas inalcanzables; registrar hallazgos D5-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T019 [P] [US3] Evaluar dimensión Código duplicado (D6) en todos los archivos bajo `src/main/java/`: bloques `buildXxxException` repetidos, lógica de truncado duplicada, patrón de logging manual replicado; registrar hallazgos D6-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T020 [P] [US3] Evaluar dimensión Código inerte (D7) en todos los archivos bajo `src/main/java/`: wrappers sin valor, retornos de objetos vacíos (`new EvaluateItfResponseInner()` sin campos), catch que rethrow sin transformación; registrar hallazgos D7-XXX en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T021 [US3] Consolidar hallazgos D3–D7 en sus secciones del informe con tablas de resumen y fichas completas en `specs/001-legacy-diagnostic/informe-diagnostico.md` (depende de T016–T020)

**Checkpoint**: Las 3 User Stories están completas con hallazgos en las 7 dimensiones.

---

## Phase 6: Consolidación e Informe Final

**Purpose**: Sintetizar todos los hallazgos en los artefactos de resumen del informe.

- [x] T022 Construir la matriz de hallazgos por severidad × categoría (Sección 4 del informe) siguiendo el esquema de `specs/001-legacy-diagnostic/contracts/diagnostic-report-schema.md`; en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T023 Redactar la lista priorizada de hallazgos Críticos y Altos (Sección 6 del informe) con referencia a cada ID de hallazgo; en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T024 Calcular el nivel de salud general (`health_level`) según la escala de `specs/001-legacy-diagnostic/data-model.md` y registrar en el encabezado del informe
- [x] T025 Redactar el resumen ejecutivo (Sección 2 del informe): estado general, dimensiones con mayor concentración, riesgo técnico principal, urgencia relativa; en `specs/001-legacy-diagnostic/informe-diagnostico.md`
- [x] T026 Completar el encabezado del informe (Sección 1) con: fecha, archivos auditados, porcentaje de cobertura, nivel de salud y conteo total de hallazgos; en `specs/001-legacy-diagnostic/informe-diagnostico.md`

---

## Phase 7: Verificación del Entregable

**Purpose**: Confirmar que el informe cumple los criterios de éxito del spec antes
de marcarlo como completado.

- [x] T027 Verificar que `git diff src/` no muestra ningún cambio — confirmar solo lectura (SC-004); registrar resultado en `specs/001-legacy-diagnostic/informe-diagnostico.md` sección de verificación
- [x] T028 [P] Verificar cobertura 100%: cruzar el inventario de archivos de T001 contra las secciones del informe; todo archivo debe tener al menos un hallazgo registrado o una entrada explícita de "sin hallazgos" (SC-001)
- [x] T029 [P] Verificar trazabilidad: revisar aleatoriamente 5 hallazgos de distintas dimensiones y confirmar que la ubicación `archivo:línea` existe en el repositorio y el snippet de evidencia es exacto (SC-002)
- [x] T030 Validar accionabilidad: revisar que cada hallazgo Crítico y Alto puede trasladarse directamente al feature 003 (recomendaciones) sin necesitar información adicional del código (SC-003)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato
- **Foundational (Phase 2)**: Depende de completar Phase 1 — BLOQUEA todas las evaluaciones
- **US1 (Phase 3)**: Depende de Phase 2 — no depende de US2 ni US3
- **US2 (Phase 4)**: Depende de Phase 2 — no depende de US1 ni US3
- **US3 (Phase 5)**: Depende de Phase 2 — no depende de US1 ni US2
- **Consolidación (Phase 6)**: Depende de que US1, US2 y US3 estén completos
- **Verificación (Phase 7)**: Depende de la Consolidación

### User Story Dependencies

- **US1 (P1)**: Puede comenzar después de Phase 2. Sin dependencias de US2 ni US3.
- **US2 (P2)**: Puede comenzar después de Phase 2. Sin dependencias de US1 ni US3.
- **US3 (P3)**: Puede comenzar después de Phase 2. Sin dependencias de US1 ni US2.

### Within Each User Story

- Tareas [P] dentro de la misma historia pueden ejecutarse en paralelo
- Las tareas de consolidación por historia (T011, T015, T021) dependen de todas las evaluaciones [P] de esa historia
- Las fases de consolidación (Phase 6) y verificación (Phase 7) son secuenciales

---

## Parallel Opportunities

```bash
# Phase 1: Setup — en paralelo
T003 [P] "Identificar puntos de entrada..."
T004 [P] "Construir mapa de dependencias..."

# Phase 2: Foundational — en paralelo (una vez T001+T002 completos)
T006 [P] "Calcular métricas de tamaño..."
T007 [P] "Inventariar cobertura de pruebas..."

# Phase 3+4+5: Las 3 User Stories pueden ejecutarse en paralelo
# Una vez Phase 2 completa:

# US1 en paralelo:
T009 [P] [US1] "Evaluar Clean Code en ItfTxnRepository..."
T010 [P] [US1] "Evaluar Clean Code en archivos restantes..."

# US2 en paralelo:
T013 [P] [US2] "Verificar existencia de puertos..."
T014 [P] [US2] "Evaluar entidades JPA como dominio..."

# US3 completamente en paralelo:
T016 [P] [US3] "Evaluar Errores de lógica (D3)..."
T017 [P] [US3] "Evaluar Malas prácticas (D4)..."
T018 [P] [US3] "Evaluar Código muerto (D5)..."
T019 [P] [US3] "Evaluar Código duplicado (D6)..."
T020 [P] [US3] "Evaluar Código inerte (D7)..."

# Phase 7: Verificación — en paralelo
T028 [P] "Verificar cobertura 100%..."
T029 [P] "Verificar trazabilidad..."
```

---

## Implementation Strategy

### MVP First (User Story 1 — Clean Code)

1. Completar Phase 1: Setup (T001–T004)
2. Completar Phase 2: Foundational — métricas (T005–T007)
3. Completar Phase 3: US1 — Clean Code (T008–T011)
4. **STOP y VALIDAR**: El informe tiene hallazgos D1 verificables con `archivo:línea` y evidencia.
5. El resultado ya es accionable para el feature 003 en la dimensión Clean Code.

### Entrega Incremental

1. Setup + Foundational → Inventario listo
2. US1 (Clean Code) → Primera dimensión auditada y accionable
3. US2 (Arquitectura Hexagonal) → Mapa arquitectónico disponible
4. US3 (D3–D7) → Diagnóstico integral completo
5. Consolidación + Verificación → Informe final entregado

### Estrategia de Equipo en Paralelo

Con varios analistas:

1. Todos completan Setup + Foundational juntos (T001–T007)
2. Una vez Phase 2 completa:
   - Analista A: US1 — Clean Code (T008–T011)
   - Analista B: US2 — Arquitectura Hexagonal (T012–T015)
   - Analista C: US3 — Dimensiones D3–D7 (T016–T021)
3. Consolidación e informe final (T022–T026) — en secuencia, todos contribuyen
4. Verificación (T027–T030)

---

## Notes

- [P] = sin dependencias en esa fase, puede ejecutarse en paralelo
- [USN] = la tarea alimenta la user story N del spec
- Ninguna tarea escribe en `src/` — verificar con `git diff src/` al final (T027)
- El entregable de cada tarea es un fragmento del `informe-diagnostico.md` o una actualización de `research.md`
- Cada hallazgo debe seguir exactamente la taxonomía de `specs/001-legacy-diagnostic/data-model.md`
- El informe completo sigue el esquema de `specs/001-legacy-diagnostic/contracts/diagnostic-report-schema.md`
