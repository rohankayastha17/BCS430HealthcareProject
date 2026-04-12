package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class DoctorScheduleController implements Initializable {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @FXML private TableView<Schedule> appointmentTable;
    @FXML private TableColumn<Schedule, String> colTime;
    @FXML private TableColumn<Schedule, String> colPatient;
    @FXML private TableColumn<Schedule, String> colType;
    @FXML private TableColumn<Schedule, String> colStatus;
    @FXML private TableColumn<Schedule, String> colNotes;

    @FXML private Button btnPrescription;
    @FXML private Button btnMessage;
    @FXML private Button btnReschedule;
    @FXML private Button btnBookSchedule;

    @FXML private TextField txtName;
    @FXML private TextField txtContact;
    @FXML private TextArea txtReason;
    @FXML private ComboBox<String> comboTime;
    @FXML private ComboBox<String> comboDuration;
    @FXML private ComboBox<String> comboType;
    @FXML private DatePicker scheduleDatePicker;

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();

    private FirebaseService firebaseService;
    private UserContext userContext;
    private Schedule editingSchedule;
    private List<Appointment> allAppointments = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        setupTable();
        setupInputs();
        setupActions();
        loadAppointments();
    }

    private void setupTable() {
        colTime.setCellValueFactory(cell -> cell.getValue().timeProperty());
        colPatient.setCellValueFactory(cell -> cell.getValue().patientNameProperty());
        colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colNotes.setCellValueFactory(cell -> cell.getValue().notesProperty());
        appointmentTable.setItems(scheduleList);
    }

    private void setupInputs() {
        scheduleDatePicker.setValue(LocalDate.now());
        comboTime.getItems().addAll(
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM",
                "11:30 AM", "01:00 PM", "01:30 PM", "02:00 PM", "02:30 PM",
                "03:00 PM", "03:30 PM", "04:00 PM", "04:30 PM"
        );
        comboDuration.getItems().addAll("15 min", "30 min", "1 hr");
        comboType.getItems().addAll("Check-up", "Consultation", "Emergency", "Follow-up");
    }

    private void setupActions() {
        btnBookSchedule.setOnAction(event -> handleBookAppointment());
    }

    private void loadAppointments() {
        firebaseService.getDoctorAppointments(userContext.getUid())
                .thenAccept(appointments -> Platform.runLater(() -> {
                    allAppointments = appointments == null ? new ArrayList<>() : new ArrayList<>(appointments);
                    allAppointments.sort(Comparator.comparing(
                            Appointment::resolveAppointmentEpochMillis,
                            Comparator.nullsLast(Long::compareTo)
                    ));
                    refreshTableForDate();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showAlert("Schedule Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    @FXML
    private void handleDateChange() {
        refreshTableForDate();
    }

    private void refreshTableForDate() {
        LocalDate selectedDate = scheduleDatePicker.getValue();
        scheduleList.clear();

        if (selectedDate == null) {
            return;
        }

        for (Appointment appointment : allAppointments) {
            if (appointment == null || appointment.getAppointmentDate() == null) {
                continue;
            }

            LocalDate appointmentDate;
            try {
                appointmentDate = LocalDate.parse(appointment.getAppointmentDate());
            } catch (Exception e) {
                continue;
            }

            if (!selectedDate.equals(appointmentDate)) {
                continue;
            }

            Schedule schedule = new Schedule(
                    formatAppointmentTime(appointment),
                    valueOrDefault(appointment.getPatientName(), "Unknown Patient"),
                    valueOrDefault(appointment.getReason(), "General"),
                    valueOrDefault(appointment.getStatus(), "SCHEDULED"),
                    valueOrDefault(appointment.getNotes(), "")
            );
            schedule.setSourceAppointment(appointment);
            scheduleList.add(schedule);
        }

        scheduleList.sort(Comparator.comparing(schedule -> parseTime(schedule.getTime()), Comparator.nullsLast(LocalTime::compareTo)));
        resetFormState();
    }

    private void handleBookAppointment() {
        if (txtName.getText().isBlank() || comboTime.getValue() == null || scheduleDatePicker.getValue() == null) {
            showAlert("Validation Error", "Please enter at least a Patient Name, Date, and Time.");
            return;
        }

        if (editingSchedule != null && editingSchedule.getSourceAppointment() != null) {
            updateExistingAppointment(editingSchedule.getSourceAppointment());
            return;
        }

        createNewAppointment();
    }

    private void updateExistingAppointment(Appointment appointment) {
        long appointmentEpoch = resolveEpochMillis(scheduleDatePicker.getValue(), comboTime.getValue());
        appointment.setAppointmentDate(scheduleDatePicker.getValue().toString());
        appointment.setAppointmentSlot(comboTime.getValue());
        appointment.setAppointmentTime(scheduleDatePicker.getValue() + " " + comboTime.getValue());
        appointment.setAppointmentDateTime(appointmentEpoch);
        appointment.setReason(valueOrDefault(comboType.getValue(), "General"));
        appointment.setNotes(txtReason.getText().trim());
        appointment.setStatus("SCHEDULED");

        firebaseService.updateAppointment(appointment)
                .thenAccept(v -> Platform.runLater(() -> {
                    firebaseService.notifyPatient(
                            appointment.getPatientUid(),
                            "Appointment Updated",
                            "Your appointment with Dr. " + appointment.getDoctorName()
                                    + " has been updated to "
                                    + appointment.getAppointmentDate() + " at " + appointment.getAppointmentSlot(),
                            "APPOINTMENT",
                            appointment.getAppointmentId()
                    );

                    loadAppointments();
                    clearForm();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Update Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    private void createNewAppointment() {
        String patientName = txtName.getText().trim();
        firebaseService.getDoctorPatients(userContext.getUid())
                .thenCompose(patients -> {
                    PatientProfile matchedPatient = patients.stream()
                            .filter(patient -> patient.getName() != null
                                    && patient.getName().trim().equalsIgnoreCase(patientName))
                            .findFirst()
                            .orElse(null);

                    if (matchedPatient == null) {
                        throw new RuntimeException("Patient must already exist in your patient list before creating a schedule entry.");
                    }

                    long appointmentEpoch = resolveEpochMillis(scheduleDatePicker.getValue(), comboTime.getValue());
                    Appointment appointment = new Appointment(
                            matchedPatient.getUid(),
                            userContext.getUid(),
                            matchedPatient.getName(),
                            userContext.getName(),
                            appointmentEpoch
                    );
                    appointment.setAppointmentDate(scheduleDatePicker.getValue().toString());
                    appointment.setAppointmentSlot(comboTime.getValue());
                    appointment.setAppointmentTime(scheduleDatePicker.getValue() + " " + comboTime.getValue());
                    appointment.setStatus("SCHEDULED");
                    appointment.setReason(valueOrDefault(comboType.getValue(), "General"));
                    appointment.setNotes(txtReason.getText().trim());
                    appointment.setNewPatient(false);

                    return firebaseService.bookAppointment(appointment)
                            .thenApply(id -> new AppointmentCreationResult(id, matchedPatient));
                })
                .thenAccept(result -> Platform.runLater(() -> {
                    firebaseService.notifyPatient(
                            result.patient.getUid(),
                            "New Appointment Scheduled",
                            "Dr. " + userContext.getName()
                                    + " scheduled an appointment for you on "
                                    + scheduleDatePicker.getValue()
                                    + " at " + comboTime.getValue(),
                            "APPOINTMENT",
                            result.appointmentId
                    );

                    loadAppointments();
                    clearForm();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Booking Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    private void clearForm() {
        txtName.clear();
        txtContact.clear();
        txtReason.clear();
        comboTime.getSelectionModel().clearSelection();
        comboDuration.getSelectionModel().clearSelection();
        comboType.getSelectionModel().clearSelection();
        resetFormState();
    }

    private void resetFormState() {
        editingSchedule = null;
        btnBookSchedule.setText("Book Schedule");
    }

    @FXML
    private void handleReschedule() {
        Schedule selectedAppt = appointmentTable.getSelectionModel().getSelectedItem();

        if (selectedAppt == null) {
            showAlert("No Selection", "Please select an appointment from the table to reschedule.");
            return;
        }

        editingSchedule = selectedAppt;
        Appointment appointment = selectedAppt.getSourceAppointment();

        txtName.setText(selectedAppt.getPatientName());
        txtContact.setText("");
        txtReason.setText(selectedAppt.getNotes());
        comboTime.setValue(selectedAppt.getTime());
        comboType.setValue(selectedAppt.getType());

        if (appointment != null && appointment.getAppointmentDate() != null) {
            try {
                scheduleDatePicker.setValue(LocalDate.parse(appointment.getAppointmentDate()));
            } catch (Exception ignored) {
                // Keep the current date if parsing fails.
            }
        }

        btnBookSchedule.setText("Update Schedule");
    }

    @FXML
    private void handleSendPrescription() {
        Schedule selectedAppt = appointmentTable.getSelectionModel().getSelectedItem();

        if (selectedAppt == null || selectedAppt.getSourceAppointment() == null) {
            showAlert("No Patient Selected", "Please select a patient from the schedule to send a prescription.");
            return;
        }

        openPatientContext(
                selectedAppt.getSourceAppointment().getPatientUid(),
                "doctor-prescription-view.fxml",
                "Send Prescription"
        );
    }

    @FXML
    private void handleMessagePatient() {
        Schedule selectedAppt = appointmentTable.getSelectionModel().getSelectedItem();

        if (selectedAppt == null || selectedAppt.getSourceAppointment() == null) {
            showAlert("No Patient Selected", "Please select a patient from the schedule to message.");
            return;
        }

        openPatientContext(
                selectedAppt.getSourceAppointment().getPatientUid(),
                "doctor-message-view.fxml",
                "Send Message"
        );
    }

    @FXML
    private void handleCancelSelected() {
        Schedule selectedAppt = appointmentTable.getSelectionModel().getSelectedItem();

        if (selectedAppt == null || selectedAppt.getSourceAppointment() == null) {
            showAlert("No Selection", "Please select an appointment to cancel.");
            return;
        }

        Appointment appointment = selectedAppt.getSourceAppointment();
        appointment.setStatus("CANCELLED");

        firebaseService.updateAppointment(appointment)
                .thenAccept(v -> Platform.runLater(() -> {
                    firebaseService.notifyPatient(
                            appointment.getPatientUid(),
                            "Appointment Cancelled",
                            "Your appointment with Dr. " + appointment.getDoctorName()
                                    + " on " + appointment.getAppointmentDate()
                                    + " at " + appointment.getAppointmentSlot()
                                    + " has been cancelled.",
                            "APPOINTMENT",
                            appointment.getAppointmentId()
                    );

                    loadAppointments();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Cancel Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    @FXML
    private void handleMarkComplete() {
        Schedule selectedAppt = appointmentTable.getSelectionModel().getSelectedItem();

        if (selectedAppt == null || selectedAppt.getSourceAppointment() == null) {
            showAlert("No Selection", "Please select an appointment to mark complete.");
            return;
        }

        Appointment appointment = selectedAppt.getSourceAppointment();
        appointment.setStatus("COMPLETED");

        firebaseService.updateAppointment(appointment)
                .thenAccept(v -> Platform.runLater(this::loadAppointments))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Update Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    @FXML
    private void onBack() {
        SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
    }

    private void openPatientContext(String patientUid, String destinationFxml, String title) {
        firebaseService.getPatientProfile(patientUid)
                .thenAccept(profile -> Platform.runLater(() -> {
                    if (profile == null) {
                        showAlert("Patient Error", "The selected patient could not be loaded.");
                        return;
                    }
                    userContext.setSelectedPatientProfile(profile);
                    userContext.setSelectedPatientUid(profile.getUid());
                    SceneRouter.go(destinationFxml, title);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Patient Error", cleanErrorMessage(e)));
                    return null;
                });
    }

    private long resolveEpochMillis(LocalDate date, String time) {
        LocalTime localTime = parseTime(time);
        LocalDateTime localDateTime = LocalDateTime.of(date, localTime);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        return LocalTime.parse(time.trim().toUpperCase(Locale.ENGLISH), TIME_FORMAT);
    }

    private String formatAppointmentTime(Appointment appointment) {
        if (appointment.getAppointmentSlot() != null && !appointment.getAppointmentSlot().isBlank()) {
            return appointment.getAppointmentSlot();
        }

        Long epoch = appointment.resolveAppointmentEpochMillis();
        if (epoch == null) {
            return "";
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault())
                .format(TIME_FORMAT);
    }

    private String cleanErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        return message == null || message.isBlank() ? "Unknown error" : message;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class AppointmentCreationResult {
        private final String appointmentId;
        private final PatientProfile patient;

        private AppointmentCreationResult(String appointmentId, PatientProfile patient) {
            this.appointmentId = appointmentId;
            this.patient = patient;
        }
    }
}