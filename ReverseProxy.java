import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ReverseProxy {

    class InfoServer {
        protected InetAddress ip;
        protected double rtt;
        protected double lossRate;
        protected int nTcpCon;
    }

    ArrayList<InfoServer> table;

    static class ReceiveNotify extends Thread {

        private DatagramSocket socket;
        private InetAddress ip;
        private byte[] receiveData;
        private DatagramPacket receive;
        boolean running;

        public ReceiveNotify (String ip, DatagramSocket socket){
                this.socket = socket;
                try{
                        this.ip = InetAddress.getByName(ip);
                } catch( Exception e) {
                        e.printStackTrace();
                }
                receiveData = new byte [64];
                running = true;
        }

        @Override
        public void run() {
                receive = new DatagramPacket(receiveData, receiveData.length);
                while(running){
                        try {
                                synchronized(socket) {
                                        socket.receive(receive);
                                }
                        String notify = new String(receive.getData());
                        System.out.println("R:. " + notify);
                        } catch( Exception e){
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

        DatagramSocket serverSocket = new DatagramSocket(5555);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        
        while(true) {
            
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            
            String sentence = new String( receivePacket.getData());
            System.out.println("RECEIVED: " + sentence);
            
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            String capitalizedSentence = sentence.toUpperCase();
            sendData = capitalizedSentence.getBytes();
            
            DatagramPacket sendPacket =
            new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
        }
    }
}
