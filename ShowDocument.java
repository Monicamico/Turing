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

public class ShowDocument implements Runnable {

	SocketChannel client;
	Message received;
	
	public ShowDocument (SocketChannel client, Message received){
		if (client == null || received == null)
			return;
		this.client = client;
		this.received = received;
	}
	@Override
	public void run() {
		
		String clientUsername = received.getUsername();
		String documentName = received.getDocumentName();
		FileChannel fileChannel = null;
		FileChannel fileChannelDocument = null;
		Message.RESULT result;
	
		System.out.println("Documento: " + documentName);
		
		try
		{
			result = Turing.databaseUsers.haveDocument(clientUsername,documentName);
			if (result == RESULT.OP_OK){
				 fileChannelDocument = FileChannel.open(Paths.get(Turing.DATA_DIR, documentName),
						 StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
				 
				 int numeroSezioni = Turing.databaseDocuments.getDocument(documentName).getNumOfSections();
					Section section = null;
					int i = 0;
					
					fileChannelDocument.close();
					fileChannelDocument = FileChannel.open(Paths.get(Turing.DATA_DIR, documentName),
							 StandardOpenOption.WRITE, StandardOpenOption.APPEND);
					
					while (i < numeroSezioni && result == RESULT.OP_OK) {
						
						section = Turing.databaseDocuments.getDocument(documentName).getSection(i);
						if (section!= null) {	
							String path = Turing.DATA_DIR; 
							path = path + section.getFileName();
							File file = new File(path);
							if (!file.exists()) file.createNewFile();
							fileChannel = FileChannel.open(Paths.get(Turing.DATA_DIR,section.getFileName()),StandardOpenOption.READ);
							//leggo contenuto della sezione
							ByteBuffer bufferRead = Message.readFileChannel(fileChannel);
							//bufferRead.flip(); la flip viene fatta dalla readFilechannel!
							Message.writeFileChannel(bufferRead, fileChannelDocument); //scrivo il contenuto sul documento
							bufferRead.clear();			
							
						} else result = RESULT.SEC_INEXISTENT;	
						
						i++;
					}
			} 


			Message toSend = new Message(); 
			toSend.setMessageResult(OPERATION.SHOW_D,result);
			Message.sendMessage(client, toSend);
			System.out.println("Server-ShowDocument ["+ received.getUsername()+", document: " + received.getDocumentName()+" ]:" +result);
			
			if (result == RESULT.OP_OK) {
				fileChannelDocument.close();
				fileChannelDocument = FileChannel.open(Paths.get(Turing.DATA_DIR,documentName),StandardOpenOption.READ);
				ByteBuffer buffer = Message.readFileChannel(fileChannelDocument);
				Message.send(client, buffer);	
				fileChannel.close();
			}
			
			client.close();
			
		} catch (UnsupportedOperationException | SecurityException | IOException e){
			Message toSend = new Message(); 
			toSend.setMessageResult(OPERATION.SHOW_D,RESULT.OP_ERR); 
			Message.sendMessage(client, toSend);//invio l'esito
			try {
				client.close();
			} catch (IOException e1) {
				
			}
		}

	}

}
