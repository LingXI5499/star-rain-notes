package com.starrainnotes.auth.controller;

import com.starrainnotes.auth.dto.CreateAdminRequest;
import com.starrainnotes.auth.dto.SetupResultView;
import com.starrainnotes.auth.dto.SetupStatusView;
import com.starrainnotes.auth.service.SetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1 setup endpoints (04-api-design.md §7).
 *
 * <p>POST /api/v1/setup/admin additionally requires a valid CSRF token
 * (enforced by the filter chain) and the X-Setup-Token header matched in
 * constant time against APP_SETUP_TOKEN.</p>
 */
@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @GetMapping("/status")
    public SetupStatusView status() {
        return new SetupStatusView(setupService.isSetupRequired());
    }

    @PostMapping("/admin")
    public ResponseEntity<SetupResultView> createAdmin(
            @Valid @RequestBody CreateAdminRequest request,
            @RequestHeader(value = "X-Setup-Token", required = false) String setupToken) {
        setupService.createAdmin(request.username(), request.password(), setupToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SetupResultView(request.username()));
    }
}
