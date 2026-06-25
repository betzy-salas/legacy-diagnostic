# Quickstart: Reproducción del Diagnóstico Técnico

**Feature**: `001-legacy-diagnostic`
**Date**: 2026-06-21

Esta guía describe cómo ejecutar o reproducir el diagnóstico paso a paso.
No requiere compilar ni ejecutar el servicio. Solo lectura del código fuente.

## Prerrequisitos

- Acceso de lectura al directorio `src/` del repositorio.
- Acceso a este directorio de diseño: `specs/001-legacy-diagnostic/`.
- Los artefactos de referencia:
  - `data-model.md` — taxonomía de hallazgos
  - `contracts/diagnostic-report-schema.md` — esquema del informe
  - `research.md` — criterios de detección por dimensión

## Paso 1 — Inventario de archivos

Listar todos los archivos a auditar y registrar la cobertura:

```bash
# Listar archivos Java fuente
find src/main/java -name "*.java" | sort

# Listar archivos de configuración
find src/main/resources -type f | sort

# Contar total de archivos Java
find src/main/java -name "*.java" | wc -l
```

Resultado esperado: lista de ~20 archivos Java + `application.yml` + contrato OpenAPI.

## Paso 2 — Análisis por dimensión

Para cada archivo del inventario, aplicar los criterios de `research.md` (Decisión 2).

**Orden sugerido**:
1. `service/impl/EvaluateItfServiceImpl.java` — clase central, máxima densidad de hallazgos esperada
2. `proxy/singlestore/ItfTxnRepository.java` — lógica de negocio en capa de infraestructura
3. `proxy/redis/ItfRateRedisProxy.java` — posible uso de comando KEYS bloqueante
4. `expose/web/PositionKeepingApiImpl.java` — adaptador REST entrante
5. `scheduler/*.java` — schedulers de caché
6. `model/entities/*.java` — entidades JPA
7. `dto/*.java` — DTOs transversales
8. `config/*.java` — configuración

Para cada hallazgo encontrado, registrar inmediatamente siguiendo el formato
de `data-model.md`:

```
id: D{dim}-{NNN}
category: {D1..D7}
severity: {CRITICA|ALTA|MEDIA|BAJA}
location: src/main/java/com/bcp/cnf/services/.../Clase.java:{línea}
description: {descripción concisa}
evidence: |
  {snippet de código}
impact: {impacto técnico o de negocio}
principle: {principio de constitución o regla}
```

## Paso 3 — Evaluación arquitectónica hexagonal

Comparar la estructura de paquetes detectada contra el modelo Ports & Adapters:

1. Abrir `research.md` sección "Estado actual detectado".
2. Para cada paquete, verificar si existe una interfaz (puerto) para cada adaptador.
3. Para cada clase en `service/`, verificar que sus imports no incluyan paquetes de `proxy.*`, `expose.*` ni clases de framework.
4. Documentar cada violación como hallazgo `D2-XXX`.

## Paso 4 — Métricas cuantitativas

Para cada método con sospecha de alta complejidad:

```
Complejidad ciclomática estimada =
  1 (base)
  + número de: if, else if, for, while, catch, case, &&, ||
```

Umbral de reporte:
- > 10 → severidad Alta (D1-XXX)
- > 15 → severidad Crítica (D1-XXX)

Tamaño de métodos:
- Contar líneas de código activas (excluir líneas en blanco y comentarios)
- > 20 líneas → severidad Media
- > 40 líneas → severidad Alta

## Paso 5 — Consolidación del informe

Una vez completado el análisis de todos los archivos:

1. Crear `specs/001-legacy-diagnostic/informe-diagnostico.md` siguiendo
   la estructura de `contracts/diagnostic-report-schema.md`.
2. Ordenar hallazgos: Crítica → Alta → Media → Baja.
3. Completar la matriz de hallazgos (Sección 4 del informe).
4. Redactar el mapa arquitectónico (Sección 5 del informe).
5. Redactar la lista priorizada (Sección 6 del informe).
6. Calcular el `health_level` según la escala de `data-model.md`.
7. Redactar el resumen ejecutivo (Sección 2 del informe).

## Paso 6 — Verificación de cobertura y trazabilidad

Antes de dar el informe por completo:

```bash
# Verificar que src/ no fue modificado (SC-004)
git diff src/

# Contar archivos auditados vs. total
# El % de cobertura debe ser 100% (SC-001)
```

Revisar que:
- Cada hallazgo tiene exactamente los 7 campos requeridos (SC-002).
- La ubicación `archivo:línea` existe realmente en el repositorio.
- No quedan archivos del inventario sin haber generado al menos una entrada
  de "sin hallazgos" explícita (para documentar cobertura).

## Resultado esperado

Al finalizar los 6 pasos, el directorio del feature contendrá:

```text
specs/001-legacy-diagnostic/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── diagnostic-report-schema.md
├── tasks.md                        ← generado por /speckit-tasks
└── informe-diagnostico.md          ← entregable final del feature
```

El `informe-diagnostico.md` es el insumo directo del feature 003 (recomendaciones).
