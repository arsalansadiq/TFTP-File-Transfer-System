import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class IntermediateHost {
	private DatagramSocket sendReceiveSocket = null;
	private DatagramPacket sendReceivePacket;
	private final int serverPort = 69;
	private int clientPort, threadPort = 0;
	private int packetNum;//this will be the packet number to be used for the given error simulation
	
	public IntermediateHost() {

		try {
			sendReceiveSocket = new DatagramSocket(23);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}
	}

	public void run() throws IOException, InterruptedException {

		operationSetup();

		byte data[] = new byte[516];
		/*initial connection
		 * */
		 
		sendReceivePacket = new DatagramPacket(data, data.length);
		//waiting for a request from client to server
		System.out.println("Intermediate host: waiting for a packet from client");
		sendReceiveSocket.receive(sendReceivePacket);
		clientPort = sendReceivePacket.getPort();

		sendReceivePacket.setPort(serverPort);
		//send request to server
		System.out.println("Intermediate host: sending packet to server");
		sendReceiveSocket.send(sendReceivePacket);
		//waiting for response from server
		System.out.println("Intermediate host: waiting for a packet from thread");
		sendReceiveSocket.receive(sendReceivePacket);
		threadPort = sendReceivePacket.getPort();
		System.out.println("Intermediate host: received packet from thread");
		//sending response to client
		System.out.println("Intermediate host: sending packet to client");
		sendReceivePacket.setPort(clientPort);
		sendReceiveSocket.send(sendReceivePacket);

		System.out.println("\nTRANSFER HAS BEGUN.................................");
		while (true) {
			
			System.out.println("Intermediate host: waiting for a packet from client");
			sendReceiveSocket.receive(sendReceivePacket);
			delayPacketErrorSim(sendReceivePacket);
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
	private void delayPacketErrorSim(DatagramPacket packet) throws InterruptedException {
		byte data[] = packet.getData();
		byte pNum[] = intToBytes(packetNum);
		//if this is not  a datagram packet return; else conduct simulation
		if (!(data[0] == 0 && data[1] == 3));
		else {
			if((data[1] == pNum[0] && data[2] == pNum[1]))//if this is the chosen packet to delay{
				TimeUnit.SECONDS.sleep(5);
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
		

		switch (chosenOperation) {
		case 1:
			System.out.println("Do you want to lose a 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToLose = input.nextInt();
			if (packetTypeToLose == 2 || packetTypeToLose == 3) {
				System.out.println("Which number DATA or ACK packet should be lost during transfer: ");
				packetNum = input.nextInt();
			}
			break;
		case 2://when packetNum 
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
