# Recomendaciones de Mejora: business-position-keeping (ITF)

**Fecha**: 2026-06-24
**Basado en**: `informe-diagnostico.md` — 79 hallazgos
**Alcance**: Correcciones concretas con código antes/después para cada hallazgo

---

## Cómo leer este documento

Cada recomendación sigue esta estructura:

- **Hallazgo del informe**: ID del hallazgo (ej. D3-001)
- **Clase**: Clase afectada
- **Línea(s)**: Línea o bloque de líneas en el código actual
- **Recomendación**: Qué cambiar y por qué
- **Beneficio**: Qué mejora concreta produce el cambio
- **Antes / Después**: Código real del servicio vs. código corregido

## Secuencia de implementación recomendada

El orden de las secciones no es arbitrario:

1. **D3 — Errores de lógica**: bugs activos en producción, independientes del naming
2. **D1 — Nombramientos**: renombrar todo primero, para que el resto de cambios ya use la nomenclatura correcta
3. **D4 — Malas prácticas**: con nombres correctos
4. **D5 — Código muerto**: eliminar antes de refactorizar
5. **D6 — Código duplicado**: centralizar con nombres correctos
6. **D7 — Código inerte**: simplificaciones puntuales
7. **D2 — Arquitectura Hexagonal**: último, porque es el cambio estructural mayor y se beneficia de todo lo anterior

---

## D3 — Errores de Lógica ⚠️ PRIORIDAD MÁXIMA

> Estos hallazgos son bugs activos en producción. Deben resolverse antes que cualquier otro cambio.

---

### D3-001 — `flagItf` con estado compartido entre iteraciones

**Hallazgo del informe**: D3-001
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 143–198

**Recomendación**: Mover la declaración de `flagItf` al interior del bucle `for`, de modo que cada entrada del request comience con un indicador limpio e independiente.

**Beneficio**: Cada entrada ITF se evalúa de forma aislada. Una entrada exonerada no contamina el indicador de las entradas siguientes. Elimina el bug de cálculo financiero activo.

**Antes**:
```java
// EvaluateItfServiceImpl.java:143–198
boolean flagItf = true;  // ← inicializado UNA VEZ, fuera del bucle

for (var entry : request.getEntries()) {
    if (verifyExonerationByAccountNumberFormat(...)
        || verifyItfExoneration(...)
        || isOwnAccountTransfer(request)) {
      itfAmountToPayed = BigDecimal.ZERO;
      flagItf = false;  // ← se pone false y NUNCA vuelve a true
    }
    response.add(new EvaluateItfResponseInner()
        .applyItfIndicator(flagItf));  // ← estado de iteraciones anteriores
}
```

**Después**:
```java
for (var entry : request.getEntries()) {
    boolean isItfApplicable = true;  // ← reiniciado en cada iteración

    if (verifyExonerationByAccountNumberFormat(...)
        || verifyItfExoneration(...)
        || isOwnAccountTransfer(request)) {
      itfAmountToPaid = BigDecimal.ZERO;
      isItfApplicable = false;
    }
    response.add(new EvaluateItfResponseInner()
        .applyItfIndicator(isItfApplicable));  // ← estado propio de esta entrada
}
```

---

### D3-002 — `nextItfTransactionId()` con condición de carrera

**Hallazgo del informe**: D3-002
**Clase**: `ItfTxnRepository`
**Línea(s)**: 124–133

**Recomendación**: Reemplazar `MAX(id) + 1` por `AUTO_INCREMENT` en el DDL de SingleStore. Eliminar la generación manual del ID en Java.

**Beneficio**: La BD garantiza unicidad atómica. Cero riesgo de violación de clave primaria bajo concurrencia.

**Antes**:
```java
// ItfTxnRepository.java:124–133
private Long nextItfTransactionId() {
    Number maxId = (Number) getEntityManager()
        .createNativeQuery(
            "SELECT COALESCE(MAX(itf_transaction_id), 0) FROM itf_transaction")
        .getSingleResult();
    return maxId.longValue() + 1;  // ← race condition
}
```

**Después** — opción A (DDL, recomendada):
```sql
ALTER TABLE itf_transaction
  MODIFY COLUMN itf_transaction_id BIGINT NOT NULL AUTO_INCREMENT;
```
```java
// Eliminar nextItfTransactionId() y el parámetro ?1 del INSERT
```

**Después** — opción B (bloqueo pesimista, si no es posible el DDL):
```java
private Long nextItfTransactionId() {
    Number maxId = (Number) getEntityManager()
        .createNativeQuery(
            "SELECT COALESCE(MAX(itf_transaction_id), 0) "
            + "FROM itf_transaction FOR UPDATE")
        .getSingleResult();
    return maxId.longValue() + 1;
    // Requiere @Transactional activo en el método que llama a este.
}
```

---

### D3-003 — Variables fecha/hora intercambiadas en `ItfRateTableStorageProxy`

**Hallazgo del informe**: D3-003
**Clase**: `ItfRateTableStorageProxy`
**Línea(s)**: 52–59

**Recomendación**: Corregir el formateo: `nowDate` usa `ISO_DATE`, `nowTime` usa `HH:mm:ss`. Renombrar las variables para que el nombre coincida con el contenido.

**Beneficio**: Los metadatos de auditoría en Azure Table Storage serán correctos. Elimina la corrupción de datos desde el inicio del servicio.

**Antes**:
```java
// ItfRateTableStorageProxy.java:52–59
var nowDateFormated = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("HH:mm:ss")); // ← guarda HORA
var nowTimeFormated = LocalDate.now()
    .format(DateTimeFormatter.ISO_DATE);               // ← guarda FECHA

entity.getProperties().put("timeCreated", nowTimeFormated); // FECHA en HORA
entity.getProperties().put("dateCreate",  nowDateFormated); // HORA en FECHA
```

**Después**:
```java
var nowDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);   // "2026-06-24"
var nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); // "14:30:00"

entity.getProperties().put("timeCreated", nowTime);  // HORA en HORA ✓
entity.getProperties().put("dateCreate",  nowDate);  // FECHA en FECHA ✓
entity.getProperties().put("timeUpdate",  nowTime);
entity.getProperties().put("dateUpdate",  nowDate);
```

---

### D3-004 — Variables fecha/hora intercambiadas en `ExoneratedCodTxnItfTableStorageProxy`

**Hallazgo del informe**: D3-004
**Clase**: `ExoneratedCodTxnItfTableStorageProxy`
**Línea(s)**: 64–71

**Recomendación**: Aplicar la misma corrección que D3-003. El bloque fue copiado de `ItfRateTableStorageProxy` propagando el bug.

**Antes**:
```java
var nowDateFormated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
var nowTimeFormated = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
entity.getProperties().put("timeCreated", nowTimeFormated);
entity.getProperties().put("dateCreate",  nowDateFormated);
```

**Después**:
```java
var nowDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
var nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
entity.getProperties().put("timeCreated", nowTime);
entity.getProperties().put("dateCreate",  nowDate);
entity.getProperties().put("timeUpdate",  nowTime);
entity.getProperties().put("dateUpdate",  nowDate);
```

---

### D3-005 — Actualización PDH silenciosa cuando no existe el registro

