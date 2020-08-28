package turing;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class InviteReader implements Runnable {

	private SocketChannel client;
	
	public InviteReader(SocketChannel client){
		this.client = client;
	}
	
	@Override
	public void run() {
		while(true){
			try {
				client.configureBlocking(true);
				Invite received = Invite.receiveInvite(client);
				if(received == null) {
					client.close();
					break;
				}
				System.out.println("Ricevuto invito:\n- Creatore: "+received.getOwner()+"\n- Documento: "+received.getDocument());
			} catch (ClosedChannelException e){
				System.out.println("Channel Chiuso");
				return;
				//e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} 
			
		}
	}

}
