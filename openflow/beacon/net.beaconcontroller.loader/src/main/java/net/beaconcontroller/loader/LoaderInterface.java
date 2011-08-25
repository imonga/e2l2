package net.beaconcontroller.loader;

import java.rmi.*;

import org.osgi.framework.BundleException;

/**
 * Remote Interface for the RMI
 */
public interface  LoaderInterface extends Remote {
  /**
   * Remotely invocable methods.
   * @exception RemoteException if the remote invocation fails.
   * @throws BundleException 
   */
    public void Installbundle(String location) throws RemoteException, BundleException;
    public void Uninstallbundle() throws RemoteException, BundleException;
    public void StartBundle() throws RemoteException, BundleException;
    public void StopBundle() throws RemoteException, BundleException;
    
}
