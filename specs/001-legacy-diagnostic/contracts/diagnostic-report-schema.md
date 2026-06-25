# Contract: Esquema del Informe de Diagnóstico

**Feature**: `001-legacy-diagnostic`
**Date**: 2026-06-21

Este documento define la estructura del informe de diagnóstico que es el
entregable del feature 001. No es un contrato de API REST: es el contrato
de formato del documento `informe-diagnostico.md`.

## Estructura del informe

```
informe-diagnostico.md
├── 1. Encabezado
├── 2. Resumen ejecutivo
├── 3. Inventario de hallazgos
├── 4. Matriz de hallazgos
├── 5. Mapa arquitectónico
└── 6. Lista priorizada de hallazgos críticos
```

---

## Sección 1 — Encabezado

```markdown
# Informe de Diagnóstico: {service_name}

**Fecha de auditoría**: {YYYY-MM-DD}
**Auditado por**: Claude Code (análisis estático manual)
**Archivos revisados**: {N} de {total} ({coverage_pct}%)
**Nivel de salud general**: {CRÍTICO | DEGRADADO | ACEPTABLE | SALUDABLE}
**Total de hallazgos**: {total} ({críticos: N, altos: N, medios: N, bajos: N})
```

---

## Sección 2 — Resumen ejecutivo

Párrafo de 3–5 oraciones describiendo:
1. El estado general del servicio.
2. Las dimensiones con mayor concentración de problemas.
3. El principal riesgo técnico identificado.
4. La urgencia relativa de atención.

Ejemplo de estructura:
```markdown
## Resumen ejecutivo

El servicio {service_name} presenta un nivel de salud {health_level}. Las
dimensiones con mayor concentración de hallazgos son {D1}, {D2} y {D4},
acumulando {N} de los {total} hallazgos totales. El riesgo técnico más crítico
es {descripción del hallazgo más grave}. Se recomienda atención prioritaria en
{área} antes de iniciar cualquier ciclo de reconstrucción.
```

---

## Sección 3 — Inventario de hallazgos

Tabla con todos los hallazgos ordenados por severidad (crítica primero) y dentro
de cada severidad, por categoría.

```markdown
## Inventario de hallazgos

| ID | Categoría | Severidad | Ubicación | Descripción |
|----|-----------|-----------|-----------|-------------|
| D1-001 | Clean Code | ALTA | `src/.../EvaluateItfServiceImpl.java:138` | Método `processItfCalculation` supera 60 líneas |
| D2-001 | Arquitectura Hexagonal | ALTA | `src/.../EvaluateItfServiceImpl.java:13` | Servicio importa directamente `ItfTxnRepository` (infraestructura) |
| ... | ... | ... | ... | ... |
```

Para cada hallazgo, incluir también su ficha completa debajo de la tabla:

```markdown
### {ID} — {Descripción corta}

**Categoría**: {categoría}
**Severidad**: {severidad}
**Ubicación**: `{archivo:línea}`
**Principio**: {principio de constitución o regla violada}

**Descripción**:
{descripción completa del problema}

**Evidencia**:
```java
// Snippet de código citado
```

**Impacto**:
{consecuencia técnica o de negocio}
```

---

## Sección 4 — Matriz de hallazgos

Tabla cruzada severidad × categoría con conteo de hallazgos.

```markdown
## Matriz de hallazgos

| Severidad | D1 Clean Code | D2 Hexagonal | D3 Lógica | D4 Prácticas | D5 Muerto | D6 Duplicado | D7 Inerte | Total |
|-----------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Crítica   | N   | N   | N   | N   | N   | N   | N   | N   |
| Alta      | N   | N   | N   | N   | N   | N   | N   | N   |
| Media     | N   | N   | N   | N   | N   | N   | N   | N   |
| Baja      | N   | N   | N   | N   | N   | N   | N   | N   |
| **Total** | N   | N   | N   | N   | N   | N   | N   | **N** |
```

---

## Sección 5 — Mapa arquitectónico

### 5.1 Estado actual de paquetes

```markdown
## Mapa arquitectónico

### Estado actual vs. arquitectura hexagonal objetivo

| Paquete actual | Rol detectado | Capa hexagonal esperada | ¿Violación? |
|----------------|--------------|------------------------|:-----------:|
| `service/` | Lógica de negocio | Dominio / Aplicación | ⚠️ Parcial |
| `proxy/redis/` | Adaptador Redis | Infraestructura | ✅ / ⚠️ |
| ... | ... | ... | ... |
```

### 5.2 Violaciones de la regla de dependencias

```markdown
### Violaciones de la regla de dependencias

| Clase origen | Import problemático | Tipo de violación | Ubicación |
|-------------|--------------------|--------------------|-----------|
| `EvaluateItfServiceImpl` | `ItfTxnRepository` | aplicacion→infraestructura | `...ServiceImpl.java:13` |
| ... | ... | ... | ... |
```

### 5.3 Estado de los puertos (interfaces)

```markdown
### Estado de puertos por adaptador

| Adaptador | ¿Tiene puerto (interfaz)? | Interfaz |
|-----------|:---:|---------|
| `ItfTxnRepository` | ❌ No | — |
| `ItfRateRedisProxy` | ❌ No | — |
| `PositionKeepingApiImpl` | ✅ Sí | `PositionKeepingsApi` (generada por OpenAPI) |
| ... | ... | ... |
```

---

## Sección 6 — Hallazgos críticos priorizados

Lista ordenada de los hallazgos de severidad Crítica y Alta, con su impacto
y referencia al ID del inventario.

```markdown
## Hallazgos críticos priorizados

1. **[{ID}] {descripción}** — Severidad: {CRÍTICA|ALTA}
   Impacto: {impacto resumido}
   Ver: Sección 3, ficha {ID}

2. **[{ID}] {descripción}** — Severidad: {CRÍTICA|ALTA}
   Impacto: {impacto resumido}
   Ver: Sección 3, ficha {ID}

...
```
