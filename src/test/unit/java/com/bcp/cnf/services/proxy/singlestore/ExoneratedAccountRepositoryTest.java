package com.bcp.cnf.services.proxy.singlestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.cnf.services.model.entities.ExoneratedAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExoneratedAccountRepositoryTest {

  @Spy
  private ExoneratedAccountRepository repository;

  @Test
  @DisplayName("Should call persist on the provided exonerated account")
  void shouldCallPersistOnProvidedExoneratedAccount() {
    // Arrange
    ExoneratedAccount mockAccount = Mockito.mock(ExoneratedAccount.class);

    // Act
    repository.persistExoneratedAccount(mockAccount);

    // Assert
    verify(mockAccount, times(1)).persist();
  }

  @Test
  @DisplayName("Should throw ApiException when persist operation fails")
  void shouldThrowApiExceptionWhenPersistOperationFails() {
    // Arrange
    ExoneratedAccount mockAccount = Mockito.mock(ExoneratedAccount.class);
    RuntimeException cause = new RuntimeException("persist failed");
    doThrow(cause).when(mockAccount).persist();

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.persistExoneratedAccount(mockAccount)
    );

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
  }

  @Test
  @DisplayName("Should return account when account number exists")
  void shouldReturnAccountWhenAccountNumberExists() {
    // Arrange
    String accountNumber = "12345";
    ExoneratedAccount expected = new ExoneratedAccount();
    Mockito.doReturn(expected).when(repository).findById(accountNumber);

    // Act
    ExoneratedAccount result = repository.findByAccountNumber(accountNumber);

    // Assert
    assertEquals(expected, result);
    verify(repository).findById(accountNumber);
  }

  @Test
  @DisplayName("Should return null when account number does not exist")
  void shouldReturnNullWhenAccountNumberDoesNotExist() {
    // Arrange
    String accountNumber = "notfound";
    Mockito.doReturn(null).when(repository).findById(accountNumber);

    // Act
    ExoneratedAccount result = repository.findByAccountNumber(accountNumber);

    // Assert
    assertNull(result);
    verify(repository).findById(accountNumber);
  }

  @Test
  @DisplayName("Should throw ApiException when findById operation fails")
  void shouldThrowApiExceptionWhenFindByIdOperationFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("findById failed");
    doThrow(cause).when(repository).findById("ERR-001");

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.findByAccountNumber("ERR-001")
    );

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
  }
}