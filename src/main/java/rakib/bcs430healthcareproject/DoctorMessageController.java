package rakib.bcs430healthcareproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Messenger-style Doctor Messaging Controller
 */
public class DoctorMessageController {

    @FXML private Label doctorNameLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label patientInfoLabel;
    @FXML private Label statusLabel;

    @FXML private ListView<PatientProfile> patientListView;

    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesVBox;
    @FXML private TextArea messageInputArea;
    @FXML private Button sendButton;

    private FirebaseService firebaseService;
    private UserContext userContext;
    private PatientProfile selectedPatient;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a");

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        userContext = UserContext.getInstance();

        if (!userContext.isLoggedIn() || !userContext.isDoctor()) {
            SceneRouter.go("login-view.fxml", "Login");
            return;
        }

        doctorNameLabel.setText("Dr. " + userContext.getName());

        setupPatientList();
        loadPatients();
    }

    // =========================================================
    // LEFT PANEL (PATIENT LIST)
    // =========================================================

    private void setupPatientList() {
        patientListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PatientProfile p, boolean empty) {
                super.updateItem(p, empty);

                if (empty || p == null) {
                    setText(null);
                } else {
                    setText(p.getName() + " • " + fallback(p.getEmail()));
                }
            }
        });

        patientListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPatient = newVal;
            updateHeader();
            loadMessages();
        });
    }

    private void loadPatients() {
        firebaseService.getDoctorPatients(userContext.getUid())
                .thenAccept(patients -> Platform.runLater(() -> {
                    patientListView.getItems().setAll(patients);

                    if (!patients.isEmpty()) {
                        patientListView.getSelectionModel().selectFirst();
                    }
                }));
    }

    // =========================================================
    // HEADER
    // =========================================================

    private void updateHeader() {
        if (selectedPatient == null) {
            patientNameLabel.setText("Select a patient");
            patientInfoLabel.setText("");
            return;
        }

        patientNameLabel.setText(selectedPatient.getName());

        patientInfoLabel.setText(
                "Email: " + fallback(selectedPatient.getEmail()) +
                        " | Phone: " + fallback(selectedPatient.getPhoneNumber())
        );
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    @FXML
    private void onSendMessage() {
        if (selectedPatient == null) return;

        String text = messageInputArea.getText().trim();
        if (text.isEmpty()) return;

        Message msg = new Message();
        msg.setDoctorUid(userContext.getUid());
        msg.setDoctorName(userContext.getName());
        msg.setPatientUid(selectedPatient.getUid());
        msg.setPatientName(selectedPatient.getName());
        msg.setSenderUid(userContext.getUid());
        msg.setSenderName(userContext.getName());
        msg.setSenderRole("DOCTOR");
        msg.setMessageText(text);
        msg.setCreatedAt(System.currentTimeMillis());
        msg.setRead(false);

        firebaseService.saveMessage(msg)
                .thenRun(() -> Platform.runLater(() -> {
                    messageInputArea.clear();
                    loadMessages();
                }));
    }

    @FXML
    private void onClear() {
        messageInputArea.clear();
    }

    @FXML
    private void onBack() {
        SceneRouter.go("doctor-dashboard-view.fxml", "Dashboard");
    }

    // =========================================================
    // LOAD MESSAGES
    // =========================================================

    private void loadMessages() {
        if (selectedPatient == null) return;

        firebaseService.getMessagesBetweenDoctorAndPatient(
                        userContext.getUid(),
                        selectedPatient.getUid()
                )
                .thenAccept(messages -> Platform.runLater(() -> {
                    renderMessages(messages);
                    markAsRead();
                }));
    }

    private void markAsRead() {
        firebaseService.markMessagesAsRead(
                userContext.getUid(),
                selectedPatient.getUid(),
                "DOCTOR"
        );
    }

    // =========================================================
    // CHAT UI
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
        boolean isMe = msg.getSenderUid().equals(userContext.getUid());

        VBox wrapper = new VBox();
        HBox row = new HBox();
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10));
        bubble.setMaxWidth(400);

        bubble.setStyle(isMe
                ? "-fx-background-color: #CCFBF1; -fx-background-radius: 12;"
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
}