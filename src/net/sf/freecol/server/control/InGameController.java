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

package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.PlayerExploredTile;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * TODO: write class comment.
 */
public final class InGameController extends Controller {

    private static Logger logger = Logger.getLogger(InGameController.class.getName());

    public int debugOnlyAITurns = 0;

    private java.util.Map<String,java.util.Map<String, java.util.Map<String,Object>>> transactionSessions;
    
    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameController(FreeColServer freeColServer) {
        super(freeColServer);
        
        transactionSessions = new HashMap<String,java.util.Map<String, java.util.Map<String,Object>>>();
    }


    /**
     * Get a list of all server players, optionally excluding the supplied one.
     *
     * @param serverPlayer An optional <code>ServerPlayer</code> to exclude.
     * @return A list of all connected server players, perhaps except one.
     */
    public List<ServerPlayer> getOtherPlayers(ServerPlayer serverPlayer) {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        for (Player otherPlayer : getGame().getPlayers()) {
            ServerPlayer enemyPlayer = (ServerPlayer) otherPlayer;
            if (!enemyPlayer.equals(serverPlayer)
                && enemyPlayer.isConnected()) {
                result.add(enemyPlayer);
            }
        }
        return result;
    }

    // Magic cookies to specify an overriding of the standard object
    // handling in arguments to buildUpdate().
    public static enum UpdateType {
        ANIMATE,    // Animate a move
        ATTRIBUTE,  // Set an attribute on the final result
        PARTIAL,    // Do a partial update
        PRIVATE,    // Marker for private objects
        REMOVE,     // Remove an object
        STANCE,     // Set stance
        UPDATE      // The default mode
    };

    /**
     * Helper function to add objects for an animation to a list.
     *
     * @param objects The list of objects to add to.
     * @param unit The <code>Unit</code> that is moving.
     * @param oldLocation The old <code>Location</code>.
     * @param newTile The new <code>Tile</code>.
     */
    private static void addAnimate(List<Object> objects, Unit unit,
                                   Location oldLocation, Tile newTile) {
        addMore(objects, UpdateType.ANIMATE, unit, oldLocation, newTile);
    }

    /**
     * Helper function to add attributes to a list.
     *
     * @param objects The list of objects to add to.
     * @param attr The attribute.
     * @param value Its value.
     */
    private static void addAttribute(List<Object> objects, String attr,
                                     String value) {
        addMore(objects, UpdateType.ATTRIBUTE, attr, value);
    }

    /**
     * Helper function to add objects to a list.
     *
     * @param objects The list of objects to add to.
     * @param more The objects to add.
     */
    private static void addMore(List<Object> objects, Object... more) {
        for (Object o : more) objects.add(o);
    }

    /**
     * Helper function to add objects for a partial update to a list.
     *
     * @param objects The list of objects to add to.
     * @param fco The object to be partially updated.
     * @param more Strings for the fields to update.
     */
    private static void addPartial(List<Object> objects, FreeColObject fco,
                                   String... more) {
        addMore(objects, UpdateType.PARTIAL, fco);
        for (Object o : more) objects.add(o);
    }

    /**
     * Helper function to add a removal of an object to a list.
     * Useful for updates to other players when the object is not
     * really gone, just moves where it can not be seen.
     *
     * @param objects The list of objects to add to.
     * @param fcgo The object to be removed.
     * TODO: public until clean up use in InGameInputHandler.
     */
    public static void addRemove(List<Object> objects,
                                 FreeColGameObject fcgo) {
        addMore(objects, UpdateType.REMOVE, fcgo);
    }

    /**
     * Helper function to add objects for a stance change to a list.
     *
     * @param objects The list of objects to add to.
     * @param stance The <code>Stance</code> to change to.
     * @param player1 The first <code>ServerPlayer</code>.
     * @param player2 The second <code>ServerPlayer</code>.
     */
    private static void addStance(List<Object> objects, Stance stance,
                                  ServerPlayer player1, ServerPlayer player2) {
        addMore(objects, UpdateType.STANCE, stance, player1, player2);
    }

    /**
     * Build a generalized update.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to send the
     *            update to.
     * @param objects The objects to consider.
     * @return An element encapsulating an update of the objects to
     *         consider, or null if there is nothing to report.
     */
    public Element buildUpdate(ServerPlayer serverPlayer, Object... objects) {
        List<Object> objectList = new ArrayList<Object>();
        for (Object o : objects) objectList.add(o);
        return buildUpdate(serverPlayer, objectList);
    }

    /**
     * Build a generalized update.
     * Beware that removing an object does not necessarily update
     * its tile correctly on the client side--- if a tile update
     * is needed the tile should be supplied in the objects list.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to send the
     *            update to.
     * @param objects A <code>List</code> of objects to consider.
     * @return An element encapsulating an update of the objects to
     *         consider, or null if there is nothing to report.
     */
    public Element buildUpdate(ServerPlayer serverPlayer,
                               List<Object> objects) {
        Document doc = Message.createNewDocument();
        List<String> attributes = new ArrayList<String>();
        List<Element> extras = new ArrayList<Element>();
        Element multiple = doc.createElement("multiple");
        Element update = doc.createElement("update");
        Element messages = doc.createElement("addMessages");
        Element history = doc.createElement("addHistory");
        Element remove = doc.createElement("remove");

        for (int i = 0; i < objects.size(); i++) {
            Object o = objects.get(i);
            if (o == null) {
                continue;
            } else if (o instanceof UpdateType) {
                switch ((UpdateType) o) {
                case ANIMATE: // expect Unit Location Tile
                    if (i+3 < objects.size()
                        && objects.get(i+1) instanceof Unit
                        && objects.get(i+2) instanceof FreeColGameObject
                        && objects.get(i+3) instanceof Tile) {
                        Unit unit = (Unit) objects.get(i+1);
                        FreeColGameObject oldLocation = (FreeColGameObject) objects.get(i+2);
                        Tile newTile = (Tile) objects.get(i+3);
                        Element animate = buildAnimate(serverPlayer, doc, unit,
                                                       oldLocation, newTile);
                        if (animate != null) {
                            extras.add(animate);
                            // Clean up units that disappear.
                            if (!unit.isVisibleTo(serverPlayer)) {
                                unit.addToRemoveElement(remove);
                            }
                        }
                        i += 3;
                    } else {
                        throw new IllegalArgumentException("bogus ANIMATE");
                    }
                    break;
                case ATTRIBUTE: // expect String String
                    if (i+2 < objects.size()
                        && objects.get(i+1) instanceof String
                        && objects.get(i+2) instanceof String) {
                        attributes.add((String) objects.get(i+1));
                        attributes.add((String) objects.get(i+2));
                        i += 2;
                    } else {
                        throw new IllegalArgumentException("bogus ATTRIBUTE");
                    }
                    break;
                case PARTIAL: // expect FCO String String...
                    if (i+2 < objects.size()
                        && objects.get(i+1) instanceof FreeColObject
                        && objects.get(i+2) instanceof String) {
                        FreeColObject fco = (FreeColObject) objects.get(i+1);
                        // Count and collect the strings.
                        int n;
                        for (n = i+3; n < objects.size()
                                 && objects.get(n) instanceof String; n++);
                        n -= i+2;
                        String[] fields = new String[n];
                        for (int j = 0; j < n; j++) {
                            fields[j] = (String) objects.get(i+2+j);
                        }
                        // Make a partial update.
                        update.appendChild(fco.toXMLElement(null, doc, true,
                                                            false, fields));
                        i += n+1;
                    } else {
                        throw new IllegalArgumentException("bogus PARTIAL");
                    }
                    break;
                case PRIVATE: // ignore
                    break;
                case REMOVE: // expect FreeColGameObject
                    if (i+1 < objects.size()
                        && objects.get(i+1) instanceof FreeColGameObject) {
                        FreeColGameObject fcgo = (FreeColGameObject) objects.get(i+1);
                        fcgo.addToRemoveElement(remove);
                        i += 1;
                    } else {
                        throw new IllegalArgumentException("bogus REMOVE");
                    }
                    break;
                case STANCE: // expect Stance Player Player
                    if (i+3 < objects.size()
                        && objects.get(i+1) instanceof Stance
                        && objects.get(i+2) instanceof ServerPlayer
                        && objects.get(i+3) instanceof ServerPlayer) {
                        Element setStance = buildStance(serverPlayer, doc,
                                (Stance) objects.get(i+1),
                                (ServerPlayer) objects.get(i+2),
                                (ServerPlayer) objects.get(i+3));
                        if (setStance != null) extras.add(setStance);
                        i += 3;
                    } else {
                        throw new IllegalArgumentException("bogus STANCE");
                    }
                    break;
                case UPDATE: // Syntactic sugar
                    break;
                }
            } else if (o instanceof ModelMessage) {
                // Always send message objects
                ((ModelMessage) o).addToOwnedElement(messages, serverPlayer);
            } else if (o instanceof HistoryEvent) {
                // Always send history objects
                ((HistoryEvent) o).addToOwnedElement(history, serverPlayer);
            } else if (o instanceof FreeColGameObject) {
                FreeColGameObject fcgo = (FreeColGameObject) o;
                if (fcgo.isDisposed()) {
                    // Always remove disposed objects
                    fcgo.addToRemoveElement(remove);
                } else if (fcgo instanceof Ownable
                           && ((Ownable) fcgo).getOwner() == (Player) serverPlayer) {
                    // Always update our own objects
                    update.appendChild(fcgo.toXMLElement(serverPlayer, doc));
                } else if (fcgo instanceof Unit) {
                    // Only update units that can be seen
                    Unit unit = (Unit) fcgo;
                    if (unit.isVisibleTo(serverPlayer)) {
                        update.appendChild(unit.toXMLElement(serverPlayer, doc));
                    }
                } else if (fcgo instanceof Settlement) {
                    // Only update settlements that can be seen
                    Tile tile = ((Settlement) fcgo).getTile();
                    if (serverPlayer.canSee(tile)) {
                        update.appendChild(fcgo.toXMLElement(serverPlayer, doc));
                    }
                } else if (fcgo instanceof Tile) {
                    // Only update tiles that can be seen
                    Tile tile = (Tile) fcgo;
                    if (serverPlayer.canSee(tile)) {
                        update.appendChild(tile.toXMLElement(serverPlayer, doc, false, false));
                    }
                } else if (fcgo instanceof Region) {
                    // Always update regions
                    update.appendChild(fcgo.toXMLElement(serverPlayer, doc));
                } else {
                    logger.warning("Attempt to update hidden object: "
                                   + fcgo.getId());
                }
            } else {
                throw new IllegalStateException("Bogus object: "
                                                + o.toString());
            }
        }

        // Decide what to return.  If there are several parts with children
        // then return multiple, if there is one viable part, return that,
        // else null.  Always put animate+setStance first, remove last.
        int n = 0;
        Element child = null;
        Element result;
        while (extras.size() > 0) {
            child = extras.remove(0);
            multiple.appendChild(child);
            n++;
        }
        if (update.hasChildNodes()) {
            multiple.appendChild(update);
            child = update;
            n++;
        }
        if (messages.hasChildNodes()) {
            multiple.appendChild(messages);
            child = messages;
            n++;
        }
        if (history.hasChildNodes()) {
            multiple.appendChild(history);
            child = history;
            n++;
        }
        if (remove.hasChildNodes()) {
            multiple.appendChild(remove);
            child = remove;
            n++;
        }
        switch (n) {
        case 0:
            return null;
        case 1:
            multiple.removeChild(child);
            doc.appendChild(child);
            result = child;
            break;
        default:
            doc.appendChild(multiple);
            result = multiple;
            break;
        }
        // Finally add any attributes
        for (int i = 0; i < attributes.size(); i += 2) {
            result.setAttribute(attributes.get(i), attributes.get(i+1));
        }

        return result;
    }

