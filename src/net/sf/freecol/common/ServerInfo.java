
package net.sf.freecol.common;


import java.util.logging.Logger;
import org.w3c.dom.*;

import java.util.ArrayList;

import net.sf.freecol.common.networking.Message;



public class ServerInfo {
    private String name;
    private String address;
    private int port;

    private int currentlyPlaying;
    private int slotsAvailable;
    private boolean isGameStarted;


    /**
    * Empty constructor that can be used by subclasses.
    */
    protected ServerInfo() {

    }


    /**
    * Creates a new object with the given information.
    *
    * @param name The name of the server.
    * @param address The IP-address of the server.
    * @param port The port number in which clients may connect.
    * @param slotsAvailable Number of players that may conncet.
    * @param currentlyPlaying Number of players that are currently connected.
    * @param isGameStarted <i>true</i> if the game has started.
    */
    public ServerInfo(String name, String address, int port, int slotsAvailable, int currentlyPlaying, boolean isGameStarted) {
        update(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted);
    }

    
    /**
    * Creates an object from the given <code>Element</code>.
    * @param element The XML DOM Element containing the information that will be
    *        used for the new object.
    */
    public ServerInfo(Element element) {
        readFromXMLElement(element);
    }


    /**
    * Updates the object with the given information.
    *
    * @param name The name of the server.
    * @param address The IP-address of the server.
    * @param port The port number in which clients may connect.
    * @param slotsAvailable Number of players that may conncet.
    * @param currentlyPlaying Number of players that are currently connected.
    * @param isGameStarted <i>true</i> if the game has started.
    */
    public void update(String name, String address, int port, int slotsAvailable, int currentlyPlaying, boolean isGameStarted) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.slotsAvailable = slotsAvailable;
        this.currentlyPlaying = currentlyPlaying;
        this.isGameStarted = isGameStarted;
    }

    
    /**
    * Returns the name of the server that is beeing represented by this object.
    */
    public String getName() {
        return name;
    }

    /**
    * Returns the IP-address.
    */
    public String getAddress() {
        return address;
    }


    /**
    * Returns the port in which clients may connect.
    */
    public int getPort() {
        return port;
    }
    
    
    /**
    * Returns the number of currently active (connected and not dead) players.
    */
    public int getCurrentlyPlaying() {
        return currentlyPlaying;
    }
    
    
    /**
    * Returns the number of players that may conncet.
    */
    public int getSlotsAvailable() {
        return slotsAvailable;
    }


    /**
    * Creates an XML-representation of this object.
    * @param document The document in which the element should be created.
    * @return The XML DOM Element representing this object.
    */
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("name", name);
        element.setAttribute("address", address);
        element.setAttribute("port", Integer.toString(port));
        element.setAttribute("slotsAvailable", Integer.toString(slotsAvailable));
        element.setAttribute("currentlyPlaying", Integer.toString(currentlyPlaying));
        element.setAttribute("isGameStarted", Boolean.toString(isGameStarted));
        
        return element;
    }


    /**
    * Reads attributes from the given element.
    * @param element The XML DOM Element containing information that
    *        should be read by this object.
    */
    public void readFromXMLElement(Element element) {
        update(element.getAttribute("name"), element.getAttribute("address"),
                Integer.parseInt(element.getAttribute("port")),
                Integer.parseInt(element.getAttribute("slotsAvailable")),
                Integer.parseInt(element.getAttribute("currentlyPlaying")),
                Boolean.valueOf(element.getAttribute("slotsAvailable")).booleanValue());
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "metaItem".
    */
    public static String getXMLElementTagName() {
        return "serverInfo";
    }


    /**
    * Returns a <code>String</code> representation of this object for debugging purposes.
    */
    public String toString() {
        return name + "(" + address + ":" + port + ") " + currentlyPlaying + ", " + slotsAvailable + ", " + isGameStarted;
    }

}
