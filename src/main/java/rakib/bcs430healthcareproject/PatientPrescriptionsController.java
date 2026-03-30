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
 * Controller for the patient's prescriptions list.
 */
public class PatientPrescriptionsController {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML private VBox prescriptionsListVBox;
    @FXML private Label statusLabel;
    @FXML private TextField medicationSearchField;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private List<Prescription> allPrescriptions = new ArrayList<>();

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isPatient()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        medicationSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        loadPrescriptions();
    }

    private void loadPrescriptions() {
        showStatus("Loading prescriptions...", false);
        firebaseService.getPatientPrescriptions(userContext.getUid())
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
        String query = medicationSearchField.getText() == null ? "" : medicationSearchField.getText().trim().toLowerCase();
        List<Prescription> filteredPrescriptions = new ArrayList<>();
        for (Prescription prescription : allPrescriptions) {
            String medicationName = valueOrDefault(getMedicationName(prescription), "").toLowerCase();
            if (query.isBlank() || medicationName.contains(query)) {
                filteredPrescriptions.add(prescription);
            }
        }

        if (filteredPrescriptions.isEmpty()) {
            showStatus("No prescriptions found yet.", false);
            return;
        }

        showStatus(query.isBlank()
                ? "Found " + filteredPrescriptions.size() + " prescription(s)."
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
        Label medicationLabel = new Label(valueOrDefault(getMedicationName(prescription), "Medication not provided"));
        medicationLabel.setWrapText(true);
        medicationLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 15; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusBadge = new Label(formatStatus(prescription.getStatus()));
        statusBadge.setStyle(getStatusStyle(prescription.getStatus()));
        topRow.getChildren().addAll(medicationLabel, spacer, statusBadge);

        Label doctorLabel = new Label("Doctor: Dr. " + valueOrDefault(prescription.getDoctorName(), "Unknown"));
        doctorLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label dosageLabel = createDetailLabel("Dosage: " + valueOrDefault(getDosage(prescription), "Not provided"));
        Label quantityLabel = createDetailLabel("Quantity: " + valueOrDefault(getQuantity(prescription), "Not provided"));
        Label refillLabel = createDetailLabel("Refill Details: " + getRefillDetails(prescription));

        Label pharmacyLabel = new Label("Pharmacy: " + formatPharmacy(prescription));
        pharmacyLabel.setWrapText(true);
        pharmacyLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label instructionsLabel = createDetailLabel("Instructions: " + valueOrDefault(prescription.getInstructions(), "Not provided"));

        Label sentLabel = new Label("Sent: " + formatTimestamp(prescription.getCreatedAt()));
        sentLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        card.getChildren().addAll(topRow, doctorLabel, dosageLabel, quantityLabel, refillLabel, pharmacyLabel, instructionsLabel, sentLabel);

        if (Boolean.TRUE.equals(prescription.getRefillRequested())) {
            Label refillRequestLabel = new Label("Refill requested by Dr. "
                    + valueOrDefault(prescription.getRefillRequestedBy(), valueOrDefault(prescription.getDoctorName(), "Unknown"))
                    + " on " + formatTimestamp(prescription.getRefillRequestedAt()));
            refillRequestLabel.setStyle("-fx-text-fill: #1D4ED8; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(refillRequestLabel);
        }

        if (Prescription.STATUS_FILLED.equalsIgnoreCase(prescription.getStatus())) {
            Label filledLabel = new Label("Filled: " + formatTimestamp(prescription.getFilledAt())
                    + " by " + valueOrDefault(prescription.getFilledBy(), valueOrDefault(prescription.getPharmacyName(), "Pharmacy")));
            filledLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(filledLabel);
        }

        return card;
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
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

    private String formatStatus(String status) {
        if (Prescription.STATUS_REFILL_REQUESTED.equalsIgnoreCase(status)) {
            return "REFILL REQUESTED";
        }
        return valueOrDefault(status, Prescription.STATUS_SENT);
    }
}
