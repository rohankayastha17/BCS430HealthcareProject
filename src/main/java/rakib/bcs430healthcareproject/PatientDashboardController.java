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
    @FXML private Button profileButton;
    @FXML private Button notificationButton;
    @FXML private Button logoutButton;

    @FXML private Button messageFabButton;

    private Timeline clockTimeline;
    private Timeline unreadCheckTimeline;

    private FirebaseService firebaseService;
    private UserContext userContext;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        startClock();
        setupNotificationBellAnimation();
        startUnreadMessageChecker();

        if (userContext.isLoggedIn()) {
            PatientProfile profile = userContext.getProfile();
            String uid = userContext.getUid();

            if (profile != null) {
                String displayName = profile.getName() != null ? profile.getName() : "Patient";
                welcomeLabel.setText("Welcome Back, " + displayName);
                patientNameLabel.setText("Patient Name: " + displayName);
                patientEmailLabel.setText("Email: " + profile.getEmail());
            }
        }
    }

    private void startUnreadMessageChecker() {
        unreadCheckTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> checkUnreadMessages())
        );
        unreadCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        unreadCheckTimeline.play();
    }

    private void checkUnreadMessages() {
        String uid = userContext.getUid();

        firebaseService.hasUnreadMessages(uid, "PATIENT")
                .thenAccept(hasUnread -> {
                    javafx.application.Platform.runLater(() -> {
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
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private void startClock() {
        updateClockLabel();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClockLabel()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
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
    private void handleOpenMessages() {
        SceneRouter.go("patient-message-view.fxml", "Messages");
    }


    @FXML
    private void onAppointments() {
        SceneRouter.go("patient-appointments-view.fxml", "My Appointments");
    }

    @FXML
    private void onPrescriptions() {
        SceneRouter.go("patient-prescriptions-view.fxml", "My Prescriptions");
    }

    @FXML
    private void onFindDoctor() {
        SceneRouter.go("doctor-search-view.fxml", "Find a Doctor");
    }

    @FXML
    private void onProfile() {
        SceneRouter.go("patient-profile-view.fxml", "My Profile");
    }

    @FXML
    private void onNotifications() {
        SceneRouter.go("patient-notifications-view.fxml", "Notifications");
    }

    @FXML
    private void onLogout() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }
}