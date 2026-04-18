package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.ArrayList;
import java.util.List;

public class HospitalAuthController {

    @FXML private TextField hospitalSearchField;
    @FXML private Label statusLabel;
    @FXML private WebView hospitalMapWebView;

    private FirebaseService firebaseService;
    private List<HospitalProfile> allHospitals = new ArrayList<>();
    private List<HospitalProfile> filteredHospitals = new ArrayList<>();
    private boolean mapLoaded = false;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        setupMap();
        hospitalSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        loadHospitals();
    }

    @FXML
    private void onSignIn() {
        SceneRouter.go("hospital-login-view.fxml", "Hospital Sign In");
    }

    @FXML
    private void onSignUp() {
        SceneRouter.go("hospital-signup-view.fxml", "Hospital Sign Up");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("login-view.fxml", "Login");
    }

    private void loadHospitals() {
        showStatus("Loading registered hospitals...", false);
        firebaseService.getAllHospitals()
                .thenAccept(hospitals -> Platform.runLater(() -> {
                    allHospitals = hospitals != null ? hospitals : new ArrayList<>();
                    applyFilter();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load hospital locations: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void applyFilter() {
        String query = hospitalSearchField.getText() == null ? "" : hospitalSearchField.getText().trim().toLowerCase();
        filteredHospitals = new ArrayList<>();

        for (HospitalProfile hospital : allHospitals) {
            String address = valueOrDefault(hospital.getFullAddress(), "").toLowerCase();
            String name = valueOrDefault(hospital.getHospitalName(), "").toLowerCase();
            if (query.isBlank() || address.contains(query) || name.contains(query)) {
                filteredHospitals.add(hospital);
            }
        }

        updateMap(filteredHospitals);
        if (query.isBlank()) {
            showStatus("Showing " + filteredHospitals.size() + " registered hospital location(s).", false);
        } else {
            showStatus("Found " + filteredHospitals.size() + " matching hospital location(s).", false);
        }
    }

    private void setupMap() {
        try {
            WebEngine engine = hospitalMapWebView.getEngine();
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
                    updateMap(filteredHospitals);
                }
            });
        } catch (Exception e) {
            showStatus("Failed to load map.", true);
        }
    }

    private void updateMap(List<HospitalProfile> hospitals) {
        if (!mapLoaded || hospitalMapWebView == null) {
            return;
        }

        Platform.runLater(() -> {
            try {
                WebEngine engine = hospitalMapWebView.getEngine();
                engine.executeScript("if (typeof clearMarkers === 'function') clearMarkers();");

                if (hospitals != null) {
                    for (HospitalProfile hospital : hospitals) {
                        String script = String.format(
                                "if (typeof addHospitalMarker === 'function') addHospitalMarker('%s','%s','%s',null,null);",
                                escapeForJs(valueOrDefault(hospital.getFullAddress(), "")),
                                escapeForJs(valueOrDefault(hospital.getHospitalName(), "Hospital")),
                                escapeForJs(buildHospitalInfoHtml(hospital))
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

    private String buildHospitalInfoHtml(HospitalProfile hospital) {
        return "<div>Address: " + escapeHtml(valueOrDefault(hospital.getFullAddress(), "Address not provided")) + "</div>"
                + "<div>Phone: " + escapeHtml(valueOrDefault(hospital.getPhoneNumber(), "Not provided")) + "</div>";
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
