package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller for the Patient Profile View.
 * Allows patients to view and edit their profile information.
 */
public class ProfileController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private DatePicker dateOfBirthPicker;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private TextField insuranceNumberField;
    @FXML private TextField insuranceCompanyField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> bloodTypeComboBox;
    @FXML private ComboBox<String> vaccinationStatusComboBox;
    @FXML private ComboBox<String> heightComboBox;
    @FXML private TextField weightField;
    @FXML private TextArea allergiesArea;
    @FXML private TextArea currentMedicationsArea;
    @FXML private TextArea chronicConditionsArea;
    @FXML private TextArea medicalHistoryArea;
    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile currentProfile;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();
        
        // Setup combo boxes
        genderComboBox.getItems().addAll("Not specified", "Male", "Female", "Other");
        bloodTypeComboBox.getItems().addAll("Not specified", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");
        vaccinationStatusComboBox.getItems().addAll("Not specified", "Up to date", "Partially vaccinated", "Not vaccinated");
        
        // Setup height dropdown
        heightComboBox.getItems().addAll(
            "Not specified", "4'8\"", "4'9\"", "4'10\"", "4'11\"", "5'0\"", "5'1\"", "5'2\"", "5'3\"", "5'4\"",
            "5'5\"", "5'6\"", "5'7\"", "5'8\"", "5'9\"", "5'10\"", "5'11\"", "6'0\"", "6'1\"", "6'2\"",
            "6'3\"", "6'4\"", "6'5\"", "6'6\""
        );
        
        // Setup age auto-calculation on date change
        dateOfBirthPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && isEditMode) {
                int calculatedAge = calculateAge(newVal);
                ageField.setText(String.valueOf(calculatedAge));
            }
        });
        
        // Load current profile
        if (userContext.isLoggedIn()) {
            currentProfile = userContext.getProfile();
            if (currentProfile != null) {
                loadProfileData();
            } else {
                showStatus("Profile not available", true);
            }
        } else {
            showStatus("Not logged in", true);
            SceneRouter.go("login-view.fxml", "Login");
        }
        
        // Setup button visibility
        updateButtonVisibility();
    }

    /**
     * Load profile data into the UI fields (read-only mode)
     */
    private void loadProfileData() {
        if (currentProfile == null) return;
        
        nameField.setText(currentProfile.getName() != null ? currentProfile.getName() : "");
        emailField.setText(currentProfile.getEmail() != null ? currentProfile.getEmail() : "");
        phoneField.setText(currentProfile.getPhoneNumber() != null ? currentProfile.getPhoneNumber() : "");
        ageField.setText(currentProfile.getAge() != null ? currentProfile.getAge().toString() : "");
        
        if (currentProfile.getDateOfBirth() != null) {
            try {
                dateOfBirthPicker.setValue(java.time.LocalDate.parse(currentProfile.getDateOfBirth()));
            } catch (Exception e) {
                System.err.println("Failed to parse date: " + e.getMessage());
            }
        }
        
        String gender = currentProfile.getGender() != null ? currentProfile.getGender() : "Not specified";
        genderComboBox.setValue(gender);
        
        insuranceNumberField.setText(currentProfile.getInsuranceNumber() != null ? currentProfile.getInsuranceNumber() : "");
        insuranceCompanyField.setText(currentProfile.getInsuranceCompany() != null ? currentProfile.getInsuranceCompany() : "");
        
        String bloodType = currentProfile.getBloodType() != null ? currentProfile.getBloodType() : "Not specified";
        bloodTypeComboBox.setValue(bloodType);
        
        String vaccinationStatus = currentProfile.getVaccinationStatus() != null ? currentProfile.getVaccinationStatus() : "Not specified";
        vaccinationStatusComboBox.setValue(vaccinationStatus);
        
        String height = currentProfile.getHeight() != null ? currentProfile.getHeight() : "Not specified";
        heightComboBox.setValue(height);
        
        Double weight = currentProfile.getWeight();
        weightField.setText(weight != null ? weight.toString() : "");
        
        allergiesArea.setText(currentProfile.getAllergies() != null ? currentProfile.getAllergies() : "");
        currentMedicationsArea.setText(currentProfile.getCurrentMedications() != null ? currentProfile.getCurrentMedications() : "");
        chronicConditionsArea.setText(currentProfile.getChronicConditions() != null ? currentProfile.getChronicConditions() : "");
        medicalHistoryArea.setText(currentProfile.getMedicalHistory() != null ? currentProfile.getMedicalHistory() : "");
        
        setFieldsEditable(false);
    }

    @FXML
    private void onEdit() {
        isEditMode = true;
        setFieldsEditable(true);
        updateButtonVisibility();
        showStatus("Editing mode enabled", false);
    }

    @FXML
    private void onSave() {
        // Validate required fields
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showStatus("Name is required", true);
            return;
        }
        
        System.out.println("Saving profile for UID: " + userContext.getUid());
        
        // Update profile with new values
        currentProfile.setName(nameField.getText().trim());
        
        if (dateOfBirthPicker.getValue() != null) {
            currentProfile.setDateOfBirth(dateOfBirthPicker.getValue().toString());
            System.out.println("Set DOB: " + dateOfBirthPicker.getValue().toString());
        }
        
        try {
            if (ageField.getText() != null && !ageField.getText().trim().isEmpty()) {
                currentProfile.setAge(Integer.parseInt(ageField.getText().trim()));
                System.out.println("Set Age: " + ageField.getText().trim());
            }
        } catch (NumberFormatException e) {
            showStatus("Invalid age value", true);
            return;
        }
        
        if (genderComboBox.getValue() != null) {
            currentProfile.setGender(genderComboBox.getValue());
            System.out.println("Set Gender: " + genderComboBox.getValue());
        }
        
        if (emailField.getText() != null) {
            currentProfile.setEmail(emailField.getText().trim());
        }
        
        if (phoneField.getText() != null) {
            currentProfile.setPhoneNumber(phoneField.getText().trim());
        }
        
        if (insuranceNumberField.getText() != null) {
            currentProfile.setInsuranceNumber(insuranceNumberField.getText().trim());
        }
        
        if (insuranceCompanyField.getText() != null) {
            currentProfile.setInsuranceCompany(insuranceCompanyField.getText().trim());
        }
        
        if (bloodTypeComboBox.getValue() != null) {
            currentProfile.setBloodType(bloodTypeComboBox.getValue());
        }
        
        if (vaccinationStatusComboBox.getValue() != null) {
            currentProfile.setVaccinationStatus(vaccinationStatusComboBox.getValue());
        }
        
        if (heightComboBox.getValue() != null && !heightComboBox.getValue().equals("Not specified")) {
            currentProfile.setHeight(heightComboBox.getValue());
        }
        
        if (weightField.getText() != null && !weightField.getText().trim().isEmpty()) {
            try {
                Double weight = Double.parseDouble(weightField.getText().trim());
                currentProfile.setWeight(weight);
            } catch (NumberFormatException e) {
                showStatus("Invalid weight value. Please enter a number.", true);
                return;
            }
        }
        
        if (allergiesArea.getText() != null) {
            currentProfile.setAllergies(allergiesArea.getText().trim());
        }
        
        if (medicalHistoryArea.getText() != null) {
            currentProfile.setMedicalHistory(medicalHistoryArea.getText().trim());
        }
        
        if (currentMedicationsArea.getText() != null) {
            currentProfile.setCurrentMedications(currentMedicationsArea.getText().trim());
        }
        
        if (chronicConditionsArea.getText() != null) {
            currentProfile.setChronicConditions(chronicConditionsArea.getText().trim());
        }
        
        // Save to Firestore
        showStatus("Saving profile...", false);
        firebaseService.updatePatientProfile(userContext.getUid(), currentProfile)
                .thenAccept(v -> {
                    Platform.runLater(() -> {
                        System.out.println("Profile saved successfully to Firestore");
                        userContext.updatePatientProfile(currentProfile);
                        isEditMode = false;
                        setFieldsEditable(false);
                        updateButtonVisibility();
                        showStatus("Profile saved successfully!", false);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        System.err.println("Error saving profile: " + e.getMessage());
                        e.printStackTrace();
                        showStatus("Failed to save profile: " + e.getMessage(), true);
                    });
                    return null;
                });
    }

    @FXML
    private void onCancel() {
        isEditMode = false;
        loadProfileData();
        updateButtonVisibility();
        showStatus("Changes cancelled", false);
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }

    /**
     * Set editable state for all profile fields
     */
    private void setFieldsEditable(boolean editable) {
        nameField.setEditable(editable);
        emailField.setEditable(editable);
        phoneField.setEditable(editable);
        dateOfBirthPicker.setDisable(!editable);
        ageField.setEditable(editable);
        genderComboBox.setDisable(!editable);
        insuranceNumberField.setEditable(editable);
        insuranceCompanyField.setEditable(editable);
        bloodTypeComboBox.setDisable(!editable);
        vaccinationStatusComboBox.setDisable(!editable);
        heightComboBox.setDisable(!editable);
        weightField.setEditable(editable);
        allergiesArea.setEditable(editable);
        medicalHistoryArea.setEditable(editable);
        currentMedicationsArea.setEditable(editable);
        chronicConditionsArea.setEditable(editable);
    }

    /**
     * Update button visibility based on current mode
     */
    private void updateButtonVisibility() {
        if (isEditMode) {
            editButton.setManaged(false);
            editButton.setVisible(false);
            saveButton.setManaged(true);
            saveButton.setVisible(true);
            cancelButton.setManaged(true);
            cancelButton.setVisible(true);
        } else {
            editButton.setManaged(true);
            editButton.setVisible(true);
            saveButton.setManaged(false);
            saveButton.setVisible(false);
            cancelButton.setManaged(false);
            cancelButton.setVisible(false);
        }
    }

    /**
     * Display status message
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
        }
        
        // Auto-hide status message after 5 seconds
        if (!isError) {
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(5000);
                    Platform.runLater(() -> {
                        statusLabel.setVisible(false);
                        statusLabel.setManaged(false);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /**
     * Calculate age from date of birth
     */
    private int calculateAge(java.time.LocalDate dateOfBirth) {
        java.time.LocalDate today = java.time.LocalDate.now();
        int age = today.getYear() - dateOfBirth.getYear();
        
        // Adjust if birthday hasn't occurred yet this year
        if (today.getMonthValue() < dateOfBirth.getMonthValue() ||
            (today.getMonthValue() == dateOfBirth.getMonthValue() && 
             today.getDayOfMonth() < dateOfBirth.getDayOfMonth())) {
            age--;
        }
        
        return age;
    }
}
