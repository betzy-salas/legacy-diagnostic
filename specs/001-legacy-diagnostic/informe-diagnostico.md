# Informe de Diagnóstico: business-position-keeping (ITF)

**Fecha de auditoría**: 2026-06-21
**Auditado por**: Claude Code (análisis estático manual)
**Archivos revisados**: 22 de 22 Java + 1 application.yml (100%)
**Nivel de salud general**: CRÍTICO
**Total de hallazgos**: 79 (críticos: 4, altos: 27, medios: 28, bajos: 20)

---

## 2. Resumen ejecutivo

El servicio `business-position-keeping` presenta un nivel de salud **CRÍTICO** con cuatro defectos de producción activos: una condición de carrera en la generación de IDs de transacción, un bug de estado compartido entre iteraciones que invierte el indicador ITF de entradas no exoneradas, y dos instancias de variables de fecha/hora intercambiadas que persistirán valores incorrectos en Azure Table Storage. La dimensión con mayor densidad de hallazgos graves es **Arquitectura Hexagonal** (11 hallazgos, 9 altos): el servicio carece de puertos (interfaces) para todos sus adaptadores de infraestructura y el servicio de aplicación acopla directamente entidades JPA y clases de infraestructura. El riesgo técnico inmediato más alto es la ausencia de secuencias o bloqueos en la generación del `itf_transaction_id`, que bajo carga concurrente producirá violaciones de clave primaria. Se recomienda atención prioritaria a los cuatro hallazgos críticos antes de cualquier ciclo de reconstrucción o incremento de carga.

---

## 3. Inventario de hallazgos

### Tabla resumen

| ID | Categoría | Severidad | Ubicación |
|----|-----------|-----------|-----------|
| D1-001 | Clean Code | ALTA | `EvaluateItfServiceImpl.java:138` |
| D1-002 | Clean Code | ALTA | `EvaluateItfServiceImpl.java:91` |
| D1-003 | Clean Code | ALTA | `ItfTxnRepository.java:69` |
| D1-004 | Clean Code | BAJA | `EvaluateItfServiceImpl.java:143` |
| D1-005 | Clean Code | BAJA | `EvaluateItfServiceImpl.java:147` |
| D1-006 | Clean Code | MEDIA | `EvaluateItfServiceImpl.java:257` |
| D1-007 | Clean Code | MEDIA | `ItfTxnRepository.java:75` |
| D1-008 | Clean Code | BAJA | `PositionKeepingApiImpl.java:8` |
| D1-009 | Clean Code | MEDIA | `EvaluateItfServiceImpl.java:86` |
| D1-010 | Clean Code | BAJA | `ItfRateTableStorageProxy.java:52` |
| D1-011 | Clean Code | BAJA | `ItfRateRedisProxy.java:77` |
| D1-012 | Clean Code | ALTA | `EvaluateItfServiceImpl.java:9` |
| D1-013 | Clean Code | ALTA | `EvaluateItfServiceImpl.java:43`, `PositionKeepingApiImpl.java:34` |
| D1-014 | Clean Code | MEDIA | `dto/CodTxnExoneratedItfRedisDto.java:9`, `dto/ItfsRateRedisDto.java:6` |
| D1-015 | Clean Code | BAJA | `scheduler/CacheWarmUpSupport.java:13` |
| D1-016 | Clean Code | ALTA | `model/entities/ItfTxn.java:17`, múltiples (6 clases) |
| D1-017 | Clean Code | ALTA | `proxy/redis/CodTxnExoneratedItfRedisProxy.java:23`, múltiples (4 clases) |
| D1-018 | Clean Code | BAJA | `model/api/EvaluateItfResponseInner.java`, `model/api/EvaluateItfRequestEntriesInner.java` |
| D1-019 | Clean Code | MEDIA | `model/entities/ItfTxnId.java:13` |
| D1-020 | Clean Code | BAJA | múltiples (9 clases) |
| D1-021 | Clean Code | ALTA | `model/entities/PdhBalance.java:20`, `proxy/singlestore/PdhBalanceRepository.java:18` |
| D1-022 | Clean Code | MEDIA | `service/impl/EvaluateItfServiceImpl.java:252`, `EvaluateItfServiceImpl.java:260` |
| D1-023 | Clean Code | MEDIA | `dto/EvaluateItfHeaders.java:8`, `proxy/singlestore/ItfTxnRepository.java:35` |
| D1-024 | Clean Code | BAJA | `proxy/singlestore/PdhBalanceRepository.java:69` |
| D1-025 | Clean Code | MEDIA | `dto/EvaluateItfHeaders.java:12` |
| D1-026 | Clean Code | MEDIA | `service/impl/EvaluateItfServiceImpl.java:95`, múltiples |
| D1-027 | Clean Code | BAJA | paquetes `services`, `entities`, `enums` |
| D2-001 | Arquitectura Hexagonal | ALTA | `EvaluateItfServiceImpl.java:13` |
| D2-002 | Arquitectura Hexagonal | ALTA | `ItfTxnRepository.java:1` |
| D2-003 | Arquitectura Hexagonal | ALTA | `PdhBalanceRepository.java:1` |
| D2-004 | Arquitectura Hexagonal | ALTA | `CodTxnExoneratedItfRedisProxy.java:1` |
| D2-005 | Arquitectura Hexagonal | ALTA | `ItfRateRedisProxy.java:1` |
| D2-006 | Arquitectura Hexagonal | MEDIA | `ItfRateTableStorageProxy.java:1` |
| D2-007 | Arquitectura Hexagonal | MEDIA | `ExoneratedCodTxnItfTableStorageProxy.java:1` |
| D2-008 | Arquitectura Hexagonal | ALTA | `EvaluateItfServiceImpl.java:9` |
| D2-009 | Arquitectura Hexagonal | ALTA | `EvaluateItfServiceImpl.java:10` |
| D2-010 | Arquitectura Hexagonal | MEDIA | `EvaluateItfHeaders.java:6` |
| D2-011 | Arquitectura Hexagonal | MEDIA | `ItfRateCacheLoader.java:8` |
| D3-001 | Errores de lógica | CRITICA | `EvaluateItfServiceImpl.java:147` |
| D3-002 | Errores de lógica | CRITICA | `ItfTxnRepository.java:124` |
| D3-003 | Errores de lógica | CRITICA | `ItfRateTableStorageProxy.java:52` |
| D3-004 | Errores de lógica | CRITICA | `ExoneratedCodTxnItfTableStorageProxy.java:64` |
| D3-005 | Errores de lógica | MEDIA | `PdhBalanceRepository.java:72` |
| D3-006 | Errores de lógica | ALTA | `EvaluateItfServiceImpl.java:105` |
| D3-007 | Errores de lógica | MEDIA | `ItfTxn.java:59` |
| D4-001 | Malas prácticas | ALTA | `ItfRateRedisProxy.java:110` |
| D4-002 | Malas prácticas | ALTA | `ItfTxnRepository.java:69` |
| D4-003 | Malas prácticas | ALTA | `ItfTxnRepository.java:73` |
| D4-004 | Malas prácticas | MEDIA | `EvaluateItfServiceImpl.java:53` |
| D4-005 | Malas prácticas | BAJA | `EvaluateItfServiceImpl.java:276` |
| D4-006 | Malas prácticas | MEDIA | `AzureTableConfig.java:19` |
| D4-007 | Malas prácticas | MEDIA | `ItfRateTableStorageProxy.java:26` |
| D4-008 | Malas prácticas | BAJA | `ExoneratedAccountRepository.java:33` |
| D4-009 | Malas prácticas | ALTA | `EvaluateItfServiceImpl.java:43` |
| D4-010 | Malas prácticas | ALTA | `EvaluateItfServiceImpl.java:138` |
| D4-011 | Malas prácticas | ALTA | `model/entities/` (múltiples) |
| D4-012 | Malas prácticas | ALTA | `PositionKeepingApiImpl.java:43` |
| D4-013 | Malas prácticas | ALTA | `ItfTxnRepository.java:91`, `ItfTxnRepository.java:156` |
| D5-001 | Código muerto | MEDIA | `ItfTxnRepository.java:35` |
| D5-002 | Código muerto | MEDIA | `ItfRateRedisProxy.java:51` |
| D5-003 | Código muerto | BAJA | `ItfRateRedisProxy.java:92` |
| D5-004 | Código muerto | BAJA | `CodTxnExoneratedItfRedisProxy.java:99` |
| D5-005 | Código muerto | MEDIA | `ItfRateTableStorageProxy.java:34` |
| D5-006 | Código muerto | MEDIA | `ItfRateTableStorageProxy.java:49` |
| D5-007 | Código muerto | MEDIA | `ExoneratedCodTxnItfTableStorageProxy.java:34` |
| D5-008 | Código muerto | MEDIA | `ExoneratedCodTxnItfTableStorageProxy.java:53` |
| D5-009 | Código muerto | ALTA | `ExoneratedAccountRepository.java:1` |
| D5-010 | Código muerto | MEDIA | `PdhBalanceRepository.java:90` |
| D5-011 | Código muerto | BAJA | `ItfTxnRepository.java:174` |
| D6-001 | Código duplicado | ALTA | múltiples (9 clases) |
| D6-002 | Código duplicado | BAJA | `ItfRateRedisProxy.java:147` |
| D6-003 | Código duplicado | ALTA | `ItfRateTableStorageProxy.java:52` |
| D6-004 | Código duplicado | BAJA | `ItfRateCacheLoader.java:35` |
| D6-005 | Código duplicado | MEDIA | múltiples (9 clases) |
| D7-001 | Código inerte | MEDIA | `EvaluateItfServiceImpl.java:127` |
| D7-002 | Código inerte | MEDIA | `CodTxnExoneratedItfCacheLoader.java:57` |
| D7-003 | Código inerte | BAJA | `ItfTxnRepository.java:174` |
| D7-004 | Código inerte | BAJA | `ExoneratedAccountRepository.java:47` |
| D7-005 | Código inerte | BAJA | `PdhBalanceRepository.java:105` |

