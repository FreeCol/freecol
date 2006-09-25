
package net.sf.freecol.server.model;

import java.net.Socket;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.option.BooleanOption;


/**
* A <code>Player</code> with additional (server specific) information.
*
* That is: pointers to this player's
* {@link Connection} and {@link Socket}
*/
public class ServerPlayer extends Player implements ServerModelObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /** The network socket to the player's client. */
    private Socket socket;

    /** The connection for this player. */
    private Connection connection;

    private boolean connected = false;


    private String serverID;



    /**
    * Creates a new <code>ServerPlayer</code>.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param name The player name.
    * @param admin Whether the player is the game administrator or not.
    * @param socket The socket to the player's client.
    * @param connection The <code>Connection</code> for the above mentioned socket.
    */
    public ServerPlayer(Game game, String name, boolean admin, Socket socket, Connection connection) {
        super(game, name, admin);

        this.socket = socket;
        this.connection = connection;

        resetExploredTiles(getGame().getMap());
        resetCanSeeTiles();

        connected = (connection != null);
    }


    /**
    * Creates a new <code>ServerPlayer</code>.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param name The player name.
    * @param admin Whether the player is the game administrator or not.
    * @param ai Whether this is an AI player.
    * @param socket The socket to the player's client.
    * @param connection The <code>Connection</code> for the above mentioned socket.
    * @param nation The nation of the <code>Player</code>.
    */
    public ServerPlayer(Game game, String name, boolean admin, boolean ai, Socket socket, Connection connection, int nation) {
        super(game, name, admin, ai, nation);

        this.socket = socket;
        this.connection = connection;

        resetExploredTiles(getGame().getMap());
        resetCanSeeTiles();

        connected = (connection != null);
    }


    public ServerPlayer(XMLStreamReader in) throws XMLStreamException {
        readFromServerAdditionElement(in);
    }


    /**
    * Checks if this player is currently connected to the server.
    * @return <i>true</i> if this player is currently connected to the server
    *         and <code>false</code> otherwise.
    */
    public boolean isConnected() {
        return connected;
    }


    /**
    * Sets the "connected"-status of this player.
    * 
    * @param connected Should be <i>true</i> if this player is currently 
    *         connected to the server and <code>false</code> otherwise.
    * @see #isConnected
    */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }


    /**
    * Resets this player's explored tiles. This is done by setting
    * all the tiles within a {@link Unit}s line of sight visible.
    * The other tiles are made unvisible.
    *
    * @param map The <code>Map</code> to reset the explored tiles on.
    * @see #hasExplored
    */
    public void resetExploredTiles(Map map) {
        if (map != null) {
            Iterator unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = (Unit) unitIterator.next();

                setExplored(unit.getTile());

                Iterator positionIterator;
                if (unit.getTile().getColony() != null) {
                    positionIterator = map.getCircleIterator(unit.getTile().getPosition(), true, 2);
                } else {
                    positionIterator = map.getCircleIterator(unit.getTile().getPosition(), true, unit.getLineOfSight());
                }

                while (positionIterator.hasNext()) {
                    Map.Position p = (Map.Position) positionIterator.next();
                    setExplored(map.getTile(p));
                }
            }

        }

    }


    /**
    * Checks if this <code>Player</code> has explored the given <code>Tile</code>.
    * @param tile The <code>Tile</code>.
    * @return <i>true</i> if the <code>Tile</code> has been explored and
    *         <i>false</i> otherwise.
    */
    public boolean hasExplored(Tile tile) {
        return tile.isExploredBy(this);
    }


    /**
    * Sets the given tile to be explored by this player and updates the player's
    * information about the tile.
    *
    * @see Tile#updatePlayerExploredTile(Player)
    */
    public void setExplored(Tile tile) {
        tile.setExploredBy(this, true);
    }


    /**
    * Sets the tiles within the given <code>Unit</code>'s line of
    * sight to be explored by this player.
    *
    * @param unit The <code>Unit</code>.
    * @see #setExplored(Tile)
    * @see #hasExplored
    */
    public void setExplored(Unit unit) {
        if (getGame() == null || getGame().getMap() == null || unit == null || unit.getLocation() == null || unit.getTile() == null) {
            return;
        }

        if (canSeeTiles == null) {
            resetCanSeeTiles();
        }

        setExplored(unit.getTile());
        canSeeTiles[unit.getTile().getPosition().getX()][unit.getTile().getPosition().getY()] = true;

        Iterator positionIterator = getGame().getMap().getCircleIterator(unit.getTile().getPosition(), true, unit.getLineOfSight());
        while (positionIterator.hasNext()) {
            Map.Position p = (Map.Position) positionIterator.next();
            if (p == null) {
                continue;
            }
            setExplored(getGame().getMap().getTile(p));
            canSeeTiles[p.getX()][p.getY()] = true;
        }
    }
    

    /**
    * (DEBUG ONLY) Makes the entire map visible.
    */
    public void revealMap() {
        Iterator positionIterator = getGame().getMap().getWholeMapIterator();

        while (positionIterator.hasNext()) {
            Map.Position p = (Map.Position) positionIterator.next();
            setExplored(getGame().getMap().getTile(p));
        }
        
        ((BooleanOption) getGame().getGameOptions().getObject(GameOptions.UNIT_HIDING)).setValue(false);
        ((BooleanOption) getGame().getGameOptions().getObject(GameOptions.FOG_OF_WAR)).setValue(false);
        
        resetCanSeeTiles();
    }


    /**
     * Gets the socket of this player.
     * @return The <code>Socket</code>.
     */
    public Socket getSocket() {
        return socket;
    }


    /**
     * Gets the connection of this player.
     * @return The <code>Connection</code>.
     */
    public Connection getConnection() {
        return connection;
    }
    
    
    /**
     * Sets the connection of this player.
     * @param connection The <code>Connection</code>.
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
        connected = (connection != null);
    }
    
    public void toServerAdditionElement(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getServerAdditionXMLElementTagName());

        out.writeAttribute("ID", getID());
        
        out.writeEndElement();
    }
    
    
    /**
     * Sets the ID of the super class to be <code>serverID</code>.
     */
    public void updateID() {
        setID(serverID);
    }
    
    
    public void readFromServerAdditionElement(XMLStreamReader in) throws XMLStreamException {
        serverID = in.getAttributeValue(null, "ID");
        in.nextTag();
    }
    
    
    /**
    * Returns the tag name of the root element representing this object.
    * @return the tag name.
    */
    public static String getServerAdditionXMLElementTagName() {
        return "serverPlayer";
    }
}
