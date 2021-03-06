package com.buinam.schedulemanger.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.buinam.schedulemanger.dto.SimpleUserDTO;
import com.buinam.schedulemanger.model.AppUser;
import com.buinam.schedulemanger.model.Role;
import com.buinam.schedulemanger.service.AppUserService;
import com.buinam.schedulemanger.utils.CommonResponse;
import com.buinam.schedulemanger.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/security")
@Slf4j
public class AppUserController {
    private final AppUserService appUserService;

    //test
    @GetMapping("/test")
    public String test(){
        return "test";
    }

    @GetMapping("/user/search")
    public ResponseEntity<CommonResponse> searchUsers(@RequestParam(required = false) String textSearch){
        try {
            List<SimpleUserDTO> appUsers = appUserService.searchUsers(textSearch);
            return new ResponseEntity<>(
                    new CommonResponse(
                            "Search users successfully",
                            true,
                            appUsers,
                            HttpStatus.OK.value()
                    ),
                    HttpStatus.OK)
                    ;
        } catch(Exception e) {
            return new ResponseEntity<>(
                    new CommonResponse(
                            e.getMessage(),
                            true,
                            null,
                            HttpStatus.BAD_REQUEST.value()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    //get all users
    @GetMapping("/users")
    public ResponseEntity<CommonResponse> getAllUsers(@RequestParam(required = false) String textSearch){
        try {
            List<AppUser> appUsers = appUserService.getUsers(textSearch);
            return new ResponseEntity<>(
                    new CommonResponse(
                            "Search schedule successfully",
                            true,
                            appUsers,
                            HttpStatus.OK.value()
                    ),
                    HttpStatus.OK)
                    ;
        } catch(Exception e) {
            return new ResponseEntity<>(
                    new CommonResponse(
                            e.getMessage(),
                            true,
                            null,
                            HttpStatus.BAD_REQUEST.value()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    //save user
    @PostMapping("/user/save")
    public ResponseEntity<AppUser> saveUser(@RequestBody AppUser appUser){
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/user/save").toString());
//        return ResponseEntity.ok().body(appUserService.saveUser(appUser));
        return ResponseEntity.created(uri).body(appUserService.saveUser(appUser));

    }

    // save role
    @PostMapping("/role/save")
    public ResponseEntity<Role> saveRole(@RequestBody Role role){
        return ResponseEntity.ok().body(appUserService.saveRole(role));
    }

    //add role to user
    @PostMapping("/role/addtouser")
    public ResponseEntity<?> saveRoleToUser(@RequestBody RoleToUserForm roleToUserForm){
        appUserService.addRoleToUser(roleToUserForm.getUsername(), roleToUserForm.getRoleName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/token/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                log.info("Before decoded JWT");

                DecodedJWT decodedJWT = JwtUtils.verifyToken(refresh_token);
                String username = JwtUtils.getUserName(decodedJWT);
                AppUser user = appUserService.getUser(username);

                String access_token = JwtUtils.generateAccessToken(user, request);

                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", access_token);
                tokens.put("refresh_token", refresh_token);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            }catch (Exception exception) {
                response.setHeader("error", exception.getMessage());
                response.setStatus(FORBIDDEN.value());
                //response.sendError(FORBIDDEN.value());
                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());
                response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new RuntimeException("Refresh token is missing");
        }
    }
}

@Data
class RoleToUserForm {
    private String username;
    private String roleName;
}