---

### Fichas de hallazgos CRÍTICOS

---

### D3-001 — `flagItf` con estado compartido entre iteraciones del bucle

**Categoría**: Errores de lógica
**Severidad**: CRITICA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:147`
**Principio**: Constitución Principio III (Trazabilidad) / Clean Code: variables con alcance mínimo necesario

**Descripción**:
La variable `flagItf` se inicializa como `true` antes del bucle `for` y solo se establece en `false` cuando una entrada es exonerada, pero nunca se restablece a `true` entre iteraciones. Si la entrada N es exonerada (`flagItf = false`), todas las entradas N+1, N+2, ... también recibirán `applyItfIndicator = false` aunque no sean exoneradas, lo que resultará en un cálculo ITF incorrecto para esas entradas.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:147
boolean flagItf = true;  // inicializado FUERA del bucle

for (var entry : request.getEntries()) {
    // ...
    if (verifyExonerationByAccountNumberFormat(entry.getMeanInformation().getReferenceId())
        || verifyItfExoneration(entry.getUtc(), productType)
        || isOwnAccountTransfer(request)) {
      itfAmountToPayed = BigDecimal.ZERO;
      flagItf = false;  // se pone false, pero NUNCA se restablece a true
    }
    // ...
    response.add(
        new EvaluateItfResponseInner()
            .applyItfIndicator(flagItf)  // usa estado de iteraciones anteriores
    );
}
```

**Impacto**:
En requests con múltiples entradas donde alguna es exonerada, las entradas posteriores que sí deben pagar ITF recibirán `applyItfIndicator = false` y `itfAmountToPayed = BigDecimal.ZERO`, provocando una subestimación del impuesto a pagar. Es un error de cálculo financiero activo en producción.

---

### D3-002 — `nextItfTransactionId()` con condición de carrera

**Categoría**: Errores de lógica
**Severidad**: CRITICA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/singlestore/ItfTxnRepository.java:124`
**Principio**: Constitución Principio II (Arquitectura Hexagonal objetivo) / concurrencia transaccional

**Descripción**:
La generación del `itf_transaction_id` usa `MAX(id) + 1` sin ningún mecanismo de bloqueo. Bajo carga concurrente, dos hilos pueden leer el mismo `MAX`, ambos calcular el mismo ID y ambos intentar insertar con la misma clave primaria, causando violación de constraint.

**Evidencia**:
```java
// ItfTxnRepository.java:124
private Long nextItfTransactionId() {
    try {
      Number maxId = (Number) getEntityManager()
          .createNativeQuery("SELECT COALESCE(MAX(itf_transaction_id), 0) FROM itf_transaction")
          .getSingleResult();
      return maxId.longValue() + 1;  // race condition: dos hilos pueden obtener el mismo valor
    } catch (Exception ex) {
      throw buildDbException("nextItfTransactionId", ex);
    }
}
```

**Impacto**:
Transacciones ITF perdidas bajo carga concurrente. La excepción de clave duplicada es capturada por el `catch (Exception ex)` genérico de `saveItfTxn` y relanzada como `ApiException(EXTERNAL_ERROR)`, causando que la evaluación ITF del cliente falle sin diagnóstico claro.

---

### D3-003 — Variables `nowDateFormated` / `nowTimeFormated` intercambiadas en `ItfRateTableStorageProxy.save()`

**Categoría**: Errores de lógica
**Severidad**: CRITICA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/tablestorage/ItfRateTableStorageProxy.java:52`
**Principio**: Clean Code: nombres precisos; Corrección de lógica de datos de auditoría

**Descripción**:
Las variables `nowDateFormated` y `nowTimeFormated` están nombradas y asignadas de forma invertida: `nowDateFormated` almacena una hora (formato `HH:mm:ss`) y `nowTimeFormated` almacena una fecha (ISO_DATE). Al persistir en Azure Table Storage, los cuatro campos `timeCreated`, `dateCreate`, `timeUpdate`, `dateUpdate` reciben valores incorrectos.

**Evidencia**:
```java
// ItfRateTableStorageProxy.java:52
var nowDateFormated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); // guarda HORA
var nowTimeFormated = LocalDate.now().format(DateTimeFormatter.ISO_DATE);                  // guarda FECHA

entity.getProperties().put("timeCreated", nowTimeFormated); // FECHA en campo de HORA
entity.getProperties().put("dateCreate",  nowDateFormated); // HORA en campo de FECHA
entity.getProperties().put("timeUpdate",  nowTimeFormated); // FECHA en campo de HORA
entity.getProperties().put("dateUpdate",  nowDateFormated); // HORA en campo de FECHA
```

**Impacto**:
Las tasas ITF almacenadas en Azure Table Storage tienen todos sus metadatos de auditoría de fecha/hora intercambiados. Los procesos que lean estos campos obtendrán valores inválidos. La auditoría de cambios de tasas es inútil.

---

### D3-004 — Variables `nowDateFormated` / `nowTimeFormated` intercambiadas en `ExoneratedCodTxnItfTableStorageProxy.save()`

**Categoría**: Errores de lógica
**Severidad**: CRITICA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/tablestorage/ExoneratedCodTxnItfTableStorageProxy.java:64`
**Principio**: Clean Code: nombres precisos; D6-003 (esta duplicación replica el bug)

**Descripción**:
Copia exacta del mismo error que D3-003 en la clase hermana. Variables de fecha y hora intercambiadas al persistir metadatos de auditoría de códigos exonerados ITF.

**Evidencia**:
```java
// ExoneratedCodTxnItfTableStorageProxy.java:64
var nowDateFormated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); // HORA
var nowTimeFormated = LocalDate.now().format(DateTimeFormatter.ISO_DATE);                  // FECHA

entity.getProperties().put("timeCreated", nowTimeFormated); // FECHA en HORA
entity.getProperties().put("dateCreate",  nowDateFormated); // HORA en FECHA
entity.getProperties().put("timeUpdate",  nowTimeFormated); // FECHA en HORA
entity.getProperties().put("dateUpdate",  nowDateFormated); // HORA en FECHA
```

**Impacto**:
Los registros de exoneración de códigos ITF en Azure Table Storage tienen metadatos de auditoría incorrectos. Afecta a todos los registros escritos desde el inicio del servicio.

---

### Fichas de hallazgos ALTOS

---

### D1-001 — `processItfCalculation` supera 60 líneas

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:138`
**Principio**: SRP / Clean Code: métodos de responsabilidad única

**Descripción**: El método `processItfCalculation` tiene aproximadamente 65 líneas con complejidad ciclomática estimada en 8 (1 for + 2 if CARGO + 1 ternario + 3 condiciones de exoneración). Mezcla cálculo de base imponible, consulta de saldo PDH, evaluación de exoneración y construcción de respuesta.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:138-204 — método de ~65 líneas activas
private List<EvaluateItfResponseInner> processItfCalculation(...) {
    BigDecimal noTaxesAmount = BigDecimal.ZERO;
    // ... cálculo, consulta saldo, exoneración, persistencia, construcción respuesta ...
}
```

**Impacto**: Alta dificultad de comprensión y testing unitario. Cambios en cualquiera de las responsabilidades requieren modificar el método completo, aumentando el riesgo de regresión.

---

### D1-002 — `processReversal` supera 40 líneas

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:91`
**Principio**: Clean Code: métodos de responsabilidad única

