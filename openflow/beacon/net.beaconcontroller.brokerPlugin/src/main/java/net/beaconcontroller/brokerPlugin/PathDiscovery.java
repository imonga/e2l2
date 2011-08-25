package net.beaconcontroller.brokerPlugin;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFSwitch;
import org.springframework.context.ApplicationContext;
import net.beaconcontroller.topology.*;
import net.beaconcontroller.getAppContext.AppContextHolder;

/**
 * This class uses a Breadth first search algorithm to find out the 
 * shortest path from source to destination in the topology graph
 * 
 * Once a Breadth first tree is formed by invoking its methods in the
 * right order, it has a method which can be invoked for programming the
 * OF switches in the shortest path
 *  
 * @author vertika
 *
 */

public class PathDiscovery extends Node implements BrokerInterface {
    
    protected ITopology topology;
    protected IBeaconProvider beaconProvider;
    protected static ApplicationContext appContext;
    private static Collection<IOFSwitch> allSwitches;
    private static Set<LinkTuple> allLinks;
    protected Map<LinkTuple, Long> topoMap;
    public HashMap<IOFSwitch, Node> nodeMap;
    
    private static BrokerInterface stub;
    private static Registry registry;
    private static PathDiscovery pathDiscovery;
    
    private int rmiPort = 2121; //rmiPort where the rmiRegistry will be hosted
    private short idleTimeOut = 3600; //idle timeout for openflow entry in seconds
    protected static Logger log = LoggerFactory.getLogger(PathDiscovery.class);

    public PathDiscovery() throws RemoteException{
        System.out.println("In PathDiscovery Constructor");
        
    }
    
    public void setTopology(ITopology topology) {
        this.topology = topology;
    }
    
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
    
    public void setRmiPort(int port) throws RemoteException{
        this.rmiPort = port;
    }
    
    public int getRmiPort() throws RemoteException{
        return this.rmiPort;
    }
    
    public void setIdleTimeOut(short timeOut) throws RemoteException{
        this.idleTimeOut = timeOut;
    }
    
    public short getIdleTimeOut() throws RemoteException{
        return this.idleTimeOut;
    }
    
    /**
     * Retrieves beans for beaconProvider and topology from application
     * context in Spring and sets them for the exported remote object when
     * called remotely
     */
    public void setAppContext() throws RemoteException{
        appContext =
            AppContextHolder.getApplicationContext(true);
         PathDiscovery appCtx = (PathDiscovery) appContext.getBean("pathDiscovery");
         this.beaconProvider = appCtx.beaconProvider;
         this.topology = appCtx.topology;
        
        
    }
    
    /**
     * Startup method creates an object of this class and makes it into a stub
     * (exportable object), then creates a registry and binds the object to the
     * registry 
     */
    public void startUp(){
        
        System.out.println("Broker Plugin starting up!");
        
        try {
            String name = "BrokerPlugin";
            pathDiscovery = new PathDiscovery();
            stub = (BrokerInterface) UnicastRemoteObject.exportObject(pathDiscovery, 0);
            registry = LocateRegistry.createRegistry(this.rmiPort);
            registry.rebind(name, stub);
            System.out.println("PathDiscovery bound");
        } catch (Exception e) {
            System.err.println("PathDiscovery exception:");
            e.printStackTrace();
        }
        
        log.info("Path Discovery starting up!");
    }
    
    /**
     * Destroy method for this class
     */
    public void shutDown() {
        System.out.println("PathDiscovery shutting down!!");
        log.info("PathDiscovery shutting down!!");
    }
    
    
     /**
      * Populates a hash map which has its key as the switch and value as the node
      */
    public void populateNodeMap () throws RemoteException {
        
        if (beaconProvider.getSwitches() == null) {
            log.info("No switches attached");
            return;
        }
        allSwitches = beaconProvider.getSwitches().values();
        this.nodeMap = new HashMap<IOFSwitch, Node>();
        
        for (IOFSwitch swLocal : allSwitches) {
            Node u = new Node();
            u.sw = swLocal;
            u.color = Node.WHITE;
            u.parent = null;
            u.adj = null;
            u.links = null;
            u.distance = Double.POSITIVE_INFINITY;
            this.nodeMap.put(swLocal, u);
        }
        
        System.out.println("NodeMap populated");
        
    }
    
    /**
     * Populates adjacencies and links for each node
     */
    public void populateAdj() throws RemoteException {
        
        if (topology.getLinks() == null) {
            log.info("No links found");
            return;
        }
        
        allLinks = topology.getLinks().keySet();
        
        for (LinkTuple link : allLinks) {
            Node srcNode = this.nodeMap.get(link.getSrc().getSw());
            Node dstNode = this.nodeMap.get(link.getDst().getSw());
            
            srcNode.adj.add(dstNode);
            srcNode.links.put(dstNode.sw, link);
            
            dstNode.adj.add(srcNode);
            dstNode.links.put(srcNode.sw, link);
        }
        
        System.out.println("Adjacency populated");
        
    }
    
