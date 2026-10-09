package com.starrainnotes.portfolio.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.portfolio.dto.WorkPrototypeDTO;
import com.starrainnotes.portfolio.service.PortfolioPrototypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PortfolioPrototypeController {
    private final PortfolioPrototypeService prototypes;
    @PutMapping("/api/admin/portfolio/works/{workId}/prototype")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<Void> bind(@PathVariable Long workId, @RequestBody WorkPrototypeDTO request) {
        prototypes.bind(workId, request); return ApiResponse.ok(null);
    }
    @GetMapping("/api/public/portfolio/works/{slug}/live/{*path}")
    public ResponseEntity<byte[]> read(@PathVariable String slug, @PathVariable String path) {
        String relative = path.startsWith("/") ? path.substring(1) : path;
        byte[] bytes = prototypes.read(slug, relative);
        return ResponseEntity.ok().header("Content-Type", prototypes.contentType(relative))
                .header("X-Content-Type-Options", "nosniff").header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Access-Control-Allow-Origin", "*")
                .header("Content-Security-Policy", "sandbox allow-scripts; default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'none'; frame-src 'none'; base-uri 'none'; form-action 'none'")
                .body(bytes);
    }
}
