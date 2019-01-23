package graderserver.databaes;

import graderserver.models.Problem;
import graderserver.models.Submission;
import graderserver.models.SubmissionType;

import java.sql.*;
import java.util.ArrayList;

public class SubmissionConnector {

    private String dbPath;
    private Connection conn;

    public SubmissionConnector(String dbPath) throws ClassNotFoundException {
        this.dbPath = dbPath;
        Class.forName("org.sqlite.JDBC");
    }

    private void connect() throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + this.dbPath);
    }

    private void disconnect() throws SQLException {
        if (this.conn != null) {
            this.conn.close();
        }
    }

    public Submission getSubmissionById(int id) {
        Submission submission = null;
        try {
            connect();
            String query = "SELECT problem_id, file_path, language, status FROM submissions WHERE id=?";
            PreparedStatement pstmt = this.conn.prepareStatement(query);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int problem_id = rs.getInt(1);
                String filePath = rs.getString(2);
                String language = rs.getString(3);
                SubmissionType status = SubmissionType.fromString(rs.getString(4));
                query = "SELECT time FROM problems WHERE id=?";
                PreparedStatement pstmt2 = this.conn.prepareStatement(query);
                pstmt2.setInt(1, problem_id);
                ResultSet rs2 = pstmt2.executeQuery();
                if (rs2.next()) {
                    int time = rs2.getInt(1);
                    query = "SELECT input_path, output_path FROM problem_test_sets WHERE problem_id=?";
                    PreparedStatement pstmt3 = this.conn.prepareStatement(query);
                    pstmt3.setInt(1, problem_id);
                    ResultSet rs3 = pstmt3.executeQuery();
                    ArrayList<String> inputFiles = new ArrayList<>();
                    ArrayList<String> outputFiles = new ArrayList<>();
                    while (rs3.next()) {
                        String inputFile = rs3.getString(1);
                        String outputFile = rs3.getString(2);
                        inputFiles.add(inputFile);
                        outputFiles.add(outputFile);
                    }
                    Problem problem = new Problem(problem_id, time, inputFiles, outputFiles);
                    submission = new Submission(id, problem, filePath, language, status);
                }
            }
            disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return submission;
    }

    public void updateSubmissionStatus(int id, SubmissionType newStatus) {
        try {
            connect();
            String query = "UPDATE submissions SET status=? WHERE id=?";
            PreparedStatement pstmt = this.conn.prepareStatement(query);
            pstmt.setString(1, newStatus.toString());
            pstmt.setInt(2, id);
            pstmt.execute();
            disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
