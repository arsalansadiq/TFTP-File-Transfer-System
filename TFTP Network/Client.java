import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
	private InetAddress inetAddress = null;
	private DatagramSocket sendReceiveSocket = null;
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;
	private DatagramPacket receiveAckPacket;
	private DatagramPacket sendDataPacket;

	private final int hostPort = 23;

	private byte[] serverRequest;
	private byte[] holdReceivingArray;
	
	String fileName;

	private void setup() throws IOException {
		byte readWriteOPCode = 00;

		inetAddress = InetAddress.getLocalHost();
		sendReceiveSocket = new DatagramSocket();

		Scanner input = new Scanner(System.in);
		System.out.println("(R)EAD or (W)RITE");
		String requestWR = input.next();
		if (requestWR.equals("R") || requestWR.equals("r")) {
			readWriteOPCode = 1;
		} else if (requestWR.equals("W") || requestWR.equals("w")) {
			readWriteOPCode = 2;
		}

		System.out.println("Enter the name of the file");
		fileName = input.next();

		input.close();

		serverRequest = createServerRequest(readWriteOPCode, fileName, "netascii");

		// sending completed request to server
		if (readWriteOPCode == 1){
		sendPacket = new DatagramPacket(serverRequest, serverRequest.length, inetAddress, hostPort);
		sendReceiveSocket.send(sendPacket);
		// receiving file from server
		ByteArrayOutputStream receivingBytes = getFile();

		//writeOutReceivedFile(receivingBytes, fileName);
		writeOutReceivedFile(receivingBytes, "writetest.txt");
		}
		else if(readWriteOPCode == 2){
			sendPacket = new DatagramPacket(serverRequest, serverRequest.length, inetAddress, hostPort);
			sendReceiveSocket.send(sendPacket);
			byte buffer[] = new byte [4];
			receiveAckPacket = new DatagramPacket(buffer, buffer.length);
		sendReceiveSocket.receive(receiveAckPacket);
		System.out.println("Ack Received for write request");
		writeRequestReceived();

		}
		
		
		



	}
	
	private void writeRequestReceived() throws IOException {
		// get filename, if file is available send in chunks to client on a new
		// random port
		// wait for acknowledgment then send another chunk until transfer is
		// complete

//		int nameLength = 0;
//		String fileName;
//		for (int i = 2; receivePacket.getData()[i] != 0; i++) {
//			nameLength++;
//		}
//
//		byte[] packetData = new byte[nameLength];
//		System.arraycopy(receivePacket.getData(), 2, packetData, 0, nameLength);
//		fileName = new String(packetData);
//
//		System.out.println("Received file name is: " + fileName);

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
			if(bytesRead<516){
				sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead, inetAddress, 23);
			}
			//else if (bytesRead<=0){
				//break;
			else{
				sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), readDataFromFile.length, inetAddress, 23);
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

	private byte[] createServerRequest(byte readWriteOPCode, String fileName, String modeType) {
		int posInArray = 0;
		byte zero = 0;

		int serverRequestLength = 4 + fileName.length() + modeType.length();

		byte[] setupFullRequest = new byte[serverRequestLength];

		setupFullRequest[posInArray] = zero; // first element
		posInArray++;
		setupFullRequest[posInArray] = readWriteOPCode; // 1 or 2
		posInArray++;

		// appending filename to byte array
		for (int i = 0; i < fileName.length(); i++) {
			setupFullRequest[posInArray] = (byte) fileName.charAt(i);
			posInArray++;
		}

		setupFullRequest[posInArray] = zero;
		posInArray++;

		// appending mode type to byte array
		for (int j = 0; j < modeType.length(); j++) {
			setupFullRequest[posInArray] = (byte) modeType.charAt(j);
			posInArray++;
		}

		setupFullRequest[posInArray] = zero; //last element of request is byte 0
		return setupFullRequest;

	}

	private ByteArrayOutputStream getFile() throws IOException {
		ByteArrayOutputStream receivingBytes = new ByteArrayOutputStream();
		int blockNum = 1;

		do {
			System.out.println("Packet #: " + blockNum);
			blockNum++;

			holdReceivingArray = new byte[516]; // 516 because 512 data + 2 byte opcode + 2 byte 0's

			receivePacket = new DatagramPacket(holdReceivingArray, holdReceivingArray.length, inetAddress,
					sendReceiveSocket.getLocalPort());

			sendReceiveSocket.receive(receivePacket);

			byte[] requestCode = { holdReceivingArray[0], holdReceivingArray[1] };

			if (requestCode[1] == 5) { // 5 is opcode for error in packet
				errorOccurred();
			} else if (requestCode[1] == 3) { // 3 is opcode for data in packet
				byte[] blockNumber = {holdReceivingArray[2], holdReceivingArray[3]};

				DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
				writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);

				acknowledgeToHost(blockNumber);
			}

		} while (!(receivePacket.getLength() < 512));
		return receivingBytes;
	}

	private void errorOccurred() {

	}

	private void acknowledgeToHost(byte[] blockNum) {
		byte[] acknowledgeCode = { 0, 4, blockNum[0], blockNum[1] };

		DatagramPacket acknowledgePacket = new DatagramPacket(acknowledgeCode, acknowledgeCode.length, inetAddress,
				receivePacket.getPort());
		try {
			sendReceiveSocket.send(acknowledgePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOutReceivedFile(ByteArrayOutputStream byteArrayOutputStream, String fileName) {
		try {
			OutputStream outputStream = new FileOutputStream(fileName);
			byteArrayOutputStream.writeTo(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		Client client = new Client();
		client.setup();
	}

}
