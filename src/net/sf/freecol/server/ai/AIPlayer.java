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

import java.io.IOException;
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
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.Element;

/**
 * 
 * Objects of this class contains AI-information for a single {@link Player} and
 * is used for controlling this player.
 * 
 * <br />
 * <br />
 * 
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public abstract class AIPlayer extends AIObject {
    private static final Logger logger = Logger.getLogger(AIPlayer.class.getName());

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


    protected AIUnit getAIUnit(Unit unit) {
        return (AIUnit) getAIMain().getAIObject(unit);
    }

    /**
     * Helper method to let implementing subclasses clear aiUnits.
     */         
    protected void clearAIUnits() {
        aiUnits.clear();    
    }

    /**
     * Creates a list of AIUnits for the player.
     *
     * @return A list of AIUnits.
     */
    protected List<AIUnit> createAIUnits() {
        ArrayList<AIUnit> au = new ArrayList<AIUnit>();
        for (Unit u : getPlayer().getUnits()) {
            AIUnit a = getAIUnit(u);
            if (a != null) {
                au.add(a);
            } else {
                logger.warning("Could not find the AIUnit for: "
                               + u + " (" + u.getId() + ")");
            }
        }
        return au;
    }

    /**
     * Returns an iterator over all the <code>AIUnit</code>s owned by this
     * player.
     * 
     * @return The <code>Iterator</code>.
     */
    protected Iterator<AIUnit> getAIUnitIterator() {
        if (aiUnits.size() == 0) {
            aiUnits = createAIUnits();
        }
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
            AIColony a = (AIColony) getAIMain().getAIObject(colony.getId());
            if (a != null) {
                ac.add(a);
            } else {
                logger.warning("Could not find the AIColony for: " + colony);
            }
        }
        return ac.iterator();
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
     * 
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
     * Send some tiles to update to all players which can see them
     * 
     * @param tiles The tiles to update.
     */
    protected void sendUpdatedTilesToAll(ArrayList<Tile> tiles) {
        Iterator<Player> enemyPlayerIterator = getGame().getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
            if (equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
                continue;
            }
            try {
                Element updateElement = Message.createNewRootElement("update");
                boolean send = false;
                for(Tile tile : tiles) {
                    if (enemyPlayer.canSee(tile)) {
                        updateElement.appendChild(tile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));
                        send = true;
                    }
                }
                if (send) {
                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                        + enemyPlayer.getConnection());
            }
        }
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
     * @param The index of the unit to recruit in the recruitables list.
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
     * For REF-players: Checks if we have lost the war of independence.
     * 
     * TODO: "REF defeat" is a gameplay decision, not a decision an AI
     * should make for itself. Move this to another class (Player?).               
     */
    protected boolean checkForREFDefeat() {
        logger.finest("Entering method checkForREFDefeat");
        if (!getPlayer().isREF()) {
        	throw new IllegalStateException("Checking for REF player defeat when player not REF.");
        }
        
        List<Player> dominions = getPlayer().getDominionsAtWar();
        
        // Return if independence should not be granted:
        
        if (dominions.isEmpty()) {
            return false;
        }
        
        if (!getPlayer().getSettlements().isEmpty()) {
            return false;
        }
        
        if (getPlayer().hasManOfWar() && getPlayer().getNumberOfKingLandUnits() > 6) {
            return false;
        }
        
        for (Player p : dominions) {
            AIMessage.askGiveIndependence(getConnection(), p);
        }
        return true;
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
        boolean symmetric;
        if (other.getREFPlayer() == player
            && other.getPlayerType() == PlayerType.REBEL) {
            newStance = Stance.WAR;
            symmetric = true;
        } else {
            newStance = player.getStance(other).getStanceFromTension(player.getTension(other));
            symmetric = newStance == Stance.WAR;
        }
        if (newStance != player.getStance(other)) {
            getAIMain().getFreeColServer().getInGameController()
                .changeStance(player, newStance, other, symmetric);
        }
        return player.getStance(other);
    }

/* INTERFACE ******************************************************************/



    /**
     * 
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     * 
     * NOTE: For the moment, any implementation of this _must_ make sure
     * to call checkForREFDefeat() at the start of a turn,
     * if the player this AI works for isREF(). See TODO at that method.
     */
    public abstract void startWorking();

    /**
     * Returns an <code>Iterator</code> over all the
     * <code>TileImprovement</code>s needed by all of this player's colonies.
     * 
     * @return The <code>Iterator</code>.
     * @see net.sf.freecol.common.model.TileImprovement
     */
    public abstract Iterator<TileImprovementPlan> getTileImprovementPlanIterator();
    
    /**
     * Remove a <code>TileImprovementPlan</code> from the list
     */
    public abstract void removeTileImprovementPlan(TileImprovementPlan plan);
    
    /**
     * This is a temporary method which are used for forcing the computer
     * players into building more colonies. The method will be removed after the
     * proper code for deciding whether a colony should be built or not has been
     * implemented.
     * 
     * @return <code>true</code> if the AI should build more colonies.
     */
    public abstract boolean hasFewColonies();
    
    /**
     * Returns an <code>Iterator</code> for all the wishes. The items are
     * sorted by the {@link Wish#getValue value}, with the item having the
     * highest value appearing first in the <code>Iterator</code>.
     * 
     * @return The <code>Iterator</code>.
     * @see Wish
     */
    public abstract Iterator<Wish> getWishIterator();
    
    /**
     * Selects the most useful founding father offered.
     * 
     * @param foundingFathers The founding fathers on offer.
     * @return The founding father selected.
     */
    public abstract FoundingFather selectFoundingFather(List<FoundingFather> foundingFathers);
    
    /**
     * Decides whether to accept the monarch's tax raise or not.
     * 
     * @param tax The new tax rate to be considered.
     * @return <code>true</code> if the tax raise should be accepted.
     */
    public abstract boolean acceptTax(int tax);
    
    /**
     * Decides whether to accept an Indian demand, or not.
     * 
     * @param unit The unit making demands.
     * @param colony The colony where demands are being made.
     * @param goods The goods demanded.
     * @param gold The amount of gold demanded.
     * @return <code>true</code> if this <code>AIPlayer</code> accepts the
     *         indian demand and <code>false</code> otherwise.
     */
    public abstract boolean acceptIndianDemand(Unit unit, Colony colony, Goods goods, int gold);
    
    /**
     * Decides whether to accept a mercenary offer, or not.
     * 
     * @return <code>true</code> if this <code>AIPlayer</code> accepts the
     *         offer and <code>false</code> otherwise.
     */
    public abstract boolean acceptMercenaryOffer();
    
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
     * Writes this object to an XML stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected abstract void toXMLImpl(XMLStreamWriter out) throws XMLStreamException;
    
    /**
     * Reads information for this object from an XML stream.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    @Override
    protected abstract void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException;
    
    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "aiPlayer";
    }

}
