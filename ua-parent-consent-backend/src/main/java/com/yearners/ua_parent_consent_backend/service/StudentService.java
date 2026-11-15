
package com.yearners.ua_parent_consent_backend.service;

import com.yearners.ua_parent_consent_backend.model.Section;
import com.yearners.ua_parent_consent_backend.model.Student;
import com.yearners.ua_parent_consent_backend.model.User;
import com.yearners.ua_parent_consent_backend.repository.SectionRepository;
import com.yearners.ua_parent_consent_backend.repository.StudentRepository;
import com.yearners.ua_parent_consent_backend.repository.TeacherSectionRepository;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final UserRepository userRepository;

    public StudentService(
            StudentRepository studentRepository,
            SectionRepository sectionRepository,
            TeacherSectionRepository teacherSectionRepository,
            UserRepository userRepository
    ) {
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
        this.teacherSectionRepository = teacherSectionRepository;
        this.userRepository = userRepository;
    }

    // Get students in a section assigned to the teacher.
    @Transactional(readOnly = true)
    public List<Student> getStudentsBySection(
            Long sectionId,
            String teacherEmail
    ) {
        User teacher = getTeacher(teacherEmail);
        validateSectionId(sectionId);
        verifyTeacherSection(teacher.getId(), sectionId);

        return studentRepository.findBySectionIdAndActiveTrue(sectionId);
    }

    // Add a student manually.
    @Transactional
    public Student addStudent(
            String studentNumber,
            String fullName,
            Long sectionId,
            String teacherEmail
    ) {
        User teacher = getTeacher(teacherEmail);

        validateStudentDetails(studentNumber, fullName);
        validateSectionId(sectionId);

        verifyTeacherSection(teacher.getId(), sectionId);

        String normalizedNumber = studentNumber.trim();

        if (studentRepository.existsByStudentNumberIgnoreCase(
                normalizedNumber
        )) {
            throw new IllegalArgumentException(
                    "A student with this student number already exists."
            );
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Section not found.")
                );

        if (!Boolean.TRUE.equals(section.getActive())) {
            throw new IllegalArgumentException(
                    "This section is currently inactive."
            );
        }

        Student student = new Student();
        student.setStudentNumber(normalizedNumber);
        student.setFullName(fullName.trim());
        student.setSection(section);
        student.setActive(true);

        return studentRepository.save(student);
    }

    // Update student information.
    @Transactional
    public Student updateStudent(
            Long studentId,
            String studentNumber,
            String fullName,
            Long sectionId,
            String teacherEmail
    ) {
        User teacher = getTeacher(teacherEmail);

        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid student ID."
            );
        }

        validateStudentDetails(studentNumber, fullName);
        validateSectionId(sectionId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found.")
                );

        verifyTeacherSection(teacher.getId(), student.getSection().getId());
        verifyTeacherSection(teacher.getId(), sectionId);

        String normalizedNumber = studentNumber.trim();

        studentRepository.findByStudentNumberIgnoreCase(normalizedNumber)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(studentId)) {
                        throw new IllegalArgumentException(
                                "A student with this student number already exists."
                        );
                    }
                });

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Section not found.")
                );

        if (!Boolean.TRUE.equals(section.getActive())) {
            throw new IllegalArgumentException(
                    "This section is currently inactive."
            );
        }

        student.setStudentNumber(normalizedNumber);
        student.setFullName(fullName.trim());
        student.setSection(section);

        return studentRepository.save(student);
    }

    // Deactivate a student without deleting historical records.
    @Transactional
    public Student deactivateStudent(
            Long studentId,
            String teacherEmail
    ) {
        User teacher = getTeacher(teacherEmail);

        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid student ID."
            );
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found.")
                );

        verifyTeacherSection(teacher.getId(), student.getSection().getId());

        student.setActive(false);

        return studentRepository.save(student);
    }

    private void validateStudentDetails(
            String studentNumber,
            String fullName
    ) {
        if (studentNumber == null || studentNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Student number is required."
            );
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Student full name is required."
            );
        }
    }

    private void validateSectionId(Long sectionId) {
        if (sectionId == null || sectionId <= 0) {
            throw new IllegalArgumentException(
                    "A valid section is required."
            );
        }
    }

    private void verifyTeacherSection(
            Long teacherId,
            Long sectionId
    ) {
        if (!teacherSectionRepository.existsByTeacherIdAndSectionId(
                teacherId,
                sectionId
        )) {
            throw new IllegalArgumentException(
                    "You are not assigned to this section."
            );
        }
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
                    "Only teachers can manage students."
            );
        }

        return teacher;
    }
}
