package com.bcp.cnf.services.model.entities;


import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad que representa saldos PDH usados durante el calculo de ITF.
 */
@Entity
@Table(name = "salary_payment_balance")
public class PdhBalance extends PanacheEntityBase {

  @Id
  @Column(name = "account_number", length = 60, nullable = false)
  public String accountNumber;

  @Column(name = "account_number_encrypted", length = 100, nullable = false)
  public String accountNumberEncrypted;

  @Column(name = "product_type", length = 1, nullable = false, columnDefinition = "CHAR(1)")
  public String productType;

  @Column(name = "currency_alpha_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  public String currencyAlphaCode;

  @Column(name = "utc", length = 4, columnDefinition = "CHAR(4)")
  public String utc;

  @Column(name = "reversal_flag")
  public Boolean reversalFlag;

  @Column(name = "previous_balance", precision = 18, scale = 6)
  public BigDecimal previousBalance;

  @Column(name = "account_balance_closing", precision = 18, scale = 6)
  public BigDecimal accountBalanceClosing;

  @Column(name = "movement_time", columnDefinition = "TIME(6)")
  public LocalTime movementTime;

  @Column(name = "movement_date", nullable = false)
  public LocalDate movementDate;

  /**
   * Actualiza fecha y hora de movimiento antes de persistir o actualizar.
   */
  @PrePersist
  @PreUpdate
  public void preUpdate() {
    this.movementTime = LocalTime.now();
    this.movementDate = LocalDate.now();
  }
}

