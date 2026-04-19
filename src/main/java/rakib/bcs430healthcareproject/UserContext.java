package rakib.bcs430healthcareproject;

public class UserContext {
    private static UserContext instance;

    private String uid;
    private String role;

    private PatientProfile patientProfile;
    private DoctorProfile doctorProfile;
    private PharmacyProfile pharmacyProfile;
    private HospitalProfile hospitalProfile;

    // For patient appointment booking
    private Doctor selectedDoctor;

    // For doctor / hospital viewing patient history/details
    private String selectedPatientUid;
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
        this.pharmacyProfile = null;
        this.hospitalProfile = null;
    }

    public void setDoctorUserData(String uid, DoctorProfile profile) {
        this.uid = uid;
        this.role = "DOCTOR";
        this.doctorProfile = profile;
        this.patientProfile = null;
        this.pharmacyProfile = null;
        this.hospitalProfile = null;
    }

    public void setPharmacyUserData(String uid, PharmacyProfile profile) {
        this.uid = uid;
        this.role = "PHARMACY";
        this.pharmacyProfile = profile;
        this.doctorProfile = null;
        this.patientProfile = null;
        this.hospitalProfile = null;
    }

    public void setHospitalUserData(String uid, HospitalProfile profile) {
        this.uid = uid;
        this.role = "HOSPITAL";
        this.hospitalProfile = profile;
        this.pharmacyProfile = null;
        this.doctorProfile = null;
        this.patientProfile = null;
    }

    public void clearUserData() {
        this.uid = null;
        this.role = null;
        this.patientProfile = null;
        this.doctorProfile = null;
        this.pharmacyProfile = null;
        this.hospitalProfile = null;
        this.selectedDoctor = null;
        this.selectedPatientUid = null;
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

    public boolean isPharmacy() {
        return "PHARMACY".equals(role);
    }

    public boolean isHospital() {
        return "HOSPITAL".equals(role);
    }

    public boolean isLoggedIn() {
        return uid != null && role != null;
    }

    public PatientProfile getProfile() {
        return patientProfile;
    }

    public PatientProfile getPatientProfile() {
        return patientProfile;
    }

    public DoctorProfile getDoctorProfile() {
        return doctorProfile;
    }

    public PharmacyProfile getPharmacyProfile() {
        return pharmacyProfile;
    }

    public HospitalProfile getHospitalProfile() {
        return hospitalProfile;
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
        if (isHospital() && hospitalProfile != null) {
            return hospitalProfile.getEmail();
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
        if (isHospital() && hospitalProfile != null) {
            return hospitalProfile.getHospitalName();
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

    public void updateHospitalProfile(HospitalProfile updatedProfile) {
        if (isHospital()) {
            this.hospitalProfile = updatedProfile;
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

    public void setSelectedPatientUid(String selectedPatientUid) {
        this.selectedPatientUid = selectedPatientUid;
    }

    public String getSelectedPatientUid() {
        return selectedPatientUid;
    }

    public void setSelectedPatientProfile(PatientProfile selectedPatientProfile) {
        this.selectedPatientProfile = selectedPatientProfile;
    }

    public PatientProfile getSelectedPatientProfile() {
        return selectedPatientProfile;
    }

    public void clearSelectedPatientProfile() {
        this.selectedPatientUid = null;
        this.selectedPatientProfile = null;
    }

    public void clearSelectedPatient() {
        this.selectedPatientUid = null;
        this.selectedPatientProfile = null;
    }
}