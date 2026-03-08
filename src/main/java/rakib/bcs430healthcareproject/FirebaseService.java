package rakib.bcs430healthcareproject;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Service class for Firebase operations related to patient account management.
 * Handles user creation in Firebase Authentication and profile storage in Firestore.
 */
public class FirebaseService {

    private final FirebaseAuth auth;
    private final Firestore firestore;

    private static final String PATIENTS_COLLECTION = "patients";
    private static final String USERS_COLLECTION = "users";
    private static final String DOCTORS_COLLECTION = "doctors";
    private static final String APPOINTMENTS_COLLECTION = "appointments";

    public FirebaseService() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirestoreClient.getFirestore();
    }

    /**
     * Creates a patient account in Firebase.
     */
    public CompletableFuture<String> createPatient(String email, String password, String name, String zip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(password)
                        .setDisplayName(name);

                UserRecord userRecord = auth.createUser(request);
                String uid = userRecord.getUid();

                PatientProfile profile = new PatientProfile(uid, name, email, zip);

                String passwordSalt = PasswordHasher.generateSalt();
                String passwordHash = PasswordHasher.hashPassword(password, passwordSalt);
                profile.setPasswordHash(passwordHash);
                profile.setPasswordSalt(passwordSalt);

                ApiFuture<?> future = firestore.collection(PATIENTS_COLLECTION).document(uid).set(profile);
                future.get();

                System.out.println("Patient created successfully with UID: " + uid);
                return uid;

            } catch (FirebaseAuthException e) {
                String errorMessage = handleAuthException(e);
                throw new RuntimeException(errorMessage);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to save patient profile: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<String> createDoctor(
            String email,
            String password,
            String name,
            String specialty,
            String clinicName,
            String address,
            String city,
            String state,
            String zip,
            boolean acceptingNewPatients,
            Map<String, String> availability
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(password)
                        .setDisplayName(name);

                UserRecord userRecord = auth.createUser(request);
                String uid = userRecord.getUid();

                DoctorProfile profile = new DoctorProfile(
                        uid,
                        name,
                        email,
                        specialty,
                        clinicName,
                        address,
                        city,
                        state,
                        zip,
                        acceptingNewPatients
                );

                String passwordSalt = PasswordHasher.generateSalt();
                String passwordHash = PasswordHasher.hashPassword(password, passwordSalt);
                profile.setPasswordHash(passwordHash);
                profile.setPasswordSalt(passwordSalt);

                profile.setCreatedAt(System.currentTimeMillis());
                profile.setUpdatedAt(System.currentTimeMillis());
                profile.setRole("DOCTOR");
                profile.setAvailability(availability);

                firestore.collection(DOCTORS_COLLECTION).document(uid).set(profile).get();

                Map<String, Object> userDoc = new HashMap<>();
                userDoc.put("uid", uid);
                userDoc.put("name", name);
                userDoc.put("email", email);
                userDoc.put("role", "DOCTOR");
                userDoc.put("createdAt", System.currentTimeMillis());

                firestore.collection(USERS_COLLECTION).document(uid).set(userDoc).get();

                System.out.println("Doctor created successfully with UID: " + uid);
                return uid;

            } catch (FirebaseAuthException e) {
                throw new RuntimeException(handleAuthException(e));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Doctor creation interrupted.", e);

            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to save doctor profile: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<String> authenticateDoctor(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Authenticating doctor with email: " + email);

                ApiFuture<QuerySnapshot> query = firestore.collection(DOCTORS_COLLECTION)
                        .whereEqualTo("email", email)
                        .get();

                QuerySnapshot querySnapshot = query.get();

                if (querySnapshot.isEmpty()) {
                    throw new RuntimeException("No account found with this email address.");
                }

                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                System.out.println("Doctor raw data: " + document.getData());

                DoctorProfile profile = document.toObject(DoctorProfile.class);

                if (profile == null) {
                    throw new RuntimeException("Failed to load doctor profile.");
                }
                if (profile.getPasswordHash() == null || profile.getPasswordSalt() == null) {
                    throw new RuntimeException("Account security data not found.");
                }

                boolean ok = PasswordHasher.verifyPassword(password, profile.getPasswordHash(), profile.getPasswordSalt());
                if (!ok) {
                    throw new RuntimeException("Invalid email or password.");
                }

                return profile.getUid();

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<LoginResult> authenticateAnyUser(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        return authenticateUser(normalizedEmail, password)
                .thenApply(uid -> new LoginResult(uid, "PATIENT"))
                .handle((result, ex) -> result)
                .thenCompose(result -> {
                    if (result != null) {
                        return CompletableFuture.completedFuture(result);
                    }

                    return authenticateDoctor(normalizedEmail, password)
                            .thenApply(uid -> new LoginResult(uid, "DOCTOR"));
                });
    }

    public CompletableFuture<DoctorProfile> getDoctorProfile(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ApiFuture<DocumentSnapshot> future = firestore.collection(DOCTORS_COLLECTION).document(uid).get();
                DocumentSnapshot document = future.get();

                if (document.exists()) {
                    DoctorProfile profile = document.toObject(DoctorProfile.class);
                    System.out.println("Doctor profile loaded for UID: " + uid);
                    return profile;
                } else {
                    throw new RuntimeException("Doctor profile not found");
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to retrieve doctor profile: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Authenticates a patient with email and password using custom Firestore verification.
     */
    public CompletableFuture<String> authenticateUser(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Authenticating user with email: " + email);

                ApiFuture<QuerySnapshot> query = firestore.collection(PATIENTS_COLLECTION)
                        .whereEqualTo("email", email)
                        .get();

                QuerySnapshot querySnapshot = query.get();

                if (querySnapshot.isEmpty()) {
                    throw new RuntimeException("No account found with this email address.");
                }

                if (querySnapshot.size() > 1) {
                    throw new RuntimeException("Multiple accounts found with this email. Please contact support.");
                }

                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                PatientProfile profile = document.toObject(PatientProfile.class);

                if (profile == null) {
                    throw new RuntimeException("Failed to load patient profile.");
                }

                if (profile.getPasswordHash() == null || profile.getPasswordSalt() == null) {
                    throw new RuntimeException("Account security data not found. Please contact support.");
                }

                boolean passwordMatches = PasswordHasher.verifyPassword(
                        password,
                        profile.getPasswordHash(),
                        profile.getPasswordSalt()
                );

                if (!passwordMatches) {
                    throw new RuntimeException("Invalid email or password.");
                }

                String uid = profile.getUid();
                System.out.println("User authenticated successfully with UID: " + uid);
                return uid;

            } catch (ExecutionException | InterruptedException e) {
                System.err.println("Authentication database error: " + e.getMessage());
                throw new RuntimeException("Authentication failed: Database error - " + e.getMessage(), e);
            } catch (RuntimeException e) {
                System.err.println("Authentication failed: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                System.err.println("Unexpected authentication error: " + e.getMessage());
                throw new RuntimeException("Authentication error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieves a patient's profile from Firestore.
     */
    public CompletableFuture<PatientProfile> getPatientProfile(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ApiFuture<DocumentSnapshot> future = firestore.collection(PATIENTS_COLLECTION).document(uid).get();
                DocumentSnapshot document = future.get();

                if (document.exists()) {
                    PatientProfile profile = document.toObject(PatientProfile.class);
                    System.out.println("Patient profile loaded for UID: " + uid);
                    return profile;
                } else {
                    throw new RuntimeException("Patient profile not found");
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to retrieve patient profile: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Updates a patient's profile in Firestore.
     */
    public CompletableFuture<Void> updatePatientProfile(String uid, PatientProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                profile.setUpdatedAt(System.currentTimeMillis());
                ApiFuture<?> future = firestore.collection(PATIENTS_COLLECTION).document(uid).set(profile);
                future.get();
                System.out.println("Patient profile updated for UID: " + uid);
                return null;
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to update patient profile: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Updates a doctor's profile in Firestore.
     */
    public CompletableFuture<Void> updateDoctorProfile(String uid, DoctorProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                profile.setUpdatedAt(System.currentTimeMillis());
                ApiFuture<?> future = firestore.collection(DOCTORS_COLLECTION).document(uid).set(profile);
                future.get();
                System.out.println("Doctor profile updated for UID: " + uid);
                return null;
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to update doctor profile: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieves all doctors from the database.
     */
    public CompletableFuture<List<Doctor>> getAllDoctors() {
        return CompletableFuture.supplyAsync(() -> {
            List<Doctor> doctors = new ArrayList<>();
            try {
                QuerySnapshot snapshot = firestore.collection(DOCTORS_COLLECTION).get().get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Doctor doctor = new Doctor();
                    doctor.setUid(doc.getId());
                    doctor.setName(doc.getString("name"));
                    doctor.setEmail(doc.getString("email"));
                    doctor.setSpecialty(doc.getString("specialty"));
                    doctor.setZip(doc.getString("zip"));
                    doctor.setClinicName(doc.getString("clinicName"));
                    doctor.setCity(doc.getString("city"));
                    doctor.setState(doc.getString("state"));
                    doctor.setAddress(doc.getString("address"));
                    doctor.setAcceptingNewPatients(doc.getBoolean("acceptingNewPatients"));

                    Object availabilityObj = doc.get("availability");
                    if (availabilityObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> availability = (Map<String, String>) availabilityObj;
                        doctor.setAvailability(availability);
                    }

                    doctors.add(doctor);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve doctors: " + e.getMessage(), e);
            }
            return doctors;
        });
    }

    /**
     * Returns all appointments for a doctor on a specific date.
     */
    public CompletableFuture<List<Appointment>> getDoctorAppointmentsForDate(String doctorUid, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            List<Appointment> appointments = new ArrayList<>();

            try {
                long startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("doctorUid", doctorUid)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Appointment appointment = doc.toObject(Appointment.class);
                    if (appointment == null || appointment.getAppointmentDateTime() == null) {
                        continue;
                    }

                    long apptTime = appointment.getAppointmentDateTime();
                    if (apptTime >= startOfDay && apptTime < endOfDay) {
                        appointment.setAppointmentId(doc.getId());
                        appointments.add(appointment);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve doctor appointments: " + e.getMessage(), e);
            }

            return appointments;
        });
    }

    /**
     * Checks whether a specific doctor time slot is available.
     */
    public CompletableFuture<Boolean> isTimeSlotAvailable(String doctorUid, long appointmentDateTime) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDate selectedDate = Instant.ofEpochMilli(appointmentDateTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                List<Appointment> appointments = getDoctorAppointmentsForDate(doctorUid, selectedDate).get();

                for (Appointment appointment : appointments) {
                    if (appointment.getAppointmentDateTime() != null
                            && appointment.getAppointmentDateTime().longValue() == appointmentDateTime
                            && appointment.getStatus() != null
                            && !appointment.getStatus().equalsIgnoreCase("CANCELLED")) {
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to check appointment availability: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Saves a new appointment after checking for conflicts.
     */
    public CompletableFuture<Boolean> saveAppointment(Appointment appointment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (appointment == null) {
                    throw new RuntimeException("Appointment cannot be null.");
                }
                if (appointment.getDoctorUid() == null || appointment.getDoctorUid().isBlank()) {
                    throw new RuntimeException("Doctor ID is required.");
                }
                if (appointment.getPatientUid() == null || appointment.getPatientUid().isBlank()) {
                    throw new RuntimeException("Patient ID is required.");
                }
                if (appointment.getAppointmentDateTime() == null) {
                    throw new RuntimeException("Appointment date/time is required.");
                }

                boolean available = isTimeSlotAvailable(
                        appointment.getDoctorUid(),
                        appointment.getAppointmentDateTime()
                ).get();

                if (!available) {
                    throw new RuntimeException("This slot has already been booked.");
                }

                String appointmentId = firestore.collection(APPOINTMENTS_COLLECTION).document().getId();
                appointment.setAppointmentId(appointmentId);

                if (appointment.getStatus() == null || appointment.getStatus().isBlank()) {
                    appointment.setStatus("SCHEDULED");
                }
                if (appointment.getCreatedAt() == null) {
                    appointment.setCreatedAt(System.currentTimeMillis());
                }

                firestore.collection(APPOINTMENTS_COLLECTION)
                        .document(appointmentId)
                        .set(appointment)
                        .get();

                System.out.println("Appointment saved: " + appointmentId);
                return true;

            } catch (Exception e) {
                throw new RuntimeException("Failed to save appointment: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Books an appointment with conflict prevention.
     *
     * Returns appointment ID for backward compatibility.
     */
    public CompletableFuture<String> bookAppointment(Appointment appointment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (appointment == null) {
                    throw new RuntimeException("Appointment cannot be null.");
                }
                if (appointment.getDoctorUid() == null || appointment.getDoctorUid().isBlank()) {
                    throw new RuntimeException("Doctor ID is required.");
                }
                if (appointment.getPatientUid() == null || appointment.getPatientUid().isBlank()) {
                    throw new RuntimeException("Patient ID is required.");
                }
                if (appointment.getAppointmentDateTime() == null) {
                    throw new RuntimeException("Appointment date/time is required.");
                }

                boolean available = isTimeSlotAvailable(
                        appointment.getDoctorUid(),
                        appointment.getAppointmentDateTime()
                ).get();

                if (!available) {
                    throw new RuntimeException("This slot has already been booked.");
                }

                String appointmentId = firestore.collection(APPOINTMENTS_COLLECTION).document().getId();
                appointment.setAppointmentId(appointmentId);

                if (appointment.getStatus() == null || appointment.getStatus().isBlank()) {
                    appointment.setStatus("SCHEDULED");
                }
                if (appointment.getCreatedAt() == null) {
                    appointment.setCreatedAt(System.currentTimeMillis());
                }

                firestore.collection(APPOINTMENTS_COLLECTION)
                        .document(appointmentId)
                        .set(appointment)
                        .get();

                System.out.println("Appointment booked: " + appointmentId);
                return appointmentId;

            } catch (Exception e) {
                throw new RuntimeException("Failed to book appointment: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieves appointments for a patient.
     */
    public CompletableFuture<List<Appointment>> getPatientAppointments(String patientUid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Appointment> appointments = new ArrayList<>();
            try {
                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("patientUid", patientUid)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Appointment appointment = doc.toObject(Appointment.class);
                    if (appointment != null) {
                        appointment.setAppointmentId(doc.getId());
                        appointments.add(appointment);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve appointments: " + e.getMessage(), e);
            }
            return appointments;
        });
    }

    /**
     * Retrieves appointments for a doctor.
     */
    public CompletableFuture<List<Appointment>> getDoctorAppointments(String doctorUid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Appointment> appointments = new ArrayList<>();
            try {
                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("doctorUid", doctorUid)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Appointment appointment = doc.toObject(Appointment.class);
                    if (appointment != null) {
                        appointment.setAppointmentId(doc.getId());
                        appointments.add(appointment);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve doctor appointments: " + e.getMessage(), e);
            }
            return appointments;
        });
    }

    /**
     * Update an existing appointment.
     */
    public CompletableFuture<Void> updateAppointment(Appointment appointment) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (appointment.getAppointmentId() == null || appointment.getAppointmentId().isEmpty()) {
                    throw new RuntimeException("Appointment ID is required for update");
                }

                firestore.collection(APPOINTMENTS_COLLECTION)
                        .document(appointment.getAppointmentId())
                        .set(appointment)
                        .get();

            } catch (Exception e) {
                throw new RuntimeException("Failed to update appointment: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Handles Firebase Authentication exceptions and returns user-friendly error messages.
     */
    private String handleAuthException(FirebaseAuthException e) {
        String details = e.getMessage().toLowerCase();

        if (details.contains("email-already-exists")) {
            return "Email is already registered.";
        } else if (details.contains("invalid-password") || details.contains("weak-password")) {
            return "Password must be at least 6 characters.";
        } else if (details.contains("invalid-email")) {
            return "Invalid email address.";
        } else if (details.contains("email-already-in-use")) {
            return "This email is already in use.";
        } else if (details.contains("invalid-argument")) {
            return "Invalid input provided.";
        }

        return "Failed to create account: " + e.getMessage();
    }

    /**
     * Handles Firebase authentication error messages from REST API.
     */
    private String handleAuthError(String errorMessage) {
        if (errorMessage.contains("INVALID_LOGIN_CREDENTIALS") || errorMessage.contains("INVALID_PASSWORD")) {
            return "Invalid email or password.";
        } else if (errorMessage.contains("USER_DISABLED")) {
            return "This account has been disabled.";
        } else if (errorMessage.contains("EMAIL_NOT_FOUND")) {
            return "No account found with this email.";
        } else if (errorMessage.contains("INVALID_EMAIL")) {
            return "Invalid email address.";
        } else if (errorMessage.contains("TOO_MANY_ATTEMPTS_LOGIN_RETRY_ACCOUNT")) {
            return "Too many failed login attempts. Please try again later.";
        }
        return "Login failed: " + errorMessage;
    }
}