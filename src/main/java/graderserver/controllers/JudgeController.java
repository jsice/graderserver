package graderserver.controllers;

import graderserver.databaes.SubmissionConnector;
import graderserver.models.Problem;
import graderserver.models.Submission;
import graderserver.models.SubmissionType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

public class JudgeController extends Thread {

    private SubmissionConnector db;
    private String projectPath;
    private int id;

    JudgeController(String projectPath, int id) throws ClassNotFoundException {
        this.projectPath = projectPath;
        String dbPath = projectPath + "/database/database.sqlite";
        this.db = new SubmissionConnector(dbPath);
        this.id = id;
    }

    private static boolean isAlive(Process p) {
        p.exitValue();
        return true;
    }

    @Override
    public void run() {
        Submission submission = this.db.getSubmissionById(id);
        try {
            Problem problem = submission.getProblem();
            String problemPath = this.projectPath + "/storage/app/problems/" + problem.getId() + "/problemtestset/";
            String submissionPath = this.projectPath + "/storage/app/submissions/";
            String codePath = submissionPath + submission.getFilePath();
            String objectPath = submissionPath + submission.getFilePath().replaceAll("/.*", "/");
            String objectName = submission.getFilePath().replaceAll(".*/", "").replaceAll("\\..*", "");
            System.out.println(submission.getStatus());
            int timeout = problem.getTime();
            SubmissionType newStatus = SubmissionType.CTA;
            System.out.println(submission.getLanguage());



            if (!submission.getLanguage().equals(submission.getFilePath().replaceAll(".*\\.", ""))) {
                System.out.printf("error! lang: %s, ext: %s\n", submission.getLanguage(), submission.getFilePath().replaceAll(".*\\.", ""));
                this.db.updateSubmissionStatus(submission.getId(), SubmissionType.CTA);
                return;
            }

            //compile code
            String compileError = null;
            if (submission.getLanguage().equals("c")) {
                String command = String.format("gcc %s -o %s", codePath, objectPath + objectName + ".exe");
                Process compileProcess = Runtime.getRuntime().exec(command);
                BufferedReader compileErrorReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                compileError = compileErrorReader.readLine();
                compileErrorReader.close();
            } else if (submission.getLanguage().equals("cpp")) {
                String command = String.format("g++ %s -o %s", codePath, objectPath + objectName + ".exe");
                Process compileProcess = Runtime.getRuntime().exec(command);
                BufferedReader compileErrorReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                compileError = compileErrorReader.readLine();
                compileErrorReader.close();
            } else if (submission.getLanguage().equals("java")) {
                String command = String.format("javac %s", codePath);
                Process compileProcess = Runtime.getRuntime().exec(command);
                BufferedReader compileErrorReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                compileError = compileErrorReader.readLine();
                compileErrorReader.close();

                if (new File(objectPath + objectName + ".class").exists()) {
                    compileError = null;
                }
            }
            if (compileError == null) {
                for (int i = 0; i < problem.getInputFiles().size(); i++) {
                    String inputPath = problemPath + problem.getInputFiles().get(i);
                    String outputPath = problemPath + problem.getOutputFiles().get(i);
                    String runtimeError;
                    String output;
                    String command = "";
                    if (submission.getLanguage().equals("c")) {
                        command = String.format("%s", objectPath + objectName + ".exe");
                    } else if (submission.getLanguage().equals("cpp")) {
                        command = String.format("%s", objectPath + objectName + ".exe");
                    } else if (submission.getLanguage().equals("java")) {
                        command = String.format("java -cp %s %s", objectPath, objectName);
                    }

                    Process runtimeProcess = Runtime.getRuntime().exec(command);

                    OutputStream outputStream = runtimeProcess.getOutputStream();
                    BufferedReader runtimeErrorReader = new BufferedReader(new InputStreamReader(runtimeProcess.getErrorStream()));
                    BufferedReader outputReader = new BufferedReader(new InputStreamReader(runtimeProcess.getInputStream()));

                    Files.copy(Paths.get(inputPath), outputStream);

                    outputStream.flush();

                    Instant start = Instant.now();

                    Worker worker = new Worker(runtimeProcess);
                    worker.start();
                    try {
                        worker.join(timeout * 1000);
                        if (worker.exit == null) {
                            Instant end = Instant.now();
                            System.out.println(Duration.between(start, end));
                            newStatus = SubmissionType.TLE;
                            break;
                        } else {
                            System.out.println(Duration.between(start, worker.end));
                        }
                    } catch (InterruptedException ex) {
                        worker.interrupt();
                        Thread.currentThread().interrupt();
                    } finally {
                        runtimeProcess.destroyForcibly();
                    }
                    runtimeError = runtimeErrorReader.readLine();
                    if (runtimeError != null) {
                        System.out.println(runtimeError);
                        newStatus = SubmissionType.RTE;
                        break;
                    } else {
                        output = "";
                        String line;
                        while ((line = outputReader.readLine()) != null) {
                            output += line + "\n";
                        }

                        if (compareStringToFile(output, outputPath)) {
                            newStatus = SubmissionType.YES;
                        } else {
                            newStatus = SubmissionType.WAE;
                        }
                    }

                }
            } else {
                System.out.println("compile error: " + compileError);
                newStatus = SubmissionType.CPE;
            }

            System.out.println(newStatus);
            this.db.updateSubmissionStatus(submission.getId(), newStatus);
        } catch (IOException e) {
            e.printStackTrace();
            this.db.updateSubmissionStatus(submission.getId(), SubmissionType.CTA);
        }
    }

    private String fileToString(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private boolean compareStringToFile(String text, String filePath) throws IOException {
        String s1 = text.trim();
        String s2 = fileToString(filePath).trim();
        return s1.equals(s2);
    }

    private static class Worker extends Thread {
        private final Process process;
        private Integer exit;
        private Instant end;

        private Worker(Process process) {
            this.process = process;
        }

        public void run() {
            try {
                exit = process.waitFor();
                end = Instant.now();
            } catch (InterruptedException ignore) {
                return;
            }
        }
    }
}
