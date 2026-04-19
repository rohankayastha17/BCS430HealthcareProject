package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HospitalPatientsController {

    @FXML private TextField searchField;
    @FXML private Label resultsLabel;
    @FXML private VBox patientsListVBox;

    private final FirebaseService firebaseService = new FirebaseService();
    private final UserContext userContext = UserContext.getInstance();

    private List<PatientProfile> allPatients = new ArrayList<>();

    @FXML
    public void initialize() {
        loadPatients();
    }

    private void loadPatients() {
        try {
            HospitalProfile hospital = userContext.getHospitalProfile();
            if (hospital == null) {
                showEmpty("No hospital is currently loaded.");
                return;
            }

            allPatients = firebaseService.getPatientsForHospital(hospital.getUid()).get();
            renderPatients(allPatients);

        } catch (Exception e) {
            System.err.println("Failed to load hospital patients: " + e.getMessage());
            e.printStackTrace();
            showEmpty("Unable to load patients.");
        }
    }

    private void renderPatients(List<PatientProfile> patients) {
        patientsListVBox.getChildren().clear();

        if (patients == null || patients.isEmpty()) {
            showEmpty("No patients found.");
            return;
        }

        resultsLabel.setText("Patient Results (" + patients.size() + ")");

        for (PatientProfile patient : patients) {
            patientsListVBox.getChildren().add(buildPatientCard(patient));
        }
    }

    private VBox buildPatientCard(PatientProfile patient) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #D1FAE5;" +
                        "-fx-border-radius: 14;"
        );

        Label nameLabel = new Label(valueOrDefault(patient.getName(), "Unnamed Patient"));
        nameLabel.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 16; -fx-font-weight: bold;");

        Label emailLabel = new Label("Email: " + valueOrDefault(patient.getEmail(), "Not listed"));
        emailLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        Label phoneLabel = new Label("Phone: " + valueOrDefault(patient.getPhoneNumber(), "Not listed"));
        phoneLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12;");

        String dob = extractOptional("DOB", safeGetDateOfBirth(patient));
        String gender = extractOptional("Gender", safeGetGender(patient));

        Label extraLabel = new Label((dob + "   " + gender).trim());
        extraLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        HBox actions = new HBox(10);

        Button viewButton = new Button("View Profile");
        viewButton.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8;");

        Button historyButton = new Button("Medical History");
        historyButton.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #134E4A; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8;");

        actions.getChildren().addAll(viewButton, historyButton);

        card.getChildren().addAll(nameLabel, emailLabel, phoneLabel, extraLabel, actions);
        return card;
    }

    private void showEmpty(String text) {
        patientsListVBox.getChildren().clear();
        resultsLabel.setText("Patient Results");

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

        patientsListVBox.getChildren().add(card);
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        if (query.isBlank()) {
            renderPatients(allPatients);
            return;
        }

        List<PatientProfile> filtered = new ArrayList<>();
        for (PatientProfile patient : allPatients) {
            String name = valueOrDefault(patient.getName(), "").toLowerCase(Locale.ROOT);
            String email = valueOrDefault(patient.getEmail(), "").toLowerCase(Locale.ROOT);

            if (name.contains(query) || email.contains(query)) {
                filtered.add(patient);
            }
        }

        renderPatients(filtered);
    }

    @FXML
    private void onClear() {
        searchField.clear();
        renderPatients(allPatients);
    }

    @FXML
    private void onDashboard() {
        SceneRouter.go("hospital-dashboard-view.fxml", "Hospital Dashboard");
    }

    @FXML
    private void onPatients() {
        // current page
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

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String extractOptional(String label, String value) {
        return (value == null || value.isBlank()) ? "" : label + ": " + value;
    }

    private String safeGetDateOfBirth(PatientProfile patient) {
        try {
            return patient.getDateOfBirth();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeGetGender(PatientProfile patient) {
        try {
            return patient.getGender();
        } catch (Exception e) {
            return "";
        }
    }
}