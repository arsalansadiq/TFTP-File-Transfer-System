import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;


public class ClientConnectionThread implements Runnable {

	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket;
	private DatagramPacket sendDataPacket;
	private InetAddress inetAddress = null;
	int receivePort = 88;
	private byte data[];

	public ClientConnectionThread(DatagramPacket receivePacket) {
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}

		// store parameter for later user
		this.receivePacket = receivePacket;
		this.data = receivePacket.getData();

	}

	public void run() {
		// process the packet here
		System.out.println("Thread is now running.");

		if (data[0] == 0 && data[1] == 1) {
			try {
				readRequestReceived();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (data[0] == 0 && data[1] == 2) {
			writeRequestReceived();
		}

	}

	private void writeRequestReceived() {
		sendWriteAcknowledgment();
		
	}

	private void sendWriteAcknowledgment() {
		byte[] acknowledgeCode = { 0, 4, 0, 0};

		DatagramPacket acknowledgePacket = new DatagramPacket(acknowledgeCode, acknowledgeCode.length, inetAddress, receivePacket.getPort());
		try {
			sendReceiveSocket.send(acknowledgePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void readRequestReceived() throws IOException {
		// get filename, if file is available send in chunks to client on a new
		// random port
		// wait for acknowledgment then send another chunk until transfer is
		// complete

		int nameLength = 0;
		String fileName;
		for (int i = 2; receivePacket.getData()[i] != 0; i++) {
			nameLength++;
		}

		byte[] packetData = new byte[nameLength];
		System.arraycopy(receivePacket.getData(), 2, packetData, 0, nameLength);
		fileName = new String(packetData);

		System.out.println("Received file name is: " + fileName);

		FileInputStream fis;

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current relative path is: " + currentPath);

		fis = new FileInputStream(new File(currentPath, fileName));

		byte[] readDataFromFile = new byte[512]; // 512 byte chunks

		int bytesRead = fis.read(readDataFromFile);

		int blockNumber = 1;


		while (bytesRead != -1) {
			System.out.println("bytes read is: " + bytesRead);
			if(bytesRead==512){
				sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), readDataFromFile.length, inetAddress, 23);
			}
			//else if (bytesRead<=0){
				//break;
			else{
				sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead+4, inetAddress, 23);
			}
			// bytesRead should contain the number of bytes read in this
			// operation.
			// send data to client on random port
			sendReceiveSocket.send(sendDataPacket);
			System.out.println("sending from thread to host. replying to rrq");
			//System.out.println("packet is this from thread: "+ new String(sendDataPacket.getData()));

			//wait for ack

			sendReceiveSocket.receive(sendDataPacket);
			System.out.println("acknolwdgement receiveed number " + blockNumber);

			blockNumber++;
			bytesRead = fis.read(readDataFromFile);
		}



		fis.close();

	}

	private byte[] createDataPacket(int blockNumber, byte[] readDataFromFile) {

		byte[] blockNumArray = new byte[2];

		blockNumArray[0] = (byte) (blockNumber & 0xFF);
		blockNumArray[1] = (byte) ((blockNumber >> 8) & 0xFF);

		byte[] dataToSendOPcode = {0, 3, blockNumArray[0], blockNumArray[1]} ;

		byte[] combined = new byte[dataToSendOPcode.length + readDataFromFile.length];

		for (int i = 0; i < combined.length; ++i)
		{
			combined[i] = i < dataToSendOPcode.length ? dataToSendOPcode[i] : readDataFromFile[i - dataToSendOPcode.length];
		}

		return combined;
	}

}
