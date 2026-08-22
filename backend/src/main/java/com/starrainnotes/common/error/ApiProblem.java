package com.starrainnotes.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

/**
 * RFC 9457 Problem Details factory (04-api-design.md §1).
 *
 * Every error response carries {@code type}, {@code title}, {@code status},
 * {@code detail}, {@code instance}, and the frozen extensions {@code code}
 * and {@code traceId}. Never leaks stack traces or SQL messages.
 */
public final class ApiProblem {

    private static final String PROBLEM_BASE = "https://starrainnotes.local/problems/";

    private ApiProblem() {
    }

    public static ProblemDetail create(HttpStatus status, String code, String title, String detail,
                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + code.toLowerCase()));
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("traceId", UUID.randomUUID().toString());
        if (request != null) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        return problem;
    }
}
