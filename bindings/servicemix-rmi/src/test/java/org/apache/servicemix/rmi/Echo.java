package org.apache.servicemix.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <p>
 * Very simple remote interface to test remote.
 * </p>
 * 
 * @author jbonofre
 */
public interface Echo extends Remote {
    
    public String echo(String message) throws RemoteException;

}
