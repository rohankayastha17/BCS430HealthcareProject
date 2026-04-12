package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        selectedDoctor = userContext.getSelectedDoctor();

        if (selectedDoctor == null) {
            showError("No doctor selected. Returning to search.");
            return;
        }

        setupUI();
        loadDoctorInfo();
    }

    private void setupUI() {
        timeSlotComboBox.getItems().clear();
        timeSlotComboBox.setDisable(true);

        appointmentDatePicker.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setDisable(true);
                    return;
                }

                boolean isPast = item.isBefore(LocalDate.now().plusDays(1));
                boolean doctorAvailable = isDoctorAvailableOnDate(item);

                if (isPast || !doctorAvailable) {
                    setDisable(true);
                    setStyle("-fx-background-color: #EAEAEA;");
                }
            }
        });

        appointmentDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                timeSlotComboBox.getItems().clear();
                timeSlotComboBox.setDisable(true);
                return;
            }
            updateAvailableSlots(newVal);
        });

        bookButton.setStyle("-fx-padding: 12 25; -fx-font-size: 14; -fx-background-color: #27AE60; -fx-text-fill: white;");
        cancelButton.setStyle("-fx-padding: 12 25; -fx-font-size: 14; -fx-background-color: #95A5A6; -fx-text-fill: white;");
    }

    private void loadDoctorInfo() {
        doctorNameLabel.setText(selectedDoctor.getName() != null
                ? "Dr. " + selectedDoctor.getName()
                : "Doctor");

        specialtyLabel.setText(selectedDoctor.getSpecialty() != null
                ? selectedDoctor.getSpecialty()
                : "Specialty not specified");

        clinicLabel.setText(selectedDoctor.getClinicName() != null
                ? selectedDoctor.getClinicName()
                : "Clinic not specified");
    }

    @FXML
    private void onBook() {
        LocalDate selectedDate = appointmentDatePicker.getValue();
        String selectedTime = timeSlotComboBox.getValue();
        boolean isNew = newPatientCheck.isSelected();
        String reason = reasonField.getText() != null ? reasonField.getText().trim() : "";
        String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

        if (selectedDate == null) {
            showError("Please select an appointment date.");
            return;
        }

        if (selectedTime == null || selectedTime.isBlank()) {
            showError("Please select an available appointment time.");
            return;
        }

        if (!timeSlotComboBox.getItems().contains(selectedTime)) {
            showError("That time is no longer available.");
            return;
        }

        showStatus("Booking appointment...", false);
        bookButton.setDisable(true);

        String patientUid = userContext.getUid();
        String patientName = userContext.getName();

        LocalTime time = parseTime(selectedTime);
        LocalDateTime dateTime = LocalDateTime.of(selectedDate, time);
        long timestamp = java.time.ZonedDateTime.of(
                dateTime,
                java.time.ZoneId.systemDefault()
        ).toInstant().toEpochMilli();

        Appointment appointment = new Appointment(
                patientUid,
                selectedDoctor.getUid(),
                patientName,
                selectedDoctor.getName(),
                timestamp
        );

        appointment.setAppointmentTime(selectedDate + " " + selectedTime);
        appointment.setAppointmentDate(selectedDate.toString());
        appointment.setAppointmentSlot(selectedTime);
        appointment.setNewPatient(isNew);
        appointment.setReason(reason);
        appointment.setNotes(notes);

        firebaseService.isSlotStillAvailable(
                selectedDoctor.getUid(),
                selectedDate.toString(),
                selectedTime
        ).thenCompose(isAvailable -> {
            if (!isAvailable) {
                throw new RuntimeException("This appointment slot has already been booked.");
            }
            return firebaseService.bookAppointment(appointment);
        }).thenAccept(appointmentId -> {
            Platform.runLater(() -> {
                firebaseService.notifyDoctor(
                        selectedDoctor.getUid(),
                        "New Appointment",
                        patientName + " booked an appointment for " + selectedDate + " at " + selectedTime,
                        "APPOINTMENT",
                        appointmentId
                );

                firebaseService.notifyPatient(
                        patientUid,
                        "Appointment Booked",
                        "Your appointment with Dr. " + selectedDoctor.getName()
                                + " is scheduled for " + selectedDate + " at " + selectedTime,
                        "APPOINTMENT",
                        appointmentId
                );

                showStatus("Appointment booked successfully! ID: " + appointmentId, false);
                bookButton.setDisable(false);
                onBack();
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                showError("Failed to book appointment: " + cleanErrorMessage(e));
                bookButton.setDisable(false);
                updateAvailableSlots(selectedDate);
            });
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

    private boolean isDoctorAvailableOnDate(LocalDate date) {
        if (selectedDoctor == null) {
            return false;
        }

        Map<String, String> availability = selectedDoctor.getAvailability();
        if (availability == null || availability.isEmpty()) {
            return false;
        }

        String dayName = getDayName(date);
        String value = availability.get(dayName);

        return value != null && !value.trim().isEmpty();
    }

    private void updateAvailableSlots(LocalDate date) {
        timeSlotComboBox.getItems().clear();
        timeSlotComboBox.setValue(null);
        timeSlotComboBox.setDisable(true);

        if (date == null || selectedDoctor == null) {
            return;
        }

        Map<String, String> availabilityMap = selectedDoctor.getAvailability();
        if (availabilityMap == null || availabilityMap.isEmpty()) {
            showError("This doctor has not set any availability.");
            return;
        }

        String dayName = getDayName(date);
        String availabilityString = availabilityMap.get(dayName);

        System.out.println("Selected day: " + dayName);
        System.out.println("Availability string from doctor profile: " + availabilityString);

        if (availabilityString == null || availabilityString.isBlank()) {
            showError("This doctor is not available on " + dayName + ".");
            return;
        }

        List<String> doctorSlots = generateSlotsFromAvailability(availabilityString);

        System.out.println("Generated doctor slots: " + doctorSlots);

        if (doctorSlots.isEmpty()) {
            showError("No appointment times are available for that day.");
            return;
        }

        showStatus("Loading available slots...", false);

        firebaseService.getBookedTimesForDoctorAndDate(
                selectedDoctor.getUid(),
                date.toString()
        ).thenAccept(bookedSlots -> {
            Platform.runLater(() -> {
                List<String> remainingSlots = new ArrayList<>();

                for (String slot : doctorSlots) {
                    if (!bookedSlots.contains(slot)) {
                        remainingSlots.add(slot);
                    }
                }

                timeSlotComboBox.getItems().setAll(remainingSlots);
                timeSlotComboBox.setDisable(remainingSlots.isEmpty());

                if (remainingSlots.isEmpty()) {
                    showError("No remaining slots available for that date.");
                } else {
                    showStatus("Please select an available time.", false);
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() ->
                    showError("Failed to load appointment slots: " + cleanErrorMessage(e))
            );
            return null;
        });
    }

    /**
     * Supports values like:
     * "09:00 AM-12:00 PM"
     * "09:00 AM - 12:00 PM"
     * "09:00 AM-12:00 PM, 02:00 PM-05:00 PM"
     * "9:00 AM - 5:00 PM"
     */
    private List<String> generateSlotsFromAvailability(String availabilityString) {
        List<String> result = new ArrayList<>();

        String[] ranges = availabilityString.split(",");

        for (String range : ranges) {
            String trimmedRange = range.trim();
            if (trimmedRange.isEmpty()) {
                continue;
            }

            String[] parts = trimmedRange.split("\\s*-\\s*");
            if (parts.length != 2) {
                System.out.println("Invalid availability range: " + trimmedRange);
                continue;
            }

            try {
                LocalTime start = parseTime(parts[0].trim());
                LocalTime end = parseTime(parts[1].trim());

                LocalTime current = start;

                while (current.isBefore(end)) {
                    result.add(formatTime(current));
                    current = current.plusMinutes(30);
                }
            } catch (Exception e) {
                System.out.println("Failed to parse range: " + trimmedRange);
                e.printStackTrace();
            }
        }

        return result;
    }

    private String getDayName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
    }

    private LocalTime parseTime(String timeString) {
        return LocalTime.parse(
                timeString.trim().toUpperCase(Locale.ENGLISH),
                DISPLAY_TIME_FORMAT
        );
    }

    private String formatTime(LocalTime time) {
        return time.format(DISPLAY_TIME_FORMAT).toUpperCase(Locale.ENGLISH);
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

    private String cleanErrorMessage(Throwable e) {
        if (e == null) {
            return "Unknown error.";
        }

        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "Unknown error.";
        }

        return message.replace("java.lang.RuntimeException: ", "").trim();
    }
}