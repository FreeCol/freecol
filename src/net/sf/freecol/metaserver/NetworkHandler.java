
package net.sf.freecol.metaserver;


import java.io.IOException;
import java.util.logging.Logger;
import org.w3c.dom.Element;


import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.Connection;




/**
* Handles all network messages beeing sent to the metaserver.
*/
public final class NetworkHandler implements MessageHandler {
    private static Logger logger = Logger.getLogger(NetworkHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private MetaServer metaServer;
    private MetaRegister metaRegister;



    /**
    * The constructor to use.
    */
    public NetworkHandler(MetaServer metaServer, MetaRegister metaRegister) {
        this.metaServer = metaServer;
        this.metaRegister = metaRegister;
    }





    /**
    * Handles a network message.
    *
    * @param connection The <code>Connection</code> the message came from.
    * @param element The message to be processed.
    */
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        String type = element.getTagName();

        if (element != null) {
            if (type.equals("register")) {
                reply = register(connection, element);
            } else if (type.equals("update")) {
                reply = update(connection, element);
            } else if (type.equals("getServerList")) {
                reply = getServerList(connection, element);
            } else if (type.equals("remove")) {
                reply = remove(connection, element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(connection, element);
            } else {
                logger.warning("Unkown request: " + type);
            }
        }

        return reply;
    }

    
    /**
    * Handles a "getServerList"-request.
    * @param element The element containing the request.
    */
    private Element getServerList(Connection connection, Element element) {
        return metaRegister.createServerList();
    }


    /**
    * Handles a "register"-request.
    * @param element The element containing the request.
    */
    private Element register(Connection connection, Element element) {
        String name = element.getAttribute("name");
        String address = connection.getSocket().getInetAddress().getHostAddress();
        int port = Integer.parseInt(element.getAttribute("port"));
        int slotsAvailable = Integer.parseInt(element.getAttribute("slotsAvailable"));
        int currentlyPlaying = Integer.parseInt(element.getAttribute("currentlyPlaying"));
        boolean isGameStarted = Boolean.valueOf(element.getAttribute("isGameStarted")).booleanValue();

        metaRegister.addServer(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted);

        return null;
    }


    /**
    * Handles an "update"-request.
    * @param element The element containing the request.
    */
    private Element update(Connection connection, Element element) {
        String name = element.getAttribute("name");
        String address = connection.getSocket().getInetAddress().getHostAddress();
        int port = Integer.parseInt(element.getAttribute("port"));
        int slotsAvailable = Integer.parseInt(element.getAttribute("slotsAvailable"));
        int currentlyPlaying = Integer.parseInt(element.getAttribute("currentlyPlaying"));
        boolean isGameStarted = Boolean.valueOf(element.getAttribute("isGameStarted")).booleanValue();

        metaRegister.updateServer(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted);
        
        return null;
    }

    
    /**
    * Handles a "remove"-request.
    * @param element The element containing the request.
    */
    private Element remove(Connection connection, Element element) {
        String address = connection.getSocket().getInetAddress().getHostAddress();
        int port = Integer.parseInt(element.getAttribute("port"));

        metaRegister.removeServer(address, port);
        
        return null;
    }
    
    
    /**
    * Handles a "disconnect"-request.
    * @param element The element containing the request.
    */
    private Element disconnect(Connection connection, Element element) {
        try {
            connection.reallyClose();
        } catch (IOException e) {
            logger.warning("Could not close the connection.");
        }

        metaServer.removeConnection(connection);
        
        return null;
    }
}
