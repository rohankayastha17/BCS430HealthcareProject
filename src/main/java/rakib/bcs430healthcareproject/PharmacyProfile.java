package rakib.bcs430healthcareproject;

public class PharmacyProfile {

    private String uid;
    private String pharmacyName;
    private String email;
    private String phoneNumber;
    private String addressLine;
    private String city;
    private String state;
    private String zip;
    private String fullAddress;
    private String addressNormalized;
    private String passwordHash;
    private String passwordSalt;
    private String role;
    private Long createdAt;
    private Long updatedAt;

    public PharmacyProfile() {
    }

    public PharmacyProfile(String uid,
                           String pharmacyName,
                           String email,
                           String phoneNumber,
                           String addressLine,
                           String city,
                           String state,
                           String zip) {
        this.uid = uid;
        this.pharmacyName = pharmacyName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.fullAddress = buildFullAddress(addressLine, city, state, zip);
        this.addressNormalized = AddressNormalizer.normalize(fullAddress);
    }

    public static String buildFullAddress(String addressLine, String city, String state, String zip) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, addressLine);
        appendPart(builder, city);
        appendPart(builder, state);
        appendPart(builder, zip);
        return builder.toString();
    }

    private static void appendPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
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

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
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

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getAddressNormalized() {
        return addressNormalized;
    }

    public void setAddressNormalized(String addressNormalized) {
        this.addressNormalized = addressNormalized;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
}
