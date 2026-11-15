
package com.yearners.ua_parent_consent_backend.service;

import com.yearners.ua_parent_consent_backend.model.Section;
import com.yearners.ua_parent_consent_backend.model.TeacherSection;
import com.yearners.ua_parent_consent_backend.model.User;
import com.yearners.ua_parent_consent_backend.repository.SectionRepository;
import com.yearners.ua_parent_consent_backend.repository.TeacherSectionRepository;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final UserRepository userRepository;

    public SectionService(
            SectionRepository sectionRepository,
            TeacherSectionRepository teacherSectionRepository,
            UserRepository userRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.teacherSectionRepository = teacherSectionRepository;
        this.userRepository = userRepository;
    }

    // Get all active sections available for assignment.
    @Transactional(readOnly = true)
    public List<Section> getAvailableSections() {
        return sectionRepository.findAll()
                .stream()
                .filter(section -> Boolean.TRUE.equals(section.getActive()))
                .toList();
    }

    // Get sections assigned to the logged-in teacher.
    @Transactional(readOnly = true)
    public List<Section> getMySections(String teacherEmail) {
        User teacher = getTeacher(teacherEmail);

        return teacherSectionRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(TeacherSection::getSection)
                .toList();
    }

    // Assign a section to the logged-in teacher.
    @Transactional
    public void assignSection(Long sectionId, String teacherEmail) {
        User teacher = getTeacher(teacherEmail);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Section not found.")
                );

        if (!Boolean.TRUE.equals(section.getActive())) {
            throw new IllegalArgumentException(
                    "This section is currently inactive."
            );
        }

        boolean alreadyAssigned =
                teacherSectionRepository.existsByTeacherIdAndSectionId(
                        teacher.getId(),
                        sectionId
                );

        if (alreadyAssigned) {
            throw new IllegalArgumentException(
                    "You are already assigned to this section."
            );
        }

        TeacherSection assignment = new TeacherSection(teacher, section);

        teacherSectionRepository.save(assignment);
    }

    // Remove a section assignment from the logged-in teacher.
    @Transactional
    public void removeSection(Long sectionId, String teacherEmail) {
        User teacher = getTeacher(teacherEmail);

        boolean assigned =
                teacherSectionRepository.existsByTeacherIdAndSectionId(
                        teacher.getId(),
                        sectionId
                );

        if (!assigned) {
            throw new IllegalArgumentException(
                    "This section is not assigned to your account."
            );
        }

        teacherSectionRepository.deleteByTeacherIdAndSectionId(
                teacher.getId(),
                sectionId
        );
    }

    // Verify that the account belongs to a teacher.
    private User getTeacher(String teacherEmail) {
        String email = teacherEmail.trim().toLowerCase(Locale.ROOT);

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Teacher account not found.")
                );

        if (!"teacher".equalsIgnoreCase(teacher.getRole())) {
            throw new IllegalArgumentException(
                    "Only teacher accounts can manage section assignments."
            );
        }

        return teacher;
    }
}
