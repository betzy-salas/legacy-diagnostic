package com.bcp.cnf.services.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para ItfTxn.
 */
@Embeddable
public class ItfTxnId implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Column(name = "itf_transaction_id", nullable = false)
  public Long itfTransactionId;

  @Column(name = "account_number", length = 60, nullable = false)
  public String accountNumber;

  @Column(name = "app_code", length = 4, nullable = false)
  public String appCode;

  @Column(name = "operation_number", length = 16, nullable = false)
  public String operationNumber;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItfTxnId itfTxnId = (ItfTxnId) o;
    return Objects.equals(itfTransactionId, itfTxnId.itfTransactionId)
        && Objects.equals(accountNumber, itfTxnId.accountNumber)
        && Objects.equals(appCode, itfTxnId.appCode)
        && Objects.equals(operationNumber, itfTxnId.operationNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itfTransactionId, accountNumber, appCode, operationNumber);
  }
}

