package rakib.bcs430healthcareproject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produces a stable address key so prescriptions and pharmacy accounts can be matched reliably.
 */
public final class AddressNormalizer {

    private static final Map<String, String> REPLACEMENTS = new LinkedHashMap<>();

    static {
        REPLACEMENTS.put("street", "st");
        REPLACEMENTS.put("st.", "st");
        REPLACEMENTS.put("avenue", "ave");
        REPLACEMENTS.put("ave.", "ave");
        REPLACEMENTS.put("road", "rd");
        REPLACEMENTS.put("rd.", "rd");
        REPLACEMENTS.put("boulevard", "blvd");
        REPLACEMENTS.put("drive", "dr");
        REPLACEMENTS.put("dr.", "dr");
        REPLACEMENTS.put("lane", "ln");
        REPLACEMENTS.put("court", "ct");
        REPLACEMENTS.put("place", "pl");
        REPLACEMENTS.put("parkway", "pkwy");
        REPLACEMENTS.put("highway", "hwy");
        REPLACEMENTS.put("suite", "ste");
        REPLACEMENTS.put("apartment", "apt");
        REPLACEMENTS.put("north", "n");
        REPLACEMENTS.put("south", "s");
        REPLACEMENTS.put("east", "e");
        REPLACEMENTS.put("west", "w");
    }

    private AddressNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            normalized = normalized.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }

        return normalized.replaceAll("\\s+", " ").trim();
    }
}
