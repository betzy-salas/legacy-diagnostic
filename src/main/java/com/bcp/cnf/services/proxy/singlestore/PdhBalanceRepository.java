package com.bcp.cnf.services.proxy.singlestore;


import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import com.bcp.cnf.services.model.entities.PdhBalance;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.jboss.logging.Logger;

/**
 * Repositorio para consultar y actualizar saldos PDH.
 */
@ApplicationScoped
@Transactional
public class PdhBalanceRepository implements PanacheRepositoryBase<PdhBalance, String> {

  private static final Logger LOG = Logger.getLogger(PdhBalanceRepository.class);
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  /**
   * Busca el saldo PDH por cuenta y UTC.
   *
   * @param accountNumber numero de cuenta
   * @param utc codigo UTC
   * @return saldo encontrado o {@code null}
   */
  public PdhBalance findByAccountAndUtc(String accountNumber, String utc) {
    try {
      LOG.info("[PdhBalanceRepository - findByAccountAndUtc] calling singlestore");
      return find(
          "accountNumber = ?1 and utc = ?2",
          accountNumber,
          utc
      ).firstResult();
    } catch (Exception ex) {
      throw buildDbException("findByAccountAndUtc", ex);
    }
  }

  /**
   * Ajusta el saldo de cierre cuando la operacion corresponde a reversa.
   *
   * @param pdh entidad PDH a actualizar
   * @param amount monto a revertir
   */
  public void updateClosingBalanceWhenReversalFlow(PdhBalance pdh, BigDecimal amount) {
    try {
      LOG.info("[PdhBalanceRepository - updateClosingBalanceWhenReversalFlow] calling singlestore");
      pdh = getEntityManager().merge(pdh);
      pdh.previousBalance = pdh.accountBalanceClosing;
      pdh.accountBalanceClosing = pdh.accountBalanceClosing.add(amount);
      pdh.reversalFlag = true;
    } catch (Exception ex) {
      throw buildDbException("updateClosingBalanceWhenReversalFlow", ex);
    }
  }

  /**
   * Actualiza un saldo PDH existente con datos del movimiento.
   *
   * @param accountNumber numero de cuenta
   * @param utc codigo UTC
   * @param movent monto del movimiento
   * @param reversalFlag indicador de reversa
   */
  public void updatePdh(String accountNumber, String utc, BigDecimal movent, Boolean reversalFlag) {
    try {
      LOG.info("[PdhBalanceRepository - updatePdh] calling singlestore");
      PdhBalance pdh = findByAccountNumber(accountNumber);

      if (pdh != null) {
        pdh.utc = utc;
        pdh.previousBalance = pdh.accountBalanceClosing;
        pdh.accountBalanceClosing = pdh.accountBalanceClosing.add(movent);
        pdh.reversalFlag = reversalFlag;
      }
    } catch (Exception ex) {
      throw buildDbException("updatePdh", ex);
    }
  }

  /**
   * Persiste un saldo PDH en SingleStore.
   *
   * @param pdhBalance entidad PDH a persistir
   */
  public void persistPdhBalance(PdhBalance pdhBalance) {
    try {
      LOG.info("[PdhBalanceRepository - persistPdhBalance] calling singlestore");
      pdhBalance.persist();
    } catch (Exception ex) {
      throw buildDbException("persistPdhBalance", ex);
    }
  }

  /**
   * Busca un saldo PDH por numero de cuenta.
   *
   * @param referenceId numero de cuenta
   * @return saldo PDH o {@code null} si no existe
   */
  public PdhBalance findByAccountNumber(String referenceId) {
    try {
      LOG.info("[PdhBalanceRepository - findByAccountNumber] calling singlestore");
      return findById(referenceId);
    } catch (Exception ex) {
      throw buildDbException("findByAccountNumber", ex);
    }
  }

  private ApiException buildDbException(String methodName, Exception cause) {
    LOG.error("[PdhBalanceRepository - " + methodName + "] Error at calling singlestore", cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
  }
}