    /** 
     * creates a breadth first tree for the topology graph(which is undirected)
     * with source as src node and destination as dst node using Breadth first
     * search algorithm
     */
    public void createBFT(Long srcDpid, Long dstDpid) throws RemoteException {
        
        boolean foundDst = false;
        
        if (this.beaconProvider.getSwitches() == null) {
            System.out.println("No Switches attached yet");
            log.info("No Switches attached yet");
            return;
        }
        
        IOFSwitch src = this.beaconProvider.getSwitches().get(srcDpid);
        if(src == null) {
            log.info("src node with srcDpid:" + srcDpid + "could not be found");
            return;
        }
        
        Node srcNode = this.nodeMap.get(src);
        
        
        Queue<Node> q = new LinkedList<Node>();
        
        srcNode.color = Node.GRAY;
        srcNode.distance = 0;
        q.add(srcNode);
        
        while (!q.isEmpty()) {
            Node u = q.remove();
            
            if (u.adj != null) {
                for (Node v : u.adj) {  
                
                    if (v.color == Node.WHITE) {
                        v.color = Node.GRAY;
                        v.distance = u.distance + 1;
                        v.parent = u;
                        q.add(v);
                    }
                
                    //break if destination is found and its parent is known
                    if (v.sw.getId() == dstDpid) {
                        foundDst = true;
                        break;
                    }
                }
            
                //break if destination's parent has been set
                if (foundDst) {
                    break;
                }
            }
            u.color = Node.BLACK;
        }
        
        System.out.println("BFT created");
    }
    
    /**
     * check if the node is reachable from source
     * If yes, program the OF switches in the shortest path 
     */
    public void programOFPath(Long dstDpid, byte[] srcMac, 
            byte[] dstMac, Short firstIngressPort, Short lastEgressPort, short etherType) throws RemoteException {
        
        Short inPort;
        Short outPort;
        
        System.out.println("OF Path programmed");
        
        if (this.beaconProvider.getSwitches() == null) {
            System.out.println("No Switches attached yet");
            log.trace("No Switches attached yet");
            return;
        }
        
        IOFSwitch dst = this.beaconProvider.getSwitches().get(dstDpid);
        
        if(dst == null) {
            log.info("dst node with dstDpid:" + dstDpid + "could not be found");
            return;
        }
        
        Node dstNode = this.nodeMap.get(dst);
        
        if (dstNode.distance < Double.POSITIVE_INFINITY) {
            
            if (dstNode.parent != null) {
                LinkTuple link = dstNode.links.get(dstNode.parent);
            
                if(link.getSrc().getSw() == dstNode.sw) {
                    inPort = link.getSrc().getPort(); 
                } else {
                    inPort = link.getDst().getPort();
                }
            } else {
                //if there is only one switch such that first 
                //switch is same as last switch in the path
                inPort = firstIngressPort;
            }
            
            this.createOFmsg(dstNode.sw, srcMac, dstMac, inPort, lastEgressPort, etherType);
        } else {
            log.error("destination is not reachable from source");
            return;
        }
        
        Node currNode = dstNode.parent;
        Node childNode = dstNode;
        
        while(currNode != null) {
            
            if (currNode.parent != null) {
                
                LinkTuple link = currNode.links.get(currNode.parent);
                
                if (link.getSrc().getSw() == currNode) {
                    inPort = link.getSrc().getPort();
                } else {
                    inPort = link.getDst().getPort();
                }
            } else {
                inPort = firstIngressPort;
            }
            
            LinkTuple link = childNode.links.get(currNode);
            
            if (link.getSrc().getSw() == currNode) {
                outPort = link.getSrc().getPort();
            } else {
                outPort = link.getDst().getPort();
            }
            this.createOFmsg(currNode.sw, srcMac, dstMac, inPort, outPort, etherType);
            
            childNode = currNode;
            currNode = currNode.parent;
        }
        
        System.out.println("switches programmed");
        
    }
    
    /**
     * Creates OF message with the appropriate match and action
     * and writes it to the socket used to communicate with the switch
     */
    public void createOFmsg (IOFSwitch sw, byte[] srcMacAddr, byte[] dstMacAddr, Short inPort, Short outPort, short etherType) throws RemoteException {
        
        OFMatch match = new OFMatch();
        match.setDataLayerType(etherType); 
        match.setDataLayerSource(srcMacAddr);
        match.setDataLayerDestination(dstMacAddr);
        match.setInputPort(inPort);
        
        //wildcard to match on data-layer destination, source, ethertype and input port
        int wildcard = ~(OFMatch.OFPFW_DL_TYPE | OFMatch.OFPFW_DL_SRC | OFMatch.OFPFW_DL_DST | OFMatch.OFPFW_IN_PORT);
        match.setWildcards(wildcard);
        
       // build action with port set to outPort
        OFActionOutput action = new OFActionOutput()
         .setPort(outPort);
       
        System.out.println("action"+action);
        System.out.println("match"+match);
        System.out.println(String.format("0x%02X", match.getDataLayerType()));
        System.out.println(String.format("0x%02X", match.getDataLayerSource()));
        System.out.println(String.format("0x%02X", match.getDataLayerDestination()));
        
        // build flow mod
        OFFlowMod fm = (OFFlowMod) sw.getInputStream().getMessageFactory()
            .getMessage(OFType.FLOW_MOD);
        
        fm.setIdleTimeout(this.idleTimeOut)
              .setOutPort((short) OFPort.OFPP_NONE.getValue())
              .setMatch(match)
              .setActions(Collections.singletonList((OFAction)action))
              .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH));
        try {
            sw.getOutputStream().write(fm);
        } catch (IOException e) {
            log.error("Failure writing FlowMod", e);
        } 
    }
}