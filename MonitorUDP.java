import java.io.*;
import java.net.*;
import java.util.Calendar;

class NotifyThread extends Thread {

    private DatagramSocket socket;
    private InetAddress ip;
    private byte[] sendData;
    private DatagramPacket sendPacket;
    private boolean running;
    private int nr;

    public NotifyThread(String ip, DatagramSocket socket) {
        sendData = new byte[64];
        try {
            this.ip = InetAddress.getByName(ip);
            this.socket = socket;
        } catch(Exception e) {
            e.printStackTrace();
        }
        running = true;
    }


    @Override
    public void run() {
        String sentence = "Available ";
        while(running) {
            try {
                sendData   = (sentence + nr++).getBytes();
                sendPacket = new DatagramPacket(sendData,
                             sendData.length, ip, 5555);
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
        int nTcp;

        if(args.length < 1) {
            System.out.println("Need Server IP to run");
            return;
        }
        String ip = args[0];

      	ipAddr = InetAddress.getByName(ip);  
        Thread notifier = new NotifyThread(ip, socket);
        notifier.start();

        while(true) {
            cmd.append("netstat | ");
            cmd.append("grep tcp | wc -l");
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

            String request = new String(receive.getData(), receive.getOffset(), 
                                        receive.getLength());
            System.out.println("RECEIVED: " + request);

            int req = Integer.parseInt(request.split(" ")[2]);
            String num = request.split(" ")[3];
            long time  = Long.parseLong(num);
            long tnow  = Calendar.getInstance().getTimeInMillis();
            long diff  = tnow - time;
            
            pdu.append("ACK ").append(req+1).append(" ");
            tnow = Calendar.getInstance().getTimeInMillis();
            pdu.append(tnow - diff);
            sendData = pdu.toString().getBytes();
            send = new DatagramPacket(sendData, sendData.length, 
                                      receive.getAddress(), 5555);
            socket.send(send);
            System.out.println("SENT: " + pdu.toString());
            pdu = new StringBuilder();
        }
    }
}


