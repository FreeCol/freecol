
package net.sf.freecol.common.model;

import java.util.logging.Logger;
import java.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;


/**
* The main component of the game model.
*/
public class Game extends FreeColGameObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Game.class.getName());



    /** Contains all the players in the game: */
    private Vector players = new Vector();

    private Map map;

    /** The name of the player whose turn it is.*/
    private Player currentPlayer = null;

    /** The maximum number of players allowed in this game */
    private int maxPlayers = 4;

    /** Contains references to all objects created in this game. */
    private HashMap freeColGameObjects = new HashMap(10000);
    
    /** Contains all the messages for this round. */
    private ArrayList modelMessages = new ArrayList();

    /** The next availeble ID, that can be given to a new <code>FreeColGameObject</code>. */
    private int nextId = 1;

    /** Indicates wether or not this object may give ID's */
    private boolean canGiveID;

    /* The market for Europe. */
    private Market market;

    private Turn turn = new Turn(1);


    /**
    * Creates a new game model.
    */
    public Game() {
        super(null);

        currentPlayer = null;
        canGiveID = true;
        market = new Market(this);
    }


    /**
    * Initiate a new <code>Game</code> object from a <code>Element</code>
    * in a DOM-parsed XML-tree.
    */
    public Game(Element element) {
        super(null, element);

        canGiveID = false;
        readFromXMLElement(element);
    }

    /**
    * Returns this Game's Market.
    * @return This game's Market.
    */
    public Market getMarket() {
        return market;
    }


    public Turn getTurn() {
        return turn;
    }


    /**
    * Resets this game's Market.
    */
    public void reinitialiseMarket() {
        market = new Market(this);
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
            //return null;
        }
    }


    /**
    * Adds the specified player to the game.
    *
    * @param player The <code>Player</code> that shall be added to this <code>Game</code>.
    */
    public void addPlayer(Player player) {
        if (player.isAI() || canAddNewPlayer()) {
            players.add(player);

            if (currentPlayer == null) {
                currentPlayer = player;
            }
        } else {
            logger.warning("Tried to add a new player, but the game was already full.");
        }
    }

    
    /**
    * Removes the specified player from the game.
    * @param player The <code>Player</code> that shall be removed from this <code>Game</code>.
    */
    public void removePlayer(Player player) {
        boolean updateCurrentPlayer = (currentPlayer == player);

        players.remove(players.indexOf(player));
        player.dispose();
        
        if (updateCurrentPlayer) {
            currentPlayer = getFirstPlayer();
        }
    }


    /**
    * Registers a new <code>FreeColGameObject</code> with the specified ID.
    *
    * @param id The unique ID of the <code>FreeColGameObject</code>.
    * @param freeColGameObject The <code>FreeColGameObject</code> that shall be added 
    *                          to this <code>Game</code>.
    * @exception NullPointerException If either <code>id</code> or <code>freeColGameObject
    *                                   </code> are <i>null</i>.
    */
    public void setFreeColGameObject(String id, FreeColGameObject freeColGameObject) {
        if (id == null || id.equals("") || freeColGameObject == null) {
            throw new NullPointerException();
        }

        freeColGameObjects.put(id, freeColGameObject);
    }


    /**
    * Gets the <code>FreeColGameObject</code> with the specified ID.
    *
    * @param id The identifier of the <code>FreeColGameObject</code>.
    * @exception NullPointerException If <code>id == null</code>.
    */
    public FreeColGameObject getFreeColGameObject(String id) {
        if (id == null || id.equals("")) {
            throw new NullPointerException();
        }
        
        return (FreeColGameObject) freeColGameObjects.get(id);
    }


    /**
    * Removes the <code>FreeColGameObject</code> with the specified ID.
    *
    * @param id The identifier of the <code>FreeColGameObject</code> that shall 
    *           be removed from this <code>Game</code>.
    * @exception NullPointerException If <code>id == null</code>.
    */
    public FreeColGameObject removeFreeColGameObject(String id) {
        if (id == null || id.equals("")) {
            throw new NullPointerException();
        }

        //return (FreeColGameObject) freeColGameObjects.remove(id);
        return null;
    }


    /**
    * Gets the <code>Map</code> that is beeing used in this game.
    *
    * @return The <code>Map</code> that is beeing used in this game
    *         or <i>null</i> if no <code>Map</code> has been created.
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
    * Sets the current player.
    *
    * @param newCp The new current player.
    * @exception NullPointerException if <code>newCp == null</code>
    */
    public void setCurrentPlayer(Player newCp) {
        if (newCp != null) {
            if (currentPlayer != null) {
                currentPlayer.endTurn();
            }
            
            currentPlayer = newCp;
        } else {
            throw new NullPointerException();
        }
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
    * @return The player that will start its turn as soon as the current player is ready.
    * @see #getCurrentPlayer
    */
    public Player getNextPlayer() {
        int index = players.indexOf(currentPlayer) + 1;

        if (index >= players.size()) {
            index = 0;
        }

        // Find first non-dead player:
        while (true) {
            Player player = (Player) players.get(index);
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
    */
    public boolean isNextPlayerInNewTurn() {
        int index = players.indexOf(currentPlayer) + 1;
        return index >= players.size();
    }


    /**
    * Gets the first player in this game.
    *
    * @return the <code>Player</code> that was first added to this <code>Game</code>.
    */
    public Player getFirstPlayer() {
        if (players.size() > 0) {
            return (Player) players.get(0);
        } else {
            return null;
        }
    }


    /**
    * Gets an <code>Iterator</code> of every registered <code>FreeColGameObject</code>.
    *
    * @return an <codeIterator</code> containing every registered <code>FreeColGameObject</code>.
    * @see #setFreeColGameObject
    */
    public Iterator getFreeColGameObjectIterator() {
        return freeColGameObjects.values().iterator();
    }


    /**
    * @deprecated Use {@link #getFreeColGameObject} instead.
    */
    public Player getPlayerByID(String id) {
        Iterator playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = (Player) playerIterator.next();
            if (player.hasID(id)) {
                return player;
            }
        }

        return null;
    }


    /**
    * Gets a <code>Player</code> specified by a name.
    *
    * @param name The name identifing the <code>Player</code>.
    * @return The <code>Player</code>.
    */
    public Player getPlayerByName(String name) {
        Iterator playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = (Player) playerIterator.next();
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
    * @return <i>true</i> if the name is already in use and <i>false</i> otherwise.
    */
    public boolean playerNameInUse(String username) {
        Iterator playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = (Player) playerIterator.next();

            if (player.getUsername().equals(username)) {
                return true;
            }
        }

        return false;
    }

    
    /**
    * Gets an <code>Iterator</code> of every <code>Player</code> in this game.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getPlayerIterator() {
        return players.iterator();
    }


    /**
    * Gets an <code>Vector</code> containing every <code>Player</code> in this game.
    *
    * @return The <code>Vector</code>.
    */
    public Vector getPlayers() {
        return players;
    }


    /**
    * Gets the maximum number of players that can be added to this game.
    *
    * @return the maximum number of players that can be added to this game
    */
    public int getMaximumPlayers() {
        return maxPlayers;
    }


    /**
    * Checks if a new <code>Player</code> can be added.
    *
    * @return <i>true</i> if a new player can be added and <i>false</i> otherwise.
    */
    public boolean canAddNewPlayer() {
        if (players.size() >= getMaximumPlayers()) {
            return false;
        } else {
            return true;
        }
    }


    /**
    * Checks if all players are ready to launch.
    *
    * @param <i>true</i> if all players are ready to launch and <i>false</i> otherwise.
    */
    public boolean isAllPlayersReadyToLaunch() {
        Iterator playerIterator = getPlayerIterator();

        while (playerIterator.hasNext()) {
            Player player = (Player) playerIterator.next();

            if (!player.isReady()) {
                return false;
            }
        }

        return true;
    }

    
    /**
    * Adds a <code>ModelMessage</code> to this game.
    * @param modelMessage The <code>ModelMessage</code>.
    */
    public void addModelMessage(ModelMessage modelMessage) {
        modelMessages.add(modelMessage);
    }


    public Iterator getModelMessageIterator(Player player) {
        ArrayList out = new ArrayList();

        Iterator i = modelMessages.iterator();
        while (i.hasNext()) {
            ModelMessage m = (ModelMessage) i.next();
            if (m.getOwner() == player && !m.hasBeenDisplayed()) {
                out.add(m);
            }
        }
        
        return out.iterator();
    }


    /**
    * Prepares this <code>Game</code> for a new turn.
    *
    * Invokes <code>newTurn()</code> for every registered <code>FreeColGamObject</code>.
    *
    * @see #setFreeColGameObject
    */
    public void newTurn() {
        //Iterator iterator = getFreeColGameObjectIterator();
        turn.increase();
        modelMessages.clear();

        Iterator iterator = ((HashMap) freeColGameObjects.clone()).values().iterator();

        ArrayList later1 = new ArrayList();
        ArrayList later2 = new ArrayList();
        while (iterator.hasNext()) {
            FreeColGameObject freeColGameObject = (FreeColGameObject) iterator.next();

            // Take the settlements after the buildings and colonytiles:
            if (freeColGameObject instanceof Settlement) {
                later2.add(freeColGameObject);
            } else if (freeColGameObject instanceof Building) {
                later1.add(freeColGameObject);
            } else {
                freeColGameObject.newTurn();
            }
        }

        iterator = later1.iterator();
        while (iterator.hasNext()) {
            FreeColGameObject freeColGameObject = (FreeColGameObject) iterator.next();
            freeColGameObject.newTurn();
        }
        
        iterator = later2.iterator();
        while (iterator.hasNext()) {
            FreeColGameObject freeColGameObject = (FreeColGameObject) iterator.next();
            freeColGameObject.newTurn();
        }
    }


    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Game".
    */
    public Element toXMLElement(Player player, Document document) {
        Element gameElement = document.createElement(getXMLElementTagName());

        gameElement.setAttribute("ID", getID());
        gameElement.setAttribute("turn", Integer.toString(getTurn().getNumber()));

        if (map != null) {
            gameElement.appendChild(map.toXMLElement(player, document));
        }

        Iterator playerIterator = getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = (Player) playerIterator.next();
            gameElement.appendChild(p.toXMLElement(player, document));
        }

        gameElement.appendChild(market.toXMLElement(player, document));

        if (currentPlayer != null) {
            gameElement.setAttribute("currentPlayer", currentPlayer.getID());
        }

        return gameElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param gameElement The DOM-element ("Document Object Model") made to represent this "Game".
    */
    public void readFromXMLElement(Element gameElement) {
        if (gameElement == null) {
            throw new NullPointerException();
        }

        setID(gameElement.getAttribute("ID"));
        
        getTurn().setNumber(Integer.parseInt(gameElement.getAttribute("turn")));

        // Get the map:
        Element mapElement = getChildElement(gameElement, Map.getXMLElementTagName());
        if (mapElement != null) {
            if (map != null) {
                map.readFromXMLElement(mapElement);
            } else {
                map = new Map(this, mapElement);
            }
        }

        // Get the players:
        NodeList playerList = gameElement.getElementsByTagName(Player.getXMLElementTagName());
        for (int i=0; i<playerList.getLength(); i++) {
            Element playerElement = (Element) playerList.item(i);

            Player player = getPlayerByID(playerElement.getAttribute("ID"));
            if (player != null) {
                player.readFromXMLElement(playerElement);
            } else {
                player = new Player(this, playerElement);
                players.add(player);
            }
        }

        Element marketElement = getChildElement(gameElement, Market.getXMLElementTagName());
        if (market != null) {
            market.readFromXMLElement(marketElement);
        } else {
            market = new Market(this, marketElement);
        }

        if (gameElement.hasAttribute("currentPlayer")) {
            currentPlayer = getPlayerByID(gameElement.getAttribute("currentPlayer"));
        } else {
            currentPlayer = null;
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
}
