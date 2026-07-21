package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.AuthFacade;
import com.insurance.ruleengine.client.dto.AuthLoginCmd;
import com.insurance.ruleengine.client.dto.AuthSessionDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerTest {

    @Test
    void delegatesLoginMeAndLogoutToFacade() {
        RecordingAuthFacade facade = new RecordingAuthFacade();
        AuthController controller = new AuthController(facade);
        AuthLoginCmd cmd = new AuthLoginCmd();
        cmd.setUsername("admin");
        cmd.setPassword("admin123");

        AuthSessionDTO login = controller.login(cmd);
        AuthSessionDTO me = controller.me("Bearer token-1");
        controller.logout("Bearer token-1");

        assertEquals("token-admin", login.getToken());
        assertEquals("admin", login.getUsername());
        assertEquals("me:token-1", facade.calls.get(1));
        assertEquals("logout:token-1", facade.calls.get(2));
        assertEquals(List.of("ADMIN"), me.getRoles());
    }

    private static class RecordingAuthFacade implements AuthFacade {
        private final List<String> calls = new java.util.ArrayList<>();

        @Override
        public AuthSessionDTO login(AuthLoginCmd cmd) {
            calls.add("login:" + cmd.getUsername());
            AuthSessionDTO dto = new AuthSessionDTO();
            dto.setToken("token-" + cmd.getUsername());
            dto.setUsername(cmd.getUsername());
            return dto;
        }

        @Override
        public AuthSessionDTO current(String token) {
            calls.add("me:" + token);
            AuthSessionDTO dto = new AuthSessionDTO();
            dto.setUsername("admin");
            dto.setRoles(List.of("ADMIN"));
            return dto;
        }

        @Override
        public void logout(String token) {
            calls.add("logout:" + token);
        }
    }
}
