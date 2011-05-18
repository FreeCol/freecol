/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.generator.MapGeneratorOptions;
import net.sf.freecol.server.model.ServerModelObject;

import net.sf.freecol.common.model.NationOptions.Advantages;

/**
 * The main component of the game model.
 *
 * <br>
 * <br>
 *
 * If an object of this class returns a non-null result to {@link #getViewOwner},
 * then this object just represents a view of the game from a single player's
 * perspective. In that case, some information might be missing from the model.
 */
public class Game extends FreeColGameObject {

    public static final String CIBOLA_TAG = "cibola";

    private static final Logger logger = Logger.getLogger(Game.class.getName());

    /**
     * A virtual player to use with enemy privateers
     */
    private Player unknownEnemy;

    /** Contains all the players in the game. */
    protected List<Player> players = new ArrayList<Player>();

    private Map map;

    /** The name of the player whose turn it is. */
    protected Player currentPlayer = null;

    /**
     * The owner of this view of the game, or <code>null</code> if this game
     * has all the information.
     */
    protected Player viewOwner = null;

    /** Contains references to all objects created in this game. */
    protected HashMap<String, WeakReference<FreeColGameObject>> freeColGameObjects = new HashMap<String, WeakReference<FreeColGameObject>>(10000);

    /**
     * The next available ID, that can be given to a new
     * <code>FreeColGameObject</code>.
     */
    protected int nextId = 1;

    private Turn turn = new Turn(1);

    /**
     * Describe nationOptions here.
     */
    private NationOptions nationOptions;

    /**
     * Whether the War of Spanish Succession has already taken place.
     */
    private boolean spanishSuccession = false;

    protected FreeColGameObjectListener freeColGameObjectListener;

    /**
     * The cities of Cibola remaining in this game.
     */
    private List<String> citiesOfCibola = null;

    /**
     * The combat model this game uses. At the moment, the only combat
     * model available is the SimpleCombatModel, which strives to
     * implement the combat model of the original game. However, it is
     * anticipated that other, more complex combat models will be
     * implemented in future. As soon as that happens, we will also
     * have to make the combat model selectable.
     */
    protected CombatModel combatModel;

    /**
     * The Specification this game uses.
     */
    private Specification specification;



    /**
     * This constructor is used by the Server to create a new Game
     * with the given Specification.
     *
     * @param specification
     */
    protected Game(Specification specification) {
        super(null);
        this.specification = specification;
    }

    /**
     * Minimal constructor,
     * Just necessary to call parent constructor
     * @param game
     */
    protected Game(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
    }

    /*
     * Initiate a new <code>Game</code> object from a <code>Element</code>
     * in a DOM-parsed XML-tree.
     *
     * Currently not used, commented.
     *
     * @param modelController A controller object the model can use to make
     * actions not allowed from the model (generate random numbers etc). @param
     * viewOwnerUsername The username of the owner of this view of the game.
     * @param e An XML-element that will be used to initialize this object.
     *
     * public Game(ModelController modelController, Element e, String
     * viewOwnerUsername){ super(null, e);
     *
     * this.modelController = modelController; canGiveID = false;
     * readFromXMLElement(e); this.viewOwner =
     * getPlayerByName(viewOwnerUsername); }
     */

    /**
     * Initiate a new <code>Game</code> object from an XML-representation.
     * <p>
     * Note that this is used on the client side; the game is really a partial
     * view of the server-side game.
     *
     * @param in The XML stream to read the data from.
     * @param viewOwnerUsername The username of the owner of this view of the
     *            game.
     * @throws XMLStreamException if an error occured during parsing.
     * @see net.sf.freecol.client.control.ConnectController#login(String,
     *      String, int)
     */
    public Game(XMLStreamReader in, String viewOwnerUsername)
        throws XMLStreamException {
        super(null, in);

        this.combatModel = new SimpleCombatModel();
        readFromXML(in);
        this.viewOwner = getPlayerByName(viewOwnerUsername);
        // setId() does not add Games to the freeColGameObjects
        this.setFreeColGameObject(getId(), this);
    }

    /**
     * Returns the "Unknown Enemy" Player, which is used for
     * privateers.
     *
     * @return a <code>Player</code> value
     */
    public Player getUnknownEnemy() {
    	return unknownEnemy;
    }