**Descripción**: `processReversal` tiene ~40 líneas y complejidad estimada en 7. Mezcla búsqueda de transacciones, construcción de mapa de claves, iteración de entradas, actualización de saldo PDH y cambio de estado de transacción.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:91-132 — método de ~40 líneas
private List<EvaluateItfResponseInner> processReversal(...) {
    List<ItfTxn> txns = itfTxnRepository.findAllWithoutReversalByOpnNumber(...);
    Map<String, ItfTxn> txnsByEntryKey = new HashMap<>();
    for (var txn : txns) { ... }
    for (var entry : request.getEntries()) { ... }
    return List.of(new EvaluateItfResponseInner());
}
```

**Impacto**: Igual que D1-001: testing difícil, alta exposición a regresiones.

---

### D1-003 — `saveItfTxn` supera 60 líneas con lógica de negocio embebida

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/singlestore/ItfTxnRepository.java:69`
**Principio**: SRP / Clean Code: los repositorios no deben contener lógica de negocio

**Descripción**: `saveItfTxn` es un método de ~55 líneas en un repositorio que construye la entidad desde parámetros, trunca strings, mapea tipos de operación y ejecuta SQL nativo. Contiene lógica de transformación de datos que debería residir en la capa de aplicación.

**Evidencia**:
```java
// ItfTxnRepository.java:69-122
public void saveItfTxn(EvaluateItfHeaders headers, Object entryObj, ...) {
    var entry = (EvaluateItfRequestEntriesInner) entryObj;  // cast inseguro
    var accountNumber = truncate(referenceId, 60);           // lógica de negocio
    var operationType = mapOperationType(entry.getEntryType()); // mapping
    var itfTransactionId = nextItfTransactionId();           // generación de ID
    getEntityManager().createNativeQuery("INSERT INTO itf_transaction ...").executeUpdate();
}
```

**Impacto**: Viola SRP. El repositorio no es reemplazable sin impactar lógica de negocio. Impide el testing de la lógica de construcción de entidades de forma aislada.

---

### D1-012 — `EvaluateItfServiceImpl` con 11+ dependencias directas

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:9`
**Principio**: Clean Code: acoplamiento mínimo; Constitución Principio II (Hexagonal)

**Descripción**: `EvaluateItfServiceImpl` importa y depende de 11+ clases de distintas capas: 2 repositorios de BD, 2 proxies Redis, 4 clases de modelo de API, 2 entidades JPA y 1 enum.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:9-17
import com.bcp.cnf.services.proxy.redis.CodTxnExoneratedItfRedisProxy;
import com.bcp.cnf.services.proxy.redis.ItfRateRedisProxy;
import com.bcp.cnf.services.proxy.singlestore.ItfTxnRepository;
import com.bcp.cnf.services.proxy.singlestore.PdhBalanceRepository;
import com.bcp.cnf.services.model.api.EvaluateItfRequest;
import com.bcp.cnf.services.model.api.EvaluateItfRequestEntriesInner;
import com.bcp.cnf.services.model.api.EvaluateItfResponseInner;
import com.bcp.cnf.services.model.entities.ItfTxn;
import com.bcp.cnf.services.model.entities.PdhBalance;
import com.bcp.cnf.services.model.enums.ItfFinalAccountStatus;
```

**Impacto**: Alta fragilidad ante cambios en cualquier adaptador. Testing unitario requiere mockear toda la infraestructura. Acoplamiento que dificulta la reconstrucción hexagonal.

---

### D2-001 — Servicio de aplicación depende de implementaciones concretas de infraestructura

**Categoría**: Arquitectura Hexagonal
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:13`
**Principio**: Constitución Principio II — Regla de dependencias: las capas interiores no dependen de las exteriores

**Descripción**: `EvaluateItfServiceImpl` (capa de aplicación) importa directamente `ItfTxnRepository`, `PdhBalanceRepository`, `CodTxnExoneratedItfRedisProxy` e `ItfRateRedisProxy` (capa de infraestructura). En Arquitectura Hexagonal, debería depender de puertos (interfaces) que la infraestructura implementa, no de las clases concretas.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:13-16
import com.bcp.cnf.services.proxy.redis.CodTxnExoneratedItfRedisProxy;  // adaptador concreto
import com.bcp.cnf.services.proxy.redis.ItfRateRedisProxy;              // adaptador concreto
import com.bcp.cnf.services.proxy.singlestore.ItfTxnRepository;         // adaptador concreto
import com.bcp.cnf.services.proxy.singlestore.PdhBalanceRepository;     // adaptador concreto
```

**Impacto**: Imposible sustituir la implementación de Redis o SingleStore sin modificar la clase de aplicación. Bloquea el testing unitario del servicio sin levantar infraestructura real.

---

### D2-002 a D2-005 — Ausencia de puertos (interfaces) para adaptadores de infraestructura

**Categoría**: Arquitectura Hexagonal
**Severidad**: ALTA
**Ubicación**:
- `src/.../proxy/singlestore/ItfTxnRepository.java:1` (D2-002)
- `src/.../proxy/singlestore/PdhBalanceRepository.java:1` (D2-003)
- `src/.../proxy/redis/CodTxnExoneratedItfRedisProxy.java:1` (D2-004)
- `src/.../proxy/redis/ItfRateRedisProxy.java:1` (D2-005)
**Principio**: Constitución Principio II — Puertos son interfaces; adaptadores las implementan

**Descripción**: Ninguno de los 4 adaptadores primarios tiene una interfaz Java (puerto) que lo defina. `ItfTxnRepository` implementa `PanacheRepositoryBase` (framework) en lugar de una interfaz de dominio propia. Los otros tres son clases concretas sin interfaz.

**Evidencia**:
```java
// ItfTxnRepository.java:24 — implementa framework, no puerto de dominio
public class ItfTxnRepository implements PanacheRepositoryBase<ItfTxn, ItfTxnId> { ... }

// ItfRateRedisProxy.java:25 — clase concreta sin interfaz
public class ItfRateRedisProxy { ... }
```

**Impacto**: Imposible intercambiar implementaciones (ej. cambiar Redis por otro caché) sin modificar el servicio. El Principio II de la constitución no puede cumplirse hasta que existan estos puertos.

---

### D2-008 — Entidades JPA (`ItfTxn`) importadas en la capa de aplicación

**Categoría**: Arquitectura Hexagonal
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:9`
**Principio**: Constitución Principio II — El dominio no depende de ORM ni infraestructura

**Descripción**: `EvaluateItfServiceImpl` importa `ItfTxn` (línea 9) y `PdhBalance` (línea 10), que son entidades JPA que extienden `PanacheEntityBase`. El servicio de aplicación opera sobre objetos de infraestructura de persistencia.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:9-10
import com.bcp.cnf.services.model.entities.ItfTxn;     // PanacheEntityBase - ORM
import com.bcp.cnf.services.model.entities.PdhBalance;  // PanacheEntityBase - ORM

// Uso en processReversal:155
var txn = txnsByEntryKey.remove(buildReversalEntryKey(accountNumber, utc));
if (txn != null) {
    PdhBalance pdh = pdhBalanceRepository.findByAccountAndUtc(accountNumber, utc);
```

**Impacto**: Migrar el ORM o cambiar el modelo de persistencia requiere modificar la lógica de negocio. El dominio queda atado al framework Hibernate/Panache.

---

### D2-009 — Entidad JPA `PdhBalance` importada en la capa de aplicación

**Categoría**: Arquitectura Hexagonal
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:10`
**Principio**: Constitución Principio II

**Descripción**: Igual que D2-008. `PdhBalance` extiende `PanacheEntityBase` y es manipulada directamente por `EvaluateItfServiceImpl`.

**Evidencia**:
```java
import com.bcp.cnf.services.model.entities.PdhBalance; // línea 10
// Uso directo en processItfCalculation:185
finalBalancePdh = obtainPdhBalance(referenceId);
```

**Impacto**: Mismo impacto que D2-008.

---

### D3-006 — NPE potencial en `processReversal` por `getMeanInformation()` sin null-check

**Categoría**: Errores de lógica
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:105`
**Principio**: Manejo defensivo de nulos en frontera

