package com.buinam.schedulemanger.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.buinam.schedulemanger.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class CustomAuthorizationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(request.getServletPath().equals("/api/v1/security/login") || request.getServletPath().equals("/api/v1/security/refresh")  || request.getServletPath().equals("/api/v1/security/user/save") || request.getServletPath().equals("/api/v1/security/test")){
            filterChain.doFilter(request, response);
        } else {
            String authorizationHeader = request.getHeader(AUTHORIZATION);
            String queryString = request.getParameter("tokenWS");

            if((authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) || (queryString != null && request.getServletPath().contains("/ws/"))){
                try {
                    String token = null;
                    if(queryString!= null) {
                        token = queryString;
                    } else {
                        token = authorizationHeader.substring("Bearer ".length());
                    }

                    // if the token is valid, it will pass the decodeJWT verification below and return a decodedJWT object
                    DecodedJWT decodedJWT = JwtUtils.verifyToken(token);

                    String username = JwtUtils.getUserName(decodedJWT);
                    log.info("username: {}", username);
                    Collection<SimpleGrantedAuthority> authorities = JwtUtils.getAuthorities(decodedJWT);
                    log.info("authorities: {}", authorities);
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    filterChain.doFilter(request, response);
                }catch (Exception exception) {
                    log.error("Error logging in: {}", exception.getMessage());
                    response.setHeader("error", exception.getMessage());
                    response.setStatus(FORBIDDEN.value());
                    //response.sendError(FORBIDDEN.value());
                    Map<String, String> error = new HashMap<>();
                    error.put("error_message", exception.getMessage());
                    response.setContentType(APPLICATION_JSON_VALUE);
                    new ObjectMapper().writeValue(response.getOutputStream(), error);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }
}
