package graderserver.controllers;

import graderserver.databaes.SubmissionConnector;
import graderserver.models.Problem;
import graderserver.models.Submission;
import graderserver.models.SubmissionType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JudgeController extends Thread {

    private SubmissionConnector db;
    private String projectPath;
    private int id;

    public JudgeController(String projectPath, int id) throws ClassNotFoundException {
        this.projectPath = projectPath;
        String dbPath = projectPath + "/database/database.sqlite";
        this.db = new SubmissionConnector(dbPath);
        this.id = id;
    }

    @Override
    public void run() {
        try {
            Submission submission = this.db.getSubmissionById(id);
            Problem problem = submission.getProblem();
            String problemPath = this.projectPath + "/storage/app/problems/" + problem.getId() + "/problemtestset/";
            String submissionPath = this.projectPath + "/storage/app/submissions/";
            String codePath = submissionPath + submission.getFilePath();
            String objectPath = submissionPath + submission.getFilePath().replaceAll("/.*","/");
            String objectName = submission.getFilePath().replaceAll(".*/","").replaceAll("\\..*","");
            System.out.println(submission.getStatus());
            SubmissionType newStatus = SubmissionType.CTA;
            //compile code
            String compileError = null;
            if (submission.getLanguage().equals("c")) {
                String command = String.format("gcc %s -o %s", codePath, objectPath + objectName + ".exe", objectPath + "compile_error.txt");
                Process compileProcess = Runtime.getRuntime().exec(command);
                BufferedReader compileErrorReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                compileError = compileErrorReader.readLine();
                compileErrorReader.close();
            } else if (submission.getLanguage().equals("cpp")) {

            } else if (submission.getLanguage().equals("java")) {

            }
            if (compileError == null) {
                for (int i = 0; i < problem.getInputFiles().size(); i++) {
                    String inputPath =  problemPath + problem.getInputFiles().get(i);
                    String outputPath = problemPath + problem.getOutputFiles().get(i);
                    String runtimeError = null;
                    String output = null;
                    if (submission.getLanguage().equals("c")) {
                        String command = String.format("%s <\"%s\"", objectPath + objectName + ".exe", inputPath);
                        Process runtimeProcess = Runtime.getRuntime().exec(command);
                        OutputStream outputStream = runtimeProcess.getOutputStream();
                        Files.copy(Paths.get(inputPath), outputStream);
                        outputStream.flush();
                        BufferedReader runtimeErrorReader = new BufferedReader(new InputStreamReader(runtimeProcess.getErrorStream()));
                        runtimeError = runtimeErrorReader.readLine();
                        if (runtimeError != null) {
                            System.out.println(runtimeError);
                            newStatus = SubmissionType.RTE;
                        }
                        else {
                            BufferedReader outputReader = new BufferedReader(new InputStreamReader(runtimeProcess.getInputStream()));
                            output = "";
                            String line;
                            while ((line = outputReader.readLine()) != null) {
                                output += line + "\n";
                            }
//                        System.out.println("output#"+(i+1));
//                        System.out.println(output.trim());
//                        System.out.println(fileToString(outputPath).trim());
//                        System.out.println(compareStringToFile(output, outputPath));
                            if (compareStringToFile(output, outputPath)) {
                                newStatus = SubmissionType.YES;
                            } else {
                                newStatus = SubmissionType.WAE;
                            }
                        }
                    } else if (submission.getLanguage().equals("cpp")) {

                    } else if (submission.getLanguage().equals("java")) {

                    }
                }
            } else {
                newStatus = SubmissionType.CPE;
            }

            System.out.println(newStatus);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runCode(int id) throws IOException {

    }

    private String fileToString(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private boolean compareStringToFile(String text, String filePath) throws IOException {
        String s1 = text.trim();
        String s2 = fileToString(filePath).trim();
        return s1.equals(s2);
    }

    private boolean compareStringToFile(String text, File file) {
        return file.equals(text);
    }

    private boolean compile() {
        return false;
    }
}
