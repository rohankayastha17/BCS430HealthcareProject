package rakib.bcs430healthcareproject;

import java.util.List;

public final class InsuranceSupport {

    private static final List<String> COMMON_INSURANCE_PROVIDERS = List.of(
            "Aetna",
            "Anthem Blue Cross Blue Shield",
            "Blue Cross Blue Shield",
            "Cigna",
            "EmblemHealth",
            "Empire BlueCross BlueShield",
            "Fidelis Care",
            "Florida Blue",
            "Healthfirst",
            "Highmark",
            "Horizon Blue Cross Blue Shield",
            "Humana",
            "Kaiser Permanente",
            "Molina Healthcare",
            "Oscar",
            "UnitedHealthcare",
            "Wellcare"
    );

    private InsuranceSupport() {
    }

    public static List<String> commonInsuranceProviders() {
        return COMMON_INSURANCE_PROVIDERS;
    }
}
