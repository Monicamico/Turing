package turing;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

import turing.Message.OPERATION;
import turing.Message.RESULT;

public class Edit implements Runnable {

	public SocketChannel client;
	public Message received;
	
	public Edit(SocketChannel client, Message received){
		this.client = client;
		this.received = received;
	}
	
	@Override
	public void run() {
		Message toSend = new Message(); 
		FileChannel fileChannel = null;
		Section editing = null;
		Document document = null;
		Message.RESULT result;
		
		try {
	
			String clientUsername = received.getUsername();
			String documentName = received.getDocumentName();
			int numeroSezione = received.getNumOfSection();
		
			result = RESULT.OP_OK;
			
			document = Turing.databaseDocuments.getDocument(documentName);
			
			if (document != null)
			{
				result = Turing.databaseUsers.haveDocument(clientUsername,documentName);
				if ( result == RESULT.OP_OK) { 
					editing = document.getSection(numeroSezione);
					if ( editing != null){
						result = editing.startEdit(clientUsername);
						if (result == RESULT.OP_OK){  	
							if ((fileChannel = FileChannel.open(Paths.get(Turing.DATA_DIR,editing.getFileName()),StandardOpenOption.READ)) == null)
								result = RESULT.OP_ERR;
						}
					} else result = RESULT.SEC_INEXISTENT;
				} 
			} else result = RESULT.DOC_UNKNOWN;
			
			
			
			
			toSend.setMessageResult(OPERATION.EDIT_DOC,result);
			Message.sendMessage(client, toSend);
			
			if (result == RESULT.OP_OK) {
				
				ByteBuffer buffer = Message.readFileChannel(fileChannel);
				Message.send(client, buffer);
				
				int porta = 0; 
				//n = numero di persone che stanno editando
				AtomicInteger n = Turing.databaseDocuments.getDocument(document.getDocName()).getNumberOfEditing();
				if (n.get() == 0){ // se nessuno edita
					porta = Turing.n_porta.incrementAndGet();
					Turing.databaseDocuments.getDocument(document.getDocName()).setGroupPort(porta);
				} else {
					porta = Turing.databaseDocuments.getDocument(document.getDocName()).getGroupPort().get();
				}
				n.incrementAndGet(); //segno che sto modificando il doc
				buffer.clear();
				
				ByteBuffer buffer1 = ByteBuffer.allocate(Integer.BYTES);
				buffer1.putInt(porta);
				buffer1.flip();
				Message.send(client, buffer1);
				buffer1.clear();

			}
			System.out.println("Server-Edit: ["+ received.getUsername()+", "+received.getDocumentName() +"]: " +result);
			client.close();
			
		} catch (Exception e){
			toSend.setMessageResult(OPERATION.EDIT_DOC,RESULT.OP_ERR); 
			Message.sendMessage(client, toSend);//invio l'esito
			e.printStackTrace();
		}
		
	}

}
