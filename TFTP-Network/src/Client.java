import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	private InetAddress inetAddress = null;
	private DatagramSocket sendReceiveSocket = null;
	private Packet sendPacket;
	private Packet receivePacket;


	private Path filePath, filePathWrittenTo;//shouldnt be instance variable

	private final int hostPort = 23;

	private byte[] serverRequest;
	private byte[] holdReceivingArray;

	private String fileName, fileNameToWrite;

	private FileInputStream fis = null;
	String currentPath;

	private void setup() throws IOException {

		int readWriteOPCode = 0;

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

		System.out.println("Enter the name of the file:");
		fileName = input.next();

		Path currentRelativePath = Paths.get("");
		currentPath = currentRelativePath.toAbsolutePath().toString();

		if (readWriteOPCode == 1) {
			System.out.println("Enter the name of the file to be written to:");
			fileNameToWrite = input.next();
			filePathWrittenTo = Paths.get(currentPath, fileNameToWrite);
		}

		input.close();



		filePath = Paths.get(currentPath, fileName);

		if (readWriteOPCode == 2) {
			Path filePath = Paths.get(currentPath, fileName);
			if (!Files.isReadable(filePath) && (new File(fileName)).exists()) {
				System.out.println("File " + fileName + " access denied");
				System.exit(0);
			}
			try {
				fis = new FileInputStream(new File(currentPath, fileName));
			} catch (FileNotFoundException e) {
				System.out.println("File " + fileName + " not found on client side at path " + currentPath);
				System.exit(0);
			}

		}

		//serverRequest = createServerRequest(readWriteOPCode, fileName, "netascii");
		sendPacket.setPacketData(sendPacket.setCode(0, readWriteOPCode), fileName.getBytes(), "netascii".getBytes());
	//	sendPacket = new DatagramPacket(serverRequest, serverRequest.length, inetAddress, hostPort);

		sendReceiveSocket.send(sendPacket.usePacket());

		// sending completed request to server
		if (readWriteOPCode == 1) {
			// receiving file from server
			if (Files.exists(filePathWrittenTo)) {
				System.out.println("File " + fileNameToWrite + " already exists on client side");
				System.exit(0);
			}

			ByteArrayOutputStream receivingBytes = getFile();
			writeOutReceivedFile(receivingBytes, fileNameToWrite);
		} else if (readWriteOPCode == 2) {

			holdReceivingArray = new byte[516]; // 516 because 512 data + 2 byte
			// opcode + 2 byte 0's

			sendReceiveSocket.receive(receivePacket.usePacket());

			if (receivePacket.getPacketType()[0] == 0 && receivePacket.getPacketType()[1] == 5) {
				errorOccurred(receivePacket);
			}

			byte[] buffer = { holdReceivingArray[0], holdReceivingArray[1],holdReceivingArray[2], holdReceivingArray[3] };
			if (receivePacket.getPacketType()[0] == 0 && receivePacket.getPacketType()[1] == 4) {//ack pack received

			//if (buffer[0] == 0 && buffer[1] == 4 && buffer[2] == 0 && buffer[3] == 0) {
				System.out.println("Acknowledgment received for our write request, now beginning to write to server.");
				beginWritingToServer();
			}

		}

	}

	private void beginWritingToServer() throws IOException {

		byte[] readDataFromFile = new byte[508]; // 512 byte chunks//we used 516 in client server thread...

		int bytesRead = fis.read(readDataFromFile);

		int blockNumber = 1;

		while (bytesRead != -1) {
			System.out.println("bytes read is: " + bytesRead);
			if (bytesRead == 512) {
				sendPacket.setPacketData(sendPacket.setCode(0, 3), sendPacket.setCode(0, blockNumber), readDataFromFile);
				/*sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile),
						readDataFromFile.length, inetAddress, 23);*/
			} else {
				sendPacket.setPacketData(sendPacket.setCode(0, 3), sendPacket.setCode(0, blockNumber), readDataFromFile);

				/*sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
						inetAddress, 23);why are we adding 4 to the length*/
			}
			// bytesRead should contain the number of bytes read in this
			// operation.
			// send data to client on random port
			sendReceiveSocket.send(sendPacket.usePacket());
			System.out.println("Sent data packet to server, writing to server.");

			// wait for acknowledgment

			sendReceiveSocket.receive(receivePacket.usePacket());
			if (receivePacket.getPacketType()[0] == 0 && receivePacket.getPacketType()[1] == 4) {//ack pack received

			//if (sendDataPacket.getData()[0] == 0 && sendDataPacket.getData()[1] == 4) {
				int checkBlock = receivePacket.getPacketCode()[0] + receivePacket.getPacketCode()[1];
				System.out
						.println("Acknowledgment received for our write in progress with block number: " + checkBlock);
				if (blockNumber != checkBlock) {
					// errorOccurred();
				}
			}

			blockNumber++;
			bytesRead = fis.read(readDataFromFile);
		}

		fis.close();

	}
