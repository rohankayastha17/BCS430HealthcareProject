package rakib.bcs430healthcareproject;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationsController {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");

    @FXML private ListView<AppNotification> notificationsListView;
    @FXML private Label statusLabel;

    private FirebaseService firebaseService;

    @FXML
    public void initialize() {
        firebaseService = new FirebaseService();
        setupListView();
        loadNotifications();
    }

    private void setupListView() {
        notificationsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AppNotification notification, boolean empty) {
                super.updateItem(notification, empty);

                if (empty || notification == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label titleLabel = new Label(notification.getTitle() != null ? notification.getTitle() : "Notification");
                titleLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

                Label messageLabel = new Label(notification.getMessage() != null ? notification.getMessage() : "");
                messageLabel.setWrapText(true);
                messageLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #475569;");

                String dateText = formatTimestamp(notification.getCreatedAt());
                Label dateLabel = new Label(dateText);
                dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #64748B;");

                Label typeLabel = new Label(notification.getType() != null ? notification.getType() : "");
                typeLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #0F766E; "
                        + "-fx-background-color: #CCFBF1; -fx-padding: 4 8; -fx-background-radius: 999;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox headerRow = new HBox(8, titleLabel, spacer, typeLabel);

                VBox container = new VBox(8, headerRow, messageLabel, dateLabel);

                String baseStyle = "-fx-background-radius: 14; "
                        + "-fx-border-radius: 14; "
                        + "-fx-padding: 14;";

                if (!notification.isRead()) {
                    container.setStyle(baseStyle
                            + "-fx-background-color: #ECFEFF; "
                            + "-fx-border-color: #14B8A6; "
                            + "-fx-border-width: 1.5;");
                } else {
                    container.setStyle(baseStyle
                            + "-fx-background-color: white; "
                            + "-fx-border-color: #DDE7EE; "
                            + "-fx-border-width: 1;");
                }

                setText(null);
                setGraphic(container);
            }
        });

        notificationsListView.setOnMouseClicked(event -> {
            AppNotification selected = notificationsListView.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isRead()) {
                boolean ok = firebaseService.markNotificationAsRead(selected.getNotificationId());
                if (ok) {
                    loadNotifications();
                }
            }
        });
    }

    private void loadNotifications() {
        try {
            UserContext userContext = UserContext.getInstance();

            if (!userContext.isLoggedIn()) {
                showStatus("No user is logged in.");
                notificationsListView.setItems(FXCollections.observableArrayList());
                return;
            }

            String uid = userContext.getUid();
            List<AppNotification> notifications = firebaseService.getNotificationsForUser(uid);

            notificationsListView.setItems(FXCollections.observableArrayList(notifications));

            if (notifications.isEmpty()) {
                showStatus("No notifications yet.");
            } else {
                hideStatus();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load notifications: " + e.getMessage());
        }
    }

    @FXML
    private void onMarkAllRead() {
        try {
            UserContext userContext = UserContext.getInstance();

            if (!userContext.isLoggedIn()) {
                showError("No user is logged in.");
                return;
            }

            boolean ok = firebaseService.markAllNotificationsAsRead(userContext.getUid());
            if (ok) {
                loadNotifications();
            } else {
                showError("Could not mark notifications as read.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to mark notifications as read: " + e.getMessage());
        }
    }

    @FXML
    private void onBack() {
        try {
            UserContext userContext = UserContext.getInstance();
            String role = userContext.getRole();

            if ("DOCTOR".equalsIgnoreCase(role)) {
                SceneRouter.go("doctor-dashboard-view.fxml", "Doctor Dashboard");
            } else if ("PATIENT".equalsIgnoreCase(role)) {
                SceneRouter.go("patient-dashboard-view.fxml", "Patient Dashboard");
            } else {
                SceneRouter.go("login-view.fxml", "Login");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to go back: " + e.getMessage());
        }
    }

    private String formatTimestamp(long createdAt) {
        if (createdAt <= 0) {
            return "Unknown time";
        }

        return Instant.ofEpochMilli(createdAt)
                .atZone(ZoneId.systemDefault())
                .format(DISPLAY_FORMAT);
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Notifications Error");
        alert.setHeaderText("Notifications");
        alert.setContentText(message);
        alert.showAndWait();
    }
}