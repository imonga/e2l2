/**
 * 
 */
package net.beaconcontroller.brokerPlugin;

import java.rmi.*;

/**
 * Remote Interface for the RMI
 * @author vertika
 *
 */
public interface BrokerInterface extends Remote {

    /**
     * Remotely invocable methods.
     * @exception RemoteException if the remote invocation fails.
     */
    public void setRmiPort(int port) throws RemoteException;
    public int getRmiPort() throws RemoteException;
    public void setIdleTimeOut(short timeOut) throws RemoteException;
    public short getIdleTimeOut() throws RemoteException;
    public void populateNodeMap () throws RemoteException;
    public void populateAdj() throws RemoteException;
    public void createBFT(Long srcDpid, Long dstDpid) throws RemoteException;
    public void programOFPath(Long dstDpid, byte[] srcMac, 
            byte[] dstMac, Short firstIngressPort, Short lastEgressPort, 
            short etherType, Short ingressVlan, Short egressVlan) throws RemoteException;
    public void programOFPath(Long dstDpid, byte[] srcMac, 
            byte[] dstMac, Short firstIngressPort, Short lastEgressPort, 
            short etherType) throws RemoteException;
    public void setAppContext() throws RemoteException;
}
