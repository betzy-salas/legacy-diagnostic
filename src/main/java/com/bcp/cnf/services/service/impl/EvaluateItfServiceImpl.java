package com.bcp.cnf.services.service.impl;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.model.api.EvaluateItfRequest;
import com.bcp.cnf.services.model.api.EvaluateItfRequestEntriesInner;
import com.bcp.cnf.services.model.api.EvaluateItfResponseInner;
import com.bcp.cnf.services.model.entities.ItfTxn;
import com.bcp.cnf.services.model.entities.PdhBalance;
import com.bcp.cnf.services.model.enums.ItfFinalAccountStatus;
import com.bcp.cnf.services.proxy.redis.CodTxnExoneratedItfRedisProxy;
import com.bcp.cnf.services.proxy.redis.ItfRateRedisProxy;
import com.bcp.cnf.services.proxy.singlestore.ItfTxnRepository;
import com.bcp.cnf.services.proxy.singlestore.PdhBalanceRepository;
import com.bcp.cnf.services.service.EvaluateItfService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Implementation of EvaluateItfService for itf operations.
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
public class EvaluateItfServiceImpl implements EvaluateItfService {

  private final ItfTxnRepository itfTxnRepository;
  private final PdhBalanceRepository pdhBalanceRepository;
  private final CodTxnExoneratedItfRedisProxy utcExoneratedItfRedisProxy;
  private final ItfRateRedisProxy itfRateRedisProxy;

  private static final Logger LOG = Logger.getLogger(EvaluateItfServiceImpl.class);
  private static final BigDecimal MIN_AMOUNT = new BigDecimal("1000");
  private static final BigDecimal DEFAULT_ITF_RATE = BigDecimal.ZERO;
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  /**
   * Crea el servicio con sus dependencias.
   *
   * @param itfTxnRepository repositorio de transacciones ITF
   * @param pdhBalanceRepository repositorio de saldos PDH
   * @param utcExoneratedItfRedisProxy proxy redis de codigos exonerados
   * @param itfRateRedisProxy proxy redis de tasas ITF
   */
  @Inject
  public EvaluateItfServiceImpl(
      ItfTxnRepository itfTxnRepository,
      PdhBalanceRepository pdhBalanceRepository,
      CodTxnExoneratedItfRedisProxy utcExoneratedItfRedisProxy,
      ItfRateRedisProxy itfRateRedisProxy) {
    this.itfTxnRepository = itfTxnRepository;
    this.pdhBalanceRepository = pdhBalanceRepository;
    this.utcExoneratedItfRedisProxy = utcExoneratedItfRedisProxy;
    this.itfRateRedisProxy = itfRateRedisProxy;
  }

  @Override
  public List<EvaluateItfResponseInner> evaluateItf(EvaluateItfHeaders evaluateItfHeaders,
                                                    EvaluateItfRequest evaluateItfRequest) {
    try {
      LOG.info("[EvaluateItfServiceImpl - evaluateItf] evaluating itf operation");
      if (Boolean.TRUE.equals(evaluateItfHeaders.reversalFlag())) {
        LOG.info("[EvaluateItfServiceImpl - evaluateItf] calling processReversal");
        return processReversal(evaluateItfHeaders, evaluateItfRequest);
      }
      LOG.info("[EvaluateItfServiceImpl - evaluateItf] calling processItfCalculation");
      return processItfCalculation(evaluateItfHeaders, evaluateItfRequest);
    } catch (Exception ex) {
      throw buildServiceException("evaluateItf", ex);
    }
  }

  private List<EvaluateItfResponseInner> processReversal(EvaluateItfHeaders headers,
                                                         EvaluateItfRequest request) {
    try {
      LOG.info("[EvaluateItfServiceImpl - processReversal] processing reversal flow");
      List<ItfTxn> txns = itfTxnRepository.findAllWithoutReversalByOpnNumber(headers.opnNumber());
      Map<String, ItfTxn> txnsByEntryKey = new HashMap<>();

      for (var txn : txns) {
        if (txn != null && txn.itfTxnId != null) {
          txnsByEntryKey.put(buildReversalEntryKey(txn.itfTxnId.accountNumber, txn.utc), txn);
        }
      }

      for (var entry : request.getEntries()) {
        String accountNumber = entry.getMeanInformation().getReferenceId();
        String utc = entry.getUtc();
        var txn = txnsByEntryKey.remove(buildReversalEntryKey(accountNumber, utc));

        if (txn == null) {
          continue;
        }

        PdhBalance pdh = pdhBalanceRepository.findByAccountAndUtc(accountNumber, utc);

        if (pdh != null) {
          BigDecimal movement = txn.useMovementAccountPdh != null
              ? txn.useMovementAccountPdh
              : BigDecimal.ZERO;

          pdhBalanceRepository.updateClosingBalanceWhenReversalFlow(pdh, movement);
        }

        itfTxnRepository.changeItfFinalAccountStatusByTxnId(
            txn.itfTxnId,
            ItfFinalAccountStatus.REVERSED
        );
      }
      return List.of(new EvaluateItfResponseInner());
    } catch (Exception ex) {
      throw buildServiceException("processReversal", ex);
    }
  }

  private String buildReversalEntryKey(String accountNumber, String utc) {
    return accountNumber + "|" + utc;
  }

