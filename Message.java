package turing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.NoSuchFileException;
import java.util.Set;

public class Message implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public enum OPERATION {
		LOGIN,
		LOGOUT,
		CREATE_DOC,
		EDIT_DOC,
		END_EDIT,
		SHARE,
		SHOW_S,
		SHOW_D,
		SEND,
		RECEIVE,
		LIST;
	}
	
	public enum RESULT {
		OP_OK,
		USR_UNKNOWN,
		PSW_INCORRECT,
		DOC_UNKNOWN,
		SEC_INEXISTENT,
		SEC_EDITING,
		SEC_ERR,
		DOC_EXISTENT,
		USR_EXISTENT,
		NOT_AUTORIZED,
		NOT_OWNER,
		OP_ERR;
	}
	
	private OPERATION op;
	private RESULT rs;
	private String username;
	private String password;
	private String destUsername;
	private String documentName;
	private Set<Invite> listOfDocuments;
	private int numOfSection;
	
	public Message(){
		this.op = null;
		this.username = null;
		this.password = null;
		this.destUsername = null;
		this.documentName = null;
		this.numOfSection = -1;
		this.rs = null;
		
	}
	
	//messaggio di inviti
	public void setMessageShare(String username, String userToInvite, String document){
		this.resetMessage();
		this.op = OPERATION.SHARE;
		this.username = username;
		this.documentName = document;
		this.destUsername = userToInvite;
	}
	
	//messaggio creazione/modifica/show documento
	public void setMessageDocument(OPERATION op, String username, String document, int numSec){
		this.resetMessage();
		this.op = op;
		this.username = username;
		this.documentName = document;
		this.numOfSection = numSec;
	}
	
	//messaggi di login
	public void setMessageLogin(String username, String password){
		this.resetMessage();
		this.op = OPERATION.LOGIN;
		this.username = username;
		this.password = password;
	}
	
	//messaggio di logout
	public void setMessageLogout(String username){
		this.resetMessage();
		this.op = OPERATION.LOGOUT;
		this.username = username;
	}
	
	//messaggio di richiesta lista documenti 
	public void setMessageListOp(String username){
		this.resetMessage();
		this.op = OPERATION.LIST;
		this.username = username;
	}
	
	//utile per messaggi di errore - successo
	public void setMessageResult(OPERATION op, RESULT r){
		this.resetMessage();
		this.op = op;
		this.rs = r;
	}
	
	//messaggi con lista documenti
	public void setMessageList(Set<Invite> list){
		this.resetMessage();
		this.op = OPERATION.LIST;
		this.listOfDocuments = list;
	}
	
	public Set<Invite> getList(){
		return this.listOfDocuments;
	}
	
	public OPERATION getOp(){
		return this.op;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public String getPassword(){
		return this.password;
	}
	
	public String getDestUsername(){
		return this.destUsername;
	}
	
	public String getDocumentName(){
		return this.documentName;
	}
	
	public int getNumOfSection(){
		return this.numOfSection;
	}
	
	public RESULT getResultType(){
		return this.rs;
	}
	
	public void resetMessage(){
		this.op = null;
		this.username = null;
		this.password = null;
		this.destUsername = null;
		this.documentName = null;
		this.numOfSection = -1;
		this.rs = null;
	}
	
	public static Message receiveMessage(SocketChannel client){
		
		ByteBuffer dimBuffer = ByteBuffer.allocate(Integer.BYTES);
		try 
		{
			client.read(dimBuffer);
			dimBuffer.flip();
			if (!dimBuffer.hasRemaining()) return null;
			ByteBuffer bytebuffer = ByteBuffer.allocate(dimBuffer.getInt());
			while (bytebuffer.hasRemaining()){
				client.read(bytebuffer);
			}	
			bytebuffer.flip();
			ByteArrayInputStream messageInput = new ByteArrayInputStream(bytebuffer.array()); 
			ObjectInputStream inStream = new ObjectInputStream(messageInput); 
			Message received = ((Message)inStream.readObject()); 
			
			inStream.close();
			bytebuffer.clear();
			
			return received;
			
		} catch (IOException | ClassNotFoundException e){
			e.printStackTrace();
			return null;
		}	
	}
	
	public static boolean sendMessage(SocketChannel client, Message message){
		try 
		{
			ByteArrayOutputStream outArr = new ByteArrayOutputStream();
			ObjectOutputStream outStream = new ObjectOutputStream(outArr); 
			outStream.writeObject(message);
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
			
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}			
		
	}
	
	//scriveil contenuto di un bytebuffer in una socket
	public static void send(SocketChannel client, ByteBuffer bufferFile) throws IOException {
		
		ByteBuffer capacity = ByteBuffer.allocate(Integer.BYTES);
		capacity.putInt(bufferFile.capacity());
		capacity.flip();
		client.write(capacity);
		capacity.clear();
		
		while (bufferFile.hasRemaining()) {
			   client.write(bufferFile);
			}
		bufferFile.clear();
		
	}
	
	//riceve un bytebuffer leggendo dalla socket
	public static ByteBuffer read(SocketChannel client) throws IOException {
		
		ByteBuffer dimBuffer = ByteBuffer.allocate(Integer.BYTES);
		int letti = client.read(dimBuffer);  //leggo dimensione
		dimBuffer.flip();
		if (letti <= 0)
			throw new IOException();
		ByteBuffer bytebuffer = ByteBuffer.allocate(dimBuffer.getInt());
		
		while (bytebuffer.hasRemaining()){
			client.read(bytebuffer);//leggo contenuto
		}	
		bytebuffer.flip();
		return bytebuffer;//restituisco contenuto
	}
	
	//scrive un bytebuffer nel filechannel
	public static synchronized boolean writeFileChannel(ByteBuffer toWrite, FileChannel fileC){
		
		if (toWrite == null || fileC == null) return false;
		if (!fileC.isOpen()) return false;
		try {
			
			while(toWrite.hasRemaining()){
				fileC.write(toWrite);
			}
			toWrite.flip();
			toWrite.clear();
			return true;
			
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		
	}
	
	//riceve un bytebuffer leggendo dal file channel
	public static synchronized ByteBuffer readFileChannel(FileChannel filec){

		if (filec == null) return null;
		try 
		{	
			ByteBuffer bytebuffer = ByteBuffer.allocate((int) filec.size());
			boolean stop = false;
			while (!stop){
				int bytesRead = filec.read(bytebuffer);
				if(bytesRead == -1 ||bytesRead == 0) stop = true;
			}
			bytebuffer.flip();
			filec.close();
			return bytebuffer;
		
		} catch (NoSuchFileException e1) {
			System.out.println("Nessun file trovato");
			return null;
			
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
