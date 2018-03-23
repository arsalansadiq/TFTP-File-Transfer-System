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

	private FileInputStream fis = null;
	String currentPath;

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

		// if (readWriteOPCode == 1) {
		// System.out.println("Enter the name of the file to be written to:");
		// fileNameToWrite = input.next();
		// filePathWrittenTo = Paths.get(currentPath, fileNameToWrite);
		// }

		input.close();
		filePathWrittenTo = Paths.get(currentPath + "\\Client", fileName);

		// filePath = Paths.get(currentPath, fileName);

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

			holdReceivingArray = new byte[512]; // 516 because 512 data + 2 byte
			// opcode + 2 byte 0's

			receivePacket = new DatagramPacket(holdReceivingArray, holdReceivingArray.length, inetAddress,
					sendReceiveSocket.getLocalPort());

			sendReceiveSocket.receive(receivePacket);

			byte[] requestCode = { holdReceivingArray[0], holdReceivingArray[1] };

			if (requestCode[0] == 0 && requestCode[1] == 5) {
				errorOccurred(receivePacket);
			}

			byte[] buffer = { holdReceivingArray[0], holdReceivingArray[1], holdReceivingArray[2],
					holdReceivingArray[3] };

			if (buffer[0] == 0 && buffer[1] == 4 && buffer[2] == 0 && buffer[3] == 0) {
				System.out.println("Acknowledgment received for our write request, now beginning to write to server.");
				beginWritingToServer();
			}

		}
		else {//Invalid request code
			System.out.println("The file request was invalid, must either be a write or read");			
		}

	}

	private void beginWritingToServer() throws IOException {

		byte[] readDataFromFile = new byte[508];

		int bytesRead = fis.read(readDataFromFile);

		int blockNumber = 1;

		System.out.println("bytes read is: " + bytesRead);
		if (bytesRead == 508) {
			sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile),
					readDataFromFile.length + 4, inetAddress, 23);
		} else {
			sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
					inetAddress, 23);
		}
		// bytesRead should contain the number of bytes read in this
		// operation
		// send data to client on random port
		sendReceiveSocket.send(sendDataPacket);
		mostRecentPacket=sendDataPacket;

		System.out.println("Client sent packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
				+ " with block number " + sendDataPacket.getData()[2] + sendDataPacket.getData()[3]);

		blockNumber++;
		bytesRead = fis.read(readDataFromFile);

		// wait for acknowledgment
		sendReceiveSocket.receive(sendDataPacket);
		//check to make sure an acknowledge was received
		int errorCount=0;
		while(sendDataPacket.getData()[1]!=4) {//loop until the recieved packet is ack... 
			errorCount++;
			if(errorCount>=5)
				{System.out.println("Critical Error,Server sent an illegal packet 5 consecutive times");System.exit(0);}
			sendReceiveSocket.send(createErrorPacket(4, "(4) Illegal TFTP operation. Expecting ACK packet",inetAddress, hostPort ));//send errorMessage to server, we recieved illegal packet type
			sendReceiveSocket.receive(sendDataPacket);	//server should re-send its ack packet...
		}
		System.out.println("Client received packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
				+ " with block number " + sendDataPacket.getData()[2] + sendDataPacket.getData()[3]);

		while (bytesRead != -1) {
			byte[] blockNumberRe = { sendDataPacket.getData()[2], sendDataPacket.getData()[3] };
			int checkBlock = byteArrToInt(blockNumberRe);

			if (sendDataPacket.getData()[0] == 0 && sendDataPacket.getData()[1] == 4
					&& checkBlock == (blockNumber - 1)) {

				System.out.println("bytes read is: " + bytesRead);
				if (bytesRead == 508) {
					sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile),
							readDataFromFile.length + 4, inetAddress, 23);
				} else {
					sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
							inetAddress, 23);
				}

				sendReceiveSocket.send(sendDataPacket);
				mostRecentPacket=sendDataPacket;
				System.out.println("Client sent packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
						+ " with block number " + sendDataPacket.getData()[2] + sendDataPacket.getData()[3]);

				// wait for acknowledgment
				sendReceiveSocket.receive(sendDataPacket);
				System.out.println("Client received packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
						+ " with block number " + sendDataPacket.getData()[2] + sendDataPacket.getData()[3]);

				blockNumber++;
				bytesRead = fis.read(readDataFromFile);
			} else if (sendDataPacket.getData()[0] == 0 && sendDataPacket.getData()[1] == 4
					&& checkBlock != (blockNumber - 1)) {
				System.out.println("DID NOT SEND ANOTHER DATA BACK");

				sendReceiveSocket.receive(sendDataPacket);
			} else if (sendDataPacket.getData()[0] == 0 && sendDataPacket.getData()[1] == 5) {//server sent packet error packet... must resend last packet and wait for server response
				errorOccurred(sendDataPacket);	//let error occurred handle this			
			} else {//the data packet is an illegal type... was expecting an ack packet received something else, send error packet to server, server should resend its last packet
				//create error packet, send to server, wait for packet
				System.out.println("ERROR OCCURRED: ILLEGAL TFTP OPERATION RECEIVED -- EXPECTING ACK PACK");
				sendReceiveSocket.send(createErrorPacket(4, "(4) Illegal TFTP operation. Expecting ack packet",inetAddress, hostPort ));//send errorMessage to server, we recieved illegal packet type
				sendReceiveSocket.receive(sendDataPacket);//after the server recieved the error message from the previous line it should resend its last packet
			}
		}

		fis.close();
		//System.out.println("Done transfer. Exiting.");
		//System.exit(0);

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
		byte[] blockNumber=new byte[2];
		holdReceivingArray = new byte[512]; // 516 because 512 data + 2 byte
		// opcode + 2 byte 0's

		receivePacket = new DatagramPacket(holdReceivingArray, holdReceivingArray.length, inetAddress,
				sendReceiveSocket.getLocalPort());
		sendReceiveSocket.receive(receivePacket);
		byte[] requestCode= new byte[2];
		do {
			// System.out.println("Packet #: " + blockNum);



			// System.out.println("client is waiting for packet");
			// System.out.println("client is still waiting");

			requestCode[0] =  holdReceivingArray[0];
			requestCode[1]=holdReceivingArray[1] ;

			if (requestCode[0] == 0 && requestCode[1] == 5) {
				errorOccurred(receivePacket);
				//server got the wrong packet, resend last packet and recieve again
			}  if (requestCode[1] == 3) { // 3 is opcode for data in packet
				blockNum++;

				blockNumber[0]=  holdReceivingArray[2];
				blockNumber[1]=holdReceivingArray[3] ;
				actualBlockNum = byteArrToInt(blockNumber);

				System.out.println("Client received block number: " + actualBlockNum);

				if (blockNum == actualBlockNum) {
					DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
					writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);

					acknowledgeToHost(byteArrToInt(blockNumber));
					sendReceiveSocket.receive(receivePacket);

				}
				else if (blockNum != actualBlockNum) {
					System.out.println("Client was expecting block number: " + blockNum + " but received block number: "
							+ actualBlockNum + ". Discarding...");
					blockNum--;
					sendReceiveSocket.receive(receivePacket);
					//acknowledgeToHost(byteArrToInt(blockNumber));
				}
			}
			else {//did not receive an error packet or data packet... packet error
				System.out.println("ERROR EXPECTING DATA PACKET RECEIVED ILLEGAL PACKET -- SEND ERROR PACKET TO HOST WAIT FOR RESPONSE");;
				sendReceiveSocket.send(createErrorPacket(4, "(3) Illegal TFTP operation. Expecting data packet",inetAddress, hostPort ));//send errorMessage to server, we recieved illegal packet type
				sendReceiveSocket.receive(receivePacket);//after the server recieved the error message from the previous line it should resend its last packet
			}

		} while (!(receivePacket.getLength() < 512));
		if(receivePacket.getLength()!=0) {
			DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
			writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);
			blockNum++;
			System.out.println("Client received block number: " + blockNum);
			acknowledgeToHost(byteArrToInt(blockNumber));
		}
		return receivingBytes;
	}

	private int byteArrToInt(byte[] blockNumber) {

		return ((byte) (blockNumber[0] & 0xFF) | (byte) ((blockNumber[1] >> 8) & 0xFF));

	}

	private void errorOccurred(DatagramPacket errorPacket) throws IOException {
		if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 1) {
			System.out.println("Error code 1: File not found. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 2) {
			System.out.println("Error code 2: Access violation. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 3) {
			System.out.println("Error code 3: Disk full or allocation exceeded. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 6) {
			System.out.println("Error code 6: File already exists. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 4) {
			//packet error occured... server recieved wrong packet resend last packet
			System.out.println("Error code 4: Server recieved wrong packet type, resending last packet.");
		//	String errorStr=new String (errorPacket.getData());
		//	char errorChar=errorStr.charAt(1);
		//	byte errorByte=(byte)errorChar;
			//should only allow this to occur so many times in a row.
		//	if((mostRecentPacket.getData()[1]+0x30)==errorByte) {//make sure mostrecent contains the packet server is expecting
				try {
					sendReceiveSocket.send(mostRecentPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				sendReceiveSocket.receive(sendDataPacket);
				receivePacket=sendDataPacket;
			//}
		//	else {
				//terminate and delete the partial file
			//}
			//wrong type of packet type was received on the server side, first two characters identify expected packet type
			//if most recent packet sent matches the type, resend... else terminate transfer and delete partial file.

		}

		int nameLength = 0;
		for (int i = 4; errorPacket.getData()[i] != 0; i++) {
			nameLength++;
		}

		byte[] packetData = new byte[nameLength];
		System.arraycopy(errorPacket.getData(), 4, packetData, 0, nameLength);
		String errorMessage = new String(packetData);

		System.out.println(errorMessage);
		System.exit(0);

	}

	private void acknowledgeToHost(int blockNum) {
		byte[] blockNumArray = new byte[2];

		blockNumArray[0] = (byte) (blockNum & 0xFF);
		blockNumArray[1] = (byte) ((blockNum >> 8) & 0xFF);

		byte[] acknowledgeCode = { 0, 4, blockNumArray[0], blockNumArray[1] };

		DatagramPacket acknowledgePacket = new DatagramPacket(acknowledgeCode, acknowledgeCode.length, inetAddress,
				receivePacket.getPort());
		try {
			sendReceiveSocket.send(acknowledgePacket);
			mostRecentPacket=acknowledgePacket;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOutReceivedFile(ByteArrayOutputStream byteArrayOutputStream, String fileName) {
		filePath = Paths.get(currentPath + "\\Client", fileName);

		// if (!Files.isWritable(filePath)) {
		// System.out.println("Cannot write to file on client side.");
		// System.exit(0);
		// }

		File file = new File(currentPath + "\\Client", fileName);

		try {
			OutputStream outputStream = new FileOutputStream(file);
			byteArrayOutputStream.writeTo(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			System.out.println("Out of memory on client side. Exiting.");
			System.exit(0);
		}
		System.out.println("Finished read transfer. Exiting.");
		System.exit(0);
	}
	public DatagramPacket createErrorPacket(int errorID, String errorMsg, InetAddress iAddy, int port) {
		byte[]errorData = createErrorPacketData(errorID, errorMsg);
		return new DatagramPacket(errorData, errorData.length, iAddy, port);
	}
	private byte[] createErrorPacketData (int errorCode, String errorMessage) {
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

	public static void main(String[] args) throws IOException {
		Client client = new Client();
		client.setup();
	}

}