**Hallazgo del informe**: D3-005
**Clase**: `PdhBalanceRepository`
**Línea(s)**: 69–83

**Recomendación**: Registrar advertencia explícita cuando `findByAccountNumber` devuelve `null` en `updatePdh`, en lugar de omitir silenciosamente la actualización.

**Beneficio**: Visibilidad inmediata de inconsistencias de saldo en logs de producción.

**Antes**:
```java
public void updatePdh(String accountNumber, String utc,
                      BigDecimal movent, Boolean reversalFlag) {
    PdhBalance pdh = findByAccountNumber(accountNumber);
    if (pdh != null) {
        // actualización...
    }
    // ← si null, silencio total
}
```

**Después**:
```java
public void updatePdh(String accountNumber, String utc,
                      BigDecimal movement, Boolean reversalFlag) {
    SalaryPaymentBalance balance = findByAccountNumber(accountNumber);
    if (balance == null) {
        LOG.warnf("[SalaryPaymentBalanceRepository - updatePdh] "
            + "No balance found for account %s — update skipped", accountNumber);
        return;
    }
    balance.utc = utc;
    balance.previousBalance = balance.accountBalanceClosing;
    balance.accountBalanceClosing = balance.accountBalanceClosing.add(movement);
    balance.reversalFlag = reversalFlag;
}
```

---

### D3-006 — NPE potencial en `processReversal` por `getMeanInformation()` sin null-check

**Hallazgo del informe**: D3-006
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 104–106

**Recomendación**: Verificar que `getMeanInformation()` no sea `null` antes de encadenar `getReferenceId()`.

**Beneficio**: Requests malformados producen un log claro y `continue`, no un `NullPointerException` disfrazado de `EXTERNAL_ERROR`.

**Antes**:
```java
for (var entry : request.getEntries()) {
    String accountNumber = entry.getMeanInformation().getReferenceId(); // ← NPE si null
```

**Después**:
```java
for (var entry : request.getEntries()) {
    if (entry.getMeanInformation() == null
        || entry.getMeanInformation().getReferenceId() == null) {
        LOG.warnf("[ItfComplianceEvaluator - processReversal] "
            + "Entry missing meanInformation — skipping");
        continue;
    }
    String accountNumber = entry.getMeanInformation().getReferenceId();
```

---

### D3-007 — `finalAccountStatus` como `String` en lugar de enum

**Hallazgo del informe**: D3-007
**Clase**: `ItfTxn` (entidad)
**Línea(s)**: 59

**Recomendación**: Cambiar el tipo de `String` a `ItfFinalAccountStatus` con `@Enumerated(EnumType.STRING)`.

**Beneficio**: La BD solo puede recibir valores válidos. El compilador detecta asignaciones incorrectas.

**Antes**:
```java
public String finalAccountStatus;

// uso: var finalAccountStatus = ItfFinalAccountStatus.CALCULATED.name();
```

**Después**:
```java
@Enumerated(EnumType.STRING)
public ItfFinalAccountStatus finalAccountStatus;

// uso: transaction.finalAccountStatus = ItfFinalAccountStatus.CALCULATED;
```

---

## D1 — Nombramientos

> Aplicar estos renombramientos antes de cualquier otra refactorización, para que el código siguiente ya use la nomenclatura correcta.

---

### D1-004 — Typo `itfAmountToPayed`

**Hallazgo del informe**: D1-004
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 143

**Recomendación**: Corregir la conjugación: en inglés es *paid*, no *payed*.

**Antes**: `BigDecimal itfAmountToPayed;`
**Después**: `BigDecimal itfAmountToPaid;`

---

### D1-005 — Nombre `flagItf` no expresa intención

**Hallazgo del informe**: D1-005
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 146

**Recomendación**: Los booleanos deben tener prefijo `is`/`has`/`can`.

**Antes**: `boolean flagItf = true;`
**Después**: `boolean isItfApplicable = true;`

---

### D1-010 — Typo `nowDateFormated` / `nowTimeFormated`

**Hallazgo del informe**: D1-010
**Clase**: `ItfRateTableStorageProxy`
**Línea(s)**: 52–53

**Recomendación**: Corregir el typo de `Formated` a `Formatted`. Cubierto en la corrección de D3-003 — los nombres quedan como `nowDate`/`nowTime`.

---

### D1-011 — Typo en claves Redis `TimeUpdateCach` / `DateUpdateCach`

**Hallazgo del informe**: D1-011
**Clase**: `ItfRateRedisProxy`
**Línea(s)**: 77–78

**Recomendación**: Corregir `Cach` → `Cache` y normalizar a `camelCase`.

**Antes**:
```java
hash.hset(key, Map.of(
    "itfRate",        dto.itfRate(),
    "TimeUpdateCach", nowTime(),
    "DateUpdateCach", nowDate()
));
```

**Después**:
```java
hash.hset(key, Map.of(
    "itfRate",          dto.itfRate(),
    "timeUpdatedCache", nowTime(),
    "dateUpdatedCache", nowDate()
));
```

---

### D1-013 — Sufijo `Impl` en clases de implementación

**Hallazgo del informe**: D1-013
**Clase**: `EvaluateItfServiceImpl`, `PositionKeepingApiImpl`
**Línea(s)**: 43, 34

**Recomendación**: Renombrar para expresar qué hace la clase, no que es una implementación.

**Antes**:
```java
public class EvaluateItfServiceImpl implements EvaluateItfService { ... }
public class PositionKeepingApiImpl implements PositionKeepingsApi { ... }
```

**Después**:
```java
public class ItfComplianceEvaluator implements EvaluateItfService { ... }
public class PositionKeepingResource implements PositionKeepingsApi { ... }
```

---

### D1-014 — Sufijo `Dto` en records de transferencia

**Hallazgo del informe**: D1-014
**Clase**: `CodTxnExoneratedItfRedisDto`, `ItfsRateRedisDto`
**Línea(s)**: 9, 6

**Antes**:
```java
public record CodTxnExoneratedItfRedisDto(...) { }
public record ItfsRateRedisDto(String itfRate, ...) { }
```

**Después**:
```java
public record ExoneratedTransactionCache(...) { }
public record ItfRateCache(String itfRate, String timeUpdatedCache, String dateUpdatedCache) { }
```

---

### D1-015 — Sufijo `Support` en `CacheWarmUpSupport`

**Hallazgo del informe**: D1-015
**Clase**: `CacheWarmUpSupport`
**Línea(s)**: 13

**Antes**: `public class CacheWarmUpSupport { ... }`
**Después**: `public class CacheWarmer { ... }`

---

### D1-016 — Abreviación `Txn` en nombres de clase

**Hallazgo del informe**: D1-016
**Clases**: `ItfTxn`, `ItfTxnId` y 4 más

**Recomendación**: Reemplazar `Txn` por `Transaction`.

| Nombre actual | Nombre sugerido |
|---|---|
| `ItfTxn` | `ItfTransaction` |
| `ItfTxnId` | `ItfTransactionKey` |
| `CodTxnExoneratedItfRedisDto` | `ExoneratedTransactionCache` |
| `CodTxnExoneratedItfRedisProxy` | `ExoneratedTransactionRedisProxy` |
| `ExoneratedCodTxnItfTableStorageProxy` | `ExoneratedTransactionTableStorageProxy` |
| `CodTxnExoneratedItfCacheLoader` | `ExoneratedTransactionCacheLoader` |

