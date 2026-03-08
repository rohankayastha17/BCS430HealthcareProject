package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DoctorSignupController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField specialtyField;
    @FXML private TextField clinicNameField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField zipField;
    @FXML private CheckBox acceptingNewPatientsCheck;
    @FXML private Label errorLabel;

    @FXML private ComboBox<String> mondayStartCombo;
    @FXML private ComboBox<String> mondayEndCombo;
    @FXML private CheckBox mondayClosedCheck;

    @FXML private ComboBox<String> tuesdayStartCombo;
    @FXML private ComboBox<String> tuesdayEndCombo;
    @FXML private CheckBox tuesdayClosedCheck;

    @FXML private ComboBox<String> wednesdayStartCombo;
    @FXML private ComboBox<String> wednesdayEndCombo;
    @FXML private CheckBox wednesdayClosedCheck;

    @FXML private ComboBox<String> thursdayStartCombo;
    @FXML private ComboBox<String> thursdayEndCombo;
    @FXML private CheckBox thursdayClosedCheck;

    @FXML private ComboBox<String> fridayStartCombo;
    @FXML private ComboBox<String> fridayEndCombo;
    @FXML private CheckBox fridayClosedCheck;

    @FXML private ComboBox<String> saturdayStartCombo;
    @FXML private ComboBox<String> saturdayEndCombo;
    @FXML private CheckBox saturdayClosedCheck;

    @FXML private ComboBox<String> sundayStartCombo;
    @FXML private ComboBox<String> sundayEndCombo;
    @FXML private CheckBox sundayClosedCheck;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        setupTimeCombos();
        setupClosedDayBehavior();
    }

    private void setupTimeCombos() {
        List<String> timeOptions = List.of(
                "8:00 AM", "8:30 AM",
                "9:00 AM", "9:30 AM",
                "10:00 AM", "10:30 AM",
                "11:00 AM", "11:30 AM",
                "12:00 PM", "12:30 PM",
                "1:00 PM", "1:30 PM",
                "2:00 PM", "2:30 PM",
                "3:00 PM", "3:30 PM",
                "4:00 PM", "4:30 PM",
                "5:00 PM", "5:30 PM",
                "6:00 PM", "6:30 PM",
                "7:00 PM", "7:30 PM"
        );

        for (ComboBox<String> combo : List.of(
                mondayStartCombo, mondayEndCombo,
                tuesdayStartCombo, tuesdayEndCombo,
                wednesdayStartCombo, wednesdayEndCombo,
                thursdayStartCombo, thursdayEndCombo,
                fridayStartCombo, fridayEndCombo,
                saturdayStartCombo, saturdayEndCombo,
                sundayStartCombo, sundayEndCombo
        )) {
            combo.setItems(FXCollections.observableArrayList(timeOptions));
        }

        setDefaultDay(mondayStartCombo, mondayEndCombo, false);
        setDefaultDay(tuesdayStartCombo, tuesdayEndCombo, false);
        setDefaultDay(wednesdayStartCombo, wednesdayEndCombo, false);
        setDefaultDay(thursdayStartCombo, thursdayEndCombo, false);
        setDefaultDay(fridayStartCombo, fridayEndCombo, false);
        setDefaultDay(saturdayStartCombo, saturdayEndCombo, true);
        setDefaultDay(sundayStartCombo, sundayEndCombo, true);
    }

    private void setDefaultDay(ComboBox<String> startCombo, ComboBox<String> endCombo, boolean closed) {
        startCombo.setValue("9:00 AM");
        endCombo.setValue("5:00 PM");
        startCombo.setDisable(closed);
        endCombo.setDisable(closed);
    }

    private void setupClosedDayBehavior() {
        bindClosedCheck(mondayClosedCheck, mondayStartCombo, mondayEndCombo, false);
        bindClosedCheck(tuesdayClosedCheck, tuesdayStartCombo, tuesdayEndCombo, false);
        bindClosedCheck(wednesdayClosedCheck, wednesdayStartCombo, wednesdayEndCombo, false);
        bindClosedCheck(thursdayClosedCheck, thursdayStartCombo, thursdayEndCombo, false);
        bindClosedCheck(fridayClosedCheck, fridayStartCombo, fridayEndCombo, false);
        bindClosedCheck(saturdayClosedCheck, saturdayStartCombo, saturdayEndCombo, true);
        bindClosedCheck(sundayClosedCheck, sundayStartCombo, sundayEndCombo, true);
    }

    private void bindClosedCheck(CheckBox closedCheck, ComboBox<String> startCombo, ComboBox<String> endCombo, boolean selectedByDefault) {
        closedCheck.setSelected(selectedByDefault);
        startCombo.setDisable(selectedByDefault);
        endCombo.setDisable(selectedByDefault);

        closedCheck.selectedProperty().addListener((obs, oldVal, isClosed) -> {
            startCombo.setDisable(isClosed);
            endCombo.setDisable(isClosed);
        });
    }

    @FXML
    private void onCreateDoctorAccount() {
        hideError();

        String name = nameField.getText().trim();
        String email = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String specialty = specialtyField.getText().trim();
        String clinicName = clinicNameField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String state = stateField.getText().trim();
        String zip = zipField.getText().trim();
        boolean acceptingNewPatients = acceptingNewPatientsCheck.isSelected();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
                || specialty.isEmpty() || clinicName.isEmpty() || address.isEmpty()
                || city.isEmpty() || state.isEmpty() || zip.isEmpty()) {
            showError("Please fill in all required fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!zip.matches("\\d{5}")) {
            showError("ZIP code must be 5 digits.");
            return;
        }

        Map<String, String> availability;
        try {
            availability = buildAvailabilityMap();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
            return;
        }

        firebaseService.createDoctor(
                email,
                password,
                name,
                specialty,
                clinicName,
                address,
                city,
                state,
                zip,
                acceptingNewPatients,
                availability
        ).thenAccept(uid -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Doctor account created successfully.");
            alert.showAndWait();

            SceneRouter.go("login-view.fxml", "Login");
        })).exceptionally(ex -> {
            Platform.runLater(() -> showError(
                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()
            ));
            return null;
        });
    }

    private Map<String, String> buildAvailabilityMap() {
        Map<String, String> availability = new LinkedHashMap<>();

        availability.put("Monday", buildDayAvailability("Monday", mondayStartCombo, mondayEndCombo, mondayClosedCheck));
        availability.put("Tuesday", buildDayAvailability("Tuesday", tuesdayStartCombo, tuesdayEndCombo, tuesdayClosedCheck));
        availability.put("Wednesday", buildDayAvailability("Wednesday", wednesdayStartCombo, wednesdayEndCombo, wednesdayClosedCheck));
        availability.put("Thursday", buildDayAvailability("Thursday", thursdayStartCombo, thursdayEndCombo, thursdayClosedCheck));
        availability.put("Friday", buildDayAvailability("Friday", fridayStartCombo, fridayEndCombo, fridayClosedCheck));
        availability.put("Saturday", buildDayAvailability("Saturday", saturdayStartCombo, saturdayEndCombo, saturdayClosedCheck));
        availability.put("Sunday", buildDayAvailability("Sunday", sundayStartCombo, sundayEndCombo, sundayClosedCheck));

        return availability;
    }

    private String buildDayAvailability(String dayName,
                                        ComboBox<String> startCombo,
                                        ComboBox<String> endCombo,
                                        CheckBox closedCheck) {
        if (closedCheck.isSelected()) {
            return "Closed";
        }

        String start = startCombo.getValue();
        String end = endCombo.getValue();

        if (start == null || end == null) {
            throw new RuntimeException("Please select start and end time for " + dayName + ".");
        }

        if (start.equals(end)) {
            throw new RuntimeException(dayName + " start and end time cannot be the same.");
        }

        return start + " - " + end;
    }

    @FXML
    private void onBack() {
        SceneRouter.go("login-view.fxml", "Login");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }
}