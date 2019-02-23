package graderserver.controllers;

import graderserver.databaes.SubmissionConnector;
import graderserver.models.Problem;
import graderserver.models.Submission;
import graderserver.models.SubmissionType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

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
                    String[] command = new String[]{"nocmd"};
                    if (submission.getLanguage().equals("c")) {
                        command = new String[] {objectPath + objectName + ".exe"};
                    } else if (submission.getLanguage().equals("cpp")) {
                        command = new String[] {objectPath + objectName + ".exe"};
                    } else if (submission.getLanguage().equals("java")) {
                        command = new String[] {"java", "-cp", objectPath, objectName};
                    }

                    ProcessBuilder runtimeProcessBuilder = new ProcessBuilder(command);
                    runtimeProcessBuilder.redirectInput(new File(inputPath));
                    runtimeProcessBuilder.redirectOutput(new File(objectPath + "output.txt"));
                    runtimeProcessBuilder.redirectError(new File(objectPath + "runtimeError.txt"));


                    Process runtimeProcess = runtimeProcessBuilder.start();

                    long startTime = System.currentTimeMillis();
                    try {
                        if (runtimeProcess.waitFor(timeout, TimeUnit.SECONDS)) {
                            long endTime = System.currentTimeMillis();
                            System.out.println(endTime - startTime);
                        } else {
                            long endTime = System.currentTimeMillis();
                            System.out.println(endTime - startTime);
                            newStatus = SubmissionType.TLE;
                            break;
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } finally {
                        runtimeProcess.destroyForcibly();
                    }
                    BufferedReader runtimeErrorReader = new BufferedReader(new FileReader(new File(objectPath + "runtimeError.txt")));
                    BufferedReader outputReader = new BufferedReader(new FileReader(new File(objectPath + "output.txt")));
                    runtimeError = runtimeErrorReader.readLine();
                    if (runtimeError != null) {
                        System.out.println(runtimeError);
                        newStatus = SubmissionType.RTE;
                        break;
                    } else {
                        if (compareFileToFile(objectPath + "output.txt", outputPath)) {
                            newStatus = SubmissionType.YES;
                        } else {
                            newStatus = SubmissionType.WAE;
                        }
                    }
                    runtimeErrorReader.close();
                    outputReader.close();

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

    private boolean compareFileToFile(String filePath1, String filePath2) throws IOException {
        String s1 = fileToString(filePath1).trim();
        String s2 = fileToString(filePath2).trim();
        return s1.equals(s2);
    }

    private boolean compareStringToFile(String text, String filePath) throws IOException {
        String s1 = text.trim();
        String s2 = fileToString(filePath).trim();
        return s1.equals(s2);
    }
}
