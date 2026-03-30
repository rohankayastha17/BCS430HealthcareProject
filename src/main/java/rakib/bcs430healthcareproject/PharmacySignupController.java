package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PharmacySignupController {

    @FXML private TextField pharmacyNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField zipField;
    @FXML private Label errorLabel;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    @FXML
    private void onCreatePharmacyAccount() {
        String pharmacyName = safe(pharmacyNameField);
        String email = safe(emailField).toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String phone = safe(phoneField);
        String address = safe(addressField);
        String city = safe(cityField);
        String state = safe(stateField).toUpperCase();
        String zip = safe(zipField);

        if (pharmacyName.isEmpty() || email.isEmpty() || password.isEmpty()
                || confirmPassword.isEmpty() || phone.isEmpty() || address.isEmpty()
                || city.isEmpty() || state.isEmpty() || zip.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }
        if (!zip.matches("\\d{5}")) {
            showError("ZIP code must be 5 digits.");
            return;
        }
        if (state.length() != 2) {
            showError("State must be a 2-letter abbreviation.");
            return;
        }

        showInfo("Creating pharmacy account and claiming location...");

        firebaseService.createPharmacy(email, password, pharmacyName, phone, address, city, state, zip)
                .thenAccept(uid -> Platform.runLater(() -> {
                    showSuccess("Pharmacy account created. Please sign in to access prescriptions.");
                    SceneRouter.go("pharmacy-login-view.fxml", "Pharmacy Sign In");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(cleanErrorMessage(ex)));
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("pharmacy-auth-view.fxml", "Pharmacy Portal");
    }

    private String safe(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill:#DC2626; -fx-font-size:12;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void showInfo(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill:#0F766E; -fx-font-size:12;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill:#15803D; -fx-font-size:12;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private String cleanErrorMessage(Throwable throwable) {
        Throwable cause = throwable != null && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause != null && cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
}
