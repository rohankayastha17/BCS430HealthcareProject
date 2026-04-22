package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class HospitalLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please enter email and password.", true);
            return;
        }

        showMessage("Signing in...", false);

        firebaseService.authenticateHospital(email, password)
                .thenCompose(uid -> firebaseService.getHospitalProfile(uid)
                        .thenApply(profile -> {
                            UserContext.getInstance().setHospitalUserData(uid, profile);
                            return profile;
                        }))
                .thenAccept(profile -> Platform.runLater(() ->
                        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Portal")))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showMessage(cleanErrorMessage(ex), true));
                    return null;
                });
    }

    @FXML
    private void onGoSignup() {
        SceneRouter.go("hospital-signup-view.fxml", "Hospital Sign Up");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("hospital-auth-view.fxml", "Hospital Portal");
    }

    private void showMessage(String message, boolean isError) {
        errorLabel.setText(message);
        errorLabel.setStyle(isError ? "-fx-text-fill:#DC2626;" : "-fx-text-fill:#0F766E;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private String cleanErrorMessage(Throwable throwable) {
        Throwable cause = throwable != null && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause != null && cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
    @FXML
    private void onForgotPassword() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            showMessage("Please enter your email in the email field to reset your password.", true);
            return;
        }

        showMessage("Sending reset link...", false);

        firebaseService.sendPasswordResetEmail(email)
                .thenAccept(v -> Platform.runLater(() ->
                        showMessage("Password reset email sent! Check your inbox.", false)
                ))
                .exceptionally(e -> {
                    Platform.runLater(() -> showMessage(cleanErrorMessage(e), true));
                    return null;
                });
    }
}
