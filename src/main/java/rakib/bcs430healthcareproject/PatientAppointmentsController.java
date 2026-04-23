package rakib.bcs430healthcareproject;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.util.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for displaying patient appointments.
 * Shows all appointments with doctor information and status.
 */
public class PatientAppointmentsController {

    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @FXML private VBox appointmentsListVBox;
    @FXML private Label statusLabel;
    @FXML private Button backButton;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button viewPastAppointmentsButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private List<Appointment> allAppointments = new ArrayList<>();
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        // Setup filter
        filterComboBox.getItems().addAll("All", "SCHEDULED");
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> displayAppointments(filterComboBox.getValue()));

        // Load appointments
        loadAppointments();
        startRealtimeRefresh();
    }

    private void startRealtimeRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> displayAppointments(filterComboBox.getValue())));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        appointmentsListVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && refreshTimeline != null) {
                refreshTimeline.stop();
            }
        });
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
                        allAppointments = appointments == null ? new ArrayList<>() : appointments;
                        if (allAppointments.isEmpty()) {
                            showStatus("No appointments found. Book one from the Find a Doctor section.", false);
                            appointmentsListVBox.getChildren().clear();
                        } else {
                            showStatus("Found " + allAppointments.size() + " appointment(s)", false);
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

        long now = System.currentTimeMillis();
        List<Appointment> upcomingAppointments = new ArrayList<>();

        for (Appointment apt : allAppointments) {
            if (apt == null || !matchesFilter(apt, filter)) {
                continue;
            }

            if (!isPastAppointment(apt, now)) {
                upcomingAppointments.add(apt);
            }
        }

        upcomingAppointments.sort(Comparator.comparing(
                Appointment::resolveAppointmentEpochMillis,
                Comparator.nullsLast(Long::compareTo)
        ));

        addSection("Upcoming Appointments", upcomingAppointments,
                filter.equals("All")
                        ? "No upcoming appointments found."
                        : "No upcoming appointments with status: " + filter);
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

        // Booking notes if available
        if (apt.getNotes() != null && !apt.getNotes().trim().isEmpty()) {
            Label notesLabel = new Label("Booking notes: " + apt.getNotes());
            notesLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7F8C8D; -fx-wrap-text: true;");
            card.getChildren().add(notesLabel);
        }

        if (apt.getVisitSummary() != null && !apt.getVisitSummary().trim().isEmpty()) {
            Label summaryLabel = new Label("Visit summary: " + apt.getVisitSummary());
            summaryLabel.setWrapText(true);
            summaryLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #1F2937; -fx-font-weight: bold;");
            card.getChildren().add(summaryLabel);
        }

        if (apt.getPrescribedMedications() != null && !apt.getPrescribedMedications().trim().isEmpty()) {
            Label medicationsLabel = new Label("Prescription sent: " + apt.getPrescribedMedications());
            medicationsLabel.setWrapText(true);
            medicationsLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #1F2937;");
            card.getChildren().add(medicationsLabel);
        }

        // Action buttons
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));
        buttonBox.setStyle("-fx-border-color: #BDC3C7; -fx-border-width: 1 0 0 0; -fx-padding: 8 0 0 0;");

        if ("SCHEDULED".equals(status) && !isPastAppointment(apt, System.currentTimeMillis())) {
            Button rescheduleBtn = new Button("Reschedule");
            rescheduleBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11; -fx-cursor: hand;");
            rescheduleBtn.setOnAction(e -> showRescheduleDialog(apt));
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
     * Show reschedule dialog and allow selecting a slot based on doctor's availability.
     */
    private void showRescheduleDialog(Appointment apt) {
        if (apt == null || apt.getAppointmentId() == null || apt.getDoctorUid() == null) {
            showStatus("Invalid appointment selected", true);
            return;
        }

        showStatus("Loading doctor availability...", false);

        firebaseService.getDoctorByUid(apt.getDoctorUid())
                .thenAccept(doctor -> Platform.runLater(() -> openRescheduleDialog(apt, doctor)))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Failed to load doctor availability: " + cleanErrorMessage(e), true)
                    );
                    return null;
                });
    }

    private void openRescheduleDialog(Appointment apt, Doctor doctor) {
        Map<String, String> availability = doctor != null ? doctor.getAvailability() : null;
        if (availability == null || availability.isEmpty()) {
            showStatus("Doctor availability is not set. Please try again later.", true);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reschedule Appointment");
        dialog.setHeaderText("Select a new time with Dr. " + (apt.getDoctorName() != null ? apt.getDoctorName() : "Doctor"));

        ButtonType rescheduleType = new ButtonType("Reschedule", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(rescheduleType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker();
        ComboBox<String> timeSlotCombo = new ComboBox<>();
        timeSlotCombo.setPrefWidth(220);
        timeSlotCombo.setDisable(true);

        Label dialogStatus = new Label("Select a date to see available times.");
        dialogStatus.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11;");

        datePicker.setDayCellFactory(datePickerControl -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setDisable(true);
                    return;
                }

                boolean isPast = item.isBefore(LocalDate.now().plusDays(1));
                boolean availableOnDay = isDoctorAvailableOnDate(doctor, item);

                if (isPast || !availableOnDay) {
                    setDisable(true);
                    setStyle("-fx-background-color: #EAEAEA;");
                }
            }
        });

        LocalDate currentDate = parseDate(apt.getAppointmentDate());
        if (currentDate != null && !currentDate.isBefore(LocalDate.now().plusDays(1))
                && isDoctorAvailableOnDate(doctor, currentDate)) {
            datePicker.setValue(currentDate);
        }

        datePicker.valueProperty().addListener((obs, oldVal, newVal) ->
                updateRescheduleSlots(doctor, apt, newVal, timeSlotCombo, dialogStatus)
        );

        Node confirmButton = dialog.getDialogPane().lookupButton(rescheduleType);
        confirmButton.setDisable(true);

        Runnable refreshConfirm = () -> {
            LocalDate selectedDate = datePicker.getValue();
            String selectedSlot = timeSlotCombo.getValue();
            confirmButton.setDisable(selectedDate == null || selectedSlot == null || selectedSlot.isBlank());
        };

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshConfirm.run());
        timeSlotCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshConfirm.run());

        VBox content = new VBox(10);
        content.setPadding(new Insets(5));
        content.getChildren().addAll(
                new Label("New date:"),
                datePicker,
                new Label("New time:"),
                timeSlotCombo,
                dialogStatus
        );

        dialog.getDialogPane().setContent(content);

        if (datePicker.getValue() != null) {
            updateRescheduleSlots(doctor, apt, datePicker.getValue(), timeSlotCombo, dialogStatus);
        }

        ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);
        if (result != rescheduleType) {
            showStatus("Reschedule cancelled", false);
            return;
        }

        LocalDate selectedDate = datePicker.getValue();
        String selectedSlot = timeSlotCombo.getValue();

        if (selectedDate == null || selectedSlot == null || selectedSlot.isBlank()) {
            showStatus("Please choose a valid date and time.", true);
            return;
        }

        if (selectedDate.toString().equals(apt.getAppointmentDate())
                && selectedSlot.equalsIgnoreCase(apt.getAppointmentSlot())) {
            showStatus("Selected date/time is the same as your current appointment.", false);
            return;
        }

        rescheduleAppointment(apt, selectedDate, selectedSlot);
    }

    private void updateRescheduleSlots(Doctor doctor,
                                       Appointment apt,
                                       LocalDate selectedDate,
                                       ComboBox<String> timeSlotCombo,
                                       Label dialogStatus) {
        timeSlotCombo.getItems().clear();
        timeSlotCombo.setValue(null);
        timeSlotCombo.setDisable(true);

        if (selectedDate == null) {
            dialogStatus.setText("Select a date to see available times.");
            dialogStatus.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11;");
            return;
        }

        String dayName = getDayName(selectedDate);
        String dayAvailability = doctor.getAvailability().get(dayName);

        if (dayAvailability == null || dayAvailability.isBlank()) {
            dialogStatus.setText("Doctor is not available on " + dayName + ".");
            dialogStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11;");
            return;
        }

        List<String> doctorSlots = generateSlotsFromAvailability(dayAvailability);
        if (doctorSlots.isEmpty()) {
            dialogStatus.setText("No valid time ranges are configured for this day.");
            dialogStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11;");
            return;
        }

        dialogStatus.setText("Loading available times...");
        dialogStatus.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11;");

        firebaseService.getBookedTimesForDoctorAndDate(doctor.getUid(), selectedDate.toString())
                .thenAccept(bookedSlots -> Platform.runLater(() -> {
                    List<String> remainingSlots = new ArrayList<>();

                    for (String slot : doctorSlots) {
                        if (!bookedSlots.contains(slot)) {
                            remainingSlots.add(slot);
                        }
                    }

                    // Keep the currently booked slot selectable when rescheduling on the same date.
                    if (selectedDate.toString().equals(apt.getAppointmentDate())
                            && apt.getAppointmentSlot() != null
                            && doctorSlots.contains(apt.getAppointmentSlot())
                            && !remainingSlots.contains(apt.getAppointmentSlot())) {
                        remainingSlots.add(apt.getAppointmentSlot());
                    }

                    remainingSlots.sort((a, b) -> parseTime(a).compareTo(parseTime(b)));

                    timeSlotCombo.getItems().setAll(remainingSlots);
                    timeSlotCombo.setDisable(remainingSlots.isEmpty());

                    if (remainingSlots.isEmpty()) {
                        dialogStatus.setText("No available slots on this date.");
                        dialogStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11;");
                    } else {
                        dialogStatus.setText("Select one of the available times.");
                        dialogStatus.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11;");

                        if (selectedDate.toString().equals(apt.getAppointmentDate())
                                && apt.getAppointmentSlot() != null
                                && remainingSlots.contains(apt.getAppointmentSlot())) {
                            timeSlotCombo.setValue(apt.getAppointmentSlot());
                        }
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        dialogStatus.setText("Failed to load slots: " + cleanErrorMessage(e));
                        dialogStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11;");
                    });
                    return null;
                });
    }

    private void rescheduleAppointment(Appointment apt, LocalDate newDate, String newSlot) {
        if (apt == null || apt.getAppointmentId() == null) {
            showStatus("Invalid appointment", true);
            return;
        }

        showStatus("Rescheduling appointment...", false);

        firebaseService.isSlotStillAvailable(apt.getDoctorUid(), newDate.toString(), newSlot)
                .thenCompose(isAvailable -> {
                    if (!isAvailable) {
                        throw new RuntimeException("That time was just booked. Please choose another slot.");
                    }

                    LocalTime localTime = parseTime(newSlot);
                    LocalDateTime localDateTime = LocalDateTime.of(newDate, localTime);
                    long newTimestamp = ZonedDateTime.of(localDateTime, ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();

                    apt.setAppointmentDate(newDate.toString());
                    apt.setAppointmentSlot(newSlot);
                    apt.setAppointmentTime(newDate + " " + newSlot);
                    apt.setAppointmentDateTime(newTimestamp);

                    return firebaseService.updateAppointment(apt);
                })
                .thenAccept(v -> Platform.runLater(() -> {
                    showStatus("Appointment rescheduled successfully", false);
                    loadAppointments();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Failed to reschedule appointment: " + cleanErrorMessage(e), true)
                    );
                    return null;
                });
    }

    private boolean isDoctorAvailableOnDate(Doctor doctor, LocalDate date) {
        if (doctor == null || date == null) {
            return false;
        }

        Map<String, String> availability = doctor.getAvailability();
        if (availability == null || availability.isEmpty()) {
            return false;
        }

        String value = availability.get(getDayName(date));
        return value != null && !value.trim().isEmpty();
    }

    private List<String> generateSlotsFromAvailability(String availabilityString) {
        List<String> result = new ArrayList<>();
        if (availabilityString == null || availabilityString.isBlank()) {
            return result;
        }

        String[] ranges = availabilityString.split(",");
        for (String range : ranges) {
            String[] parts = range.trim().split("\\s*-\\s*");
            if (parts.length != 2) {
                continue;
            }

            try {
                LocalTime start = parseTime(parts[0]);
                LocalTime end = parseTime(parts[1]);

                LocalTime current = start;
                while (current.isBefore(end)) {
                    result.add(formatTime(current));
                    current = current.plusMinutes(30);
                }
            } catch (Exception ignored) {
                // Ignore invalid ranges and continue with valid configured ranges.
            }
        }

        return result;
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(String timeString) {
        return LocalTime.parse(timeString.trim().toUpperCase(Locale.ENGLISH), DISPLAY_TIME_FORMAT);
    }

    private String formatTime(LocalTime time) {
        return time.format(DISPLAY_TIME_FORMAT).toUpperCase(Locale.ENGLISH);
    }

    private String getDayName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
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
            firebaseService.deleteAppointment(apt.getAppointmentId())
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

    private boolean matchesFilter(Appointment appointment, String filter) {
        String status = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase(Locale.ENGLISH);
        if ("CANCELLED".equals(status)) {
            return false;
        }
        return "All".equals(filter) || status.equalsIgnoreCase(filter);
    }

    private boolean isPastAppointment(Appointment appointment, long now) {
        String status = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase(Locale.ENGLISH);
        return "COMPLETED".equals(status) || appointment.hasPassed(now);
    }

    private void addSection(String title, List<Appointment> appointments, String emptyMessage) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-padding: 0 0 8 0;");
        appointmentsListVBox.getChildren().add(titleLabel);

        if (appointments.isEmpty()) {
            Label emptyLabel = new Label(emptyMessage);
            emptyLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13; -fx-padding: 0 0 12 0;");
            appointmentsListVBox.getChildren().add(emptyLabel);
            return;
        }

        for (Appointment appointment : appointments) {
            appointmentsListVBox.getChildren().add(createAppointmentCard(appointment));
        }
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

    @FXML
    private void onViewPastAppointments() {
        SceneRouter.go("patient-past-appointments-view.fxml", "Past Appointments");
    }
}
