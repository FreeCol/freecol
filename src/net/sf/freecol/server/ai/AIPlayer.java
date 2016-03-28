/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.util.Utils;
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
        = Comparator.comparing(AIUnit::getUnit, Unit.locComparator);

    /** The FreeColGameObject this AIObject contains AI-information for. */
    private ServerPlayer player;

    /** The PRNG to use for this AI player. */
    private Random aiRandom;

    /**
     * Temporary variable, used for debugging purposes only.
     * See setDebuggingConnection()
     */
    private Connection debuggingConnection;

    /** The wrapper for the server. */
    private AIServerAPI serverAPI;

    
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
        this.serverAPI = new AIServerAPI(this);

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
        
        this.serverAPI = new AIServerAPI(this);
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
     * @return A <code>Random</code> to use for this player.
     */
    public Random getAIRandom() {
        return aiRandom;
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
            : ((DummyConnection)player.getConnection()).getOtherConnection();
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
     * Meaningfully named access to the server API.
     *
     * @return The <code>AIServerAPI</code> wrapper.
     */
    public AIServerAPI askServer() {
        return this.serverAPI;
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
        List<AIUnit> aiUnits = new ArrayList<>();
        for (Unit u : getPlayer().getUnits()) {
            if (u.isDisposed()) {
                logger.warning("getAIUnits ignoring: " + u.getId());
                continue;
            }
            AIUnit a = getAIUnit(u);
            if (a != null) {
                if (a.getUnit() != u) {
                    throw new IllegalStateException("getAIUnits fail: " + u
                                                    + "/" + a);
                }
                aiUnits.add(a);
            } else {
                logger.warning("Could not find the AIUnit for: "
                               + u + " (" + u.getId() + ")");
            }
        }
        return aiUnits;
    }

    /**
     * Removes an AI unit owned by this player.
     *
     * @param aiUnit The <code>AIUnit</code> to remove.
     */
    public void removeAIUnit(AIUnit aiUnit) {}

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
     * Decide whether to accept an Indian demand, or not.  Or for native
     * players, return the result of the demand.
     *
     * @param unit The <code>Unit</code> making demands.
     * @param colony The <code>Colony</code> where demands are being made.
     * @param type The <code>GoodsType</code> demanded.
     * @param amount The amount of gold demanded.
     * @param accept The acceptance state of the demand.
     * @return True if this player accepts the demand, false if the demand
     *     is rejected, null if no further consideration is required.
     */
    public Boolean indianDemand(Unit unit, Colony colony,
                                GoodsType type, int amount, Boolean accept) {
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
     *     {@link net.sf.freecol.common.model.Constants#NO_TRADE}.
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
     *     {@link net.sf.freecol.common.model.Constants#NO_TRADE}.
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


    // European players need to implement these for AIColony

    public int getNeededWagons(Tile tile) {
        return 0;
    }

    public int scoutsNeeded() {
        return 0;
    }

    public void completeWish(Wish w) {
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
    public static String getTagName() {
        return "aiPlayer";
    }
}
