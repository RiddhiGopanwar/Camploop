package com.camploop.controller;

import com.camploop.config.AuthContext;
import com.camploop.config.CurrentUser;
import com.camploop.dto.ApiMessageResponse;
import com.camploop.dto.ReportRequest;
import com.camploop.service.ProfileService;
import com.camploop.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1 reporting: a logged-in student can flag a listing as inappropriate/suspicious.
 * The report is simply stored for now — no moderation workflow yet (that's a V2
 * admin-dashboard feature).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ProfileService profileService;
    private final AuthContext authContext;

    @Autowired
    public ReportController(ReportService reportService, ProfileService profileService, AuthContext authContext) {
        this.reportService = reportService;
        this.profileService = profileService;
        this.authContext = authContext;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiMessageResponse> report(@PathVariable Long productId, @Valid @RequestBody ReportRequest request,
                                                       HttpServletRequest httpRequest) {
        CurrentUser currentUser = authContext.require(httpRequest);
        reportService.create(productId, request.getReason(), profileService.getOrCreate(currentUser, null));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessageResponse("Thanks — this listing has been reported for review."));
    }
}
