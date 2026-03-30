package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private FirebaseService firebaseService;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email.");
            return;
        }

        // Show loading status and disable button temporarily
        showError("Logging in...");
        
        // Authenticate user with Firebase
        firebaseService.authenticateAnyUser(email, pass)
                .thenAccept(result -> {
                    System.out.println("Authentication successful. UID=" + result.getUid() + " ROLE=" + result.getRole());

                    if ("PATIENT".equals(result.getRole())) {

                        // Load patient profile then route
                        firebaseService.getPatientProfile(result.getUid())
                                .thenAccept(profile -> Platform.runLater(() -> {
                                    UserContext.getInstance().setUserData(result.getUid(), profile);
                                    SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
                                }))
                                .exceptionally(e -> {
                                    Platform.runLater(() -> showError("Failed to load patient profile: " + e.getMessage()));
                                    return null;
                                });

                    } else {
                        // Load doctor profile then route
                        firebaseService.getDoctorProfile(result.getUid())
                                .thenAccept(doctorProfile -> Platform.runLater(() -> {
                                    UserContext.getInstance().setDoctorUserData(result.getUid(), doctorProfile);
                                    SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
                                }))
                                .exceptionally(e -> {
                                    Platform.runLater(() -> showError("Failed to load doctor profile: " + e.getMessage()));
                                    return null;
                                });
                    }
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showError(e.getMessage()));
                    return null;
                });
    }


    @FXML
    private void onGoSignup() {
        SceneRouter.go("signup-role-view.fxml", "Sign Up");
    }

    @FXML
    private void onPharmacyPortal() {
        SceneRouter.go("pharmacy-auth-view.fxml", "Pharmacy Portal");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill: #cc0000;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }
}
