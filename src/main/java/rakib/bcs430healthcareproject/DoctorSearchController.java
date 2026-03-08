package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for searching and filtering doctors.
 * Allows patients to find doctors by specialty and ZIP code,
 * view their profiles, and book appointments.
 */
public class DoctorSearchController {

    @FXML private TextField nameSearchField;
    @FXML private TextField specialtySearchField;
    @FXML private TextField zipSearchField;
    @FXML private Button searchButton;
    @FXML private Button clearFiltersButton;
    @FXML private VBox doctorListVBox;
    @FXML private Label statusLabel;
    @FXML private Button backButton;

    private FirebaseService firebaseService;
    private List<Doctor> allDoctors = new ArrayList<>();
    private List<Doctor> filteredDoctors = new ArrayList<>();

    private static final LocalTime DEFAULT_OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSE_TIME = LocalTime.of(17, 0);
    private static final int APPOINTMENT_SLOT_MINUTES = 30;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        setupUI();
        loadDoctors();
    }

    private void setupUI() {
        nameSearchField.setPromptText("Doctor name...");
        specialtySearchField.setPromptText("e.g., Cardiology, Family Medicine...");
        zipSearchField.setPromptText("e.g., 11735");

        nameSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        specialtySearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        zipSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        searchButton.setStyle(
                "-fx-padding: 10 20; -fx-font-size: 13; -fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand;"
        );
        clearFiltersButton.setStyle(
                "-fx-padding: 10 20; -fx-font-size: 13; -fx-background-color: #95A5A6; -fx-text-fill: white; -fx-cursor: hand;"
        );
    }

    /**
     * Load all doctors from Firebase.
     */
    private void loadDoctors() {
        showStatus("Loading doctors...", false);

        firebaseService.getAllDoctors()
                .thenAccept(doctors -> Platform.runLater(() -> {
                    allDoctors = doctors;
                    filteredDoctors = new ArrayList<>(doctors);

                    if (doctors.isEmpty()) {
                        showStatus("No doctors found.", false);
                    } else {
                        showStatus("Found " + doctors.size() + " doctors", false);
                        displayDoctors(filteredDoctors);
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showStatus("Error loading doctors: " + e.getMessage(), true)
                    );
                    return null;
                });
    }

    @FXML
    private void onSearch() {
        applyFilters();
    }

    @FXML
    private void onClearFilters() {
        nameSearchField.clear();
        specialtySearchField.clear();
        zipSearchField.clear();
        filteredDoctors = new ArrayList<>(allDoctors);
        displayDoctors(filteredDoctors);
        showStatus("Filters cleared. Showing " + filteredDoctors.size() + " doctors", false);
    }

    /**
     * Apply specialty and ZIP code filters.
     */
    private void applyFilters() {
        String name = nameSearchField.getText().trim().toLowerCase();
        String specialty = specialtySearchField.getText().trim().toLowerCase();
        String zip = zipSearchField.getText().trim();

        filteredDoctors = new ArrayList<>();

        for (Doctor doctor : allDoctors) {
            boolean matchesName = name.isEmpty() ||
                    (doctor.getName() != null && doctor.getName().toLowerCase().contains(name));

            boolean matchesSpecialty = specialty.isEmpty() ||
                    (doctor.getSpecialty() != null &&
                            doctor.getSpecialty().toLowerCase().contains(specialty));

            boolean matchesZip = zip.isEmpty() ||
                    (doctor.getZip() != null && doctor.getZip().equals(zip));

            if (matchesName && matchesSpecialty && matchesZip) {
                filteredDoctors.add(doctor);
            }
        }

        displayDoctors(filteredDoctors);

        String filterNotice = "";
        if (!name.isEmpty() || !specialty.isEmpty() || !zip.isEmpty()) {
            filterNotice = " (filtered)";
        }

        showStatus("Found " + filteredDoctors.size() + " doctors" + filterNotice, false);
    }

    /**
     * Display filtered doctors in the list.
     */
    private void displayDoctors(List<Doctor> doctors) {
        doctorListVBox.getChildren().clear();

        if (doctors.isEmpty()) {
            Label noResultsLabel = new Label("No doctors match your search criteria.");
            noResultsLabel.setStyle("-fx-text-alignment: center; -fx-padding: 20;");
            doctorListVBox.getChildren().add(noResultsLabel);
            return;
        }

        for (Doctor doctor : doctors) {
            doctorListVBox.getChildren().add(createDoctorCard(doctor));
        }
    }

    /**
     * Create a card displaying a doctor's information with action buttons.
     */
    private VBox createDoctorCard(Doctor doctor) {
        VBox card = new VBox();
        card.setStyle("-fx-border-color: #E8E8E8; -fx-border-radius: 5; -fx-padding: 15; -fx-spacing: 10; -fx-background-color: white;");
        card.setPrefWidth(Double.MAX_VALUE);

        Label nameLabel = new Label(doctor.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label specialtyLabel = new Label(doctor.getSpecialty());
        specialtyLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7F8C8D;");

        Label clinicLabel = new Label(
                doctor.getClinicName() != null
                        ? "Clinic: " + doctor.getClinicName()
                        : "Clinic: Not specified"
        );
        clinicLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495E;");

        String location = (doctor.getCity() != null ? doctor.getCity() : "Unknown") +
                ", " + (doctor.getState() != null ? doctor.getState() : "Unknown") +
                " " + (doctor.getZip() != null ? doctor.getZip() : "");
        Label locationLabel = new Label("Location: " + location);
        locationLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495E;");

        Label acceptingLabel = new Label(
                doctor.getAcceptingNewPatients() != null && doctor.getAcceptingNewPatients()
                        ? "✓ Accepting New Patients"
                        : "Not accepting new patients"
        );
        acceptingLabel.setStyle(
                doctor.getAcceptingNewPatients() != null && doctor.getAcceptingNewPatients()
                        ? "-fx-font-size: 11; -fx-text-fill: #27AE60; -fx-font-weight: bold;"
                        : "-fx-font-size: 11; -fx-text-fill: #E74C3C;"
        );

        VBox infoSection = new VBox(5);
        infoSection.getChildren().addAll(specialtyLabel, clinicLabel, locationLabel, acceptingLabel);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setStyle("-fx-alignment: center-right;");

        Button viewProfileButton = new Button("View Profile");
        viewProfileButton.setStyle("-fx-padding: 8 15; -fx-font-size: 12; -fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand;");
        viewProfileButton.setOnAction(e -> viewDoctorProfile(doctor));

        Button bookAppointmentButton = new Button("Book Appointment");
        bookAppointmentButton.setStyle("-fx-padding: 8 15; -fx-font-size: 12; -fx-background-color: #27AE60; -fx-text-fill: white; -fx-cursor: hand;");
        bookAppointmentButton.setOnAction(e -> openAppointmentDialog(doctor));

        buttonsBox.getChildren().addAll(viewProfileButton, bookAppointmentButton);

        card.getChildren().addAll(nameLabel, infoSection, buttonsBox);
        VBox.setVgrow(card, Priority.NEVER);

        return card;
    }

    /**
     * View doctor's full profile (read-only).
     */
    private void viewDoctorProfile(Doctor doctor) {
        showStatus("Loading profile...", false);

        firebaseService.getDoctorProfile(doctor.getUid())
                .thenAccept(profile -> {
                    Doctor full = new Doctor();
                    full.setUid(profile.getUid());
                    full.setName(profile.getName());
                    full.setSpecialty(profile.getSpecialty());
                    full.setZip(profile.getZip());
                    full.setClinicName(profile.getClinicName());
                    full.setCity(profile.getCity());
                    full.setState(profile.getState());
                    full.setAddress(profile.getAddress());
                    full.setPhone(profile.getPhone());
                    full.setPublicEmail(profile.getEmail());
                    full.setAcceptingNewPatients(profile.getAcceptingNewPatients());
                    full.setLicenseNumber(profile.getLicenseNumber());
                    full.setBio(profile.getBio());
                    full.setInsuranceInfo(profile.getInsuranceInfo());
                    full.setHours(profile.getHours());
                    full.setAvailability(profile.getAvailability() != null
                            ? profile.getAvailability()
                            : new HashMap<>());
                    full.setVisitType(profile.getVisitType());
                    full.setNotes(profile.getNotes());

                    Platform.runLater(() -> {
                        UserContext.getInstance().setSelectedDoctor(full);
                        showStatus("", false);
                        SceneRouter.go("doctor-profile-view.fxml", "Doctor Profile");
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Failed to load profile", true));
                    return null;
                });
    }

    /**
     * Opens booking dialog in the doctor search window flow.
     */
    private void openAppointmentDialog(Doctor doctor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Book Appointment");
        dialog.setHeaderText("Schedule with Dr. " + doctor.getName());

        ButtonType confirmButtonType = new ButtonType("Confirm Booking", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<String> timeSlotComboBox = new ComboBox<>();
        timeSlotComboBox.setPromptText("Select a time slot");
        timeSlotComboBox.setPrefWidth(240);

        CheckBox newPatientCheckBox = new CheckBox("I am a new patient");
        TextField reasonField = new TextField();
        reasonField.setPromptText("Reason for visit");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Additional notes (optional)");
        notesArea.setPrefRowCount(3);

        Label officeHoursLabel = new Label(getOfficeHoursText(doctor));
        officeHoursLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11;");

        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date.isBefore(LocalDate.now()) || !isDoctorWorkingOnDate(doctor, date)) {
                    setDisable(true);
                }
            }
        });

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            timeSlotComboBox.getItems().clear();
            timeSlotComboBox.setValue(null);
            feedbackLabel.setText("");

            if (newDate == null) {
                return;
            }

            List<String> slots = generateAvailableTimeSlots(doctor, newDate);
            timeSlotComboBox.getItems().addAll(slots);

            if (slots.isEmpty()) {
                feedbackLabel.setText("No available appointment slots for the selected date.");
            }
        });

        if (datePicker.getValue() != null) {
            timeSlotComboBox.getItems().addAll(generateAvailableTimeSlots(doctor, datePicker.getValue()));
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Appointment Date"),
                datePicker,
                new Label("Available Time"),
                timeSlotComboBox,
                officeHoursLabel,
                newPatientCheckBox,
                new Label("Reason for Visit"),
                reasonField,
                new Label("Notes"),
                notesArea,
                feedbackLabel
        );

        dialog.getDialogPane().setContent(content);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            LocalDate selectedDate = datePicker.getValue();
            String selectedTimeText = timeSlotComboBox.getValue();

            if (selectedDate == null) {
                feedbackLabel.setText("Please select an appointment date.");
                event.consume();
                return;
            }

            if (selectedTimeText == null || selectedTimeText.isBlank()) {
                feedbackLabel.setText("Please select an appointment time.");
                event.consume();
                return;
            }

            LocalTime selectedTime;
            try {
                selectedTime = LocalTime.parse(selectedTimeText, TIME_FORMATTER);
            } catch (Exception ex) {
                feedbackLabel.setText("Invalid time slot selected.");
                event.consume();
                return;
            }

            if (!isWithinOfficeHours(doctor, selectedDate, selectedTime)) {
                feedbackLabel.setText("The selected time is outside office hours.");
                event.consume();
                return;
            }

            LocalDateTime selectedDateTime = LocalDateTime.of(selectedDate, selectedTime);
            if (selectedDateTime.isBefore(LocalDateTime.now())) {
                feedbackLabel.setText("You cannot book an appointment in the past.");
                event.consume();
                return;
            }

            long appointmentTimestamp = selectedDateTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            Appointment appointment = new Appointment();
            appointment.setDoctorUid(doctor.getUid());
            appointment.setDoctorName(doctor.getName());
            appointment.setPatientUid(UserContext.getInstance().getUid());
            appointment.setPatientName(UserContext.getInstance().getName());
            appointment.setAppointmentDateTime(appointmentTimestamp);
            appointment.setAppointmentTime(selectedDateTime.format(DISPLAY_DATE_TIME_FORMATTER));
            appointment.setStatus("SCHEDULED");
            appointment.setNewPatient(newPatientCheckBox.isSelected());
            appointment.setReason(reasonField.getText() != null ? reasonField.getText().trim() : "");
            appointment.setNotes(notesArea.getText() != null ? notesArea.getText().trim() : "");
            appointment.setCreatedAt(System.currentTimeMillis());

            confirmButton.setDisable(true);
            feedbackLabel.setStyle("-fx-text-fill: #3498DB; -fx-font-size: 11;");
            feedbackLabel.setText("Saving appointment...");

            firebaseService.isTimeSlotAvailable(doctor.getUid(), appointmentTimestamp)
                    .thenCompose(isAvailable -> {
                        if (!isAvailable) {
                            throw new RuntimeException("This slot has already been booked.");
                        }
                        return firebaseService.saveAppointment(appointment);
                    })
                    .thenAccept(saved -> Platform.runLater(() -> {
                        dialog.close();
                        showStatus(
                                "Appointment booked with Dr. " + doctor.getName() +
                                        " on " + selectedDate + " at " + selectedTimeText,
                                false
                        );
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            confirmButton.setDisable(false);
                            feedbackLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11;");
                            feedbackLabel.setText(
                                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()
                            );
                        });
                        return null;
                    });

            event.consume();
        });

        dialog.showAndWait();
    }

    /**
     * Generate available time slots within office hours,
     * excluding already-booked appointments and past times.
     */
    private List<String> generateAvailableTimeSlots(Doctor doctor, LocalDate date) {
        if (!isDoctorWorkingOnDate(doctor, date)) {
            return Collections.emptyList();
        }

        LocalTime openTime = getOpenTimeForDate(doctor, date);
        LocalTime closeTime = getCloseTimeForDate(doctor, date);

        List<LocalTime> allSlots = new ArrayList<>();
        LocalTime current = openTime;

        while (current.isBefore(closeTime)) {
            allSlots.add(current);
            current = current.plusMinutes(APPOINTMENT_SLOT_MINUTES);
        }

        try {
            List<Appointment> existingAppointments = firebaseService
                    .getDoctorAppointmentsForDate(doctor.getUid(), date)
                    .join();

            Set<Long> bookedTimes = existingAppointments.stream()
                    .filter(appt -> appt.getAppointmentDateTime() != null)
                    .filter(appt -> appt.getStatus() == null || !appt.getStatus().equalsIgnoreCase("CANCELLED"))
                    .map(Appointment::getAppointmentDateTime)
                    .collect(Collectors.toSet());

            return allSlots.stream()
                    .filter(time -> {
                        LocalDateTime dateTime = LocalDateTime.of(date, time);
                        long ts = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        return !bookedTimes.contains(ts);
                    })
                    .filter(time -> {
                        LocalDateTime dateTime = LocalDateTime.of(date, time);
                        return dateTime.isAfter(LocalDateTime.now());
                    })
                    .map(time -> time.format(TIME_FORMATTER))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return allSlots.stream()
                    .filter(time -> {
                        LocalDateTime dateTime = LocalDateTime.of(date, time);
                        return dateTime.isAfter(LocalDateTime.now());
                    })
                    .map(time -> time.format(TIME_FORMATTER))
                    .collect(Collectors.toList());
        }
    }

    private boolean isDoctorWorkingOnDate(Doctor doctor, LocalDate date) {
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        Map<String, String> availability = doctor.getAvailability();

        if (availability == null || availability.isEmpty()) {
            return !(date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY);
        }

        String daySchedule = availability.get(dayName);
        return daySchedule != null
                && !daySchedule.isBlank()
                && !daySchedule.equalsIgnoreCase("Closed");
    }

    private boolean isWithinOfficeHours(Doctor doctor, LocalDate date, LocalTime time) {
        if (!isDoctorWorkingOnDate(doctor, date)) {
            return false;
        }

        LocalTime open = getOpenTimeForDate(doctor, date);
        LocalTime close = getCloseTimeForDate(doctor, date);

        return !time.isBefore(open) && time.isBefore(close);
    }

    private LocalTime getOpenTimeForDate(Doctor doctor, LocalDate date) {
        String schedule = getDaySchedule(doctor, date);
        return parseStartTime(schedule);
    }

    private LocalTime getCloseTimeForDate(Doctor doctor, LocalDate date) {
        String schedule = getDaySchedule(doctor, date);
        return parseEndTime(schedule);
    }

    private String getDaySchedule(Doctor doctor, LocalDate date) {
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        Map<String, String> availability = doctor.getAvailability();

        if (availability == null || availability.isEmpty()) {
            return "9:00 AM - 5:00 PM";
        }

        String schedule = availability.get(dayName);
        return (schedule == null || schedule.isBlank()) ? "Closed" : schedule;
    }

    private LocalTime parseStartTime(String schedule) {
        try {
            if (schedule == null || schedule.equalsIgnoreCase("Closed")) {
                return DEFAULT_OPEN_TIME;
            }
            String[] parts = schedule.split("-");
            return LocalTime.parse(parts[0].trim(), TIME_FORMATTER);
        } catch (Exception e) {
            return DEFAULT_OPEN_TIME;
        }
    }

    private LocalTime parseEndTime(String schedule) {
        try {
            if (schedule == null || schedule.equalsIgnoreCase("Closed")) {
                return DEFAULT_CLOSE_TIME;
            }
            String[] parts = schedule.split("-");
            return LocalTime.parse(parts[1].trim(), TIME_FORMATTER);
        } catch (Exception e) {
            return DEFAULT_CLOSE_TIME;
        }
    }

    private String getOfficeHoursText(Doctor doctor) {
        Map<String, String> availability = doctor.getAvailability();
        if (availability == null || availability.isEmpty()) {
            return "Office hours: Monday-Friday, 9:00 AM - 5:00 PM";
        }
        return "Only available days and hours can be booked.";
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #E74C3C; -fx-font-size: 12;"
                : "-fx-text-fill: #27AE60; -fx-font-size: 12;");
    }
}