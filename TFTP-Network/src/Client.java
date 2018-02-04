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

	String fileName, fileNameToWrite;

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

		if (readWriteOPCode == 1) {
			System.out.println("Enter the name of the file to be written to:");
			fileNameToWrite = input.next();
		}

		input.close();

		serverRequest = createServerRequest(readWriteOPCode, fileName, "netascii");

		sendPacket = new DatagramPacket(serverRequest, serverRequest.length, inetAddress, hostPort);

		sendReceiveSocket.send(sendPacket);

		// sending completed request to server
		if (readWriteOPCode == 1) {
			// receiving file from server
			ByteArrayOutputStream receivingBytes = getFile();
			writeOutReceivedFile(receivingBytes, fileNameToWrite);
		} else if (readWriteOPCode == 2) {
			byte buffer[] = new byte[4];
			receiveAckPacket = new DatagramPacket(buffer, buffer.length);
			sendReceiveSocket.receive(receiveAckPacket);

			if (buffer[0] == 0 && buffer[1] == 4 && buffer[2] == 0 && buffer[3] == 0) {
				System.out.println("Acknowledgment received for our write request, now beginning to write to server.");
				beginWritingToServer();
			}

		}

	}

	private void beginWritingToServer() throws IOException {

		FileInputStream fis;

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current relative path is: " + currentPath);

		fis = new FileInputStream(new File(currentPath, fileName));

		byte[] readDataFromFile = new byte[508]; // 512 byte chunks

		int bytesRead = fis.read(readDataFromFile);

		int blockNumber = 1;

		while (bytesRead != -1) {
			System.out.println("bytes read is: " + bytesRead);
			if (bytesRead == 512) {
				sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile),
						readDataFromFile.length, inetAddress, 23);
			} else {
				sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
						inetAddress, 23);
			}
			// bytesRead should contain the number of bytes read in this
			// operation.
			// send data to client on random port
			sendReceiveSocket.send(sendDataPacket);
			System.out.println("Sent data packet to server, writing to server.");

			// wait for acknowledgment

			sendReceiveSocket.receive(sendDataPacket);

			if (sendDataPacket.getData()[0] == 0 && sendDataPacket.getData()[1] == 4) {
				int checkBlock = sendDataPacket.getData()[2] + sendDataPacket.getData()[3];
				System.out
						.println("Acknowledgment received for our write in progress with block number: " + checkBlock);
				if (blockNumber != checkBlock) {
					errorOccurred();
				}
			}

			blockNumber++;
			bytesRead = fis.read(readDataFromFile);
		}

		fis.close();

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
		int blockNum = 1;

		do {
			System.out.println("Packet #: " + blockNum);
			blockNum++;

			holdReceivingArray = new byte[516]; // 516 because 512 data + 2 byte
												// opcode + 2 byte 0's

			receivePacket = new DatagramPacket(holdReceivingArray, holdReceivingArray.length, inetAddress,
					sendReceiveSocket.getLocalPort());

			sendReceiveSocket.receive(receivePacket);

			byte[] requestCode = { holdReceivingArray[0], holdReceivingArray[1] };

			if (requestCode[1] == 5) { // 5 is opcode for error in packet
				errorOccurred();
			} else if (requestCode[1] == 3) { // 3 is opcode for data in packet
				byte[] blockNumber = { holdReceivingArray[2], holdReceivingArray[3] };

				DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
				writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);

				acknowledgeToHost(blockNumber);
			}

		} while (!(receivePacket.getLength() < 512));
		return receivingBytes;
	}

	private void errorOccurred() {
		System.out.println("ERROR HAS OCCURRED");

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