//package appBroker;

import java.rmi.Naming;
import java.rmi.RemoteException;

import net.beaconcontroller.brokerPlugin.BrokerInterface;

/**
 * Client program for the RMI
 * @author vertika
 *
 */
public class Client {
    
    private static BrokerInterface brokerPlugin;
    private Long srcDatapathId;
    private Long dstDatapathId;
    private Short srcPort;
    private Short dstPort;
    private byte[] srcMac;
    private byte[] dstMac;
    private short etherType;
    private String rmiServerName = "//localhost:2121/BrokerPlugin";    
    
    public Long getSrcDpid() {
        return this.srcDatapathId;
    }
    
    public void setSrcDpid(Long srcDpid) {
        this.srcDatapathId = srcDpid;
    }
    
    public Long getDstDpid() {
        return this.dstDatapathId;
    }
    
    public void setDstDpid(Long dstDpid) {
        this.dstDatapathId = dstDpid;
    }
    
    public Short getSrcPort() {
        return this.srcPort;
    }
    
    public void setSrcPort(Short srcPort) {
        this.srcPort = srcPort;
    }
    
    public Short getDstPort() {
        return this.dstPort;
    }
    
    public void setDstPort(Short dstPort) {
        this.dstPort = dstPort;
    }
    
    public byte[] getSrcMac() {
        return this.srcMac;
    }
    
    public void setSrcMac(byte[] srcMac) {
        this.srcMac = srcMac;
    }
    
    public byte[] getDstMac() {
        return this.dstMac;
    }
    
    public void setDstMac(byte[] dstMac) {
        this.dstMac = dstMac;
    }
    
    public Short getEtherType() {
        return this.etherType;
    }
    
    public void setEtherType(Short etherType) {
        this.etherType = etherType;
    }
   
    public String getRmiServerName() {
        return this.rmiServerName;
    }
    
    public void setRmiServerName(String rmiServer) {
        this.rmiServerName = rmiServer;
    }    
  
    public void programPath() {
        
        //Install Security Manager
        if (System.getSecurityManager() == null) {
            System.out.println("Setting Security Manager!");
            try {
                System.setSecurityManager(new SecurityManager());
            } catch (SecurityException e) {
                System.out.println("SECURITY EXCEPTION CAUSED!");
                e.printStackTrace();
            }
        }
        
        //Get the remote object
        try { 
            brokerPlugin = (BrokerInterface) Naming.lookup (this.rmiServerName);
            if (brokerPlugin == null) {
                System.out.println("loader is null");
            }
        } catch (Exception e) {
            System.out.println ("BrokerPlugin not bound exception: " + e);
        }
        
       
 	System.out.println("Remote Object found*******"); 
        try {
            brokerPlugin.setAppContext();
            brokerPlugin.populateNodeMap();
            brokerPlugin.populateAdj();
           
	    //real code  
	    //brokerPlugin.createBFT(this.srcDatapathId, this.dstDatapathId);
            //brokerPlugin.programOFPath(this.dstDatapathId, this.srcMac, 
            //        this.dstMac, this.srcPort, this.dstPort, this.etherType);
	   
	    //dummy code for testing 
            brokerPlugin.createBFT(5L, 5L);
            brokerPlugin.programOFPath(5L, new byte[]{0x01, 0x01, 0x01, 0x01, 0x01, 0x01}, 
                    new byte[] {0x02, 0x02, 0x02, 0x02, 0x02, 0x02}, (short)0x19, (short)0x1a, (short) 0x8915);
            
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
 
}

