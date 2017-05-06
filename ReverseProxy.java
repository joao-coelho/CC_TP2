import java.io.*;
import java.net.*;
import java.util.*;

class InfoServer {
    protected double rtt;
    protected double lossRate;
    protected int nTcpCon;
    protected int rec, env;
    protected int dup, lastAck;
    protected Calendar lastProbing;

    public InfoServer() {
        rtt = 0.0;
        lossRate = 0.0;
        nTcpCon  = 0;
        rec = env = 0;
    }

    public void updateLoss() {
        double aux = (double)rec/env;
        if(aux <= 1)
            lossRate = 1 - aux;
        else
            lossRate = aux - 1;
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
            ArrayList<InetAddress> rm = getTimedOut();
            
            for(InetAddress ia : rm)
                table.remove(ia);
            
            try {
                Thread.sleep(ReverseProxy.TIMECHK);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<InetAddress> getTimedOut() {
        ArrayList<InetAddress> rm = new ArrayList<>();
        double time, diff;
            
        for(Map.Entry<InetAddress, InfoServer> entry : table.entrySet()) {
            InetAddress key  = entry.getKey();
            InfoServer value = entry.getValue();
            time = Calendar.getInstance().getTimeInMillis();
            diff = time  -  value.lastProbing.getTimeInMillis();

            if(diff > ReverseProxy.TIMEOUT) {
                String s = "Lost connection with ";
                System.out.println(s + key.toString());
                System.out.println("");
                rm.add(key);
            }
        }
        return rm;
    }

    public void stopCycle() {
        running = false;
    }
}

class Probing extends Thread {
    HashMap<InetAddress,InfoServer> table;
    boolean running;
    DatagramSocket socket;
    String request;
    String response;
    DatagramPacket send, receive;
    InfoServer backEnd;
    InetAddress addr;
    byte[] receiveData;
    byte[] sendData;

    public Probing(HashMap<InetAddress,InfoServer> table) {
        this.table = table;
        try {
            socket = new DatagramSocket(5555);
        } catch(Exception e) {
            e.printStackTrace();
        }
        running = true;
        request = "Request Probing ";
        receiveData = new byte[128];
        sendData    = new byte[128];
    }


    @Override
    public void run() {

        while(running) {
            receive = new DatagramPacket(receiveData, receiveData.length);
            try {
                socket.receive(receive);
                String received = new String(receive.getData(), receive.getOffset(), 
                                             receive.getLength());
                addr = receive.getAddress();
                System.out.println(received);

                if(received.contains("Available"))
                    sendProbing();
                else {
                    updateInfo(received);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendProbing() throws Exception {
        System.out.println("");
        StringBuilder env = new StringBuilder();

        if(!table.containsKey(addr)) {
            String s = "New connection: " + addr.toString();
            System.out.println(s);
            backEnd = new InfoServer();
            table.put(addr, backEnd);
        }
        else
            backEnd = table.get(addr);
        Calendar c = Calendar.getInstance();
        long time = c.getTimeInMillis();
        env.append(request);
        env.append(backEnd.env);
        env.append(" ").append(time);
        String cena = env.toString();
        sendData = env.toString().getBytes();
        send = new DatagramPacket(sendData, sendData.length, 
                                  addr, 5555);
        backEnd.lastProbing = c;
        backEnd.env++;
        backEnd.updateLoss();
        socket.send(send);
    }

    private void updateInfo(String received) throws Exception {
        String[] rec = received.split(" ");
        backEnd  = table.get(addr);
        long i = Long.parseLong(rec[4]);
        long f = Calendar.getInstance().getTimeInMillis();
        int ac = Integer.parseInt(rec[1]);
        backEnd.rtt = (backEnd.rtt + (f - i))/2;
        backEnd.nTcpCon = Integer.parseInt(rec[3]);
        if(ac == backEnd.lastAck)
            backEnd.dup++;
        else 
            backEnd.rec++;
        backEnd.updateLoss();
        backEnd.lastAck = ac;

        System.out.println("IP: " + addr.toString());
        System.out.printf ("RTT: %.2f ms\n", backEnd.rtt);
        System.out.println("LossRate: " + backEnd.lossRate);
        System.out.println("Tcp connections: " + backEnd.nTcpCon);
        System.out.println("Duplicated packets:" + backEnd.dup);
        System.out.println("");
    }
}


class TcpConnection extends Thread {
    Socket client;
    HashMap<InetAddress, InfoServer> table;

    public TcpConnection(Socket client, HashMap<InetAddress, InfoServer> table) {
        this.client = client;
        this.table = table;
    }

    @Override
    public void run() {
        for(Map.Entry<InetAddress, InfoServer> entry : table.entrySet()) {
            InetAddress key  = entry.getKey();
            InfoServer value = entry.getValue();
            if(value.nTcpCon == 0) {
                //Socket e siga
            }
        }

    }

}

public class ReverseProxy {
    public static final int TIMEOUT = 20000;
    public static final int TIMECHK = 10000;
    public static final int PORT = 80;

    public static void main(String args[]) throws Exception {

        ServerSocket socket = new ServerSocket(PORT);
        Socket client;
        HashMap<InetAddress,InfoServer> table = new HashMap<>();
        Thread check = new CheckTimeOut(table);
        Thread probn = new Probing(table);
        check.start();
        probn.start();
        while((client = socket.accept()) != null) {
            Thread tcpCon = new TcpConnection(client, table);
            tcpCon.start();
        }

    }
}
