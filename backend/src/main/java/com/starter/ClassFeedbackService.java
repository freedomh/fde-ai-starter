package com.starter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClassFeedbackService {
    static final String STATS_KEY = "fde:class-feedback:stats";
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;

    public ClassFeedbackService(JdbcTemplate jdbc, StringRedisTemplate redis, TransactionTemplate transactions, ObjectMapper json) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.transactions = transactions;
        this.json = json;
    }

    public record Student(long id, String name) {}
    public record Lesson(long id, long studentId, String studentName, String topic, String performance, String homework, LocalDateTime createdAt) {}
    public record Feedback(long id, long lessonId, long studentId, String studentName, String topic, String content, String status, LocalDateTime createdAt, LocalDateTime confirmedAt) {}
    private static final String FEEDBACK_QUERY = "SELECT f.*, l.student_id, l.topic, s.name AS student_name FROM demo_feedback f JOIN demo_lesson l ON l.id=f.lesson_id JOIN demo_student s ON s.id=l.student_id";
    private static final RowMapper<Feedback> FEEDBACK_MAPPER = (rs, row) -> new Feedback(
        rs.getLong("id"), rs.getLong("lesson_id"), rs.getLong("student_id"), rs.getString("student_name"),
        rs.getString("topic"), rs.getString("content"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("confirmed_at") == null ? null : rs.getTimestamp("confirmed_at").toLocalDateTime());

    public List<Student> students() {
        return jdbc.query("SELECT id,name FROM demo_student ORDER BY id", (rs, row) -> new Student(rs.getLong("id"), rs.getString("name")));
    }

    public Student addStudent(String name) {
        long id = insert("INSERT INTO demo_student(name) VALUES (?)", name.trim());
        invalidate();
        return new Student(id, name.trim());
    }

    public List<Lesson> lessons() {
        return jdbc.query("SELECT l.*,s.name AS student_name FROM demo_lesson l JOIN demo_student s ON s.id=l.student_id ORDER BY l.id DESC",
            (rs, row) -> new Lesson(rs.getLong("id"), rs.getLong("student_id"), rs.getString("student_name"),
                rs.getString("topic"), rs.getString("performance"), rs.getString("homework"), rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public Lesson addLesson(long studentId, String topic, String performance, String homework) {
        requireStudent(studentId);
        long id = insert("INSERT INTO demo_lesson(student_id,topic,performance,homework) VALUES (?,?,?,?)",
            studentId, topic.trim(), performance.trim(), homework == null ? "" : homework.trim());
        invalidate();
        return lessons().stream().filter(lesson -> lesson.id() == id).findFirst().orElseThrow();
    }

    public List<Feedback> feedbacks() {
        return jdbc.query(FEEDBACK_QUERY + " ORDER BY f.id DESC", FEEDBACK_MAPPER);
    }

    public List<Feedback> published(long studentId) {
        requireStudent(studentId);
        return jdbc.query(FEEDBACK_QUERY + " WHERE l.student_id=? AND f.status='CONFIRMED' ORDER BY f.id DESC", FEEDBACK_MAPPER, studentId);
    }

    public Feedback generate(long lessonId) {
        List<Feedback> existing = jdbc.query(FEEDBACK_QUERY + " WHERE f.lesson_id=?", FEEDBACK_MAPPER, lessonId);
        if (!existing.isEmpty()) return existing.get(0);
        Lesson lesson = lessons().stream().filter(item -> item.id() == lessonId).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课堂记录不存在"));
        String content = "本次学习主题：" + lesson.topic() + "\n\n课堂表现：" + lesson.performance()
            + "\n\n课后任务：" + (lesson.homework().isBlank() ? "巩固本次课堂内容，记录遇到的问题。" : lesson.homework())
            + "\n\n教师建议：请结合本次表现，补充一条具体、可执行的改进建议。";
        try {
            insert("INSERT INTO demo_feedback(lesson_id,content,status) VALUES (?,?,'DRAFT')", lessonId, content);
        } catch (DuplicateKeyException concurrentGeneration) {
            // A unique lesson_id keeps repeated requests from creating duplicate feedback.
        }
        invalidate();
        return jdbc.query(FEEDBACK_QUERY + " WHERE f.lesson_id=?", FEEDBACK_MAPPER, lessonId).get(0);
    }

    public Feedback edit(long id, String content) {
        Feedback current = findFeedback(id);
        if (!"DRAFT".equals(current.status())) throw new ResponseStatusException(HttpStatus.CONFLICT, "已确认反馈不能继续编辑");
        int changed = jdbc.update("UPDATE demo_feedback SET content=? WHERE id=? AND status='DRAFT'", content.trim(), id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "反馈状态已改变，请重新读取");
        return findFeedback(id);
    }

    public Feedback confirm(long id) {
        findFeedback(id);
        jdbc.update("UPDATE demo_feedback SET status='CONFIRMED',confirmed_at=CURRENT_TIMESTAMP WHERE id=? AND status='DRAFT'", id);
        invalidate();
        return findFeedback(id);
    }

    public Map<String, Long> stats() {
        try {
            String cached = redis.opsForValue().get(STATS_KEY);
            if (cached != null) return json.readValue(cached, new TypeReference<Map<String, Long>>() {});
        } catch (Exception ignored) { }
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("students", count("SELECT COUNT(*) FROM demo_student"));
        result.put("lessons", count("SELECT COUNT(*) FROM demo_lesson"));
        result.put("drafts", count("SELECT COUNT(*) FROM demo_feedback WHERE status='DRAFT'"));
        result.put("confirmed", count("SELECT COUNT(*) FROM demo_feedback WHERE status='CONFIRMED'"));
        try { redis.opsForValue().set(STATS_KEY, json.writeValueAsString(result), Duration.ofSeconds(30)); }
        catch (Exception ignored) { }
        return result;
    }

    private long count(String sql) { return jdbc.queryForObject(sql, Long.class); }

    private void requireStudent(long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_student WHERE id=?", Long.class, id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "演示学员不存在");
        }
    }

    private Feedback findFeedback(long id) {
        List<Feedback> items = jdbc.query(FEEDBACK_QUERY + " WHERE f.id=?", FEEDBACK_MAPPER, id);
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "反馈不存在");
        return items.get(0);
    }

    private long insert(String sql, Object... arguments) {
        return transactions.execute(status -> {
            GeneratedKeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var statement = connection.prepareStatement(sql, new String[]{"id"});
                for (int index = 0; index < arguments.length; index++) statement.setObject(index + 1, arguments[index]);
                return statement;
            }, keys);
            return keys.getKey().longValue();
        });
    }

    private void invalidate() {
        try { redis.delete(STATS_KEY); } catch (RuntimeException ignored) { }
    }
}