    /**
     * Build an "animateMove" element for an update.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to update.
     * @param doc The owner <code>Document</code> to build the element in.
     * @param unit The <code>Unit</code> that is moving.
     * @param oldLocation The location from which the unit is moving.
     * @param newTile The <code>Tile</code> to which the unit is moving.
     * @return An "animateMove" element, or null if the move can not be
     *         seen by the serverPlayer.
     */
    private Element buildAnimate(ServerPlayer serverPlayer, Document doc,
                                 Unit unit,
                                 FreeColGameObject oldLocation, Tile newTile) {
        Tile oldTile = ((Location) oldLocation).getTile();
        boolean seeOld = unit.getOwner() == serverPlayer
            || (serverPlayer.canSee(oldTile) && oldLocation instanceof Tile
                && oldTile.getSettlement() == null);
        boolean seeNew = unit.isVisibleTo(serverPlayer);
        if (seeOld || seeNew) {
            Element element = doc.createElement("animateMove");
            element.setAttribute("unit", unit.getId());
            element.setAttribute("oldTile", oldTile.getId());
            element.setAttribute("newTile", newTile.getId());
            if (!seeOld) {
                // We can not rely on the unit that is about to move
                // being present on the client side, and it is needed
                // before we can run the animation, so it is appended
                // to animateMove.
                element.appendChild(unit.toXMLElement(serverPlayer, doc));
            }
            return element;
        }
        return null;
    }

    /**
     * Build an "setStance" element for an update.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to update.
     * @param doc The owner <code>Document</code> to build the element in.
     * @param stance The <code>Stance</code> to set.
     * @param player1 The first <code>ServerPlayer</code> to set stance for.
     * @param player2 The second <code>ServerPlayer</code> to set stance for.
     * @return A "setStance" element, or null if the stance change is not
     *         for the serverPlayer.
     */
    private Element buildStance(ServerPlayer serverPlayer, Document doc,
                                Stance stance,
                                ServerPlayer player1, ServerPlayer player2) {
        if (serverPlayer == player1 || serverPlayer == player2
            || stance == Stance.WAR) {
            Element element = doc.createElement("setStance");
            element.setAttribute("stance", stance.toString());
            element.setAttribute("first", player1.getId());
            element.setAttribute("second", player2.getId());
            return element;
        }
        return null;
    }

    /**
     * Deprecated. Going away soon.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param objects The objects to consider.
     */
    public void sendUpdateToAll(ServerPlayer serverPlayer, Object... objects) {
        List<Object> objectList = new ArrayList<Object>();
        for (Object o : objects) objectList.add(o);
        sendToOthers(serverPlayer, objectList);
    }

    /**
     * Deprecated. Going away soon.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param objects The objects to consider.
     */
    public void sendUpdateToAll(ServerPlayer serverPlayer, List<Object> objects) {
        sendToOthers(serverPlayer, objects);
    }

    /**
     * Send an update to all players except one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param objects The objects to consider.
     */
    public void sendToOthers(ServerPlayer serverPlayer, Object... objects) {
        List<Object> objectList = new ArrayList<Object>();
        for (Object o : objects) objectList.add(o);
        sendToOthers(serverPlayer, objectList);
    }

    /**
     * Send an update to all players except one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param objects The objects to consider.
     */
    public void sendToOthers(ServerPlayer serverPlayer,
                             List<Object> allObjects) {
        // Strip off all objects at PRIVATE onward before updating.
        List<Object> objects = new ArrayList<Object>();
        for (Object o : allObjects) {
            if (o == UpdateType.PRIVATE) break;
            objects.add(o);
        }
        if (objects.isEmpty()) return;

        // Now send each other player their update.
        for (ServerPlayer other : getOtherPlayers(serverPlayer)) {
            sendElement(other, objects);
        }
    }

    /**
     * Send an element to all players except one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param element An <code>Element</code> to send.
     */
    public void sendToOthers(ServerPlayer serverPlayer, Element element) {
        if (element != null) {
            for (ServerPlayer other : getOtherPlayers(serverPlayer)) {
                sendElement(other, element);
            }
        }
    }

    /**
     * Send an element to all players.
     *
     * @param element The <code>Element</code> to send.
     */
    public void sendToAll(Element element) {
        if (element != null) {
            for (ServerPlayer other : getOtherPlayers(null)) {
                sendElement(other, element);
            }
        }
    }

    /**
     * Send an element to a specific player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to update.
     * @param objects A list of objects to build an <code>Element</code> with.
     */
    public void sendElement(ServerPlayer serverPlayer, List<Object> objects) {
        sendElement(serverPlayer, buildUpdate(serverPlayer, objects));
    }

    /**
     * Send an element to a specific player.
     *
     * @param player The <code>ServerPlayer</code> to update.
     * @param element An <code>Element</code> containing the update.
     */
    public void sendElement(ServerPlayer player, Element element) {
        if (element != null) {
            try {
                player.getConnection().sendAndWait(element);
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }
    }


    /**
     * Ends the turn of the given player.
     * 
     * @param player The player to end the turn of.
     */
    public void endTurn(ServerPlayer player) {
        /* BEGIN FIX
         * 
         * TODO: Remove this temporary fix for bug:
         *       [ 1709196 ] Waiting for next turn (inifinite wait)
         *       
         *       This fix can be removed when FIFO ordering of
         *       of network messages is working correctly.
         *       (scheduled to be fixed as part of release 0.8.0)
         */
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        // END FIX
        
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer oldPlayer = (ServerPlayer) getGame().getCurrentPlayer();
        
        if (oldPlayer != player) {
            throw new IllegalArgumentException("It is not "
                + player.getName() + "'s turn, it is "
                + ((oldPlayer == null) ? "noone" : oldPlayer.getName()) + "'s!");
        }
        
        player.clearModelMessages();
        freeColServer.getModelController().clearTaskRegister();

        Player winner = checkForWinner();
        if (winner != null && (!freeColServer.isSingleplayer() || !winner.isAI())) {
            Element gameEndedElement = Message.createNewRootElement("gameEnded");
            gameEndedElement.setAttribute("winner", winner.getId());
            sendToAll(gameEndedElement);
            
            // TODO: Remove when the server can properly revert to a pre-game state:
            if (FreeCol.getFreeColClient() == null) {
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 20000);
            }
            return;
        }
        
        ServerPlayer newPlayer = (ServerPlayer) nextPlayer();
        
        if (newPlayer != null 
            && !newPlayer.isAI()
            && (!newPlayer.isConnected() || debugOnlyAITurns > 0)) {
            endTurn(newPlayer);
            return;
        }
    }

    /**
     * Remove a standard yearly amount of storable goods, and
     * a random extra amount of a random type.
     * Send the market and change messages to the player.
     * This method is public so it can be use in the Market test code.
     *
     * @param player The player whose market is to be updated.
     */
    public void yearlyGoodsRemoval(ServerPlayer player) {
        List<ModelMessage> messages = new ArrayList<ModelMessage>();
        List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
        Market market = player.getMarket();

        // Pick a random type of goods to remove an extra amount of.
        GoodsType removeType;
        do {
            int randomGoods = getPseudoRandom().nextInt(goodsTypes.size());
            removeType = goodsTypes.get(randomGoods);
        } while (!removeType.isStorable());

        // Remove standard amount, and the extra amount.
        for (GoodsType type : goodsTypes) {
            if (type.isStorable() && market.hasBeenTraded(type)) {
                int amount = getGame().getTurn().getNumber() / 10;
                if (type == removeType && amount > 0) {
                    amount += getPseudoRandom().nextInt(2 * amount + 1);
                }
                if (amount > 0) {
                    market.addGoodsToMarket(type, -amount);
                }
            }
            if (market.hasPriceChanged(type)) {
                messages.add(market.makePriceChangeMessage(type));
                market.flushPriceChange(type);
            }
        }

        // Update the client
        Element element = Message.createNewRootElement("multiple");
        Document doc = element.getOwnerDocument();
        Element update = doc.createElement("update");
        element.appendChild(update);
        update.appendChild(market.toXMLElement(player, doc));
        Element mess = doc.createElement("addMessages");
        for (ModelMessage m : messages) {
            m.addToOwnedElement(mess, player);
        }
        if (mess.hasChildNodes()) {
            element.appendChild(mess);
        }
        try {
            player.getConnection().send(element);
        } catch (Exception e) {
            logger.warning("Error sending yearly market update to "
                           + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Create an element to mark a player as dead, inform other players, and
     * remove any leftover units.
     *
     * @param serverPlayer The player to kill.
     * @return An element to kill the player.
     */
    private Element killPlayerElement(ServerPlayer serverPlayer) {
        Element element = Message.createNewRootElement("multiple");
        Document doc = element.getOwnerDocument();

        Element update = doc.createElement("update");
        element.appendChild(update);
        Player player = (Player) serverPlayer;
        player.setDead(true);
        update.appendChild(player.toXMLElementPartial(doc, "dead"));

        if (!serverPlayer.getUnits().isEmpty()) {
            Element remove = doc.createElement("remove");
            element.appendChild(remove);
            List<Unit> unitList = new ArrayList<Unit>(serverPlayer.getUnits());
            for (Unit unit : unitList) {
                serverPlayer.removeUnit(unit);
                unit.addToRemoveElement(remove);
                unit.dispose();
            }
        }

        Element messages = doc.createElement("addMessages");
        element.appendChild(messages);
        String messageId = serverPlayer.isEuropean() ? "model.diplomacy.dead.european"
            : "model.diplomacy.dead.native";
        ModelMessage m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                          messageId, serverPlayer)
            .addStringTemplate("%nation%", serverPlayer.getNationName());
        m.addToOwnedElement(messages, serverPlayer);

        Element setDeadElement = doc.createElement("setDead");
        element.appendChild(setDeadElement);
        setDeadElement.setAttribute("player", serverPlayer.getId());

        return element;
    }


    /**
     * Sets a new current player and notifies the clients.
     * @return The new current player.
     */
    private Player nextPlayer() {
        final FreeColServer freeColServer = getFreeColServer();
        
        if (!isHumanPlayersLeft()) {
            getGame().setCurrentPlayer(null);
            return null;
        }
        
        if (getGame().isNextPlayerInNewTurn()) {
            getGame().newTurn();
            if (getGame().getTurn().getAge() > 1
                && !getGame().getSpanishSuccession()) {
                checkSpanishSuccession();
            }
            if (debugOnlyAITurns > 0) {
                debugOnlyAITurns--;
            }
            Element newTurnElement = Message.createNewRootElement("newTurn");
            sendToAll(newTurnElement);
        }
        
        ServerPlayer newPlayer = (ServerPlayer) getGame().getNextPlayer();
        getGame().setCurrentPlayer(newPlayer);
        if (newPlayer == null) {
            getGame().setCurrentPlayer(null);
            return null;
        }
        
        synchronized (newPlayer) {
            if (Player.checkForDeath(newPlayer)) {
                newPlayer.setDead(true);
                Element element = killPlayerElement(newPlayer);
                sendToAll(element);
                logger.info(newPlayer.getNation() + " is dead.");
                return nextPlayer();
            }
        }
        
        if (newPlayer.isEuropean()) {
            yearlyGoodsRemoval(newPlayer);

            if (newPlayer.getCurrentFather() == null && newPlayer.getSettlements().size() > 0) {
                chooseFoundingFather(newPlayer);
            }
            if (newPlayer.getMonarch() != null) {
                monarchAction(newPlayer);
            }
            bombardEnemyShips(newPlayer);
        }
        else if (newPlayer.isIndian()) {
            
            for (IndianSettlement indianSettlement: newPlayer.getIndianSettlements()) {
                if (indianSettlement.checkForNewMissionaryConvert()) {
                    // an Indian brave gets converted by missionary
                    Unit missionary = indianSettlement.getMissionary();
                    ServerPlayer european = (ServerPlayer) missionary.getOwner();
                    // search for a nearby colony
                    Tile settlementTile = indianSettlement.getTile();
                    Tile targetTile = null;
                    Iterator<Position> ffi = getGame().getMap().getFloodFillIterator(settlementTile.getPosition());
                    while (ffi.hasNext()) {
                        Tile t = getGame().getMap().getTile(ffi.next());
                        if (settlementTile.getDistanceTo(t) > IndianSettlement.MAX_CONVERT_DISTANCE) {
                            break;
                        }
                        if (t.getSettlement() != null && t.getSettlement().getOwner() == european) {
                            targetTile = t;
                            break;
                        }
                    }
        
                    if (targetTile != null) {
                        
                        List<UnitType> converts = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.convert");
                        if (converts.size() > 0) {
                            // perform the conversion from brave to convert in the server
                            Unit brave = indianSettlement.getUnitIterator().next();
                            String nationId = brave.getOwner().getNationID();
                            brave.dispose();
                            ModelController modelController = getGame().getModelController();
                            int random = modelController.getRandom(indianSettlement.getId() + "getNewConvertType", converts.size());
                            UnitType unitType = converts.get(random);
                            Unit unit = modelController.createUnit(indianSettlement.getId() + "newTurn100missionary", targetTile,
                                    european, unitType);
                            // and send update information to the client
                            try {
                                Element updateElement = Message.createNewRootElement("newConvert");
                                updateElement.setAttribute("nation", nationId);
                                updateElement.setAttribute("colonyTile", targetTile.getId());
                                updateElement.appendChild(unit.toXMLElement(european,updateElement.getOwnerDocument()));
                                european.getConnection().send(updateElement);
                                logger.info("New convert created for " + european.getName() + " with ID=" + unit.getId());
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + european.getName());
                            }
                        }
                    }
                }
            }
        }
        
        Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
        setCurrentPlayerElement.setAttribute("player", newPlayer.getId());
        sendToAll(setCurrentPlayerElement);
        
        return newPlayer;
    }

    private void checkSpanishSuccession() {
        boolean rebelMajority = false;
        Player weakestAIPlayer = null;
        Player strongestAIPlayer = null;
        java.util.Map<Player, Element> documentMap = new HashMap<Player, Element>();
        for (Player player : getGame().getPlayers()) {
            documentMap.put(player, Message.createNewRootElement("spanishSuccession"));
            if (player.isEuropean()) {
                if (player.isAI() && !player.isREF()) {
                    if (weakestAIPlayer == null
                        || weakestAIPlayer.getScore() > player.getScore()) {
                        weakestAIPlayer = player;
                    }
                    if (strongestAIPlayer == null
                        || strongestAIPlayer.getScore() < player.getScore()) {
                        strongestAIPlayer = player;
                    }
                } else if (player.getSoL() > 50) {
                    rebelMajority = true;
                }
            }
        }

        if (rebelMajority
            && weakestAIPlayer != null
            && strongestAIPlayer != null
            && weakestAIPlayer != strongestAIPlayer) {
            documentMap.remove(weakestAIPlayer);
            for (Element element : documentMap.values()) {
                element.setAttribute("loser", weakestAIPlayer.getId());
                element.setAttribute("winner", strongestAIPlayer.getId());
            }
            for (Colony colony : weakestAIPlayer.getColonies()) {
                colony.changeOwner(strongestAIPlayer);
                for (Entry<Player, Element> entry : documentMap.entrySet()) {
                    if (entry.getKey().canSee(colony.getTile())) {
                        entry.getValue().appendChild(colony.toXMLElement(entry.getKey(),
                                                                         entry.getValue().getOwnerDocument()));
                    }
                }
            }
            for (Unit unit : weakestAIPlayer.getUnits()) {
                unit.setOwner(strongestAIPlayer);
                for (Entry<Player, Element> entry : documentMap.entrySet()) {
                    if (entry.getKey().canSee(unit.getTile())) {
                        entry.getValue().appendChild(unit.toXMLElement(entry.getKey(),
                                                                       entry.getValue().getOwnerDocument()));
                    }
                }
            }
            for (Entry<Player, Element> entry : documentMap.entrySet()) {
                try {
                    ((ServerPlayer) entry.getKey()).getConnection().send(entry.getValue());
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + entry.getKey().getName());
                }
            }
            weakestAIPlayer.setDead(true);
            getGame().setSpanishSuccession(true);
        }
    }
    
