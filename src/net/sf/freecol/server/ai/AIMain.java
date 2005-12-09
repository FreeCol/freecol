
package net.sf.freecol.server.ai;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectListener;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
* The main AI-class. Keeps references to all other AI-classes.
*/
public class AIMain implements FreeColGameObjectListener {
    private static final Logger logger = Logger.getLogger(AIMain.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private FreeColServer freeColServer;
    private Random random = new Random();
    private int nextID = 1;

    /**
    * Contains mappings between <code>FreeColGameObject</code>s
    * and <code>AIObject</code>s.
    */
    private HashMap aiObjects = new HashMap();




    /**
    * Creates a new <code>AIMain</code> and searches the current
    * game for <code>FreeColGameObject</code>s.
    *
    * @see #findNewObjects()
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
    * Gets a unique ID for identifying an <code>AIObject</code>.
    * @return A unique ID.
    */
    public String getNextID() {
        String id = "aiMain:" + Integer.toString(nextID);
        nextID++;
        return id;
    }


    /**
    * Returns the game.
    * @return The <code>Game</code>.
    */
    public Game getGame() {
        return freeColServer.getGame();
    }


    /**
    * Returns an instance of the class <code>Random</code>. It that can be
    * used to generate random numbers.
    * @return The instance of <code>Random</code>.
    */
    public Random getRandom() {
        return random;
    }


    /**
    * Searches for new {@link FreeColGameObject FreeColGameObjects}. An AI-object is
    * created for each object.
    *
    * <br><br>
    *
    * Note: Any existing <code>AIObject</code>s will be overwritten.
    * @see #findNewObjects(boolean)
    */
    private void findNewObjects() {
        findNewObjects(true);
    }


    /**
    * Searches for new {@link FreeColGameObject FreeColGameObjects}. An AI-object is
    * created for each new object.
    * @param overwrite Determines wether any old <code>AIObject</code>
    *       should be overwritten or not.
    */
    private void findNewObjects(boolean overwrite) {
        Iterator i = freeColServer.getGame().getFreeColGameObjectIterator();
        while (i.hasNext()) {
            FreeColGameObject fcgo = (FreeColGameObject) i.next();
            if (overwrite || getAIObject(fcgo) == null) {
                setFreeColGameObject(fcgo.getID(), fcgo);
            }
        }
    }


    /**
    * Gets the <code>AIObject</code> for the given
    * <code>FreeColGameObject</code>.
    *
    * @param fcgo The <code>FreeColGameObject</code> to find
    *        the <code>AIObject</code> for.
    * @see #getAIObject(String)
    * @return The <code>AIObject</code>.
    */
    public AIObject getAIObject(FreeColGameObject fcgo) {
        return getAIObject(fcgo.getID());
    }


    /**
    * Gets the <code>AIObject</code> identified by the given ID.
    *
    * @param id The ID of the <code>AIObject</code>.
    * @see #getAIObject(FreeColGameObject)
    * @return The <code>AIObject</code>.
    */
    public AIObject getAIObject(String id) {
        return (AIObject) aiObjects.get(id);
    }


    /**
    * Adds a reference to the given <code>AIObject</code>.
    *
    * @param id The ID of the <code>AIObject</code>.
    * @param aiObject The <code>AIObject</code> to store a reference
    *        for.
    */
    public void addAIObject(String id, AIObject aiObject) {
        aiObjects.put(id, aiObject);
    }


    /**
    * Removes a reference to the given <code>AIObject</code>.
    *
    * @param id The ID of the <code>AIObject</code>.
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
    * @return The <code>FreeColGameObject</code>.
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
        } else if (freeColGameObject instanceof Colony) {
            addAIObject(id, new AIColony(this, (Colony) freeColGameObject));
        }
    }


    /**
    * Removes the <code>AIObject</code> for the given <code>FreeColGameObject</code>.
    * @param id The ID of the <code>FreeColGameObject</code>.
    */
    public void removeFreeColGameObject(String id) {
        AIObject o = getAIObject(id);
        if (o != null) {
            o.dispose();
        }
        //removeAIObject(id);
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

        element.setAttribute("nextID", Integer.toString(nextID));

        Iterator i = aiObjects.values().iterator();
        while (i.hasNext()) {
            AIObject aio = (AIObject) i.next();

            // TODO: Remove debugging line:
            if (aio instanceof AIUnit && ((AIUnit) aio).getUnit() == null) {
                logger.warning("aoi.getUnit() == null");
                continue;
            }

            try {
                element.appendChild(aio.toXMLElement(document));
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warning(sw.toString());
            }
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
        if (element.hasAttribute("nextID")) {
            nextID = Integer.parseInt(element.getAttribute("nextID"));
        }
        NodeList nl = element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Node n = nl.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            Element childElement = (Element) n;
            if (childElement.getTagName().equals(AIUnit.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new AIUnit(this, childElement));
            } else if (childElement.getTagName().equals(AIPlayer.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new AIPlayer(this, childElement));
            } else if (childElement.getTagName().equals(AIColony.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new AIColony(this, childElement));
            } else if (childElement.getTagName().equals(AIGoods.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new AIGoods(this, childElement));
            } else if (childElement.getTagName().equals(WorkerWish.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new WorkerWish(this, childElement));
            } else if (childElement.getTagName().equals(GoodsWish.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new GoodsWish(this, childElement));
            } else if (childElement.getTagName().equals(TileImprovement.getXMLElementTagName())) {
                addAIObject(childElement.getAttribute("ID"), new TileImprovement(this, childElement));                
            } else {
                logger.warning("Unkown AI-object read: " + childElement.getTagName());
            }
        }

        // TODO: Avoid this:
        logger.info("Second pass.");
        nl = element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Node n = nl.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            Element childElement = (Element) n;
            AIObject ao = getAIObject(childElement.getAttribute("ID"));
            if (ao != null) {
                ao.readFromXMLElement(childElement);
            }
        }

        // This should not be necessary - but just in case:
        findNewObjects(false);
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiMain"
    */
    public static String getXMLElementTagName() {
        return "aiMain";
    }
}