    /**
     * Sets the "Unknown Enemy" Player, which is used for
     * privateers.
     *
     * @param player a <code>Player</code> value
     */
    public void setUnknownEnemy(Player player) {
        this.unknownEnemy = player;
    }

    /**
     * Get the <code>VacantNations</code> value.
     *
     * @return a <code>List<Nation></code> value
     */
    public final List<Nation> getVacantNations() {
        List<Nation> result = new ArrayList<Nation>();
        for (Entry<Nation, NationState> entry : nationOptions.getNations().entrySet()) {
            if (entry.getValue() == NationState.AVAILABLE) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Check if the clients are trusted or if the server should keep secrets in
     * order to prevent cheating.
     *
     * @return true if clients are to be trusted.
     */
    public boolean isClientTrusted() {
        // Trust the clients in order to prevent certain bugs, fix this later
        return false;
    }

    /**
     * Returns the owner of this view of the game, or <code>null</code> if
     * this game has all the information. <br>
     * <br>
     * If this value is <code>null</code>, then it means that this
     * <code>Game</code> object has access to all information (ie is the
     * server model).
     *
     * @return The <code>Player</code> using this <code>Game</code>-object
     *         as a view.
     */
    public Player getViewOwner() {
        return viewOwner;
    }

    /**
     * Finds a settlement by name.
     *
     * @param name The name of the <code>Settlement</code>.
     * @return The <code>Settlement</code> or <code>null</code> if
     *         there is no known <code>Settlement</code> with the
     *         specified name (the settlement might not be visible to
     *         a client).
     */
    public Settlement getSettlement(String name) {
        for (Player p : getPlayers()) {
            for (Settlement s : p.getSettlements()) {
                if (name.equals(s.getName())) return s;
            }
        }
        return null;
    }

    /**
     * Gets the current turn in this game.
     *
     * @return The current <code>Turn</code>.
     */
    public Turn getTurn() {
        return turn;
    }

    /**
     * Sets the current turn in this game.
     *
     * @param newTurn The new <code>Turn</code> to set.
     */
    public void setTurn(Turn newTurn) {
        turn = newTurn;
    }

    /**
     * Get the <code>CombatModel</code> value.
     *
     * @return a <code>CombatModel</code> value
     */
    public final CombatModel getCombatModel() {
        return combatModel;
    }

    /**
     * Set the <code>CombatModel</code> value.
     *
     * @param newCombatModel The new CombatModel value.
     */
    public final void setCombatModel(final CombatModel newCombatModel) {
        this.combatModel = newCombatModel;
    }

    /**
     * Get the <code>OptionGroup</code> value.
     *
     * @return a <code>OptionGroup</code> value
     */
    public final OptionGroup getDifficultyLevel() {
        return specification.getDifficultyLevel();
    }

    /**
     * Adds the specified player to the game.
     *
     * @param player The <code>Player</code> that shall be added to this
     *            <code>Game</code>.
     */
    public void addPlayer(Player player) {
        if (player.isAI() || canAddNewPlayer()) {
            players.add(player);
            Nation nation = getSpecification().getNation(player.getNationID());
            nationOptions.getNations().put(nation, NationState.NOT_AVAILABLE);
            if (currentPlayer == null) {
                currentPlayer = player;
            }
        } else {
            logger.warning("Tried to add a new player, but the game was already full.");
        }
    }

    /**
     * Removes the specified player from the game.
     *
     * @param player The <code>Player</code> that shall be removed from this
     *            <code>Game</code>.
     */
    public void removePlayer(Player player) {
        boolean updateCurrentPlayer = (currentPlayer == player);

        players.remove(players.indexOf(player));
        Nation nation = getSpecification().getNation(player.getNationID());
        nationOptions.getNations().put(nation, NationState.AVAILABLE);
        player.dispose();

        if (updateCurrentPlayer) {
            currentPlayer = getFirstPlayer();
        }
    }

    /**
     * Registers a new <code>FreeColGameObject</code> with the specified ID.
     *
     * @param id The unique ID of the <code>FreeColGameObject</code>.
     * @param freeColGameObject The <code>FreeColGameObject</code> that shall
     *            be added to this <code>Game</code>.
     * @exception IllegalArgumentException If either <code>id</code>
     *                or <code>freeColGameObject </code> are
     *                <i>null</i>.
     */
    public void setFreeColGameObject(String id, FreeColGameObject freeColGameObject) {
        if (id == null || id.equals("")) {
            throw new IllegalArgumentException("Parameter 'id' must not be 'null' or empty string.");
        } else if (freeColGameObject == null) {
            throw new IllegalArgumentException("Parameter 'freeColGameObject' must not be 'null'.");
        }

        final WeakReference<FreeColGameObject> wr = new WeakReference<FreeColGameObject>(freeColGameObject);
        final FreeColGameObject old = getFreeColGameObjectSafely(id);
        if (old != null) {
            throw new IllegalArgumentException("Replacing FreeColGameObject "
                + id + ": " + old.getClass()
                + " with " + freeColGameObject.getClass());
        }
        freeColGameObjects.put(id, wr);

        if (freeColGameObjectListener != null) {
            freeColGameObjectListener.setFreeColGameObject(id, freeColGameObject);
        }
    }

    public void setFreeColGameObjectListener(FreeColGameObjectListener freeColGameObjectListener) {
        this.freeColGameObjectListener = freeColGameObjectListener;
    }

    public FreeColGameObjectListener getFreeColGameObjectListener() {
        return freeColGameObjectListener;
    }

    /**
     * Gets the <code>FreeColGameObject</code> with the specified ID.
     *
     * @param id The identifier of the <code>FreeColGameObject</code>.
     * @return The <code>FreeColGameObject</code>.
     * @exception IllegalArgumentException If <code>id == null</code>, or <code>id = ""</code>.
     */
    public FreeColGameObject getFreeColGameObject(String id) {
        if (id == null || id.equals("")) {
            throw new IllegalArgumentException("Parameter 'id' must not be null or empty string.");
        }
        return getFreeColGameObjectSafely(id);
    }

    /**
     * Get the {@link FreeColGameObject} with the given id or null. This method
     * does NOT throw if the id is invalid.
     *
     * @param id The id, may be null or invalid.
     * @return game object with id or null.
     */
    public FreeColGameObject getFreeColGameObjectSafely(String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        final WeakReference<FreeColGameObject> ro = freeColGameObjects.get(id);
        if (ro != null) {
            final FreeColGameObject o = ro.get();
            if (o != null) {
                return o;
            } else {
                freeColGameObjects.remove(id);
            }
        }
        return null;
    }

    /**
     * Removes the <code>FreeColGameObject</code> with the specified ID.
     *
     * @param id The identifier of the <code>FreeColGameObject</code> that
     *            shall be removed from this <code>Game</code>.
     * @return The <code>FreeColGameObject</code> that has been removed.
     * @exception IllegalArgumentException If <code>id == null</code>, or <code>id = ""</code>.
     */
    public FreeColGameObject removeFreeColGameObject(String id) {
        if (id == null || id.equals("")) {
            throw new IllegalArgumentException("Parameter 'id' must not be null or empty string.");
        }

        final FreeColGameObject o = getFreeColGameObjectSafely(id);

        if (freeColGameObjectListener != null) {
            freeColGameObjectListener.removeFreeColGameObject(id);
        }

        freeColGameObjects.remove(id);
        return o;
    }

    /**
     * Gets the <code>Map</code> that is being used in this game.
     *
     * @return The <code>Map</code> that is being used in this game or
     *         <i>null</i> if no <code>Map</code> has been created.
     */
    public Map getMap() {
        return map;
    }

    /**
     * Sets the <code>Map</code> that is going to be used in this game.
     *
     * @param map The <code>Map</code> that is going to be used in this game.
     */
    public void setMap(Map map) {
        this.map = map;
    }

    /**
     * Get the <code>NationOptions</code> value.
     *
     * @return a <code>NationOptions</code> value
     */
    public final NationOptions getNationOptions() {
        return nationOptions;
    }

    /**
     * Set the <code>NationOptions</code> value.
     *
     * @param newNationOptions The new NationOptions value.
     */
    public final void setNationOptions(final NationOptions newNationOptions) {
        this.nationOptions = newNationOptions;
    }

    /**
     * Returns a vacant nation.
     *
     * @return A vacant nation.
     */
    public Nation getVacantNation() {
        //System.out.println("NationOptions: " + nationOptions);
        for (Entry<Nation, NationState> entry : nationOptions.getNations().entrySet()) {
            if (entry.getValue() == NationState.AVAILABLE) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Return a <code>Player</code> identified by it's nation.
     *
     * @param nationID The nation.
     * @return The <code>Player</code> of the given nation.
     */
    public Player getPlayer(String nationID) {
        Iterator<Player> playerIterator = getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player player = playerIterator.next();
            if (player.getNationID().equals(nationID)) {
                return player;
            }
        }

        return null;
    }

    /**
     * Sets the current player.
     *
     * @param newCp The new current player.
     */
    public void setCurrentPlayer(Player newCp) {
        if (newCp != null) {
            if (currentPlayer != null) {
                currentPlayer.removeModelMessages();
                currentPlayer.invalidateCanSeeTiles();
            }
        } else {
            logger.info("Current player set to 'null'.");
        }

        currentPlayer = newCp;
    }

    /**
     * Gets the current player. This is the <code>Player</code> currently
     * playing the <code>Game</code>.
     *
     * @return The current player.
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Gets the next current player.
     *
     * @return The player that will start its turn as soon as the current player
     *         is ready.
     * @see #getCurrentPlayer
     */
    public Player getNextPlayer() {
        return getPlayerAfter(currentPlayer);
    }

    /**
     * Gets the player after the given player.
     *
     * @param beforePlayer The <code>Player</code> before the
     *            <code>Player</code> to be returned.
     * @return The <code>Player</code> after the <code>beforePlayer</code>
     *         in the list which determines the order each player becomes the
     *         current player.
     * @see #getNextPlayer
     */
    public Player getPlayerAfter(Player beforePlayer) {
        if (players.size() == 0) {
            return null;
        }

        int index = players.indexOf(beforePlayer) + 1;

        if (index >= players.size()) {
            index = 0;
        }

        // Find first non-dead player:
        while (true) {
            Player player = players.get(index);
            if (!player.isDead()) {
                return player;
            }

            index++;

            if (index >= players.size()) {
                index = 0;
            }
        }
    }

    /**
     * Gets the first player in this game.
     *
     * @return the <code>Player</code> that was first added to this
     *         <code>Game</code>.
     */
    public Player getFirstPlayer() {
        if (players.isEmpty()) {
            return null;
        } else {
            return players.get(0);
        }
    }

    /**
     * Collects a list of all the ServerModelObjects in this game.
     *
     * @return A list of all the ServerModelObjects in this game.
     */
    public List<ServerModelObject> getServerModelObjects() {
        List<ServerModelObject> objs = new ArrayList<ServerModelObject>();
        for (WeakReference<FreeColGameObject> wr :freeColGameObjects.values()) {
            if (wr.get() instanceof ServerModelObject) {
                objs.add((ServerModelObject) wr.get());
            }
        }
        return objs;
    }

    /**
     * Gets an <code>Iterator</code> of every registered
     * <code>FreeColGameObject</code>.
     *
     * This <code>Iterator</code> should be iterated at least once
     * in a while since it cleans the <code>FreeColGameObject</code>
     * cache.
     *
     * @return an <code>Iterator</code> containing every registered
     *         <code>FreeColGameObject</code>.
     * @see #setFreeColGameObject
     */
    public Iterator<FreeColGameObject> getFreeColGameObjectIterator() {
        return new Iterator<FreeColGameObject>() {
            final Iterator<Entry<String, WeakReference<FreeColGameObject>>> it = freeColGameObjects.entrySet().iterator();
            FreeColGameObject nextValue = null;

            public boolean hasNext() {
                while (nextValue == null) {
                    if (!it.hasNext()) {
                        return false;
                    }
                    final Entry<String, WeakReference<FreeColGameObject>> entry = it.next();
                    final WeakReference<FreeColGameObject> wr = entry.getValue();
                    final FreeColGameObject o = wr.get();
                    if (o == null) {
                        final String id = entry.getKey();
                        if (freeColGameObjectListener != null) {
                            freeColGameObjectListener.removeFreeColGameObject(id);
                        }
                        it.remove();
                    } else {
                        nextValue = o;
                    }
                }

                return nextValue != null;
            }

            public FreeColGameObject next() {
                hasNext();
                final FreeColGameObject o = nextValue;
                nextValue = null;
                return o;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Gets a <code>Player</code> specified by a name.
     *
     * @param name The name identifying the <code>Player</code>.
     * @return The <code>Player</code>.
     */
    public Player getPlayerByName(String name) {
        Iterator<Player> playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = playerIterator.next();
            if (player.getName().equals(name)) {
                return player;
            }
        }

        return null;
    }

    /**
     * Checks if the specified name is in use.
     *
     * @param username The name.
     * @return <i>true</i> if the name is already in use and <i>false</i>
     *         otherwise.
     */
    public boolean playerNameInUse(String username) {

        for (Player player : players) {
            if (player.getName().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Player</code> in this
     * game.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Player> getPlayerIterator() {
        return players.iterator();
    }

    /**
     * Gets an <code>Vector</code> containing every <code>Player</code> in
     * this game.
     *
     * @return The <code>Vector</code>.
     */
    public List<Player> getPlayers() {
        return players;
    }

    public int getNumberOfPlayers() {
        return players.size();
    }

    /**
     * Returns all the live European players known by the player of this game.
     *
     * @return All the live European players known by the player of this game.
     */
    public List<Player> getLiveEuropeanPlayers() {
        List<Player> europeans = new ArrayList<Player>();
        for (Player player : players) {
            if (player.isEuropean() && !player.isDead()) {
                europeans.add(player);
            }
        }
        return europeans;
    }

    /**
     * Checks if a new <code>Player</code> can be added.
     *
     * @return <i>true</i> if a new player can be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAddNewPlayer() {
        return (getVacantNation() != null);
    }

    /**
     * Checks if all players are ready to launch.
     *
     * @return <i>true</i> if all players are ready to launch and <i>false</i>
     *         otherwise.
     */
    public boolean isAllPlayersReadyToLaunch() {
        for (Player player : players) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the <code>SpanishSuccession</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean getSpanishSuccession() {
        return spanishSuccession;
    }

    /**
     * Set the <code>SpanishSuccession</code> value.
     *
     * @param newSpanishSuccession The new SpanishSuccession value.
     */
    public final void setSpanishSuccession(final boolean newSpanishSuccession) {
        this.spanishSuccession = newSpanishSuccession;
    }

    /**
     * Checks the integrity of this <code>Game</code
     * by checking if there are any
     * {@link FreeColGameObject#isUninitialized() uninitialized objects}.
     *
     * Detected problems gets written to the log.
     *
     * @return <code>true</code> if the <code>Game</code> has
     *      been loaded properly.
     */
    public boolean checkIntegrity() {
    	List<String> brokenObjects = new ArrayList<String>();
        boolean ok = true;
        Iterator<FreeColGameObject> iterator = getFreeColGameObjectIterator();
        while (iterator.hasNext()) {
            FreeColGameObject fgo = iterator.next();
            if (fgo.isUninitialized()) {
            	brokenObjects.add(fgo.getId());
                logger.warning("Uninitialized object: " + fgo.getId() + " (" + fgo.getClass() + ")");
                ok = false;
            }
        }
        if (ok) {
            logger.info("Game integrity ok.");
        } else {
            logger.warning("Game integrity test failed.");
            fixIntegrity(brokenObjects);
        }
        return ok;
    }

    /**
     * Try to fix integrity problems
     */
    private boolean fixIntegrity(List<String> list){
    	// try to update Units who may have missing info
    	for(Player player : this.getPlayers()){
    		for(Unit unit : player.getUnits()){
    			if(unit.getOwner() == null){
    				logger.warning("Fixing " + unit.getId() + ": owner missing");
    				unit.setOwner(player);
    			}
    		}
    	}
    	return false;
    }


    /**
     * Gets the <code>MapGeneratorOptions</code> that is associated with this
     * {@link Game}.
     */
    public OptionGroup getMapGeneratorOptions() {
        return specification.getOptionGroup("mapGeneratorOptions");
    }

    /**
     * Initialize the list of cities of Cibola.
     * Pull them out of the message file and randomize the order.
     */
    private void initializeCitiesOfCibola() {
        citiesOfCibola = new ArrayList<String>();
        for (int index = 0; index < 7; index++) {
            citiesOfCibola.add("lostCityRumour.cityName." + index);
        }
        Collections.shuffle(citiesOfCibola);
    }

    /**
     * Get the next name for a city of Cibola.
     *
     * @return The next name for a city of Cibola, or null if none available.
     */
    public String getCityOfCibola() {
        if (citiesOfCibola == null) initializeCitiesOfCibola();
        return (citiesOfCibola.size() == 0) ? null : citiesOfCibola.remove(0);
    }

    /**
     * Helper function to get the source object of a message in this game.
     *
     * @param message The <code>ModelMessage</code> to find the object in.
     * @return The source object.
     */
    public FreeColGameObject getMessageSource(ModelMessage message) {
        return getFreeColGameObjectSafely(message.getSourceId());
    }

    /**
     * Helper function to get the object to display with a message in
     * this game.
     *
     * @param message The <code>ModelMessage</code> to find the object in.
     * @return An object to display.
     */
    public FreeColObject getMessageDisplay(ModelMessage message) {
        String id = message.getDisplayId();
        if (id == null) id = message.getSourceId();
        FreeColObject o = getFreeColGameObjectSafely(id);
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
     * Return the specification for this Game.
     *
     * @return a <code>Specification</code> value
     */
    @Override
    public Specification getSpecification() {
        return specification;
    }

    /**
     * Need to overwrite behavior of equals inherited from FreeColGameObject,
     * since two games are not the same if the have the same id.
     */
    public boolean equals(Object o) {
        return this == o;
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * <br>
     * <br>
     *
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        if (toSavedGame && !showAll) {
            throw new IllegalArgumentException("showAll must be set to true when toSavedGame is true.");
        }

        out.writeAttribute("ID", getId());
        out.writeAttribute("turn", Integer.toString(getTurn().getNumber()));
        out.writeAttribute("spanishSuccession", Boolean.toString(spanishSuccession));

        writeAttribute(out, "currentPlayer", currentPlayer);

        if (toSavedGame) {
            out.writeAttribute("nextID", Integer.toString(nextId));
        }

        specification.toXMLImpl(out);

        if (citiesOfCibola == null) initializeCitiesOfCibola();
        for (String cityName : citiesOfCibola) {
            out.writeStartElement(CIBOLA_TAG);
            out.writeAttribute(ID_ATTRIBUTE_TAG, cityName);
            out.writeEndElement();
        }
        nationOptions.toXML(out);

        // serialize players
        Iterator<Player> playerIterator = getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = playerIterator.next();
            p.toXML(out, player, showAll, toSavedGame);
        }
        writeFreeColGameObject(getUnknownEnemy(), out, player, showAll, toSavedGame);

        // serialize map
        writeFreeColGameObject(map, out, player, showAll, toSavedGame);

        /* Moved to within player.  Last used in 0.9.x.
        // serialize messages
        playerIterator = getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = playerIterator.next();
            if (this.isClientTrusted() || showAll || p.equals(player)) {
                for (ModelMessage message : p.getModelMessages()) {
                    message.toXML(out);
                }
            }
        }
        */

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        turn = new Turn(getAttribute(in, "turn", 1));
        setSpanishSuccession(getAttribute(in, "spanishSuccession", false));

        final String nextIDStr = in.getAttributeValue(null, "nextID");
        if (nextIDStr != null) {
            nextId = Integer.parseInt(nextIDStr);
        }

        final String currentPlayerStr = in.getAttributeValue(null, "currentPlayer");
        if (currentPlayerStr != null) {
            currentPlayer = (Player) getFreeColGameObject(currentPlayerStr);
            if (currentPlayer == null) {
                currentPlayer = new Player(this, currentPlayerStr);
                players.add(currentPlayer);
            }
        } else {
            currentPlayer = null;
        }

        citiesOfCibola = new ArrayList<String>(7);
        OptionGroup gameOptions = null;
        OptionGroup mapGeneratorOptions = null;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String tagName = in.getLocalName();
            logger.finest("Found tag " + tagName);
            if (tagName.equals("gameOptions") || tagName.equals("game-options")) {
                // TODO: remove 0.9.x compatibility code
                gameOptions = new OptionGroup(in);
            } else if (tagName.equals(NationOptions.getXMLElementTagName())) {
                if (nationOptions == null) {
                    nationOptions = new NationOptions(specification, Advantages.SELECTABLE);
                }
                nationOptions.readFromXML(in);
            } else if (tagName.equals(Player.getXMLElementTagName())) {
                Player player = (Player) getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (player == null) {
                    player = new Player(this, in);
                    if (player.isUnknownEnemy()) {
                        setUnknownEnemy(player);
                    } else {
                        players.add(player);
                    }
                } else {
                    player.readFromXML(in);
                }
            } else if (tagName.equals(Map.getXMLElementTagName())) {
                String mapId = in.getAttributeValue(null, "ID");
                map = (Map) getFreeColGameObject(mapId);
                if (map != null) {
                    map.readFromXML(in);
                } else {
                    map = new Map(this, mapId);
                    map.readFromXML(in);
                }
            } else if (tagName.equals(ModelMessage.getXMLElementTagName())) {
                // 0.9.x save format compatibility.  Remove one day.
                ModelMessage m = new ModelMessage();
                m.readFromXML(in);
                // When this goes, remove getOwnerId().
                String owner = m.getOwnerId();
                if (owner != null) {
                    Player player = (Player) getFreeColGameObjectSafely(owner);
                    player.addModelMessage(m);
                }
            } else if (tagName.equals("citiesOfCibola")) {
                // TODO: remove support for old format
                citiesOfCibola = readFromListElement("citiesOfCibola", in, String.class);
            } else if (CIBOLA_TAG.equals(tagName)) {
                citiesOfCibola.add(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
                in.nextTag();
            } else if (OptionGroup.getXMLElementTagName().equals(tagName)
                       || "difficultyLevel".equals(tagName)) {
                // remove compatibility code after 0.10.0
                OptionGroup difficultyLevel = new OptionGroup(in);
            } else if (MapGeneratorOptions.getXMLElementTagName().equals(tagName)) {
                // TODO: remove 0.9.x compatibility code
                mapGeneratorOptions = new OptionGroup(in);
            } else if (Specification.getXMLElementTagName().equals(tagName)) {
                Specification spec = new Specification();
                spec.readFromXMLImpl(in);
                if (specification == null) {
                    specification = spec;
                    specification.clean();
                }
            } else {
                logger.warning("Unknown tag: " + tagName + " loading game");
                in.nextTag();
            }
        }
        // sanity check: we should be on the closing tag
        if (!in.getLocalName().equals(Game.getXMLElementTagName())) {
            logger.warning("Error parsing xml: expecting closing tag </" + Game.getXMLElementTagName() + "> "+
                           "found instead: " + in.getLocalName());
        }

        // TODO: remove compatibility code post 0.10.0
        if (gameOptions != null) {
            addOldOptions(gameOptions);
        }
        if (mapGeneratorOptions != null) {
            addOldOptions(mapGeneratorOptions);
        }
        // end compatibility code

    }

    // TODO: remove compatibility code post 0.10.0
    private void addOldOptions(OptionGroup group) {
        Iterator<Option> iterator = group.iterator();
        while (iterator.hasNext()) {
            Option opt = iterator.next();
            if (opt instanceof IntegerOption) {
                IntegerOption option = (IntegerOption) opt;
                if (specification.hasOption(option.getId())) {
                    specification.getIntegerOption(option.getId())
                        .setValue(option.getValue());
                } else {
                    specification.addAbstractOption(option);
                }
            } else if (opt instanceof BooleanOption) {
                BooleanOption option = (BooleanOption) opt;
                if (specification.hasOption(option.getId())) {
                specification.getBooleanOption(option.getId())
                    .setValue(option.getValue());
                } else {
                    specification.addAbstractOption(option);
                }
            }
        }
    }
    // end compatibility code


    /**
     * Partial writer, so that simple updates can be brief.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException If there are problems writing the stream.
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * Partial reader, so that simple updates can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    protected void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "game";
    }

}
