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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
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

    public static final int MAX_DISTANCE_TO_BRING_GIFT = 5;

    public static final int MAX_NUMBER_OF_GIFTS_BEING_DELIVERED = 1;

    public static final int MAX_DISTANCE_TO_MAKE_DEMANDS = 5;

    public static final int MAX_NUMBER_OF_DEMANDS = 1;

    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private ServerPlayer player;

    /**
     * Temporary variable, used for debugging purposes only.
     * See setDebuggingConnection()
     */
    private Connection debuggingConnection;

    /**
     * Temporary variable, used to hold all AIUnit objects belonging to this AI.
     * Any implementation of AIPlayer needs to make sure this list is invalidated
     * as necessary, using clearAIUnits(), so that getAIUnitIterator() will
     * create a new list.
     */
    private List<AIUnit> aiUnits = new ArrayList<AIUnit>();


    public AIPlayer(AIMain aiMain, String id) {
        super(aiMain, id);
    }

    /**
     * Returns the <code>Player</code> this <code>AIPlayer</code> is
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
     */
    protected void setPlayer(ServerPlayer p) {
        player = p;
    }

    /**
     * Returns the ID for this <code>AIPlayer</code>. This is the same as the
     * ID for the {@link Player} this <code>AIPlayer</code> controls.
     *
     * @return The ID.
     */
    @Override
    public String getId() {
        return player.getId();
    }

    /**
     * Gets the connection to the server.
     *
     * @return The connection that can be used when communication with the
     *         server.
     */
    public Connection getConnection() {
        if (debuggingConnection != null) {
            return debuggingConnection;
        } else {
            return ((DummyConnection) player.getConnection()).getOtherConnection();
        }
    }

    /**
     * Sets the <code>Connection</code> to be used while communicating with
     * the server.
     *
     * This method is only used for debugging.
     *
     * @param debuggingConnection The connection to be used for debugging.
     */
    public void setDebuggingConnection(Connection debuggingConnection) {
        this.debuggingConnection = debuggingConnection;
    }

    /**
     * Clears the cache of AI units.
     */
    protected void clearAIUnits() {
        aiUnits.clear();
    }

    /**
     * Build the cache of AI units.
     */
    private void createAIUnits() {
        clearAIUnits();
        for (Unit u : getPlayer().getUnits()) {
            AIUnit a = getAIUnit(u);
            if (a != null) {
                aiUnits.add(a);
            } else {
                logger.warning("Could not find the AIUnit for: "
                               + u + " (" + u.getId() + ")");
            }
        }
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
     * Returns an iterator over all the <code>AIColony</code>s owned by this
     * player.
     *
     * @return The <code>Iterator</code>.
     */
    protected Iterator<AIColony> getAIColonyIterator() {
        ArrayList<AIColony> ac = new ArrayList<AIColony>();
        for (Colony colony : getPlayer().getColonies()) {
            AIColony a = getAIColony(colony);
            if (a != null) {
                ac.add(a);
            } else {
                logger.warning("Could not find the AIColony for: " + colony);
            }
        }
        return ac.iterator();
    }

    /**
     * Gets the AI colony corresponding to a given colony, if any.
     *
     * @param colony The <code>Colony</code> to look up.
     * @return The corresponding AI colony or null if not found.
     */
    protected AIColony getAIColony(Colony colony) {
        return getAIMain().getAIColony(colony);
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

/* INTERFACE ******************************************************************/



    /**
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public abstract void startWorking();

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
     * Returns an <code>Iterator</code> for all the wishes. The items are
     * sorted by the {@link Wish#getValue value}, with the item having the
     * highest value appearing first in the <code>Iterator</code>.
     *
     * @return The <code>Iterator</code>.
     * @see Wish
     */
    //public abstract Iterator<Wish> getWishIterator();

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
     *         {@link net.sf.freecol.common.networking.NetworkConstants#NO_TRADE}.
     */
    public abstract int buyProposition(Unit unit, Settlement settlement, Goods goods, int gold);

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
     *         {@link net.sf.freecol.common.networking.NetworkConstants#NO_TRADE}.
     */
    public abstract int sellProposition(Unit unit, Settlement settlement, Goods goods, int gold);

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
    protected void determineStances() {
        logger.finest("Entering method determineStances");
        Player player = getPlayer();
        for (Player p : getGame().getPlayers()) {
            if (p != player && !p.isDead()) determineStance(p);
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    protected void abortInvalidMissions() {
        for (AIUnit au : getAIUnits()) {
            Mission mission = au.getMission();
            if (mission == null) continue;
            if (!mission.isValid()) {
                logger.finest("Abort invalid mission: " + mission
                    + " for: " + au.getUnit());
                au.setMission(null);
            }
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    protected void abortInvalidAndOneTimeMissions() {
        for (AIUnit au : getAIUnits()) {
            Mission mission = au.getMission();
            if (mission == null) continue;
            if (!mission.isValid()) {
                logger.finest("Abort invalid mission: " + mission
                              + " for: " + au.getUnit());
                au.setMission(null);
            } else if (mission.isOneTime()) {
                logger.finest("Abort one-time mission: " + mission
                              + " for: " + au.getUnit());
                au.setMission(null);
            }
        }
    }

    /**
     * Makes every unit perform their mission.
     */
    protected void doMissions() {
        logger.finest("Entering method doMissions");
        for (AIUnit au : getAIUnits()) {
            if (au.hasMission() && au.getMission().isValid()
                && !(au.getUnit().isOnCarrier())) {
                try {
                    au.doMission(getConnection());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "doMissions failed", e);
                }
            }
        }
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
        Player attackerPlayer = attacker.getOwner();

        // Insist the attacker exists.
        if (attacker == null) return false;

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

        // A naval unit can never attack a land unit or settlement,
        // but a land unit *can* attack a naval unit if it is on land.
        // Otherwise naval units can only fight at sea, land units
        // only on land.
        if (attacker.isNaval()) {
            if (settlement != null
                || !defender.isNaval() || defender.getTile().isLand()) {
                return false;
            }
        } else {
            if (defender != null && !defender.getTile().isLand()) {
                return false;
            }
        }

        // Otherwise, attack.
        return true;
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


    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        setPlayer((ServerPlayer) getAIMain()
            .getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE)));
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "aiPlayer";
    }
}
