package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
                    Platform.runLater(() ->
                            showStatus("Failed to load patients: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void renderPatients(List<PatientProfile> patients) {
        patientsListVBox.getChildren().clear();

        if (patients == null || patients.isEmpty()) {
            showStatus("No patients are linked to this doctor yet.", false);
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
        nameLabel.setStyle("-fx-font-size: 17; -fx-font-weight: bold;");

        Label emailLabel = new Label("Email: " + fallback(patient.getEmail()));
        Label phoneLabel = new Label("Phone: " + fallback(patient.getPhoneNumber()));

        Label medsLabel = new Label("Current Medications: " + fallback(patient.getCurrentMedications()));
        medsLabel.setWrapText(true);

        HBox buttonRow = new HBox(10);

        Button medicalHistoryButton = new Button("Medical History");
        medicalHistoryButton.setOnAction(e -> onViewMedicalHistory(patient));

        Button sendPrescriptionButton = new Button("Send Prescription");
        sendPrescriptionButton.setOnAction(e -> onSendPrescription(patient));

        Button sendTextButton = new Button("Send Text");
        sendTextButton.setOnAction(e -> onSendText(patient));

        Button refillPrescriptionButton = new Button("Refill Prescription");
        refillPrescriptionButton.setOnAction(e -> onRefillPrescription(patient));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonRow.getChildren().addAll(
                medicalHistoryButton,
                sendPrescriptionButton,
                sendTextButton,
                refillPrescriptionButton,
                spacer
        );

        card.getChildren().addAll(nameLabel, emailLabel, phoneLabel, medsLabel, buttonRow);

        return card;
    }

    private void onViewMedicalHistory(PatientProfile patient) {
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("doctor-patient-history-view.fxml", "Patient Medical History");
    }

    private void onSendPrescription(PatientProfile patient) {
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("doctor-prescription-view.fxml", "Send Prescription");
    }

    private void onSendText(PatientProfile patient) {
        userContext.setSelectedPatientProfile(patient);
        SceneRouter.go("doctor-message-view.fxml", "Send Message");
    }

    private void onRefillPrescription(PatientProfile patient) {
        showStatus("Loading refillable prescriptions...", false);

        firebaseService.getPatientPrescriptions(patient.getUid())
                .thenAccept(prescriptions -> Platform.runLater(() ->
                        showRefillChooser(patient, prescriptions)))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Failed: " + cleanErrorMessage(e), true));
                    return null;
                });
    }

    private void showRefillChooser(PatientProfile patient, List<Prescription> prescriptions) {

        List<PrescriptionChoice> refillable = new ArrayList<>();

        if (prescriptions != null) {
            for (Prescription p : prescriptions) {
                Integer remaining = PrescriptionRefillSupport.getRemainingRefills(p);

                if (remaining != null && remaining > 0) {
                    refillable.add(new PrescriptionChoice(p));
                }
            }
        }

        if (refillable.isEmpty()) {
            showStatus("No refills available.", true);
            return;
        }

        ChoiceDialog<PrescriptionChoice> dialog =
                new ChoiceDialog<>(refillable.get(0), refillable);

        Optional<PrescriptionChoice> result = dialog.showAndWait();

        result.ifPresent(choice -> {
            firebaseService.refillPrescription(
                    choice.prescription.getPrescriptionId(),
                    "DOCTOR",
                    userContext.getName(),
                    null
            ).thenAccept(r -> Platform.runLater(() ->
                    showStatus("Refill sent.", false)));
        });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
    }

    private String cleanErrorMessage(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : "Unknown error";
    }

    private String fallback(String v) {
        return v == null || v.isBlank() ? "Not provided" : v;
    }

    private static class PrescriptionChoice {
        private final Prescription prescription;

        private PrescriptionChoice(Prescription p) {
            this.prescription = p;
        }

        @Override
        public String toString() {
            return prescription.getMedicationName();
        }
    }
}