package rakib.bcs430healthcareproject;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a patient's profile data.
 * This is stored in Firestore under /patients/{uid}
 */
public class PatientProfile {
    @DocumentId
    private String uid;
    private String name;
    private String email;
    private String phoneNumber;
    private String zip;
    private String role;
    private String passwordHash;
    private String passwordSalt;
    
    // Personal Information
    private String dateOfBirth;
    private Integer age;
    private String gender;
    
    // Insurance Information
    private String insuranceNumber;
    private String insuranceCompany;
    
    // Medical Information
    private String allergies;
    private String currentMedications;
    private String chronicConditions;
    private String bloodType;
    private String vaccinationStatus;
    private String medicalHistory;
    private String height;  // e.g., "5'10\"" or "178 cm"
    private Double weight;  // in lbs or kg
    
    private Long createdAt;
    private Long updatedAt;

    // Default constructor required for Firestore
    public PatientProfile() {
    }

    public PatientProfile(String uid, String name, String email, String zip) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.zip = zip;
        this.role = "PATIENT";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    public void setInsuranceCompany(String insuranceCompany) {
        this.insuranceCompany = insuranceCompany;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getCurrentMedications() {
        return currentMedications;
    }

    public void setCurrentMedications(String currentMedications) {
        this.currentMedications = currentMedications;
    }

    public String getChronicConditions() {
        return chronicConditions;
    }

    public void setChronicConditions(String chronicConditions) {
        this.chronicConditions = chronicConditions;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getVaccinationStatus() {
        return vaccinationStatus;
    }

    public void setVaccinationStatus(String vaccinationStatus) {
        this.vaccinationStatus = vaccinationStatus;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Convert PatientProfile to a Map for Firestore storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("name", name);
        result.put("email", email);
        result.put("phoneNumber", phoneNumber);
        result.put("zip", zip);
        result.put("role", role);
        result.put("passwordHash", passwordHash);
        result.put("passwordSalt", passwordSalt);
        result.put("dateOfBirth", dateOfBirth);
        result.put("age", age);
        result.put("gender", gender);
        result.put("insuranceNumber", insuranceNumber);
        result.put("insuranceCompany", insuranceCompany);
        result.put("allergies", allergies);
        result.put("currentMedications", currentMedications);
        result.put("chronicConditions", chronicConditions);
        result.put("bloodType", bloodType);
        result.put("vaccinationStatus", vaccinationStatus);
        result.put("medicalHistory", medicalHistory);
        result.put("height", height);
        result.put("weight", weight);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        return result;
    }
}
