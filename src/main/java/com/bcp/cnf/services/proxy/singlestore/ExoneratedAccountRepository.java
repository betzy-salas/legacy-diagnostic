package com.bcp.cnf.services.proxy.singlestore;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import com.bcp.cnf.services.model.entities.ExoneratedAccount;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Repositorio para operaciones de persistencia de cuentas exoneradas.
 */
@ApplicationScoped
@Transactional
public class ExoneratedAccountRepository
    implements PanacheRepositoryBase<ExoneratedAccount, String> {

  private static final Logger LOG = Logger.getLogger(ExoneratedAccountRepository.class);
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  /**
   * Persiste una cuenta exonerada en SingleStore.
   *
   * @param exoneratedAccount entidad de cuenta exonerada a persistir
   */
  public void persistExoneratedAccount(ExoneratedAccount exoneratedAccount) {
    try {
      LOG.info("[ExoneratedAccountRepository - persistExoneratedAccount] calling singlestore");
      exoneratedAccount.persist();
    } catch (Exception ex) {
      LOG.error("[ExoneratedAccountRepository - persistExoneratedAccount] "
          + "Error at calling singlestore", ex);
      throw ApiException.builder()
          .cause(ex)
          .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
          .build();
    }
  }

  /**
   * Busca una cuenta exonerada por numero de cuenta.
   *
   * @param accountNumber numero de cuenta
   * @return cuenta exonerada o {@code null} si no existe
   */
  public ExoneratedAccount findByAccountNumber(String accountNumber) {
    try {
      LOG.info("[ExoneratedAccountRepository - findByAccountNumber] calling singlestore");
      return findById(accountNumber);
    } catch (Exception ex) {
      LOG.error("[ExoneratedAccountRepository - findByAccountNumber] "
          + "Error at calling singlestore", ex);
      throw ApiException.builder()
          .cause(ex)
          .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
          .build();
    }
  }
}

