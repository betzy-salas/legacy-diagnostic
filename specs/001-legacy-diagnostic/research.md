# Research: Diagnóstico Técnico del Servicio Legacy

**Feature**: `001-legacy-diagnostic`
**Date**: 2026-06-21

## Stack Detectado

Stack identificado inspeccionando `application.yml`, imports de clases Java y archivos de build.

| Componente | Tecnología | Evidencia |
|------------|-----------|-----------|
| Framework runtime | Quarkus | `application.yml`: claves `quarkus.*`; anotaciones `@ApplicationScoped` vía `jakarta.enterprise.context` |
| Lenguaje | Java | Extensión `.java`, paquete base `com.bcp.cnf.services` |
| Build | Maven | `pom.xml` en raíz del proyecto |
| CDI | Jakarta CDI | `jakarta.enterprise.context.ApplicationScoped`, `jakarta.inject.Inject` |
| REST | JAX-RS | `jakarta.ws.rs.core.Response`, interfaz `PositionKeepingsApi` |
| ORM | Hibernate ORM + Panache | `io.quarkus.hibernate.orm.panache.PanacheRepositoryBase` en `ItfTxnRepository` |
| Base de datos | SingleStore | `quarkus.datasource.db-kind: singlestore`, dialect `MySQL8Dialect` |
| Caché | Redis | `io.quarkus.redis.datasource.RedisDataSource`, `@RedisClientName` |
| Almacenamiento referencia | Azure Table Storage | `AzureTableConfig.java` |
| Config distribuida | MicroProfile + Kubernetes Config | `quarkus.kubernetes-config.enabled: true`, `org.eclipse.microprofile.rest.client` |
| OpenAPI | SmallRye OpenAPI | `quarkus.smallrye-openapi.path`, contrato en `src/main/resources/openapi/` |
| Framework interno | BCP ATLA FWK | `com.bcp.atla.fwk.core.error.ApiException`, `ExceptionCategoryTypes` |
| Logging | JBoss Logging | `org.jboss.logging.Logger` |
| HTTP port | 18080 | `quarkus.http.port: 18080` |
| Root path | `/business-position-keeping/v1` | `quarkus.http.root-path` |

## Decisiones Metodológicas

### Decisión 1 — Revisión manual en lugar de herramientas automatizadas

**Decisión**: El análisis estático se realiza mediante revisión manual directa del código
fuente, con criterios objetivos predefinidos por dimensión.

**Justificación**: No se dispone de un pipeline de build ejecutable en este entorno de
análisis. No es posible ejecutar PMD, SpotBugs, SonarQube ni jdeps sin una JDK y Maven
configurados y conectados a los artefactos del proyecto. La revisión manual con criterios
cuantitativos concretos (umbrales de complejidad, conteo de líneas, detección de patrones)
produce evidencia igualmente trazable y citable.

**Alternativas consideradas**:
- PMD: requiere Maven ejecutable y acceso al classpath completo
- SpotBugs: requiere compilación previa del proyecto
- SonarQube: requiere servidor configurado y análisis previo
- jdeps: requiere JAR compilado

**Método manual equivalente** por métrica:
- Complejidad ciclomática: contar ramas de decisión (`if`, `else if`, `for`, `while`,
  `catch`, `switch case`, `&&`, `||`) por método. Umbral crítico: > 10.
- Tamaño de métodos: contar líneas de código activas por método. Umbral: > 20 líneas.
- Tamaño de clases: contar líneas totales. Umbral: > 200 líneas.
- Duplicación: comparar bloques lógicos entre archivos del mismo paquete y entre capas.
- Acoplamiento aferente/eferente: contar imports externos por clase.

---

### Decisión 2 — Criterios de detección por dimensión

#### Dimensión 1: Clean Code

| Criterio | Evidencia a recolectar |
|----------|----------------------|
| Nombres no descriptivos | Identificar variables de 1-2 letras sin contexto claro, abreviaturas ambiguas |
| Métodos > 20 líneas | Citar método con conteo de líneas y snippet de inicio/fin |
| Complejidad ciclomática > 10 | Citar método con conteo de ramas y snippet |
| Números/cadenas mágicas | Citar literal hardcodeado sin constante nombrada |
| Comentarios obsoletos o de tipo JavaDoc boilerplate | Citar sección del comentario |
| Catch silenciados o demasiado genéricos | Citar bloque try-catch |
| Acoplamiento excesivo (> 5 dependencias directas) | Listar imports de la clase |

#### Dimensión 2: Arquitectura Hexagonal

