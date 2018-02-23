import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Random;
import java.util.Scanner;

public class ClientConnectionThread implements Runnable {

	private DatagramSocket sendReceiveSocket;
	private Packet receivePacket;
	private InetAddress hostINet;//inet to send the packet to shouldnt be an instance cariable
	private int hostPort;//port to send the packet shouldnt be instance variable
	private Packet sendPacket;
	private InetAddress inetAddress = null;
	int receivePort = 88;	
	//private Path filePath;
	private byte[] holdReceivingArray;//rf
	private FileInputStream fis = null;//rf
	private String fileNameToWrite;//should not be an instance variable

	public ClientConnectionThread(Packet receivePacket) {
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
	}

	public void run() {

		//which design pattern is this
		//there all packets just call recieve packet...
		// process the packet here
		System.out.println("Thread is now running.");
		while(true){
			if(receivePacket.getPacketType()[0]==0 &&receivePacket.getPacketType()[1]==1 ){
				//read request received
				try {
					readRequestReceived();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(receivePacket.getPacketType()[0]==0 &&receivePacket.getPacketType()[1]==2 ){
				//write request recieved
				writeRequestReceived();
			}
			else if(receivePacket.getPacketType()[0]==0 &&receivePacket.getPacketType()[1]==3){
				//data packet
			}
			else if(receivePacket.getPacketType()[0]==0 &&receivePacket.getPacketType()[1]==4){
				//ack pack
			}
			else if(receivePacket.getPacketType()[0]==0 &&receivePacket.getPacketType()[1]==5){
				//error packet
			}

		}
	}
	/**
	 * write request recieved
	 * i. must prepare a file to save written files to
	 * ii. must send ack pack to client
	 */
	private void writeRequestReceived() {

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();

		Scanner input = new Scanner(System.in);
		System.out.println("Enter the name of the file to be written to:");
		fileNameToWrite = input.next();
		input.close();


		currentPath = currentRelativePath.toAbsolutePath().toString();

		Path filePath = Paths.get(currentPath, fileNameToWrite);



		if (Files.exists(filePath)) {
			String errorMessage= new String("File already exits at server side");
			sendPacket.setPacketData(sendPacket.setCode(0, 5), sendPacket.setCode(0,6),errorMessage.getBytes());
			try {
				sendReceiveSocket.send(sendPacket.usePacket());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("file exists on server already");
			System.exit(0);
		}
		byte[] firstAck=new byte[2];
		firstAck[0]=0;
		firstAck[1]=0;
		acknowledgeToHost(firstAck);//there should just be one method for sending ack packs

		ByteArrayOutputStream receivingBytes;
		try {
			receivingBytes = getFile();//refactor


			writeOutReceivedFile(receivingBytes, fileNameToWrite);//should be getting filename from packet ie receivePacket.getData();
			System.out.println("Writing to file is done.");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	//handled by class Packet
	/*private byte[] createErrorPacket(int errorCode, String errorMessage) {
		int posInErrorArray = 0;
		byte zeroByte = 0;
		byte five = 5;

		int errorPacketLength = 4 + errorMessage.length() + 1;

		byte[] setupErrorPacket = new byte[errorPacketLength];
		setupErrorPacket[posInErrorArray] = zeroByte;
		posInErrorArray++;
		setupErrorPacket[posInErrorArray] = five;
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

	private void writeOutReceivedFile(ByteArrayOutputStream byteArrayOutputStream, String fileName) {
		try {
			OutputStream outputStream = new FileOutputStream(fileName);
			byteArrayOutputStream.writeTo(outputStream);
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e){
			String errorMessage= new String("File " + fileName + " ran out of memory on server side");
			sendPacket.setPacketData(sendPacket.setCode(0,5), sendPacket.setCode(0,3), errorMessage.getBytes());
			try {
				sendReceiveSocket.send(sendPacket.usePacket());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
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

			receivePacket = new Packet(new DatagramPacket(new byte[516], 516, inetAddress,
					sendReceiveSocket.getLocalPort()));

			sendReceiveSocket.receive(receivePacket.usePacket());
			if(receivePacket.getPacketType()[1]==5){
				errorOccurred();//parameter should be receivePacket.getPacketCode() which is a 2byte array containing the error code
			}
			else if (receivePacket.getPacketType()[1] == 3) { // 3 is opcode for data in packet
				DataOutputStream writeOutBytes = new DataOutputStream(receivingBytes);//what is receiving bytes
				writeOutBytes.write(receivePacket.getPacketData(), 0, receivePacket.getPacketData().length-1);

				acknowledgeToHost(receivePacket.getPacketCode());//after writing bytes to file, send ack pack
			}

		} while (!(receivePacket.getPacketData().length < 512));//once the data is less than 512 bytes, file transfer is finished
		return receivingBytes;
	}
	/**
	 * method to send ackPack to host
	 * @param blockNum
	 */
	private void acknowledgeToHost(byte[] blockNum) {//use class packet to make ackpack
		sendPacket.setPacketData(sendPacket.setCode(0,4), blockNum, new byte[1]);

		try {
			sendReceiveSocket.send(sendPacket.usePacket());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void errorOccurred() {
		System.out.println("ERROR HAS OCCURRED");
	}

	/*private void sendFirstWriteAcknowledgment() {
		byte[] acknowledgeCode = { 0, 4, 0, 0 };

		DatagramPacket acknowledgePacket = new DatagramPacket(acknowledgeCode, acknowledgeCode.length, inetAddress,
				receivePacket.getPort());
		try {
			sendReceiveSocket.send(acknowledgePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}*/

	private void readRequestReceived() throws IOException {
		// get filename, if file is available send in chunks to client on a new
		// random port
		// wait for acknowledgment then send another chunk until transfer is
		// complete

		String fileName = new String(receivePacket.getPacketData());

		System.out.println("Received file name is: " + fileName);

		FileInputStream fis = null;

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();

		if (!isFileReadable(fileName)){
			String errorMessage = new String ("File " + fileName + " not readable on server at path " + currentPath);
			sendPacket.setPacketData(sendPacket.setCode(0, 5), sendPacket.setCode(0, 2), errorMessage.getBytes());
			try {
				sendReceiveSocket.send(sendPacket.usePacket());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}

		try {
			File file = new File(currentPath, fileName);
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			String errorMessage = new String("File " + fileName + " not found on server at path " + currentPath);
			sendPacket.setPacketData(sendPacket.setCode(0, 5), sendPacket.setCode(0, 1), errorMessage.getBytes());
			try {
				sendReceiveSocket.send(sendPacket.usePacket());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}


		byte[] readDataFromFile = new byte[508]; // 512 byte chunks

		int bytesRead = fis.read(readDataFromFile);

		int blockNumber = 1;

		while (bytesRead != -1) {
			System.out.println("bytes read is: " + bytesRead);
			if (bytesRead == 512) {
				sendPacket.setPacketData(sendPacket.setCode(0, 3), sendPacket.setCode(0,blockNumber), readDataFromFile); 
			} else {
				sendPacket.setPacketData(sendPacket.setCode(0, 3), sendPacket.setCode(0,blockNumber), readDataFromFile); 
				/*	= new DatagramPacket(createDataPacket(blockNumber, readDataFromFile), bytesRead + 4,
						inetAddress, 23); why were we adding 4 to bytes read, bytes read should be length of data read from file*/
			}
			// bytesRead should contain the number of bytes read in this
			// operation.
			// send data to client on random port
			sendReceiveSocket.send(sendPacket.usePacket());
			System.out.println("Sending data packet from thread to host. Replying to read request");

			// wait for acknowledgment
			sendReceiveSocket.receive(sendPacket.usePacket());

			if (sendPacket.getPacketType()[0] == 0 && sendPacket.getPacketType()[1] == 4) {
				int checkBlock = sendPacket.getPacketCode()[0] + sendPacket.getPacketCode()[1];
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
	/**
	 * determine if a given file is readable.... should create a file handler class
	 * @param fileName2
	 * @return
	 */
	private boolean isFileReadable(String fileName2) {
		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();
		Path filePath = Paths.get(currentPath, fileName2);

		if((new File(fileName2)).exists() && !Files.isReadable(filePath)){
			return false;
		}
		else
			return true;
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

}
