package turing;

import java.nio.channels.SocketChannel;
import java.util.Set;

public class List implements Runnable {

	private SocketChannel client;
	private Message received;
	
	public List(SocketChannel client, Message received){
		this.client = client;
		this.received = received;
	}
	public void run() {
		Message toSend = new Message();
		try {
			Set<Invite> list;
			list = Turing.databaseUsers.getDocumentsOfUser(received.getUsername());
			if (list.isEmpty())
				System.out.println("Server-List ["+ received.getUsername()+"]: nessun documento" );
			else System.out.println("Server-List ["+ received.getUsername()+"]: Invio lista..." );
			
			toSend.setMessageList(list);
			Message.sendMessage(client, toSend);
			
			client.close(); 
		} catch (Exception e){
			e.printStackTrace();
		}	
	}

}
