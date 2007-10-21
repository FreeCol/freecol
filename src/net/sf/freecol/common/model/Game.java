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

package net.sf.freecol.common.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

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



    private static final Logger logger = Logger.getLogger(Game.class.getName());

    protected int playerIndex;

    /** 
     * A virtual player to use with enemy privateers 
     * TODO: player should have a game.
     */
    protected final Player unknownEnemy = new Player(null, "unknown enemy", false, null);

    public Player getUnknownEnemy() {
    	return unknownEnemy;
    }
    
    /** Contains all the players in the game. */
    private List<Player> players = new ArrayList<Player>();

    private Map map;

    private GameOptions gameOptions;

    /** The name of the player whose turn it is. */
    private Player currentPlayer = null;

    /**
     * The owner of this view of the game, or <code>null</code> if this game
     * has all the information.
     */
    private Player viewOwner;

    /** The maximum number of (human) players allowed in this game. */
    private int maxPlayers = 4;

    /** Contains references to all objects created in this game. */
    private HashMap<String, FreeColGameObject> freeColGameObjects = new HashMap<String, FreeColGameObject>(10000);

    /**
     * The next availeble ID, that can be given to a new
     * <code>FreeColGameObject</code>.
     */
    private int nextId = 1;

    /** Indicates wether or not this object may give IDs. */
    private boolean canGiveID;

    private Turn turn = new Turn(1);

    private final ModelController modelController;

    private FreeColGameObjectListener freeColGameObjectListener;

    /** The lost city rumour class. */
    private final static LostCityRumour lostCityRumour = new LostCityRumour();

    /**
     * The nations that can still be selected in this Game.
     */
    private List<Nation> vacantNations = new ArrayList<Nation>();


    /**
     * Creates a new game model.
     * 
     * @param modelController A controller object the model can use to make
     *            actions not allowed from the model (generate random numbers
     *            etc).
     * @see net.sf.freecol.server.FreeColServer#FreeColServer(boolean, boolean,
     *      int, String)
     */
    public Game(ModelController modelController) {
        super(null);

        this.modelController = modelController;
        this.viewOwner = null;

        gameOptions = new GameOptions();

        currentPlayer = null;
        canGiveID = true;
        //market = new Market(this);
    }

    /**
     * Initiate a new <code>Game</code> with information from a saved game.
     * 
     * @param freeColGameObjectListener A listener that should be monitoring
     *            this <code>Game</code>.
     * @param modelController A controller object the model can use to make
     *            actions not allowed from the model (generate random numbers
     *            etc).
     * @param in The input stream containing the XML.
     * @param fcgos A list of <code>FreeColGameObject</code>s to be added to
     *            this <code>Game</code>.
     * @throws XMLStreamException if an error occured during parsing.
     * @see net.sf.freecol.server.FreeColServer#loadGame(File)
     */
    public Game(FreeColGameObjectListener freeColGameObjectListener, ModelController modelController,
                XMLStreamReader in, FreeColGameObject[] fcgos) throws XMLStreamException {
        super(null, in);

        setFreeColGameObjectListener(freeColGameObjectListener);
        this.modelController = modelController;
        this.viewOwner = null;

        canGiveID = true;

        for (int i = 0; i < fcgos.length; i++) {
            fcgos[i].setGame(this);
            fcgos[i].updateID();

            if (fcgos[i] instanceof Player) {
                players.add((Player) fcgos[i]);
            }
        }

        readFromXML(in);
    }

    /*
     * Initiate a new <code>Game</code> with information from a saved game.
     * 
     * Currently not used, commented.
     * 
     * @param freeColGameObjectListener A listener that should be monitoring
     * this <code>Game</code>. @param modelController A controller object the
     * model can use to make actions not allowed from the model (generate random
     * numbers etc). @param fcgos A list of <code>FreeColGameObject</code>s
     * to be added to this <code>Game</code>. @param e An XML-element that
     * will be used to initialize this object.
     * 
     * public Game(FreeColGameObjectListener freeColGameObjectListener,
     * ModelController modelController, Element e, FreeColGameObject[] fcgos){
     * super(null, e);
     * 
     * setFreeColGameObjectListener(freeColGameObjectListener);
     * this.modelController = modelController; this.viewOwner = null;
     * 
     * canGiveID = true;
     * 
     * for (int i=0; i<fcgos.length; i++) { fcgos[i].setGame(this);
     * fcgos[i].updateID();
     * 
     * if (fcgos[i] instanceof Player) { players.add((Player) fcgos[i]); } }
     * 
     * readFromXMLElement(e); }
     */

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
     * @param modelController A controller object the model can use to make
     *            actions not allowed from the model (generate random numbers
     *            etc).
     * @param in The XML stream to read the data from.
     * @param viewOwnerUsername The username of the owner of this view of the
     *            game.
     * @throws XMLStreamException if an error occured during parsing.
     * @see net.sf.freecol.client.control.ConnectController#login(String,
     *      String, int)
     */
    public Game(ModelController modelController, XMLStreamReader in, String viewOwnerUsername)
        throws XMLStreamException {
        super(null, in);

        this.modelController = modelController;
        canGiveID = false;
        readFromXML(in);
        this.viewOwner = getPlayerByName(viewOwnerUsername);
    }

    public ModelController getModelController() {
        return modelController;
    }

    /**
     * Get the <code>VacantNations</code> value.
     *
     * @return a <code>List<Nation></code> value
     */
    public final List<Nation> getVacantNations() {
        return vacantNations;
    }

    /**
     * Set the <code>VacantNations</code> value.
     *
     * @param newNations the new list of vacant nations
     */
    public final void setVacantNations(final List<Nation> newNations) {
        this.vacantNations = newNations;
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
     * Returns the first <code>Colony</code> with the given name.
     * 
     * @param name The name of the <code>Colony</code>.
     * @return The <code>Colony</code> or <code>null</code> if there is no
     *         known <code>Colony</code> with the specified name (the colony
     *         might not be visible to a client).
     */
    public Colony getColony(String name) {
        Iterator<Player> pit = getPlayerIterator();
        while (pit.hasNext()) {
            Player p = pit.next();
            Iterator<Settlement> it = p.getSettlementIterator();
            while (it.hasNext()) {
                Colony colony = (Colony) it.next();
                if (colony.getName().equals(name)) {
                    return colony;
                }
            }
        }
        return null;
    }

    public Turn getTurn() {
        return turn;
    }

    /**
     * Returns this game's LostCityRumour.
     * 
     * @return This game's LostCityRumour.
     */
    public LostCityRumour getLostCityRumour() {
        return lostCityRumour;
    }

    /**
     * Get a unique ID to identify a <code>FreeColGameObject</code>.
     * 
     * @return A unique ID.
     */
    public String getNextID() {
        if (canGiveID) {
            String id = Integer.toString(nextId);
            nextId++;

            return id;
        } else {
            logger.warning("The client's \"Game\" was requested to give out an id.");
            throw new Error("The client's \"Game\" was requested to give out an id.");
            // return null;
        }
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
            vacantNations.remove(FreeCol.getSpecification().getNation(player.getNationID()));

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
        vacantNations.add(FreeCol.getSpecification().getNation(player.getNationID()));
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
     * @exception NullPointerException If either <code>id</code> or
     *                <code>freeColGameObject
     *                                   </code> are <i>null</i>.
     */
    public void setFreeColGameObject(String id, FreeColGameObject freeColGameObject) {
        if (id == null || id.equals("") || freeColGameObject == null) {
            throw new NullPointerException();
        }

        FreeColGameObject old = freeColGameObjects.put(id, freeColGameObject);
        if (old != null) {
            logger.warning("Replacing FreeColGameObject: " + old.getClass() + " with " + freeColGameObject.getClass());
            throw new IllegalArgumentException("Replacing FreeColGameObject: " + old.getClass() + " with "
                                               + freeColGameObject.getClass());
        }

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
     * @exception NullPointerException If <code>id == null</code>.
     */
    public FreeColGameObject getFreeColGameObject(String id) {
        if (id == null || id.equals("")) {
            throw new NullPointerException();
        }

        return freeColGameObjects.get(id);
    }

    /**
     * Get the {@link FreeColGameObject} with the given id or null. This method
     * does NOT throw if the id is invalid.
     * 
     * @param id The id, may be null or invalid.
     * @return game object with id or null.
     */
    public FreeColGameObject getFreeColGameObjectSafely(String id) {
        if (id != null) {
            return freeColGameObjects.get(id);
        } else {
            return null;
        }
    }

    /**
     * Removes the <code>FreeColGameObject</code> with the specified ID.
     * 
     * @param id The identifier of the <code>FreeColGameObject</code> that
     *            shall be removed from this <code>Game</code>.
     * @return The <code>FreeColGameObject</code> that has been removed.
     * @exception NullPointerException If <code>id == null</code>.
     */
    public FreeColGameObject removeFreeColGameObject(String id) {
        if (id == null || id.equals("")) {
            throw new NullPointerException();
        }

        if (freeColGameObjectListener != null) {
            freeColGameObjectListener.removeFreeColGameObject(id);
        }

        return freeColGameObjects.remove(id);
    }

    /**
     * Gets the <code>Map</code> that is beeing used in this game.
     * 
     * @return The <code>Map</code> that is beeing used in this game or
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
     * Returns a vacant nation.
     * 
     * @return A vacant nation.
     */
    public Nation getVacantNation() {
        if (vacantNations.isEmpty()) {
            return null;
        } else {
            return vacantNations.get(0);
        }
    }

    /**
     * Return a <code>Player</code> identified by it's nation.
     * 
     * @param nation The nation.
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

    public Player getPlayer(int index) {
        return players.get(index);
    }

    /**
     * Sets the current player.
     * 
     * @param newCp The new current player.
     */
    public void setCurrentPlayer(Player newCp) {
        if (newCp != null) {
            if (currentPlayer != null) {
                currentPlayer.endTurn();
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
     * Checks if the next player is in a new turn.
     * 
     * @return <code>true</code> if changing to the <code>Player</code>
     *         given by {@link #getNextPlayer()} would increase the current
     *         number of turns by one.
     */
    public boolean isNextPlayerInNewTurn() {
        return (players.indexOf(currentPlayer) > players.indexOf(getNextPlayer()) || currentPlayer == getNextPlayer());
        /*
         * int index = players.indexOf(currentPlayer) + 1; return index >=
         * players.size();
         */
    }

    /**
     * Gets the first player in this game.
     * 
     * @return the <code>Player</code> that was first added to this
     *         <code>Game</code>.
     */
    public Player getFirstPlayer() {
        if (players.size() > 0) {
            return players.get(0);
        } else {
            return null;
        }
    }

    /**
     * Gets an <code>Iterator</code> of every registered
     * <code>FreeColGameObject</code>.
     * 
     * @return an <code>Iterator</code> containing every registered
     *         <code>FreeColGameObject</code>.
     * @see #setFreeColGameObject
     */
    public Iterator<FreeColGameObject> getFreeColGameObjectIterator() {
        return freeColGameObjects.values().iterator();
    }

    /**
     * Gets a <code>Player</code> specified by a name.
     * 
     * @param name The name identifing the <code>Player</code>.
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
     * Checks if the specfied name is in use.
     * 
     * @param username The name.
     * @return <i>true</i> if the name is already in use and <i>false</i>
     *         otherwise.
     */
    public boolean playerNameInUse(String username) {
        Iterator<Player> playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = playerIterator.next();

            if (player.getUsername().equals(username)) {
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
     * Returns all the European players known by the player of this game.
     * 
     * @return All the European players known by the player of this game.
     */
    public List<Player> getEuropeanPlayers() {
        List<Player> europeans = new ArrayList<Player>();
        Iterator<Player> playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = playerIterator.next();

            if (player.isEuropean()) {
                europeans.add(player);
            }
        }
        return europeans;
    }

    /**
     * Gets the maximum number of (human) players that can be added to
     * this game.
     * 
     * @return the maximum number of (human) players that can be added to this
     *         game
     */
    public int getMaximumPlayers() {
        return maxPlayers;
    }

    /**
     * Sets the maximum number of (human) players that can be added to
     * this game.
     *
     * @param newMax an <code>int</code> value
     */
    public void setMaximumPlayers(int newMax) {
        maxPlayers = newMax;
    }

    /**
     * Checks if a new <code>Player</code> can be added.
     * 
     * @return <i>true</i> if a new player can be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAddNewPlayer() {

        int realPlayers = 0;

        for (Player player : players) {
            if (!player.isAI())
                realPlayers++;
        }

        return realPlayers < getMaximumPlayers();
    }

    /**
     * Checks if all players are ready to launch.
     * 
     * @return <i>true</i> if all players are ready to launch and <i>false</i>
     *         otherwise.
     */
    public boolean isAllPlayersReadyToLaunch() {
        Iterator<Player> playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = playerIterator.next();

            if (!player.isReady()) {
                return false;
            }
        }

        return true;
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
    @SuppressWarnings("unchecked")
    public boolean checkIntegrity() {
        boolean ok = true;
        Iterator<FreeColGameObject> iterator = ((HashMap<String, FreeColGameObject>) freeColGameObjects.clone())
            .values().iterator();
        while (iterator.hasNext()) {
            FreeColGameObject fgo = iterator.next();
            if (fgo.isUninitialized()) {
                logger.warning("Uinitialized object: " + fgo.getId() + " (" + fgo.getClass() + ")");
                ok = false;
            }
        }
        if (ok) {
            logger.info("Game integrity ok.");
        } else {
            logger.warning("Game integrity test failed.");
        }
        return ok;
    }

    /**
     * Prepares this <code>Game</code> for a new turn.
     * 
     * Invokes <code>newTurn()</code> for every registered
     * <code>FreeColGamObject</code>.
     * 
     * @see #setFreeColGameObject
     */
    @SuppressWarnings("unchecked")
    public void newTurn() {
        turn.increase();
        logger.info("Turn is now " + turn.toString());

        for (Player player : players) {
            logger.finer("Calling newTurn for player " + player.getName());
            player.newTurn();
        }

    }

    /**
     * Gets the <code>GameOptions</code> that is associated with this
     * {@link Game}.
     */
    public GameOptions getGameOptions() {
        return gameOptions;
    }

    /**
     * Gets the amount of gold needed for inciting. This method should NEVER be
     * randomized: it should always return the same amount if given the same
     * three parameters.
     * 
     * @param payingPlayer The <code>Player</code> paying for the incite.
     * @param targetPlayer The <code>Player</code> to be attacked by the
     *            <code>attackingPlayer</code>.
     * @param attackingPlayer The player that would be receiving the money for
     *            incite.
     * @return The amount of gold that should be payed by
     *         <code>payingPlayer</code> to <code>attackingPlayer</code> in
     *         order for <code>attackingPlayer</code> to attack
     *         <code>targetPlayer</code>.
     */
    public static int getInciteAmount(Player payingPlayer, Player targetPlayer, Player attackingPlayer) {
        int amount = 0;
        if (attackingPlayer.getTension(payingPlayer).getValue() > attackingPlayer.getTension(targetPlayer).getValue()) {
            amount = 10000;
        } else {
            amount = 5000;
        }
        amount += 20 * (attackingPlayer.getTension(payingPlayer).getValue() - attackingPlayer.getTension(targetPlayer)
                        .getValue());

        return Math.max(amount, 650);
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
        if (currentPlayer != null) {
            out.writeAttribute("currentPlayer", currentPlayer.getId());
        }

        if (toSavedGame) {
            out.writeAttribute("nextID", Integer.toString(nextId));
        }

        gameOptions.toXML(out);

        Iterator<Player> playerIterator = getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = playerIterator.next();
            p.toXML(out, player, showAll, toSavedGame);
        }

        if (map != null) {
            map.toXML(out, player, showAll, toSavedGame);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        getTurn().setNumber(Integer.parseInt(in.getAttributeValue(null, "turn")));

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

        gameOptions = null;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(GameOptions.getXMLElementTagName())) {
                // Gets the game options:
                if (gameOptions != null) {
                    gameOptions.readFromXML(in);
                } else {
                    gameOptions = new GameOptions(in);
                }
            } else if (in.getLocalName().equals(Player.getXMLElementTagName())) {
                Player player = (Player) getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (player != null) {
                    player.readFromXML(in);
                } else {
                    player = new Player(this, in);
                    players.add(player);
                }
            } else if (in.getLocalName().equals(Map.getXMLElementTagName())) {
                map = (Map) getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (map != null) {
                    map.readFromXML(in);
                } else {
                    map = new Map(this, in);
                }
            }
        }
        if (gameOptions == null) {
            gameOptions = new GameOptions();
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "game";
    }
    
    /**
     * Need to overwrite behavior of equals inherited from FreeColGameObject,
     * since two games are not the same if the have the same id.
     */
    public boolean equals(Object o) {
        return this == o;
    }

    /**
     * Returns an increasing number that can be used when creating players.
     * @return
     */
    public int getNextPlayerIndex() {
        return playerIndex++;
    }
}
