package com.insurance.ruleengine.client.api;

import com.insurance.ruleengine.client.dto.AuthLoginCmd;
import com.insurance.ruleengine.client.dto.AuthSessionDTO;

public interface AuthFacade {
    AuthSessionDTO login(AuthLoginCmd cmd);
    AuthSessionDTO current(String token);
    void logout(String token);
}
