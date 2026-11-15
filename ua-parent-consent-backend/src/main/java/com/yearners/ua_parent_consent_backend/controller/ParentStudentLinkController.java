
package com.yearners.ua_parent_consent_backend.controller;

import com.yearners.ua_parent_consent_backend.dto.ParentStudentLinkResponse;
import com.yearners.ua_parent_consent_backend.model.ParentStudentLink;
import com.yearners.ua_parent_consent_backend.service.ParentStudentLinkService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/parent-student-links")
public class ParentStudentLinkController {

    private final ParentStudentLinkService linkService;

    public ParentStudentLinkController(
            ParentStudentLinkService linkService
    ) {
        this.linkService = linkService;
    }

    public record LinkRequest(
            @NotBlank(message = "Student number is required.")
            String studentNumber,

            String relationship
    ) {}

    public record ReviewRequest(
            @NotBlank(message = "Decision is required.")
            String decision
    ) {}

    // Parent requests to link a child.
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public ParentStudentLinkResponse requestLink(
            @Valid @RequestBody LinkRequest request,
            Authentication authentication
    ) {
        try {
            ParentStudentLink link = linkService.requestLink(
                    request.studentNumber(),
                    request.relationship(),
                    authentication.getName()
            );

            return ParentStudentLinkResponse.from(link);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Parent views their children and verification statuses.
    @GetMapping("/my-links")
    public List<ParentStudentLinkResponse> getMyLinks(
            Authentication authentication
    ) {
        try {
            return linkService.getMyLinks(authentication.getName())
                    .stream()
                    .map(ParentStudentLinkResponse::from)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Teacher views pending requests in assigned sections.
    @GetMapping("/pending")
    public List<ParentStudentLinkResponse> getPendingRequests(
            Authentication authentication
    ) {
        try {
            return linkService.getPendingRequests(authentication.getName())
                    .stream()
                    .map(ParentStudentLinkResponse::from)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Teacher views approved and rejected requests.
    @GetMapping("/reviewed")
    public List<ParentStudentLinkResponse> getReviewedRequests(
            Authentication authentication
    ) {
        try {
            return linkService.getReviewedRequests(authentication.getName())
                    .stream()
                    .map(ParentStudentLinkResponse::from)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Teacher approves or rejects a request.
    @PatchMapping("/{linkId}/review")
    public ParentStudentLinkResponse reviewRequest(
            @PathVariable Long linkId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        try {
            ParentStudentLink link = linkService.reviewRequest(
                    linkId,
                    request.decision(),
                    authentication.getName()
            );

            return ParentStudentLinkResponse.from(link);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }
}
