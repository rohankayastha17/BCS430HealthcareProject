package rakib.bcs430healthcareproject;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the Doctor Dashboard.
 * Displays doctor information and provides navigation to other features.
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
    @FXML private Button logoutButton;

    private Timeline clockTimeline;

    @FXML
    public void initialize() {
        startClock();

        UserContext userContext = UserContext.getInstance();

        if (userContext.isLoggedIn()) {
            String uid = userContext.getUid();

            System.out.println("Loading doctor dashboard for user: " + uid);

            // If you stored DoctorProfile in UserContext, use it:
            DoctorProfile doctorProfile = userContext.getDoctorProfile(); // add this getter if missing

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

        } else {
            welcomeLabel.setText("Welcome to Your Doctor Dashboard");
            doctorNameLabel.setText("Doctor Name: [Not loaded]");
            doctorEmailLabel.setText("Email: [Not loaded]");
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

    @FXML
    private void onPatients() {
        System.out.println("Navigating to patients list...");
        SceneRouter.go("doctor-patients-view.fxml", "My Patients");
    }

    @FXML
    private void onSchedule() {
        System.out.println("Navigating to schedule...");
        // TODO later:
        // SceneRouter.go("doctor-schedule-view.fxml", "Schedule");
    }

    @FXML
    private void onProfile() {
        System.out.println("Navigating to doctor profile...");
        SceneRouter.go("doctor-profile-view.fxml", "Doctor Profile");
    }

    @FXML
    private void onLogout() {
        System.out.println("Logging out...");
        UserContext userContext = UserContext.getInstance();
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }
}
