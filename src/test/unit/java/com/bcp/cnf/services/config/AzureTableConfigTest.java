package com.bcp.cnf.services.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class AzureTableConfigTest {

  @Test
  @DisplayName("Should build table service client using configured connection string")
  void shouldBuildTableServiceClientUsingConfiguredConnectionString() {
    // Arrange
    AzureTableConfig config = new AzureTableConfig();
    config.connectionString = "UseDevelopmentStorage=true";
    TableServiceClient expectedClient = Mockito.mock(TableServiceClient.class);

    try (MockedConstruction<TableServiceClientBuilder> mockedBuilder = Mockito.mockConstruction(
        TableServiceClientBuilder.class,
        (builder, context) -> {
          when(builder.connectionString(config.connectionString)).thenReturn(builder);
          when(builder.buildClient()).thenReturn(expectedClient);
        })) {

      // Act
      TableServiceClient result = config.tableServiceClient();

      // Assert
      assertSame(expectedClient, result);
      TableServiceClientBuilder constructedBuilder = mockedBuilder.constructed().getFirst();
      verify(constructedBuilder).connectionString(config.connectionString);
      verify(constructedBuilder).buildClient();
    }
  }

  @Test
  @DisplayName("Should build exonerated transaction table client with configured table name")
  void shouldBuildExoneratedTransactionTableClientWithConfiguredTableName() {
    // Arrange
    AzureTableConfig config = new AzureTableConfig();
    config.connectionString = "UseDevelopmentStorage=true";
    config.codTxnItfExonerated = "cod-itf-exoneradas";
    TableClient expectedClient = Mockito.mock(TableClient.class);

    try (MockedConstruction<TableClientBuilder> mockedBuilder = Mockito.mockConstruction(
        TableClientBuilder.class,
        (builder, context) -> {
          when(builder.connectionString(config.connectionString)).thenReturn(builder);
          when(builder.tableName(config.codTxnItfExonerated)).thenReturn(builder);
          when(builder.buildClient()).thenReturn(expectedClient);
        })) {

      // Act
      TableClient result = config.codTxnExoneratedClient();

      // Assert
      assertSame(expectedClient, result);
      TableClientBuilder constructedBuilder = mockedBuilder.constructed().getFirst();
      verify(constructedBuilder).connectionString(config.connectionString);
      verify(constructedBuilder).tableName(config.codTxnItfExonerated);
      verify(constructedBuilder).buildClient();
    }
  }

  @Test
  @DisplayName("Should build ITF rates table client with configured table name")
  void shouldBuildItfRatesTableClientWithConfiguredTableName() {
    // Arrange
    AzureTableConfig config = new AzureTableConfig();
    config.connectionString = "UseDevelopmentStorage=true";
    config.itfRateTable = "tasas-itf";
    TableClient expectedClient = Mockito.mock(TableClient.class);

    try (MockedConstruction<TableClientBuilder> mockedBuilder = Mockito.mockConstruction(
        TableClientBuilder.class,
        (builder, context) -> {
          when(builder.connectionString(config.connectionString)).thenReturn(builder);
          when(builder.tableName(config.itfRateTable)).thenReturn(builder);
          when(builder.buildClient()).thenReturn(expectedClient);
        })) {

      // Act
      TableClient result = config.itfRatesClient();

      // Assert
      assertSame(expectedClient, result);
      TableClientBuilder constructedBuilder = mockedBuilder.constructed().getFirst();
      verify(constructedBuilder).connectionString(config.connectionString);
      verify(constructedBuilder).tableName(config.itfRateTable);
      verify(constructedBuilder).buildClient();
    }
  }
}

