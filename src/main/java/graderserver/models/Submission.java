package graderserver.models;

public class Submission {
    private int id;
    private Problem problem;
    private String filePath;
    private String language;
    private SubmissionType status;

    public Submission(int id, Problem problem, String filePath, String language, SubmissionType status) {
        this.id = id;
        this.problem = problem;
        this.filePath = filePath;
        this.language = language;
        this.status = status;
    }

    public Problem getProblem() {
        return problem;
    }

    public int getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getLanguage() {
        return language;
    }

    public SubmissionType getStatus() {
        return status;
    }
}
