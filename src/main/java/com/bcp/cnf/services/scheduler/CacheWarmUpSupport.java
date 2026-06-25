package com.bcp.cnf.services.scheduler;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

/**
 * Utilidad compartida para reducir duplicacion en warm-up de cache.
 */
final class CacheWarmUpSupport {

  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  private CacheWarmUpSupport() {
    // Utility class
  }

  static <T> void warmUp(Logger log, String context, Supplier<List<T>> fetcher,
                         Consumer<T> cacheWriter) {
    try {
      log.info("[" + context + " - warmUpCache] loading records to redis cache");
      List<T> entities = fetcher.get();
      entities.forEach(cacheWriter);
      log.info("[" + context + " - warmUpCache] cache loaded successfully");
    } catch (Exception ex) {
      log.error("[" + context + " - warmUpCache] Error at loading cache", ex);
      throw ApiException.builder()
          .cause(ex)
          .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
          .build();
    }
  }
}

