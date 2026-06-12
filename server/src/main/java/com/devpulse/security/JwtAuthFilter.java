package com.devpulse.security;

import com.devpulse.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAndExtract(token);
            UUID userId = UUID.fromString(claims.getSubject());
            UUID teamId = UUID.fromString(claims.get("teamId", String.class));
            String username = claims.get("username", String.class);

            DevPulseUserDetails userDetails =
                    new DevPulseUserDetails(userId, teamId, username);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // Set teamId as request attribute for controllers
            request.setAttribute("teamId", teamId.toString());
            request.setAttribute("userId", userId.toString());
            request.setAttribute("username", username);

        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}