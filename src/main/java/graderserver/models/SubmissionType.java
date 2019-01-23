package graderserver.models;

public enum SubmissionType {
    YES("YES"),
    CPE("NO:CompilationError"),
    RTE("NO:RunTimeError"),
    WAE("NO:WrongAnswer"),
    TLE("NO:TimeLimitExceeded"),
    CTA("NO:ContactTA"),
    DEL("DELETED"),
    PND("PENDING");

    private String value;
    private SubmissionType(String value)
    {
        this.value = value;
    }

    public String toString()
    {
        return this.value; //will return , or ' instead of COMMA or APOSTROPHE
    }

    public static SubmissionType fromString(String text) {
        for (SubmissionType type : SubmissionType.values()) {
            if (type.value.equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