---

### D1-017 — Abreviación `Cod` (español) en nombres de clase

**Hallazgo del informe**: D1-017
**Clases**: 4 clases con prefijo `Cod`

**Recomendación**: `Cod` = "código" en español — no pertenece a código en inglés.

**Antes**: `CodTxnExoneratedItfRedisProxy`
**Después**: `ExoneratedTransactionItfRedisProxy`

---

### D1-018 — Sufijo `Inner` en clases generadas por OpenAPI

**Hallazgo del informe**: D1-018
**Clase**: `EvaluateItfResponseInner`, `EvaluateItfRequestEntriesInner`

**Recomendación**: Cambiar el contrato OpenAPI para usar `$ref` con nombres explícitos.

**Antes** (spec OpenAPI):
```yaml
entries:
  type: array
  items:
    type: object   # ← genera EvaluateItfRequestEntriesInner
```

**Después**:
```yaml
entries:
  type: array
  items:
    $ref: '#/components/schemas/ItfEntry'

components:
  schemas:
    ItfEntry:
      type: object
      properties: ...
    ItfEvaluationResult:
      type: object
      properties: ...
```

---

### D1-019 — Sufijo `Id` en clave compuesta `ItfTxnId`

**Hallazgo del informe**: D1-019
**Clase**: `ItfTxnId`
**Línea(s)**: 13

**Antes**: `public class ItfTxnId implements Serializable { ... }`
**Después**: `public class ItfTransactionKey implements Serializable { ... }`

---

### D1-020 — Constante tautológica `EXTERNAL_ERROR = "EXTERNAL_ERROR"`

**Hallazgo del informe**: D1-020
**Clases**: 9 clases

**Recomendación**: Renombrar y centralizar (ver D6-005).

**Antes**: `private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";`
**Después** (centralizada):
```java
public final class InfrastructureErrorCodes {
    public static final String EXTERNAL_SYSTEM_FAILURE = "EXTERNAL_ERROR";
    private InfrastructureErrorCodes() {}
}
```

---

### D1-021 — Sigla interna `PDH` en `PdhBalance` / `PdhBalanceRepository`

**Hallazgo del informe**: D1-021
**Clase**: `PdhBalance`, `PdhBalanceRepository`
**Línea(s)**: 20, 18

**Recomendación**: La tabla BD ya revela el concepto: `salary_payment_balance`. Usar ese nombre.

**Antes**:
```java
@Table(name = "salary_payment_balance")
public class PdhBalance extends PanacheEntityBase { ... }
public class PdhBalanceRepository implements PanacheRepositoryBase<PdhBalance, String> { ... }
```

**Después**:
```java
@Table(name = "salary_payment_balance")
public class SalaryPaymentBalance extends PanacheEntityBase { ... }
public class SalaryPaymentBalanceRepository
    implements PanacheRepositoryBase<SalaryPaymentBalance, String> { ... }
```

---

### D1-022 — Prefijo `verify` en métodos predicado booleanos

**Hallazgo del informe**: D1-022
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 252, 260

**Antes**:
```java
private boolean verifyExonerationByAccountNumberFormat(String referenceId) { ... }
private boolean verifyItfExoneration(String utc, String productType) { ... }
```

**Después**:
```java
private boolean hasExemptAccountFormat(String referenceId) { ... }
private boolean isItfExempt(String utc, String productType) { ... }
```

---

### D1-023 — Abreviación `opnNumber`

**Hallazgo del informe**: D1-023
**Clase**: `EvaluateItfHeaders`, `ItfTxnRepository`
**Línea(s)**: 8, 35, 51

**Antes**: `String opnNumber` / `headers.opnNumber()`
**Después**: `String operationNumber` / `headers.operationNumber()`

---

### D1-024 — Typo `movent` en parámetro de método

**Hallazgo del informe**: D1-024
**Clase**: `PdhBalanceRepository`
**Línea(s)**: 69

**Antes**: `public void updatePdh(String accountNumber, String utc, BigDecimal movent, ...)`
**Después**: `public void updatePdh(String accountNumber, String utc, BigDecimal movement, ...)`

---

### D1-025 — `guildAccountFlag` tipado como `String` pese a sugerir `Boolean`

**Hallazgo del informe**: D1-025
**Clase**: `EvaluateItfHeaders`
**Línea(s)**: 12

**Antes**: `String guildAccountFlag`
**Después**: `String guildAccountIndicator`

---

### D1-026 — Variables locales `txn`, `pdh`, `dto` con abreviaciones

**Hallazgo del informe**: D1-026
**Clase**: `EvaluateItfServiceImpl`, `PdhBalanceRepository`, `ItfRateRedisProxy`

**Antes**:
```java
List<ItfTxn> txns = ...;
PdhBalance pdh = ...;
ItfsRateRedisDto dto = fromMap(data);
```

**Después**:
```java
List<ItfTransaction> transactions = ...;
SalaryPaymentBalance salaryBalance = ...;
ItfRateCache rateCache = fromMap(data);
```

---

### D1-027 — Paquetes en plural

**Hallazgo del informe**: D1-027
**Paquetes**: `com.bcp.cnf.services`, `model.entities`, `model.enums`

| Paquete actual | Paquete sugerido |
|---|---|
| `com.bcp.cnf.services` | `com.bcp.cnf.service` |
| `model.entities` | `model.entity` |
| `model.enums` | `model.status` (evita la keyword `enum`) |

---

### D1-001 — `processItfCalculation` supera 60 líneas

**Hallazgo del informe**: D1-001
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 138–204

**Recomendación**: Extraer cada responsabilidad interna en un método privado con nombre descriptivo.

**Beneficio**: Complejidad ciclomática por método baja de 8 a ≤ 3. Cada paso es testeable de forma aislada.

**Antes**: método de ~65 líneas con 8 ramas que mezcla cálculo, PDH, exoneración, persistencia y respuesta.

**Después**:
```java
private List<EvaluateItfResponseInner> processItfCalculation(
        EvaluateItfHeaders headers, EvaluateItfRequest request) {
    BigDecimal itfRate = getItfRate();
    List<EvaluateItfResponseInner> response = new ArrayList<>();

    for (var entry : request.getEntries()) {
        BigDecimal taxFreeAmount    = calculateTaxFreeAmount(entry);
        applyPdhMovement(headers, entry, taxFreeAmount);
        BigDecimal itfAmount        = calculateItfAmount(entry, taxFreeAmount, itfRate);
        boolean isItfApplicable     = !isExonerated(entry, request);
        if (!isItfApplicable) itfAmount = BigDecimal.ZERO;
        BigDecimal closingBalance   = obtainPdhBalance(entry.getMeanInformation().getReferenceId());
        itfTransactionPort.save(buildRecord(headers, entry, itfAmount, taxFreeAmount, closingBalance));
        response.add(buildResponse(entry, itfRate, itfAmount, closingBalance, isItfApplicable));
    }
    return response;
}

private BigDecimal calculateTaxFreeAmount(EvaluateItfRequestEntriesInner entry) {
    if (!EntryTypeEnum.CARGO.equals(entry.getEntryType())) return BigDecimal.ZERO;
    BigDecimal pdhBalance = obtainPdhBalance(entry.getMeanInformation().getReferenceId());
    return pdhBalance.compareTo(entry.getAmount()) >= 0 ? entry.getAmount() : pdhBalance;
}

private BigDecimal calculateItfAmount(EvaluateItfRequestEntriesInner entry,
                                      BigDecimal taxFreeAmount, BigDecimal itfRate) {
    BigDecimal taxableBase = EntryTypeEnum.CARGO.equals(entry.getEntryType())
        ? entry.getAmount().subtract(taxFreeAmount)
        : entry.getAmount();
    return taxableBase.divide(MIN_AMOUNT, 0, RoundingMode.DOWN)
        .multiply(MIN_AMOUNT).multiply(itfRate);
}

private boolean isExonerated(EvaluateItfRequestEntriesInner entry, EvaluateItfRequest request) {
    String referenceId  = entry.getMeanInformation().getReferenceId();
    String productType  = entry.getMeanInformation().getProductDetail().getProduct().getCode();
    return hasExemptAccountFormat(referenceId)
        || isItfExempt(entry.getUtc(), productType)
        || isOwnAccountTransfer(request);
}
```

