package rakib.bcs430healthcareproject;

public class AppNotification {

    private String notificationId;
    private String userUid;
    private String userRole;
    private String title;
    private String message;
    private String type;
    private String relatedId;
    private boolean read;
    private long createdAt;

    public AppNotification() {
    }

    public AppNotification(String notificationId,
                           String userUid,
                           String userRole,
                           String title,
                           String message,
                           String type,
                           String relatedId,
                           boolean read,
                           long createdAt) {
        this.notificationId = notificationId;
        this.userUid = userUid;
        this.userRole = userRole;
        this.title = title;
        this.message = message;
        this.type = type;
        this.relatedId = relatedId;
        this.read = read;
        this.createdAt = createdAt;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}