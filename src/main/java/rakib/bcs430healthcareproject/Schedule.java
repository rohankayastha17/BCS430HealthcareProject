package rakib.bcs430healthcareproject;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Schedule {

    private final StringProperty time;
    private final StringProperty patientName;
    private final StringProperty type;
    private final StringProperty status;
    private final StringProperty notes;
    private Appointment sourceAppointment;

    public Schedule(String time, String patientName, String type, String status, String notes) {
        this.time = new SimpleStringProperty(time);
        this.patientName = new SimpleStringProperty(patientName);
        this.type = new SimpleStringProperty(type);
        this.status = new SimpleStringProperty(status);
        this.notes = new SimpleStringProperty(notes);
    }

    public StringProperty timeProperty() {
        return time;
    }

    public StringProperty patientNameProperty() {
        return patientName;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public String getPatientName() {
        return patientName.get();
    }

    public String getTime() {
        return time.get();
    }

    public String getType() {
        return type.get();
    }

    public String getNotes() {
        return notes.get();
    }

    public String getStatus() {
        return status.get();
    }

    public void setTime(String value) {
        time.set(value);
    }

    public void setPatientName(String value) {
        patientName.set(value);
    }

    public void setType(String value) {
        type.set(value);
    }

    public void setStatus(String value) {
        status.set(value);
    }

    public void setNotes(String value) {
        notes.set(value);
    }

    public Appointment getSourceAppointment() {
        return sourceAppointment;
    }

    public void setSourceAppointment(Appointment sourceAppointment) {
        this.sourceAppointment = sourceAppointment;
    }
}
