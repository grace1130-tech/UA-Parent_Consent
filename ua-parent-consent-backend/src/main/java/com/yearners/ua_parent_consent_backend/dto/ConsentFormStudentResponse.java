
package com.yearners.ua_parent_consent_backend.dto;

import com.yearners.ua_parent_consent_backend.model.ConsentFormStudent;
import lombok.Data;

@Data
public class ConsentFormStudentResponse {

    private Long id;
    private Long consentFormId;
    private Long studentId;
    private String studentNumber;
    private String studentName;
    private String sectionName;

    public static ConsentFormStudentResponse from(
            ConsentFormStudent assignment
    ) {
        ConsentFormStudentResponse response =
                new ConsentFormStudentResponse();

        response.setId(assignment.getId());

        response.setConsentFormId(
                assignment.getConsentForm().getId()
        );

        response.setStudentId(
                assignment.getStudent().getId()
        );

        response.setStudentNumber(
                assignment.getStudent().getStudentNumber()
        );

        response.setStudentName(
                assignment.getStudent().getFullName()
        );

        response.setSectionName(
                assignment.getStudent().getSection().getName()
        );

        return response;
    }
}
