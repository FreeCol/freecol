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
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerEurope;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerModelObject;
import net.sf.freecol.server.model.ServerUnit;


/**
* A <code>Player</code> with additional (server specific) information.
*
* That is: pointers to this player's
* {@link Connection} and {@link Socket}
*/
public class ServerPlayer extends Player implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerPlayer.class.getName());

    /** The network socket to the player's client. */
    private Socket socket;

    /** The connection for this player. */
    private Connection connection;

    private boolean connected = false;

    /** Remaining emigrants to select due to a fountain of youth */
    private int remainingEmigrants = 0;


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerPlayer(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerPlayer.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param name The player name.
     * @param admin Whether the player is the game administrator or not.
     * @param nation The nation of the <code>Player</code>.
     * @param socket The socket to the player's client.
     * @param connection The <code>Connection</code> for the socket.
     */
    public ServerPlayer(Game game, String name, boolean admin, Nation nation,
                        Socket socket, Connection connection) {
        super(game);

        this.name = name;
        this.admin = admin;
        featureContainer = new FeatureContainer(game.getSpecification());
        europe = null;
        if (nation != null && nation.getType() != null) {
            this.nationType = nation.getType();
            this.nationID = nation.getId();
            try {
                featureContainer.add(nationType.getFeatureContainer());
            } catch (Throwable error) {
                error.printStackTrace();
            }
            if (nationType.isEuropean()) {
                /*
                 * Setting the amount of gold to
                 * "getGameOptions().getInteger(GameOptions.STARTING_MONEY)"
                 *
                 * just before starting the game. See
                 * "net.sf.freecol.server.control.PreGameController".
                 */
                gold = 0;
                europe = new ServerEurope(game, this);
                playerType = (nationType.isREF()) ? PlayerType.ROYAL
                    : PlayerType.COLONIAL;
                if (playerType == PlayerType.COLONIAL) {
                    monarch = new Monarch(game, this, nation.getRulerNameKey());
                }
            } else { // indians
                gold = 1500;
                playerType = PlayerType.NATIVE;
            }
        } else {
            // virtual "enemy privateer" player
            // or undead ?
            this.nationID = Nation.UNKNOWN_NATION_ID;
            this.playerType = PlayerType.COLONIAL;
        }
        market = new Market(getGame(), this);
        immigration = 0;
        liberty = 0;
        currentFather = null;

        //call of super() will lead to this object being registered with AIMain
        //before playerType has been set. AIMain will fall back to use of
        //standard AIPlayer in this case. Set object again to fix this.
        //Possible TODO: Is there a better way to do this?
        final String curId = getId();
        game.removeFreeColGameObject(curId);
        game.setFreeColGameObject(curId, this);

        this.socket = socket;
        this.connection = connection;
        connected = connection != null;

        resetExploredTiles(getGame().getMap());
        resetCanSeeTiles();
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
     * Gets the socket of this player.
     * @return The <code>Socket</code>.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Gets the connection of this player.
     *
     * @return The <code>Connection</code>.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Sets the connection of this player.
     *
     * @param connection The <code>Connection</code>.
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
        connected = (connection != null);
    }

    /**
     * Checks if this player has died.
     *
     * @return True if this player should die.
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
            if (isUnknownEnemy()) return false;
            break;

        case REBEL: case INDEPENDENT:
            // Post-declaration European player needs a coastal colony
            // and can not hope for resupply from Europe.
            for (Colony colony : getColonies()) {
                if (colony.isConnected()) return false;
            }
            return true;

        case ROYAL: // Still alive if there are rebels to quell
            for (Player enemy : getGame().getPlayers()) {
                if (enemy.getREFPlayer() == (Player) this
                    && enemy.getPlayerType() == PlayerType.REBEL) {
                    return false;
                }
            }

            // Still alive if there are units not in Europe
            for (Unit u : getUnits()) {
                if (!u.isInEurope()) return false;
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

            Iterator<UnitType> navalUnits = getSpecification()
                .getUnitTypesWithAbility("model.ability.navalUnit").iterator();

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
        for (Unit eu : getEurope().getUnitList()) {
            if (eu.isCarrier()) {
                /*
                 * The carrier has colonist units on board
                 */
                for (Unit u : eu.getUnitList()) {
                    if (u.isColonist()) return false;
                }

                // The carrier has units or goods that can be sold.
                if (eu.getGoodsCount() > 0) {
                    logger.info(getName() + " has goods to sell");
                    return false;
                }
                continue;
            }
            if (eu.isColonist()) {
                logger.info(getName() + " has colonist unit waiting in port");
                return false;
            }
        }

        // No colonists, check if has gold to train or recruit one.
        int goldToRecruit = getEurope().getRecruitPrice();

        /*
         * Find the cheapest colonist, either by recruiting or training
         */

        Iterator<UnitType> trainedUnits = getSpecification().getUnitTypesTrainedInEurope().iterator();

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
     * Checks whether the current founding father has been recruited.
     *
     * @return The new founding father, or null if none available or ready.
     */
    public FoundingFather checkFoundingFather() {
        FoundingFather father = null;
        if (currentFather != null) {
            int extraLiberty = getRemainingFoundingFatherCost();
            if (extraLiberty <= 0) {
                boolean overflow = getSpecification()
                    .getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW);
                setLiberty((overflow) ? -extraLiberty : 0);
                father = currentFather;
                currentFather = null;
            }
        }
        return father;
    }

    /**
     * Checks whether to start recruiting a founding father.
     *
     * @return True if a new father should be chosen.
     */
    public boolean canRecruitFoundingFather() {
        return getPlayerType() == PlayerType.COLONIAL
            && canHaveFoundingFathers()
            && currentFather == null
            && !getSettlements().isEmpty();
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
            for (Unit unit : getUnits()) {
                setExplored(unit.getTile());

                int radius;
                if (unit.getColony() != null) {
                    radius = 2; // TODO: magic number
                } else {
                    radius = unit.getLineOfSight();
                }

                for (Tile tile: unit.getTile().getSurroundingTiles(radius)) {
                    setExplored(tile);
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
        canSeeTiles[unit.getTile().getX()][unit.getTile().getY()] = true;

        for (Tile tile: unit.getTile().getSurroundingTiles(unit.getLineOfSight())) {
            setExplored(tile);
            if (canSeeTiles != null) {
                canSeeTiles[tile.getX()][tile.getY()] = true;
            } else {
                invalidateCanSeeTiles();
            }
        }
    }


    /**
     * Makes the entire map visible.
     * Debug mode helper.
     */
    public void revealMap() {
        for (Tile tile: getGame().getMap().getAllTiles()) {
            setExplored(tile);
        }
        getSpecification().getBooleanOption(GameOptions.FOG_OF_WAR).setValue(false);
        resetCanSeeTiles();
    }

    /**
     * Propagate an European market change to the other European markets.
     *
     * @param type The type of goods that was traded.
     * @param amount The amount of goods that was traded.
     * @param random A <code>Random</code> number source.
     */
    private void propagateToEuropeanMarkets(GoodsType type, int amount,
                                            Random random) {
        if (!type.isStorable()) return;

        // Propagate 5-30% of the original change.
        final int lowerBound = 5; // TODO: make into game option?
        final int upperBound = 30;// TODO: make into game option?
        amount *= random.nextInt(upperBound - lowerBound + 1) + lowerBound;
        amount /= 100;
        if (amount == 0) return;

        // Do not need to update the clients here, these changes happen
        // while it is not their turn.
        for (Player other : getGame().getEuropeanPlayers()) {
            Market market;
            if ((ServerPlayer) other != this
                && (market = other.getMarket()) != null) {
                market.addGoodsToMarket(type, amount);
            }
        }
    }

    /**
     * Buy goods in Europe.
     * Do not update the container or player in the ChangeSet, this
     * routine is called from higher level routines where other updates
     * happen.
     *
     * @param container The <code>GoodsContainer</code> to carry the goods.
     * @param type The <code>GoodsType</code> to buy.
     * @param amount The amount of goods to buy.
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     * @throws IllegalStateException If the <code>player</code> cannot afford
     *                               to buy the goods.
     */
    public void csBuy(GoodsContainer container, GoodsType type, int amount,
                      Random random, ChangeSet cs)
        throws IllegalStateException {
        Market market = getMarket();
        int price = market.getBidPrice(type, amount);
        if (price > getGold()) {
            throw new IllegalStateException("Player " + getName()
                + " tried to buy " + Integer.toString(amount)
                + " " + type.toString()
                + " for " + Integer.toString(price)
                + " but has " + Integer.toString(getGold()) + " gold.");
        }
        modifyGold(-price);
        market.modifySales(type, -amount);
        market.modifyIncomeBeforeTaxes(type, -price);
        market.modifyIncomeAfterTaxes(type, -price);
        int marketAmount = -(int) getFeatureContainer()
            .applyModifier(amount, "model.modifier.tradeBonus",
                           type, getGame().getTurn());
        market.addGoodsToMarket(type, marketAmount);
        propagateToEuropeanMarkets(type, marketAmount, random);

        container.addGoods(type, amount);
        if (market.hasPriceChanged(type)) {
            // This type of goods has changed price, so we will update
            // the market and send a message as well.
            cs.addMessage(See.only(this),
                          market.makePriceChangeMessage(type));
            market.flushPriceChange(type);
            cs.add(See.only(this), market.getMarketData(type));
        }
    }

    /**
     * Sell goods in Europe.
     * Do not update the container or player in the ChangeSet, this
     * routine is called from higher level routines where other updates
     * happen.
     *
     * @param container An optional <code>GoodsContainer</code>
     *     carrying the goods.
     * @param type The <code>GoodsType</code> to sell.
     * @param amount The amount of goods to sell.
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csSell(GoodsContainer container, GoodsType type, int amount,
                       Random random, ChangeSet cs) {
        Market market = getMarket();
        int tax = getTax();
        int incomeBeforeTaxes = market.getSalePrice(type, amount);
        int incomeAfterTaxes = ((100 - tax) * incomeBeforeTaxes) / 100;
        modifyGold(incomeAfterTaxes);
        market.modifySales(type, amount);
        market.modifyIncomeBeforeTaxes(type, incomeBeforeTaxes);
        market.modifyIncomeAfterTaxes(type, incomeAfterTaxes);
        int marketAmount = (int) getFeatureContainer()
            .applyModifier(amount, "model.modifier.tradeBonus",
                           type, getGame().getTurn());
        market.addGoodsToMarket(type, marketAmount);
        propagateToEuropeanMarkets(type, amount, random);

        if (container != null) container.addGoods(type, -amount);
        if (market.hasPriceChanged(type)) {
            // This type of goods has changed price, so update the
            // market and send a message as well.
            cs.addMessage(See.only(this),
                          market.makePriceChangeMessage(type));
            market.flushPriceChange(type);
            cs.add(See.only(this), market.getMarketData(type));
        }
    }

    /**
     * New turn for this player.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerPlayer.csNewTurn, for " + toString());

        // Settlements
        List<Settlement> settlements
            = new ArrayList<Settlement>(getSettlements());
        int newSoL = 0;
        for (Settlement settlement : settlements) {
            ((ServerModelObject) settlement).csNewTurn(random, cs);
            newSoL += settlement.getSoL();
        }
        int numberOfColonies = settlements.size();
        if (numberOfColonies > 0) {
            newSoL = newSoL / numberOfColonies;
            if (oldSoL / 10 != newSoL / 10) {
                cs.addMessage(See.only(this),
                    new ModelMessage(ModelMessage.MessageType.SONS_OF_LIBERTY,
                                     (newSoL > oldSoL)
                                     ? "model.player.SoLIncrease"
                                     : "model.player.SoLDecrease", this)
                              .addAmount("%oldSoL%", oldSoL)
                              .addAmount("%newSoL%", newSoL));
            }
            oldSoL = newSoL; // Remember SoL for check changes at next turn.
        }

        // Europe.
        if (europe != null) {
            ((ServerModelObject) europe).csNewTurn(random, cs);
        }
        // Units.
        for (Unit unit : new ArrayList<Unit>(getUnits())) {
            ((ServerModelObject) unit).csNewTurn(random, cs);
        }
    }

    @Override
    public String toString() {
        return "ServerPlayer[name=" + getName() + ",ID=" + getId()
            + ",conn=" + connection + "]";
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverPlayer"
     */
    public String getServerXMLElementTagName() {
        return "serverPlayer";
    }
}
