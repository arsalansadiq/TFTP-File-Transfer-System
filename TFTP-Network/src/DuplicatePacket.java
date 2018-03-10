import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class DuplicatePacket implements Runnable {
	private DatagramSocket socket;
	private DatagramPacket	packyPack;
	private int delay;
	public DuplicatePacket (DatagramPacket packet, int delayTime) throws SocketException {
		socket = new DatagramSocket();
		packyPack=packet;
		delay=delayTime;
	}
	@Override
	public void run() {
		try {
			TimeUnit.SECONDS.sleep(delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			socket.send(packyPack);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