---

### D1-002 — `processReversal` supera 40 líneas

**Hallazgo del informe**: D1-002
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 91–132

**Recomendación**: Extraer la construcción del mapa de transacciones y el procesamiento de cada entrada en métodos privados.

**Antes**: método de ~40 líneas con 2 bucles anidados.

**Después**:
```java
private List<EvaluateItfResponseInner> processReversal(
        EvaluateItfHeaders headers, EvaluateItfRequest request) {
    Map<String, ItfTransaction> transactionsByKey = buildTransactionIndex(
        itfTransactionPort.findActiveByOperationNumber(headers.operationNumber()));
    request.getEntries().forEach(entry -> reverseEntry(entry, transactionsByKey));
    return List.of();
}

private Map<String, ItfTransaction> buildTransactionIndex(List<ItfTransaction> transactions) {
    Map<String, ItfTransaction> index = new HashMap<>();
    for (var transaction : transactions) {
        if (transaction != null && transaction.key() != null) {
            index.put(buildReversalEntryKey(
                transaction.key().accountNumber(), transaction.utc()), transaction);
        }
    }
    return index;
}

private void reverseEntry(EvaluateItfRequestEntriesInner entry,
                          Map<String, ItfTransaction> transactionsByKey) {
    String accountNumber = entry.getMeanInformation().getReferenceId();
    var transaction = transactionsByKey.remove(
        buildReversalEntryKey(accountNumber, entry.getUtc()));
    if (transaction == null) return;
    restorePdhBalance(accountNumber, entry.getUtc(), transaction);
    itfTransactionPort.markAsReversed(transaction.key());
}
```

---

### D1-003 — `saveItfTxn` con lógica de negocio embebida en el repositorio

**Hallazgo del informe**: D1-003
**Clase**: `ItfTxnRepository`
**Línea(s)**: 69–122

**Recomendación**: Mover la lógica de construcción de la transacción a un value object `ItfTransactionRecord`. El repositorio solo persiste.

**Beneficio**: El repositorio tiene una sola responsabilidad. Los truncados manuales (D1-007) desaparecen al usar el modelo JPA.

**Antes**: `saveItfTxn` recibe `Object entryObj`, hace cast, trunca 7 campos, construye 17 variables y ejecuta INSERT nativo.

**Después**:
```java
// ItfTransactionRecord.java — encapsula la construcción
public record ItfTransactionRecord(
    String accountNumber, String appCode, String operationNumber,
    String productType, String currencyCode, String operationType,
    String utc, BigDecimal transactionAmount, BigDecimal previousBalance,
    BigDecimal itfAmount, BigDecimal taxFreeAmount,
    LocalTime movementTime, LocalDate movementDate, ItfFinalAccountStatus status
) {
    public static ItfTransactionRecord from(EvaluateItfHeaders headers,
                                            EvaluateItfRequestEntriesInner entry,
                                            BigDecimal itfAmount, BigDecimal taxFreeAmount,
                                            BigDecimal previousBalance) {
        return new ItfTransactionRecord(
            entry.getMeanInformation().getReferenceId(),
            headers.appCode(),
            headers.operationNumber(),
            // ... todos los campos tipados, sin truncados manuales
            ItfFinalAccountStatus.CALCULATED
        );
    }
}

// ItfTransactionRepository.java — solo persiste con Panache
public void save(ItfTransactionRecord record) {
    ItfTransaction entity = toEntity(record);
    persist(entity);  // Panache valida contra @Column(length=...) automáticamente
}
```

---

### D1-006 — Número mágico `20` como longitud de cuenta

**Hallazgo del informe**: D1-006
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 257

**Antes**: `return referenceId.trim().length() != 20;`

**Después**:
```java
private static final int STANDARD_ACCOUNT_NUMBER_LENGTH = 20;

return referenceId.trim().length() != STANDARD_ACCOUNT_NUMBER_LENGTH;
```

---

### D1-007 — Números mágicos en `saveItfTxn` (longitudes de truncado)

**Hallazgo del informe**: D1-007
**Clase**: `ItfTxnRepository`
**Línea(s)**: 75–84

**Recomendación**: Con la adopción de D4-013 (eliminar queries nativos) y el modelo JPA, estos truncados desaparecen. Si persisten temporalmente, extraer a constantes.

**Antes**:
```java
var accountNumber = truncate(referenceId, 60);
var appCode       = truncate(headers.appCode(), 4);
var operationNumber = truncate(headers.opnNumber(), 16);
```

**Después** (con Panache):
```java
// @Column(length = 60) en la entidad ItfTransaction valida automáticamente
// Los truncados manuales se eliminan por completo
```

**Después** (si los truncados son temporales):
```java
private static final int MAX_ACCOUNT_NUMBER   = 60;
private static final int MAX_APP_CODE         = 4;
private static final int MAX_OPERATION_NUMBER = 16;

var accountNumber   = truncate(referenceId, MAX_ACCOUNT_NUMBER);
var appCode         = truncate(headers.appCode(), MAX_APP_CODE);
```

---

### D1-008 — JavaDoc boilerplate con HTML entities

**Hallazgo del informe**: D1-008
**Clase**: `PositionKeepingApiImpl`, `EvaluateItfServiceImpl`
**Línea(s)**: 15–41

**Recomendación**: Eliminar los bloques JavaDoc de cabecera que solo repiten autoría y fecha. Esa información pertenece al historial de git.

**Antes**:
```java
/**
 * @author Banco de Cr&eacute;dito del Per&uacute; (BCP)
 * <u>Developed by</u>: <ul><li>Franco Carrillo</li></ul>
 * <u>Changes</u>: <ul><li>Febrero 24, 2026 Creaci&oacute;n.</li></ul>
 * @version 1.0
 */
@ApplicationScoped
public class ItfComplianceEvaluator implements EvaluateItfService {
```

**Después**:
```java
@ApplicationScoped
public class ItfComplianceEvaluator implements EvaluateItfService {
```

---

### D1-009 — `catch (Exception ex)` demasiado genérico en `evaluateItf`

**Hallazgo del informe**: D1-009
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 86–88

