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

package net.sf.freecol.common.model;

import java.lang.ref.WeakReference;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The main component of the game model.
 */
public class Game extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(Game.class.getName());

    public static final String TAG = "game";

    /** Reasons for logging out. */
    public static enum LogoutReason {
        DEFEATED,   // Game over, player lost
        LOGIN,      // Logging in again FIXME: this should go away
        LOGOUT,     // Player quits but game continues
        MAIN_TITLE, // Complete reset back to the MainPanel
        NEW_GAME,   // Part reset back to the NewPanel
        QUIT,       // Player quits and whole game is over
        RECONNECT,  // Implement reconnect
    };
    
    /** State for the FCGO iterator, out here because it has to be static. */
    private static enum FcgoState {
        INVALID,
        VALID,
        CONSUMED,
    };

    /** Map of all classes with corresponding server classes. */
    private static final java.util.Map<Class<? extends FreeColObject>,
                                       Class<? extends FreeColObject>>
        serverClasses = new HashMap<>();
    static {
        serverClasses.put(net.sf.freecol.common.model.Building.class,
                          net.sf.freecol.server.model.ServerBuilding.class);
        serverClasses.put(net.sf.freecol.common.model.Colony.class,
                          net.sf.freecol.server.model.ServerColony.class);
        serverClasses.put(net.sf.freecol.common.model.ColonyTile.class,
                          net.sf.freecol.server.model.ServerColonyTile.class);
        serverClasses.put(net.sf.freecol.common.model.Europe.class,
                          net.sf.freecol.server.model.ServerEurope.class);
        serverClasses.put(net.sf.freecol.common.model.Game.class,
                          net.sf.freecol.server.model.ServerGame.class);
        serverClasses.put(net.sf.freecol.common.model.IndianSettlement.class,
                          net.sf.freecol.server.model.ServerIndianSettlement.class);
        serverClasses.put(net.sf.freecol.common.model.Region.class,
                          net.sf.freecol.server.model.ServerRegion.class);
        serverClasses.put(net.sf.freecol.common.model.Player.class,
                          net.sf.freecol.server.model.ServerPlayer.class);
        serverClasses.put(net.sf.freecol.common.model.Unit.class,
                          net.sf.freecol.server.model.ServerUnit.class);
    };

    /**
     * Map of class name to class for the location classes, to speed
     * up game loading.
     */
    private static final java.util.Map<String, Class<? extends FreeColGameObject>>
        locationClasses = new HashMap<>();
    static {
        locationClasses.put("Building",
                            net.sf.freecol.common.model.Building.class);
        locationClasses.put("Colony",
                            net.sf.freecol.common.model.Colony.class);
        locationClasses.put("ColonyTile",
                            net.sf.freecol.common.model.ColonyTile.class);
        locationClasses.put("Europe",
                            net.sf.freecol.common.model.Europe.class);
        locationClasses.put("HighSeas",
                            net.sf.freecol.common.model.HighSeas.class);
        locationClasses.put("IndianSettlement",
                            net.sf.freecol.common.model.IndianSettlement.class);
        locationClasses.put("Map",
                            net.sf.freecol.common.model.Map.class);
        locationClasses.put("Tile",
                            net.sf.freecol.common.model.Tile.class);
        locationClasses.put("Unit",
                            net.sf.freecol.common.model.Unit.class);
    };


    /** The Specification this game uses. */
    private Specification specification = null;

    /**
     * The next available identifier that can be given to a new
     * {@code FreeColGameObject}.
     */
    protected int nextId = 1;

    /** Game UUID, persistent in savegame files */
    private UUID uuid = UUID.randomUUID();

    /** The client player name, null in the server. */
    private String clientUserName;

    /** All the players in the game. */
    protected final List<Player> players = new ArrayList<>();

    /** A virtual player to use for enemy privateers. */
    private Player unknownEnemy = null;

    /** The map of the New World. */
    protected Map map = null;

    /**
     * The current nation options.  Mainly used to see if a player
     * nation is available.
     */
    private NationOptions nationOptions = null;

    /** The player whose turn it is. */
    protected Player currentPlayer = null;

    /** The current turn. */
    private Turn turn = new Turn(1);

    /** Whether the War of Spanish Succession has already taken place. */
    private boolean spanishSuccession = false;

    /** Initial active unit identifier. */
    private String initialActiveUnitId = null;
    
    // Serialization not required below.

    /**
     * References to all objects created in this game.
     * Serialization is not needed directly as these must be completely
     * within { players, unknownEnemy, map } which are directly serialized.
     */
    protected final HashMap<String, WeakReference<FreeColGameObject>>
        freeColGameObjects;

    /**
     * The combat model this game uses. At the moment, the only combat
     * model available is the SimpleCombatModel, which strives to
     * implement the combat model of the original game.  However, it is
     * anticipated that other, more complex combat models will be
     * implemented in future.  As soon as that happens, we will also
     * have to make the combat model selectable.
     */
    protected CombatModel combatModel = null;

    /** The number of removed FCGOs that should trigger a cache clean. */
    private static final int REMOVE_GC_THRESHOLD = 64;

    /** The number of FCGOs removed since last cache clean. */
    private int removeCount = 0;

    /**
     * A FreeColGameObjectListener to watch the objects in the game.
     * Usually this is the AIMain instance.
     * FIXME: is this better done with a property change listener?
     */
    protected FreeColGameObjectListener freeColGameObjectListener = null;


    /**
     * Root constructor for games.
     *
     * Game.newInstance uses this so it must be public.
     */
    public Game() {
        super(); // Use the special FCGO Game-specific constructor

        // freeColGameObjects has to be in place before we can
        // call internId()
        this.freeColGameObjects = new HashMap<>(10000);
        internId("0"); // Games are always id 0

        this.clientUserName = null;
        this.players.clear();
        this.unknownEnemy = null;
        this.map = null;
        this.nationOptions = null;
        this.currentPlayer = null;
        this.spanishSuccession = false;
        this.initialActiveUnitId = null;
        this.specification = null;
        this.combatModel = new SimpleCombatModel();
        this.removeCount = 0;

        this.initialized = true; // Explicit initialization needed for Games
    }

    /**
     * Constructor used by the ServerGame constructor.
     *
     * @param specification The {@code Specification} for this game.
     */
    protected Game(Specification specification) {
        this();

        setSpecification(specification);
    }

    /**
     * Creates a new {@code Game} instance from a stream.
     *
     * @param game A base {@code Game} (not used here, this is a very
     *     special case).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if an error occurs
     */
    public Game(@SuppressWarnings("unused")Game game,
                FreeColXMLReader xr) throws XMLStreamException {
        this();
        
        readFromXML(xr);
    }


    /**
     * Special update handler for the pre-game update.
     *
     * When starting a new game the server sends an update that
     * contains the game map.  This must be synchronized so that the
     * client does not race ahead and launch into the game before the
     * update completes.
     *
     * We used to check integrity here, but 1) the server has already
     * done that and 2) the unexplored Tiles have no type which makes
     * a lot of integrity failures.
     *
     * @param game The update for this {@code Game}.
     * @return True if the update succeeds.
     */
    public synchronized boolean preGameUpdate(Game game) {
        return copyIn(game);
    }

    /**
     * Instantiate an uninitialized FreeColGameObject within a game.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to instantiate in.
     * @param returnClass The required {@code FreeColObject} class.
     * @param server Create a server object if possible.
     * @return The new uninitialized object, or null on error.
     */
    public static <T extends FreeColObject> T newInstance(Game game,
                                                          Class<T> returnClass,
                                                          boolean server) {
        // Do not restrict trying the full (Game,String) constructor
        // to just server objects as there are simpler FCOs that
        // implement it (e.g. Goods).
        if (server) {
            @SuppressWarnings("unchecked")
            Class<T> sc = (Class<T>)serverClasses.get(returnClass);
            if (sc != null) returnClass = sc;
        }
        try {
            return Introspector.instantiate(returnClass,
                new Class[] { Game.class, String.class },
                new Object[] { game, (String)null }); // No intern!
        } catch (Introspector.IntrospectorException ex) {
            ; // Allow another try on failure
        }

        if (game != null
            && FreeColSpecObject.class.isAssignableFrom(returnClass)) {
            try {
                return Introspector.instantiate(returnClass,
                    new Class[] { Specification.class },
                    new Object[] { game.getSpecification() });
            } catch (Introspector.IntrospectorException ex) {
                logger.log(Level.WARNING, "newInstance(spec) fail for: "
                    + returnClass.getName(), ex);
            }
        } else { // Or just use the trivial constructor
            try {
                return Introspector.instantiate(returnClass,
                    new Class[] {}, new Object[] {});
            } catch (Introspector.IntrospectorException ex) {
                logger.log(Level.WARNING, "newInstance(trivial) fail for: "
                    + returnClass.getName(), ex);
            }
        }
        return null;
    }

    /**
     * Get the difficulty level of this game.
     *
     * @return An {@code OptionGroup} containing the difficulty settings.
     */
    public final OptionGroup getDifficultyOptionGroup() {
        return specification.getDifficultyOptionGroup();
    }

    /**
     * Gets the game options associated with this game.
     *
     * @return An {@code OptionGroup} containing the game options.
     */
    public OptionGroup getGameOptions() {
        return specification.getGameOptions();
    }

    /**
     * Sets the game options associated with this game.
     *
     * @param go An {@code OptionGroup} containing the game options.
     */
    public void setGameOptions(OptionGroup go) {
        specification.setGameOptions(go);
    }

    /**
     * Gets the map generator options associated with this game.
     *
     * @return An {@code OptionGroup} containing the map
     *     generator options.
     */
    public OptionGroup getMapGeneratorOptions() {
        return specification.getMapGeneratorOptions();
    }

    /**
     * Sets the map generator options associated with this game.
     *
     * @param mgo An {@code OptionGroup} containing the map
     *     generator options.
     */
    public void setMapGeneratorOptions(OptionGroup mgo) {
        specification.setMapGeneratorOptions(mgo);
    }

    /**
     * Stub for routine only meaningful in the server.
     *
     * @return Nothing.
     */
    public int getNextId() {
        throw new RuntimeException("game.getNextId not implemented: " + this);
    }

    /**
     * Gets the {@code FreeColGameObject} with the given identifier.
     *
     * @param id The object identifier.
     * @return The game object, or null if not found.
     */
    public FreeColGameObject getFreeColGameObject(String id) {
        if (id == null || id.isEmpty()) return null;
        final WeakReference<FreeColGameObject> ro;
        synchronized (this.freeColGameObjects) {
            ro = freeColGameObjects.get(id);
        }
        if (ro == null) return null;
        final FreeColGameObject o = ro.get();
        if (o == null) {
            removeFreeColGameObject(id, "missed");
            return null;
        }
        return o;
    }

    /**
     * Gets the {@code FreeColGameObject} with the specified
     * identifier and class.
     *
     * @param <T> The actual return type.
     * @param id The object identifier.
     * @param returnClass The expected class of the object.
     * @return The game object, or null if not found.
     */
    public <T extends FreeColGameObject> T getFreeColGameObject(String id,
        Class<T> returnClass) {
        FreeColGameObject fcgo = getFreeColGameObject(id);
        try {
            return returnClass.cast(fcgo);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Set the mapping between object identifier and object.
     *
     * @param id The object identifier.
     * @param fcgo The {@code FreeColGameObject} to add to this {@code Game}.
     */
    public void setFreeColGameObject(String id, FreeColGameObject fcgo) {
        if (id == null || id.isEmpty()) {
            throw new RuntimeException("Null/empty identifier: " + this);
        } else  if (fcgo == null) {
            throw new RuntimeException("Null FreeColGameObject: " + id);
        }

        final WeakReference<FreeColGameObject> wr = new WeakReference<>(fcgo);
        synchronized (this.freeColGameObjects) {
            this.freeColGameObjects.put(id, wr);
        }
    }
        
    /**
     * Add a new {@code FreeColGameObject} with a given identifier.
     *
     * @param id The object identifier.
     * @param fcgo The {@code FreeColGameObject} to add to this {@code Game}.
     */
    public void addFreeColGameObject(String id, FreeColGameObject fcgo) {
        if (id == null || id.isEmpty()) {
            throw new RuntimeException("Null/empty identifier: " + this);
        } else if (fcgo == null) {
            throw new RuntimeException("Null FreeColGameObject: " + id);
        }

        final FreeColGameObject old = getFreeColGameObject(id);
        if (old != null) {
            throw new RuntimeException("Tried to replace FCGO "
                + id + " : " + old.getClass()
                + " with " + fcgo.getId() + " : " + fcgo.getClass());
        }

        setFreeColGameObject(id, fcgo);
        notifySetFreeColGameObject(id, fcgo);
    }

    /**
     * Removes the {@code FreeColGameObject} with the specified
     * identifier.
     *
     * @param id The object identifier.
     * @param reason A reason to remove the object.
     * @exception IllegalArgumentException If the identifier is null or empty.
     */
    public void removeFreeColGameObject(String id, String reason) {
        if (id == null || id.isEmpty()) {
            throw new RuntimeException("Null/empty identifier: " + this);
        }

        logger.finest("removeFCGO/" + reason + ": " + id);
        notifyRemoveFreeColGameObject(id);
        synchronized (this.freeColGameObjects) {
            this.freeColGameObjects.remove(id);
        }

        // Garbage collect the FCGOs if enough have been removed.
        if (++removeCount > REMOVE_GC_THRESHOLD) {
            synchronized (this.freeColGameObjects) {
                Iterator<FreeColGameObject> iter = getFreeColGameObjectIterator();
                while (iter.hasNext()) iter.next();
            }
            removeCount = 0;
        }
    }

    /**
     * Update a {@code FreeColGameObject} from another.
     *
     * @param <T> The type of object to update.
     * @param other The other object.
     * @param create If true, create the object if it is missing.
     * @return The resulting object after update.
     */
    public <T extends FreeColGameObject> T update(T other, boolean create) {
        return (other == null) ? null
            : update(other, other.getFreeColObjectClass(), create);
    }

    /**
     * Update a {@code FreeColGameObject} from another, optionally allowing
     * missing objects to be created.
     *
     * @param <T> The type of object to update.
     * @param other The other object.
     * @param returnClass The expected class of the object.
     * @param create If true, create missing objects.
     * @return The resulting object after update.
     */
    private <T extends FreeColGameObject> T update(T other,
                                                   Class<T> returnClass,
                                                   boolean create) {
        if (other == null) return null;
        final String id = other.getId();
        FreeColGameObject fcgo = getFreeColGameObject(id);
        if (fcgo == null) {
            if (create) {
                fcgo = newInstance(this, returnClass, false);
                fcgo.setId(id);
            } else {
                // Otherwise this is an error
                logger.warning("Update of missing object: " + id
                    + "\n" + net.sf.freecol.common.debug.FreeColDebugger.stackTraceToString());
                return null;
            }
        }
        T t;
        try {
            t = returnClass.cast(fcgo);
        } catch (ClassCastException cce) {
            // "Can not happen"
            throw new RuntimeException("Update class clash: " + fcgo.getClass()
                + " / " + returnClass, cce);
        }
        if (!t.copyIn(other)) {
            // "Can not happen"
            throw new RuntimeException("Update copy failed: " + id
                + " onto " + t);
        }
        if (create) t.internId(id);
        return t;
    }

    /**
     * Convenience wrapper to update several {@code FreeColGameObject}s.
     *
     * @param <T> The type of object to update.
     * @param other The collection of objects to update.
     * @param create If true, create missing objects.
     * @return The resulting list of updated objects.
     */
    public <T extends FreeColGameObject> List<T> update(Collection<T> other,
                                                        boolean create) {
        if (other == null) return null;
        List<T> ret = new ArrayList<>();
        for (T t : other) {
            T nt = update(t, create);
            if (nt != null) ret.add(nt);
        }
        return ret;
    }

    /**
     * Update a {@code FreeColGameObject} from a reference to it in an update.
     *
     * @param <T> The type of object to update.
     * @param other The other object.
     * @return The resulting object after update.
     */
    public <T extends FreeColGameObject> T updateRef(T other) {
        return (other == null) ? null
            : updateRef(other, other.getFreeColObjectClass());
    }

    /**
     * Update a {@code FreeColGameObject} from a reference to it in an update.
     *
     * @param <T> The type of object to update.
     * @param other The other object.
     * @param returnClass The expected class of the object.
     * @return The resulting object after update.
     */
    private <T extends FreeColGameObject> T updateRef(T other,
                                                      Class<T> returnClass) {
        if (other == null) return null;
        final String id = other.getId();
        return getFreeColGameObject(id, returnClass);
    }        

    /**
     * Update several {@code FreeColGameObject}s from a list of
     * references to it in an update.
     *
     * @param <T> The type of object to update.
     * @param other The other object.
     * @return The resulting object after update.
     */
    public <T extends FreeColGameObject> List<T> updateRef(Collection<T> other) {
        if (other == null) return null;
        List<T> ret = new ArrayList<>();
        for (T t : other) {
            T nt = updateRef(t);
            if (nt != null) ret.add(nt);
        }
        return ret;
    }        

    /**
     * Update a {@code Location} from a reference to it in an update.
     *
     * @param loc The {@code Location}.
     * @return The resulting location after update.
     */
    public Location updateLocationRef(Location loc) {
        return (loc == null) ? null : findFreeColLocation(loc.getId());
    }        

    /**
     * Convenience wrapper to find a location (which is an interface,
     * precluding using the typed version of getFreeColGameObject())
     * by identifier.
     *
     * Use this routine when the object should already be present in the game.
     *
     * @param id The object identifier.
     * @return The {@code Location} if any.
     */
    public Location findFreeColLocation(String id) {
        FreeColGameObject fcgo = getFreeColGameObject(id);
        return (fcgo instanceof Location) ? (Location)fcgo : null;
    }

    /**
     * Gets an {@code Iterator} over every registered
     * {@code FreeColGameObject}.
     *
     * This {@code Iterator} should be iterated once in a while
     * since it cleans the {@code FreeColGameObject} cache.  Very
     * few routines call this any more, so there is a thresholded call
     * in removeFreeColGameObject to ensure the cache is still
     * cleaned.  Reconsider this if the situation changes.
     *
     * Lock freeColGameObjects when using this.
     *
     * @return An {@code Iterator} containing every registered
     *     {@code FreeColGameObject}.
     */
    private Iterator<FreeColGameObject> getFreeColGameObjectIterator() {
        return new Iterator<FreeColGameObject>() {

            /** An iterator over the freeColGameObjects map. */
            private final Iterator<Entry<String,
                                         WeakReference<FreeColGameObject>>> it
                = freeColGameObjects.entrySet().iterator();

            /** Read ahead to this next entry. */
            private Entry<String, WeakReference<FreeColGameObject>> readAhead
                = null;

            /** State of the readahead value. */
            private FcgoState fcgoState = FcgoState.INVALID;


            @Override
            public boolean hasNext() {
                if (this.fcgoState == FcgoState.VALID) return true;
                while (this.it.hasNext()) {
                    this.readAhead = this.it.next();
                    if (this.readAhead.getValue().get() != null) {
                        this.fcgoState = FcgoState.VALID;
                        return true;
                    }
                    this.fcgoState = FcgoState.CONSUMED;
                    remove();
                }
                return false;
            }

            @Override
            public FreeColGameObject next() {
                if (!hasNext()) throw new NoSuchElementException();
                FreeColGameObject fcgo = this.readAhead.getValue().get();
                this.fcgoState = FcgoState.CONSUMED;
                return fcgo;
            }

            @Override
            public void remove() {
                if (this.fcgoState == FcgoState.INVALID) {
                    throw new RuntimeException("No current entry: "
                        + this.fcgoState);
                }
                final String key = this.readAhead.getKey();
                this.fcgoState = FcgoState.INVALID;
                this.it.remove();
                logger.finest("removeFCGO/expire: " + key);
                notifyRemoveFreeColGameObject(key);
            }
        };
    }

    /**
     * Get a list of all the {@code FreeColGameObjects}.
     *
     * @return A suitable list.
     */
    public List<FreeColGameObject> getFreeColGameObjectList() {
        List<FreeColGameObject> ret = new ArrayList<>();
        synchronized (this.freeColGameObjects) {
            Iterator<FreeColGameObject> iter = getFreeColGameObjectIterator();
            while (iter.hasNext()) ret.add(iter.next());
        }
        return ret;
    }

    /**
     * Gets the unique identifier for this game. 
     * A game UUID persists in save game files.
     *
     * @return The game {@code UUID}.
     */
    public UUID getUUID () {
       return this.uuid;
    }

    /**
     * Get players in the game.
     *
     * @param predicate A {@code Predicate} to select suitable players with.
     * @return A list of {@code Player}s.
     */
    protected List<Player> getPlayerList(Predicate<? super Player> predicate) {
        synchronized (this.players) {
            return transform(this.players, predicate);
        }
    }

    /**
     * Get players in the game.
     *
     * @param predicate A {@code Predicate} to select suitable players with.
     * @return The stream of {@code Player}s.
     */
    public Stream<Player> getPlayers(Predicate<? super Player> predicate) {
        synchronized (this.players) {
            return getPlayerList(predicate).stream();
        }
    }

    /**
     * Get a particular player in the game by a predicate.
     *
     * @param predicate A {@code Predicate} to select suitable players with.
     * @return The {@code Player} found or null if not present.
     */
    public Player getPlayer(Predicate<? super Player> predicate) {
        synchronized (this.players) {
            return find(this.players, predicate);
        }
    }

    /**
     * Set the players in the game.
     *
     * @param players The new {@code Player}s to add.
     */
    private void setPlayers(List<Player> players) {
        synchronized (this.players) {
            this.players.clear();
            if (players != null) this.players.addAll(players);
        }
    }

    /**
     * Gets the live player after the given player.
     *
     * @param beforePlayer The {@code Player} before the
     *     {@code Player} to be returned.
     * @return The {@code Player} after the {@code beforePlayer}
     *     in the list which determines the order each player becomes the
     *     current player.
     * @see #getNextPlayer
     */
    private Player getPlayerAfter(Player beforePlayer) {
        synchronized (this.players) {
            if (this.players.isEmpty()) return null;

            final int start = this.players.indexOf(beforePlayer);
            int index = start;
            do {
                if (++index >= this.players.size()) index = 0;
                Player player = this.players.get(index);
                if (!player.isUnknownEnemy()
                    && !player.isDead()) return player;
            } while (index != start);
        }
        return null;
    }

    /**
     * Adds the specified player to the game.
     *
     * @param player The {@code Player} to add.
     */
    public void addPlayer(Player player) {
        synchronized (this.players) {
            if (!this.players.contains(player)) this.players.add(player);
        }
        Nation nation = getSpecification().getNation(player.getNationId());
        nationOptions.getNations().put(nation, NationState.NOT_AVAILABLE);
    }

    /**
     * Removes the specified player from the game.
     *
     * We do not really remove the full FCGO from the game, just from the
     * active players list.
     *
     * @param player The {@code Player} to remove.
     * @return True if the player was removed.
     */
    public boolean removePlayer(Player player) {
        Player newCurrent = (currentPlayer != player) ? null
            : getPlayerAfter(currentPlayer);

        synchronized (this.players) {
            if (!this.players.remove(player)) return false;
        }

        Nation nation = getSpecification().getNation(player.getNationId());
        nationOptions.getNations().put(nation, NationState.AVAILABLE);
        player.dispose();

        if (newCurrent != null) currentPlayer = newCurrent;
        return true;
    }

    /**
     * Get the first player in this game.
     *
     * @return The first player, or null if none present.
     */
    public Player getFirstPlayer() {
        synchronized (this.players) {
            return first(this.players);
        }
    }

    /**
     * Sort the players list.
     *
     * @param comparator The {@code Comparator} to sort with.
     */
    public void sortPlayers(Comparator<Player> comparator) {
        synchronized (this.players) {
            this.players.sort(comparator);
        }
    }


    /**
     * Gets a player specified by a name.
     *
     * @param name The name identifying the {@code Player}.
     * @return The {@code Player} or null if none found.
     */
    public Player getPlayerByName(String name) {
        return getPlayer(matchKeyEquals(name, Player::getName));
    }

    /**
     * Get a {@code Player} identified by its nation.
     *
     * @param nation The {@code Nation} to search for.
     * @return The {@code Player} of the given nation, or null if
     *     not found.
     */
    public Player getPlayerByNation(Nation nation) {
        return getPlayerByNationId(nation.getId());
    }

    /**
     * Get a {@code Player} identified by its nation identifier.
     *
     * @param nationId The nation identifier to search for.
     * @return The {@code Player} of the given nation, or null if
     *     not found.
     */
    public Player getPlayerByNationId(String nationId) {
        return getPlayer(matchKeyEquals(nationId, Player::getNationId));
    }

    /**
     * Get a list of the live players in the game, optionally
     * excluding supplied ones.
     *
     * @param exclude The {@code Player}s to exclude.
     * @return A list of live {@code Player}s, without the excluded ones.
     */
    public List<Player> getLivePlayerList(final Player... exclude) {
        final Predicate<Player> livePred = p ->
            !p.isUnknownEnemy() && !p.isDead() && !any(exclude, matchKey(p));
        return getPlayerList(livePred);
    }

    /**
     * Get a stream of the live players in the game, optionally excluding
     * supplied ones.
     *
     * @param exclude The {@code Player}s to exclude.
     * @return A stream of live {@code Player}s, without the
     *     excluded ones.
     */
    public Stream<Player> getLivePlayers(final Player... exclude) {
        return getLivePlayerList(exclude).stream();
    }

    /**
     * Get a list of the live European players in this game.
     *
     * @param exclude {@code Player}s to exclude.
     * @return A list of live European {@code Player}s in this game,
     *     without the excluded ones.
     */
    public List<Player> getLiveEuropeanPlayerList(final Player... exclude) {
        final Predicate<Player> europeanPred = p ->
            !p.isUnknownEnemy() && !p.isDead() && p.isEuropean()
                && !any(exclude, matchKey(p));
        return getPlayerList(europeanPred);
    }

    /**
     * Get a stream of the live European players in this game.
     *
     * @param exclude {@code Player}s to exclude.
     * @return A stream of live European {@code Player}s in this game,
     *     without the excluded ones.
     */
    public Stream<Player> getLiveEuropeanPlayers(final Player... exclude) {
        return getLiveEuropeanPlayerList(exclude).stream();
    }

    /**
     * Get a list of the live native players in this game.
     *
     * @param exclude {@code Player}s to exclude.
     * @return A list of live native {@code Player}s in this game.
     */
    public List<Player> getLiveNativePlayerList(final Player... exclude) {
        final Predicate<Player> nativePred = p ->
            !p.isUnknownEnemy() && !p.isDead() && p.isIndian()
                && !any(exclude, matchKey(p));
        return getPlayerList(nativePred);
    }

    /**
     * Get a stream of the live native players in this game.
     *
     * @param exclude {@code Player}s to exclude.
     * @return A stream of live native {@code Player}s in this game.
     */
    public Stream<Player> getLiveNativePlayers(final Player... exclude) {
        return getLiveNativePlayerList(exclude).stream();
    }
    
    /**
     * Gets the next current player.
     *
     * @return The {@code Player} whose turn follows the current player.
     */
    public Player getNextPlayer() {
        return getPlayerAfter(currentPlayer);
    }

    /**
     * Add a list of players to this game.
     *
     * Called from the pre and in-game controllers when players change.
     * ATM we never remove players, even dead ones.  Must tolerate player
     * being both present and not present.
     *
     * @param players The list of {@code players} to add.
     * @return The players that were successfully copied in.
     */
    public List<Player> addPlayers(List<Player> players) {
        List<Player> valid = new ArrayList<>();
        for (Player p : players) {
            FreeColGameObject fcgo = getFreeColGameObject(p.getId());
            if (fcgo == null) {
                if ((fcgo = update(p, Player.class, true)) != null) {
                    addPlayer((Player)fcgo);
                } else {
                    logger.warning("addPlayers create new fail: " + p);
                }                
            } else if (fcgo instanceof Player) {
                if (fcgo.copyIn(p)) {
                    valid.add(p);
                } else {
                    logger.warning("addPlayers copyIn existing fail: " + p);
                }
            } else {
                logger.warning("addPlayers onto non-player: " + fcgo);
            }
        }
        return valid;
    }

    /**
     * Gets the unknown enemy player, which is used for privateers.
     *
     * @return The unknown enemy {@code Player}.
     */
    public Player getUnknownEnemy() {
        return this.unknownEnemy;
    }

    /**
     * Sets the unknown enemy player.
     *
     * @param player The {@code Player} to serve as the unknown enemy.
     */
    public void setUnknownEnemy(Player player) {
        this.unknownEnemy = player;
    }

    /**
     * Get the client user name.
     *
     * @return The client user name.
     */
    public String getClientUserName() {
        return this.clientUserName;
    }

    /**
     * Are we executing in a client?
     *
     * @return True in a client.
     */
    public boolean isInClient() {
        return this.clientUserName != null;
    }

    /**
     * Are we executing in the server?
     *
     * @return True in the server.
     */
    public boolean isInServer() {
        return this.clientUserName == null;
    }

    /**
     * Get the client player this thread is operating for.  If in the server
     * there will be none.
     *
     * @return The client {@code Player}.
     */
    public Player getClientPlayer() {
        return (this.clientUserName == null) ? null
            : getPlayerByName(this.clientUserName);
    }

    /**
     * Is this game in revenge mode?
     *
     * @return True if an undead player is present.
     */
    public boolean isInRevengeMode() {
        return getPlayer(Player::isUndead) != null;
    }

    /**
     * Gets the current player.
     *
     * @return The current player.
     */
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    /**
     * Sets the current player.
     *
     * @param newCurrentPlayer The new current {@code Player}.
     */
    public void setCurrentPlayer(Player newCurrentPlayer) {
        this.currentPlayer = newCurrentPlayer;
    }

    /**
     * Gets the map that is being used in this game.
     *
     * @return The game {@code Map}.
     */
    public synchronized Map getMap() {
        return this.map;
    }

    /**
     * Sets the game map.
     *
     * @param newMap The new {@code Map} to use.
     * @return The old {@code Map}.
     */
    public synchronized Map setMap(Map newMap) {
        Map oldMap = this.map;
        this.map = newMap;
        return oldMap;
    }

    /**
     * Change the map in this game, fixing player destinations.
     *
     * @param newMap The new {@code Map} to use.
     */
    public void changeMap(Map newMap) {
        Map oldMap = setMap(newMap);
        if (newMap != oldMap) {
            for (HighSeas hs : transform(getLivePlayers(), alwaysTrue(),
                                         Player::getHighSeas, toListNoNulls())) {
                hs.removeDestination(oldMap);

                hs.addDestination(newMap);
            }
        }
    }

    /**
     * Get the current nation options.
     *
     * @return The current {@code NationOptions}.
     */
    public final synchronized NationOptions getNationOptions() {
        return nationOptions;
    }

    /**
     * Set the current nation options.
     *
     * Public for the test suite.
     *
     * @param newNationOptions The new {@code NationOptions} value.
     */
    public final synchronized void setNationOptions(final NationOptions newNationOptions) {
        this.nationOptions = newNationOptions;
    }

    /**
     * Find an available (i.e. vacant) nation.
     *
     * @return A vacant {@code Nation} or null if none found.
     */
    public Nation getVacantNation() {
        Entry<Nation, NationState> entry
            = find(nationOptions.getNations().entrySet(),
                   matchKey(NationState.AVAILABLE, Entry::getValue));
        return (entry == null) ? null : entry.getKey();
    }

    /**
     * Get the currently available nations.
     *
     * @return A list of available {@code Nation}s.
     */
    public final List<Nation> getVacantNations() {
        return transform(nationOptions.getNations().entrySet(),
                         matchKey(NationState.AVAILABLE, Entry::getValue),
                         Entry::getKey);
    }

    /**
     * Can a new player be added to this game?
     *
     * @return True if a new player can be added.
     */
    public boolean canAddNewPlayer() {
        return getVacantNation() != null;
    }

    /**
     * Gets the current turn in this game.
     *
     * @return The current {@code Turn}.
     */
    public Turn getTurn() {
        return turn;
    }

    /**
     * Sets the current turn in this game.
     *
     * @param newTurn The new {@code Turn} to set.
     */
    public void setTurn(Turn newTurn) {
        turn = newTurn;
    }

    /**
     * Get the age for the current turn.
     *
     * @return The age (0-2).
     */
    public int getAge() {
        return getSpecification().getAge(turn);
    }
    
    /**
     * Get the combat model in this game.
     *
     * @return The {@code CombatModel}.
     */
    public final CombatModel getCombatModel() {
        return combatModel;
    }

    /**
     * Set the game combat model.
     *
     * @param newCombatModel The new {@code CombatModel} value.
     */
    public final void setCombatModel(final CombatModel newCombatModel) {
        this.combatModel = newCombatModel;
    }

    /**
     * Has the Spanish Succession event occured?
     *
     * @return True if the Spanish Succession has occurred.
     */
    public final boolean getSpanishSuccession() {
        return spanishSuccession;
    }

    /**
     * Set the Spanish Succession value.
     *
     * @param spanishSuccession The new Spanish Succession value.
     */
    public final void setSpanishSuccession(final boolean spanishSuccession) {
        this.spanishSuccession = spanishSuccession;
    }

    /**
     * Get the identifier for the initial active unit.
     *
     * @return The active unit identifier, if any.
     */
    public String getInitialActiveUnitId() {
        return this.initialActiveUnitId;
    }

    /**
     * Get the initial active unit.
     *
     * @return The initial active {@code Unit} or null if none.
     */
    public Unit getInitialActiveUnit() {
        return (this.initialActiveUnitId == null) ? null
            : getFreeColGameObject(this.initialActiveUnitId, Unit.class);
    }

    /**
     * Set the identifier for the initial active unit.
     *
     * @param initialActiveUnitId The identifier for the current active unit.
     */
    public void setInitialActiveUnitId(String initialActiveUnitId) {
        this.initialActiveUnitId = initialActiveUnitId;
    }
        
    /**
     * Sets the {@code FreeColGameObjectListener} attached to this game.
     *
     * @param fcgol The new {@code FreeColGameObjectListener}.
     */
    public void setFreeColGameObjectListener(FreeColGameObjectListener fcgol) {
        freeColGameObjectListener = fcgol;
    }

    /**
     * Notify a listener (if any) of a new game object.
     *
     * @param id The object identifier.
     * @param fcgo The new {@code FreeColGameObject}.
     */
    public void notifySetFreeColGameObject(String id, FreeColGameObject fcgo) {
        if (freeColGameObjectListener != null) {
            freeColGameObjectListener.setFreeColGameObject(id, fcgo);
        }
    }

    /**
     * Notify a listener (if any) of that a game object has gone.
     *
     * @param id The object identifier.
     */
    public void notifyRemoveFreeColGameObject(String id) {
        if (freeColGameObjectListener != null) {
            freeColGameObjectListener.removeFreeColGameObject(id);
        }
    }

    /**
     * Notify a listener (if any) of that a game object has changed owner.
     *
     * @param source The {@code FreeColGameObject} that changed owner.
     * @param oldOwner The old owning {@code Player}.
     * @param newOwner The new owning {@code Player}.
     */
    public void notifyOwnerChanged(FreeColGameObject source,
                                   Player oldOwner, Player newOwner) {
        if (freeColGameObjectListener != null) {
            freeColGameObjectListener.ownerChanged(source, oldOwner, newOwner);
        }
    }

    /**
     * Maintain the player containers for certain ownables.
     * Mainly useful in the client, informing the player that it has
     * gained or lost an ownable.
     *
     * @param o The {@code Ownable} that may have changed.
     * @param oldOwner The previous (possible unchanged) owning
     *     {@code Player}.
     */
    public void checkOwners(Ownable o, Player oldOwner) {
        Player newOwner = o.getOwner();
        if (oldOwner == newOwner) return;

        if (oldOwner != null && oldOwner.removeOwnable(o)) {
            oldOwner.invalidateCanSeeTiles();//+vis
        }
        if (newOwner != null && newOwner.addOwnable(o)) {
            newOwner.invalidateCanSeeTiles();//+vis
        }
    }


    // Miscellaneous utilities.

    /**
     * Checks if all players are ready to launch.
     *
     * @return True if all players are ready to launch.
     */
    public boolean allPlayersReadyToLaunch() {
        return all(getLiveEuropeanPlayerList(), Player::isReady);
    }

    /**
     * Get all the colonies in the game.
     *
     * @param player An optional {@code Player} to omit.
     * @return A stream of all the {@code Colony}s in the game.
     */
    public Stream<Colony> getAllColonies(Player player) {
        return flatten(getLiveEuropeanPlayerList(player), Player::getColonies);
    }
            
    /**
     * Get a list of all the colonies in the game.
     *
     * @param player An optional {@code Player} to omit.
     * @return A list of all the {@code Colony}s in the game.
     */
    public List<Colony> getAllColoniesList(Player player) {
        return toList(getAllColonies(player));
    }
            
    /**
     * Finds a settlement by name.
     *
     * @param name The name of the {@code Settlement}.
     * @return The {@code Settlement} found, or {@code null}
     *     if there is no known {@code Settlement} with the
     *     specified name (the settlement might not be visible to a client).
     */
    public Settlement getSettlementByName(String name) {
        return find(flatten(getLivePlayers(), Player::getSettlements),
                    matchKeyEquals(name, Settlement::getName));
    }

    /**
     * Helper function to get the source object of a message in this game.
     *
     * @param message The {@code ModelMessage} to find the object in.
     * @return The source object.
     */
    public FreeColGameObject getMessageSource(ModelMessage message) {
        return getFreeColGameObject(message.getSourceId());
    }

    /**
     * Helper function to get the object to display with a message in
     * this game.
     *
     * @param message The {@code ModelMessage} to find the object in.
     * @return An object to display.
     */
    public FreeColObject getMessageDisplay(ModelMessage message) {
        String id = message.getDisplayId();
        if (id == null) id = message.getSourceId();
        FreeColObject o = getFreeColGameObject(id);
        if (o == null) {
            try {
                o = getSpecification().getType(id);
            } catch (Exception e) {
                o = null; // Ignore
            }
        }
        return o;
    }

    /**
     * Gets the statistics of this game.
     *
     * @return A {@code Map} of the statistics.
     */
    public java.util.Map<String, String> getStatistics() {
        java.util.Map<String, String> stats = new HashMap<>();

        // Memory
        Utils.garbageCollect();
        long free = Runtime.getRuntime().freeMemory()/(1024*1024);
        long total = Runtime.getRuntime().totalMemory()/(1024*1024);
        long max = Runtime.getRuntime().maxMemory()/(1024*1024);
        stats.put("freeMemory", Long.toString(free));
        stats.put("totalMemory", Long.toString(total));
        stats.put("maxMemory", Long.toString(max));

        // Game objects
        java.util.Map<String, Long> objStats = new HashMap<>();
        long disposed = 0;
        for (FreeColGameObject fcgo : getFreeColGameObjectList()) {
            String className = fcgo.getClass().getSimpleName();
            Long count = objStats.get(className);
            if (count != null) {
                count++;
                objStats.put(className, count);
            } else {
                objStats.put(className, 1L);
            }
            if (fcgo.isDisposed()) disposed++;
        }
        stats.put("disposed", Long.toString(disposed));
        forEachMapEntry(objStats,
                e -> stats.put(e.getKey(), Long.toString(e.getValue())));

        return stats;
    }

    /**
     * Get a location class from an identifier.
     *
     * @param id The identifier to dissect.
     * @return The location class.
     */
    public static Class<? extends FreeColGameObject> getLocationClass(String id) {
        return locationClasses.get(capitalize(FreeColObject.getIdTypeByName(id)));
    }

    /**
     * Unserialize from XML to a FreeColObject in this game.
     *
     * @param <T> The actual return type.
     * @param xml The xml serialized version of an object.
     * @param returnClass The expected object class.
     * @return The unserialized object.
     * @exception XMLStreamException if there are any problems reading from
     *     the stream.
     */
    public <T extends FreeColObject> T unserialize(String xml,
                                                   Class<T> returnClass)
        throws XMLStreamException {
        try {
            FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
            xr.nextTag();
            T ret = newInstance(this, returnClass, false);
            ret.readFromXML(xr);
            return ret;

        } catch (Exception ex) {
            throw new XMLStreamException(ex);
        }
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        lb.mark();
        synchronized (freeColGameObjects) {
            Iterator<FreeColGameObject> iterator = getFreeColGameObjectIterator();
            while (iterator.hasNext()) {
                FreeColGameObject fcgo = iterator.next();
                if (fcgo == null) {
                    lb.add(" null-fcgo");
                } else if (!fcgo.isInitialized()) {
                    lb.add(" ", fcgo.getId(),
                        "(", lastPart(fcgo.getClass().getName(), "."), ")");
                } else {
                    continue;
                }
                if (fix) {
                    iterator.remove();
                    result = result.fix();
                } else {
                    result = result.fail();
                }
            }
        }
        if (lb.grew("\n  Uninitialized game ids: ")) {
            if (fix) lb.add(" (dropped)");
        }

        Map map = getMap();
        if (map != null) {
            result = result.combine(map.checkIntegrity(fix, lb));
        }
        synchronized (this.players) {
            for (Player p : this.players) {
                result = result.combine(p.checkIntegrity(fix, lb));
            }
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Specification getSpecification() {
        return this.specification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setSpecification(Specification specification) {
        this.specification = specification;
    }
    
    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Game o = copyInCast(other, Game.class);
        if (o == null || !super.copyIn(o)) return false;
        this.specification = o.getSpecification();
        // Do not update nextId, it is not meaningful in the client.
        this.uuid = o.getUUID();
        this.clientUserName = o.getClientUserName();

        // Players before map, so the tile owners work efficiently
        this.setPlayers(addPlayers(o.players));

        // Allow creation, might be first sight of the map.
        // Do map early, so map references work
        setMap(update(o.getMap(), Map.class, true));

        this.unknownEnemy = update(o.getUnknownEnemy(), false);
        setNationOptions(o.getNationOptions());
        this.currentPlayer = updateRef(o.getCurrentPlayer(), Player.class);
        this.turn = o.getTurn();
        this.spanishSuccession = o.getSpanishSuccession();
        this.initialActiveUnitId = o.getInitialActiveUnitId();
        return true;
    }


    // Serialization
    // Note: The order of the children is really sensitive.
    // Several fields can not be read without a specification, so it
    // must be written first if the intent is to use that spec in the
    // game when it is read again.  Similarly we try to fail fast
    // if required to read those fields if a spec has not shown up.
    private static final String CIBOLA_TAG = "cibola";
    private static final String CLIENT_USER_NAME_TAG = "clientUserName";
    private static final String CURRENT_PLAYER_TAG = "currentPlayer";
    private static final String INITIAL_ACTIVE_UNIT_ID = "initialActiveUnitId";
    private static final String NEXT_ID_TAG = "nextId";
    private static final String SPANISH_SUCCESSION_TAG = "spanishSuccession";
    private static final String TURN_TAG = "turn";
    private static final String UUID_TAG = "UUID";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (xw.validForSave()) {
            xw.writeAttribute(NEXT_ID_TAG, nextId);
        } else {
            Player client = xw.getClientPlayer();
            if (client != null) {
                xw.writeAttribute(CLIENT_USER_NAME_TAG, client.getName());
            }
        }

        xw.writeAttribute(UUID_TAG, getUUID());

        xw.writeAttribute(TURN_TAG, getTurn().getNumber());

        xw.writeAttribute(SPANISH_SUCCESSION_TAG, spanishSuccession);

        if (initialActiveUnitId != null) {
            xw.writeAttribute(INITIAL_ACTIVE_UNIT_ID, initialActiveUnitId);
        }

        if (currentPlayer != null) {
            xw.writeAttribute(CURRENT_PLAYER_TAG, currentPlayer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (this.specification != null) {
            synchronized (this.specification) {
                // Specification *must be first* if present.
                // It is not necessarily present when reading maps, but an
                // overriding spec is provided there so all should be well.
                this.specification.toXML(xw);
            }
        }

        for (String cityName : NameCache.getCitiesOfCibola()) {
            // Preserve existing order
            xw.writeStartElement(CIBOLA_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, cityName);

            xw.writeEndElement();
        }

        synchronized (this.nationOptions) {
            this.nationOptions.toXML(xw);
        }

        synchronized (this.players) { // Should already be sorted
            for (Player p : this.players) p.toXML(xw);
        }
        Player unknown = getUnknownEnemy();
        if (unknown != null) unknown.toXML(xw);

        Map map = getMap();
        if (map != null) map.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        nextId = xr.getAttribute(NEXT_ID_TAG, -1);

        this.clientUserName = xr.getAttribute(CLIENT_USER_NAME_TAG,
                                              (String)null);

        String str = xr.getAttribute(UUID_TAG, (String)null);
        if (str == null) {
            this.uuid = UUID.randomUUID();
        } else {
            try {
                UUID u = UUID.fromString(str);
                this.uuid = u;
            } catch (IllegalArgumentException iae) {
                ;// Preserve existing uuid
            }
        }

        turn = new Turn(xr.getAttribute(TURN_TAG, 1));

        spanishSuccession = xr.getAttribute(SPANISH_SUCCESSION_TAG, false);

        initialActiveUnitId = xr.getAttribute(INITIAL_ACTIVE_UNIT_ID,
                                              (String)null);

        // currentPlayer handled in readChildren()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        NameCache.clearCitiesOfCibola();
        players.clear();
        unknownEnemy = null;

        // The current player is special.  Defer lookup of the current
        // player tag until we read the children, because that is
        // where the players are defined.
        String current = xr.getAttribute(CURRENT_PLAYER_TAG, (String)null);

        super.readChildren(xr);

        currentPlayer = xr.lookup(this, current, Player.class);

        // Make sure all work locations have rational default production
        // now that all tiles are defined.
        for (Colony c : getAllColoniesList(null)) {
            c.updateProductionTypes();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (CIBOLA_TAG.equals(tag)) {
            String cibola = xr.readId();
            // @compat 0.11.3
            final String oldPrefix = "lostCityRumour.cityName";
            if (cibola.startsWith(oldPrefix)) cibola = "nameCache." + cibola;
            // end @compat 0.11.3
            NameCache.addCityOfCibola(cibola);
            xr.closeTag(CIBOLA_TAG);

        } else if (Map.TAG.equals(tag)) {
            if (this.specification == null) {
                throw new XMLStreamException("Tried to read " + tag
                    + " with null specification");
            }
            setMap(xr.readFreeColObject(game, Map.class));

        } else if (NationOptions.TAG.equals(tag)) {
            if (this.specification == null) {
                throw new XMLStreamException("Tried to read " + tag
                    + " with null specification");
            }
            setNationOptions(new NationOptions(xr, specification));

        } else if (Player.TAG.equals(tag)) {
            if (this.specification == null) {
                throw new XMLStreamException("Tried to read " + tag
                    + " with null specification");
            }
            Player player = xr.readFreeColObject(game, Player.class);
            if (player.isUnknownEnemy()) {
                setUnknownEnemy(player);
            } else {
                players.add(player);
            }

        } else if (Specification.TAG.equals(tag)) {
            setSpecification(new Specification(xr));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        // Two games are not the same just because they have the same
        // identifier, but to avoid having to check everything in the
        // Game just insist on object equality for the equals() test,
        // and accept the basic id-based hashCode().
        return this == o;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Utils.hashCode(getId());
    }
}
