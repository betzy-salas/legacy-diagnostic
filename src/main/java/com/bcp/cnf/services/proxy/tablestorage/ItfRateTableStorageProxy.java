package com.bcp.cnf.services.proxy.tablestorage;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.atla.fwk.core.error.ExceptionCategoryTypes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Proxy para acceder a la tabla de tasas ITF en Azure Table Storage.
 */
@ApplicationScoped
public class ItfRateTableStorageProxy {

  private static final Logger LOG = Logger.getLogger(ItfRateTableStorageProxy.class);
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  @Named("itfRates")
  TableClient itfRates;

  /**
   * Obtiene una tasa ITF por su llave.
   *
   * @param itf identificador de tasa ITF
   * @return entidad encontrada
   */
  public TableEntity findByItf(String itf) {
    try {
      LOG.info("[ItfRateTableStorageProxy - findByItf] calling itfRates table");
      return itfRates.getEntity("itfRate", itf);
    } catch (Exception ex) {
      throw buildTableStorageException("findByItf", ex);
    }
  }

  /**
   * Inserta o actualiza una tasa ITF en la tabla.
   *
   * @param itfRate valor de tasa ITF
   * @param userCode usuario que registra la tasa
   */
  public void save(Double itfRate, String userCode) {
    try {
      LOG.info("[ItfRateTableStorageProxy - save] calling itfRates table");
      var nowDateFormated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      var nowTimeFormated = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

      TableEntity entity = new TableEntity("itfRate", itfRate.toString());
      entity.getProperties().put("timeCreated", nowTimeFormated);
      entity.getProperties().put("dateCreate", nowDateFormated);
      entity.getProperties().put("timeUpdate", nowTimeFormated);
      entity.getProperties().put("dateUpdate", nowDateFormated);
      entity.getProperties().put("UserCode", userCode);

      itfRates.upsertEntity(entity);
    } catch (Exception ex) {
      throw buildTableStorageException("save", ex);
    }
  }

  /**
   * Lista todas las tasas ITF almacenadas.
   *
   * @return listado de entidades
   */
  public List<TableEntity> findAll() {
    try {
      LOG.info("[ItfRateTableStorageProxy - findAll] calling itfRates table");
      List<TableEntity> result = new ArrayList<>();
      itfRates.listEntities().forEach(result::add);
      return result;
    } catch (Exception ex) {
      throw buildTableStorageException("findAll", ex);
    }
  }

  private ApiException buildTableStorageException(String methodName, Exception cause) {
    LOG.error("[ItfRateTableStorageProxy - " + methodName
        + "] Error at calling itfRates table", cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
  }


}
