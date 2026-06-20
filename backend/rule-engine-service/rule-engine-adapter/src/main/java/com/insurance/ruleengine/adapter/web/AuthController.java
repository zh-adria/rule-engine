package com.insurance.ruleengine.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username, request.password));
            UserDetails user = (UserDetails) auth.getPrincipal();
            String token = jwtTokenProvider.generateToken(user.getUsername());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("token", token);
            body.put("username", user.getUsername());
            body.put("expiresIn", jwtTokenProvider.getExpirationSeconds());
            return ResponseEntity.ok(body);
        } catch (AuthenticationException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", "AUTH_FAILED");
            body.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Use Spring-injected bean to check user existence
        // Note: In production, delegate to a proper UserService
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "REGISTER_DISABLED");
        body.put("message", "注册功能暂未开放，请联系管理员创建账号");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        public String username;
        @NotBlank(message = "密码不能为空")
        public String password;
    }

    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        public String username;
        @NotBlank(message = "密码不能为空")
        public String password;
        public String displayName;
    }
}
