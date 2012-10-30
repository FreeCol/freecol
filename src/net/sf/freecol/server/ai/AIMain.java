/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectListener;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The main AI-class. Keeps references to all other AI-classes.
 */
public class AIMain extends FreeColObject
    implements FreeColGameObjectListener {

    private static final Logger logger = Logger.getLogger(AIMain.class.getName());

    /** The server that this AI is operating within. */
    private FreeColServer freeColServer;

    /** The next AI identifier index. */
    private int nextId = 1;

    /**
     * Contains mappings between <code>FreeColGameObject</code>s
     * and <code>AIObject</code>s.
     */
    private final HashMap<String, AIObject> aiObjects
        = new HashMap<String, AIObject>();


    /**
     * Creates a new <code>AIMain</code> and searches the current
     * game for <code>FreeColGameObject</code>s.
     *
     * @param freeColServer The main controller object for the server.
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
    public AIMain(FreeColServer freeColServer, XMLStreamReader in)
        throws XMLStreamException {
        this(freeColServer);

        readFromXML(in);
    }


    /**
     * Gets the main controller object for the server.
     *
     * @return The <code>FreeColServer</code>-object.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Convenience accessor for the Game.
     *
     * @return The <code>Game</code> this AI is operating in.
     */
    public Game getGame() {
        return freeColServer.getGame();
    }

    /**
     * Gets a unique identifier for an <code>AIObject</code>.
     *
     * @return A unique identifier.
     */
    public String getNextId() {
        String id = "am" + Integer.toString(nextId);
        nextId++;
        return id;
    }

    /**
     * Gets a random value from the server to use for individual AI player
     * PRNG seeds.
     *
     * @param logMe A logging string.
     * @return A random seed.
     */
    public int getRandomSeed(String logMe) {
        return Utils.randomInt(logger, logMe, freeColServer.getServerRandom(),
            Integer.MAX_VALUE);
    }

    /**
     * Checks the integrity of this <code>AIMain</code> by checking if
     * there are any invalid objects.
     *
     * Detected problems are logged.
     *
     * @return <code>true</code> if the <code>Game</code> has
     *      been loaded properly.
     */
    public boolean checkIntegrity() {
        boolean ok = true;
        for (AIObject ao : aiObjects.values()) {
            if (!ao.checkIntegrity()) {
                logger.warning("Invalid AIObject: " + ao.getId()
                    + " (" + ao.getClass() + ")");
                ok = false;
            }
        }

        Iterator<FreeColGameObject> fit
            = getGame().getFreeColGameObjectIterator();
        while (fit.hasNext()) {
            FreeColGameObject f = fit.next();
            if ((f instanceof Unit
                 || f instanceof Colony
                 || (f instanceof Player && !((Player)f).isUnknownEnemy()))
                && !aiObjects.containsKey(f.getId())) {
                logger.warning("Missing AIObject for: " + f.getId());
                ok = false;
            }
        }
        return ok;
    }

    /**
     * Fixes some integrity problems of this <code>AIMain</code>.
     *
     * @return True if the integrity problems are fixed.
     */
    public boolean fixIntegrity() {
        for (AIObject ao : new ArrayList<AIObject>(aiObjects.values())) {
            if (!ao.checkIntegrity()) {
                logger.warning("Dropping invalid AIObject: " + ao.getId()
                    + " (" + ao.getClass() + ")");
                ao.fixIntegrity();
            }
        }

        Iterator<FreeColGameObject> fit
            = getGame().getFreeColGameObjectIterator();
        while (fit.hasNext()) {
            FreeColGameObject f = fit.next();
            if ((f instanceof Unit
                 || f instanceof Colony
                 || (f instanceof Player && !((Player)f).isUnknownEnemy()))
                && !aiObjects.containsKey(f.getId())) {
                logger.warning("Added missing AIObject for: " + f.getId());
                setFreeColGameObject(f.getId(), f);
            }
        }

        return checkIntegrity();
    }

    /**
     * Searches for new {@link FreeColGameObject
     * FreeColGameObjects}. An AI-object is created for each object.
     *
     * Note: Any existing <code>AIObject</code>s will be overwritten.
     * @see #findNewObjects(boolean)
     */
    private void findNewObjects() {
        findNewObjects(true);
    }

    /**
     * Searches for new {@link FreeColGameObject FreeColGameObjects}.
     * An AI-object is created for each new object.
     *
     * @param overwrite Determines wether any old <code>AIObject</code>
     *       should be overwritten or not.
     */
    public void findNewObjects(boolean overwrite) {
        Iterator<FreeColGameObject> i
            = freeColServer.getGame().getFreeColGameObjectIterator();
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
     * Gets the AI colony corresponding to a given colony.
     *
     * @param colony The <code>Colony</code> to look up.
     * @return The corresponding AI colony, or null if not found.
     */
    public AIColony getAIColony(Colony colony) {
        AIObject aio = getAIObject(colony.getId());
        return (aio instanceof AIColony) ? (AIColony) aio : null;
    }

    /**
     * Gets the AI player corresponding to a given player.
     *
     * @param player The <code>Player</code> to look up.
     * @return The corresponding AI player, or null if not found.
     */
    public AIPlayer getAIPlayer(Player player) {
        AIObject aio = getAIObject(player.getId());
        return (aio instanceof AIPlayer) ? (AIPlayer) aio : null;
    }

    /**
     * Gets the AI unit corresponding to a given unit.
     *
     * @param unit The <code>Unit</code> to look up.
     * @return The corresponding AI unit, or null if not found.
     */
    public AIUnit getAIUnit(Unit unit) {
        AIObject aio = getAIObject(unit.getId());
        return (aio instanceof AIUnit) ? (AIUnit) aio : null;
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
     * Replaces the AI object when ownership changes.
     *
     * @param source The <code>FreeColGameObject</code> that has changed.
     * @param oldOwner The old owning <code>Player</code>.
     * @param newOwner The new owning <code>Player</code>.
     */
    public void ownerChanged(FreeColGameObject source, Player oldOwner,
                             Player newOwner) {
        AIObject ao = getAIObject(source);
        logger.finest("Owner changed for " + source.getId()
            + " with AI object: " + ao);
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
     * @param fcgo The <code>FreeColGameObject</code> to add.
     * @see AIObject
     * @see FreeColGameObject
     * @see FreeColGameObject#getId
     */
    public void setFreeColGameObject(String id, FreeColGameObject fcgo) {
        if (aiObjects.containsKey(id)) return;
        if (!id.equals(fcgo.getId())) {
            throw new IllegalArgumentException("!id.equals(fcgo.getId())");
        }
        if (fcgo instanceof Colony) {
            new AIColony(this, (Colony)fcgo);
        } else if (fcgo instanceof ServerPlayer) {
            ServerPlayer p = (ServerPlayer)fcgo;
            if (p.isIndian()) {
                new NativeAIPlayer(this, p);
            } else if (p.isREF()) {
                new REFAIPlayer(this, p);
            } else if (p.isEuropean()) {
                new EuropeanAIPlayer(this, p);
            }
        } else if (fcgo instanceof Unit) {
            new AIUnit(this, (Unit)fcgo);
        }
    }

    /**
     * Removes the <code>AIObject</code> for a given AI id.
     * Needed for interface FreeColGameObjectListener.
     *
     * @param id The AI object identifier.
     */
    public void removeFreeColGameObject(String id) {
        AIObject o = getAIObject(id);
        if (o != null) o.dispose();
        removeAIObject(id);
    }

    /**
     * Computes how many objects of each class have been created, to
     * track memory leaks over time
     */
    public HashMap<String, String> getAIStatistics() {
        HashMap<String, String> stats = new HashMap<String, String>();
        HashMap<String, Long> objStats = new HashMap<String, Long>();
        Iterator<AIObject> iter = aiObjects.values().iterator();
        while (iter.hasNext()) {
            AIObject obj = iter.next();
            String className = obj.getClass().getSimpleName();
            if (objStats.containsKey(className)) {
                Long count = objStats.get(className);
                count++;
                objStats.put(className, count);
            } else {
                Long count = new Long(1);
                objStats.put(className, count);
            }
        }
        for (String k : objStats.keySet()) {
            stats.put(k, Long.toString(objStats.get(k)));
        }

        return stats;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute("nextID", Integer.toString(nextId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        // Using a copy of the objects defensively against races.
        for (AIObject aio : new ArrayList<AIObject>(aiObjects.values())) {
            if (!aio.checkIntegrity()) {
                logger.warning("Integrity failure: " + aio);
                continue;
            }
            if (aio instanceof Wish) {
                Wish wish = (Wish)aio;
                if (!wish.shouldBeStored()) continue;
            }

            try {
                if (aio.getId() == null) {
                    logger.warning("Null AI identifier for: "
                        + aio.getClass().getName());
                } else {
                    aio.toXML(out);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to write AI object: " + aio,
                    e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        aiObjects.clear();

        if (!in.getLocalName().equals(getXMLElementTagName())) {
            logger.warning("Expected element name, got: " + in.getLocalName());
        }
        nextId = getAttribute(in, "nextID", 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        String lastTag = "";
        Wish wish;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final String tagName = in.getLocalName();
            final String oid = in.getAttributeValue(null, ID_ATTRIBUTE);
            wish = null;
            try {
System.err.println("AT " + oid + " / " + aiObjects.containsKey(oid));
                if (oid != null && aiObjects.containsKey(oid)) {
System.err.println("READFROMXML " + oid);
                    getAIObject(oid).readFromXML(in);
                } else if (tagName.equals(AIUnit.getXMLElementTagName())) {
                    new AIUnit(this, in);
                } else if (tagName.equals(AIPlayer.getXMLElementTagName())) {
                    Player p = getGame().getFreeColGameObject(oid, Player.class);
                    if (p != null) {
                        if (p.isIndian()) {
                            new NativeAIPlayer(this, in);
                        } else if (p.isREF()) {
                            new REFAIPlayer(this, in);
                        } else if (p.isEuropean()) {
                            new EuropeanAIPlayer(this, in);
                        } else {
                            logger.warning("Bogus AIPlayer: " + p);
                            in.nextTag();
                        }
                    }
                // @compat 0.10.1
                } else if (tagName.equals("colonialAIPlayer")) {
                    new EuropeanAIPlayer(this, in);
                // end compatibility code
                } else if (tagName.equals(AIColony.getXMLElementTagName())) {
                    new AIColony(this, in);
                } else if (tagName.equals(AIGoods.getXMLElementTagName())) {
                    new AIGoods(this, in);
                } else if (tagName.equals(WorkerWish.getXMLElementTagName())) {
                    wish = new WorkerWish(this, in);
                } else if (tagName.equals(GoodsWish.getXMLElementTagName())
                    // @compat 0.10.3
                    || tagName.equals("GoodsWish")
                    // end compatibility code
                    ) {
                    wish = new GoodsWish(this, in);
                } else if (tagName.equals(TileImprovementPlan.getXMLElementTagName())
                    // @compat 0.10.3
                    || tagName.equals("tileimprovementplan")
                    // end compatibility code
                    ) {
System.err.println("NEW " + oid);
                    new TileImprovementPlan(this, in);
                } else {
                    throw new IllegalStateException("Unknown AI-object read: "
                        + tagName + "(" + lastTag + ")");
                }
                if (wish != null) {
                    AIColony ac = wish.getDestinationAIColony();
                    if (ac != null) ac.addWish(wish);
                }
                lastTag = in.getLocalName();

            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception reading AIObject("
                    + tagName + ", " + oid + ")", e);
                while (!in.getLocalName().equals(tagName)
                    && !in.getLocalName().equals(getXMLElementTagName())) {
                    in.nextTag();
                }
                if (!in.getLocalName().equals(getXMLElementTagName())) {
                    in.nextTag();
                }
            }
        }

        if (!in.getLocalName().equals(getXMLElementTagName())) {
            logger.warning("Expected closing element name, got: "
                + in.getLocalName());
        }

        // TODO: This should not be necessary.  Try dropping it.
        findNewObjects(false);
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "aiMain"
     */
    public static String getXMLElementTagName() {
        return "aiMain";
    }
}
