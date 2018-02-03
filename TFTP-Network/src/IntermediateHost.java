import java.io.*;
import java.net.*;

public class IntermediateHost {
	private DatagramSocket sendReceiveSocket = null;
	private DatagramPacket sendReceivePacket;
	private final int serverPort = 69;

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
		int clientPort, serverThreadPort = 0;

		while (true) {

			System.out.println("HOST: WAITING FOR PACKET TO BE RECEIVED FROM CLIENT...\n");
			sendReceiveSocket.receive(sendReceivePacket);
			clientPort = sendReceivePacket.getPort();

			System.out.println("Host: received packet from client port:" + clientPort);
			// if read request received or write request received then send to
			// server
			if ((data[0] == 0 && data[1] == 1) || (data[0] == 0 && data[1] == 2)) {
				System.out.println("Sending packet to server port:" + serverPort);
				sendReceivePacket.setPort(serverPort);
			} else {// send to thread
				System.out.println("Sending packet to threaded server port:" + serverThreadPort);
				sendReceivePacket.setPort(serverThreadPort);
			}
			sendReceiveSocket.send(sendReceivePacket);

			System.out.println("HOST: WAITING FOR PACKET TO BE RECEIVED FROM SERVER THREAD PORT...\n");
			sendReceiveSocket.receive(sendReceivePacket);
			serverThreadPort = sendReceivePacket.getPort();
			System.out.println("Host: received packet from server thread port:" + serverThreadPort);

			System.out.println("Sending packet to client port:" + clientPort);
			sendReceivePacket.setPort(clientPort);
			sendReceiveSocket.send(sendReceivePacket);
		}
	}

	public static void main(String[] args) throws IOException {
		IntermediateHost host = new IntermediateHost();
		host.run();

	}

}
