import java.io.*;
import java.net.*;
import java.util.zip.CRC32;
import java.nio.*;

public class FileSender {
	
	public static void main (String[] args) throws Exception {
		
		// Input validation
		if (args.length != 4) {
			System.err.println("Usage: FileSender <host> <port> <src> <dest>");
			System.exit(-1);
		}
		
		// Create a socket address from a hostname and a port number
		InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		
		// Create new File using src filename?
		File srcFile = new File(args[2]);
		long fileLen = srcFile.length();
		
		DatagramSocket sk = new DatagramSocket();
		DatagramPacket pkt;
		byte[] data = new byte[(int) fileLen];
		ByteBuffer b = ByteBuffer.wrap(data);
		
		// Call function to send filename over to FileReceiver for storing
		String DEST_FILE = args[3];
		sendDestFileName(DEST_FILE, addr, sk);
	}
	
	// Sends filename over to FileReceiver for storing
	public static void sendDestFileName(String dest, InetSocketAddress addr, DatagramSocket sk) throws Exception {
		byte[] buffer = dest.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, addr);
		sk.send(packet);
	}
