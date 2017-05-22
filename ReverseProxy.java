import java.io.*;
import java.net.*;
import java.util.*;


class InfoServer implements Comparable {
    protected double rtt;
    protected double lossRate;
    protected int nTcpCon;
    protected int rec, env, warn;
    protected int dup, lastAck;
    protected double score;
    protected Calendar lastProbing;

    public InfoServer() {
        rtt = 0.0;
        lossRate = 0.0;
        nTcpCon  = 0;
        rec   = env = 0;
        warn  = -1;
        score = 0;
    }

    public void update() {
        double aux = (double)rec/env;
        if(aux <= 1)
            lossRate = 1 - aux;
        else
            lossRate = aux - 1;
        score  = rtt + lossRate;
        score += nTcpCon;
    }

    public int compareTo(Object o) {
        if(!(o instanceof InfoServer))
            return -1;
        InfoServer s = (InfoServer)o;
        if(score > s.score)
            return 1;
        else if(score < s.score)
            return -1;
        return 0;
    }

}

class CheckTimeOut extends Thread {

    Map<InetAddress,InfoServer> table;
    boolean running;

    public CheckTimeOut(Map<InetAddress,InfoServer> table) {
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
    Map<InetAddress,InfoServer> table;
    boolean running;
    DatagramSocket socket;
    String request;
    String response;
    DatagramPacket send, receive;
    InfoServer backEnd;
    InetAddress addr;
    byte[] receiveData;
    byte[] sendData;

    public Probing(Map<InetAddress,InfoServer> table) {
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
                    sendProbing(received.split(" ")[1]);
                else {
                    updateInfo(received);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendProbing(String num) throws Exception {
        System.out.println("");
        StringBuilder env = new StringBuilder();
        int numb = Integer.parseInt(num);

        if(!table.containsKey(addr)) {
            String s = "New connection: " + addr.toString();
            System.out.println(s);
            backEnd = new InfoServer();
            table.put(addr, backEnd);
        }
        else
            backEnd = table.get(addr);

        int d = numb - backEnd.warn;
        if(d == 0) {
            backEnd.dup++;
            return;
        }

        Calendar c = Calendar.getInstance();
        long time = c.getTimeInMillis();
        env.append(request);
        env.append(backEnd.env);
        env.append(" ").append(time);
        sendData = env.toString().getBytes();
        send = new DatagramPacket(sendData, sendData.length, 
                                  addr, 5555);
        backEnd.lastProbing = c;
        backEnd.env++;
        backEnd.update();
        backEnd.warn = numb;
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
        backEnd.update();
        backEnd.lastAck = ac;
        printTable();

        /*System.out.println("IP: " + addr.toString());
        System.out.printf ("RTT: %.2f ms\n", backEnd.rtt);
        System.out.printf ("LossRate: %.2f\n",backEnd.lossRate);
        System.out.println("Tcp connections: " + backEnd.nTcpCon);
        System.out.println("Duplicated packets:" + backEnd.dup);
        System.out.println("");*/
    }

    private void printTable() {
        System.out.print("\033[H\033[2J");  
        System.out.flush();
        System.out.printf("%15s", "IP");
        System.out.printf("  |  ");
        System.out.printf("%10s", "RTT(ms)");
        System.out.printf("  |  ");
        System.out.printf("%10s", "LossRate");
        System.out.printf("  |  ");
        System.out.printf("%10s", "NºTcpCon");
        System.out.printf("  |  ");
        System.out.printf("%10s", "NºDupPack");
        System.out.printf("  |\n");
        System.out.print("-----------------+");
        System.out.print("--------------+");
        System.out.print("--------------+");
        System.out.print("--------------+");
        System.out.print("--------------+\n");
         for(Map.Entry<InetAddress, InfoServer> entry : table.entrySet()) {
            InetAddress key  = entry.getKey();
            InfoServer value = entry.getValue();
            System.out.printf("%15s", key.toString());
            System.out.printf("  |  ");
            System.out.printf("%10f", value.rtt);
            System.out.printf("  |  ");
            System.out.printf("%10f", value.lossRate);
            System.out.printf("  |  ");
            System.out.printf("%10d", value.nTcpCon);
            System.out.printf("  |  ");
            System.out.printf("%10d", value.dup);
            System.out.printf("  |\n");
            System.out.print("-----------------+");
            System.out.print("--------------+");
            System.out.print("--------------+");
            System.out.print("--------------+");
            System.out.print("--------------+\n");
        }
    }
}


class TcpConnection extends Thread {
    Socket client;
    Socket backEnd;
    Map<InetAddress, InfoServer> table;

    public TcpConnection(Socket client, Map<InetAddress, InfoServer> table) {
        this.client = client;
        this.table = table;
    }

    @Override
    public void run() {
        double score = -1;
        try {
            InetAddress ip = InetAddress.getByName("localhost");
            for(Map.Entry<InetAddress, InfoServer> entry : table.entrySet()) {
                InfoServer s = entry.getValue();
                InetAddress i = entry.getKey();
                if(s.score < score || score < 0) {
                    score = s.score;
                    ip = i;
                }
            }
            String str;
            String hostStr = "Host: " + ip.toString().split("/")[1];
            backEnd = new Socket(ip, ReverseProxy.PORT);
            InputStreamReader be = new InputStreamReader(backEnd.getInputStream());
            InputStreamReader cl = new InputStreamReader(client.getInputStream());
            BufferedReader inBE  = new BufferedReader(be);
            BufferedReader inCL  = new BufferedReader(cl);
            PrintWriter outCL = new PrintWriter(client.getOutputStream(), true);
            PrintWriter outBE = new PrintWriter(backEnd.getOutputStream(), true);            
            str = inCL.readLine();
            while(!str.equals("")) {
                if(str.contains("Host:"))
                    str = hostStr;
                outBE.print(str+"\n\r");
                outBE.flush();
                str = inCL.readLine(); 
            }
            outBE.print("\n\r");
            outBE.flush();
            str = inBE.readLine();
            while(str != null) {
                outCL.println(str);
                outCL.flush();
                str = inBE.readLine();
            }

        } catch(Exception e) {}

    }
}


public class ReverseProxy {
    public static final int TIMEOUT = 20000;
    public static final int TIMECHK = 10000;
    public static final int PORT = 80;

    public static void main(String args[]) throws Exception {

        ServerSocket socket = new ServerSocket(PORT);
        Socket client;
        Map<InetAddress,InfoServer> table = new HashMap<>();
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
