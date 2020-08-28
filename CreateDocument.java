package turing;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import turing.Message.OPERATION;
import turing.Message.RESULT;

public class CreateDocument implements Runnable {

	private SocketChannel client;
	private Message received;
	
	public CreateDocument (SocketChannel client, Message received){
		this.client = client;
		this.received = received;
	}
	
	public void run() {
		Message toSend = new Message();
		Message.RESULT result;
		
		try {
			String usernameOnline = received.getUsername();
			String document = received.getDocumentName();
			int numSection = received.getNumOfSection();
			
			result = Turing.databaseDocuments.addDocumentDB(new Document(document,usernameOnline,numSection)); //aggiunge documento al database documenti
			if (result == RESULT.OP_OK){
				//aggiunge il documento nella lista documenti dell'utente e l'utente nella lista degli utenti autorizzati del documento
				result = Turing.databaseUsers.addDocumentToUser(usernameOnline, usernameOnline, document,Turing.databaseDocuments);
				if (result == RESULT.OP_OK)
					System.out.println("Documento "+received.getDocumentName()+ " creato.");
			}
			toSend.setMessageResult(OPERATION.CREATE_DOC,result);
			Message.sendMessage(client, toSend);
			System.out.println("Server-CreateDocument ["+ received.getUsername()+", document: "+received.getDocumentName() +"]: " +result);

			client.close(); 
			
		} catch (IOException e){
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
