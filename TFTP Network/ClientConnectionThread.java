import java.io.*;
import java.net.*;
import java.util.Arrays;

public class ClientConnectionThread implements Runnable {
	private DatagramPacket receivePacket;
	private byte data[];
	private byte msg[];
	private DatagramPacket sendPacket;
	
	public ClientConnectionThread(DatagramPacket receivePacket) {
		// store parameter for later user
		this.receivePacket = receivePacket;
		this.data = receivePacket.getData();
	
	}

	public void run() {
		//process the packet here
		System.out.println("this thread is now running");
		deCodePacket(receivePacket);
		/*
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

		//-------------------------------------------------------------- decoding of byte array
		
		if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 1){
			byte[] fileName= new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), 2, fileName, 0, fileName.length);
			
			System.out.println("FileName: " + new String(fileName));
			String fileNameString = new String (fileName);
			FileInputStream dataStream = makeInputStream(fileNameString);			
			byte fileContent[]=new byte[(int) (new File(fileNameString)).length()];
			try {
				dataStream.read(fileContent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Error writing from data stream to byte array in server");
				e.printStackTrace();
			}
			System.out.println(fileContent);
		}*/

	}
	public FileInputStream makeInputStream(String fileNameString){
		try { 
			//gotta fix directory
			//jeffs directory:C:\Users\Jeff\Documents\GitHub\TFTP-File-Transfer-System\TFTP Network
			return new FileInputStream(new File("C:/Users/alybarolia/Documents/text", fileNameString)); 			 
		} catch (FileNotFoundException e) {
			System.out.println("Error making file input stream. --TERMINATING--"); 
			e.printStackTrace();
			System.exit(0);
			return null; 
		}
	}
	public void deCodePacket(DatagramPacket packet){
		int dataLength=0;
		String packetString;
		//find the length of the data portion of the packet
		//test contents of packet
		System.out.println(new String(packet.getData()));
		for(int i=2;packet.getData()[i]!=0;i++){
			dataLength++;
		}
		System.out.println("Data length = "+dataLength);
		byte[] packetData=new byte[dataLength];
		System.arraycopy(packet.getData(), 2, packetData, 0, dataLength);
		packetString = new String(packetData);

	//packetString = new String(packetData);
		if (packet.getData()[1] == 1 && packet.getData()[0] == 0) { //check the buffer of the received packet to see if its a read 
			System.out.println("READ REQUEST RECEIVED");
			System.out.println("THE CLIENT Port IS *****" + packet.getPort());
			readRequestRecieved(packetString, packet.getAddress(), packet.getPort());
		} else if (packet.getData()[1] == 2 && packet.getData()[0] == 0) {//check the buffer of the received packet to see if its a write
			System.out.println("WRITE REQUEST RECEIVED");
		} else if (packet.getData()[0] == 1) {
			System.out.println("ERROR REQUEST RECEIVED"); //or if its an error
		}
	}
	public void readRequestRecieved(String fileName, InetAddress clientAddress, int clientPort){
		FileInputStream dataStream = makeInputStream(fileName);	
		byte fileContent[] = new byte[(int) (new File(fileName)).length()];
		try {
			dataStream.read(fileContent);
			System.out.println("Datastream created");
			writeToClient(fileContent, clientAddress, clientPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error writing from designated file using data stream to byte array in method:readRequestRecieved");
			e.printStackTrace();
		}

	}
	public void writeToClient(byte [] fileContent,InetAddress clientAddress, int clientPort) throws SocketException{
		DatagramSocket tempSoc = new DatagramSocket();
		byte[] bytesToWrite=new byte[512];
		//int fileContentIndex=0;
		for(int i=fileContent.length ; i>=0 ; i-=512){
			bytesToWrite=fileContent;
			System.out.println(clientPort);
			sendPacket = new DatagramPacket(bytesToWrite,bytesToWrite.length, clientAddress, clientPort);
			
			try {
				tempSoc.send(sendPacket); // once user has all the information, send the packet to the intermediate host
			}catch (IOException e) { // throw exception
				e.printStackTrace();
				System.exit(0);
			}
			
			
		}
		tempSoc.close();
	}
}