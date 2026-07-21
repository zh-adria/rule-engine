package com.insurance.ruleengine.app;

import com.insurance.ruleengine.client.dto.AuthLoginCmd;
import com.insurance.ruleengine.client.dto.AuthSessionDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthFacadeImplTest {

    @Test
    void loginReturnsTokenAndContextForConfiguredUser() {
        AuthUserProperties properties = new AuthUserProperties();
        AuthUserProperties.User user = new AuthUserProperties.User();
        user.setUsername("admin");
        user.setPassword("admin123");
        user.setDisplayName("Administrator");
        user.setRoles(List.of("ADMIN"));
        user.setPermissions(List.of("ADMIN", "RULE_READ"));
        user.setTenantCode("default");
        properties.setUsers(List.of(user));
        AuthFacadeImpl facade = new AuthFacadeImpl(properties);

        AuthSessionDTO session = facade.login(cmd("admin", "admin123"));

        assertNotNull(session.getToken());
        assertFalse(session.getToken().isBlank());
        assertEquals("admin", session.getUsername());
        assertEquals("Administrator", session.getDisplayName());
        assertEquals(List.of("ADMIN"), session.getRoles());
        assertEquals(List.of("ADMIN", "RULE_READ"), session.getPermissions());
        assertEquals("default", session.getTenantCode());
    }

    @Test
    void loginRejectsWrongPassword() {
        AuthUserProperties properties = new AuthUserProperties();
        AuthUserProperties.User user = new AuthUserProperties.User();
        user.setUsername("admin");
        user.setPassword("admin123");
        properties.setUsers(List.of(user));
        AuthFacadeImpl facade = new AuthFacadeImpl(properties);

        assertThrows(IllegalArgumentException.class, () -> facade.login(cmd("admin", "bad")));
    }

    private static AuthLoginCmd cmd(String username, String password) {
        AuthLoginCmd cmd = new AuthLoginCmd();
        cmd.setUsername(username);
        cmd.setPassword(password);
        return cmd;
    }
}
