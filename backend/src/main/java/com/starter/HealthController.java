package com.starter;

import java.util.Map;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public HealthController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        String mysql = "DOWN";
        String cache = "DOWN";
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            mysql = "UP";
        } catch (RuntimeException ignored) { }
        try {
            if ("PONG".equals(redis.execute((RedisCallback<String>) connection -> connection.ping()))) {
                cache = "UP";
            }
        } catch (RuntimeException ignored) { }
        boolean ready = "UP".equals(mysql) && "UP".equals(cache);
        return ResponseEntity.status(ready ? 200 : 503)
            .body(Map.of("status", ready ? "UP" : "DOWN", "mysql", mysql, "redis", cache));
    }
}
