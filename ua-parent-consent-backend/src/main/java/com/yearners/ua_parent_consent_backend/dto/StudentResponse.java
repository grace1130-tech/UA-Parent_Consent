
package com.yearners.ua_parent_consent_backend.dto;

import com.yearners.ua_parent_consent_backend.model.Student;
import lombok.Data;

@Data
public class StudentResponse {

    private Long id;
    private String studentNumber;
    private String fullName;
    private Long sectionId;
    private String sectionName;
    private Boolean active;

    public static StudentResponse from(Student student) {
        StudentResponse response = new StudentResponse();

        response.setId(student.getId());
        response.setStudentNumber(student.getStudentNumber());
        response.setFullName(student.getFullName());
        response.setSectionId(student.getSection().getId());
        response.setSectionName(student.getSection().getName());
        response.setActive(student.getActive());

        return response;
    }
}
