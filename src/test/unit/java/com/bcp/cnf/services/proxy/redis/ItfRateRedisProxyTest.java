package com.bcp.cnf.services.proxy.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bcp.atla.fwk.core.error.ApiException;
import com.bcp.cnf.services.dto.ItfsRateRedisDto;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItfRateRedisProxyTest {

  @Mock
  RedisDataSource redisDataSource;

  @Mock
  HashCommands<String, String, String> hashCommands;

  @Mock
  KeyCommands<String> keyCommands;

  ItfRateRedisProxy proxy;

  private static final String ITF = "0.00005";
  private static final String KEY = "itfs:rate:" + ITF;

  @BeforeEach
  void setUp() {
    when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
    when(redisDataSource.key(String.class)).thenReturn(keyCommands);
    proxy = new ItfRateRedisProxy(redisDataSource);
  }

  @Test
  @DisplayName("Should return DTO when key exists and hash contains values")
  void shouldReturnDtoWhenKeyExistsAndHashContainsValues() {
    // Arrange
    when(keyCommands.exists(KEY)).thenReturn(true);
    when(hashCommands.hgetall(KEY)).thenReturn(Map.of(
        "itfRate", ITF,
        "TimeUpdateCach", "08:15:00",
        "DateUpdateCach", "2024-02-20"
    ));

    // Act
    ItfsRateRedisDto result = proxy.get(ITF);

    // Assert
    assertNotNull(result);
    assertEquals(ITF, result.itfRate());
    assertEquals("08:15:00", result.timeUpdateCach());
    assertEquals("2024-02-20", result.dateUpdateCach());
    verify(keyCommands).exists(KEY);
    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands, keyCommands);
  }

  @Test
  @DisplayName("Should return null when key does not exist")
  void shouldReturnNullWhenKeyDoesNotExist() {
    // Arrange
    when(keyCommands.exists(KEY)).thenReturn(false);

    // Act
    ItfsRateRedisDto result = proxy.get(ITF);

    // Assert
    assertNull(result);
    verify(keyCommands).exists(KEY);
    verifyNoInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should return null when key exists but hash is empty")
  void shouldReturnNullWhenKeyExistsButHashIsEmpty() {
    // Arrange
    when(keyCommands.exists(KEY)).thenReturn(true);
    when(hashCommands.hgetall(KEY)).thenReturn(Map.of());

    // Act
    ItfsRateRedisDto result = proxy.get(ITF);

    // Assert
    assertNull(result);
    verify(keyCommands).exists(KEY);
    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands, keyCommands);
  }

  @Test
  @DisplayName("Should return null when key exists but hash is null")
  void shouldReturnNullWhenKeyExistsButHashIsNull() {
    // Arrange
    when(keyCommands.exists(KEY)).thenReturn(true);
    when(hashCommands.hgetall(KEY)).thenReturn(null);

    // Act
    ItfsRateRedisDto result = proxy.get(ITF);

    // Assert
    assertNull(result);
    verify(keyCommands).exists(KEY);
    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands, keyCommands);
  }

  @Test
  @DisplayName("Should store rate values and set Redis TTL to 24 hours")
  void shouldStoreRateValuesAndSetRedisTtlTo24Hours() {
    // Arrange
    ItfsRateRedisDto dto = new ItfsRateRedisDto(ITF, "00:00:00", "2026-01-01");

    // Act
    proxy.put(dto);

    // Assert
    verify(hashCommands).hset(eq(KEY), argThat(map ->
        ITF.equals(map.get("itfRate"))
            && isValidTime(map.get("TimeUpdateCach"))
            && isValidDate(map.get("DateUpdateCach"))
    ));
    verify(keyCommands).expire(KEY, Duration.ofHours(24));
    verifyNoMoreInteractions(hashCommands, keyCommands);
  }

  @Test
  @DisplayName("Should delete key from Redis when evict is called")
  void shouldDeleteKeyFromRedisWhenEvictIsCalled() {
    // Arrange
    String itfToDelete = "0.00007";

    // Act
    proxy.evict(itfToDelete);

    // Assert
    verify(keyCommands).del("itfs:rate:" + itfToDelete);
    verifyNoMoreInteractions(keyCommands);
    verifyNoInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should return all valid rates when Redis contains multiple keys")
  void shouldReturnAllValidRatesWhenRedisContainsMultipleKeys() {
    // Arrange
    String key1 = "itfs:rate:ITF01";
    String key2 = "itfs:rate:ITF02";
    when(keyCommands.keys("itfs:rate:*")).thenReturn(List.of(key1, key2));
    when(hashCommands.hgetall(key1)).thenReturn(Map.of(
        "itfRate", "ITF01",
        "TimeUpdateCach", "07:00:00",
        "DateUpdateCach", "2024-01-10"
    ));
    when(hashCommands.hgetall(key2)).thenReturn(Map.of(
        "itfRate", "ITF02",
        "TimeUpdateCach", "09:30:00",
        "DateUpdateCach", "2024-01-11"
    ));

    // Act
    List<ItfsRateRedisDto> result = proxy.getAll();

    // Assert
    assertEquals(2, result.size());
    assertEquals("ITF01", result.get(0).itfRate());
    assertEquals("ITF02", result.get(1).itfRate());
    verify(keyCommands).keys("itfs:rate:*");
    verify(hashCommands).hgetall(key1);
    verify(hashCommands).hgetall(key2);
  }

  @Test
  @DisplayName("Should ignore invalid hashes when retrieving all rates")
  void shouldIgnoreInvalidHashesWhenRetrievingAllRates() {
    // Arrange
    String key1 = "itfs:rate:ITF01";
    String key2 = "itfs:rate:ITF02";
    String key3 = "itfs:rate:ITF03";
    when(keyCommands.keys("itfs:rate:*")).thenReturn(List.of(key1, key2, key3));
    when(hashCommands.hgetall(key1)).thenReturn(Map.of());
    when(hashCommands.hgetall(key2)).thenReturn(null);
    when(hashCommands.hgetall(key3)).thenReturn(Map.of(
        "itfRate", "ITF03",
        "TimeUpdateCach", "10:30:00",
        "DateUpdateCach", "2024-01-12"
    ));

    // Act
    List<ItfsRateRedisDto> result = proxy.getAll();

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.stream().anyMatch(dto -> "ITF03".equals(dto.itfRate())));
  }

  @Test
  @DisplayName("Should throw ApiException when Redis exists call fails during get")
  void shouldThrowApiExceptionWhenRedisExistsCallFailsDuringGet() {
    // Arrange
    RuntimeException cause = new RuntimeException("exists error");
    when(keyCommands.exists(KEY)).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.get(ITF));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(keyCommands).exists(KEY);
    verifyNoInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should throw ApiException when hash retrieval fails during get")
  void shouldThrowApiExceptionWhenHashRetrievalFailsDuringGet() {
    // Arrange
    RuntimeException cause = new RuntimeException("hgetall error");
    when(keyCommands.exists(KEY)).thenReturn(true);
    when(hashCommands.hgetall(KEY)).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.get(ITF));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(keyCommands).exists(KEY);
    verify(hashCommands).hgetall(KEY);
  }

  @Test
  @DisplayName("Should throw ApiException when map access fails during get conversion")
  void shouldThrowApiExceptionWhenMapAccessFailsDuringGetConversion() {
    // Arrange
    @SuppressWarnings("unchecked")
    Map<String, String> brokenMap = org.mockito.Mockito.mock(Map.class);
    when(keyCommands.exists(KEY)).thenReturn(true);
    when(hashCommands.hgetall(KEY)).thenReturn(brokenMap);
    when(brokenMap.isEmpty()).thenReturn(false);
    when(brokenMap.get("itfRate")).thenThrow(new RuntimeException("map read error"));

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.get(ITF));

    // Assert
    assertNotNull(result.getCause());
    verify(keyCommands).exists(KEY);
    verify(hashCommands).hgetall(KEY);
  }

  @Test
  @DisplayName("Should throw ApiException when Redis put operation fails")
  void shouldThrowApiExceptionWhenRedisPutOperationFails() {
    // Arrange
    ItfsRateRedisDto dto = new ItfsRateRedisDto(ITF, "00:00:00", "2026-01-01");
    RuntimeException cause = new RuntimeException("hset error");
    when(hashCommands.hset(eq(KEY), org.mockito.ArgumentMatchers.anyMap())).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.put(dto));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(hashCommands).hset(eq(KEY), org.mockito.ArgumentMatchers.anyMap());
  }

  @Test
  @DisplayName("Should throw ApiException when Redis evict operation fails")
  void shouldThrowApiExceptionWhenRedisEvictOperationFails() {
    // Arrange
    String itfToDelete = "0.00007";
    RuntimeException cause = new RuntimeException("del error");
    when(keyCommands.del("itfs:rate:" + itfToDelete)).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.evict(itfToDelete));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(keyCommands).del("itfs:rate:" + itfToDelete);
  }

  @Test
  @DisplayName("Should throw ApiException when Redis keys call fails during getAll")
  void shouldThrowApiExceptionWhenRedisKeysCallFailsDuringGetAll() {
    // Arrange
    RuntimeException cause = new RuntimeException("keys error");
    when(keyCommands.keys("itfs:rate:*")).thenThrow(cause);

    // Act
    ApiException result = assertThrows(ApiException.class, () -> proxy.getAll());

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(keyCommands).keys("itfs:rate:*");
    verifyNoInteractions(hashCommands);
  }

  private boolean isValidTime(String value) {
    try {
      LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss"));
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  private boolean isValidDate(String value) {
    try {
      LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}