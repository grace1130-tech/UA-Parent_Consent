
package com.yearners.ua_parent_consent_backend.controller;

import com.yearners.ua_parent_consent_backend.dto.ConsentFormStudentResponse;
import com.yearners.ua_parent_consent_backend.dto.StudentResponse;
import com.yearners.ua_parent_consent_backend.model.ConsentFormStudent;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.service.ConsentFormStudentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/consent-forms")
public class ConsentFormStudentController {

    private final ConsentFormStudentService assignmentService;

    public ConsentFormStudentController(
            ConsentFormStudentService assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

    public record StudentAssignmentRequest(
            @NotNull(message = "Student IDs are required.")
            @NotEmpty(message = "Select at least one student.")
            List<@NotNull @Positive Long> studentIds
    ) {}

    // Assign selected students to a consent form.
    @PostMapping("/{formId}/students")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ConsentFormStudentResponse> assignStudents(
            @PathVariable Long formId,
            @Valid @RequestBody StudentAssignmentRequest request,
            Authentication authentication
    ) {
        try {
            return assignmentService.assignStudents(
                            formId,
                            request.studentIds(),
                            authentication.getName()
                    )
                    .stream()
                    .map(ConsentFormStudentResponse::from)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // View students assigned to a consent form.
    @GetMapping("/{formId}/students")
    public List<StudentResponse> getAssignedStudents(
            @PathVariable Long formId,
            Authentication authentication
    ) {
        try {
            return assignmentService.getAssignedStudents(
                            formId,
                            authentication.getName()
                    )
                    .stream()
                    .map(StudentResponse::from)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Replace the assigned student list.
    @PutMapping("/{formId}/students")
    public List<ConsentFormStudentResponse> replaceAssignedStudents(
            @PathVariable Long formId,
            @Valid @RequestBody StudentAssignmentRequest request,
            Authentication authentication
    ) {
        try {
            return assignmentService.replaceAssignedStudents(
                            formId,
                            request.studentIds(),
                            authentication.getName()
                    )
                    .stream()
                    .map(ConsentFormStudentResponse::from)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }
}
