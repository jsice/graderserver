package graderserver.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServerController extends Thread {
    private DatagramSocket socket;
    private byte[] buffer = new byte[1024];
    private String projectPath;

    public ServerController(int port, String projectPath) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.projectPath = projectPath;
    }

    private DatagramPacket receive() throws IOException {
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(receivedPacket);
        return receivedPacket;
    }

    private int byteArrayToInteger(byte[] bam) {
        return Integer.parseInt(new String(bam).trim());
    }
    @Override
    public void run() {
        System.out.println("Server start!");
        while (true) {
            try {
                System.out.println("receiving...");
                DatagramPacket receivedPacket = receive();
                InetAddress senderIP = receivedPacket.getAddress();
                int senderPort = receivedPacket.getPort();
                int id = byteArrayToInteger(receivedPacket.getData());
                System.out.println("From: " + senderIP+":"+ senderPort + " Message: " + id);
                JudgeController jc = new JudgeController(this.projectPath, id);
                jc.run();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
