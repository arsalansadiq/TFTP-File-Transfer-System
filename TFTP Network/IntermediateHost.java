import java.io.*;
import java.net.*;

public class IntermediateHost {
	private DatagramSocket sendReceiveSocket = null;
	private DatagramPacket receivePacket;
	private InetAddress inetAddress = null;
	private final int serverPort=69;
	public IntermediateHost() {

		try {
			sendReceiveSocket = new DatagramSocket(23);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}
		
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void run() throws IOException{
		byte data[] = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		int clientPort, serverThreadPort;
		while(true){
			System.out.println("HOST: WAITING FOR PACKET TO BE RECEIVED FROM CLIENT...\n");
			sendReceiveSocket.receive(receivePacket);
			clientPort=receivePacket.getPort();
			System.out.println("Host: received packet from client port:" + clientPort);
			System.out.println("Sending packet to server port:" + serverPort);
			receivePacket.setPort(serverPort);
			sendReceiveSocket.send(receivePacket);
			System.out.println("HOST: WAITING FOR PACKET TO BE RECEIVED FROM SERVER...\n");
			sendReceiveSocket.receive(receivePacket);
			serverThreadPort=receivePacket.getPort();
			System.out.println("Host: received packet from server thread port:" + serverThreadPort);
			System.out.println("Host: the received packet from thread has data: " + new String(receivePacket.getData()));
			System.out.println("Sending packet to client port:" + clientPort);
			receivePacket.setPort(clientPort);
			sendReceiveSocket.send(receivePacket);

		}
	}
	public void receivePacket() throws IOException {
		System.out.println("HOST: WAITING FOR PACKET TO BE RECEIVED FROM CLIENT...\n");

		while (true) {
			byte data[] = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);
			sendReceiveSocket.receive(receivePacket);
			
			System.out.println("Host: received packet" + receivePacket.getPort());
		}

	}
	
	public static void main(String[] args) throws IOException {
		IntermediateHost host = new IntermediateHost();
		host.run();

	}

}
