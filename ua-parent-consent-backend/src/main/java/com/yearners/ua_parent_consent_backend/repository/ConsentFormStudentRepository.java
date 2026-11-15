
package com.yearners.ua_parent_consent_backend.repository;

import com.yearners.ua_parent_consent_backend.model.ConsentFormStudent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ConsentFormStudentRepository
        extends JpaRepository<ConsentFormStudent, Long> {

    List<ConsentFormStudent> findByConsentFormId(Long consentFormId);

    List<ConsentFormStudent> findByStudentId(Long studentId);

    boolean existsByConsentFormIdAndStudentId(
            Long consentFormId,
            Long studentId
    );

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query("""
            DELETE FROM ConsentFormStudent assignment
            WHERE assignment.consentForm.id = :consentFormId
            """)
    void deleteByConsentFormId(
            @Param("consentFormId") Long consentFormId
    );
}
