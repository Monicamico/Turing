package turing;

import java.nio.channels.SocketChannel;

import turing.Message.OPERATION;

public class Logout implements Runnable {

	SocketChannel client; //socket a cui invia l'esito
	Message received;
	
	public Logout(SocketChannel c, Message received){
		this.client = c;
		this.received = received;
	}
	
	public void run() {
		Message toSend = new Message();
		try 
		{
			Message.RESULT result = Turing.databaseUsers.setOffline(received.getUsername());
			//setOffline chiude socket del login e imposta socket associata all'utente a null
			System.out.println("Server-Logout ["+ received.getUsername()+"]: "+ result);
			toSend.setMessageResult(OPERATION.LOGOUT,result);
			Message.sendMessage(client, toSend);
			client.close(); //socket a cui invia l'esito
			
		} catch (Exception e){
			e.printStackTrace();
		}
	
	}

}
