package rakib.bcs430healthcareproject;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PatientDashboardController {

    private static final DateTimeFormatter DATE_TIME_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy - h:mm:ss a");

    @FXML private Label welcomeLabel;
    @FXML private Label currentDateTimeLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label patientEmailLabel;
    @FXML private VBox upcomingAppointmentsPreviewVBox;
    @FXML private Label upcomingAppointmentsStatusLabel;

    @FXML private Button appointmentsButton;
    @FXML private Button findDoctorButton;
    @FXML private Button findHospitalButton;
    @FXML private Button prescriptionsButton;
    @FXML private Button profileButton;
    @FXML private Button notificationButton;
    @FXML private Button logoutButton;

    @FXML private Button messageFabButton;

    // 🔴 NEW: Notification badge
    @FXML private Label notificationCountLabel;

    private Timeline clockTimeline;
    private Timeline unreadCheckTimeline;
    private Timeline notificationPolling;

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

            // 🔔 LOAD NOTIFICATION COUNT
            loadNotificationCount(uid);
            loadUpcomingAppointments(uid);

            // 🔄 START POLLING
            startNotificationPolling(uid);
        } else {
            hideNotificationBadge();
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
    // MESSAGE CHECKER
    // =========================================================

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
                .thenAccept(hasUnread -> Platform.runLater(() -> {
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
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    // =========================================================
    // 🔔 NOTIFICATION SYSTEM
    // =========================================================

    private void startNotificationPolling(String uid) {
        notificationPolling = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> {
                    loadNotificationCount(uid);
                    loadUpcomingAppointments(uid);
                })
        );
        notificationPolling.setCycleCount(Timeline.INDEFINITE);
        notificationPolling.play();
    }

    private void loadUpcomingAppointments(String uid) {
        firebaseService.getPatientAppointments(uid)
                .thenAccept(appointments -> Platform.runLater(() -> renderUpcomingAppointments(appointments)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (upcomingAppointmentsStatusLabel != null) {
                            upcomingAppointmentsStatusLabel.setText("Unable to load upcoming appointments.");
                        }
                    });
                    return null;
                });
    }

    private void renderUpcomingAppointments(List<Appointment> appointments) {
        if (upcomingAppointmentsPreviewVBox == null) {
            return;
        }

        upcomingAppointmentsPreviewVBox.getChildren().clear();
        List<Appointment> upcomingAppointments = new ArrayList<>();
        long now = System.currentTimeMillis();

        if (appointments != null) {
            for (Appointment appointment : appointments) {
                if (appointment == null) {
                    continue;
                }
                String status = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase(Locale.ENGLISH);
                if ("CANCELLED".equals(status) || "COMPLETED".equals(status) || appointment.hasPassed(now)) {
                    continue;
                }
                upcomingAppointments.add(appointment);
            }
        }

        upcomingAppointments.sort(Comparator.comparing(
                Appointment::resolveAppointmentEpochMillis,
                Comparator.nullsLast(Long::compareTo)
        ));

        if (upcomingAppointments.isEmpty()) {
            upcomingAppointmentsPreviewVBox.getChildren().add(buildEmptyAppointmentCard());
            upcomingAppointmentsStatusLabel.setText("No upcoming appointments scheduled.");
            return;
        }

        int previewCount = Math.min(3, upcomingAppointments.size());
        for (int index = 0; index < previewCount; index++) {
            upcomingAppointmentsPreviewVBox.getChildren().add(buildUpcomingAppointmentCard(upcomingAppointments.get(index)));
        }

        upcomingAppointmentsStatusLabel.setText("Showing " + previewCount + " upcoming appointment(s).");
    }

    private VBox buildUpcomingAppointmentCard(Appointment appointment) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #D1FAE5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 14;");

        HBox topRow = new HBox(10);
        Label doctorLabel = new Label("Dr. " + valueOrDefault(appointment.getDoctorName(), "Unknown"));
        doctorLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 14; -fx-font-weight: bold;");

        Label statusBadge = new Label(valueOrDefault(appointment.getStatus(), "SCHEDULED"));
        statusBadge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 4 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(doctorLabel, spacer, statusBadge);

        Label timeLabel = new Label("When: " + formatAppointmentDateTime(appointment));
        timeLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");

        Label reasonLabel = new Label("Reason: " + valueOrDefault(appointment.getReason(), "General visit"));
        reasonLabel.setWrapText(true);
        reasonLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        card.getChildren().addAll(topRow, timeLabel, reasonLabel);
        return card;
    }

    private VBox buildEmptyAppointmentCard() {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 14;");

        Label title = new Label("No upcoming appointments");
        title.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 14; -fx-font-weight: bold;");

        Label subtitle = new Label("Book your next visit from Find a Doctor.");
        subtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        card.getChildren().addAll(title, subtitle);
        return card;
    }

    private String formatAppointmentDateTime(Appointment appointment) {
        Long epoch = appointment == null ? null : appointment.resolveAppointmentEpochMillis();
        if (epoch == null) {
            return "Time unavailable";
        }

        return Instant.ofEpochMilli(epoch)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a", Locale.ENGLISH));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void loadNotificationCount(String uid) {
        new Thread(() -> {
            try {
                int unread = firebaseService.getUnreadNotificationCount(uid);

                Platform.runLater(() -> updateNotificationBadge(unread));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(this::hideNotificationBadge);
            }
        }).start();
    }

    private void updateNotificationBadge(int count) {
        if (notificationCountLabel == null) return;

        if (count > 0) {
            notificationCountLabel.setText(String.valueOf(count));
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
    private void onFindHospital() {
        SceneRouter.go("hospital-search-view.fxml", "Find a Hospital");
    }

    @FXML
    private void onProfile() {
        SceneRouter.go("patient-profile-view.fxml", "My Profile");
    }

    // 🔥 FIXED
    @FXML
    private void onNotifications() {
        SceneRouter.go("notifications-view.fxml", "Notifications");
    }

    @FXML
    private void onLogout() {
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
}
