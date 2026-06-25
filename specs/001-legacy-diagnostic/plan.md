# Implementation Plan: Diagnóstico Técnico del Servicio Legacy

**Branch**: `001-legacy-diagnostic` | **Date**: 2026-06-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification desde `specs/001-legacy-diagnostic/spec.md`

**Note**: Este feature NO produce código de aplicación. Su entregable es un informe
de diagnóstico. Este plan describe la METODOLOGÍA DE AUDITORÍA, no una arquitectura
de software a construir.

## Summary

Auditoría estática completa del servicio legacy `business-position-keeping` de BCP CNF,
que evalúa el código fuente bajo `src/` en 7 dimensiones de calidad. El resultado es
un informe estructurado de hallazgos con trazabilidad exacta (archivo:línea + evidencia)
que servirá como insumo directo para los features de recomendaciones (003) y specs de
reconstrucción (004). El análisis es de solo lectura: no se modifica ningún archivo de `src/`.

## Technical Context

**Language/Version**: Java (Quarkus runtime, Jakarta EE / MicroProfile)

**Primary Dependencies** (detectadas inspeccionando el repositorio):
- Quarkus (framework principal — confirmado en `application.yml` y anotaciones `@ApplicationScoped`)
- Jakarta CDI (`jakarta.enterprise.context`, `jakarta.inject`)
- JAX-RS (`jakarta.ws.rs`) para REST
- Quarkus Hibernate ORM with Panache (`io.quarkus.hibernate.orm.panache.PanacheRepositoryBase`)
- Quarkus Redis datasource (`io.quarkus.redis.datasource`)
- MicroProfile REST Client (`org.eclipse.microprofile.rest.client`)
- SmallRye OpenAPI (`quarkus.smallrye-openapi`)
- Kubernetes Config (`quarkus.kubernetes-config`)
- `com.bcp.atla.fwk.core.error` (framework interno BCP — `ApiException`, `ExceptionCategoryTypes`)

**Storage**:
- SingleStore (via Hibernate ORM/Panache, dialect `MySQL8Dialect`)
- Redis (caché de tasas ITF y códigos exonerados)
- Azure Table Storage (datos de referencia — `AzureTableConfig.java`)

**Testing**: JUnit (archivos bajo `src/test/unit/java/`)

**Target Platform**: Kubernetes (`quarkus.kubernetes-config.enabled: true`)

**Project Type**: Microservicio REST (1 endpoint: `POST /business-position-keeping/v1/...`)

**Performance Goals**: N/A — feature de análisis, no de construcción

**Constraints**:
- Solo lectura sobre `src/`; sin ejecución de código del servicio auditado
- Sin acceso a entornos externos (DB, Redis, Azure) — análisis estático únicamente
- Documentación en español; identificadores y rutas en inglés

**Scale/Scope**: ~20 archivos Java en `src/main/java/`, 1 archivo de configuración
(`application.yml`), 1 especificación OpenAPI

## Constitution Check

*GATE: Debe pasar antes de ejecutar la auditoría.*

| Principio | Estado | Evidencia |
|-----------|--------|-----------|
| I. Diagnóstico Antes de Reconstrucción | ✅ PASA | Este feature ES el feature 002 (diagnóstico). No hay predecesor requerido. |
| II. Arquitectura Hexagonal como Objetivo | ✅ PASA | La arquitectura hexagonal se usa como **vara de medir** del estado actual, no como algo a implementar en este feature. |
| III. Trazabilidad Total | ✅ PASA | Cada hallazgo del informe incluirá `archivo:línea` + snippet de evidencia obligatorios. Definido en FR-002. |
| IV. Solo Lectura en Fases de Análisis | ✅ PASA | El plan explícitamente prohíbe modificar `src/`. SC-004 lo verifica con diff. |
| V. Idioma Mixto Controlado | ✅ PASA | Documentación en español; identificadores, rutas y claves de configuración en inglés. |
| VI. Especificación Antes de Implementación | ✅ PASA | `spec.md` validado (checklist completo). Este plan es el `plan.md` requerido. |

**Nota Test-First**: Este feature no viola el principio de especificación antes de
implementación porque no produce código de aplicación. Su verificación es la
cobertura del 100% de archivos y la trazabilidad de hallazgos (SC-001, SC-002).

## Project Structure

### Documentación (este feature)

