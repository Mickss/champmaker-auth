package org.micks.champmaker.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.micks.champmaker.auth.exceptions.UserLoginFailedException;
import org.micks.champmaker.auth.jwt.ValidateTokenRequest;
import org.micks.champmaker.auth.user.CreateUserRequest;
import org.micks.champmaker.auth.user.LoginRequest;
import org.micks.champmaker.auth.user.PasswordResetConfirmRequest;
import org.micks.champmaker.auth.user.PasswordResetRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/public/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void createUser(@RequestBody CreateUserRequest createUserRequest) {
        authService.createUser(createUserRequest);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void loginUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) throws UserLoginFailedException {
        log.info("Received login request for user: {}", loginRequest.getEmail());

        String token = authService.loginUser(loginRequest);

        Cookie jwtCookie = new Cookie("token", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(60 * 60 * 24);
        response.addCookie(jwtCookie);

        log.info("User authenticated: {}", loginRequest.getEmail());
    }

    @PostMapping(value = "/logout")
    public void logoutUser(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        log.info("User logged out.");
    }

    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void validateToken(@RequestBody ValidateTokenRequest validateTokenRequest) {
        authService.validateToken(validateTokenRequest);
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmPasswordReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
