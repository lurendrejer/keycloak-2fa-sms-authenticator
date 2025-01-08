package dasniko.keycloak.authenticator.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Keycloak extension for sending SMS via GatewayAPI using Token Authentication.
 *
 * @author Niko Köbler
 */
public class AwsSmsService implements SmsService {

    private static final Logger LOG = LoggerFactory.getLogger(AwsSmsService.class);

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;
    private final String senderId;

    public AwsSmsService(Map<String, String> config) {
        this.apiKey = System.getenv("GATEWAYAPI_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("GATEWAYAPI_KEY environment variable is not set.");
        }
        this.senderId = config.get("senderId");
    }

    @Override
    public void send(String phoneNumber, String message) {
        try {
            // Log the raw input
            LOG.debug("Received phone number: {}", phoneNumber);
            LOG.debug("Received message: {}", message);

            // Sanitize the phone number
            String sanitizedPhoneNumber = sanitizePhoneNumber(phoneNumber);
            LOG.debug("Sanitized phone number: {}", sanitizedPhoneNumber);

            // Construct JSON payload
            String jsonPayload = String.format(
                "{\"recipients\": [{\"msisdn\": \"%s\"}], \"message\": \"%s\", \"sender\": \"%s\"}",
                sanitizedPhoneNumber, message, senderId
            );
            LOG.debug("Constructed JSON payload: {}", jsonPayload);

            // Debug before creating HTTP request
            LOG.debug("Preparing to send HTTP request to GatewayAPI...");
            LOG.debug("Request URI: {}", "https://gatewayapi.com/rest/mtsms");
            LOG.debug("Authorization header: Token {}", apiKey);
            LOG.debug("Request payload: {}", jsonPayload);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://gatewayapi.com/rest/mtsms"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Token " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            LOG.debug("HTTP request created successfully.");

            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.debug("Received response with status code: {}", response.statusCode());
            LOG.debug("Response body: {}", response.body());

            // Handle the response
            if (response.statusCode() != 200) {
                LOG.error("Failed to send SMS. HTTP status: {}, response body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to send SMS: " + response.body());
            }

            LOG.info("SMS sent successfully to {}", sanitizedPhoneNumber);

        } catch (Exception e) {
            LOG.error("Error occurred while sending SMS", e);
            throw new RuntimeException("Error occurred while sending SMS", e);
        }
    }

    /**
     * Sanitizes a phone number by removing spaces and ensuring it has the correct format.
     *
     * @param phoneNumber The raw phone number (e.g., "+45 10 10 10 10").
     * @return The sanitized phone number (e.g., "4510101010").
     */
    private String sanitizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank.");
        }
        // Remove spaces and the '+' prefix
        return phoneNumber.replaceAll("\\s+", "").replaceFirst("^\\+", "");
    }
}
