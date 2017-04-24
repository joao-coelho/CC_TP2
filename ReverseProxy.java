import java.io.*;
import java.net.*;
import java.util.*;

class InfoServer {
    protected double rtt;
    protected double lossRate;
    protected int nTcpCon;
    protected int rec, env;
    protected Calendar lastReq;


    public InfoServer() {
        rtt = 0.0;
        lossRate = 0.0;
        nTcpCon  = 0;
        rec = env = 0;
    }

    public void updateLoss() {
        lossRate = 1 - (double)rec/env;
    }

}

class CheckTimeOut extends Thread {

    HashMap<InetAddress,InfoServer> table;
    boolean running;

    public CheckTimeOut(HashMap<InetAddress,InfoServer> table) {
        this.table = table;
        running = true;
    }


    @Override
    public void run() {
           
            while(running) {
                double time, diff;
                ArrayList<InetAddress> rm = new ArrayList<>();
                
                for(Map.Entry<InetAddress, InfoServer> entry : table.entrySet()) {
                    InetAddress key = entry.getKey();
                    InfoServer value = entry.getValue();
                    time = Calendar.getInstance().getTimeInMillis();
                    diff = time - value.lastReq.getTimeInMillis();
                    
                    if(diff > 20000) {
                        String s = "Lost connection with ";
                        System.out.println(s + key.toString());
                        System.out.println("");
                        rm.add(key);
                    }
                }
                for(InetAddress ia : rm)
                    table.remove(ia);
                try {
                    Thread.sleep(10000);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
    }

        public void stopCycle() {
            running = false;
        }
}



public class ReverseProxy {

    
    public static void main(String args[]) throws Exception {

        HashMap<InetAddress,InfoServer> table = new HashMap<>();
        DatagramSocket socket = new DatagramSocket(5555);
        DatagramPacket send;
        byte[] sendData = new byte[128];
        InetAddress addr;
        DatagramPacket receive;
        String request, response;
        InfoServer backEnd;
        request = "Request Probing ";
        Thread check = new CheckTimeOut(table);
        check.start();

        while(true){
            byte[] receiveData = new byte[64];
            receive = new DatagramPacket(receiveData,
                                    receiveData.length);
            socket.receive(receive);
            String received = new String(receive.getData(), 0, receive.getLength());
            addr = receive.getAddress();
            System.out.println(received);

            if(received.contains("Available")) {

                System.out.println("");
                
                if(!table.containsKey(addr)) {
                    System.out.println("New connection: " + addr.toString());
                    backEnd = new InfoServer();
                    table.put(addr, backEnd);
                }
                else
                    backEnd = table.get(addr);

                sendData = (request + backEnd.env).getBytes();
                send = new DatagramPacket(sendData, sendData.length, addr, 5555);
                backEnd.lastReq = Calendar.getInstance();
                backEnd.env++;
                backEnd.updateLoss();
                socket.send(send);
            }
            else {
                backEnd = table.get(addr);
                double i = backEnd.lastReq.getTimeInMillis();
                double f = Calendar.getInstance().getTimeInMillis();
                String s = received.split(" ")[3];
                backEnd.rtt = (backEnd.rtt + (f - i))/2;
                backEnd.rec++;
                backEnd.updateLoss();
                backEnd.nTcpCon = Integer.parseInt(s);

                System.out.println("IP: " + addr.toString());
                System.out.println("RTT: " + backEnd.rtt);
                System.out.println("LossRate: " + backEnd.lossRate);
                System.out.println("Tcp connections: " + backEnd.nTcpCon);
                System.out.println("");
            }

        }
    }
}
