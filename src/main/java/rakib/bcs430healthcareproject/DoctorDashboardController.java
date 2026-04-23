package rakib.bcs430healthcareproject;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    @FXML private Label todayAppointmentsStatusLabel;
    @FXML private VBox todayAppointmentsVBox;

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
            loadTodayAppointments(uid);
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
        notificationPolling = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            loadNotificationCount(uid);
            loadTodayAppointments(uid);
        }));
        notificationPolling.setCycleCount(Timeline.INDEFINITE);
        notificationPolling.play();
    }

    private void loadTodayAppointments(String doctorUid) {
        firebaseService.getDoctorAppointmentsForDate(doctorUid, LocalDate.now())
                .thenAccept(appointments -> Platform.runLater(() -> renderTodayAppointments(appointments)))
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        if (todayAppointmentsStatusLabel != null) {
                            todayAppointmentsStatusLabel.setText("Unable to load today's appointments.");
                        }
                    });
                    return null;
                });
    }

    private void renderTodayAppointments(List<Appointment> appointments) {
        if (todayAppointmentsVBox == null) {
            return;
        }

        todayAppointmentsVBox.getChildren().clear();
        List<Appointment> todaysAppointments = new ArrayList<>();

        if (appointments != null) {
            for (Appointment appointment : appointments) {
                if (appointment == null) {
                    continue;
                }

                String status = normalizedStatus(appointment.getStatus());
                if ("CANCELLED".equals(status)) {
                    continue;
                }

                todaysAppointments.add(appointment);
            }
        }

        todaysAppointments.sort(Comparator.comparing(
                Appointment::resolveAppointmentEpochMillis,
                Comparator.nullsLast(Long::compareTo)
        ));

        if (todaysAppointments.isEmpty()) {
            todayAppointmentsVBox.getChildren().add(buildEmptyAppointmentCard());
            todayAppointmentsStatusLabel.setText("No appointments scheduled for today.");
            return;
        }

        int previewCount = Math.min(5, todaysAppointments.size());
        for (int index = 0; index < previewCount; index++) {
            todayAppointmentsVBox.getChildren().add(buildTodayAppointmentCard(todaysAppointments.get(index)));
        }

        todayAppointmentsStatusLabel.setText("Showing " + previewCount + " appointment(s) for today.");
    }

    private VBox buildTodayAppointmentCard(Appointment appointment) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #D1FAE5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 14;");

        Label patientLabel = new Label(valueOrDefault(appointment.getPatientName(), "Unknown Patient"));
        patientLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 14; -fx-font-weight: bold;");

        Label timeLabel = new Label("Time: " + formatAppointmentTime(appointment));
        timeLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");

        Label reasonLabel = new Label("Reason: " + valueOrDefault(appointment.getReason(), "General visit"));
        reasonLabel.setWrapText(true);
        reasonLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label statusLabel = new Label(valueOrDefault(appointment.getStatus(), "SCHEDULED"));
        statusLabel.setStyle(getStatusBadgeStyle(appointment.getStatus()));

        card.getChildren().addAll(patientLabel, timeLabel, reasonLabel, statusLabel);
        return card;
    }

    private VBox buildEmptyAppointmentCard() {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 14;");

        Label title = new Label("No appointments today");
        title.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 14; -fx-font-weight: bold;");

        Label subtitle = new Label("Today's schedule is clear.");
        subtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        card.getChildren().addAll(title, subtitle);
        return card;
    }

    private String formatAppointmentTime(Appointment appointment) {
        Long epoch = appointment == null ? null : appointment.resolveAppointmentEpochMillis();
        if (epoch == null) {
            return "Time unavailable";
        }

        return Instant.ofEpochMilli(epoch)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
    }

    private String getStatusBadgeStyle(String status) {
        String normalized = normalizedStatus(status);
        return switch (normalized) {
            case "COMPLETED" -> "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 4 10;";
            default -> "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 4 10;";
        };
    }

    private String normalizedStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ENGLISH);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
