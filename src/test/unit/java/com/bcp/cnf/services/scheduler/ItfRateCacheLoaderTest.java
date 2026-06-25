package com.bcp.cnf.services.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.data.tables.models.TableEntity;
import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.cnf.services.dto.ItfsRateRedisDto;
import com.bcp.cnf.services.proxy.redis.ItfRateRedisProxy;
import com.bcp.cnf.services.proxy.tablestorage.ItfRateTableStorageProxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItfRateCacheLoaderTest {

  @Mock
  ItfRateRedisProxy redisProxy;

  @Mock
  ItfRateTableStorageProxy tableProxy;

  @InjectMocks
  ItfRateCacheLoader loader;

  @Test
  @DisplayName("Should warm up cache on startup and store all rates in Redis")
  void shouldWarmUpCacheOnStartupAndStoreAllRatesInRedis() {
    // Arrange
    TableEntity first = new TableEntity("itf", "0.00005");
    TableEntity second = new TableEntity("itf", "0.00010");
    when(tableProxy.findAll()).thenReturn(List.of(first, second));

    // Act
    loader.onStart();

    // Assert
    ArgumentCaptor<ItfsRateRedisDto> captor = ArgumentCaptor.forClass(ItfsRateRedisDto.class);
    verify(tableProxy).findAll();
    verify(redisProxy, times(2)).put(captor.capture());

    List<ItfsRateRedisDto> sent = captor.getAllValues();
    assertEquals(2, sent.size());
    assertEquals("0.00005", sent.get(0).itfRate());
    assertEquals("0.00010", sent.get(1).itfRate());

    assertNotNull(sent.get(0).timeUpdateCach());
    assertNotNull(sent.get(0).dateUpdateCach());
    assertNotNull(sent.get(1).timeUpdateCach());
    assertNotNull(sent.get(1).dateUpdateCach());

    assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_DATE), sent.get(0).dateUpdateCach());
    assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_DATE), sent.get(1).dateUpdateCach());

    assertTrue(isValidTime(sent.get(0).timeUpdateCach()));
    assertTrue(isValidTime(sent.get(1).timeUpdateCach()));
  }

  @Test
  @DisplayName("Should run scheduled warmup and skip Redis writes when table has no entities")
  void shouldRunScheduledWarmupAndSkipRedisWritesWhenTableHasNoEntities() {
    // Arrange
    when(tableProxy.findAll()).thenReturn(List.of());

    // Act
    loader.scheduledWarmUp();

    // Assert
    verify(tableProxy).findAll();
    verify(redisProxy, never()).put(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Should throw ApiException on startup when table storage lookup fails")
  void shouldThrowApiExceptionOnStartupWhenTableStorageLookupFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("table lookup error");
    when(tableProxy.findAll()).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> loader.onStart());

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(tableProxy).findAll();
    verify(redisProxy, never()).put(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Should throw ApiException on schedule when Redis write fails")
  void shouldThrowApiExceptionOnScheduleWhenRedisWriteFails() {
    // Arrange
    TableEntity entity = new TableEntity("itf", "0.00005");
    RuntimeException cause = new RuntimeException("redis write error");
    when(tableProxy.findAll()).thenReturn(List.of(entity));
    org.mockito.Mockito.doThrow(cause).when(redisProxy).put(org.mockito.ArgumentMatchers.any());

    // Act
    ApiException result = assertThrows(ApiException.class, () -> loader.scheduledWarmUp());

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(tableProxy).findAll();
    verify(redisProxy).put(org.mockito.ArgumentMatchers.any());
  }

  private boolean isValidTime(String value) {
    try {
      LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss"));
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}

