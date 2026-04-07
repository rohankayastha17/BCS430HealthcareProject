package rakib.bcs430healthcareproject;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the Doctor Dashboard.
 */
public class DoctorDashboardController {

    private static final DateTimeFormatter DATE_TIME_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy - h:mm:ss a");

    @FXML private Label welcomeLabel;
    @FXML private Label currentDateTimeLabel;
    @FXML private Label doctorNameLabel;
    @FXML private Label doctorEmailLabel;

    @FXML private Button patientsButton;
    @FXML private Button scheduleButton;
    @FXML private Button profileButton;
    @FXML private Button notificationButton;
    @FXML private Button logoutButton;

    // 🔥 NEW FAB BUTTON
    @FXML private Button messageFabButton;

    private Timeline clockTimeline;

    // 🔥 NEW polling for unread
    private Timeline unreadPolling;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();

        startClock();
        setupNotificationBellAnimation();

        UserContext userContext = UserContext.getInstance();

        if (userContext.isLoggedIn()) {
            String uid = userContext.getUid();

            System.out.println("Loading doctor dashboard for user: " + uid);

            DoctorProfile doctorProfile = userContext.getDoctorProfile();

            if (doctorProfile != null) {
                String displayName = doctorProfile.getName() != null ? doctorProfile.getName() : "Doctor";
                welcomeLabel.setText("Welcome Back, " + displayName);
                doctorNameLabel.setText("Doctor Name: " + displayName);
                doctorEmailLabel.setText("Email: " + doctorProfile.getEmail());
            } else {
                welcomeLabel.setText("Welcome to Your Doctor Dashboard");
                doctorNameLabel.setText("Doctor Name: [Not loaded]");
                doctorEmailLabel.setText("Email: [Not loaded]");
            }

            // 🔥 START unread checker
            startUnreadPolling(uid);

        } else {
            welcomeLabel.setText("Welcome to Your Doctor Dashboard");
            doctorNameLabel.setText("Doctor Name: [Not loaded]");
            doctorEmailLabel.setText("Email: [Not loaded]");
        }
    }

    // =========================================================
    // CLOCK
    // =========================================================

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

    // =========================================================
    // 🔥 UNREAD POLLING SYSTEM
    // =========================================================

    private void startUnreadPolling(String uid) {
        unreadPolling = new Timeline(new KeyFrame(Duration.seconds(3), e -> checkUnread(uid)));
        unreadPolling.setCycleCount(Timeline.INDEFINITE);
        unreadPolling.play();
    }

    private void checkUnread(String uid) {
        firebaseService.hasUnreadMessages(uid, "DOCTOR")
                .thenAccept(hasUnread -> Platform.runLater(() -> updateFabIndicator(hasUnread)))
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private void updateFabIndicator(boolean hasUnread) {
        if (messageFabButton == null) return;

        if (hasUnread) {
            messageFabButton.getStyleClass().remove("fab-message");
            if (!messageFabButton.getStyleClass().contains("fab-message-unread")) {
                messageFabButton.getStyleClass().add("fab-message-unread");
            }
        } else {
            messageFabButton.getStyleClass().remove("fab-message-unread");
            if (!messageFabButton.getStyleClass().contains("fab-message")) {
                messageFabButton.getStyleClass().add("fab-message");
            }
        }
    }

    // =========================================================
    // 🔥 NAVIGATION TO MESSAGE SCREEN
    // =========================================================

    @FXML
    private void handleOpenMessages() {
        System.out.println("Opening doctor messages...");
        SceneRouter.go("doctor-message-view.fxml", "Messages");
    }

    // =========================================================
    // OTHER BUTTONS
    // =========================================================

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
    private void onPatients() {
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }

    @FXML
    private void onSchedule() {
        SceneRouter.go("doctor-schedule-view.fxml", "Doctor Schedule");
    }

    @FXML
    private void onProfile() {
        SceneRouter.go("doctor-profile-view.fxml", "Doctor Profile");
    }

    @FXML
    private void onNotifications() {
        try {
            SceneRouter.go("doctor-notifications-view.fxml", "Notifications");
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open Notifications");
            alert.setContentText("An error occurred while loading doctor notifications. " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onLogout() {
        UserContext userContext = UserContext.getInstance();
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }
}
