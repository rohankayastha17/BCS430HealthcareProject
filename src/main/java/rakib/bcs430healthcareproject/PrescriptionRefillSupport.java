package rakib.bcs430healthcareproject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PrescriptionRefillSupport {

    private static final Pattern FIRST_NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private PrescriptionRefillSupport() {
    }

    public static Integer parseRemainingRefills(String refillDetails) {
        if (refillDetails == null || refillDetails.isBlank()) {
            return null;
        }

        String normalized = refillDetails.trim().toLowerCase();
        if (normalized.contains("no refill") || normalized.contains("zero refill")) {
            return 0;
        }

        Matcher matcher = FIRST_NUMBER_PATTERN.matcher(normalized);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    public static String formatRemainingRefills(Integer remainingRefills) {
        if (remainingRefills == null) {
            return "Refill count unavailable";
        }
        if (remainingRefills <= 0) {
            return "No refills remaining";
        }
        if (remainingRefills == 1) {
            return "1 refill remaining";
        }
        return remainingRefills + " refills remaining";
    }

    public static Integer getRemainingRefills(Prescription prescription) {
        if (prescription == null) {
            return null;
        }
        if (prescription.getRemainingRefills() != null) {
            return prescription.getRemainingRefills();
        }
        return parseRemainingRefills(prescription.getRefillDetails());
    }
}