**Recomendación**: No envolver en `catch(Exception)` las `ApiException` que ya vienen tipadas de los submétodos. Dejarlas subir directamente al framework.

**Antes**:
```java
try {
    return processItfCalculation(headers, request);
} catch (Exception ex) {  // ← captura NPE, CCE y ApiException por igual
    throw buildServiceException("evaluateItf", ex);
}
```

**Después**:
```java
public List<EvaluateItfResponseInner> evaluateItf(
        EvaluateItfHeaders headers, EvaluateItfRequest request) {
    if (Boolean.TRUE.equals(headers.reversalFlag())) {
        return processReversal(headers, request);
    }
    return processItfCalculation(headers, request);
    // Las ApiException de los submétodos suben directamente sin envoltura adicional.
}
```

---

### D1-012 — `EvaluateItfServiceImpl` con 11+ dependencias directas

**Hallazgo del informe**: D1-012
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 9–16

**Recomendación**: Con la introducción de puertos (D2-001), las 4 dependencias de infraestructura concretas se reemplazan por 4 interfaces. Adicionalmente, D4-009 propone dividir responsabilidades en servicios más pequeños.

**Antes**: 4 dependencias de infraestructura concretas + entidades JPA importadas directamente.

**Después** (con puertos):
```java
private final ItfTransactionPort itfTransactionPort;
private final SalaryBalancePort  salaryBalancePort;
private final ItfRatePort        itfRatePort;
private final ExonerationPort    exonerationPort;
```

---

## D4 — Malas Prácticas

---

### D4-001 — Redis `KEYS` bloqueante en `getAll()`

**Hallazgo del informe**: D4-001
**Clase**: `ItfRateRedisProxy`
**Línea(s)**: 110–117

**Recomendación**: Reemplazar `keys.keys(pattern)` por `SCAN` con cursor.

**Beneficio**: Elimina el riesgo de bloquear Redis durante segundos bajo carga. `SCAN` itera el keyspace en lotes sin bloquear otras operaciones.

**Antes**:
```java
for (String key : keys.keys(KEY_PREFIX + "*")) {  // ← KEYS: O(N) bloqueante
    Map<String, String> data = hash.hgetall(key);
    ...
}
```

**Después**:
```java
public List<ItfRateCache> getAll() {
    List<ItfRateCache> result = new ArrayList<>();
    var scanArgs = new KeyScanArgs().match(KEY_PREFIX + "*").count(100);
    KeyScanCursor<String> cursor = keys.scan(scanArgs);

    while (cursor != null) {
        for (String key : cursor.keys()) {
            ItfRateCache cache = fromMap(hash.hgetall(key));
            if (cache != null) result.add(cache);
        }
        if (cursor.isFinished()) break;
        cursor = keys.scan(cursor, scanArgs);
    }
    return result;
}
```

---

### D4-002 y D4-003 — Parámetro `Object entryObj` con cast inseguro

**Hallazgo del informe**: D4-002, D4-003
**Clase**: `ItfTxnRepository`
**Línea(s)**: 69, 73

**Recomendación**: Cambiar el tipo del parámetro a `EvaluateItfRequestEntriesInner`. Eliminar el cast.

**Antes**:
```java
public void saveItfTxn(EvaluateItfHeaders headers, Object entryObj, ...) {
    var entry = (EvaluateItfRequestEntriesInner) entryObj;  // ← ClassCastException en runtime
```

**Después**:
```java
public void save(ItfTransactionRecord record) {  // ← tipo correcto, sin cast
```

---

### D4-004 y D4-005 — Logging con concatenación de String

**Hallazgo del informe**: D4-004, D4-005
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 53, 276

**Antes**:
```java
LOG.error("[EvaluateItfServiceImpl - " + methodName + "] Error at evaluating itf service", cause);
```

**Después**:
```java
LOG.errorf(cause, "[ItfComplianceEvaluator - %s] Error evaluating ITF compliance", methodName);
```

---

### D4-006 y D4-007 — Inyección de campo en lugar de constructor

**Hallazgo del informe**: D4-006, D4-007
**Clase**: `AzureTableConfig`, `ItfRateTableStorageProxy`
**Línea(s)**: 19, 26

**Antes**:
```java
@Named("itfRates")
TableClient itfRates;  // ← campo inyectado, no visible en constructor
```

**Después**:
```java
private final TableClient itfRates;

@Inject
public ItfRateTableStorageProxy(@Named("itfRates") TableClient itfRates) {
    this.itfRates = itfRates;
}
```

---

### D4-008 — Patrón de logging inconsistente

**Hallazgo del informe**: D4-008
**Clase**: `ExoneratedAccountRepository`
**Línea(s)**: 33

**Antes**: log inline en el catch.
**Después**: usar `buildDbException` (patrón ya existente en la clase).

---

### D4-009 — God Object: `EvaluateItfServiceImpl` con 5 responsabilidades

**Hallazgo del informe**: D4-009
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 43

**Recomendación**: Distribuir responsabilidades en clases especializadas.

**Antes**: una clase con orquestación + cálculo + exoneración + PDH + persistencia.

**Después**:
```
ItfComplianceEvaluator (orquestador)
  ├── usa → ItfCalculationPolicy   (calcula base imponible y monto ITF)
  ├── usa → ExonerationPolicy      (3 reglas de exoneración)
  ├── usa → ItfTransactionPort     (persiste)
  └── usa → SalaryBalancePort      (gestiona saldo PDH)
```

---

### D4-010 — Transaction Script: `processItfCalculation` procedimental

**Hallazgo del informe**: D4-010
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 138

**Recomendación**: Introducir objetos de dominio que encapsulen las reglas de negocio.

**Antes**: 8 pasos secuenciales en 65 líneas sin abstracciones de dominio.

**Después**:
```java
// ItfCalculationPolicy.java
public BigDecimal calculate(BigDecimal amount, BigDecimal taxFreeAmount,
                            BigDecimal itfRate, EntryType entryType) {
    BigDecimal taxableBase = entryType == CARGO
        ? amount.subtract(taxFreeAmount) : amount;
    return taxableBase.divide(MIN_TAXABLE_UNIT, 0, DOWN)
        .multiply(MIN_TAXABLE_UNIT).multiply(itfRate);
}

// ExonerationPolicy.java
public boolean isExempt(String referenceId, String utc,
                        String productType, TransferType transferType) {
    return hasExemptAccountFormat(referenceId)
        || isProductExempt(utc, productType)
        || isOwnAccountTransfer(transferType);
}
```

---

### D4-011 — Anemic Domain Model: entidades sin comportamiento

**Hallazgo del informe**: D4-011
**Clase**: `ItfTxn`, `PdhBalance`, `ExoneratedAccount`

**Recomendación**: Mover la lógica de construcción y cambio de estado a las propias entidades.

**Antes**:
```java
// ItfTxn.java — solo campos
public class ItfTxn extends PanacheEntityBase {
    public ItfTxnId itfTxnId;
    public BigDecimal itfAmount;
    public Boolean reversalFlag;
    // sin ningún método de negocio
}
// La construcción y el cambio de estado están dispersos en ItfTxnRepository
```