| Criterio | Evidencia a recolectar |
|----------|----------------------|
| Dominio importando infraestructura | Citar import en clase de dominio |
| Servicio importando directamente repositorios/proxies (no puertos) | Citar import en `service/` |
| Lógica de negocio en controlador o repositorio | Citar método con lógica de negocio fuera del servicio |
| Ausencia de puertos (interfaces) para adaptadores | Verificar si existe interfaz para cada proxy |
| Entidades JPA en la capa de servicio como objetos de dominio | Citar uso de `@Entity` en servicio |

#### Dimensión 3: Errores de lógica

| Criterio | Evidencia a recolectar |
|----------|----------------------|
| Variables mutables en bucle con riesgo de estado incorrecto | Citar variable y bucle |
| Nulos sin protección (`NullPointerException` potencial) | Citar acceso sin null-check previo |
| Condición de carrera en ID secuencial manual | Citar `MAX(id) + 1` sin bloqueo |
| Flujos de retorno inconsistentes | Citar método con retornos de distinto tipo según rama |
| Transacciones demasiado amplias o ausentes donde se requieren | Citar clase/método con `@Transactional` |

#### Dimensión 4: Malas prácticas

| Criterio | Evidencia a recolectar |
|----------|----------------------|
| Violación SRP (clase hace más de una cosa) | Citar clase con responsabilidades múltiples |
| Uso de `keys.keys("pattern")` en Redis (comando KEYS bloquea el servidor) | Citar llamada |
| Cast explícito `(Type) obj` sin verificación | Citar cast |
| `Object` como tipo de parámetro donde se conoce el tipo real | Citar firma del método |
| Logging con concatenación de string en lugar de parámetros | Citar llamada al logger |
| Secret o configuración hardcodeada | Buscar literals con formato de credencial |
| Uso de `createNativeQuery` cuando el ORM puede resolver la operación | Citar query nativo; verificar si existe equivalente JPQL o Panache que elimine el SQL hardcodeado y los truncados manuales asociados |

#### Dimensión 5: Código muerto

| Criterio | Evidencia a recolectar |
|----------|----------------------|
| Método definido en clase pero sin llamadas desde ningún otro archivo | Citar método |
| Import sin uso | Citar import |
| Variable local asignada pero no leída | Citar variable |
| Rama de código inalcanzable | Citar condición |

#### Dimensión 6: Código duplicado

| Criterio | Evidencia a recolectar |
|----------|----------------------|
| Bloque `buildXxxException` idéntico en múltiples clases | Citar ambas ocurrencias |
| Lógica de truncado repetida | Citar ocurrencias |
| Patrón de logging `[ClassName - methodName]` replicado manualmente en cada método | Citar patrón |
| Validación de nulos/vacíos duplicada entre clases | Citar ambas |

#### Dimensión 7: Código inerte

| Criterio | Evidencia a recolectar |
|----------|----------------------|
| Método que solo llama a otro sin añadir valor | Citar wrapper innecesario |
| Asignación a variable que nunca se lee | Citar asignación |
| Catch que relanza la misma excepción sin transformación | Citar bloque |
| Retorno de objeto vacío `new EvaluateItfResponseInner()` sin campos | Citar retorno |

---

### Decisión 3 — Mapa arquitectónico hexagonal

**Decisión**: Usar una tabla de mapeo paquete-actual → capa-hexagonal como formato
del mapa arquitectónico, complementada con una lista de violaciones de la regla
de dependencias.

**Justificación**: El formato tabular es trazable, citabe en el informe y directamente
accionable para el feature 003 (recomendaciones). No requiere herramientas de diagramado.

**Estado actual detectado**:

| Paquete actual | Rol aparente | Capa hexagonal esperada | ¿Violación? |
|----------------|-------------|------------------------|-------------|
| `service/` | Lógica de negocio | Dominio / Aplicación | Parcial — `EvaluateItfServiceImpl` importa `proxy.*` directamente |
| `proxy/redis/` | Adaptador de caché | Infraestructura | Sin interfaz (puerto) expuesto |
| `proxy/singlestore/` | Adaptador de BD | Infraestructura | Sin interfaz (puerto); implementa `PanacheRepositoryBase` directamente |
| `proxy/tablestorage/` | Adaptador de referencia | Infraestructura | Sin interfaz (puerto) expuesto |
| `expose/web/` | Adaptador REST entrante | Infraestructura | Correcto: implementa interfaz generada por OpenAPI |
| `scheduler/` | Adaptador de scheduling | Infraestructura | Accede directamente a proxies sin puertos |
| `model/entities/` | Entidades JPA | Infraestructura (BD) | Mezclado con modelo de dominio |
| `model/api/` | DTOs generados por OpenAPI | Infraestructura (REST) | Usados como tipos de retorno del servicio |
| `dto/` | DTOs internos | Aplicación / Infraestructura | Ambiguo — `EvaluateItfHeaders` pasa por todas las capas |
| `config/` | Configuración de infraestructura | Infraestructura | Correcto |
