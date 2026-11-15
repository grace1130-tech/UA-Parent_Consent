
package com.yearners.ua_parent_consent_backend.controller;

import com.yearners.ua_parent_consent_backend.dto.ConsentFormResponse;
import com.yearners.ua_parent_consent_backend.model.ConsentForm;
import com.yearners.ua_parent_consent_backend.model.ParentStudentLink;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.model.User;

import com.yearners.ua_parent_consent_backend.repository.ConsentFormRepository;
import com.yearners.ua_parent_consent_backend.repository.ParentStudentLinkRepository;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/parent/consent-forms")
public class ParentConsentFormController {

    private final ConsentFormRepository consentFormRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final UserRepository userRepository;

    public ParentConsentFormController(
            ConsentFormRepository consentFormRepository,
            ParentStudentLinkRepository parentStudentLinkRepository,
            UserRepository userRepository
    ) {
        this.consentFormRepository = consentFormRepository;
        this.parentStudentLinkRepository = parentStudentLinkRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ConsentFormResponse> getPublishedForms(
            Authentication authentication
    ) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated parent information is missing."
            );
        }

        String email = authentication.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        User parent = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Parent account not found.")
                );

        if (!"PARENT".equalsIgnoreCase(parent.getRole())) {
            throw new IllegalArgumentException(
                    "Only parent accounts can view assigned consent forms."
            );
        }

        List<ParentStudentLink> verifiedLinks =
                parentStudentLinkRepository
                        .findByParentIdAndVerificationStatus(
                                parent.getId(),
                                "VERIFIED"
                        );

        List<ConsentFormResponse> results = new ArrayList<>();

        for (ParentStudentLink link : verifiedLinks) {

            Student student = link.getStudent();

            if (student == null) {
                continue;
            }

            List<ConsentForm> assignedForms =
                    consentFormRepository
                            .findPublishedFormsByStudentId(student.getId());

            for (ConsentForm form : assignedForms) {
                results.add(
                        ConsentFormResponse.from(form, student)
                );
            }
        }

        return results;
    }
}
