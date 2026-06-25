package com.bcp.cnf.services.scheduler;

import com.azure.data.tables.models.TableEntity;
import com.bcp.cnf.services.dto.CodTxnExoneratedItfRedisDto;
import com.bcp.cnf.services.proxy.redis.CodTxnExoneratedItfRedisProxy;
import com.bcp.cnf.services.proxy.tablestorage.ExoneratedCodTxnItfTableStorageProxy;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.jboss.logging.Logger;

/**
 * Carga y refresca en Redis el cache de codigos de transaccion ITF exonerados.
 */
@ApplicationScoped
public class CodTxnExoneratedItfCacheLoader {

  private static final Logger LOG = Logger.getLogger(CodTxnExoneratedItfCacheLoader.class);

  private final CodTxnExoneratedItfRedisProxy redisProxy;

  private final ExoneratedCodTxnItfTableStorageProxy tableProxy;

  @Inject
  public CodTxnExoneratedItfCacheLoader(
      CodTxnExoneratedItfRedisProxy redisProxy,
      ExoneratedCodTxnItfTableStorageProxy tableProxy) {
    this.redisProxy = redisProxy;
    this.tableProxy = tableProxy;
  }

  @Startup
  void onStart() {
    LOG.info("[CodTxnExoneratedItfCacheLoader - onStart] calling warmUpCache");
    warmUpCache();
  }

  @Scheduled(cron = "0 30 0 * * ?")
  void scheduledWarmUp() {
    LOG.info("[CodTxnExoneratedItfCacheLoader - scheduledWarmUp] calling warmUpCache");
    warmUpCache();
  }

  private void warmUpCache() {
    CacheWarmUpSupport.warmUp(
        LOG,
        "CodTxnExoneratedItfCacheLoader",
        tableProxy::findAll,
        (TableEntity entity) -> redisProxy.put(new CodTxnExoneratedItfRedisDto(
            String.valueOf(entity.getProperties().get("RowKey")),
            entity.getProperties().get("productType").toString(),
            Boolean.valueOf(entity.getProperties().get("affectItf").toString()),
            LocalTime.parse(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))),
            LocalDate.parse(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
        ))
    );
  }
}