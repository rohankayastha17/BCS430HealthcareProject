package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HospitalDashboardController {

    @FXML private Label hospitalNameLabel;
    @FXML private Label hospitalEmailLabel;
    @FXML private Label hospitalPhoneLabel;
    @FXML private Label hospitalAddressLabel;

    private UserContext userContext;

    @FXML
    public void initialize() {
        userContext = UserContext.getInstance();
        HospitalProfile profile = userContext.getHospitalProfile();
        if (profile != null) {
            hospitalNameLabel.setText(valueOrDefault(profile.getHospitalName(), "Hospital"));
            hospitalEmailLabel.setText("Email: " + valueOrDefault(profile.getEmail(), "Not listed"));
            hospitalPhoneLabel.setText("Contact: " + valueOrDefault(profile.getPhoneNumber(), "Not listed"));
            hospitalAddressLabel.setText("Address: " + valueOrDefault(profile.getFullAddress(), "Not listed"));
        }
    }

    @FXML
    private void onLogout() {
        userContext.clearUserData();
        SceneRouter.go("login-view.fxml", "Login");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
