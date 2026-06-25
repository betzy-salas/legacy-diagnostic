package com.bcp.cnf.services.utils;

import com.bcp.cnf.services.dto.CodTxnExoneratedItfRedisDto;
import com.bcp.cnf.services.dto.EvaluateItfHeaders;
import com.bcp.cnf.services.dto.ItfsRateRedisDto;
import java.time.LocalDate;
import java.time.LocalTime;

public class ItfEstimateMockUtils {

  public static final String utc_withItfExonerated_andAffectPdhBalance = "4707";
  public static final String utc_withItfExonerated_andNoAffectPdhBalance = "5673";
  public static final String utc_withItfNoExonerated_andAffectPdhBalance = "2002";
  public static final String utc_withItfNoExonerated_andNoAffectPdhBalance = "3565";

  public static final String ITF = "0.00005";

  public static EvaluateItfHeaders getEvaluateItfHeaders_withNoExoneration() {
    return new EvaluateItfHeaders(
        "Bearer sometoken",
        "550e8400-e29b-41d4-a716-446655440076",
        "2021-05-25T17:12:20.509-0400",
        "Y1",
        "UxName1",
        "3cfdad6e03c24d0ab7112dce75cdba35",
        false,
        "00000001",
        "859",
        "319",
        "7",
        "N",
        "CA",
        "1413"
    );
  }

  public static CodTxnExoneratedItfRedisDto getCodTxnExoneratedItfRedisDto_withItfExonerated_andAffectPdhBalance() {
    return new CodTxnExoneratedItfRedisDto(
        utc_withItfExonerated_andAffectPdhBalance,
        "234",
        true,
        LocalTime.now(),
        LocalDate.now()
    );
  }

  public static ItfsRateRedisDto getItfsRateRedisDto() {
    return new ItfsRateRedisDto(
        ITF,
        LocalTime.now().toString(),
        LocalDate.now().toString()
    );
  }
}
