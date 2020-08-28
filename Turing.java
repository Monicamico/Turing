package turing;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class Turing {

	final static int RMI_PORT = 3000;
	final static int LOGIN_PORT = 2010;
	final static int REQUEST_PORT = 2011;
	final static String DATA_DIR = "./ServerDocuments/";

	//coda condivisa con il dispatcher
	static LinkedBlockingQueue<SocketChannel> sockets = new LinkedBlockingQueue<SocketChannel>(); 
	
	static UserDB databaseUsers = new UserDB();
	static DocumentDB databaseDocuments = new DocumentDB();

	static AtomicInteger n_porta = new AtomicInteger(2012); //numero porta di una chat di gruppo
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
		//stub per la registrazione di un nuovo utente
		try  {
			UserRMI stub = (UserRMI) UnicastRemoteObject.exportObject(databaseUsers, 0);
			LocateRegistry.createRegistry(RMI_PORT);
			Registry r = LocateRegistry.getRegistry(RMI_PORT); 
			r.rebind("USER-RMI", (Remote) stub);
		}
		catch (RemoteException e) {
			System.out.println("Communication-RMI error");
		}
		
		ServerSocketChannel loginChannel;
		ServerSocketChannel requestChannel;
		Selector selector;
		
		File dir = new File(DATA_DIR);
		if (!dir.exists()) dir.mkdirs();
		
		
		try {
			loginChannel = ServerSocketChannel.open();
			requestChannel = ServerSocketChannel.open();
			
			ServerSocket loginSocket = loginChannel.socket();
		    InetSocketAddress loginAddress = new InetSocketAddress(LOGIN_PORT);
		    loginSocket.bind(loginAddress);
		    
		    ServerSocket requestSocket = requestChannel.socket();
		    InetSocketAddress requestAddress = new InetSocketAddress(REQUEST_PORT);
		    requestSocket.bind(requestAddress);
		    
		    System.out.println("Turing-Server: \nListening for connections...\n ");
		    
		    loginChannel.configureBlocking(false);
		    requestChannel.configureBlocking(false);
		    
		    selector = Selector.open();

		    loginChannel.register(selector, SelectionKey.OP_ACCEPT);    
		    requestChannel.register(selector, SelectionKey.OP_ACCEPT);
		    
		} 
		catch (IOException ex) { 
			ex.printStackTrace();
			return; 
		}
		
		Thread dispatcher = new Thread(new Dispatcher());
		dispatcher.start(); //avvio il thread dispatcher
		
		Timer timer = new Timer();
	    //thread che controlla status dei client
	    timer.scheduleAtFixedRate(new CheckClientStatus(), 3000,30000);
		
		try {
			while (true) { 
				
				selector.select(); 
				Set <SelectionKey> readyKeys = selector.selectedKeys();
				Iterator <SelectionKey> iterator = readyKeys.iterator();
				
				while (iterator.hasNext()) {
					
					SelectionKey key = iterator.next();
					iterator.remove(); //rimuove la chiave dal Selected Set, ma non dal registered Set 
					
					if (key.isAcceptable()) {
						ServerSocketChannel server = (ServerSocketChannel)key.channel(); 
						SocketChannel client = server.accept(); 
						
						client.configureBlocking(false);
						client.register(selector, SelectionKey.OP_READ);
					}
		
					else if(key.isReadable()) {
						SocketChannel client = (SocketChannel)key.channel();
						sockets.put(client); //inserisco nella coda per far prelevare al dispatcher
						key.cancel();
					}
				}
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		dispatcher.join();
	}

}
