/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;


/**
 * Objects of this class contains AI-information for a single {@link
 * Player} and is used for controlling this player.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public abstract class AIPlayer extends AIObject {

    private static final Logger logger = Logger.getLogger(AIPlayer.class.getName());

    /** A comparator to sort AI units by location. */
    private static final Comparator<AIUnit> aiUnitLocationComparator
        = new Comparator<AIUnit>() {
            @Override
            public int compare(AIUnit a1, AIUnit a2) {
                Location l1 = (a1 == null) ? null
                    : (a1.getUnit() == null) ? null
                    : a1.getUnit().getLocation();
                Location l2 = (a2 == null) ? null
                    : (a2.getUnit() == null) ? null
                    : a2.getUnit().getLocation();
                FreeColObject f1 = (l1 instanceof WorkLocation)
                    ? l1.getColony()
                    : (FreeColObject)l1;
                FreeColObject f2 = (l2 instanceof WorkLocation)
                    ? l2.getColony()
                    : (FreeColObject)l2;
                return FreeColObject.compareIds(f1, f2);
            }
        };

    /** The FreeColGameObject this AIObject contains AI-information for. */
    private ServerPlayer player;

    /** The PRNG to use for this AI player. */
    private Random aiRandom;

    /**
     * Temporary variable, used for debugging purposes only.
     * See setDebuggingConnection()
     */
    private Connection debuggingConnection;

    /**
     * Temporary variable, used to hold all AIUnit objects belonging
     * to this AI.  Any implementation of AIPlayer needs to make sure
     * this list is invalidated as necessary, using clearAIUnits(), so
     * that getAIUnitIterator() will create a new list.
     */
    private final List<AIUnit> aiUnits = new ArrayList<>();


    /**
     * Creates a new AI player.
     *
     * @param aiMain The <code>AIMain</code> the player exists within.
     * @param player The <code>ServerPlayer</code> to associate this
     *            AI player with.
     */
    public AIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player.getId());

        this.player = player;
        this.aiRandom = new Random(aiMain.getRandomSeed("Seed for " + getId()));

        uninitialized = false;
    }

    /**
     * Creates a new <code>AIPlayer</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIPlayer(AIMain aiMain,
                    FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);
        
        uninitialized = player == null;
    }


    /**
     * Gets the <code>Player</code> this <code>AIPlayer</code> is
     * controlling.
     *
     * @return The <code>Player</code>.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Sets the ServerPlayer this AIPlayer is controlling.
     * Used by implementing subclasses.
     *
     * @param p The new <code>Player</code>.
    protected void setPlayer(ServerPlayer p) {
        player = p;
    }
     */

    /**
     * Gets the PRNG to use for this player.
     *
     * @return A <code>Random<code> to use for this player.
     */
    public Random getAIRandom() {
        return aiRandom;
    }

    /**
     * Clears the cache of AI units.
     */
    protected void clearAIUnits() {
        aiUnits.clear();
    }

    /**
     * Add an AI unit owned by this player.
     *
     * @param aiUnit The <code>AIUnit</code> to add.
     */
    public void addAIUnit(AIUnit aiUnit) {
        aiUnits.add(aiUnit);
    }

    /**
     * Removes an AI unit owned by this player.
     *
     * @param aiUnit The <code>AIUnit</code> to remove.
     */
    public void removeAIUnit(AIUnit aiUnit) {
        aiUnit.dropTransport();
        aiUnit.changeMission(null);
        aiUnits.remove(aiUnit);
    }

    /**
     * Build the cache of AI units.
     */
    private void createAIUnits() {
        clearAIUnits();
        for (Unit u : getPlayer().getUnits()) {
            if (u.isDisposed()) {
                logger.warning("createAIUnits ignoring: " + u.getId());
                continue;
            }
            AIUnit a = getAIUnit(u);
            if (a != null) {
                if (a.getUnit() != u) {
                    throw new IllegalStateException("createAIUnits fail: " + u
                                                    + "/" + a);
                }
                aiUnits.add(a);
            } else {
                logger.warning("Could not find the AIUnit for: "
                               + u + " (" + u.getId() + ")");
            }
        }
    }

    /**
     * Gets the advantage of this AI player from the nation type.
     *
     * @return A short string stating the national advantage.
     */
    protected String getAIAdvantage() {
        final String prefix = "model.nationType.";
        String id = (player == null || player.getNationType() == null) ? ""
            : player.getNationType().getId();
        return (id.startsWith(prefix)) ? id.substring(prefix.length())
            : "";
    }

    /**
     * Gets the connection to the server.
     *
     * @return The connection that can be used when communication with the
     *     server.
     */
    public Connection getConnection() {
        return (debuggingConnection != null) ? debuggingConnection
            : ((DummyConnection) player.getConnection()).getOtherConnection();
    }

    /**
     * Sets the <code>Connection</code> to be used while communicating with
     * the server.
     *
     * This method is only used for debugging.
     *
     * @param debuggingConnection The <code>Connection</code> to be
     *     used for debugging.
     */
    public void setDebuggingConnection(Connection debuggingConnection) {
        this.debuggingConnection = debuggingConnection;
    }

    /**
     * Gets the AI colony corresponding to a given colony, if any.
     *
     * @param colony The <code>Colony</code> to look up.
     * @return The corresponding AI colony or null if not found.
     */
    public AIColony getAIColony(Colony colony) {
        return getAIMain().getAIColony(colony);
    }

    /**
     * Gets a list of the players AI colonies.
     *
     * @return A list of AI colonies.
     */
    public List<AIColony> getAIColonies() {
        List<AIColony> ac = new ArrayList<>();
        for (Colony colony : getPlayer().getColonies()) {
            AIColony a = getAIColony(colony);
            if (a != null) {
                ac.add(a);
            } else {
                logger.warning("Could not find the AIColony for: " + colony);
            }
        }
        return ac;
    }

    /**
     * Remove an AI colony.
     * Do nothing here, but European player classes will be more active.
     *
     * @param aic The <code>AIColony</code> to remove.
     */
    public void removeAIColony(AIColony aic) {}

    /**
     * Gets the AI unit corresponding to a given unit, if any.
     *
     * @param unit The <code>Unit</code> to look up.
     * @return The corresponding AI unit or null if not found.
     */
    protected AIUnit getAIUnit(Unit unit) {
        return getAIMain().getAIUnit(unit);
    }

    /**
     * Gets a list of AIUnits for the player.
     *
     * @return A list of AIUnits.
     */
    protected List<AIUnit> getAIUnits() {
        if (aiUnits.isEmpty()) createAIUnits();
        return new ArrayList<>(aiUnits);
    }

    /**
     * Returns an iterator over all the <code>AIUnit</code>s owned by this
     * player.
     *
     * @return The <code>Iterator</code>.
     */
    protected Iterator<AIUnit> getAIUnitIterator() {
        if (aiUnits.isEmpty()) createAIUnits();
        return aiUnits.iterator();
    }

    /**
     * Standard stance change determination.
     *
     * @param other The <code>Player</code> wrt consider stance.
     * @return The new <code>Stance</code>.
     */
    protected Stance determineStance(Player other) {
        return player.getStance(other)
            .getStanceFromTension(player.getTension(other));
    }

    /**
     * Counts the number of defenders allocated to a settlement.
     *
     * @param settlement The <code>Settlement</code> to examine.
     * @return The number of defenders.
     */
    public int getSettlementDefenders(Settlement settlement) {
        int defenders = 0;
        for (AIUnit au : getAIUnits()) {
            Mission dm = au.getMission(DefendSettlementMission.class);
            if (dm != null
                && dm.getTarget() == settlement
                && au.getUnit().getSettlement() == settlement) {
                defenders++;
            }
        }
        return defenders;
    }

    /**
     * Find out if a tile contains a suitable target for seek-and-destroy.
     *
     * FIXME: Package for access by a test only - necessary?
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param tile The <code>Tile</code> to attack into.
     * @return True if an attack can be launched.
     */
    public boolean isTargetValidForSeekAndDestroy(Unit attacker, Tile tile) {
        // Insist the attacker exists.
        if (attacker == null) return false;

        Player attackerPlayer = attacker.getOwner();

        // Determine the defending player.
        Settlement settlement = tile.getSettlement();
        Unit defender = tile.getDefendingUnit(attacker);
        Player defenderPlayer = (settlement != null) ? settlement.getOwner()
            : (defender != null) ? defender.getOwner()
            : null;

        // Insist there be a defending player.
        if (defenderPlayer == null) return false;

        // Can not attack our own units.
        if (attackerPlayer == defenderPlayer) return false;

        // If European, do not attack if not at war.
        // If native, do not attack if not at war and at least content.
        // Otherwise some attacks are allowed even if not at war.
        boolean atWar = attackerPlayer.atWarWith(defenderPlayer);
        if (attackerPlayer.isEuropean()) {
            if (!atWar) return false;
        } else if (attackerPlayer.isIndian()) {
            if (!atWar && attackerPlayer.getTension(defenderPlayer)
                .getLevel().compareTo(Tension.Level.CONTENT) <= 0) {
                return false;
            }
        }
        return attacker.canAttack(defender);
    }

    // Mission support

    /**
     * Log the missions of this player.
     *
     * @param reasons A map of reasons for the current mission by unit.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    protected void logMissions(java.util.Map<Unit, String> reasons,
                               LogBuilder lb) {
        List<AIUnit> units = getAIUnits();
        Collections.sort(units, aiUnitLocationComparator);
        for (AIUnit aiu : units) {
            Unit u = aiu.getUnit();
            String reason = reasons.get(u);
            if (reason == null) reason = "OMITTED";
            String ms = "NONE";
            Location target = null;
            if (aiu.hasMission()) {
                Mission m = aiu.getMission();
                ms = lastPart(m.getClass().toString(), ".");
                ms = ms.substring(0, ms.length() - "Mission".length());
                target = m.getTarget();
            }

            lb.add("\n  @",
                String.format("%-30s%-10s%-40s%-16s",
                    chop(u.getLocation().toShortString(), 30),
                    chop(reason, 10),
                    chop(u.toShortString(), 40),
                    chop(ms, 16)));
            if (target != null) lb.add("->", target);
        }
    }

    /**
     * Get a DefendSettlementMission for the current settlement of a
     * unit if it is badly defended.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new misison, or null if impossible or not worthwhile.
     */
    public Mission getDefendCurrentSettlementMission(AIUnit aiUnit) {
        if (DefendSettlementMission.invalidReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Location loc = unit.getLocation();
        final Settlement settlement = (loc == null) ? null
            : loc.getSettlement();
        return (settlement != null && settlement.isBadlyDefended())
            ? getDefendSettlementMission(aiUnit, settlement)
            : null;
    }

    /**
     * Gets a new DefendSettlementMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param target The <code>Settlement</code> to defend.
     * @return A new mission, or null if impossible.
     */
    public Mission getDefendSettlementMission(AIUnit aiUnit,
                                              Settlement target) {
        return (DefendSettlementMission.invalidReason(aiUnit) != null) ? null
            : new DefendSettlementMission(getAIMain(), aiUnit, target);
    }

    /**
     * Gets a new IdleAtSettlementMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to use.
     * @return A new mission, or null if impossible.
     */
    public Mission getIdleAtSettlementMission(AIUnit aiUnit) {
        return (IdleAtSettlementMission.invalidReason(aiUnit) != null) ? null
            : new IdleAtSettlementMission(getAIMain(), aiUnit);
    }
       
    /**
     * Gets a UnitSeekAndDestroyMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param range A maximum range to search for a target within.
     * @return A new mission, or null if impossible.
     */
    public Mission getSeekAndDestroyMission(AIUnit aiUnit, int range) {
        Location loc = null;
        if (UnitSeekAndDestroyMission.invalidReason(aiUnit) == null) {
            loc = UnitSeekAndDestroyMission.findTarget(aiUnit, range, false);
        }
        return (loc == null) ? null
            : getSeekAndDestroyMission(aiUnit, loc);
    }

    /**
     * Gets a UnitSeekAndDestroyMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The target <code>Location</code>.
     * @return A new mission, or null if impossible.
     */
    public Mission getSeekAndDestroyMission(AIUnit aiUnit, Location loc) {
        return (UnitSeekAndDestroyMission.invalidReason(aiUnit) != null
            || loc == null) ? null
            : new UnitSeekAndDestroyMission(getAIMain(), aiUnit, loc);
    }

    /**
     * Gets a new UnitWanderHostileMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    public Mission getWanderHostileMission(AIUnit aiUnit) {
        return (UnitWanderHostileMission.invalidReason(aiUnit) != null) ? null
            : new UnitWanderHostileMission(getAIMain(), aiUnit);
    }

    /**
     * Checks the integrity of this AIPlayer.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        if (player == null || player.isDisposed() || !player.isAI()) {
            result = -1;
        }
        return result;
    }


    // AI behaviour interface to be implemented by subclasses

    /**
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public abstract void startWorking();

    /**
     * Makes every unit perform their mission.
     *
     * @param aiUnits A list of <code>AIUnit</code>s to perform missions.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return A list of <code>AIUnit</code>s that have moves left.
     */
    protected List<AIUnit> doMissions(List<AIUnit> aiUnits, LogBuilder lb) {
        lb.add("\n  Do Missions:");
        List<AIUnit> result = new ArrayList<>();
        for (AIUnit aiu : aiUnits) {
            final Unit unit = aiu.getUnit();
            if (unit == null || unit.isDisposed()) continue;
            lb.add("\n  ", unit, " ");
            try {
                aiu.doMission(lb);
            } catch (Exception e) {
                logger.log(Level.WARNING, "doMissions failed for: " + aiu, e);
            }
            if (!unit.isDisposed() && unit.getMovesLeft() > 0) result.add(aiu);
        }
        return result;
    }

    /**
     * Adjusts the score of this proposed mission for this player type.
     * Subclasses should override and refine this.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to the target of this mission.
     * @param value The proposed value.
     * @param type The mission type.
     * @return A score representing the desirability of this mission.
     */
    public abstract int adjustMission(AIUnit aiUnit, PathNode path, Class type,
                                      int value);

    /**
     * Decides whether to accept an Indian demand, or not.
     *
     * @param unit The <code>Unit</code> making demands.
     * @param colony The <code>Colony</code> where demands are being made.
     * @param type The <code>GoodsType</code> demanded.
     * @param amount The amount of gold demanded.
     * @return True if this player accepts the demand.
     */
    public boolean indianDemand(Unit unit, Colony colony,
                                GoodsType type, int amount) {
        return false;
    }

    /**
     * Resolves a diplomatic trade offer.
     *
     * @param agreement The proposed <code>DiplomaticTrade</code>.
     * @return The <code>TradeStatus</code> to apply to the agreement.
     *
     */
    public TradeStatus acceptDiplomaticTrade(DiplomaticTrade agreement) {
        return TradeStatus.REJECT_TRADE;
    }

    /**
     * Called after another <code>Player</code> sends a
     * <code>trade</code> message
     *
     * @param goods The goods which we are going to offer
     */
    public abstract void registerSellGoods(Goods goods);

    /**
     * Called when another <code>Player</code> proposes to buy.
     *
     *
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param settlement The <code>Settlement</code> this player owns and
     *            which the given <code>Unit</code> is trading.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *     {@link net.sf.freecol.common.networking.NetworkConstants#NO_TRADE}.
     */
    public abstract int buyProposition(Unit unit, Settlement settlement,
                                       Goods goods, int gold);

    /**
     * Called when another <code>Player</code> proposes a sale.
     *
     *
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param settlement The <code>Settlement</code> this player owns and
     *            which the given <code>Unit</code> if trying to sell goods.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *     {@link net.sf.freecol.common.networking.NetworkConstants#NO_TRADE}.
     */
    public abstract int sellProposition(Unit unit, Settlement settlement,
                                        Goods goods, int gold);

    /**
     * Decides to accept a tax raise or not.
     * Overridden by the European player.
     *
     * @param tax The tax raise.
     * @return True if the raise is accepted.
     */
    public boolean acceptTax(int tax) {
        return false;
    }

    /**
     * Decides to accept an offer of mercenaries or not.
     * Overridden by the European player.
     *
     * @return True if the mercenaries are accepted.
     */
    public boolean acceptMercenaries() {
        return false;
    }

    /**
     * Selects the most useful founding father offered.
     * Overridden by EuropeanAIPlayers.
     *
     * @param ffs The founding fathers on offer.
     * @return The founding father selected.
     */
    public FoundingFather selectFoundingFather(List<FoundingFather> ffs) {
        return null;
    }


    // Serialization

    private static final String RANDOM_STATE_TAG = "randomState";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(RANDOM_STATE_TAG, Utils.getRandomState(aiRandom));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        player = xr.findFreeColGameObject(aiMain.getGame(), ID_ATTRIBUTE_TAG,
            ServerPlayer.class, (ServerPlayer)null, true);

        Random rnd = Utils.restoreRandomState(xr.getAttribute(RANDOM_STATE_TAG,
                                                              (String)null));
        aiRandom = (rnd != null) ? rnd
            : new Random(aiMain.getRandomSeed("Seed for " + getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        if (getPlayer() != null) uninitialized = false;
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "aiPlayer";
    }
}
