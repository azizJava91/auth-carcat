package com.carland.carland_auth.jwt;

import com.carland.carland_auth.enums.EnumMessagesLangValues;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CustomFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/users/updatePassword",
            "/invite-ui/get",
            "/api/v1/users/getNameSurname"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String lang = request.getHeader("Accept-Language");
        if (lang == null) lang = "az";

        String path = request.getRequestURI();
        if (PUBLIC_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);
        if (token == null) {
            writeErrorResponse(response, EnumMessagesLangValues.TOKEN_MISSING.getMessageByLang(lang));
            return;
        }

        try {
            if (path.equals("/api/v1/otp/createAndSend")
                    || path.equals("/api/v1/otp/verify")
                    || path.equals("/api/v1/users/setPassword")) {
                if (!jwtService.isRegisterTokenValid(token)) {
                    writeErrorResponse(response, EnumMessagesLangValues.REGISTER_TOKEN_EXPIRED.getMessageByLang(lang));
                    return;
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken("register-flow", null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);

                filterChain.doFilter(request, response);

                return;
            }
            if (path.equals("/api/v1/users/refresh")) {

                if (!jwtService.isRefreshTokenValid(token)) {
                    writeErrorResponse(response, EnumMessagesLangValues.REGISTER_TOKEN_EXPIRED.getMessageByLang(lang));
                    return;
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken("refresh-flow", null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);

                filterChain.doFilter(request, response);

                return;
            }
            if (!jwtService.isAccessTokenValid(token)) {

                writeErrorResponse(response, EnumMessagesLangValues.TOKEN_INVALID.getMessageByLang(lang));
                return;
            }

            Long userId = jwtService.extractUserId(token);
            String username = jwtService.extractUsername(token);

            CarlandPrincipal principal = new CarlandPrincipal(userId, username);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            writeErrorResponse(response, EnumMessagesLangValues.TOKEN_EXPIRED.getMessageByLang(lang));
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            writeErrorResponse(response, "Token yoxlanışı zamanı xəta baş verdi");
        }
    }


    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                    {"error":"JWT error","message":"%s","timeStamp":"%s","status":401}
                """.formatted(message, java.time.LocalDateTime.now()));
    }
}


