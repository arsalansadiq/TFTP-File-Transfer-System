import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {

	//Declaration of variables
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;
	private final int hostPort=23;
	byte buffer[]  = new byte [128];
	//private Scanner input;

	public Client() {

		try {
			sendReceiveSocket = new DatagramSocket(); //creation of a sending and receiving socket
		} catch (SocketException se) { //throw exception
			se.printStackTrace();
			System.exit(0);
		}
	}


	public void setup (){
		Scanner input = new Scanner(System.in);
		System.out.println("(R)EAD or (W)RITE");
		String request = input.next();
		input = new Scanner(System.in);
		System.out.println("Enter the name of the file");
		String fileName = input.next();
		if (request.equalsIgnoreCase("r")){
			readRequest(fileName);	
		}
		else if(request.equalsIgnoreCase("w")){

		}



	}
	///////////////////////////I/O Streams//////////////////////////////////////
	public FileInputStream OpenIFile(String path){
		try { 
			return new FileInputStream(path); 			 
		} catch (FileNotFoundException e) {
			System.out.println("Error making file input stream. --TERMINATING--"); 
			e.printStackTrace();
			System.exit(0);
			return null; 
		}
	}
	public FileOutputStream makeOutputFileStream(String dir){
		File directory = new File(dir);
		if(!directory.exists()){
			try { 
				directory.createNewFile(); 
			} 
			catch (IOException e) { 
				System.out.println("Error creating new file for output stream. -- TERMINATING --");
				e.printStackTrace();
				System.exit(0);
			}
		}else {
			System.out.println("directory already exists -- terminating --");//should be changed
			System.exit(0);
		}

		try { 
			return new FileOutputStream(directory);
		} catch (FileNotFoundException e) {
			//The code should just quit if it somehow comes to this.
			System.out.println("Error making fileoutput stream -- terminating");
			e.printStackTrace(); 
			System.exit(0);
		}
		return null;
	}
	///////////////////////////I/O Streams//////////////////////////////////////


	public void readRequest(String fileName) {
		//if sending a read request, the client has to create a FileOutputStream to a file
		//which will be holding the data read from the file received by the read request
		FileOutputStream writingToFile =makeOutputFileStream(fileName);
		//now must create a datagram packet and send it to the host, (01 for read followed by the filename to be read)
		sendPacket(createRRDatagramPacket(fileName));

	}
	public DatagramPacket createRRDatagramPacket(String accessingFileName) {
		buffer[0]= 0; 
		buffer[1] = 1;
		byte fileAccessName[]=accessingFileName.getBytes();
		System.arraycopy(fileAccessName, 0, buffer, 2, fileAccessName.length);
		try {
			return new DatagramPacket(buffer, buffer.length,InetAddress.getLocalHost(),  hostPort);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.out.println("Error in createRRdatagrampacket terminating");
			e.printStackTrace();
			System.exit(0);
			return null;
		}		
	}

	public void sendPacket(DatagramPacket packet){
		System.out.println("CLIENT: PACKET SENT TO SERVER");
		if (buffer[1] == 1) { //if the 2nd element of the buffer is 1, tell the user its a read request
			System.out.println("READ REQUEST SENT");
		} else if (buffer[1] == 2) { //otherwise its a write request
			System.out.println("WRITE REQUEST SENT");
		}
		System.out.println("From host: " + packet.getAddress()); //tell the user where the packet is from
		System.out.println("Host port: " + packet.getPort()); //which port its coming from
		int len = packet.getLength();
		System.out.print("Whats being sent: "); //and what is being sent into the server
		for (int i= 0; i < buffer.length ; i++) {
			System.out.print(buffer[i]);
		}
		System.out.println("\nLength: " + len + " bytes\n"); //the length in bytes is also provided
		try {
			sendReceiveSocket.send(packet); // once user has all the information, send the packet to the intermediate host
		}catch (IOException e) { // throw exception
			e.printStackTrace();
			System.exit(0);
		}

	}
/**
	public void createDatagramPacket()  {

		//creation of buffer to be sent/received
		String s = "test.txt";
		buffer[0]= 0; 
		buffer[1] = 1;

		byte msg[] = s.getBytes();
		System.arraycopy(msg, 0, buffer, 2, msg.length);

		int pos = msg.length + 2;
		buffer[pos] = 0;

		String mode = "ocTEt";
		byte [] modeMsg = mode.getBytes();
		System.arraycopy(modeMsg, 0, buffer, pos + 1, modeMsg.length);

		int newPos = msg.length + modeMsg.length + 3;
		buffer[newPos] = 0;

		//SET ALL OTHER NUMBER TO NULL
		int k = buffer.length - 1;
		while (k >= 0 && buffer[k] == 0) {
			k--;
		}
		buffer = Arrays.copyOf(buffer, k+1);




		try {
			sendPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), 23); //create a new packet to be sent down port 23
		} catch (UnknownHostException e1) { //throw exception
			e1.printStackTrace();
			System.exit(0);
		}

		//Print statements to tell the user that the packet had been sent, containing all its information
		System.out.println("CLIENT: PACKET SENT TO SERVER");
		if (buffer[1] == 1) { //if the 2nd element of the buffer is 1, tell the user its a read request
			System.out.println("READ REQUEST SENT");
		} else if (buffer[1] == 2) { //otherwise its a write request
			System.out.println("WRITE REQUEST SENT");
		}
		System.out.println("From host: " + sendPacket.getAddress()); //tell the user where the packet is from
		System.out.println("Host port: " + sendPacket.getPort()); //which port its coming from
		int len = sendPacket.getLength();
		System.out.print("Whats being sent: "); //and what is being sent into the server
		for (int i= 0; i < buffer.length ; i++) {
			System.out.print(buffer[i]);
		}
		System.out.println("\nLength: " + len + " bytes\n"); //the length in bytes is also provided




		try {
			sendReceiveSocket.send(sendPacket); // once user has all the information, send the packet to the intermediate host
		}catch (IOException e) { // throw exception
			e.printStackTrace();
			System.exit(0);
		}

		byte[] data = new byte[128]; 
		receivePacket = new DatagramPacket(data, data.length); //initiate a new packet

		//=======================================================

		//		try {
		//			sendReceiveSocket.send(sendPacket); //then send this error request
		//		}catch (IOException e) {
		//			e.printStackTrace();
		//			System.exit(0);
		//		}

		////////////////////////////////////COMMENTED THIS BECAUSE COULDNT FIND A WAY TO SEND PACKETS FROM SERVER TO INTERMEIDATE HOST
		try {
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		System.out.println("Client: Packet received:");
		System.out.println("From host: " +
				receivePacket.getAddress());
		System.out.println("Host port: " +
				receivePacket.getPort());
		int len3 = receivePacket.getLength();
		System.out.println("Length: " + len3 + " bytes");
		System.out.print("Containing: " );


		sendReceiveSocket.close(); //finally since we are done using this socket, close it so we can use it for other things


	}
*/
	public static void main(String[] args) {

		Client client = new Client (); //create an instance of the client
		client.setup();
		//client.createDatagramPacket(); //call the method that sends packets
	}

}