package com.seguros.polizas.config;

import com.seguros.polizas.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/** Exige el header x-api-key en todas las peticiones, excepto la consola H2. */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "x-api-key";

    private final String apiKeyEsperada;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${app.api-key}") String apiKeyEsperada, ObjectMapper objectMapper) {
        this.apiKeyEsperada = apiKeyEsperada;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!apiKeyEsperada.equals(request.getHeader(HEADER))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ErrorResponse.of("API_KEY_INVALIDA", "Falta el header " + HEADER + " o no es válido"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
