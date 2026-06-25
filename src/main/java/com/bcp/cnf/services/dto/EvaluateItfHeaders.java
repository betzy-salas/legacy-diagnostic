package com.bcp.cnf.services.dto;

/**
 * Encapsula los headers requeridos para evaluar cumplimiento ITF.
 */
public record EvaluateItfHeaders(String authorization, String requestId,
                                 String requestDate, String appCode,
                                 String callerName, String ocpApimSubscriptionKey,
                                 Boolean reversalFlag, String opnNumber,
                                 String agencyCode, String branchOfficeCode,
                                 String orgCode, String guildAccountFlag,
                                 String userCode, String serverTerminal) {
}
