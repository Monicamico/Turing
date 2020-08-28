package turing;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import turing.Message.RESULT;

/* ConcurrentSkipListSet This implementation provides expected average log(n) time cost for the contains, add, and remove operations and their variants. 
 * Insertion, removal, and access operations safely execute concurrently by multiple threads. 
 * Iterators are weakly consistent, returning elements reflecting the state of the set at some point 
 * at or since the creation of the iterator. 
 * They do not throw ConcurrentModificationException, and may proceed concurrently with other operations.
 *  Ascending ordered views and their iterators are faster than descending ones.*/

public class UserDB extends RemoteServer implements UserRMI{
	
	private static final long serialVersionUID = 1L;
	private ConcurrentSkipListSet<User> users =  null;
	
	public UserDB(){
		if (this.users == null)
			this.users = new ConcurrentSkipListSet<User>();
	}
	
	//aggiunge un utente nel database
	public boolean registerUser(String username, String password) throws RemoteException{
		username = username.trim(); //elimino spazi bianchi
		password = password.trim();
		boolean result = this.users.add(new User(username,password));
		System.out.println("Server-Registrazione ["+username+"]: "+result);
		return result;
	}
	
	//lista di utenti nel database
	public ArrayList<String> getListOfUsers(){
		ArrayList<String> list = new ArrayList<String>();
		Iterator<User> it = this.users.iterator();
		while(it.hasNext()){
			list.add(it.next().getUsername());
		}
		return list;
	}
	
	public ConcurrentSkipListSet<User> getUsers(){
		return this.users;
	}
	
	//preleva un utente dal database
	public User getUser(String user) {
		User u = null;
		try { u = users.stream().filter(u1 -> u1.getUsername().equals(user)).findFirst().get();
		} catch (NoSuchElementException e){
			return null;
		}
		return u;
	}
	
	public String getUser(SocketChannel client){
		User user = null;
		try {  user = users.stream().filter(u1 -> (u1.isOnline()).equals(client)).findFirst().get();
		}catch (NoSuchElementException e){
			return null;
		}
		return user.getUsername();	
	}
	
	/* Controlla se l'utente è autorizzato a modificare il documento
	 * non effettua controlli sull'esistenza del documento */
	
	public Message.RESULT haveDocument(String user, String doc){
		User u = getUser(user);
		if (u == null) return RESULT.USR_UNKNOWN;
		Set<Invite> documents = u.getDocuments();
		Iterator<Invite> it = documents.iterator();
		while (it.hasNext()){
			Invite i = it.next();
			if (i.getDocument().equals(doc)) return RESULT.OP_OK;
		}
		return RESULT.NOT_AUTORIZED;
	}
	
	/* controlla se l'utente e il documento esistono
	 * aggiunge il documento nella lista documenti dell'utente e 
	 * aggiunge l'utente nella lista degli utenti autorizzati del documento */
	public Message.RESULT addDocumentToUser(String owner,String user, String doc, DocumentDB dbd) {
		User u = getUser(user);
		Document d = dbd.getDocument(doc);
		if(u == null) return RESULT.USR_UNKNOWN;
		if (d == null) return RESULT.DOC_UNKNOWN;
		if (! d.getOwner().equals(owner)) 
			return RESULT.NOT_OWNER;
		if ( (u.addDocuments(doc,owner) == RESULT.OP_OK ) && (d.addAutorizedUser(user) == RESULT.OP_OK))
			return RESULT.OP_OK;
		return RESULT.OP_ERR;
	}
	
	public Message.RESULT addInviteToUser(Invite i, String user) { 
		//Invito, utente da invitare
		//va chiamata sempre dopo la addDocument
		if ( user == null || i == null) return RESULT.OP_ERR;
		User u = getUser(user);
		if(u == null) 
			return RESULT.USR_UNKNOWN;
		if (u.addInvite(i)) return RESULT.OP_OK;
		return RESULT.OP_ERR;
	}
	
	//lista documenti di un utente
	public Set<Invite> getDocumentsOfUser(String user) {
		User u = getUser(user);
		if(u == null) 
			return null;
		return u.getDocuments();
	}
	
	//lista inviti pendenti di un utente
	public Set<Invite> getInvitesOfUser(String user){
		User u = getUser(user);
		if(u == null) 
			return null;
		return u.getInvites();
	}
	
	public boolean removeInviteOfUser(String user,Invite i){
		User u = getUser(user);
		if(u == null) 
			return false;
		return u.removeInvite(i);
	}
	
	public SocketChannel isOnline(String user){
		User u = getUser(user);
		if(u == null) 
			return null;
		else 
			return u.isOnline();
	}
	
	public  Message.RESULT setOnline(String user, String password, SocketChannel c){
		
		if (user == null || password == null || c == null) return RESULT.OP_ERR;
		User u = getUser(user);
		if (u == null) return RESULT.USR_UNKNOWN;
		if (u.isOnline() != null) return RESULT.OP_ERR;
		if (u.getPassword().equals(password)) {
			u.setOnline(c);
			return RESULT.OP_OK;
		}
		return RESULT.PSW_INCORRECT;	
	}
	
	public Message.RESULT setOffline(String user) throws IOException {
		
		if (user == null) return RESULT.OP_ERR;
		User u = getUser(user);
		if (u == null) return RESULT.DOC_UNKNOWN;
		u.setOffline();
		return RESULT.OP_OK;
	}
	
}
