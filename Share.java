package turing;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import turing.Message.OPERATION;
import turing.Message.RESULT;

public class Share implements Runnable {

	private Message received;
	private SocketChannel client; //chi ha mandato la richiesta di invito
	
	public Share(SocketChannel c,Message m){
		this.received = m;
		this.client = c;
	}
	
	public void run() {
		
		String receiver = this.received.getDestUsername();
		String document = this.received.getDocumentName();
		String owner = this.received.getUsername();
		Message toSend = new Message();
		Message.RESULT result;
		
		try 
		{
			//aggiorna i database (Utenti e Documenti)
			result = Turing.databaseUsers.addDocumentToUser(owner,receiver, document, Turing.databaseDocuments);
			
			if (result == RESULT.OP_OK){ 
				/*
				 * se l'utente non è online lo inserisco nella sua lista degli inviti
				 * altrimenti invio
				 * 
				 * */
				Invite invite = new Invite(owner,document);
				SocketChannel clientInvite = Turing.databaseUsers.isOnline(receiver);
				//non è online
				if (clientInvite == null) 
					result = Turing.databaseUsers.addInviteToUser(invite, receiver);
				else 
				{
					ByteBuffer b = ByteBuffer.allocate(1);
					client.configureBlocking(false);
					int letti = client.read(b); //controllo che non sia crashato
					if (letti== -1)
					{
						result = Turing.databaseUsers.addInviteToUser(invite, receiver);
						
					} else { //è sicuramente online. Lo invio sulla socket
						
						/*Invio gli inviti pendenti (Non dovrebbero esserci perchè 
						 * se online li riceve immediatamente, se non è online li riceve al login).
						 */
						Invite current;
						Set<Invite> setInvite = Turing.databaseUsers.getInvitesOfUser(receiver);
						
						if (!setInvite.isEmpty()){
							Iterator<Invite> it = setInvite.iterator();
							while(it.hasNext()){
								current = it.next();
								Invite.sendInvite(clientInvite, current);
								Turing.databaseUsers.removeInviteOfUser(receiver, current);
							}
						}
						//invio invito
						if (Invite.sendInvite(clientInvite, invite)) result = RESULT.OP_OK;
					}
					
				}
			}
			
			toSend.setMessageResult(OPERATION.SHARE,result); //invio esito
			Message.sendMessage(client, toSend); //mando esito a chi ha mandato la richiesta
			System.out.println("Server-Share ["+ received.getUsername()+" TO "+received.getDestUsername() +"]: " +result);
			client.close(); //chiudo socket di richiesta
		}
		catch (Exception e){
			//System.out.println(e);
		}
	}

}
