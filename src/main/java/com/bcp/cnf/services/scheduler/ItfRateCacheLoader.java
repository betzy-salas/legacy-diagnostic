package com.bcp.cnf.services.scheduler;

import com.azure.data.tables.models.TableEntity;
import com.bcp.cnf.services.dto.ItfsRateRedisDto;
import com.bcp.cnf.services.proxy.redis.ItfRateRedisProxy;
import com.bcp.cnf.services.proxy.tablestorage.ItfRateTableStorageProxy;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.jboss.logging.Logger;

/**
 * Carga y refresca en Redis el cache de tasas ITF desde Table Storage.
 */
@ApplicationScoped
public class ItfRateCacheLoader {

  private static final Logger LOG = Logger.getLogger(ItfRateCacheLoader.class);

  private final ItfRateRedisProxy redisProxy;

  private final ItfRateTableStorageProxy tableProxy;

  @Inject
  public ItfRateCacheLoader(ItfRateRedisProxy redisProxy, ItfRateTableStorageProxy tableProxy) {
    this.redisProxy = redisProxy;
    this.tableProxy = tableProxy;
  }

  @Startup
  void onStart() {
    LOG.info("[ItfRateCacheLoader - onStart] calling warmUpCache");
    warmUpCache();
  }


  @Scheduled(cron = "0 30 0 * * ?")
  void scheduledWarmUp() {
    LOG.info("[ItfRateCacheLoader - scheduledWarmUp] calling warmUpCache");
    warmUpCache();
  }

  private void warmUpCache() {
    CacheWarmUpSupport.warmUp(
        LOG,
        "ItfRateCacheLoader",
        tableProxy::findAll,
        (TableEntity entity) -> redisProxy.put(new ItfsRateRedisDto(
            String.valueOf(entity.getRowKey()),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        ))
    );
  }
}