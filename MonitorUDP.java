import java.io.*;
import java.net.*;

public class MonitorUDP {

    static class NotifyThread extends Thread {

        private DatagramSocket socket;
        private InetAddress ip;
        private byte[] sendData;
        private DatagramPacket sendPacket;
        private boolean running;

        public NotifyThread(String ip) {
            String sentence = "Available";
            sendData = new byte[64];
            try {
                this.ip = InetAddress.getByName(ip);
                sendData = sentence.getBytes();
                socket = new DatagramSocket();
            } catch(Exception e) {
                e.printStackTrace();
            }
            running = true;
        }


        @Override
        public void run() {
            sendPacket = new DatagramPacket(sendData, 
                         sendData.length, ip, 5555);
            while(running) {
                try {
                    socket.send(sendPacket);
                    Thread.sleep(5000);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        public void stopCycle() {
            running = false;
            socket.close();
        }


    } 

    public static void main(String args[]) throws Exception {
        
        if(args.length < 1) {
            System.out.println("Need Server IP to run");
            return;
        }
        
        String ip = args[0];
        Thread notifier = new NotifyThread(ip);
        notifier.start();

    }
}