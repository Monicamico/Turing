package turing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Invite implements Serializable, Comparable<Invite>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String owner;
	private String document;
	
	public Invite (String o, String d){
		this.owner = o;
		this.document = d;
	}
	
	public String getOwner(){
		return owner;
	}
	//fare funzioni send e receive

	public String getDocument() {
		return document;
	}

	public int compareTo(Invite i) {
		return document.compareTo(i.getDocument());
	}
	
	public static Invite receiveInvite(SocketChannel client) throws IOException, ClassNotFoundException {
		
		ByteBuffer dimBuffer = ByteBuffer.allocate(Integer.BYTES);
		client.read(dimBuffer);
		dimBuffer.flip();
		if(!dimBuffer.hasRemaining()) return null;
		ByteBuffer bytebuffer = ByteBuffer.allocate(dimBuffer.getInt());
		while (bytebuffer.hasRemaining()){
			client.read(bytebuffer);
		}	
		bytebuffer.flip();
		ByteArrayInputStream messageInput = new ByteArrayInputStream(bytebuffer.array()); 
		ObjectInputStream inStream = new ObjectInputStream(messageInput); 
		Invite invite = ((Invite)inStream.readObject()); 
		
		inStream.close();
		bytebuffer.clear();
		
		return invite;
	}
	
	public static boolean sendInvite(SocketChannel client, Invite invite){
		try 
		{
			ByteArrayOutputStream outArr = new ByteArrayOutputStream();
			ObjectOutputStream outStream = new ObjectOutputStream(outArr); 
			outStream.writeObject(invite);
			
			ByteBuffer buffer = ByteBuffer.wrap(outArr.toByteArray());
		
			ByteBuffer capacity = ByteBuffer.allocate(Integer.BYTES);
			capacity.putInt(buffer.capacity());
			capacity.flip();
			
			client.write(capacity);
			capacity.clear();
			
			while (buffer.hasRemaining()) {
				client.write(buffer);
			}
			buffer.clear();
			outStream.close();
			return true;
			
		} catch (BufferOverflowException e){
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;	
		}
				
		
	}
}
