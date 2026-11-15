
package com.yearners.ua_parent_consent_backend.controller;

import com.yearners.ua_parent_consent_backend.dto.StudentResponse;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.service.StudentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/students")
@Validated
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Request body for adding or updating a student.
    public record StudentRequest(

            @NotBlank(message = "Student number is required.")
            String studentNumber,

            @NotBlank(message = "Student name is required.")
            String fullName,

            @NotNull(message = "Section is required.")
            @Positive(message = "Invalid section ID.")
            Long sectionId

    ) {}

    // Get active students in a teacher's assigned section.
    @GetMapping("/section/{sectionId}")
    public List<StudentResponse> getStudentsBySection(
            @PathVariable @Positive(message = "Invalid section ID.")
            Long sectionId,
            Authentication authentication
    ) {
        return studentService.getStudentsBySection(
                        sectionId,
                        authentication.getName()
                )
                .stream()
                .map(StudentResponse::from)
                .toList();
    }

    // Add a student manually.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse addStudent(
            @Valid @RequestBody StudentRequest request,
            Authentication authentication
    ) {
        Student student = studentService.addStudent(
                request.studentNumber(),
                request.fullName(),
                request.sectionId(),
                authentication.getName()
        );

        return StudentResponse.from(student);
    }

    // Update student information.
    @PutMapping("/{studentId}")
    public StudentResponse updateStudent(
            @PathVariable @Positive(message = "Invalid student ID.")
            Long studentId,
            @Valid @RequestBody StudentRequest request,
            Authentication authentication
    ) {
        Student student = studentService.updateStudent(
                studentId,
                request.studentNumber(),
                request.fullName(),
                request.sectionId(),
                authentication.getName()
        );

        return StudentResponse.from(student);
    }

    // Deactivate a student without deleting historical records.
    @PatchMapping("/{studentId}/deactivate")
    public Map<String, Object> deactivateStudent(
            @PathVariable @Positive(message = "Invalid student ID.")
            Long studentId,
            Authentication authentication
    ) {
        Student student = studentService.deactivateStudent(
                studentId,
                authentication.getName()
        );

        StudentResponse response = StudentResponse.from(student);

        return Map.of(
                "message", "Student deactivated successfully.",
                "student", response
        );
    }
}
