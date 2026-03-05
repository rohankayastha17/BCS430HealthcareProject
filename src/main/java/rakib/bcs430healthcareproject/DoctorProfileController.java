package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DoctorProfileController {

    @FXML private Button backButton;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;

    @FXML private TextField nameField;
    @FXML private TextField specialtyField;
    @FXML private TextField licenseField;
    @FXML private TextArea bioArea;

    @FXML private TextField clinicNameField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField zipField;

    @FXML private TextField phoneField;
    @FXML private TextField publicEmailField;

    @FXML private CheckBox acceptingNewPatientsCheck;
    @FXML private TextArea insuranceArea;
    // weekly availability fields (e.g. "9:00 AM - 5:00 PM")
    @FXML private ComboBox<String> mondayField;
    @FXML private ComboBox<String> tuesdayField;
    @FXML private ComboBox<String> wednesdayField;
    @FXML private ComboBox<String> thursdayField;
    @FXML private ComboBox<String> fridayField;
    @FXML private ComboBox<String> saturdayField;
    @FXML private ComboBox<String> sundayField;
    // legacy freeform area kept for backward compatibility (still stored in profile.hours)
    @FXML private TextArea hoursArea;

    @FXML private ComboBox<String> visitTypeComboBox;
    @FXML private TextArea notesArea;

    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // Snapshot of original values for Cancel
    private DoctorProfileSnapshot original;
    
    // flag if this view is just a patient looking at a doctor's record
    private boolean viewedByPatient = false;

    @FXML
    public void initialize() {
        visitTypeComboBox.getItems().setAll(
                "In-Person",
                "Telehealth",
                "In-Person + Telehealth"
        );

        // Setup availability dropdowns with time slots
        setupAvailabilityDropdowns();

        // Always start in view mode
        setEditing(false);

        // If a patient requested to view a doctor's profile, populate fields
        Doctor selected = UserContext.getInstance().getSelectedDoctor();
        if (selected != null && UserContext.getInstance().isPatient()) {
            viewedByPatient = true;
            populateFromDoctor(selected);
            titleLabel.setText("Doctor Profile");
            // hide edit controls since patient should not edit and disable fields
            editButton.setVisible(false);
            editButton.setManaged(false);
            saveButton.setVisible(false);
            saveButton.setManaged(false);
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);
            // Ensure all fields are disabled for patient view
            setEditing(false);
        } else if (UserContext.getInstance().isDoctor()) {
            // doctor editing their own profile; load from context if available
            DoctorProfile dp = UserContext.getInstance().getDoctorProfile();
            if (dp != null) {
                populateFromDoctorProfile(dp);
            } else {
                loadMockData();
            }
            original = snapshot();
        } else {
            // fallback
            loadMockData();
            original = snapshot();
        }
    }

    /**
     * Setup availability dropdowns with common time slots and "Closed" option
     */
    private void setupAvailabilityDropdowns() {
        // Common time slots for doctor's office hours
        java.util.List<String> timeSlots = java.util.Arrays.asList(
            "Closed",
            "8:00 AM - 5:00 PM",
            "8:30 AM - 5:30 PM",
            "9:00 AM - 5:00 PM",
            "9:00 AM - 6:00 PM",
            "9:30 AM - 5:30 PM",
            "10:00 AM - 6:00 PM",
            "8:00 AM - 12:00 PM",
            "9:00 AM - 1:00 PM",
            "10:00 AM - 2:00 PM",
            "1:00 PM - 5:00 PM",
            "2:00 PM - 6:00 PM"
        );

        mondayField.getItems().setAll(timeSlots);
        tuesdayField.getItems().setAll(timeSlots);
        wednesdayField.getItems().setAll(timeSlots);
        thursdayField.getItems().setAll(timeSlots);
        fridayField.getItems().setAll(timeSlots);
        saturdayField.getItems().setAll(timeSlots);
        sundayField.getItems().setAll(timeSlots);

        // Set default values to "Closed" for weekends
        saturdayField.setValue("Closed");
        sundayField.setValue("Closed");
    }

    private void populateFromDoctor(Doctor doctor) {
        // fill fields from Doctor object (used when patient views another doctor's profile)
        nameField.setText(doctor.getName());
        specialtyField.setText(doctor.getSpecialty());
        licenseField.setText(doctor.getLicenseNumber());
        bioArea.setText(doctor.getBio());

        clinicNameField.setText(doctor.getClinicName());
        addressField.setText(doctor.getAddress());
        cityField.setText(doctor.getCity());
        stateField.setText(doctor.getState());
        zipField.setText(doctor.getZip());

        phoneField.setText(doctor.getPhone());
        publicEmailField.setText(doctor.getPublicEmail());

        acceptingNewPatientsCheck.setSelected(
                doctor.getAcceptingNewPatients() != null && doctor.getAcceptingNewPatients());
        insuranceArea.setText(doctor.getInsuranceInfo());
        // populate availability fields if map present
        if (doctor.getAvailability() != null && !doctor.getAvailability().isEmpty()) {
            mondayField.setValue(doctor.getAvailability().getOrDefault("Monday", "Closed"));
            tuesdayField.setValue(doctor.getAvailability().getOrDefault("Tuesday", "Closed"));
            wednesdayField.setValue(doctor.getAvailability().getOrDefault("Wednesday", "Closed"));
            thursdayField.setValue(doctor.getAvailability().getOrDefault("Thursday", "Closed"));
            fridayField.setValue(doctor.getAvailability().getOrDefault("Friday", "Closed"));
            saturdayField.setValue(doctor.getAvailability().getOrDefault("Saturday", "Closed"));
            sundayField.setValue(doctor.getAvailability().getOrDefault("Sunday", "Closed"));
            // also keep legacy text area in sync
            hoursArea.setText(doctor.getHours());
        } else {
            hoursArea.setText(doctor.getHours());
        }

        visitTypeComboBox.setValue(doctor.getVisitType());
        notesArea.setText(doctor.getNotes());
    }

    /**
     * Fill UI with data from DoctorProfile (used when doctor edits their own information).
     */
    private void populateFromDoctorProfile(DoctorProfile profile) {
        nameField.setText(profile.getName());
        specialtyField.setText(profile.getSpecialty());
        licenseField.setText(profile.getLicenseNumber());
        bioArea.setText(profile.getBio());

        clinicNameField.setText(profile.getClinicName());
        addressField.setText(profile.getAddress());
        cityField.setText(profile.getCity());
        stateField.setText(profile.getState());
        zipField.setText(profile.getZip());

        phoneField.setText(profile.getPhone());
        publicEmailField.setText(profile.getEmail());

        acceptingNewPatientsCheck.setSelected(
                profile.getAcceptingNewPatients() != null && profile.getAcceptingNewPatients());
        insuranceArea.setText(profile.getInsuranceInfo());

        if (profile.getAvailability() != null && !profile.getAvailability().isEmpty()) {
            mondayField.setValue(profile.getAvailability().getOrDefault("Monday", "Closed"));
            tuesdayField.setValue(profile.getAvailability().getOrDefault("Tuesday", "Closed"));
            wednesdayField.setValue(profile.getAvailability().getOrDefault("Wednesday", "Closed"));
            thursdayField.setValue(profile.getAvailability().getOrDefault("Thursday", "Closed"));
            fridayField.setValue(profile.getAvailability().getOrDefault("Friday", "Closed"));
            saturdayField.setValue(profile.getAvailability().getOrDefault("Saturday", "Closed"));
            sundayField.setValue(profile.getAvailability().getOrDefault("Sunday", "Closed"));
            hoursArea.setText(profile.getHours());
        } else {
            hoursArea.setText(profile.getHours());
        }

        visitTypeComboBox.setValue(profile.getVisitType());
        notesArea.setText(profile.getNotes());
    }

    private void loadMockData() {
        nameField.setText("Dr. Rakib Ahmed");
        specialtyField.setText("Family Medicine");
        clinicNameField.setText("TealCare Clinic");
        addressField.setText("123 Main St");
        cityField.setText("Farmingdale");
        stateField.setText("NY");
        zipField.setText("11735");
        phoneField.setText("(555) 555-5555");
        publicEmailField.setText("office@tealcare.com");
        acceptingNewPatientsCheck.setSelected(true);
        insuranceArea.setText("Aetna, BCBS, UnitedHealthcare");
        // sample weekly availability
        mondayField.setValue("9:00 AM - 5:00 PM");
        tuesdayField.setValue("9:00 AM - 5:00 PM");
        wednesdayField.setValue("9:00 AM - 5:00 PM");
        thursdayField.setValue("9:00 AM - 5:00 PM");
        fridayField.setValue("9:00 AM - 5:00 PM");
        saturdayField.setValue("9:00 AM - 1:00 PM");
        sundayField.setValue("Closed");
        hoursArea.setText("Mon-Fri 9am-5pm\nSat 9am-1pm");
        visitTypeComboBox.setValue("In-Person + Telehealth");
        bioArea.setText("Board-certified physician focused on preventive care and patient education.");
    }

    @FXML
    private void onBack() {
        if (viewedByPatient) {
            // back to search results
            UserContext.getInstance().clearSelectedDoctor();
            SceneRouter.go("doctor-search-view.fxml", "Find a Doctor");
            return;
        }

        if (UserContext.getInstance().isDoctor()) {
            SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
        } else {
            SceneRouter.go("signup-role-view.fxml", "Sign Up");
        }
    }

    @FXML
    private void onEdit() {
        // Prevent patients from editing doctor profiles
        if (viewedByPatient) {
            showStatus("You cannot edit this doctor's profile", true);
            return;
        }
        original = snapshot();
        setEditing(true);
        showStatus("Editing enabled", false);
    }

    @FXML
    private void onCancel() {
        // Prevent patients from canceling (shouldn't reach here anyway)
        if (viewedByPatient) {
            return;
        }
        if (original != null) restore(original);
        setEditing(false);
        showStatus("Changes cancelled", false);
    }

    @FXML
    private void onSave() {
        // Prevent patients from saving changes
        if (viewedByPatient) {
            showStatus("You cannot edit this doctor's profile", true);
            return;
        }
        // Basic validation
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()
                || specialtyField.getText() == null || specialtyField.getText().trim().isEmpty()
                || clinicNameField.getText() == null || clinicNameField.getText().trim().isEmpty()
                || addressField.getText() == null || addressField.getText().trim().isEmpty()
                || cityField.getText() == null || cityField.getText().trim().isEmpty()
                || stateField.getText() == null || stateField.getText().trim().length() != 2
                || zipField.getText() == null || !zipField.getText().trim().matches("\\d{5}")) {
            showStatus("Please fill required fields (State=2 letters, ZIP=5 digits).", true);
            return;
        }

        showStatus("Saving to server...", false);

        // build or update profile
        UserContext ctx = UserContext.getInstance();
        DoctorProfile tempProfile = ctx.getDoctorProfile();
        if (tempProfile == null) {
            tempProfile = new DoctorProfile();
            tempProfile.setUid(ctx.getUid());
        }

        tempProfile.setName(text(nameField));
        tempProfile.setSpecialty(text(specialtyField));
        tempProfile.setLicenseNumber(text(licenseField));
        tempProfile.setBio(text(bioArea));

        tempProfile.setClinicName(text(clinicNameField));
        tempProfile.setAddress(text(addressField));
        tempProfile.setCity(text(cityField));
        tempProfile.setState(text(stateField));
        tempProfile.setZip(text(zipField));

        tempProfile.setPhone(text(phoneField));
        tempProfile.setEmail(text(publicEmailField));

        tempProfile.setAcceptingNewPatients(acceptingNewPatientsCheck.isSelected());
        tempProfile.setInsuranceInfo(text(insuranceArea));

        // collect daily availability map
        java.util.Map<String,String> availability = new java.util.HashMap<>();
        availability.put("Monday", text(mondayField));
        availability.put("Tuesday", text(tuesdayField));
        availability.put("Wednesday", text(wednesdayField));
        availability.put("Thursday", text(thursdayField));
        availability.put("Friday", text(fridayField));
        availability.put("Saturday", text(saturdayField));
        availability.put("Sunday", text(sundayField));
        tempProfile.setAvailability(availability);

        // also update legacy hours string for older views (join nonempty)
        StringBuilder sb = new StringBuilder();
        availability.forEach((day, hours) -> {
            if (hours != null && !hours.isEmpty()) {
                sb.append(day).append(": ").append(hours).append("\n");
            }
        });
        tempProfile.setHours(sb.toString().trim());

        tempProfile.setVisitType(visitTypeComboBox.getValue());
        tempProfile.setNotes(text(notesArea));

        final DoctorProfile profile = tempProfile;  // final reference for lambda
        FirebaseService service = new FirebaseService();
        service.updateDoctorProfile(profile.getUid(), profile)
                .thenRun(() -> javafx.application.Platform.runLater(() -> {
                    ctx.updateDoctorProfile(profile);
                    setEditing(false);
                    showStatus("Profile saved successfully ✅", false);
                }))
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() ->
                            showStatus("Failed to save profile: " + ex.getMessage(), true)
                    );
                    return null;
                });
    }

    private void setEditing(boolean editing) {
        // In edit mode = fields enabled
        boolean disabled = !editing;

        nameField.setDisable(disabled);
        specialtyField.setDisable(disabled);
        licenseField.setDisable(disabled);
        bioArea.setDisable(disabled);

        clinicNameField.setDisable(disabled);
        addressField.setDisable(disabled);
        cityField.setDisable(disabled);
        stateField.setDisable(disabled);
        zipField.setDisable(disabled);

        phoneField.setDisable(disabled);
        publicEmailField.setDisable(disabled);

        acceptingNewPatientsCheck.setDisable(disabled);
        insuranceArea.setDisable(disabled);
        mondayField.setDisable(disabled);
        tuesdayField.setDisable(disabled);
        wednesdayField.setDisable(disabled);
        thursdayField.setDisable(disabled);
        fridayField.setDisable(disabled);
        saturdayField.setDisable(disabled);
        sundayField.setDisable(disabled);
        hoursArea.setDisable(disabled);

        visitTypeComboBox.setDisable(disabled);
        notesArea.setDisable(disabled);

        // Toggle buttons - but don't show edit button for patients viewing profiles
        if (!viewedByPatient) {
            editButton.setVisible(!editing);
            editButton.setManaged(!editing);
        }

        saveButton.setVisible(editing);
        saveButton.setManaged(editing);

        cancelButton.setVisible(editing);
        cancelButton.setManaged(editing);
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #dc2626; -fx-font-size: 11; -fx-padding: 5 0 0 0;"
                : "-fx-text-fill: #dcfce7; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
    }

    private DoctorProfileSnapshot snapshot() {
        return new DoctorProfileSnapshot(
                text(nameField), text(specialtyField), text(licenseField), text(bioArea),
                text(clinicNameField), text(addressField), text(cityField), text(stateField), text(zipField),
                text(phoneField), text(publicEmailField),
                acceptingNewPatientsCheck.isSelected(),
                text(insuranceArea),
                text(mondayField), text(tuesdayField), text(wednesdayField),
                text(thursdayField), text(fridayField), text(saturdayField), text(sundayField),
                text(hoursArea),
                visitTypeComboBox.getValue(),
                text(notesArea)
        );
    }

    private void restore(DoctorProfileSnapshot s) {
        nameField.setText(s.name);
        specialtyField.setText(s.specialty);
        licenseField.setText(s.license);
        bioArea.setText(s.bio);

        clinicNameField.setText(s.clinicName);
        addressField.setText(s.address);
        cityField.setText(s.city);
        stateField.setText(s.state);
        zipField.setText(s.zip);

        phoneField.setText(s.phone);
        publicEmailField.setText(s.publicEmail);

        acceptingNewPatientsCheck.setSelected(s.acceptingNewPatients);
        insuranceArea.setText(s.insurance);
        mondayField.setValue(s.monday);
        tuesdayField.setValue(s.tuesday);
        wednesdayField.setValue(s.wednesday);
        thursdayField.setValue(s.thursday);
        fridayField.setValue(s.friday);
        saturdayField.setValue(s.saturday);
        sundayField.setValue(s.sunday);
        hoursArea.setText(s.hours);

        visitTypeComboBox.setValue(s.visitType);
        notesArea.setText(s.notes);
    }

    private String text(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private String text(TextArea ta) {
        return ta.getText() == null ? "" : ta.getText().trim();
    }

    private String text(ComboBox<String> cb) {
        return cb.getValue() == null ? "" : cb.getValue().trim();
    }

    private static class DoctorProfileSnapshot {
        final String name, specialty, license, bio;
        final String clinicName, address, city, state, zip;
        final String phone, publicEmail;
        final boolean acceptingNewPatients;
        final String insurance;
        final String monday, tuesday, wednesday, thursday, friday, saturday, sunday;
        final String hours;
        final String visitType, notes;

        DoctorProfileSnapshot(String name, String specialty, String license, String bio,
                              String clinicName, String address, String city, String state, String zip,
                              String phone, String publicEmail,
                              boolean acceptingNewPatients,
                              String insurance,
                              String monday, String tuesday, String wednesday, String thursday,
                              String friday, String saturday, String sunday,
                              String hours,
                              String visitType, String notes) {
            this.name = name;
            this.specialty = specialty;
            this.license = license;
            this.bio = bio;
            this.clinicName = clinicName;
            this.address = address;
            this.city = city;
            this.state = state;
            this.zip = zip;
            this.phone = phone;
            this.publicEmail = publicEmail;
            this.acceptingNewPatients = acceptingNewPatients;
            this.insurance = insurance;
            this.monday = monday;
            this.tuesday = tuesday;
            this.wednesday = wednesday;
            this.thursday = thursday;
            this.friday = friday;
            this.saturday = saturday;
            this.sunday = sunday;
            this.hours = hours;
            this.visitType = visitType;
            this.notes = notes;
        }
    }
}