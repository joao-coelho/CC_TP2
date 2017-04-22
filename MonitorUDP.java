import java.io.*;
import java.net.*;

public class MonitorUDP {

    static class NotifyThread extends Thread {

        private DatagramSocket socket;
        private InetAddress ip;
        private byte[] sendData;
        private DatagramPacket sendPacket;
        private boolean running;

        public NotifyThread(String ip, DatagramSocket socket) {
            String sentence = "Available";
            sendData = new byte[64];
            try {
                this.ip = InetAddress.getByName(ip);
                sendData = sentence.getBytes();
                this.socket = socket;
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
                    synchronized(socket){
                            socket.send(sendPacket);
                    }
    
                    } catch(Exception e){
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
        DatagramSocket socket = new DatagramSocket();
        if(args.length < 1) {
            System.out.println("Need Server IP to run");
            return;
        }
        byte[] receiveData = new byte[64];
        byte[] sendData = new byte[128];

        String ip = args[0];
        Thread notifier = new NotifyThread(ip, socket);
        notifier.start();

        while(true){
                DatagramPacket receive = 
                	new DatagramPacket( receiveData, receiveData.length);
                
                synchronized(socket){
                        socket.receive(receive);
                }

                String request = 
                	new String(receive.getData());
                
                System.out.println("RECEIVED: " + request);
                String response = "RESPONSE";
                
                sendData = response.getBytes();
                DatagramPacket send = 
                	new DatagramPacket( sendData, sendData.length, receive.getAddress(), 5555);
                
                synchronized(socket){
                        socket.send(send);
                }
        }
    }
}


