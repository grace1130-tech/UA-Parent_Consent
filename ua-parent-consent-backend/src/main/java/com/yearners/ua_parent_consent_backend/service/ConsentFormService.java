
package com.yearners.ua_parent_consent_backend.service;

import com.yearners.ua_parent_consent_backend.dto.CreateConsentFormRequest;
import com.yearners.ua_parent_consent_backend.dto.ConsentFormResponse;

import com.yearners.ua_parent_consent_backend.model.ConsentForm;
import com.yearners.ua_parent_consent_backend.model.ConsentFormStudent;
import com.yearners.ua_parent_consent_backend.model.ParentStudentLink;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.model.User;

import com.yearners.ua_parent_consent_backend.repository.ConsentFormRepository;
import com.yearners.ua_parent_consent_backend.repository.ParentStudentLinkRepository;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ConsentFormService {

    private final ConsentFormRepository consentFormRepository;
    private final UserRepository userRepository;
    private final ConsentFormStudentService consentFormStudentService;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final NotificationService notificationService;

    public ConsentFormService(
            ConsentFormRepository consentFormRepository,
            UserRepository userRepository,
            ConsentFormStudentService consentFormStudentService,
            ParentStudentLinkRepository parentStudentLinkRepository,
            NotificationService notificationService
    ) {
        this.consentFormRepository = consentFormRepository;
        this.userRepository = userRepository;
        this.consentFormStudentService = consentFormStudentService;
        this.parentStudentLinkRepository = parentStudentLinkRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConsentFormResponse createForm(
            CreateConsentFormRequest request,
            String teacherEmail,
            String status
    ) {

        User teacher = getTeacher(teacherEmail);

        if (!"Draft".equals(status) && !"Published".equals(status)) {
            throw new IllegalArgumentException(
                    "Invalid consent form status."
            );
        }

        validateDates(
                request.getActivityDate(),
                request.getDeadline()
        );

        ConsentForm form = new ConsentForm();

        form.setTitle(request.getTitle());
        form.setDescription(request.getDescription());
        form.setClassName(request.getClassName());
        form.setCourse(request.getCourse());
        form.setProfessor(request.getProfessor());
        form.setActivityDate(request.getActivityDate());
        form.setLocation(request.getLocation());
        form.setDeadline(request.getDeadline());
        form.setInstructions(request.getInstructions());
        form.setStatus(status);
        form.setTeacher(teacher);

        ConsentForm savedForm = consentFormRepository.save(form);

        List<ConsentFormStudent> assignments =
                consentFormStudentService.assignStudents(
                        savedForm.getId(),
                        request.getStudentIds(),
                        teacherEmail
                );

        if ("Published".equals(status)) {
            notifyParents(savedForm, assignments.stream()
                    .map(ConsentFormStudent::getStudent)
                    .toList());
        }

        return ConsentFormResponse.from(savedForm);
    }

    @Transactional(readOnly = true)
    public List<ConsentFormResponse> getMyForms(String teacherEmail) {

        User teacher = getTeacher(teacherEmail);

        return consentFormRepository.findByTeacher(teacher)
                .stream()
                .map(ConsentFormResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsentFormResponse getFormById(
            Long formId,
            String teacherEmail
    ) {

        User teacher = getTeacher(teacherEmail);

        ConsentForm form = getOwnedForm(formId, teacher);

        List<Student> assignedStudents =
                consentFormStudentService.getAssignedStudents(
                        formId,
                        teacherEmail
                );

        return ConsentFormResponse.from(form, assignedStudents);
    }

    // Edit an existing draft without creating a new consent form.
    @Transactional
    public ConsentFormResponse editDraft(
            Long formId,
            CreateConsentFormRequest request,
            String teacherEmail
    ) {

        User teacher = getTeacher(teacherEmail);

        ConsentForm form = getOwnedForm(formId, teacher);

        if (!"Draft".equalsIgnoreCase(form.getStatus())) {
            throw new IllegalArgumentException(
                    "Only draft consent forms can be edited."
            );
        }

        validateDates(
                request.getActivityDate(),
                request.getDeadline()
        );

        // Update the existing consent form.
        form.setTitle(request.getTitle());
        form.setDescription(request.getDescription());
        form.setClassName(request.getClassName());
        form.setCourse(request.getCourse());
        form.setProfessor(request.getProfessor());
        form.setActivityDate(request.getActivityDate());
        form.setLocation(request.getLocation());
        form.setDeadline(request.getDeadline());
        form.setInstructions(request.getInstructions());

        // Save changes to the same database record.
        ConsentForm updatedForm =
                consentFormRepository.save(form);

        // Replace the assigned students.
        consentFormStudentService.replaceAssignedStudents(
                formId,
                request.getStudentIds(),
                teacherEmail
        );

        List<Student> updatedStudents =
                consentFormStudentService.getAssignedStudents(
                        formId,
                        teacherEmail
                );

        return ConsentFormResponse.from(
                updatedForm,
                updatedStudents
        );
    }

    // Publish an existing draft without creating a duplicate form.
    @Transactional
    public ConsentFormResponse publishDraft(
            Long formId,
            String teacherEmail
    ) {

        User teacher = getTeacher(teacherEmail);

        ConsentForm form = getOwnedForm(formId, teacher);

        if (!"Draft".equalsIgnoreCase(form.getStatus())) {
            throw new IllegalArgumentException(
                    "Only draft consent forms can be published."
            );
        }

        validateDates(
                form.getActivityDate(),
                form.getDeadline()
        );

        List<Student> assignedStudents =
                consentFormStudentService.getAssignedStudents(
                        formId,
                        teacherEmail
                );

        if (assignedStudents.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot publish a consent form without assigned students."
            );
        }

        form.setStatus("Published");

        ConsentForm publishedForm =
                consentFormRepository.save(form);

        notifyParents(publishedForm, assignedStudents);

        return ConsentFormResponse.from(publishedForm);
    }

    // Notify parents who have verified links to assigned students.
    private void notifyParents(
            ConsentForm form,
            List<Student> assignedStudents
    ) {

        Set<Long> notifiedParentIds = new LinkedHashSet<>();

        for (Student student : assignedStudents) {

            List<ParentStudentLink> verifiedLinks =
                    parentStudentLinkRepository
                            .findByStudentIdAndVerificationStatus(
                                    student.getId(),
                                    "VERIFIED"
                            );

            for (ParentStudentLink link : verifiedLinks) {

                User parent = link.getParent();

                if (parent == null) {
                    continue;
                }

                if (notifiedParentIds.add(parent.getId())) {

                    notificationService.createNotification(
                            parent.getId(),
                            "New Consent Form",
                            "A new consent form titled \""
                                    + form.getTitle()
                                    + "\" has been published for "
                                    + student.getFullName()
                                    + ". Please review and submit your response.",
                            "NEW_FORM"
                    );
                }
            }
        }
    }

    // Validate activity date and submission deadline.
    private void validateDates(
            LocalDate activityDate,
            LocalDate deadline
    ) {

        if (deadline.isAfter(activityDate)) {
            throw new IllegalArgumentException(
                    "The submission deadline cannot be after the activity date."
            );
        }

        if (activityDate.isBefore(LocalDate.now())
                || deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Activity date and deadline cannot be in the past."
            );
        }
    }

    // Retrieve a consent form and verify teacher ownership.
    private ConsentForm getOwnedForm(
            Long formId,
            User teacher
    ) {

        ConsentForm form = consentFormRepository.findById(formId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Consent form not found."
                        )
                );

        if (form.getTeacher() == null
                || !form.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException(
                    "You can only manage your own consent forms."
            );
        }

        return form;
    }

    // Validate authenticated teacher.
    private User getTeacher(String teacherEmail) {

        if (teacherEmail == null || teacherEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated teacher email is missing."
            );
        }

        String normalizedEmail =
                teacherEmail.trim().toLowerCase(Locale.ROOT);

        User teacher = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Teacher account not found."
                        )
                );

        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            throw new IllegalArgumentException(
                    "Only teachers can manage consent forms."
            );
        }

        return teacher;
    }
}
