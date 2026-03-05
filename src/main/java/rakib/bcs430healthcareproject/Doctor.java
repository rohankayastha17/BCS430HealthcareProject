package rakib.bcs430healthcareproject;

/**
 * Model class representing a doctor for search results.
 * Simplified version of DoctorProfile for displaying in search results.
 */
public class Doctor {
    private String uid;
    private String name;
    private String specialty;
    private String zip;
    private String clinicName;
    private String city;
    private String state;
    private Boolean acceptingNewPatients;
    private String address;
    private String phone;
    private String email; // public contact address

    // additional profile details that may be loaded when viewing full profile
    private String licenseNumber;
    private String bio;
    private String insuranceInfo;
    private String hours;    // legacy string representation of office hours
    private java.util.Map<String,String> availability; // map day->time range (e.g. "Monday"->"9:00 AM-5:00 PM")
    private String visitType;
    private String notes;

    public Doctor() {
        this.availability = new java.util.HashMap<>();
    }

    public Doctor(String uid, String name, String specialty, String zip, 
                  String clinicName, String city, String state, 
                  Boolean acceptingNewPatients) {
        this.uid = uid;
        this.name = name;
        this.specialty = specialty;
        this.zip = zip;
        this.clinicName = clinicName;
        this.city = city;
        this.state = state;
        this.acceptingNewPatients = acceptingNewPatients;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getAcceptingNewPatients() {
        return acceptingNewPatients;
    }

    public void setAcceptingNewPatients(Boolean acceptingNewPatients) {
        this.acceptingNewPatients = acceptingNewPatients;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPublicEmail() {
        return email;
    }

    public void setPublicEmail(String email) {
        this.email = email;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getInsuranceInfo() {
        return insuranceInfo;
    }

    public void setInsuranceInfo(String insuranceInfo) {
        this.insuranceInfo = insuranceInfo;
    }

    public String getHours() {
        return hours;
    }

    public void setHours(String hours) {
        this.hours = hours;
    }

    public java.util.Map<String, String> getAvailability() {
        return availability;
    }

    public void setAvailability(java.util.Map<String, String> availability) {
        this.availability = availability;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return name + " - " + specialty;
    }
}
