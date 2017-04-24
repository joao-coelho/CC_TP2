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
        nTcpCon = 0;
        rec = env = 0;
    }

    public void updateLoss() {
        lossRate = 1 - rec/env;
    }

}


public class ReverseProxy {


    public static void main(String args[]) throws Exception {
        HashMap<InetAddress,InfoServer> table = new HashMap<>();
        DatagramSocket socket = new DatagramSocket(5555);
        DatagramPacket send;
        byte[] sendData    = new byte[128];
        InetAddress addr;
        DatagramPacket receive;
        String request, response;
        InfoServer backEnd;

        
        request = "Request Probing";
        while(true){
            byte[] receiveData = new byte[64];
            receive = new DatagramPacket(receiveData,
                                    receiveData.length);
            socket.receive(receive);
            String received = new String(receive.getData());
            System.out.println(received);
            addr = receive.getAddress();

            if(received.contains("Available")) {

                sendData = (request).getBytes();
                send = new DatagramPacket(sendData, sendData.length, addr, 5555);
                if(!table.containsKey(addr)) {
                    backEnd = new InfoServer();
                    table.put(addr, backEnd);
                }
                else
                    backEnd = table.get(addr);
                
                backEnd.lastReq = Calendar.getInstance();
                backEnd.env++;
                backEnd.updateLoss();
                socket.send(send);
            }
            else {
                backEnd = table.get(addr);
                double i = backEnd.lastReq.getTimeInMillis();
                double f = Calendar.getInstance().getTimeInMillis();
                backEnd.rtt = (backEnd.rtt + (f - i))/2;
                backEnd.rec++;
                backEnd.updateLoss();
                System.out.println(backEnd.rtt);
                System.out.println(backEnd.lossRate);
            }
        }
    }
}
