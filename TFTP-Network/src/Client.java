import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
	private InetAddress inetAddress = null;
	private DatagramSocket sendReceiveSocket = null;
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;
	private DatagramPacket sendDataPacket;
	private DatagramPacket mostRecentPacket;

	private Path filePath, filePathWrittenTo;

	private final int hostPort = 23;

	private byte[] serverRequest;
	private byte[] holdReceivingArray;

	private String fileName, fileNameToWrite;
	private int safePort;
	private FileInputStream fis = null;
	String currentPath, IPAddress, dir;

	int normOrTest = 0;
	int isQuietMode = 0;
	int def;

	private DatagramPacket sendErrorPacket;

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

		System.out.println("Enter the name of the file:");
		fileName = input.next();

		Path currentRelativePath = Paths.get("");
		currentPath = currentRelativePath.toAbsolutePath().toString();

		System.out.println(
				"0: Edit settings (normal vs. test, verbose vs. quiet, IP address of server, client directory) or 1: for Default");
		def = input.nextInt();
		if (def == 0) {

			System.out.println("0: Normal, 1: Test");
			normOrTest = input.nextInt();

			System.out.println("0: Verbose, 1: Quiet");
			isQuietMode = input.nextInt();
			
			System.out.println("Enter client directory (Ex. M:\\git\\TFTP-File-Transfer-System) or type '0' for a default location:");
			dir = input.next();
			if (!dir.equalsIgnoreCase("0"))
				currentPath = dir;

			System.out.println(
					"No need to enter ip address because error simulator runs on the same computer as client, therefore using local address: \n"
							+ InetAddress.getLocalHost());
			// inetAddress = InetAddress.getByName(input.next());

		}
		
		filePathWrittenTo = Paths.get(currentPath + "\\Client", fileName);

		if (readWriteOPCode == 2) {
			Path filePath = Paths.get(currentPath + "\\Client", fileName);
			if (!Files.isReadable(filePath) && (new File(currentPath + "\\Client", fileName).exists())) {
				System.out.println("File " + fileName + " is not readable.");
				System.exit(0);
			}
			try {
				fis = new FileInputStream(new File(currentPath + "\\Client", fileName));
			} catch (FileNotFoundException e) {
				System.out
						.println("File " + fileName + " not found on client side at path " + currentPath + "\\Client");
				System.exit(0);
			}

		}

		serverRequest = createServerRequest(readWriteOPCode, fileName, "netascii");

		sendPacket = new DatagramPacket(serverRequest, serverRequest.length, inetAddress, hostPort);

		sendReceiveSocket.send(sendPacket);

		// sending completed request to server
		if (readWriteOPCode == 1) {
			// receiving file from server
			if (Files.exists(filePathWrittenTo)) {
				System.out.println("File " + fileName + " already exists on client side.");
				System.exit(0);
			}

			ByteArrayOutputStream receivingBytes = getFile();
			writeOutReceivedFile(receivingBytes, fileName);
		} else if (readWriteOPCode == 2) {

			holdReceivingArray = new byte[512];

			receivePacket = new DatagramPacket(holdReceivingArray, holdReceivingArray.length, inetAddress,
					sendReceiveSocket.getLocalPort());

			sendReceiveSocket.receive(receivePacket);
			safePort = receivePacket.getPort();

			byte[] requestCode = { holdReceivingArray[0], holdReceivingArray[1] };

			if (requestCode[0] == 0 && requestCode[1] == 5) {
				errorOccurred(receivePacket);
			}

			byte[] buffer = { holdReceivingArray[0], holdReceivingArray[1], holdReceivingArray[2],
					holdReceivingArray[3] };

			if (buffer[0] == 0 && buffer[1] == 4 && buffer[2] == 0 && buffer[3] == 0) {
				verboseTrue("Acknowledgment received for our write request, now beginning to write to server.");
				safePort = receivePacket.getPort();
				beginWritingToServer();
			}

		} else {// Invalid request code
			System.out.println("The file request was invalid, must either be a write or read");
		}

	}

	private void beginWritingToServer() throws IOException {

		byte[] readDataFromFile = new byte[508];

		int bytesRead = fis.read(readDataFromFile);

		int blockNumber = 1;

		verboseTrue("bytes read is: " + bytesRead);
		if (bytesRead == 508) {
			sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile),
					readDataFromFile.length + 4, inetAddress, 23);
		} else {
			sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
					inetAddress, 23);
		}
		// bytesRead should contain the number of bytes read in this
		// operation.
		// send data to client on random port
		sendReceiveSocket.send(sendDataPacket);
		verboseTrue("Client sent packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
				+ " with block number " + sendDataPacket.getData()[2] + sendDataPacket.getData()[3]);

		blockNumber++;
		bytesRead = fis.read(readDataFromFile);

		// wait for acknowledgment
		sendReceiveSocket.receive(receivePacket);
		verboseTrue("Client received packet: " + receivePacket.getData()[0] + receivePacket.getData()[1]
				+ " with block number " + receivePacket.getData()[2] + receivePacket.getData()[3]);

		while (bytesRead != -1) {

			if (receivePacket.getPort() != hostPort) {// packet came from unkownID... discard packet, send off
														// errorPacket
				verboseTrue("PACKET COMING FROM DIFFERENT HOST, SENDING ERROR PACKET BACK.....................");
				byte[] errorPacket = createErrorPacket(5,
						"Packet came from port: " + receivePacket.getPort() + " but expected from port: " + 23);
				sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, 23);
				try {
					sendReceiveSocket.send(sendErrorPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				// blockNum--;
				sendReceiveSocket.receive(receivePacket);

			}
			if (!(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4)
					&& !(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 5)) {
				// the packet is not a data packet and is not a error packet... therefore there
				// is a packet error
				verboseTrue("PACKET ERROR OCCURED--EXPECTING ACKNOWLEDGEMENT--SENDING ERROR PACKET");
				byte[] errorPacket = createErrorPacket(4,
						"Packet type: " + receivePacket.getData()[1] + " but expected acknowledgement packet");
				sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, hostPort);
				try {
					sendReceiveSocket.send(sendErrorPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				blockNumber--;
				sendReceiveSocket.receive(receivePacket);// wait for proper packet
			}
			if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 5) {
				errorOccurred(receivePacket);
				sendReceiveSocket.receive(receivePacket);// wait for clients response

			}

			byte[] blockNumberRe = { receivePacket.getData()[2], receivePacket.getData()[3] };
			int checkBlock = byteArrToInt(blockNumberRe);

			if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4 && checkBlock == (blockNumber - 1)) {

				verboseTrue("bytes read is: " + bytesRead);
				if (bytesRead == 508) {
					sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile),
							readDataFromFile.length + 4, inetAddress, 23);
				} else {
					sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
							inetAddress, 23);
				}

				sendReceiveSocket.send(sendDataPacket);
				verboseTrue("Client sent packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
						+ " with block number " + sendDataPacket.getData()[2]);

				// wait for acknowledgment
				sendReceiveSocket.receive(receivePacket);
				verboseTrue("Client received packet: " + receivePacket.getData()[0] + receivePacket.getData()[1]
						+ " with block number " + receivePacket.getData()[2]);

				blockNumber++;
				bytesRead = fis.read(readDataFromFile);
			} else if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4
					&& checkBlock != (blockNumber - 1)) {
				verboseTrue("DID NOT SEND ANOTHER DATA BACK");
				sendReceiveSocket.receive(receivePacket);
			}
		}

		fis.close();
		System.out.println("Done transfer. Exiting.");
		System.exit(0);

	}

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

		setupFullRequest[posInArray] = zero; // last element of request is byte
		// 0
		return setupFullRequest;

	}

	private ByteArrayOutputStream getFile() throws IOException {
		ByteArrayOutputStream receivingBytes = new ByteArrayOutputStream();
		int blockNum = 0;
		int actualBlockNum = 0;
		byte[] blockNumber = new byte[2];
		holdReceivingArray = new byte[512];

		receivePacket = new DatagramPacket(holdReceivingArray, holdReceivingArray.length, inetAddress,
				sendReceiveSocket.getLocalPort());
		sendReceiveSocket.receive(receivePacket);
		safePort = receivePacket.getPort();
		do {
			blockNum++;

			byte[] requestCode = { holdReceivingArray[0], holdReceivingArray[1] };
			if (receivePacket.getPort() != hostPort) {// packet came from unkownID... discard packet, send off
				// errorPacket
				verboseTrue("PACKET COMING FROM DIFFERENT HOST, SENDING ERROR PACKET BACK.....................");
				byte[] errorPacket = createErrorPacket(5,
						"Packet came from port: " + receivePacket.getPort() + " but expected from port: " + safePort);
				sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, 23);
				try {
					sendReceiveSocket.send(sendErrorPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				sendReceiveSocket.receive(receivePacket);
			}
			if (!(requestCode[0] == 0 && requestCode[1] == 3) && !(requestCode[0] == 0 && requestCode[1] == 5)) {
				// the packet is not a data packet and is not a error packet... therefore there
				// is a packet error
				verboseTrue("PACKET ERROR OCCURED--EXPECTING DATA--SENDING ERROR PACKET");
				byte[] errorPacket = createErrorPacket(4,
						"Packet type: " + receivePacket.getData()[1] + " but expected data packet");
				sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, hostPort);
				try {
					sendReceiveSocket.send(sendErrorPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				blockNum--;
				sendReceiveSocket.receive(receivePacket);// wait for proper packet
			}
			if (requestCode[0] == 0 && requestCode[1] == 5) {
				errorOccurred(receivePacket);
				sendReceiveSocket.receive(receivePacket);
				blockNum--;
			}

			if (requestCode[0] == 0 && requestCode[1] == 3) { // 3 is opcode for data in packet
				blockNumber[0] = holdReceivingArray[2];
				blockNumber[1] = holdReceivingArray[3];
				actualBlockNum = byteArrToInt(blockNumber);

				verboseTrue("Client received block number: " + actualBlockNum);
				System.out.println();
				if (blockNum == actualBlockNum) {
					DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
					writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);

					acknowledgeToHost(byteArrToInt(blockNumber));
					sendReceiveSocket.receive(receivePacket);

				} else if (blockNum != actualBlockNum) {
					verboseTrue("Client was expecting block number: " + blockNum + " but received block number: "
							+ actualBlockNum + ". Discarding...");
					blockNum--;
					sendReceiveSocket.receive(receivePacket);
				}
			}
		} while (!(receivePacket.getLength() < 512) || (receivePacket.getData()[1] == 5));

		if (receivePacket.getLength() != 0) {
			DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
			writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);
			blockNum++;
			verboseTrue("Client received block number: " + blockNum);
			acknowledgeToHost(byteArrToInt(blockNumber));
		}
		return receivingBytes;
	}

	private byte[] createErrorPacket(int errorCode, String errorMessage) {
		int posInErrorArray = 0;
		byte zeroByte = 0;
		byte five = 5;

		int errorPacketLength = 4 + errorMessage.length() + 1;

		byte[] setupErrorPacket = new byte[errorPacketLength];

		setupErrorPacket[posInErrorArray] = zeroByte;
		posInErrorArray++;
		setupErrorPacket[posInErrorArray] = five;
		posInErrorArray++;

		setupErrorPacket[posInErrorArray] = (byte) ((errorCode >> 8) & 0xFF);
		posInErrorArray++;
		setupErrorPacket[posInErrorArray] = (byte) (errorCode & 0xFF);
		posInErrorArray++;

		for (int i = 0; i < errorMessage.length(); i++) {
			setupErrorPacket[posInErrorArray] = (byte) errorMessage.charAt(i);
			posInErrorArray++;
		}

		setupErrorPacket[posInErrorArray] = zeroByte;

		return setupErrorPacket;

	}

	private int byteArrToInt(byte[] blockNumber) {

		return ((byte) (blockNumber[0] & 0xFF) | (byte) ((blockNumber[1] >> 8) & 0xFF));

	}

	private void errorOccurred(DatagramPacket errorPacket) throws IOException {
		if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 1) {
			verboseTrue("Error code 1: File not found. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 2) {
			verboseTrue("Error code 2: Access violation. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 3) {
			verboseTrue("Error code 3: Disk full or allocation exceeded. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 6) {
			verboseTrue("Error code 6: File already exists. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 4) {
			verboseTrue("Error code 4: Packet Error. The error message is: ");

			int nameLength = 0;
			for (int i = 4; errorPacket.getData()[i] != 0; i++) {
				nameLength++;
			}

			byte[] packetData = new byte[nameLength];
			System.arraycopy(errorPacket.getData(), 4, packetData, 0, nameLength);
			String errorMessage = new String(packetData);

			verboseTrue(errorMessage);

			sendReceiveSocket.send(sendDataPacket);// resend the last packet

			return;
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 5) {
			verboseTrue("Error code 5: Unknown TID. The error message is: ");

			int nameLength = 0;
			for (int i = 4; errorPacket.getData()[i] != 0; i++) {
				nameLength++;
			}

			byte[] packetData = new byte[nameLength];
			System.arraycopy(errorPacket.getData(), 4, packetData, 0, nameLength);
			String errorMessage = new String(packetData);

			verboseTrue(errorMessage);

			sendReceiveSocket.send(sendDataPacket);// resend the last packet

			return;
		}

		int nameLength = 0;
		for (int i = 4; errorPacket.getData()[i] != 0; i++) {
			nameLength++;
		}

		byte[] packetData = new byte[nameLength];
		System.arraycopy(errorPacket.getData(), 4, packetData, 0, nameLength);
		String errorMessage = new String(packetData);

		verboseTrue(errorMessage);

	}

	private void acknowledgeToHost(int blockNum) {
		byte[] blockNumArray = new byte[2];

		blockNumArray[0] = (byte) (blockNum & 0xFF);
		blockNumArray[1] = (byte) ((blockNum >> 8) & 0xFF);

		byte[] acknowledgeCode = { 0, 4, blockNumArray[0], blockNumArray[1] };

		sendDataPacket = new DatagramPacket(acknowledgeCode, acknowledgeCode.length, inetAddress,
				receivePacket.getPort());
		try {
			sendReceiveSocket.send(sendDataPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOutReceivedFile(ByteArrayOutputStream byteArrayOutputStream, String fileName) {
		filePathWrittenTo = Paths.get(currentPath + "\\Client", fileName);

		// if (!Files.isWritable(filePath)) {
		// System.out.println("Cannot write to file on client side.");
		// System.exit(0);
		// }

		File file = new File(currentPath + "\\Client", fileName);

		try {
			OutputStream outputStream = new FileOutputStream(file);
			byteArrayOutputStream.writeTo(outputStream);
		} catch (IOException e) {
			
		} catch (OutOfMemoryError e) {
			verboseTrue("Out of memory on client side. Exiting.");
			System.exit(0);
		}
		System.out.println("Finished read transfer.");
		// System.exit(0);

	}

	public void verboseTrue(String displayText) {
		if (isQuietMode == 0) {
			System.out.println(displayText);
		} else {

		}

	}

	public static void main(String[] args) throws IOException {
		Scanner redoInput = new Scanner(System.in);

		String input;
		Client client = new Client();
		client.setup();

		// do {
		// client.setup();
		// System.out.println("Would you like to initiate another file transfer? (Y)es
		// or (N)o ");
		// input = redoInput.nextLine();
		// }while(input.equalsIgnoreCase("Y"));
		System.out.println("Exiting....");
		System.exit(0);

	}

}