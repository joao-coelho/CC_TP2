import java.io.*;
import java.net.*;

class NotifyThread extends Thread {

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


public class MonitorUDP {

    public static void main(String args[]) throws Exception {
        DatagramSocket socket = new DatagramSocket(5555);
        DatagramPacket receive;
        DatagramPacket send;
        InetAddress ipAddr;
       	String notify = "Available";
       	StringBuilder pdu  = new StringBuilder();
        StringBuilder cmd  = new StringBuilder();
        byte[] receiveData = new byte[128];
        byte[] sendData    = new byte[128];
        int lastAck = 1;
        int nTcp;

        if(args.length < 1) {
            System.out.println("Need Server IP to run");
            return;
        }
        String ip = args[0];

      	ipAddr = InetAddress.getByName(ip);  
        Thread notifier = new NotifyThread(ip, socket);
        notifier.start();

        while(true){     
            cmd.append("netstat --inet localhost --tcp | ");
            cmd.append("grep ESTABLISHED | wc -l");
            Process p = Runtime.getRuntime().exec(cmd.toString());
                
            InputStreamReader i = new InputStreamReader(p.getInputStream());
            BufferedReader res  = new BufferedReader(i);
            String tmp = res.readLine();

            try {
                nTcp = Integer.parseInt(tmp);
            } catch(NumberFormatException e) {
                nTcp = 0;
            }

      		receive = new DatagramPacket(receiveData, receiveData.length);               
            socket.receive(receive);
            String request = new String(receive.getData());
            System.out.println("RECEIVED: " + request);
            
            pdu.append("ACK ").append(lastAck++);
            pdu.append(" #TCP: " + nTcp);

            sendData = pdu.toString().getBytes();
            send = new DatagramPacket(sendData, sendData.length, 
                                      receive.getAddress(), 5555);
            socket.send(send);
            pdu = new StringBuilder();
        }
    }
}


