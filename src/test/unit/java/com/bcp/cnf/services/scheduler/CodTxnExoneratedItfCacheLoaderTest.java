package com.bcp.cnf.services.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.data.tables.models.TableEntity;
import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.cnf.services.proxy.redis.CodTxnExoneratedItfRedisProxy;
import com.bcp.cnf.services.proxy.tablestorage.ExoneratedCodTxnItfTableStorageProxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodTxnExoneratedItfCacheLoaderTest {

  @Mock
  CodTxnExoneratedItfRedisProxy redisProxy;

  @Mock
  ExoneratedCodTxnItfTableStorageProxy tableProxy;

  @InjectMocks
  CodTxnExoneratedItfCacheLoader loader;

  @Test
  @DisplayName("Should warm up cache on startup and persist mapped DTO in Redis")
  void shouldWarmUpCacheOnStartupAndPersistMappedDtoInRedis() {
    // Arrange
    TableEntity entity = new TableEntity("utc", "ignored-row-key")
        .addProperty("RowKey", "000123")
        .addProperty("productType", "001")
        .addProperty("affectItf", true);

    when(tableProxy.findAll()).thenReturn(List.of(entity));

    // Act
    loader.onStart();

    // Assert
    verify(tableProxy).findAll();
    verify(redisProxy).put(argThat(dto -> {
      assertEquals("000123", dto.utc());
      assertEquals("001", dto.productType());
      assertEquals(Boolean.TRUE, dto.affectItf());
      assertNotNull(dto.timeUpdateCach());
      assertNotNull(dto.dateUpdateCach());
      assertEquals(LocalDate.now(), dto.dateUpdateCach());
      assertEquals(LocalTime.now().getHour(), dto.timeUpdateCach().getHour());
      return true;
    }));
  }

  @Test
  @DisplayName("Should warm up cache on schedule and skip Redis writes when no entities exist")
  void shouldWarmUpCacheOnScheduleAndSkipRedisWritesWhenNoEntitiesExist() {
    // Arrange
    when(tableProxy.findAll()).thenReturn(List.of());

    // Act
    loader.scheduledWarmUp();

    // Assert
    verify(tableProxy).findAll();
    verify(redisProxy, never()).put(argThat(dto -> true));
  }

  @Test
  @DisplayName("Should throw ApiException on startup when table storage lookup fails")
  void shouldThrowApiExceptionOnStartupWhenTableStorageLookupFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("table storage error");
    when(tableProxy.findAll()).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> loader.onStart());

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(tableProxy).findAll();
    verify(redisProxy, never()).put(argThat(dto -> true));
  }

  @Test
  @DisplayName("Should throw ApiException on schedule when entity data is invalid")
  void shouldThrowApiExceptionOnScheduleWhenEntityDataIsInvalid() {
    // Arrange
    TableEntity invalidEntity = new TableEntity("utc", "row-1")
        .addProperty("RowKey", "000123")
        .addProperty("affectItf", true);
    when(tableProxy.findAll()).thenReturn(List.of(invalidEntity));

    // Act
    ApiException result = assertThrows(ApiException.class, () -> loader.scheduledWarmUp());

    // Assert
    assertNotNull(result.getCause());
    verify(tableProxy).findAll();
    verify(redisProxy, never()).put(argThat(dto -> true));
  }
}