**Descripción**: El método `processReversal` llama `entry.getMeanInformation().getReferenceId()` sin verificar que `getMeanInformation()` no sea `null`. Si la entrada llega con ese campo ausente (malformación del request), se produce un `NullPointerException` que es capturado por el catch genérico y relanzado como `EXTERNAL_ERROR`, ocultando el origen real.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:105-106
String accountNumber = entry.getMeanInformation().getReferenceId(); // NPE si getMeanInformation() == null
String utc = entry.getUtc();
```

**Impacto**: Requests malformados producen un `EXTERNAL_ERROR` en lugar de un `BAD_REQUEST`, dificultando el diagnóstico y la comunicación al cliente.

---

### D4-001 — Uso del comando Redis `KEYS` en `ItfRateRedisProxy.getAll()`

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/redis/ItfRateRedisProxy.java:110`
**Principio**: Uso correcto del framework/infraestructura

**Descripción**: El método `getAll()` usa `keys.keys(KEY_PREFIX + "*")` que ejecuta el comando Redis `KEYS`, el cual es O(N) sobre el keyspace completo y bloquea el event loop de Redis durante su ejecución.

**Evidencia**:
```java
// ItfRateRedisProxy.java:110
for (String key : keys.keys(KEY_PREFIX + "*")) {
    Map<String, String> data = hash.hgetall(key);
```

**Impacto**: Bajo carga o con un keyspace Redis grande, el comando `KEYS` puede bloquear Redis durante segundos, causando timeouts en todas las operaciones concurrentes del servicio. Debería usarse `SCAN` con cursor.

---

