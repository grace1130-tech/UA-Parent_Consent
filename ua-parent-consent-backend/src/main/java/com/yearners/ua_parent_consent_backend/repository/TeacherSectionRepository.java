
package com.yearners.ua_parent_consent_backend.repository;

import com.yearners.ua_parent_consent_backend.model.TeacherSection;
import com.yearners.ua_parent_consent_backend.model.TeacherSectionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherSectionRepository
        extends JpaRepository<TeacherSection, TeacherSectionId> {

    List<TeacherSection> findByTeacherId(Long teacherId);

    List<TeacherSection> findBySectionId(Long sectionId);

    boolean existsByTeacherIdAndSectionId(Long teacherId, Long sectionId);

    void deleteByTeacherIdAndSectionId(Long teacherId, Long sectionId);
}