**Después**:
```java
public class ItfTransaction extends PanacheEntityBase {
    // campos...

    public static ItfTransaction create(ItfTransactionRecord record) {
        // validaciones e invariantes aquí
        ItfTransaction tx = new ItfTransaction();
        tx.itfAmount = record.itfAmount();
        tx.status = ItfFinalAccountStatus.CALCULATED;
        return tx;
    }

    public void markAsReversed() {
        if (Boolean.TRUE.equals(this.reversalFlag)) {
            throw new IllegalStateException("Transaction already reversed");
        }
        this.reversalFlag = true;
        this.status = ItfFinalAccountStatus.REVERSED;
    }
}
```

---

### D4-012 — Ausencia de validación en frontera REST

**Hallazgo del informe**: D4-012
**Clase**: `PositionKeepingApiImpl`
**Línea(s)**: 43–55

**Recomendación**: Añadir anotaciones Bean Validation a los parámetros del endpoint.

**Beneficio**: Requests inválidos son rechazados con `400 Bad Request` en frontera, antes de entrar a la lógica de negocio.

**Antes**:
```java
public Response evaluateItfCompliance(
    String authorization,
    String requestId,
    EvaluateItfRequest evaluateItfRequest,  // ← sin @Valid
    ...) {
```

**Después**:
```java
public Response evaluateItfCompliance(
    @NotBlank String authorization,
    @NotBlank String requestId,
    @NotBlank String requestDate,
    @NotBlank String appCode,
    @NotBlank String opnNumber,
    Boolean reversalFlag,
    @Valid @NotNull EvaluateItfRequest evaluateItfRequest,
    String userCode,
    String serverTerminal) {
```

---

### D4-013 — Uso de queries nativos cuando el ORM puede resolverlo

**Hallazgo del informe**: D4-013
**Clase**: `ItfTxnRepository`
**Línea(s)**: 91–118 (INSERT nativo), 156–168 (UPDATE nativo)

**Recomendación**: Reemplazar los `createNativeQuery` por la API de Panache/JPQL. El INSERT se reemplaza con `persist()` sobre la entidad mapeada. El UPDATE con una query JPQL tipada.

**Beneficio**:
- El compilador detecta parámetros mal posicionados (actualmente `?3` ↔ `?4` no genera error de compilación).
- Los truncados manuales de D1-007 desaparecen: Hibernate valida contra `@Column(length=...)`.
- La lógica no queda acoplada al dialecto SQL de SingleStore.
- `nextItfTransactionId()` (D3-002) se vuelve innecesario con `AUTO_INCREMENT`.

**Antes** — INSERT nativo de 17 parámetros:
```java
// ItfTxnRepository.java:91–118
getEntityManager().createNativeQuery(
    "INSERT INTO itf_transaction ("
        + "itf_transaction_id, account_number, app_code, ..."
        + ") VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17)")
    .setParameter(1, itfTransactionId)
    .setParameter(2, accountNumber)
    // ... 15 setParameter más, sin validación de tipos
    .executeUpdate();
```

**Antes** — UPDATE nativo:
```java
// ItfTxnRepository.java:156–168
getEntityManager().createNativeQuery(
    "UPDATE itf_transaction SET final_account_status = ?1, reversal_flag = ?2 "
        + "WHERE itf_transaction_id = ?3 AND account_number = ?4 "
        + "AND app_code = ?5 AND operation_number = ?6 AND reversal_flag = false")
    .setParameter(1, status.name())
    .setParameter(2, true)
    // ...
    .executeUpdate();
```

**Después** — INSERT con Panache:
```java
public void save(ItfTransactionRecord record) {
    ItfTransaction entity = ItfTransaction.create(record);
    persist(entity);  // Hibernate valida @Column(length=...) automáticamente
}
```

**Después** — UPDATE con JPQL tipado:
```java
public void markAsReversed(ItfTransactionKey key) {
    update("finalAccountStatus = ?1, reversalFlag = true "
         + "WHERE itfTxnId.itfTransactionId = ?2 AND itfTxnId.accountNumber = ?3 "
         + "AND itfTxnId.appCode = ?4 AND itfTxnId.operationNumber = ?5 "
         + "AND reversalFlag = false",
         ItfFinalAccountStatus.REVERSED,
         key.itfTransactionId(), key.accountNumber(),
         key.appCode(), key.operationNumber());
}
```

---

## D5 — Código Muerto

> Para todos los hallazgos de código muerto la recomendación es la misma: **eliminar**. El código muerto no aporta valor y aumenta la superficie de mantenimiento. Git preserva el historial si se necesita recuperar algo.

---

### D5-001 — `findWithoutReversalByOpnNumber` sin llamantes

**Hallazgo del informe**: D5-001 | **Clase**: `ItfTxnRepository` | **Línea(s)**: 35–43

**Antes**: `public Optional<ItfTxn> findWithoutReversalByOpnNumber(String opnNumber) { ... }`
**Después**: método eliminado. Solo `findAllWithoutReversalByOpnNumber` es invocado en producción.

---

### D5-002 — `ItfRateRedisProxy.get(String itf)` sin llamantes

**Hallazgo del informe**: D5-002 | **Clase**: `ItfRateRedisProxy` | **Línea(s)**: 51–64

**Después**: método eliminado. Solo `getAll()` es invocado desde producción.

---

### D5-003 — `ItfRateRedisProxy.evict(String utc)` sin llamantes

**Hallazgo del informe**: D5-003 | **Clase**: `ItfRateRedisProxy` | **Línea(s)**: 92–99

**Después**: método eliminado.

---

### D5-004 — `CodTxnExoneratedItfRedisProxy.evict(...)` sin llamantes

**Hallazgo del informe**: D5-004 | **Clase**: `CodTxnExoneratedItfRedisProxy` | **Línea(s)**: 99

**Después**: método eliminado.

---

### D5-005 — `ItfRateTableStorageProxy.findByItf()` sin llamantes

**Hallazgo del informe**: D5-005 | **Clase**: `ItfRateTableStorageProxy` | **Línea(s)**: 34–41

**Después**: método eliminado. Las tasas ITF se leen vía Redis en el flujo de evaluación.

---

### D5-006 — `ItfRateTableStorageProxy.save()` sin llamantes

**Hallazgo del informe**: D5-006 | **Clase**: `ItfRateTableStorageProxy` | **Línea(s)**: 49–66

**Recomendación**: Evaluar si el flujo de escritura de tasas fue eliminado o nunca implementado. Si no existe caso de uso activo, eliminar. Si está planificado, introducirlo en un feature propio con spec y tests.

---

### D5-007 — `ExoneratedCodTxnItfTableStorageProxy.findByUtc()` sin llamantes

**Hallazgo del informe**: D5-007 | **Clase**: `ExoneratedCodTxnItfTableStorageProxy` | **Línea(s)**: 34

**Después**: método eliminado.

---

### D5-008 — `ExoneratedCodTxnItfTableStorageProxy.save()` sin llamantes

**Hallazgo del informe**: D5-008 | **Clase**: `ExoneratedCodTxnItfTableStorageProxy` | **Línea(s)**: 53

**Recomendación**: Misma evaluación que D5-006.

---

### D5-009 — `ExoneratedAccountRepository` completamente sin uso en producción

**Hallazgo del informe**: D5-009 | **Clase**: `ExoneratedAccountRepository` completa

