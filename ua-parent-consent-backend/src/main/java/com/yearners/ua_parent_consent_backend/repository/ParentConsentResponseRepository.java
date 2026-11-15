
package com.yearners.ua_parent_consent_backend.repository;

import com.yearners.ua_parent_consent_backend.model.ParentConsentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParentConsentResponseRepository
        extends JpaRepository<ParentConsentResponse, Long> {

    // Get all responses submitted by a specific parent
    List<ParentConsentResponse> findByParentId(Long parentId);

    // Get all responses for a specific consent form
    List<ParentConsentResponse> findByConsentFormId(Long consentFormId);

    // Get responses for a specific parent, form, and student
    Optional<ParentConsentResponse> findByParentIdAndConsentFormIdAndStudentId(
            Long parentId,
            Long consentFormId,
            Long studentId
    );

    // Check whether a parent already responded for a specific student
    boolean existsByParentIdAndConsentFormIdAndStudentId(
            Long parentId,
            Long consentFormId,
            Long studentId
    );

    // Get responses belonging to a specific teacher
    List<ParentConsentResponse> findByConsentForm_Teacher_Id(Long teacherId);

    // Get responses for a specific student
    List<ParentConsentResponse> findByStudentId(Long studentId);
}
