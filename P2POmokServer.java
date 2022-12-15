/**
 * 20176342 Song Min Joon
 * P2POmokServer.java
 **/

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.sql.Time;

class client {
    String nickname;
    int uniqueID;
    Socket conn;
    String ip;
    String port;
    String remote;

    client(int uniqueID){
        this.uniqueID = uniqueID;
    }

    client(String nickname, int uniqueID, Socket conn, String ip, String port, String remoteAddr){
        this.nickname = nickname;
        this.uniqueID = uniqueID;
        this.conn = conn;
        this.ip = ip;
        this.port = port;
        this.remote = remoteAddr;
    }

    

}
public class P2POmokServer {
	

    final public static int serverPort = 56342;// for server port
    public static int uniqueID = 1;// for server port
	public static ConcurrentHashMap<Integer ,client> clientMap = new ConcurrentHashMap<Integer, client>();


    public static void sendPacket(client client, String packet) {
        // send packet to server
        OutputStream os;
        try {
            os = client.conn.getOutputStream();

            try {
                os.write(packet.getBytes());
                os.flush();
            } catch (IOException e) {
    
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    
    }

	public static void main(String args[]){
        ServerSocket listener = null;
        Socket conn = null;
        try {
            listener = new ServerSocket(serverPort); // create listener for tcp socket

        } catch (IOException e) {

        }
        System.out.printf("Server is ready to receive on port %s\n", serverPort);
        Runtime.getRuntime().addShutdownHook(new ByeByeThread(listener)); // shutdown hook for graceful exit

        try {
            while (true) {
                // listener is waiting for tcp connection of clients.
                conn = listener.accept();

                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                InputStream is = conn.getInputStream();
                int count = is.read(buffer);

                if (count == -1) {
                    System.out.println("Client disconnected");
                    break;
                }

                String targetNick = new String(buffer, 0, count);

                String remoteAddr = conn.getInetAddress().toString().split("/")[1] + ":" + conn.getPort();
                String ip = conn.getInetAddress().toString().split("/")[1];
                String port = conn.getPort()+"";

                client newClient = new client(targetNick, uniqueID, conn, ip, port, remoteAddr);

                System.out.printf("%s joined from from %s. UDP port %s\n", targetNick,ip,conn.getPort());
                
                registerClient(newClient, uniqueID);
                
                ServerReceiver th = new ServerReceiver(conn, uniqueID);
                uniqueID++;

                th.start();

                if (clientMap.size() == 1) {
                    sendPacket(newClient, "waiting||");
                    System.out.printf("1 user connected, waiting for another\n");
                } else if(clientMap.size() == 2) {
                    client otherRemote = new client(-1);

                    for( int key : clientMap.keySet() ){
                        client v = clientMap.get(key);

                        if( v.uniqueID != newClient.uniqueID){
                            otherRemote = v;
                            sendPacket(v,"matched|"+newClient.nickname+"|"+newClient.remote);
                        }
                    }

			        System.out.printf("2 users connected, notifying %s and %s\n",otherRemote.nickname,newClient.nickname );

                   
                    sendPacket(newClient,"success|"+otherRemote.nickname+"|"+otherRemote.remote);
        
                    for( int key : clientMap.keySet() ){
                        client v = clientMap.get(key);
                        unregisterClient(v.uniqueID);
                    }
			        System.out.printf("%s and %s disconnected.\n",otherRemote.nickname,newClient.nickname );
                   
        
                }
            }

        } catch (IOException e) {

        }
	}
	public static void byebye() {
        // print bye bye~
        System.out.println("Bye bye~");
    }

    public static int registerClient(client client, int uniqueID){
        clientMap.put(uniqueID, client);
        return uniqueID;
    }
    public static client unregisterClient(int uniqueID){
        return clientMap.remove(uniqueID);
    }
	public static class ByeByeThread extends Thread {
        // ByeBye Thread for graceful exit program.
        ServerSocket listener;

        ByeByeThread(ServerSocket listener) {
            this.listener = listener;
        }

        public void run() {
            try {
                Thread.sleep(200);
                byebye();//print bye bye~

                try {
                    this.listener.close(); // close listener
                } catch (IOException e) {

                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

	static class ServerReceiver extends Thread {
        // ServerReceiver Thread for each client
        Socket conn;
        DataInputStream is;
        DataOutputStream os;
        int uniqueID;

        ServerReceiver(Socket conn, int uniqueID) {
            this.conn = conn;
            this.uniqueID = uniqueID;
            try {
                is = new DataInputStream(conn.getInputStream());
                os = new DataOutputStream(conn.getOutputStream());
            } catch (IOException e) {

            }
        }

        public void run() {
            //read packet for nickname

            while (true) {
                try {
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];

                    int count = is.read(buffer);

                    if (count == -1) {
                        System.out.println("Client disconnected");

                        client client = unregisterClient(this.uniqueID);

                        if (P2POmokServer.clientMap.size() == 1){
				            System.out.printf("%s disconnected. 1 User left in server.\n",client.nickname);

                        } else if(P2POmokServer.clientMap.size() == 0){
				            System.out.printf("%s disconnected. No User left in server.\n",client.nickname);

                        }

                        break;
                    }

                    

                } catch (IOException e) {
                    unregisterClient(this.uniqueID);

                    System.out.println("client is disconnected");
                    break;
                }
            }

        }
        void sendPacket(String packet) {
            // send packet to server
            try {
                os.write(packet.getBytes());
                os.flush();
            } catch (IOException e) {

            }

        }

    }
}