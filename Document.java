package turing;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import turing.Message.RESULT;

public class Document implements Comparable<Document> {
	
	private String docName;
	private String owner;
	private ConcurrentSkipListSet<Section> sections;
	private ConcurrentSkipListSet<String> autorizedUsers = null;
	private AtomicInteger group_port = new AtomicInteger();
	private AtomicInteger numEditing = new AtomicInteger();

	
	public Document (String name, String owner, int numOfSections) {
		name = name.trim(); //elimino spazi bianchi
		owner = owner.trim();
		this.docName = name;
		this.owner = owner;
		this.sections = new ConcurrentSkipListSet<Section>();
		this.autorizedUsers = new ConcurrentSkipListSet<String>();
		this.numEditing.set(0);
		this.group_port.set(0);
		
		for(int i = 0; i<numOfSections; i++){
			Section s = new Section(name,i);
			this.sections.add(s);
		}	
	}
	
	public String getOwner(){
		return this.owner;
	}
	

	public AtomicInteger getNumberOfEditing(){
		return numEditing;
	}
	
	public String getDocName(){
		return this.docName;
	}
	
	public Section getSection(int i){
		if (this.sections.size() <= i) return null;
		return this.sections.stream().filter(s -> s.getNumSec() == i).findFirst().get();
	}
	
	public ConcurrentSkipListSet<Section> getSections(){
		return this.sections;
	}
	
	public int getNumOfSections(){
		return this.sections.size();
	}
	
	public Set<String> getAutorizedUsers(){
		return this.autorizedUsers;
	}
	
	public Message.RESULT addAutorizedUser(String user){
		if (autorizedUsers.add(user)) return RESULT.OP_OK;
		else return RESULT.OP_ERR;
	}
	
	public boolean removeAutorizedUser(String user){
		return this.autorizedUsers.remove(user);
	}
	

	public AtomicInteger getGroupPort(){
		return this.group_port;
	}
	
	public void setGroupPort(int port){
		this.group_port.set(port);
	}

	@Override
	public int compareTo(Document d) {
		return this.docName.compareTo(d.getDocName());
	}

	
}
