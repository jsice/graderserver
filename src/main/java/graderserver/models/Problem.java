package graderserver.models;

import java.util.ArrayList;

public class Problem {
    private int id;
    private int time;
    private ArrayList<String> inputFiles;
    private ArrayList<String> outputFiles;

    public Problem(int id, int time, ArrayList<String> inputFiles, ArrayList<String> outputFiles) {
        this.id = id;
        this.time = time;
        this.inputFiles = inputFiles;
        this.outputFiles = outputFiles;
    }

    public int getId() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public ArrayList<String> getInputFiles() {
        return inputFiles;
    }

    public ArrayList<String> getOutputFiles() {
        return outputFiles;
    }
}
