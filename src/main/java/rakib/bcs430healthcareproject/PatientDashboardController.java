package rakib.bcs430healthcareproject;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the Patient Dashboard.
 * Displays patient information and provides navigation to other features.
 */
public class PatientDashboardController {

    private static final DateTimeFormatter DATE_TIME_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy - h:mm:ss a");

    @FXML private Label welcomeLabel;
    @FXML private Label currentDateTimeLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label patientEmailLabel;

    @FXML private Button appointmentsButton;
    @FXML private Button findDoctorButton;
    @FXML private Button prescriptionsButton;
    @FXML private Button messageButton;
    @FXML private Button profileButton;
    @FXML private Button notificationButton;
    @FXML private Button logoutButton;

    private Timeline clockTimeline;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        startClock();
        setupNotificationBellAnimation();

        // Load current user data from session
        UserContext userContext = UserContext.getInstance();

        if (userContext.isLoggedIn()) {
            PatientProfile profile = userContext.getProfile();
            String uid = userContext.getUid();

            System.out.println("Loading dashboard for user: " + uid);

            if (profile != null) {
                String displayName = profile.getName() != null ? profile.getName() : "Patient";
                welcomeLabel.setText("Welcome Back, " + displayName);
                patientNameLabel.setText("Patient Name: " + displayName);
                patientEmailLabel.setText("Email: " + profile.getEmail());
            } else {
                welcomeLabel.setText("Welcome to Your Patient Dashboard");
                patientNameLabel.setText("Patient Name: [Not loaded]");
                patientEmailLabel.setText("Email: [Not loaded]");
            }
        } else {
            welcomeLabel.setText("Welcome to Your Patient Dashboard");
            patientNameLabel.setText("Patient Name: [Not loaded]");
            patientEmailLabel.setText("Email: [Not loaded]");
        }
    }

    private void startClock() {
        updateClockLabel();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClockLabel()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        currentDateTimeLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && clockTimeline != null) {
                clockTimeline.stop();
            }
        });
    }

    private void updateClockLabel() {
        currentDateTimeLabel.setText("Today: " + LocalDateTime.now().format(DATE_TIME_DISPLAY_FORMAT));
    }

    private void setupNotificationBellAnimation() {
        if (notificationButton != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(800), notificationButton);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.12);
            pulse.setToY(1.12);
            pulse.setCycleCount(2);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    @FXML
    private void onAppointments() {
        System.out.println("Navigating to appointments...");
        SceneRouter.go("patient-appointments-view.fxml", "My Appointments");
    }

    @FXML
    private void onPrescriptions() {
        System.out.println("Navigating to prescriptions...");
        SceneRouter.go("patient-prescriptions-view.fxml", "My Prescriptions");
    }

    @FXML
    private void onFindDoctor() {
        System.out.println("Navigating to find doctor...");
        java.net.URL res = SceneRouter.class.getResource("doctor-search-view.fxml");
        System.out.println("doctor-search-view.fxml resource: " + res);
        try {
            SceneRouter.go("doctor-search-view.fxml", "Find a Doctor");
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open Find a Doctor");
            alert.setContentText("An error occurred while loading the doctor search. " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onMessage() {
        System.out.println("Navigating to messages...");
        SceneRouter.go("patient-message-view.fxml", "Messages");
    }

    @FXML
    private void onProfile() {
        System.out.println("Navigating to profile...");
        SceneRouter.go("patient-profile-view.fxml", "My Profile");
    }

    @FXML
    private void onNotifications() {
        System.out.println("Navigating to notifications...");
        try {
            SceneRouter.go("patient-notifications-view.fxml", "Notifications");
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open Notifications");
            alert.setContentText("An error occurred while loading notifications. " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onLogout() {
        System.out.println("Logging out...");
        UserContext userContext = UserContext.getInstance();
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }
}