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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.option.BooleanOption;


/**
* A <code>Player</code> with additional (server specific) information.
*
* That is: pointers to this player's
* {@link Connection} and {@link Socket}
*/
public class ServerPlayer extends Player implements ServerModelObject {
    
    private static final Logger logger = Logger.getLogger(ServerPlayer.class.getName());

    public static final int SCORE_INDEPENDENCE_GRANTED = 1000;

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

    /**
     * Checks if this player has died.
     *
     * @return <i>true</i> if this player should die.
     */
    public boolean checkForDeath() {
        /*
         * Die if: (isNative && (no colonies or units))
         *      || ((rebel or independent) && !(has coastal colony))
         *      || (isREF && !(rebel nation left) && (all units in Europe))
         *      || ((no units in New World)
         *         && ((year > 1600) || (cannot get a unit from Europe)))
         */

        switch (getPlayerType()) {
        case NATIVE: // All natives units are viable
            return getUnits().isEmpty();

        case COLONIAL: // Handle the hard case below
            break;

        case REBEL: case INDEPENDENT:
            // Post-declaration European player needs a coastal colony
            // and can not hope for resupply from Europe.
            for (Colony colony : getColonies()) {
                if (colony.isConnected()) return false;
            }
            return true;

        case ROYAL: // Still alive if there are rebels to quell
            Iterator<Player> players = getGame().getPlayerIterator();
            while (players.hasNext()) {
                Player enemy = players.next();
                if (enemy.getREFPlayer() == (Player) this
                    && enemy.getPlayerType() == PlayerType.REBEL) {
                    return false;
                }
            }

            // Still alive if there are units not in Europe
            Iterator<Unit> units = getUnitIterator();
            while (units.hasNext()) {
                if (!units.next().isInEurope()) {
                    return false;
                }
            }

            // Otherwise, the REF has been defeated and gone home.
            return true;

        case UNDEAD:
            return getUnits().isEmpty();

        default:
            throw new IllegalStateException("Bogus player type");
        }

        // Quick check for a colony
        if (!getColonies().isEmpty()) {
            return false;
        }

        // Verify player units
        boolean hasCarrier = false;
        List<Unit> unitList = getUnits();
        for(Unit unit : unitList){
            boolean isValidUnit = false;

            if(unit.isCarrier()){
                hasCarrier = true;
                continue;
            }

            // Can found new colony
            if(unit.isColonist()){
                isValidUnit = true;
            }

            // Can capture units
            if(unit.isOffensiveUnit()){
                isValidUnit = true;
            }

            if(!isValidUnit){
                continue;
            }

            // Verify if unit is in new world
            Location unitLocation = unit.getLocation();
            // unit in new world
            if(unitLocation instanceof Tile){
                logger.info(getName() + " found colonist in new world");
                return false;
            }
            // onboard a carrier
            if(unit.isOnCarrier()){
                Unit carrier = (Unit) unitLocation;
                // carrier in new world
                if(carrier.getLocation() instanceof Tile){
                    logger.info(getName() + " found colonist aboard carrier in new world");
                    return false;
                }
            }
        }
        /*
         * At this point we know the player does not have any valid units or
         * settlements on the map.
         */

        // After the year 1600, no presence in New World means endgame
        if (getGame().getTurn().getYear() >= 1600) {
            logger.info(getName() + " no presence in new world after 1600");
            return true;
        }

        int goldNeeded = 0;
        /*
         * No carrier, check if has gold to buy one
         */
        if(!hasCarrier){
            /*
             * Find the cheapest naval unit
             */

            Iterator<UnitType> navalUnits = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.navalUnit").iterator();

            int lowerPrice = Integer.MAX_VALUE;

            while(navalUnits.hasNext()){
                UnitType unit = navalUnits.next();

                int unitPrice = getEurope().getUnitPrice(unit);

                // cannot be bought
                if(unitPrice == UnitType.UNDEFINED){
                    continue;
                }

                if(unitPrice < lowerPrice){
                    lowerPrice = unitPrice;
                }
            }

            //Sanitation
            if(lowerPrice == Integer.MAX_VALUE){
                logger.warning(getName() + " could not find naval unit to buy");
                return true;
            }

            goldNeeded += lowerPrice;

            // cannot buy carrier
            if(goldNeeded > getGold()){
                logger.info(getName() + " does not have enough money to buy carrier");
                return true;
            }
            logger.info(getName() + " has enough money to buy carrier, has=" + getGold() + ", needs=" + lowerPrice);
        }

        /*
         * Check if player has colonists.
         * We already checked that it has (or can buy) a carrier to
         * transport them to New World
         */
        Iterator<Unit> unitIterator = getEurope().getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = unitIterator.next();
            if (unit.isCarrier()) {
                /*
                 * The carrier has colonist units on board
                 */
                for(Unit u : unit.getUnitList()){
                    if(u.isColonist()){
                        return false;
                    }
                }

                // The carrier has units or goods that can be sold.
                if(unit.getGoodsCount() > 0){
                    logger.info(getName() + " has goods to sell");
                    return false;
                }
                continue;
            }
            if (unit.isColonist()){
                logger.info(getName() + " has colonist unit waiting in port");
                return false;
            }
        }

        // No colonists, check if has gold to train or recruit one.
        int goldToRecruit =  getEurope().getRecruitPrice();

        /*
         * Find the cheapest colonist, either by recruiting or training
         */

        Iterator<UnitType> trainedUnits = FreeCol.getSpecification().getUnitTypesTrainedInEurope().iterator();

        int goldToTrain = Integer.MAX_VALUE;

        while(trainedUnits.hasNext()){
            UnitType unit = trainedUnits.next();

            if(!unit.hasAbility("model.ability.foundColony")){
                continue;
            }

            int unitPrice = getEurope().getUnitPrice(unit);

            // cannot be bought
            if(unitPrice == UnitType.UNDEFINED){
                continue;
            }

            if(unitPrice < goldToTrain){
                goldToTrain = unitPrice;
            }
        }

        goldNeeded += Math.min(goldToTrain, goldToRecruit);

        if (goldNeeded <= getGold()) return false;
        // Does not have enough money for recruiting or training
        logger.info(getName() + " does not have enough money for recruiting or training");
        return true;
    }


    public int getRemainingEmigrants() {
        return remainingEmigrants;
    }
    
    public void setRemainingEmigrants(int emigrants) {
        remainingEmigrants = emigrants;
    }

    /**
     * Sever ties with the European homeland.
     *
     * @return A list of objects disposed.
     */
    public List<Object> severEurope() {
        List<Object> objects = new ArrayList<Object>();
        objects.addAll(europe.disposeList());
        europe = null;
        objects.add(monarch);
        monarch = null;
        return objects;
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
