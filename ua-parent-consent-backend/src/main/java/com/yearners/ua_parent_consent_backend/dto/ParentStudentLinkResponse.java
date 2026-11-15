
package com.yearners.ua_parent_consent_backend.dto;

import com.yearners.ua_parent_consent_backend.model.ParentStudentLink;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParentStudentLinkResponse {

    private Long id;

    private Long parentId;
    private String parentName;

    private Long studentId;
    private String studentNumber;
    private String studentName;
    private String sectionName;

    private String relationship;
    private String verificationStatus;

    private Long verifiedById;
    private String verifiedByName;
    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    public static ParentStudentLinkResponse from(ParentStudentLink link) {

        ParentStudentLinkResponse response =
                new ParentStudentLinkResponse();

        response.setId(link.getId());

        response.setParentId(link.getParent().getId());
        response.setParentName(link.getParent().getFullName());

        response.setStudentId(link.getStudent().getId());
        response.setStudentNumber(
                link.getStudent().getStudentNumber()
        );
        response.setStudentName(
                link.getStudent().getFullName()
        );
        response.setSectionName(
                link.getStudent().getSection().getName()
        );

        response.setRelationship(link.getRelationship());
        response.setVerificationStatus(
                link.getVerificationStatus()
        );

        if (link.getVerifiedBy() != null) {
            response.setVerifiedById(
                    link.getVerifiedBy().getId()
            );
            response.setVerifiedByName(
                    link.getVerifiedBy().getFullName()
            );
        }

        response.setVerifiedAt(link.getVerifiedAt());
        response.setCreatedAt(link.getCreatedAt());

        return response;
    }
}
