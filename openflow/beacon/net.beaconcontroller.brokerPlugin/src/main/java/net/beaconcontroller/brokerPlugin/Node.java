package net.beaconcontroller.brokerPlugin;

import java.util.Collection;
import java.util.HashMap;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.topology.LinkTuple;

/**
 * This class defines a graph node for the OF network topology
 * @author vertika
 *
 */
public class Node {
    
    /*color indicates if the node has been visited and 
     * all its neighbors are visited*/
    
    /*white indicates that the node has not been visited*/
    public static final int WHITE = 0;
    
    /*gray indicates that the node has been visited but 
     * all its neighbors haven't been visited yet*/
    public static final int GRAY = 1;
    
    /*black indicates that the node itself and all its neighbors have been visited*/
    public static final int BLACK = 2;
    
    protected IOFSwitch sw;
    protected int color;
    protected Node parent;
    protected Collection<Node> adj;
    protected HashMap<IOFSwitch, LinkTuple> links;
    protected double distance;

}
