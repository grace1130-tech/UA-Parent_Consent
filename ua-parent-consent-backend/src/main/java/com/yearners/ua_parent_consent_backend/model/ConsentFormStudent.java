package com.yearners.ua_parent_consent_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "consent_form_students",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_consent_form_student",
                        columnNames = {"consent_form_id", "student_id"}
                )
        }
)
public class ConsentFormStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_form_id", nullable = false)
    private ConsentForm consentForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
}