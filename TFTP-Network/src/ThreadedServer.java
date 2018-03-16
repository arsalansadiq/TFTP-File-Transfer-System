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
		String receive = "";
		String receiveOld = "";

		while (true) {
			receivePacket = new DatagramPacket(data, data.length);

			try {
				System.out.println("Server: waiting to receive a packet");
				// receiveSocket.setSoTimeout(30000);
				receiveSocket.receive(receivePacket); // wait for a packet
				// System.out.println("Server: receivePacket is: "+new
				// String(receivePacket.getData()));
				receive = new String(receivePacket.getData());
				System.out.println("Server: packet received");
			} catch (IOException e) { // throws exception
				e.printStackTrace();
				System.out.println("SERVER TIMED OUT......................");
				System.exit(0);
			}

			if (receive.equals(receiveOld)) {
				System.out.println("Server: Duplicate read or request received. Discarding...");
			} else {
				Runnable newClient = new ClientConnectionThread(receivePacket);
				new Thread(newClient).start();
			}
			receiveOld = receive;
		}
	}

	public void close() {
		receiveSocket.close();
	}

	public static void main(String[] args) {
		ThreadedServer server = new ThreadedServer();
		server.receivingServer();
	}

}