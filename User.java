package turing;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentSkipListSet;

import turing.Message.RESULT;


public class User implements Serializable, Comparable<User> {

	private static final long serialVersionUID = 1L;
	
	private String username;
	private String password;
	private SocketChannel onLine; // se online salvo il socketchannel sulla variabile
	private ConcurrentSkipListSet<Invite> documents;
	private ConcurrentSkipListSet<Invite> invites;
	
	public User (String u, String p){
		this.username = u;
		this.password = p;
		documents = new ConcurrentSkipListSet<Invite>();
		invites = new ConcurrentSkipListSet<Invite>();
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public String getPassword(){
		return this.password;
	}
	
	public SocketChannel isOnline(){
		return this.onLine;
	}
	
	public ConcurrentSkipListSet<Invite> getDocuments(){
		return this.documents;
	}
	
	public ConcurrentSkipListSet<Invite> getInvites(){
		return this.invites;
	}
	
	public boolean addInvite(Invite i){
		return this.invites.add(i);
	}

	public boolean removeInvite(Invite i){
		return this.invites.remove(i);
	}
	
	public Message.RESULT addDocuments(String doc,String owner){
		if (this.documents.add(new Invite(owner,doc))) return RESULT.OP_OK;
		else return RESULT.DOC_EXISTENT;
	}
	
	
	public boolean equals(User u){
		return ( this.username.equals(u.getUsername()));
	}

	@Override
	public int compareTo(User o) {
		return  this.username.compareTo(o.getUsername());
	}
	
	public void setOnline(SocketChannel c){
		this.onLine = c;
	}
	
	public void setOffline() throws IOException{
		this.onLine.close();
		this.onLine = null;
	}
	
	
}
