package graderserver;

import graderserver.controllers.ServerController;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws ClassNotFoundException, IOException {
        String projectPath = "C:/Users/pc2grader/Desktop/grader";
//        String projectPath = "D:/Study/4thYear/2ndSemester/TA_Algo/grader";
        ServerController controller = new ServerController(13500, projectPath);
        controller.run();
    }
}
