import java.io.*;
import java.net.*;
import java.util.Arrays;

public class ClientConnectionThread implements Runnable {
	private DatagramPacket receivePacket;
	private byte data[];

	public ClientConnectionThread(DatagramPacket receivePacket) {
		// store parameter for later user
		this.receivePacket = receivePacket;
		this.data = receivePacket.getData();
	}

	public void run() {
		//process the packet here
		System.out.println("this thread is now running");
		
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

	}

}
