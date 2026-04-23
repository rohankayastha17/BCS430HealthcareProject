package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

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
    @FXML private ComboBox<String> insuranceCompanyComboBox;
    @FXML private ComboBox<String> insurancePlanTypeComboBox;
    @FXML private TextField insuranceGroupNumberField;
    @FXML private ComboBox<PharmacyOption> preferredPickupPharmacyComboBox;
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
    @FXML private Button pastAppointmentsButton;
    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile currentProfile;
    private boolean isEditMode = false;
    private boolean isDoctorPatientView = false;
    private boolean isPharmacyReadOnlyView = false;
    private List<PharmacyOption> pharmacyOptions = new ArrayList<>();

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();
        
        // Setup combo boxes
        genderComboBox.getItems().addAll("Not specified", "Male", "Female", "Other");
        bloodTypeComboBox.getItems().addAll("Not specified", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");
        vaccinationStatusComboBox.getItems().addAll("Not specified", "Up to date", "Partially vaccinated", "Not vaccinated");
        insuranceCompanyComboBox.getItems().addAll(InsuranceSupport.commonInsuranceProviders());
        insuranceCompanyComboBox.setEditable(true);
        insurancePlanTypeComboBox.getItems().addAll("Not specified", "HMO", "PPO", "EPO", "POS", "HDHP", "Medicaid", "Medicare", "Other");
        preferredPickupPharmacyComboBox.getItems().add(new PharmacyOption(null, "No preferred pharmacy selected", "", ""));
        insuranceCompanyComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateInsuranceNumberAvailability());
        if (insuranceCompanyComboBox.getEditor() != null) {
            insuranceCompanyComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> updateInsuranceNumberAvailability());
        }
        
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
        
        if (!userContext.isLoggedIn()) {
            showStatus("Not logged in", true);
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        if (userContext.isDoctor() && userContext.getSelectedPatientProfile() != null) {
            isDoctorPatientView = true;
            currentProfile = userContext.getSelectedPatientProfile();
            titleLabel.setText("Patient Profile");
        } else if (userContext.isPharmacy() && userContext.getSelectedPatientProfile() != null) {
            isPharmacyReadOnlyView = true;
            currentProfile = userContext.getSelectedPatientProfile();
            titleLabel.setText("Patient Insurance Profile");
        } else {
            currentProfile = userContext.getProfile();
            titleLabel.setText("My Profile");
        }

        if (currentProfile != null) {
            loadProfileData();
        } else {
            showStatus("Profile not available", true);
        }

        loadPharmacyOptions();
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
        insuranceCompanyComboBox.setValue(currentProfile.getInsuranceCompany() != null ? currentProfile.getInsuranceCompany() : "");
        insurancePlanTypeComboBox.setValue(currentProfile.getInsurancePlanType() != null ? currentProfile.getInsurancePlanType() : "Not specified");
        insuranceGroupNumberField.setText(currentProfile.getInsuranceGroupNumber() != null ? currentProfile.getInsuranceGroupNumber() : "");
        selectPreferredPharmacy();
        
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
        updateInsuranceNumberAvailability();
    }

    @FXML
    private void onEdit() {
        if (isReadOnlyPatientView()) {
            showStatus(readOnlyViewMessage(), true);
            return;
        }
        isEditMode = true;
        setFieldsEditable(true);
        updateButtonVisibility();
        showStatus("Editing mode enabled", false);
    }

    @FXML
    private void onSave() {
        if (isReadOnlyPatientView()) {
            showStatus(readOnlyViewMessage(), true);
            return;
        }
        // Validate required fields
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showStatus("Name is required", true);
            return;
        }
        
        String targetUid = isDoctorPatientView ? currentProfile.getUid() : userContext.getUid();
        System.out.println("Saving profile for UID: " + targetUid);
        
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
        
        String insuranceCompany = readInsuranceCompany();
        if (insuranceCompany == null || insuranceCompany.isBlank()) {
            currentProfile.setInsuranceNumber(null);
            currentProfile.setInsurancePlanType(null);
            currentProfile.setInsuranceGroupNumber(null);
        } else if (insuranceNumberField.getText() != null) {
            currentProfile.setInsuranceNumber(insuranceNumberField.getText().trim());
            currentProfile.setInsurancePlanType(readInsurancePlanType());
            currentProfile.setInsuranceGroupNumber(readTrimmedText(insuranceGroupNumberField));
        }
        
        currentProfile.setInsuranceCompany(insuranceCompany);

        PharmacyOption preferredOption = preferredPickupPharmacyComboBox.getValue();
        if (preferredOption == null || preferredOption.uid == null || preferredOption.uid.isBlank()) {
            currentProfile.setPreferredPharmacyUid(null);
            currentProfile.setPreferredPharmacyName(null);
            currentProfile.setPreferredPharmacyAddress(null);
            currentProfile.setPreferredPharmacyPhoneNumber(null);
        } else {
            currentProfile.setPreferredPharmacyUid(preferredOption.uid);
            currentProfile.setPreferredPharmacyName(preferredOption.name);
            currentProfile.setPreferredPharmacyAddress(preferredOption.address);
            currentProfile.setPreferredPharmacyPhoneNumber(preferredOption.phoneNumber);
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
        firebaseService.updatePatientProfile(targetUid, currentProfile)
                .thenAccept(v -> {
                    Platform.runLater(() -> {
                        System.out.println("Profile saved successfully to Firestore");
                        if (isDoctorPatientView) {
                            userContext.setSelectedPatientProfile(currentProfile);
                        } else {
                            userContext.updatePatientProfile(currentProfile);
                        }
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
        if (isDoctorPatientView) {
            userContext.clearSelectedPatientProfile();
            SceneRouter.go("doctor-patients-view.fxml", "My Patients");
            return;
        }
        if (isPharmacyReadOnlyView) {
            userContext.clearSelectedPatientProfile();
            SceneRouter.go("pharmacy-prescriptions-view.fxml", "Pharmacy Portal");
            return;
        }

        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }

    @FXML
    private void onViewPastAppointments() {
        if (isDoctorPatientView && currentProfile != null) {
            userContext.setSelectedPatientUid(currentProfile.getUid());
            userContext.setSelectedPatientProfile(currentProfile);
        }
        SceneRouter.go("patient-past-appointments-view.fxml", "Past Appointments");
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
        insuranceCompanyComboBox.setDisable(!editable);
        insuranceCompanyComboBox.setEditable(editable);
        insurancePlanTypeComboBox.setDisable(!editable);
        insuranceGroupNumberField.setEditable(editable);
        preferredPickupPharmacyComboBox.setDisable(!editable);
        bloodTypeComboBox.setDisable(!editable);
        vaccinationStatusComboBox.setDisable(!editable);
        heightComboBox.setDisable(!editable);
        weightField.setEditable(editable);
        allergiesArea.setEditable(editable);
        medicalHistoryArea.setEditable(editable);
        currentMedicationsArea.setEditable(editable);
        chronicConditionsArea.setEditable(editable);
        updateInsuranceNumberAvailability();
    }

    /**
     * Update button visibility based on current mode
     */
    private void updateButtonVisibility() {
        boolean showPastAppointmentsButton = !isPharmacyReadOnlyView;
        pastAppointmentsButton.setManaged(showPastAppointmentsButton);
        pastAppointmentsButton.setVisible(showPastAppointmentsButton);

        if (isReadOnlyPatientView()) {
            editButton.setManaged(false);
            editButton.setVisible(false);
            saveButton.setManaged(false);
            saveButton.setVisible(false);
            cancelButton.setManaged(false);
            cancelButton.setVisible(false);
            return;
        }

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

    private void loadPharmacyOptions() {
        firebaseService.getAllPharmacies()
                .thenAccept(pharmacies -> Platform.runLater(() -> {
                    pharmacyOptions = new ArrayList<>();
                    preferredPickupPharmacyComboBox.getItems().clear();
                    preferredPickupPharmacyComboBox.getItems().add(new PharmacyOption(null, "No preferred pharmacy selected", "", ""));

                    for (PharmacyProfile pharmacy : pharmacies) {
                        pharmacyOptions.add(new PharmacyOption(
                                pharmacy.getUid(),
                                defaultText(pharmacy.getPharmacyName(), "Pharmacy"),
                                defaultText(pharmacy.getFullAddress(), "Address not provided"),
                                defaultText(pharmacy.getPhoneNumber(), "Phone not provided")
                        ));
                    }

                    preferredPickupPharmacyComboBox.getItems().addAll(pharmacyOptions);
                    selectPreferredPharmacy();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showStatus("Failed to load pharmacy options: " + e.getMessage(), true));
                    return null;
                });
    }

    private void selectPreferredPharmacy() {
        if (preferredPickupPharmacyComboBox == null || currentProfile == null) {
            return;
        }

        String preferredUid = currentProfile.getPreferredPharmacyUid();
        if (preferredUid != null && !preferredUid.isBlank()) {
            for (PharmacyOption option : preferredPickupPharmacyComboBox.getItems()) {
                if (preferredUid.equals(option.uid)) {
                    preferredPickupPharmacyComboBox.setValue(option);
                    return;
                }
            }
        }

        String preferredAddress = currentProfile.getPreferredPharmacyAddress();
        if (preferredAddress != null && !preferredAddress.isBlank()) {
            for (PharmacyOption option : preferredPickupPharmacyComboBox.getItems()) {
                if (preferredAddress.equalsIgnoreCase(option.address)) {
                    preferredPickupPharmacyComboBox.setValue(option);
                    return;
                }
            }
        }

        preferredPickupPharmacyComboBox.getSelectionModel().selectFirst();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String readInsuranceCompany() {
        String editorValue = insuranceCompanyComboBox.getEditor() == null
                ? null
                : insuranceCompanyComboBox.getEditor().getText();

        if (editorValue != null && !editorValue.isBlank()) {
            return editorValue.trim();
        }

        String selectedValue = insuranceCompanyComboBox.getValue();
        return selectedValue == null || selectedValue.isBlank() ? null : selectedValue.trim();
    }

    private String readInsurancePlanType() {
        String selectedValue = insurancePlanTypeComboBox.getValue();
        if (selectedValue == null || selectedValue.isBlank() || "Not specified".equalsIgnoreCase(selectedValue.trim())) {
            return null;
        }
        return selectedValue.trim();
    }

    private String readTrimmedText(TextField field) {
        if (field == null || field.getText() == null) {
            return null;
        }

        String value = field.getText().trim();
        return value.isBlank() ? null : value;
    }

    private void updateInsuranceNumberAvailability() {
        boolean hasInsuranceCompany = readInsuranceCompany() != null;
        boolean allowEditing = !isReadOnlyPatientView() && isEditMode && hasInsuranceCompany;

        insuranceNumberField.setEditable(allowEditing);
        insuranceNumberField.setDisable(!allowEditing && !hasInsuranceCompany && !isReadOnlyPatientView());
        insuranceNumberField.setPromptText(hasInsuranceCompany
                ? "Enter insurance number"
                : "Select insurance company first");
        insurancePlanTypeComboBox.setDisable(!allowEditing);
        insuranceGroupNumberField.setEditable(allowEditing);
        insuranceGroupNumberField.setDisable(!allowEditing && !hasInsuranceCompany && !isReadOnlyPatientView());
        insuranceGroupNumberField.setPromptText(hasInsuranceCompany
                ? "Enter group number"
                : "Select insurance company first");

        if (!hasInsuranceCompany && !isReadOnlyPatientView()) {
            insuranceNumberField.clear();
            insurancePlanTypeComboBox.setValue("Not specified");
            insuranceGroupNumberField.clear();
        }
    }

    private boolean isReadOnlyPatientView() {
        return isPharmacyReadOnlyView;
    }

    private String readOnlyViewMessage() {
        return "Pharmacies can only view patient profiles here.";
    }

    private static class PharmacyOption {
        private final String uid;
        private final String name;
        private final String address;
        private final String phoneNumber;

        private PharmacyOption(String uid, String name, String address, String phoneNumber) {
            this.uid = uid;
            this.name = name;
            this.address = address;
            this.phoneNumber = phoneNumber;
        }

        @Override
        public String toString() {
            if (uid == null || uid.isBlank()) {
                return name;
            }
            return name + " | " + address + " | " + phoneNumber;
        }
    }
}
