package turing;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;

import turing.Message.RESULT;

public class DocumentDB {
	private ConcurrentSkipListSet<Document> documents = null;
	
	public DocumentDB(){
		if (this.documents == null)
			this.documents = new ConcurrentSkipListSet<Document>();
	}
	
	//crea un documento nel database
	public Message.RESULT addDocumentDB(Document d) {
		if (d.getNumOfSections() == 0) return RESULT.SEC_ERR;
		if (documents.add(d) == true) return RESULT.OP_OK;
		else return RESULT.DOC_EXISTENT;
	}
	
	public ConcurrentSkipListSet<Document> getDocuments(){
		return this.documents;
	}
	
	//preleva un documento
	public Document getDocument(String doc){
		Document document = null;
		try { document = this.documents.stream().filter(d1 -> d1.getDocName().equals(doc)).findFirst().get();
		} catch (NoSuchElementException e){
			return null;
		}
		return document;
	}
	
	public boolean containsDocumentDB(String doc){
		if (getDocument(doc) == null) return false;
		return true;
	}
	
}
