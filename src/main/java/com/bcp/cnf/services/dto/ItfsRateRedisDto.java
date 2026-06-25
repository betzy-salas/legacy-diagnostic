package com.bcp.cnf.services.dto;

/**
 * Representa una tasa ITF cacheada en Redis.
 */
public record ItfsRateRedisDto(
    String itfRate,
    String timeUpdateCach,
    String dateUpdateCach
) {
}
