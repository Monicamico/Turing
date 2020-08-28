package turing;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UserRMI extends Remote {
	//REGISTRAZIONE
	public boolean registerUser(String username, String password) throws RemoteException;

}
