
package net.sf.freecol.server.ai;

import java.util.*;
import java.util.logging.Logger;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.model.*;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.*;


/**
* The main AI-class. Keeps references to all other AI-classes.
*/
public class AIMain implements FreeColGameObjectListener {
    private static final Logger logger = Logger.getLogger(AIMain.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private FreeColServer freeColServer;    

    /**
    * Contains mappings between <code>FreeColGameObject</code>s
    * and <code>AIObject</code>s.
    */
    private HashMap aiObjects = new HashMap();




    /**
    * Creates a new <code>AIMain</code> and searches the current
    * game for <code>FreeColGameObject</code>s.
    *
    * @see #findNewObjects
    */
    public AIMain(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
        findNewObjects();
    }
    
    
    /**
    * Creates a new <code>AIMain</code> and reads the given element.
    * @see #readFromXMLElement
    */
    public AIMain(FreeColServer freeColServer, Element element) {
        this(freeColServer);
        readFromXMLElement(element);
    }

    

    /**
    * Returns the game.
    */
    public Game getGame() {
        return freeColServer.getGame();
    }


    /**
    * Searches for new {@link FreeColGameObject FreeColGameObjects}. An AI-object is
    * created for each new object.
    */
    private void findNewObjects() {
        Iterator i = freeColServer.getGame().getFreeColGameObjectIterator();
        while (i.hasNext()) {
            FreeColGameObject fcgo = (FreeColGameObject) i.next();
            setFreeColGameObject(fcgo.getID(), fcgo);
        }
    }


    /**
    * Gets the <code>AIObject</code> for the given 
    * <code>FreeColGameObject</code>.
    *
    * @param fcgo The <code>FreeColGameObject</code> to find
    *        the <code>AIObject</code> for.
    * @see #getAIObject(String)
    */
    public AIObject getAIObject(FreeColGameObject fcgo) {
        return getAIObject(fcgo.getID());
    }
    

    /**
    * Gets the <code>AIObject</code> for the given 
    * <code>FreeColGameObject</code>.
    *
    * @param id The ID of the <code>FreeColGameObject</code> to find
    *        the <code>AIObject</code> for.
    * @see #getAIObject(FreeColGameObject)
    * @see FreeColGameObject#getID
    */
    public AIObject getAIObject(String id) {
        return (AIObject) aiObjects.get(id);
    }


    /**
    * Adds a reference to the given <code>AIObject</code>.
    *
    * @param id The ID of the <code>FreeColGameObject</code> the
    *        <code>AIObject</code> is connected to.
    * @param aiObject The <code>AIObject</code> to store a reference
    *        for.
    */
    public void addAIObject(String id, AIObject aiObject) {
        aiObjects.put(id, aiObject);
    }

    
    /**
    * Removes a reference to the given <code>AIObject</code>.
    *
    * @param id The ID of the <code>FreeColGameObject</code> the
    *        <code>AIObject</code> is connected to.
    */
    public void removeAIObject(String id) {
        aiObjects.remove(id);
    }


    /**
    * Gets the <code>FreeColGameObject</code> with the given ID.
    * This is just a convenience method for:
    * {@link Game#getFreeColGameObject}
    *
    * @param id The ID of the <code>FreeColGameObject</code> to find.
    */
    public FreeColGameObject getFreeColGameObject(String id) {
        return freeColServer.getGame().getFreeColGameObject(id);
    }


    /**
    * Creates a new <code>AIObject</code> for a given
    * <code>FreeColGameObject</code>. This method gets called 
    * whenever a new object gets added to the {@link Game}.
    *
    * @param id The ID of the <code>FreeColGameObject</code> to add.
    * @param freeColGameObject The <code>FreeColGameObject</code> to add.
    * @see AIObject
    * @see FreeColGameObject
    * @see FreeColGameObject#getID
    */
    public void setFreeColGameObject(String id, FreeColGameObject freeColGameObject) {
        if (freeColGameObject instanceof Unit) {
            addAIObject(id, new AIUnit(this, (Unit) freeColGameObject));
        } else if (freeColGameObject instanceof ServerPlayer) {
            addAIObject(id, new AIPlayer(this, (ServerPlayer) freeColGameObject));
        }
    }

    
    /**
    * Removes the <code>AIObject</code> for the given <code>FreeColGameObject</code>.
    * @param id The ID of the <code>FreeColGameObject</code>.
    */
    public void removeFreeColGameObject(String id) {
        //AIObject o = getAIObject(id);
        //o.dispose();
        removeAIObject(id);
    }


    /**
    * Stores all the <code>AIObject</code>s and other AI-related information
    * to an <code>Element</code> and returns it.
    *
    * @param document The document in which the <code>Element</code>
    *        should be created.
    * @return The XML-<code>Element</code>.
    */
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        Iterator i = aiObjects.values().iterator();
        while (i.hasNext()) {
            AIObject aio = (AIObject) i.next();
            element.appendChild(aio.toXMLElement(document));
        }

        return element;
    }


    /**
    * Reads all the <code>AIObject</code>s and other AI-related information
    * stored in an <code>Element</code>.
    *
    * @param element The XML-<code>Element</code> containing the information.
    */
    public void readFromXMLElement(Element element) {
        NodeList nl = element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Element childElement = (Element) nl.item(i);
            if (childElement.getTagName().equals(AIUnit.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new AIUnit(this, childElement));
            } else if (childElement.getTagName().equals(AIPlayer.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new AIPlayer(this, childElement));
            } else {
                logger.warning("Unkown AI-object read: " + childElement.getTagName());
            }
        }
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiMain"
    */
    public static String getXMLElementTagName() {
        return "aiMain";
    }
}
