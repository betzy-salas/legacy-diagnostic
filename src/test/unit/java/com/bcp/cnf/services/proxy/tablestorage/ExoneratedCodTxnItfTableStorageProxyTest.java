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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
class ExoneratedCodTxnItfTableStorageProxyTest {

  @Mock
  TableClient exoneratedCodTxnItf;

  @InjectMocks
  ExoneratedCodTxnItfTableStorageProxy proxy;

  @Test
  @DisplayName("Should return entity when UTC exists in table storage")
  void shouldReturnEntityWhenUtcExistsInTableStorage() {
    // Arrange
    String utc = "20240101";
    TableEntity expected = new TableEntity("UTC", utc);
    when(exoneratedCodTxnItf.getEntity("UTC", utc)).thenReturn(expected);

    // Act
    TableEntity result = proxy.findByUtc(utc);

    // Assert
    assertNotNull(result);
    assertEquals(expected, result);
    verify(exoneratedCodTxnItf).getEntity("UTC", utc);
  }

  @Test
  @DisplayName("Should throw ApiException when table storage getEntity fails")
  void shouldThrowApiExceptionWhenTableStorageGetEntityFails() {
    // Arrange
    String utc = "not-found";
    RuntimeException cause = new RuntimeException("Azure error");
    when(exoneratedCodTxnItf.getEntity("UTC", utc)).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.findByUtc(utc));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(exoneratedCodTxnItf).getEntity("UTC", utc);
  }

  @Test
  @DisplayName("Should build and upsert entity with expected properties")
  void shouldBuildAndUpsertEntityWithExpectedProperties() {
    // Arrange
    String utc = "20240101";
    String description = "Test description";
    boolean affectItf = true;
    String productType = "SAVINGS";
    String userCode = "USR01";

    // Act
    proxy.save(utc, description, affectItf, productType, userCode);

    // Assert
    ArgumentCaptor<TableEntity> captor = ArgumentCaptor.forClass(TableEntity.class);
    verify(exoneratedCodTxnItf).upsertEntity(captor.capture());

    TableEntity entity = captor.getValue();
    assertEquals("utc", entity.getPartitionKey());
    assertEquals(utc, entity.getRowKey());
    assertEquals(productType, entity.getProperty("productType"));
    assertEquals(description, entity.getProperty("description"));
    assertEquals(affectItf, entity.getProperty("affectItf"));
    assertEquals(userCode, entity.getProperty("userCode"));

    assertNotNull(entity.getProperty("timeCreated"));
    assertNotNull(entity.getProperty("dateCreate"));
    assertNotNull(entity.getProperty("timeUpdate"));
    assertNotNull(entity.getProperty("dateUpdate"));

    assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_DATE), entity.getProperty("timeCreated"));
    assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_DATE), entity.getProperty("timeUpdate"));

    String dateCreate = (String) entity.getProperty("dateCreate");
    String dateUpdate = (String) entity.getProperty("dateUpdate");
    assertNotNull(LocalTime.parse(dateCreate, DateTimeFormatter.ofPattern("HH:mm:ss")));
    assertNotNull(LocalTime.parse(dateUpdate, DateTimeFormatter.ofPattern("HH:mm:ss")));
  }

  @Test
  @DisplayName("Should return all entities from table storage list")
  void shouldReturnAllEntitiesFromTableStorageList() {
    // Arrange
    TableEntity first = new TableEntity("UTC", "1");
    TableEntity second = new TableEntity("UTC", "2");
    PagedIterable<TableEntity> pagedIterable = Mockito.mock(PagedIterable.class);

    Mockito.doAnswer(invocation -> {
      Iterable<TableEntity> iterable = Arrays.asList(first, second);
      iterable.forEach(invocation.getArgument(0));
      return null;
    }).when(pagedIterable).forEach(Mockito.any());

    when(exoneratedCodTxnItf.listEntities()).thenReturn(pagedIterable);

    // Act
    List<TableEntity> result = proxy.findAll();

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.contains(first));
    assertTrue(result.contains(second));
    verify(exoneratedCodTxnItf).listEntities();
  }

  @Test
  @DisplayName("Should throw ApiException when upsert fails during save")
  void shouldThrowApiExceptionWhenUpsertFailsDuringSave() {
    // Arrange
    RuntimeException cause = new RuntimeException("Azure upsert error");
    Mockito.doThrow(cause).when(exoneratedCodTxnItf).upsertEntity(Mockito.any(TableEntity.class));

    // Act
    ApiException result = assertThrows(
        ApiException.class,
        () -> proxy.save("20240101", "desc", true, "001", "USR01")
    );

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(exoneratedCodTxnItf).upsertEntity(Mockito.any(TableEntity.class));
  }

  @Test
  @DisplayName("Should throw ApiException when listEntities fails")
  void shouldThrowApiExceptionWhenListEntitiesFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("Azure list error");
    when(exoneratedCodTxnItf.listEntities()).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.findAll());

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(exoneratedCodTxnItf).listEntities();
  }
}