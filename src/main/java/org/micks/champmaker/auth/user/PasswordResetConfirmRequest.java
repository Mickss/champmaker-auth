package org.micks.champmaker.auth.user;

import lombok.Getter;

@Getter
public class PasswordResetConfirmRequest {
    private String token;
    private String newPassword;
}
