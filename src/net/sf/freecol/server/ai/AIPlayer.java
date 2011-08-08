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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.IdleAtColonyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.UnitWanderMission;
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
     * Helper function for server communication - Ask the server
     * to train a unit in Europe on behalf of the AIPlayer.
     *
     * TODO: Move this to a specialized Handler class (AIEurope?)
     * TODO: Give protected access?
     *
     * @return the new AIUnit created by this action. May be null.
     */
    public AIUnit trainAIUnitInEurope(UnitType unitType) {
        if (unitType==null) {
            throw new IllegalArgumentException("Invalid UnitType.");
        }

        AIUnit aiUnit = null;
        Europe europe = player.getEurope();
        int n = europe.getUnitCount();

        if (AIMessage.askTrainUnitInEurope(getConnection(), unitType)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
        }
        return aiUnit;
    }

    /**
     * Helper function for server communication - Ask the server
     * to recruit a unit in Europe on behalf of the AIPlayer.
     *
     * TODO: Move this to a specialized Handler class (AIEurope?)
     * TODO: Give protected access?
     *
     * @param index The index of the unit to recruit in the recruitables list.
     * @return the new AIUnit created by this action. May be null.
     */
    public AIUnit recruitAIUnitInEurope(int index) {
        AIUnit aiUnit = null;
        Europe europe = player.getEurope();
        int n = europe.getUnitCount();

        // CHEAT: give the AI a selection ability
        final String selectAbility = "model.ability.selectRecruit";
        boolean canSelect = player.hasAbility(selectAbility);
        Ability ability = null;
        if (!canSelect) {
            ability = new Ability(selectAbility);
            player.getFeatureContainer().addAbility(ability);
        }
        if (AIMessage.askEmigrate(getConnection(), index+1)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
        }
        if (ability != null) {
            player.getFeatureContainer().removeAbility(ability);
        }
        return aiUnit;
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
     * Called after another <code>Player</code> sends a <code>trade</code> message
     *
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
            if (au.getMission() == null) continue;
            if (!au.getMission().isValid()) {
                logger.finest("Abort invalid mission for: " + au.getUnit());
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
            } else if (mission instanceof UnitWanderHostileMission
                       || mission instanceof UnitWanderMission
                       || mission instanceof IdleAtColonyMission
                       // TODO: Mission.isOneTime()
                       ) {
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
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.hasMission() && aiUnit.getMission().isValid()
                    && !(aiUnit.getUnit().isOnCarrier())) {
                try {
                    aiUnit.doMission(getConnection());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "doMissions failed", e);
                }
            }
        }
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
