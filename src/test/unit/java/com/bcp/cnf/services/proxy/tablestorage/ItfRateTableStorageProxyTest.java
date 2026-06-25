package com.bcp.cnf.services.proxy.tablestorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.bcp.atla.fwk.core.error.ApiException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItfRateTableStorageProxyTest {

  @Mock
  TableClient itfRates;

  @InjectMocks
  ItfRateTableStorageProxy proxy;

  @Test
  @DisplayName("Should return entity when ITF exists in table storage")
  void shouldReturnEntityWhenItfExistsInTableStorage() {
    // Arrange
    String itf = "0.005";
    TableEntity entity = new TableEntity("itfRate", itf);
    when(itfRates.getEntity("itfRate", itf)).thenReturn(entity);

    // Act
    TableEntity result = proxy.findByItf(itf);

    // Assert
    assertNotNull(result);
    assertEquals(entity, result);
    verify(itfRates).getEntity("itfRate", itf);
  }

  @Test
  @DisplayName("Should throw ApiException when ITF lookup fails")
  void shouldThrowApiExceptionWhenItfLookupFails() {
    // Arrange
    String itf = "not-exists";
    RuntimeException cause = new RuntimeException("Azure error");
    when(itfRates.getEntity("itfRate", itf)).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.findByItf(itf));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(itfRates).getEntity("itfRate", itf);
  }

  @Test
  @DisplayName("Should create and upsert ITF rate entity with expected fields")
  void shouldCreateAndUpsertItfRateEntityWithExpectedFields() {
    // Arrange
    Double itfRate = 0.005;
    String userCode = "USR01";

    // Act
    proxy.save(itfRate, userCode);

    // Assert
    ArgumentCaptor<TableEntity> captor = ArgumentCaptor.forClass(TableEntity.class);
    verify(itfRates).upsertEntity(captor.capture());

    TableEntity entity = captor.getValue();
    assertEquals("itfRate", entity.getPartitionKey());
    assertEquals(itfRate.toString(), entity.getRowKey());
    assertEquals(userCode, entity.getProperty("UserCode"));
    assertNotNull(entity.getProperty("timeCreated"));
    assertNotNull(entity.getProperty("dateCreate"));
    assertNotNull(entity.getProperty("timeUpdate"));
    assertNotNull(entity.getProperty("dateUpdate"));
  }

  @Test
  @DisplayName("Should throw ApiException when upsert fails during save")
  void shouldThrowApiExceptionWhenUpsertFailsDuringSave() {
    // Arrange
    RuntimeException cause = new RuntimeException("Azure upsert error");
    Mockito.doThrow(cause).when(itfRates).upsertEntity(Mockito.any(TableEntity.class));

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.save(0.005, "USR01"));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(itfRates).upsertEntity(Mockito.any(TableEntity.class));
  }

  @Test
  @DisplayName("Should return all entities from table storage")
  void shouldReturnAllEntitiesFromTableStorage() {
    // Arrange
    TableEntity first = new TableEntity("itfRate", "0.005");
    TableEntity second = new TableEntity("itfRate", "0.010");
    PagedIterable<TableEntity> pagedIterable = Mockito.mock(PagedIterable.class);

    Mockito.doAnswer(invocation -> {
      Iterable<TableEntity> iterable = Arrays.asList(first, second);
      iterable.forEach(invocation.getArgument(0));
      return null;
    }).when(pagedIterable).forEach(Mockito.any());

    when(itfRates.listEntities()).thenReturn(pagedIterable);

    // Act
    List<TableEntity> result = proxy.findAll();

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.contains(first));
    assertTrue(result.contains(second));
    verify(itfRates).listEntities();
  }

  @Test
  @DisplayName("Should throw ApiException when list entities fails")
  void shouldThrowApiExceptionWhenListEntitiesFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("Azure list error");
    when(itfRates.listEntities()).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.findAll());

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(itfRates).listEntities();
  }
}