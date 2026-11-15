
package com.yearners.ua_parent_consent_backend.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TeacherSectionId implements Serializable {

    private Long teacherId;

    private Long sectionId;
}