**Recomendación**: Eliminar la clase completa junto con su entidad `ExoneratedAccount` y sus tests. Si el caso de uso de cuentas exoneradas está planificado, debe introducirse en un feature propio.

**Beneficio**: Se elimina un repositorio JPA completo + entidad + tests que no aportan valor activo.

---

### D5-010 — `PdhBalanceRepository.persistPdhBalance()` sin llamantes

**Hallazgo del informe**: D5-010 | **Clase**: `PdhBalanceRepository` | **Línea(s)**: 90–96

**Después**: método eliminado. Los saldos PDH se actualizan pero nunca se crean desde este servicio.

---

### D5-011 — Override de `delete()` sin lógica adicional

**Hallazgo del informe**: D5-011 | **Clase**: `ItfTxnRepository` | **Línea(s)**: 174–182

**Después**: override eliminado. `PanacheRepositoryBase` ya provee la implementación equivalente.

---

## D6 — Código Duplicado

---

### D6-001 — Patrón `buildXxxException` duplicado en 9 clases

**Hallazgo del informe**: D6-001
**Clases**: `EvaluateItfServiceImpl`, `ItfRateRedisProxy`, `CodTxnExoneratedItfRedisProxy`, `ItfTxnRepository`, `PdhBalanceRepository`, `ItfRateTableStorageProxy`, `ExoneratedCodTxnItfTableStorageProxy`, `ExoneratedAccountRepository`, `CacheWarmUpSupport`

**Recomendación**: Extraer a un método utilitario estático compartido.

**Beneficio**: Un único punto de cambio para la estrategia de manejo de errores de infraestructura.

**Antes** (repetido en 9 clases):
```java
private ApiException buildDbException(String methodName, Exception cause) {
    LOG.error("[ClassName - " + methodName + "] Error at calling singlestore", cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
}
```

**Después**:
```java
// infrastructure/InfrastructureExceptionBuilder.java
public final class InfrastructureExceptionBuilder {
    private InfrastructureExceptionBuilder() {}

    public static ApiException build(Logger log, String className,
                                     String methodName, String system, Exception cause) {
        log.errorf(cause, "[%s - %s] Error calling %s", className, methodName, system);
        return ApiException.builder()
            .cause(cause)
            .category(ExceptionCategoryTypes.ofValue(
                InfrastructureErrorCodes.EXTERNAL_SYSTEM_FAILURE))
            .build();
    }
}

// Uso en cada clase (una línea):
throw InfrastructureExceptionBuilder.build(LOG, "ItfTransactionRepository",
    "save", "SingleStore", ex);
```

---

### D6-002 — `nowTime()` / `nowDate()` duplicados en dos proxies Redis

**Hallazgo del informe**: D6-002
**Clases**: `ItfRateRedisProxy`, `CodTxnExoneratedItfRedisProxy`

**Después**:
```java
// infrastructure/redis/RedisTimestampHelper.java
public final class RedisTimestampHelper {
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm:ss");
    private RedisTimestampHelper() {}

    public static String currentTime() {
        return LocalTime.now().format(TIME_FORMAT);
    }
    public static String currentDate() {
        return LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    }
}

// Uso en ambos proxies:
hash.hset(key, Map.of(
    "itfRate",          dto.itfRate(),
    "timeUpdatedCache", RedisTimestampHelper.currentTime(),
    "dateUpdatedCache", RedisTimestampHelper.currentDate()
));
```

---

### D6-003 — Bug de fecha/hora duplicado en dos clases de Table Storage

**Hallazgo del informe**: D6-003
**Clases**: `ItfRateTableStorageProxy`, `ExoneratedCodTxnItfTableStorageProxy`

**Recomendación**: Corregir ambas clases (D3-003 y D3-004) y extraer el bloque de metadatos de auditoría a una utilidad compartida.

**Después**:
```java
// infrastructure/tablestorage/AuditMetadataBuilder.java
public final class AuditMetadataBuilder {
    private AuditMetadataBuilder() {}

    public static void addAuditTimestamps(TableEntity entity) {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        entity.getProperties().put("timeCreated", time);
        entity.getProperties().put("dateCreate",  date);
        entity.getProperties().put("timeUpdate",  time);
        entity.getProperties().put("dateUpdate",  date);
    }
}

// Uso en ambas clases:
AuditMetadataBuilder.addAuditTimestamps(entity);
```

---

### D6-004 — Estructura `@Startup` + `@Scheduled` duplicada en dos loaders

**Hallazgo del informe**: D6-004
**Clases**: `ItfRateCacheLoader`, `CodTxnExoneratedItfCacheLoader`

**Después**:
```java
// infrastructure/cache/ScheduledCacheLoader.java
public abstract class ScheduledCacheLoader {
    @Startup
    void onStart() { load(); }

    @Scheduled(every = "${cache.reload.interval:1h}")
    void scheduledReload() { load(); }

    protected abstract void load();
}

// ItfRateCacheLoader.java
public class ItfRateCacheLoader extends ScheduledCacheLoader {
    @Override
    protected void load() { /* carga tasas ITF */ }
}

// ExoneratedTransactionCacheLoader.java
public class ExoneratedTransactionCacheLoader extends ScheduledCacheLoader {
    @Override
    protected void load() { /* carga códigos exonerados */ }
}
```

---

### D6-005 — Constante `EXTERNAL_ERROR` definida en 9 clases

**Hallazgo del informe**: D6-005

**Recomendación**: Ver D1-020. Centralizar en `InfrastructureErrorCodes.EXTERNAL_SYSTEM_FAILURE` y eliminar de todas las clases individuales.

---

## D7 — Código Inerte

---

### D7-001 — `processReversal` retorna objeto vacío ignorado por el controlador

**Hallazgo del informe**: D7-001
**Clase**: `EvaluateItfServiceImpl`
**Línea(s)**: 128

**Recomendación**: Retornar `List.of()` en lugar de construir un `EvaluateItfResponseInner` vacío que nadie usa. O mover la decisión de respuesta al controlador.

**Antes**:
```java
return List.of(new EvaluateItfResponseInner());  // ← objeto construido y descartado
```

**Después**:
```java
return List.of();  // ← vacío semántico, sin construir objeto innecesario
```

---

### D7-002 — Ciclo parse-format-parse sin valor

**Hallazgo del informe**: D7-002
**Clase**: `CodTxnExoneratedItfCacheLoader`
**Línea(s)**: 57

**Antes**:
```java
LocalTime.parse(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
```

**Después**:
```java
LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
```

---

### D7-003 — Override de `delete()` sin valor

**Hallazgo del informe**: D7-003
**Clase**: `ItfTxnRepository`
**Línea(s)**: 174–182

**Después**: override eliminado. `PanacheRepositoryBase` ya provee la implementación equivalente.

---

### D7-004 — `ExoneratedAccountRepository.findByAccountNumber` delega a `findById`

**Hallazgo del informe**: D7-004
**Clase**: `ExoneratedAccountRepository`
**Línea(s)**: 47

**Después**: método eliminado (o toda la clase si se adopta D5-009).

---

### D7-005 — `PdhBalanceRepository.findByAccountNumber` delega a `findById`

**Hallazgo del informe**: D7-005
**Clase**: `PdhBalanceRepository`
**Línea(s)**: 105–111

