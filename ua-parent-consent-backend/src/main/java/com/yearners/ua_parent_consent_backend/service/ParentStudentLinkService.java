
package com.yearners.ua_parent_consent_backend.service;

import com.yearners.ua_parent_consent_backend.model.ParentStudentLink;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.model.User;
import com.yearners.ua_parent_consent_backend.repository.ParentStudentLinkRepository;
import com.yearners.ua_parent_consent_backend.repository.StudentRepository;
import com.yearners.ua_parent_consent_backend.repository.TeacherSectionRepository;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ParentStudentLinkService {

    private final ParentStudentLinkRepository linkRepository;
    private final StudentRepository studentRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final UserRepository userRepository;

    public ParentStudentLinkService(
            ParentStudentLinkRepository linkRepository,
            StudentRepository studentRepository,
            TeacherSectionRepository teacherSectionRepository,
            UserRepository userRepository
    ) {
        this.linkRepository = linkRepository;
        this.studentRepository = studentRepository;
        this.teacherSectionRepository = teacherSectionRepository;
        this.userRepository = userRepository;
    }

    // Parent requests to link their account to a student.
    @Transactional
    public ParentStudentLink requestLink(
            String studentNumber,
            String relationship,
            String parentEmail
    ) {
        User parent = getUser(parentEmail, "parent");

        if (studentNumber == null || studentNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Student number is required."
            );
        }

        String normalizedNumber = studentNumber.trim();

        Student student = studentRepository
                .findByStudentNumberIgnoreCase(normalizedNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found.")
                );

        if (!Boolean.TRUE.equals(student.getActive())) {
            throw new IllegalArgumentException(
                    "This student is currently inactive."
            );
        }

        if (linkRepository.findByParentIdAndStudentId(
                parent.getId(),
                student.getId()
        ).isPresent()) {
            throw new IllegalArgumentException(
                    "A link request for this student already exists."
            );
        }

        ParentStudentLink link = new ParentStudentLink();
        link.setParent(parent);
        link.setStudent(student);
        link.setRelationship(
                relationship == null || relationship.isBlank()
                        ? null
                        : relationship.trim()
        );
        link.setVerificationStatus("PENDING");

        return linkRepository.save(link);
    }

    // Parent views their own children and request statuses.
    @Transactional(readOnly = true)
    public List<ParentStudentLink> getMyLinks(String parentEmail) {
        User parent = getUser(parentEmail, "parent");

        return linkRepository.findByParentId(parent.getId());
    }

    // Teacher views pending requests in assigned sections.
    @Transactional(readOnly = true)
    public List<ParentStudentLink> getPendingRequests(
            String teacherEmail
    ) {
        User teacher = getUser(teacherEmail, "teacher");

        List<Long> assignedSectionIds =
                teacherSectionRepository.findByTeacherId(teacher.getId())
                        .stream()
                        .map(assignment -> assignment.getSection().getId())
                        .toList();

        if (assignedSectionIds.isEmpty()) {
            return List.of();
        }

        return linkRepository
                .findByVerificationStatusAndStudent_Section_IdIn(
                        "PENDING",
                        assignedSectionIds
                );
    }

    // Teacher views approved and rejected requests in assigned sections.
    @Transactional(readOnly = true)
    public List<ParentStudentLink> getReviewedRequests(
            String teacherEmail
    ) {
        User teacher = getUser(teacherEmail, "teacher");

        List<Long> assignedSectionIds =
                teacherSectionRepository.findByTeacherId(teacher.getId())
                        .stream()
                        .map(assignment -> assignment.getSection().getId())
                        .toList();

        if (assignedSectionIds.isEmpty()) {
            return List.of();
        }

        List<ParentStudentLink> verified =
                linkRepository.findByVerificationStatusAndStudent_Section_IdIn(
                        "VERIFIED",
                        assignedSectionIds
                );

        List<ParentStudentLink> rejected =
                linkRepository.findByVerificationStatusAndStudent_Section_IdIn(
                        "REJECTED",
                        assignedSectionIds
                );

        return java.util.stream.Stream.concat(
                verified.stream(),
                rejected.stream()
        ).toList();
    }

    // Teacher approves or rejects a pending request.
    @Transactional
    public ParentStudentLink reviewRequest(
            Long linkId,
            String decision,
            String teacherEmail
    ) {
        User teacher = getUser(teacherEmail, "teacher");

        if (linkId == null || linkId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid link request ID."
            );
        }

        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException(
                    "Decision is required."
            );
        }

        ParentStudentLink link = linkRepository.findById(linkId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Parent-child link request not found."
                        )
                );

        if (!"PENDING".equalsIgnoreCase(
                link.getVerificationStatus()
        )) {
            throw new IllegalArgumentException(
                    "This request has already been reviewed."
            );
        }

        Long sectionId = link.getStudent().getSection().getId();

        boolean assigned = teacherSectionRepository
                .existsByTeacherIdAndSectionId(
                        teacher.getId(),
                        sectionId
                );

        if (!assigned) {
            throw new IllegalArgumentException(
                    "You are not assigned to this student's section."
            );
        }

        String normalizedDecision = decision.trim().toUpperCase(
                Locale.ROOT
        );

        if (!normalizedDecision.equals("APPROVE")
                && !normalizedDecision.equals("REJECT")) {
            throw new IllegalArgumentException(
                    "Decision must be APPROVE or REJECT."
            );
        }

        link.setVerificationStatus(
                normalizedDecision.equals("APPROVE")
                        ? "VERIFIED"
                        : "REJECTED"
        );

        link.setVerifiedBy(teacher);
        link.setVerifiedAt(LocalDateTime.now());

        return linkRepository.save(link);
    }

    private User getUser(String email, String requiredRole) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "User email is required."
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User account not found.")
                );

        if (user.getRole() == null
                || !requiredRole.equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                    "Only " + requiredRole + " accounts can perform this action."
            );
        }

        return user;
    }
}
