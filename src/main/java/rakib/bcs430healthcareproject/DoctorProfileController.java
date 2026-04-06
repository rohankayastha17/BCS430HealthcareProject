package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.controlsfx.control.CheckComboBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @FXML private CheckComboBox<String> insuranceCheckComboBox;

    // weekly availability fields
    @FXML private ComboBox<String> mondayField;
    @FXML private ComboBox<String> tuesdayField;
    @FXML private ComboBox<String> wednesdayField;
    @FXML private ComboBox<String> thursdayField;
    @FXML private ComboBox<String> fridayField;
    @FXML private ComboBox<String> saturdayField;
    @FXML private ComboBox<String> sundayField;

    // legacy freeform area kept for backward compatibility
    @FXML private TextArea hoursArea;

    @FXML private ComboBox<String> visitTypeComboBox;
    @FXML private TextArea notesArea;

    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private DoctorProfileSnapshot original;
    private boolean viewedByPatient = false;

    @FXML
    public void initialize() {
        visitTypeComboBox.getItems().setAll(
                "In-Person",
                "Telehealth",
                "In-Person + Telehealth"
        );
        insuranceCheckComboBox.getItems().setAll(InsuranceSupport.commonInsuranceProviders());

        setupAvailabilityDropdowns();
        setEditing(false);

        Doctor selected = UserContext.getInstance().getSelectedDoctor();
        if (selected != null && UserContext.getInstance().isPatient()) {
            viewedByPatient = true;
            populateFromDoctor(selected);
            titleLabel.setText("Doctor Profile");

            editButton.setVisible(false);
            editButton.setManaged(false);
            saveButton.setVisible(false);
            saveButton.setManaged(false);
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);

            setEditing(false);
        } else if (UserContext.getInstance().isDoctor()) {
            DoctorProfile dp = UserContext.getInstance().getDoctorProfile();
            if (dp != null) {
                populateFromDoctorProfile(dp);
            } else {
                loadMockData();
            }
            original = snapshot();
        } else {
            loadMockData();
            original = snapshot();
        }
    }

    /**
     * Setup availability dropdowns with common time slots and "Closed" option
     */
    private void setupAvailabilityDropdowns() {
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
                "2:00 PM - 6:00 PM",
                "9:00 AM - 12:00 PM, 2:00 PM - 5:00 PM",
                "10:00 AM - 1:00 PM, 3:00 PM - 6:00 PM"
        );

        mondayField.getItems().setAll(timeSlots);
        tuesdayField.getItems().setAll(timeSlots);
        wednesdayField.getItems().setAll(timeSlots);
        thursdayField.getItems().setAll(timeSlots);
        fridayField.getItems().setAll(timeSlots);
        saturdayField.getItems().setAll(timeSlots);
        sundayField.getItems().setAll(timeSlots);

        saturdayField.setValue("Closed");
        sundayField.setValue("Closed");
    }

    private void populateFromDoctor(Doctor doctor) {
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

        applyInsuranceSelection(doctor.getInsuranceInfo());

        populateAvailabilityFields(doctor.getAvailability(), doctor.getHours());

        visitTypeComboBox.setValue(doctor.getVisitType());
        notesArea.setText(doctor.getNotes());
    }

    private void populateFromDoctorProfile(DoctorProfile profile) {
        System.out.println("Bio loaded: '" + profile.getBio() + "'");

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

        applyInsuranceSelection(profile.getInsuranceInfo());

        populateAvailabilityFields(profile.getAvailability(), profile.getHours());

        visitTypeComboBox.setValue(profile.getVisitType());
        notesArea.setText(profile.getNotes());
    }

    private void populateAvailabilityFields(Map<String, String> availability, String legacyHours) {
        mondayField.setValue(readAvailabilityValue(availability, "Monday"));
        tuesdayField.setValue(readAvailabilityValue(availability, "Tuesday"));
        wednesdayField.setValue(readAvailabilityValue(availability, "Wednesday"));
        thursdayField.setValue(readAvailabilityValue(availability, "Thursday"));
        fridayField.setValue(readAvailabilityValue(availability, "Friday"));
        saturdayField.setValue(readAvailabilityValue(availability, "Saturday"));
        sundayField.setValue(readAvailabilityValue(availability, "Sunday"));

        hoursArea.setText(legacyHours == null ? "" : legacyHours);
    }

    private String readAvailabilityValue(Map<String, String> availability, String day) {
        if (availability == null || availability.isEmpty()) {
            return "Closed";
        }

        String value = availability.get(day);
        if (value == null || value.trim().isEmpty()) {
            return "Closed";
        }

        return value.trim();
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
        applyInsuranceSelection("Aetna, UnitedHealthcare");

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
        if (viewedByPatient) {
            return;
        }

        if (original != null) {
            restore(original);
        }

        setEditing(false);
        showStatus("Changes cancelled", false);
    }

    @FXML
    private void onSave() {
        if (viewedByPatient) {
            showStatus("You cannot edit this doctor's profile", true);
            return;
        }

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
        tempProfile.setState(text(stateField).toUpperCase());
        tempProfile.setZip(text(zipField));

        tempProfile.setPhone(text(phoneField));
        tempProfile.setEmail(text(publicEmailField));

        tempProfile.setAcceptingNewPatients(acceptingNewPatientsCheck.isSelected());
        tempProfile.setInsuranceInfo(selectedInsuranceInfo());

        Map<String, String> availability = new HashMap<>();
        putAvailabilityIfOpen(availability, "Monday", mondayField);
        putAvailabilityIfOpen(availability, "Tuesday", tuesdayField);
        putAvailabilityIfOpen(availability, "Wednesday", wednesdayField);
        putAvailabilityIfOpen(availability, "Thursday", thursdayField);
        putAvailabilityIfOpen(availability, "Friday", fridayField);
        putAvailabilityIfOpen(availability, "Saturday", saturdayField);
        putAvailabilityIfOpen(availability, "Sunday", sundayField);
        tempProfile.setAvailability(availability);

        tempProfile.setHours(buildLegacyHoursText());

        tempProfile.setVisitType(visitTypeComboBox.getValue());
        tempProfile.setNotes(text(notesArea));

        final DoctorProfile profile = tempProfile;
        FirebaseService service = new FirebaseService();

        service.updateDoctorProfile(profile.getUid(), profile)
                .thenRun(() -> javafx.application.Platform.runLater(() -> {
                    ctx.updateDoctorProfile(profile);
                    setEditing(false);
                    original = snapshot();
                    showStatus("Profile saved successfully ✅", false);
                }))
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() ->
                            showStatus("Failed to save profile: " + cleanErrorMessage(ex), true)
                    );
                    return null;
                });
    }

    private void putAvailabilityIfOpen(Map<String, String> availability, String day, ComboBox<String> comboBox) {
        String value = text(comboBox);

        if (value.isEmpty() || value.equalsIgnoreCase("Closed")) {
            return;
        }

        availability.put(day, value);
    }

    private String buildLegacyHoursText() {
        StringBuilder sb = new StringBuilder();

        appendHoursLine(sb, "Monday", mondayField);
        appendHoursLine(sb, "Tuesday", tuesdayField);
        appendHoursLine(sb, "Wednesday", wednesdayField);
        appendHoursLine(sb, "Thursday", thursdayField);
        appendHoursLine(sb, "Friday", fridayField);
        appendHoursLine(sb, "Saturday", saturdayField);
        appendHoursLine(sb, "Sunday", sundayField);

        return sb.toString().trim();
    }

    private void appendHoursLine(StringBuilder sb, String day, ComboBox<String> comboBox) {
        String value = text(comboBox);

        if (value.isEmpty() || value.equalsIgnoreCase("Closed")) {
            return;
        }

        if (!sb.isEmpty()) {
            sb.append("\n");
        }

        sb.append(day).append(": ").append(value);
    }

    private void setEditing(boolean editing) {
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
        insuranceCheckComboBox.setDisable(disabled);

        mondayField.setDisable(disabled);
        tuesdayField.setDisable(disabled);
        wednesdayField.setDisable(disabled);
        thursdayField.setDisable(disabled);
        fridayField.setDisable(disabled);
        saturdayField.setDisable(disabled);
        sundayField.setDisable(disabled);

        hoursArea.setDisable(true);

        visitTypeComboBox.setDisable(disabled);
        notesArea.setDisable(disabled);

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
                : "-fx-text-fill: #16a34a; -fx-font-size: 11; -fx-padding: 5 0 0 0;");
    }

    private DoctorProfileSnapshot snapshot() {
        return new DoctorProfileSnapshot(
                text(nameField), text(specialtyField), text(licenseField), text(bioArea),
                text(clinicNameField), text(addressField), text(cityField), text(stateField), text(zipField),
                text(phoneField), text(publicEmailField),
                acceptingNewPatientsCheck.isSelected(),
                selectedInsuranceInfo(),
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
        applyInsuranceSelection(s.insurance);

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

    private String cleanErrorMessage(Throwable e) {
        if (e == null) {
            return "Unknown error.";
        }

        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "Unknown error.";
        }

        return message.replace("java.lang.RuntimeException: ", "").trim();
    }

    private void applyInsuranceSelection(String insuranceInfo) {
        insuranceCheckComboBox.getCheckModel().clearChecks();

        List<String> selectedInsurances = parseInsuranceInfo(insuranceInfo);
        for (String insurance : selectedInsurances) {
            if (insuranceCheckComboBox.getItems().contains(insurance)) {
                insuranceCheckComboBox.getCheckModel().check(insurance);
            }
        }
    }

    private List<String> parseInsuranceInfo(String insuranceInfo) {
        if (insuranceInfo == null || insuranceInfo.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(insuranceInfo.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String selectedInsuranceInfo() {
        return insuranceCheckComboBox.getCheckModel().getCheckedItems().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
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
