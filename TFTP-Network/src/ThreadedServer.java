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

				receive = new String(receivePacket.getData());
				System.out.println("Server: packet received");
			} catch (IOException e) { // throws exception
				e.printStackTrace();
				System.out.println("SERVER TIMED OUT......................");
				System.exit(0);
			}
			boolean mode = false;
			String str = getFileName(receivePacket);
			String str2 = getFileName2(receivePacket);
			if (str.equals("netascii") || str2.equals("octet"))
				mode = true;

			if (receive.equals(receiveOld)) {
				System.out.println("Server: Duplicate read or write request received. Discarding...");
			}else if(!(receivePacket.getData()[0]==0 && (receivePacket.getData()[1]==1 || receivePacket.getData()[1]==2))){ 
				System.out.println("Invalid connection request recieved");
				
			}else if(!mode) {
				System.out.println("Invalid mode recieved");
			}
			else {
				Runnable newClient = new ClientConnectionThread(receivePacket);
				new Thread(newClient).start();
			}
			receiveOld = receive;
		}
	}

	public void close() {
		receiveSocket.close();
	}
	
	private String getFileName(DatagramPacket fileNamePacket) {
		int nameLengthend = (fileNamePacket.getLength()) - 1;
		int nameLengthBegin = nameLengthend - 8;
		

		byte[] packetData = new byte[8];
		System.arraycopy(receivePacket.getData(), nameLengthBegin, packetData, 0, 8);
		return new String(packetData);
	}
	
	private String getFileName2(DatagramPacket fileNamePacket) {
		int nameLengthend = (fileNamePacket.getLength()) - 1;
		int nameLengthBegin = nameLengthend - 5;
		

		byte[] packetData = new byte[5];
		System.arraycopy(receivePacket.getData(), nameLengthBegin, packetData, 0, 5);
		return new String(packetData);
	}

	public static void main(String[] args) {
		ThreadedServer server = new ThreadedServer();
		server.receivingServer();
	}

}