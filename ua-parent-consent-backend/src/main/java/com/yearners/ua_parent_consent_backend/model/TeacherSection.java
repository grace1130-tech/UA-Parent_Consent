
package com.yearners.ua_parent_consent_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "teacher_sections")
public class TeacherSection {

    @EmbeddedId
    private TeacherSectionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sectionId")
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    public TeacherSection(User teacher, Section section) {
        this.teacher = teacher;
        this.section = section;

        this.id = new TeacherSectionId(
                teacher.getId(),
                section.getId()
        );
    }
}
