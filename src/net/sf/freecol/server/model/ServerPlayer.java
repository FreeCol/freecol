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


package net.sf.freecol.server.model;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Europe.MigrationType;
import net.sf.freecol.common.model.Event;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
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
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
* A <code>Player</code> with additional (server specific) information.
*
* That is: pointers to this player's
* {@link Connection} and {@link Socket}
*/
public class ServerPlayer extends Player implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerPlayer.class.getName());

    // TODO: move to options or spec?
    public static final int ALARM_RADIUS = 2;
    public static final int ALARM_TILE_IN_USE = 2;
    public static final int ALARM_MISSIONARY_PRESENT = -10;

    // How far to search for a colony to add an Indian convert to.
    public static final int MAX_CONVERT_DISTANCE = 10;


    /** The network socket to the player's client. */
    private Socket socket;

    /** The connection for this player. */
    private Connection connection;

    private boolean connected = false;

    /** Remaining emigrants to select due to a fountain of youth */
    private int remainingEmigrants = 0;

    /** Players with respect to which stance has changed. */
    private List<ServerPlayer> stanceDirty = new ArrayList<ServerPlayer>();


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
        featureContainer = new FeatureContainer();
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
                this.playerType = (nationType.isREF()) ? PlayerType.ROYAL
                    : PlayerType.COLONIAL;
                europe = new ServerEurope(game, this);
                if (this.playerType == PlayerType.COLONIAL) {
                    monarch = new Monarch(game, this, nation.getRulerNameKey());
                }
                gold = 0;
            } else { // indians
                this.playerType = PlayerType.NATIVE;
                gold = Player.GOLD_NOT_ACCOUNTED;
            }
        } else {
            // virtual "enemy privateer" player
            // or undead ?
            this.nationID = Nation.UNKNOWN_NATION_ID;
            this.playerType = PlayerType.COLONIAL;
            gold = 0;
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
        invalidateCanSeeTiles();
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
     * Performs initial randomizations for this player.
     *
     * @param random A pseudo-random number source.
     */
    public void startGame(Random random) {
        Specification spec = getGame().getSpecification();
        if (isEuropean() && !isREF()) {
            modifyGold(spec.getIntegerOption(GameOptions.STARTING_MONEY)
                       .getValue());
            ((ServerEurope) getEurope()).initializeMigration(random);
            getMarket().randomizeInitialPrice(random);
        }
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

        case ROYAL:
            return getRebels().isEmpty();

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

        // After the season cutover year, no presence in New World
        // means death
        if (getGame().getTurn().getYear() >= Turn.SEASON_YEAR) {
            logger.info(getName() + " no presence in new world after "
                        + Turn.SEASON_YEAR);
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
                .getUnitTypesWithAbility(Ability.NAVAL_UNIT).iterator();

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
            if (!checkGold(goldNeeded)) {
                logger.info(getName() + " does not have enough money to buy carrier");
                return true;
            }
            logger.info(getName() + " has enough money to buy carrier, has="
                        + getGold() + ", needs=" + lowerPrice);
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

        if (checkGold(goldNeeded)) return false;
        // Does not have enough money for recruiting or training
        logger.info(getName() + " does not have enough money for recruiting or training");
        return true;
    }

    /**
     * Check if a REF player has been defeated and should surrender.
     *
     * @return True if this REF player has been defeated.
     */
    public boolean checkForREFDefeat() {
        if (!isREF()) {
            throw new IllegalStateException("Checking for REF player defeat when player not REF.");
        }

        // No one to fight?  Either the rebels are dead, or the REF
        // was already defeated and the rebels are independent.
        // Either way, it does not need to surrender.
        if (getRebels().isEmpty()) return false;

        // Not defeated if there are settlements.
        if (!getSettlements().isEmpty()) return false;

        // Not defeated if there is a non-zero navy and enough land units.
        final int landREFUnitsRequired = 7; // TODO: magic number
        final CombatModel cm = getGame().getCombatModel();
        boolean naval = false;
        int land = 0;
        int power = 0;
        for (Unit u : getUnits()) {
            if (u.isNaval()) naval = true; else {
                if (u.hasAbility("model.ability.refUnit")) {
                    land++;
                    power += cm.getOffencePower(u, null);
                }
            }
        }
        if (naval && land >= landREFUnitsRequired) return false;

        // Still not defeated as long as military strength is greater
        // than the rebels.
        int rebelPower = 0;
        for (Player rebel : getRebels()) {
            for (Unit r : rebel.getUnits()) {
                if (!r.isNaval()) rebelPower += cm.getOffencePower(r, null);
            }
        }
        if (power > rebelPower) return false;

        // REF is defeated
        return true;
    }

    /**
     * Kills the missionary in a settlement.
     *
     * @param settlement The <code>IndianSettlement</code> to kill the
     *     missionary from.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csKillMissionary(IndianSettlement settlement, ChangeSet cs) {
        Unit missionary = settlement.getMissionary();
        settlement.changeMissionary(null);

        // Inform the enemy of loss of mission
        cs.add(See.only(this), settlement);
        cs.addDispose(See.perhaps().always(this),
            settlement.getTile(), missionary);
        cs.addMessage(See.only(this),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                "indianSettlement.mission.denounced", settlement)
            .addName("%settlement%", settlement.getNameFor(this)));
    }

    /**
     * Kill off a player and clear out its remains.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csKill(ChangeSet cs) {
        setDead(true);
        cs.addPartial(See.all(), this, "dead");
        cs.addDead(this);

        // Clean up missions and remove tension/alarm/stance.
        for (Player other : getGame().getPlayers()) {
            if (other == this) continue;
            if (isEuropean() && other.isIndian()) {
                for (IndianSettlement s : other.getIndianSettlements()) {
                    Unit unit = s.getMissionary();
                    if (unit != null
                        && ((ServerPlayer) unit.getOwner()) == this) {
                        csKillMissionary(s, cs);
                    }
                    s.removeAlarm(this);
                }
                other.removeTension(this);
            }
            other.setStance(this, null);
        }

        // Remove settlements.  Update formerly owned tiles.
        List<Settlement> settlements = getSettlements();
        while (!settlements.isEmpty()) {
            Settlement settlement = settlements.remove(0);
            cs.addDispose(See.perhaps().always(this),
                settlement.getTile(), settlement);
        }

        // Clean up remaining tile ownerships
        for (Tile tile : getGame().getMap().getAllTiles()) {
            if (tile.getOwner() == this) {
                tile.changeOwnership(null, null);
                cs.add(See.perhaps().always(this), tile);
            }
        }

        // Remove units
        List<Unit> units = getUnits();
        while (!units.isEmpty()) {
            Unit unit = units.remove(0);
            if (unit.getLocation() instanceof Tile) {
                cs.add(See.perhaps().always(this), unit.getTile());
            }
            cs.addDispose(See.perhaps().always(this),
                unit.getLocation(), unit);
        }
    }

    /**
     * Withdraw a player from the new world.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csWithdraw(ChangeSet cs) {
        cs.addMessage(See.all(),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                ((isEuropean() && getPlayerType() == PlayerType.COLONIAL)
                    ? "model.diplomacy.dead.european"
                    : "model.diplomacy.dead.native"),
                this)
            .addStringTemplate("%nation%", getNationName()));
        Game game = getGame();
        cs.addGlobalHistory(game,
            new HistoryEvent(game.getTurn(),
                HistoryEvent.EventType.NATION_DESTROYED)
            .addStringTemplate("%nation%", getNationName()));
        csKill(cs);
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
     * Build a list of random FoundingFathers, one per type.
     * Do not include any the player has or are not available.
     *
     * @param random A pseudo-random number source.
     * @return A list of FoundingFathers.
     */
    private List<FoundingFather> getRandomFoundingFathers(Random random) {
        // Build weighted random choice for each father type
        Specification spec = getGame().getSpecification();
        int age = getGame().getTurn().getAge();
        EnumMap<FoundingFatherType, List<RandomChoice<FoundingFather>>> choices
            = new EnumMap<FoundingFatherType,
                List<RandomChoice<FoundingFather>>>(FoundingFatherType.class);
        for (FoundingFather father : spec.getFoundingFathers()) {
            if (!hasFather(father) && father.isAvailableTo(this)) {
                FoundingFatherType type = father.getType();
                List<RandomChoice<FoundingFather>> rc = choices.get(type);
                if (rc == null) {
                    rc = new ArrayList<RandomChoice<FoundingFather>>();
                }
                int weight = father.getWeight(age);
                rc.add(new RandomChoice<FoundingFather>(father, weight));
                choices.put(father.getType(), rc);
            }
        }

        // Select one from each father type
        List<FoundingFather> randomFathers = new ArrayList<FoundingFather>();
        String logMessage = "Random fathers";
        for (FoundingFatherType type : FoundingFatherType.values()) {
            List<RandomChoice<FoundingFather>> rc = choices.get(type);
            if (rc != null) {
                FoundingFather f = RandomChoice.getWeightedRandom(logger,
                    "Choose founding father", random, rc);
                if (f != null) {
                    randomFathers.add(f);
                    logMessage += ":" + f.getNameKey();
                }
            }
        }
        logger.info(logMessage);
        return randomFathers;
    }

    /**
     * Generate a weighted list of unit types recruitable by this player.
     *
     * @return A weighted list of recruitable unit types.
     */
    public List<RandomChoice<UnitType>> generateRecruitablesList() {
        ArrayList<RandomChoice<UnitType>> recruitables
            = new ArrayList<RandomChoice<UnitType>>();
        FeatureContainer fc = getFeatureContainer();
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isRecruitable()
                && fc.hasAbility("model.ability.canRecruitUnit", unitType)) {
                recruitables.add(new RandomChoice<UnitType>(unitType,
                        unitType.getRecruitProbability()));
            }
        }
        return recruitables;
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
                Tile tile = unit.getTile();
                setExplored(tile);

                int radius = (unit.getColony() != null)
                    ? unit.getColony().getLineOfSight()
                    : unit.getLineOfSight();
                for (Tile t : tile.getSurroundingTiles(radius)) {
                    setExplored(t);
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
     * Sets the given tile to be explored by this player and updates
     * the player's information about the tile.
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
        if (getGame() == null || getGame().getMap() == null || unit == null
            || unit.getLocation() == null || unit.getTile() == null) {
            return;
        }

        Tile tile = unit.getTile();
        setExplored(tile);
        for (Tile t : tile.getSurroundingTiles(unit.getLineOfSight())) {
            setExplored(t);
        }
        invalidateCanSeeTiles();
    }

    /**
     * Create units from a list of abstract units.  Only used by
     * Europeans at present, so the units are created in Europe.
     *
     * @param abstractUnits The list of <code>AbstractUnit</code>s to create.
     * @return A list of units created.
     */
    public List<Unit> createUnits(List<AbstractUnit> abstractUnits) {
        Game game = getGame();
        Specification spec = game.getSpecification();
        List<Unit> units = new ArrayList<Unit>();
        Europe europe = getEurope();
        if (europe == null) return units;

        for (AbstractUnit au : abstractUnits) {
            for (int i = 0; i < au.getNumber(); i++) {
                units.add(new ServerUnit(game, europe, this,
                                         au.getUnitType(spec),
                                         UnitState.ACTIVE,
                                         au.getEquipment(spec)));
            }
        }
        return units;
    }

    /**
     * Makes the entire map visible.
     * Debug mode helper.
     */
    public void revealMap() {
        for (Tile tile: getGame().getMap().getAllTiles()) {
            setExplored(tile);
        }
        getSpecification().getBooleanOption(GameOptions.FOG_OF_WAR)
            .setValue(false);
        invalidateCanSeeTiles();
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
        amount *= Utils.randomInt(logger, "Propagate goods", random,
                                  upperBound - lowerBound + 1) + lowerBound;
        amount /= 100;
        if (amount == 0) return;

        // Do not need to update the clients here, these changes happen
        // while it is not their turn.
        for (Player other : getGame().getLiveEuropeanPlayers()) {
            Market market;
            if ((ServerPlayer) other != this
                && (market = other.getMarket()) != null) {
                market.addGoodsToMarket(type, amount);
            }
        }
    }

    /**
     * Flush any market price changes for a specified goods type.
     *
     * @param type The <code>GoodsType</code> to check.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csFlushMarket(GoodsType type, ChangeSet cs) {
        Market market = getMarket();
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
     * Buy goods in Europe.
     *
     * @param container The <code>GoodsContainer</code> to carry the goods.
     * @param type The <code>GoodsType</code> to buy.
     * @param amount The amount of goods to buy.
     * @param random A <code>Random</code> number source.
     * @throws IllegalStateException If the <code>player</code> cannot afford
     *                               to buy the goods.
     */
    public void buy(GoodsContainer container, GoodsType type, int amount,
                    Random random)
        throws IllegalStateException {
        logger.finest(getName() + " buys " + amount + " " + type);
        Market market = getMarket();
        int price = market.getBidPrice(type, amount);
        if (!checkGold(price)) {
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
    }

    /**
     * Sell goods in Europe.
     *
     * @param container An optional <code>GoodsContainer</code>
     *     carrying the goods.
     * @param type The <code>GoodsType</code> to sell.
     * @param amount The amount of goods to sell.
     * @param random A <code>Random</code> number source.
     */
    public void sell(GoodsContainer container, GoodsType type, int amount,
                     Random random) {
        logger.finest(getName() + " sells " + amount + " " + type);
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
        propagateToEuropeanMarkets(type, marketAmount, random);

        if (container != null) container.addGoods(type, -amount);
    }

    /**
     * Adds a player to the list of players for whom the stance has changed.
     *
     * @param other The <code>ServerPlayer</code> to add.
     */
    public void addStanceChange(ServerPlayer other) {
        if (!stanceDirty.contains(other)) stanceDirty.add(other);
    }

    /**
     * Modifies stance.
     *
     * @param stance The new <code>Stance</code>.
     * @param otherPlayer The <code>Player</code> wrt which the stance changes.
     * @param symmetric If true, change the otherPlayer stance as well.
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if there was a change in stance at all.
     */
    public boolean csChangeStance(Stance stance, Player otherPlayer,
                                  boolean symmetric, ChangeSet cs) {
        ServerPlayer other = (ServerPlayer) otherPlayer;
        boolean change = false;
        Stance old = getStance(otherPlayer);

        if (old != stance) {
            int modifier = old.getTensionModifier(stance);
            setStance(otherPlayer, stance);
            if (modifier != 0) {
                cs.add(See.only(null).perhaps(other),
                    modifyTension(otherPlayer, modifier));
            }
            logger.info("Stance modification " + getName()
                + " " + old.toString() + " -> " + stance.toString()
                + " wrt " + otherPlayer.getName());
            this.addStanceChange(other);
            cs.addMessage(See.only(other),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                    "model.diplomacy." + stance + ".declared", this)
                .addStringTemplate("%nation%", getNationName()));
            cs.addStance(See.only(this), this, stance, otherPlayer);
            cs.addStance(See.only(other), this, stance, otherPlayer);
            change = true;
        }
        if (symmetric && (old = otherPlayer.getStance(this)) != stance) {
            int modifier = old.getTensionModifier(stance);
            otherPlayer.setStance(this, stance);
            if (modifier != 0) {
                cs.add(See.only(null).perhaps(this),
                    otherPlayer.modifyTension(this, modifier));
            }
            logger.info("Stance modification " + otherPlayer.getName()
                + " " + old.toString() + " -> " + stance.toString()
                + " wrt " + getName() + " (symmetric)");
            other.addStanceChange(this);
            cs.addMessage(See.only(this),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                    "model.diplomacy." + stance + ".declared", otherPlayer)
                .addStringTemplate("%nation%", otherPlayer.getNationName()));
            cs.addStance(See.only(this), otherPlayer, stance, this);
            cs.addStance(See.only(other), otherPlayer, stance, this);
            change = true;
        }

        return change;
    }

    /**
     * New turn for this player.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerPlayer.csNewTurn, for " + getName());

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
            try {
                ((ServerModelObject) unit).csNewTurn(random, cs);
            } catch (ClassCastException e) {
                logger.log(Level.SEVERE, "Not a ServerUnit: " + unit.getId(), e);
            }
        }

        if (isEuropean()) { // Update liberty and immigration
            if (checkEmigrate()
                && !hasAbility("model.ability.selectRecruit")) {
                // Auto-emigrate if selection not allowed.
                csEmigrate(0, MigrationType.NORMAL, random, cs);
            } else {
                cs.addPartial(See.only(this), this, "immigration");
            }
            cs.addPartial(See.only(this), this, "liberty");
        }

        // Update stances
        while (!stanceDirty.isEmpty()) {
            ServerPlayer s = stanceDirty.remove(0);
            Stance sta = getStance(s);
            boolean war = sta == Stance.WAR;
            if (sta == Stance.UNCONTACTED) continue;
            for (Player p : getGame().getLiveEuropeanPlayers()) {
                ServerPlayer sp = (ServerPlayer) p;
                if (sp == this || p == s
                    || !p.hasContacted(this) || !p.hasContacted(p)) continue;
                if (p.hasAbility("model.ability.betterForeignAffairsReport")
                    || war) {
                    cs.addStance(See.only(sp), this, sta, s);
                    cs.addMessage(See.only(sp),
                        new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                            "model.diplomacy." + sta + ".others", this)
                        .addStringTemplate("%attacker%", getNationName())
                        .addStringTemplate("%defender%", s.getNationName()));
                }
            }
        }
    }

    /**
     * Starts a new turn for a player.
     * Carefully do any random number generation outside of any
     * threads that start so as to keep random number generation
     * deterministic.
     *
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csStartTurn(Random random, ChangeSet cs) {
        Game game = getGame();
        if (isEuropean()) {
            csBombardEnemyShips(random, cs);

            csYearlyGoodsAdjust(random, cs);

            FoundingFather father = checkFoundingFather();
            if (father != null) {
                csAddFoundingFather(father, random, cs);
                clearOfferedFathers();
            }
            if (canRecruitFoundingFather()) {
                List<FoundingFather> ffs = getOfferedFathers();
                if (ffs.isEmpty()) {
                    ffs = getRandomFoundingFathers(random);
                    setOfferedFathers(ffs);
                }
                List<String> attributes = new ArrayList<String>();
                for (FoundingFather ff : ffs) {
                    attributes.add(ff.getType().toString());
                    attributes.add(ff.getId());
                }
                cs.addTrivial(See.only(this), "chooseFoundingFather",
                              ChangeSet.ChangePriority.CHANGE_NORMAL,
                              attributes.toArray(new String[0]));
            }

        } else if (isIndian()) {
            // We do not have to worry about Player level stance
            // changes driving Stance, as that is delegated to the AI.
            //
            // However we want to notify of individual settlements
            // that change tension level, but there are complex
            // interactions between settlement and player tensions.
            // The simple way to do it is just to save all old tension
            // levels and check if they have changed after applying
            // all the changes.
            List<IndianSettlement> allSettlements = getIndianSettlements();
            java.util.Map<IndianSettlement,
                java.util.Map<Player, Tension.Level>> oldLevels
                = new HashMap<IndianSettlement,
                java.util.Map<Player, Tension.Level>>();
            for (IndianSettlement settlement : allSettlements) {
                java.util.Map<Player, Tension.Level> oldLevel
                    = new HashMap<Player, Tension.Level>();
                oldLevels.put(settlement, oldLevel);
                for (Player enemy : game.getLiveEuropeanPlayers()) {
                    Tension alarm = settlement.getAlarm(enemy);
                    if (alarm != null) oldLevel.put(enemy, alarm.getLevel());
                }
            }

            // Do the settlement alarms first.
            for (IndianSettlement settlement : allSettlements) {
                java.util.Map<Player, Integer> extra
                    = new HashMap<Player, Integer>();
                for (Player enemy : game.getLiveEuropeanPlayers()) {
                    extra.put(enemy, new Integer(0));
                }

                // Look at the uses of tiles surrounding the settlement.
                int alarmRadius = settlement.getRadius() + ALARM_RADIUS;
                int alarm = 0;
                for (Tile tile: settlement.getTile()
                         .getSurroundingTiles(alarmRadius)) {
                    Colony colony = tile.getColony();
                    if (tile.getFirstUnit() != null) { // Military units
                        Player enemy =  tile.getFirstUnit().getOwner();
                        if (enemy.isEuropean()) {
                            alarm = extra.get(enemy);
                            for (Unit unit : tile.getUnitList()) {
                                if (unit.isOffensiveUnit() && !unit.isNaval()) {
                                    alarm += unit.getType().getOffence();
                                }
                            }
                            extra.put(enemy, alarm);
                        }
                    } else if (colony != null) { // Colonies
                        Player enemy = colony.getOwner();
                        extra.put(enemy, extra.get(enemy).intValue()
                                  + ALARM_TILE_IN_USE
                                  + colony.getUnitCount());
                    } else if (tile.getOwningSettlement() != null) { // Control
                        Player enemy = tile.getOwningSettlement().getOwner();
                        if (enemy != null && enemy.isEuropean()) {
                            extra.put(enemy, extra.get(enemy).intValue()
                                      + ALARM_TILE_IN_USE);
                        }
                    }
                }
                // Missionary helps reducing alarm a bit
                if (settlement.getMissionary() != null) {
                    Unit mission = settlement.getMissionary();
                    int missionAlarm = ALARM_MISSIONARY_PRESENT;
                    if (mission.hasAbility("model.ability.expertMissionary")) {
                        missionAlarm *= 2;
                    }
                    Player enemy = mission.getOwner();
                    extra.put(enemy,
                              extra.get(enemy).intValue() + missionAlarm);
                }
                // Apply modifiers, and commit the total change.
                for (Entry<Player, Integer> entry : extra.entrySet()) {
                    Player player = entry.getKey();
                    int change = entry.getValue().intValue();
                    if (change != 0) {
                        change = (int) player.getFeatureContainer()
                            .applyModifier(change,
                                           "model.modifier.nativeAlarmModifier",
                                           null, game.getTurn());
                        settlement.modifyAlarm(player, change);
                    }
                }
            }

            // Calm down a bit at the whole-tribe level.
            for (Player enemy : game.getLiveEuropeanPlayers()) {
                if (getTension(enemy).getValue() > 0) {
                    int change = -getTension(enemy).getValue()/100 - 4;
                    modifyTension(enemy, change);
                }
            }

            // Now collect the settlements that changed.
            for (IndianSettlement settlement : allSettlements) {
                java.util.Map<Player, Tension.Level> oldLevel
                    = oldLevels.get(settlement);
                for (Entry<Player, Tension.Level> entry : oldLevel.entrySet()) {
                    Player enemy = entry.getKey();
                    Tension.Level newLevel
                        = settlement.getAlarm(enemy).getLevel();
                    if (entry.getValue() != newLevel) {
                        cs.add(See.only(null).perhaps((ServerPlayer) enemy),
                               settlement);
                    }
                }
            }

            // Check for braves converted by missionaries
            List<UnitType> converts = game.getSpecification()
                .getUnitTypesWithAbility("model.ability.convert");
            StringTemplate nation = getNationName();
            for (IndianSettlement settlement : allSettlements) {
                if (settlement.checkForNewMissionaryConvert()) {
                    Unit missionary = settlement.getMissionary();
                    ServerPlayer other = (ServerPlayer) missionary.getOwner();
                    Settlement colony = settlement.getTile()
                        .getNearestSettlement(other, MAX_CONVERT_DISTANCE);
                    if (colony != null && converts.size() > 0) {
                        Unit brave = settlement.getUnitList().get(0);
                        brave.clearEquipment();
                        brave.setOwner(other);
                        brave.setNationality(other.getNationID());
                        brave.setType(Utils.getRandomMember(logger,
                                "Choose brave", converts, random));
                        brave.setLocation(colony.getTile());
                        cs.add(See.perhaps(), colony.getTile(), settlement);
                        cs.addMessage(See.only(other),
                            new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                "model.colony.newConvert", brave)
                            .addStringTemplate("%nation%", nation)
                            .addName("%colony%", colony.getName()));
                    }
                }
            }
        }
    }

    /**
     * All player colonies bombard all available targets.
     *
     * @param random A random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csBombardEnemyShips(Random random, ChangeSet cs) {
        for (Colony colony : getColonies()) {
            if (colony.canBombardEnemyShip()) {
                for (Tile tile : colony.getTile().getSurroundingTiles(1)) {
                    if (!tile.isLand() && tile.getFirstUnit() != null
                        && tile.getFirstUnit().getOwner() != this) {
                        for (Unit unit : tile.getUnitList()) {
                            if (atWarWith(unit.getOwner())
                                || unit.hasAbility(Ability.PIRACY)) {
                                csCombat(colony, unit, null, random, cs);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Add or remove a standard yearly amount of storable goods, and a
     * random extra amount of a random type.
     *
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csYearlyGoodsAdjust(Random random, ChangeSet cs) {
        List<GoodsType> goodsTypes = getGame().getSpecification()
            .getGoodsTypeList();
        Market market = getMarket();

        // Pick a random type of storable goods to add/remove an extra
        // amount of.
        GoodsType extraType;
        while (!(extraType = Utils.getRandomMember(logger, "Choose goods type",
                                                   goodsTypes, random))
               .isStorable());

        // Remove standard amount, and the extra amount.
        for (GoodsType type : goodsTypes) {
            if (type.isStorable() && market.hasBeenTraded(type)) {
                boolean add = market.getAmountInMarket(type)
                    < type.getInitialAmount();
                int amount = getGame().getTurn().getNumber() / 10;
                if (type == extraType) amount = 2 * amount + 1;
                if (amount <= 0) continue;
                amount = Utils.randomInt(logger, "Market adjust " + type,
                                         random, amount);
                if (!add) amount = -amount;
                market.addGoodsToMarket(type, amount);
                logger.finest(getName() + " adjust of " + amount
                              + " " + type
                              + ", total: " + market.getAmountInMarket(type)
                              + ", initial: " + type.getInitialAmount());
                csFlushMarket(type, cs);
            }
        }
    }

    /**
     * Adds a founding father to a players continental congress.
     *
     * @param father The <code>FoundingFather</code> to add.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csAddFoundingFather(FoundingFather father, Random random,
                                    ChangeSet cs) {
        Game game = getGame();
        Specification spec = game.getSpecification();
        Europe europe = getEurope();
        boolean europeDirty = false;

        // TODO: We do not want to have to update the whole player
        // just to get the FF into the client.  Use this hack until
        // the client gets proper containers.
        cs.addFather(this, father);

        cs.addMessage(See.only(this),
            new ModelMessage(ModelMessage.MessageType.SONS_OF_LIBERTY,
                             "model.player.foundingFatherJoinedCongress",
                             this)
                      .add("%foundingFather%", father.getNameKey())
                      .add("%description%", father.getDescriptionKey()));
        cs.addHistory(this,
            new HistoryEvent(getGame().getTurn(),
                             HistoryEvent.EventType.FOUNDING_FATHER)
                      .add("%father%", father.getNameKey()));

        List<AbstractUnit> units = father.getUnits();
        if (units != null && !units.isEmpty() && europe != null) {
            createUnits(father.getUnits());
            europeDirty = true;
        }

        java.util.Map<UnitType, UnitType> upgrades = father.getUpgrades();
        if (upgrades != null) {
            for (Unit u : getUnits()) {
                UnitType newType = upgrades.get(u.getType());
                if (newType != null) {
                    u.setType(newType);
                    cs.add(See.perhaps(), u);
                }
            }
        }

        if (recalculateBellsBonus()) {
            cs.add(See.only(this), this);
        }

        for (Event event : father.getEvents()) {
            String eventId = event.getId();
            if (eventId.equals("model.event.resetNativeAlarm")) {
                for (Player p : game.getPlayers()) {
                    if (!p.isDead() && p.isIndian() && p.hasContacted(this)) {
                        p.setTension(this, new Tension(Tension.TENSION_MIN));
                        for (IndianSettlement is : p.getIndianSettlements()) {
                            if (is.hasContactedSettlement(this)) {
                                is.setAlarm(this,
                                            new Tension(Tension.TENSION_MIN));
                                cs.add(See.only(this), is);
                            }
                        }
                        csChangeStance(Stance.PEACE, p, true, cs);
                    }
                }

            } else if (eventId.equals("model.event.boycottsLifted")) {
                Market market = getMarket();
                for (GoodsType goodsType : spec.getGoodsTypeList()) {
                    if (market.getArrears(goodsType) > 0) {
                        market.setArrears(goodsType, 0);
                        cs.add(See.only(this), market.getMarketData(goodsType));
                    }
                }

            } else if (eventId.equals("model.event.freeBuilding")) {
                BuildingType type = spec.getBuildingType(event.getValue());
                for (Colony colony : getColonies()) {
                    if (colony.canBuild(type)) {
                        colony.addBuilding(new ServerBuilding(game, colony, type));
                        colony.getBuildQueue().remove(type);
                        cs.add(See.only(this), colony);
                    }
                }

            } else if (eventId.equals("model.event.seeAllColonies")) {
                for (Tile t : game.getMap().getAllTiles()) {
                    Colony colony = t.getColony();
                    if (colony != null
                        && (ServerPlayer) colony.getOwner() != this) {
                        if (!t.isExploredBy(this)) {
                            t.setExploredBy(this, true);
                        }
                        t.updatePlayerExploredTile(this, false);
                        cs.add(See.only(this), t);
                        for (Tile x : colony.getOwnedTiles()) {
                            if (!x.isExploredBy(this)) {
                                x.setExploredBy(this, true);
                            }
                            x.updatePlayerExploredTile(this, false);
                            cs.add(See.only(this), x);
                        }
                    }
                }

            } else if (eventId.equals("model.event.increaseSonsOfLiberty")) {
                int value = Integer.parseInt(event.getValue());
                GoodsType bells = spec.getLibertyGoodsTypeList().get(0);
                int totalBells = 0;
                for (Colony colony : getColonies()) {
                    float oldRatio = (float) colony.getLiberty()
                        / (colony.getUnitCount() * Colony.LIBERTY_PER_REBEL);
                    float reqRatio = Math.min(1.0f, oldRatio + 0.01f * value);
                    int reqBells = (int) Math.round(Colony.LIBERTY_PER_REBEL
                                                    * colony.getUnitCount()
                                                    * (reqRatio - oldRatio));
                    if (reqBells > 0) { // Can go negative if already over 100%
                        colony.addGoods(bells, reqBells);
                        colony.updateSoL();
                        cs.add(See.only(this), colony);
                        totalBells += reqBells;
                    }
                }
                // Bonus bells from the FF do not count towards recruiting
                // the next one!
                incrementLiberty(-totalBells);

            } else if (eventId.equals("model.event.newRecruits")
                       && europe != null) {
                List<RandomChoice<UnitType>> recruits
                    = generateRecruitablesList();
                FeatureContainer fc = getFeatureContainer();
                for (int i = 0; i < Europe.RECRUIT_COUNT; i++) {
                    if (!fc.hasAbility("model.ability.canRecruitUnit",
                                       europe.getRecruitable(i))) {
                        UnitType newType = RandomChoice
                            .getWeightedRandom(logger,
                                "Replace recruit", random, recruits);
                        europe.setRecruitable(i, newType);
                        europeDirty = true;
                    }
                }

            } else if (eventId.equals("model.event.movementChange")) {
                for (Unit u : getUnits()) {
                    if (u.getMovesLeft() > 0) {
                        u.setMovesLeft(u.getInitialMovesLeft());
                        cs.addPartial(See.only(this), u, "movesLeft");
                    }
                }
            }
        }

        if (europeDirty) cs.add(See.only(this), europe);
    }


    /**
     * Claim land.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param settlement The <code>Settlement</code> to claim for.
     * @param price The price to pay for the land, which must agree
     *     with the owner valuation, unless negative which denotes stealing.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csClaimLand(Tile tile, Settlement settlement, int price,
                            ChangeSet cs) {
        Player owner = tile.getOwner();
        Settlement ownerSettlement = tile.getOwningSettlement();
        tile.changeOwnership(this, settlement);

        // Update the tile for all, and privately any now-angrier
        // owners, or the player gold if a price was paid.
        cs.add(See.perhaps(), tile);
        if (price > 0) {
            modifyGold(-price);
            owner.modifyGold(price);
            cs.addPartial(See.only(this), this, "gold");
        } else if (price < 0 && owner.isIndian()) {
            IndianSettlement is = (IndianSettlement) ownerSettlement;
            if (is != null) {
                cs.add(See.only(null).perhaps(this),
                       owner.modifyTension(this, Tension.TENSION_ADD_LAND_TAKEN, is));
            }
        }
    }


    /**
     * A unit migrates from Europe.
     *
     * @param slot The slot within <code>Europe</code> to select the unit from.
     * @param type The type of migration occurring.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csEmigrate(int slot, MigrationType type, Random random,
                           ChangeSet cs) {
        // Valid slots are in [1,3], recruitable indices are in [0,2].
        // An invalid slot is normal when the player has no control over
        // recruit type.
        boolean selected = 1 <= slot && slot <= Europe.RECRUIT_COUNT;
        int index = (selected) ? slot-1
            : Utils.randomInt(logger, "Choose emigrant", random,
                              Europe.RECRUIT_COUNT);

        // Create the recruit, move it to the docks.
        Europe europe = getEurope();
        UnitType recruitType = europe.getRecruitable(index);
        Game game = getGame();
        Unit unit = new ServerUnit(game, europe, this, recruitType,
                                   UnitState.ACTIVE);
        unit.setLocation(europe);

        // Handle migration type specific changes.
        switch (type) {
        case FOUNTAIN:
            setRemainingEmigrants(getRemainingEmigrants() - 1);
            break;
        case RECRUIT:
            modifyGold(-europe.getRecruitPrice());
            cs.addPartial(See.only(this), this, "gold");
            europe.increaseRecruitmentDifficulty();
            // Fall through
        case NORMAL:
            updateImmigrationRequired();
            reduceImmigration();
            cs.addPartial(See.only(this), this,
                          "immigration", "immigrationRequired");
            break;
        default:
            throw new IllegalArgumentException("Bogus migration type");
        }

        // Replace the recruit we used.  Shuffle them down first
        // as AI is always recruiting slot 0.
        for (int i = index; i < Europe.RECRUIT_COUNT-1; i++) {
            europe.setRecruitable(i, europe.getRecruitable(i+1));
        }
        List<RandomChoice<UnitType>> recruits = generateRecruitablesList();
        europe.setRecruitable(Europe.RECRUIT_COUNT-1,
            RandomChoice.getWeightedRandom(logger,
                "Replace recruit", random, recruits));
        cs.add(See.only(this), europe);

        // Add an informative message only if this was an ordinary
        // migration where we did not select the unit type.
        // Other cases were selected.
        if (!selected) {
            cs.addMessage(See.only(this),
                new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                 "model.europe.emigrate",
                                 this, unit)
                    .add("%europe%", europe.getNameKey())
                    .addStringTemplate("%unit%", unit.getLabel()));
        }
    }


    /**
     * Combat.
     *
     * @param attacker The <code>FreeColGameObject</code> that is attacking.
     * @param defender The <code>FreeColGameObject</code> that is defending.
     * @param crs A list of <code>CombatResult</code>s defining the result.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csCombat(FreeColGameObject attacker,
                         FreeColGameObject defender,
                         List<CombatResult> crs,
                         Random random,
                         ChangeSet cs) throws IllegalStateException {
        CombatModel combatModel = getGame().getCombatModel();
        boolean isAttack = combatModel.combatIsAttack(attacker, defender);
        boolean isBombard = combatModel.combatIsBombard(attacker, defender);
        Unit attackerUnit = null;
        Settlement attackerSettlement = null;
        Tile attackerTile = null;
        Unit defenderUnit = null;
        ServerPlayer defenderPlayer = null;
        Tile defenderTile = null;
        if (isAttack) {
            attackerUnit = (Unit) attacker;
            attackerTile = attackerUnit.getTile();
            defenderUnit = (Unit) defender;
            defenderPlayer = (ServerPlayer) defenderUnit.getOwner();
            defenderTile = defenderUnit.getTile();
            boolean bombard = attackerUnit.hasAbility("model.ability.bombard");
            cs.addAttribute(See.only(this), "sound",
                (attackerUnit.isNaval()) ? "sound.attack.naval"
                : (bombard) ? "sound.attack.artillery"
                : (attackerUnit.isMounted()) ? "sound.attack.mounted"
                : "sound.attack.foot");
            if (attackerUnit.getOwner().isIndian()
                && defenderPlayer.isEuropean()
                && defenderUnit.getLocation().getColony() != null
                && !defenderPlayer.atWarWith(attackerUnit.getOwner())) {
                StringTemplate attackerNation
                    = attackerUnit.getApparentOwnerName();
                Colony colony = defenderUnit.getLocation().getColony();
                cs.addMessage(See.only(defenderPlayer),
                    new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                        "model.unit.indianSurprise", colony)
                    .addStringTemplate("%nation%", attackerNation)
                    .addName("%colony%", colony.getName()));
            }
        } else if (isBombard) {
            attackerSettlement = (Settlement) attacker;
            attackerTile = attackerSettlement.getTile();
            defenderUnit = (Unit) defender;
            defenderPlayer = (ServerPlayer) defenderUnit.getOwner();
            defenderTile = defenderUnit.getTile();
            cs.addAttribute(See.only(this), "sound", "sound.attack.bombard");
        } else {
            throw new IllegalStateException("Bogus combat");
        }

        // If the combat results were not specified (usually the case),
        // query the combat model.
        if (crs == null) {
            crs = combatModel.generateAttackResult(random, attacker, defender);
        }
        if (crs.isEmpty()) {
            throw new IllegalStateException("empty attack result");
        }
        // Extract main result, insisting it is one of the fundamental cases,
        // and add the animation.
        // Set vis so that loser always sees things.
        // TODO: Bombard animations
        See vis; // Visibility that insists on the loser seeing the result.
        CombatResult result = crs.remove(0);
        switch (result) {
        case NO_RESULT:
            vis = See.perhaps();
            break; // Do not animate if there is no result.
        case WIN:
            vis = See.perhaps().always(defenderPlayer);
            if (isAttack) {
                cs.addAttack(vis, attackerUnit, defenderUnit, true);
            }
            break;
        case LOSE:
            vis = See.perhaps().always(this);
            if (isAttack) {
                cs.addAttack(vis, attackerUnit, defenderUnit, false);
            }
            break;
        default:
            throw new IllegalStateException("generateAttackResult returned: "
                                            + result);
        }
        // Now process the details.
        boolean attackerTileDirty = false;
        boolean defenderTileDirty = false;
        boolean moveAttacker = false;
        boolean burnedNativeCapital = false;
        Settlement settlement = defenderTile.getSettlement();
        Colony colony = defenderTile.getColony();
        IndianSettlement natives = (settlement instanceof IndianSettlement)
            ? (IndianSettlement) settlement
            : null;
        int attackerTension = 0;
        int defenderTension = 0;
        for (CombatResult cr : crs) {
            boolean ok;
            switch (cr) {
            case AUTOEQUIP_UNIT:
                ok = isAttack && settlement != null;
                if (ok) {
                    csAutoequipUnit(defenderUnit, settlement, cs);
                }
                break;
            case BURN_MISSIONS:
                ok = isAttack && result == CombatResult.WIN
                    && natives != null
                    && isEuropean() && defenderPlayer.isIndian();
                if (ok) {
                    defenderTileDirty |= natives.getMissionary(this) != null;
                    csBurnMissions(attackerUnit, natives, cs);
                }
                break;
            case CAPTURE_AUTOEQUIP:
                ok = isAttack && result == CombatResult.WIN
                    && settlement != null
                    && defenderPlayer.isEuropean();
                if (ok) {
                    csCaptureAutoEquip(attackerUnit, defenderUnit, cs);
                    attackerTileDirty = defenderTileDirty = true;
                }
                break;
            case CAPTURE_COLONY:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null
                    && isEuropean() && defenderPlayer.isEuropean();
                if (ok) {
                    csCaptureColony(attackerUnit, colony, random, cs);
                    attackerTileDirty = defenderTileDirty = true;
                    moveAttacker = true;
                    defenderTension += Tension.TENSION_ADD_MAJOR;
                }
                break;
            case CAPTURE_CONVERT:
                ok = isAttack && result == CombatResult.WIN
                    && natives != null
                    && isEuropean() && defenderPlayer.isIndian();
                if (ok) {
                    csCaptureConvert(attackerUnit, natives, random, cs);
                    attackerTileDirty = true;
                }
                break;
            case CAPTURE_EQUIP:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csCaptureEquip(attackerUnit, defenderUnit, cs);
                    } else {
                        csCaptureEquip(defenderUnit, attackerUnit, cs);
                    }
                    attackerTileDirty = defenderTileDirty = true;
                }
                break;
            case CAPTURE_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csCaptureUnit(attackerUnit, defenderUnit, cs);
                    } else {
                        csCaptureUnit(defenderUnit, attackerUnit, cs);
                    }
                    attackerTileDirty = defenderTileDirty = true;
                }
                break;
            case DAMAGE_COLONY_SHIPS:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null;
                if (ok) {
                    csDamageColonyShips(attackerUnit, colony, cs);
                    defenderTileDirty = true;
                }
                break;
            case DAMAGE_SHIP_ATTACK:
                ok = isAttack && result != CombatResult.NO_RESULT
                    && ((result == CombatResult.WIN) ? defenderUnit
                        : attackerUnit).isNaval();
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csDamageShipAttack(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csDamageShipAttack(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case DAMAGE_SHIP_BOMBARD:
                ok = isBombard && result == CombatResult.WIN
                    && defenderUnit.isNaval();
                if (ok) {
                    csDamageShipBombard(attackerSettlement, defenderUnit, cs);
                    defenderTileDirty = true;
                }
                break;
            case DEMOTE_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csDemoteUnit(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csDemoteUnit(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case DESTROY_COLONY:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null
                    && isIndian() && defenderPlayer.isEuropean();
                if (ok) {
                    csDestroyColony(attackerUnit, colony, random, cs);
                    attackerTileDirty = defenderTileDirty = true;
                    moveAttacker = true;
                    attackerTension -= Tension.TENSION_ADD_NORMAL;
                    defenderTension += Tension.TENSION_ADD_MAJOR;
                }
                break;
            case DESTROY_SETTLEMENT:
                ok = isAttack && result == CombatResult.WIN
                    && natives != null
                    && defenderPlayer.isIndian();
                if (ok) {
                    csDestroySettlement(attackerUnit, natives, random, cs);
                    attackerTileDirty = defenderTileDirty = true;
                    moveAttacker = true;
                    burnedNativeCapital = settlement.isCapital();
                    attackerTension -= Tension.TENSION_ADD_NORMAL;
                    if (!burnedNativeCapital) {
                        defenderTension += Tension.TENSION_ADD_MAJOR;
                    }
                }
                break;
            case EVADE_ATTACK:
                ok = isAttack && result == CombatResult.NO_RESULT
                    && defenderUnit.isNaval();
                if (ok) {
                    csEvadeAttack(attackerUnit, defenderUnit, cs);
                }
                break;
            case EVADE_BOMBARD:
                ok = isBombard && result == CombatResult.NO_RESULT
                    && defenderUnit.isNaval();
                if (ok) {
                    csEvadeBombard(attackerSettlement, defenderUnit, cs);
                }
                break;
            case LOOT_SHIP:
                ok = isAttack && result != CombatResult.NO_RESULT
                    && attackerUnit.isNaval() && defenderUnit.isNaval();
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csLootShip(attackerUnit, defenderUnit, cs);
                    } else {
                        csLootShip(defenderUnit, attackerUnit, cs);
                    }
                }
                break;
            case LOSE_AUTOEQUIP:
                ok = isAttack && result == CombatResult.WIN
                    && settlement != null
                    && defenderPlayer.isEuropean();
                if (ok) {
                    csLoseAutoEquip(attackerUnit, defenderUnit, cs);
                    defenderTileDirty = true;
                }
                break;
            case LOSE_EQUIP:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csLoseEquip(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csLoseEquip(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case PILLAGE_COLONY:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null
                    && isIndian() && defenderPlayer.isEuropean();
                if (ok) {
                    csPillageColony(attackerUnit, colony, random, cs);
                    defenderTileDirty = true;
                    attackerTension -= Tension.TENSION_ADD_NORMAL;
                }
                break;
            case PROMOTE_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csPromoteUnit(attackerUnit, defenderUnit, cs);
                        attackerTileDirty = true;
                    } else {
                        csPromoteUnit(defenderUnit, attackerUnit, cs);
                        defenderTileDirty = true;
                    }
                }
                break;
            case SINK_COLONY_SHIPS:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null;
                if (ok) {
                    csSinkColonyShips(attackerUnit, colony, cs);
                    defenderTileDirty = true;
                }
                break;
            case SINK_SHIP_ATTACK:
                ok = isAttack && result != CombatResult.NO_RESULT
                    && ((result == CombatResult.WIN) ? defenderUnit
                        : attackerUnit).isNaval();
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csSinkShipAttack(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csSinkShipAttack(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case SINK_SHIP_BOMBARD:
                ok = isBombard && result == CombatResult.WIN
                    && defenderUnit.isNaval();
                if (ok) {
                    csSinkShipBombard(attackerSettlement, defenderUnit, cs);
                    defenderTileDirty = true;
                }
                break;
            case SLAUGHTER_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csSlaughterUnit(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                        attackerTension -= Tension.TENSION_ADD_NORMAL;
                        defenderTension += getSlaughterTension(defenderUnit);
                    } else {
                        csSlaughterUnit(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                        attackerTension += getSlaughterTension(attackerUnit);
                        defenderTension -= Tension.TENSION_ADD_NORMAL;
                    }
                }
                break;
            default:
                ok = false;
                break;
            }
            if (!ok) {
                throw new IllegalStateException("Attack (result=" + result
                                                + ") has bogus subresult: "
                                                + cr);
            }
        }

        // Handle stance and tension.
        // - Privateers do not provoke stance changes but can set the
        //     attackedByPrivateers flag
        // - Attacks among Europeans imply war
        // - Burning of a native capital results in surrender
        // - Other attacks involving natives do not imply war, but
        //     changes in Tension can drive Stance, however this is
        //     decided by the native AI in their turn so just adjust tension.
        if (attacker.hasAbility(Ability.PIRACY)) {
            if (!defenderPlayer.getAttackedByPrivateers()) {
                defenderPlayer.setAttackedByPrivateers(true);
                cs.addPartial(See.only(defenderPlayer), defenderPlayer,
                              "attackedByPrivateers");
            }
        } else if (defender.hasAbility(Ability.PIRACY)) {
            ; // do nothing
        } else if (isEuropean() && defenderPlayer.isEuropean()) {
            csChangeStance(Stance.WAR, defenderPlayer, true, cs);
        } else if (burnedNativeCapital) {
            csChangeStance(Stance.PEACE, defenderPlayer, true, cs);
            defenderPlayer.getTension(this).setValue(Tension.SURRENDERED);
            cs.add(See.only(this), defenderPlayer); // TODO: just the tension
            for (IndianSettlement is : defenderPlayer.getIndianSettlements()) {
                is.makeContactSettlement(this);
                is.getAlarm(this).setValue(Tension.SURRENDERED);
                cs.add(See.only(this), is);
            }
        } else { // At least one player is non-European
            if (isEuropean()) {
                csChangeStance(Stance.WAR, defenderPlayer, true, cs);
            } else if (isIndian()) {
                if (result == CombatResult.WIN) {
                    attackerTension -= Tension.TENSION_ADD_MINOR;
                } else if (result == CombatResult.LOSE) {
                    attackerTension += Tension.TENSION_ADD_MINOR;
                }
            }
            if (defenderPlayer.isEuropean()) {
                defenderPlayer.csChangeStance(Stance.WAR, this, true, cs);
            } else if (defenderPlayer.isIndian()) {
                if (result == CombatResult.WIN) {
                    defenderTension += Tension.TENSION_ADD_MINOR;
                } else if (result == CombatResult.LOSE) {
                    defenderTension -= Tension.TENSION_ADD_MINOR;
                }
            }
            if (attackerTension != 0) {
                cs.add(See.only(null).perhaps(defenderPlayer),
                       modifyTension(defenderPlayer, attackerTension));
            }
            if (defenderTension != 0) {
                cs.add(See.only(null).perhaps(this),
                       defenderPlayer.modifyTension(this, defenderTension));
            }
        }

        // Move the attacker if required.
        if (moveAttacker) {
            attackerUnit.setMovesLeft(attackerUnit.getInitialMovesLeft());
            ((ServerUnit) attackerUnit).csMove(defenderTile, random, cs);
            attackerUnit.setMovesLeft(0);
            // Move adds in updates for the tiles, but...
            attackerTileDirty = defenderTileDirty = false;
            // ...with visibility of perhaps().
            // Thus the defender might see the change,
            // but because its settlement is gone it also might not.
            // So add in another defender-specific update.
            // The worst that can happen is a duplicate update.
            cs.add(See.only(defenderPlayer), defenderTile);
        } else if (isAttack) {
            // The Revenger unit can attack multiple times, so spend
            // at least the eventual cost of moving to the tile.
            // Other units consume the entire move.
            if (attacker.hasAbility("model.ability.multipleAttacks")) {
                int movecost = attackerUnit.getMoveCost(defenderTile);
                attackerUnit.setMovesLeft(attackerUnit.getMovesLeft()
                                          - movecost);
            } else {
                attackerUnit.setMovesLeft(0);
            }
            if (!attackerTileDirty) {
                cs.addPartial(See.only(this), attacker, "movesLeft");
            }
        }

        // Make sure we always update the attacker and defender tile
        // if it is not already done yet.
        if (attackerTileDirty) cs.add(vis, attackerTile);
        if (defenderTileDirty) cs.add(vis, defenderTile);
    }

    /**
     * Gets the amount to raise tension by when a unit is slaughtered.
     *
     * @param loser The <code>Unit</code> that dies.
     * @return An amount to raise tension by.
     */
    private int getSlaughterTension(Unit loser) {
        // Tension rises faster when units die.
        Settlement settlement = loser.getSettlement();
        if (settlement != null) {
            if (settlement instanceof IndianSettlement) {
                return (((IndianSettlement) settlement).isCapital())
                    ? Tension.TENSION_ADD_CAPITAL_ATTACKED
                    : Tension.TENSION_ADD_SETTLEMENT_ATTACKED;
            } else {
                return Tension.TENSION_ADD_NORMAL;
            }
        } else { // attack in the open
            return (loser.getIndianSettlement() != null)
                ? Tension.TENSION_ADD_UNIT_DESTROYED
                : Tension.TENSION_ADD_MINOR;
        }
    }

    /**
     * Notifies of automatic arming.
     *
     * @param unit The <code>Unit</code> that is auto-equipping.
     * @param settlement The <code>Settlement</code> being defended.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csAutoequipUnit(Unit unit, Settlement settlement,
                                 ChangeSet cs) {
        ServerPlayer player = (ServerPlayer) unit.getOwner();
        cs.addMessage(See.only(player),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.automaticDefence", unit)
            .addStringTemplate("%unit%", unit.getLabel())
            .addName("%colony%", settlement.getName()));
    }

    /**
     * Burns a players missions.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param settlement The <code>IndianSettlement</code> that was attacked.
     * @param cs The <code>ChangeSet</code> to update.
     */
    private void csBurnMissions(Unit attacker, IndianSettlement settlement,
                                ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attackerPlayer.getNationName();
        ServerPlayer nativePlayer = (ServerPlayer) settlement.getOwner();
        StringTemplate nativeNation = nativePlayer.getNationName();

        // Message only for the European player
        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.burnMissions", attacker, settlement)
                      .addStringTemplate("%nation%", attackerNation)
                      .addStringTemplate("%enemyNation%", nativeNation));

        // Burn down the missions
        for (IndianSettlement s : nativePlayer.getIndianSettlements()) {
            Unit missionary = s.getMissionary(attackerPlayer);
            if (missionary != null) {
                s.changeMissionary(null);
                if (s != settlement) cs.add(See.perhaps(), s.getTile());
            }
        }
    }

    /**
     * Defender autoequips but loses and attacker captures the equipment.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param defender The <code>Unit</code> that defended and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureAutoEquip(Unit attacker, Unit defender,
                                    ChangeSet cs) {
        EquipmentType equip
            = defender.getBestCombatEquipmentType(defender.getAutomaticEquipment());
        csLoseAutoEquip(attacker, defender, cs);
        csCaptureEquipment(attacker, defender, equip, cs);
    }

    /**
     * Captures a colony.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param colony The <code>Colony</code> to capture.
     * @param random A pseudo-random number source.
     * @param cs The <code>ChangeSet</code> to update.
     */
    private void csCaptureColony(Unit attacker, Colony colony, Random random,
                                 ChangeSet cs) {
        Game game = attacker.getGame();
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attackerPlayer.getNationName();
        ServerPlayer colonyPlayer = (ServerPlayer) colony.getOwner();
        StringTemplate colonyNation = colonyPlayer.getNationName();
        Tile tile = colony.getTile();
        int plunder = colony.getPlunder(attacker, random);

        // Handle history and messages before colony handover
        cs.addHistory(attackerPlayer,
                      new HistoryEvent(game.getTurn(),
                                       HistoryEvent.EventType.CONQUER_COLONY)
                      .addStringTemplate("%nation%", colonyNation)
                      .addName("%colony%", colony.getName()));
        cs.addHistory(colonyPlayer,
                      new HistoryEvent(game.getTurn(),
                                       HistoryEvent.EventType.COLONY_CONQUERED)
                      .addStringTemplate("%nation%", attackerNation)
                      .addName("%colony%", colony.getName()));
        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.colonyCaptured",
                                       colony)
                      .addName("%colony%", colony.getName())
                      .addAmount("%amount%", plunder));
        cs.addMessage(See.only(colonyPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.colonyCapturedBy",
                                       colony.getTile())
                      .addName("%colony%", colony.getName())
                      .addAmount("%amount%", plunder)
                      .addStringTemplate("%player%", attackerNation));

        // Allocate some plunder
        if (plunder > 0) {
            attackerPlayer.modifyGold(plunder);
            colonyPlayer.modifyGold(-plunder);
            cs.addPartial(See.only(attackerPlayer), attackerPlayer, "gold");
            cs.addPartial(See.only(colonyPlayer), colonyPlayer, "gold");
        }

        // Hand over the colony
        colony.changeOwner(attackerPlayer);

        // Inform former owner of loss of owned tiles, and process possible
        // increase in line of sight.  Leave other exploration etc to csMove.
        for (Tile t : colony.getOwnedTiles()) {
            cs.add(See.perhaps().always(colonyPlayer), t);
        }
        if (colony.getLineOfSight() > attacker.getLineOfSight()) {
            for (Tile t : tile.getSurroundingTiles(attacker.getLineOfSight(),
                                                   colony.getLineOfSight())) {
                // New owner has now explored within settlement line of sight.
                attackerPlayer.setExplored(t);
                cs.add(See.only(attackerPlayer), t);
            }
        }

        cs.addAttribute(See.only(attackerPlayer), "sound",
                        "sound.event.captureColony");
    }

    /**
     * Extracts a convert from a native settlement.
     *
     * @param attacker The <code>Unit</code> that is attacking.
     * @param natives The <code>IndianSettlement</code> under attack.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureConvert(Unit attacker, IndianSettlement natives,
                                  Random random, ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate convertNation = natives.getOwner().getNationName();
        List<UnitType> converts = getGame().getSpecification()
            .getUnitTypesWithAbility("model.ability.convert");
        UnitType type = Utils.getRandomMember(logger, "Choose convert",
                                              converts, random);
        Unit convert = natives.getUnitList().get(0);
        convert.clearEquipment();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.newConvertFromAttack",
                                       convert)
                      .addStringTemplate("%nation%", convertNation)
                      .addStringTemplate("%unit%", convert.getLabel()));

        convert.setOwner(attacker.getOwner());
        // do not change nationality: convert was forcibly captured and wants to run away
        convert.setType(type);
        convert.setLocation(attacker.getTile());
    }

    /**
     * Captures equipment.
     *
     * @param winner The <code>Unit</code> that captures equipment.
     * @param loser The <code>Unit</code> that defended and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureEquip(Unit winner, Unit loser, ChangeSet cs) {
        EquipmentType equip
            = loser.getBestCombatEquipmentType(loser.getEquipment());
        csLoseEquip(winner, loser, cs);
        csCaptureEquipment(winner, loser, equip, cs);
    }

    /**
     * Capture equipment.
     *
     * @param winner The <code>Unit</code> that is capturing equipment.
     * @param loser The <code>Unit</code> that is losing equipment.
     * @param equip The <code>EquipmentType</code> to capture.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureEquipment(Unit winner, Unit loser,
                                    EquipmentType equip, ChangeSet cs) {
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        if ((equip = winner.canCaptureEquipment(equip, loser)) != null) {
            // TODO: what if winner captures equipment that is
            // incompatible with their current equipment?
            // Currently, can-not-happen, so ignoring the return from
            // changeEquipment.  Beware.
            winner.changeEquipment(equip, 1);

            // Currently can not capture equipment back so this only
            // makes sense for native players, and the message is
            // native specific.
            if (winnerPlayer.isIndian()) {
                StringTemplate winnerNation = winnerPlayer.getNationName();
                cs.addMessage(See.only(loserPlayer),
                              new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               "model.unit.equipmentCaptured",
                                               winnerPlayer)
                              .addStringTemplate("%nation%", winnerNation)
                              .add("%equipment%", equip.getNameKey()));

                // CHEAT: Immediately transferring the captured goods
                // back to a potentially remote settlement is pretty
                // dubious.  Apparently Col1 did it.  Better would be
                // to give the capturing unit a go-home-with-plunder mission.
                IndianSettlement settlement = winner.getIndianSettlement();
                if (settlement != null) {
                    for (AbstractGoods goods : equip.getGoodsRequired()) {
                        settlement.addGoods(goods);
                        logger.finest("CHEAT teleporting " + goods.toString()
                                      + " back to " + settlement.getName());
                    }
                    cs.add(See.only(winnerPlayer), settlement);
                }
            }
        }
    }

    /**
     * Capture a unit.
     *
     * @param winner A <code>Unit</code> that is capturing.
     * @param loser A <code>Unit</code> to capture.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loserPlayer.getNationName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        StringTemplate oldName = loser.getLabel();
        String messageId = loser.getType().getId() + ".captured";
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winnerPlayer.getNationName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);

        // Capture the unit
        UnitType type = loser.getTypeChange((winnerPlayer.isUndead())
                                            ? ChangeType.UNDEAD
                                            : ChangeType.CAPTURE, winnerPlayer);
        loser.setOwner(winnerPlayer);
        if (type != null) loser.setType(type);
        loser.setLocation(winner.getTile());
        loser.setState(UnitState.ACTIVE);

        // Winner message post-capture when it owns the loser
        cs.addMessage(See.only(winnerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, loser)
                      .setDefaultId("model.unit.unitCaptured")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%unit%", oldName)
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, loser.getTile())
                      .setDefaultId("model.unit.unitCaptured")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%unit%", oldName)
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", loserLocation));
    }

    /**
     * Damages all ships in a colony.
     *
     * @param attacker The <code>Unit</code> that is damaging.
     * @param colony The <code>Colony</code> to damage ships in.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageColonyShips(Unit attacker, Colony colony,
                                     ChangeSet cs) {
        List<Unit> units = colony.getTile().getUnitList();
        while (!units.isEmpty()) {
            Unit unit = units.remove(0);
            if (unit.isNaval()) {
                csDamageShipAttack(attacker, unit, cs);
            }
        }
    }

    /**
     * Damage a ship through normal attack.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param ship The <code>Unit</code> which is a ship to damage.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageShipAttack(Unit attacker, Unit ship,
                                    ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attacker.getApparentOwnerName();
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        Location repair = ship.getRepairLocation();
        StringTemplate repairLoc = repair.getLocationNameFor(shipPlayer);
        StringTemplate shipNation = ship.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.enemyShipDamaged", attacker)
            .addStringTemplate("%unit%", attacker.getLabel())
            .addStringTemplate("%enemyNation%", shipNation)
            .addStringTemplate("%enemyUnit%", ship.getLabel()));
        cs.addMessage(See.only(shipPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipDamaged", ship)
            .addStringTemplate("%unit%", ship.getLabel())
            .addStringTemplate("%enemyUnit%", attacker.getLabel())
            .addStringTemplate("%enemyNation%", attackerNation)
            .addStringTemplate("%repairLocation%", repairLoc));

        csDamageShip(ship, repair, cs);
    }

    /**
     * Damage a ship through bombard.
     *
     * @param settlement The attacker <code>Settlement</code>.
     * @param ship The <code>Unit</code> which is a ship to damage.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageShipBombard(Settlement settlement, Unit ship,
                                     ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) settlement.getOwner();
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        Location repair = ship.getRepairLocation();
        StringTemplate repairLoc = repair.getLocationNameFor(shipPlayer);
        StringTemplate shipNation = ship.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.enemyShipDamagedByBombardment", settlement)
            .addName("%colony%", settlement.getName())
            .addStringTemplate("%nation%", shipNation)
            .addStringTemplate("%unit%", ship.getLabel()));
        cs.addMessage(See.only(shipPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipDamagedByBombardment", ship)
            .addName("%colony%", settlement.getName())
            .addStringTemplate("%unit%", ship.getLabel())
            .addStringTemplate("%repairLocation%", repairLoc));

        csDamageShip(ship, repair, cs);
    }

    /**
     * Damage a ship.
     *
     * @param ship The naval <code>Unit</code> to damage.
     * @param repair The <code>Location</code> to send it to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageShip(Unit ship, Location repair, ChangeSet cs) {
        ServerPlayer player = (ServerPlayer) ship.getOwner();

        // Lose the units aboard
        Unit u;
        while ((u = ship.getFirstUnit()) != null) {
            u.setLocation(null);
            cs.addDispose(See.only(player), null, u); // Only owner-visible
        }

        // Damage the ship and send it off for repair
        ship.getGoodsContainer().removeAll();
        ship.setHitpoints(1);
        ship.setDestination(null);
        ship.setLocation(repair);
        ship.setState(UnitState.ACTIVE);
        ship.setMovesLeft(0);
        cs.add(See.only(player), (FreeColGameObject) repair);
    }

    /**
     * Demotes a unit.
     *
     * @param winner The <code>Unit</code> that won.
     * @param loser The <code>Unit</code> that lost and should be demoted.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDemoteUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loser.getApparentOwnerName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        StringTemplate oldName = loser.getLabel();
        String messageId = loser.getType().getId() + ".demoted";
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winner.getApparentOwnerName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);

        UnitType type = loser.getTypeChange(ChangeType.DEMOTION, loserPlayer);
        if (type == null || type == loser.getType()) {
            logger.warning("Demotion failed, type="
                + ((type == null) ? "null" : "same type: " + type));
            return;
        }
        loser.setType(type);

        cs.addMessage(See.only(winnerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                messageId, winner)
            .setDefaultId("model.unit.unitDemoted")
            .addStringTemplate("%nation%", loserNation)
            .addStringTemplate("%oldName%", oldName)
            .addStringTemplate("%unit%", loser.getLabel())
            .addStringTemplate("%enemyNation%", winnerPlayer.getNationName())
            .addStringTemplate("%enemyUnit%", winner.getLabel())
            .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                messageId, loser)
            .setDefaultId("model.unit.unitDemoted")
            .addStringTemplate("%nation%", loserPlayer.getNationName())
            .addStringTemplate("%oldName%", oldName)
            .addStringTemplate("%unit%", loser.getLabel())
            .addStringTemplate("%enemyNation%", winnerNation)
            .addStringTemplate("%enemyUnit%", winner.getLabel())
            .addStringTemplate("%location%", loserLocation));
    }

    /**
     * Destroy a colony.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param colony The <code>Colony</code> that was attacked.
     * @param random A pseudo-random number source.
     * @param cs The <code>ChangeSet</code> to update.
     */
    private void csDestroyColony(Unit attacker, Colony colony, Random random,
                                 ChangeSet cs) {
        Game game = attacker.getGame();
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attacker.getApparentOwnerName();
        ServerPlayer colonyPlayer = (ServerPlayer) colony.getOwner();
        StringTemplate colonyNation = colonyPlayer.getNationName();
        int plunder = colony.getPlunder(attacker, random);

        // Handle history and messages before colony destruction.
        cs.addHistory(colonyPlayer,
            new HistoryEvent(game.getTurn(),
                HistoryEvent.EventType.COLONY_DESTROYED)
            .addStringTemplate("%nation%", attackerNation)
            .addName("%colony%", colony.getName()));
        cs.addMessage(See.only(colonyPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.colonyBurning", colony.getTile())
            .addName("%colony%", colony.getName())
            .addAmount("%amount%", plunder)
            .addStringTemplate("%nation%", attackerNation)
            .addStringTemplate("%unit%", attacker.getLabel()));
        cs.addMessage(See.all().except(colonyPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.colonyBurning.other", colonyPlayer)
            .addName("%colony%", colony.getName())
            .addStringTemplate("%nation%", colonyNation)
            .addStringTemplate("%attackerNation%", attackerNation));

        // Allocate some plunder.
        if (plunder > 0) {
            attackerPlayer.modifyGold(plunder);
            colonyPlayer.modifyGold(-plunder);
            cs.addPartial(See.only(attackerPlayer), attackerPlayer, "gold");
            cs.addPartial(See.only(colonyPlayer), colonyPlayer, "gold");
        }

        // Dispose of the colony and its contents.
        csDisposeSettlement(colony, cs);
    }

    /**
     * Destroys an Indian settlement.
     *
     * @param attacker an <code>Unit</code> value
     * @param settlement an <code>IndianSettlement</code> value
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDestroySettlement(Unit attacker,
                                     IndianSettlement settlement,
                                     Random random, ChangeSet cs) {
        Game game = getGame();
        Tile tile = settlement.getTile();
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        ServerPlayer nativePlayer = (ServerPlayer) settlement.getOwner();
        StringTemplate nativeNation = nativePlayer.getNationName();
        String settlementName = settlement.getName();
        boolean capital = settlement.isCapital();
        int plunder = settlement.getPlunder(attacker, random);

        // Destroy the settlement, update settlement tiles.
        csDisposeSettlement(settlement, cs);

        // Make the treasure train if there is treasure.
        if (plunder > 0) {
            List<UnitType> unitTypes = game.getSpecification()
                .getUnitTypesWithAbility(Ability.CARRY_TREASURE);
            UnitType type = Utils.getRandomMember(logger, "Choose train",
                unitTypes, random);
            Unit train = new ServerUnit(game, tile, attackerPlayer, type,
                UnitState.ACTIVE);
            train.setTreasureAmount(plunder);
        }

        // This is an atrocity.
        int atrocities = Player.SCORE_SETTLEMENT_DESTROYED;
        if (settlement.getType().getClaimableRadius() > 1) atrocities *= 2;
        if (capital) atrocities = (atrocities * 3) / 2;
        attackerPlayer.modifyScore(atrocities);
        cs.addPartial(See.only(attackerPlayer), attackerPlayer, "score");

        // Finish with messages and history.
        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.indianTreasure", attacker)
            .addName("%settlement%", settlementName)
            .addAmount("%amount%", plunder));
        cs.addHistory(attackerPlayer,
            new HistoryEvent(game.getTurn(),
                HistoryEvent.EventType.DESTROY_SETTLEMENT)
            .addStringTemplate("%nation%", nativeNation)
            .addName("%settlement%", settlementName));
        if (capital) {
            cs.addMessage(See.only(this),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                    "indianSettlement.capitalBurned", attacker)
                .addName("%name%", settlementName)
                .addStringTemplate("%nation%", nativeNation));
        }
        if (nativePlayer.checkForDeath()) {
            cs.addGlobalHistory(game,
                new HistoryEvent(game.getTurn(),
                    HistoryEvent.EventType.DESTROY_NATION)
                .addStringTemplate("%nation%", nativeNation));
        }
        cs.addAttribute(See.only(attackerPlayer), "sound",
            "sound.event.destroySettlement");
    }

    /**
     * Disposes of a settlement and reassign its tiles.
     *
     * @param settlement The <code>Settlement</code> under attack.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDisposeSettlement(Settlement settlement, ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) settlement.getOwner();
        HashMap<Settlement, Integer> votes = new HashMap<Settlement, Integer>();

        // Try to reassign the tiles
        List<Tile> owned = settlement.getOwnedTiles();
        Tile centerTile = settlement.getTile();
        Settlement centerClaimant = null;
        while (!owned.isEmpty()) {
            Tile tile = owned.remove(0);
            votes.clear();
            for (Tile t : tile.getSurroundingTiles(1)) {
                // For each lost tile, find any neighbouring
                // settlements and give them a shout at claiming the tile.
                // Note that settlements now can own tiles outside
                // their radius--- if we encounter any of these clean
                // them up too.
                Settlement s = t.getOwningSettlement();
                if (s == null) {
                    ;
                } else if (s == settlement) {
                    // Add this to the tiles to process if its not
                    // there already.
                    if (!owned.contains(t)) owned.add(t);
                } else if (s.getOwner().canOwnTile(tile)
                           && (s.getOwner().isIndian()
                               || s.getTile().getDistanceTo(tile) <= s.getRadius())) {
                    // Weight claimant settlements:
                    //   settlements owned by the same player
                    //     > settlements owned by same type of player
                    //     > other settlements
                    int value = (s.getOwner() == owner) ? 3
                        : (s.getOwner().isEuropean() == owner.isEuropean()) ? 2
                        : 1;
                    if (votes.get(s) != null) value += votes.get(s).intValue();
                    votes.put(s, new Integer(value));
                }
            }
            Settlement bestClaimant = null;
            int bestClaim = 0;
            for (Entry<Settlement, Integer> vote : votes.entrySet()) {
                if (vote.getValue().intValue() > bestClaim) {
                    bestClaimant = vote.getKey();
                    bestClaim = vote.getValue().intValue();
                }
            }
            if (tile == centerTile) {
                centerClaimant = bestClaimant; // Defer until settlement gone
            } else {
                if (bestClaimant == null) {
                    tile.changeOwnership(null, null);
                } else {
                    tile.changeOwnership(bestClaimant.getOwner(), bestClaimant);
                }
                cs.add(See.perhaps().always(owner), tile);
            }
        }

        // Settlement goes away
        cs.addDispose(See.perhaps().always(owner), centerTile, settlement);
        if (centerClaimant != null) {
            centerTile.changeOwnership(centerClaimant.getOwner(),
                                       centerClaimant);
        }
    }

    /**
     * Evade a normal attack.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param defender A naval <code>Unit</code> that evades the attacker.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csEvadeAttack(Unit attacker, Unit defender, ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attacker.getApparentOwnerName();
        ServerPlayer defenderPlayer = (ServerPlayer) defender.getOwner();
        StringTemplate defenderNation = defender.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.enemyShipEvaded", attacker)
            .addStringTemplate("%unit%", attacker.getLabel())
            .addStringTemplate("%enemyUnit%", defender.getLabel())
            .addStringTemplate("%enemyNation%", defenderNation));
        cs.addMessage(See.only(defenderPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipEvaded", defender)
            .addStringTemplate("%unit%", defender.getLabel())
            .addStringTemplate("%enemyUnit%", attacker.getLabel())
            .addStringTemplate("%enemyNation%", attackerNation));
    }

    /**
     * Evade a bombardment.
     *
     * @param settlement The attacker <code>Settlement</code>.
     * @param defender A naval <code>Unit</code> that evades the attacker.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csEvadeBombard(Settlement settlement, Unit defender,
                                ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) settlement.getOwner();
        ServerPlayer defenderPlayer = (ServerPlayer) defender.getOwner();
        StringTemplate defenderNation = defender.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipEvadedBombardment", settlement)
            .addName("%colony%", settlement.getName())
            .addStringTemplate("%unit%", defender.getLabel())
            .addStringTemplate("%nation%", defenderNation));
        cs.addMessage(See.only(defenderPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipEvadedBombardment", defender)
            .addName("%colony%", settlement.getName())
            .addStringTemplate("%unit%", defender.getLabel())
            .addStringTemplate("%nation%", defenderNation));
    }

    /**
     * Loot a ship.
     *
     * @param winner The winning naval <code>Unit</code>.
     * @param loser The losing naval <code>Unit</code>
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csLootShip(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        if (loser.getGoodsList().size() > 0 && winner.getSpaceLeft() > 0) {
            List<Goods> capture = new ArrayList<Goods>(loser.getGoodsList());
            for (Goods g : capture) g.setLocation(null);
            TransactionSession.establishLootSession(winner, loser, capture);
            cs.add(See.only(winnerPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                new LootCargoMessage(winner, loser.getId(), capture));
        }
        loser.getGoodsContainer().removeAll();
        loser.setState(UnitState.ACTIVE);
    }

    /**
     * Unit autoequips but loses equipment.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param defender The <code>Unit</code> that defended and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csLoseAutoEquip(Unit attacker, Unit defender, ChangeSet cs) {
        ServerPlayer defenderPlayer = (ServerPlayer) defender.getOwner();
        StringTemplate defenderNation = defenderPlayer.getNationName();
        Settlement settlement = defender.getSettlement();
        StringTemplate defenderLocation = defender.getLocation()
            .getLocationNameFor(defenderPlayer);
        EquipmentType equip = defender
            .getBestCombatEquipmentType(defender.getAutomaticEquipment());
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerLocation = attacker.getLocation()
            .getLocationNameFor(attackerPlayer);
        StringTemplate attackerNation = attacker.getApparentOwnerName();

        // Autoequipment is not actually with the unit, it is stored
        // in the settlement of the unit.  Remove it from there.
        for (AbstractGoods goods : equip.getGoodsRequired()) {
            settlement.removeGoods(goods);
        }

        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.unitWinColony", attacker)
            .addStringTemplate("%location%", attackerLocation)
            .addStringTemplate("%nation%", attackerPlayer.getNationName())
            .addStringTemplate("%unit%", attacker.getLabel())
            .addName("%settlement%", settlement.getNameFor(attackerPlayer))
            .addStringTemplate("%enemyNation%", defenderNation)
            .addStringTemplate("%enemyUnit%", defender.getLabel()));
        cs.addMessage(See.only(defenderPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.unitLoseAutoEquip", defender)
            .addStringTemplate("%location%", defenderLocation)
            .addStringTemplate("%nation%", defenderNation)
            .addStringTemplate("%unit%", defender.getLabel())
            .addName("%settlement%", settlement.getNameFor(defenderPlayer))
            .addStringTemplate("%enemyNation%", attackerNation)
            .addStringTemplate("%enemyUnit%", attacker.getLabel()));
    }

    /**
     * Unit drops some equipment.
     *
     * @param winner The <code>Unit</code> that won.
     * @param loser The <code>Unit</code> that lost and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csLoseEquip(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loserPlayer.getNationName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        StringTemplate oldName = loser.getLabel();
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winner.getApparentOwnerName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);
        EquipmentType equip
            = loser.getBestCombatEquipmentType(loser.getEquipment());

        // Remove the equipment, accounting for possible loss of
        // mobility due to horses going away.
        loser.changeEquipment(equip, -1);
        loser.setMovesLeft(Math.min(loser.getMovesLeft(),
                                    loser.getInitialMovesLeft()));

        String messageId;
        if (loser.getEquipment().isEmpty()) {
            messageId = "model.unit.unitDemotedToUnarmed";
            loser.setState(UnitState.ACTIVE);
        } else {
            messageId = loser.getType().getId() + ".demoted";
        }

        cs.addMessage(See.only(winnerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                messageId, winner)
            .setDefaultId("model.unit.unitDemoted")
            .addStringTemplate("%nation%", loserNation)
            .addStringTemplate("%oldName%", oldName)
            .addStringTemplate("%unit%", loser.getLabel())
            .addStringTemplate("%enemyNation%", winnerPlayer.getNationName())
            .addStringTemplate("%enemyUnit%", winner.getLabel())
            .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                messageId, loser)
            .setDefaultId("model.unit.unitDemoted")
            .addStringTemplate("%nation%", loserNation)
            .addStringTemplate("%oldName%", oldName)
            .addStringTemplate("%unit%", loser.getLabel())
            .addStringTemplate("%enemyNation%", winnerNation)
            .addStringTemplate("%enemyUnit%", winner.getLabel())
            .addStringTemplate("%location%", loserLocation));
    }

    /**
     * Damage a building or a ship or steal some goods or gold.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param colony The <code>Colony</code> to pillage.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csPillageColony(Unit attacker, Colony colony,
                                 Random random, ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attacker.getApparentOwnerName();
        ServerPlayer colonyPlayer = (ServerPlayer) colony.getOwner();
        StringTemplate colonyNation = colonyPlayer.getNationName();

        // Collect the damagable buildings, ships, movable goods.
        List<Building> buildingList = colony.getBurnableBuildingList();
        List<Unit> shipList = colony.getShipList();
        List<Goods> goodsList = colony.getLootableGoodsList();

        // Pick one, with one extra choice for stealing gold.
        int pillage = Utils.randomInt(logger, "Pillage choice", random,
            buildingList.size() + shipList.size() + goodsList.size()
            + ((colony.canBePlundered()) ? 1 : 0));
        if (pillage < buildingList.size()) {
            Building building = buildingList.get(pillage);
            cs.addMessage(See.only(colonyPlayer),
                new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                    "model.unit.buildingDamaged", colony)
                .add("%building%", building.getNameKey())
                .addName("%colony%", colony.getName())
                .addStringTemplate("%enemyNation%", attackerNation)
                .addStringTemplate("%enemyUnit%", attacker.getLabel()));
            colony.damageBuilding(building);
        } else if (pillage < buildingList.size() + shipList.size()) {
            Unit ship = shipList.get(pillage - buildingList.size());
            if (ship.getRepairLocation() == null) {
                csSinkShipAttack(attacker, ship, cs);
            } else {
                csDamageShipAttack(attacker, ship, cs);
            }
        } else if (pillage < buildingList.size() + shipList.size()
                   + goodsList.size()) {
            Goods goods = goodsList.get(pillage - buildingList.size()
                - shipList.size());
            goods.setAmount(Math.min(goods.getAmount() / 2, 50));
            colony.removeGoods(goods);
            if (attacker.getSpaceLeft() > 0) attacker.add(goods);
            cs.addMessage(See.only(colonyPlayer),
                new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                    "model.unit.goodsStolen", colony, goods)
                .addAmount("%amount%", goods.getAmount())
                .add("%goods%", goods.getNameKey())
                .addName("%colony%", colony.getName())
                .addStringTemplate("%enemyNation%", attackerNation)
                .addStringTemplate("%enemyUnit%", attacker.getLabel()));

        } else {
            int plunder = colony.getPlunder(attacker, random) / 10;
            if (plunder > 0) {
                colonyPlayer.modifyGold(-plunder);
                attackerPlayer.modifyGold(plunder);
                cs.addPartial(See.only(colonyPlayer), colonyPlayer, "gold");
            }
            cs.addMessage(See.only(colonyPlayer),
                new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                    "model.unit.indianPlunder", colony)
                .addAmount("%amount%", plunder)
                .addName("%colony%", colony.getName())
                .addStringTemplate("%enemyNation%", attackerNation)
                .addStringTemplate("%enemyUnit%", attacker.getLabel()));
        }
        cs.addMessage(See.all().except(colonyPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.indianRaid", colonyPlayer)
            .addStringTemplate("%nation%", attackerNation)
            .addName("%colony%", colony.getName())
            .addStringTemplate("%colonyNation%", colonyNation));
    }

    /**
     * Promotes a unit.
     *
     * @param winner The <code>Unit</code> that won and should be promoted.
     * @param loser The <code>Unit</code> that lost.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csPromoteUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winnerPlayer.getNationName();
        StringTemplate oldName = winner.getLabel();

        UnitType type = winner.getTypeChange(ChangeType.PROMOTION,
                                             winnerPlayer);
        if (type == null || type == winner.getType()) {
            logger.warning("Promotion failed, type="
                + ((type == null) ? "null" : "same type: " + type));
            return;
        }
        winner.setType(type);
        cs.addMessage(See.only(winnerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                          "model.unit.unitPromoted", winner)
            .addStringTemplate("%oldName%", oldName)
            .addStringTemplate("%unit%", winner.getLabel())
            .addStringTemplate("%nation%", winnerNation));
    }

    /**
     * Sinks all ships in a colony.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param colony The <code>Colony</code> to sink ships in.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkColonyShips(Unit attacker, Colony colony, ChangeSet cs) {
        List<Unit> units = colony.getTile().getUnitList();
        while (!units.isEmpty()) {
            Unit unit = units.remove(0);
            if (unit.isNaval()) {
                csSinkShipAttack(attacker, unit, cs);
            }
        }
    }

    /**
     * Sinks this ship as result of a normal attack.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param ship The naval <code>Unit</code> to sink.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkShipAttack(Unit attacker, Unit ship, ChangeSet cs) {
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        StringTemplate shipNation = ship.getApparentOwnerName();
        Unit attackerUnit = (Unit) attacker;
        ServerPlayer attackerPlayer = (ServerPlayer) attackerUnit.getOwner();
        StringTemplate attackerNation = attackerUnit.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.enemyShipSunk", attackerUnit)
            .addStringTemplate("%unit%", attackerUnit.getLabel())
            .addStringTemplate("%enemyUnit%", ship.getLabel())
            .addStringTemplate("%enemyNation%", shipNation));
        cs.addMessage(See.only(shipPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipSunk", ship.getTile())
            .addStringTemplate("%unit%", ship.getLabel())
            .addStringTemplate("%enemyUnit%", attackerUnit.getLabel())
            .addStringTemplate("%enemyNation%", attackerNation));

        csSinkShip(ship, attackerPlayer, cs);
    }

    /**
     * Sinks this ship as result of a bombard.
     *
     * @param settlement The bombarding <code>Settlement</code>.
     * @param ship The naval <code>Unit</code> to sink.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkShipBombard(Settlement settlement, Unit ship,
                                   ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) settlement.getOwner();
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        StringTemplate shipNation = ship.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipSunkByBombardment", settlement)
            .addName("%colony%", settlement.getName())
            .addStringTemplate("%unit%", ship.getLabel())
            .addStringTemplate("%nation%", shipNation));
        cs.addMessage(See.only(shipPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                "model.unit.shipSunkByBombardment", ship.getTile())
            .addName("%colony%", settlement.getName())
            .addStringTemplate("%unit%", ship.getLabel()));

        csSinkShip(ship, attackerPlayer, cs);
    }

    /**
     * Sink the ship.
     *
     * @param ship The naval <code>Unit</code> to sink.
     * @param attackerPlayer The <code>ServerPlayer</code> that attacked.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkShip(Unit ship, ServerPlayer attackerPlayer,
                            ChangeSet cs) {
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        cs.addDispose(See.perhaps().always(shipPlayer),
            ship.getLocation(), ship);
        cs.addAttribute(See.only(attackerPlayer), "sound",
            "sound.event.shipSunk");
    }

    /**
     * Slaughter a unit.
     *
     * @param winner The <code>Unit</code> that is slaughtering.
     * @param loser The <code>Unit</code> to slaughter.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSlaughterUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winner.getApparentOwnerName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loser.getApparentOwnerName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        String messageId = loser.getType().getId() + ".destroyed";

        cs.addMessage(See.only(winnerPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                messageId, winner)
            .setDefaultId("model.unit.unitSlaughtered")
            .addStringTemplate("%nation%", loserNation)
            .addStringTemplate("%unit%", loser.getLabel())
            .addStringTemplate("%enemyNation%", winnerPlayer.getNationName())
            .addStringTemplate("%enemyUnit%", winner.getLabel())
            .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                messageId, loser.getTile())
            .setDefaultId("model.unit.unitSlaughtered")
            .addStringTemplate("%nation%", loserPlayer.getNationName())
            .addStringTemplate("%unit%", loser.getLabel())
            .addStringTemplate("%enemyNation%", winnerNation)
            .addStringTemplate("%enemyUnit%", winner.getLabel())
            .addStringTemplate("%location%", loserLocation));
        if (loserPlayer.isIndian() && loserPlayer.checkForDeath()) {
            StringTemplate nativeNation = loserPlayer.getNationName();
            cs.addGlobalHistory(getGame(),
                new HistoryEvent(getGame().getTurn(),
                    HistoryEvent.EventType.DESTROY_NATION)
                .addStringTemplate("%nation%", nativeNation));
        }

        // Transfer equipment, do not generate messages for the loser.
        EquipmentType equip;
        while ((equip = loser.getBestCombatEquipmentType(loser.getEquipment()))
               != null) {
            loser.changeEquipment(equip, -loser.getEquipmentCount(equip));
            csCaptureEquipment(winner, loser, equip, cs);
        }

        // Destroy unit.
        cs.addDispose(See.perhaps().always(loserPlayer),
            loser.getLocation(), loser);
    }

    /**
     * Updates the PlayerExploredTile for each new tile on a supplied list,
     * and update a changeset as well.
     *
     * @param newTiles A list of <code>Tile</code>s to update.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csSeeNewTiles(List<Tile> newTiles, ChangeSet cs) {
        for (Tile t : newTiles) {
            t.updatePlayerExploredTile(this, false);
            cs.add(See.only(this), t);
        }
    }

    /**
     * Set the player tax rate.
     * If this requires a change to the bells bonuses, we have to update
     * the whole player (bah) because we can not yet independently update
     * the feature container.
     *
     * @param tax The new tax rate.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csSetTax(int tax, ChangeSet cs) {
        setTax(tax);
        if (recalculateBellsBonus()) {
            cs.add(See.only(this), this);
        } else {
            cs.addPartial(See.only(this), this, "tax");
        }
    }


    /**
     * Check for a special contact panel for a nation.  If not found,
     * check for a more general one if allowed.
     * Assumes this player is European.
     *
     * @param other The <code>Player</code> nation to being contacted.
     * @return An <code>EventPanel</code> key, or null if none appropriate.
     */
    private String getContactKey(ServerPlayer other) {
        String key = "EventPanel.MEETING_" + other.getNationNameKey();
        if (!Messages.containsKey(key)) {
            if (other.isEuropean()) {
                key = (hasContactedEuropeans()) ? null
                    : "EventPanel.MEETING_EUROPEANS";
            } else {
                key = (hasContactedIndians()) ? null
                    : "EventPanel.MEETING_NATIVES";
            }
        }
        return key;
    }

    /**
     * Make contact between two nations if necessary.
     *
     * @param other The other <code>ServerPlayer</code>.
     * @param tile The <code>Tile</code> contact is made at.
     * @param cs A <code>ChangeSet</code> to update.
     * @return The other nation if it is welcoming this nation on first landing.
     */
    public ServerPlayer csContact(ServerPlayer other, Tile tile, ChangeSet cs) {
        if (hasContacted(other)) return null;

        // Must be a first contact!
        Game game = getGame();
        Turn turn = game.getTurn();
        ServerPlayer welcomer = null;
        if (isIndian()) {
            // Ignore native-to-native contacts.
            if (!other.isIndian()) {
                String key = other.getContactKey(this);
                if (key != null) {
                    cs.addMessage(See.only(other),
                        new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                            key, other, this));
                }
                cs.addHistory(other, new HistoryEvent(turn,
                        HistoryEvent.EventType.MEET_NATION)
                    .addStringTemplate("%nation%", getNationName()));
            }
        } else { // (serverPlayer.isEuropean)
            String key = getContactKey(other);
            if (key != null) {
                cs.addMessage(See.only(this),
                    new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                        key, this, other));
            }

            // History event for European players.
            cs.addHistory(this, new HistoryEvent(turn,
                    HistoryEvent.EventType.MEET_NATION)
                .addStringTemplate("%nation%", other.getNationName()));

            // Extra special meeting on first landing!
            if (other.isIndian()
                && !isNewLandNamed()
                && tile != null && tile.getOwner() == other) {
                welcomer = other;
            }
        }

        // Now make the contact properly.
        csChangeStance(Stance.PEACE, other, true, cs);
        setTension(other, new Tension(Tension.TENSION_MIN));
        other.setTension(this, new Tension(Tension.TENSION_MIN));

        return welcomer;
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
