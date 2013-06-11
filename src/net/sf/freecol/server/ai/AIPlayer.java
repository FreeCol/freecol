/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
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
    private List<AIUnit> aiUnits = new ArrayList<AIUnit>();


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
     */
    protected void setPlayer(ServerPlayer p) {
        player = p;
    }

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
     * Removes an AI unit owned by this player.
     *
     * @param aiUnit The <code>AIUnit</code> to remove.
     */
    public void removeAIUnit(AIUnit aiUnit) {
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
        List<AIColony> ac = new ArrayList<AIColony>();
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
        if (aiUnits.size() == 0) createAIUnits();
        return new ArrayList<AIUnit>(aiUnits);
    }

    /**
     * Returns an iterator over all the <code>AIUnit</code>s owned by this
     * player.
     *
     * @return The <code>Iterator</code>.
     */
    protected Iterator<AIUnit> getAIUnitIterator() {
        if (aiUnits.size() == 0) createAIUnits();
        return aiUnits.iterator();
    }

    /**
     * Standard stance change determination.  If a change occurs,
     * contact the server and propagate.
     *
     * @param other The <code>Player</code> wrt consider stance.
     * @return The stance, which may have been updated.
     */
    protected Stance determineStance(Player other) {
        Player player = getPlayer();
        Stance newStance;
        if (other.getREFPlayer() == player
            && other.getPlayerType() == PlayerType.REBEL) {
            newStance = Stance.WAR;
        } else {
            newStance = player.getStance(other)
                .getStanceFromTension(player.getTension(other));
        }
        if (newStance != player.getStance(other)) {
            getAIMain().getFreeColServer().getInGameController()
                .changeStance(player, newStance, other, true);
        }
        return player.getStance(other);
    }

    /**
     * Aborts the mission for a unit, but tries to recover the unit onto
     * a neighbouring carrier.
     *
     * @param aiU The <code>AIUnit</code> whose mission is ended.
     * @param why A reason for aborting the mission.
     */
    private void abortUnitMission(AIUnit aiU, String why) {
        aiU.abortMission(why);
        // There is a common stuffup where the AIs converge on the
        // same location on a small island.  First mover gets the
        // colony, the rest get stuck there when their mission is aborted.
        //
        // TODO: drop this when a more general `marooned unit rescue'
        // mission is written.
        Unit unit = aiU.getUnit();
        Tile tile = unit.getTile();
        if (!unit.isCarrier() && !unit.isOnCarrier() && tile != null
            && tile.getColony() == null) {
            for (Tile t : tile.getSurroundingTiles(1)) {
                for (Unit u : t.getUnitList()) {
                    if (u.getOwner() == unit.getOwner()
                        && u.isCarrier()
                        && u.canAdd(unit)
                        && unit.getMovesLeft() > 0) {
                        AIMessage.askEmbark(getAIMain().getAIUnit(u), unit,
                            tile.getDirection(t));
                        return; // Let the carrier update its transport list.
                    }
                }
            }
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    protected void abortInvalidMissions() {
        for (AIUnit au : getAIUnits()) {
            Mission mission = au.getMission();
            String reason = (mission == null) ? null : mission.invalidReason();
            if (reason != null) abortUnitMission(au, reason);
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    protected void abortInvalidAndOneTimeMissions() {
        for (AIUnit au : getAIUnits()) {
            Mission mission = au.getMission();
            String reason = (mission == null) ? null
                : (mission.isOneTime()) ? "oneTime"
                : mission.invalidReason();
            if (reason != null) abortUnitMission(au, reason);
        }
    }

    /**
     * Makes every unit perform their mission.
     */
    protected void doMissions() {
        for (AIUnit au : getAIUnits()) {
            try {
                au.doMission();
            } catch (Exception e) {
                logger.log(Level.WARNING, "doMissions failed for: " + au, e);
            }
        }
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
            Mission m = au.getMission();
            if (m instanceof DefendSettlementMission
                && ((DefendSettlementMission)m).getTarget() == settlement
                && au.getUnit().getSettlement() == settlement) {
                defenders++;
            }
        }
        return defenders;
    }

    /**
     * Find out if a tile contains a suitable target for seek-and-destroy.
     * TODO: Package for access by a test only - necessary?
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


    // Interface to be implemented by subclasses

    /**
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public abstract void startWorking();

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
     * Resolves a native demand.
     * One of goods/gold is significant.
     * Overridden by the European player.
     *
     * @param unit The native <code>Unit</code> making the demand.
     * @param colony The <code>Colony</code> being demanded of.
     * @param goods The <code>Goods</code> demanded (may be null).
     * @param gold The gold demanded (invalid if goods non-null).
     * @return The response of the player.
     */
    public boolean indianDemand(Unit unit, Colony colony,
                                Goods goods, int gold) {
        return false;
    }

    /**
     * Resolves a diplomatic trade offer.
     *
     * @param agreement The proposed <code>DiplomaticTrade</code>.
     * @return True if the agreement is accepted.
     */
    public abstract boolean acceptDiplomaticTrade(DiplomaticTrade agreement);

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
     * Determines the stances towards each player.
     * That is: should we declare war?
     * TODO: something better, that includes peacemaking.
     */
    public void determineStances() {
        logger.finest("Entering method determineStances");
        Player player = getPlayer();
        for (Player p : getGame().getPlayers()) {
            if (p != player && !p.isDead()) determineStance(p);
        }
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
