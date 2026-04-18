package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.ArrayList;
import java.util.List;

public class HospitalSearchController {

    @FXML private TextField hospitalSearchField;
    @FXML private TextField zipSearchField;
    @FXML private Label statusLabel;
    @FXML private VBox hospitalListVBox;
    @FXML private WebView mapWebView;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private List<HospitalProfile> allHospitals = new ArrayList<>();
    private List<HospitalProfile> filteredHospitals = new ArrayList<>();
    private boolean mapLoaded = false;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        hospitalSearchField.setPromptText("Hospital name");
        zipSearchField.setPromptText("11735");

        if (userContext.getProfile() != null && userContext.getProfile().getZip() != null) {
            zipSearchField.setText(userContext.getProfile().getZip());
        }

        hospitalSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        zipSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        setupMap();
        loadHospitals();
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }

    @FXML
    private void onSearch() {
        applyFilters();
    }

    @FXML
    private void onClearFilters() {
        hospitalSearchField.clear();
        String patientZip = userContext.getProfile() != null ? userContext.getProfile().getZip() : null;
        zipSearchField.setText(patientZip == null ? "" : patientZip);
        applyFilters();
    }

    private void loadHospitals() {
        showStatus("Loading registered hospitals...", false);
        firebaseService.getAllHospitals()
                .thenAccept(hospitals -> Platform.runLater(() -> {
                    allHospitals = hospitals != null ? hospitals : new ArrayList<>();
                    applyFilters();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load hospital accounts: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void applyFilters() {
        String query = hospitalSearchField.getText() == null ? "" : hospitalSearchField.getText().trim().toLowerCase();
        String zip = zipSearchField.getText() == null ? "" : zipSearchField.getText().trim();
        boolean noFilters = query.isBlank() && zip.isBlank();

        filteredHospitals = new ArrayList<>();
        for (HospitalProfile hospital : allHospitals) {
            boolean matchesName = !query.isBlank()
                    && valueOrDefault(hospital.getHospitalName(), "").toLowerCase().contains(query);

            boolean matchesZip = !zip.isBlank()
                    && valueOrDefault(hospital.getZip(), "").startsWith(zip);

            if (noFilters || matchesName || matchesZip) {
                filteredHospitals.add(hospital);
            }
        }

        displayHospitals(filteredHospitals);
        if (filteredHospitals.isEmpty()) {
            showStatus("No registered hospitals matched your search.", true);
        } else {
            showStatus("Found " + filteredHospitals.size() + " registered hospital account(s).", false);
        }
    }

    private void displayHospitals(List<HospitalProfile> hospitals) {
        hospitalListVBox.getChildren().clear();

        if (hospitals.isEmpty()) {
            Label emptyLabel = new Label("No registered hospitals to show.");
            emptyLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13;");
            hospitalListVBox.getChildren().add(emptyLabel);
            updateMap(hospitals);
            return;
        }

        for (HospitalProfile hospital : hospitals) {
            VBox card = new VBox(8);
            card.setStyle(
                    "-fx-border-color: #E2E8F0; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-color: white; " +
                    "-fx-background-radius: 10; " +
                    "-fx-padding: 16;"
            );

            Label nameLabel = new Label(valueOrDefault(hospital.getHospitalName(), "Hospital"));
            nameLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

            Label locationLabel = new Label("Location: " + buildLocationText(hospital));
            locationLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #334155;");

            Label addressLabel = new Label("Address: " + valueOrDefault(hospital.getFullAddress(), "Address unavailable"));
            addressLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #334155;");
            addressLabel.setWrapText(true);

            Label phoneLabel = new Label("Contact: " + valueOrDefault(hospital.getPhoneNumber(), "Phone not listed"));
            phoneLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");

            Label portalLabel = new Label("Registered hospital account on HealthConnect");
            portalLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #0F766E; -fx-font-weight: bold;");

            Button detailsButton = new Button("View Details");
            detailsButton.setStyle(
                    "-fx-padding: 8 16; " +
                    "-fx-background-radius: 20; " +
                    "-fx-background-color: #0F766E; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold;"
            );
            detailsButton.setOnAction(event -> showHospitalDetails(hospital));

            HBox buttonRow = new HBox(detailsButton);
            buttonRow.setStyle("-fx-alignment: center-left;");

            card.getChildren().addAll(nameLabel, locationLabel, addressLabel, phoneLabel, portalLabel, buttonRow);
            hospitalListVBox.getChildren().add(card);
        }

        updateMap(hospitals);
    }

    private void showHospitalDetails(HospitalProfile hospital) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Hospital Details");
        alert.setHeaderText(valueOrDefault(hospital.getHospitalName(), "Hospital"));
        alert.setContentText(
                "Address: " + valueOrDefault(hospital.getFullAddress(), "Not listed") + "\n" +
                        "Phone: " + valueOrDefault(hospital.getPhoneNumber(), "Not listed") + "\n" +
                        "Email: " + valueOrDefault(hospital.getEmail(), "Not listed")
        );
        alert.showAndWait();
    }

    private void setupMap() {
        try {
            WebEngine engine = mapWebView.getEngine();
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
        if (!mapLoaded || mapWebView == null) {
            return;
        }

        Platform.runLater(() -> {
            try {
                WebEngine engine = mapWebView.getEngine();
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

    private String buildLocationText(HospitalProfile hospital) {
        String city = valueOrDefault(hospital.getCity(), "Unknown");
        String state = valueOrDefault(hospital.getState(), "Unknown");
        String zip = valueOrDefault(hospital.getZip(), "");
        return city + ", " + state + (zip.isBlank() ? "" : " " + zip);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill:#DC2626;" : "-fx-text-fill:#0F766E;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
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
