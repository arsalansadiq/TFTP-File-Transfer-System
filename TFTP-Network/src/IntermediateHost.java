import java.io.*;
import java.net.*;

public class IntermediateHost {
	private DatagramSocket sendReceiveSocket = null;
	private DatagramPacket sendReceivePacket;
	private final int serverPort = 69;
	private int clientPort, threadPort = 0;

	public IntermediateHost() {

		try {
			sendReceiveSocket = new DatagramSocket(23);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}
	}

	public void run() throws IOException {
		byte data[] = new byte[516];
		sendReceivePacket = new DatagramPacket(data, data.length);

		System.out.println("Intermediate host: waiting for a packet from client");
		sendReceiveSocket.receive(sendReceivePacket);
		clientPort = sendReceivePacket.getPort();

		sendReceivePacket.setPort(serverPort);

		System.out.println("Intermediate host: sending packet to server");
		sendReceiveSocket.send(sendReceivePacket);

		System.out.println("Intermediate host: waiting for a packet from thread");
		sendReceiveSocket.receive(sendReceivePacket);
		threadPort = sendReceivePacket.getPort();
		System.out.println("Intermediate host: received packet from thread");

		System.out.println("Intermediate host: sending packet to client");
		sendReceivePacket.setPort(clientPort);
		sendReceiveSocket.send(sendReceivePacket);

		System.out.println("\nTRANSFER HAS BEGUN.................................");
		while (true) {
			System.out.println("Intermediate host: waiting for a packet from client");
			sendReceiveSocket.receive(sendReceivePacket);
			sendReceivePacket.setPort(threadPort);
			System.out.println("Intermediate host: sending packet to thread");
			sendReceiveSocket.send(sendReceivePacket);

			System.out.println("Intermediate host: waiting for a packet from thread");
			sendReceiveSocket.receive(sendReceivePacket);
			System.out.println("Intermediate host: sending packet to client");
			sendReceivePacket.setPort(clientPort);
			sendReceiveSocket.send(sendReceivePacket);
		}
	}

	public static void main(String[] args) throws IOException {
		IntermediateHost host = new IntermediateHost();
		host.run();
	}
}
