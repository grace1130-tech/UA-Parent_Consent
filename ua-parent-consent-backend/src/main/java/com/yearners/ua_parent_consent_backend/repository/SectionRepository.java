
package com.yearners.ua_parent_consent_backend.repository;

import com.yearners.ua_parent_consent_backend.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    Optional<Section> findByNameIgnoreCase(String name);

}
