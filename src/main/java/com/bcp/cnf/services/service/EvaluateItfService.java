package com.bcp.cnf.services.service;

import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.model.api.EvaluateItfRequest;
import com.bcp.cnf.services.model.api.EvaluateItfResponseInner;
import java.util.List;


/**
 * Service interface for evaluating ITF compliance.
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
public interface EvaluateItfService {
  List<EvaluateItfResponseInner> evaluateItf(EvaluateItfHeaders evaluateItfHeaders,
                                             EvaluateItfRequest evaluateItfRequest);
}

