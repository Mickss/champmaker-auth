package org.micks.champmaker.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class EmailNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${email.service.url}")
    private String emailServiceUrl;

    public void sendWelcomeEmail(String userEmail) {
        Map<String, String> payload = Map.of(
                "toEmail",   userEmail,
                "toName",    userEmail,
                "eventType", "WELCOME"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(emailServiceUrl + "/api/email/send", request, Void.class);
            log.info("Welcome email sent to: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", userEmail, e);
        }
    }
}
