package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Controller for booking appointments with doctors.
 */
public class BookAppointmentController {

    @FXML private Label doctorNameLabel;
    @FXML private Label specialtyLabel;
    @FXML private Label clinicLabel;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private ComboBox<String> timeSlotComboBox;
    @FXML private CheckBox newPatientCheck;
    @FXML private TextField reasonField;
    @FXML private TextArea notesArea;
    @FXML private Button bookButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private Button backButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private Doctor selectedDoctor;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();
        
        // Get selected doctor from context
        selectedDoctor = userContext.getSelectedDoctor();
        
        if (selectedDoctor == null) {
            showError("No doctor selected. Returning to search.");
            return;
        }
        
        setupUI();
        loadDoctorInfo();
    }

    private void setupUI() {
        // Set minimum appointment date to tomorrow
        appointmentDatePicker.setDayCellFactory(dateCell -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        // Setup time slots (will be filtered later based on availability)
        timeSlotComboBox.getItems().addAll(
                "09:00 AM",
                "09:30 AM",
                "10:00 AM",
                "10:30 AM",
                "11:00 AM",
                "11:30 AM",
                "02:00 PM",
                "02:30 PM",
                "03:00 PM",
                "03:30 PM",
                "04:00 PM",
                "04:30 PM"
        );

        // listener to update slots when date changed
        appointmentDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableSlots(newVal));

        // Setup buttons
        bookButton.setStyle("-fx-padding: 12 25; -fx-font-size: 14; -fx-background-color: #27AE60; -fx-text-fill: white;");
        cancelButton.setStyle("-fx-padding: 12 25; -fx-font-size: 14; -fx-background-color: #95A5A6; -fx-text-fill: white;");
    }

    private void loadDoctorInfo() {
        doctorNameLabel.setText(selectedDoctor.getName() != null ? 
                "Dr. " + selectedDoctor.getName() : "Doctor");
        specialtyLabel.setText(selectedDoctor.getSpecialty() != null ? 
                selectedDoctor.getSpecialty() : "Specialty not specified");
        clinicLabel.setText(selectedDoctor.getClinicName() != null ? 
                selectedDoctor.getClinicName() : "Clinic not specified");
    }

    @FXML
    private void onBook() {
        // Validate inputs
        LocalDate selectedDate = appointmentDatePicker.getValue();
        String selectedTime = timeSlotComboBox.getValue();
        boolean isNew = newPatientCheck.isSelected();
        String reason = reasonField.getText() != null ? reasonField.getText().trim() : "";
        String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

        if (selectedDate == null) {
            showError("Please select an appointment date.");
            return;
        }

        if (selectedTime == null || selectedTime.isEmpty()) {
            showError("Please select an appointment time.");
            return;
        }

        showStatus("Booking appointment...", false);

        // Create appointment
        String patientUid = userContext.getUid();
        String patientName = userContext.getName();
        
        // Convert date and time to timestamp
        LocalTime time = parseTime(selectedTime);
        LocalDateTime dateTime = LocalDateTime.of(selectedDate, time);
        long timestamp = java.time.ZonedDateTime.of(dateTime, java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        Appointment appointment = new Appointment(
                patientUid,
                selectedDoctor.getUid(),
                patientName,
                selectedDoctor.getName(),
                timestamp
        );
        appointment.setAppointmentTime(selectedDate + " " + selectedTime);
        appointment.setNewPatient(isNew);
        appointment.setReason(reason);
        appointment.setNotes(notes);

        // Save to Firebase
        firebaseService.bookAppointment(appointment)
                .thenAccept(appointmentId -> {
                    javafx.application.Platform.runLater(() -> {
                        showStatus("Appointment booked successfully! ID: " + appointmentId, false);
                        // Navigate back after 2 seconds
                        javafx.application.Platform.runLater(() -> {
                            try {
                                Thread.sleep(2000);
                                onBack();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    });
                })
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() ->
                            showError("Failed to book appointment: " + e.getMessage())
                    );
                    return null;
                });
    }

    @FXML
    private void onCancel() {
        onBack();
    }

    @FXML
    private void onBack() {
        userContext.clearSelectedDoctor();
        SceneRouter.go("doctor-search-view.fxml", "Find a Doctor");
    }

    private LocalTime parseTime(String timeString) {
        String[] parts = timeString.split(" ");
        String[] timeParts = parts[0].split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Adjust hour for PM
        if (parts.length > 1 && parts[1].equals("PM") && hour != 12) {
            hour += 12;
        } else if (parts.length > 1 && parts[1].equals("AM") && hour == 12) {
            hour = 0;
        }

        return LocalTime.of(hour, minute);
    }

    /**
     * Filter the time slots based on the doctor's availability map.
     */
    private void updateAvailableSlots(java.time.LocalDate date) {
        if (date == null || selectedDoctor == null) return;
        String dow = date.getDayOfWeek().toString().substring(0,1).toUpperCase()
                + date.getDayOfWeek().toString().substring(1).toLowerCase();
        timeSlotComboBox.getItems().clear();
        java.util.List<String> slots = java.util.List.of(
                "09:00 AM","09:30 AM","10:00 AM","10:30 AM","11:00 AM","11:30 AM",
                "02:00 PM","02:30 PM","03:00 PM","03:30 PM","04:00 PM","04:30 PM"
        );
        java.util.Map<String,String> avail = selectedDoctor.getAvailability();
        if (avail != null && avail.containsKey(dow) && !avail.get(dow).isEmpty()) {
            // simple parsing: assume "start - end" in same format as slots
            String range = avail.get(dow);
            String[] parts = range.split("-\\s*");
            if (parts.length == 2) {
                java.time.LocalTime start = parseTime(parts[0].trim());
                java.time.LocalTime end = parseTime(parts[1].trim());
                for (String slot : slots) {
                    LocalTime t = parseTime(slot);
                    if (!t.isBefore(start) && !t.isAfter(end)) {
                        timeSlotComboBox.getItems().add(slot);
                    }
                }
                return;
            }
        }
        // fallback to all slots if availability not set or parse failed
        timeSlotComboBox.getItems().addAll(slots);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #E74C3C;"
                : "-fx-text-fill: #27AE60;");
        statusLabel.setVisible(true);
    }

    private void showError(String message) {
        showStatus(message, true);
    }
}
