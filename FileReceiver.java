import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class FileReceiver  {
	
	public static void main(String[] args) throws Exception {
	
		// Input validation
		if (args.length != 1) {
			System.err.println("Usage: FileReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket sk = new DatagramSocket(port);
		
		// Receive destination filename from FileSender
    		String destFileName = rcvDestFileName(sk);
    		System.out.println("Dest filename: " + destFileName);
	}
	
	public static String rcvDestFileName(DatagramSocket sk) throws Exception {
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		sk.receive(packet);
		System.out.println("Hostname and port no. of remote host where the packet is coming from: " + packet.getSocketAddress());
		buffer = packet.getData();
		String dest = new String(buffer);
		System.out.println("String received: " + dest);
		
		CRC32 crc = new CRC32();
		crc.update(buffer);
		ByteBuffer b = ByteBuffer.wrap(buffer);
		b.rewind();
		long checksum = b.getLong();
		if (crc.getValue() != checksum) {
		  System.out.println("Packet corrupt!");
		}
		else {
		  DatagramPacket ack = new DatagramPacket(new byte[0], 0, 0, packet.getSocketAddress());
		  sk.send(ack);
  		}
		return dest;
	}
}
