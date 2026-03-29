package rakib.bcs430healthcareproject;

/**
 * Manages the current logged-in user context across the application.
 * This is a simple singleton to store the current user's UID and profile information.
 */
public class UserContext {
    private static UserContext instance;

    private String uid;
    private String role;

    private PatientProfile patientProfile;
    private DoctorProfile doctorProfile;
    private PharmacyProfile pharmacyProfile;

    // For patient appointment booking
    private Doctor selectedDoctor;
    private PatientProfile selectedPatientProfile;

    private UserContext() {
    }

    public static synchronized UserContext getInstance() {
        if (instance == null) {
            instance = new UserContext();
        }
        return instance;
    }

    public void setUserData(String uid, PatientProfile profile) {
        this.uid = uid;
        this.role = "PATIENT";
        this.patientProfile = profile;
        this.doctorProfile = null;
    }

    public void setDoctorUserData(String uid, DoctorProfile profile) {
        this.uid = uid;
        this.role = "DOCTOR";
        this.doctorProfile = profile;
        this.patientProfile = null;
        this.pharmacyProfile = null;
    }

    public void setPharmacyUserData(String uid, PharmacyProfile profile) {
        this.uid = uid;
        this.role = "PHARMACY";
        this.pharmacyProfile = profile;
        this.doctorProfile = null;
        this.patientProfile = null;
    }

    public void clearUserData() {
        this.uid = null;
        this.role = null;
        this.patientProfile = null;
        this.doctorProfile = null;
        this.pharmacyProfile = null;
        this.selectedDoctor = null;
        this.selectedPatientProfile = null;
    }

    public String getUid() {
        return uid;
    }

    public String getRole() {
        return role;
    }

    public boolean isPatient() {
        return "PATIENT".equals(role);
    }

    public boolean isDoctor() {
        return "DOCTOR".equals(role);
    }

    public boolean isLoggedIn() {
        return uid != null && role != null;
    }

    public boolean isPharmacy() {
        return "PHARMACY".equals(role);
    }

    public PatientProfile getProfile() {
        return patientProfile;
    }

    public DoctorProfile getDoctorProfile() {
        return doctorProfile;
    }

    public PharmacyProfile getPharmacyProfile() {
        return pharmacyProfile;
    }

    public String getEmail() {
        if (isPatient() && patientProfile != null) {
            return patientProfile.getEmail();
        }
        if (isDoctor() && doctorProfile != null) {
            return doctorProfile.getEmail();
        }
        if (isPharmacy() && pharmacyProfile != null) {
            return pharmacyProfile.getEmail();
        }
        return null;
    }

    public String getName() {
        if (isPatient() && patientProfile != null) {
            return patientProfile.getName();
        }
        if (isDoctor() && doctorProfile != null) {
            return doctorProfile.getName();
        }
        if (isPharmacy() && pharmacyProfile != null) {
            return pharmacyProfile.getPharmacyName();
        }
        return null;
    }

    public void updatePatientProfile(PatientProfile updatedProfile) {
        if (isPatient()) {
            this.patientProfile = updatedProfile;
        }
    }

    public void updateDoctorProfile(DoctorProfile updatedProfile) {
        if (isDoctor()) {
            this.doctorProfile = updatedProfile;
        }
    }

    public void updatePharmacyProfile(PharmacyProfile updatedProfile) {
        if (isPharmacy()) {
            this.pharmacyProfile = updatedProfile;
        }
    }

    public void setSelectedDoctor(Doctor doctor) {
        this.selectedDoctor = doctor;
    }

    public Doctor getSelectedDoctor() {
        return selectedDoctor;
    }

    public void clearSelectedDoctor() {
        this.selectedDoctor = null;
    }

    public void setSelectedPatientProfile(PatientProfile selectedPatientProfile) {
        this.selectedPatientProfile = selectedPatientProfile;
    }

    public PatientProfile getSelectedPatientProfile() {
        return selectedPatientProfile;
    }

    public void clearSelectedPatientProfile() {
        this.selectedPatientProfile = null;
    }
}
