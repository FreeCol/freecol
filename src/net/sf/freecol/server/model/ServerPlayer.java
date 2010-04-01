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


package net.sf.freecol.server.model;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.util.Utils;


/**
* A <code>Player</code> with additional (server specific) information.
*
* That is: pointers to this player's
* {@link Connection} and {@link Socket}
*/
public class ServerPlayer extends Player implements ServerModelObject {
    
    /** The network socket to the player's client. */
    private Socket socket;

    /** The connection for this player. */
    private Connection connection;

    private boolean connected = false;

    /** Remaining emigrants to select due to a fountain of youth */
    private int remainingEmigrants = 0;

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
    public ServerPlayer(Game game, String name, boolean admin, boolean ai, Socket socket, Connection connection,
                        Nation nation) {
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

    public int getRemainingEmigrants() {
        return remainingEmigrants;
    }
    
    public void setRemainingEmigrants(int emigrants) {
        remainingEmigrants = emigrants;
    }

    /**
     * Declare independence.
     *
     * @param nationName The new name for the independent nation.
     * @param countryName The new name for its residents.
     */
    public List<FreeColObject> declareIndependence(String nationName,
                                                   String countryName) {
        ArrayList<FreeColObject> result = new ArrayList<FreeColObject>();
        // Cross the Rubicon
        setIndependentNationName(nationName);
        setNewLandName(countryName);
        setPlayerType(PlayerType.REBEL);
        getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        modifyScore(SCORE_INDEPENDENCE_DECLARED);
        history.add(new HistoryEvent(getGame().getTurn().getNumber(),
                                     HistoryEvent.EventType.DECLARE_INDEPENDENCE));

        // Clean up unwanted connections
        divertModelMessages(europe, null);

        // Dispose of units in Europe.
        List<FreeColGameObject> objects = europe.disposeList();
        result.addAll(objects);
        europe = null;
        monarch = null; // "No more kings"
        StringTemplate seized = StringTemplate.label(", ");
        for (FreeColGameObject o : objects) {
            if (o instanceof Unit) {
                seized.addStringTemplate(((Unit) o).getLabel());
            }
        }
        if (!seized.getReplacements().isEmpty()) {
            result.add(new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                        "model.player.independence.unitsSeized", this)
                       .addStringTemplate("%units%", seized));
        }

        // Generalized continental army muster
        java.util.Map<UnitType, UnitType> upgrades = new HashMap<UnitType, UnitType>();
        for (UnitType unitType : Specification.getSpecification().getUnitTypeList()) {
            UnitType upgrade = unitType.getUnitTypeChange(ChangeType.INDEPENDENCE, this);
            if (upgrade != null) {
                upgrades.put(unitType, upgrade);
            }
        }

        for (Colony colony : getColonies()) {
            int sol = colony.getSoL();
            if (sol > 50) {
                java.util.Map<UnitType, List<Unit>> unitMap = new HashMap<UnitType, List<Unit>>();
                List<Unit> allUnits = new ArrayList<Unit>(colony.getTile().getUnitList());
                allUnits.addAll(colony.getUnitList());
                for (Unit unit : allUnits) {
                    if (upgrades.containsKey(unit.getType())) {
                        List<Unit> unitList = unitMap.get(unit.getType());
                        if (unitList == null) {
                            unitList = new ArrayList<Unit>();
                            unitMap.put(unit.getType(), unitList);
                        }
                        unitList.add(unit);
                    }
                }
                for (Entry<UnitType, List<Unit>> entry : unitMap.entrySet()) {
                    int limit = (entry.getValue().size() + 2) * (sol - 50) / 100;
                    if (limit > 0) {
                        for (int index = 0; index < limit; index++) {
                            Unit unit = entry.getValue().get(index);
                            if (unit == null) break;
                            unit.setType(upgrades.get(entry.getKey()));
                            result.add(unit);
                        }
                        result.add(new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                                    "model.player.continentalArmyMuster",
                                                    this, colony)
                                   .addName("%colony%", colony.getName())
                                   .addAmount("%number%", limit)
                                   .add("%oldUnit%", entry.getKey().getNameKey())
                                   .add("%unit%", upgrades.get(entry.getKey()).getNameKey()));
                    }
                }
            }
        }

