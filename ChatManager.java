package turing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatManager implements Runnable {

	private MulticastSocket ms;
	public static LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<String>();
	
	public ChatManager(MulticastSocket ms){
		this.ms = ms;
	}
	
	@Override
	public void run() {
	
		while(true){
			try 
			{
				byte[] buffer = new byte[Integer.BYTES];
				DatagramPacket len = new DatagramPacket(buffer,buffer.length);
				ms.receive(len);
				
				int l = (ByteBuffer.wrap(len.getData()).getInt());
				buffer = new byte[l];
				DatagramPacket p = new DatagramPacket(buffer,buffer.length);
				ms.receive(p);
				String message = new String(p.getData());
				//System.out.println("Ricevuto messaggio ->"+ message);
				messages.add(message);
				
			} catch (SocketException e) {	
				break;
			} catch (IOException e) {
				break;
			}
		}
		

	}

}
