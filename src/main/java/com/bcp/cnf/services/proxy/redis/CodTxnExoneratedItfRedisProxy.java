package com.bcp.cnf.services.proxy.redis;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import com.bcp.cnf.services.dto.CodTxnExoneratedItfRedisDto;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Gestiona la cache Redis de codigos de transaccion ITF exonerados.
 */
@ApplicationScoped
public class CodTxnExoneratedItfRedisProxy {

  private static final String KEY_PREFIX = "utcs:exoneradas:itf:";
  private static final Duration TTL = Duration.ofHours(24);
  private static final Logger LOG = Logger.getLogger(CodTxnExoneratedItfRedisProxy.class);
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  private final HashCommands<String, String, String> hash;
  private final KeyCommands<String> keys;

  /**
   * Crea el proxy usando el datasource Redis configurado.
   *
   * @param redis datasource del cliente Redis de codigos exonerados
   */
  @Inject
  public CodTxnExoneratedItfRedisProxy(
      @RedisClientName("utcs-exoneradas-itf") RedisDataSource redis) {

    this.hash = redis.hash(String.class);
    this.keys = redis.key(String.class);
  }

  private String keyFor(String utc, String productType) {
    return KEY_PREFIX + utc + ":" + productType;
  }

  /**
   * Obtiene de Redis el valor cacheado para una combinacion UTC y producto.
   *
   * @param utc         codigo UTC
   * @param productType tipo de producto
   * @return dto cacheado o {@code null} cuando no existe
   */
  public CodTxnExoneratedItfRedisDto get(String utc, String productType) {
    String key = keyFor(utc, productType);

    try {
      LOG.info("[CodTxnExoneratedItfRedisProxy - get] calling codTxnExoneratedItf redis");
      Map<String, String> data = hash.hgetall(key);
      return fromMap(data);
    } catch (Exception ex) {
      throw buildRedisException("get", ex);
    }
  }

  /**
   * Guarda o actualiza en Redis un codigo exonerado.
   *
   * @param dto informacion a cachear
   */
  public void put(CodTxnExoneratedItfRedisDto dto) {
    String key = keyFor(dto.utc(), dto.productType());

    try {
      LOG.info("[CodTxnExoneratedItfRedisProxy - put] calling codTxnExoneratedItf redis");
      hash.hset(key, Map.of(
          "utc", dto.utc(),
          "productType", dto.productType(),
          "affectItf", dto.affectItf().toString(),
          "timeUpdateCach", nowTime(),
          "dateUpdateCach", nowDate()
      ));

      keys.expire(key, TTL);
    } catch (Exception ex) {
      throw buildRedisException("put", ex);
    }
  }

  /**
   * Elimina del cache el codigo exonerado para una combinacion UTC/producto.
   *
   * @param utc         codigo UTC
   * @param productType tipo de producto
   */
  public void evict(String utc, String productType) {
    String key = keyFor(utc, productType);
    try {
      LOG.info("[CodTxnExoneratedItfRedisProxy - evict] calling codTxnExoneratedItf redis");
      keys.del(key);
    } catch (Exception ex) {
      throw buildRedisException("evict", ex);
    }
  }

  private CodTxnExoneratedItfRedisDto fromMap(Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }

    try {
      return new CodTxnExoneratedItfRedisDto(
          map.get("utc"),
          map.get("productType"),
          Boolean.valueOf(map.get("affectItf")),
          LocalTime.parse(map.get("timeUpdateCach"), DateTimeFormatter.ofPattern("HH:mm:ss")),
          LocalDate.parse(map.get("dateUpdateCach"))
      );
    } catch (Exception ex) {
      throw buildRedisException("fromMap", ex);
    }
  }

  private ApiException buildRedisException(String methodName, Exception cause) {
    LOG.error("[CodTxnExoneratedItfRedisProxy - " + methodName + "] Error at calling "
        + "codTxnExoneratedItf redis", cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
  }

  private String nowTime() {
    return LocalTime.now()
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
  }

  private String nowDate() {
    return LocalDate.now()
        .format(DateTimeFormatter.ISO_DATE);
  }
}