  private List<EvaluateItfResponseInner> processItfCalculation(EvaluateItfHeaders headers,
                                                               EvaluateItfRequest request) {
    try {
      LOG.info("[EvaluateItfServiceImpl - processItfCalculation] processing calculation flow");
      BigDecimal noTaxesAmount = BigDecimal.ZERO;
      BigDecimal itfAmountToPayed;
      BigDecimal itfRate = getItfRate();
      BigDecimal finalBalancePdh;
      boolean flagItf = true;
      List<EvaluateItfResponseInner> response = new ArrayList<>();

      for (var entry : request.getEntries()) {
        String referenceId = entry.getMeanInformation().getReferenceId();
        String utc = entry.getUtc();

        if (EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO.equals(entry.getEntryType())) {
          BigDecimal pdhBalance = obtainPdhBalance(referenceId);
          if (pdhBalance.compareTo(entry.getAmount()) >= 0) {
            noTaxesAmount = entry.getAmount();
          } else {
            noTaxesAmount = pdhBalance;
          }
        }

        if (EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO.equals(entry.getEntryType())) {
          pdhBalanceRepository.updatePdh(referenceId, utc, noTaxesAmount.negate(),
              headers.reversalFlag());
        }

        BigDecimal baseEstimate =
            EvaluateItfRequestEntriesInner.EntryTypeEnum.CARGO.equals(entry.getEntryType())
                ? entry.getAmount().subtract(noTaxesAmount) : entry.getAmount();

        itfAmountToPayed = baseEstimate.divide(MIN_AMOUNT, 0, RoundingMode.DOWN);
        itfAmountToPayed = itfAmountToPayed
            .multiply(MIN_AMOUNT)
            .multiply(itfRate);

        var productType = entry.getMeanInformation().getProductDetail().getProduct().getCode();

        if (verifyExonerationByAccountNumberFormat(entry.getMeanInformation().getReferenceId())
            || verifyItfExoneration(entry.getUtc(), productType)
            || isOwnAccountTransfer(request)) {
          itfAmountToPayed = BigDecimal.ZERO;
          flagItf = false;
        }

        finalBalancePdh = obtainPdhBalance(referenceId);
        var previousBalance = finalBalancePdh.add(noTaxesAmount);

        itfTxnRepository.saveItfTxn(
            headers, entry, itfAmountToPayed, noTaxesAmount, previousBalance);

        response.add(
            new EvaluateItfResponseInner()
                .referenceId(entry.getMeanInformation().getReferenceId())
                .itfRate(itfRate)
                .itfAmount(itfAmountToPayed)
                .finalBalancePdh(finalBalancePdh)
                .applyItfIndicator(flagItf)
        );
      }

      return response;
    } catch (Exception ex) {
      throw buildServiceException("processItfCalculation", ex);
    }
  }

  private BigDecimal obtainPdhBalance(String referenceId) {
    try {
      PdhBalance pdh = pdhBalanceRepository.findByAccountNumber(referenceId);
      return pdh != null && pdh.accountBalanceClosing != null ? pdh.accountBalanceClosing :
          BigDecimal.ZERO;
    } catch (Exception ex) {
      throw buildServiceException("obtainPdhBalance", ex);
    }
  }

  private BigDecimal getItfRate() {
    try {
      var rates = itfRateRedisProxy.getAll();
      if (rates == null || rates.isEmpty()) {
        LOG.warn(
            "[EvaluateItfServiceImpl - getItfRate] ITF rate not found in redis, using default"
        );
        return DEFAULT_ITF_RATE;
      }

      var dto = rates.getFirst();
      if (dto == null || dto.itfRate() == null || dto.itfRate().isBlank()) {
        LOG.warn(
            "[EvaluateItfServiceImpl - getItfRate] ITF rate value is empty, using default"
        );
        return DEFAULT_ITF_RATE;
      }
      return parseItfRateOrDefault(dto.itfRate());
    } catch (Exception ex) {
      throw buildServiceException("getItfRate", ex);
    }
  }

  private BigDecimal parseItfRateOrDefault(String rawItfRate) {
    try {
      return new BigDecimal(rawItfRate);
    } catch (NumberFormatException ex) {
      LOG.warn(
          "[EvaluateItfServiceImpl - getItfRate] Invalid ITF rate format in redis, using default",
          ex
      );
      return DEFAULT_ITF_RATE;
    }
  }

  private boolean verifyExonerationByAccountNumberFormat(String referenceId) {
    if (referenceId == null || referenceId.isBlank()) {
      return false;
    }

    return referenceId.trim().length() != 20;
  }

  private boolean verifyItfExoneration(String utc, String productType) {
    try {
      var exonerated = utcExoneratedItfRedisProxy.get(utc, productType);
      return exonerated != null && !exonerated.affectItf();
    } catch (Exception ex) {
      throw buildServiceException("verifyItfExoneration", ex);
    }
  }

  private boolean isOwnAccountTransfer(EvaluateItfRequest request) {
    return EvaluateItfRequest.TransferIndicatorTypeEnum.CP.equals(
        request.getTransferIndicatorType());
  }

  private ApiException buildServiceException(String methodName, Exception cause) {
    LOG.error("[EvaluateItfServiceImpl - " + methodName + "] Error at evaluating itf service",
        cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
  }
}
