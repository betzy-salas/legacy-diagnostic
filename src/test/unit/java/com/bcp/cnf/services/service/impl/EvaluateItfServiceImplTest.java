package com.bcp.cnf.services.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.cnf.services.dto.CodTxnExoneratedItfRedisDto;
import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.dto.ItfsRateRedisDto;
import com.bcp.cnf.services.model.api.EvaluateItfRequest;
import com.bcp.cnf.services.model.api.EvaluateItfRequestEntriesInner;
import com.bcp.cnf.services.model.api.EvaluateItfResponseInner;
import com.bcp.cnf.services.model.entities.ItfTxn;
import com.bcp.cnf.services.model.entities.ItfTxnId;
import com.bcp.cnf.services.model.entities.PdhBalance;
import com.bcp.cnf.services.model.enums.ItfFinalAccountStatus;
import com.bcp.cnf.services.proxy.redis.CodTxnExoneratedItfRedisProxy;
import com.bcp.cnf.services.proxy.redis.ItfRateRedisProxy;
import com.bcp.cnf.services.proxy.singlestore.ItfTxnRepository;
import com.bcp.cnf.services.proxy.singlestore.PdhBalanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluateItfServiceImplTest {

  @Mock
  private ItfTxnRepository itfTxnRepository;
  @Mock
  private PdhBalanceRepository pdhBalanceRepository;
  @Mock
  private CodTxnExoneratedItfRedisProxy utcExoneratedItfRedisProxy;
  @Mock
  private ItfRateRedisProxy itfRateRedisProxy;

  @InjectMocks
  private EvaluateItfServiceImpl service;

  @Test
  @DisplayName("Should process reversal and update PDH with zero movement when transaction movement is null")
  void shouldProcessReversalAndUpdatePdhWithZeroMovementWhenTransactionMovementIsNull() {
    // Arrange
    EvaluateItfHeaders headers = headers(true, "OPN-001");
    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        "12345678901234567890",
        "1001",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    ItfTxn txn = buildTxnForReversal("OPN-001", "12345678901234567890", "1001");
    txn.useMovementAccountPdh = null;
    PdhBalance pdh = new PdhBalance();

    when(itfTxnRepository.findAllWithoutReversalByOpnNumber("OPN-001")).thenReturn(List.of(txn));
    when(pdhBalanceRepository.findByAccountAndUtc("12345678901234567890", "1001")).thenReturn(pdh);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers, request);

    // Assert
    assertEquals(1, result.size());
    verify(pdhBalanceRepository).updateClosingBalanceWhenReversalFlow(pdh, BigDecimal.ZERO);
    verify(itfTxnRepository).changeItfFinalAccountStatusByTxnId(
        txn.itfTxnId,
        ItfFinalAccountStatus.REVERSED);
  }

  @Test
  @DisplayName("Should process reversal and update PDH with movement amount when transaction movement exists")
  void shouldProcessReversalAndUpdatePdhWithMovementAmountWhenTransactionMovementExists() {
    // Arrange
    EvaluateItfHeaders headers = headers(true, "OPN-002");
    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        "12345678901234567890",
        "1002",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    ItfTxn txn = buildTxnForReversal("OPN-002", "12345678901234567890", "1002");
    txn.useMovementAccountPdh = new BigDecimal("25.50");
    PdhBalance pdh = new PdhBalance();

    when(itfTxnRepository.findAllWithoutReversalByOpnNumber("OPN-002")).thenReturn(List.of(txn));
    when(pdhBalanceRepository.findByAccountAndUtc("12345678901234567890", "1002")).thenReturn(pdh);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers, request);

    // Assert
    assertEquals(1, result.size());
    verify(pdhBalanceRepository).updateClosingBalanceWhenReversalFlow(pdh, new BigDecimal("25.50"));
    verify(itfTxnRepository).changeItfFinalAccountStatusByTxnId(
        txn.itfTxnId,
        ItfFinalAccountStatus.REVERSED);
  }

  @Test
  @DisplayName("Should process reversal and skip PDH update when balance does not exist")
  void shouldProcessReversalAndSkipPdhUpdateWhenBalanceDoesNotExist() {
    // Arrange
    EvaluateItfHeaders headers = headers(true, "OPN-002");
    EvaluateItfRequestEntriesInner firstEntry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        "12345678901234567890",
        "1002",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequestEntriesInner secondEntry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        "12345678901234567890",
        "1003",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(
        List.of(firstEntry, secondEntry),
        EvaluateItfRequest.TransferIndicatorTypeEnum.TX
    );

    ItfTxn txn = buildTxnForReversal("OPN-002", "12345678901234567890", "1002");
    txn.useMovementAccountPdh = new BigDecimal("25.50");

    when(itfTxnRepository.findAllWithoutReversalByOpnNumber("OPN-002"))
        .thenReturn(List.of(txn));
    when(pdhBalanceRepository.findByAccountAndUtc("12345678901234567890", "1002")).thenReturn(null);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers, request);

    // Assert
    assertEquals(1, result.size());
    verify(pdhBalanceRepository, never()).updateClosingBalanceWhenReversalFlow(any(), any());
    verify(itfTxnRepository).changeItfFinalAccountStatusByTxnId(
        txn.itfTxnId,
        ItfFinalAccountStatus.REVERSED);
  }

  @Test
  @DisplayName("Should wrap exception when reversal processing fails")
  void shouldWrapExceptionWhenReversalProcessingFails() {
    // Arrange
    EvaluateItfHeaders headers = headers(true, "OPN-003");
    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        "12345678901234567890",
        "1004",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    when(itfTxnRepository.findAllWithoutReversalByOpnNumber("OPN-003"))
        .thenThrow(new RuntimeException("single store failure"));

    // Act
    ApiException result = assertThrows(ApiException.class, () -> service.evaluateItf(headers, request));

    // Assert
    assertNotNull(result.getCause());
  }

  @Test
  @DisplayName("Should ignore null transactions and transactions without id in reversal map building")
  void shouldIgnoreNullTransactionsAndTransactionsWithoutIdInReversalMapBuilding() {
    // Arrange
    EvaluateItfHeaders headers = headers(true, "OPN-004");
    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        "12345678901234567890",
        "1005",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    ItfTxn txnWithoutId = new ItfTxn();
    txnWithoutId.itfTxnId = null;
    txnWithoutId.utc = "1005";

    when(itfTxnRepository.findAllWithoutReversalByOpnNumber("OPN-004"))
        .thenReturn(Arrays.asList(null, txnWithoutId));

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers, request);

    // Assert
    assertEquals(1, result.size());
    verify(pdhBalanceRepository, never()).findByAccountAndUtc(any(), any());
    verify(pdhBalanceRepository, never()).updateClosingBalanceWhenReversalFlow(any(), any());
    verify(itfTxnRepository, never()).changeItfFinalAccountStatusByTxnId(any(), any());
  }

  @Test
  @DisplayName("Should calculate zero ITF for cargo when PDH balance covers full amount")
  void shouldCalculateZeroItfForCargoWhenPdhBalanceCoversFullAmount() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");
    mockPdhBalance(referenceId, new BigDecimal("1000"));
    when(utcExoneratedItfRedisProxy.get("2001", "1")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        referenceId,
        "2001",
        "1",
        new BigDecimal("800")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-010"), request);

    // Assert
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getItfAmount()));
    assertTrue(result.get(0).getApplyItfIndicator());
    verify(pdhBalanceRepository).updatePdh(referenceId, "2001", new BigDecimal("-800"), false);
    verify(itfTxnRepository).saveItfTxn(any(), any(), any(), any(), eq(new BigDecimal("1800")));
  }

  @Test
  @DisplayName("Should calculate ITF for cargo when PDH balance is lower than entry amount")
  void shouldCalculateItfForCargoWhenPdhBalanceIsLowerThanEntryAmount() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");
    mockPdhBalance(referenceId, new BigDecimal("5100"));
    when(utcExoneratedItfRedisProxy.get("2002", "1")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        referenceId,
        "2002",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-011"), request);

    // Assert
    assertEquals(0, new BigDecimal("0.05").compareTo(result.get(0).getItfAmount()));
    assertTrue(result.get(0).getApplyItfIndicator());
    assertEquals(0, new BigDecimal("5100").compareTo(result.get(0).getFinalBalancePdh()));
  }

  @Test
  @DisplayName("Should calculate ITF for abono and avoid PDH update")
  void shouldCalculateItfForAbonoAndAvoidPdhUpdate() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");
    mockPdhBalance(referenceId, new BigDecimal("5100"));
    when(utcExoneratedItfRedisProxy.get("2003", "1")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        referenceId,
        "2003",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-012"), request);

    // Assert
    assertEquals(0, new BigDecimal("0.30").compareTo(result.get(0).getItfAmount()));
    assertTrue(result.get(0).getApplyItfIndicator());
    verify(pdhBalanceRepository, never()).updatePdh(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Should set ITF amount to zero when account number format is exonerated")
  void shouldSetItfAmountToZeroWhenAccountNumberFormatIsExonerated() {
    // Arrange
    mockItfRate("0.00005");
    when(pdhBalanceRepository.findByAccountNumber("ABC123")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        "ABC123",
        "2004",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-013"), request);

    // Assert
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getItfAmount()));
    assertFalse(result.get(0).getApplyItfIndicator());
    verify(utcExoneratedItfRedisProxy, never()).get(any(), any());
  }

  @Test
  @DisplayName("Should apply ITF when account number is blank and UTC is not exonerated")
  void shouldApplyItfWhenAccountNumberIsBlankAndUtcIsNotExonerated() {
    // Arrange
    mockItfRate("0.00005");
    when(pdhBalanceRepository.findByAccountNumber("   ")).thenReturn(null);
    when(utcExoneratedItfRedisProxy.get("2005", "1")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        "   ",
        "2005",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-014"), request);

    // Assert
    assertEquals(0, new BigDecimal("0.30").compareTo(result.get(0).getItfAmount()));
    assertTrue(result.get(0).getApplyItfIndicator());
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getFinalBalancePdh()));
  }

  @Test
  @DisplayName("Should apply ITF when trimmed account number length is exactly twenty")
  void shouldApplyItfWhenTrimmedAccountNumberLengthIsExactlyTwenty() {
    // Arrange
    String referenceId = " 12345678901234567890 ";
    mockItfRate("0.00005");
    when(pdhBalanceRepository.findByAccountNumber(referenceId)).thenReturn(null);
    when(utcExoneratedItfRedisProxy.get("2006", "1")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        referenceId,
        "2006",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-015"), request);

    // Assert
    assertEquals(0, new BigDecimal("0.30").compareTo(result.get(0).getItfAmount()));
    assertTrue(result.get(0).getApplyItfIndicator());
  }

  @Test
  @DisplayName("Should set ITF amount to zero when UTC exoneration indicates no ITF affectation")
  void shouldSetItfAmountToZeroWhenUtcExonerationIndicatesNoItfAffectation() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");
    mockPdhBalance(referenceId, new BigDecimal("5100"));
    when(utcExoneratedItfRedisProxy.get("2007", "1")).thenReturn(
        new CodTxnExoneratedItfRedisDto("2007", "1", false, LocalTime.of(10, 0), LocalDate.of(2026, 5, 20))
    );

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        referenceId,
        "2007",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-016"), request);

    // Assert
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getItfAmount()));
    assertFalse(result.get(0).getApplyItfIndicator());
  }

  @Test
  @DisplayName("Should set ITF amount to zero when transfer indicator is own account")
  void shouldSetItfAmountToZeroWhenTransferIndicatorIsOwnAccount() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");
    mockPdhBalance(referenceId, new BigDecimal("5100"));
    when(utcExoneratedItfRedisProxy.get("2008", "1")).thenReturn(
        new CodTxnExoneratedItfRedisDto("2008", "1", true, LocalTime.of(11, 0), LocalDate.of(2026, 5, 20))
    );

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        referenceId,
        "2008",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.CP);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-017"), request);

    // Assert
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getItfAmount()));
    assertFalse(result.get(0).getApplyItfIndicator());
  }

  @Test
  @DisplayName("Should use default ITF rate when ITF rate list is empty")
  void shouldUseDefaultItfRateWhenItfRateListIsEmpty() {
    // Arrange
    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        "12345678901234567890",
        "2009",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);
    EvaluateItfHeaders headers = headers(false, "OPN-018");

    when(itfRateRedisProxy.getAll()).thenReturn(List.of());

    when(pdhBalanceRepository.findByAccountNumber("12345678901234567890")).thenReturn(null);
    when(utcExoneratedItfRedisProxy.get("2009", "1")).thenReturn(null);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers, request);

    // Assert
    assertEquals(1, result.size());
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getItfRate()));
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getItfAmount()));
  }

  @Test
  @DisplayName("Should wrap exception when PDH balance retrieval fails")
  void shouldWrapExceptionWhenPdhBalanceRetrievalFails() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        referenceId,
        "2010",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);
    EvaluateItfHeaders headers = headers(false, "OPN-019");

    when(pdhBalanceRepository.findByAccountNumber(referenceId))
        .thenThrow(new RuntimeException("pdh lookup error"));

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> service.evaluateItf(headers, request)
    );

    // Assert
    assertNotNull(result.getCause());
  }

  @Test
  @DisplayName("Should wrap exception when UTC exoneration lookup fails")
  void shouldWrapExceptionWhenUtcExonerationLookupFails() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");
    mockPdhBalance(referenceId, new BigDecimal("100"));

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        referenceId,
        "2011",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);
    EvaluateItfHeaders headers = headers(false, "OPN-020");

    when(utcExoneratedItfRedisProxy.get("2011", "1"))
        .thenThrow(new RuntimeException("redis lookup error"));

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> service.evaluateItf(headers, request)
    );

    // Assert
    assertNotNull(result.getCause());
  }

  @Test
  @DisplayName("Should apply ITF when account reference is null because format exoneration is false")
  void shouldApplyItfWhenAccountReferenceIsNullBecauseFormatExonerationIsFalse() {
    // Arrange
    mockItfRate("0.00005");
    when(pdhBalanceRepository.findByAccountNumber(null)).thenReturn(null);
    when(utcExoneratedItfRedisProxy.get("2012", "1")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.ABONO,
        null,
        "2012",
        "1",
        new BigDecimal("6100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-021"), request);

    // Assert
    assertEquals(0, new BigDecimal("0.30").compareTo(result.get(0).getItfAmount()));
    assertTrue(result.get(0).getApplyItfIndicator());
  }

  @Test
  @DisplayName("Should return zero PDH balance when PDH row exists with null closing balance")
  void shouldReturnZeroPdhBalanceWhenPdhRowExistsWithNullClosingBalance() {
    // Arrange
    String referenceId = "12345678901234567890";
    mockItfRate("0.00005");

    PdhBalance pdh = new PdhBalance();
    pdh.accountBalanceClosing = null;
    when(pdhBalanceRepository.findByAccountNumber(referenceId)).thenReturn(pdh);
    when(utcExoneratedItfRedisProxy.get("2013", "1")).thenReturn(null);

    EvaluateItfRequestEntriesInner entry = entry(
        EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO,
        referenceId,
        "2013",
        "1",
        new BigDecimal("100")
    );
    EvaluateItfRequest request = request(List.of(entry), EvaluateItfRequest.TransferIndicatorTypeEnum.TX);

    // Act
    List<EvaluateItfResponseInner> result = service.evaluateItf(headers(false, "OPN-022"), request);

    // Assert
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getFinalBalancePdh()));
    verify(pdhBalanceRepository).updatePdh(referenceId, "2013", BigDecimal.ZERO, false);
  }

  private EvaluateItfHeaders headers(Boolean reversalFlag, String opnNumber) {
    return new EvaluateItfHeaders(
        "authorization",
        "request-id",
        "request-date",
        "app-code",
        "caller",
        "subscription",
        reversalFlag,
        opnNumber,
        "agency",
        "branch",
        "org",
        "guild",
        "user",
        "terminal"
    );
  }

  private EvaluateItfRequest request(
      List<EvaluateItfRequestEntriesInner> entries,
      EvaluateItfRequest.TransferIndicatorTypeEnum transferIndicatorType) {

    EvaluateItfRequest request = org.mockito.Mockito.mock(EvaluateItfRequest.class);
    when(request.getEntries()).thenReturn(entries);
    when(request.getTransferIndicatorType()).thenReturn(transferIndicatorType);
    return request;
  }

  private ItfTxn buildTxnForReversal(String opnNumber, String accountNumber, String utc) {
    ItfTxn txn = new ItfTxn();
    ItfTxnId id = new ItfTxnId();
    id.itfTransactionId = 1L;
    id.accountNumber = accountNumber;
    id.appCode = "app";
    id.operationNumber = opnNumber;
    txn.itfTxnId = id;
    txn.utc = utc;
    return txn;
  }

  private EvaluateItfRequestEntriesInner entry(
      EvaluateItfRequestEntriesInner.EntryTypeEnum entryType,
      String referenceId,
      String utc,
      String productCode,
      BigDecimal amount) {

    EvaluateItfRequestEntriesInner entry = org.mockito.Mockito.mock(EvaluateItfRequestEntriesInner.class);
    com.bcp.cnf.services.model.api.MeanInformation meanInfo =
        org.mockito.Mockito.mock(com.bcp.cnf.services.model.api.MeanInformation.class);
    com.bcp.cnf.services.model.api.ProductDetail productDetail =
        org.mockito.Mockito.mock(com.bcp.cnf.services.model.api.ProductDetail.class);
    com.bcp.cnf.services.model.api.Product product =
        org.mockito.Mockito.mock(com.bcp.cnf.services.model.api.Product.class);

    when(entry.getEntryType()).thenReturn(entryType);
    when(entry.getAmount()).thenReturn(amount);
    when(entry.getUtc()).thenReturn(utc);
    when(entry.getMeanInformation()).thenReturn(meanInfo);

    when(meanInfo.getReferenceId()).thenReturn(referenceId);
    when(meanInfo.getProductDetail()).thenReturn(productDetail);
    when(productDetail.getProduct()).thenReturn(product);
    when(product.getCode()).thenReturn(productCode);

    return entry;
  }

  private void mockItfRate(String rate) {
    when(itfRateRedisProxy.getAll())
        .thenReturn(List.of(new ItfsRateRedisDto(rate, "10:00:00", "2026-05-20")));
  }

  private void mockPdhBalance(String referenceId, BigDecimal closingBalance) {
    PdhBalance pdh = new PdhBalance();
    pdh.accountBalanceClosing = closingBalance;
    when(pdhBalanceRepository.findByAccountNumber(referenceId)).thenReturn(pdh);
  }
}