```text
specs/001-legacy-diagnostic/
├── plan.md              # Este archivo
├── research.md          # Stack detectado + decisiones metodológicas
├── data-model.md        # Taxonomía de hallazgos y severidades
├── quickstart.md        # Guía de reproducción del diagnóstico paso a paso
├── contracts/
│   └── diagnostic-report-schema.md   # Esquema del informe de diagnóstico
└── tasks.md             # Generado por /speckit-tasks (no creado aquí)
```

### Código fuente auditado (solo lectura)

```text
src/main/java/com/bcp/cnf/services/
├── config/
│   └── AzureTableConfig.java
├── dto/
│   ├── CodTxnExoneratedItfRedisDto.java
│   ├── EvaluateItfHeaders.java
│   └── ItfsRateRedisDto.java
├── expose/web/
│   └── PositionKeepingApiImpl.java        ← adaptador REST
├── model/
│   ├── api/                               ← modelos generados por OpenAPI
│   └── entities/
│       ├── ExoneratedAccount.java
│       ├── ItfTxn.java
│       ├── ItfTxnId.java
│       └── PdhBalance.java
├── proxy/
│   ├── redis/
│   │   ├── CodTxnExoneratedItfRedisProxy.java
│   │   └── ItfRateRedisProxy.java
│   ├── singlestore/
│   │   ├── ExoneratedAccountRepository.java
│   │   ├── ItfTxnRepository.java
│   │   └── PdhBalanceRepository.java
│   └── tablestorage/
│       ├── ExoneratedCodTxnItfTableStorageProxy.java
│       └── ItfRateTableStorageProxy.java
├── scheduler/
│   ├── CacheWarmUpSupport.java
│   ├── CodTxnExoneratedItfCacheLoader.java
│   └── ItfRateCacheLoader.java
└── service/
    ├── EvaluateItfService.java            ← interfaz (puerto candidato)
    └── impl/
        └── EvaluateItfServiceImpl.java    ← implementación del servicio
```

## Metodología de Auditoría

### Fase 1 — Inventario y mapeo

- Listar todos los archivos bajo `src/main/java/` y `src/main/resources/`.
- Identificar paquetes, capas y puntos de entrada (endpoints JAX-RS, schedulers).
- Construir mapa de dependencias entre componentes (qué importa qué).
- Registrar conteo de archivos auditados para cumplir SC-001.

### Fase 2 — Análisis estático y métricas (revisión manual)

No se dispone de pipeline de build en este entorno; el análisis es manual con
criterios objetivos definidos:

- **Complejidad ciclomática**: contar ramas (if/else/for/while/try-catch/switch/&&/||)
  por método. Umbral: > 10 = alta complejidad.
- **Tamaño de métodos**: líneas de código por método. Umbral: > 20 líneas = candidato.
- **Tamaño de clases**: líneas totales. Umbral: > 200 líneas = posible violación SRP.
- **Duplicación**: comparación visual de bloques entre archivos de la misma capa.
- **Cobertura existente**: inventario de clases de test vs. clases de producción.

### Fase 3 — Evaluación por dimensión (7 dimensiones)

Ver `research.md` para criterios detallados de detección por dimensión.

**Sub-análisis de nombramientos (parte de D1 — Clean Code)**:
Recorrer sistemáticamente todos los identificadores del código fuente (clases,
interfaces, enums, métodos, variables, parámetros, constantes, paquetes) y
evaluar cada uno contra las reglas definidas en
`.specify/memory/naming-conventions.md`. Registrar cada violación como hallazgo
D1-XXX con: tipo de identificador, nombre actual, regla violada y nombre sugerido.

### Fase 4 — Evaluación arquitectónica hexagonal

- Mapear paquetes actuales vs. capas hexagonales esperadas (dominio/aplicación/infraestructura).
- Verificar la regla de dependencias: el dominio no debe importar nada de `proxy.*`,
  `expose.*`, ni frameworks externos.
- Ver `contracts/diagnostic-report-schema.md` para el formato del mapa arquitectónico.

### Fase 5 — Consolidación

- Agrupar hallazgos por categoría y severidad → matriz.
- Priorizar hallazgos críticos y altos → lista priorizada.
- Redactar resumen ejecutivo con nivel de salud general.

## Complexity Tracking

> No aplica: este feature no construye código. No hay violaciones de constitución
> que justificar. La complejidad del plan proviene del alcance del análisis (7
> dimensiones), no de decisiones de arquitectura de software.
