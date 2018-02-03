import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;

public class ClientConnectionThread implements Runnable {

	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket;
	private DatagramPacket sendDataPacket;
	private InetAddress inetAddress = null;
	int receivePort = 88;
	private byte data[];

	private byte[] holdReceivingArray;

	String fileNameToWrite;

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
			try {
				writeRequestReceived();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void writeRequestReceived() throws IOException {
		sendFirstWriteAcknowledgment();

		ByteArrayOutputStream receivingBytes = getFile();

		Scanner input = new Scanner(System.in);
		System.out.println("Enter the name of the file to be written to:");
		fileNameToWrite = input.next();
		input.close();

		writeOutReceivedFile(receivingBytes, fileNameToWrite);
		System.out.println("Writing to file is done.");

	}

	private void writeOutReceivedFile(ByteArrayOutputStream byteArrayOutputStream, String fileName) {
		try {
			OutputStream outputStream = new FileOutputStream(fileName);
			byteArrayOutputStream.writeTo(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	private void errorOccurred() {
		System.out.println("ERROR HAS OCCURRED");
	}

	private void sendFirstWriteAcknowledgment() {
		byte[] acknowledgeCode = { 0, 4, 0, 0 };

		DatagramPacket acknowledgePacket = new DatagramPacket(acknowledgeCode, acknowledgeCode.length, inetAddress,
				receivePacket.getPort());
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
			System.out.println("Sending data packet from thread to host. Replying to read request");

			// wait for acknowledgment
			sendReceiveSocket.receive(sendDataPacket);

			if (sendDataPacket.getData()[0] == 0 && sendDataPacket.getData()[1] == 4) {
				int checkBlock = sendDataPacket.getData()[2] + sendDataPacket.getData()[3];
				System.out.println(
						"Acknowledgment from client, sending file for read request in progress with block number :"
								+ checkBlock);
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

}
