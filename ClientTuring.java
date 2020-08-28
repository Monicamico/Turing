package turing;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import turing.Message.RESULT;


public class ClientTuring {
	
	final static int RMI_PORT = 3000;
	final static int LOGIN_PORT = 2010;
	final static int REQUEST_PORT = 2011;
	final static String GROUP_ADDRESS = "224.0.0.4";
	final static String DATA_DIR = "./ClientDocuments/";
	
	
	public static void main(String[] args) {
		
		String[] arguments;
		UserRMI databaseUSER;
		Remote RemoteObject;
		
		/* variabili utili per gli stati del client*/
		boolean alreadyLogged = false;
		boolean editing = false;
		String UsernameOnline = null;
		
		String editingDoc = null;
		int editingSec = -1;
		
		Thread inviteReader = null;
		Thread readGroup = null;
		SocketChannel client = null;
		
		Message esito = new Message();
		Message request = new Message();
		Scanner in = new Scanner(System.in);
		
		File dir = new File(DATA_DIR); //directory del client
		if (!dir.exists()) dir.mkdirs();
		
		File directory; //directory dell'utente
		String command = "prova";
		String newLine;
		
		MulticastSocket group_socket = null;
		int porta = 0;
		
		try 
		{
			Registry r = LocateRegistry.getRegistry(3000); 
			RemoteObject = r.lookup("USER-RMI");
			databaseUSER = (UserRMI) RemoteObject; //oggetto per la registrazione
			
			SocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(),LOGIN_PORT);
			SocketAddress addressRequest = new InetSocketAddress(InetAddress.getLocalHost(),REQUEST_PORT); 
			
			
			System.out.println("turing --help per visualizzare la lista dei comandi");
			
			while(!command.equals("exit") ){
				
				System.out.println("Inserire un comando");
				newLine = in.nextLine();
				arguments = newLine.split(" ");
				
				if(arguments.length >= 2) 
				{ 	
					command = arguments[1];
					switch(command) {
	
					 	case "register": {
					 		if (arguments.length < 4){
					 			System.out.println("ERRORE: inserire tutti i parametri richiesti.\n"
					 					+ "turing register <username> <password>");
					 		} else if(alreadyLogged == true){
					 			System.out.println("ERRORE: non puoi registrare utenti mentre sei loggato");
					 		} else {
					 			if (databaseUSER.registerUser(arguments[2],arguments[3]))
					 				System.out.println("Utente "+arguments[2]+" registrato con successo.");
					 			else System.out.println("Utente "+arguments[2]+" non registrato.");
					 		}	break;
							
						} case "login": {
							
							if (arguments.length < 4){
								System.out.println("ERRORE: inserire tutti i parametri richiesti.\n"
										+ "- turing login <username> <password>");
								
							} else if (alreadyLogged == false) {
								
								SocketChannel clientTemp = SocketChannel.open(address); //apre connessione temporanea
								clientTemp.configureBlocking(true);
								
								request.setMessageLogin(arguments[2],arguments[3]);
								Message.sendMessage(clientTemp, request);
								
								esito = Message.receiveMessage(clientTemp);
								RESULT rs = esito.getResultType();
								if (rs == RESULT.OP_OK){
									
									System.out.println("Login "+arguments[2] +" effettuato con successo.");
									/* se login ha avuto successo assegno la connessione temporanea 
									 * alla connessione permanente, dove riceverà gli inviti. */
									client = clientTemp;
									client.configureBlocking(true);
									//avvio thread che legge gli inviti
									inviteReader = new Thread(new InviteReader(client));
									inviteReader.start();
									/* imposto lo stato del client: Loggato*/
									UsernameOnline = arguments[2];
									alreadyLogged = true;
									
									directory = new File(DATA_DIR+UsernameOnline+"/");
									if (!directory.exists()) directory.mkdirs();
									
								}
								else if (rs == RESULT.PSW_INCORRECT) {
									System.out.println("ERRORE: Password non corretta");
									clientTemp.close();
								}
								else if (rs == RESULT.USR_UNKNOWN) {
									System.out.println("ERRORE: Utente non registrato");
									clientTemp.close();
								} else if (rs == RESULT.OP_ERR) {
									System.out.println("ERRORE: Impossibile effettuare il login");
									clientTemp.close();
								}
								else clientTemp.close();
								
							} else {
								
								if (UsernameOnline.equals(arguments[2]))
									System.out.println("ERRORE: sei già loggato come "+UsernameOnline);
								else System.out.println("ERRORE: Per effettuare il login come "+arguments[2]+" effettua prima il logout");
							} break;
									
						} case "create":{
							
							if (arguments.length < 4){
								System.out.println("ERRORE: inserire tutti i parametri richiesti.\n"
										+ "turing create <doc> <num_sezioni>");
								
							} else if (alreadyLogged == false){
									System.out.println("ERRORE: Devi loggarti per creare un documento");
									
							} else if (editing == true){
								System.out.println("ERRORE: Non puoi creare un documento mentre modifichi");
							} else {
								String document = arguments[2];
								int numSezioni = 0;
								
								boolean prova = false;
								/* controllo se il numero di sezione passato è un intero*/
								while (prova != true){
									try {
										numSezioni = Integer.parseInt(arguments[3]);
										prova = true;
									} catch (NumberFormatException e){
										System.out.println("Attenzione: il numero di sezioni non può essere rappresento da una stringa\n"
												+ "Inserire un intero che rappresenti il numero di sezioni");
										arguments[3]= in.nextLine();
									}	
								}
							
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
								
								request.setMessageDocument(Message.OPERATION.CREATE_DOC,UsernameOnline,document,numSezioni);
								Message.sendMessage(clientRequest, request); //invio richiesta
								
								esito.resetMessage();
								esito = Message.receiveMessage(clientRequest);
								//controllo esito
								RESULT rs = esito.getResultType();
								if (rs == RESULT.OP_OK)
									System.out.println("Documento "+document +" composto da "+numSezioni +" sezioni creato con successo.");
								else if (rs == RESULT.DOC_EXISTENT) System.out.println("ERRORE: documento "+document+" già esistente!");
								else if (rs == RESULT.SEC_ERR) System.out.println("ERRORE: impossbile creare un documento con "+ numSezioni+" sezioni.");
								else System.out.println("ERRORE: documento "+document+" non creato.");
								
								clientRequest.close();	//chiudo la connessione della richiesta
							} break;
							
						} case "share":{ //invia un invito
							
							if (arguments.length < 4){
								System.out.println("ERRORE: inserire tutti i parametri richiesti.\n"
										+ "turing share <document> <user>");
								
							} else if (alreadyLogged == false) {
								System.out.println("ERRORE: Devi loggarti per richiedere la visualizzazione di un documento");
								
							} else if (editing == true){
								System.out.println("ERRORE: Non puoi inviare un invito mentre editi");
							} else {
								
								String userToInvite = arguments[3];
								String document = arguments[2];
								
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
								
								request.setMessageShare(UsernameOnline, userToInvite, document);
								Message.sendMessage(clientRequest, request);
								
								esito = Message.receiveMessage(clientRequest);
								RESULT rs = esito.getResultType();
								
								if (rs == RESULT.OP_OK) System.out.println("Inviato invito a "+userToInvite+" con successo!");
								else if (rs == RESULT.DOC_UNKNOWN) System.out.println("ERRORE: il documento "+document+" non esiste.");
								else if (rs == RESULT.USR_UNKNOWN) System.out.println("ERRORE: l'utente "+userToInvite+" non esiste.");
								else if (rs == RESULT.NOT_OWNER) System.out.println("ERRORE: soltanto il creatore del documento può condividerlo.");
								else System.out.println("ERRORE: invito non inviato.");
								clientRequest.close();
										
							}
							
							break;
										
						} case "show":{ //showDocument e showSection
							
							if (arguments.length < 3){
								System.out.println("ERRORE: inserire tutti i parametri richiesti.\n"
										+ "turing show <doc>\n"
										+ "turing show <doc> <section>");
								
							} else if (alreadyLogged == false) {
									System.out.println("ERRORE: Devi loggarti per richiedere la visualizzazione di un documento");
									
							} else if (editing == true){
								System.out.println("ERRORE: Non puoi richiedere un documento mentre editi");
								
							} else {
								
								int numSezione = 0;
								String document = arguments[2];
								boolean isInteger = false;
								
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
							
								String filePath;
								
								if (arguments.length == 4){

									//controllo che il numero di sezione sia un intero
									while (!isInteger){
										try {
											numSezione= Integer.parseInt(arguments[3]);
											isInteger = true;
										} catch (NumberFormatException e){
											System.out.println("Attenzione: il numero di sezione non può essere rappresento da una stringa\n"
													+ "Inserire un intero che lo rappresenti\n"
													+ "Altrimenti inserire -1 per visualizzare l'intero documento");
											arguments[3]= in.nextLine();
										}	
									}
									request.setMessageDocument(Message.OPERATION.SHOW_S,UsernameOnline,document,numSezione);
									filePath = document + "_"+numSezione;
									
								} else {
									request.setMessageDocument(Message.OPERATION.SHOW_D,UsernameOnline,document,-1);
									filePath = document;
								}
								
								Message.sendMessage(clientRequest, request);
								
								esito = Message.receiveMessage(clientRequest);
								RESULT rs = esito.getResultType();
								
								//se l'esito è positivo scarico il file
								if (rs == RESULT.OP_OK) {
									ByteBuffer bufferfile = Message.read(clientRequest);
									FileChannel file = FileChannel.open(Paths.get(DATA_DIR+UsernameOnline+"/",filePath),StandardOpenOption.WRITE,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
									Message.writeFileChannel(bufferfile, file);
									System.out.println("Ricevuto documento "+filePath+" in:\n"+DATA_DIR+UsernameOnline+"/"+filePath);	
								} 
								else if (rs == RESULT.DOC_UNKNOWN) System.out.println("ERRORE : Documento "+filePath+" non esiste");
								else if (rs == RESULT.SEC_INEXISTENT) System.out.println("ERRORE : impossibile richiedere "+filePath+" sezione "+numSezione+" inesistente.");
								else if (rs == RESULT.NOT_AUTORIZED) System.out.println("ERRORE : non hai le autorizzazioni necessarie per richiedere il documento");
								else if (rs == RESULT.USR_UNKNOWN) System.out.println("ERRORE : utente non registrato");
								else System.out.println("ERRORE : documento non scaricato.");
								
								clientRequest.close();	
							} break;
							
						} case "list":{
							
							if (alreadyLogged == false){
								System.out.println("Devi loggarti per richiedere la lista");
							} else if (editing == true){
								System.out.println("Non richiedere la lista mentre editi");
							} else {
								
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
								
								request.setMessageListOp(UsernameOnline);
								Message.sendMessage(clientRequest, request);
					
								esito = Message.receiveMessage(clientRequest);
								Set<Invite> listDocuments = esito.getList();
								if (listDocuments.isEmpty()) 
									System.out.println("Nessun documento");
								else {
									System.out.println("Lista documenti:");
									Iterator<Invite> it = listDocuments.iterator();
									int n = 1;
									while (it.hasNext()){
										Invite i = it.next();
										System.out.println(n+". documento: "+i.getDocument());
										System.out.println("    creatore: "+i.getOwner());
										n++;
									}
								}
								clientRequest.close();	
							}
							break;
									
						} case "edit":{
							
							if (arguments.length < 4) {
								System.out.println("ERRORE: inserire tutti i parametri richiesti.\n"
										+ "turing edit <doc> <num_sezione>");
								
							} else if (alreadyLogged == false){
									System.out.println("ERRORE: devi loggarti per modificare un documento");
									
							} else if (editing == true){
								System.out.println("ERRORE: non puoi modificare più di una sezione");
							} else {
									
								int numSezione = 0;
								String document = arguments[2];
								boolean isInteger = false;
								
								while (!isInteger){
									try {
										numSezione= Integer.parseInt(arguments[3]);
										isInteger = true;
									} catch (NumberFormatException e){
										System.out.println("Attenzione: il numero di sezione non può essere rappresento da una stringa\n"
												+ "Inserire un intero che lo rappresenti");
										arguments[3]= in.nextLine();
									}	
								}
								
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
								
								request.setMessageDocument(Message.OPERATION.EDIT_DOC,UsernameOnline,document,numSezione);
								Message.sendMessage(clientRequest, request);
						
								esito = Message.receiveMessage(clientRequest);
								
								RESULT rs = esito.getResultType();
								
								//nome del file da ricevere 
								String filePath = document + "_"+numSezione;
								
								if (rs == RESULT.OP_OK)
								{
									/* imposto stato del client: Editing*/
									editing = true;
									editingDoc = document;
									editingSec = numSezione;
									
									
									//leggo il file
									ByteBuffer bufferfile = Message.read(clientRequest);
									bufferfile.flip();
									
									System.out.println("Ricevuto documento "+document+" sezione "+numSezione +" in:\n"+DATA_DIR+UsernameOnline+"/"+filePath);
								
									FileChannel file = FileChannel.open(Paths.get(DATA_DIR+UsernameOnline+"/",filePath),StandardOpenOption.READ,StandardOpenOption.WRITE,StandardOpenOption.CREATE);
						
									Message.writeFileChannel(bufferfile, file);
									bufferfile.clear();
									
									//legge porta e indirizzo per il gruppo di chat
									ByteBuffer infoMs = Message.read(clientRequest);
									
									if (infoMs.hasRemaining()){
										porta = infoMs.getInt();
										infoMs.clear();
										
										/* creo il gruppo */
										group_socket = new MulticastSocket(porta);
										InetAddress ia_g = InetAddress.getByName(GROUP_ADDRESS);
										group_socket.joinGroup(ia_g);
										
										//thread che legge messaggi del gruppo e li inserisce in una coda
										readGroup = new Thread(new ChatManager(group_socket)); 
										readGroup.start();
									}
									
								} else if (rs == RESULT.DOC_UNKNOWN) System.out.println("ERRORE: Documento richiesto non esistente");
								else if (rs == RESULT.SEC_INEXISTENT) System.out.println("ERRORE: impossibile richiedere "+filePath+" sezione "+numSezione+" inesistente.");
								else if (rs == RESULT.NOT_AUTORIZED) System.out.println("ERRORE: non hai le autorizzazioni necessarie per richiedere il documento");
								else System.out.println("ERRORE: Impossibile modificare la sezione richiesta");
								
								clientRequest.close();	//chiudo socket della richiesta
								
							} break;
							
						} case "end-edit":{
							
							if (alreadyLogged == false){
									System.out.println("ERRORE: Devi loggarti per modificare un documento");
									
							} else if (editing == false) {
								System.out.println("ERRORE: non stai modificando nessun documento");
								
							} else {
								
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
								/* invio la richiesta */
								request.setMessageDocument(Message.OPERATION.END_EDIT,UsernameOnline,editingDoc,editingSec);
								Message.sendMessage(clientRequest, request);
								
								String filePath = editingDoc + "_"+ editingSec;
								FileChannel filec = FileChannel.open(Paths.get(DATA_DIR+UsernameOnline+"/",filePath),StandardOpenOption.READ);
								ByteBuffer bufferToSend = Message.readFileChannel(filec);
								//invio contenuto del file
								Message.send(clientRequest,bufferToSend);
								
								//ricevo l'esito
								esito = Message.receiveMessage(clientRequest);
								
								RESULT rs = esito.getResultType();
								
								if (rs == RESULT.OP_OK) {
									
									/* imposto lo stato del client: Logged*/
									editing = false;
									editingDoc = null;
									editingSec = -1;
									InetAddress ia = InetAddress.getByName(GROUP_ADDRESS);
									group_socket.leaveGroup(ia); //lascio il gruppo 
									group_socket.close();
									
									System.out.println(filePath + " inviato correttamente.");
									readGroup.join();
									
								} else System.out.println(filePath + "non inviato.");
								clientRequest.close();	
							}
							
							break;
							
						} case "send":{ //manda messaggio sul gruppo
							
							if (alreadyLogged == false){
								System.out.println("ERRORE: Devi loggarti prima di fare una send");
								
							} else if (editing == false){
								System.out.println("ERRORE: Non stai modificando nessun documento");
									
							} else {
								
								System.out.println("Inserisci il messaggio da mandare");
								String line = in.nextLine();
								
								LocalTime time = LocalTime.now();
								String message = time.getHour() + "."+ time.getMinute()+ " " + UsernameOnline +": " +line;
								
								ByteBuffer len_aux = ByteBuffer.allocate(Integer.BYTES).putInt(message.length());
								len_aux.flip();
								DatagramPacket packet_dim = new DatagramPacket(len_aux.array(),Integer.BYTES, InetAddress.getByName(GROUP_ADDRESS), porta);
								group_socket.send(packet_dim); //invio dimensione del messaggio
								DatagramPacket packet = new DatagramPacket(message.getBytes(),message.getBytes().length, InetAddress.getByName(GROUP_ADDRESS), porta);
								group_socket.send(packet); //invio il pacchetto
								
							}
							
							break;
							
						} case "receive":{ //riceve messaggi dal gruppo
							
							if (alreadyLogged == false){
								System.out.println("ERRORE: Devi loggarti prima di fare una receive");
							} else if (editing == false){
								
								System.out.println("ERRORE: Nessuna chat aperta! inizia a modificare un documento per ricevere messaggi");
									
							} else {
								//leggo messaggi ricevuti
								while (ChatManager.messages.size() != 0){
									String message = ChatManager.messages.take();
									System.out.println(message);
								}
							
							} break;
							
						} case "--help":{
							
							System.out.println("usage: turing COMMAND [ARGS...] \n\ncommands:\n"
									+ "register <username> <password>\n"
									+ "login <username> <password>\n"
									+ "logout\n"
									+ "create <doc> <numsezioni>\n"
									+ "share <doc> \n"
									+ "show <doc> <sec>\n"
									+ "show <doc>\n"
									+ "list: mostra la lista dei documenti\n"
									+ "edit <doc> <sec>: modifica una sezione del documento\n"
									+ "end-edit: fine modifica\n"
									+ "send: invia un msg sulla chat\n"
									+ "receive: visualizza i msg ricevuti sulla chat\n"
									+ "exit: esce dal programma\n");
							break;
						
						} case "logout":{
							
							if (alreadyLogged == true && editing == false){
								
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
								
								request.setMessageLogout(UsernameOnline);
								Message.sendMessage(clientRequest, request);
								
								esito = Message.receiveMessage(clientRequest);
								RESULT rs = esito.getResultType();
								if (rs == RESULT.OP_OK)
									System.out.println("Logout da "+UsernameOnline +" eseguito con successo.");
								else System.out.println("Logout non eseguito.");
								UsernameOnline = null;
								alreadyLogged = false;
								
								clientRequest.close(); //chiudo socket di richiesta operazione
								client.close();	//chiudo socket permanente appartenente all'utente
								inviteReader.join();
								
							} else if (editing == true){
								System.out.println("ERRORE: prima di effettuare il logout salva il documento con end-edit.");
									
							} else if (alreadyLogged == false) {
								System.out.println("ERRORE: Non puoi effettuare il logout, non sei loggato");
							}
							break;
							
						} case "exit":{
							
							if (editing == true){
								
								System.out.println("ERRORE: prima di uscire salva il documento con end-edit.");
								command = "riprova";
								
							} else if (alreadyLogged == true)
							{
								System.out.println("Effettuo il Logout...");
								SocketChannel clientRequest = SocketChannel.open(addressRequest); //apro socket per operazione
								clientRequest.configureBlocking(true);
								
								request.setMessageLogout(UsernameOnline);
								Message.sendMessage(clientRequest, request);
								
								esito = Message.receiveMessage(clientRequest);
								
								System.out.println("Logout "+UsernameOnline +": "+ esito.getResultType());
								UsernameOnline = null;
								alreadyLogged = false;
								clientRequest.close(); //chiudo socket di richiesta operazione
									//chiudo socket permanente appartenente all'utente	
							} 
							if (alreadyLogged == false && editing == false)
								client.close();
							break;
							
						} default: {
							System.out.println("ERRORE: comando non riconosciuto.\n usage: turing COMMAND [ARGS...]");
						}
						
					}
				} else System.out.println("ERRORE: numero di parametri non valido!\n"
						+ "usage: turing COMMAND [ARGS...]");
				
			}//end while
		
			//esco
			inviteReader.join();
			System.out.println("Esco...");
			in.close();	
			System.exit(0);
	
		}
		catch(InterruptedException e){
			System.out.println("Thread Interrotto");
			in.close();	
			System.exit(-1);
		} catch (ConnectException e) {
			System.out.println("Nessuna connessione con il server");
			in.close();	
			System.exit(-1);
			
		} catch(IOException e) {
			System.out.println("Connessione interrotta");
			in.close();
			System.exit(-1);
			
		} catch(NotBoundException e){
			in.close();	
		}
	}

}