    private boolean isHumanPlayersLeft() {
        for (Player player : getFreeColServer().getGame().getPlayers()) {
            if (!player.isDead() && !player.isAI() && ((ServerPlayer) player).isConnected()) {
                return true;
            }
        }
        return false;
    }

    private void chooseFoundingFather(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        Thread t = new Thread(FreeCol.SERVER_THREAD+"FoundingFather-thread") {
                public void run() {
                    List<FoundingFather> randomFoundingFathers = getRandomFoundingFathers(nextPlayer);
                    boolean atLeastOneChoice = false;
                    Element chooseFoundingFatherElement = Message.createNewRootElement("chooseFoundingFather");
                    for (FoundingFather father : randomFoundingFathers) {
                        chooseFoundingFatherElement.setAttribute(father.getType().toString(),
                                                                 father.getId());
                        atLeastOneChoice = true;
                    }
                    if (!atLeastOneChoice) {
                        nextPlayer.setCurrentFather(null);
                    } else {
                        Connection conn = nextPlayer.getConnection();
                        if (conn != null) {
                            try {
                                Element reply = conn.ask(chooseFoundingFatherElement);
                                FoundingFather father = FreeCol.getSpecification().
                                    getFoundingFather(reply.getAttribute("foundingFather"));
                                if (!randomFoundingFathers.contains(father)) {
                                    throw new IllegalArgumentException();
                                }
                                nextPlayer.setCurrentFather(father);
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                        }
                    }
                }
            };
        t.start();
    }

    /**
     * Build a list of random FoundingFathers, one per type.
     * Do not include any the player has or are not available.
     * 
     * @param player The <code>Player</code> that should pick a founding
     *            father from this list.
     * @return A list of FoundingFathers.
     */
    private List<FoundingFather> getRandomFoundingFathers(Player player) {
        // Build weighted random choice for each father type
        Specification spec = FreeCol.getSpecification();
        int age = getGame().getTurn().getAge();
        EnumMap<FoundingFatherType, List<RandomChoice<FoundingFather>>> choices
            = new EnumMap<FoundingFatherType,
                List<RandomChoice<FoundingFather>>>(FoundingFatherType.class);
        for (FoundingFather father : spec.getFoundingFathers()) {
            if (!player.hasFather(father) && father.isAvailableTo(player)) {
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
                FoundingFather father
                    = RandomChoice.getWeightedRandom(getPseudoRandom(), rc);
                randomFathers.add(father);
                logMessage += ":" + father.getNameKey();
            }
        }
        logger.info(logMessage);
        return randomFathers;
    }

    /**
     * Checks if anybody has won the game and returns that player.
     * 
     * @return The <code>Player</code> who have won the game or <i>null</i>
     *         if the game is not finished.
     */
    public Player checkForWinner() {
        List<Player> players = getGame().getPlayers();
        GameOptions go = getGame().getGameOptions();
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            for (Player player : players) {
                if (!player.isAI() && player.getPlayerType() == PlayerType.INDEPENDENT) {
                    return player;
                }
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            Player winner = null;
            for (Player player : players) {
                if (!player.isDead() && player.isEuropean() && !player.isREF()) {
                    if (winner != null) {
                        // There is more than one european player alive:
                        winner = null;
                        break;
                    } else {
                        winner = player;
                    }
                }
            }
            if (winner != null) {
                return winner;
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
            Player winner = null;
            for (Player player : players) {
                if (!player.isDead() && !player.isAI()) {
                    if (winner != null) {
                        // There is more than one human player alive:
                        winner = null;
                        break;
                    } else {
                        winner = player;
                    }
                }
            }
            if (winner != null) {
                return winner;
            }
        }
        return null;
    }

    /**
     * Checks for monarch actions.
     * 
     * @param player The server player.
     */
    private void monarchAction(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        final Connection conn = player.getConnection();
        if (conn == null) return;
        Thread t = new Thread("monarchAction") {
                public void run() {
                    try {
                        Monarch monarch = nextPlayer.getMonarch();
                        MonarchAction action = monarch.getAction();
                        Element monarchActionElement = Message.createNewRootElement("monarchAction");
                        monarchActionElement.setAttribute("action", String.valueOf(action));
                        switch (action) {
                        case RAISE_TAX:
                            int oldTax = nextPlayer.getTax();
                            int newTax = monarch.getNewTax(MonarchAction.RAISE_TAX);
                            if (newTax > 100) {
                                logger.warning("Tax rate exceeds 100 percent.");
                                return;
                            }
                            Goods goods = nextPlayer.getMostValuableGoods();
                            if (goods == null) {
                                return;
                            }
                            monarchActionElement.setAttribute("amount", String.valueOf(newTax));
                            // TODO: don't use localized name
                            monarchActionElement.setAttribute("goods", Messages.message(goods.getNameKey()));
                            monarchActionElement.setAttribute("force", String.valueOf(false));
                            try {
                                nextPlayer.setTax(newTax); // to avoid cheating
                                Element reply = conn.ask(monarchActionElement);
                                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                            
                                if (!accepted) {
                                    Colony colony = (Colony) goods.getLocation();
                                    if (colony.getGoodsCount(goods.getType()) >= goods.getAmount()) {
                                        nextPlayer.setTax(oldTax); // player hasn't accepted, restoring tax
                                        Element removeGoodsElement = Message.createNewRootElement("removeGoods");
                                        colony.removeGoods(goods);
                                        nextPlayer.setArrears(goods);
                                        colony.getFeatureContainer().addModifier(Modifier
                                            .createTeaPartyModifier(getGame().getTurn()));
                                        removeGoodsElement.appendChild(goods.toXMLElement(nextPlayer, removeGoodsElement
                                                                                          .getOwnerDocument()));
                                        conn.send(removeGoodsElement);
                                    } else {
                                        // player has cheated and removed goods from colony, don't restore tax
                                        monarchActionElement.setAttribute("force", String.valueOf(true));
                                        conn.send(monarchActionElement);
                                    }
                                }
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                        case LOWER_TAX:
                            int taxLowered = monarch.getNewTax(MonarchAction.LOWER_TAX);
                            if (taxLowered < 0) {
                                logger.warning("Tax rate less than 0 percent.");
                                return;
                            }
                            monarchActionElement.setAttribute("amount", String.valueOf(taxLowered));
                            try {
                                nextPlayer.setTax(taxLowered); // to avoid cheating
                                conn.send(monarchActionElement);
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                        case ADD_TO_REF:
                            List<AbstractUnit> unitsToAdd = monarch.addToREF();
                            monarch.addToREF(unitsToAdd);
                            Element additionElement = monarchActionElement.getOwnerDocument().createElement("addition");
                            for (AbstractUnit unit : unitsToAdd) {
                                additionElement.appendChild(unit.toXMLElement(nextPlayer,additionElement.getOwnerDocument()));
                            }
                            monarchActionElement.appendChild(additionElement);
                            try {
                                conn.send(monarchActionElement);
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                        case DECLARE_WAR:
                            Player enemy = monarch.declareWar();
                            if (enemy == null) {
                                // this should not happen
                                logger.warning("Declared war on nobody.");
                                return;
                            }
                            // We also need to change the tension to avoid the AI player declaring cease-fire right away
                            if(nextPlayer.isAI()){
                                nextPlayer.modifyTension(enemy, Tension.TENSION_ADD_DECLARE_WAR_FROM_PEACE);
                            }
                            nextPlayer.changeRelationWithPlayer(enemy, Stance.WAR);
                            monarchActionElement.setAttribute("enemy", enemy.getId());
                            try {
                                conn.send(monarchActionElement);
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                            /** TODO: restore
                                case Monarch.SUPPORT_LAND:
                                int[] additions = monarch.supportLand();
                                createUnits(additions, monarchActionElement, nextPlayer);
                                try {
                                conn.send(monarchActionElement);
                                } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                                }
                                break;
                                case Monarch.SUPPORT_SEA:
                                // TODO: make this generic
                                UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.frigate");
                                newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer, unitType, UnitState.ACTIVE);
                                //nextPlayer.getEurope().add(newUnit);
                                monarchActionElement.appendChild(newUnit.toXMLElement(nextPlayer, monarchActionElement
                                .getOwnerDocument()));
                                try {
                                conn.send(monarchActionElement);
                                } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                                }
                                break;
                            */
                        case OFFER_MERCENARIES:
                            Element mercenaryElement = monarchActionElement.getOwnerDocument().createElement("mercenaries");
                            List<AbstractUnit> units = monarch.getMercenaries();
                            int price = monarch.getPrice(units, true);
                            monarchActionElement.setAttribute("price", String.valueOf(price));
                            for (AbstractUnit unit : units) {
                                mercenaryElement.appendChild(unit.toXMLElement(monarchActionElement.getOwnerDocument()));
                            }
                            monarchActionElement.appendChild(mercenaryElement);
                            try {
                                Element reply = conn.ask(monarchActionElement);
                                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                                if (accepted) {
                                    Element updateElement = Message.createNewRootElement("monarchAction");
                                    updateElement.setAttribute("action", String.valueOf(MonarchAction.ADD_UNITS));
                                    nextPlayer.modifyGold(-price);
                                    createUnits(units, updateElement, nextPlayer);
                                    conn.send(updateElement);
                                }
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                        case NO_ACTION:
                            // nothing to do here. :-)
                            break;
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Monarch action failed!", e);
                    }
                }
            };
        t.start();
    }

    /**
     * Create the Royal Expeditionary Force player corresponding to
     * a given player that is about to rebel.
     *
     * @param player The <code>ServerPlayer</code> about to rebel.
     * @return The REF player.
     */
    public ServerPlayer createREFPlayer(ServerPlayer player) {
        Nation refNation = player.getNation().getRefNation();
        ServerPlayer refPlayer = getFreeColServer().addAIPlayer(refNation);
        refPlayer.setEntryLocation(player.getEntryLocation());
        // This will change later, just for setup
        player.setStance(refPlayer, Stance.PEACE);
        refPlayer.setTension(player, new Tension(Tension.Level.CONTENT.getLimit()));
        player.setTension(refPlayer, new Tension(Tension.Level.CONTENT.getLimit()));
        createREFUnits(player, refPlayer);
        return refPlayer;
    }
    
    public List<Unit> createREFUnits(ServerPlayer player, ServerPlayer refPlayer){
        EquipmentType muskets = Specification.getSpecification().getEquipmentType("model.equipment.muskets");
        EquipmentType horses = Specification.getSpecification().getEquipmentType("model.equipment.horses");
        
        List<Unit> unitsList = new ArrayList<Unit>();
        List<Unit> navalUnits = new ArrayList<Unit>();
        List<Unit> landUnits = new ArrayList<Unit>();
        
        // Create naval units
        for (AbstractUnit unit : player.getMonarch().getNavalUnits()) {
            for (int index = 0; index < unit.getNumber(); index++) {
                Unit newUnit = new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                        unit.getUnitType(), UnitState.TO_AMERICA);
                navalUnits.add(newUnit);
            }
        }
        unitsList.addAll(navalUnits);
        
        // Create land units
        for (AbstractUnit unit : player.getMonarch().getLandUnits()) {
            EquipmentType[] equipment = EquipmentType.NO_EQUIPMENT;
            switch(unit.getRole()) {
            case SOLDIER:
                equipment = new EquipmentType[] { muskets };
                break;
            case DRAGOON:
                equipment = new EquipmentType[] { horses, muskets };
                break;
            default:
            }
            for (int index = 0; index < unit.getNumber(); index++) {
                landUnits.add(new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                        unit.getUnitType(), UnitState.ACTIVE, equipment));
            }
        }
        unitsList.addAll(landUnits);
            
        // Board land units
        Iterator<Unit> carriers = navalUnits.iterator();
        for(Unit unit : landUnits){
            //cycle through the naval units to find a carrier for this unit
            
            // check if there is space for this unit
            boolean noSpaceForUnit=true;
            for(Unit carrier : navalUnits){
                if (unit.getSpaceTaken() <= carrier.getSpaceLeft()) {
                    noSpaceForUnit=false;
                    break;
                }
            }
            // There is no space for this unit, stays in Europe
            if(noSpaceForUnit){
                continue;
            }
            // Find carrier
            Unit carrier = null;
            while (carrier == null){
                // got to the end of the list, restart
                if (!carriers.hasNext()) {
                    carriers = navalUnits.iterator();
                }
                carrier = carriers.next();
                // this carrier cant carry this unit
                if (unit.getSpaceTaken() > carrier.getSpaceLeft()) {
                    carrier = null;
                }
            }
            // set unit aboard carrier
            unit.setLocation(carrier);
            //XXX: why only the units that can be aboard are sent to the player?
            //unitsList.add(unit);
        }
        return unitsList;
    }

    private void createUnits(List<AbstractUnit> units, Element element, ServerPlayer nextPlayer) {
        String musketsTypeStr = null;
        String horsesTypeStr = null;
        if(nextPlayer.isIndian()){
                musketsTypeStr = "model.equipment.indian.muskets";
            horsesTypeStr = "model.equipment.indian.horses";
        } else {
                musketsTypeStr = "model.equipment.muskets";
            horsesTypeStr = "model.equipment.horses";
        }

        final EquipmentType muskets = FreeCol.getSpecification().getEquipmentType(musketsTypeStr);
        final EquipmentType horses = FreeCol.getSpecification().getEquipmentType(horsesTypeStr);

        EquipmentType[] soldier = new EquipmentType[] { muskets };
        EquipmentType[] dragoon = new EquipmentType[] { horses, muskets };
        for (AbstractUnit unit : units) {
            EquipmentType[] equipment = EquipmentType.NO_EQUIPMENT;
            for (int count = 0; count < unit.getNumber(); count++) {
                switch(unit.getRole()) {
                case SOLDIER:
                    equipment = soldier;
                    break;
                case DRAGOON:
                    equipment = dragoon;
                    break;
                default:
                }
                Unit newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer,
                                        unit.getUnitType(), UnitState.ACTIVE, equipment);
                //nextPlayer.getEurope().add(newUnit);
                if (element != null) {
                    element.appendChild(newUnit.toXMLElement(nextPlayer, element.getOwnerDocument()));
                }
            }
        }
    }

    private void bombardEnemyShips(ServerPlayer currentPlayer) {
        logger.finest("Entering method bombardEnemyShips.");
        Map map = getFreeColServer().getGame().getMap();
        CombatModel combatModel = getFreeColServer().getGame().getCombatModel();
        for (Settlement settlement : currentPlayer.getSettlements()) {
            Colony colony = (Colony) settlement;
            
            if (!colony.canBombardEnemyShip()){
            	continue;
            }

            logger.fine("Colony " + colony.getName() + " can bombard enemy ships.");
            Position colonyPosition = colony.getTile().getPosition();
            for (Direction direction : Direction.values()) {
            	Tile tile = map.getTile(Map.getAdjacent(colonyPosition, direction));

            	// ignore land tiles and borders
            	if(tile == null || tile.isLand()){
            		continue;
            	}

            	// Go through the units in the tile
            	// a new list must be created, since the original may be changed while iterating
            	List<Unit> unitList = new ArrayList<Unit>(tile.getUnitList());
            	for(Unit unit : unitList){
                    logger.fine(colony.getName() + " found unit : " + unit.toString());
            		// we need to save the tile of the unit
            		//before the location of the unit can change
            		Tile unitTile = unit.getTile();
            		
            		Player player = unit.getOwner();

            		// ignore own units
            		if(player == currentPlayer){
            			continue;
            		}

            		// ignore friendly units
            		if(currentPlayer.getStance(player) != Stance.WAR &&
            				!unit.hasAbility("model.ability.piracy")){
                            logger.warning(colony.getName() + " found unit to not bombard: "
                                           + unit.toString());
            			continue;
            		}

            		logger.warning(colony.getName() + " found enemy unit to bombard: " +
                                       unit.toString());
            		// generate bombardment result
            		CombatModel.CombatResult result = combatModel.generateAttackResult(colony, unit);

            		// ship was damaged, get repair location
            		Location repairLocation = null;
            		if(result.type == CombatModel.CombatResultType.WIN){
            			repairLocation = player.getRepairLocation(unit);
            		}

            		// update server data
            		getGame().getCombatModel().bombard(colony, unit, result, repairLocation);

            		// Inform the players (other then the player
            		// attacking) about the attack:
            		int plunderGold = -1;
            		Iterator<Player> enemyPlayerIterator = getFreeColServer().getGame().getPlayerIterator();
            		while (enemyPlayerIterator.hasNext()) {
            			ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            			if (enemyPlayer.getConnection() == null) {
            				continue;
            			}

            			// unit tile not visible to player, move to next player
            			if(!enemyPlayer.canSee(unitTile)){
            				continue;
            			}

            			Element opponentAttackElement = Message.createNewRootElement("opponentAttack");                                 
            			opponentAttackElement.setAttribute("direction", direction.toString());
            			opponentAttackElement.setAttribute("result", result.type.toString());
            			opponentAttackElement.setAttribute("plunderGold", Integer.toString(plunderGold));
            			opponentAttackElement.setAttribute("colony", colony.getId());
            			opponentAttackElement.setAttribute("defender", unit.getId());
            			opponentAttackElement.setAttribute("damage", String.valueOf(result.damage));

            			// Add repair location to defending player
            			if(enemyPlayer == player && repairLocation != null){
            				opponentAttackElement.setAttribute("repairIn", repairLocation.getId());
            			}

            			// Every player who witness the confrontation needs to know about the attacker
            			if (!enemyPlayer.canSee(colony.getTile())) {
            				opponentAttackElement.setAttribute("update", "tile");
            				enemyPlayer.setExplored(colony.getTile());
            				opponentAttackElement.appendChild(colony.getTile().toXMLElement(
            						enemyPlayer, opponentAttackElement.getOwnerDocument()));
            			}

            			// Send response
            			try {
            				enemyPlayer.getConnection().send(opponentAttackElement);
            			} catch (IOException e) {
            				logger.warning("Could not send message to: " + enemyPlayer.getName()
            						+ " with connection " + enemyPlayer.getConnection());
            			}
            		}
                }
            }
        }
    }
    
    /**
     * Cash in a treasure train.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is cashing in.
     * @param unit The treasure train <code>Unit</code> to cash in.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element cashInTreasureTrain(ServerPlayer serverPlayer, Unit unit) {
        List<Object> objects = new ArrayList<Object>();

        // Work out the cash in amount.
        int fullAmount = unit.getTreasureAmount();
        int cashInAmount = (fullAmount - unit.getTransportFee())
            * (100 - serverPlayer.getTax()) / 100;
        serverPlayer.modifyGold(cashInAmount);
        objects.add((FreeColGameObject) unit.getLocation());
        addPartial(objects, (Player) serverPlayer, "gold", "score");

        // Generate a suitable message.
        String messageId = (serverPlayer.getPlayerType() == PlayerType.REBEL
            || serverPlayer.getPlayerType() == PlayerType.INDEPENDENT)
            ? "model.unit.cashInTreasureTrain.independent"
            : "model.unit.cashInTreasureTrain.colonial";
        objects.add(new ModelMessage(messageId, serverPlayer, unit)
                    .addAmount("%amount%", fullAmount)
                    .addAmount("%cashInAmount%", cashInAmount));

        // Dispose.
        objects.addAll(unit.disposeList());

        // Do not update others, they can not see cash-ins which
        // happen in colony or in Europe.
        return buildUpdate(serverPlayer, objects);
    }


    public java.util.Map<String,Object> getTransactionSession(Unit unit, Settlement settlement){
        java.util.Map<String, java.util.Map<String,Object>> unitTransactions = null;

        if(transactionSessions.containsKey(unit.getId())){
            unitTransactions = transactionSessions.get(unit.getId());
            if(unitTransactions.containsKey(settlement.getId())){
                return unitTransactions.get(settlement.getId());
            }
        }
        // Session does not exist, create, store, and return it
        java.util.Map<String,Object> session = new HashMap<String,Object>();
        // default values
        session.put("canGift", true);
        session.put("canSell", true);
        session.put("canBuy", true);
        session.put("actionTaken", false);
        session.put("hasSpaceLeft", unit.getSpaceLeft() != 0);
        session.put("unitMoves", unit.getMovesLeft());
        if(settlement.getOwner().getStance(unit.getOwner()) == Stance.WAR){
            session.put("canSell", false);
            session.put("canBuy", false);
        }
        else{
        	// the unit took nothing to sell, so nothing should be in this session
            if(unit.getSpaceTaken() == 0){
                session.put("canSell", false);
            }
        }
        // only keep track of human player sessions
        if(unit.getOwner().isAI()){
            return session;
        }
        
        // Save session for tracking
        
        // unit has no open transactions
        if(unitTransactions == null){
            unitTransactions = new HashMap<String,java.util.Map<String, Object>>();
            transactionSessions.put(unit.getId(), unitTransactions);
        }
        unitTransactions.put(settlement.getId(), session);
        return session;
    }

    public void closeTransactionSession(Unit unit, Settlement settlement){
        java.util.Map<String, java.util.Map<String,Object>> unitTransactions;
        
        // only keep track of human player sessions
        if(unit.getOwner().isAI()){
          return;  
        }
        
        if(!transactionSessions.containsKey(unit.getId())){
            throw new IllegalStateException("Trying to close a non-existing session");
        }
        
        unitTransactions = transactionSessions.get(unit.getId());   
        if(!unitTransactions.containsKey(settlement.getId())){
            throw new IllegalStateException("Trying to close a non-existing session");
        }
        
        unitTransactions.remove(settlement.getId());
        if(unitTransactions.isEmpty()){
            transactionSessions.remove(unit.getId());
        }
    }
    
    public boolean isTransactionSessionOpen(Unit unit, Settlement settlement){
        // AI does not need to send a message to open a session
        if(unit.getOwner().isAI()){
            return true;
        }
        
        if(!transactionSessions.containsKey(unit.getId())){
            return false;
        }
        if(settlement != null &&
           !transactionSessions.get(unit.getId()).containsKey(settlement.getId())){
                return false;
        }
        return true;
    }

    /**
     * A unit migrates from Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose unit it will be.
     * @param slot The slot within <code>Europe</code> to select the unit from.
     * @param fountain True if this occurs as a result of a Fountain of Youth.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element emigrate(ServerPlayer serverPlayer, int slot,
                            boolean fountain) {
        List<Object> objects = new ArrayList<Object>();

        // Valid slots are in [1,3], recruitable indices are in [0,2].
        // An invalid slot is normal when the player has no control over
        // recruit type.
        boolean validSlot = 1 <= slot && slot <= Europe.RECRUIT_COUNT;
        int index = (validSlot) ? slot-1
            : getPseudoRandom().nextInt(Europe.RECRUIT_COUNT);

        // Create the recruit, move it to the docks.
        Europe europe = serverPlayer.getEurope();
        UnitType recruitType = europe.getRecruitable(index);
        Game game = getGame();
        Unit unit = new Unit(game, europe, serverPlayer, recruitType,
                             UnitState.ACTIVE,
                             recruitType.getDefaultEquipment());
        unit.setLocation(europe);

        // Update immigration counters if this was an ordinary
        // decision to migrate.
        if (!fountain) {
            serverPlayer.updateImmigrationRequired();
            serverPlayer.reduceImmigration();
            addPartial(objects, (Player) serverPlayer,
                       "immigration", "immigrationRequired");
        }

        // Replace the recruit we used.
        // This annoying taskId stuff can go away when
        // addFather/generateRecruitable moves server-side.
        String taskId = serverPlayer.getId()
            + ".emigrate." + game.getTurn().toString()
            + ".slot." + Integer.toString(slot)
            + "." + Integer.toString(getPseudoRandom().nextInt(1000000));
        europe.setRecruitable(index, serverPlayer.generateRecruitable(taskId));
        objects.add(europe);

        // Return an informative message only if this was an ordinary migration
        // where we did not select the unit type.
        // Fountain of Youth migrants have already been announced in bulk.
        if (!fountain && !validSlot) {
            objects.add(new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                         "model.europe.emigrate",
                                         serverPlayer, unit)
                        .add("%europe%", europe.getNameKey())
                        .addStringTemplate("%unit%", unit.getLabel()));
        }

        // Do not update others, emigration is private.
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * If a unit moves, check if an opposing naval unit slows it down.
     * Note that the unit moves are reduced here.
     *
     * @param unit The <code>Unit</code> that is moving.
     * @param newTile The <code>Tile</code> the unit is moving to.
     * @return Either an enemy unit that causes a slowdown, or null if none.
     */
    private Unit getSlowedBy(Unit unit, Tile newTile) {
        Player player = unit.getOwner();
        Game game = unit.getGame();
        CombatModel combatModel = game.getCombatModel();
        Unit attacker = null;
        boolean pirate = unit.hasAbility("model.ability.piracy");
        float attackPower = 0;

        if (!unit.isNaval() || unit.getMovesLeft() <= 0) return null;
        for (Tile tile : game.getMap().getSurroundingTiles(newTile, 1)) {
            // Ships in settlements do not slow enemy ships, but:
            // TODO should a fortress slow a ship?
            Player enemy;
            if (tile.isLand()
                || tile.getColony() != null
                || tile.getFirstUnit() == null
                || (enemy = tile.getFirstUnit().getOwner()) == player) continue;
            for (Unit enemyUnit : tile.getUnitList()) {
                if (pirate || enemyUnit.hasAbility("model.ability.piracy")
                    || (enemyUnit.isOffensiveUnit()
                        && player.getStance(enemy) == Stance.WAR)) {
                    attackPower += combatModel.getOffencePower(enemyUnit, unit);
                    if (attacker == null) {
                        attacker = enemyUnit;
                    }
                }
            }
        }
        if (attackPower > 0) {
            float defencePower = combatModel.getDefencePower(attacker, unit);
            float totalProbability = attackPower + defencePower;
            if (getPseudoRandom().nextInt(Math.round(totalProbability) + 1)
                < attackPower) {
                int diff = Math.max(0, Math.round(attackPower - defencePower));
                int moves = Math.min(9, 3 + diff / 3);
                unit.setMovesLeft(unit.getMovesLeft() - moves);
                logger.info(unit.getId()
                            + " slowed by " + attacker.getId()
                            + " by " + Integer.toString(moves) + " moves.");
            } else {
                attacker = null;
            }
        }
        return attacker;
    }

    /**
     * Returns a type of Lost City Rumour. The type of rumour depends on the
     * exploring unit, as well as player settings.
     *
     * @param lostCity The <code>LostCityRumour</code> to investigate.
     * @param unit The <code>Unit</code> exploring the lost city rumour.
     * @param difficulty The difficulty level.
     * @return The type of rumour.
     * TODO: Move all the magic numbers in here to the specification.
     *       Also change the logic so that the special events appear a fixed number
     *       of times throughout the game, according to the specification.
     *       Names for the cities of gold is also on the wishlist.
     */
    private RumourType getLostCityRumourType(LostCityRumour lostCity,
                                             Unit unit, int difficulty) {
        Tile tile = unit.getTile();
        Player player = unit.getOwner();
        RumourType rumour = lostCity.getType();
        if (rumour != null) {
            // Filter out failing cases that could only occur if the
            // type was explicitly set in debug mode.
            switch (rumour) {
            case BURIAL_GROUND:
                if (tile.getOwner() == null || !tile.getOwner().isIndian()) {
                    rumour = RumourType.NOTHING;
                }
                break;
            case LEARN:
                if (unit.getType().getUnitTypesLearntInLostCity().isEmpty()) {
                    rumour = RumourType.NOTHING;
                }
                break;
            default:
                break;
            }
            return rumour;
        }

        // The following arrays contain percentage values for
        // "good" and "bad" events when scouting with a non-expert
        // at the various difficulty levels [0..4] exact values
        // but generally "bad" should increase, "good" decrease
        final int BAD_EVENT_PERCENTAGE[]  = { 11, 17, 23, 30, 37 };
        final int GOOD_EVENT_PERCENTAGE[] = { 75, 62, 48, 33, 17 };
        // remaining to 100, event NOTHING:   14, 21, 29, 37, 46

        // The following arrays contain the modifiers applied when
        // expert scout is at work exact values; modifiers may
        // look slightly "better" on harder levels since we're
        // starting from a "worse" percentage.
        final int BAD_EVENT_MOD[]  = { -6, -7, -7, -8, -9 };
        final int GOOD_EVENT_MOD[] = { 14, 15, 16, 18, 20 };

        // The scouting outcome is based on three factors: level,
        // expert scout or not, DeSoto or not.  Based on this, we
        // are going to calculate probabilites for neutral, bad
        // and good events.
        boolean isExpertScout = unit.hasAbility("model.ability.expertScout")
            && unit.hasAbility("model.ability.scoutIndianSettlement");
        boolean hasDeSoto = player.hasAbility("model.ability.rumoursAlwaysPositive");
        int percentNeutral;
        int percentBad;
        int percentGood;
        if (hasDeSoto) {
            percentBad  = 0;
            percentGood = 100;
            percentNeutral = 0;
        } else {
            // First, get "basic" percentages
            percentBad  = BAD_EVENT_PERCENTAGE[difficulty];
            percentGood = GOOD_EVENT_PERCENTAGE[difficulty];

            // Second, apply ExpertScout bonus if necessary
            if (isExpertScout) {
                percentBad  += BAD_EVENT_MOD[difficulty];
                percentGood += GOOD_EVENT_MOD[difficulty];
            }

            // Third, get a value for the "neutral" percentage,
            // unless the other values exceed 100 already
            if (percentBad + percentGood < 100) {
                percentNeutral = 100 - percentBad - percentGood;
            } else {
                percentNeutral = 0;
            }
        }

        // Now, the individual events; each section should add up to 100
        // The NEUTRAL
        int eventNothing = 100;

        // The BAD
        int eventVanish = 100;
        int eventBurialGround = 0;
        // If the tile not is European-owned, allow burial grounds rumour.
        if (tile.getOwner() != null && tile.getOwner().isIndian()) {
            eventVanish = 75;
            eventBurialGround = 25;
        }

        // The GOOD
        int eventLearn    = 30;
        int eventTrinkets = 30;
        int eventColonist = 20;
        // or, if the unit can't learn
        if (unit.getType().getUnitTypesLearntInLostCity().isEmpty()) {
            eventLearn    =  0;
            eventTrinkets = 50;
            eventColonist = 30;
        }

        // The SPECIAL
        // Right now, these are considered "good" events that happen randomly.
        int eventRuins    = 9;
        int eventCibola   = 6;
        int eventFountain = 5;

        // Finally, apply the Good/Bad/Neutral modifiers from
        // above, so that we end up with a ton of values, some of
        // them zero, the sum of which should be 10000.
        eventNothing      *= percentNeutral;
        eventVanish       *= percentBad;
        eventBurialGround *= percentBad;
        eventLearn        *= percentGood;
        eventTrinkets     *= percentGood;
        eventColonist     *= percentGood;
        eventRuins        *= percentGood;
        eventCibola       *= percentGood;
        eventFountain     *= percentGood;

        // Add all possible events to a RandomChoice List
        List<RandomChoice<RumourType>> choices = new ArrayList<RandomChoice<RumourType>>();
        if (eventNothing > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.NOTHING, eventNothing));
        }
        if (eventVanish > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.EXPEDITION_VANISHES, eventVanish));
        }
        if (eventBurialGround > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.BURIAL_GROUND, eventBurialGround));
        }
        if (eventLearn > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.LEARN, eventLearn));
        }
        if (eventTrinkets > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.TRIBAL_CHIEF, eventTrinkets));
        }
        if (eventColonist > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.COLONIST, eventColonist));
        }
        if (eventRuins > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.RUINS, eventRuins));
        }
        if (eventCibola > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.CIBOLA, eventCibola));
        }
        if (eventFountain > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.FOUNTAIN_OF_YOUTH, eventFountain));
        }
        return RandomChoice.getWeightedRandom(getPseudoRandom(), choices);
    }

    /**
     * Explore a lost city.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @return A list of private objects to update.
     */
    private List<Object> exploreLostCityRumour(ServerPlayer serverPlayer,
                                               Unit unit) {
        List<Object> objects = new ArrayList<Object>();
        Tile tile = unit.getTile();
        LostCityRumour lostCity = tile.getLostCityRumour();
        if (lostCity == null) return objects;

        Specification specification = FreeCol.getSpecification();
        int difficulty = specification.getRangeOption("model.option.difficulty").getValue();
        int dx = 10 - difficulty;
        Game game = unit.getGame();
        UnitType unitType;
        Unit newUnit = null;
        List<UnitType> treasureUnitTypes = null;

        switch (getLostCityRumourType(lostCity, unit, difficulty)) {
        case BURIAL_GROUND:
            Player indianPlayer = tile.getOwner();
            indianPlayer.modifyTension(serverPlayer, Tension.Level.HATEFUL.getLimit());
            objects.add(indianPlayer);
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.BurialGround",
                                        serverPlayer, unit)
                       .addStringTemplate("%nation%", indianPlayer.getNationName()));
            break;
        case EXPEDITION_VANISHES:
            objects.addAll(unit.disposeList());
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.ExpeditionVanishes", serverPlayer));
            break;
        case NOTHING:
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.Nothing", serverPlayer, unit));
            break;
        case LEARN:
            List<UnitType> learntUnitTypes = unit.getType().getUnitTypesLearntInLostCity();
            StringTemplate oldName = unit.getLabel();
            unit.setType(learntUnitTypes.get(getPseudoRandom().nextInt(learntUnitTypes.size())));
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.Learn", serverPlayer, unit)
                       .addStringTemplate("%unit%", oldName)
                       .add("%type%", unit.getType().getNameKey()));
            break;
        case TRIBAL_CHIEF:
            int chiefAmount = getPseudoRandom().nextInt(dx * 10) + dx * 5;
            serverPlayer.modifyGold(chiefAmount);
            addPartial(objects, serverPlayer, "gold", "score");
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.TribalChief", serverPlayer, unit)
                       .addAmount("%money%", chiefAmount));
            break;
        case COLONIST:
            List<UnitType> newUnitTypes = specification.getUnitTypesWithAbility("model.ability.foundInLostCity");
            newUnit = new Unit(game, tile, serverPlayer,
                               newUnitTypes.get(getPseudoRandom().nextInt(newUnitTypes.size())),
                               UnitState.ACTIVE);
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.Colonist", serverPlayer, newUnit));
            break;
        case CIBOLA:
            String cityName = game.getCityOfCibola();
            if (cityName != null) {
                int treasureAmount = getPseudoRandom().nextInt(dx * 600) + dx * 300;
                if (treasureUnitTypes == null) {
                    treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");
                }
                unitType = treasureUnitTypes.get(getPseudoRandom().nextInt(treasureUnitTypes.size()));
                newUnit = new Unit(game, tile, serverPlayer, unitType, UnitState.ACTIVE);
                newUnit.setTreasureAmount(treasureAmount);
                objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                            "lostCityRumour.Cibola", serverPlayer, newUnit)
                           .add("%city%", cityName)
                           .addAmount("%money%", treasureAmount));
                objects.add(new HistoryEvent(game.getTurn().getNumber(), HistoryEvent.EventType.CITY_OF_GOLD)
                           .add("%city%", cityName)
                           .addAmount("%treasure%", treasureAmount));
                break;
            }
            // Fall through, found all the cities of gold.
        case RUINS:
            int ruinsAmount = getPseudoRandom().nextInt(dx * 2) * 300 + 50;
            if (ruinsAmount < 500) { // TODO remove magic number
                serverPlayer.modifyGold(ruinsAmount);
                addPartial(objects, serverPlayer, "gold", "score");
            } else {
                if (treasureUnitTypes == null) {
                    treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");
                }
                unitType = treasureUnitTypes.get(getPseudoRandom().nextInt(treasureUnitTypes.size()));
                newUnit = new Unit(game, tile, serverPlayer, unitType, UnitState.ACTIVE);
                newUnit.setTreasureAmount(ruinsAmount);
            }
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.Ruins",
                                        serverPlayer, ((newUnit != null) ? newUnit : unit))
                       .addAmount("%money%", ruinsAmount));
            break;
        case FOUNTAIN_OF_YOUTH:
            Europe europe = serverPlayer.getEurope();
            if (europe == null) {
                objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                            "lostCityRumour.FountainOfYouthWithoutEurope",
                                            serverPlayer, unit));
            } else {
                if (serverPlayer.hasAbility("model.ability.selectRecruit")
                    && !serverPlayer.isAI()) { // TODO: let the AI select
                    // Remember, and ask player to select
                    serverPlayer.setRemainingEmigrants(dx);
                    addAttribute(objects, "fountainOfYouth", Integer.toString(dx));
                } else {
                    for (int k = 0; k < dx; k++) {
                        new Unit(game, europe, serverPlayer, serverPlayer.generateRecruitable(serverPlayer.getId() + "fountain." + Integer.toString(k)),
                                 UnitState.ACTIVE);
                    }
                    objects.add(europe);
                }
                objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                            "lostCityRumour.FountainOfYouth",
                                            serverPlayer, unit));
            }
            break;
        case NO_SUCH_RUMOUR:
        default:
            throw new IllegalStateException("No such rumour.");
        }
        tile.removeLostCityRumour();
        return objects;
    }

    /**
     * Find uncontacted players with units or settlements on
     * surrounding tiles.
     * Removed the restriction that the unit must not be naval which
     * avoids the special case where a scout, student, missionary, or
     * military unit can arrive by ship at an uncontacted settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is moving.
     * @param tile The <code>Tile</code> to check.
     * @return A list of <code>ServerPlayer</code>s newly contacted.
     */
    private List<ServerPlayer> findUncontacted(ServerPlayer serverPlayer,
                                               Tile tile) {
        List<ServerPlayer> players = new ArrayList<ServerPlayer>();
        for (Tile t : getGame().getMap().getSurroundingTiles(tile, 1)) {
            if (t == null || !t.isLand()) {
                continue; // Invalid tile for contact
            }

            ServerPlayer otherPlayer = null;
            if (t.getSettlement() != null) {
                otherPlayer = (ServerPlayer) t.getSettlement().getOwner();
            } else if (t.getFirstUnit() != null) {
                otherPlayer = (ServerPlayer) t.getFirstUnit().getOwner();
            }

            // Ignore ourself and previously contacted nations.
            if (otherPlayer != null && otherPlayer != serverPlayer
                && !serverPlayer.hasContacted(otherPlayer)) {
                players.add(otherPlayer);
            }
        }
        return players;
    }

    /**
     * Move a unit.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is moving.
     * @param unit The <code>Unit</code> to move.
     * @param newTile The <code>Tile</code> to move to.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element move(ServerPlayer serverPlayer, Unit unit, Tile newTile) {
        List<Object> objects = new ArrayList<Object>();
        Game game = getGame();
        Turn turn = game.getTurn();

        // Plan to update tiles that could not be seen before but will
        // now be within the line-of-sight.
        List<Object> privateObjects = new ArrayList<Object>();
        int los = unit.getLineOfSight();
        for (Tile tile : game.getMap().getSurroundingTiles(newTile, los)) {
            if (!serverPlayer.canSee(tile)) privateObjects.add(tile);
        }

        // Update unit state.
        Location oldLocation = unit.getLocation();
        unit.setState(UnitState.ACTIVE);
        unit.setStateToAllChildren(UnitState.SENTRY);
        if (oldLocation instanceof Unit) {
            unit.setMovesLeft(0); // Disembark always consumes all moves.
        } else {
            unit.setMovesLeft(unit.getMovesLeft() - unit.getMoveCost(newTile));
        }
        unit.setLocation(newTile);
        unit.activeAdjacentSentryUnits(newTile);
        // Clear the alreadyOnHighSea flag if we move onto a non-highsea tile.
        unit.setAlreadyOnHighSea(newTile.canMoveToEurope());

        // Always update old location and new tile.
        objects.add((FreeColGameObject) oldLocation);
        objects.add(newTile);

        // Explore a rumour if present, but do not ignore the rumour
        // objects as the only way the unit can die is the VANISHES rumour.
        if (newTile.hasLostCityRumour() && serverPlayer.isEuropean()) {
            List<Object> rumourObjects
                = exploreLostCityRumour(serverPlayer, unit);
            if (unit.isDisposed()) {
                privateObjects.clear(); // No discovery if died.
            }
            privateObjects.addAll(rumourObjects);
        }

        // Add the animation, even if the unit dies.
        addAnimate(objects, unit, oldLocation, newTile);

        // Check for new contacts.
        if (!unit.isDisposed() && newTile.isLand()) {
            ServerPlayer welcomer = null;
            for (ServerPlayer other : findUncontacted(serverPlayer, newTile)) {
                // Special meeting on first landing.
                if (serverPlayer.isEuropean() && other.isIndian()
                    && !serverPlayer.isNewLandNamed()
                    && (welcomer == null || newTile.getOwner() == other)) {
                    welcomer = other;
                }
                serverPlayer.setContacted(other);
                other.setContacted(serverPlayer);
                addStance(objects, Stance.PEACE, serverPlayer, other);
                if (serverPlayer.isEuropean()) {
                    HistoryEvent h = new HistoryEvent(turn.getNumber(),
                            HistoryEvent.EventType.MEET_NATION)
                        .addStringTemplate("%nation%", other.getNationName());
                    serverPlayer.addHistory(h);
                    privateObjects.add(h);
                }
            }
            if (welcomer != null) {
                addAttribute(privateObjects, "welcome", welcomer.getId());
                addAttribute(privateObjects, "camps",
                    Integer.toString(welcomer.getNumberOfSettlements()));
            }
        }

        // Everything afterwards is private to the moving player... almost.
        objects.add(UpdateType.PRIVATE);
        objects.addAll(privateObjects);

        if (!unit.isDisposed() && serverPlayer.isEuropean()) {
            // Also check for arriving next to an IndianSettlement
            // without alarm set, in which case it should be initialized.
            for (Tile t : game.getMap().getSurroundingTiles(newTile, 1)) {
                Settlement settlement = t.getSettlement();
                if (settlement != null
                    && settlement instanceof IndianSettlement) {
                    IndianSettlement indians = (IndianSettlement) settlement;
                    if (indians.getAlarm(serverPlayer) == null) {
                        Player indianPlayer = indians.getOwner();
                        indians.setAlarm(serverPlayer,
                                         indianPlayer.getTension(serverPlayer));
                        objects.add(indians);
                    }
                }
            }

            // Check for slowing units.
            Unit slowedBy = getSlowedBy(unit, newTile);
            if (slowedBy != null) {
                addAttribute(objects, "slowedBy", slowedBy.getId());
            }

            // Check for first landing
            if (newTile.isLand() && !serverPlayer.isNewLandNamed()) {
                String newLandName = Messages.getNewLandName(serverPlayer);
                if (serverPlayer.isAI()) {
                    // TODO: Not convinced shortcutting the AI like
                    // this is a good idea--- even though this is
                    // trivial functionality, it really should be in
                    // the AI code.
                    serverPlayer.setNewLandName(newLandName);
                } else { // Ask player to name the land.
                    addAttribute(objects, "nameNewLand", newLandName);
                }
            }

            // Check for region discovery
            Region region = newTile.getDiscoverableRegion();
            if (region != null) {
                HistoryEvent h = null;
                if (region.isPacific()) {
                    addAttribute(objects, "discoverPacific", "true");
                    h = region.discover(serverPlayer, turn,
                                        "model.region.pacific");
                    objects.add(0, region); // Public discovery!
                } else {
                    String regionName
                        = Messages.getDefaultRegionName(serverPlayer,
                                                        region.getType());
                    if (serverPlayer.isAI()) {
                        // TODO: here is another dubious AI shortcut.
                        region.discover(serverPlayer, turn, regionName);
                        objects.add(0, region); // Public discovery!
                    } else { // Ask player to name the region.
                        addAttribute(objects, "discoverRegion", regionName);
                        addAttribute(objects, "regionType",
                                     Messages.message(region.getLabel()));
                    }
                }
                if (h != null) {
                    serverPlayer.addHistory(h);
                    objects.add(h);
                }
            }
        }

        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Set land name.
     *
     * @param serverPlayer The <code>ServerPlayer</code> who landed.
     * @param name The new land name.
     * @param welcomer An optional <code>ServerPlayer</code> that has offered
     *            a treaty.
     * @param accept True if the treaty has been accepted.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setNewLandName(ServerPlayer serverPlayer, String name,
                                  ServerPlayer welcomer, boolean accept) {
        List<Object> objects = new ArrayList<Object>();
        serverPlayer.setNewLandName(name);

        // Special case of a welcome from an adjacent native unit,
        // offering the land the landing unit is on if a peace treaty
        // is accepted.  Slight awkwardness that we have to find the
        // unit that landed, which relies on this code being triggered
        // from the first landing and thus there is only one land unit
        // in the new world (which is not the case in a debug game).
        if (welcomer != null) {
            if (accept) { // Claim land
                for (Unit u : serverPlayer.getUnits()) {
                    if (u.isNaval()) continue;
                    Tile tile = u.getTile();
                    if (tile == null) continue;
                    if (tile.isLand() && tile.getOwner() == welcomer) {
                        tile.setOwner(serverPlayer);
                        objects.add(tile);
                        break;
                    }
                }
                welcomer = null;
            } else {
                // Consider not accepting the treaty to be a minor
                // insult.  WWC1D?
                // TODO: rework modifyTension to notify of stance change
                // which should be in the update.  Can not happen here,
                // for now, but needs fixing.
                welcomer.modifyTension(serverPlayer,
                                       Tension.TENSION_ADD_MINOR);
            }
        }

        // Only the tile change is not private.
        sendToOthers(serverPlayer, objects);

        // Update the name and note the history.
        addPartial(objects, serverPlayer, "newLandName");
        int turn = serverPlayer.getGame().getTurn().getNumber();
        HistoryEvent h = new HistoryEvent(turn,
                    HistoryEvent.EventType.DISCOVER_NEW_WORLD)
            .addName("%name%", name);
        objects.add(h);
        serverPlayer.addHistory(h);

        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Set region name.
     *
     * @param serverPlayer The <code>ServerPlayer</code> discovering.
     * @param unit The <code>Unit</code> discovering the region.
     * @param region The <code>Region</code> to discover.
     * @param name The new region name.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setNewRegionName(ServerPlayer serverPlayer, Unit unit,
                                    Region region, String name) {
        Game game = serverPlayer.getGame();
        List<Object> objects = new ArrayList<Object>();
        // Tell others about the region.
        objects.add(region);

        // History is private.
        objects.add(UpdateType.PRIVATE);
        HistoryEvent h = region.discover(serverPlayer, game.getTurn(), name);
        serverPlayer.addHistory(h);
        objects.add(h);

        // Others do find out about region name changes.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Embark a unit onto a carrier.
     * Checking that the locations are appropriate is not done here.
     *
     * @param serverPlayer The <code>ServerPlayer</code> embarking.
     * @param unit The <code>Unit</code> that is embarking.
     * @param carrier The <code>Unit</code> to embark onto.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element embarkUnit(ServerPlayer serverPlayer, Unit unit,
                              Unit carrier) {
        if (unit.isNaval()) {
            return Message.clientError("Naval unit " + unit.getId()
                                       + " can not embark.");
        }
        if (carrier.getSpaceLeft() < unit.getSpaceTaken()) {
            return Message.clientError("No space available for unit "
                                       + unit.getId() + " to embark.");
        }

        List<Object> objects = new ArrayList<Object>();
        Location oldLocation = unit.getLocation();
        boolean visible = oldLocation instanceof Tile
            && ((Tile) oldLocation).getSettlement() == null
            && carrier.getLocation() != oldLocation;
        unit.setLocation(carrier);
        unit.setMovesLeft(0);
        unit.setState(UnitState.SENTRY);
        objects.add(oldLocation);
        if (carrier.getLocation() != oldLocation) {
            objects.add(carrier);
            addAnimate(objects, unit, oldLocation, carrier.getTile());
        }

        // Others can see the carrier capacity, and might see the
        // embarking unit board, but will certainly see it disappear.
        List<Object> otherObjects = new ArrayList<Object>();
        otherObjects.add(oldLocation);
        if (visible) {
            objects.add(carrier);
            addAnimate(objects, unit, oldLocation, carrier.getTile());
        } else {
            addRemove(otherObjects, unit);
        }
        sendToOthers(serverPlayer, otherObjects);
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Disembark unit from a carrier.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose unit is
     *                     embarking.
     * @param unit The <code>Unit</code> that is disembarking.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element disembarkUnit(ServerPlayer serverPlayer, Unit unit) {
        if (unit.isNaval()) {
            return Message.clientError("Naval unit " + unit.getId()
                                       + " can not disembark.");
        }
        if (unit.getLocation() instanceof Unit) {
            return Message.clientError("Unit " + unit.getId()
                                       + " is not embarked.");
        }

        Unit carrier = (Unit) unit.getLocation();
        Location newLocation = carrier.getLocation();
        unit.setLocation(newLocation);
        unit.setMovesLeft(0); // In Col1 disembark consumes whole move.
        unit.setState(UnitState.ACTIVE);

        // Others can (potentially) see the location.
        sendToOthers(serverPlayer, newLocation);
        return buildUpdate(serverPlayer, newLocation);
    }


    /**
     * Ask about learning a skill at an IndianSettlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is learning.
     * @param unit The <code>Unit</code> that is learning.
     * @param settlement The <code>Settlement</code> to learn from.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element askLearnSkill(ServerPlayer serverPlayer, Unit unit,
                                 IndianSettlement settlement) {
        List<Object> objects = new ArrayList<Object>();
        Tile tile = settlement.getTile();
        PlayerExploredTile pet = tile.getPlayerExploredTile(serverPlayer);
        pet.setVisited();
        pet.setSkill(settlement.getLearnableSkill());
        objects.add(tile);
        unit.setMovesLeft(0);
        addPartial(objects, unit, "movesLeft");

        // Do not update others, nothing to see yet.
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Learn a skill at an IndianSettlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is learning.
     * @param unit The <code>Unit</code> that is learning.
     * @param settlement The <code>Settlement</code> to learn from.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element learnFromIndianSettlement(ServerPlayer serverPlayer,
                                             Unit unit,
                                             IndianSettlement settlement) {
        // Sanity checks.
        UnitType skill = settlement.getLearnableSkill();
        if (skill == null) {
            return Message.clientError("No skill to learn at "
                                       + settlement.getName());
        }
        if (!unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
            return Message.clientError("Unit " + unit.toString()
                                       + " can not learn skill " + skill
                                       + " at " + settlement.getName());
        }

        // Try to learn
        List<Object> objects = new ArrayList<Object>();
        unit.setMovesLeft(0);
        FreeColGameObject fcgo = (FreeColGameObject) unit.getLocation();
        Tension tension = settlement.getAlarm(serverPlayer);
        switch (tension.getLevel()) {
        case HATEFUL: // Killed
            objects.addAll(unit.disposeList());
            objects.add(fcgo);
            break;
        case ANGRY: // Learn nothing, not even a pet update
            objects.add(UpdateType.PRIVATE);
            addPartial(objects, unit, "movesLeft");
            break;
        default:
            // Teach the unit, and expend the skill if necessary.
            // Do a full information update as the unit is in the settlement.
            unit.setType(skill);
            if (!settlement.isCapital()) {
                settlement.setLearnableSkill(null);
            }
            Tile tile = settlement.getTile();
            tile.updateIndianSettlementInformation(serverPlayer);
            objects.add(unit);
            objects.add(UpdateType.PRIVATE);
            objects.add(tile);
            break;
        }

        // Update others as the unit type change should be visible.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Demand a tribute from a native settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> demanding the tribute.
     * @param unit The <code>Unit</code> that is demanding the tribute.
     * @param settlement The <code>IndianSettlement</code> demanded of.
     * @return An <code>Element</code> encapsulating this action.
     * TODO: Move TURNS_PER_TRIBUTE magic number to the spec.
     */
    public Element demandTribute(ServerPlayer serverPlayer, Unit unit,
                                 IndianSettlement settlement) {
        List<Object> objects = new ArrayList<Object>();

        final int TURNS_PER_TRIBUTE = 5;
        Player indianPlayer = settlement.getOwner();
        int gold = 0;
        int year = getGame().getTurn().getNumber();
        if (settlement.getLastTribute() + TURNS_PER_TRIBUTE < year
            && indianPlayer.getGold() > 0) {
            switch (indianPlayer.getTension(serverPlayer).getLevel()) {
            case HAPPY:
            case CONTENT:
                gold = Math.min(indianPlayer.getGold() / 10, 100);
                break;
            case DISPLEASED:
                gold = Math.min(indianPlayer.getGold() / 20, 100);
                break;
            case ANGRY:
            case HATEFUL:
            default:
                break; // do nothing
            }
        }

        // Increase tension whether we paid or not.  Apply tension
        // directly to the settlement and let propagation work.
        settlement.modifyAlarm(serverPlayer, Tension.TENSION_ADD_NORMAL);
        settlement.setLastTribute(year);
        ModelMessage m;
        if (gold > 0) {
            indianPlayer.modifyGold(-gold);
            serverPlayer.modifyGold(gold);
            addPartial(objects, serverPlayer, "gold", "score");
            m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeAgree",
                                 unit, settlement)
                .addAmount("%amount%", gold);
        } else {
            m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeDisagree",
                                 unit, settlement);
        }
        objects.add(m);
        unit.setMovesLeft(0);
        addPartial(objects, unit, "movesLeft");

        // Do not update others, this is all private.
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Scout a native settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is scouting.
     * @param unit The scout <code>Unit</code>.
     * @param settlement The <code>IndianSettlement</code> to scout.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element scoutIndianSettlement(ServerPlayer serverPlayer,
                                         Unit unit,
                                         IndianSettlement settlement) {
        List<Object> objects = new ArrayList<Object>();
        String result;

        // Hateful natives kill the scout right away.
        Player player = unit.getOwner();
        Tension tension = settlement.getAlarm(player);
        if (tension != null && tension.getLevel() == Tension.Level.HATEFUL) {
            objects.addAll(unit.disposeList());
            result = "die";
        } else {
            // Otherwise player gets to visit, and learn about the settlement.
            int gold = 0;
            Tile tile = settlement.getTile();
            settlement.setVisited(player);
            tile.updateIndianSettlementInformation(player);
            unit.setMovesLeft(0);

            int radius = unit.getLineOfSight();
            UnitType skill = settlement.getLearnableSkill();
            if (settlement.hasBeenVisited()) {
                // Pre-visited settlements are a noop.
                result = "nothing";
            } else if (skill != null
                       && skill.hasAbility("model.ability.expertScout")
                       && unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
                // If the scout can be taught to be an expert it will be.
                // TODO: in the old code the settlement retains the
                // teaching ability.  Is this Col1 compliant?
                unit.setType(settlement.getLearnableSkill());
                // settlement.setLearnableSkill(null);
                objects.add(unit);
                result = "expert";
            } else if (getPseudoRandom().nextInt(3) == 0) {
                // Otherwise 1/3 of cases are tales...
                radius = Math.max(radius, IndianSettlement.TALES_RADIUS);
                result = "tales";
            } else {
                // ...and the rest are beads.
                gold = (getPseudoRandom().nextInt(400)
                            * settlement.getBonusMultiplier()) + 50;
                if (unit.hasAbility("model.ability.expertScout")) {
                    gold = (gold * 11) / 10;
                }
                serverPlayer.modifyGold(gold);
                settlement.getOwner().modifyGold(-gold);
                result = "beads";
            }

            // Private information from here.
            objects.add(UpdateType.PRIVATE);
            if (gold > 0) {
                addPartial(objects, serverPlayer, "gold", "score");
            }

            // Update settlement tile with new information, and any
            // newly visible tiles, possibly with enhanced radius.
            objects.add(tile);
            Map map = getFreeColServer().getGame().getMap();
            for (Tile t : map.getSurroundingTiles(tile, radius)) {
                if (!serverPlayer.canSee(t) && (t.isLand() || t.isCoast())) {
                    player.setExplored(t);
                    objects.add(t);
                }
            }

            // If the unit did not get promoted, update it for moves.
            if (!objects.contains(unit)) {
                addPartial(objects, unit, "movesLeft");
            }
        }
        // Always add result.
        addAttribute(objects, "result", result);

        // Other players may be able to see unit disappearing, or
        // learning.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Denounce an existing mission.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is denouncing.
     * @param unit The <code>Unit</code> denouncing.
     * @param settlement The <code>IndianSettlement</code> containing the
     *                   mission to denounce.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element denounceMission(ServerPlayer serverPlayer, Unit unit,
                                   IndianSettlement settlement) {
        // Determine result
        Location oldLocation = unit.getLocation();
        Unit missionary = settlement.getMissionary();
        ServerPlayer enemy = (ServerPlayer) missionary.getOwner();
        double random = Math.random();
        random *= enemy.getImmigration() / (serverPlayer.getImmigration() + 1);
        if (missionary.hasAbility("model.ability.expertMissionary")) {
            random += 0.2;
        }
        if (unit.hasAbility("model.ability.expertMissionary")) {
            random -= 0.2;
        }

        if (random < 0.5) { // Success, remove old mission and establish ours
            settlement.setMissionary(null);

            // Inform the enemy of loss of mission
            if (enemy.isConnected()) {
                List<Object> objects = new ArrayList<Object>();
                objects.addAll(missionary.disposeList());
                ModelMessage m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                             "indianSettlement.mission.denounced",
                                             settlement)
                            .addStringTemplate("%settlement%", settlement.getLocationName());
                objects.add(m);
                objects.add(settlement);
                sendElement(enemy, objects);
            }

            return establishMission(serverPlayer, unit, settlement);
        }

        // Denounce failed
        List<Object> objects = new ArrayList<Object>();
        objects.addAll(unit.disposeList());
        objects.add((FreeColGameObject) oldLocation);
        objects.add(UpdateType.PRIVATE);
        objects.add(new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     "indianSettlement.mission.noDenounce",
                                     serverPlayer, unit)
                    .addStringTemplate("%nation%", settlement.getOwner().getNationName()));

        // Others can see missionary disappear
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Establish a new mission.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is establishing.
     * @param unit The missionary <code>Unit</code>.
     * @param settlement The <code>IndianSettlement</code> to establish at.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element establishMission(ServerPlayer serverPlayer, Unit unit,
                                    IndianSettlement settlement) {
        List<Object> objects = new ArrayList<Object>();
        // Always update old location.
        objects.add((FreeColGameObject) unit.getLocation());

        // Result depends on tension wrt this settlement.
        // Establish if at least not angry.
        Tension tension = settlement.getAlarm(serverPlayer);
        if (tension == null) {
            tension = new Tension(0);
            settlement.setAlarm(serverPlayer, tension);
        }
        switch (tension.getLevel()) {
        case HATEFUL: case ANGRY:
            objects.addAll(unit.disposeList());
            break;
        case HAPPY: case CONTENT: case DISPLEASED:
            settlement.setMissionary(unit);
            objects.add(settlement);
        }
        objects.add(UpdateType.PRIVATE);
        String messageId = "indianSettlement.mission."
            + settlement.getAlarm(serverPlayer).toString().toLowerCase();
        objects.add(new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     messageId, serverPlayer, unit)
                    .addStringTemplate("%nation%", settlement.getOwner().getNationName()));

        // Others can see missionary disappear and settlement acquire
        // mission.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Incite a settlement against an enemy.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is inciting.
     * @param unit The missionary <code>Unit</code> inciting.
     * @param settlement The <code>IndianSettlement</code> to incite.
     * @param enemy The <code>Player</code> to be incited against.
     * @param gold The amount of gold in the bribe.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element incite(ServerPlayer serverPlayer, Unit unit,
                          IndianSettlement settlement, Player enemy, int gold) {
        List<Object> objects = new ArrayList<Object>();

        // How much gold will be needed?
        Player nativePlayer = settlement.getOwner();
        Tension payingTension = nativePlayer.getTension(serverPlayer);
        Tension targetTension = nativePlayer.getTension(enemy);
        int payingValue = (payingTension == null) ? 0 : payingTension.getValue();
        int targetValue = (targetTension == null) ? 0 : targetTension.getValue();
        int goldToPay = (payingTension != null && targetTension != null
                      && payingValue > targetValue) ? 10000 : 5000;
        goldToPay += 20 * (payingValue - targetValue);
        goldToPay = Math.max(goldToPay, 650);

        // Try to incite?
        unit.setMovesLeft(0);
        addPartial(objects, unit, "movesLeft");
        if (gold < 0) { // Initial enquiry.
            addAttribute(objects, "gold", Integer.toString(goldToPay));
        } else if (gold < goldToPay || serverPlayer.getGold() < gold) {
            objects.add(new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                         "indianSettlement.inciteGoldFail",
                                         serverPlayer, settlement)
                        .addStringTemplate("%player%", enemy.getNationName())
                        .addAmount("%amount%", goldToPay));
            addAttribute(objects, "gold", "0");
        } else {
            // Success.  Set the indian player at war with the european
            // player (and vice versa) and raise tension.
            serverPlayer.modifyGold(-gold);
            nativePlayer.modifyGold(gold);
            addAttribute(objects, "gold", Integer.toString(gold));
            addPartial(objects, serverPlayer, "gold");
            nativePlayer.changeRelationWithPlayer(enemy, Stance.WAR);
            settlement.modifyAlarm(enemy, 1000); // Let propagation work.
            enemy.modifyTension(nativePlayer, 500);
            enemy.modifyTension(serverPlayer, 250);
        }

        // Do not update others, they can not see what happened.
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Set current stop of a unit to the next valid stop if any.
     *
     * @param serverPlayer The <code>ServerPlayer</code> the unit belongs to.
     * @param unit The <code>Unit</code> to update.
     */
    public void updateCurrentStop(ServerPlayer serverPlayer, Unit unit) {
        // Check if there is a valid current stop?
        int current = unit.validateCurrentStop();
        if (current < 0) return;

        // Step to next valid stop.
        ArrayList<Stop> stops = unit.getTradeRoute().getStops();
        int next = current;
        for (;;) {
            if (++next >= stops.size()) next = 0;
            if (next == current) break;
            if (hasWorkAtStop(unit, stops.get(next))) break;
        }

        // Next is the updated stop.
        unit.setCurrentStop(next);
        unit.setDestination(stops.get(next).getLocation());
    }

    /**
     * Is there work for a unit to do at a stop?
     *
     * @param unit The <code>Unit</code> to check.
     * @param stop The <code>Stop</code> to test.
     * @return True if the unit should load or unload cargo at the stop.
     */
    private boolean hasWorkAtStop(Unit unit, Stop stop) {
        ArrayList<GoodsType> stopGoods = stop.getCargo();
        int cargoSize = stopGoods.size();
        for (Goods goods : unit.getGoodsList()) {
            GoodsType type = goods.getType();
            if (stopGoods.contains(type)) {
                if (unit.getLoadableAmount(type) > 0) {
                    // There is space on the unit to load some more
                    // of this goods type, so return true if there is
                    // some available at the stop.
                    Location loc = stop.getLocation();
                    if (loc instanceof Colony) {
                        if (((Colony) loc).getExportAmount(type) > 0) {
                            return true;
                        }
                    } else if (loc instanceof Europe) {
                        return true;
                    }
                } else {
                    cargoSize--; // No room for more of this type.
                }
            } else {
                return true; // This type should be unloaded here.
            }
        }

        // Return true if there is space left, and something to load.
        return unit.getSpaceLeft() > 0 && cargoSize > 0;
    }

    /**
     * Move goods from current location to another.
     *
     * @param goods The <code>Goods</code> to move.
     * @param loc The new <code>Location</code>.
     */
    public void moveGoods(Goods goods, Location loc)
        throws IllegalStateException {
        Location oldLoc = goods.getLocation();
        if (oldLoc == null) {
            throw new IllegalStateException("Goods in null location.");
        } else if (loc == null) {
            ;
        } else if (loc instanceof Unit) {
            if (((Unit) loc).isInEurope()) {
                if (!(oldLoc instanceof Unit && ((Unit) oldLoc).isInEurope())) {
                    throw new IllegalStateException("Goods and carrier not both in Europe.");
                }
            } else if (loc.getTile() == null) {
                throw new IllegalStateException("Carrier not on the map.");
            } else if (oldLoc instanceof IndianSettlement) {
                // Can not be co-located when buying from natives.
            } else if (loc.getTile() != oldLoc.getTile()) {
                throw new IllegalStateException("Goods and carrier not co-located.");
            }
        } else if (loc instanceof IndianSettlement) {
            // Can not be co-located when selling to natives.
        } else if (loc instanceof Colony) {
            if (oldLoc instanceof Unit
                && ((Unit) oldLoc).getOwner() != ((Colony) loc).getOwner()) {
                // Gift delivery
            } else if (loc.getTile() != oldLoc.getTile()) {
                throw new IllegalStateException("Goods and carrier not both in Colony.");
            }
        } else if (loc.getGoodsContainer() == null) {
            throw new IllegalStateException("New location with null GoodsContainer.");
        }

        oldLoc.remove(goods);
        goods.setLocation(null);

        if (loc != null) {
            loc.add(goods);
            goods.setLocation(loc);
        }
    }

    /**
     * Propagate an European market change to the other European markets.
     *
     * @param type The type of goods that was traded.
     * @param amount The amount of goods that was traded.
     * @param serverPlayer The player that performed the trade.
     */
    public void propagateToEuropeanMarkets(GoodsType type, int amount,
                                           ServerPlayer serverPlayer) {
        // Propagate 5-30% of the original change.
        final int lowerBound = 5; // TODO: make into game option?
        final int upperBound = 30;// TODO: make into game option?
        amount *= getPseudoRandom().nextInt(upperBound - lowerBound + 1)
            + lowerBound;
        amount /= 100;
        if (amount == 0) return;

        // Do not need to update the clients here, these changes happen
        // while it is not their turn, and they will get a fresh copy
        // of the altered market in the update sent in nextPlayer above.
        Market market;
        for (ServerPlayer other : getOtherPlayers(serverPlayer)) {
            if (other.isEuropean() && (market = other.getMarket()) != null) {
                market.addGoodsToMarket(type, amount);
            }
        }
    }

    /**
     * Clear the specialty of a unit.
     *
     * @param unit The <code>Unit</code> to clear the speciality of.
     * @param serverPlayer The owner of the unit.
     */
    public void clearSpeciality(Unit unit, ServerPlayer serverPlayer) {
        UnitType newType = unit.getType().getUnitTypeChange(ChangeType.CLEAR_SKILL,
                                                            serverPlayer);
        if (newType == null) {
            throw new IllegalStateException("Can not clear this unit speciality: " + unit.getId());
        }
        // There can be some restrictions that may prevent the
        // clearing of the speciality.  For example, teachers cannot
        // not be cleared of their speciality.
        Location oldLocation = unit.getLocation();
        if (oldLocation instanceof Building
            && !((Building) oldLocation).canAdd(newType)) {
            throw new IllegalStateException("Cannot clear speciality, building does not allow new unit type");
        }

        unit.setType(newType);
        if (oldLocation instanceof Tile) {
            sendToOthers(serverPlayer, (Tile) oldLocation);
        }
    }

    /**
     * Abandon a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is abandoning.
     * @param settlement The <code>Settlement</code> to abandon.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element abandonSettlement(ServerPlayer serverPlayer,
                                     Settlement settlement) {
        List<Object> objects = new ArrayList<Object>();
        // Collect the tiles the settlement owns before disposing.
        objects.addAll(settlement.getOwnedTiles());

        HistoryEvent h = null;
        // Create history event before disposing.
        if (settlement instanceof Colony) {
            h = new HistoryEvent(getGame().getTurn().getNumber(),
                                 HistoryEvent.EventType.ABANDON_COLONY)
                .addName("%colony%", settlement.getName());
        }

        // Now do the dispose.
        objects.addAll(settlement.disposeList());

        if (h != null) { // History is private.
            objects.add(UpdateType.PRIVATE);
            objects.add(h);
        }

        // TODO: Player.settlements is still being fixed on the client side.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }

}
