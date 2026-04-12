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

    // Notification badge label near bell
    @FXML private Label notificationCountLabel;

    // Message floating action button
    @FXML private Button messageFabButton;

    private Timeline clockTimeline;
    private Timeline unreadPolling;
    private Timeline notificationPolling;

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

            loadNotificationCount(uid);
            startUnreadPolling(uid);
            startNotificationPolling(uid);

        } else {
            welcomeLabel.setText("Welcome to Your Doctor Dashboard");
            doctorNameLabel.setText("Doctor Name: [Not loaded]");
            doctorEmailLabel.setText("Email: [Not loaded]");
            hideNotificationBadge();
        }

        if (currentDateTimeLabel != null) {
            currentDateTimeLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    stopTimelines();
                }
            });
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
    }

    private void updateClockLabel() {
        currentDateTimeLabel.setText("Today: " + LocalDateTime.now().format(DATE_TIME_DISPLAY_FORMAT));
    }

    // =========================================================
    // MESSAGE UNREAD POLLING
    // =========================================================

    private void startUnreadPolling(String uid) {
        unreadPolling = new Timeline(new KeyFrame(Duration.seconds(3), e -> checkUnreadMessages(uid)));
        unreadPolling.setCycleCount(Timeline.INDEFINITE);
        unreadPolling.play();
    }

    private void checkUnreadMessages(String uid) {
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
    // NOTIFICATION BELL SYSTEM
    // =========================================================

    private void startNotificationPolling(String uid) {
        notificationPolling = new Timeline(new KeyFrame(Duration.seconds(5), e -> loadNotificationCount(uid)));
        notificationPolling.setCycleCount(Timeline.INDEFINITE);
        notificationPolling.play();
    }

    private void loadNotificationCount(String uid) {
        CompletableFutureRunner.runAsync(() -> firebaseService.getUnreadNotificationCount(uid),
                unreadCount -> Platform.runLater(() -> updateNotificationBadge(unreadCount)),
                error -> {
                    error.printStackTrace();
                    Platform.runLater(this::hideNotificationBadge);
                });
    }

    private void updateNotificationBadge(int unreadCount) {
        if (notificationCountLabel == null) return;

        if (unreadCount > 0) {
            notificationCountLabel.setText(String.valueOf(unreadCount));
            notificationCountLabel.setVisible(true);
            notificationCountLabel.setManaged(true);
        } else {
            hideNotificationBadge();
        }
    }

    private void hideNotificationBadge() {
        if (notificationCountLabel == null) return;

        notificationCountLabel.setText("");
        notificationCountLabel.setVisible(false);
        notificationCountLabel.setManaged(false);
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    @FXML
    private void handleOpenMessages() {
        System.out.println("Opening doctor messages...");
        SceneRouter.go("doctor-message-view.fxml", "Messages");
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
            SceneRouter.go("notifications-view.fxml", "Notifications");
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
        stopTimelines();
        UserContext userContext = UserContext.getInstance();
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }

    // =========================================================
    // UI EFFECTS
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

    // =========================================================
    // CLEANUP
    // =========================================================

    private void stopTimelines() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        if (unreadPolling != null) {
            unreadPolling.stop();
        }
        if (notificationPolling != null) {
            notificationPolling.stop();
        }
    }

    // =========================================================
    // SMALL INTERNAL ASYNC HELPER
    // =========================================================

    private static class CompletableFutureRunner {
        public static <T> void runAsync(
                java.util.concurrent.Callable<T> task,
                java.util.function.Consumer<T> onSuccess,
                java.util.function.Consumer<Throwable> onError
        ) {
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(onSuccess).exceptionally(error -> {
                onError.accept(error);
                return null;
            });
        }
    }
}