package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.ArrayList;
import java.util.List;

public class PharmacyAuthController {

    @FXML private TextField pharmacySearchField;
    @FXML private Label statusLabel;
    @FXML private WebView pharmacyMapWebView;

    private FirebaseService firebaseService;
    private List<PharmacyProfile> allPharmacies = new ArrayList<>();
    private List<PharmacyProfile> filteredPharmacies = new ArrayList<>();
    private boolean mapLoaded = false;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        setupMap();
        pharmacySearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        loadPharmacies();
    }

    @FXML
    private void onSignIn() {
        SceneRouter.go("pharmacy-login-view.fxml", "Pharmacy Sign In");
    }

    @FXML
    private void onSignUp() {
        SceneRouter.go("pharmacy-signup-view.fxml", "Pharmacy Sign Up");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("login-view.fxml", "Login");
    }

    private void loadPharmacies() {
        showStatus("Loading pharmacy locations...", false);
        firebaseService.getAllPharmacies()
                .thenAccept(pharmacies -> Platform.runLater(() -> {
                    allPharmacies = pharmacies != null ? pharmacies : new ArrayList<>();
                    applyFilter();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load pharmacy locations: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void applyFilter() {
        String query = pharmacySearchField.getText() == null ? "" : pharmacySearchField.getText().trim().toLowerCase();
        filteredPharmacies = new ArrayList<>();

        for (PharmacyProfile pharmacy : allPharmacies) {
            String address = valueOrDefault(pharmacy.getFullAddress(), "").toLowerCase();
            if (query.isBlank() || address.contains(query)) {
                filteredPharmacies.add(pharmacy);
            }
        }

        updateMap(filteredPharmacies);
        if (query.isBlank()) {
            showStatus("Showing " + filteredPharmacies.size() + " registered pharmacy location(s).", false);
        } else {
            showStatus("Found " + filteredPharmacies.size() + " matching pharmacy location(s).", false);
        }
    }

    private void setupMap() {
        try {
            WebEngine engine = pharmacyMapWebView.getEngine();
            engine.setUserAgent(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36"
            );
            String mapUrl = getClass().getResource("/rakib/bcs430healthcareproject/map.html").toExternalForm();
            engine.load(mapUrl);
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    mapLoaded = true;
                    updateMap(filteredPharmacies);
                }
            });
        } catch (Exception e) {
            showStatus("Failed to load map.", true);
        }
    }

    private void updateMap(List<PharmacyProfile> pharmacies) {
        if (!mapLoaded || pharmacyMapWebView == null) {
            return;
        }

        Platform.runLater(() -> {
            try {
                WebEngine engine = pharmacyMapWebView.getEngine();
                engine.executeScript("if (typeof clearMarkers === 'function') clearMarkers();");

                if (pharmacies != null) {
                    for (PharmacyProfile pharmacy : pharmacies) {
                        String script = String.format(
                                "if (typeof addPharmacyMarker === 'function') addPharmacyMarker('%s','%s','%s',null,null);",
                                escapeForJs(valueOrDefault(pharmacy.getFullAddress(), "")),
                                escapeForJs(valueOrDefault(pharmacy.getPharmacyName(), "Pharmacy")),
                                escapeForJs(buildPharmacyInfoHtml(pharmacy))
                        );
                        engine.executeScript(script);
                    }
                }

                engine.executeScript("if (typeof fitMapToMarkers === 'function') fitMapToMarkers();");
            } catch (Exception e) {
                showStatus("Failed to update map.", true);
            }
        });
    }

    private String buildPharmacyInfoHtml(PharmacyProfile pharmacy) {
        return "<div>Address: " + escapeHtml(valueOrDefault(pharmacy.getFullAddress(), "Address not provided")) + "</div>"
                + "<div>Phone: " + escapeHtml(valueOrDefault(pharmacy.getPhoneNumber(), "Not provided")) + "</div>";
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill:#DC2626;" : "-fx-text-fill:#0F766E;");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String cleanErrorMessage(Throwable throwable) {
        Throwable cause = throwable != null && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause != null && cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }

    private String escapeForJs(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
