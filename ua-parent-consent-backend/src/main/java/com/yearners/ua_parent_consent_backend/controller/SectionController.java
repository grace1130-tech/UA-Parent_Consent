
package com.yearners.ua_parent_consent_backend.controller;

import com.yearners.ua_parent_consent_backend.dto.SectionResponse;
import com.yearners.ua_parent_consent_backend.model.Section;
import com.yearners.ua_parent_consent_backend.service.SectionService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    // Get all active sections available for assignment.
    @GetMapping("/available")
    public List<SectionResponse> getAvailableSections() {
        return sectionService.getAvailableSections()
                .stream()
                .map(SectionResponse::from)
                .toList();
    }

    // Get sections assigned to the logged-in teacher.
    @GetMapping("/my-sections")
    public List<SectionResponse> getMySections(
            Authentication authentication
    ) {
        try {
            return sectionService.getMySections(
                            authentication.getName()
                    )
                    .stream()
                    .map(SectionResponse::from)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Assign a section to the logged-in teacher.
    @PostMapping("/{sectionId}")
    public Map<String, String> assignSection(
            @PathVariable Long sectionId,
            Authentication authentication
    ) {
        try {
            sectionService.assignSection(
                    sectionId,
                    authentication.getName()
            );

            return Map.of(
                    "message",
                    "Section assigned successfully."
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Remove a section from the logged-in teacher's assignments.
    @DeleteMapping("/{sectionId}")
    public Map<String, String> removeSection(
            @PathVariable Long sectionId,
            Authentication authentication
    ) {
        try {
            sectionService.removeSection(
                    sectionId,
                    authentication.getName()
            );

            return Map.of(
                    "message",
                    "Section removed successfully."
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }
}
