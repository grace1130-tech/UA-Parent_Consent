
package com.yearners.ua_parent_consent_backend.repository;

import com.yearners.ua_parent_consent_backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentNumberIgnoreCase(String studentNumber);

    List<Student> findBySectionIdAndActiveTrue(Long sectionId);

    List<Student> findBySectionId(Long sectionId);

    boolean existsByStudentNumberIgnoreCase(String studentNumber);
}
