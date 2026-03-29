package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller for sending a prescription to a patient's pharmacy.
 */
public class DoctorPrescriptionController {

    @FXML private Label patientNameLabel;
    @FXML private Label doctorNameLabel;
    @FXML private TextField pharmacyNameField;
    @FXML private TextField pharmacyStreetAddressField;
    @FXML private TextField pharmacyCityField;
    @FXML private TextField pharmacyStateField;
    @FXML private TextField pharmacyZipField;
    @FXML private TextField pharmacyPhoneField;
    @FXML private TextField medicationNameField;
    @FXML private TextField dosageField;
    @FXML private TextField quantityField;
    @FXML private TextField refillDetailsField;
    @FXML private TextArea instructionsArea;
    @FXML private Label statusLabel;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile selectedPatient;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        selectedPatient = userContext.getSelectedPatientProfile();
        if (selectedPatient == null) {
            SceneRouter.go("doctor-patients-view.fxml", "My Patients");
            return;
        }

        patientNameLabel.setText(selectedPatient.getName() != null ? selectedPatient.getName() : "Patient");
        doctorNameLabel.setText(userContext.getName() != null ? "Prescribing Doctor: Dr. " + userContext.getName() : "Prescribing Doctor");
        populatePreferredPharmacy();
    }

    @FXML
    private void onSendPrescription() {
        String pharmacyStreetAddress = safeTrim(pharmacyStreetAddressField.getText());
        String pharmacyCity = safeTrim(pharmacyCityField.getText());
        String pharmacyState = safeTrim(pharmacyStateField.getText()).toUpperCase();
        String pharmacyZip = safeTrim(pharmacyZipField.getText());
        String pharmacyAddress = PharmacyProfile.buildFullAddress(
                pharmacyStreetAddress,
                pharmacyCity,
                pharmacyState,
                pharmacyZip
        );
        String pharmacyPhone = safeTrim(pharmacyPhoneField.getText());
        String medicationName = safeTrim(medicationNameField.getText());
        String dosage = safeTrim(dosageField.getText());
        String quantity = safeTrim(quantityField.getText());
        String refillDetails = safeTrim(refillDetailsField.getText());
        String medicationInformation = buildMedicationInformation(medicationName, dosage, quantity, refillDetails);
        Integer remainingRefills = PrescriptionRefillSupport.parseRemainingRefills(refillDetails);
        String instructions = safeTrim(instructionsArea.getText());

        if (pharmacyStreetAddress.isBlank() || pharmacyCity.isBlank() || pharmacyState.isBlank() || pharmacyZip.isBlank()) {
            showStatus("Street address, city, state, and ZIP are required.", true);
            return;
        }
        if (pharmacyState.length() != 2) {
            showStatus("State must be a 2-letter abbreviation.", true);
            return;
        }
        if (!pharmacyZip.matches("\\d{5}")) {
            showStatus("ZIP code must be 5 digits.", true);
            return;
        }
        if (pharmacyPhone.isBlank()) {
            showStatus("Pharmacy phone number is required.", true);
            return;
        }
        if (medicationName.isBlank()) {
            showStatus("Medication name is required.", true);
            return;
        }
        if (dosage.isBlank()) {
            showStatus("Dosage is required.", true);
            return;
        }
        if (quantity.isBlank()) {
            showStatus("Quantity is required.", true);
            return;
        }
        if (refillDetails.isBlank()) {
            showStatus("Refill details are required.", true);
            return;
        }
        if (remainingRefills == null) {
            showStatus("Refill details must include a count, like '2 refills remaining' or 'No refills remaining'.", true);
            return;
        }
        if (instructions.isBlank()) {
            showStatus("Instructions are required.", true);
            return;
        }

        Prescription prescription = new Prescription();
        prescription.setDoctorUid(userContext.getUid());
        prescription.setDoctorName(userContext.getName());
        prescription.setPatientUid(selectedPatient.getUid());
        prescription.setPatientName(selectedPatient.getName());
        prescription.setPharmacyName(safeTrim(pharmacyNameField.getText()));
        prescription.setPharmacyAddress(pharmacyAddress);
        prescription.setPharmacyAddressNormalized(AddressNormalizer.normalize(pharmacyAddress));
        prescription.setPharmacyPhoneNumber(pharmacyPhone);
        prescription.setMedicationName(medicationName);
        prescription.setDosage(dosage);
        prescription.setQuantity(quantity);
        prescription.setRefillDetails(PrescriptionRefillSupport.formatRemainingRefills(remainingRefills));
        prescription.setRemainingRefills(remainingRefills);
        prescription.setMedicationInformation(medicationInformation);
        prescription.setInstructions(instructions);

        sendButton.setDisable(true);
        showStatus("Sending prescription...", false);

        firebaseService.savePrescription(prescription)
                .thenAccept(prescriptionId -> Platform.runLater(() -> {
                    showStatus("Prescription sent successfully. Reference ID: " + prescriptionId, false);
                    sendButton.setDisable(false);
                    clearForm();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showStatus("Failed to send prescription: " + cleanErrorMessage(e), true);
                        sendButton.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }

    private void clearForm() {
        pharmacyNameField.clear();
        pharmacyStreetAddressField.clear();
        pharmacyCityField.clear();
        pharmacyStateField.clear();
        pharmacyZipField.clear();
        pharmacyPhoneField.clear();
        medicationNameField.clear();
        dosageField.clear();
        quantityField.clear();
        refillDetailsField.clear();
        instructionsArea.clear();
        populatePreferredPharmacy();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12; -fx-font-weight: bold;");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void populatePreferredPharmacy() {
        if (selectedPatient == null) {
            return;
        }

        if (!safeTrim(selectedPatient.getPreferredPharmacyName()).isBlank()) {
            pharmacyNameField.setText(selectedPatient.getPreferredPharmacyName());
        }

        String fullAddress = safeTrim(selectedPatient.getPreferredPharmacyAddress());
        if (!fullAddress.isBlank()) {
            String[] parts = fullAddress.split(",\\s*");
            if (parts.length > 0) {
                pharmacyStreetAddressField.setText(parts[0]);
            }
            if (parts.length > 1) {
                pharmacyCityField.setText(parts[1]);
            }
            if (parts.length > 2) {
                pharmacyStateField.setText(parts[2].trim());
            }
            if (parts.length > 3) {
                pharmacyZipField.setText(parts[3].trim());
            }
        }

        if (!safeTrim(selectedPatient.getPreferredPharmacyPhoneNumber()).isBlank()) {
            pharmacyPhoneField.setText(selectedPatient.getPreferredPharmacyPhoneNumber());
        }
    }

    private String buildMedicationInformation(String medicationName, String dosage, String quantity, String refillDetails) {
        return "Medication Name: " + medicationName
                + " | Dosage: " + dosage
                + " | Quantity: " + quantity
                + " | Refills: " + refillDetails;
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
}
