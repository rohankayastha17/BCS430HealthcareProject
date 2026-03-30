package rakib.bcs430healthcareproject;

/**
 * Model representing a prescription sent by a doctor for a patient.
 */
public class Prescription {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FILLED = "FILLED";
    public static final String STATUS_REFILL_REQUESTED = "REFILL_REQUESTED";

    private String prescriptionId;
    private String doctorUid;
    private String doctorName;
    private String patientUid;
    private String patientName;
    private String pharmacyName;
    private String pharmacyAddress;
    private String pharmacyAddressNormalized;
    private String pharmacyPhoneNumber;
    private String medicationName;
    private String dosage;
    private String quantity;
    private String refillDetails;
    private Integer remainingRefills;
    private String medicationInformation;
    private String instructions;
    private String status;
    private String filledBy;
    private Long filledAt;
    private Long createdAt;
    private Boolean refillRequested;
    private String refillRequestedBy;
    private Long refillRequestedAt;

    public Prescription() {
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public String getPharmacyAddress() {
        return pharmacyAddress;
    }

    public void setPharmacyAddress(String pharmacyAddress) {
        this.pharmacyAddress = pharmacyAddress;
    }

    public String getPharmacyAddressNormalized() {
        return pharmacyAddressNormalized;
    }

    public void setPharmacyAddressNormalized(String pharmacyAddressNormalized) {
        this.pharmacyAddressNormalized = pharmacyAddressNormalized;
    }

    public String getPharmacyPhoneNumber() {
        return pharmacyPhoneNumber;
    }

    public void setPharmacyPhoneNumber(String pharmacyPhoneNumber) {
        this.pharmacyPhoneNumber = pharmacyPhoneNumber;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getRefillDetails() {
        return refillDetails;
    }

    public void setRefillDetails(String refillDetails) {
        this.refillDetails = refillDetails;
    }

    public Integer getRemainingRefills() {
        return remainingRefills;
    }

    public void setRemainingRefills(Integer remainingRefills) {
        this.remainingRefills = remainingRefills;
    }

    public String getMedicationInformation() {
        return medicationInformation;
    }

    public void setMedicationInformation(String medicationInformation) {
        this.medicationInformation = medicationInformation;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilledBy() {
        return filledBy;
    }

    public void setFilledBy(String filledBy) {
        this.filledBy = filledBy;
    }

    public Long getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(Long filledAt) {
        this.filledAt = filledAt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getRefillRequested() {
        return refillRequested;
    }

    public void setRefillRequested(Boolean refillRequested) {
        this.refillRequested = refillRequested;
    }

    public String getRefillRequestedBy() {
        return refillRequestedBy;
    }

    public void setRefillRequestedBy(String refillRequestedBy) {
        this.refillRequestedBy = refillRequestedBy;
    }

    public Long getRefillRequestedAt() {
        return refillRequestedAt;
    }

    public void setRefillRequestedAt(Long refillRequestedAt) {
        this.refillRequestedAt = refillRequestedAt;
    }
}
