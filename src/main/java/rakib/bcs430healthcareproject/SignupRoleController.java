package rakib.bcs430healthcareproject;

import javafx.fxml.FXML;

public class SignupRoleController {

    @FXML
    private void onPatient() {
        SceneRouter.go("patient-signup-view.fxml", "Patient Sign Up");
    }

    @FXML
    private void onDoctor() {
        SceneRouter.go("doctor-signup-view.fxml", "Doctor Sign Up");
    }

    @FXML
    private void onBack() {
        SceneRouter.go("login-view.fxml", "Healthcare Project");
    }
}
