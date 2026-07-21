package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.AuthFacade;
import com.insurance.ruleengine.client.dto.AuthLoginCmd;
import com.insurance.ruleengine.client.dto.AuthSessionDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthFacade authFacade;

    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @Operation(summary = "Sa-Token 登录")
    @PostMapping("/login")
    public AuthSessionDTO login(@Valid @RequestBody AuthLoginCmd cmd) {
        return authFacade.login(cmd);
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public AuthSessionDTO me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return authFacade.current(extractBearerToken(authorization));
    }

    @Operation(summary = "Sa-Token 登出")
    @PostMapping("/logout")
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authFacade.logout(extractBearerToken(authorization));
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring(7);
    }
}
