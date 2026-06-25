package com.bcp.cnf.services.model.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad que persiste una transaccion evaluada para ITF.
 */
@Entity
@Table(name = "itf_transaction")
public class ItfTxn extends PanacheEntityBase {

  @EmbeddedId
  public ItfTxnId itfTxnId;

  @Column(name = "account_number_encrypted", length = 100, nullable = false)
  public String accountNumberEncrypted;

  @Column(name = "product_type", length = 1, nullable = false, columnDefinition = "CHAR(1)")
  public String productType;

  @Column(name = "currency_alpha_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  public String currencyAlphaCode;

  @Column(name = "operation_type", length = 1, nullable = false, columnDefinition = "CHAR(1)")
  public String operationType;

  @Column(name = "utc", length = 4, columnDefinition = "CHAR(4)")
  public String utc;

  @Column(name = "reversal_flag", nullable = false)
  public Boolean reversalFlag;

  @Column(name = "transaction_amount", precision = 18, scale = 6, nullable = false)
  public BigDecimal transactionAmount;

  @Column(name = "previous_balance", precision = 18, scale = 6)
  public BigDecimal previousBalance;

  @Column(name = "ift_amount", precision = 18, scale = 6)
  public BigDecimal iftAmount;

  @Column(name = "use_movement_account_pdh", precision = 18, scale = 6)
  public BigDecimal useMovementAccountPdh;

  @Column(name = "movement_time", columnDefinition = "TIME(6)")
  public LocalTime movementTime;

  @Column(name = "movement_date")
  public LocalDate movementDate;

  @Column(name = "final_account_status", length = 20)
  public String finalAccountStatus;

}



