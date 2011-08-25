package net.beaconcontroller.loader;


import net.beaconcontroller.core.IBeaconProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.FrameworkUtil;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/**
 * This plugin is used to dynamically load other plugins into the running
 * instance of beacon
 * @author Vertika Singh
 *
 */
public class Loader implements LoaderInterface{
    
    //bundle context of this plugin is used as a dummy to install other plugins
    private static BundleContext bundleContext = FrameworkUtil.getBundle(Loader.class)
    .getBundleContext();
    private Bundle bundle = null;
    private static LoaderInterface stub;
    private static Registry registry;
    private static Loader loader;
    
    protected static Logger logger = LoggerFactory.getLogger(Loader.class);
    protected IBeaconProvider beaconProvider;

    public Loader () throws RemoteException {
    }

    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    /**
     * Startup method which creates a new static loader object and exports
     * it to the rmi registry, port 2020 is used to host the rmi registry
     * @param None
     * @return None 
     */
    public void startUp() {
        System.out.println("loader starting up!");
        
        try {
            String name = "Loader";
            loader= new Loader();
            stub = (LoaderInterface) UnicastRemoteObject.exportObject(loader, 0);
            registry = LocateRegistry.createRegistry(2020);
            registry.rebind(name, stub);
            System.out.println("Loader bound");
        } catch (Exception e) {
            System.err.println("Loader exception:");
            e.printStackTrace();
        }
        
        logger.info("Loader starting up!");
    }
    
    /**
     * Shutdown method notifies the user that the loader plugin is
     * shutting down
     * @param None
     * @return None
     */
    public void shutDown() {
        System.out.println("Loader shutting down!!");
        logger.info("Loader shutting down!!");
    }
    
    /**
     * This method installs a bundle from a location given 
     * to it as a parameter
     * @param location location of the jar file to be installed, as a string
     * @return void
     */
    public void Installbundle(String location) throws RemoteException, BundleException {
        System.out.println("Inside install bundle");
        try {
            this.bundle = bundleContext.installBundle(location);
            System.out.println(bundle.getBundleId());
        } catch (BundleException e) {
            System.out.println("hit exception");
            e.printStackTrace();
        }
    }
    
    /**
     * Uninstalls the bundle which was previously installed 
     * using this class object
     * @param None
     * @return None
     */
    public void Uninstallbundle() throws RemoteException, BundleException {
        
        //return if bundle class variable has not 
        //been set by Installbundle method
        if (this.bundle == null) {
            System.out.println("bundle not installed");
            return;
        }
        
        long bundleId = this.bundle.getBundleId();
        //Uninstall bundle if bundle class variable is set by 
        //Installbundle method
        try {
            this.bundle.uninstall();
            System.out.println("Uninstalled bundle " + bundleId);
        } catch (BundleException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Starts bundle installed by Installbundle method on this object
     * @param None
     * @return None
     */
    public void StartBundle() throws RemoteException, BundleException {
        if (this.bundle == null) {
            System.out.println("bundle not installed");
            return;
        }
        
        long bundleId = this.bundle.getBundleId();
        
        try {
            this.bundle.start();
        } catch (BundleException e) {
            e.printStackTrace();
        }
        System.out.println("Started bundle " + bundleId);
    }
    
    /**
     * Stops bundle installed by Installbundle method on this object
     * @param None
     * @return None
     */
    public void StopBundle() throws RemoteException, BundleException {
        if (this.bundle == null) {
            System.out.println("bundle not installed");
            return;
        }
        
        long bundleId = this.bundle.getBundleId();
        logger.info("Bundle id is {}", bundleId);
        try {
                this.bundle.stop();
        } catch (BundleException e) {
            System.out.println("Stopping bundle hit exception" +
            		"." + bundleId);
            e.printStackTrace();
        }
        System.out.println("Stopped bundle " + bundleId);
        
    }
    
}
