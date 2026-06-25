# Data Model: Diagnóstico Técnico del Servicio Legacy

**Feature**: `001-legacy-diagnostic`
**Date**: 2026-06-21

## Entidades del Diagnóstico

### Finding (Hallazgo)

Unidad atómica del diagnóstico. Cada hallazgo es independiente, trazable y citable.

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `id` | String | ✅ | Identificador único. Formato: `D{dimensión}-{NNN}` (ej. `D1-001`, `D2-003`) |
| `category` | Enum (7 valores) | ✅ | Dimensión de análisis a la que pertenece el hallazgo |
| `severity` | Enum (4 valores) | ✅ | Nivel de criticidad del hallazgo |
| `location` | String | ✅ | Ruta relativa del archivo y número de línea: `archivo:línea` (ej. `src/main/java/.../EvaluateItfServiceImpl.java:86`) |
| `description` | String | ✅ | Descripción concisa del problema detectado |
| `evidence` | String (code snippet) | ✅ | Fragmento de código o configuración que evidencia el hallazgo |
| `impact` | String | ✅ | Consecuencia técnica o de negocio del problema si no se corrige |
| `principle` | String | ✅ | Principio de la constitución o regla de buenas prácticas violada |

#### Formato de id

```
D{dimensión}-{NNN}
```

Donde `dimensión` es el número de la categoría (1–7) y `NNN` es un secuencial de 3 dígitos dentro de esa categoría.

Ejemplos:
- `D1-001`: primer hallazgo de Clean Code
- `D2-001`: primer hallazgo de Arquitectura Hexagonal
- `D3-002`: segundo hallazgo de Errores de lógica

---

### Category (Categoría / Dimensión)

| Valor | Nombre completo |
|-------|----------------|
| `D1` | Clean Code |
| `D2` | Arquitectura Hexagonal |
| `D3` | Errores de lógica |
| `D4` | Malas prácticas |
| `D5` | Código muerto |
| `D6` | Código duplicado |
| `D7` | Código inerte |

---

### Severity (Severidad)

| Valor | Nombre | Definición |
|-------|--------|-----------|
| `CRITICA` | Crítica | Puede causar comportamiento incorrecto en producción, pérdida de datos, fallo de seguridad o condición de carrera. Requiere corrección antes de cualquier release. |
| `ALTA` | Alta | Degrada significativamente la mantenibilidad, introduce riesgo técnico alto o viola un principio arquitectónico fundamental. Debe corregirse en el próximo ciclo. |
| `MEDIA` | Media | Reduce la legibilidad o introduce deuda técnica considerable. Debe corregirse en el backlog próximo. |
| `BAJA` | Baja | Violación menor de buenas prácticas con impacto limitado. Puede corregirse oportunistamente. |

---

### DiagnosticReport (Informe de Diagnóstico)

Agregado que contiene todos los hallazgos más los artefactos de síntesis.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `service_name` | String | Nombre del servicio auditado |
| `audit_date` | Date | Fecha de ejecución del análisis |
| `audited_files` | List\<String\> | Lista de todos los archivos revisados |
| `coverage_pct` | Number | Porcentaje de archivos auditados vs. total de archivos fuente |
| `health_level` | Enum | Nivel de salud general: `CRÍTICO` / `DEGRADADO` / `ACEPTABLE` / `SALUDABLE` |
| `findings` | List\<Finding\> | Inventario completo de hallazgos |
| `severity_matrix` | Map\<Severity, Map\<Category, Integer\>\> | Conteo de hallazgos por severidad y categoría |
| `architecture_map` | ArchitectureMap | Mapa de estado actual vs. hexagonal objetivo |
| `critical_findings` | List\<Finding\> | Lista priorizada de hallazgos críticos y altos |
| `executive_summary` | String | Resumen ejecutivo del estado del servicio |

---

### HealthLevel (Nivel de salud)

Escala para el resumen ejecutivo, derivada de la distribución de severidades:

| Valor | Criterio |
|-------|---------|
| `CRÍTICO` | Existen hallazgos de severidad **Crítica** |
| `DEGRADADO` | Sin críticos, pero hay ≥ 5 hallazgos de severidad **Alta** |
| `ACEPTABLE` | Sin críticos ni degradación alta, pero hay hallazgos **Medios** relevantes |
| `SALUDABLE` | Solo hallazgos de severidad **Baja** o ninguno |

---

### ArchitectureMap (Mapa arquitectónico)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `current_packages` | List\<PackageMapping\> | Mapeo de cada paquete actual a su capa hexagonal |
| `dependency_violations` | List\<DependencyViolation\> | Violaciones de la regla de dependencias detectadas |
| `ports_inventory` | List\<PortAssessment\> | Estado de los puertos (interfaces) por adaptador |

#### PackageMapping

| Campo | Descripción |
|-------|-------------|
| `package` | Paquete Java actual (ej. `com.bcp.cnf.services.proxy.redis`) |
| `apparent_role` | Rol semántico detectado (ej. "Adaptador de caché Redis") |
| `expected_layer` | Capa hexagonal objetivo: `dominio` / `aplicacion` / `infraestructura` |
| `has_violation` | Boolean — ¿existe alguna violación de dependencias en este paquete? |

#### DependencyViolation

| Campo | Descripción |
|-------|-------------|
| `source_class` | Clase que introduce la dependencia incorrecta |
| `imported_class` | Clase importada que no debería ser visible desde la capa origen |
| `violation_type` | Tipo: `dominio→infraestructura` / `aplicacion→infraestructura` |
| `location` | `archivo:línea` del import problemático |

#### PortAssessment

| Campo | Descripción |
|-------|-------------|
| `adapter_class` | Clase adaptadora evaluada |
| `has_port_interface` | Boolean — ¿existe una interfaz que defina el puerto? |
| `port_interface` | Nombre de la interfaz si existe, `null` si ausente |
