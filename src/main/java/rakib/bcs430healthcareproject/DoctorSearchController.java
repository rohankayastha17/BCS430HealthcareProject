package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

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

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        setupUI();
        loadDoctors();
    }

    private void setupUI() {
        // Setup search fields
        nameSearchField.setPromptText("Doctor name...");
        specialtySearchField.setPromptText("e.g., Cardiology, Family Medicine...");
        zipSearchField.setPromptText("e.g., 11735");

        // Add search functionality on text change
        nameSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        specialtySearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        zipSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Setup buttons
        searchButton.setStyle(
                "-fx-padding: 10 20; -fx-font-size: 13; -fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand;"
        );
        clearFiltersButton.setStyle(
                "-fx-padding: 10 20; -fx-font-size: 13; -fx-background-color: #95A5A6; -fx-text-fill: white; -fx-cursor: hand;"
        );
    }

    /**
     * Load all doctors from Firebase
     */
    private void loadDoctors() {
        showStatus("Loading doctors...", false);

        firebaseService.getAllDoctors()
                .thenAccept(doctors -> {
                    javafx.application.Platform.runLater(() -> {
                        allDoctors = doctors;
                        filteredDoctors = new ArrayList<>(doctors);
                        if (doctors.isEmpty()) {
                            showStatus("No doctors found.", false);
                        } else {
                            showStatus("Found " + doctors.size() + " doctors", false);
                            displayDoctors(filteredDoctors);
                        }
                    });
                })
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() ->
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
     * Apply specialty and ZIP code filters
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
     * Display filtered doctors in the list
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
     * Create a card displaying a doctor's information with action buttons
     */
    private VBox createDoctorCard(Doctor doctor) {
        VBox card = new VBox();
        card.setStyle("-fx-border-color: #E8E8E8; -fx-border-radius: 5; -fx-padding: 15; -fx-spacing: 10; -fx-background-color: white;");
        card.setPrefWidth(Double.MAX_VALUE);

        // Doctor name and specialty
        Label nameLabel = new Label(doctor.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label specialtyLabel = new Label(doctor.getSpecialty());
        specialtyLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7F8C8D;");

        // Clinic info
        Label clinicLabel = new Label(doctor.getClinicName() != null ? 
                "Clinic: " + doctor.getClinicName() : "Clinic: Not specified");
        clinicLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495E;");

        // Location info
        String location = (doctor.getCity() != null ? doctor.getCity() : "Unknown") + 
                         ", " + (doctor.getState() != null ? doctor.getState() : "Unknown") + 
                         " " + (doctor.getZip() != null ? doctor.getZip() : "");
        Label locationLabel = new Label("Location: " + location);
        locationLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495E;");

        // Accepting new patients status
        Label acceptingLabel = new Label(
                doctor.getAcceptingNewPatients() != null && doctor.getAcceptingNewPatients() ? 
                "✓ Accepting New Patients" : 
                "Not accepting new patients"
        );
        acceptingLabel.setStyle(doctor.getAcceptingNewPatients() != null && doctor.getAcceptingNewPatients() ? 
                "-fx-font-size: 11; -fx-text-fill: #27AE60; -fx-font-weight: bold;" : 
                "-fx-font-size: 11; -fx-text-fill: #E74C3C;");

        // Info section
        VBox infoSection = new VBox(5);
        infoSection.getChildren().addAll(specialtyLabel, clinicLabel, locationLabel, acceptingLabel);

        // Action buttons
        HBox buttonsBox = new HBox(10);
        buttonsBox.setStyle("-fx-alignment: center-right;");

        Button viewProfileButton = new Button("View Profile");
        viewProfileButton.setStyle("-fx-padding: 8 15; -fx-font-size: 12; -fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand;");
        viewProfileButton.setOnAction(e -> viewDoctorProfile(doctor));

        Button bookAppointmentButton = new Button("Book Appointment");
        bookAppointmentButton.setStyle("-fx-padding: 8 15; -fx-font-size: 12; -fx-background-color: #27AE60; -fx-text-fill: white; -fx-cursor: hand;");
        bookAppointmentButton.setOnAction(e -> bookAppointment(doctor));

        buttonsBox.getChildren().addAll(viewProfileButton, bookAppointmentButton);

        // Add all to card
        card.getChildren().addAll(nameLabel, infoSection, buttonsBox);
        VBox.setVgrow(card, Priority.NEVER);

        return card;
    }

    /**
     * View doctor's full profile (read-only)
     */
    private void viewDoctorProfile(Doctor doctor) {
        System.out.println("Viewing profile for doctor: " + doctor.getName());
        // request full profile from backend before navigating
        showStatus("Loading profile...", false);
        firebaseService.getDoctorProfile(doctor.getUid())
                .thenAccept(profile -> {
                    // convert DoctorProfile to lightweight Doctor for viewing
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
                    full.setAvailability(profile.getAvailability() != null ? profile.getAvailability() : new java.util.HashMap<>());
                    full.setVisitType(profile.getVisitType());
                    full.setNotes(profile.getNotes());

                    javafx.application.Platform.runLater(() -> {
                        UserContext.getInstance().setSelectedDoctor(full);
                        showStatus("", false);
                        SceneRouter.go("doctor-profile-view.fxml", "Doctor Profile");
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> showStatus("Failed to load profile", true));
                    return null;
                });
    }

    /**
     * Navigate to appointment booking for this doctor
     */
    private void bookAppointment(Doctor doctor) {
        System.out.println("Booking appointment with: " + doctor.getName());
        // Pass doctor data to appointment booking controller
        UserContext.getInstance().setSelectedDoctor(doctor);
        SceneRouter.go("book-appointment-view.fxml", "Book Appointment");
    }

    @FXML
    private void onBack() {
        // return to patient dashboard rather than edit profile
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
