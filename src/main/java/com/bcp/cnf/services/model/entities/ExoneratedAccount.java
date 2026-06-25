package com.bcp.cnf.services.model.entities;


import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidad que representa una cuenta exonerada del calculo de ITF.
 */
@Entity
@Table(name = "itf_exonerated_account")
public class ExoneratedAccount extends PanacheEntityBase {

  @Id
  @Column(name = "account_number", length = 60, nullable = false)
  public String accountNumber;

  @Column(name = "account_number_encrypted", length = 100, nullable = false)
  public String accountNumberEncrypted;

  @Column(name = "currency_alpha_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  public String currencyAlphaCode;

  @Column(name = "created_at", columnDefinition = "DATETIME(6)")
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
  public LocalDateTime updatedAt;

  /**
   * Asigna fechas y hora de auditoria antes de insertar o actualizar la entidad.
   */
  @PrePersist
  @PreUpdate
  public void preUpdate() {
    LocalDateTime now = LocalDateTime.now();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    this.updatedAt = now;
  }
}

