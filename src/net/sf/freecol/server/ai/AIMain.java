/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectListener;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.RandomUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The main AI-class.
 * Keeps references to all other AI-classes.
 */
public class AIMain extends FreeColObject
    implements FreeColGameObjectListener {

    private static final Logger logger = Logger.getLogger(AIMain.class.getName());

    public static final String TAG = "aiMain";

    /** The server that this AI is operating within. */
    private final FreeColServer freeColServer;

    /** The next AI identifier index. */
    private int nextId = 1;

    /**
     * Contains mappings between {@code FreeColGameObject}s
     * and {@code AIObject}s.
     */
    private final Map<String, AIObject> aiObjects = new HashMap<>();


    /**
     * Creates a new {@code AIMain} and searches the current
     * game for {@code FreeColGameObject}s.
     *
     * @param freeColServer The main controller object for the server.
     */
    public AIMain(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }

    /**
     * Creates a new {@code AIMain} from a reader.
     *
     * @param freeColServer The main controller object for the
     *     server.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIMain(FreeColServer freeColServer,
                  FreeColXMLReader xr) throws XMLStreamException {
        this(freeColServer);

        readFromXML(xr);
    }


    /**
     * Gets the main controller object for the server.
     *
     * @return The {@code FreeColServer}-object.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Convenience accessor for the Game.
     *
     * @return The {@code Game} this AI is operating in.
     */
    public Game getGame() {
        return freeColServer.getGame();
    }

    /**
     * Gets a unique identifier for an {@code AIObject}.
     *
     * @return A unique identifier.
     */
    public String getNextId() {
        String id = Integer.toString(nextId);
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
        return randomInt(logger, logMe, freeColServer.getServerRandom(),
                         Integer.MAX_VALUE);
    }

    /**
     * Should a {@code FreeColGameObject} have a corresponding AI
     * object?
     *
     * Strictly true only for AI players and their units and colonies
     * (not (yet) for native settlements).  However object
     * initialization is not necessarily complete when we arrive here,
     * which means we can not yet use the Colony or Unit owner fields.
     * So the actual test implemented here is somewhat sloppy.
     *
     * @param fcgo The {@code FreeColGameObject} to test.
     * @return True if a corresponding AI object is needed.
     */
    private boolean shouldHaveAIObject(FreeColGameObject fcgo) {
        return (fcgo instanceof Colony) ? true
            : (fcgo instanceof Player)  ? ((Player)fcgo).isAI()
            : (fcgo instanceof Unit)    ? true
            : false;
    }

    /**
     * Searches for new {@link FreeColGameObject FreeColGameObjects}.
     * An AI-object is created for each new object.
     *
     * @param overwrite Determines wether any old {@code AIObject}
     *     should be overwritten or not.
     */
    public void findNewObjects(boolean overwrite) {
        for (FreeColGameObject fcgo : freeColServer.getGame()
                 .getFreeColGameObjectList()) {
            if (!shouldHaveAIObject(fcgo)) continue;
            if (overwrite || getAIObject(fcgo) == null) {
                setFreeColGameObject(fcgo.getId(), fcgo);
            }
        }
    }

    /**
     * Gets the {@code AIObject} for the given
     * {@code FreeColGameObject}.
     *
     * @param fcgo The {@code FreeColGameObject} to find the
     *     {@code AIObject} for.
     * @see #getAIObject(String)
     * @return The {@code AIObject}.
     */
    private AIObject getAIObject(FreeColGameObject fcgo) {
        return getAIObject(fcgo.getId());
    }

    /**
     * Gets the {@code AIObject} for a given object identifier.
     *
     * @param id The object identifier.
     * @see #getAIObject(FreeColGameObject)
     * @return The {@code AIObject}.
     */
    public AIObject getAIObject(String id) {
        synchronized (aiObjects) {
            return aiObjects.get(id);
        }
    }

    /**
     * Adds a reference to the given {@code AIObject}.
     *
     * Note: this is called only for new AI objects, thus it is an
     * error for the object to already be present in the aiObjects
     * map.
     *
     * @param id The object identifier.
     * @param aiObject The {@code AIObject} to store a reference
     *        for.
     */
    public void addAIObject(String id, AIObject aiObject) {
        if (aiObject == null) {
            throw new NullPointerException("aiObject == null: " + this);
        }
        boolean present;
        synchronized (aiObjects) {
            present = aiObjects.containsKey(id);
            if (!present) aiObjects.put(id, aiObject);
        }
        if (present) {
            throw new RuntimeException("AIObject already created: " + id);
        }
    }

    /**
     * Removes a reference to the given {@code AIObject}.
     *
     * @param id The object identifier.
     * @return True if an object for the identifier is removed.
     */
    public boolean removeAIObject(String id) {
        boolean result;
        synchronized (aiObjects) {
            result = aiObjects.remove(id) != null;
        }
        if (result) logger.finest("Removed AI object: " + id);
        return result;
    }

    /**
     * Get a copy of the list of all AI objects.
     *
     * @return A list of {@code AIObject}s.
     */
    private List<AIObject> getAIObjects() {
        synchronized (aiObjects) {
            return new ArrayList<>(aiObjects.values());
        }
    }

    /**
     * Gets the {@code AIObject} with the specified object
     * identifier and class.
     *
     * @param <T> The actual return type.
     * @param id The object identifier.
     * @param returnClass The expected class of the object.
     * @return The {@code AIObject} found, or null if not.
     */
    public <T extends AIObject> T getAIObject(String id, Class<T> returnClass) {
        AIObject aio = getAIObject(id);
        try {
            return returnClass.cast(aio);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Gets the AI colony corresponding to a given colony.
     *
     * @param colony The {@code Colony} to look up.
     * @return The corresponding AI colony, or null if not found.
     */
    public AIColony getAIColony(Colony colony) {
        return getAIObject(colony.getId(), AIColony.class);
    }

    /**
     * Gets the AI player corresponding to a given player.
     *
     * @param player The {@code Player} to look up.
     * @return The corresponding AI player, or null if not found.
     */
    public AIPlayer getAIPlayer(Player player) {
        return getAIObject(player.getId(), AIPlayer.class);
    }

    /**
     * Gets the AI unit corresponding to a given unit.
     *
     * @param unit The {@code Unit} to look up.
     * @return The corresponding AI unit, or null if not found.
     */
    public AIUnit getAIUnit(Unit unit) {
        return getAIObject(unit.getId(), AIUnit.class);
    }

    /**
     * Computes how many objects of each class have been created, to
     * track memory leaks over time
     *
     * @return A map of AI statistics.
     */
    public Map<String, String> getAIStatistics() {
        Map<String, Long> stats = new HashMap<>();
        for (AIObject aio : getAIObjects()) {
            String className = aio.getClass().getSimpleName();
            Long count = stats.get(className);
            if (count == null) {
                stats.put(className, 1L);
            } else {
                stats.put(className, count + 1L);
            }
        }
        return transform(stats.entrySet(), alwaysTrue(),
                         Function.<Entry<String,Long>>identity(),
                         Collectors.toMap(Entry::getKey,
                                          e -> Long.toString(e.getValue())));
    }

    /**
     * Check sort consistency.
     *
     * @return A string describing sort consistency failures, or null
     *     on success.
     */
    public String checkSortConsistency() {
        LogBuilder lb = new LogBuilder(256);
        lb.add("AI object sort inconsistencies");
        lb.mark();

        // Check sort consistency of the AI objects
        AIObject[] objects = getAIObjects().toArray(new AIObject[0]);
        for (int i0 = 0; i0 < objects.length; i0++) {
            for (int i1 = i0+1; i1 < objects.length; i1++) {
                int c0 = objects[i0].compareTo(objects[i1]);
                int c1 = objects[i1].compareTo(objects[i0]);
                if (c0 == 0 && c1 == 0) continue;
                if (c0 == -1 && c1 == 1) continue;
                if (c0 == 1 && c1 == -1) continue;
                lb.add("\n  ", objects[i0], " ", objects[i1],
                       " = ", c0, "/", c1);
            }
        }
        return (lb.grew()) ? lb.toString() : null;
    }
    
    /**
     * Check the AI integrity too.
     *
     * @param fix If true, fix problems if possible.
     * @param lb A {@code LogBuilder} to log to.
     * @return -1 if there are problems remaining, zero if problems
     *     were fixed, +1 if no problems found at all.
     */
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = IntegrityType.INTEGRITY_GOOD;
        for (AIObject aio : getAIObjects()) {
            IntegrityType integ = aio.checkIntegrity(fix, lb);
            if (!integ.safe()) {
                if (fix) {
                    lb.add("\n  Invalid AIObject dropped: ", aio.getId(),
                        "(", lastPart(aio.getClass().getName(), "."), ")");
                    removeAIObject(aio.getId());
                    aio.dispose();
                    result = result.fix();
                } else {
                    lb.add("\n  Invalid AIObject: ", aio.getId(),
                        "(", lastPart(aio.getClass().getName(), "."), ")");
                    result = result.fail();
                }
            }
        }

        for (FreeColGameObject fcgo : getGame().getFreeColGameObjectList()) {
            if (shouldHaveAIObject(fcgo)
                && getAIObject(fcgo.getId()) == null) {
                if (fix) {
                    lb.add("\n  Missing AIObject added: ", fcgo.getId());
                    setFreeColGameObject(fcgo.getId(), fcgo);
                    result = result.fix();
                } else {
                    lb.add("\n  Missing AIObject: ", fcgo.getId());
                    result = result.fail();
                }
            }
        }
        return result;
    }


    // Interface FreeColGameObjectListener

    /**
     * Creates a new {@code AIObject} for a given
     * {@code FreeColGameObject}. This method gets called
     * whenever a new object gets added to the {@link Game}.
     *
     * @param id The object identifier.
     * @param fcgo The {@code FreeColGameObject} to add.
     * @see AIObject
     * @see FreeColGameObject
     * @see FreeColGameObject#getId
     */
    @Override
    public void setFreeColGameObject(String id, FreeColGameObject fcgo) {
        if (getAIObject(id) != null || !shouldHaveAIObject(fcgo)) return;
        if (!id.equals(fcgo.getId())) {
            throw new RuntimeException("!id.equals(fcgo.getId()): " + id);
        }
        AIObject aio = null;
        if (fcgo instanceof Colony) {
            aio = new AIColony(this, (Colony)fcgo);
        } else if (fcgo instanceof Player) {
            Player player = (Player)fcgo;
            if (player.getPlayerType() == null) {
                // No point doing anything with the object yet, as we
                // need the player type before we can create the
                // right class of AI player.
                logger.info("Temporarily ignoring incomplete AI player: "
                    + fcgo.getId());
            } else if (player.isIndian()) {
                aio = new NativeAIPlayer(this, player);
            } else if (player.isREF()) {
                aio = new REFAIPlayer(this, player);
            } else if (player.isEuropean()) {
                aio = new EuropeanAIPlayer(this, player);
            } else {
                throw new RuntimeException("Bogus player: " + player);
            }
        } else if (fcgo instanceof Unit) {
            aio = new AIUnit(this, (Unit)fcgo);
        }
        if (aio == null) {
            throw new RuntimeException("Bogus non-AI FCGO: " + fcgo);
        }
    }

    /**
     * Removes the {@code AIObject} for a given AI identifier.
     * Needed for interface FreeColGameObjectListener.
     *
     * @param id The object identifier.
     */
    @Override
    public void removeFreeColGameObject(String id) {
        AIObject o = getAIObject(id);
        if (o != null) o.dispose();
        removeAIObject(id);
    }

    /**
     * Replaces the AI object when ownership changes.
     *
     * @param source The {@code FreeColGameObject} that has changed.
     * @param oldOwner The old owning {@code Player}.
     * @param newOwner The new owning {@code Player}.
     */
    @Override
    public void ownerChanged(FreeColGameObject source, Player oldOwner,
                             Player newOwner) {
        AIObject ao = getAIObject(source);
        if (ao == null) return;
        logger.finest("Owner changed for " + source.getId()
            + " with AI object: " + ao);

        AIPlayer oldAIOwner = getAIPlayer(oldOwner);
        if (oldAIOwner != null) oldAIOwner.removeAIObject(ao);
        ao.dispose();
        setFreeColGameObject(source.getId(), source);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Specification getSpecification() {
        return getGame().getSpecification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        AIMain o = copyInCast(other, AIMain.class);
        if (o == null || !super.copyIn(o)) return false;
        this.nextId = o.nextId;
        return true;
    }


    // Serialization

    private static final String NEXT_ID_TAG = "nextId";
    // @compat 0.11.3
    private static final String OLD_GOODS_WISH_TAG = "GoodsWish";
    private static final String OLD_TILE_IMPROVEMENT_PLAN_TAG = "tileimprovementplan";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // Does not have an identifier, so no need for
        // super.writeAttributes()

        xw.writeAttribute(NEXT_ID_TAG, nextId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (AIObject aio : sort(getAIObjects())) {
            if (!aio.checkIntegrity(false).safe()) {
                // We expect to see integrity failure when AIGoods are
                // aboard a unit that gets destroyed or if its
                // destination is destroyed, and probably more.  These
                // are hard to catch because AIGoods ids are not
                // linked to the Goods ids (Goods ids are just the
                // type ids) so we do not get notification of the
                // Goods being destroyed.
                aio.dispose();
                continue;
            }
            if (aio instanceof Wish) {
                if (!((Wish)aio).shouldBeStored()) continue;
            }

            try {
                if (aio.getId() == null) {
                    logger.warning("Null AI identifier for: "
                        + aio.getClass().getName());
                } else {
                    aio.toXML(xw);
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
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        nextId = xr.getAttribute(NEXT_ID_TAG, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        aiObjects.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();
        final String oid = xr.readId();

        try {
            // The AI data is quite shallow, so we can get away with
            // fixing up forward references just with this simple
            // lookup.  We complete initialization of such objects
            // with the call to setInitialized().
            AIObject aio = null;
            boolean indirect = false;
            if (oid != null && (aio = getAIObject(oid)) != null) {
                aio.readFromXML(xr);
                aio.setInitialized();

            } else if (AIColony.TAG.equals(tag)) {
                aio = new AIColony(this, xr);

            } else if (AIGoods.TAG.equals(tag)) {
                aio = new AIGoods(this, xr);

            } else if (AIPlayer.TAG.equals(tag)) {
                Player p = getGame().getFreeColGameObject(oid, Player.class);
                if (p != null) {
                    if (p.isIndian()) {
                        aio = new NativeAIPlayer(this, xr);
                    } else if (p.isREF()) {
                        aio = new REFAIPlayer(this, xr);
                    } else if (p.isEuropean()) {
                        aio = new EuropeanAIPlayer(this, xr);
                    } else {
                        throw new XMLStreamException("Bogus AIPlayer: " + p);
                    }
                }

            } else if (AIUnit.TAG.equals(tag)) {
                aio = new AIUnit(this, xr);

            } else if (GoodsWish.TAG.equals(tag)
                // @compat 0.11.3
                || OLD_GOODS_WISH_TAG.equals(tag)
                // end @compat 0.11.3
                       ) {
                aio = new GoodsWish(this, xr);

            } else if (TileImprovementPlan.TAG.equals(tag)
                // @compat 0.11.3
                || OLD_TILE_IMPROVEMENT_PLAN_TAG.equals(tag)
                // end @compat
                       ) {
                aio = new TileImprovementPlan(this, xr);

            } else if (WorkerWish.TAG.equals(tag)) {
                aio = new WorkerWish(this, xr);
            
            } else {
                System.err.println("AT " + tag + " with " + oid);
                indirect = true;
                super.readChild(xr);
            }
            if (aio == null && !indirect) {
                throw new XMLStreamException("Bad AI object at " + tag
                    + " with " + oid);
            }
        } catch (XMLStreamException xse) { // Expected
            throw xse;
        } catch (Exception e) {
            // Nothing above apparently tries to throw an exception
            // that can end up here, but that keeps changing and
            // this is a great place to resynchronize the reader
            // so keep this in place.
            logger.log(Level.WARNING, "Exception reading AIObject: "
                       + tag + ", id=" + oid, e);
            final String mainTag = TAG;
            while (xr.moreTags() || !(xr.atTag(tag) || xr.atTag(mainTag)));
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
