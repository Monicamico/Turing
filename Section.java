package turing;

import java.io.File;
import java.io.IOException;

import turing.Message.RESULT;

public class Section implements Comparable<Section> {
	private String fileName; //filename
	private String activeUser;
	private int numSec;
	
	public Section(String doc, int index){
		this.fileName = new String(doc+"_"+index);
		this.activeUser = null;
		this.numSec = index;
	}
	
	
	public synchronized Message.RESULT startEdit(String user){
		try {
			
			if (this.activeUser != null) return RESULT.SEC_EDITING;
			this.activeUser = user; 
			String directorypath = Turing.DATA_DIR;
			File dir = new File(directorypath);
			if (!dir.exists()) dir.mkdirs();
			
			String filepath = directorypath+ this.fileName;
			File file = new File(filepath);
			if (!file.exists()) file.createNewFile();

			return RESULT.OP_OK;
			
		} catch (IOException e){
			e.printStackTrace();
			return null;
		}	
	}
	
	public synchronized Message.RESULT endEdit(String user){
		if (this.activeUser == null) return RESULT.OP_ERR;
		if (! (this.activeUser.equals(user)) )
			return RESULT.NOT_AUTORIZED;
		 else 
			 this.activeUser = null;
		return RESULT.OP_OK;
	}
	
	public synchronized boolean isEditing(String user){
		if (this.activeUser == null || ! (this.activeUser.equals(user)) || user == null )
			return false;
		return true;
	}
	public String getFileName(){
		return this.fileName;
	}
	
	public String getActiveUser(){
		return this.activeUser;
	}

	public int getNumSec() {
		return numSec;
	}

	@Override
	public int compareTo(Section o) {
		return this.fileName.compareTo(o.fileName);
	}

}
