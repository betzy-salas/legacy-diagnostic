package com.bcp.cnf.services.proxy.redis;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import com.bcp.cnf.services.dto.ItfsRateRedisDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Gestiona la cache Redis de tasas ITF.
 */
@ApplicationScoped
public class ItfRateRedisProxy {

  private static final String KEY_PREFIX = "itfs:rate:";
  private static final Duration TTL = Duration.ofHours(24);
  private static final Logger LOG = Logger.getLogger(ItfRateRedisProxy.class);
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  private final HashCommands<String, String, String> hash;
  private final KeyCommands<String> keys;

  @Inject
  public ItfRateRedisProxy(@RedisClientName("itfs-rate") RedisDataSource redis) {
    this.hash = redis.hash(String.class);
    this.keys = redis.key(String.class);
  }

  private String keyFor(String itf) {
    return KEY_PREFIX + itf;
  }

  /**
   * Obtiene de Redis una tasa ITF por llave.
   *
   * @param itf identificador de tasa
   * @return tasa cacheada o {@code null} si no existe
   */
  public ItfsRateRedisDto get(String itf) {
    String key = keyFor(itf);
    try {
      LOG.info("[ItfRateRedisProxy - get] calling itfRate redis");
      if (!keys.exists(key)) {
        return null;
      }

      Map<String, String> data = hash.hgetall(key);
      return fromMap(data);
    } catch (Exception ex) {
      throw buildRedisException("get", ex);
    }
  }

  /**
   * Inserta o actualiza una tasa ITF en Redis.
   *
   * @param dto tasa a cachear
   */
  public void put(ItfsRateRedisDto dto) {
    String key = keyFor(dto.itfRate());
    try {
      LOG.info("[ItfRateRedisProxy - put] calling itfRate redis");
      hash.hset(key, Map.of(
          "itfRate", dto.itfRate(),
          "TimeUpdateCach", nowTime(),
          "DateUpdateCach", nowDate()
      ));

      keys.expire(key, TTL);
    } catch (Exception ex) {
      throw buildRedisException("put", ex);
    }
  }

  /**
   * Elimina una tasa ITF del cache Redis.
   *
   * @param utc llave de la tasa a eliminar
   */
  public void evict(String utc) {
    try {
      LOG.info("[ItfRateRedisProxy - evict] calling itfRate redis");
      keys.del(keyFor(utc));
    } catch (Exception ex) {
      throw buildRedisException("evict", ex);
    }
  }

  /**
   * Obtiene todas las tasas ITF disponibles en Redis.
   *
   * @return listado de tasas cacheadas
   */
  public List<ItfsRateRedisDto> getAll() {
    try {
      LOG.info("[ItfRateRedisProxy - getAll] calling itfRate redis");
      List<ItfsRateRedisDto> result = new ArrayList<>();
      for (String key : keys.keys(KEY_PREFIX + "*")) {
        Map<String, String> data = hash.hgetall(key);
        ItfsRateRedisDto dto = fromMap(data);
        if (dto != null) {
          result.add(dto);
        }
      }
      return result;
    } catch (Exception ex) {
      throw buildRedisException("getAll", ex);
    }
  }

  private ItfsRateRedisDto fromMap(Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    try {
      return new ItfsRateRedisDto(
          map.get("itfRate"),
          map.get("TimeUpdateCach"),
          map.get("DateUpdateCach")
      );
    } catch (Exception ex) {
      throw buildRedisException("fromMap", ex);
    }
  }

  private ApiException buildRedisException(String methodName, Exception cause) {
    LOG.error("[ItfRateRedisProxy - " + methodName + "] Error at calling itfRate redis", cause);
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
