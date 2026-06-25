<!--
## Sync Impact Report
- **Version change**: (sin versión previa) → 1.0.0
- **Tipo de bump**: MAJOR (ratificación inicial)
- **Principios modificados**: N/A (ratificación inicial)
- **Secciones añadidas**: Core Principles (I–VI), Flujo de Reconstrucción, Governance
- **Secciones eliminadas**: N/A
- **Plantillas revisadas**:
  - `.specify/templates/plan-template.md` ✅ (Constitution Check alineado; sin cambios necesarios)
  - `.specify/templates/spec-template.md` ✅ (estructura compatible; sin cambios necesarios)
  - `.specify/templates/tasks-template.md` ✅ (categorías compatibles; sin cambios necesarios)
- **TODOs diferidos**: ninguno
-->

# BCP CNF ITF Position Keeping — Constitución de Reconstrucción

## Core Principles

### I. Diagnóstico Antes de Reconstrucción

Ningún feature de reconstrucción puede comenzar sin que su predecesor en la
cadena de análisis esté completo y validado. La secuencia es obligatoria:

- **002 — Diagnóstico** (solo lectura; entregable: informe de hallazgos)
- **003 — Recomendaciones** (basadas en los hallazgos de 002)
- **004 — Specs de reconstrucción** (diseño basado en recomendaciones de 003)
- **005+ — Implementación** (basada exclusivamente en los specs de 004)

Un feature de implementación que no tenga spec.md y plan.md validados de la
cadena anterior DEBE ser rechazado.

### II. Arquitectura Hexagonal como Objetivo (NO NEGOCIABLE)

El servicio reconstruido DEBE implementar Arquitectura Hexagonal (Ports & Adapters):

- **Dominio**: cero dependencias en frameworks, ORM o infraestructura. Solo lógica de negocio pura.
- **Aplicación**: orquesta casos de uso invocando únicamente puertos (interfaces).
- **Infraestructura**: los adaptadores implementan los puertos (caché, base de datos,
  almacenamiento externo, controladores REST, schedulers).
- **Regla de dependencia**: las capas exteriores dependen de las interiores;
  nunca a la inversa.

Cualquier dependencia del dominio hacia un framework o detalle de infraestructura
DEBE ser rechazada sin excepción.

### III. Trazabilidad Total de Hallazgos

Todo hallazgo diagnóstico, recomendación y requisito DEBE incluir:

- **Ubicación exacta**: `archivo:línea` para artefactos de código.
- **Evidencia**: snippet de código o fragmento de configuración que respalde la afirmación.

Un hallazgo sin ubicación exacta y evidencia no es un hallazgo válido. No se aceptan
afirmaciones genéricas sin cita concreta del repositorio.

### IV. Solo Lectura en Fases de Análisis

Los features clasificados como "diagnóstico" o "análisis" DEBEN producir
exclusivamente documentación. Está prohibido:

- Modificar cualquier archivo bajo `src/`.
- Crear, eliminar o renombrar clases, métodos o recursos de código.
- Alterar archivos de configuración o migraciones.

Un PR de un feature de análisis que toque `src/` es automáticamente inválido.

### V. Idioma Mixto Controlado

- **Documentación** (specs, planes, informes): DEBE redactarse en español.
- **Identificadores de código** (clases, métodos, variables, paquetes), rutas de
  archivos y claves de configuración: DEBEN permanecer en inglés.
- La mezcla en una misma oración está permitida cuando el término técnico no tiene
  equivalente preciso en español.

### VI. Especificación Antes de Implementación

Ningún feature de implementación puede iniciar codificación hasta que:

1. `spec.md` esté validado: criterios de aceptación revisados y aprobados.
2. `plan.md` esté completo: decisión de arquitectura documentada y verificada
   contra esta constitución.
3. Todos los features bloqueantes predecesores estén completados.

## Flujo de Reconstrucción

Los features siguen una cadena de dependencia estricta y numeración secuencial.
El progreso DEBE ser lineal:

| Feature | Nombre                        | Tipo        | Predecesor |
|---------|-------------------------------|-------------|------------|
| 002     | Diagnóstico técnico legacy    | Análisis    | —          |
| 003     | Recomendaciones               | Diseño      | 002        |
| 004     | Specs de reconstrucción       | Especif.    | 003        |
| 005+    | Implementación hexagonal      | Desarrollo  | 004        |

## Governance

Esta constitución es el documento normativo de mayor jerarquía del proyecto.
Supersede cualquier convención, práctica o preferencia individual que le contradiga.

**Procedimiento de enmienda**:
1. Proponer el cambio con justificación documentada.
2. Actualizar esta constitución e incrementar la versión según semver:
   - MAJOR: eliminación o redefinición incompatible de principios.
   - MINOR: adición de nuevos principios o secciones.
   - PATCH: aclaraciones, correcciones de redacción.
3. Propagar el cambio a las plantillas afectadas y actualizar el Sync Impact Report.
4. La enmienda entra en vigor en el siguiente feature que se inicie tras la ratificación.

**Cumplimiento**:
- Todos los PRs DEBEN verificarse contra esta constitución antes de aprobarse.
- El `plan.md` de cada feature incluye una sección "Constitution Check" que DEBE
  estar completa y sin violaciones no justificadas.
- La complejidad adicional no alineada con los principios DEBE documentarse en la
  tabla "Complexity Tracking" del `plan.md` con justificación explícita.

**Version**: 1.0.0 | **Ratified**: 2026-06-21 | **Last Amended**: 2026-06-21
