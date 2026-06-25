package com.bcp.cnf.services.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configura y expone clientes de Azure Table Storage para inyeccion CDI.
 */
@ApplicationScoped
public class AzureTableConfig {

  @ConfigProperty(name = "azure.tables.connection-string")
  String connectionString;

  @ConfigProperty(name = "azure.tables.table.cod-itf-exoneradas")
  String codTxnItfExonerated;

  @ConfigProperty(name = "azure.tables.table.tasas-itf")
  String itfRateTable;


  /**
   * Crea el cliente de servicio para operaciones generales sobre Azure Tables.
   *
   * @return instancia singleton de {@link TableServiceClient}
   */
  @Produces
  @Singleton
  public TableServiceClient tableServiceClient() {
    return new TableServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient();
  }

  /**
   * Crea el cliente para la tabla de codigos de transacciones ITF exoneradas.
   *
   * @return instancia singleton de {@link TableClient} asociada a la tabla configurada
   */
  @Produces
  @Singleton
  @Named("exoneratedCodTxnItf")
  public TableClient codTxnExoneratedClient() {
    return new TableClientBuilder()
        .connectionString(connectionString)
        .tableName(codTxnItfExonerated)
        .buildClient();
  }

  /**
   * Crea el cliente para la tabla de tasas ITF.
   *
   * @return instancia singleton de {@link TableClient} asociada a la tabla configurada
   */
  @Produces
  @Singleton
  @Named("itfRates")
  public TableClient itfRatesClient() {
    return new TableClientBuilder()
        .connectionString(connectionString)
        .tableName(itfRateTable)
        .buildClient();
  }


}
