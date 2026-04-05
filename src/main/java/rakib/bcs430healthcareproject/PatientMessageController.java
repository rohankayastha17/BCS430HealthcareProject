package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Messenger-style Patient Messaging Controller
 */
public class PatientMessageController {

    @FXML private Label patientNameLabel;
    @FXML private Label doctorHeaderLabel;
    @FXML private Label doctorInfoLabel;
    @FXML private Label statusLabel;

    @FXML private ListView<Doctor> doctorListView;

    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesVBox;
    @FXML private TextArea messageInputArea;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile patientProfile;
    private Doctor selectedDoctor;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        patientProfile = userContext.getProfile();
        patientNameLabel.setText(patientProfile.getName());

        setupDoctorList();
        loadDoctors();
    }

    // =========================================================
    // LEFT PANEL (CONVERSATIONS)
    // =========================================================

    private void setupDoctorList() {
        doctorListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);

                if (empty || doctor == null) {
                    setText(null);
                } else {
                    setText(doctor.getName() + " • " + fallback(doctor.getSpecialty()));
                }
            }
        });

        doctorListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedDoctor = newVal;
            updateDoctorHeader();
            loadMessages();
        });
    }

    private void loadDoctors() {
        firebaseService.getDoctorsForPatient(patientProfile.getUid())
                .thenAccept(doctors -> Platform.runLater(() -> {
                    doctorListView.getItems().setAll(doctors);

                    if (!doctors.isEmpty()) {
                        doctorListView.getSelectionModel().selectFirst();
                    }
                }));
    }

    // =========================================================
    // HEADER
    // =========================================================

    private void updateDoctorHeader() {
        if (selectedDoctor == null) {
            doctorHeaderLabel.setText("Select a doctor");
            doctorInfoLabel.setText("");
            return;
        }

        doctorHeaderLabel.setText(selectedDoctor.getName());

        doctorInfoLabel.setText(
                "Specialty: " + fallback(selectedDoctor.getSpecialty()) +
                        " | Clinic: " + fallback(selectedDoctor.getClinicName())
        );
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    @FXML
    private void onSendMessage() {
        if (selectedDoctor == null) return;

        String text = messageInputArea.getText().trim();
        if (text.isEmpty()) return;

        Message msg = new Message();
        msg.setDoctorUid(selectedDoctor.getUid());
        msg.setDoctorName(selectedDoctor.getName());
        msg.setPatientUid(patientProfile.getUid());
        msg.setPatientName(patientProfile.getName());
        msg.setSenderUid(patientProfile.getUid());
        msg.setSenderName(patientProfile.getName());
        msg.setSenderRole("PATIENT");
        msg.setMessageText(text);
        msg.setCreatedAt(System.currentTimeMillis());
        msg.setRead(false);

        firebaseService.saveMessage(msg)
                .thenRun(() -> Platform.runLater(() -> {
                    messageInputArea.clear();
                    loadMessages();
                }));
    }

    // =========================================================
    // LOAD MESSAGES
    // =========================================================

    private void loadMessages() {
        if (selectedDoctor == null) return;

        firebaseService.getMessagesBetweenDoctorAndPatient(
                        selectedDoctor.getUid(),
                        patientProfile.getUid()
                )
                .thenAccept(messages -> Platform.runLater(() -> {
                    renderMessages(messages);
                    markAsRead();
                }));
    }

    private void markAsRead() {
        firebaseService.markMessagesAsRead(
                selectedDoctor.getUid(),
                patientProfile.getUid(),
                "PATIENT"
        );
    }

    // =========================================================
    // RENDER CHAT
    // =========================================================

    private void renderMessages(List<Message> messages) {
        messagesVBox.getChildren().clear();

        if (messages == null) messages = new ArrayList<>();

        messages.sort(Comparator.comparingLong(m ->
                m.getCreatedAt() != null ? m.getCreatedAt() : 0
        ));

        for (Message msg : messages) {
            messagesVBox.getChildren().add(createBubble(msg));
        }

        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    private VBox createBubble(Message msg) {
        boolean isMe = msg.getSenderUid().equals(patientProfile.getUid());

        VBox wrapper = new VBox();
        HBox row = new HBox();
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10));
        bubble.setMaxWidth(400);

        bubble.setStyle(isMe
                ? "-fx-background-color: #DBEAFE; -fx-background-radius: 12;"
                : "-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-background-radius: 12;");

        Label text = new Label(msg.getMessageText());
        text.setWrapText(true);

        Label time = new Label(formatTime(msg.getCreatedAt()));
        time.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");

        bubble.getChildren().addAll(text, time);
        row.getChildren().add(bubble);
        wrapper.getChildren().add(row);

        return wrapper;
    }

    private String formatTime(Long t) {
        return t == null ? "" : timeFormat.format(new Date(t));
    }

    // =========================================================
    // UTIL
    // =========================================================

    private String fallback(String v) {
        return v == null ? "" : v;
    }

    @FXML
    private void onClear() {
        messageInputArea.clear();
    }

    @FXML
    private void onBack() {
        SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
    }
}