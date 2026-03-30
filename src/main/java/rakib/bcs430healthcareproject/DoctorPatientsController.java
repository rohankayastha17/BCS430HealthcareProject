package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the doctor-side patient list.
 */
public class DoctorPatientsController {

    @FXML private Label doctorNameLabel;
    @FXML private Label statusLabel;
    @FXML private VBox patientsListVBox;
    @FXML private ScrollPane patientsScrollPane;

    private FirebaseService firebaseService;
    private UserContext userContext;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        String doctorName = userContext.getName() != null ? userContext.getName() : "Doctor";
        doctorNameLabel.setText("Patients for Dr. " + doctorName);

        loadPatients();
    }

    private void loadPatients() {
        showStatus("Loading patient list...", false);
        firebaseService.getDoctorPatients(userContext.getUid())
                .thenAccept(patients -> Platform.runLater(() -> renderPatients(patients)))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load patients: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void renderPatients(List<PatientProfile> patients) {
        patientsListVBox.getChildren().clear();

        if (patients == null || patients.isEmpty()) {
            showStatus("No patients are linked to this doctor yet. Once appointments are booked, they will appear here.", false);
            return;
        }

        showStatus("Found " + patients.size() + " patient(s).", false);
        for (PatientProfile patient : patients) {
            patientsListVBox.getChildren().add(createPatientCard(patient));
        }
    }

    private VBox createPatientCard(PatientProfile patient) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #D1FAE5; -fx-border-radius: 12;");

        Label nameLabel = new Label(patient.getName() != null ? patient.getName() : "Unnamed Patient");
        nameLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 17; -fx-font-weight: bold;");

        Label emailLabel = new Label("Email: " + fallback(patient.getEmail()));
        emailLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label phoneLabel = new Label("Phone: " + fallback(patient.getPhoneNumber()));
        phoneLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        Label medsLabel = new Label("Current Medications: " + fallback(patient.getCurrentMedications()));
        medsLabel.setWrapText(true);
        medsLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");

        HBox buttonRow = new HBox(10);

        Button viewProfileButton = new Button("View Profile");
        viewProfileButton.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        viewProfileButton.setOnAction(event -> onViewProfile(patient));

        Button sendPrescriptionButton = new Button("Send Prescription");
        sendPrescriptionButton.setStyle("-fx-background-color: #14B8A6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        sendPrescriptionButton.setOnAction(event -> onSendPrescription(patient));

        Button sendTextButton = new Button("Send Text");
        sendTextButton.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        sendTextButton.setOnAction(event -> onSendText(patient));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonRow.getChildren().addAll(viewProfileButton, sendPrescriptionButton, sendTextButton, spacer);
        Button refillPrescriptionButton = new Button("Refill Prescription");
        refillPrescriptionButton.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        refillPrescriptionButton.setOnAction(event -> onRefillPrescription(patient));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonRow.getChildren().addAll(viewProfileButton, sendPrescriptionButton, refillPrescriptionButton, spacer);

        card.getChildren().addAll(nameLabel, emailLabel, phoneLabel, medsLabel, buttonRow);
        return card;
    }

    private void onViewProfile(PatientProfile patient) {
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("patient-profile-view.fxml", "Patient Profile");
    }

    private void onSendPrescription(PatientProfile patient) {
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("doctor-prescription-view.fxml", "Send Prescription");
    }

    private void onSendText(PatientProfile patient) {
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("doctor-message-view.fxml", "Send Message");
    private void onRefillPrescription(PatientProfile patient) {
        showStatus("Loading refillable prescriptions for " + fallback(patient.getName()) + "...", false);
        firebaseService.getPatientPrescriptions(patient.getUid())
                .thenAccept(prescriptions -> Platform.runLater(() -> showRefillChooser(patient, prescriptions)))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load refill options: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void showRefillChooser(PatientProfile patient, List<Prescription> prescriptions) {
        List<PrescriptionChoice> refillablePrescriptions = new ArrayList<>();
        if (prescriptions != null) {
            for (Prescription prescription : prescriptions) {
                Integer remainingRefills = PrescriptionRefillSupport.getRemainingRefills(prescription);
                if (remainingRefills != null
                        && remainingRefills > 0
                        && Prescription.STATUS_FILLED.equalsIgnoreCase(prescription.getStatus())
                        && !Boolean.TRUE.equals(prescription.getRefillRequested())) {
                    refillablePrescriptions.add(new PrescriptionChoice(prescription));
                }
            }
        }

        if (refillablePrescriptions.isEmpty()) {
            showStatus("No refills are available for " + fallback(patient.getName()) + ".", true);
            return;
        }

        ChoiceDialog<PrescriptionChoice> dialog = new ChoiceDialog<>(refillablePrescriptions.getFirst(), refillablePrescriptions);
        dialog.setTitle("Refill Prescription");
        dialog.setHeaderText("Choose a prescription to refill for " + fallback(patient.getName()));
        dialog.setContentText("Prescription:");

        Optional<PrescriptionChoice> selectedChoice = dialog.showAndWait();
        if (selectedChoice.isEmpty()) {
            showStatus("Refill cancelled.", false);
            return;
        }

        Prescription prescription = selectedChoice.get().prescription;
        showStatus("Sending refill for " + fallback(patient.getName()) + "...", false);
        firebaseService.refillPrescription(prescription.getPrescriptionId(), "DOCTOR", userContext.getName(), null)
                .thenAccept(refill -> Platform.runLater(() -> {
                    showStatus("Refill request sent for " + fallback(refill.getMedicationName()) + ".", false);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus(cleanErrorMessage(e), true));
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
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

    private String fallback(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}

    private static class PrescriptionChoice {
        private final Prescription prescription;

        private PrescriptionChoice(Prescription prescription) {
            this.prescription = prescription;
        }

        @Override
        public String toString() {
            String medicationName = prescription.getMedicationName() != null && !prescription.getMedicationName().isBlank()
                    ? prescription.getMedicationName()
                    : "Medication";
            return medicationName + " | " + PrescriptionRefillSupport.formatRemainingRefills(PrescriptionRefillSupport.getRemainingRefills(prescription));
        }
    }
}
