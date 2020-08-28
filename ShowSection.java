package turing;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import turing.Message.OPERATION;
import turing.Message.RESULT;

public class ShowSection implements Runnable {

	SocketChannel client;
	Message received;
	
	public ShowSection (SocketChannel client, Message received){
		if (client == null || received == null)
			return;
		this.client = client;
		this.received = received;
		
	}
	public void run() {
		
		String clientUsername = received.getUsername();
		String documentName = received.getDocumentName();
		int numeroSezione = received.getNumOfSection();
		FileChannel fileChannel = null;
		Section section = null;
		RESULT result;
		Message toSend = new Message();
		
		System.out.println("Documento: " + documentName);
		System.out.println("Sezione: "+ numeroSezione);
		
		try {
			result = Turing.databaseUsers.haveDocument(clientUsername,documentName);
			if (result == RESULT.OP_OK)
			{
				if ( (section = Turing.databaseDocuments.getDocument(documentName).getSection(numeroSezione)) != null)
				{	
					String path = Turing.DATA_DIR; //+ Turing.databaseDocuments.getDocument(documentName).getOwner() +"/";
					path = path + section.getFileName();
					File file = new File(path);
					System.out.println(path);
					if (!file.exists()) file.createNewFile();
					fileChannel = FileChannel.open(Paths.get(Turing.DATA_DIR,section.getFileName()),StandardOpenOption.READ);
					if (fileChannel == null) 
						result = RESULT.OP_ERR;
					
				} else result = RESULT.SEC_INEXISTENT;
			}
			
			toSend.setMessageResult(OPERATION.SHOW_S,result); 
			Message.sendMessage(client, toSend);
			System.out.println("Server-ShowSection ["+ received.getUsername()+", document: " + 
													received.getDocumentName()+" section:" +
													received.getNumOfSection()+" ]:" +result);
			if (result == RESULT.OP_OK) {
				ByteBuffer buffer = Message.readFileChannel(fileChannel);
				Message.send(client, buffer);	
			}
			client.close();
			
		} catch (Exception e){
			toSend.resetMessage();
			toSend.setMessageResult(OPERATION.SHOW_S,RESULT.OP_ERR); 
			Message.sendMessage(client, toSend);//invio l'esito
			try {
				client.close();
			} catch (IOException e1) {
				
			}
		}
		
	}

}
