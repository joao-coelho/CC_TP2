import java.io.*;
import java.net.*;

public class UDPClient {

   public static void main(String args[]) throws Exception {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        String sentence = "Estou disponível";
        sendData = sentence.getBytes();
        while(true) {
            DatagramPacket sendPacket = 
                new DatagramPacket(sendData, sendData.length, IPAddress, 5555);
            clientSocket.send(sendPacket);
            DatagramPacket receivePacket = 
                new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            System.out.println("FROM SERVER:" + modifiedSentence);
            Thread.sleep(3000);
        }
        //clientSocket.close();
   }
}