        // inelegant, but a lot happens here, once
        result.add(this);
        return result;
    }

    /**
     * Gives independence to this player.
     */
    public List<ModelMessage> giveIndependence(ServerPlayer REFplayer) {
        ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
        setPlayerType(PlayerType.INDEPENDENT);
        modifyScore(SCORE_INDEPENDENCE_GRANTED - getGame().getTurn().getNumber());
        setTax(0);
        reinitialiseMarket();
        getHistory().add(new HistoryEvent(getGame().getTurn().getNumber(),
                                          HistoryEvent.EventType.INDEPENDENCE));
        messages.add(new ModelMessage("model.player.independence", this));
        ArrayList<Unit> surrenderUnits = new ArrayList<Unit>();
        for (Unit u : REFplayer.getUnits()) {
            if (!u.isNaval()) surrenderUnits.add(u);
        }
        StringTemplate surrender = StringTemplate.label(", ");
        for (Unit u : surrenderUnits) {
            if (u.getType().hasAbility("model.ability.refUnit")) {
                // Make sure the independent player does not end up owning
                // any Kings Regulars!
                UnitType downgrade = u.getType().getUnitTypeChange(ChangeType.CAPTURE, this);
                if (downgrade != null) u.setType(downgrade);
            }
            u.setOwner(this);
            surrender.addStringTemplate(u.getLabel());
        }
        messages.add(new ModelMessage("model.player.independence.unitsAcquired", this)
                     .addStringTemplate("%units%", surrender));
        return messages;
    }

    /**
     * Add a HistoryEvent to this player.
     *
     * @param event The <code>HistoryEvent</code> to add.
     */
    public void addHistory(HistoryEvent event) {
        history.add(event);
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
            Iterator<Unit> unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();

                setExplored(unit.getTile());

                Iterator<Position> positionIterator;
                if (unit.getColony() != null) {
                    positionIterator = map.getCircleIterator(unit.getTile().getPosition(), true, 2);
                } else {
                    positionIterator = map.getCircleIterator(unit.getTile().getPosition(), true, unit.getLineOfSight());
                }

                while (positionIterator.hasNext()) {
                    Map.Position p = positionIterator.next();
                    setExplored(map.getTile(p));
                }
            }

        }

    }

    /**
     * Checks if this <code>Player</code> has explored the given
     * <code>Tile</code>.
     *
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

        Iterator<Position> positionIterator = getGame().getMap().getCircleIterator(unit.getTile().getPosition(), true, unit.getLineOfSight());
        while (positionIterator.hasNext()) {
            Map.Position p = positionIterator.next();
            if (p == null) {
                continue;
            }
            setExplored(getGame().getMap().getTile(p));
            if (canSeeTiles != null) {
                canSeeTiles[p.getX()][p.getY()] = true;
            } else {
                invalidateCanSeeTiles();
            }
        }
    }
    

    /**
    * (DEBUG ONLY) Makes the entire map visible.
    */
    public void revealMap() {
        Iterator<Position> positionIterator = getGame().getMap().getWholeMapIterator();

        while (positionIterator.hasNext()) {
            Map.Position p = positionIterator.next();
            setExplored(getGame().getMap().getTile(p));
        }
        
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

        out.writeAttribute("ID", getId());
        
        out.writeEndElement();
    }
    
    
    /**
     * Sets the ID of the super class to be <code>serverID</code>.
     */
    public void updateID() {
        setId(serverID);
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
    
    @Override
    public String toString() {
        return "ServerPlayer[name="+getName()+",serverID=" + serverID + ",conn=" + connection + "]";
    }
}
