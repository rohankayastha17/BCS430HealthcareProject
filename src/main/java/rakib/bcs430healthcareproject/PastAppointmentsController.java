package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PastAppointmentsController {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a", Locale.ENGLISH);

    @FXML private Button backButton;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ScrollPane pastAppointmentsScrollPane;
    @FXML private VBox appointmentsListVBox;

    private final FirebaseService firebaseService = new FirebaseService();
    private final UserContext userContext = UserContext.getInstance();
    private List<Appointment> pastAppointments = new ArrayList<>();

    @FXML
    public void initialize() {
        sortComboBox.getItems().addAll("Newest to Oldest", "Oldest to Newest");
        sortComboBox.setValue("Newest to Oldest");
        sortComboBox.setOnAction(event -> renderPastAppointments());
        loadAppointments();
    }

    private void loadAppointments() {
        String patientUid = resolveTargetPatientUid();
        if (patientUid == null || patientUid.isBlank()) {
            showStatus("No patient selected.", true);
            appointmentsListVBox.getChildren().clear();
            return;
        }

        titleLabel.setText(resolveTitle());
        showStatus("Loading past appointments...", false);

        firebaseService.getPatientAppointments(patientUid)
                .thenAccept(appointments -> Platform.runLater(() -> {
                    long now = System.currentTimeMillis();
                    pastAppointments = new ArrayList<>();

                    if (appointments != null) {
                        for (Appointment appointment : appointments) {
                            if (appointment == null) {
                                continue;
                            }
                            String status = normalizedStatus(appointment);
                            if ("CANCELLED".equals(status)) {
                                continue;
                            }
                            if ("COMPLETED".equals(status) || appointment.hasPassed(now)) {
                                pastAppointments.add(appointment);
                            }
                        }
                    }

                    renderPastAppointments();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Failed to load past appointments: " + cleanErrorMessage(ex), true));
                    return null;
                });
    }

    private void renderPastAppointments() {
        appointmentsListVBox.getChildren().clear();

        Comparator<Appointment> comparator = Comparator.comparing(
                Appointment::resolveAppointmentEpochMillis,
                Comparator.nullsLast(Long::compareTo)
        );
        if ("Newest to Oldest".equals(sortComboBox.getValue())) {
            comparator = comparator.reversed();
        }
        pastAppointments.sort(comparator);

        if (pastAppointments.isEmpty()) {
            Label empty = new Label("No past appointments found.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13;");
            appointmentsListVBox.getChildren().add(empty);
            showStatus("No past appointments found.", false);
            return;
        }

        showStatus("Found " + pastAppointments.size() + " past appointment(s).", false);
        for (Appointment appointment : pastAppointments) {
            appointmentsListVBox.getChildren().add(buildAppointmentCard(appointment));
        }
    }

    private VBox buildAppointmentCard(Appointment appointment) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #D1D5DB; -fx-border-radius: 12;");

        HBox topRow = new HBox(10);
        Label doctorLabel = new Label("Dr. " + valueOrDefault(appointment.getDoctorName(), "Unknown"));
        doctorLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 15; -fx-font-weight: bold;");

        Label statusBadge = new Label(valueOrDefault(appointment.getStatus(), "COMPLETED"));
        statusBadge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 4 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(doctorLabel, spacer, statusBadge);

        Label timeLabel = new Label("Date: " + formatTimestamp(appointment.resolveAppointmentEpochMillis()));
        timeLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");

        Label reasonLabel = new Label("Reason: " + valueOrDefault(appointment.getReason(), "General visit"));
        reasonLabel.setWrapText(true);
        reasonLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        card.getChildren().addAll(topRow, timeLabel, reasonLabel);

        if (appointment.getVisitSummary() != null && !appointment.getVisitSummary().isBlank()) {
            Label summaryLabel = new Label("Visit summary: " + appointment.getVisitSummary());
            summaryLabel.setWrapText(true);
            summaryLabel.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(summaryLabel);
        }

        if (appointment.getPrescribedMedications() != null && !appointment.getPrescribedMedications().isBlank()) {
            Label medicationLabel = new Label("Prescription sent: " + appointment.getPrescribedMedications());
            medicationLabel.setWrapText(true);
            medicationLabel.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 12;");
            card.getChildren().add(medicationLabel);
        }

        if (appointment.getNotes() != null && !appointment.getNotes().isBlank()) {
            Label notesLabel = new Label("Booking notes: " + appointment.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11;");
            card.getChildren().add(notesLabel);
        }

        return card;
    }

    @FXML
    private void onBack() {
        if (userContext.isDoctor()) {
            SceneRouter.go("patient-profile-view.fxml", "Patient Profile");
            return;
        }
        SceneRouter.go("patient-appointments-view.fxml", "My Appointments");
    }

    private String resolveTargetPatientUid() {
        if (userContext.isDoctor()) {
            if (userContext.getSelectedPatientUid() != null && !userContext.getSelectedPatientUid().isBlank()) {
                return userContext.getSelectedPatientUid();
            }
            PatientProfile selectedProfile = userContext.getSelectedPatientProfile();
            return selectedProfile != null ? selectedProfile.getUid() : null;
        }
        return userContext.getUid();
    }

    private String resolveTitle() {
        if (userContext.isDoctor()) {
            PatientProfile selectedProfile = userContext.getSelectedPatientProfile();
            String patientName = selectedProfile != null ? selectedProfile.getName() : "Patient";
            return patientName + " Past Appointments";
        }
        return "My Past Appointments";
    }

    private String normalizedStatus(Appointment appointment) {
        if (appointment == null || appointment.getStatus() == null) {
            return "";
        }
        return appointment.getStatus().trim().toUpperCase(Locale.ENGLISH);
    }

    private String formatTimestamp(Long epochMillis) {
        if (epochMillis == null) {
            return "Unknown";
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                    .format(DISPLAY_DATE_FORMAT);
        } catch (Exception ex) {
            return "Unknown";
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #DC2626; -fx-font-size: 12;"
                : "-fx-text-fill: #0F766E; -fx-font-size: 12;");
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null || cause.getMessage().isBlank()
                ? "Unknown error"
                : cause.getMessage();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
