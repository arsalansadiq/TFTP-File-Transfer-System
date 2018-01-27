import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class IntermediateHost {

	private DatagramSocket receiveSocket;
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket;
	private DatagramPacket sendPacket;
	private DatagramSocket sendSocket;
	private byte data[] = new byte[4];


	public IntermediateHost() {

		try {
			receiveSocket = new DatagramSocket(23); //Initialize the receiveSocket to read from port 23
			sendReceiveSocket = new DatagramSocket(); //Initialize sendReceiveSocket
		} catch (SocketException se) { //throw exception
			se.printStackTrace();
			System.exit(0);
		}
	}

	public void receivePacket() {

		System.out.println("HOST: WAITING FOR PACKET TO BE RECEIVED FROM CLIENT...\n"); //a prompt letting the user know its waiting on data
		System.out.println("Still waiting...");

		for (;;) { //a for loop to iteratively keep reading in the requests of the user

			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length); //create a new packet


			try {
				receiveSocket.receive(receivePacket); //allow the socket to receive the packet
			} catch (IOException e) { //throw exception
				e.printStackTrace();
				System.out.println("IOException occured!");
				System.exit(0);

			}
			int clientPort = receivePacket.getPort();
			//statements that provide information about the packet
			System.out.println("\nHOST: PACKET RECEIVED FROM CLIENT"); //once a packet arrives, let the user know
			if (receivePacket.getData()[1] == 1 && receivePacket.getData()[0]==0) { //its a read request if the first element is 0 and the second element is 1 in the buffer
				System.out.println("READ REQUEST RECEIVED");
			} else if (receivePacket.getData()[1] == 2 && receivePacket.getData()[0]==0) { //its a write request if the first element is 0 and the second element is 2 in the buffer
				System.out.println("WRITE REQUEST RECEIVED");
			} else if (receivePacket.getData()[0]==1) { //otherwise its an invalid request
				System.out.println("ERROR REQUEST RECEIVED");
			}
			System.out.println("From host: " + receivePacket.getAddress()); 
			System.out.println("Host port: " + receivePacket.getPort());
			int port = receivePacket.getPort();
			int len = receivePacket.getLength();
			System.out.print("Whats being received: " ); //tell the user what message was received
			System.out.print("\nAs bytes: ");
			int k = data.length - 1;
			while (k >= 0 && data[k] == 0) { //get rid of trailing zeros
				k--;
			}
			data = Arrays.copyOf(data, k+1);
			for (int i= 0; i < data.length ; i++) {
				System.out.print(data[i]);
			}
			System.out.print("\nAs String: " + new String(data));
			System.out.println("\nLength: " + len + " bytes");
			/////////////////////////////////////////

			//-------------sending to server----------------------------
			try {
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),InetAddress.getLocalHost(), 69); // create a packet that goes through prot 69
			}catch (UnknownHostException e1) { //throws exception
				e1.printStackTrace();
				System.exit(0);
			}


			System.out.println("\nHOST: PACKET BEING SENT TO SERVER"); //describe to the user whats going on
			int len1 = sendPacket.getLength();
			System.out.print("Whats being sent: " );
			System.out.print("\nAs bytes: ");
			int k1 = sendPacket.getLength() - 1;
			while (k1 >= 0 && data[k1] == 0) {
				k1--;
			}
			byte y[] = sendPacket.getData(); 
			y = Arrays.copyOf(data, k1+1);
			for (int i= 0; i < y.length ; i++) {
				System.out.print(y[i]);
			}
			byte fileData[] = new byte[(int)y.length];
			System.out.print("\nAs String: " + new String(y));
			System.out.println("\nLength: " + len1 + " bytes");
			System.out.println("\n----------------------------------------------------------");

			//-------------------------------------------------------------------------



			try {
				sendReceiveSocket.send(sendPacket); // send the packet to the server
			}catch (IOException e) { //throws exception
				e.printStackTrace();
				System.exit(0);
			}


///////////////////////////COMMENTED BECAUSE COULDNT RECEIVE INFO FROM SERVER
			
			//receivePacket = new DatagramPacket(data, 4); //create a new packet
			
			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}

			System.out.println("Server: Packet received:");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			int len11 = receivePacket.getLength();
			System.out.println("Length: " + len11 + " bytes");
			for (int i = 0; i<4 ; i++) {
				data[i] = receivePacket.getData()[i];
			}
			
			System.out.print("Containing: "); 
			


			sendPacket = new DatagramPacket (data, 4, receivePacket.getAddress(), port);
			try {
				sendSocket = new DatagramSocket();
			} catch (SocketException se) { //throw exception
				se.printStackTrace();
				System.exit(0);
			}
//			System.out.println(sendPacket.getLength());
//			try {
//				sendReceiveSocket.receive(sendPacket);
//			} catch (IOException e) {
//				e.printStackTrace();
//				System.exit(0);
//			}
//			
			
			sendPacket = new DatagramPacket (data, 4, receivePacket.getAddress(), clientPort);
			try {
				sendReceiveSocket.send(sendPacket); // send the packet to the server
			}catch (IOException e) { //throws exception
				e.printStackTrace();
				System.exit(0);
			}
			sendSocket.close();
		}
		

	}


	public static void main(String[] args) {
		IntermediateHost host = new IntermediateHost();
		host.receivePacket();

	}

}
