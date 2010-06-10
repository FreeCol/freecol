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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.PlayerExploredTile;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The main server controller.
 */
public final class InGameController extends Controller {

    private static Logger logger = Logger.getLogger(InGameController.class.getName());

    public static final int SCORE_INDEPENDENCE_DECLARED = 100;

    private final PseudoRandom random;

    public int debugOnlyAITurns = 0;

    private java.util.Map<String,java.util.Map<String, java.util.Map<String,Object>>> transactionSessions;
    
    public static enum MigrationType {
        NORMAL,     // Unit decided to migrate
        RECRUIT,    // Player is paying
        FOUNTAIN    // As a result of a Fountain of Youth discovery
    }


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameController(FreeColServer freeColServer) {
        super(freeColServer);

        random = freeColServer.getPrivatePseudoRandom();
        transactionSessions = new HashMap<String,java.util.Map<String, java.util.Map<String,Object>>>();
    }


    /**
     * Get a list of all server players, optionally excluding supplied ones.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to exclude.
     * @return A list of all connected server players, with exclusions.
     */
    public List<ServerPlayer> getOtherPlayers(ServerPlayer... serverPlayers) {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        outer: for (Player otherPlayer : getGame().getPlayers()) {
            ServerPlayer enemyPlayer = (ServerPlayer) otherPlayer;
            if (!enemyPlayer.isConnected()) continue;
            for (ServerPlayer exclude : serverPlayers) {
                if (enemyPlayer == exclude) continue outer;
            }
            result.add(enemyPlayer);
        }
        return result;
    }

