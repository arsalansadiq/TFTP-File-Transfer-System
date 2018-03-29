import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.Scanner;

public class ClientConnectionThread implements Runnable {

	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket;
	private DatagramPacket sendDataPacket;
	private DatagramPacket mostRecentPacket;
	private InetAddress inetAddress = null;
	int receivePort = 88;
	private byte data[];

	// private Path filePath;

	private byte[] holdReceivingArray;
	private FileInputStream fis = null;
	private DatagramPacket sendErrorPacket;
	private String fileName;

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
			writeRequestReceived();
		}

	}

	private void writeRequestReceived() {

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();

		currentPath = currentRelativePath.toAbsolutePath().toString();
		fileName = getFileName(receivePacket);

		Path filePath = Paths.get(currentPath + "\\Server", fileName);

		if (Files.exists(filePath)) {
			byte[] errorPacket = createErrorPacket(6, "File " + fileName + " already exists at server side.");
			sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, 23);
			try {
				sendReceiveSocket.send(sendErrorPacket);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("file exists on server already");
			System.exit(0);
		}

		sendFirstWriteAcknowledgment();

		ByteArrayOutputStream receivingBytes;
		try {
			receivingBytes = getFile();
			writeOutReceivedFile(receivingBytes, fileName);
			System.out.println("Writing to file is done.");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String getFileName(DatagramPacket fileNamePacket) {
		int nameLength = 0;
		for (int i = 2; fileNamePacket.getData()[i] != 0; i++) {
			nameLength++;
		}

		byte[] packetData = new byte[nameLength];
		System.arraycopy(receivePacket.getData(), 2, packetData, 0, nameLength);
		return new String(packetData);
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

	private void writeOutReceivedFile(ByteArrayOutputStream byteArrayOutputStream, String fileNameTowrite) {

		// filePath = Paths.get(currentPath+"\\Server", fileName);
		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();

		File file = new File(currentPath + "\\Server", fileNameTowrite);

		try {
			OutputStream outputStream = new FileOutputStream(file);
			byteArrayOutputStream.writeTo(outputStream);
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			byte[] errorPacket = createErrorPacket(3, "File " + fileNameTowrite + " ran out of memory on server side");
			sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, receivePacket.getPort());
			try {
				sendReceiveSocket.send(sendErrorPacket);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}
	}

	private ByteArrayOutputStream getFile() throws IOException {
		ByteArrayOutputStream receivingBytes = new ByteArrayOutputStream();
		int blockNum = 0;
		int actualBlockNum = 0;
		byte[] blockNumber = new byte[2];
		holdReceivingArray = new byte[512]; // 516 because 512 data + 2 byte
		// opcode + 2 byte 0's

		receivePacket = new DatagramPacket(holdReceivingArray, holdReceivingArray.length, inetAddress,
				sendReceiveSocket.getLocalPort());
		sendReceiveSocket.receive(receivePacket);
		do {
			// System.out.println("Packet #: " + blockNum);
			blockNum++;

			// System.out.println("client is waiting for packet");

			// System.out.println("client is still waiting");

			byte[] requestCode = { holdReceivingArray[0], holdReceivingArray[1] };

			if (requestCode[0] == 0 && requestCode[1] == 5) {
				errorOccurred(receivePacket);
			} else if (requestCode[1] == 3) { // 3 is opcode for data in packet
				blockNumber[0] = holdReceivingArray[2];
				blockNumber[1] = holdReceivingArray[3];
				actualBlockNum = byteArrToInt(blockNumber);

				System.out.println("Thread received block number: " + actualBlockNum);
				if (blockNum == actualBlockNum) {
					DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
					writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);

					acknowledgeToHost(byteArrToInt(blockNumber));
					sendReceiveSocket.receive(receivePacket);
					//

				} else if (blockNum != actualBlockNum) {
					System.out.println("Thread was expecting block number: " + blockNum + " but received block number: "
							+ actualBlockNum + ". Discarding...");
					// blockNum = actualBlockNum+1;
					blockNum--;
					sendReceiveSocket.receive(receivePacket);
					// acknowledgeToHost(byteArrToInt(blockNumber));
					// System.out.println("Thread blockNum is: " + blockNum);
				}
			}

		} while (!(receivePacket.getLength() < 512));
		if (receivePacket.getLength() != 0) {
			DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);
			writeOutBytes.write(receivePacket.getData(), 4, receivePacket.getLength() - 4);
			System.out.println("Thread received block number: " + (actualBlockNum + 1));
			acknowledgeToHost(byteArrToInt(blockNumber));
		}
		return receivingBytes;
	}

	private int byteArrToInt(byte[] blockNumber) {

		return ((byte) (blockNumber[0] & 0xFF) | (byte) ((blockNumber[1] >> 8) & 0xFF));

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

	private void errorOccurred(DatagramPacket errorPacket) throws IOException {
		if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 1) {
			System.out.println("Error code 1: File not found. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 2) {
			System.out.println("Error code 2: Access violation. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 3) {
			System.out.println("Error code 3: Disk full or allocation exceeded. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 6) {
			System.out.println("Error code 6: File already exists. The error message is: ");
		} else if (errorPacket.getData()[2] == 0 && errorPacket.getData()[3] == 5) {
			System.out.println("Error code 5: Unknown TID. The error message is: ");
			// if (bytesRead == 508) {
			// sendDataPacket = new DatagramPacket(createDataPacket(blockNumber,
			// readDataFromFile),
			// readDataFromFile.length + 4, inetAddress, 23);
			// } else {
			// sendDataPacket = new DatagramPacket(createDataPacket(blockNumber,
			// readDataFromFile), bytesRead + 4,
			// inetAddress, 23);
			// }

			int nameLength = 0;
			for (int i = 4; errorPacket.getData()[i] != 0; i++) {
				nameLength++;
			}

			byte[] packetData = new byte[nameLength];
			System.arraycopy(errorPacket.getData(), 4, packetData, 0, nameLength);
			String errorMessage = new String(packetData);

			System.out.println(errorMessage);

			sendReceiveSocket.send(sendDataPacket);// resend the last packet
			// System.out.println("Thread sending retry packet: " +
			// sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
			// + " with block number " + sendDataPacket.getData()[2] +
			// sendDataPacket.getData()[3]);

			// sendReceiveSocket.receive(receivePacket);//wait for clients response

			return;
		}

		int nameLength = 0;
		for (int i = 4; errorPacket.getData()[i] != 0; i++) {
			nameLength++;
		}

		byte[] packetData = new byte[nameLength];
		System.arraycopy(errorPacket.getData(), 4, packetData, 0, nameLength);
		String errorMessage = new String(packetData);

		System.out.println(errorMessage);

	}

	private void sendFirstWriteAcknowledgment() {
		byte[] acknowledgeCode = { 0, 4, 0, 0 };

		sendDataPacket = new DatagramPacket(acknowledgeCode, acknowledgeCode.length, inetAddress,
				receivePacket.getPort());
		try {
			sendReceiveSocket.send(sendDataPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void readRequestReceived() throws IOException {
		// get filename, if file is available send in chunks to client on a new
		// random port
		// wait for acknowledgment then send another chunk until transfer is
		// complete

		fileName = getFileName(receivePacket);
		FileInputStream fis = null;

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString() + "\\Server";

		if (!isFileReadable(fileName)) {
			byte[] errorPacket = createErrorPacket(2,
					"File " + fileName + " not readable on server at path " + currentPath + "\\Server");
			sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, receivePacket.getPort());
			try {
				sendReceiveSocket.send(sendErrorPacket);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}

		try {
			File file = new File(currentPath, fileName);
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			byte[] errorPacket = createErrorPacket(1,
					"File " + fileName + " not found on server at path " + currentPath);
			sendErrorPacket = new DatagramPacket(errorPacket, errorPacket.length, inetAddress, receivePacket.getPort());
			try {
				sendReceiveSocket.send(sendErrorPacket);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}

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
		// operation.
		// send data to client on random port
		sendReceiveSocket.send(sendDataPacket);
		System.out.println("Thread sent packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
				+ " with block number " + sendDataPacket.getData()[2] + sendDataPacket.getData()[3]);

		blockNumber++;
		bytesRead = fis.read(readDataFromFile);

		// wait for acknowledgment
		sendReceiveSocket.receive(receivePacket);
		System.out.println("Thread received packet: " + receivePacket.getData()[0] + receivePacket.getData()[1]
				+ " with block number " + receivePacket.getData()[2] + receivePacket.getData()[3]);

		while (bytesRead != -1) {

			if (receivePacket.getPort() != 23) {// packet came from unkownID... discard packet, send off errorPacket
				System.out.println("PACKET COMING FROM DIFFERENT HOST, SENDING ERROR PACKET BACK.....................");
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

			if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 5) {
				errorOccurred(receivePacket);
				sendReceiveSocket.receive(receivePacket);// wait for clients response
				// System.out.println("Thread received response for retry packet: " +
				// receivePacket.getData()[0] + receivePacket.getData()[1]
				// + " with block number " + receivePacket.getData()[2] +
				// receivePacket.getData()[3]);
				// blockNumber++;
			}

			byte[] blockNumberRe = { receivePacket.getData()[2], receivePacket.getData()[3] };
			int checkBlock = byteArrToInt(blockNumberRe);

			if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4 && checkBlock == (blockNumber - 1)) {

				System.out.println("bytes read is: " + bytesRead);
				if (bytesRead == 508) {
					sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile),
							readDataFromFile.length + 4, inetAddress, 23);
				} else {
					sendDataPacket = new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
							inetAddress, 23);
				}

				sendReceiveSocket.send(sendDataPacket);
				System.out.println("Thread sent packet: " + sendDataPacket.getData()[0] + sendDataPacket.getData()[1]
						+ " with block number " + sendDataPacket.getData()[2]);

				// wait for acknowledgment
				sendReceiveSocket.receive(receivePacket);
				System.out.println("Thread received packet: " + receivePacket.getData()[0] + receivePacket.getData()[1]
						+ " with block number " + receivePacket.getData()[2]);

				blockNumber++;
				bytesRead = fis.read(readDataFromFile);
			} else if (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4
					&& checkBlock != (blockNumber - 1)) {
				System.out.println("DID NOT SEND ANOTHER DATA BACK");

				sendReceiveSocket.receive(receivePacket);
			}
		}

		fis.close();
		System.out.println("Done transfer. Exiting.");
		System.exit(0);

	}

	private boolean isFileReadable(String fileName2) {
		Path currentRelativePath = Paths.get("");
		String currentPath2 = currentRelativePath.toAbsolutePath().toString() + "\\Server";
		Path filePath = Paths.get(currentPath2, fileName2);

		if ((new File(currentPath2, fileName2)).exists() && !Files.isReadable(filePath)) {
			return false;
		} else
			return true;
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