
package com.yearners.ua_parent_consent_backend.repository;

import com.yearners.ua_parent_consent_backend.model.ParentStudentLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParentStudentLinkRepository
        extends JpaRepository<ParentStudentLink, Long> {

    List<ParentStudentLink> findByParentId(Long parentId);

    List<ParentStudentLink> findByParentIdAndVerificationStatus(
            Long parentId,
            String verificationStatus
    );

    Optional<ParentStudentLink> findByParentIdAndStudentId(
            Long parentId,
            Long studentId
    );

    boolean existsByParentIdAndStudentIdAndVerificationStatus(
            Long parentId,
            Long studentId,
            String verificationStatus
    );

    // Retrieve requests for students in specified sections.
    List<ParentStudentLink>
    findByVerificationStatusAndStudent_Section_IdIn(
            String verificationStatus,
            List<Long> sectionIds
    );

    // Find verified parent links for a specific student.
    List<ParentStudentLink> findByStudentIdAndVerificationStatus(
            Long studentId,
            String verificationStatus
    );
}
