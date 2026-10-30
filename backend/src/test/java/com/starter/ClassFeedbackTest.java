package com.starter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:feedback;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.flyway.enabled=false",
    "spring.data.redis.host=unused","spring.data.redis.password=unused"})
@AutoConfigureMockMvc
class ClassFeedbackTest {
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired ClassFeedbackService service;
    @MockitoBean StringRedisTemplate redis;
    private ValueOperations<String,String> values;

    @BeforeEach @SuppressWarnings("unchecked") void resetData() {
        jdbc.execute("DROP TABLE IF EXISTS demo_feedback");
        jdbc.execute("DROP TABLE IF EXISTS demo_lesson");
        jdbc.execute("DROP TABLE IF EXISTS demo_student");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V2__class_feedback.sql")).execute(jdbc.getDataSource());
        reset(redis);
        values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(any(RedisCallback.class))).thenReturn("PONG");
    }

    @Test void draftIsHiddenUntilConfirmedAndConfirmationIsIdempotent() throws Exception {
        long student=service.students().get(0).id();
        var lesson=service.addLesson(student,"分数应用题","能画图解释思路","完成练习 1–3 题");
        var draft=service.generate(lesson.id());
        assertEquals("DRAFT",draft.status());
        assertEquals(draft.id(),service.generate(lesson.id()).id());
        http.perform(get("/api/students/"+student+"/feedback")).andExpect(jsonPath("$.length()").value(0));
        http.perform(put("/api/feedback/"+draft.id()).contentType("application/json").content(json.writeValueAsString(java.util.Map.of("content","下次先画图，再列式。"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("下次先画图，再列式。"));
        service.confirm(draft.id());
        service.confirm(draft.id());
        http.perform(get("/api/students/"+student+"/feedback")).andExpect(jsonPath("$.length()").value(1));
        http.perform(put("/api/feedback/"+draft.id()).contentType("application/json").content("{\"content\":\"不能改\"}"))
            .andExpect(status().isConflict());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM demo_feedback",Integer.class));
    }

    @Test void blankInputDoesNotCreateRows() throws Exception {
        http.perform(post("/api/students").contentType("application/json").content("{\"name\":\"  \"}"))
            .andExpect(status().isBadRequest());
        http.perform(post("/api/lessons").contentType("application/json").content("{\"studentId\":1,\"topic\":\"数学\",\"performance\":\"  \"}"))
            .andExpect(status().isBadRequest());
        assertEquals(2,service.students().size());
        assertTrue(service.lessons().isEmpty());
    }

    @Test void invalidReferencesReturn404() throws Exception {
        http.perform(post("/api/lessons/999/feedback")).andExpect(status().isNotFound());
        http.perform(post("/api/feedback/999/confirm")).andExpect(status().isNotFound());
        http.perform(get("/api/students/999/feedback")).andExpect(status().isNotFound());
    }

    @Test void cacheFailureDoesNotLoseCommittedData() {
        when(redis.opsForValue()).thenThrow(new IllegalStateException("cache offline"));
        doThrow(new IllegalStateException("cache offline")).when(redis).delete(ClassFeedbackService.STATS_KEY);
        service.addStudent("缓存故障演示学员");
        assertEquals(3,service.stats().get("students"));
    }

    @Test void cacheHitIsUsedAndWriteInvalidates() {
        when(values.get(ClassFeedbackService.STATS_KEY)).thenReturn("{\"students\":7,\"lessons\":0,\"drafts\":0,\"confirmed\":0}");
        assertEquals(7,service.stats().get("students"));
        service.addStudent("统计练习");
        verify(redis).delete(ClassFeedbackService.STATS_KEY);
    }
}