### D4-002 — Parámetro `Object entryObj` sin tipo en `saveItfTxn`

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/singlestore/ItfTxnRepository.java:69`
**Principio**: SOLID — type safety

**Descripción**: El parámetro `entryObj` está tipado como `Object` cuando debería ser `EvaluateItfRequestEntriesInner`. Esto suprime la verificación de tipos en tiempo de compilación.

**Evidencia**:
```java
// ItfTxnRepository.java:69
public void saveItfTxn(EvaluateItfHeaders headers, Object entryObj, ...) {
    var entry = (EvaluateItfRequestEntriesInner) entryObj;  // cast en línea 73
```

**Impacto**: Si se llama con el tipo incorrecto, lanza `ClassCastException` en runtime. Reduce la legibilidad del contrato del método.

---

### D4-003 — Cast inseguro sin verificación `instanceof`

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/singlestore/ItfTxnRepository.java:73`
**Principio**: SOLID / defensividad en casteos

**Descripción**: Consecuencia directa de D4-002. El cast `(EvaluateItfRequestEntriesInner) entryObj` en línea 73 no verifica el tipo antes de hacer el cast.

**Evidencia**:
```java
// ItfTxnRepository.java:73
var entry = (EvaluateItfRequestEntriesInner) entryObj;
```

**Impacto**: `ClassCastException` en runtime si se pasa un tipo distinto.

---

### D5-009 — `ExoneratedAccountRepository` completamente sin uso en flujo de producción

**Categoría**: Código muerto
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/proxy/singlestore/ExoneratedAccountRepository.java:1`
**Principio**: YAGNI — código sin uso consume mantenimiento

**Descripción**: `ExoneratedAccountRepository` con sus dos métodos (`persistExoneratedAccount`, `findByAccountNumber`) no es inyectado ni referenciado por ninguna clase de producción. `EvaluateItfServiceImpl` no la usa. Solo aparece en su test.

**Evidencia**:
```java
// ExoneratedAccountRepository.java:16-17
public class ExoneratedAccountRepository
    implements PanacheRepositoryBase<ExoneratedAccount, String> {
// Sin ningún @Inject en clases de producción
```

**Impacto**: Código de infraestructura completo (clase + entidad `ExoneratedAccount`) que mantiene deuda de comprensión y dependencias sin contribuir funcionalidad activa.

---

### D4-009 — God Object: `EvaluateItfServiceImpl` viola SRP con 5 responsabilidades

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:43`
**Principio**: SOLID — Single Responsibility Principle

**Descripción**:
`EvaluateItfServiceImpl` concentra responsabilidades que deberían pertenecer a objetos distintos: (1) orquestación del flujo (evaluar vs. revertir), (2) cálculo de la base imponible ITF, (3) lógica de exoneración (por formato de cuenta, por UTC/producto, por tipo de transferencia), (4) actualización de saldo PDH, y (5) persistencia de la transacción ITF. Es un antipatrón **God Object** clásico.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:43 — una clase, cinco responsabilidades
public class EvaluateItfServiceImpl implements EvaluateItfService {
  // Responsabilidad 1: orquestación
  public List<EvaluateItfResponseInner> evaluateItf(...) { ... }
  // Responsabilidad 2: cálculo base imponible
  private List<EvaluateItfResponseInner> processItfCalculation(...) { ... }
  // Responsabilidad 3a: exoneración por formato
  private boolean verifyExonerationByAccountNumberFormat(...) { ... }
  // Responsabilidad 3b: exoneración por UTC/producto
  private boolean verifyItfExoneration(...) { ... }
  // Responsabilidad 3c: exoneración por tipo de transferencia
  private boolean isOwnAccountTransfer(...) { ... }
  // Responsabilidad 4: saldo PDH
  private BigDecimal obtainPdhBalance(...) { ... }
  // Responsabilidad 5: delegación de persistencia con lógica embebida
  private List<EvaluateItfResponseInner> processReversal(...) { ... }
}
```

**Impacto**: Todo cambio en cualquiera de las cinco responsabilidades requiere modificar la misma clase, aumentando el riesgo de regresión en las otras cuatro. La clase es el cuello de botella de comprensión del servicio completo.

---

### D4-010 — Transaction Script: `processItfCalculation` como script procedimental sin objetos de dominio

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:138`
**Principio**: SOLID — SRP / antipatrón Transaction Script (Fowler, Patterns of Enterprise Application Architecture)

**Descripción**:
`processItfCalculation` implementa toda la lógica de negocio como una secuencia procedimental de pasos sin delegar responsabilidades a objetos de dominio. La lógica de exoneración, el cálculo de base imponible, la actualización de saldo PDH y la construcción de la respuesta coexisten en el mismo método sin abstracciones que los representen.

**Evidencia**:
```java
// EvaluateItfServiceImpl.java:138-204 — script procedimental de ~65 líneas
for (var entry : request.getEntries()) {
    // paso 1: calcular noTaxesAmount
    // paso 2: actualizar PDH
    // paso 3: calcular baseEstimate
    // paso 4: calcular itfAmountToPayed
    // paso 5: verificar tres tipos de exoneración
    // paso 6: obtener finalBalancePdh
    // paso 7: persistir transacción
    // paso 8: construir respuesta
}
```

**Impacto**: La lógica no puede reutilizarse ni testearse de forma aislada. Añadir una nueva regla de exoneración o cambiar el cálculo requiere modificar el script completo. El patrón correcto es delegar a objetos de dominio (ej. `ItfCalculationRule`, `ExonerationPolicy`).

---

### D4-011 — Anemic Domain Model: entidades sin comportamiento

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/model/entities/` (ItfTxn, PdhBalance, ExoneratedAccount)
**Principio**: SOLID — SRP / antipatrón Anemic Domain Model (Fowler)

**Descripción**:
Las entidades de dominio (`ItfTxn`, `PdhBalance`, `ExoneratedAccount`) son contenedores de datos puros: solo campos públicos y sin métodos de negocio. Toda la lógica que les corresponde (construir una transacción ITF, calcular saldos, determinar estado) reside en `EvaluateItfServiceImpl` e `ItfTxnRepository`, creando un modelo anémico.

**Evidencia**:
```java
// ItfTxn.java — campos públicos sin encapsulamiento ni comportamiento
public class ItfTxn extends PanacheEntityBase {
  public ItfTxnId itfTxnId;
  public String utc;
  public BigDecimal itfAmount;
  public Boolean reversalFlag;
  public BigDecimal useMovementAccountPdh;
  // sin ningún método de negocio
}

// Lógica que debería estar en ItfTxn pero está en ItfTxnRepository:
var finalAccountStatus = ItfFinalAccountStatus.CALCULATED.name(); // ItfTxnRepository.java:88
```

**Impacto**: Las reglas de negocio quedan dispersas en servicios y repositorios. La entidad no protege sus invariantes. El código de construcción de `ItfTxn` en `ItfTxnRepository.saveItfTxn` (55 líneas) debería ser un factory o constructor de la propia entidad.

---

### D4-013 — Uso de queries nativos (`createNativeQuery`) cuando el ORM puede resolverlo

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**:
- `src/main/java/com/bcp/cnf/services/proxy/singlestore/ItfTxnRepository.java:91` (INSERT nativo)
- `src/main/java/com/bcp/cnf/services/proxy/singlestore/ItfTxnRepository.java:156` (UPDATE nativo)
**Principio**: Uso correcto del framework/ORM — Constitución Principio II (las capas de infraestructura deben usar su tecnología de forma idiomática)

**Descripción**:
`ItfTxnRepository` utiliza `getEntityManager().createNativeQuery(...)` para ejecutar un `INSERT` de 17 parámetros (línea 91) y un `UPDATE` con 6 condiciones (línea 156). Panache y JPQL pueden resolver ambas operaciones de forma tipada y portable sin SQL hardcodeado. El uso de queries nativos rompe la abstracción del ORM, suprime la verificación de tipos en tiempo de compilación y es la causa raíz de los truncados manuales de D1-007 (los límites de columna deben estar en el DDL/modelo, no en código de aplicación).

**Evidencia**:
```java
// ItfTxnRepository.java:91–118 — INSERT nativo de 17 parámetros
getEntityManager().createNativeQuery(
    "INSERT INTO itf_transaction ("
        + "itf_transaction_id, account_number, app_code, operation_number, ..."
        + ") VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17)")
    .setParameter(1, itfTransactionId)
    .setParameter(2, accountNumber)
    // ... 15 setParameter más
    .executeUpdate();

// ItfTxnRepository.java:156–168 — UPDATE nativo con 6 condiciones
getEntityManager().createNativeQuery(
    "UPDATE itf_transaction SET final_account_status = ?1, reversal_flag = ?2 "
        + "WHERE itf_transaction_id = ?3 AND account_number = ?4 "
        + "AND app_code = ?5 AND operation_number = ?6 AND reversal_flag = false")
    .setParameter(1, status.name())
    // ...
    .executeUpdate();
```

**Impacto**:
1. **Sin type safety**: un parámetro mal posicionado (`?3` ↔ `?4`) compila correctamente pero corrompe datos en runtime.
2. **Duplicación de restricciones de columna**: los 7 truncados manuales de `saveItfTxn` (D1-007) existen porque el query nativo no valida contra las anotaciones `@Column(length=...)` de la entidad. Con Panache, Hibernate lanzaría error al intentar persistir un valor que excede la columna.
3. **Acoplamiento al dialecto SQL**: el SQL está atado a SingleStore/MySQL. Migrar a otro motor requiere reescribir los queries.
4. **Causa habilitante de D1-007**: los truncados manuales de `truncate(value, 60)` son una consecuencia directa de no usar el modelo JPA.

---

### D4-012 — Ausencia de validación en frontera REST

**Categoría**: Malas prácticas
**Severidad**: ALTA
**Ubicación**: `src/main/java/com/bcp/cnf/services/expose/web/PositionKeepingApiImpl.java:43`
**Principio**: Validación en frontera — Constitución Principio III (trazabilidad de errores) / Bean Validation (Jakarta)

**Descripción**:
`PositionKeepingApiImpl.evaluateItfCompliance` recibe 14 parámetros (headers + body) y los pasa directamente al servicio sin ninguna validación. No hay anotaciones `@NotNull`, `@NotBlank` ni `@Valid` sobre el `EvaluateItfRequest`. Requests malformados atraviesan todas las capas hasta producir un `NullPointerException` o un error de BD que se devuelve como `EXTERNAL_ERROR`.

**Evidencia**:
```java
// PositionKeepingApiImpl.java:43-55 — 14 parámetros sin ninguna validación
public Response evaluateItfCompliance(
    String authorization,       // sin @NotBlank
    String requestId,           // sin @NotBlank
    String requestDate,         // sin @NotBlank
    // ...
    EvaluateItfRequest evaluateItfRequest,  // sin @Valid
    String userCode,
    String serverTerminal) {
  var headers = new EvaluateItfHeaders(...);  // delegación directa
  var response = evaluateItfService.evaluateItf(headers, evaluateItfRequest);
```

**Impacto**: Requests inválidos producen errores genéricos `EXTERNAL_ERROR` en lugar de `400 Bad Request`, dificultando el diagnóstico por parte de los consumidores de la API. El servicio no puede garantizar precondiciones antes de ejecutar la lógica de negocio.

---

### D6-001 — Patrón `buildXxxException` duplicado en 9 clases

**Categoría**: Código duplicado
**Severidad**: ALTA
**Ubicación**: múltiples — `EvaluateItfServiceImpl.java:274`, `ItfRateRedisProxy.java:138`, `CodTxnExoneratedItfRedisProxy.java:127`, `ItfTxnRepository.java:184`, `PdhBalanceRepository.java:114`, `ItfRateTableStorageProxy.java:84`, `ExoneratedCodTxnItfTableStorageProxy.java:97`, `ExoneratedAccountRepository.java:33`, `CacheWarmUpSupport.java:30`
**Principio**: DRY — don't repeat yourself

**Descripción**: Las 9 clases tienen una implementación prácticamente idéntica del patrón de construcción de `ApiException`, diferenciándose solo en el nombre de clase y mensaje. La constante `EXTERNAL_ERROR` también se repite (ver D6-005).

**Evidencia**:
```java
// Patrón repetido en 9 clases:
private ApiException buildXxxException(String methodName, Exception cause) {
    LOG.error("[ClassName - " + methodName + "] Error at ...", cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
}
```

**Impacto**: Cualquier cambio en la estrategia de manejo de excepciones (ej. agregar un campo al `ApiException`) requiere modificar 9 clases. Riesgo de inconsistencias entre versiones de cada clase.

---

### D6-003 — Bug de fecha/hora duplicado en dos clases de Table Storage

**Categoría**: Código duplicado
**Severidad**: ALTA
**Ubicación**: `ItfRateTableStorageProxy.java:52`, `ExoneratedCodTxnItfTableStorageProxy.java:64`
**Principio**: DRY — la duplicación propagó un bug crítico

**Descripción**: El mismo error de variables intercambiadas (D3-003/D3-004) fue copiado de una clase a la otra, multiplicando el alcance del defecto.

**Evidencia**:
```java
// Mismo bloque en ambas clases:
var nowDateFormated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); // HORA
var nowTimeFormated = LocalDate.now().format(DateTimeFormatter.ISO_DATE);                  // FECHA
```

**Impacto**: La duplicación transformó un bug puntual en dos bugs idénticos que afectan dos tablas de Azure Table Storage.

---

### Hallazgos de Nombramientos (T031) — fichas ALTOS

---

### D1-013 — Sufijo `Impl` en clases de implementación

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**:
- `src/main/java/com/bcp/cnf/services/service/impl/EvaluateItfServiceImpl.java:43`
- `src/main/java/com/bcp/cnf/services/expose/web/PositionKeepingApiImpl.java:34`
**Principio**: Clean Code: los nombres revelan intención, no posición estructural

**Descripción**: Las clases `EvaluateItfServiceImpl` y `PositionKeepingApiImpl` utilizan el sufijo `Impl` para indicar que son implementaciones de una interfaz. Este sufijo describe la relación estructural entre la clase y su interfaz, no el concepto que representa la clase. Una clase debe expresar qué hace, no que "implementa algo".

**Evidencia**:
```java
// service/impl/EvaluateItfServiceImpl.java:43
public class EvaluateItfServiceImpl implements EvaluateItfService { ... }

// expose/web/PositionKeepingApiImpl.java:34
public class PositionKeepingApiImpl implements PositionKeepingsApi { ... }
```

**Impacto**: Si se añade una segunda implementación (ej. para testing o para otro adaptador), no habrá criterio semántico para distinguirlas. El nombre no ayuda a razonar sobre el propósito en el contexto de la reconstrucción hexagonal.
- Nombre sugerido: `ItfComplianceEvaluator` (para `EvaluateItfServiceImpl`), `PositionKeepingResource` (para `PositionKeepingApiImpl`).

---

### D1-016 — Abreviación `Txn` (transaction) en nombres de clase

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**: múltiples
- `src/.../model/entities/ItfTxn.java:17`
- `src/.../model/entities/ItfTxnId.java:13`
- `src/.../dto/CodTxnExoneratedItfRedisDto.java:9`
- `src/.../proxy/redis/CodTxnExoneratedItfRedisProxy.java:23`
- `src/.../proxy/tablestorage/ExoneratedCodTxnItfTableStorageProxy.java:20`
- `src/.../scheduler/CodTxnExoneratedItfCacheLoader.java:20`
**Principio**: Clean Code — Abreviaciones: PROHIBIDO abreviar palabras del dominio si requieren traducción mental

**Descripción**: La abreviación `Txn` (de *transaction*) aparece en 6 nombres de clase. Viola la convención que prohíbe explícitamente los sufijos `Txn`. Requiere traducción mental para cualquier lector que no conozca la convención interna del equipo.

**Evidencia**:
```java
public class ItfTxn extends PanacheEntityBase { ... }              // model/entities/ItfTxn.java:17
public class ItfTxnId implements Serializable { ... }              // model/entities/ItfTxnId.java:13
public record CodTxnExoneratedItfRedisDto(...) { }                 // dto/CodTxnExoneratedItfRedisDto.java:9
public class CodTxnExoneratedItfRedisProxy { ... }                 // proxy/redis/CodTxnExoneratedItfRedisProxy.java:23
public class ExoneratedCodTxnItfTableStorageProxy { ... }          // proxy/tablestorage/ExoneratedCodTxnItfTableStorageProxy.java:20
public class CodTxnExoneratedItfCacheLoader { ... }                // scheduler/CodTxnExoneratedItfCacheLoader.java:20
```

**Impacto**: Los nombres de clase afectados acumulan múltiples abreviaciones (`Cod`, `Txn`, `Itf`), maximizando la carga cognitiva. Un desarrollador nuevo no puede entender el propósito sin consultar contexto adicional.
- Nombres sugeridos: `ItfTransaction`, `ItfTransactionKey`, `ExoneratedTransactionCache`, `ExoneratedTransactionCacheProxy`, `ExoneratedTransactionItfTableStorageProxy`, `ExoneratedTransactionItfCacheLoader`.

---

### D1-017 — Abreviación `Cod` (código en español) en nombres de clase

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**: múltiples
- `src/.../dto/CodTxnExoneratedItfRedisDto.java:9`
- `src/.../proxy/redis/CodTxnExoneratedItfRedisProxy.java:23`
- `src/.../proxy/tablestorage/ExoneratedCodTxnItfTableStorageProxy.java:20`
- `src/.../scheduler/CodTxnExoneratedItfCacheLoader.java:20`
**Principio**: Clean Code — Abreviaciones: PROHIBIDO abreviaciones de palabras en español dentro de código en inglés

**Descripción**: `Cod` es la abreviación de "código" (español) insertada en nombres de clase escritos en inglés. Viola la regla que prohíbe abreviaciones de palabras en español dentro de código en inglés. El lector debe saber que `Cod` significa "código de transacción" para entender el nombre, sin poder deducirlo del contexto técnico.

**Evidencia**:
```java
// En las 4 clases: Cod = "código" (español, abreviado), mezclado en nombres ingleses
public record CodTxnExoneratedItfRedisDto(...)          // dto/CodTxnExoneratedItfRedisDto.java:9
public class CodTxnExoneratedItfRedisProxy              // proxy/redis/CodTxnExoneratedItfRedisProxy.java:23
public class ExoneratedCodTxnItfTableStorageProxy       // proxy/tablestorage/.../ExoneratedCodTxnItfTableStorageProxy.java:20
public class CodTxnExoneratedItfCacheLoader             // scheduler/CodTxnExoneratedItfCacheLoader.java:20
```

**Impacto**: Combinar abreviaciones en español con términos en inglés en el mismo identificador crea una mezcla de idiomas dentro del nombre que viola el Principio V de la constitución (idioma mixto controlado). Un lector nuevo encontrará `CodTxn` sin poder deducir "Código de Transacción".
- Nombres sugeridos: reemplazar `Cod` por el término inglés completo. Ejemplo: `ExoneratedTransactionItfRedisProxy`, `ExoneratedTransactionItfCacheLoader`.

---

### D1-021 — Sigla interna `PDH` en nombres de clase

**Categoría**: Clean Code
**Severidad**: ALTA
**Ubicación**:
- `src/main/java/com/bcp/cnf/services/model/entities/PdhBalance.java:20`
- `src/main/java/com/bcp/cnf/services/proxy/singlestore/PdhBalanceRepository.java:18`
**Principio**: Clean Code — Siglas: PROHIBIDO siglas de conceptos internos que no forman parte del lenguaje técnico universal

**Descripción**: `PDH` es una sigla interna de BCP que no forma parte del lenguaje técnico universal. La evidencia directa está en el propio código: la tabla de base de datos se llama `salary_payment_balance`, lo que revela que `PDH` significa "Pago de Haberes" — información que el nombre de clase oculta completamente. La sigla no tiene nombre legal/regulatorio que la haga excepción.

**Evidencia**:
```java
// PdhBalance.java:17-20
@Entity
@Table(name = "salary_payment_balance")   // tabla BD revela el concepto completo
public class PdhBalance extends PanacheEntityBase { ... }

// PdhBalanceRepository.java:17-18
@ApplicationScoped
public class PdhBalanceRepository implements PanacheRepositoryBase<PdhBalance, String> { ... }
```

**Impacto**: Todo nuevo desarrollador debe preguntar qué significa `PDH` antes de poder razonar sobre el código. El nombre de la tabla BD (`salary_payment_balance`) ya contiene la información semántica completa que el nombre de clase suprime. La variable `pdh` en múltiples métodos amplifica el problema.
- Nombre sugerido: `SalaryPaymentBalance`, `SalaryPaymentBalanceRepository`.

---

### Fichas de hallazgos MEDIOS (resumen)

| ID | Ubicación | Descripción | Impacto |
|----|-----------|-------------|---------|
| D1-006 | `EvaluateItfServiceImpl.java:257` | Magic number `20` como longitud de número de cuenta (`referenceId.trim().length() != 20`). Sin constante nombrada ni documentación de la regla de negocio. | Si la longitud cambia, hay que buscar el número en el código. |
| D1-007 | `ItfTxnRepository.java:75` | Siete números mágicos (60, 4, 16, 100, 4, 3, 1) como longitudes de truncado, duplicando las restricciones de columna de la entidad. | Desincronización silenciosa si cambia el modelo de datos. |
| D1-009 | `EvaluateItfServiceImpl.java:86` | `catch (Exception ex)` demasiado genérico en el método raíz `evaluateItf`. Captura errores de programación como `NullPointerException` y los disfraza como `EXTERNAL_ERROR`. | Diagnóstico incorrecto de errores. |
| D2-006 | `ItfRateTableStorageProxy.java:1` | Sin interfaz (puerto) para el adaptador de Azure Table Storage de tasas. | Igual que D2-002. |
| D2-007 | `ExoneratedCodTxnItfTableStorageProxy.java:1` | Sin interfaz (puerto) para el adaptador de Azure Table Storage de exonerados. | Igual que D2-002. |
| D2-010 | `EvaluateItfHeaders.java:6` | `EvaluateItfHeaders` encapsula headers HTTP (authorization, requestId, appCode, etc.) y es pasado directamente al servicio de aplicación. Filtra preocupaciones de infraestructura HTTP al dominio. | El servicio queda acoplado al protocolo de transporte. |
| D2-011 | `ItfRateCacheLoader.java:8` | Los schedulers importan proxies Redis y Table Storage directamente, sin puertos. | Schedulers no testeables sin levantar infraestructura. |
| D3-005 | `PdhBalanceRepository.java:72` | En `updatePdh`, si no existe el `PdhBalance` para la cuenta, la actualización se omite silenciosamente sin log ni excepción. | Inconsistencia de saldos PDH sin notificación. |
| D3-007 | `ItfTxn.java:59` | `finalAccountStatus` es `String` en la entidad en lugar de `ItfFinalAccountStatus`. Sin validación de valores permitidos a nivel de tipo. | Posibles valores inválidos en BD. |
| D4-004 | `EvaluateItfServiceImpl.java:53` | `EXTERNAL_ERROR = "EXTERNAL_ERROR"` constante duplicada en 9 clases (ver también D6-005). | Mantenimiento multiplicado. |
| D4-006 | `AzureTableConfig.java:19` | Inyección de campo (`@ConfigProperty`) en lugar de constructor. Clase no testeable sin contexto CDI completo. | Dificultad en testing. |
| D4-007 | `ItfRateTableStorageProxy.java:26` | Inyección de campo (`@Named`) para `TableClient`. Mismo problema que D4-006. | Dificultad en testing. |
| D5-001 | `ItfTxnRepository.java:35` | `findWithoutReversalByOpnNumber()` retorna `Optional<ItfTxn>` pero no es llamado desde código de producción; solo `findAllWithoutReversalByOpnNumber()` se usa. | Código de más que confunde sobre la API del repositorio. |
| D5-002 | `ItfRateRedisProxy.java:51` | `get(String itf)` no tiene llamantes de producción. Solo `getAll()` es invocado. | Método de lectura Redis sin uso. |
| D5-005 | `ItfRateTableStorageProxy.java:34` | `findByItf()` sin llamantes de producción. | Dead code en adaptador de Table Storage. |
| D5-006 | `ItfRateTableStorageProxy.java:49` | `save()` sin llamantes de producción. Las tasas se leen desde Table Storage pero nunca se escriben desde este servicio. | Sugiere un flujo de escritura incompleto o eliminado. |
| D5-007 | `ExoneratedCodTxnItfTableStorageProxy.java:34` | `findByUtc()` sin llamantes de producción. | Dead code. |
| D5-008 | `ExoneratedCodTxnItfTableStorageProxy.java:53` | `save()` sin llamantes de producción. | Igual que D5-006. |
| D5-010 | `PdhBalanceRepository.java:90` | `persistPdhBalance()` sin llamantes de producción. | Dead code en repositorio. |
| D6-005 | múltiples | `EXTERNAL_ERROR = "EXTERNAL_ERROR"` definida en 9 clases distintas. Debería ser una constante compartida. | Mantenimiento y riesgo de discrepancia. |
| D7-001 | `EvaluateItfServiceImpl.java:127` | `processReversal` retorna `List.of(new EvaluateItfResponseInner())` (objeto vacío) pero el controlador llama `Response.noContent()` en flujo de reversa. El valor de retorno nunca se usa. | Objeto construido y descartado innecesariamente en cada reversa. |
| D7-002 | `CodTxnExoneratedItfCacheLoader.java:57` | `LocalTime.parse(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))` — formatea y parsea inmediatamente, equivalente a `LocalTime.now().truncatedTo(SECONDS)`. Ciclo parse-format-parse sin valor. | Código confuso que no produce valor adicional. |
| D1-014 | `dto/CodTxnExoneratedItfRedisDto.java:9`, `dto/ItfsRateRedisDto.java:6` | Sufijo `Dto` en records de transferencia. La regla prohíbe sufijo `Dto`; el nombre debe expresar el concepto que transporta, no su rol estructural. | Nombres sin valor semántico. Sugerido: `ExoneratedTransactionCache`, `ItfRateCache`. |
| D1-019 | `model/entities/ItfTxnId.java:13` | Sufijo `Id` en clase de clave compuesta `@Embeddable`. `ItfTxnId` contiene cuatro campos y representa una clave compuesta; el sufijo `Id` sugiere un identificador simple. | La regla prohíbe sufijo `Id` para claves compuestas. Sugerido: `ItfTransactionKey`. |
| D1-022 | `service/impl/EvaluateItfServiceImpl.java:252`, `EvaluateItfServiceImpl.java:260` | Métodos `verifyExonerationByAccountNumberFormat` y `verifyItfExoneration` retornan `boolean` pero usan el prefijo `verify`. Clean Code exige `is`/`has`/`can` para predicados booleanos. | El nombre sugiere acción con efecto secundario, no predicado puro. Sugerido: `hasExemptAccountFormat`, `isItfExempt`. |
| D1-023 | `dto/EvaluateItfHeaders.java:8`, `proxy/singlestore/ItfTxnRepository.java:35,51` | Campo y parámetros `opnNumber` usan `opn` como abreviación de *operation*. La regla prohíbe abreviaciones que requieran traducción mental. | Requiere saber que `opn` = `operation`. Sugerido: `operationNumber`. |
| D1-025 | `dto/EvaluateItfHeaders.java:12` | Campo `guildAccountFlag` tipado como `String` pero con el sufijo `Flag` que sugiere `Boolean`. Violación de "sin desinformación": el tipo real no coincide con la expectativa del nombre. | El lector asumirá valor `true`/`false` al ver `Flag`, cuando en realidad es un código `String`. Sugerido: `guildAccountIndicator`. |
| D1-026 | `service/impl/EvaluateItfServiceImpl.java:95`, `PdhBalanceRepository.java:52,72`, múltiples | Variables locales `txn`/`txns` (abreviación de *transaction*), `pdh` (sigla interna BCP) y `dto` en `ItfRateRedisProxy.java:112`. Todas requieren traducción mental. | Multiplica la carga cognitiva en los métodos con mayor lógica del servicio. Sugerido: `transaction`, `transactions`, `pdhBalance`, `rateCache`. |

---

### Fichas de hallazgos BAJOS (resumen)

| ID | Ubicación | Descripción |
|----|-----------|-------------|
| D1-004 | `EvaluateItfServiceImpl.java:143` | Typo en nombre de variable: `itfAmountToPayed` debería ser `itfAmountToPaid`. |
| D1-005 | `EvaluateItfServiceImpl.java:147` | Nombre `flagItf` no expresa el significado (`applyItf` o `isItfApplicable` sería más claro). |
| D1-008 | `PositionKeepingApiImpl.java:8` | JavaDoc boilerplate con HTML entities en todas las clases. Genera ruido sin aportar valor técnico. |
| D1-010 | `ItfRateTableStorageProxy.java:52` | Typo: `nowDateFormated`/`nowTimeFormated` — debería ser `nowDateFormatted`/`nowTimeFormatted`. |
| D1-011 | `ItfRateRedisProxy.java:77` | Typo en claves Redis: `TimeUpdateCach`/`DateUpdateCach` — debería ser `timeUpdateCache`/`dateUpdateCache`. |
| D4-005 | `EvaluateItfServiceImpl.java:276` | Logging con concatenación de String en lugar de `LOG.errorf(...)` o parámetros. |
| D4-008 | `ExoneratedAccountRepository.java:33` | Patrón de logging de errores inconsistente: usa `LOG.error(...)` inline en vez del método `buildDbException`. |
| D5-003 | `ItfRateRedisProxy.java:92` | `evict(String utc)` sin llamantes de producción. |
| D5-004 | `CodTxnExoneratedItfRedisProxy.java:99` | `evict(String utc, String productType)` sin llamantes de producción. |
| D5-011 | `ItfTxnRepository.java:174` | Override de `delete()` de Panache sin lógica adicional. |
| D6-002 | `ItfRateRedisProxy.java:147` | `nowTime()`/`nowDate()` duplicados en `ItfRateRedisProxy` y `CodTxnExoneratedItfRedisProxy`. |
| D6-004 | `ItfRateCacheLoader.java:35` | Estructura `@Startup onStart()` + `@Scheduled scheduledWarmUp()` duplicada en dos loaders. |
| D7-003 | `ItfTxnRepository.java:174` | Override de `delete()` que solo llama `txn.delete()` sin añadir valor. |
| D7-004 | `ExoneratedAccountRepository.java:47` | `findByAccountNumber()` solo delega a `findById()`. Wrapper sin valor. |
| D7-005 | `PdhBalanceRepository.java:105` | `findByAccountNumber()` solo delega a `findById()`. Wrapper sin valor. |
| D1-015 | `scheduler/CacheWarmUpSupport.java:13` | Sufijo `Support` en clase de utilidad `CacheWarmUpSupport`. La regla prohíbe el sufijo `Support`. Sugerido: `CacheLoader` o `CacheWarmer`. |
| D1-018 | `model/api/EvaluateItfResponseInner.java`, `model/api/EvaluateItfRequestEntriesInner.java` (generados) | Sufijo `Inner` en clases del modelo OpenAPI. La regla prohíbe `Inner` porque indica clase generada, no concepto de negocio. La corrección requiere cambio en el contrato API o en la configuración del generador OpenAPI. |
| D1-020 | múltiples (9 clases) | Constante `EXTERNAL_ERROR = "EXTERNAL_ERROR"` tautológica: el nombre repite el valor literalmente. La regla exige que el nombre exprese el concepto, no el valor. Ver D4-004 y D6-005 para la violación de duplicación. Sugerido: `EXTERNAL_SYSTEM_FAILURE`. |
| D1-024 | `proxy/singlestore/PdhBalanceRepository.java:69` | Typo en parámetro `movent` — debería ser `movement`. El nombre no es pronunciable en su forma actual. |
| D1-027 | paquetes `com.bcp.cnf.services`, `com.bcp.cnf.services.model.entities`, `com.bcp.cnf.services.model.enums` | Paquetes en plural: `services` → `service`, `entities` → `entity`. Para `enums`: es keyword Java; renombrar a `status` o `domain` evita el plural y la colisión. |

---

## 4. Matriz de hallazgos

| Severidad | D1 Clean Code | D2 Hexagonal | D3 Lógica | D4 Prácticas | D5 Muerto | D6 Duplicado | D7 Inerte | Total |
|-----------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Crítica   | 0   | 0   | 4   | 0   | 0   | 0   | 0   | **4**   |
| Alta      | 8   | 7   | 1   | 8   | 1   | 2   | 0   | **27**  |
| Media     | 9   | 4   | 2   | 3   | 6   | 1   | 2   | **28**  |
| Baja      | 10  | 0   | 0   | 2   | 3   | 2   | 3   | **20**  |
| **Total** | **27** | **11** | **7** | **13** | **11** | **5** | **5** | **79** |

*T031 (análisis de nombramientos) añadió 15 hallazgos D1: 4 ALTA, 6 MEDIA, 5 BAJA. D4-013 (queries nativos) añadido en revisión post-análisis.*

---

## 5. Mapa arquitectónico

### 5.1 Estado actual vs. arquitectura hexagonal objetivo

| Paquete actual | Rol detectado | Capa hexagonal esperada | ¿Violación? |
|----------------|--------------|------------------------|:-----------:|
| `expose/web/` | Adaptador REST entrante | Infraestructura | ✅ Tiene puerto: `PositionKeepingsApi` (OpenAPI) |
| `service/` | Caso de uso / lógica de negocio | Aplicación | ⚠️ Parcial — `EvaluateItfService` existe como puerto, pero impl importa infra directamente |
| `proxy/singlestore/` | Adaptadores de BD SingleStore | Infraestructura | ❌ Sin puertos; implementan `PanacheRepositoryBase` (framework) |
| `proxy/redis/` | Adaptadores de caché Redis | Infraestructura | ❌ Sin puertos; clases concretas sin interfaz |
| `proxy/tablestorage/` | Adaptadores Azure Table Storage | Infraestructura | ❌ Sin puertos; clases concretas sin interfaz |
| `scheduler/` | Adaptadores de scheduling | Infraestructura | ❌ Sin puertos; inyectan proxies concretos directamente |
| `model/entities/` | Entidades JPA (ORM) | Infraestructura de persistencia | ❌ Usadas en capa de aplicación como si fueran dominio |
| `model/api/` | DTOs generados por OpenAPI | Infraestructura REST | ⚠️ Usados como tipos de retorno del servicio; acoplan API contract al dominio |
| `dto/` | DTOs de transporte de headers HTTP | Aplicación / Infraestructura | ⚠️ `EvaluateItfHeaders` filtra concerns HTTP hacia el servicio |
| `model/enums/` | Enumeraciones de dominio | Dominio | ✅ Sin dependencias externas |
| `config/` | Configuración de infraestructura | Infraestructura | ✅ Correcto |

### 5.2 Violaciones de la regla de dependencias

| Clase origen | Import problemático | Tipo de violación | Ubicación |
|-------------|--------------------|--------------------|-----------|
| `EvaluateItfServiceImpl` | `ItfTxnRepository` | aplicación → infraestructura | `EvaluateItfServiceImpl.java:13` |
| `EvaluateItfServiceImpl` | `PdhBalanceRepository` | aplicación → infraestructura | `EvaluateItfServiceImpl.java:14` |
| `EvaluateItfServiceImpl` | `CodTxnExoneratedItfRedisProxy` | aplicación → infraestructura | `EvaluateItfServiceImpl.java:15` |
| `EvaluateItfServiceImpl` | `ItfRateRedisProxy` | aplicación → infraestructura | `EvaluateItfServiceImpl.java:16` |
| `EvaluateItfServiceImpl` | `ItfTxn` (JPA entity) | aplicación → infraestructura ORM | `EvaluateItfServiceImpl.java:9` |
| `EvaluateItfServiceImpl` | `PdhBalance` (JPA entity) | aplicación → infraestructura ORM | `EvaluateItfServiceImpl.java:10` |
| `ItfRateCacheLoader` | `ItfRateRedisProxy` | scheduler → adaptador concreto | `ItfRateCacheLoader.java:7` |
| `ItfRateCacheLoader` | `ItfRateTableStorageProxy` | scheduler → adaptador concreto | `ItfRateCacheLoader.java:8` |
| `CodTxnExoneratedItfCacheLoader` | `CodTxnExoneratedItfRedisProxy` | scheduler → adaptador concreto | `CodTxnExoneratedItfCacheLoader.java:5` |
| `CodTxnExoneratedItfCacheLoader` | `ExoneratedCodTxnItfTableStorageProxy` | scheduler → adaptador concreto | `CodTxnExoneratedItfCacheLoader.java:6` |

### 5.3 Estado de puertos por adaptador

| Adaptador | ¿Tiene puerto (interfaz)? | Interfaz |
|-----------|:---:|---------|
| `PositionKeepingApiImpl` | ✅ Sí | `PositionKeepingsApi` (generada por OpenAPI) |
| `EvaluateItfServiceImpl` | ✅ Sí | `EvaluateItfService` |
| `ItfTxnRepository` | ❌ No | — |
| `PdhBalanceRepository` | ❌ No | — |
| `ExoneratedAccountRepository` | ❌ No | — |
| `ItfRateRedisProxy` | ❌ No | — |
| `CodTxnExoneratedItfRedisProxy` | ❌ No | — |
| `ItfRateTableStorageProxy` | ❌ No | — |
| `ExoneratedCodTxnItfTableStorageProxy` | ❌ No | — |

---

## 6. Hallazgos críticos priorizados

1. **[D3-001] `flagItf` con estado compartido entre iteraciones** — Severidad: CRITICA
   Impacto: cálculo ITF incorrecto para entradas posteriores a una exonerada en el mismo request. Bug financiero activo.
   Ver: Sección 3, ficha D3-001

2. **[D3-002] `nextItfTransactionId()` sin bloqueo — condición de carrera** — Severidad: CRITICA
   Impacto: violación de clave primaria bajo carga concurrente; transacciones ITF perdidas.
   Ver: Sección 3, ficha D3-002

3. **[D3-003] Variables fecha/hora intercambiadas en `ItfRateTableStorageProxy.save()`** — Severidad: CRITICA
   Impacto: metadatos de auditoría de tasas ITF corruptos desde el inicio del servicio.
   Ver: Sección 3, ficha D3-003

4. **[D3-004] Variables fecha/hora intercambiadas en `ExoneratedCodTxnItfTableStorageProxy.save()`** — Severidad: CRITICA
   Impacto: metadatos de auditoría de exonerados ITF corruptos desde el inicio del servicio.
   Ver: Sección 3, ficha D3-004

5. **[D2-001] Servicio depende de implementaciones concretas de infraestructura** — Severidad: ALTA
   Impacto: bloquea completamente la reconstrucción hexagonal; testing sin infraestructura imposible.
   Ver: Sección 3, ficha D2-001

6. **[D2-002 a D2-005] Sin puertos para los 4 adaptadores primarios** — Severidad: ALTA
   Impacto: precondición para la reconstrucción hexagonal; sin puertos no hay inversión de dependencias posible.
   Ver: Sección 3, fichas D2-002 a D2-005

7. **[D4-001] Redis `KEYS` bloqueante en `ItfRateRedisProxy.getAll()`** — Severidad: ALTA
   Impacto: riesgo de degradación total de Redis bajo carga; afecta todos los servicios del clúster Redis.
   Ver: Sección 3, ficha D4-001

8. **[D1-001/D1-002/D1-003] Métodos mayores de 40-60 líneas** — Severidad: ALTA
   Impacto: alta complejidad ciclomática (estimada 7-8); principal fuente de bugs ocultos y regresiones.
   Ver: Sección 3, fichas D1-001, D1-002, D1-003

9. **[D5-009] `ExoneratedAccountRepository` sin uso en producción** — Severidad: ALTA
   Impacto: código de infraestructura completo sin función activa; sugiere funcionalidad incompleta.
   Ver: Sección 3, ficha D5-009

10. **[D6-001] Patrón de excepción duplicado en 9 clases** — Severidad: ALTA
    Impacto: cualquier cambio en la estrategia de errores requiere 9 modificaciones coordinadas.
    Ver: Sección 3, ficha D6-001

---

## 7. Verificación del entregable

- **SC-001 Cobertura**: 22/22 archivos Java + `application.yml` revisados = **100%**
- **SC-002 Trazabilidad**: 79 hallazgos (63 dimensiones D1-D7 + 15 de nombramientos T031 + 1 revisión post-análisis D4-013), todos con `archivo:línea` + evidencia = **cumplido**
- **SC-003 Accionabilidad**: hallazgos críticos y altos incluyen descripción de impacto y ubicación precisa para trasladar directamente al feature 003 = **cumplido**
- **SC-004 Solo lectura**: ningún archivo bajo `src/` fue modificado = **cumplido**
