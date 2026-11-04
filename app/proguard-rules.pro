# Firestore mappings use explicit field access; retain Firebase SDK consumer rules.
# Strip debug/verbose logs from release without removing operational warnings.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
