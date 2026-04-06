package rakib.bcs430healthcareproject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PrescriptionRefillSupport {

    private static final Pattern FIRST_NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

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

    public static boolean hasValidRefillInterval(Prescription prescription) {
        return getRefillIntervalDays(prescription) != null;
    }

    public static Integer getRefillIntervalDays(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        Integer intervalDays = prescription.getRefillIntervalDays();
        if (intervalDays == null || intervalDays <= 0) {
            return null;
        }
        return intervalDays;
    }

    public static String formatRefillInterval(Integer intervalDays) {
        if (intervalDays == null || intervalDays <= 0) {
            return "Refill interval unavailable";
        }
        if (intervalDays == 1) {
            return "Refill every 1 day";
        }
        return "Refill every " + intervalDays + " days";
    }

    public static Long calculateNextRefillEligibleAt(Long filledAt, Integer intervalDays) {
        if (filledAt == null || intervalDays == null || intervalDays <= 0) {
            return null;
        }
        return filledAt + (intervalDays * MILLIS_PER_DAY);
    }

    public static Long getNextRefillEligibleAt(Prescription prescription) {
        if (prescription == null) {
            return null;
        }
        if (prescription.getNextRefillEligibleAt() != null) {
            return prescription.getNextRefillEligibleAt();
        }
        return calculateNextRefillEligibleAt(
                prescription.getFilledAt(),
                getRefillIntervalDays(prescription)
        );
    }

    public static boolean isRefillEligibleNow(Prescription prescription) {
        Long nextEligibleAt = getNextRefillEligibleAt(prescription);
        return nextEligibleAt != null && System.currentTimeMillis() >= nextEligibleAt;
    }
}
