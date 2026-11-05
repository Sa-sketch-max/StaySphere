package com.sahil.project.StaySphere.security;

import com.sahil.project.StaySphere.entity.User;
import com.sahil.project.StaySphere.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;

import java.io.IOException;


@Configuration
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final UserService userService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("JWT FILTER INVOKED");
        final String requestTokenHeader = request.getHeader("Authorization");


        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            System.out.println("JWT FILTER EXECUTED");

            String token = requestTokenHeader.substring(7);
            System.out.println("TOKEN RECEIVED");

            String tokenType = jwtService.getTokenType(token);
            System.out.println("TOKEN TYPE: " + tokenType);

            if (!"ACCESS".equals(tokenType)) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid token type"
                );
                return;
            }

            Long userId = jwtService.getUserIdFromToken(token);
            System.out.println("USER ID: " + userId);

            if (userId != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userService.getUserById(userId);
                System.out.println("USER FOUND: " + user.getEmail());

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authenticationToken);

                System.out.println("AUTHENTICATION SET SUCCESSFULLY");
            }

        } catch (ExpiredJwtException e) {
            System.out.println("JWT EXPIRED: " + e.getMessage());
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Access token expired"
            );
            return;

        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("JWT ERROR: " + e.getMessage());
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid access token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
