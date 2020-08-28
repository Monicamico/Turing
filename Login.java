package turing;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import turing.Message.OPERATION;
import turing.Message.RESULT;

public class Login implements Runnable{

	SocketChannel client;
	Message received;
	
	public Login(SocketChannel c, Message received){
		this.client = c;
		this.received = received;
	}
	
	//effettua il login di un utente
	public void run() {
		
		Message toSend = new Message();
		//esegue il login
		Message.RESULT result = Turing.databaseUsers.setOnline(received.getUsername(), received.getPassword(), this.client);
		System.out.println("Server-Login ["+ received.getUsername()+"]: "+result);
		toSend.setMessageResult(OPERATION.LOGIN,result);
		
		try 
		{
			Message.sendMessage(client, toSend);
			if (result != RESULT.OP_OK) client.close();
			else //mando gli inviti pendenti
			{
				Invite current = null;
				Set<Invite> setInvite = Turing.databaseUsers.getInvitesOfUser(received.getUsername());
				Iterator<Invite> it = setInvite.iterator();
				while(it.hasNext()){
					current = it.next();
					Invite.sendInvite(client, current);
					//Turing.databaseUsers.removeInviteOfUser(received.getUsername(), current);
					it.remove();
				}
				current = null;	
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}

}
