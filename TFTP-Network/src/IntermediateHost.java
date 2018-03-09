import java.io.*;
import java.net.*;
import java.util.Scanner;

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

		operationSetup();

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

	private void operationSetup() {
		Scanner input = new Scanner(System.in);
		
		System.out.println(
				"Choose an operation. 0: normal operation, 1: lose a packet, 2: delay a packet, 3: duplicate a packet.");
		int chosenOperation = input.nextInt();

		int packetTypeToLose;
		int packetTypeToDuplicate;
		int packetTypeToDelay;
		int packetNum;

		switch (chosenOperation) {
		case 1:
			System.out.println("Do you want to lose a 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToLose = input.nextInt();
			if (packetTypeToLose == 2 || packetTypeToLose == 3) {
				System.out.println("Which number DATA or ACK packet should be lost during transfer: ");
				packetNum = input.nextInt();
			}
			break;
		case 2:
			System.out.println("Which packet should be delayed: 0: DATA or 1: ACK");
			packetTypeToDelay = input.nextInt();
			System.out.println("Which number DATA or ACK packet should be delayed during transfer: ");
			packetNum = input.nextInt();
			System.out.println("How long should the delay be in seconds: ");
			int delayTime = input.nextInt();
			break;
		case 3:
			System.out.println("Do you want to duplicate a 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToDuplicate = input.nextInt();
			break;
		}

		input.close();

	}

	public static void main(String[] args) throws IOException {
		IntermediateHost host = new IntermediateHost();
		host.run();
	}
}
