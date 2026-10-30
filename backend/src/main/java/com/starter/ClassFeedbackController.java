package com.starter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ClassFeedbackController {
    private final ClassFeedbackService service;
    public ClassFeedbackController(ClassFeedbackService service) { this.service = service; }
    public record StudentInput(@NotBlank @Size(max=40) String name) {}
    public record LessonInput(@NotNull @Positive Long studentId, @NotBlank @Size(max=100) String topic,
        @NotBlank @Size(max=2000) String performance, @Size(max=1000) String homework) {}
    public record ContentInput(@NotBlank @Size(max=4000) String content) {}

    @GetMapping("/students") public List<ClassFeedbackService.Student> students() { return service.students(); }
    @PostMapping("/students") @ResponseStatus(HttpStatus.CREATED)
    public ClassFeedbackService.Student createStudent(@Valid @RequestBody StudentInput request) { return service.addStudent(request.name()); }
    @GetMapping("/lessons") public List<ClassFeedbackService.Lesson> lessons() { return service.lessons(); }
    @PostMapping("/lessons") @ResponseStatus(HttpStatus.CREATED)
    public ClassFeedbackService.Lesson createLesson(@Valid @RequestBody LessonInput request) { return service.addLesson(request.studentId(), request.topic(), request.performance(), request.homework()); }
    @GetMapping("/feedback") public List<ClassFeedbackService.Feedback> feedback() { return service.feedbacks(); }
    @PostMapping("/lessons/{id}/feedback") public ClassFeedbackService.Feedback generate(@PathVariable long id) { return service.generate(id); }
    @PutMapping("/feedback/{id}") public ClassFeedbackService.Feedback edit(@PathVariable long id, @Valid @RequestBody ContentInput request) { return service.edit(id, request.content()); }
    @PostMapping("/feedback/{id}/confirm") public ClassFeedbackService.Feedback confirm(@PathVariable long id) { return service.confirm(id); }
    @GetMapping("/students/{id}/feedback") public List<ClassFeedbackService.Feedback> published(@PathVariable long id) { return service.published(id); }
    @GetMapping("/stats") public Map<String, Long> stats() { return service.stats(); }
}
