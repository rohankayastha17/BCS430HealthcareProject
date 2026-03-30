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
    private static final String PHARMACIES_COLLECTION = "pharmacies";
    private static final String APPOINTMENTS_COLLECTION = "appointments";
    private static final String PRESCRIPTIONS_COLLECTION = "prescriptions";
    private static final String MESSAGES_COLLECTION = "messages";

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

    public CompletableFuture<String> createPharmacy(
            String email,
            String password,
            String pharmacyName,
            String phoneNumber,
            String addressLine,
            String city,
            String state,
            String zip
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
                String fullAddress = PharmacyProfile.buildFullAddress(addressLine, city, state, zip);
                String normalizedAddress = AddressNormalizer.normalize(fullAddress);

                QuerySnapshot existingLocation = firestore.collection(PHARMACIES_COLLECTION)
                        .whereEqualTo("addressNormalized", normalizedAddress)
                        .get()
                        .get();

                if (!existingLocation.isEmpty()) {
                    throw new RuntimeException("That pharmacy location has already been claimed.");
                }

                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(normalizedEmail)
                        .setPassword(password)
                        .setDisplayName(pharmacyName);

                UserRecord userRecord = auth.createUser(request);
                String uid = userRecord.getUid();

                PharmacyProfile profile = new PharmacyProfile(
                        uid,
                        pharmacyName,
                        normalizedEmail,
                        phoneNumber,
                        addressLine,
                        city,
                        state,
                        zip
                );

                String passwordSalt = PasswordHasher.generateSalt();
                String passwordHash = PasswordHasher.hashPassword(password, passwordSalt);
                profile.setPasswordHash(passwordHash);
                profile.setPasswordSalt(passwordSalt);
                profile.setRole("PHARMACY");
                profile.setCreatedAt(System.currentTimeMillis());
                profile.setUpdatedAt(System.currentTimeMillis());

                firestore.collection(PHARMACIES_COLLECTION).document(uid).set(profile).get();

                Map<String, Object> userDoc = new HashMap<>();
                userDoc.put("uid", uid);
                userDoc.put("name", pharmacyName);
                userDoc.put("email", normalizedEmail);
                userDoc.put("role", "PHARMACY");
                userDoc.put("createdAt", System.currentTimeMillis());

                firestore.collection(USERS_COLLECTION).document(uid).set(userDoc).get();

                return uid;
            } catch (FirebaseAuthException e) {
                throw new RuntimeException(handleAuthException(e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Pharmacy creation interrupted.", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to save pharmacy profile: " + e.getMessage(), e);
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

    public CompletableFuture<String> authenticatePharmacy(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ApiFuture<QuerySnapshot> query = firestore.collection(PHARMACIES_COLLECTION)
                        .whereEqualTo("email", email)
                        .get();

                QuerySnapshot querySnapshot = query.get();

                if (querySnapshot.isEmpty()) {
                    throw new RuntimeException("No account found with this email address.");
                }

                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                PharmacyProfile profile = document.toObject(PharmacyProfile.class);

                if (profile == null) {
                    throw new RuntimeException("Failed to load pharmacy profile.");
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

    public CompletableFuture<PharmacyProfile> getPharmacyProfile(String uid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ApiFuture<DocumentSnapshot> future = firestore.collection(PHARMACIES_COLLECTION).document(uid).get();
                DocumentSnapshot document = future.get();

                if (document.exists()) {
                    PharmacyProfile profile = document.toObject(PharmacyProfile.class);
                    if (profile != null && (profile.getAddressNormalized() == null || profile.getAddressNormalized().isBlank())) {
                        profile.setAddressNormalized(AddressNormalizer.normalize(profile.getFullAddress()));
                    }
                    return profile;
                } else {
                    throw new RuntimeException("Pharmacy profile not found");
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to retrieve pharmacy profile: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<PharmacyProfile>> getAllPharmacies() {
        return CompletableFuture.supplyAsync(() -> {
            List<PharmacyProfile> pharmacies = new ArrayList<>();
            try {
                QuerySnapshot snapshot = firestore.collection(PHARMACIES_COLLECTION).get().get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    PharmacyProfile profile = doc.toObject(PharmacyProfile.class);
                    if (profile == null) {
                        continue;
                    }
                    if (profile.getUid() == null || profile.getUid().isBlank()) {
                        profile.setUid(doc.getId());
                    }
                    if (profile.getFullAddress() == null || profile.getFullAddress().isBlank()) {
                        profile.setFullAddress(PharmacyProfile.buildFullAddress(
                                profile.getAddressLine(),
                                profile.getCity(),
                                profile.getState(),
                                profile.getZip()
                        ));
                    }
                    if (profile.getAddressNormalized() == null || profile.getAddressNormalized().isBlank()) {
                        profile.setAddressNormalized(AddressNormalizer.normalize(profile.getFullAddress()));
                    }
                    pharmacies.add(profile);
                }

                pharmacies.sort((left, right) -> {
                    String leftName = left.getPharmacyName() != null ? left.getPharmacyName() : "";
                    String rightName = right.getPharmacyName() != null ? right.getPharmacyName() : "";
                    return leftName.compareToIgnoreCase(rightName);
                });
                return pharmacies;
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve pharmacies: " + e.getMessage(), e);
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
                    doctors.add(mapDoctorDocument(doc));
                }

                // If no doctors, add a test doctor
                if (doctors.isEmpty()) {
                    Doctor testDoctor = new Doctor();
                    testDoctor.setUid("test123");
                    testDoctor.setName("Dr. Test");
                    testDoctor.setSpecialty("General Medicine");
                    testDoctor.setZip("10001");
                    testDoctor.setClinicName("Test Clinic");
                    testDoctor.setCity("New York");
                    testDoctor.setState("NY");
                    testDoctor.setAddress("123 Test St");
                    testDoctor.setAcceptingNewPatients(true);
                    testDoctor.setAvailability(new HashMap<>());
                    doctors.add(testDoctor);
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve doctors: " + e.getMessage(), e);
            }
            return doctors;
        });
    }

    /**
     * Retrieves a single doctor record by UID.
     */
    public CompletableFuture<Doctor> getDoctorByUid(String doctorUid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot doc = firestore.collection(DOCTORS_COLLECTION)
                        .document(doctorUid)
                        .get()
                        .get();

                if (!doc.exists()) {
                    throw new RuntimeException("Doctor not found.");
                }

                return mapDoctorDocument(doc);
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve doctor: " + e.getMessage(), e);
            }
        });
    }

    private Doctor mapDoctorDocument(DocumentSnapshot doc) {
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
        doctor.setPhone(doc.getString("phone"));
        doctor.setAcceptingNewPatients(doc.getBoolean("acceptingNewPatients"));
        doctor.setHours(doc.getString("hours"));
        doctor.setInsuranceInfo(doc.getString("insuranceInfo"));
        doctor.setBio(doc.getString("bio"));
        doctor.setVisitType(doc.getString("visitType"));
        doctor.setNotes(doc.getString("notes"));

        Object availabilityObj = doc.get("availability");
        if (availabilityObj instanceof Map<?, ?> rawMap) {
            Map<String, String> availability = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    availability.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            doctor.setAvailability(availability);
        }

        return doctor;
    }

    /**
     * Returns all appointments for a doctor on a specific date.
     */
    public CompletableFuture<List<Appointment>> getDoctorAppointmentsForDate(String doctorUid, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            List<Appointment> appointments = new ArrayList<>();

            try {
                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("doctorUid", doctorUid)
                        .whereEqualTo("appointmentDate", date.toString())
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
     * Returns all booked slot strings like "09:30 AM" for a doctor on a given date.
     */
    public CompletableFuture<List<String>> getBookedTimesForDoctorAndDate(String doctorUid, String appointmentDate) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> bookedTimes = new ArrayList<>();

            try {
                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("doctorUid", doctorUid)
                        .whereEqualTo("appointmentDate", appointmentDate)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Appointment appointment = doc.toObject(Appointment.class);
                    if (appointment == null) {
                        continue;
                    }

                    String status = appointment.getStatus();
                    if (status != null && status.equalsIgnoreCase("CANCELLED")) {
                        continue;
                    }

                    String slot = appointment.getAppointmentSlot();
                    if (slot != null && !slot.isBlank()) {
                        bookedTimes.add(slot);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve booked times: " + e.getMessage(), e);
            }

            return bookedTimes;
        });
    }

    /**
     * Checks whether a specific doctor date/slot is still available.
     */
    public CompletableFuture<Boolean> isSlotStillAvailable(String doctorUid, String appointmentDate, String appointmentSlot) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("doctorUid", doctorUid)
                        .whereEqualTo("appointmentDate", appointmentDate)
                        .whereEqualTo("appointmentSlot", appointmentSlot)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Appointment appointment = doc.toObject(Appointment.class);
                    if (appointment == null) {
                        continue;
                    }

                    String status = appointment.getStatus();
                    if (status == null || !status.equalsIgnoreCase("CANCELLED")) {
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to check slot availability: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Checks whether a specific doctor time slot is available.
     * Legacy timestamp-based check kept for compatibility.
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
                String appointmentId = internalSaveAppointment(appointment);
                return appointmentId != null && !appointmentId.isBlank();
            } catch (Exception e) {
                throw new RuntimeException("Failed to save appointment: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Books an appointment and returns the created appointment ID.
     */
    public CompletableFuture<String> bookAppointment(Appointment appointment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return internalSaveAppointment(appointment);
            } catch (Exception e) {
                throw new RuntimeException("Failed to book appointment: " + e.getMessage(), e);
            }
        });
    }

    private String internalSaveAppointment(Appointment appointment) throws Exception {
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

        // Ensure date + slot are filled for Firebase queries
        if ((appointment.getAppointmentDate() == null || appointment.getAppointmentDate().isBlank())
                || (appointment.getAppointmentSlot() == null || appointment.getAppointmentSlot().isBlank())) {

            if (appointment.getAppointmentTime() != null && appointment.getAppointmentTime().contains(" ")) {
                String[] parts = appointment.getAppointmentTime().split(" ", 2);
                if (parts.length == 2) {
                    appointment.setAppointmentDate(parts[0]);
                    appointment.setAppointmentSlot(parts[1]);
                }
            }
        }

        if (appointment.getAppointmentDate() == null || appointment.getAppointmentDate().isBlank()) {
            LocalDate derivedDate = Instant.ofEpochMilli(appointment.getAppointmentDateTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            appointment.setAppointmentDate(derivedDate.toString());
        }

        if (appointment.getAppointmentSlot() == null || appointment.getAppointmentSlot().isBlank()) {
            throw new RuntimeException("Appointment time slot is required.");
        }

        boolean available = isSlotStillAvailable(
                appointment.getDoctorUid(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentSlot()
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
        return appointmentId;
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
     * Retrieves unique patients who have appointments with a doctor.
     */
    public CompletableFuture<List<PatientProfile>> getDoctorPatients(String doctorUid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, PatientProfile> uniquePatients = new HashMap<>();
            try {
                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("doctorUid", doctorUid)
                        .get()
                        .get();

                for (DocumentSnapshot appointmentDoc : snapshot.getDocuments()) {
                    String patientUid = appointmentDoc.getString("patientUid");
                    if (patientUid == null || patientUid.isBlank() || uniquePatients.containsKey(patientUid)) {
                        continue;
                    }

                    DocumentSnapshot patientDoc = firestore.collection(PATIENTS_COLLECTION)
                            .document(patientUid)
                            .get()
                            .get();

                    if (!patientDoc.exists()) {
                        continue;
                    }

                    PatientProfile profile = patientDoc.toObject(PatientProfile.class);
                    if (profile != null) {
                        uniquePatients.put(patientUid, profile);
                    }
                }

                List<PatientProfile> patients = new ArrayList<>(uniquePatients.values());
                patients.sort((left, right) -> {
                    String leftName = left.getName() != null ? left.getName() : "";
                    String rightName = right.getName() != null ? right.getName() : "";
                    return leftName.compareToIgnoreCase(rightName);
                });
                return patients;
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve doctor patients: " + e.getMessage(), e);
            }
        });
    }
    /**
     * Retrieves unique doctors a patient has appointments with.
     */
    public CompletableFuture<List<Doctor>> getDoctorsForPatient(String patientUid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Doctor> uniqueDoctors = new HashMap<>();

            try {
                QuerySnapshot snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("patientUid", patientUid)
                        .get()
                        .get();

                for (DocumentSnapshot appointmentDoc : snapshot.getDocuments()) {
                    String doctorUid = appointmentDoc.getString("doctorUid");

                    if (doctorUid == null || doctorUid.isBlank() || uniqueDoctors.containsKey(doctorUid)) {
                        continue;
                    }

                    DocumentSnapshot doctorDoc = firestore.collection(DOCTORS_COLLECTION)
                            .document(doctorUid)
                            .get()
                            .get();

                    if (!doctorDoc.exists()) {
                        continue;
                    }

                    Doctor doctor = mapDoctorDocument(doctorDoc);
                    if (doctor != null) {
                        uniqueDoctors.put(doctorUid, doctor);
                    }
                }

                List<Doctor> doctors = new ArrayList<>(uniqueDoctors.values());

                doctors.sort((left, right) -> {
                    String leftName = left.getName() != null ? left.getName() : "";
                    String rightName = right.getName() != null ? right.getName() : "";
                    return leftName.compareToIgnoreCase(rightName);
                });

                return doctors;

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve patient doctors: " + e.getMessage(), e);
            }
        });
    }
    /**
     * Saves a prescription record.
     */
    public CompletableFuture<String> savePrescription(Prescription prescription) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (prescription == null) {
                    throw new RuntimeException("Prescription cannot be null.");
                }
                if (prescription.getDoctorUid() == null || prescription.getDoctorUid().isBlank()) {
                    throw new RuntimeException("Doctor ID is required.");
                }
                if (prescription.getPatientUid() == null || prescription.getPatientUid().isBlank()) {
                    throw new RuntimeException("Patient ID is required.");
                }
                if (prescription.getPharmacyAddress() == null || prescription.getPharmacyAddress().isBlank()) {
                    throw new RuntimeException("Pharmacy address is required.");
                }
                if (prescription.getPharmacyPhoneNumber() == null || prescription.getPharmacyPhoneNumber().isBlank()) {
                    throw new RuntimeException("Pharmacy phone number is required.");
                }
                if (prescription.getMedicationInformation() == null || prescription.getMedicationInformation().isBlank()) {
                    throw new RuntimeException("Medication information is required.");
                }
                if (prescription.getMedicationName() == null || prescription.getMedicationName().isBlank()) {
                    throw new RuntimeException("Medication name is required.");
                }
                if (prescription.getDosage() == null || prescription.getDosage().isBlank()) {
                    throw new RuntimeException("Dosage is required.");
                }
                if (prescription.getQuantity() == null || prescription.getQuantity().isBlank()) {
                    throw new RuntimeException("Quantity is required.");
                }
                if (prescription.getRefillDetails() == null || prescription.getRefillDetails().isBlank()) {
                    throw new RuntimeException("Refill details are required.");
                }
                if (prescription.getRemainingRefills() == null) {
                    Integer parsedRemainingRefills = PrescriptionRefillSupport.parseRemainingRefills(prescription.getRefillDetails());
                    if (parsedRemainingRefills == null) {
                        throw new RuntimeException("Refill details must include a valid refill count.");
                    }
                    prescription.setRemainingRefills(parsedRemainingRefills);
                    prescription.setRefillDetails(PrescriptionRefillSupport.formatRemainingRefills(parsedRemainingRefills));
                }
                if (prescription.getInstructions() == null || prescription.getInstructions().isBlank()) {
                    throw new RuntimeException("Prescription instructions are required.");
                }

                String prescriptionId = firestore.collection(PRESCRIPTIONS_COLLECTION).document().getId();
                prescription.setPrescriptionId(prescriptionId);

                if (prescription.getStatus() == null || prescription.getStatus().isBlank()) {
                    prescription.setStatus(Prescription.STATUS_SENT);
                }
                if (prescription.getCreatedAt() == null) {
                    prescription.setCreatedAt(System.currentTimeMillis());
                }
                if (prescription.getRefillRequested() == null) {
                    prescription.setRefillRequested(false);
                }
                if (prescription.getPharmacyAddressNormalized() == null || prescription.getPharmacyAddressNormalized().isBlank()) {
                    prescription.setPharmacyAddressNormalized(AddressNormalizer.normalize(prescription.getPharmacyAddress()));
                }

                firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .document(prescriptionId)
                        .set(prescription)
                        .get();

                return prescriptionId;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save prescription: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Saves a doctor-patient message.
     */
    public CompletableFuture<String> saveMessage(Message message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (message == null) {
                    throw new RuntimeException("Message cannot be null.");
                }
                if (message.getDoctorUid() == null || message.getDoctorUid().isBlank()) {
                    throw new RuntimeException("Doctor ID is required.");
                }
                if (message.getPatientUid() == null || message.getPatientUid().isBlank()) {
                    throw new RuntimeException("Patient ID is required.");
                }
                if (message.getMessageText() == null || message.getMessageText().isBlank()) {
                    throw new RuntimeException("Message text is required.");
                }

                String messageId = firestore.collection(MESSAGES_COLLECTION).document().getId();
                message.setMessageId(messageId);

                if (message.getCreatedAt() == null) {
                    message.setCreatedAt(System.currentTimeMillis());
                }

                firestore.collection(MESSAGES_COLLECTION)
                        .document(messageId)
                        .set(message)
                        .get();

                return messageId;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save message: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieves all messages between a doctor and patient.
     */
    public CompletableFuture<List<Message>> getMessagesBetweenDoctorAndPatient(String doctorUid, String patientUid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Message> messages = new ArrayList<>();

            try {
                QuerySnapshot snapshot = firestore.collection(MESSAGES_COLLECTION)
                        .whereEqualTo("doctorUid", doctorUid)
                        .whereEqualTo("patientUid", patientUid)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Message message = doc.toObject(Message.class);
                    if (message != null) {
                        message.setMessageId(doc.getId());
                        messages.add(message);
                    }
                }

                messages.sort((a, b) -> Long.compare(
                        a.getCreatedAt() != null ? a.getCreatedAt() : 0L,
                        b.getCreatedAt() != null ? b.getCreatedAt() : 0L
                ));

                return messages;
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve messages: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Prescription>> getPatientPrescriptions(String patientUid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Prescription> prescriptions = new ArrayList<>();
            try {
                QuerySnapshot snapshot = firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .whereEqualTo("patientUid", patientUid)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Prescription prescription = doc.toObject(Prescription.class);
                    if (prescription != null) {
                        prescription.setPrescriptionId(doc.getId());
                        prescriptions.add(prescription);
                    }
                }

                prescriptions.sort((left, right) -> Long.compare(
                        right.getCreatedAt() != null ? right.getCreatedAt() : 0L,
                        left.getCreatedAt() != null ? left.getCreatedAt() : 0L
                ));
                return prescriptions;
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve patient prescriptions: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Prescription>> getAllPrescriptions() {
        return CompletableFuture.supplyAsync(() -> {
            List<Prescription> prescriptions = new ArrayList<>();
            try {
                QuerySnapshot snapshot = firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Prescription prescription = doc.toObject(Prescription.class);
                    if (prescription != null) {
                        prescription.setPrescriptionId(doc.getId());
                        prescriptions.add(prescription);
                    }
                }

                prescriptions.sort((left, right) -> Long.compare(
                        right.getCreatedAt() != null ? right.getCreatedAt() : 0L,
                        left.getCreatedAt() != null ? left.getCreatedAt() : 0L
                ));
                return prescriptions;
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve prescriptions: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Prescription>> getPrescriptionsForPharmacy(String pharmacyAddressNormalized) {
        return CompletableFuture.supplyAsync(() -> {
            List<Prescription> prescriptions = new ArrayList<>();
            try {
                QuerySnapshot snapshot = firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .whereEqualTo("pharmacyAddressNormalized", pharmacyAddressNormalized)
                        .get()
                        .get();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Prescription prescription = doc.toObject(Prescription.class);
                    if (prescription != null) {
                        prescription.setPrescriptionId(doc.getId());
                        prescriptions.add(prescription);
                    }
                }

                prescriptions.sort((left, right) -> Long.compare(
                        right.getCreatedAt() != null ? right.getCreatedAt() : 0L,
                        left.getCreatedAt() != null ? left.getCreatedAt() : 0L
                ));
                return prescriptions;
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve pharmacy prescriptions: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Void> updatePrescription(Prescription prescription) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (prescription == null || prescription.getPrescriptionId() == null || prescription.getPrescriptionId().isBlank()) {
                    throw new RuntimeException("Prescription ID is required for update.");
                }

                firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .document(prescription.getPrescriptionId())
                        .set(prescription)
                        .get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to update prescription: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Void> markPrescriptionFilled(String prescriptionId, PharmacyProfile pharmacyProfile) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (prescriptionId == null || prescriptionId.isBlank()) {
                    throw new RuntimeException("Prescription ID is required.");
                }
                if (pharmacyProfile == null || pharmacyProfile.getAddressNormalized() == null || pharmacyProfile.getAddressNormalized().isBlank()) {
                    throw new RuntimeException("Pharmacy account is required.");
                }

                DocumentSnapshot document = firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .document(prescriptionId)
                        .get()
                        .get();

                if (!document.exists()) {
                    throw new RuntimeException("Prescription not found.");
                }

                Prescription prescription = document.toObject(Prescription.class);
                if (prescription == null) {
                    throw new RuntimeException("Prescription could not be loaded.");
                }

                String prescriptionAddress = prescription.getPharmacyAddressNormalized();
                if (prescriptionAddress == null || prescriptionAddress.isBlank()) {
                    prescriptionAddress = AddressNormalizer.normalize(prescription.getPharmacyAddress());
                    prescription.setPharmacyAddressNormalized(prescriptionAddress);
                }

                if (!pharmacyProfile.getAddressNormalized().equals(prescriptionAddress)) {
                    throw new RuntimeException("This prescription is not assigned to your pharmacy.");
                }

                prescription.setStatus(Prescription.STATUS_FILLED);
                prescription.setFilledAt(System.currentTimeMillis());
                prescription.setFilledBy(pharmacyProfile.getPharmacyName());

                firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .document(prescriptionId)
                        .set(prescription)
                        .get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to fill prescription: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Prescription> refillPrescription(String prescriptionId,
                                                              String actorRole,
                                                              String actorName,
                                                              PharmacyProfile pharmacyProfile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (prescriptionId == null || prescriptionId.isBlank()) {
                    throw new RuntimeException("Prescription ID is required.");
                }

                DocumentSnapshot document = firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .document(prescriptionId)
                        .get()
                        .get();

                if (!document.exists()) {
                    throw new RuntimeException("Prescription not found.");
                }

                Prescription sourcePrescription = document.toObject(Prescription.class);
                if (sourcePrescription == null) {
                    throw new RuntimeException("Prescription could not be loaded.");
                }
                sourcePrescription.setPrescriptionId(document.getId());

                Integer remainingRefills = PrescriptionRefillSupport.getRemainingRefills(sourcePrescription);
                if (remainingRefills == null) {
                    throw new RuntimeException("This prescription does not have a usable refill count.");
                }
                if (remainingRefills <= 0) {
                    throw new RuntimeException("No refills are available for this prescription.");
                }

                if ("PHARMACY".equalsIgnoreCase(actorRole)) {
                    if (pharmacyProfile == null || pharmacyProfile.getAddressNormalized() == null || pharmacyProfile.getAddressNormalized().isBlank()) {
                        throw new RuntimeException("Pharmacy account is required.");
                    }

                    String prescriptionAddress = sourcePrescription.getPharmacyAddressNormalized();
                    if (prescriptionAddress == null || prescriptionAddress.isBlank()) {
                        prescriptionAddress = AddressNormalizer.normalize(sourcePrescription.getPharmacyAddress());
                        sourcePrescription.setPharmacyAddressNormalized(prescriptionAddress);
                    }

                    if (!pharmacyProfile.getAddressNormalized().equals(prescriptionAddress)) {
                        throw new RuntimeException("This prescription is not assigned to your pharmacy.");
                    }
                }

                if ("PHARMACY".equalsIgnoreCase(actorRole)) {
                    if (!Boolean.TRUE.equals(sourcePrescription.getRefillRequested())) {
                        throw new RuntimeException("A doctor must send a refill request before the pharmacy can refill this prescription.");
                    }

                    int updatedRemainingRefills = remainingRefills - 1;
                    sourcePrescription.setRemainingRefills(updatedRemainingRefills);
                    sourcePrescription.setRefillDetails(PrescriptionRefillSupport.formatRemainingRefills(updatedRemainingRefills));
                    sourcePrescription.setStatus(Prescription.STATUS_FILLED);
                    sourcePrescription.setFilledAt(System.currentTimeMillis());
                    sourcePrescription.setFilledBy(actorName != null && !actorName.isBlank() ? actorName : sourcePrescription.getPharmacyName());
                    sourcePrescription.setRefillRequested(false);
                    sourcePrescription.setRefillRequestedBy(null);
                    sourcePrescription.setRefillRequestedAt(null);
                } else {
                    if (Boolean.TRUE.equals(sourcePrescription.getRefillRequested())) {
                        throw new RuntimeException("A refill request has already been sent for this prescription.");
                    }

                    sourcePrescription.setStatus(Prescription.STATUS_REFILL_REQUESTED);
                    sourcePrescription.setRefillRequested(true);
                    sourcePrescription.setRefillRequestedBy(actorName);
                    sourcePrescription.setRefillRequestedAt(System.currentTimeMillis());
                }

                firestore.collection(PRESCRIPTIONS_COLLECTION)
                        .document(sourcePrescription.getPrescriptionId())
                        .set(sourcePrescription)
                        .get();

                return sourcePrescription;
            } catch (Exception e) {
                throw new RuntimeException("Failed to refill prescription: " + e.getMessage(), e);
            }
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