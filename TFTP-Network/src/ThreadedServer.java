import java.io.*;
import java.net.*;

public class ThreadedServer {
	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	private byte data[];
	int receivePort = 69;

	public ThreadedServer() {
		
		try {
			receiveSocket = new DatagramSocket(receivePort);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}

	}

	public void receivingServer() {
		data = new byte[128];

		for (;;) {
			receivePacket = new DatagramPacket(data, data.length);

			try {
				System.out.println("Server: waiting to receive a packet");
				receiveSocket.receive(receivePacket); // wait for a packet
				System.out.println("Server: packet received");
			} catch (IOException e) { // throws exception
				e.printStackTrace();
				System.out.println("IOException occured!");
				System.exit(0);
			}
			
			//new thread pass received packet 
			Runnable newClient = new ClientConnectionThread(receivePacket);
			new Thread(newClient).start();
		}
	}
	
	public void close(){
		receiveSocket.close();
	}
	
	public static void main(String[] args) {
		ThreadedServer server = new ThreadedServer();
		server.receivingServer();
	}

}
