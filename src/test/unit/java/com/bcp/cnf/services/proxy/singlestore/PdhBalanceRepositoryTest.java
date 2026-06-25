package com.bcp.cnf.services.proxy.singlestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.cnf.services.model.entities.PdhBalance;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PdhBalanceRepositoryTest {

  @Spy
  PdhBalanceRepository repository;

  @Test
  @DisplayName("Should return PDH balance when account and UTC exist")
  void shouldReturnPdhBalanceWhenAccountAndUtcExist() {
    // Arrange
    String account = "123";
    String utc = "20240101";

    PdhBalance expected = new PdhBalance();
    @SuppressWarnings("unchecked")
    PanacheQuery<PdhBalance> query = Mockito.mock(PanacheQuery.class);

    when(query.firstResult()).thenReturn(expected);

    doReturn(query)
        .when(repository)
        .find("accountNumber = ?1 and utc = ?2", account, utc);

    // Act
    PdhBalance result = repository.findByAccountAndUtc(account, utc);

    // Assert
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should return null when account and UTC do not exist")
  void shouldReturnNullWhenAccountAndUtcDoNotExist() {
    // Arrange
    String account = "notfound";
    String utc = "0000";

    @SuppressWarnings("unchecked")
    PanacheQuery<PdhBalance> query = Mockito.mock(PanacheQuery.class);
    when(query.firstResult()).thenReturn(null);

    doReturn(query)
        .when(repository)
        .find("accountNumber = ?1 and utc = ?2", account, utc);

    // Act
    PdhBalance result = repository.findByAccountAndUtc(account, utc);

    // Assert
    assertNull(result);
  }

  @Test
  @DisplayName("Should update closing balance and mark reversal flow")
  void shouldUpdateClosingBalanceAndMarkReversalFlow() {
    // Arrange
    PdhBalance pdh = new PdhBalance();
    pdh.accountBalanceClosing = new BigDecimal("100");

    EntityManager entityManager = Mockito.mock(EntityManager.class);
    doReturn(entityManager).when(repository).getEntityManager();
    when(entityManager.merge(pdh)).thenReturn(pdh);

    BigDecimal amount = new BigDecimal("10");

    // Act
    repository.updateClosingBalanceWhenReversalFlow(pdh, amount);

    // Assert
    assertEquals(new BigDecimal("100"), pdh.previousBalance);
    assertEquals(new BigDecimal("110"), pdh.accountBalanceClosing);
    assertTrue(pdh.reversalFlag);
    verify(entityManager).merge(pdh);
  }

  @Test
  @DisplayName("Should update PDH movement fields when account exists")
  void shouldUpdatePdhMovementFieldsWhenAccountExists() {
    // Arrange
    String account = "ACC001";
    String utc = "UTC001";
    BigDecimal movement = new BigDecimal("25.50");

    PdhBalance pdh = new PdhBalance();
    pdh.accountBalanceClosing = new BigDecimal("100.00");

    doReturn(pdh).when(repository).findByAccountNumber(account);

    // Act
    repository.updatePdh(account, utc, movement, false);

    // Assert
    assertEquals(utc, pdh.utc);
    assertEquals(new BigDecimal("100.00"), pdh.previousBalance);
    assertEquals(new BigDecimal("125.50"), pdh.accountBalanceClosing);
    assertEquals(Boolean.FALSE, pdh.reversalFlag);
  }

  @Test
  @DisplayName("Should skip PDH update when account does not exist")
  void shouldSkipPdhUpdateWhenAccountDoesNotExist() {
    // Arrange
    String account = "ACC999";
    doReturn(null).when(repository).findByAccountNumber(account);

    // Act
    repository.updatePdh(account, "UTC999", BigDecimal.ONE, true);

    // Assert
    verify(repository).findByAccountNumber(account);
  }

  @Test
  @DisplayName("Should call persist on PDH entity")
  void shouldCallPersistOnPdhEntity() {
    // Arrange
    PdhBalance pdh = Mockito.mock(PdhBalance.class);

    // Act
    repository.persistPdhBalance(pdh);

    // Assert
    verify(pdh).persist();
  }

  @Test
  @DisplayName("Should return PDH balance when account number exists")
  void shouldReturnPdhBalanceWhenAccountNumberExists() {
    // Arrange
    String account = "ACC777";
    PdhBalance expected = new PdhBalance();
    doReturn(expected).when(repository).findById(account);

    // Act
    PdhBalance result = repository.findByAccountNumber(account);

    // Assert
    assertEquals(expected, result);
    verify(repository).findById(account);
  }

  @Test
  @DisplayName("Should return null when account number does not exist")
  void shouldReturnNullWhenAccountNumberDoesNotExist() {
    // Arrange
    String account = "ACC000";
    doReturn(null).when(repository).findById(account);

    // Act
    PdhBalance result = repository.findByAccountNumber(account);

    // Assert
    assertNull(result);
    verify(repository).findById(account);
  }

  @Test
  @DisplayName("Should throw ApiException when query execution fails on findByAccountAndUtc")
  void shouldThrowApiExceptionWhenQueryExecutionFailsOnFindByAccountAndUtc() {
    // Arrange
    RuntimeException cause = new RuntimeException("db find error");
    doThrow(cause)
        .when(repository)
        .find("accountNumber = ?1 and utc = ?2", "123", "20240101");

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.findByAccountAndUtc("123", "20240101")
    );

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
  }

  @Test
  @DisplayName("Should throw ApiException when merge fails during reversal flow update")
  void shouldThrowApiExceptionWhenMergeFailsDuringReversalFlowUpdate() {
    // Arrange
    PdhBalance pdh = new PdhBalance();
    pdh.accountBalanceClosing = new BigDecimal("100");

    EntityManager entityManager = Mockito.mock(EntityManager.class);
    doReturn(entityManager).when(repository).getEntityManager();
    RuntimeException cause = new RuntimeException("db merge error");
    when(entityManager.merge(pdh)).thenThrow(cause);

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.updateClosingBalanceWhenReversalFlow(pdh, BigDecimal.TEN)
    );

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
  }

  @Test
  @DisplayName("Should throw ApiException when lookup fails during PDH update")
  void shouldThrowApiExceptionWhenLookupFailsDuringPdhUpdate() {
    // Arrange
    RuntimeException cause = new RuntimeException("db lookup error");
    doThrow(cause).when(repository).findByAccountNumber("ACC001");

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.updatePdh("ACC001", "UTC001", BigDecimal.ONE, false)
    );

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
  }

  @Test
  @DisplayName("Should throw ApiException when persist operation fails")
  void shouldThrowApiExceptionWhenPersistOperationFails() {
    // Arrange
    PdhBalance pdh = Mockito.mock(PdhBalance.class);
    RuntimeException cause = new RuntimeException("db persist error");
    doThrow(cause).when(pdh).persist();

    // Act
    ApiException result = assertThrows(ApiException.class, () -> repository.persistPdhBalance(pdh));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
  }

  @Test
  @DisplayName("Should throw ApiException when findById fails on findByAccountNumber")
  void shouldThrowApiExceptionWhenFindByIdFailsOnFindByAccountNumber() {
    // Arrange
    RuntimeException cause = new RuntimeException("db id lookup error");
    doThrow(cause).when(repository).findById("ACC500");

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.findByAccountNumber("ACC500")
    );

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
  }
}