
package com.yearners.ua_parent_consent_backend.dto;

import com.yearners.ua_parent_consent_backend.model.ConsentForm;
import com.yearners.ua_parent_consent_backend.model.Student;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConsentFormResponse {

    private Long id;
    private String title;
    private String description;
    private String className;
    private String course;
    private String professor;
    private LocalDate activityDate;
    private String location;
    private LocalDate deadline;
    private String instructions;
    private String status;
    private Long teacherId;
    private String teacherName;
    private LocalDateTime createdAt;

    // Assigned student information
    private Long studentId;
    private String studentName;
    private String studentNumber;
    private String studentSection;

    // IDs of all students assigned to this form
    private List<Long> studentIds;

    // Existing method - preserves compatibility
    public static ConsentFormResponse from(ConsentForm form) {

        ConsentFormResponse response = new ConsentFormResponse();

        response.setId(form.getId());
        response.setTitle(form.getTitle());
        response.setDescription(form.getDescription());
        response.setClassName(form.getClassName());
        response.setCourse(form.getCourse());
        response.setProfessor(form.getProfessor());
        response.setActivityDate(form.getActivityDate());
        response.setLocation(form.getLocation());
        response.setDeadline(form.getDeadline());
        response.setInstructions(form.getInstructions());
        response.setStatus(form.getStatus());
        response.setTeacherId(form.getTeacher().getId());
        response.setTeacherName(form.getTeacher().getFullName());
        response.setCreatedAt(form.getCreatedAt());

        return response;
    }

    // Includes all assigned student IDs
    public static ConsentFormResponse from(
            ConsentForm form,
            List<Student> students
    ) {

        ConsentFormResponse response = from(form);

        response.setStudentIds(
                students.stream()
                        .map(Student::getId)
                        .toList()
        );

        return response;
    }

    // Includes individual assigned student information
    public static ConsentFormResponse from(
            ConsentForm form,
            Student student
    ) {

        ConsentFormResponse response = from(form);

        response.setStudentId(student.getId());
        response.setStudentName(student.getFullName());
        response.setStudentNumber(student.getStudentNumber());

        if (student.getSection() != null) {
            response.setStudentSection(
                    student.getSection().getName()
            );
        }

        return response;
    }
}
