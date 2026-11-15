
package com.yearners.ua_parent_consent_backend.service;

import com.yearners.ua_parent_consent_backend.dto.SubmitConsentRequest;
import com.yearners.ua_parent_consent_backend.dto.ParentConsentResponseDto;
import com.yearners.ua_parent_consent_backend.model.ConsentForm;
import com.yearners.ua_parent_consent_backend.model.ParentConsentResponse;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.model.User;
import com.yearners.ua_parent_consent_backend.repository.ConsentFormRepository;
import com.yearners.ua_parent_consent_backend.repository.ConsentFormStudentRepository;
import com.yearners.ua_parent_consent_backend.repository.ParentConsentResponseRepository;
import com.yearners.ua_parent_consent_backend.repository.ParentStudentLinkRepository;
import com.yearners.ua_parent_consent_backend.repository.StudentRepository;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ParentConsentResponseService {

    private static final String ENCRYPTED_PREFIX = "ENC:";

    private final ParentConsentResponseRepository responseRepository;
    private final ConsentFormRepository consentFormRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final ConsentFormStudentRepository consentFormStudentRepository;
    private final AesEncryptionService aesEncryptionService;

    public ParentConsentResponseService(
            ParentConsentResponseRepository responseRepository,
            ConsentFormRepository consentFormRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            ParentStudentLinkRepository parentStudentLinkRepository,
            ConsentFormStudentRepository consentFormStudentRepository,
            AesEncryptionService aesEncryptionService
    ) {
        this.responseRepository = responseRepository;
        this.consentFormRepository = consentFormRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.parentStudentLinkRepository = parentStudentLinkRepository;
        this.consentFormStudentRepository = consentFormStudentRepository;
        this.aesEncryptionService = aesEncryptionService;
    }

    @Transactional
    public ParentConsentResponse submitResponse(
            Long formId,
            SubmitConsentRequest request,
            String parentEmail
    ) {
        String email = parentEmail.trim().toLowerCase(Locale.ROOT);

        User parent = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Parent account not found.")
                );

        if (!"parent".equalsIgnoreCase(parent.getRole())) {
            throw new IllegalArgumentException(
                    "Only parent accounts can submit consent."
            );
        }

        ConsentForm form = consentFormRepository.findById(formId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Consent form not found.")
                );

        if (!"Published".equalsIgnoreCase(form.getStatus())) {
            throw new IllegalArgumentException(
                    "This consent form is not available."
            );
        }

        // Check whether the submission deadline has passed.
        // Submissions are allowed on the deadline date.
        if (form.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "The submission deadline for this consent form has passed."
            );
        }

        if (request.getStudentId() == null) {
            throw new IllegalArgumentException(
                    "Please select a student."
            );
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found.")
                );

        if (!Boolean.TRUE.equals(student.getActive())) {
            throw new IllegalArgumentException(
                    "This student is inactive."
            );
        }

        // Verify that this parent is linked to this student.
        boolean verifiedLink =
                parentStudentLinkRepository
                        .existsByParentIdAndStudentIdAndVerificationStatus(
                                parent.getId(),
                                student.getId(),
                                "VERIFIED"
                        );

        if (!verifiedLink) {
            throw new IllegalArgumentException(
                    "Your relationship with this student has not been verified."
            );
        }

        // Verify that this student was assigned to this consent form.
        boolean assigned =
                consentFormStudentRepository
                        .existsByConsentFormIdAndStudentId(
                                formId,
                                student.getId()
                        );

        if (!assigned) {
            throw new IllegalArgumentException(
                    "This consent form is not assigned to this student."
            );
        }

        // Check duplicate response for this parent, form, and student.
        if (responseRepository
                .existsByParentIdAndConsentFormIdAndStudentId(
                        parent.getId(),
                        formId,
                        student.getId()
                )) {
            throw new IllegalArgumentException(
                    "You have already submitted a response for this student."
            );
        }

        ParentConsentResponse response = new ParentConsentResponse();

        response.setParent(parent);
        response.setConsentForm(form);
        response.setStudent(student);
        response.setDecision(request.getDecision());

        // Encrypt sensitive information before saving.
        response.setPrintedName(
                encryptValue(request.getPrintedName().trim())
        );

        response.setSignature(
                encryptValue(request.getSignature())
        );

        response.setSubmittedAt(LocalDateTime.now());

        return responseRepository.save(response);
    }

    @Transactional(readOnly = true)
    public List<ParentConsentResponseDto> getMyResponses(
            String parentEmail
    ) {
        String email = parentEmail.trim().toLowerCase(Locale.ROOT);

        User parent = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Parent account not found.")
                );

        if (!"parent".equalsIgnoreCase(parent.getRole())) {
            throw new IllegalArgumentException(
                    "Only parent accounts can view their responses."
            );
        }

        return responseRepository.findByParentId(parent.getId())
                .stream()
                .map(response -> {
                    ParentConsentResponseDto dto =
                            ParentConsentResponseDto.from(response);

                    // Decrypt printed name.
                    dto.setPrintedName(
                            decryptValue(response.getPrintedName())
                    );

                    // Decrypt signature.
                    dto.setSignature(
                            decryptValue(response.getSignature())
                    );

                    return dto;
                })
                .toList();
    }

    private String encryptValue(String value) {
        if (value == null) {
            return null;
        }

        return ENCRYPTED_PREFIX + aesEncryptionService.encrypt(value);
    }

    private String decryptValue(String value) {
        if (value == null) {
            return null;
        }

        // Keep older, unencrypted records readable.
        if (!value.startsWith(ENCRYPTED_PREFIX)) {
            return value;
        }

        String encryptedValue =
                value.substring(ENCRYPTED_PREFIX.length());

        return aesEncryptionService.decrypt(encryptedValue);
    }
}
