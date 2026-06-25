package com.bcp.cnf.services.expose.web;


import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.model.api.EvaluateItfRequest;
import com.bcp.cnf.services.model.api.EvaluateItfResponseInner;
import com.bcp.cnf.services.service.EvaluateItfService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Class Implement API contract first.<br/>
 * <b>Class</b>: PositionKeepingApiImpl for evaluate itf, create movement
 * interface and obtain sas token <br/>
 * <b>Copyright</b>: &copy; 2026 Banco de Cr&eacute;dito del Per&uacute;<br/>
 * <b>Company</b>: Banco de Cr&eacute;dito del Per&uacute;<br/>
 *
 * @author Banco de Cr&eacute;dito del Per&uacute; (BCP) <br/>
 *     <u>Service Provider</u>: BCP <br/>
 *     <u>Developed by</u>:
 *     <ul>
 *     <li>Franco Carrillo</li>
 *     </ul>
 *     <u>Changes</u>:<br/>
 *     <ul>
 *     <li>Febrero 24, 2026 Creaci&oacute;n de Clase.</li>
 *     </ul>
 * @version 1.0
 */
@ApplicationScoped
public class PositionKeepingApiImpl implements PositionKeepingsApi {

  private final EvaluateItfService evaluateItfService;

  public PositionKeepingApiImpl(EvaluateItfService evaluateItfService) {
    this.evaluateItfService = evaluateItfService;
  }

  @Override
  public Response evaluateItfCompliance(String authorization,
                                        String requestId, String requestDate,
                                        String appCode, String callerName,
                                        String ocpApimSubscriptionKey,
                                        Boolean reversalFlag,
                                        String opnNumber, String agencyCode,
                                        String branchOfficeCode,
                                        String orgCode,
                                        String guildAccountFlag,
                                        EvaluateItfRequest evaluateItfRequest,
                                        String userCode,
                                        String serverTerminal) {

    var headers = new EvaluateItfHeaders(authorization, requestId, requestDate, appCode, callerName,
        ocpApimSubscriptionKey, reversalFlag, opnNumber, agencyCode, branchOfficeCode, orgCode,
        guildAccountFlag, userCode, serverTerminal);

    var response = evaluateItfService.evaluateItf(headers, evaluateItfRequest);

    return Boolean.TRUE.equals(headers.reversalFlag()) ? Response.noContent().build()
        : Response.ok(response).build();

  }
}