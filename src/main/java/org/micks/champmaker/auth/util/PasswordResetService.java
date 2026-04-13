package org.micks.champmaker.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.micks.champmaker.auth.connection.DiscGolfDbConnection;
import org.micks.champmaker.auth.connection.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.UUID;

@Service
@Slf4j
public class PasswordResetService {

    @Autowired
    private DiscGolfDbConnection discGolfDbConnection;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public void requestPasswordReset(String email) {
        try (Connection connection = discGolfDbConnection.connect()) {

            PreparedStatement checkUser = connection.prepareStatement(
                    "SELECT user_id FROM users WHERE email = ?");
            checkUser.setString(1, email);
            ResultSet rs = checkUser.executeQuery();

            if (!rs.next()) {
                log.info("Password reset requested for non-existent email: {}", email);
                return;
            }

            String userId = rs.getString("user_id");

            PreparedStatement invalidate = connection.prepareStatement(
                    "UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ? AND used = FALSE");
            invalidate.setString(1, userId);
            invalidate.executeUpdate();

            String token = UUID.randomUUID().toString();
            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 3_600_000);

            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO password_reset_tokens (id, user_id, token, expires_at) VALUES (UUID(), ?, ?, ?)");
            insert.setString(1, userId);
            insert.setString(2, token);
            insert.setTimestamp(3, expiresAt);
            insert.execute();

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailNotificationService.sendPasswordResetEmail(email, resetLink);

        } catch (SQLException e) {
            throw new RuntimeException("Database error during password reset request", e);
        }
    }

    public void confirmPasswordReset(String token, String newPassword) {
        try (Connection connection = discGolfDbConnection.connect()) {

            PreparedStatement check = connection.prepareStatement(
                    "SELECT user_id, expires_at, used FROM password_reset_tokens WHERE token = ?");
            check.setString(1, token);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                throw new RuntimeException("Invalid token");
            }
            if (rs.getBoolean("used")) {
                throw new RuntimeException("Token already used");
            }
            if (rs.getTimestamp("expires_at").before(new Timestamp(System.currentTimeMillis()))) {
                throw new RuntimeException("Token expired");
            }

            String userId = rs.getString("user_id");

            String hashedPassword = passwordService.hashPassword(newPassword);
            PreparedStatement updatePassword = connection.prepareStatement(
                    "UPDATE users SET password_hash = ? WHERE user_id = ?");
            updatePassword.setString(1, hashedPassword);
            updatePassword.setString(2, userId);
            updatePassword.executeUpdate();

            PreparedStatement useToken = connection.prepareStatement(
                    "UPDATE password_reset_tokens SET used = TRUE WHERE token = ?");
            useToken.setString(1, token);
            useToken.executeUpdate();

            log.info("Password reset successful for user: {}", userId);

        } catch (SQLException e) {
            throw new RuntimeException("Database error during password reset confirm", e);
        }
    }
}
