package com.starter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:health;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.flyway.enabled=false",
    "spring.data.redis.host=unused", "spring.data.redis.password=unused"})
@AutoConfigureMockMvc
class HealthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean StringRedisTemplate redis;

    @Test void healthChecksBothServices() throws Exception {
        when(redis.execute(any(RedisCallback.class))).thenReturn("PONG");
        mvc.perform(get("/api/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.mysql").value("UP")).andExpect(jsonPath("$.redis").value("UP"));
    }

    @Test void reportsUnavailableCache() throws Exception {
        when(redis.execute(any(RedisCallback.class))).thenThrow(new IllegalStateException("offline"));
        mvc.perform(get("/api/health")).andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.redis").value("DOWN"));
    }
}
