package com.bcp.cnf.services.proxy.singlestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.model.api.EvaluateItfRequestEntriesInner;
import com.bcp.cnf.services.model.entities.ItfTxn;
import com.bcp.cnf.services.model.entities.ItfTxnId;
import com.bcp.cnf.services.model.enums.ItfFinalAccountStatus;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItfTxnRepositoryTest {

  @Spy
  ItfTxnRepository repository;

  @Test
  @DisplayName("Should return transaction when operation number exists without reversal")
  void shouldReturnTransactionWhenOperationNumberExistsWithoutReversal() {
    // Arrange
    String opnNumber = "0000000001";
    ItfTxn expected = new ItfTxn();

    @SuppressWarnings("unchecked")
    PanacheQuery<ItfTxn> query = Mockito.mock(PanacheQuery.class);
    Mockito.when(query.firstResultOptional()).thenReturn(Optional.of(expected));
    Mockito.doReturn(query)
        .when(repository)
        .find("itfTxnId.operationNumber = ?1 and reversalFlag = false", opnNumber);

    // Act
    Optional<ItfTxn> result = repository.findWithoutReversalByOpnNumber(opnNumber);

    // Assert
    assertTrue(result.isPresent());
    assertSame(expected, result.get());
  }

  @Test
  @DisplayName("Should return empty optional when operation number does not exist")
  void shouldReturnEmptyOptionalWhenOperationNumberDoesNotExist() {
    // Arrange
    String opnNumber = "NOT_FOUND";

    @SuppressWarnings("unchecked")
    PanacheQuery<ItfTxn> query = Mockito.mock(PanacheQuery.class);
    Mockito.when(query.firstResultOptional()).thenReturn(Optional.empty());
    Mockito.doReturn(query)
        .when(repository)
        .find("itfTxnId.operationNumber = ?1 and reversalFlag = false", opnNumber);

    // Act
    Optional<ItfTxn> result = repository.findWithoutReversalByOpnNumber(opnNumber);

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should throw ApiException when find without reversal query fails")
  void shouldThrowApiExceptionWhenFindWithoutReversalQueryFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("db find failure");
    Mockito.doThrow(cause)
        .when(repository)
        .find("itfTxnId.operationNumber = ?1 and reversalFlag = false", "OPN-ERR");

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.findWithoutReversalByOpnNumber("OPN-ERR")
    );

    // Assert
    assertNotNull(result.getCause());
    assertSame(cause, result.getCause());
  }

  @Test
  @DisplayName("Should return transactions list when operation number exists without reversal")
  void shouldReturnTransactionsListWhenOperationNumberExistsWithoutReversal() {
    // Arrange
    String opnNumber = "0000000002";
    List<ItfTxn> expected = List.of(new ItfTxn(), new ItfTxn());
    Mockito.doReturn(expected)
        .when(repository)
        .list("itfTxnId.operationNumber = ?1 and reversalFlag = false", opnNumber);

    // Act
    List<ItfTxn> result = repository.findAllWithoutReversalByOpnNumber(opnNumber);

    // Assert
    assertEquals(2, result.size());
    assertSame(expected, result);
  }

  @Test
  @DisplayName("Should throw ApiException when find all without reversal query fails")
  void shouldThrowApiExceptionWhenFindAllWithoutReversalQueryFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("db list failure");
    Mockito.doThrow(cause)
        .when(repository)
        .list("itfTxnId.operationNumber = ?1 and reversalFlag = false", "OPN-LIST-ERR");

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.findAllWithoutReversalByOpnNumber("OPN-LIST-ERR")
    );

    // Assert
    assertNotNull(result.getCause());
    assertSame(cause, result.getCause());
  }

  @Test
  @DisplayName("Should persist transaction with truncated values and generated next id")
  void shouldPersistTransactionWithTruncatedValuesAndGeneratedNextId() {
    // Arrange
    String referenceId = "R" + "X".repeat(110);
    EvaluateItfRequestEntriesInner entry = buildEntry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        referenceId,
        "123456",
        null,
        "USDX",
        "CHK"
    );
    EvaluateItfHeaders headers = buildHeaders("APPCODE", "12345678901234567890");
    Query nativeInsert = mockNativeInsertWithMaxId(9L);

    // Act
    repository.saveItfTxn(
        headers,
        entry,
        new BigDecimal("0.50"),
        new BigDecimal("100.00"),
        new BigDecimal("500.00")
    );

    // Assert
    Mockito.verify(nativeInsert).setParameter(1, 10L);
    Mockito.verify(nativeInsert).setParameter(2, referenceId.substring(0, 60));
    Mockito.verify(nativeInsert).setParameter(3, "APPC");
    Mockito.verify(nativeInsert).setParameter(4, "1234567890123456");
    Mockito.verify(nativeInsert).setParameter(5, referenceId.substring(0, 100));
    Mockito.verify(nativeInsert).setParameter(6, "C");
    Mockito.verify(nativeInsert).setParameter(7, "USD");
    Mockito.verify(nativeInsert).setParameter(8, "C");
    Mockito.verify(nativeInsert).setParameter(9, "1234");
    Mockito.verify(nativeInsert).setParameter(10, false);
    Mockito.verify(nativeInsert).setParameter(11, BigDecimal.ZERO);
    Mockito.verify(nativeInsert).setParameter(12, new BigDecimal("500.00"));
    Mockito.verify(nativeInsert).setParameter(13, new BigDecimal("0.50"));
    Mockito.verify(nativeInsert).setParameter(14, new BigDecimal("100.00"));
    Mockito.verify(nativeInsert).setParameter(17, "CALCULATED");
    Mockito.verify(nativeInsert).executeUpdate();
  }

  @Test
  @DisplayName("Should persist transaction using nullable values and ABONO operation mapping")
  void shouldPersistTransactionUsingNullableValuesAndAbonoOperationMapping() {
    // Arrange
    EvaluateItfRequestEntriesInner entry = buildEntry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        null,
        null,
        new BigDecimal("25.00"),
        null,
        null
    );
    EvaluateItfHeaders headers = buildHeaders(null, null);
    Query nativeInsert = mockNativeInsertWithMaxId(0L);

    // Act
    repository.saveItfTxn(
        headers,
        entry,
        null,
        new BigDecimal("4.00"),
        new BigDecimal("10.00")
    );

    // Assert
    Mockito.verify(nativeInsert).setParameter(1, 1L);
    Mockito.verify(nativeInsert).setParameter(2, null);
    Mockito.verify(nativeInsert).setParameter(3, null);
    Mockito.verify(nativeInsert).setParameter(4, null);
    Mockito.verify(nativeInsert).setParameter(5, null);
    Mockito.verify(nativeInsert).setParameter(6, null);
    Mockito.verify(nativeInsert).setParameter(7, null);
    Mockito.verify(nativeInsert).setParameter(8, "A");
    Mockito.verify(nativeInsert).setParameter(9, null);
    Mockito.verify(nativeInsert).setParameter(11, new BigDecimal("25.00"));
    Mockito.verify(nativeInsert).setParameter(13, null);
    Mockito.verify(nativeInsert).setParameter(17, "CALCULATED");
    Mockito.verify(nativeInsert).executeUpdate();
  }

  @Test
  @DisplayName("Should throw ApiException when save receives an invalid entry object")
  void shouldThrowApiExceptionWhenSaveReceivesAnInvalidEntryObject() {
    // Arrange
    EvaluateItfHeaders headers = buildHeaders("Y1", "0000000001");

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.saveItfTxn(
            headers,
            "invalid-entry-object",
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
        )
    );

    // Assert
    assertNotNull(result.getCause());
    assertInstanceOf(ClassCastException.class, result.getCause());
  }

  @Test
  @DisplayName("Should throw ApiException when next transaction id query fails")
  void shouldThrowApiExceptionWhenNextTransactionIdQueryFails() {
    // Arrange
    EvaluateItfRequestEntriesInner entry = buildEntry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        "REF-ERR",
        "4707",
        BigDecimal.TEN,
        "PEN",
        "C"
    );
    EvaluateItfHeaders headers = buildHeaders("Y1", "0000000002");

    EntityManager entityManager = Mockito.mock(EntityManager.class);
    Query selectMaxId = Mockito.mock(Query.class);
    Mockito.doReturn(entityManager).when(repository).getEntityManager();
    Mockito.when(entityManager.createNativeQuery(
            Mockito.contains("SELECT COALESCE(MAX(itf_transaction_id), 0) FROM itf_transaction")))
        .thenReturn(selectMaxId);
    Mockito.when(selectMaxId.getSingleResult()).thenThrow(new RuntimeException("max id failure"));

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.saveItfTxn(headers, entry, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)
    );

    // Assert
    assertNotNull(result.getCause());
  }

  @Test
  @DisplayName("Should update transaction status and reversal flag by transaction id")
  void shouldUpdateTransactionStatusAndReversalFlagByTransactionId() {
    // Arrange
    EntityManager entityManager = Mockito.mock(EntityManager.class);
    Query nativeUpdate = Mockito.mock(Query.class);
    ItfTxnId itfTxnId = new ItfTxnId();
    itfTxnId.itfTransactionId = 35L;
    itfTxnId.accountNumber = "ACC-001";
    itfTxnId.appCode = "APP1";
    itfTxnId.operationNumber = "OPE-0001";

    Mockito.doReturn(entityManager).when(repository).getEntityManager();
    Mockito.when(entityManager.createNativeQuery(Mockito.contains("UPDATE itf_transaction")))
        .thenReturn(nativeUpdate);
    Mockito.when(nativeUpdate.setParameter(Mockito.anyInt(), Mockito.any()))
        .thenReturn(nativeUpdate);
    Mockito.when(nativeUpdate.executeUpdate()).thenReturn(1);

    // Act
    repository.changeItfFinalAccountStatusByTxnId(itfTxnId, ItfFinalAccountStatus.REVERSED);

    // Assert
    Mockito.verify(nativeUpdate).setParameter(1, ItfFinalAccountStatus.REVERSED.name());
    Mockito.verify(nativeUpdate).setParameter(2, true);
    Mockito.verify(nativeUpdate).setParameter(3, 35L);
    Mockito.verify(nativeUpdate).setParameter(4, "ACC-001");
    Mockito.verify(nativeUpdate).setParameter(5, "APP1");
    Mockito.verify(nativeUpdate).setParameter(6, "OPE-0001");
    Mockito.verify(nativeUpdate).executeUpdate();
  }

  @Test
  @DisplayName("Should throw ApiException when status update query fails")
  void shouldThrowApiExceptionWhenStatusUpdateQueryFails() {
    // Arrange
    EntityManager entityManager = Mockito.mock(EntityManager.class);
    Query nativeUpdate = Mockito.mock(Query.class);
    RuntimeException cause = new RuntimeException("update failure");
    ItfTxnId itfTxnId = new ItfTxnId();
    itfTxnId.itfTransactionId = 1L;
    itfTxnId.accountNumber = "ACC";
    itfTxnId.appCode = "APP";
    itfTxnId.operationNumber = "OPN";

    Mockito.doReturn(entityManager).when(repository).getEntityManager();
    Mockito.when(entityManager.createNativeQuery(Mockito.contains("UPDATE itf_transaction")))
        .thenReturn(nativeUpdate);
    Mockito.when(nativeUpdate.setParameter(Mockito.anyInt(), Mockito.any()))
        .thenReturn(nativeUpdate);
    Mockito.when(nativeUpdate.executeUpdate()).thenThrow(cause);

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> repository.changeItfFinalAccountStatusByTxnId(itfTxnId, ItfFinalAccountStatus.REVERSED)
    );

    // Assert
    assertNotNull(result.getCause());
    assertSame(cause, result.getCause());
  }

  @Test
  @DisplayName("Should call delete on transaction entity")
  void shouldCallDeleteOnTransactionEntity() {
    // Arrange
    ItfTxn txn = Mockito.mock(ItfTxn.class);

    // Act
    repository.delete(txn);

    // Assert
    Mockito.verify(txn).delete();
  }

  @Test
  @DisplayName("Should throw ApiException when transaction delete fails")
  void shouldThrowApiExceptionWhenTransactionDeleteFails() {
    // Arrange
    ItfTxn txn = Mockito.mock(ItfTxn.class);
    RuntimeException cause = new RuntimeException("delete failure");
    Mockito.doThrow(cause).when(txn).delete();

    // Act
    ApiException result = assertThrows(ApiException.class, () -> repository.delete(txn));

    // Assert
    assertNotNull(result.getCause());
    assertSame(cause, result.getCause());
  }

  private EvaluateItfRequestEntriesInner buildEntry(
      EvaluateItfRequestEntriesInner.EntryTypeEnum entryType,
      String referenceId,
      String utc,
      BigDecimal amount,
      String currencyAlphaCode,
      String productCode
  ) {
    EvaluateItfRequestEntriesInner entry = Mockito.mock(EvaluateItfRequestEntriesInner.class);
    var meanInfo = Mockito.mock(com.bcp.cnf.services.model.api.MeanInformation.class);
    var productDetail = Mockito.mock(com.bcp.cnf.services.model.api.ProductDetail.class);
    var currency = Mockito.mock(com.bcp.cnf.services.model.api.Currency.class);
    var product = Mockito.mock(com.bcp.cnf.services.model.api.Product.class);

    Mockito.when(entry.getEntryType()).thenReturn(entryType);
    Mockito.when(entry.getUtc()).thenReturn(utc);
    Mockito.when(entry.getAmount()).thenReturn(amount);

    Mockito.when(entry.getMeanInformation()).thenReturn(meanInfo);
    Mockito.when(meanInfo.getReferenceId()).thenReturn(referenceId);
    Mockito.when(meanInfo.getProductDetail()).thenReturn(productDetail);
    Mockito.when(productDetail.getCurrency()).thenReturn(currency);
    Mockito.when(productDetail.getProduct()).thenReturn(product);
    Mockito.when(currency.getAlphaCode()).thenReturn(currencyAlphaCode);
    Mockito.when(product.getCode()).thenReturn(productCode);

    return entry;
  }

  private EvaluateItfHeaders buildHeaders(String appCode, String opnNumber) {
    return new EvaluateItfHeaders(
        "Bearer token",
        "request-id",
        "2021-05-25T17:12:20.509-0400",
        appCode,
        "caller",
        "subscription-key",
        false,
        opnNumber,
        "859",
        "319",
        "7",
        "N",
        "CA",
        "1413"
    );
  }

  private Query mockNativeInsertWithMaxId(long maxId) {
    EntityManager entityManager = Mockito.mock(EntityManager.class);
    Query selectMaxId = Mockito.mock(Query.class);
    Query nativeInsert = Mockito.mock(Query.class);

    Mockito.doReturn(entityManager).when(repository).getEntityManager();
    Mockito.when(entityManager.createNativeQuery(
            Mockito.contains("SELECT COALESCE(MAX(itf_transaction_id), 0) FROM itf_transaction")))
        .thenReturn(selectMaxId);
    Mockito.when(entityManager.createNativeQuery(Mockito.contains("INSERT INTO itf_transaction")))
        .thenReturn(nativeInsert);
    Mockito.when(selectMaxId.getSingleResult()).thenReturn(maxId);
    Mockito.when(nativeInsert.setParameter(Mockito.anyInt(), Mockito.any()))
        .thenReturn(nativeInsert);
    Mockito.when(nativeInsert.executeUpdate()).thenReturn(1);

    return nativeInsert;
  }
}

