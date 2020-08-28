package turing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;

import turing.Message.RESULT;

public class CheckClientStatus extends TimerTask{
	
	public void run() {
		
		for (User u: Turing.databaseUsers.getUsers())
		{	
			String user = u.getUsername();
			SocketChannel client = Turing.databaseUsers.isOnline(user);
			ByteBuffer b = ByteBuffer.allocate(1);

			if (client != null) {
				try {
					client.configureBlocking(false);
					int letti = client.read(b);
					if (letti== -1){ //client è crashato
						client.close(); //chiudo la socket
						Turing.databaseUsers.setOffline(user); //imposto il client come offline
						System.out.println(user+" chiuso in modo errato, impostato offline.");
						/* controllo se stava modificando documenti, 
						 * in tal caso li rendo disponibili per la modifica*/
						for (Document d: Turing.databaseDocuments.getDocuments()){
							for (Section s: d.getSections()){
								if (s.endEdit(user) == RESULT.OP_OK)
									System.out.println(d.getDocName()+"_"+s.getNumSec() + " disponibile per la modifica.");
							}
						}
					}
				} catch (IOException e){	
					e.printStackTrace();
				}
			}
		}
	}

}
