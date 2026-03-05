package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for displaying patient appointments.
 * Shows all appointments with doctor information and status.
 */
public class PatientAppointmentsController {

    @FXML private VBox appointmentsListVBox;
    @FXML private Label statusLabel;
    @FXML private Button backButton;
    @FXML private ComboBox<String> filterComboBox;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private List<Appointment> allAppointments = new ArrayList<>();

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        // Setup filter
        filterComboBox.getItems().addAll("All", "SCHEDULED", "COMPLETED", "CANCELLED");
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> displayAppointments(filterComboBox.getValue()));

        // Load appointments
        loadAppointments();
    }

    /**
     * Load patient's appointments from Firebase
     */
    private void loadAppointments() {
        if (!userContext.isLoggedIn()) {
            showStatus("Not logged in", true);
            return;
        }

        showStatus("Loading appointments...", false);
        String patientUid = userContext.getUid();

        firebaseService.getPatientAppointments(patientUid)
                .thenAccept(appointments -> {
                    Platform.runLater(() -> {
                        allAppointments = appointments;
                        if (appointments.isEmpty()) {
                            showStatus("No appointments found. Book one from the Find a Doctor section.", false);
                            appointmentsListVBox.getChildren().clear();
                        } else {
                            showStatus("Found " + appointments.size() + " appointment(s)", false);
                            displayAppointments(filterComboBox.getValue());
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Error loading appointments: " + e.getMessage(), true)
                    );
                    return null;
                });
    }

    /**
     * Display appointments filtered by status
     */
    private void displayAppointments(String filter) {
        appointmentsListVBox.getChildren().clear();

        List<Appointment> toDisplay = new ArrayList<>();
        for (Appointment apt : allAppointments) {
            if (filter.equals("All") || apt.getStatus().equals(filter)) {
                toDisplay.add(apt);
            }
        }

        if (toDisplay.isEmpty()) {
            Label emptyLabel = new Label("No appointments with status: " + filter);
            emptyLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13;");
            appointmentsListVBox.getChildren().add(emptyLabel);
            return;
        }

        for (Appointment apt : toDisplay) {
            appointmentsListVBox.getChildren().add(createAppointmentCard(apt));
        }
    }

    /**
     * Create a card for single appointment
     */
    private VBox createAppointmentCard(Appointment apt) {
        VBox card = new VBox();
        card.setStyle(
                "-fx-border-color: #BDC3C7; -fx-border-radius: 5; -fx-padding: 10; " +
                "-fx-background-color: #ECEFF1; -fx-spacing: 8; -fx-margin: 0 0 10 0;"
        );

        // Doctor info
        HBox doctorRow = new HBox();
        doctorRow.setSpacing(10);
        Label doctorLabel = new Label("Dr. " + (apt.getDoctorName() != null ? apt.getDoctorName() : "Unknown"));
        doctorLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        doctorRow.getChildren().add(doctorLabel);

        // Status badge
        String status = apt.getStatus() != null ? apt.getStatus() : "PENDING";
        Label statusBadge = new Label(status);
        statusBadge.setStyle(getStatusStyle(status));
        statusBadge.setPadding(new Insets(3, 8, 3, 8));
        doctorRow.getChildren().add(statusBadge);
        HBox.setHgrow(statusBadge, Priority.ALWAYS);

        card.getChildren().add(doctorRow);

        // Date and time
        String dateTimeStr = formatDateTime(apt.getAppointmentDateTime());
        Label dateTimeLabel = new Label("📅 " + dateTimeStr);
        dateTimeLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495E;");
        card.getChildren().add(dateTimeLabel);

        // New patient indicator
        if (Boolean.TRUE.equals(apt.getNewPatient())) {
            Label newLabel = new Label("New patient");
            newLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #E67E22; -fx-font-weight: bold;");
            card.getChildren().add(newLabel);
        }

        // Reason for visit
        if (apt.getReason() != null && !apt.getReason().trim().isEmpty()) {
            Label reasonLabel = new Label("Reason: " + apt.getReason());
            reasonLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7F8C8D; -fx-wrap-text: true;");
            card.getChildren().add(reasonLabel);
        }

        // Notes if available
        if (apt.getNotes() != null && !apt.getNotes().trim().isEmpty()) {
            Label notesLabel = new Label("Notes: " + apt.getNotes());
            notesLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7F8C8D; -fx-wrap-text: true;");
            card.getChildren().add(notesLabel);
        }

        // Action buttons
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));
        buttonBox.setStyle("-fx-border-color: #BDC3C7; -fx-border-width: 1 0 0 0; -fx-padding: 8 0 0 0;");

        if ("SCHEDULED".equals(status)) {
            Button rescheduleBtn = new Button("Reschedule");
            rescheduleBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11; -fx-cursor: hand;");
            rescheduleBtn.setOnAction(e -> showStatus("Reschedule feature coming soon", false));
            buttonBox.getChildren().add(rescheduleBtn);

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11; -fx-background-color: #E74C3C; -fx-text-fill: white; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> cancelAppointment(apt));
            buttonBox.getChildren().add(cancelBtn);
        }

        if (!buttonBox.getChildren().isEmpty()) {
            card.getChildren().add(buttonBox);
        }

        return card;
    }

    /**
     * Cancel an appointment
     */
    private void cancelAppointment(Appointment apt) {
        if (apt == null || apt.getAppointmentId() == null) {
            showStatus("Invalid appointment", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Appointment");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Cancel appointment with Dr. " + (apt.getDoctorName() != null ? apt.getDoctorName() : "Doctor") + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            apt.setStatus("CANCELLED");
            firebaseService.updateAppointment(apt)
                    .thenAccept(v -> {
                        Platform.runLater(() -> {
                            showStatus("Appointment cancelled", false);
                            loadAppointments();
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() ->
                                showStatus("Error cancelling appointment: " + e.getMessage(), true)
                        );
                        return null;
                    });
        }
    }

    /**
     * Format Unix timestamp to readable date-time string
     */
    private String formatDateTime(Long timestamp) {
        if (timestamp == null) return "Unknown";
        try {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
            return formatter.format(dateTime);
        } catch (Exception e) {
            return "Invalid date";
        }
    }

    /**
     * Get CSS style for status badge
     */
    private String getStatusStyle(String status) {
        return switch (status) {
            case "SCHEDULED" -> "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;";
            case "COMPLETED" -> "-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;";
            case "CANCELLED" -> "-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;";
            default -> "-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;";
        };
    }

    /**
     * Display status message
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #E74C3C; -fx-font-size: 12;"
                : "-fx-text-fill: #27AE60; -fx-font-size: 12;"
        );
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Dashboard");
    }
}
