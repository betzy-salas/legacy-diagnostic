package com.bcp.cnf.services.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Representa el valor cacheado en Redis para un UTC exonerado de ITF.
 */
public record CodTxnExoneratedItfRedisDto(
    String utc,
    String productType,
    Boolean affectItf,
    LocalTime timeUpdateCach,
    LocalDate dateUpdateCach
) {
}