**Recomendación**: Si se adopta el nombre `SalaryPaymentBalanceRepository` (D1-021), el alias `findByAccountNumber` tiene valor semántico sobre `findById` (el ID es el número de cuenta, y no es obvio). En ese caso, **conservarlo** está justificado.

---

## D2 — Arquitectura Hexagonal

> Se implementa al final: es el cambio estructural más grande y se beneficia de todos los renombramientos y limpiezas anteriores.

---

### D2-001 — Servicio depende de implementaciones concretas de infraestructura

**Hallazgo del informe**: D2-001
**Clase**: `ItfComplianceEvaluator` (antes `EvaluateItfServiceImpl`)
**Línea(s)**: 12–15

**Recomendación**: Introducir interfaces (puertos) para cada adaptador. El servicio de aplicación depende de los puertos; cada proxy/repositorio implementa su puerto.

**Beneficio**: El servicio es testeable sin Redis, SingleStore ni Azure. Cada adaptador puede reemplazarse sin modificar la lógica de negocio. Cumple el Principio II de la constitución.

**Antes**:
```java
import com.bcp.cnf.services.proxy.redis.CodTxnExoneratedItfRedisProxy;  // concreto
import com.bcp.cnf.services.proxy.redis.ItfRateRedisProxy;              // concreto
import com.bcp.cnf.services.proxy.singlestore.ItfTxnRepository;         // concreto
import com.bcp.cnf.services.proxy.singlestore.PdhBalanceRepository;     // concreto
```

**Después** — puertos en el paquete de dominio/aplicación:
```java
// port/ItfTransactionPort.java
public interface ItfTransactionPort {
    void save(ItfTransactionRecord record);
    List<ItfTransaction> findActiveByOperationNumber(String operationNumber);
    void markAsReversed(ItfTransactionKey key);
}

// port/SalaryBalancePort.java
public interface SalaryBalancePort {
    SalaryPaymentBalance findByAccount(String accountNumber);
    SalaryPaymentBalance findByAccountAndUtc(String accountNumber, String utc);
    void updateWithMovement(SalaryPaymentBalance balance, BigDecimal movement, Boolean reversal);
    void restoreAfterReversal(SalaryPaymentBalance balance, BigDecimal movement);
}

// port/ItfRatePort.java
public interface ItfRatePort {
    BigDecimal getCurrentRate();
}

// port/ExonerationPort.java
public interface ExonerationPort {
    boolean isExempt(String utc, String productType);
}

// ItfComplianceEvaluator usa puertos:
@Inject
public ItfComplianceEvaluator(ItfTransactionPort itfTransactionPort,
                               SalaryBalancePort salaryBalancePort,
                               ItfRatePort itfRatePort,
                               ExonerationPort exonerationPort) { ... }

// Adaptadores implementan los puertos:
public class ItfTransactionRepository
    implements ItfTransactionPort, PanacheRepositoryBase<ItfTransaction, ItfTransactionKey> {
    @Override
    public void save(ItfTransactionRecord record) {
        persist(ItfTransaction.create(record));
    }
    // ...
}
```

---

### D2-002 a D2-005 — Sin puertos para adaptadores primarios

**Hallazgo del informe**: D2-002, D2-003, D2-004, D2-005
**Clases**: `ItfTxnRepository`, `PdhBalanceRepository`, `CodTxnExoneratedItfRedisProxy`, `ItfRateRedisProxy`

**Recomendación**: Ver D2-001. Cada adaptador implementa su puerto correspondiente. La inversión de dependencias queda completa.

---

### D2-006 y D2-007 — Sin puertos para adaptadores de Azure Table Storage

**Hallazgo del informe**: D2-006, D2-007
**Clases**: `ItfRateTableStorageProxy`, `ExoneratedCodTxnItfTableStorageProxy`

**Después**:
```java
// port/ItfRateTablePort.java
public interface ItfRateTablePort {
    Optional<ItfRateRecord> findByRate(String itfRate);
    void save(ItfRateRecord record);
    List<ItfRateRecord> findAll();
}

public class ItfRateTableStorageProxy implements ItfRateTablePort {
    // implementación con Azure SDK
}
```

---

### D2-008 y D2-009 — Entidades JPA importadas en la capa de aplicación

**Hallazgo del informe**: D2-008, D2-009
**Clase**: `ItfComplianceEvaluator`
**Línea(s)**: 9–10

**Recomendación**: Usar objetos de dominio puros (sin anotaciones JPA) para el intercambio entre la capa de aplicación y los puertos. Los adaptadores mapean internamente.

**Después**:
```java
// domain/SalaryPaymentBalance.java — objeto puro, sin JPA
public record SalaryPaymentBalance(
    String accountNumber, String utc,
    BigDecimal closingBalance, BigDecimal previousBalance, Boolean reversalFlag
) {}

// Servicio usa el objeto de dominio:
SalaryPaymentBalance balance = salaryBalancePort.findByAccount(referenceId);

// Adaptador hace el mapeo:
@Override
public SalaryPaymentBalance findByAccount(String accountNumber) {
    SalaryPaymentBalanceEntity entity = findById(accountNumber);
    return entity == null ? null
        : new SalaryPaymentBalance(entity.accountNumber, entity.utc,
            entity.accountBalanceClosing, entity.previousBalance, entity.reversalFlag);
}
```

---

### D2-010 — `EvaluateItfHeaders` filtra concerns HTTP hacia el servicio

**Hallazgo del informe**: D2-010
**Clase**: `EvaluateItfHeaders`

**Recomendación**: Crear un command object de dominio con solo los campos que el servicio necesita.

**Después**:
```java
// command/EvaluateItfCommand.java
public record EvaluateItfCommand(
    String operationNumber,
    String appCode,
    Boolean reversalFlag
) {}

// PositionKeepingResource construye el command:
var command = new EvaluateItfCommand(opnNumber, appCode, reversalFlag);
evaluateItfService.evaluateItf(command, request);
```

---

### D2-011 — Schedulers inyectan proxies concretos

**Hallazgo del informe**: D2-011
**Clase**: `ItfRateCacheLoader`
**Línea(s)**: 7–8

**Antes**:
```java
@Inject ItfRateRedisProxy redisProxy;
@Inject ItfRateTableStorageProxy tableProxy;
```

**Después**:
```java
@Inject ItfRatePort itfRatePort;
@Inject ItfRateTablePort itfRateTablePort;
```

---

## Resumen de impacto

| Dimensión | Hallazgos | Fixes producción | Refactorizaciones | Eliminaciones |
|-----------|:---------:|:----------------:|:-----------------:|:-------------:|
| D3 — Errores de lógica | 7 | 4 críticos + 3 altos | — | — |
| D1 — Nombramientos | 27 | 2 (typos en BD) | 22 | 3 |
| D4 — Malas prácticas | 13 | 3 (KEYS Redis, cast, queries nativos) | 8 | 2 |
| D5 — Código muerto | 11 | — | — | 11 |
| D6 — Código duplicado | 5 | — | 4 | 1 |
| D7 — Código inerte | 5 | — | 2 | 3 |
| D2 — Hexagonal | 11 | — | 11 | — |
| **Total** | **79** | **7** | **47** | **20** |
