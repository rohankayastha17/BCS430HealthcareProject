package rakib.bcs430healthcareproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Small client for the public RxNorm API used to search medications.
 */
public class RxNormMedicationService {

    private static final String RXNORM_DRUGS_URL = "https://rxnav.nlm.nih.gov/REST/drugs.json?name=%s&expand=psn";
    private static final int MAX_RESULTS = 25;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RxNormMedicationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<List<MedicationOption>> searchMedications(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String trimmedQuery = query == null ? "" : query.trim();
                if (trimmedQuery.length() < 2) {
                    return List.of();
                }

                String encodedQuery = URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format(RXNORM_DRUGS_URL, encodedQuery)))
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("RxNorm search failed with status " + response.statusCode() + ".");
                }

                return parseMedicationOptions(response.body());
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("Unable to reach the RxNorm medication service right now.", e);
            }
        });
    }

    private List<MedicationOption> parseMedicationOptions(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode conceptGroups = root.path("drugGroup").path("conceptGroup");

        if (!conceptGroups.isArray()) {
            return List.of();
        }

        Map<String, MedicationOption> uniqueOptions = new LinkedHashMap<>();

        for (JsonNode conceptGroup : conceptGroups) {
            JsonNode conceptProperties = conceptGroup.path("conceptProperties");
            if (!conceptProperties.isArray()) {
                continue;
            }

            for (JsonNode conceptProperty : conceptProperties) {
                String rxcui = textValue(conceptProperty, "rxcui");
                String displayName = firstNonBlank(
                        textValue(conceptProperty, "psn"),
                        textValue(conceptProperty, "synonym"),
                        textValue(conceptProperty, "name")
                );

                if (displayName == null) {
                    continue;
                }

                String key = (rxcui == null ? "" : rxcui) + "::" + displayName.toLowerCase();
                uniqueOptions.putIfAbsent(key, new MedicationOption(rxcui, displayName));

                if (uniqueOptions.size() >= MAX_RESULTS) {
                    return new ArrayList<>(uniqueOptions.values());
                }
            }
        }

        return new ArrayList<>(uniqueOptions.values());
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return null;
        }
        String value = field.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public static class MedicationOption {
        private final String rxcui;
        private final String displayName;

        public MedicationOption(String rxcui, String displayName) {
            this.rxcui = rxcui;
            this.displayName = displayName;
        }

        public String getRxcui() {
            return rxcui;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