    // Magic cookies to specify an overriding of the standard object
    // handling in arguments to buildUpdate().
    private static enum UpdateType {
        ATTACK,     // Animate an attack
        ATTRIBUTE,  // Set an attribute on the final result
        MOVE,       // Animate a move
        PARTIAL,    // Do a partial update
        PRIVATE,    // Marker for private objects
        REMOVE,     // Remove an object
        STANCE,     // Set stance
        UPDATE      // The default mode
    };

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
     * Helper function to add objects for a move animation to a list.
     *
     * @param objects The list of objects to add to.
     * @param unit The <code>Unit</code> that is moving.
     * @param oldLocation The old <code>Location</code>.
     * @param newTile The new <code>Tile</code>.
     */
    private static void addMove(List<Object> objects, Unit unit,
                                Location oldLocation, Tile newTile) {
        addMore(objects, UpdateType.MOVE, unit, oldLocation, newTile);
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
     */
    private static void addRemove(List<Object> objects,
                                 FreeColGameObject fcgo) {
        addMore(objects, UpdateType.REMOVE, fcgo);
    }

    /**
     * Helper function to add objects for a stance change to a list.
     *
     * @param objects The list of objects to add to.
     * @param stance The <code>Stance</code> to change to.
     * @param player1 The first <code>Player</code>.
     * @param player2 The second <code>Player</code>.
     */
    private static void addStance(List<Object> objects, Stance stance,
                                  Player player1, Player player2) {
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
        Element multiple = doc.createElement("multiple");
        Element update = doc.createElement("update");
        Element messages = null;
        Element history = null;
        Element remove = doc.createElement("remove");
        boolean allVisible = false;

        for (int i = 0; i < objects.size(); i++) {
            Object o = objects.get(i);
            if (o == null) {
                continue;
            } else if (o instanceof UpdateType) {
                switch ((UpdateType) o) {
                case ATTACK: // expect Unit Unit CombatResult
                    if (i+3 < objects.size()
                        && objects.get(i+1) instanceof Unit
                        && objects.get(i+2) instanceof Unit
                        && objects.get(i+3) instanceof CombatResult) {
                        Unit unit = (Unit) objects.get(i+1);
                        Unit defender = (Unit) objects.get(i+2);
                        CombatResult result = (CombatResult) objects.get(i+3);
                        Element animate = buildAttack(serverPlayer, doc, unit,
                                                      defender, result);
                        if (animate != null) {
                            multiple.appendChild(animate);
                        }
                        i += 3;
                    } else {
                        throw new IllegalArgumentException("bogus ATTACK");
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
                case MOVE: // expect Unit Location Tile
                    if (i+3 < objects.size()
                        && objects.get(i+1) instanceof Unit
                        && objects.get(i+2) instanceof FreeColGameObject
                        && objects.get(i+3) instanceof Tile) {
                        Unit unit = (Unit) objects.get(i+1);
                        FreeColGameObject oldLocation = (FreeColGameObject) objects.get(i+2);
                        Tile newTile = (Tile) objects.get(i+3);
                        Element animate = buildMove(serverPlayer, doc, unit,
                                                    oldLocation, newTile);
                        if (animate != null) {
                            multiple.appendChild(animate);
                            // Clean up units that disappear.
                            if (!unit.isVisibleTo(serverPlayer)) {
                                unit.addToRemoveElement(remove);
                            }
                        }
                        i += 3;
                    } else {
                        throw new IllegalArgumentException("bogus MOVE");
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
                case PRIVATE: // ignore visibility test for what follows
                    allVisible = true;
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
                        && objects.get(i+2) instanceof Player
                        && objects.get(i+3) instanceof Player) {
                        Element setStance = buildStance(serverPlayer, doc,
                                (Stance) objects.get(i+1),
                                (Player) objects.get(i+2),
                                (Player) objects.get(i+3));
                        if (setStance != null) {
                            multiple.appendChild(setStance);
                        }
                        i += 3;
                    } else {
                        throw new IllegalArgumentException("bogus STANCE");
                    }
                    break;
                case UPDATE: // Syntactic sugar
                    break;
                }
            } else if (o instanceof Element) {
                multiple.appendChild((Element) doc.importNode((Element) o,
                                                              true));
            } else if (o instanceof ModelMessage) {
                // Always send message objects
                if (messages == null) {
                    messages = doc.createElement("addMessages");
                }
                ((ModelMessage) o).addToOwnedElement(messages, serverPlayer);
            } else if (o instanceof HistoryEvent) {
                // Always send history objects
                if (history == null) {
                    history = doc.createElement("addHistory");
                }
                ((HistoryEvent) o).addToOwnedElement(history, serverPlayer);
            } else if (o instanceof FreeColGameObject) {
                FreeColGameObject fcgo = (FreeColGameObject) o;
                if (fcgo.isDisposed()) {
                    // Always remove disposed objects
                    fcgo.addToRemoveElement(remove);
                } else if (allVisible) {
                    // Ignore visibility test
                    update.appendChild(fcgo.toXMLElement(serverPlayer, doc));
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
                } else if (fcgo instanceof Player || fcgo instanceof Region) {
                    // Always update players and regions.
                    update.appendChild(fcgo.toXMLElement(serverPlayer, doc));
                } else {
                    logger.warning("Attempt to update: " + fcgo.getId()
                                   + " hidden from player " + serverPlayer.getId());
                }
            } else {
                throw new IllegalStateException("Bogus object: "
                                                + o.toString()
                                                + " class: " + o.getClass().toString());
            }
        }

        // Decide what to return.  If there are several parts with
        // children then return multiple, if there is one viable part,
        // return that, else null.  Remove elements need to be last,
        // then messages and history before everything else.
        int n = multiple.getChildNodes().getLength();
        Element result;
        if (update.hasChildNodes()) {
            multiple.appendChild(update);
            n++;
        }
        if (messages != null) {
            multiple.appendChild(messages);
            n++;
        }
        if (history != null) {
            multiple.appendChild(history);
            n++;
        }
        if (remove.hasChildNodes()) {
            multiple.appendChild(remove);
            n++;
        }
        switch (n) {
        case 0:
            if (attributes.isEmpty()) return null;
            doc.appendChild(update);
            result = update;
            break;
        case 1:
            result = (Element) multiple.getFirstChild();
            doc.appendChild(multiple.removeChild(result));
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
     * Build an "animateAttack" element for an update.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to update.
     * @param doc The owner <code>Document</code> to build the element in.
     * @param unit The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> which is defending.
     * @param result The result of the attack.
     * @return An "animateAttack" element, or null if the move can not be
     *         seen by the serverPlayer.
     */
    private Element buildAttack(ServerPlayer serverPlayer, Document doc,
                                Unit unit, Unit defender, CombatResult result) {
        if (serverPlayer == unit.getOwner()
            || serverPlayer == defender.getOwner()
            || (unit.isVisibleTo(serverPlayer)
                && defender.isVisibleTo(serverPlayer))) {
            Element element = doc.createElement("animateAttack");
            element.setAttribute("unit", unit.getId());
            element.setAttribute("defender", defender.getId());
            element.setAttribute("result", result.type.toString());
            return element;
        }
        return null;
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
    private Element buildMove(ServerPlayer serverPlayer, Document doc,
                              Unit unit, FreeColGameObject oldLocation,
                              Tile newTile) {
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
     * @param player1 The first <code>Player</code> to set stance for.
     * @param player2 The second <code>Player</code> to set stance for.
     * @return A "setStance" element, or null if the stance change is not
     *         for the serverPlayer.
     */
    private Element buildStance(ServerPlayer serverPlayer, Document doc,
                                Stance stance, Player player1, Player player2) {
        if ((Player) serverPlayer == player1 || (Player) serverPlayer == player2
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
     * Send an element to all players.
     *
     * @param element The <code>Element</code> to send.
     */
    public void sendToAll(Element element) {
        sendToList(getOtherPlayers(), element);
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
     * @param allObjects The objects to consider.
     */
    public void sendToOthers(ServerPlayer serverPlayer,
                             List<Object> allObjects) {
        sendToList(getOtherPlayers(serverPlayer), allObjects);
    }

    /**
     * Send an update to a list of players.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to send to.
     * @param allObjects The objects to consider.
     */
    public void sendToList(List<ServerPlayer> serverPlayers,
                           List<Object> allObjects) {
        // Strip off all objects at PRIVATE onward before updating.
        List<Object> objects = new ArrayList<Object>();
        for (Object o : allObjects) {
            if (o == UpdateType.PRIVATE) break;
            objects.add(o);
        }
        if (objects.isEmpty()) return;

        // Now send each other player their update.
        for (ServerPlayer other : serverPlayers) {
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
        sendToList(getOtherPlayers(serverPlayer), element);
    }

    /**
     * Send an element to a list of players.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to send to.
     * @param element An <code>Element</code> to send.
     */
    public void sendToList(List<ServerPlayer> serverPlayers, Element element) {
        if (element != null) {
            for (ServerPlayer other : serverPlayers) {
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
     * Ask for a reply from a specific player.
     *
     * @param player The <code>ServerPlayer</code> to ask.
     * @param element An <code>Element</code> containing a query.
     */
    public Element askElement(ServerPlayer player, Element element) {
        if (element != null) {
            try {
                return player.getConnection().ask(element);
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Change stance and Collect objects that need updating.
     *
     * @param player The originating <code>Player</code>.
     * @param stance The new <code>Stance</code>.
     * @param otherPlayer The <code>Player</code> wrt which the stance changes.
     * @param playerObjects A list of objects the player sees change.
     * @param otherObjects A list of objects the other player sees change.
     * @param restObjects A list of objects the rest of the players see change.
     * @return True if there was a change in stance at all.  If not,
     *           none of the lists will be changed, if so, they will.
     */
    private boolean changeStance(Player player, Stance stance,
                                 Player otherPlayer,
                                 List<Object> playerObjects,
                                 List<Object> otherObjects,
                                 List<Object> restObjects) {
        Stance oldStance = player.getStance(otherPlayer);
        if (oldStance == stance) return false;

        int pmodifier, omodifier;
        try {
            pmodifier = oldStance.getTensionModifier(stance);
            omodifier = otherPlayer.getStance(player).getTensionModifier(stance);
        } catch (IllegalStateException e) { // Catch illegal transitions
            logger.warning(e.getMessage());
            return false;
        }

        player.setStance(otherPlayer, stance);
        otherPlayer.setStance(player, stance);

        // Players not involved may see the stance change if it
        // meets the visibility criteria.
        addStance(restObjects, stance, player, otherPlayer);

        // The other player certainly sees the stance change, and
        // possibly some settlement alarm if player.isNative().
        addStance(otherObjects, stance, player, otherPlayer);
        if (pmodifier != 0) {
            otherObjects.addAll(player.modifyTension(otherPlayer, pmodifier));
        }

        // Player sees the stance change and similarly, possibly
        // some settlement alarm.
        addStance(playerObjects, stance, player, otherPlayer);
        if (omodifier != 0) {
            playerObjects.addAll(otherPlayer.modifyTension(player, omodifier));
        }
        return true;
    }

    /**
     * Change stance and inform all but the originating player.
     *
     * @param player The originating <code>Player</code>.
     * @param stance The new <code>Stance</code>.
     * @param otherPlayer The <code>Player</code> wrt which the stance changes.
     * @return A list of objects to update.
     */
    public List<Object> sendChangeStance(Player player, Stance stance,
                                         Player otherPlayer) {
        List<Object> playerObjects = new ArrayList<Object>();
        List<Object> otherObjects = new ArrayList<Object>();
        List<Object> restObjects = new ArrayList<Object>();
        if (changeStance(player, stance, otherPlayer,
                         playerObjects, otherObjects, restObjects)) {
            sendToList(getOtherPlayers((ServerPlayer) player,
                                       (ServerPlayer) otherPlayer),
                       restObjects);
            sendElement((ServerPlayer) otherPlayer, otherObjects);
        }
        return playerObjects;
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
     * @param serverPlayer The <code>ServerPlayer</code> whose market
     *            is to be updated.
     */
    public void yearlyGoodsRemoval(ServerPlayer serverPlayer) {
        List<Object> objects = new ArrayList<Object>();
        List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
        Market market = serverPlayer.getMarket();

        // Pick a random type of goods to remove an extra amount of.
        GoodsType removeType;
        do {
            int randomGoods = random.nextInt(goodsTypes.size());
            removeType = goodsTypes.get(randomGoods);
        } while (!removeType.isStorable());

        // Remove standard amount, and the extra amount.
        for (GoodsType type : goodsTypes) {
            if (type.isStorable() && market.hasBeenTraded(type)) {
                int amount = getGame().getTurn().getNumber() / 10;
                if (type == removeType && amount > 0) {
                    amount += random.nextInt(2 * amount + 1);
                }
                if (amount > 0) {
                    market.addGoodsToMarket(type, -amount);
                }
            }
            if (market.hasPriceChanged(type)) {
                objects.add(market.makePriceChangeMessage(type));
                market.flushPriceChange(type);
            }
        }

        // Update the client
        objects.add(market);
        sendElement(serverPlayer, objects);
    }

    /**
     * Create an element to mark a player as dead, inform other players, and
     * remove any leftover units.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to kill.
     * @return An <code>Element</code> to kill the player.
     */
    private Element killPlayerElement(ServerPlayer serverPlayer) {
        List<Object> objects = new ArrayList<Object>();
        List<Tile> tiles = new ArrayList<Tile>();

        // Notify
        objects.add(new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     ((serverPlayer.isEuropean())
                                      ? "model.diplomacy.dead.european"
                                      : "model.diplomacy.dead.native"),
                                     serverPlayer)
                    .addStringTemplate("%nation%",
                                       serverPlayer.getNationName()));

        // Clean up missions
        if (serverPlayer.isEuropean()) {
            for (ServerPlayer other : getOtherPlayers(serverPlayer)) {
                if (other.isIndian()) {
                    for (IndianSettlement s : other.getIndianSettlements()) {
                        Unit unit = s.getMissionary();
                        if (unit != null && unit.getOwner() == serverPlayer) {
                            s.setMissionary(null);
                            objects.add(unit.disposeList());
                            tiles.add(s.getTile());
                        }
                    }
                }
            }
        }

        for (Settlement settlement : serverPlayer.getSettlements()) {
            for (Tile tile : settlement.getOwnedTiles()) {
                if (!tiles.contains(tile)) tiles.add(tile);
            }
            objects.addAll(settlement.disposeList());
        }

        // Remove units
        for (Unit unit : serverPlayer.getUnits()) {
            Tile tile = unit.getTile();
            if (tile != null && !tiles.contains(tile)) tiles.add(tile);
            objects.addAll(unit.disposeList());
        }

        // Add setDead element
        Element element = Message.createNewRootElement("setDead");
        element.setAttribute("player", serverPlayer.getId());
        objects.add(element);

        // Add in the tiles, and return.
        objects.addAll(0, tiles);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Sets a new current player and notifies the clients.
     * @return The new current player.
     */
    private Player nextPlayer() {
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
            if (newPlayer.checkForDeath()) {
                newPlayer.setDead(true);
                Element element = killPlayerElement(newPlayer);
                sendToAll(element);
                logger.info(newPlayer.getNation() + " is dead.");
                return nextPlayer();
            }
        }
        
        if (newPlayer.isEuropean()) {
            yearlyGoodsRemoval(newPlayer);

            if (newPlayer.getCurrentFather() == null
                && newPlayer.getSettlements().size() > 0) {
                chooseFoundingFather(newPlayer);
            }

            if (newPlayer.getMonarch() != null && newPlayer.isConnected()) {
                List<RandomChoice<MonarchAction>> choices
                    = newPlayer.getMonarch().getActionChoices();
                final ServerPlayer player = newPlayer;
                final MonarchAction action
                    = (choices == null) ? MonarchAction.NO_ACTION
                    : RandomChoice.getWeightedRandom(random, choices);
                Thread t = new Thread("monarchAction") {
                        public void run() {
                            try {
                                monarchAction(player, action);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Monarch action failed!", e);
                            }
                        }
                    };
                t.start();
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
                FoundingFather father = RandomChoice.getWeightedRandom(random, rc);
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
     * Performs the monarchs actions.
     * 
     * @param serverPlayer The <code>ServerPlayer</code> whose monarch
     *            is acting.
     * @param action The monarch action.
     */
    private void monarchAction(ServerPlayer serverPlayer, MonarchAction action) {
        Specification spec = Specification.getSpecification();
        Monarch monarch = serverPlayer.getMonarch();
        Connection conn = serverPlayer.getConnection();
        int turn = getGame().getTurn().getNumber();
        Element monarchActionElement = Message.createNewRootElement("monarchAction");
        monarchActionElement.setAttribute("action", String.valueOf(action));

        switch (action) {
        case RAISE_TAX:
            int oldTax = serverPlayer.getTax();
            int newTax = monarch.raiseTax(random);
            Goods goods = serverPlayer.getMostValuableGoods();
            if (goods == null) return;
            monarchActionElement.setAttribute("amount", String.valueOf(newTax));
            // TODO: don't use localized name
            monarchActionElement.setAttribute("goods", Messages.message(goods.getNameKey()));
            monarchActionElement.setAttribute("force", String.valueOf(false));
            try {
                serverPlayer.setTax(newTax); // to avoid cheating
                Element reply = conn.ask(monarchActionElement);
                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                if (!accepted) {
                    Colony colony = (Colony) goods.getLocation();
                    if (colony.getGoodsCount(goods.getType()) >= goods.getAmount()) {
                        serverPlayer.setTax(oldTax); // player hasn't accepted, restoring tax
                        Element removeGoodsElement = Message.createNewRootElement("removeGoods");
                        colony.removeGoods(goods);
                        serverPlayer.setArrears(goods);
                        colony.getFeatureContainer().addModifier(Modifier
                                                                 .createTeaPartyModifier(getGame().getTurn()));
                        removeGoodsElement.appendChild(goods.toXMLElement(serverPlayer, removeGoodsElement
                                                                          .getOwnerDocument()));
                        conn.send(removeGoodsElement);
                    } else {
                        // player has cheated and removed goods from colony, don't restore tax
                        monarchActionElement.setAttribute("force", String.valueOf(true));
                        conn.send(monarchActionElement);
                    }
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
            }
            break;
        case LOWER_TAX:
            int lowerTax = monarch.lowerTax(random);
            monarchActionElement.setAttribute("amount", String.valueOf(lowerTax));
            try {
                serverPlayer.setTax(lowerTax); // to avoid cheating
                conn.send(monarchActionElement);
            } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
            }
            break;
        case ADD_TO_REF:
            List<AbstractUnit> unitsToAdd = monarch.addToREF(random);
            monarch.addToREF(unitsToAdd);
            Element additionElement = monarchActionElement.getOwnerDocument().createElement("addition");
            for (AbstractUnit unit : unitsToAdd) {
                additionElement.appendChild(unit.toXMLElement(serverPlayer,additionElement.getOwnerDocument()));
            }
            monarchActionElement.appendChild(additionElement);
            try {
                conn.send(monarchActionElement);
            } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
            }
            break;
        case DECLARE_WAR:
            Player enemy = monarch.declareWar(random);
            if (enemy == null) { // this should not happen
                logger.warning("Declared war on nobody.");
                return;
            }
            monarchActionElement.setAttribute("enemy", enemy.getId());
            List<Object> objects = sendChangeStance(serverPlayer, Stance.WAR, enemy);
            objects.add(0, monarchActionElement);
            sendElement(serverPlayer, objects);
            break;
            /** TODO: restore
                case Monarch.SUPPORT_LAND:
                int[] additions = monarch.supportLand();
                createUnits(additions, monarchActionElement, serverPlayer);
                try {
                conn.send(monarchActionElement);
                } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
                }
                break;
                case Monarch.SUPPORT_SEA:
                // TODO: make this generic
                UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.frigate");
                newUnit = new Unit(getGame(), serverPlayer.getEurope(), serverPlayer, unitType, UnitState.ACTIVE);
                //serverPlayer.getEurope().add(newUnit);
                monarchActionElement.appendChild(newUnit.toXMLElement(serverPlayer, monarchActionElement
                .getOwnerDocument()));
                try {
                conn.send(monarchActionElement);
                } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
                }
                break;
            */
        case OFFER_MERCENARIES:
            Element mercenaryElement = monarchActionElement.getOwnerDocument().createElement("mercenaries");
            List<AbstractUnit> units = monarch.getMercenaries(random);
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
                    serverPlayer.modifyGold(-price);
                    createUnits(units, updateElement, serverPlayer);
                    conn.send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
            }
            break;
        case NO_ACTION:
            // nothing to do here. :-)
            break;
        }
    }

    /**
     * Create the Royal Expeditionary Force player corresponding to
     * a given player that is about to rebel.
     *
     * @param serverPlayer The <code>ServerPlayer</code> about to rebel.
     * @return The REF player.
     */
    public ServerPlayer createREFPlayer(ServerPlayer serverPlayer) {
        Nation refNation = serverPlayer.getNation().getRefNation();
        ServerPlayer refPlayer = getFreeColServer().addAIPlayer(refNation);
        refPlayer.setEntryLocation(serverPlayer.getEntryLocation());
        Player.makeContact(serverPlayer, refPlayer); // Will change, setup only
        createREFUnits(serverPlayer, refPlayer);
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
            	Tile tile = map.getTile(colonyPosition.getAdjacent(direction));

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
            		if (!currentPlayer.atWarWith(player)
                    && !unit.hasAbility("model.ability.piracy")) {
                    logger.warning(colony.getName()
                                   + " found unit to not bombard: "
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


    /**
     * Declare independence.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is naming.
     * @param nationName The new name for the independent nation.
     * @param countryName The new name for its residents.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element declareIndependence(ServerPlayer serverPlayer,
                                       String nationName, String countryName) {
        List<Object> objects = new ArrayList<Object>();

        // Cross the Rubicon
        serverPlayer.setIndependentNationName(nationName);
        serverPlayer.setNewLandName(countryName);
        serverPlayer.setPlayerType(PlayerType.REBEL);
        serverPlayer.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        serverPlayer.modifyScore(SCORE_INDEPENDENCE_DECLARED);
        HistoryEvent h = new HistoryEvent(getGame().getTurn(),
                                          HistoryEvent.EventType.DECLARE_INDEPENDENCE);
        serverPlayer.addHistory(h);
        objects.add(h);

        // Clean up unwanted connections
        Europe europe = serverPlayer.getEurope();
        serverPlayer.divertModelMessages(europe, null);

        // Dispose of units in Europe.
        StringTemplate seized = StringTemplate.label(", ");
        for (Unit u : europe.getUnitList()) {
            seized.addStringTemplate(u.getLabel());
        }
        if (!seized.getReplacements().isEmpty()) {
            objects.add(new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                         "model.player.independence.unitsSeized",
                                         serverPlayer)
                        .addStringTemplate("%units%", seized));
        }

        // Generalized continental army muster
        java.util.Map<UnitType, UnitType> upgrades
            = new HashMap<UnitType, UnitType>();
        Specification spec = Specification.getSpecification();
        for (UnitType unitType : spec.getUnitTypeList()) {
            UnitType upgrade = unitType.getUnitTypeChange(ChangeType.INDEPENDENCE,
                                                          serverPlayer);
            if (upgrade != null) {
                upgrades.put(unitType, upgrade);
            }
        }
        for (Colony colony : serverPlayer.getColonies()) {
            int sol = colony.getSoL();
            if (sol > 50) {
                java.util.Map<UnitType, List<Unit>> unitMap = new HashMap<UnitType, List<Unit>>();
                List<Unit> allUnits = new ArrayList<Unit>(colony.getTile().getUnitList());
                allUnits.addAll(colony.getUnitList());
                for (Unit unit : allUnits) {
                    if (upgrades.containsKey(unit.getType())) {
                        List<Unit> unitList = unitMap.get(unit.getType());
                        if (unitList == null) {
                            unitList = new ArrayList<Unit>();
                            unitMap.put(unit.getType(), unitList);
                        }
                        unitList.add(unit);
                    }
                }
                for (Entry<UnitType, List<Unit>> entry : unitMap.entrySet()) {
                    int limit = (entry.getValue().size() + 2) * (sol - 50) / 100;
                    if (limit > 0) {
                        for (int index = 0; index < limit; index++) {
                            Unit unit = entry.getValue().get(index);
                            if (unit == null) break;
                            unit.setType(upgrades.get(entry.getKey()));
                            objects.add(unit);
                        }
                        objects.add(new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                                     "model.player.continentalArmyMuster",
                                                     serverPlayer, colony)
                                    .addName("%colony%", colony.getName())
                                    .addAmount("%number%", limit)
                                    .add("%oldUnit%", entry.getKey().getNameKey())
                                    .add("%unit%", upgrades.get(entry.getKey()).getNameKey()));
                    }
                }
            }
        }

        // Create the REF.
        ServerPlayer refPlayer = createREFPlayer(serverPlayer);

        // Now the REF is ready, we can dispose of the European connection.
        objects.addAll(serverPlayer.severEurope());

        // Other players only need a partial player update.
        List<Object> otherObjects = new ArrayList<Object>();
        addPartial(otherObjects, serverPlayer, "playerType",
                   "independentNationName", "newLandName");
        sendToOthers(serverPlayer, otherObjects);

        // Do this after the above update, so the other players see
        // the new nation name declaring war.
        // TODO: cut over to server-side stance handling.
        serverPlayer.changeRelationWithPlayer(refPlayer, Stance.WAR);

        // Pity to have to update such a heavy object as the player,
        // but we do this, at most, once per player.
        objects.add(serverPlayer);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Rename an object.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is naming.
     * @param object The <code>Nameable</code> to rename.
     * @param newName The new name.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element renameObject(ServerPlayer serverPlayer, Nameable object,
                                String newName) {
        object.setName(newName);

        // Others may be able to see the name change.
        List<Object> objects = new ArrayList<Object>();
        FreeColGameObject fcgo = (FreeColGameObject) object;
        addPartial(objects, fcgo, "name");
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Get a transaction session.  Either the current one if it exists,
     * or create a fresh one.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return A session describing the transaction.
     */
    public java.util.Map<String,Object> getTransactionSession(Unit unit, Settlement settlement) {
        java.util.Map<String, java.util.Map<String,Object>> unitTransactions = null;
        // Check for existing session, return it if present.
        if (transactionSessions.containsKey(unit.getId())) {
            unitTransactions = transactionSessions.get(unit.getId());
            if (unitTransactions.containsKey(settlement.getId())) {
                return unitTransactions.get(settlement.getId());
            }
        }

        // Session does not exist, create, store, and return it.
        java.util.Map<String,Object> session = new HashMap<String,Object>();
        // Default values
        session.put("actionTaken", false);
        session.put("unitMoves", unit.getMovesLeft());
        session.put("canGift", true);
        if (settlement.getOwner().atWarWith(unit.getOwner())) {
            session.put("canSell", false);
            session.put("canBuy", false);
        } else {
            session.put("canBuy", true);
            // The unit took nothing to sell, so nothing should be in
            // this session.
            session.put("canSell", unit.getSpaceTaken() != 0);
        }
        session.put("agreement", null);

        // Only keep track of human player sessions.
        if (unit.getOwner().isAI()) {
            return session;
        }
        
        // Save session for tracking
        // Unit has no open transactions?
        if (unitTransactions == null) {
            unitTransactions = new HashMap<String,java.util.Map<String, Object>>();
            transactionSessions.put(unit.getId(), unitTransactions);
        }
        unitTransactions.put(settlement.getId(), session);
        return session;
    }

    /**
     * Close a transaction session.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    public void closeTransactionSession(Unit unit, Settlement settlement) {
        // Only keep track of human player sessions.
        if (unit.getOwner().isAI()) {
            return;
        }

        if (!transactionSessions.containsKey(unit.getId())) {
            throw new IllegalStateException("Trying to close a non-existing session (unit)");
        }

        java.util.Map<String, java.util.Map<String,Object>> unitTransactions
            = transactionSessions.get(unit.getId());
        if (!unitTransactions.containsKey(settlement.getId())) {
            throw new IllegalStateException("Trying to close a non-existing session (settlement)");
        }

        unitTransactions.remove(settlement.getId());
        if (unitTransactions.isEmpty()) {
            transactionSessions.remove(unit.getId());
        }
    }
    
    /**
     * Query whether a transaction session exists.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return True if a session is already open.
     */
    public boolean isTransactionSessionOpen(Unit unit, Settlement settlement) {
        // AI does not need to send a message to open a session
        if (unit.getOwner().isAI()) return true;

        return transactionSessions.containsKey(unit.getId())
            && settlement != null
            && transactionSessions.get(unit.getId()).containsKey(settlement.getId());
    }

    /**
     * Get the client view of a transaction session, either existing or
     * newly opened.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element getTransaction(ServerPlayer serverPlayer, Unit unit,
                                  Settlement settlement) {
        List<Object> objects = new ArrayList<Object>();
        java.util.Map<String,Object> session;

        if (isTransactionSessionOpen(unit, settlement)) {
            session = getTransactionSession(unit, settlement);
        } else {
            if (unit.getMovesLeft() <= 0) {
                return Message.clientError("Unit " + unit.getId()
                                           + " has no moves left.");
            }
            session = getTransactionSession(unit, settlement);
            // Sets unit moves to zero to avoid cheating.  If no
            // action is taken, the moves will be restored when
            // closing the session
            unit.setMovesLeft(0);
            addPartial(objects, unit, "movesLeft");
        }

        // Add just the attributes the client needs.
        addAttribute(objects, "canBuy",
                     ((Boolean) session.get("canBuy")).toString());
        addAttribute(objects, "canSell",
                     ((Boolean) session.get("canSell")).toString());
        addAttribute(objects, "canGift",
                     ((Boolean) session.get("canGift")).toString());

        // Others can not see transactions.
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Close a transaction.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element closeTransaction(ServerPlayer serverPlayer, Unit unit,
                                    Settlement settlement) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("No such transaction session.");
        }

        java.util.Map<String,Object> session
            = getTransactionSession(unit, settlement);
        List<Object> objects = new ArrayList<Object>();

        // Restore unit movement if no action taken.
        Boolean actionTaken = (Boolean) session.get("actionTaken");
        if (!actionTaken) {
            Integer unitMoves = (Integer) session.get("unitMoves");
            unit.setMovesLeft(unitMoves);
            addPartial(objects, unit, "movesLeft");
        }
        closeTransactionSession(unit, settlement);

        // Others can not see end of transaction.
        return (objects.isEmpty()) ? null : buildUpdate(serverPlayer, objects);
    }


    /**
     * Get the goods for sale in a settlement.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An list of <code>Goods</code> for sale at the settlement.
     */
    public List<Goods> getGoodsForSale(Unit unit, Settlement settlement)
        throws IllegalStateException {
        List<Goods> sellGoods = null;

        if (settlement instanceof IndianSettlement) {
            IndianSettlement indianSettlement = (IndianSettlement) settlement;
            sellGoods = indianSettlement.getSellGoods();
            if (!sellGoods.isEmpty()) {
                AIPlayer aiPlayer = (AIPlayer) getFreeColServer().getAIMain()
                    .getAIObject(indianSettlement.getOwner());
                for (Goods goods : sellGoods) {
                    aiPlayer.registerSellGoods(goods);
                }
            }
        } else { // Colony might be supported one day?
            throw new IllegalStateException("Bogus settlement");
        }
        return sellGoods;
    }


    /**
     * Price some goods for sale from a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to buy.
     * @param price The buyers proposed price for the goods.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buyProposition(ServerPlayer serverPlayer,
                                  Unit unit, Settlement settlement,
                                  Goods goods, int price) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Proposing to buy without opening a transaction session?!");
        }
        java.util.Map<String,Object> session
            = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canBuy")) {
            return Message.clientError("Proposing to buy in a session where buying is not allowed.");
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain()
            .getAIObject(settlement.getOwner());
        int gold = ai.buyProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        List<Object> objects = new ArrayList<Object>();
        addAttribute(objects, "gold", Integer.toString(gold));
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Price some goods for sale to a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param price The sellers proposed price for the goods.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element sellProposition(ServerPlayer serverPlayer,
                                   Unit unit, Settlement settlement,
                                   Goods goods, int price) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Proposing to sell without opening a transaction session");
        }
        java.util.Map<String,Object> session
            = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canSell")) {
            return Message.clientError("Proposing to sell in a session where selling is not allowed.");
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain()
            .getAIObject(settlement.getOwner());
        int gold = ai.sellProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        List<Object> objects = new ArrayList<Object>();
        addAttribute(objects, "gold", Integer.toString(gold));
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Propagate an European market change to the other European markets.
     *
     * @param type The type of goods that was traded.
     * @param amount The amount of goods that was traded.
     * @param serverPlayer The player that performed the trade.
     */
    private void propagateToEuropeanMarkets(GoodsType type, int amount,
                                            ServerPlayer serverPlayer) {
        // Propagate 5-30% of the original change.
        final int lowerBound = 5; // TODO: make into game option?
        final int upperBound = 30;// TODO: make into game option?
        amount *= random.nextInt(upperBound - lowerBound + 1) + lowerBound;
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
     * Buy goods.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> to carry the goods.
     * @param type The <code>GoodsType</code> to buy.
     * @param amount The amount of goods to buy.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buyGoods(ServerPlayer serverPlayer, Unit unit,
                            GoodsType type, int amount) {
        List<Object> objects = new ArrayList<Object>();
        Market market = serverPlayer.getMarket();

        // FIXME: market.buy() should be here in the controller, but
        // there are two cases remaining that are hard to move still.
        //
        // 1. There is a shortcut buying of equipment in Europe in
        // Unit.equipWith().
        // 2. Also for the goods required for a building in
        // Colony.payForBuilding().  This breaks the pattern implemented
        // here as there is no unit involved.
        market.buy(type, amount, serverPlayer);
        unit.getGoodsContainer().addGoods(type, amount);
        objects.add(unit);
        addPartial(objects, serverPlayer, "gold");
        if (market.hasPriceChanged(type)) {
            // This type of goods has changed price, so we will update
            // the market and send a message as well.
            objects.add(market.makePriceChangeMessage(type));
            market.flushPriceChange(type);
        }
        propagateToEuropeanMarkets(type, amount, serverPlayer);

        // Action occurs in Europe, nothing is visible to other players.
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Sell goods.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is selling.
     * @param unit The <code>Unit</code> carrying the goods.
     * @param type The <code>GoodsType</code> to sell.
     * @param amount The amount of goods to sell.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element sellGoods(ServerPlayer serverPlayer, Unit unit,
                             GoodsType type, int amount) {
        List<Object> objects = new ArrayList<Object>();
        Market market = serverPlayer.getMarket();

        // FIXME: market.sell() should be in the controller, but the
        // following cases will have to wait.
        //
        // 1. Unit.dumpEquipment() gets called from a few places.
        // 2. Colony.exportGoods() is in the newTurn mess.
        // Its also still in MarketTest, which needs to be moved to
        // ServerPlayerTest where it also is already.
        //
        // Try to sell.
        market.sell(type, amount, serverPlayer);
        unit.getGoodsContainer().addGoods(type, -amount);
        objects.add(unit);
        addPartial(objects, serverPlayer, "gold");
        if (market.hasPriceChanged(type)) {
            // This type of goods has changed price, so update the
            // market and send a message as well.
            objects.add(market.makePriceChangeMessage(type));
            market.flushPriceChange(type);
        }
        propagateToEuropeanMarkets(type, amount, serverPlayer);

        // Action occurs in Europe, nothing is visible to other players.
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * A unit migrates from Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose unit it will be.
     * @param slot The slot within <code>Europe</code> to select the unit from.
     * @param type The type of migration occurring.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element emigrate(ServerPlayer serverPlayer, int slot,
                            MigrationType type) {
        List<Object> objects = new ArrayList<Object>();

        // Valid slots are in [1,3], recruitable indices are in [0,2].
        // An invalid slot is normal when the player has no control over
        // recruit type.
        boolean selected = 1 <= slot && slot <= Europe.RECRUIT_COUNT;
        int index = (selected) ? slot-1 : random.nextInt(Europe.RECRUIT_COUNT);

        // Create the recruit, move it to the docks.
        Europe europe = serverPlayer.getEurope();
        UnitType recruitType = europe.getRecruitable(index);
        Game game = getGame();
        Unit unit = new Unit(game, europe, serverPlayer, recruitType,
                             UnitState.ACTIVE,
                             recruitType.getDefaultEquipment());
        unit.setLocation(europe);

        // Handle migration type specific changes.
        switch (type) {
        case FOUNTAIN:
            serverPlayer.setRemainingEmigrants(serverPlayer.getRemainingEmigrants() - 1);
            break;
        case RECRUIT:
            serverPlayer.modifyGold(-europe.getRecruitPrice());
            europe.increaseRecruitmentDifficulty();
            // Fall through
        case NORMAL:
            serverPlayer.updateImmigrationRequired();
            serverPlayer.reduceImmigration();
            addPartial(objects, (Player) serverPlayer,
                       "immigration", "immigrationRequired");
            break;
        default:
            throw new IllegalArgumentException("Bogus migration type");
        }

        // Replace the recruit we used.
        europe.setRecruitable(index, serverPlayer.generateRecruitable());
        objects.add(europe);

        // Return an informative message only if this was an ordinary
        // migration where we did not select the unit type.
        // Other cases were selected.
        if (!selected) {
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
                    || (enemyUnit.isOffensiveUnit() && player.atWarWith(enemy))) {
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
            if (random.nextInt(Math.round(totalProbability) + 1) < attackPower) {
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
        return RandomChoice.getWeightedRandom(random, choices);
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
            unit.setType(learntUnitTypes.get(random.nextInt(learntUnitTypes.size())));
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.Learn", serverPlayer, unit)
                       .addStringTemplate("%unit%", oldName)
                       .add("%type%", unit.getType().getNameKey()));
            break;
        case TRIBAL_CHIEF:
            int chiefAmount = random.nextInt(dx * 10) + dx * 5;
            serverPlayer.modifyGold(chiefAmount);
            addPartial(objects, serverPlayer, "gold", "score");
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.TribalChief", serverPlayer, unit)
                       .addAmount("%money%", chiefAmount));
            break;
        case COLONIST:
            List<UnitType> newUnitTypes = specification.getUnitTypesWithAbility("model.ability.foundInLostCity");
            newUnit = new Unit(game, tile, serverPlayer,
                               newUnitTypes.get(random.nextInt(newUnitTypes.size())),
                               UnitState.ACTIVE);
            objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        "lostCityRumour.Colonist", serverPlayer, newUnit));
            break;
        case CIBOLA:
            String cityName = game.getCityOfCibola();
            if (cityName != null) {
                int treasureAmount = random.nextInt(dx * 600) + dx * 300;
                if (treasureUnitTypes == null) {
                    treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");
                }
                unitType = treasureUnitTypes.get(random.nextInt(treasureUnitTypes.size()));
                newUnit = new Unit(game, tile, serverPlayer, unitType, UnitState.ACTIVE);
                newUnit.setTreasureAmount(treasureAmount);
                objects.add(new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                            "lostCityRumour.Cibola", serverPlayer, newUnit)
                           .add("%city%", cityName)
                           .addAmount("%money%", treasureAmount));
                HistoryEvent h = new HistoryEvent(game.getTurn(), HistoryEvent.EventType.CITY_OF_GOLD)
                    .add("%city%", cityName)
                    .addAmount("%treasure%", treasureAmount);
                serverPlayer.addHistory(h);
                objects.add(h);
                break;
            }
            // Fall through, found all the cities of gold.
        case RUINS:
            int ruinsAmount = random.nextInt(dx * 2) * 300 + 50;
            if (ruinsAmount < 500) { // TODO remove magic number
                serverPlayer.modifyGold(ruinsAmount);
                addPartial(objects, serverPlayer, "gold", "score");
            } else {
                if (treasureUnitTypes == null) {
                    treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");
                }
                unitType = treasureUnitTypes.get(random.nextInt(treasureUnitTypes.size()));
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
                    List<RandomChoice<UnitType>> recruitables
                        = serverPlayer.generateRecruitablesList();
                    for (int k = 0; k < dx; k++) {
                        new Unit(game, europe, serverPlayer,
                                 RandomChoice.getWeightedRandom(random, recruitables),
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
                && !serverPlayer.hasContacted(otherPlayer)
                && !players.contains(otherPlayer)) {
                players.add(otherPlayer);
            }
        }
        return players;
    }

    /**
     * Activate units with sentry state which are adjacent to a tile
     * and not owned by the specified player.
     *
     * @param serverPlayer The player to exclude.
     * @param tile The center <code>Tile</code> to check from.
     * @return A list of <code>Unit</code>s now activated.
     */
    private List<Unit> activateSentries(ServerPlayer serverPlayer,
                                        Tile tile) {
        List<Unit> objects = new ArrayList<Unit>();
        Map map = getGame().getMap();
        for (Tile t : map.getSurroundingTiles(tile, 1, 1)) {
            for (Unit u : t.getUnitList()) {
                if (u.getState() == UnitState.SENTRY
                    && u.getOwner() != (Player) serverPlayer) {
                    u.setState(UnitState.ACTIVE);
                    objects.add(u);
                }
            }
        }
        return objects;
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
        objects.addAll(activateSentries(serverPlayer, newTile));

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
        addMove(objects, unit, oldLocation, newTile);

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
                Player.makeContact(serverPlayer, other);
                addStance(objects, Stance.PEACE, serverPlayer, other);
                // Add special first contact messages.
                boolean contactedIndians = false;
                boolean contactedEuro = false;
                for (ServerPlayer p : getOtherPlayers(serverPlayer)) {
                    if (serverPlayer.hasContacted(p) && p != other) {
                        if (p.isEuropean()) {
                            contactedEuro = true;
                            if (contactedIndians) break;
                        } else {
                            contactedIndians = true;
                            if (contactedEuro) break;
                        }
                    }
                }
                // Check first for a special panel for this nation
                String key = "EventPanel.MEETING_"
                    + Messages.message(other.getNationName()).toUpperCase();
                if (!Messages.containsKey(key)) {
                    key = (other.isEuropean() && !contactedEuro)
                        ? "EventPanel.MEETING_EUROPEANS"
                        : (other.isIndian() && !contactedIndians)
                        ? "EventPanel.MEETING_NATIVES"
                        : null;
                }
                if (key != null) {
                    privateObjects.add(new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                                        key, serverPlayer, other));
                }

                // History event for European players.
                if (serverPlayer.isEuropean()) {
                    HistoryEvent h = new HistoryEvent(turn,
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
        Turn turn = serverPlayer.getGame().getTurn();
        HistoryEvent h = new HistoryEvent(turn,
                    HistoryEvent.EventType.DISCOVER_NEW_WORLD)
            .addName("%name%", name);
        serverPlayer.addHistory(h);
        objects.add(h);

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
     * Move a unit to Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to move to Europe.
     */
    public Element moveToEurope(ServerPlayer serverPlayer, Unit unit) {

        if (unit.getLocation() instanceof Europe) {
            // Unit already in Europe, nothing to see for the others.
            unit.setState(UnitState.TO_EUROPE);
            return buildUpdate(serverPlayer, unit);
        }

        List<Object> objects = new ArrayList<Object>();
        Tile tile = unit.getTile();
        // Set entry location before setState (satisfy its check), then
        // location.
        Europe europe = serverPlayer.getEurope();
        unit.setEntryLocation(tile);
        unit.setState(UnitState.TO_EUROPE);
        unit.setLocation(europe);
        addRemove(objects, unit);
        objects.add(tile);
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, unit, tile, europe);
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
            addMove(objects, unit, oldLocation, carrier.getTile());
        }

        // Others can see the carrier capacity, and might see the
        // embarking unit board, but will certainly see it disappear.
        List<Object> otherObjects = new ArrayList<Object>();
        otherObjects.add(oldLocation);
        if (visible) {
            otherObjects.add(carrier);
            addMove(otherObjects, unit, oldLocation, carrier.getTile());
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
        if (!(unit.getLocation() instanceof Unit)) {
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
            } else if (random.nextInt(3) == 0) {
                // Otherwise 1/3 of cases are tales...
                radius = Math.max(radius, IndianSettlement.TALES_RADIUS);
                result = "tales";
            } else {
                // ...and the rest are beads.
                gold = (random.nextInt(400)
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
            settlement.setVisited(player);
            tile.updateIndianSettlementInformation(player);
            objects.add(tile);
            Map map = getFreeColServer().getGame().getMap();
            for (Tile t : map.getSurroundingTiles(tile, radius)) {
                if (!serverPlayer.canSee(t) && (t.isLand() || t.isCoast())) {
                    player.setExplored(t);
                    objects.add(t);
                }
            }

            // If the unit did not get promoted, update it for moves.
            unit.setMovesLeft(0);
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
        double denounce = random.nextInt(1000000) * 1.0e-6
            * enemy.getImmigration() / (serverPlayer.getImmigration() + 1);
        if (missionary.hasAbility("model.ability.expertMissionary")) {
            denounce += 0.2;
        }
        if (unit.hasAbility("model.ability.expertMissionary")) {
            denounce -= 0.2;
        }

        if (denounce < 0.5) { // Success, remove old mission and establish ours
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
                          IndianSettlement settlement,
                          Player enemy, int gold) {
        List<Object> objects = new ArrayList<Object>();
        ServerPlayer enemyPlayer = (ServerPlayer) enemy;

        // How much gold will be needed?
        Player nativePlayer = settlement.getOwner();
        Tension payingTension = nativePlayer.getTension(serverPlayer);
        Tension targetTension = nativePlayer.getTension(enemyPlayer);
        int payingValue = (payingTension == null) ? 0 : payingTension.getValue();
        int targetValue = (targetTension == null) ? 0 : targetTension.getValue();
        int goldToPay = (payingTension != null && targetTension != null
                      && payingValue > targetValue) ? 10000 : 5000;
        goldToPay += 20 * (payingValue - targetValue);
        goldToPay = Math.max(goldToPay, 650);

        // Try to incite?
        if (gold < 0) { // Initial enquiry.
            addAttribute(objects, "gold", Integer.toString(goldToPay));
        } else if (gold < goldToPay || serverPlayer.getGold() < gold) {
            objects.add(new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                         "indianSettlement.inciteGoldFail",
                                         serverPlayer, settlement)
                        .addStringTemplate("%player%", enemyPlayer.getNationName())
                        .addAmount("%amount%", goldToPay));
            addAttribute(objects, "gold", "0");
            unit.setMovesLeft(0);
            addPartial(objects, unit, "movesLeft");
        } else {
            // Success.  Raise the tension for the native player with respect
            // to the european player.  Let resulting stance changes happen
            // naturally in the AI player turn/s.
            nativePlayer.modifyTension(enemyPlayer, Tension.WAR_MODIFIER);
            enemyPlayer.modifyTension(serverPlayer,
                Tension.TENSION_ADD_WAR_INCITER);

            serverPlayer.modifyGold(-gold);
            nativePlayer.modifyGold(gold);
            addAttribute(objects, "gold", Integer.toString(gold));
            addPartial(objects, serverPlayer, "gold");
            unit.setMovesLeft(0);
            addPartial(objects, unit, "movesLeft");
        }

        // Update the inciter.
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Set a unit destination.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to set the destination for.
     * @param destination The <code>Location</code> to set as destination.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setDestination(ServerPlayer serverPlayer, Unit unit,
                                  Location destination) {
        if (unit.getTradeRoute() != null) unit.setTradeRoute(null);
        unit.setDestination(destination);

        // Others can not see a destination change.
        return buildUpdate(serverPlayer, unit);
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
     * Set current stop of a unit to the next valid stop if any.
     *
     * @param serverPlayer The <code>ServerPlayer</code> the unit belongs to.
     * @param unit The <code>Unit</code> to update.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element updateCurrentStop(ServerPlayer serverPlayer, Unit unit) {
        // Check if there is a valid current stop?
        int current = unit.validateCurrentStop();
        if (current < 0) return null; // No valid stop.

        ArrayList<Stop> stops = unit.getTradeRoute().getStops();
        int next = current;
        for (;;) {
            if (++next >= stops.size()) next = 0;
            if (next == current) break;
            if (hasWorkAtStop(unit, stops.get(next))) break;
        }

        // Next is the updated stop.
        // Could do just a partial update of currentStop if we did not
        // also need to set the unit destination.
        unit.setCurrentStop(next);
        unit.setDestination(stops.get(next).getLocation());

        // Others can not see a stop change.
        return buildUpdate(serverPlayer, unit);
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
            ; // Dumping is allowed
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
     * Buy from a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> that will carry the goods.
     * @param settlement The <code>IndianSettlement</code> to buy from.
     * @param goods The <code>Goods</code> to buy.
     * @param amount How much gold to pay.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buyFromSettlement(ServerPlayer serverPlayer, Unit unit,
                                     IndianSettlement settlement,
                                     Goods goods, int amount) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trying to buy without opening a transaction session");
        }
        java.util.Map<String,Object> session
            = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canBuy")) {
            return Message.clientError("Trying to buy in a session where buying is not allowed.");
        }
        if (unit.getSpaceLeft() <= 0) {
            return Message.clientError("Unit is full, unable to buy.");
        }
        // Check that this is the agreement that was made
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner());
        int returnGold = ai.buyProposition(unit, settlement, goods, amount);
        if (returnGold != amount) {
            return Message.clientError("This was not the price we agreed upon! Cheater?");
        }
        // Check this is funded.
        if (serverPlayer.getGold() < amount) {
            return Message.clientError("Insufficient gold to buy.");
        }

        // Valid, make the trade.
        List<Object> objects = new ArrayList<Object>();
        moveGoods(goods, unit);
        objects.add(unit);

        Player settlementPlayer = settlement.getOwner();
        settlement.updateWantedGoods();
        settlement.getTile().updateIndianSettlementInformation(serverPlayer);
        settlement.modifyAlarm(serverPlayer, -amount / 50);
        settlementPlayer.modifyGold(amount);
        serverPlayer.modifyGold(-amount);
        objects.add(UpdateType.PRIVATE);
        objects.add(settlement.getTile());
        addPartial(objects, serverPlayer, "gold");
        session.put("actionTaken", true);
        session.put("canBuy", false);

        // Others can see the unit capacity.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Sell to a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is selling.
     * @param unit The <code>Unit</code> carrying the goods.
     * @param settlement The <code>IndianSettlement</code> to sell to.
     * @param goods The <code>Goods</code> to sell.
     * @param amount How much gold to expect.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element sellToSettlement(ServerPlayer serverPlayer, Unit unit,
                                    IndianSettlement settlement,
                                    Goods goods, int amount) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trying to sell without opening a transaction session");
        }
        java.util.Map<String,Object> session
            = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canSell")) {
            return Message.clientError("Trying to sell in a session where selling is not allowed.");
        }

        // Check that the gold is the agreed amount
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner());
        int returnGold = ai.sellProposition(unit, settlement, goods, amount);
        if (returnGold != amount) {
            return Message.clientError("This was not the price we agreed upon! Cheater?");
        }

        // Valid, make the trade.
        List<Object> objects = new ArrayList<Object>();
        moveGoods(goods, settlement);
        objects.add(unit);

        Player settlementPlayer = settlement.getOwner();
        settlementPlayer.modifyGold(-amount);
        settlement.modifyAlarm(serverPlayer, -settlement.getPrice(goods) / 500);
        serverPlayer.modifyGold(amount);
        settlement.updateWantedGoods();
        settlement.getTile().updateIndianSettlementInformation(serverPlayer);
        objects.add(UpdateType.PRIVATE);
        objects.add(settlement.getTile());
        addPartial(objects, serverPlayer, "gold");
        session.put("actionTaken", true);
        session.put("canSell", false);

        // Others can see the unit capacity.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Deliver gift to settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is delivering.
     * @param unit The <code>Unit</code> that is delivering.
     * @param goods The <code>Goods</code> to deliver.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element deliverGiftToSettlement(ServerPlayer serverPlayer,
                                           Unit unit, Settlement settlement,
                                           Goods goods) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trying to deliverGift without opening a transaction session");
        }

        List<Object> objects = new ArrayList<Object>();
        java.util.Map<String,Object> session
            = getTransactionSession(unit, settlement);
        Tile tile = settlement.getTile();
        moveGoods(goods, settlement);
        objects.add(unit);
        if (settlement instanceof IndianSettlement) {
            IndianSettlement indianSettlement = (IndianSettlement) settlement;
            indianSettlement.modifyAlarm(serverPlayer, -indianSettlement.getPrice(goods) / 50);
            indianSettlement.updateWantedGoods();
            tile.updateIndianSettlementInformation(serverPlayer);
            objects.add(UpdateType.PRIVATE);
            objects.add(tile);
        }
        session.put("actionTaken", true);
        session.put("canGift", false);

        // Inform the receiver of the gift.
        ServerPlayer receiver = (ServerPlayer) settlement.getOwner();
        if (!receiver.isAI() && receiver.isConnected()
            && settlement instanceof Colony) {
            List<Object> giftObjects = new ArrayList<Object>();
            giftObjects.add(unit);
            giftObjects.add(settlement);
            giftObjects.add(new ModelMessage(ModelMessage.MessageType.GIFT_GOODS,
                                             "model.unit.gift", settlement, goods.getType())
                            .addStringTemplate("%player%", serverPlayer.getNationName())
                            .add("%type%", goods.getNameKey())
                            .addAmount("%amount%", goods.getAmount())
                            .addName("%colony%", settlement.getName()));
            sendElement(receiver, giftObjects);
        }
        logger.info("Gift delivered by unit: " + unit.getId()
                    + " to settlement: " + settlement.getName());

        // Others can see unit capacity.
        sendToList(getOtherPlayers(serverPlayer, receiver), objects);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Load cargo.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is loading.
     * @param unit The <code>Unit</code> to load.
     * @param goods The <code>Goods</code> to load.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element loadCargo(ServerPlayer serverPlayer, Unit unit,
                             Goods goods) {
        goods.adjustAmount();
        moveGoods(goods, unit);
        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
        }

        // Only have to update the carrier location, as that *must*
        // include the original location of the goods.
        // Others can see capacity change.
        FreeColGameObject fcgo = (FreeColGameObject) unit.getLocation();
        sendToOthers(serverPlayer, fcgo);
        return buildUpdate(serverPlayer, fcgo);
    }

    /**
     * Unload cargo.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is unloading.
     * @param unit The <code>Unit</code> to unload.
     * @param goods The <code>Goods</code> to unload.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element unloadCargo(ServerPlayer serverPlayer, Unit unit,
                               Goods goods) {
        FreeColGameObject update;
        Location loc;
        if (unit.isInEurope()) { // Must be a dump of boycotted goods
            loc = null;
            update = unit;
        } else if (unit.getTile() == null) {
            return Message.clientError("Unit not on the map.");
        } else if (unit.getTile().getSettlement() instanceof Colony) {
            loc = unit.getTile().getSettlement();
            update = unit.getTile();
        } else { // Dump of goods onto a tile
            loc = null;
            update = unit;
        }
        goods.adjustAmount();
        moveGoods(goods, loc);
        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
        }

        // Others can see the capacity change.
        if (update == unit) sendToOthers(serverPlayer, update);
        return buildUpdate(serverPlayer, update);
    }


    /**
     * Clear the specialty of a unit.
     *
     * @param serverPlayer The owner of the unit.
     * @param unit The <code>Unit</code> to clear the speciality of.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element clearSpeciality(ServerPlayer serverPlayer, Unit unit) {
        UnitType newType = unit.getType()
            .getUnitTypeChange(ChangeType.CLEAR_SKILL, serverPlayer);
        if (newType == null) {
            return Message.clientError("Can not clear unit speciality: "
                                       + unit.getId());
        }
        // There can be some restrictions that may prevent the
        // clearing of the speciality.  For example, teachers cannot
        // not be cleared of their speciality.
        Location oldLocation = unit.getLocation();
        if (oldLocation instanceof Building
            && !((Building) oldLocation).canAdd(newType)) {
            return Message.clientError("Cannot clear speciality, building does not allow new unit type");
        }

        // Valid, change type.
        unit.setType(newType);

        // Update just the unit, others can not see it.
        return buildUpdate(serverPlayer, unit);
    }


    /**
     * Disband a unit.
     * 
     * @param serverPlayer The owner of the unit.
     * @param unit The <code>Unit</code> to disband.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element disbandUnit(ServerPlayer serverPlayer, Unit unit) {
        List<Object> objects = new ArrayList<Object>();
        Location loc = unit.getLocation();

        // Dispose of the unit, send its old location but disable visibility
        // check as it may be a now-invisible tile.
        objects.addAll(unit.disposeList());
        objects.add(UpdateType.PRIVATE);
        objects.add(loc);

        // Others can see the unit removal and the space it leaves.
        sendToOthers(serverPlayer, loc, unit);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Generates a skill that could be taught from a settlement on the
     * given Tile.
     *
     * @param map The <code>Map</code>.
     * @param tile The <code>Tile</code> where the settlement will be located.
     * @param nationType The <code>IndianNationType</code> teaching.
     * @return A skill that can be taught to Europeans.
     */
    private UnitType generateSkillForLocation(Map map, Tile tile,
                                              IndianNationType nationType) {
        List<RandomChoice<UnitType>> skills = nationType.getSkills();
        java.util.Map<GoodsType, Integer> scale
            = new HashMap<GoodsType, Integer>();

        for (RandomChoice<UnitType> skill : skills) {
            scale.put(skill.getObject().getExpertProduction(), 1);
        }

        Iterator<Position> iter = map.getAdjacentIterator(tile.getPosition());
        while (iter.hasNext()) {
            Map.Position p = iter.next();
            Tile t = map.getTile(p);
            for (GoodsType goodsType : scale.keySet()) {
                scale.put(goodsType, scale.get(goodsType).intValue()
                          + t.potential(goodsType, null));
            }
        }

        List<RandomChoice<UnitType>> scaledSkills
            = new ArrayList<RandomChoice<UnitType>>();
        for (RandomChoice<UnitType> skill : skills) {
            UnitType unitType = skill.getObject();
            int scaleValue = scale.get(unitType.getExpertProduction()).intValue();
            scaledSkills.add(new RandomChoice<UnitType>(unitType, skill.getProbability() * scaleValue));
        }

        UnitType skill = RandomChoice.getWeightedRandom(random, scaledSkills);
        if (skill == null) {
            // Seasoned Scout
            Specification spec = FreeCol.getSpecification();
            List<UnitType> unitList
                = spec.getUnitTypesWithAbility("model.ability.expertScout");
            return unitList.get(random.nextInt(unitList.size()));
        }
        return skill;
    }

    /**
     * Build a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is building.
     * @param unit The <code>Unit</code> that is building.
     * @param name The new settlement name.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buildSettlement(ServerPlayer serverPlayer, Unit unit,
                                   String name) {
        Game game = serverPlayer.getGame();
        Tile tile = unit.getTile();
        Settlement settlement;
        if (serverPlayer.isEuropean()) {
            settlement = new Colony(game, serverPlayer, name, tile);
        } else {
            IndianNationType nationType
                = (IndianNationType) serverPlayer.getNationType();
            UnitType skill = generateSkillForLocation(game.getMap(), tile,
                                                      nationType);
            settlement = new IndianSettlement(game, serverPlayer, tile,
                                              name, false, skill,
                                              new HashSet<Player>(), null);
            // TODO: its lame that the settlement starts with no contacts
        }
        settlement.placeSettlement();

        // Join.
        unit.setState(UnitState.IN_COLONY);
        unit.setLocation(settlement);
        unit.setMovesLeft(0);

        // Update with settlement tile, and newly owned tiles.
        List<Object> objects = new ArrayList<Object>();
        objects.add(tile);
        Map map = game.getMap();
        for (Tile t : map.getSurroundingTiles(tile, settlement.getRadius())) {
            if (t.getOwningSettlement() == settlement) objects.add(t);
        }

        // History is private.
        objects.add(UpdateType.PRIVATE);
        HistoryEvent h = new HistoryEvent(game.getTurn(),
                                          HistoryEvent.EventType.FOUND_COLONY)
            .addName("%colony%", settlement.getName());
        serverPlayer.addHistory(h);
        objects.add(h);

        // Also send any tiles that can now be seen because the colony
        // can perhaps see further than the founding unit.
        for (Tile t : map.getSurroundingTiles(tile, unit.getLineOfSight() + 1,
                                              settlement.getLineOfSight())) {
            if (!objects.contains(t)) objects.add(t);
        }

        // Potentially lots to see.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Join a colony.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> that is joining.
     * @param colony The <code>Colony</code> to join.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element joinColony(ServerPlayer serverPlayer, Unit unit,
                              Colony colony) {
        List<Tile> ownedTiles = colony.getOwnedTiles();
        Tile tile = colony.getTile();

        // Join.
        unit.setState(UnitState.IN_COLONY);
        unit.setLocation(colony);
        unit.setMovesLeft(0);

        // Update with colony tile, and tiles now owned.
        List<Object> objects = new ArrayList<Object>();
        objects.add(tile);
        Map map = serverPlayer.getGame().getMap();
        for (Tile t : map.getSurroundingTiles(tile, colony.getRadius())) {
            if (t.getOwningSettlement() == colony && !ownedTiles.contains(t)) {
                objects.add(t);
            }
        }

        // Potentially lots to see.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
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
            h = new HistoryEvent(getGame().getTurn(),
                                 HistoryEvent.EventType.ABANDON_COLONY)
                .addName("%colony%", settlement.getName());
        }

        // Now do the dispose.
        objects.addAll(settlement.disposeList());

        if (h != null) { // History is private.
            serverPlayer.addHistory(h);
            objects.add(UpdateType.PRIVATE);
            objects.add(h);
        }

        // TODO: Player.settlements is still being fixed on the client side.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Claim land.
     *
     * @param serverPlayer The <code>ServerPlayer</code> claiming.
     * @param tile The <code>Tile</code> to claim.
     * @param settlement The <code>Settlement</code> to claim for.
     * @param price The price to pay for the land, which must agree
     *              with the owner valuation, unless negative which
     *              denotes stealing.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element claimLand(ServerPlayer serverPlayer, Tile tile,
                             Settlement settlement, int price) {
        Player owner = tile.getOwner();
        Settlement ownerSettlement = tile.getOwningSettlement();
        tile.setOwningSettlement(settlement);
        tile.setOwner(serverPlayer);
        tile.updatePlayerExploredTiles();

        // Update the tile for all, and privately any now-angrier
        // owners, or the player gold if a price was paid.
        List<Object> objects = new ArrayList<Object>();
        objects.add(tile);
        objects.add(UpdateType.PRIVATE);
        if (price > 0) {
            serverPlayer.modifyGold(-price);
            owner.modifyGold(price);
            addPartial(objects, serverPlayer, "gold");
        } else if (price < 0 && owner.isIndian()) {
            owner.modifyTension(serverPlayer, Tension.TENSION_ADD_LAND_TAKEN,
                                (IndianSettlement) ownerSettlement);
            objects.add(ownerSettlement);
        }

        // Others can see the tile.
        sendToOthers(serverPlayer, objects);
        return buildUpdate(serverPlayer, objects);
    }


    /**
     * Accept a diplomatic trade.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param other The other <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param agreement The <code>DiplomaticTrade</code> to consider.
     * @return An <code>Element</code> encapsulating the changes.
     */
    private Element acceptTrade(ServerPlayer serverPlayer, ServerPlayer other,
                                Unit unit, Settlement settlement,
                                DiplomaticTrade agreement) {
        closeTransactionSession(unit, settlement);

        ArrayList<Object> objects = new ArrayList<Object>();
        boolean sawGold = false;
        boolean sawGoods = false;
        for (TradeItem tradeItem : agreement.getTradeItems()) {
            // Check trade carefully before committing.
            if (!tradeItem.isValid()) {
                logger.warning("Trade with invalid tradeItem: "
                               + tradeItem.toString());
                continue;
            }
            ServerPlayer source = (ServerPlayer) tradeItem.getSource();
            if (source != serverPlayer && source != other) {
                logger.warning("Trade with invalid source: "
                               + ((source == null) ? "null" : source.getId()));
                continue;
            }
            ServerPlayer dest = (ServerPlayer) tradeItem.getDestination();
            if (dest != serverPlayer && dest != other) {
                logger.warning("Trade with invalid destination: "
                               + ((dest == null) ? "null" : dest.getId()));
                continue;
            }
            tradeItem.makeTrade();

            // Collect objects for updating.  Not very OO but
            // TradeItem should not know about server internals.
            Colony colony = tradeItem.getColony();
            if (colony != null) {
                objects.addAll(colony.getOwnedTiles());
            }
            if (tradeItem.getGold() > 0) sawGold = true;
            Goods goods = tradeItem.getGoods();
            if (goods != null) {
                sawGoods = true;
                objects.add(unit);
            }
            Stance stance = tradeItem.getStance();
            if (stance != null) {
                addStance(objects, stance, serverPlayer, other);
            }
            Unit newUnit = tradeItem.getUnit();
            if (newUnit != null) {
                objects.add(newUnit);
            }
        }
        sendToList(getOtherPlayers(serverPlayer, other), objects);
        // Have to disable the visibility tests for objects that
        // change hands.
        ArrayList<Object> ourObjects = new ArrayList<Object>();
        objects.add(0, UpdateType.PRIVATE);
        ourObjects.addAll(objects);
        if (sawGold) {
            addPartial(ourObjects, serverPlayer, "gold", "score");
            addPartial(objects, other, "gold", "score");
        }
        if (sawGoods) {
            objects.add(settlement);
        }
        sendElement(other, objects);
        // Original player sees conclusion of diplomacy, and move update.
        addPartial(ourObjects, unit, "movesLeft");
        objects.add(new DiplomacyMessage(unit, settlement, agreement)
                    .toXMLElement());
        return buildUpdate(serverPlayer, ourObjects);
    }

    /**
     * Reject a diplomatic trade.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param other The other <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param agreement The <code>DiplomaticTrade</code> to consider.
     * @return An <code>Element</code> encapsulating the changes.
     */
    private Element rejectTrade(ServerPlayer serverPlayer, ServerPlayer other,
                                Unit unit, Settlement settlement,
                                DiplomaticTrade agreement) {
        closeTransactionSession(unit, settlement);

        ArrayList<Object> objects = new ArrayList<Object>();
        addPartial(objects, unit, "movesLeft");
        objects.add(new DiplomacyMessage(unit, settlement, agreement)
                    .toXMLElement());
        return buildUpdate(serverPlayer, objects);
    }

    /**
     * Diplomatic trades.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param agreement The <code>DiplomaticTrade</code> to consider.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element diplomaticTrade(ServerPlayer serverPlayer, Unit unit,
                                   Settlement settlement,
                                   DiplomaticTrade agreement) {
        DiplomacyMessage diplomacy;
        java.util.Map<String,Object> session;
        DiplomaticTrade current;
        ServerPlayer other = (ServerPlayer) settlement.getOwner();
        unit.setMovesLeft(0);

        switch (agreement.getStatus()) {
        case ACCEPT_TRADE:
            if (!isTransactionSessionOpen(unit, settlement)) {
                return Message.clientError("Accepting without open session.");
            }
            session = getTransactionSession(unit, settlement);
            // Act on what was proposed, not what is in the accept
            // message to frustrate tricksy client changing the conditions.
            current = (DiplomaticTrade) session.get("agreement");
            current.setStatus(TradeStatus.ACCEPT_TRADE);

            diplomacy = new DiplomacyMessage(unit, settlement, current);
            sendElement(other, diplomacy.toXMLElement());
            return acceptTrade(serverPlayer, other, unit, settlement, current);

        case REJECT_TRADE:
            if (!isTransactionSessionOpen(unit, settlement)) {
                return Message.clientError("Rejecting without open session.");
            }
            session = getTransactionSession(unit, settlement);
            current = (DiplomaticTrade) session.get("agreement");
            current.setStatus(TradeStatus.REJECT_TRADE);

            diplomacy = new DiplomacyMessage(unit, settlement, current);
            sendElement(other, diplomacy.toXMLElement());
            return rejectTrade(serverPlayer, other, unit, settlement, current);

        case PROPOSE_TRADE:
            session = getTransactionSession(unit, settlement);
            current = agreement;
            session.put("agreement", agreement);

            // If the unit is on a carrier we need to update the
            // client with it first as the diplomacy message refers to it.
            // Ask the other player about this proposal.
            diplomacy = new DiplomacyMessage(unit, settlement, agreement);
            Element proposal = (unit.isOnCarrier())
                ? buildUpdate(other, UpdateType.PRIVATE, unit,
                              diplomacy.toXMLElement())
                : diplomacy.toXMLElement();
            Element response = askElement(other, proposal);

            // What did they think?
            diplomacy = (response == null) ? null
                : new DiplomacyMessage(getGame(), response);
            agreement = (diplomacy == null) ? null : diplomacy.getAgreement();
            TradeStatus status = (agreement == null) ? TradeStatus.REJECT_TRADE
                : agreement.getStatus();
            switch (status) {
            case ACCEPT_TRADE:
                // Act on the proposed agreement, not what was passed back
                // as accepted.
                current.setStatus(TradeStatus.ACCEPT_TRADE);
                return acceptTrade(serverPlayer, other, unit, settlement,
                                   current);

            case PROPOSE_TRADE:
                // Save the counter-proposal, sanity test, then pass back.
                if ((ServerPlayer) agreement.getSender() == serverPlayer
                    && (ServerPlayer) agreement.getRecipient() == other) {
                    session.put("agreement", agreement);
                    return diplomacy.toXMLElement();
                }
                logger.warning("Trade counter-proposal was incompatible.");
                // Fall through

            case REJECT_TRADE:
            default:
                // Reject the current trade.
                current.setStatus(TradeStatus.REJECT_TRADE);
                return rejectTrade(serverPlayer, other, unit, settlement,
                                   current);
            }

        default:
            return Message.clientError("Bogus trade");
        }
    }

}
