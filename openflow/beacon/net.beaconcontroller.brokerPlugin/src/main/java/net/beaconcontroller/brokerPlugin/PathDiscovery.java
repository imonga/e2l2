package net.beaconcontroller.brokerPlugin;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Arrays;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
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

	public static final int OFACTION_LAST_SWITCH = 1;
	public static final int OFACTION_FIRST_SWITCH = 0;
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
     * 
     * @param srcDpid Datapath id of source OF Switch
     * @param dstDpid Datapath id of destination OF Switch
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
     * 
     *  @param dstDpid Datapath id of destination switch
     *  @param srcMac source mac address 
     *  @param dstMac destination mac address
     *  @param firstIngressPort Ingress port of first OF switch connected to host
     *  @param lastEgressPort Egress port of last OF switch connected to edge router
     *  @param etherType data layer ethertype to create openflow filter
     *  @param bidirectionalFlag flag to indicate whether switch is to be programmed
     *  for bidirectional traffic - true for bidirectional and false for unidirectional
     */
    public void programOFPath(Long dstDpid, byte[] srcMac, 
            byte[] dstMac, Short firstIngressPort, Short lastEgressPort, short etherType, boolean bidirectionalFlag) throws RemoteException {
        
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
            
            if (bidirectionalFlag) {
            	this.createOFmsg(dstNode.sw, dstMac, srcMac, lastEgressPort, inPort, etherType);
            }
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
            
            if (bidirectionalFlag) {
                this.createOFmsg(currNode.sw, dstMac, srcMac, outPort, inPort, etherType);
            }
            childNode = currNode;
            currNode = currNode.parent;
        }
        
        System.out.println("switches programmed");
        
    }
    
    /**
     * check if the node is reachable from source
     * If yes, program the OF switches in the shortest path 
     * 
     * @param dstDpid Datapath id of last OF Switch
     * @param srcMac source mac address 
     * @param dstMac destination mac address
     * @param firstIngressPort Ingress port of first OF switch connected to host
     * @param lastEgressPort Egress port of last OF switch connected to edge router
     * @param etherType data layer ethertype to create openflow filter
     * @param ingressVlan vlan tag by the host
     * @param egressVlan vlan tag returned by the OSCARS
     * @param bidirectionalFlag flag to indicate whether switch is to be programmed
     *  for bidirectional traffic - true for bidirectional and false for unidirectional
     */
    public void programOFPath(Long dstDpid, byte[] srcMac, 
            byte[] dstMac, Short firstIngressPort, Short lastEgressPort, short etherType, Short ingressVlan, Short egressVlan, boolean bidirectionalFlag) throws RemoteException {
        
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
                
                //first switch so create a rule to match on ingress vlan and strip it off
                this.createOFmsg(dstNode.sw, srcMac, dstMac, inPort, lastEgressPort, etherType, ingressVlan, PathDiscovery.OFACTION_FIRST_SWITCH);
                
                if (bidirectionalFlag) {
                	//also last switch for other direction so add ingressVlan for traffic in other direction
                    this.createOFmsg(dstNode.sw, dstMac, srcMac, lastEgressPort, inPort, etherType, ingressVlan, PathDiscovery.OFACTION_LAST_SWITCH);
                }
                
            }
            //last switch so write egressVlan as part of action
            this.createOFmsg(dstNode.sw, srcMac, dstMac, inPort, lastEgressPort, etherType, egressVlan, PathDiscovery.OFACTION_LAST_SWITCH);
            if (bidirectionalFlag) {
            	//for the opposite direction traffic, it will be the first switch
            	//so it will match on OSCARS vlan and strip it off
                this.createOFmsg(dstNode.sw, dstMac, srcMac, lastEgressPort, inPort, etherType, egressVlan, PathDiscovery.OFACTION_FIRST_SWITCH);	
            } 
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
            
            if (currNode.parent != null) {
            	//not the first or last switch, no vlan involved 
            	this.createOFmsg(currNode.sw, srcMac, dstMac, inPort, outPort, etherType);
            	if (bidirectionalFlag) {
            		this.createOFmsg(currNode.sw, dstMac, srcMac, outPort, inPort, etherType);
            	}
            } else {
            	//first switch, match with ingress vlan and strip vlan as a part of action
            	this.createOFmsg(currNode.sw, srcMac, dstMac, inPort, outPort, etherType, ingressVlan, PathDiscovery.OFACTION_FIRST_SWITCH);
            	
            	if (bidirectionalFlag) {
            		//also last switch for other direction
            		this.createOFmsg(currNode.sw, dstMac, srcMac, outPort, inPort, etherType, ingressVlan, PathDiscovery.OFACTION_LAST_SWITCH);
            	}
            }
            
            childNode = currNode;
            currNode = currNode.parent;
        }
        
        System.out.println("switches programmed");
        
    }
    
    
    /**
     * Creates OF message with the appropriate match and action
     * and writes it to the socket used to communicate with the switch
     * 
     * @param sw switch on which rule is to be created
     * @param srcMacAddr source mac address
     * @param dstMacAddr destination mac address
     * @param inPort ingress port
     * @param outPort egress port
     * @param etherType datalayer ethertype of the packets to be matched
     */
    public void createOFmsg (IOFSwitch sw, byte[] srcMacAddr, byte[] dstMacAddr, Short inPort, Short outPort, short etherType) throws RemoteException {
        
        OFMatch match = new OFMatch();
        match.setDataLayerType(etherType); 
        match.setDataLayerSource(srcMacAddr);
        match.setDataLayerDestination(dstMacAddr);
        match.setInputPort(inPort);
        
        //wildcard to match on data-layer destination, source, ethertype and input port
        int wildcard = ~(OFMatch.OFPFW_DL_TYPE | OFMatch.OFPFW_DL_SRC | OFMatch.OFPFW_DL_DST | OFMatch.OFPFW_IN_PORT | OFMatch.OFPFW_DL_VLAN);
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
              .setActions(Collections.singletonList((OFAction) action))
              .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH));
        try {
            sw.getOutputStream().write(fm);
        } catch (IOException e) {
            log.error("Failure writing FlowMod", e);
        } 
    }
    
    /**
     * Creates OF message with the appropriate match (also includes vlan translation)
     * and action and writes it to the socket used to communicate with the switch
     * @param sw switch on which rule is to be created
     * @param srcMacAddr source mac address
     * @param dstMacAddr destination mac address
     * @param inPort ingress port
     * @param outPort egress port
     * @param etherType datalayer ethertype of the packets to be matched
     * @param vlan vlan to include in the rule
     * @param flag flag to indicate if it is first switch in the path or last switch
     */
    public void createOFmsg (IOFSwitch sw, byte[] srcMacAddr, byte[] dstMacAddr, Short inPort, Short outPort, short etherType, Short vlan, int flag) throws RemoteException {
    	
    	int wildcard = 0;
        OFMatch match = new OFMatch();
        match.setDataLayerType(etherType); 
        match.setDataLayerSource(srcMacAddr);
        match.setDataLayerDestination(dstMacAddr);
        match.setInputPort(inPort);
        
        if(flag == PathDiscovery.OFACTION_FIRST_SWITCH) {
        	match.setDataLayerVirtualLan(vlan);
        }
        
        //wildcard to match on data-layer destination, source, ethertype and input port and vlan optionally
        
        if (flag == PathDiscovery.OFACTION_FIRST_SWITCH) {
        	wildcard = ~(OFMatch.OFPFW_DL_TYPE | OFMatch.OFPFW_DL_SRC | OFMatch.OFPFW_DL_DST | OFMatch.OFPFW_IN_PORT | OFMatch.OFPFW_DL_VLAN);
        } else {
        	wildcard = ~(OFMatch.OFPFW_DL_TYPE | OFMatch.OFPFW_DL_SRC | OFMatch.OFPFW_DL_DST | OFMatch.OFPFW_IN_PORT); 
        }
        match.setWildcards(wildcard);
        
        // build action with port set to outPort
        OFActionOutput action = new OFActionOutput()
         .setPort(outPort);
        
        ArrayList<OFAction> actionList = new ArrayList<OFAction> ();
        actionList.add((OFAction)action);
         
        //if last switch create an action to write egress vlan  
        if (flag == PathDiscovery.OFACTION_LAST_SWITCH) { 
        	OFActionVirtualLanIdentifier actionVlan = new OFActionVirtualLanIdentifier();
        	actionVlan.setVirtualLanIdentifier(vlan);
        	actionList.add((OFAction)actionVlan);
        } else if (flag == PathDiscovery.OFACTION_FIRST_SWITCH) {
        	//if first switch create an action to strip vlan
        	OFActionStripVirtualLan actionStripVlan = new OFActionStripVirtualLan();
        	actionList.add((OFAction)actionStripVlan);
        }
        
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
              .setActions(actionList)
              .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH));
        try {
            sw.getOutputStream().write(fm);
        } catch (IOException e) {
            log.error("Failure writing FlowMod", e);
        } 
    }
}