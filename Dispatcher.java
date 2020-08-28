package turing;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import turing.Message.RESULT;

public class Dispatcher implements Runnable {
	ExecutorService executor = new ThreadPoolExecutor(5, 10, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	@Override
	public void run(){

		while(true){
			try 
			{ 
				SocketChannel client = Turing.sockets.take();
				client.configureBlocking(true);
				Message received = Message.receiveMessage(client);
				if (received == null) {
					client.close();
					System.out.println("client chiuso.");
				} else {
					switch(received.getOp()){
					
						case LOGIN:{
							System.out.println("\nRICHIESTO LOGIN");
							try { executor.execute(new Login(client, received)); }
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
								client.close();
							}
							break;
						}
						
						case LOGOUT: {
							System.out.println("\nRICHIESTO LOGOUT");
							try { executor.execute(new Logout(client, received));}
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						}
						
						case LIST: {
							System.out.println("\nRICHIESTA LISTA DOCUMENTI");
							try {executor.execute(new List(client, received));}
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						}
						
						case CREATE_DOC:{
							System.out.println("\nRICHIESTA CREAZIONE DOCUMENTO");
							try {executor.execute(new CreateDocument(client, received));}
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						}
						
						case SHARE:{
							System.out.println("\nRICHIESTA INVITO");
							try {executor.execute(new Share(client, received));}
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						}
						
						
						case EDIT_DOC:{
							System.out.println("\nRICHIESTA MODIFICA DOCUMENTO");
							try {executor.execute(new Edit(client, received));}
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						} 
						
						case END_EDIT: {
							System.out.println("\nRICHIESTA FINE MODIFICA");
							try {executor.execute(new EndEdit(client, received));}
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						}
						
						case SHOW_S: {
							System.out.println("\nRICHIESTA MOSTRA SEZIONE");
							try {executor.execute(new ShowSection(client, received));}
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						} 
						
						case SHOW_D: {
							System.out.println("\nRICHIESTA MOSTRA DOCUMENTO");
							try {executor.execute(new ShowDocument(client, received)); }
							catch (RejectedExecutionException e){
								System.out.println("Impossibile eseguire la richiesta.");
								Message error = new Message();
								error.setMessageResult(received.getOp(),RESULT.OP_ERR);
								Message.sendMessage(client, error);
							}
							break;
						}
						default: {
							System.out.println("Ricevuta operazione errata!");
							Message error = new Message();
							error.setMessageResult(received.getOp(),RESULT.OP_ERR);
							Message.sendMessage(client, error);
						}
						
					}
				}
				}  catch (IOException e) {
					
				} catch (InterruptedException e) {
					
				}
		}
		
	}
	
}
