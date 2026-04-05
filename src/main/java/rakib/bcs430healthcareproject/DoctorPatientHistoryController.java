package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class DoctorPatientHistoryController {

    @FXML private Label patientNameLabel;
    @FXML private Label statusLabel;

    // Overview
    @FXML private Label overviewAgeLabel;
    @FXML private Label overviewGenderLabel;
    @FXML private Label overviewDobLabel;

    // Personal Info
    @FXML private Label fullNameLabel;
    @FXML private Label ageLabel;
    @FXML private Label genderLabel;
    @FXML private Label dobLabel;

    // Contact Info
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label zipLabel;

    // Insurance Info
    @FXML private Label insuranceCompanyLabel;
    @FXML private Label insuranceNumberLabel;
    @FXML private Label preferredPharmacyLabel;
    @FXML private Label preferredPharmacyAddressLabel;
    @FXML private Label preferredPharmacyPhoneLabel;

    // Medical Info
    @FXML private Label bloodTypeLabel;
    @FXML private Label vaccinationStatusLabel;
    @FXML private Label heightLabel;
    @FXML private Label weightLabel;

    @FXML private TextArea allergiesArea;
    @FXML private TextArea medicationsArea;
    @FXML private TextArea chronicConditionsArea;
    @FXML private TextArea medicalHistoryArea;

    // Emergency Contact
    @FXML private Label emergencyContactNameLabel;
    @FXML private Label emergencyContactRelationshipLabel;
    @FXML private Label emergencyContactPhoneLabel;

    private FirebaseService firebaseService;
    private UserContext userContext;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        setReadOnly(allergiesArea);
        setReadOnly(medicationsArea);
        setReadOnly(chronicConditionsArea);
        setReadOnly(medicalHistoryArea);

        loadPatientHistory();
    }

    private void loadPatientHistory() {
        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            statusLabel.setText("Access denied.");
            return;
        }

        String selectedPatientUid = userContext.getSelectedPatientUid();
        PatientProfile selectedPatientProfile = userContext.getSelectedPatientProfile();

        if (selectedPatientProfile != null) {
            populateFields(selectedPatientProfile);
            statusLabel.setText("Patient information loaded.");
            return;
        }

        if (selectedPatientUid == null || selectedPatientUid.isBlank()) {
            statusLabel.setText("No patient selected.");
            return;
        }

        statusLabel.setText("Loading patient information...");

        firebaseService.getPatientProfile(selectedPatientUid)
                .thenAccept(profile -> Platform.runLater(() -> {
                    userContext.setSelectedPatientProfile(profile);
                    populateFields(profile);
                    statusLabel.setText("Patient information loaded.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            statusLabel.setText("Failed to load patient information.")
                    );
                    ex.printStackTrace();
                    return null;
                });
    }

    private void populateFields(PatientProfile profile) {
        if (profile == null) {
            statusLabel.setText("Patient profile not found.");
            return;
        }

        String ageText = profile.getAge() == null ? "Not provided" : String.valueOf(profile.getAge());
        String genderText = valueOrDefault(profile.getGender());
        String dobText = valueOrDefault(profile.getDateOfBirth());

        patientNameLabel.setText(valueOrDefault(profile.getName()));

        // Overview
        overviewAgeLabel.setText(ageText);
        overviewGenderLabel.setText(genderText);
        overviewDobLabel.setText(dobText);

        // Personal Information
        fullNameLabel.setText(valueOrDefault(profile.getName()));
        ageLabel.setText(ageText);
        genderLabel.setText(genderText);
        dobLabel.setText(dobText);

        // Contact Information
        emailLabel.setText(valueOrDefault(profile.getEmail()));
        phoneLabel.setText(valueOrDefault(profile.getPhoneNumber()));
        zipLabel.setText(valueOrDefault(profile.getZip()));

        // Insurance Information
        insuranceCompanyLabel.setText(valueOrDefault(profile.getInsuranceCompany()));
        insuranceNumberLabel.setText(valueOrDefault(profile.getInsuranceNumber()));
        preferredPharmacyLabel.setText(valueOrDefault(profile.getPreferredPharmacyName()));
        preferredPharmacyAddressLabel.setText(valueOrDefault(profile.getPreferredPharmacyAddress()));
        preferredPharmacyPhoneLabel.setText(valueOrDefault(profile.getPreferredPharmacyPhoneNumber()));

        // Medical Information
        bloodTypeLabel.setText(valueOrDefault(profile.getBloodType()));
        vaccinationStatusLabel.setText(valueOrDefault(profile.getVaccinationStatus()));
        heightLabel.setText(valueOrDefault(profile.getHeight()));
        weightLabel.setText(profile.getWeight() == null ? "Not provided" : profile.getWeight().toString());

        allergiesArea.setText(valueOrDefault(profile.getAllergies()));
        medicationsArea.setText(valueOrDefault(profile.getCurrentMedications()));
        chronicConditionsArea.setText(valueOrDefault(profile.getChronicConditions()));
        medicalHistoryArea.setText(valueOrDefault(profile.getMedicalHistory()));

        // Emergency Contact
        emergencyContactNameLabel.setText(valueOrDefault(profile.getEmergencyContactName()));
        emergencyContactRelationshipLabel.setText(valueOrDefault(profile.getEmergencyContactRelationship()));
        emergencyContactPhoneLabel.setText(valueOrDefault(profile.getEmergencyContactPhone()));
    }

    private void setReadOnly(TextArea area) {
        area.setEditable(false);
        area.setWrapText(true);
        area.setFocusTraversable(false);
    }

    private String valueOrDefault(String value) {
        return (value == null || value.isBlank()) ? "Not provided" : value;
    }

    @FXML
    private void handleBack() {
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }
}