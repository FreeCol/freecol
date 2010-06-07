/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.server.ai;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectListener;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

/**
* The main AI-class. Keeps references to all other AI-classes.
*/
public class AIMain extends FreeColObject implements FreeColGameObjectListener {
    private static final Logger logger = Logger.getLogger(AIMain.class.getName());


    private FreeColServer freeColServer;
    private int nextID = 1;

    /**
    * Contains mappings between <code>FreeColGameObject</code>s
    * and <code>AIObject</code>s.
    */
    private HashMap<String, AIObject> aiObjects = new HashMap<String, AIObject>();




    /**
    * Creates a new <code>AIMain</code> and searches the current
    * game for <code>FreeColGameObject</code>s.
    *
    * @param freeColServer The main controller object for the
    *       server.
    * @see #findNewObjects()
    */
    public AIMain(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
        findNewObjects();
    }
    
    /**
    * Creates a new <code>AIMain</code> and reads the given element.
    * 
    * @param freeColServer The main controller object for the
    *       server.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) 
    *       that describes this object.
    * @see #readFromXMLElement
    */
    public AIMain(FreeColServer freeColServer, Element element) {
        this(freeColServer);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>AIMain</code> and reads the given element.
     * 
     * @param freeColServer The main controller object for the
     *       server.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see #readFromXML
     */
     public AIMain(FreeColServer freeColServer, XMLStreamReader in) throws XMLStreamException {
         this(freeColServer);
         readFromXML(in);
     }
     

    /**
     * Gets the main controller object for the server.
     * @return The <code>FreeColServer</code>-object.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
    * Gets a unique ID for identifying an <code>AIObject</code>.
    * @return A unique ID.
    */
    public String getNextID() {
        String id = "am" + Integer.toString(nextID);
        nextID++;
        return id;
    }

    /**
     * Checks the integrity of this <code>AIMain</code>
     * by checking if there are any
     * {@link AIObject#isUninitialized() uninitialized objects}.
     * 
     * Detected problems gets written to the log.
     * 
     * @return <code>true</code> if the <code>Game</code> has
     *      been loaded properly.
     */
    public boolean checkIntegrity() {
        boolean ok = true;
        for (AIObject ao : aiObjects.values()) {
            if (ao.isUninitialized()) {
                logger.warning("Uninitialized object: " + ao.getId() + " (" + ao.getClass() + ")");
                ok = false;
            }
        }
        Iterator<FreeColGameObject> fit = getGame().getFreeColGameObjectIterator();
        while (fit.hasNext()) {
            FreeColGameObject f = fit.next();
            if ((f instanceof Unit || f instanceof Colony ||
                 (f instanceof Player && !((Player)f).getName().equals(Player.UNKNOWN_ENEMY)) )
                    && !aiObjects.containsKey(f.getId())) {
                logger.warning("Missing AIObject for: " + f.getId());
                ok = false;
            }
        }
        if (ok) {
            logger.info("AIMain integrity ok.");
        } else {
            logger.warning("AIMain integrity test failed.");
        }
        return ok;
    }

    /**
    * Returns the game.
    * @return The <code>Game</code>.
    */
    public Game getGame() {
        return freeColServer.getGame();
    }


    /**
    * Returns an instance of <code>PseudoRandom</code>. It that can be
    * used to generate random numbers.
    * @return The instance of <code>PseudoRandom</code>.
    */
    public PseudoRandom getPseudoRandom() {
        return freeColServer.getPrivatePseudoRandom();
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
    public void findNewObjects(boolean overwrite) {
        Iterator<FreeColGameObject> i = freeColServer.getGame().getFreeColGameObjectIterator();
        while (i.hasNext()) {
            FreeColGameObject fcgo = i.next();
            if (overwrite || getAIObject(fcgo) == null) {
                setFreeColGameObject(fcgo.getId(), fcgo);
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
        return getAIObject(fcgo.getId());
    }


    /**
    * Gets the <code>AIObject</code> identified by the given ID.
    *
    * @param id The ID of the <code>AIObject</code>.
    * @see #getAIObject(FreeColGameObject)
    * @return The <code>AIObject</code>.
    */
    public AIObject getAIObject(String id) {        
        return aiObjects.get(id);
    }


    /**
    * Adds a reference to the given <code>AIObject</code>.
    *
    * @param id The ID of the <code>AIObject</code>.
    * @param aiObject The <code>AIObject</code> to store a reference
    *        for.
    * @exception IllegalStateException if an <code>AIObject</code> with
    *       the same <code>id</code> has already been created.
    */
    public void addAIObject(String id, AIObject aiObject) {
        if (aiObjects.containsKey(id)) {
            throw new IllegalStateException("AIObject already created: " + id);
        }
        if (aiObject == null) {
            throw new NullPointerException("aiObject == null");
        }
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

    public void ownerChanged(FreeColGameObject source, Player oldOwner, Player newOwner) {
        AIObject ao = getAIObject(source);
        if (ao != null) {
            ao.dispose();
            setFreeColGameObject(source.getId(), source);
        }
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
    * @see FreeColGameObject#getId
    */
    public void setFreeColGameObject(String id, FreeColGameObject freeColGameObject) {
        if (aiObjects.containsKey(id)) {
            return;
        }
        if (!id.equals(freeColGameObject.getId())) {
            throw new IllegalArgumentException("!id.equals(freeColGameObject.getId())");
        }
        if (freeColGameObject instanceof Unit) {
            new AIUnit(this, (Unit) freeColGameObject);
        } else if (freeColGameObject instanceof ServerPlayer) {
            if (FreeCol.usesExperimentalAI()) {
                ServerPlayer p = (ServerPlayer) freeColGameObject;
                if (!p.isREF() && !p.isIndian() && p.isEuropean()) {
                    logger.info("Using experimental ColonialAIPlayer for "+p.getName());
                    new ColonialAIPlayer(this, p);
                } else {
                    new StandardAIPlayer(this, p);
                }
            } else {
                new StandardAIPlayer(this, (ServerPlayer) freeColGameObject);
            }
        } else if (freeColGameObject instanceof Colony) {
            new AIColony(this, (Colony) freeColGameObject);
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
        removeAIObject(id);
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related 
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("nextID", Integer.toString(nextID));

        Iterator<AIObject> i = aiObjects.values().iterator();
        while (i.hasNext()) {
            AIObject aio = i.next();
            
            if ((aio instanceof Wish) && !((Wish) aio).shouldBeStored()) {
                continue;
            }

            try {
                if (aio.getId() != null) {
                    aio.toXML(out);
                } else {
                    logger.warning("aio.getId() == null, for: " + aio.getClass().getName());
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warning(sw.toString());
            }
        }

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if an error occured during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        aiObjects.clear();
        
        if (!in.getLocalName().equals(getXMLElementTagName())) {
            logger.warning("Expected element name, got: " + in.getLocalName());
        }
        final String nextIDStr = in.getAttributeValue(null, "nextID");
        if (nextIDStr != null) {
            nextID = Integer.parseInt(nextIDStr);
        }
        
        String lastTag = "";
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final String tagName = in.getLocalName();
            try {         
                final String oid = in.getAttributeValue(null, "ID");
                if (oid != null && aiObjects.containsKey(oid)) {
                    getAIObject(oid).readFromXML(in);
                } else if (tagName.equals(AIUnit.getXMLElementTagName())) {
                    new AIUnit(this, in);
                } else if (tagName.equals(AIPlayer.getXMLElementTagName())) {
                    new StandardAIPlayer(this, in);
                } else if (tagName.equals(ColonialAIPlayer.getXMLElementTagName())) {
                    new ColonialAIPlayer(this, in);
                } else if (tagName.equals(AIColony.getXMLElementTagName())) {
                    new AIColony(this, in);
                } else if (tagName.equals(AIGoods.getXMLElementTagName())) {
                    new AIGoods(this, in);
                } else if (tagName.equals(WorkerWish.getXMLElementTagName())) {
                    new WorkerWish(this, in);
                } else if (tagName.equals(GoodsWish.getXMLElementTagName())) {
                    new GoodsWish(this, in);
                } else if (tagName.equals(TileImprovementPlan.getXMLElementTagName())) {
                    new TileImprovementPlan(this, in);                
                } else {
                    logger.warning("Unkown AI-object read: " + tagName + "(" + lastTag + ")");
                }
                lastTag = in.getLocalName();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warning("Exception while reading an AIObject: " + sw.toString());
                while (!in.getLocalName().equals(tagName) && !in.getLocalName().equals(getXMLElementTagName())) {
                    in.nextTag();                    
                }
                if (!in.getLocalName().equals(getXMLElementTagName())) {
                    in.nextTag();
                }
            }
        }
        
        if (!in.getLocalName().equals(getXMLElementTagName())) {
            logger.warning("Expected element name (2), got: " + in.getLocalName());
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
    
    /**
     * Computes how many objects of each class have been created, 
     * to track memory leaks over time
     */
    public HashMap<String, Long> getAIStatistics() {
        
        HashMap<String, Long> map = new HashMap<String, Long>();
        Iterator<AIObject> iter = aiObjects.values().iterator();
        while (iter.hasNext()) {
            AIObject obj = iter.next();
            String className = obj.getClass().getSimpleName();
            if (map.containsKey(className)) {
                Long count = map.get(className);
                count++;
                map.put(className, count);
            } else {
                Long count = new Long(1);
                map.put(className, count);
            }
        }
        
        return map;
    }
}
