package uk.ac.ebi.eva.submission.controller.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private final BruteForceProtectionService bruteForceService;

    public BruteForceProtectionFilter(BruteForceProtectionService bruteForceService) {
        this.bruteForceService = bruteForceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (req.getRequestURI().contains("/v1/admin/")) {
            String ip = req.getRemoteAddr();
            if (bruteForceService.isBlocked(ip)) {
                res.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many failed login attempts. Try again later.");
                return;
            }
            chain.doFilter(req, res);
            if (res.getStatus() == HttpStatus.OK.value()) {
                bruteForceService.onAuthenticationSuccess(ip);
            }
        } else {
            chain.doFilter(req, res);
        }
    }
}
