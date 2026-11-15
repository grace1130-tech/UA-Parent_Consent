
package com.yearners.ua_parent_consent_backend.service;

import com.yearners.ua_parent_consent_backend.model.ConsentForm;
import com.yearners.ua_parent_consent_backend.model.ConsentFormStudent;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.model.User;

import com.yearners.ua_parent_consent_backend.repository.ConsentFormRepository;
import com.yearners.ua_parent_consent_backend.repository.ConsentFormStudentRepository;
import com.yearners.ua_parent_consent_backend.repository.StudentRepository;
import com.yearners.ua_parent_consent_backend.repository.TeacherSectionRepository;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ConsentFormStudentService {

    private final ConsentFormRepository consentFormRepository;
    private final ConsentFormStudentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final UserRepository userRepository;

    public ConsentFormStudentService(
            ConsentFormRepository consentFormRepository,
            ConsentFormStudentRepository assignmentRepository,
            StudentRepository studentRepository,
            TeacherSectionRepository teacherSectionRepository,
            UserRepository userRepository
    ) {
        this.consentFormRepository = consentFormRepository;
        this.assignmentRepository = assignmentRepository;
        this.studentRepository = studentRepository;
        this.teacherSectionRepository = teacherSectionRepository;
        this.userRepository = userRepository;
    }

    // Assign selected students to a teacher's consent form.
    @Transactional
    public List<ConsentFormStudent> assignStudents(
            Long formId,
            List<Long> studentIds,
            String teacherEmail
    ) {
        User teacher = getTeacher(teacherEmail);

        ConsentForm form = consentFormRepository.findById(formId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Consent form not found.")
                );

        if (form.getTeacher() == null
                || !form.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException(
                    "You can only manage your own consent forms."
            );
        }

        if (studentIds == null || studentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select at least one student."
            );
        }

        Set<Long> uniqueStudentIds = new LinkedHashSet<>(studentIds);

        List<ConsentFormStudent> assignments = new ArrayList<>();

        for (Long studentId : uniqueStudentIds) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Student not found: " + studentId
                            )
                    );

            if (!Boolean.TRUE.equals(student.getActive())) {
                throw new IllegalArgumentException(
                        "Inactive students cannot be assigned."
                );
            }

            boolean assignedToTeacher =
                    teacherSectionRepository.existsByTeacherIdAndSectionId(
                            teacher.getId(),
                            student.getSection().getId()
                    );

            if (!assignedToTeacher) {
                throw new IllegalArgumentException(
                        "You cannot assign students outside your sections."
                );
            }

            if (assignmentRepository.existsByConsentFormIdAndStudentId(
                    formId,
                    studentId
            )) {
                throw new IllegalArgumentException(
                        "A selected student is already assigned to this form."
                );
            }

            ConsentFormStudent assignment = new ConsentFormStudent();
            assignment.setConsentForm(form);
            assignment.setStudent(student);

            assignments.add(assignmentRepository.save(assignment));
        }

        return assignments;
    }

    // View students assigned to a consent form.
    @Transactional(readOnly = true)
    public List<Student> getAssignedStudents(
            Long formId,
            String teacherEmail
    ) {
        User teacher = getTeacher(teacherEmail);

        ConsentForm form = consentFormRepository.findById(formId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Consent form not found.")
                );

        if (form.getTeacher() == null
                || !form.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException(
                    "You can only view your own consent forms."
            );
        }

        return assignmentRepository.findByConsentFormId(formId)
                .stream()
                .map(ConsentFormStudent::getStudent)
                .toList();
    }

    // Replace all assigned students when editing a form.
    @Transactional
    public List<ConsentFormStudent> replaceAssignedStudents(
            Long formId,
            List<Long> studentIds,
            String teacherEmail
    ) {
        User teacher = getTeacher(teacherEmail);

        ConsentForm form = consentFormRepository.findById(formId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Consent form not found.")
                );

        if (form.getTeacher() == null
                || !form.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException(
                    "You can only manage your own consent forms."
            );
        }

        if (studentIds == null || studentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select at least one student."
            );
        }

        Set<Long> uniqueStudentIds = new LinkedHashSet<>(studentIds);

        // Validate every student before changing existing assignments.
        List<Student> selectedStudents = new ArrayList<>();

        for (Long studentId : uniqueStudentIds) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Student not found: " + studentId
                            )
                    );

            if (!Boolean.TRUE.equals(student.getActive())) {
                throw new IllegalArgumentException(
                        "Inactive students cannot be assigned."
                );
            }

            boolean assignedToTeacher =
                    teacherSectionRepository.existsByTeacherIdAndSectionId(
                            teacher.getId(),
                            student.getSection().getId()
                    );

            if (!assignedToTeacher) {
                throw new IllegalArgumentException(
                        "You cannot assign students outside your sections."
                );
            }

            selectedStudents.add(student);
        }

        assignmentRepository.deleteByConsentFormId(formId);

        List<ConsentFormStudent> newAssignments = new ArrayList<>();

        for (Student student : selectedStudents) {
            ConsentFormStudent assignment = new ConsentFormStudent();
            assignment.setConsentForm(form);
            assignment.setStudent(student);

            newAssignments.add(assignmentRepository.save(assignment));
        }

        return newAssignments;
    }

    private User getTeacher(String teacherEmail) {

        if (teacherEmail == null || teacherEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated teacher email is missing."
            );
        }

        String email = teacherEmail.trim().toLowerCase(Locale.ROOT);

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Teacher account not found.")
                );

        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            throw new IllegalArgumentException(
                    "Only teachers can manage consent form assignments."
            );
        }

        return teacher;
    }
}
