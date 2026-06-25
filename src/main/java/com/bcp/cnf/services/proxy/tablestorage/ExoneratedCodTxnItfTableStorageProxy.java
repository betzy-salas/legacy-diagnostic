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
 * Proxy para acceder a la tabla de codigos de transaccion ITF exonerados.
 */
@ApplicationScoped
public class ExoneratedCodTxnItfTableStorageProxy {

  private static final Logger LOG = Logger.getLogger(ExoneratedCodTxnItfTableStorageProxy.class);
  private static final String EXTERNAL_ERROR = "EXTERNAL_ERROR";

  @Named("exoneratedCodTxnItf")
  TableClient exoneratedCodTxnItf;

  /**
   * Obtiene una entidad por su UTC desde Azure Table Storage.
   *
   * @param utc codigo UTC
   * @return entidad encontrada
   */
  public TableEntity findByUtc(String utc) {
    try {
      LOG.info("[ExoneratedCodTxnItfTableStorageProxy - findByUtc] "
          + "calling exoneratedCodTxnItf table");
      return exoneratedCodTxnItf.getEntity("UTC", utc);
    } catch (Exception ex) {
      throw buildTableStorageException("findByUtc", ex);
    }
  }

  /**
   * Inserta o actualiza un registro de codigo exonerado.
   *
   * @param utc codigo UTC
   * @param description descripcion del codigo
   * @param affectItf indicador de afectacion ITF
   * @param productType tipo de producto
   * @param userCode usuario que realiza la operacion
   */
  public void save(String utc, String description, boolean affectItf,
                   String productType, String userCode) {

    LOG.info("[ExoneratedCodTxnItfTableStorageProxy - save] calling exoneratedCodTxnItf table");

    try {
      TableEntity entity = new TableEntity("utc", utc);
      entity.getProperties().put("productType", productType);
      entity.getProperties().put("description", description);
      entity.getProperties().put("affectItf", affectItf);

      var nowDateFormated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      var nowTimeFormated = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

      entity.getProperties().put("timeCreated", nowTimeFormated);
      entity.getProperties().put("dateCreate", nowDateFormated);
      entity.getProperties().put("timeUpdate", nowTimeFormated);
      entity.getProperties().put("dateUpdate", nowDateFormated);

      entity.getProperties().put("userCode", userCode);

      exoneratedCodTxnItf.upsertEntity(entity);
    } catch (Exception ex) {
      throw buildTableStorageException("save", ex);
    }
  }

  /**
   * Lista todos los codigos exonerados almacenados en la tabla.
   *
   * @return listado de entidades
   */
  public List<TableEntity> findAll() {
    try {
      LOG.info("[ExoneratedCodTxnItfTableStorageProxy - findAll] "
          + "calling exoneratedCodTxnItf table");
      List<TableEntity> result = new ArrayList<>();
      exoneratedCodTxnItf.listEntities().forEach(result::add);
      return result;
    } catch (Exception ex) {
      throw buildTableStorageException("findAll", ex);
    }
  }

  private ApiException buildTableStorageException(String methodName, Exception cause) {
    LOG.error("[ExoneratedCodTxnItfTableStorageProxy - "
        + methodName
        + "] Error at calling exoneratedCodTxnItf table", cause);
    return ApiException.builder()
        .cause(cause)
        .category(ExceptionCategoryTypes.ofValue(EXTERNAL_ERROR))
        .build();
  }



}
