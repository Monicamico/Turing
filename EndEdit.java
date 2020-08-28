package turing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import turing.Message.OPERATION;
import turing.Message.RESULT;

public class EndEdit implements Runnable {

	private SocketChannel client;
	private Message received;
	
	public EndEdit(SocketChannel client, Message received){
		this.client = client;
		this.received = received;
	}
	
	public void run() {
		
		String clientUsername = received.getUsername();
		String documentName = received.getDocumentName();
		int numeroSezione = received.getNumOfSection();
		Section editing = null;
		Document document = null;
		Message.RESULT result;
		
		try {
			 if ( (result = Turing.databaseUsers.haveDocument(clientUsername,documentName)) == RESULT.OP_OK)
			 {
				document = Turing.databaseDocuments.getDocument(documentName);
				editing = document.getSection(numeroSezione);
				
				if (editing != null && editing.isEditing(clientUsername))
				{
					FileChannel fileChannel = FileChannel.open(Paths.get(Turing.DATA_DIR,editing.getFileName()),StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING);
					ByteBuffer read = Message.read(client); //leggo il contenuto 
					//la Message.read fa la flip!
					
					if (Message.writeFileChannel(read, fileChannel)){
						result = editing.endEdit(clientUsername);
						fileChannel.close();
					}
					read.clear();
				} 
					
			} 
			
			Message toSend = new Message(); 
			toSend.setMessageResult(OPERATION.EDIT_DOC,result);
			Message.sendMessage(client, toSend);
			
			Turing.databaseDocuments.getDocument(document.getDocName()).getNumberOfEditing().decrementAndGet();
			System.out.println("Server-EndEdit ["+ received.getUsername()+", "+received.getDocumentName() +"]: " +result);

			client.close();
		
		} catch (IOException e){
			e.printStackTrace();
		}
		
	}

}
