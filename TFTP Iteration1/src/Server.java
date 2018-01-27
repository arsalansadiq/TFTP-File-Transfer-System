   

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Server {

	//declaration of variables
	private DatagramSocket receiveSocket;
	//private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket;
	private DatagramPacket sendPacket;
	private DatagramSocket sendSocket;
	private byte data[];
	byte valid[] = {0, 3, 0, 1};
	byte invalid[] = {0, 4, 0, 0};
	byte response[];

	public Server() {
		try {
			receiveSocket = new DatagramSocket(69); //create a socket that reads from 69
		} catch (SocketException se) { //throws exception
			se.printStackTrace();
			System.exit(0);
		}
	}

	public void receivingServer() {
		data = new byte[128];

		System.out.println("SERVER: WAITING FOR PACKET TO BE RECEIVED FROM HOST...\n"); //let the user know that the server is waitng for data from the host
		System.out.println("Still waiting...");

		for (;;) {
			receivePacket = new DatagramPacket(data, data.length); //create a new packet to be received

			try {

				receiveSocket.receive(receivePacket); //send a receive request
			} catch (IOException e) { //throws exception
				e.printStackTrace();
				System.out.println("IOException occured!");
				System.exit(0);

			}

			System.out.println("\nSERVER: PACKET RECEIVED FROM HOST");
			if (receivePacket.getData()[1] == 1 && receivePacket.getData()[0] == 0) { //check the buffer of the received packet to see if its a read 
				System.out.println("READ REQUEST RECEIVED");
			} else if (receivePacket.getData()[1] == 2 && receivePacket.getData()[0] == 0) {//check the buffer of the received packet to see if its a write
				System.out.println("WRITE REQUEST RECEIVED");
			} else if (receivePacket.getData()[0] == 1) {
				System.out.println("ERROR REQUEST RECEIVED"); //or if its an error
			}
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.print("Whats being received: " );
			System.out.print("\nAs bytes: ");
			int k = data.length - 1;
			while (k >= 0 && data[k] == 0) {
				k--;
			}
			data = Arrays.copyOf(data, k+1);
			for (int i= 0; i < data.length ; i++) {
				System.out.print(data[i]);
			}
			System.out.print("\nAs String: " + new String(data));
			System.out.println("\nLength: " + len + " bytes");

			//--------------------------------------------------------------

			if (receivePacket.getData()[1] == 1 && receivePacket.getData()[0] == 0) { //check if its a valid read, if it is send bytes 0301
				response = valid;
				sendPacket = new DatagramPacket(valid, 4,receivePacket.getAddress(), receivePacket.getPort());
				//System.out.println("Response: " + Arrays.toString(valid) );
				
			} else if (receivePacket.getData()[1] == 2 && receivePacket.getData()[0] == 0) { //if valid write send bytes 0400
				
				response = invalid;
				sendPacket = new DatagramPacket(invalid, 4,receivePacket.getAddress(), receivePacket.getPort());
				//System.out.println("Response: " + Arrays.toString(invalid));
				
			} else {
				response = null;
			}
			
			//-----------------------------------------------------------------------------------------------------------------------------------------------------------------


			

			//send the new packet containing 4 bytes back to the intermediate host
			System.out.println("\nSERVER: PACKET SENT TO HOST");
			int len1 = sendPacket.getLength();
			System.out.print("Whats being sent: " );
			//System.out.println(Arrays.toString(sendPacket.getData()));
			System.out.print("\nAs bytes: " + Arrays.toString(response));
			System.out.println("\nLength: " + len1 + " bytes");
			System.out.println("\n----------------------------------------------------------");

			try {
				sendSocket = new DatagramSocket();
			} catch (SocketException se) { //throws exception
				se.printStackTrace();
				System.exit(0);
			}
			
			try {

				sendSocket.send(sendPacket); //send the packet back
			} catch (IOException e) { //throw exception
				e.printStackTrace();
				System.out.println("IOException occured!");
				System.exit(0);
			}
			sendSocket.close();

		}
			
		//receiveSocket.close(); //close the sockets that are not needed anymore


	}


	public static void main(String[] args) {
		Server server = new Server();
		server.receivingServer();
	}

}