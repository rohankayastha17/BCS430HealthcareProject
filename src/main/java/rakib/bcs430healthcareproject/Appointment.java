package rakib.bcs430healthcareproject;

/**
 * Model class representing an appointment between a patient and doctor.
 */
public class Appointment {
    private String appointmentId;
    private String patientUid;
    private String doctorUid;
    private String patientName;
    private String doctorName;
    private Long appointmentDateTime; // Unix timestamp
    private String appointmentTime; // e.g., "2024-03-15 10:30 AM"
    private String status; // SCHEDULED, COMPLETED, CANCELLED
    private Boolean newPatient;
    private String reason;
    private String notes;
    private Long createdAt;

    public Appointment() {
    }

    public Appointment(String patientUid, String doctorUid, String patientName, 
                       String doctorName, Long appointmentDateTime) {
        this.patientUid = patientUid;
        this.doctorUid = doctorUid;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.appointmentDateTime = appointmentDateTime;
        this.status = "SCHEDULED";
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Long getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(Long appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getNewPatient() {
        return newPatient;
    }

    public void setNewPatient(Boolean newPatient) {
        this.newPatient = newPatient;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
