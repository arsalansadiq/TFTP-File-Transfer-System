import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class IntermediateHost {
	private DatagramSocket sendReceiveSocket = null;
	private DatagramPacket sendReceivePacket;
	private final int serverPort = 69;
	private int clientPort, threadPort = 0;
	private boolean delaySim = false;
	private boolean lostSim = false;
	private boolean duplicateSim = false;
	private boolean rrqSim = false;
	private boolean wrqSim = false;
	private boolean dataSim = false;
	private boolean ackSim = false;
	private int packetNum;// this will be the packet number to be used for the given error simulation
	private int delayTime;

	public IntermediateHost() {

		try {
			sendReceiveSocket = new DatagramSocket(23);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}
	}

	// delay requests, duplicate, lost packets
	public void run() throws IOException, InterruptedException {

		operationSetup();

		byte data[] = new byte[516];
		/*
		 * initial connection
		 */

		sendReceivePacket = new DatagramPacket(data, data.length);

		// waiting for a request from client to server

		System.out.println("Intermediate host: waiting for a packet from client");
		sendReceiveSocket.receive(sendReceivePacket);
		
		clientPort = sendReceivePacket.getPort();

		sendReceivePacket.setPort(serverPort);
		// send request to server
		System.out.println("Intermediate host: sending packet to server");
		sim(sendReceivePacket);
		sendReceiveSocket.send(sendReceivePacket);
		//sim(sendReceivePacket);

		// waiting for response from server
		System.out.println("Intermediate host: waiting for a packet from thread");
		sendReceiveSocket.receive(sendReceivePacket);
		sim(sendReceivePacket);

		threadPort = sendReceivePacket.getPort();
		System.out.println("Intermediate host: received packet from thread");
		// sending response to client
		System.out.println("Intermediate host: sending packet to client");
		sendReceivePacket.setPort(clientPort);
		sendReceiveSocket.send(sendReceivePacket);
		sim(sendReceivePacket);

		System.out.println("\nTRANSFER HAS BEGUN.................................");
		while (true) {

			System.out.println("Intermediate host: waiting for a packet from client");
			sendReceiveSocket.receive(sendReceivePacket);
			sim(sendReceivePacket);

			sendReceivePacket.setPort(threadPort);
			System.out.println("Intermediate host: sending packet to thread");

			sendReceiveSocket.send(sendReceivePacket);
			sim(sendReceivePacket);

			System.out.println("Intermediate host: waiting for a packet from thread");
			sendReceiveSocket.receive(sendReceivePacket);
			sim(sendReceivePacket);

			System.out.println("Intermediate host: sending packet to client");
			sendReceivePacket.setPort(clientPort);
			sendReceiveSocket.send(sendReceivePacket);
			sim(sendReceivePacket);

		}
	}// handle rrq and wrq

	public void sim(DatagramPacket packet) throws InterruptedException, IOException {
		byte data[] = packet.getData();
		byte pNum[] = intToBytes(packetNum);
		if ((wrqSim && data[0] == 0 && data[1] == 2) || (rrqSim && data[0] == 0 && data[1] == 1)) {
			if (duplicateSim)
				duplicatePacketErrorSim(packet);
			if (delaySim)
				delayPacketErrorSim(packet);
			if (lostSim)
				lostPacketErrorSim(packet);
		} else if ((dataSim && data[0] == 0 && data[1] == 3) || (ackSim && data[0] == 0 && data[1] == 4)) {
			if (duplicateSim)
				if (blockNumMatch(packet))
					duplicatePacketErrorSim(packet);
			if (delaySim)
				if (blockNumMatch(packet))
					delayPacketErrorSim(packet);
			if (lostSim)
				if (blockNumMatch(packet))
					lostPacketErrorSim(packet);
		}

	}

	public void lostPacketErrorSim(DatagramPacket packet) throws IOException {
		sendReceiveSocket.receive(sendReceivePacket);
	}

	public boolean blockNumMatch(DatagramPacket packet) {
		byte[] blockNumber = { packet.getData()[2], packet.getData()[3] };
		int blockNum = blockNumber[0] + blockNumber[1];
			
		return blockNum == packetNum;
	}

	public void delayPacketErrorSim(DatagramPacket packet) throws InterruptedException {
		// byte data[] = packet.getData();
		// byte pNum[] = intToBytes(packetNum);
		// if this is not a datagram packet return; else conduct simulation
		// if (!(data[0] == 0 && data[1] == 3));
		// else {
		// if((data[1] == pNum[0] && data[2] == pNum[1]))//if this is the chosen packet
		// to delay{
		TimeUnit.SECONDS.sleep(delayTime);
		// }
	}


	private void duplicatePacketErrorSim(DatagramPacket packet) throws InterruptedException, IOException {
		TimeUnit.SECONDS.sleep(delayTime);
 		sendReceiveSocket.send(packet);
	}

	private void operationSetup() {
		Scanner input = new Scanner(System.in);

		System.out.println(
				"Choose an operation. 0: normal operation, 1: lose a packet, 2: delay a packet, 3: duplicate a packet.");
		int chosenOperation = input.nextInt();

		int packetTypeToLose = -1;
		int packetTypeToDuplicate = -1;
		int packetTypeToDelay = -1;

		switch (chosenOperation) {
		case 1:
			System.out.println("Do you want to lose a 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToLose = input.nextInt();
			if (packetTypeToLose == 2 || packetTypeToLose == 3) {
				System.out.println("Which number DATA or ACK packet should be lost during transfer: ");
				packetNum = input.nextInt();
			}
			break;
		case 2:// when packetNum
			System.out.println("Which packet should be delayed: 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToDelay = input.nextInt();
			if (packetTypeToDelay == 2 || packetTypeToDelay == 3) {
				System.out.println("Which number DATA or ACK packet should be delayed during transfer: ");
				packetNum = input.nextInt();
			}
			System.out.println("How long should the delay be in seconds: ");
			delayTime = input.nextInt();
			break;
		case 3:
			System.out.println("Do you want to duplicate a 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToDuplicate = input.nextInt();
			if (packetTypeToDuplicate == 2 || packetTypeToDuplicate == 3) {
				System.out.println("Which number DATA or ACK packet should be duplicated during transfer: ");
				packetNum = input.nextInt();
			}
			System.out.println("How long should the delay be in seconds: ");
			delayTime = input.nextInt();
			break;
		}

		input.close();

		if (packetTypeToLose == 0 || packetTypeToDelay == 0 || packetTypeToDuplicate == 0)
			rrqSim = true;
		else if (packetTypeToLose == 1 || packetTypeToDelay == 1 || packetTypeToDuplicate == 1)
			wrqSim = true;
		else if (packetTypeToLose == 2 || packetTypeToDelay == 2 || packetTypeToDuplicate == 2)
			dataSim = true;
		else if (packetTypeToLose == 3 || packetTypeToDelay == 3 || packetTypeToDuplicate == 3)
			ackSim = true;
		if (chosenOperation == 1)
			lostSim = true;
		if (chosenOperation == 2)
			delaySim = true;
		if (chosenOperation == 3)
			duplicateSim = true;

	}

	public byte[] intToBytes(int num) {
		byte[] numAsBytes = new byte[2];
		numAsBytes[1] = (byte) (num & 0xFF);
		numAsBytes[0] = (byte) ((num >> 8) & 0xFF);
		return numAsBytes;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		IntermediateHost host = new IntermediateHost();
		host.run();
	}
}