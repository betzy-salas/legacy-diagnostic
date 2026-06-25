package com.bcp.cnf.services.expose.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.model.api.EvaluateItfRequest;
import com.bcp.cnf.services.model.api.EvaluateItfResponseInner;
import com.bcp.cnf.services.service.EvaluateItfService;
import java.util.List;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionKeepingApiImplTest {

  @Mock
  private EvaluateItfService evaluateItfService;

  private PositionKeepingApiImpl api;

  @BeforeEach
  void setUp() {
    api = new PositionKeepingApiImpl(evaluateItfService);
  }

  @Test
  @DisplayName("Should build headers from incoming parameters and delegate request to service")
  void shouldBuildHeadersAndDelegateToService() {
    // Arrange
    String authorization = "auth";
    String requestId = "req-id";
    String requestDate = "2026-01-01";
    String appCode = "APP";
    String callerName = "CALLER";
    String ocpApimSubscriptionKey = "SUB";
    Boolean reversalFlag = false;
    String opnNumber = "OPN";
    String agencyCode = "AGENCY";
    String branchOfficeCode = "BRANCH";
    String orgCode = "ORG";
    String guildAccountFlag = "N";
    String userCode = "USER";
    String serverTerminal = "TERM";
    EvaluateItfRequest request = new EvaluateItfRequest();
    List<EvaluateItfResponseInner> serviceResponse = List.of(new EvaluateItfResponseInner());
    when(evaluateItfService.evaluateItf(any(EvaluateItfHeaders.class), same(request)))
        .thenReturn(serviceResponse);

    // Act
    try (Response response = api.evaluateItfCompliance(
        authorization,
        requestId,
        requestDate,
        appCode,
        callerName,
        ocpApimSubscriptionKey,
        reversalFlag,
        opnNumber,
        agencyCode,
        branchOfficeCode,
        orgCode,
        guildAccountFlag,
        request,
        userCode,
        serverTerminal)) {

      // Assert
      assertEquals(200, response.getStatus());
      assertSame(serviceResponse, response.getEntity());
    }

    ArgumentCaptor<EvaluateItfHeaders> headersCaptor =
        ArgumentCaptor.forClass(EvaluateItfHeaders.class);
    ArgumentCaptor<EvaluateItfRequest> requestCaptor =
        ArgumentCaptor.forClass(EvaluateItfRequest.class);

    verify(evaluateItfService).evaluateItf(headersCaptor.capture(), requestCaptor.capture());

    EvaluateItfHeaders headers = headersCaptor.getValue();
    assertSame(request, requestCaptor.getValue());
    assertEquals(authorization, headers.authorization());
    assertEquals(requestId, headers.requestId());
    assertEquals(requestDate, headers.requestDate());
    assertEquals(appCode, headers.appCode());
    assertEquals(callerName, headers.callerName());
    assertEquals(ocpApimSubscriptionKey, headers.ocpApimSubscriptionKey());
    assertEquals(reversalFlag, headers.reversalFlag());
    assertEquals(opnNumber, headers.opnNumber());
    assertEquals(agencyCode, headers.agencyCode());
    assertEquals(branchOfficeCode, headers.branchOfficeCode());
    assertEquals(orgCode, headers.orgCode());
    assertEquals(guildAccountFlag, headers.guildAccountFlag());
    assertEquals(userCode, headers.userCode());
    assertEquals(serverTerminal, headers.serverTerminal());
  }

  @Test
  @DisplayName("Should return HTTP 204 with empty body when reversal flag is true")
  void shouldReturnNoContentWhenReversalFlagIsTrue() {
    // Arrange
    EvaluateItfRequest request = new EvaluateItfRequest();
    List<EvaluateItfResponseInner> serviceResponse = List.of(new EvaluateItfResponseInner());
    when(evaluateItfService.evaluateItf(any(EvaluateItfHeaders.class), same(request)))
        .thenReturn(serviceResponse);

    // Act
    try (Response response = api.evaluateItfCompliance(
        "auth", "id", "date", "app", "caller", "key",
        true, "opn", "agency", "branch", "org", "N",
        request, "user", "terminal")) {

      // Assert
      assertEquals(204, response.getStatus());
      assertNull(response.getEntity());
    }
    verify(evaluateItfService).evaluateItf(any(EvaluateItfHeaders.class), same(request));
  }

  @Test
  @DisplayName("Should return HTTP 200 when reversal flag is null")
  void shouldReturnOkWhenReversalFlagIsNull() {
    // Arrange
    EvaluateItfRequest request = new EvaluateItfRequest();
    List<EvaluateItfResponseInner> serviceResponse = List.of(new EvaluateItfResponseInner());
    when(evaluateItfService.evaluateItf(any(EvaluateItfHeaders.class), same(request)))
        .thenReturn(serviceResponse);

    // Act
    try (Response response = api.evaluateItfCompliance(
        "auth", "id", "date", "app", "caller", "key",
        null, "opn", "agency", "branch", "org", "N",
        request, "user", "terminal")) {

      // Assert
      assertEquals(200, response.getStatus());
      assertSame(serviceResponse, response.getEntity());
    }
    verify(evaluateItfService).evaluateItf(any(EvaluateItfHeaders.class), same(request));
  }
}