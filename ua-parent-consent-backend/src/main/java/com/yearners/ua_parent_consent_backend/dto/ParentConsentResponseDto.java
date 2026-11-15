
package com.yearners.ua_parent_consent_backend.dto;

import com.yearners.ua_parent_consent_backend.model.ParentConsentResponse;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParentConsentResponseDto {

    private Long id;
    private Long formId;
    private String formTitle;

    private Long studentId;
    private String studentName;
    private String studentNumber;
    private String studentSection;

    private String decision;
    private String printedName;
    private String signature;
    private LocalDateTime submittedAt;

    public static ParentConsentResponseDto from(
            ParentConsentResponse response
    ) {
        ParentConsentResponseDto dto = new ParentConsentResponseDto();

        dto.setId(response.getId());
        dto.setFormId(response.getConsentForm().getId());
        dto.setFormTitle(response.getConsentForm().getTitle());

        if (response.getStudent() != null) {
            dto.setStudentId(response.getStudent().getId());
            dto.setStudentName(response.getStudent().getFullName());
            dto.setStudentNumber(response.getStudent().getStudentNumber());

            if (response.getStudent().getSection() != null) {
                dto.setStudentSection(
                        response.getStudent().getSection().getName()
                );
            }
        }

        dto.setDecision(response.getDecision());
        dto.setPrintedName(response.getPrintedName());
        dto.setSignature(response.getSignature());
        dto.setSubmittedAt(response.getSubmittedAt());

        return dto;
    }
}
