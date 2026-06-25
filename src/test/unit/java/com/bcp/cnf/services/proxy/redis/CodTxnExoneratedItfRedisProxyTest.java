package com.bcp.cnf.services.proxy.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bcp.cnf.services.dto.CodTxnExoneratedItfRedisDto;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodTxnExoneratedItfRedisProxyTest {

  @Mock
  RedisDataSource redisDataSource;

  @Mock
  HashCommands<String, String, String> hashCommands;

  @Mock
  KeyCommands<String> keyCommands;

  CodTxnExoneratedItfRedisProxy proxy;

  private static final String UTC = "0000000010";
  private static final String PRODUCT_TYPE = "234";
  private static final String KEY = "utcs:exoneradas:itf:" + UTC + ":" + PRODUCT_TYPE;

  @BeforeEach
  void setUp() {
    when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
    when(redisDataSource.key(String.class)).thenReturn(keyCommands);
    proxy = new CodTxnExoneratedItfRedisProxy(redisDataSource);
  }

  @Test
  @DisplayName("Should return DTO when Redis hash contains data")
  void getShouldReturnDtoWhenRedisHashContainsData() {
    // Arrange
    when(hashCommands.hgetall(KEY)).thenReturn(Map.of(
        "utc", UTC,
        "productType", PRODUCT_TYPE,
        "affectItf", "true",
        "timeUpdateCach", "10:30:15",
        "dateUpdateCach", "2024-04-01"
    ));

    // Act
    CodTxnExoneratedItfRedisDto result = proxy.get(UTC, PRODUCT_TYPE);

    // Assert
    assertNotNull(result);
    assertEquals(UTC, result.utc());
    assertEquals(PRODUCT_TYPE, result.productType());
    assertEquals(Boolean.TRUE, result.affectItf());
    assertEquals(LocalTime.of(10, 30, 15), result.timeUpdateCach());
    assertEquals(LocalDate.of(2024, 4, 1), result.dateUpdateCach());

    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should return null when Redis hash is empty")
  void getShouldReturnNullWhenRedisHashIsEmpty() {
    // Arrange
    when(hashCommands.hgetall(KEY)).thenReturn(Map.of());

    // Act
    CodTxnExoneratedItfRedisDto result = proxy.get(UTC, PRODUCT_TYPE);

    // Assert
    assertNull(result);
    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should return null when Redis hash is null")
  void getShouldReturnNullWhenRedisHashIsNull() {
    // Arrange
    when(hashCommands.hgetall(KEY)).thenReturn(null);

    // Act
    CodTxnExoneratedItfRedisDto result = proxy.get(UTC, PRODUCT_TYPE);

    // Assert
    assertNull(result);
    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should store DTO fields and set 24 hours TTL")
  void putShouldStoreDtoFieldsAndSet24HoursTtl() {
    // Arrange
    CodTxnExoneratedItfRedisDto dto = new CodTxnExoneratedItfRedisDto(
        UTC,
        PRODUCT_TYPE,
        false,
        LocalTime.of(7, 45, 30),
        LocalDate.of(2024, 1, 1)
    );

    // Act
    proxy.put(dto);

    // Assert
    verify(hashCommands).hset(eq(KEY), argThat(map ->
        UTC.equals(map.get("utc"))
            && PRODUCT_TYPE.equals(map.get("productType"))
            && "false".equals(map.get("affectItf"))
            && map.containsKey("timeUpdateCach")
            && map.containsKey("dateUpdateCach")
            && isValidTime(map.get("timeUpdateCach"))
            && isValidDate(map.get("dateUpdateCach"))
    ));

    verify(keyCommands).expire(KEY, Duration.ofHours(24));
    verifyNoMoreInteractions(hashCommands, keyCommands);
  }

  @Test
  @DisplayName("Should include the requested product type in the generated key")
  void getShouldUseProductTypeInGeneratedKey() {
    // Arrange
    String otherProductType = "999";
    String expectedKey = "utcs:exoneradas:itf:" + UTC + ":" + otherProductType;
    when(hashCommands.hgetall(expectedKey)).thenReturn(Map.of(
        "utc", UTC,
        "productType", otherProductType,
        "affectItf", "false",
        "timeUpdateCach", "01:02:03",
        "dateUpdateCach", "2026-05-18"
    ));

    // Act
    CodTxnExoneratedItfRedisDto result = proxy.get(UTC, otherProductType);

    // Assert
    assertNotNull(result);
    assertEquals(otherProductType, result.productType());
    assertFalse(result.affectItf());
    verify(hashCommands).hgetall(expectedKey);
    verifyNoMoreInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should delete Redis key for UTC and product type")
  void evictShouldDeleteRedisKeyForUtcAndProductType() {
    // Arrange
    String utc = "1234567890";
    String productType = "888";
    String expectedKey = "utcs:exoneradas:itf:" + utc + ":" + productType;

    // Act
    proxy.evict(utc, productType);

    // Assert
    verify(keyCommands).del(expectedKey);
    verifyNoMoreInteractions(hashCommands, keyCommands);
  }

  @Test
  @DisplayName("Should throw ApiException when Redis get operation fails")
  void getShouldThrowApiExceptionWhenRedisGetOperationFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("redis get error");
    when(hashCommands.hgetall(KEY)).thenThrow(cause);

    // Act
    com.bcp.atla.fwk.core.error.ApiException result =
        assertThrows(com.bcp.atla.fwk.core.error.ApiException.class, () -> proxy.get(UTC, PRODUCT_TYPE));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should throw ApiException when Redis data cannot be parsed into DTO")
  void getShouldThrowApiExceptionWhenRedisDataCannotBeParsedIntoDto() {
    // Arrange
    when(hashCommands.hgetall(KEY)).thenReturn(Map.of(
        "utc", UTC,
        "productType", PRODUCT_TYPE,
        "affectItf", "true",
        "timeUpdateCach", "invalid-time",
        "dateUpdateCach", "2024-04-01"
    ));

    // Act
    com.bcp.atla.fwk.core.error.ApiException result =
        assertThrows(com.bcp.atla.fwk.core.error.ApiException.class, () -> proxy.get(UTC, PRODUCT_TYPE));

    // Assert
    assertNotNull(result.getCause());
    verify(hashCommands).hgetall(KEY);
    verifyNoMoreInteractions(hashCommands);
  }

  @Test
  @DisplayName("Should throw ApiException when Redis put operation fails")
  void putShouldThrowApiExceptionWhenRedisPutOperationFails() {
    // Arrange
    CodTxnExoneratedItfRedisDto dto = new CodTxnExoneratedItfRedisDto(
        UTC,
        PRODUCT_TYPE,
        true,
        LocalTime.of(8, 0, 0),
        LocalDate.of(2026, 5, 26)
    );
    RuntimeException cause = new RuntimeException("redis put error");
    when(hashCommands.hset(eq(KEY), org.mockito.ArgumentMatchers.anyMap())).thenThrow(cause);

    // Act
    com.bcp.atla.fwk.core.error.ApiException result =
        assertThrows(com.bcp.atla.fwk.core.error.ApiException.class, () -> proxy.put(dto));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(hashCommands).hset(eq(KEY), org.mockito.ArgumentMatchers.anyMap());
    verifyNoMoreInteractions(hashCommands, keyCommands);
  }

  @Test
  @DisplayName("Should throw ApiException when Redis evict operation fails")
  void evictShouldThrowApiExceptionWhenRedisEvictOperationFails() {
    // Arrange
    RuntimeException cause = new RuntimeException("redis del error");
    when(keyCommands.del(KEY)).thenThrow(cause);

    // Act
    com.bcp.atla.fwk.core.error.ApiException result =
        assertThrows(com.bcp.atla.fwk.core.error.ApiException.class, () -> proxy.evict(UTC, PRODUCT_TYPE));

    // Assert
    assertNotNull(result.getCause());
    assertEquals(cause, result.getCause());
    verify(keyCommands).del(KEY);
    verifyNoMoreInteractions(hashCommands, keyCommands);
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