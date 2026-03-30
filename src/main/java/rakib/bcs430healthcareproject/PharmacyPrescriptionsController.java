package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for a simple pharmacy portal that can fill prescriptions.
 */
public class PharmacyPrescriptionsController {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML private VBox prescriptionsListVBox;
    @FXML private Label statusLabel;
    @FXML private Label pharmacyNameLabel;
    @FXML private Label pharmacyAddressLabel;
    @FXML private TextField patientSearchField;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PharmacyProfile pharmacyProfile;
    private List<Prescription> allPrescriptions = new ArrayList<>();

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isPharmacy()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        pharmacyProfile = userContext.getPharmacyProfile();
        if (pharmacyProfile == null) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        pharmacyNameLabel.setText(valueOrDefault(pharmacyProfile.getPharmacyName(), "Pharmacy Portal"));
        pharmacyAddressLabel.setText(valueOrDefault(pharmacyProfile.getFullAddress(), "Claimed location unavailable"));
        patientSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        loadPrescriptions();
    }

    private void loadPrescriptions() {
        showStatus("Loading prescriptions...", false);
        firebaseService.getPrescriptionsForPharmacy(pharmacyProfile.getAddressNormalized())
                .thenAccept(prescriptions -> Platform.runLater(() -> renderPrescriptions(prescriptions)))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load prescriptions: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void renderPrescriptions(List<Prescription> prescriptions) {
        allPrescriptions = prescriptions == null ? new ArrayList<>() : new ArrayList<>(prescriptions);
        applyFilter();
    }

    private void applyFilter() {
        prescriptionsListVBox.getChildren().clear();
        String query = patientSearchField.getText() == null ? "" : patientSearchField.getText().trim().toLowerCase();
        List<Prescription> filteredPrescriptions = new ArrayList<>();
        for (Prescription prescription : allPrescriptions) {
            String patientName = valueOrDefault(prescription.getPatientName(), "").toLowerCase();
            if (query.isBlank() || patientName.contains(query)) {
                filteredPrescriptions.add(prescription);
            }
        }

        if (filteredPrescriptions.isEmpty()) {
            showStatus("No prescriptions available.", false);
            return;
        }

        showStatus(query.isBlank()
                ? "Loaded " + filteredPrescriptions.size() + " prescription(s)."
                : "Found " + filteredPrescriptions.size() + " matching prescription(s).", false);
        for (Prescription prescription : filteredPrescriptions) {
            prescriptionsListVBox.getChildren().add(createPrescriptionCard(prescription));
        }
    }

    private VBox createPrescriptionCard(Prescription prescription) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #D1FAE5; -fx-border-radius: 12;");

        HBox topRow = new HBox(10);
        Label patientLabel = new Label(valueOrDefault(prescription.getPatientName(), "Unknown Patient"));
        patientLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 16; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusBadge = new Label(formatStatus(prescription.getStatus()));
        statusBadge.setStyle(getStatusStyle(prescription.getStatus()));
        topRow.getChildren().addAll(patientLabel, spacer, statusBadge);

        Label medicationLabel = createDetailLabel("Medication Name: " + valueOrDefault(getMedicationName(prescription), "Not provided"));
        Label dosageLabel = createDetailLabel("Dosage: " + valueOrDefault(getDosage(prescription), "Not provided"));
        Label quantityLabel = createDetailLabel("Quantity: " + valueOrDefault(getQuantity(prescription), "Not provided"));
        Label refillLabel = createDetailLabel("Refill Details: " + getRefillDetails(prescription));

        Label pharmacyLabel = new Label("Pharmacy: " + formatPharmacy(prescription));
        pharmacyLabel.setWrapText(true);
        pharmacyLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label instructionsLabel = createDetailLabel("Instructions: " + valueOrDefault(prescription.getInstructions(), "Not provided"));

        Label sentLabel = new Label("Sent by Dr. " + valueOrDefault(prescription.getDoctorName(), "Unknown")
                + " on " + formatTimestamp(prescription.getCreatedAt()));
        sentLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        card.getChildren().addAll(topRow, medicationLabel, dosageLabel, quantityLabel, refillLabel, pharmacyLabel, instructionsLabel, sentLabel);

        if (Boolean.TRUE.equals(prescription.getRefillRequested())) {
            Label refillRequestLabel = new Label("Doctor refill request sent on " + formatTimestamp(prescription.getRefillRequestedAt()));
            refillRequestLabel.setStyle("-fx-text-fill: #1D4ED8; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(refillRequestLabel);
        }

        HBox actionRow = new HBox(10);

        if (Prescription.STATUS_FILLED.equalsIgnoreCase(prescription.getStatus())
                || Prescription.STATUS_REFILL_REQUESTED.equalsIgnoreCase(prescription.getStatus())) {
            Label filledLabel = new Label("Filled: " + formatTimestamp(prescription.getFilledAt())
                    + " by " + valueOrDefault(prescription.getFilledBy(), valueOrDefault(prescription.getPharmacyName(), "Pharmacy")));
            filledLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(filledLabel);
        } else {
            Button fillButton = new Button("Mark Filled");
            fillButton.setStyle("-fx-background-color: #14B8A6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
            fillButton.setOnAction(event -> markPrescriptionFilled(prescription, fillButton));
            actionRow.getChildren().add(fillButton);
        }

        if (canProcessRefillRequest(prescription)) {
            Button refillButton = new Button("Refill Prescription");
            refillButton.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
            refillButton.setOnAction(event -> refillPrescription(prescription, refillButton));
            actionRow.getChildren().add(refillButton);
        }

        if (!actionRow.getChildren().isEmpty()) {
            card.getChildren().add(actionRow);
        }

        return card;
    }

    private void markPrescriptionFilled(Prescription prescription, Button fillButton) {
        fillButton.setDisable(true);
        showStatus("Updating prescription status...", false);

        firebaseService.markPrescriptionFilled(prescription.getPrescriptionId(), pharmacyProfile)
                .thenRun(() -> Platform.runLater(this::loadPrescriptions))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        fillButton.setDisable(false);
                        showStatus("Failed to update prescription: " + cleanErrorMessage(e), true);
                    });
                    return null;
                });
    }

    private void refillPrescription(Prescription prescription, Button refillButton) {
        refillButton.setDisable(true);
        showStatus("Processing refill...", false);

        firebaseService.refillPrescription(
                        prescription.getPrescriptionId(),
                        "PHARMACY",
                        pharmacyProfile.getPharmacyName(),
                        pharmacyProfile
                )
                .thenAccept(refill -> Platform.runLater(() -> {
                    showStatus("Refill completed for " + valueOrDefault(refill.getMedicationName(), "medication")
                            + ". " + PrescriptionRefillSupport.formatRemainingRefills(refill.getRemainingRefills()) + ".", false);
                    loadPrescriptions();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        refillButton.setDisable(false);
                        showStatus(cleanErrorMessage(e), true);
                    });
                    return null;
                });
    }

    @FXML
    private void onBack() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }

    private String getStatusStyle(String status) {
        if (Prescription.STATUS_REFILL_REQUESTED.equalsIgnoreCase(status)) {
            return "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12;";
        }
        if (Prescription.STATUS_FILLED.equalsIgnoreCase(status)) {
            return "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12;";
        }
        return "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12;";
    }

    private String formatPharmacy(Prescription prescription) {
        String name = valueOrDefault(prescription.getPharmacyName(), "Pharmacy");
        String address = valueOrDefault(prescription.getPharmacyAddress(), "Address not provided");
        String phone = valueOrDefault(prescription.getPharmacyPhoneNumber(), "Phone not provided");
        return name + " | " + address + " | " + phone;
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMAT);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }

    private Label createDetailLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");
        return label;
    }

    private String getMedicationName(Prescription prescription) {
        return firstNonBlank(prescription.getMedicationName(), extractMedicationPart(prescription.getMedicationInformation(), "Medication Name: ", " | "));
    }

    private String getDosage(Prescription prescription) {
        return firstNonBlank(prescription.getDosage(), extractMedicationPart(prescription.getMedicationInformation(), "Dosage: ", " | "));
    }

    private String getQuantity(Prescription prescription) {
        return firstNonBlank(prescription.getQuantity(), extractMedicationPart(prescription.getMedicationInformation(), "Quantity: ", " | "));
    }

    private String getRefillDetails(Prescription prescription) {
        Integer remainingRefills = PrescriptionRefillSupport.getRemainingRefills(prescription);
        if (remainingRefills != null) {
            return PrescriptionRefillSupport.formatRemainingRefills(remainingRefills);
        }
        return valueOrDefault(firstNonBlank(prescription.getRefillDetails(), extractMedicationPart(prescription.getMedicationInformation(), "Refills: ", null)), "Not provided");
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    private String extractMedicationPart(String text, String prefix, String suffix) {
        if (text == null || text.isBlank() || prefix == null || !text.contains(prefix)) {
            return null;
        }

        int start = text.indexOf(prefix) + prefix.length();
        int end = suffix == null ? text.length() : text.indexOf(suffix, start);
        if (end < 0) {
            end = text.length();
        }

        String value = text.substring(start, end).trim();
        return value.isBlank() ? null : value;
    }

    private boolean hasAvailableRefills(Prescription prescription) {
        Integer remainingRefills = PrescriptionRefillSupport.getRemainingRefills(prescription);
        return remainingRefills != null && remainingRefills > 0;
    }

    private boolean canProcessRefillRequest(Prescription prescription) {
        return Boolean.TRUE.equals(prescription.getRefillRequested()) && hasAvailableRefills(prescription);
    }

    private String formatStatus(String status) {
        if (Prescription.STATUS_REFILL_REQUESTED.equalsIgnoreCase(status)) {
            return "REFILL REQUESTED";
        }
        return valueOrDefault(status, Prescription.STATUS_SENT);
    }
}
