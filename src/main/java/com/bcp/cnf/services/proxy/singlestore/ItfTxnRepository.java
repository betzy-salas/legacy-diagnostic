package com.bcp.cnf.services.proxy.singlestore;


import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.model.api.EvaluateItfRequestEntriesInner;
import com.bcp.cnf.services.model.entities.ItfTxn;
import com.bcp.cnf.services.model.entities.ItfTxnId;
import com.bcp.cnf.services.model.enums.ItfFinalAccountStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Repositorio para registrar y consultar transacciones ITF.
 */
@ApplicationScoped
@Transactional
public class ItfTxnRepository implements PanacheRepositoryBase<ItfTxn, ItfTxnId> {

  private static final Logger LOG = Logger.getLogger(ItfTxnRepository.class);
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  /**
   * Busca transacciones no reversadas por numero de operacion.
   *
   * @param opnNumber numero de operacion
   * @return transaccion encontrada sin reversa
   */
  public Optional<ItfTxn> findWithoutReversalByOpnNumber(String opnNumber) {
    try {
      LOG.info("[ItfTxnRepository - findWithoutReversalByOpnNumber] calling singlestore");
      return find("itfTxnId.operationNumber = ?1 and reversalFlag = false", opnNumber)
          .firstResultOptional();
    } catch (Exception ex) {
      throw buildDbException("findWithoutReversalByOpnNumber", ex);
    }
  }

  /**
   * Busca todas las transacciones no reversadas por numero de operacion.
   *
   * @param opnNumber numero de operacion
   * @return lista de transacciones encontradas sin reversa
   */
  public List<ItfTxn> findAllWithoutReversalByOpnNumber(String opnNumber) {
    try {
      LOG.info("[ItfTxnRepository - findAllWithoutReversalByOpnNumber] calling singlestore");
      return list("itfTxnId.operationNumber = ?1 and reversalFlag = false", opnNumber);
    } catch (Exception ex) {
      throw buildDbException("findAllWithoutReversalByOpnNumber", ex);
    }
  }

  /**
   * Construye y persiste una transaccion ITF a partir de headers y entry evaluado.
   *
   * @param headers headers de la solicitud
   * @param entryObj entry evaluado
   * @param itfAmount monto ITF calculado
   * @param noTaxesAmount monto del movimiento sin impuestos
   * @param previousBalance saldo previo de la cuenta
   */
  public void saveItfTxn(EvaluateItfHeaders headers, Object entryObj, BigDecimal itfAmount,
                         BigDecimal noTaxesAmount, BigDecimal previousBalance) {
    try {
      LOG.info("[ItfTxnRepository - saveItfTxn] calling singlestore");
      var entry = (EvaluateItfRequestEntriesInner) entryObj;
      var referenceId = entry.getMeanInformation().getReferenceId();
      var accountNumber = truncate(referenceId, 60);
      var appCode = truncate(headers.appCode(), 4);
      var operationNumber = truncate(headers.opnNumber(), 16);
      var accountNumberEncrypted = truncate(referenceId, 100);
      var utc = truncate(entry.getUtc(), 4);
      var transactionAmount = entry.getAmount() != null ? entry.getAmount() : BigDecimal.ZERO;
      var currencyAlphaCode = truncate(
          entry.getMeanInformation().getProductDetail().getCurrency().getAlphaCode(), 3);
      var productType = truncate(
          entry.getMeanInformation().getProductDetail().getProduct().getCode(), 1);
      var operationType = mapOperationType(entry.getEntryType());
      var movementTime = java.time.LocalTime.now();
      var movementDate = java.time.LocalDate.now();
      var finalAccountStatus = ItfFinalAccountStatus.CALCULATED.name();
      var itfTransactionId = nextItfTransactionId();

      getEntityManager().createNativeQuery(
              "INSERT INTO itf_transaction ("
                  + "itf_transaction_id, account_number, app_code, operation_number, "
                  + "account_number_encrypted, "
                  + "product_type, currency_alpha_code, operation_type, utc, reversal_flag, "
                  + "transaction_amount, previous_balance, ift_amount, use_movement_account_pdh, "
                  + "movement_time, movement_date, final_account_status"
                  + ") VALUES ("
                  + "?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17"
                  + ")")
          .setParameter(1, itfTransactionId)
          .setParameter(2, accountNumber)
          .setParameter(3, appCode)
          .setParameter(4, operationNumber)
          .setParameter(5, accountNumberEncrypted)
          .setParameter(6, productType)
          .setParameter(7, currencyAlphaCode)
          .setParameter(8, operationType)
          .setParameter(9, utc)
          .setParameter(10, false)
          .setParameter(11, transactionAmount)
          .setParameter(12, previousBalance)
          .setParameter(13, itfAmount)
          .setParameter(14, noTaxesAmount)
          .setParameter(15, movementTime)
          .setParameter(16, movementDate)
          .setParameter(17, finalAccountStatus)
          .executeUpdate();
    } catch (Exception ex) {
      throw buildDbException("saveItfTxn", ex);
    }
  }

  private Long nextItfTransactionId() {
    try {
      Number maxId = (Number) getEntityManager()
          .createNativeQuery("SELECT COALESCE(MAX(itf_transaction_id), 0) FROM itf_transaction")
          .getSingleResult();
      return maxId.longValue() + 1;
    } catch (Exception ex) {
      throw buildDbException("nextItfTransactionId", ex);
    }
  }

  private String mapOperationType(EvaluateItfRequestEntriesInner.EntryTypeEnum entryType) {
    return entryType == EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO ? "C" : "A";
  }

  private String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  /**
   * Actualiza el estado final de cuenta para una transaccion ITF especifica.
   *
   * @param itfTxnId identificador de la transaccion
   * @param status nuevo estado final de cuenta
   */
  public void changeItfFinalAccountStatusByTxnId(ItfTxnId itfTxnId,
                                                 ItfFinalAccountStatus status) {
    try {
      LOG.info("[ItfTxnRepository - changeItfFinalAccountStatusByTxnId] calling singlestore");
      getEntityManager().createNativeQuery(
              "UPDATE itf_transaction SET final_account_status = ?1 "
                  + ", reversal_flag = ?2 "
                  + "WHERE itf_transaction_id = ?3 AND account_number = ?4 "
                  + "AND app_code = ?5 AND operation_number = ?6 "
                  + "AND reversal_flag = false")
          .setParameter(1, status.name())
          .setParameter(2, true)
          .setParameter(3, itfTxnId.itfTransactionId)
          .setParameter(4, itfTxnId.accountNumber)
          .setParameter(5, itfTxnId.appCode)
          .setParameter(6, itfTxnId.operationNumber)
          .executeUpdate();
    } catch (Exception ex) {
      throw buildDbException("changeItfFinalAccountStatusByTxnId", ex);
    }
  }

  @Override
  public void delete(ItfTxn txn) {
    try {
      LOG.info("[ItfTxnRepository - delete] calling singlestore");
      txn.delete();
    } catch (Exception ex) {
      throw buildDbException("delete", ex);
    }
  }

  private ApiException buildDbException(String methodName, Exception cause) {
    LOG.error("[ItfTxnRepository - " + methodName + "] Error at calling singlestore", cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
  }

}
