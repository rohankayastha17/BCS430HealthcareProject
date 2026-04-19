package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HospitalDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label hospitalInfoLabel;

    @FXML private Label totalPatientsLabel;
    @FXML private Label appointmentsTodayLabel;
    @FXML private Label departmentsLabel;

    @FXML private VBox patientsListVBox;
    @FXML private VBox scheduleListVBox;

    private UserContext userContext;
    private FirebaseService firebaseService;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");

    @FXML
    public void initialize() {
        userContext = UserContext.getInstance();
        firebaseService = new FirebaseService();

        HospitalProfile profile = userContext.getHospitalProfile();

        if (profile != null) {
            String hospitalName = valueOrDefault(profile.getHospitalName(), "Hospital");
            String fullAddress = valueOrDefault(profile.getFullAddress(), "Manage patients and appointments.");

            setLabelText(welcomeLabel, "Welcome, " + hospitalName);
            setLabelText(hospitalInfoLabel, fullAddress);

            loadDashboardData(profile.getUid());
        } else {
            setLabelText(welcomeLabel, "Welcome, Hospital");
            setLabelText(hospitalInfoLabel, "Manage patients and appointments.");
            loadEmptyState();
        }
    }

    private void loadDashboardData(String hospitalUid) {
        try {
            List<PatientProfile> patients = firebaseService.getPatientsForHospital(hospitalUid).get();
            List<Appointment> appointments = firebaseService.getAppointmentsForHospital(hospitalUid).get();

            updateStats(patients, appointments);
            loadPatientsPreview(patients);
            loadSchedulePreview(appointments);

        } catch (Exception e) {
            System.err.println("Failed to load hospital dashboard data: " + e.getMessage());
            e.printStackTrace();
            loadEmptyState();
        }
    }

    private void updateStats(List<PatientProfile> patients, List<Appointment> appointments) {
        setLabelText(totalPatientsLabel, String.valueOf(patients != null ? patients.size() : 0));

        int todayCount = 0;
        if (appointments != null) {
            for (Appointment appointment : appointments) {
                if (appointment != null && isToday(appointment)) {
                    todayCount++;
                }
            }
        }

        setLabelText(appointmentsTodayLabel, String.valueOf(todayCount));

        // You removed departments support, so keep this simple for now
        setLabelText(departmentsLabel, "—");
    }

    private void loadPatientsPreview(List<PatientProfile> patients) {
        if (patientsListVBox == null) {
            return;
        }

        patientsListVBox.getChildren().clear();

        if (patients == null || patients.isEmpty()) {
            patientsListVBox.getChildren().add(buildEmptyCard("No patients found yet."));
            return;
        }

        int limit = Math.min(5, patients.size());
        for (int i = 0; i < limit; i++) {
            PatientProfile patient = patients.get(i);
            patientsListVBox.getChildren().add(buildPatientCard(patient));
        }
    }

    private void loadSchedulePreview(List<Appointment> appointments) {
        if (scheduleListVBox == null) {
            return;
        }

        scheduleListVBox.getChildren().clear();

        if (appointments == null || appointments.isEmpty()) {
            scheduleListVBox.getChildren().add(buildEmptyCard("No appointments scheduled."));
            return;
        }

        int shown = 0;
        for (Appointment appointment : appointments) {
            if (appointment == null) {
                continue;
            }

            scheduleListVBox.getChildren().add(buildAppointmentCard(appointment));
            shown++;

            if (shown >= 5) {
                break;
            }
        }
    }

    private VBox buildPatientCard(PatientProfile patient) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: #F8FAFC;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #D1FAE5;" +
                        "-fx-border-radius: 12;"
        );

        Label nameLabel = new Label(valueOrDefault(patient.getName(), "Unnamed Patient"));
        nameLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 14; -fx-font-weight: bold;");

        Label emailLabel = new Label(valueOrDefault(patient.getEmail(), "No email"));
        emailLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label phoneLabel = new Label(valueOrDefault(patient.getPhoneNumber(), "No phone"));
        phoneLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        card.getChildren().addAll(nameLabel, emailLabel, phoneLabel);
        return card;
    }

    private VBox buildAppointmentCard(Appointment appointment) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: #ECFDF5;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #A7F3D0;" +
                        "-fx-border-radius: 12;"
        );

        String patientName = valueOrDefault(appointment.getPatientName(), "Unknown Patient");
        String status = valueOrDefault(appointment.getStatus(), "SCHEDULED");
        String timeText = formatAppointmentTime(appointment);

        Label patientLabel = new Label(patientName);
        patientLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 14; -fx-font-weight: bold;");

        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");

        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 11; -fx-font-weight: bold;");

        card.getChildren().addAll(patientLabel, timeLabel, statusLabel);
        return card;
    }

    private VBox buildEmptyCard(String text) {
        VBox box = new VBox();
        box.setPadding(new Insets(14));
        box.setStyle(
                "-fx-background-color: #F8FAFC;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-radius: 12;"
        );

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        box.getChildren().add(label);
        return box;
    }

    private String formatAppointmentTime(Appointment appointment) {
        try {
            Long millis = appointment.getAppointmentDateTime();
            if (millis == null) {
                return valueOrDefault(appointment.getAppointmentTime(), "Time not available");
            }

            return Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMAT);
        } catch (Exception e) {
            return valueOrDefault(appointment.getAppointmentTime(), "Time not available");
        }
    }

    private boolean isToday(Appointment appointment) {
        try {
            Long millis = appointment.getAppointmentDateTime();
            if (millis == null) {
                return false;
            }

            LocalDate appointmentDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            return appointmentDate.equals(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void loadEmptyState() {
        setLabelText(totalPatientsLabel, "0");
        setLabelText(appointmentsTodayLabel, "0");
        setLabelText(departmentsLabel, "—");

        if (patientsListVBox != null) {
            patientsListVBox.getChildren().clear();
            patientsListVBox.getChildren().add(buildEmptyCard("No patients found yet."));
        }

        if (scheduleListVBox != null) {
            scheduleListVBox.getChildren().clear();
            scheduleListVBox.getChildren().add(buildEmptyCard("No appointments scheduled."));
        }
    }

    @FXML
    private void onPatients() {
        SceneRouter.go("hospital-patients-view.fxml", "Hospital Patients");
    }

    @FXML
    private void onSchedule() {
        SceneRouter.go("hospital-schedule-view.fxml", "Hospital Schedule");
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

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}