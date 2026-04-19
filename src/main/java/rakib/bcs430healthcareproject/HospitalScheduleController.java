package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HospitalScheduleController {

    @FXML private DatePicker datePicker;
    @FXML private Label scheduleHeaderLabel;
    @FXML private VBox scheduleListVBox;

    private final FirebaseService firebaseService = new FirebaseService();
    private final UserContext userContext = UserContext.getInstance();

    private static final DateTimeFormatter DATE_HEADER_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());
        loadAppointmentsForDate(LocalDate.now());
    }

    @FXML
    private void onLoadForDate() {
        LocalDate selectedDate = datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue();
        loadAppointmentsForDate(selectedDate);
    }

    @FXML
    private void onToday() {
        datePicker.setValue(LocalDate.now());
        loadAppointmentsForDate(LocalDate.now());
    }

    private void loadAppointmentsForDate(LocalDate date) {
        try {
            HospitalProfile hospital = userContext.getHospitalProfile();
            if (hospital == null) {
                showEmpty("No hospital is currently loaded.");
                return;
            }

            List<Appointment> allAppointments = firebaseService.getAppointmentsForHospital(hospital.getUid()).get();
            List<Appointment> filtered = new ArrayList<>();

            for (Appointment appointment : allAppointments) {
                if (appointment != null && isSameDate(appointment, date)) {
                    filtered.add(appointment);
                }
            }

            scheduleHeaderLabel.setText("Appointments • " + date.format(DATE_HEADER_FORMAT));
            renderAppointments(filtered);

        } catch (Exception e) {
            System.err.println("Failed to load hospital schedule: " + e.getMessage());
            e.printStackTrace();
            showEmpty("Unable to load appointments.");
        }
    }

    private void renderAppointments(List<Appointment> appointments) {
        scheduleListVBox.getChildren().clear();

        if (appointments == null || appointments.isEmpty()) {
            showEmpty("No appointments found for this date.");
            return;
        }

        for (Appointment appointment : appointments) {
            scheduleListVBox.getChildren().add(buildAppointmentCard(appointment));
        }
    }

    private VBox buildAppointmentCard(Appointment appointment) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #D1FAE5;" +
                        "-fx-border-radius: 14;"
        );

        Label patientLabel = new Label(valueOrDefault(appointment.getPatientName(), "Unknown Patient"));
        patientLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 16; -fx-font-weight: bold;");

        Label timeLabel = new Label("Time: " + formatTime(appointment));
        timeLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");

        Label doctorLabel = new Label("Doctor: " + valueOrDefault(appointment.getDoctorName(), "Not assigned"));
        doctorLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label reasonLabel = new Label("Reason: " + valueOrDefault(appointment.getReason(), "General visit"));
        reasonLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        Label statusLabel = new Label("Status: " + valueOrDefault(appointment.getStatus(), "SCHEDULED"));
        statusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 12; -fx-font-weight: bold;");

        card.getChildren().addAll(patientLabel, timeLabel, doctorLabel, reasonLabel, statusLabel);
        return card;
    }

    private void showEmpty(String text) {
        scheduleListVBox.getChildren().clear();

        VBox card = new VBox();
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: #F8FAFC;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-radius: 12;"
        );

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
        card.getChildren().add(label);

        scheduleListVBox.getChildren().add(card);
    }

    private boolean isSameDate(Appointment appointment, LocalDate targetDate) {
        try {
            LocalDate appointmentDate = Instant.ofEpochMilli(appointment.getAppointmentDateTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return appointmentDate.equals(targetDate);
        } catch (Exception e) {
            return false;
        }
    }

    private String formatTime(Appointment appointment) {
        try {
            return Instant.ofEpochMilli(appointment.getAppointmentDateTime())
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMAT);
        } catch (Exception e) {
            return valueOrDefault(appointment.getAppointmentTime(), "Unknown");
        }
    }

    @FXML
    private void onDashboard() {
        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
    }

    @FXML
    private void onPatients() {
        SceneRouter.go("hospital-patients-view.fxml", "Hospital Patients");
    }

    @FXML
    private void onSchedule() {
        // current page
    }

    @FXML
    private void onProfile() {
        SceneRouter.go("hospital-profile-view.fxml", "Hospital Profile");
    }

    @FXML
    private void onLogout() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}