/*
	private byte[] createDataPacket(int blockNumber, byte[] readDataFromFile) {

		byte[] blockNumArray = new byte[2];

		blockNumArray[0] = (byte) (blockNumber & 0xFF);
		blockNumArray[1] = (byte) ((blockNumber >> 8) & 0xFF);

		byte[] dataToSendOPcode = { 0, 3, blockNumArray[0], blockNumArray[1] };

		byte[] combined = new byte[dataToSendOPcode.length + readDataFromFile.length];

		for (int i = 0; i < combined.length; ++i) {
			combined[i] = i < dataToSendOPcode.length ? dataToSendOPcode[i]
					: readDataFromFile[i - dataToSendOPcode.length];
		}

		return combined;
	}*/
/*
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

		setupFullRequest[posInArray] = zero; // last element of request is byte
												// 0
		return setupFullRequest;

	}*/
/*
	private byte[] createErrorPacket(int errorCode, String errorMessage) {
		int posInErrorArray = 0;
		byte zeroByte = 0;
		byte errorOpCode = 05;
		byte[] errorCodeAsBytes = new byte[2];

		int errorPacketLength = 4 + errorMessage.length() + 1;

		byte[] setupErrorPacket = new byte[errorPacketLength];
		setupErrorPacket[posInErrorArray] = errorOpCode;
		posInErrorArray++;
		posInErrorArray++;

		setupErrorPacket[posInErrorArray] = (byte) errorCode;

		for (int i = 0; i < errorMessage.length(); i++) {
			setupErrorPacket[posInErrorArray] = (byte) errorMessage.charAt(i);
			posInErrorArray++;
		}

		setupErrorPacket[posInErrorArray] = (byte) (errorCode & 0xFF);
		posInErrorArray++;
		setupErrorPacket[posInErrorArray] = (byte) ((errorCode >> 8) & 0xFF);
		posInErrorArray++;

		setupErrorPacket[posInErrorArray] = zeroByte;

		return setupErrorPacket;

	}*/

	private ByteArrayOutputStream getFile() throws IOException {
		ByteArrayOutputStream receivingBytes = new ByteArrayOutputStream();
		int blockNum = 1;

		do {
			System.out.println("Packet #: " + blockNum);
			blockNum++;

			holdReceivingArray = new byte[516]; // 516 because 512 data + 2 byte
												// opcode + 2 byte 0's


			sendReceiveSocket.receive(receivePacket.usePacket());

			if (receivePacket.getPacketType()[0] == 0 && receivePacket.getPacketType()[1] == 5) {
				errorOccurred(receivePacket);
			} else if (receivePacket.getPacketType()[1] == 3) { // 3 is opcode for data in packet
				DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
				writeOutBytes.write(receivePacket.getPacketData(), 0, receivePacket.getPacketData().length);

				acknowledgeToHost(receivePacket.getPacketCode());
			}

		} while (!(receivePacket.getPacketData().length < 512));
		return receivingBytes;
	}

	private void errorOccurred(Packet errorPacket) {
		System.out.println("The error message in the packet is: "+new String(errorPacket.getPacketData()));
		System.out.println("The error code in the packet is"+ new String(errorPacket.getPacketCode()));

	}

	private void acknowledgeToHost(byte[] blockNum) {
		byte[] acknowledgeCode = { 0, 4, blockNum[0], blockNum[1] };
		sendPacket.setPacketData(sendPacket.setCode(0,4), blockNum, new byte[1]);
		try {
			sendReceiveSocket.send(sendPacket.usePacket());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOutReceivedFile(ByteArrayOutputStream byteArrayOutputStream, String fileName) {

		if (!Files.isWritable(filePath)) {
			System.out.println("Cannot write to file on client side.");
			System.exit(0);
		}

		try {
			OutputStream outputStream = new FileOutputStream(fileName);
			byteArrayOutputStream.writeTo(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			System.out.println("Out of memory on client side. Exiting.");

			System.exit(0);
		}
	}

	public static void main(String[] args) throws IOException {
		Client client = new Client();
		client.setup();
	}

}
