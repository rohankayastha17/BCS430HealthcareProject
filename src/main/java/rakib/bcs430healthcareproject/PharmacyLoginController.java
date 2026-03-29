package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PharmacyLoginController {

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

        firebaseService.authenticatePharmacy(email, password)
                .thenCompose(uid -> firebaseService.getPharmacyProfile(uid)
                        .thenApply(profile -> {
                            UserContext.getInstance().setPharmacyUserData(uid, profile);
                            return profile;
                        }))
                .thenAccept(profile -> Platform.runLater(() ->
                        SceneRouter.go("pharmacy-prescriptions-view.fxml", "Pharmacy Portal")))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showMessage(cleanErrorMessage(ex), true));
                    return null;
                });
    }

    @FXML
    private void onGoSignup() {
        SceneRouter.go("pharmacy-signup-view.fxml", "Pharmacy Sign Up");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("pharmacy-auth-view.fxml", "Pharmacy Portal");
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
}
