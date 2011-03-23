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

package net.sf.freecol.client.control;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.panel.MonarchPanel;
import net.sf.freecol.client.gui.panel.VictoryPanel;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Handles the network messages that arrives while in the getGame().
 */
public final class InGameInputHandler extends InputHandler {

    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    private Unit lastAnimatedUnit = null;


    /**
     * The constructor to use.
     * 
     * @param freeColClient The main controller.
     */
    public InGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);
    }

    /**
     * Deals with incoming messages that have just been received.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The root element of the message.
     * @return The reply.
     */
    @Override
    public Element handle(Connection connection, Element element) {
        Element reply = null;

        if (element != null) {
            String type = element.getTagName();

            logger.log(Level.FINEST, "Received message " + type);

            if (type.equals("update")) {
                reply = update(element);
            } else if (type.equals("remove")) {
                reply = remove(element);
            } else if (type.equals("animateMove")) {
                reply = animateMove(element);
            } else if (type.equals("animateAttack")) {
                reply = animateAttack(element);
            } else if (type.equals("setCurrentPlayer")) {
                reply = setCurrentPlayer(element);
            } else if (type.equals("newTurn")) {
                reply = newTurn(element);
            } else if (type.equals("setDead")) {
                reply = setDead(element);
            } else if (type.equals("gameEnded")) {
                reply = gameEnded(element);
            } else if (type.equals("chat")) {
                reply = chat(element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(element);
            } else if (type.equals("error")) {
                reply = error(element);
            } else if (type.equals("chooseFoundingFather")) {
                reply = chooseFoundingFather(element);
            } else if (type.equals("indianDemand")) {
                reply = indianDemand(element);
            } else if (type.equals("reconnect")) {
                reply = reconnect(element);
            } else if (type.equals("setAI")) {
                reply = setAI(element);
            } else if (type.equals("monarchAction")) {
                reply = monarchAction(element);
            } else if (type.equals("setStance")) {
                reply = setStance(element);
            } else if (type.equals("diplomacy")) {
                reply = diplomacy(element);
            } else if (type.equals("addPlayer")) {
                reply = addPlayer(element);
            } else if (type.equals("addObject")) {
                reply = addObject(element);
            } else if (type.equals("multiple")) {
                reply = multiple(connection, element);
            } else {
                logger.warning("Message is of unsupported type \"" + type + "\".");
            }
            logger.log(Level.FINEST, "Handled message " + type
                       + " replying with "
                       + ((reply == null) ? "null" : reply.getTagName()));
        } else {
            throw new RuntimeException("Received empty (null) message! - should never happen");
        }

        return reply;
    }

    /**
     * Handles an "reconnect"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element reconnect(Element element) {
        logger.finest("Entered reconnect...");
        if (new ShowConfirmDialogSwingTask(null, "reconnect.text", "reconnect.yes", "reconnect.no").confirm()) {
            logger.finest("User wants to reconnect, do it!");
            new ReconnectSwingTask().invokeLater();
        } else {
            // This fairly drastic operation can be done in any thread,
            // no need to use SwingUtilities.
            logger.finest("No reconnect, quit.");
            getFreeColClient().quit();
        }
        return null;
    }

    /**
     * Handles an "update"-message.
     * 
     * @param updateElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     * @return The reply.
     */
    public Element update(Element updateElement) {

        updateGameObjects(updateElement.getChildNodes());

        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }

    /**
     * Updates all FreeColGameObjects from the childNodes of the message
     * 
     * @param nodeList The list of nodes from the message
     */
    private void updateGameObjects(NodeList nodeList) {
        Game game = getGame();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = element.getAttribute("ID");
            FreeColGameObject fcgo = game.getFreeColGameObjectSafely(id);
            if (fcgo == null) {
                logger.warning("Object in update not present in client: " + id);
            } else {
                fcgo.readFromXMLElement(element);
            }
        }
    }

    /**
     * Handles a "remove"-message.
     * 
     * @param removeElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     */
    private Element remove(Element removeElement) {
        Game game = getGame();
        String divertString = removeElement.getAttribute("divert");
        FreeColGameObject divert
            = (divertString == null || divertString.isEmpty()) ? null
            : game.getFreeColGameObject(divertString);
        Player player = getFreeColClient().getMyPlayer();
        NodeList nodeList = removeElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String idString = element.getAttribute("ID");
            FreeColGameObject fcgo
                = (idString == null || idString.isEmpty()) ? null
                : game.getFreeColGameObject(idString);
            if (fcgo == null) {
                logger.warning("Could not find FreeColGameObject with ID: "
                               + idString);
            } else {
                if (divert != null) {
                    player.divertModelMessages(fcgo, divert);
                }
                // Deselect the object if it is the current active unit.
                GUI gui = getFreeColClient().getCanvas().getGUI();
                if (fcgo instanceof Unit
                    && (Unit)fcgo == gui.getActiveUnit()) {
                    gui.setActiveUnit(null);
                }

                // Do just the low level dispose that removes
                // reference to this object in the client.  The other
                // updates should have done the rest.
                fcgo.fundamentalDispose();
            }
        }
        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }

    /**
     * Sometimes units appear which the client does not know about,
     * and are passed in as the first child of the parent element.
     *
     * @param game The <code>Game</code> to add the unit to.
     * @param element The <code>Element</code> to find a unit in.
     * @return A unit or null if none found.
     */
    private static Unit getUnitFromElement(Game game, Element element) {
        if (element.getFirstChild() != null) {
            return new Unit(game, (Element) element.getFirstChild());
        }
        return null;
    }

    /**
     * Sometimes units appear which the client does not know about,
     * and are passed in as the children of the parent element.
     *
     * @param game The <code>Game</code> to add the unit to.
     * @param element The <code>Element</code> to find a unit in.
     * @param id The id of the unit to find.
     * @return A unit or null if none found.
     */
    private static Unit selectUnitFromElement(Game game, Element element,
                                              String id) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            if (id.equals(e.getAttribute("ID"))) {
                return new Unit(game, e);
            }
        }
        return null;
    }

    /**
     * Handles an "animateMove"-message. This only performs animation,
     * if required. It does not actually change unit positions, which
     * happens in an "update".
     * 
     * @param element An element (root element in a DOM-parsed XML tree) that
     *            holds attributes for the old and new tiles and an element for
     *            the unit that is moving (which are used solely to operate the
     *            animation).
     */
    private Element animateMove(Element element) {
        FreeColClient client = getFreeColClient();
        Game game = getGame();
        String unitId = element.getAttribute("unit");
        if (unitId == null) {
            logger.warning("Animation"
                           + " for: " + client.getMyPlayer().getId()
                           + " ommitted unitId");
            return null;
        }
        Unit unit = (Unit) game.getFreeColGameObjectSafely(unitId);
        if (unit == null
            && (unit = selectUnitFromElement(game, element, unitId)) == null) {
            logger.warning("Animation"
                           + " for: " + client.getMyPlayer().getId()
                           + " incorrectly omitted unit: " + unitId);
            return null;
        }
        ClientOptions options = client.getClientOptions();
        boolean ourUnit = unit.getOwner() == client.getMyPlayer();
        String key = (ourUnit) ? ClientOptions.MOVE_ANIMATION_SPEED
            : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        if (!client.isHeadless() && options.getInteger(key) > 0) {
            String oldTileId = element.getAttribute("oldTile");
            String newTileId = element.getAttribute("newTile");
            Tile oldTile = (Tile) game.getFreeColGameObjectSafely(oldTileId);
            Tile newTile = (Tile) game.getFreeColGameObjectSafely(newTileId);
            if (newTile == null || oldTile == null) {
                throw new IllegalStateException("animateMove unit: " + unitId
                    + ((oldTile == null) ? ": null oldTile" : "")
                    + ((newTile == null) ? ": null newTile" : ""));
            }

            // All is well, queue the animation.
            // Use lastAnimatedUnit as a filter to avoid excessive refocussing.
            try {
                new UnitMoveAnimationCanvasSwingTask(unit, oldTile, newTile,
                                                     unit != lastAnimatedUnit)
                    .invokeSpecial();
            } catch (Exception exception) {
                logger.warning("UnitMoveAnimationCanvasSwingTask raised "
                               + exception.toString());
            }
            lastAnimatedUnit = unit;
        }
        return null;
    }

    /**
     * Handles an "animateAttack"-message. This only performs animation, if
     * required. It does not actually perform any attacks.
     * 
     * @param element An element (root element in a DOM-parsed XML tree) that
     *            holds attributes for the old and new tiles and an element for
     *            the unit that is moving (which are used solely to operate the
     *            animation).
     */
    private Element animateAttack(Element element) {
        FreeColClient client = getFreeColClient();
        if (client.isHeadless()) return null;
        Game game = getGame();
        String attackerId = element.getAttribute("attacker");
        Unit attacker = (Unit) game.getFreeColGameObjectSafely(attackerId);
        if (attacker == null
            && (attacker = selectUnitFromElement(game, element,
                                                 attackerId)) == null) {
            logger.warning("Attack animation"
                           + " for: " + client.getMyPlayer().getId()
                           + " incorrectly omitted attacker: " + attackerId);
            return null;
        }
        String defenderId = element.getAttribute("defender");
        Unit defender = (Unit) game.getFreeColGameObjectSafely(defenderId);
        if (defender == null
            && (defender = selectUnitFromElement(game, element,
                                                 defenderId)) == null) {
            logger.warning("Attack animation"
                           + " for: " + client.getMyPlayer().getId()
                           + " incorrectly omitted defender: " + defenderId);
            return null;
        }

        boolean success = Boolean.parseBoolean(element.getAttribute("success"));
        if (attacker == null || defender == null) {
            throw new IllegalStateException("animateAttack"
                    + ((attacker == null) ? ": null attacker" : "")
                    + ((defender == null) ? ": null defender" : ""));
        }

        // All is well, queue the animation.
        // Use lastAnimatedUnit as a filter to avoid excessive refocussing.
        try {
            new UnitAttackAnimationCanvasSwingTask(attacker, defender, success,
                                                   attacker != lastAnimatedUnit)
                .invokeSpecial();
        } catch (Exception exception) {
            logger.warning("UnitAttackAnimationCanvasSwingTask raised "
                           + exception.toString());
        }
        lastAnimatedUnit = attacker;
        return null;
    }

    /**
     * Handles a "setCurrentPlayer"-message.
     * 
     * @param setCurrentPlayerElement The element (root element in a DOM-parsed
     *            XML tree) that holds all the information.
     */
    private Element setCurrentPlayer(Element element) {
        Game game = getGame();
        Player newPlayer = (Player) game
            .getFreeColGameObject(element.getAttribute("player"));
        Player player = getFreeColClient().getMyPlayer();

        new SetCurrentPlayerSwingTask(newPlayer,
                                      player.equals(game.getCurrentPlayer()),
                                      player.equals(newPlayer))
            .invokeLater();
        return null;
    }

    /**
     * Handles a "newTurn"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     */
    private Element newTurn(Element element) {
        Game game = getGame();
        String turnString = element.getAttribute("turn");
        try {
            int turnNumber = Integer.parseInt(turnString);
            game.setTurn(new Turn(turnNumber));
        } catch (NumberFormatException e) {
            logger.warning("Bad turn in newTurn: " + turnString);
        }
        Turn currTurn = game.getTurn();
        if (currTurn.getYear() == 1600
            && Turn.getYear(currTurn.getNumber() - 1) == 1599) {
            new ShowInformationMessageSwingTask("twoTurnsPerYear").invokeLater();
        }
        new UpdateMenuBarSwingTask().invokeLater();
        return null;
    }

    /**
     * Handles a "setDead"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element setDead(Element element) {
        FreeColClient freeColClient = getFreeColClient();
        Player player = (Player) getGame().getFreeColGameObject(element.getAttribute("player"));

        if (player == freeColClient.getMyPlayer()) {
            if (freeColClient.isSingleplayer()) {
                if (!new ShowConfirmDialogSwingTask(null, "defeatedSingleplayer.text", "defeatedSingleplayer.yes",
                        "defeatedSingleplayer.no").confirm()) {
                    freeColClient.quit();
                } else {
                    freeColClient.getFreeColServer().enterRevengeMode(player.getName());
                }
            } else {
                if (!new ShowConfirmDialogSwingTask(null, "defeated.text", "defeated.yes", "defeated.no").confirm()) {
                    freeColClient.quit();
                }
            }
        }

        return null;
    }

    /**
     * Handles a "gameEnded"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element gameEnded(Element element) {
        FreeColClient freeColClient = getFreeColClient();

        Player winner = (Player) getGame().getFreeColGameObject(element.getAttribute("winner"));
        if (winner == freeColClient.getMyPlayer()) {
            new ShowVictoryPanelSwingTask().invokeLater();
        } // else: The client has already received the message of defeat.

        return null;
    }

    /**
     * Handles a "chat"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element chat(Element element) {
        final ChatMessage chatMessage = new ChatMessage(getGame(), element);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Canvas canvas = getFreeColClient().getCanvas();
                canvas.displayChatMessage(chatMessage.getPlayer(),
                                          chatMessage.getMessage(),
                                          chatMessage.isPrivate());
            }
        });
        return null;
    }

    /**
     * Handles an "error"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element error(Element element) {
        try {
            new ShowErrorMessageSwingTask(element.getAttribute("messageID"),
                                          element.getAttribute("message"))
                .invokeSpecial();
        } catch (Exception exception) {
            logger.warning("error() raised " + exception.toString());
        }
        return null;
    }

    /**
     * Handles a "setAI"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element setAI(Element element) {

        Player p = (Player) getGame().getFreeColGameObject(element.getAttribute("player"));
        p.setAI(Boolean.valueOf(element.getAttribute("ai")).booleanValue());

        return null;
    }

    /**
     * Handles an "chooseFoundingFather"-request.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element chooseFoundingFather(Element element) {
        final List<FoundingFather> possibleFoundingFathers = new ArrayList<FoundingFather>();
        for (FoundingFatherType type : FoundingFatherType.values()) {
            String id = element.getAttribute(type.toString());
            if (id != null && !id.equals("")) {
                FoundingFather father = getGame().getSpecification()
                    .getFoundingFather(id);
                if (father == null) {
                    logger.warning("Bogus " + type + " father: " + id);
                } else {
                    possibleFoundingFathers.add(father);
                }
            }
        }

        FoundingFather foundingFather = new ShowSelectFoundingFatherSwingTask(possibleFoundingFathers).select();
        if (foundingFather != null) {
            getFreeColClient().getMyPlayer().setCurrentFather(foundingFather);
            Element e = Message.createNewRootElement("chooseFoundingFather");
            e.setAttribute("foundingFather", foundingFather.getId());
            getFreeColClient().getClient().send(e);
        }
        return null;
    }

    /**
     * Handles a "diplomacy"-request.  If the message informs of an
     * acceptance or rejection then display the result and return
     * null.  If the message is a proposal, then ask the user about
     * it and return the response with appropriate response set.
     *
     * @param element The element (root element in a DOM-parsed XML tree)
     *            containing a "diplomacy"-message.
     * @return A diplomacy response, or null if none required.
     */
    private Element diplomacy(Element element) {
        Game game = getGame();
        Player player = getFreeColClient().getMyPlayer();
        DiplomacyMessage message = new DiplomacyMessage(game, element);
        Unit unit = message.getUnit(game);
        if (unit == null
            && (unit = getUnitFromElement(game, element)) == null) {
            logger.warning("Unit incorrectly omitted from diplomacy message.");
            return null;
        }
        Settlement settlement = message.getSettlement(game);
        Player other = (unit.getOwner() == player) ? settlement.getOwner()
            : unit.getOwner();
        String nation = Messages.message(other.getNationName());
        DiplomaticTrade ourAgreement;
        DiplomaticTrade theirAgreement = message.getAgreement();

        switch (theirAgreement.getStatus()) {
        case ACCEPT_TRADE:
            new ShowInformationMessageSwingTask("negotiationDialog.offerAccepted",
                                                "%nation%", nation).show();
            break;
        case REJECT_TRADE:
            new ShowInformationMessageSwingTask("negotiationDialog.offerRejected",
                                                "%nation%", nation).show();
            break;
        case PROPOSE_TRADE:
            ourAgreement = new ShowNegotiationDialogSwingTask(unit, settlement,
                                                              theirAgreement)
                .select();
            if (ourAgreement == null) {
                theirAgreement.setStatus(TradeStatus.REJECT_TRADE);
            } else {
                message.setAgreement(ourAgreement);
            }
            return message.toXMLElement();
        default:
            logger.warning("Bogus trade status");
            break;
        }
        return null;
    }

    /**
     * Handles an "indianDemand"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element indianDemand(Element element) {
        Game game = getGame();
        Player player = getFreeColClient().getMyPlayer();
        IndianDemandMessage message = new IndianDemandMessage(game, element);

        Unit unit = message.getUnit(game);
        if (unit == null) {
            logger.warning("IndianDemand with null unit: " + element.getAttribute("unit"));
            return null;
        }
        Colony colony = message.getColony(game);
        if (colony == null) {
            logger.warning("IndianDemand with null colony: " + element.getAttribute("colony"));
            return null;
        } else if (colony.getOwner() != player) {
            throw new IllegalArgumentException("Demand to anothers colony");
        }
        Goods goods = message.getGoods();
        String goldStr = Integer.toString(message.getGold());
        boolean accepted;
        ModelMessage m = null;
        String nation = Messages.message(unit.getOwner().getNationName());
        int opt = getFreeColClient().getClientOptions()
            .getInteger(ClientOptions.INDIAN_DEMAND_RESPONSE);
        if (goods == null) {
            switch (opt) {
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ASK:
                accepted = new ShowConfirmDialogSwingTask(colony.getTile(),
                        "indianDemand.gold.text",
                        "indianDemand.gold.yes", "indianDemand.gold.no",
                        "%nation%", nation,
                        "%colony%", colony.getName(),
                        "%amount%", goldStr).confirm();
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ACCEPT:
                m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                    "indianDemand.gold.text", colony, unit)
                    .addName("%nation%", nation)
                    .addName("%colony%", colony.getName())
                    .addName("%amount%", goldStr);
                accepted = true;
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_REJECT:
                m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                    "indianDemand.gold.text", colony, unit)
                    .addName("%nation%", nation)
                    .addName("%colony%", colony.getName())
                    .addName("%amount%", goldStr);
                accepted = false;
                break;
            default:
                throw new IllegalArgumentException("Impossible option value.");
            }
        } else {
            String amount = String.valueOf(goods.getAmount());
            switch (opt) {
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ASK:
                if (goods.getType().isFoodType()) {
                    accepted = new ShowConfirmDialogSwingTask(colony.getTile(),
                            "indianDemand.food.text",
                            "indianDemand.food.yes", "indianDemand.food.no",
                            "%nation%", nation,
                            "%colony%", colony.getName(),
                            "%amount%", String.valueOf(goods.getAmount()))
                        .confirm();
                } else {
                    accepted = new ShowConfirmDialogSwingTask(colony.getTile(),
                            "indianDemand.other.text",
                            "indianDemand.other.yes", "indianDemand.other.no",
                            "%nation%", nation,
                            "%colony%", colony.getName(),
                            "%amount%", String.valueOf(goods.getAmount()),
                            "%goods%", Messages.message(goods.getNameKey()))
                        .confirm();
                }
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ACCEPT:
                if (goods.getType().isFoodType()) {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                        "indianDemand.food.text", colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", amount);
                } else {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                        "indianDemand.other.text", colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", amount)
                        .add("%goods%", goods.getNameKey());
                }
                accepted = true;
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_REJECT:
                if (goods.getType().isFoodType()) {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                        "indianDemand.food.text", colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", amount);
                } else {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                         "indianDemand.other.text", colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", amount)
                        .add("%goods%", goods.getNameKey());
                }
                accepted = false;
                break;
            default:
                throw new IllegalArgumentException("Impossible option value.");
            }
        }
        if (m != null) {
            player.addModelMessage(m);
        }
        element.setAttribute("accepted", String.valueOf(accepted));
        return element;
    }

    /**
     * Handles a "monarchAction"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element monarchAction(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        Game game = getGame();
        Specification spec = game.getSpecification();
        Player player = freeColClient.getMyPlayer();
        Monarch monarch = player.getMonarch();
        MonarchActionMessage message = new MonarchActionMessage(game, element);
        final MonarchAction action = message.getAction();
        boolean accept;
        String amount;
        String additions;

        switch (action) {
        case NO_ACTION:
            break;

        case RAISE_TAX:
            amount = Integer.toString(message.getAmount());
            GoodsType goodsType = message.getGoodsType(game);
            String goods = Messages.message(goodsType.getLabel(true));
            accept = new ShowMonarchPanelSwingTask(action,
                                                   "%replace%", amount,
                                                   "%goods%", goods).confirm();
            element.setAttribute("accepted", String.valueOf(accept));
            new UpdateMenuBarSwingTask().invokeLater();
            return element;

        case LOWER_TAX:
            int newTax = message.getAmount();
            amount = Integer.toString(newTax);
            String diff = Integer.toString(player.getTax() - newTax);
            new ShowMonarchPanelSwingTask(action,
                                          "%difference%", diff,
                                          "%newTax%", amount).confirm();
            new UpdateMenuBarSwingTask().invokeLater();
            break;

        case WAIVE_TAX:
            new ShowMonarchPanelSwingTask(action).confirm();
            break;

        case ADD_TO_REF: case SUPPORT_LAND: case SUPPORT_SEA:
            additions = unitListSummary(message.getAdditions());
            new ShowMonarchPanelSwingTask(action,
                                          "%addition%", additions).confirm();
            break;

        case DECLARE_WAR:
            Player enemy = message.getEnemy(game);
            String nationName = Messages.message(enemy.getNationName());
            new ShowMonarchPanelSwingTask(action,
                                          "%nation%", nationName).confirm();
            break;

        case OFFER_MERCENARIES:
            amount = Integer.toString(message.getAmount());
            additions = unitListSummary(message.getAdditions());
            accept = new ShowMonarchPanelSwingTask(action,
                                                   "%gold%", amount,
                                                   "%mercenaries%", additions)
                .confirm();
            element.setAttribute("accepted", String.valueOf(accept));
            if (accept) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        freeColClient.getCanvas().updateGoldLabel();
                    }
                });
            }
            return element;

        default:
            logger.warning("Bogus action: " + action);
            break;
        }
        return null;
    }

    /**
     * Summarizes a list of units to a string.
     *
     * @param unitList The list of <code>AbstractUnit</code>s to summarize.
     * @return A summary.
     */
    private String unitListSummary(List<AbstractUnit> unitList) {
        ArrayList<String> unitNames = new ArrayList<String>();
        for (AbstractUnit au : unitList) {
            unitNames.add(au.getNumber() + " "
                          + Messages.message(Messages.getLabel(au)));
        }
        return Utils.join(" " + Messages.message("and") + " ", unitNames);
    }

    /**
     * Handles a "setStance"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element setStance(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        Player player = freeColClient.getMyPlayer();
        Game game = getGame();
        Stance stance = Enum.valueOf(Stance.class, element.getAttribute("stance"));
        Player first = (Player) game.getFreeColGameObject(element.getAttribute("first"));
        Player second = (Player) game.getFreeColGameObject(element.getAttribute("second"));

        if (player == first && first.getStance(second) == Stance.UNCONTACTED) {
            freeColClient.playSound("sound.event.meet." + second.getNationID());
        }
        try {
            first.setStance(second, stance);
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Illegal stance transition", e);
            return null;
        }

        return null;
    }

    /**
     * Handles an "addPlayer"-message.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element addPlayer(Element element) {

        Element playerElement = (Element) element.getElementsByTagName(Player.getXMLElementTagName()).item(0);
        if (getGame().getFreeColGameObject(playerElement.getAttribute("ID")) == null) {
            Player newPlayer = new Player(getGame(), playerElement);
            getGame().addPlayer(newPlayer);
        } else {
            getGame().getFreeColGameObject(playerElement.getAttribute("ID")).readFromXMLElement(playerElement);
        }

        return null;
    }

    /**
     * Disposes of the <code>Unit</code>s which are the children of this
     * Element.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element disposeUnits(Element element) {
        Game game = getGame();
        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            // Do not read the whole unit out of the element as we are
            // only going to dispose of it, not forgetting that the
            // server may have already done so and its view will only
            // mislead us here in the client.
            Element e = (Element) nodes.item(i);
            FreeColGameObject fcgo = game.getFreeColGameObjectSafely(e.getAttribute("ID"));

            if (fcgo instanceof Unit) {
                ((Unit) fcgo).dispose();
            } else {
                logger.warning("Object is not a unit: " + ((fcgo == null) ? "null" : fcgo.getId()));
            }
        }
        return null;
    }

    /**
     * Add the objects which are the children of this Element.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element addObject(Element element) {
        Game game = getGame();
        Specification spec = game.getSpecification();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String owner = e.getAttribute("owner");
            Player player = null;
            if (!(game.getFreeColGameObjectSafely(owner) instanceof Player)) {
                logger.warning("addObject with broken owner: "
                               + ((owner == null) ? "(null)" : owner));
                continue;
            }
            player = (Player) game.getFreeColGameObjectSafely(owner);
            String tag = e.getTagName();
            if (tag == null) {
                logger.warning("addObject null tag");
            } else if (tag == FoundingFather.getXMLElementTagName()) {
                String id = e.getAttribute("id");
                FoundingFather father = spec.getFoundingFather(id);
                if (father != null) player.addFather(father);
            } else if (tag == HistoryEvent.getXMLElementTagName()) {
                HistoryEvent h = new HistoryEvent();
                h.readFromXMLElement(e);
                player.getHistory().add(h);
            } else if (tag == LastSale.getXMLElementTagName()) {
                LastSale s = new LastSale();
                s.readFromXMLElement(e);
                player.saveSale(s);
            } else if (tag == ModelMessage.getXMLElementTagName()) {
                ModelMessage m = new ModelMessage();
                m.readFromXMLElement(e);
                player.addModelMessage(m);
            } else if (tag == TradeRoute.getXMLElementTagName()) {
                TradeRoute t = new TradeRoute(game, e);
                player.getTradeRoutes().add(t);
            } else {
                logger.warning("addObject unrecognized: " + tag);
            }
        }
        return null;
    }


    /**
     * Handle all the children of this element.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element multiple(Connection connection, Element element) {
        NodeList nodes = element.getChildNodes();
        Element reply = null;

        for (int i = 0; i < nodes.getLength(); i++) {
            reply = handle(connection, (Element) nodes.item(i));
        }
        return reply;
    }


    /**
     * 
     * Handler methods end here.
     * 
     */

    /**
     * This utility class is the base class for tasks that need to run in the
     * event dispatch thread.
     */
    abstract static class SwingTask implements Runnable {
        private static final Logger taskLogger = Logger.getLogger(SwingTask.class.getName());


        /**
         * Run the task and wait for it to complete.
         * 
         * @return return value from {@link #doWork()}.
         * @throws InvocationTargetException on unexpected exceptions.
         */
        public Object invokeAndWait() throws InvocationTargetException {
            verifyNotStarted();
            markStarted(true);
            try {
                SwingUtilities.invokeAndWait(this);
            } catch (InterruptedException e) {
                throw new InvocationTargetException(e);
            }
            return _result;
        }

        /**
         * Run the task at some later time. Any exceptions will occur in the
         * event dispatch thread. The return value will be set, but at present
         * there is no good way to know if it is valid yet.
         */
        public void invokeLater() {
            verifyNotStarted();
            markStarted(false);
            SwingUtilities.invokeLater(this);
        }

        /*
         * Some calls can be required from both within the EventDispatchThread
         * (when the client controller calls InGameInputHandler.handle() with
         * replies to its requests to the server), and from outside of the
         * thread when handling other player moves. The former case must be done
         * right now, the latter needs to be queued and waited for.
         */
        public Object invokeSpecial() throws InvocationTargetException {
            return (SwingUtilities.isEventDispatchThread())
                ? doWork()
                : invokeAndWait();
        }

        /**
         * Mark started and set the synchronous flag.
         * 
         * @param synchronous The synch/asynch flag.
         */
        private synchronized void markStarted(boolean synchronous) {
            _synchronous = synchronous;
            _started = true;
        }

        /**
         * Mark finished.
         */
        private synchronized void markDone() {
            _started = false;
        }

        /**
         * Throw an exception if the task is started.
         */
        private synchronized void verifyNotStarted() {
            if (_started) {
                throw new IllegalStateException("Swing task already started!");
            }
        }

        /**
         * Check if the client is waiting.
         * 
         * @return true if client is waiting for a result.
         */
        private synchronized boolean isSynchronous() {
            return _synchronous;
        }

        /**
         * Run method, call {@link #doWork()} and save the return value. Also
         * catch any exceptions. In synchronous mode they will be rethrown to
         * the original thread, in asynchronous mode they will be logged and
         * ignored. Nothing is gained by crashing the event dispatch thread.
         */
        public final void run() {
            try {
                if (taskLogger.isLoggable(Level.FINEST)) {
                    taskLogger.log(Level.FINEST, "Running Swing task " + getClass().getName() + "...");
                }

                setResult(doWork());

                if (taskLogger.isLoggable(Level.FINEST)) {
                    taskLogger.log(Level.FINEST, "Swing task " + getClass().getName() + " returned " + _result);
                }
            } catch (RuntimeException e) {
                taskLogger.log(Level.WARNING, "Swing task " + getClass().getName() + " failed!", e);
                // Let the exception bubble up if the calling thread is waiting
                if (isSynchronous()) {
                    throw e;
                }
            } finally {
                markDone();
            }
        }

        /**
         * Get the return vale from {@link #doWork()}.
         * 
         * @return result.
         */
        public synchronized Object getResult() {
            return _result;
        }

        /**
         * Save result.
         * 
         * @param r The result.
         */
        private synchronized void setResult(Object r) {
            _result = r;
        }

        /**
         * Override this method to do the actual work.
         * 
         * @return result.
         */
        protected abstract Object doWork();


        private Object _result;

        private boolean _synchronous;

        private boolean _started;
    }

    /**
     * Base class for Swing tasks that need to do a simple update without return
     * value using the canvas.
     */
    abstract class NoResultCanvasSwingTask extends SwingTask {

        protected Object doWork() {
            doWork(getFreeColClient().getCanvas());
            return null;
        }

        abstract void doWork(Canvas canvas);
    }

    /**
     * This task refreshes the entire canvas.
     */
    class RefreshCanvasSwingTask extends NoResultCanvasSwingTask {
        /**
         * Default constructor, simply refresh canvas.
         */
        public RefreshCanvasSwingTask() {
            this(false);
        }

        /**
         * Constructor.
         * 
         * @param requestFocus True to request focus after refresh.
         */
        public RefreshCanvasSwingTask(boolean requestFocus) {
            _requestFocus = requestFocus;
        }

        protected void doWork(Canvas canvas) {
            canvas.refresh();

            if (_requestFocus && !canvas.isShowingSubPanel()) {
                canvas.requestFocusInWindow();
            }
        }


        private final boolean _requestFocus;
    }

    /**
     * A task to set a new current player.
     */
    class SetCurrentPlayerSwingTask extends RefreshCanvasSwingTask {

        private Player newPlayer;
        private boolean oldTurn;
        private boolean newTurn;

        /**
         * Task constructor.
         *
         * @param newPlayer The <code>Player</code> to set to be the
         *     current player.
         * @param oldTurn True if the client player is to be replaced as
         *     current player.
         * @param newTurn True if the client player is to become the
         *     current player.
         */
        public SetCurrentPlayerSwingTask(Player newPlayer, boolean oldTurn,
                                         boolean newTurn) {
            super(true);
            this.newPlayer = newPlayer;
            this.oldTurn = oldTurn;
            this.newTurn = newTurn;
        }

        public void doWork(Canvas canvas) {
            super.doWork(canvas);
            FreeColClient fcc = getFreeColClient();

            // If our turn is ending, clean up all open popups.
            if (oldTurn) canvas.closeMenus();

            // Set the new player
            fcc.getInGameController().setCurrentPlayer(newPlayer);

            // If our turn is beginning, select a unit.
            if (newTurn) {
                fcc.getInGameController()
                    .nextActiveUnit(newPlayer.getEntryLocation().getTile());
            }
        }
    }

    class RefreshTilesSwingTask extends NoResultCanvasSwingTask {

        public RefreshTilesSwingTask(Tile oldTile, Tile newTile) {
            super();
            _oldTile = oldTile;
            _newTile = newTile;
        }

        void doWork(Canvas canvas) {
            canvas.refreshTile(_oldTile);
            canvas.refreshTile(_newTile);
        }


        private final Tile _oldTile;

        private final Tile _newTile;

    }

    /**
     * This task plays an unit movement animation in the Canvas.
     */
    class UnitMoveAnimationCanvasSwingTask extends NoResultCanvasSwingTask {

        private final Unit unit;

        private final Tile destinationTile;

        private final Tile sourceTile;

        private boolean focus;


        /**
         * Constructor - Play the unit movement animation, always focusing on
         * the source tile.
         * 
         * @param unit The unit that is moving.
         * @param sourceTile The Tile from which the unit is moving.
         * @param destinationTile The Tile where the unit will be moving to.
         */
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Tile sourceTile, Tile destinationTile) {
            this(unit, sourceTile, destinationTile, true);
        }

        /**
         * Constructor - Play the unit movement animation, optionally focusing
         * on the source tile.
         * 
         * @param unit The unit that is moving.
         * @param sourceTile The Tile from which the unit is moving.
         * @param destinationTile The Tile where the unit will be moving to.
         * @param focus Focus on the source tile before the animation.
         */
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Tile sourceTile, Tile destinationTile, boolean focus) {
            this.unit = unit;
            this.sourceTile = sourceTile;
            this.destinationTile = destinationTile;
            this.focus = focus;
        }

        protected void doWork(Canvas canvas) {
            GUI gui = canvas.getGUI();
            if (focus || !gui.onScreen(sourceTile.getPosition())) {
                gui.setFocusImmediately(sourceTile.getPosition());
            }
            Animations.unitMove(canvas, unit, sourceTile, destinationTile);
            canvas.refresh();
        }
    }

    /**
     * This task plays an unit attack animation in the Canvas.
     */
    class UnitAttackAnimationCanvasSwingTask extends NoResultCanvasSwingTask {

        private final Unit unit;

        private final Unit defender;

        private final boolean success;

        private boolean focus;


        /**
         * Constructor - Play the unit attack animation, always focusing on the
         * source tile.
         * 
         * @param unit The <code>Unit</code> that is attacking.
         * @param defender The <code>Unit</code> that is defending.
         * @param success Did the attack succeed?
         */
        public UnitAttackAnimationCanvasSwingTask(Unit unit, Unit defender,
                                                  boolean success) {
            this(unit, defender, success, true);
        }

        /**
         * Constructor - Play the unit attack animation, optionally focusing on
         * the source tile.
         * 
         * @param unit The <code>Unit</code> that is attacking.
         * @param defender The <code>Unit</code> that is defending.
         * @param success Did the attack succeed?
         * @param focus Focus on the source tile before the animation.
         */
        public UnitAttackAnimationCanvasSwingTask(Unit unit, Unit defender,
                                                  boolean success, boolean focus) {
            this.unit = unit;
            this.defender = defender;
            this.success = success;
            this.focus = focus;
        }

        protected void doWork(Canvas canvas) {
            GUI gui = canvas.getGUI();
            if (focus || !gui.onScreen(unit.getTile().getPosition())) {
                gui.setFocusImmediately(unit.getTile().getPosition());
            }
            Animations.unitAttack(canvas, unit, defender, success);
            canvas.refresh();
        }
    }

    /**
     * This task reconnects to the server.
     */
    class ReconnectSwingTask extends SwingTask {
        protected Object doWork() {
            getFreeColClient().getConnectController().reconnect();
            return null;
        }
    }

    /**
     * This task updates the menu bar.
     */
    class UpdateMenuBarSwingTask extends NoResultCanvasSwingTask {
        protected void doWork(Canvas canvas) {
            getFreeColClient().updateMenuBar();
        }
    }

    /**
     * This task shows the victory panel.
     */
    class ShowVictoryPanelSwingTask extends NoResultCanvasSwingTask {
        protected void doWork(Canvas canvas) {
            canvas.showPanel(new VictoryPanel(canvas));
        }
    }

    /**
     * This class shows a dialog and saves the answer (ok/cancel).
     */
    class ShowConfirmDialogSwingTask extends SwingTask {

        private Tile tile;

        private String text;

        private String okText;

        private String cancelText;

        private String[] replace;


        /**
         * Constructor.
         * 
         * @param tile An optional tile to make visible.
         * @param text The key for the question.
         * @param okText The key for the OK button.
         * @param cancelText The key for the Cancel button.
         * @param replace The replacement values.
         */
        public ShowConfirmDialogSwingTask(Tile tile, String text, String okText, String cancelText, String... replace) {
            this.tile = tile;
            this.text = text;
            this.okText = okText;
            this.cancelText = cancelText;
            this.replace = replace;
        }

        /**
         * Show dialog and wait for selection.
         * 
         * @return true if OK, false if Cancel.
         */
        public boolean confirm() {
            try {
                Object result = invokeAndWait();
                return ((Boolean) result).booleanValue();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            boolean choice = canvas.showConfirmDialog(tile, text, okText, cancelText, replace);
            return Boolean.valueOf(choice);
        }
    }

    /**
     * Base class for dialog SwingTasks.
     */
    abstract class ShowMessageSwingTask extends SwingTask {
        /**
         * Show dialog and wait for the user to dismiss it.
         */
        public void show() {
            try {
                invokeAndWait();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    /**
     * This class shows a model message.
     */
    class ShowModelMessageSwingTask extends ShowMessageSwingTask {

        /**
         * Constructor.
         * 
         * @param modelMessage The model message to show.
         */
        public ShowModelMessageSwingTask(ModelMessage modelMessage) {
            _modelMessage = modelMessage;
        }

        protected Object doWork() {
            getFreeColClient().getCanvas().showModelMessages(_modelMessage);
            return null;
        }


        private ModelMessage _modelMessage;
    }

    /**
     * This class shows an informational dialog.
     */
    class ShowInformationMessageSwingTask extends ShowMessageSwingTask {

        private String messageId;

        private String[] replace;


        /**
         * Constructor.
         * 
         * @param messageId The key for the message.
         * @param replace The values to replace text with.
         */
        public ShowInformationMessageSwingTask(String messageId, String... replace) {
            this.messageId = messageId;
            this.replace = replace;
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            canvas.showInformationMessage(null, messageId, replace);
            return null;
        }
    }

    /**
     * This class shows an error dialog.
     */
    class ShowErrorMessageSwingTask extends ShowMessageSwingTask {

        /**
         * Constructor.
         * 
         * @param messageId The i18n-keyname of the error message to display.
         * @param message An alternative message to display if the resource
         *            specified by <code>messageID</code> is unavailable.
         */
        public ShowErrorMessageSwingTask(String messageId, String message) {
            _messageId = messageId;
            _message = message;
        }

        protected Object doWork() {
            getFreeColClient().getCanvas().errorMessage(_messageId, _message);
            return null;
        }


        private String _messageId;

        private String _message;
    }

    /**
     * This class displays a dialog that lets the player pick a Founding Father.
     */
    abstract class ShowSelectSwingTask extends SwingTask {
        /**
         * Show dialog and wait for selection.
         * 
         * @return selection.
         */
        public int select() {
            try {
                Object result = invokeAndWait();
                return ((Integer) result).intValue();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    /**
     * This class displays a dialog that lets the player pick a Founding Father.
     */
    class ShowSelectFoundingFatherSwingTask extends SwingTask {

        private List<FoundingFather> choices;


        /**
         * Constructor.
         * 
         * @param choices The possible founding fathers.
         */
        public ShowSelectFoundingFatherSwingTask(List<FoundingFather> choices) {
            this.choices = choices;
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            return canvas.showFreeColDialog(new ChooseFoundingFatherDialog(canvas, choices));
        }

        /**
         * Show dialog and wait for selection.
         * 
         * @return selection.
         */
        public FoundingFather select() {
            try {
                Object result = invokeAndWait();
                return (FoundingFather) result;
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    /**
     * This class displays a negotiation dialog.
     */
    class ShowNegotiationDialogSwingTask extends SwingTask {

        /**
         * Constructor.
         * 
         * @param unit The unit which init the negotiation.
         * @param settlement The settlement where the unit has made the proposal
         * @param proposal The proposal made by unit's owner.
         */
        public ShowNegotiationDialogSwingTask(Unit unit, Settlement settlement, DiplomaticTrade proposal) {
            this.unit = unit;
            this.settlement = settlement;
            this.proposal = proposal;
        }

        /**
         * Show dialog and wait for selection.
         * 
         * @return selection.
         */
        public DiplomaticTrade select() {
            try {
                Object result = invokeAndWait();
                return (DiplomaticTrade) result;
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        protected Object doWork() {
            return getFreeColClient().getCanvas().showNegotiationDialog(unit, settlement, proposal);
        }


        private Unit unit;

        private Settlement settlement;

        private DiplomaticTrade proposal;
    }

    /**
     * This class shows the monarch panel.
     */
    class ShowMonarchPanelSwingTask extends SwingTask {

        /**
         * Constructor.
         * 
         * @param action The action key.
         * @param replace The replacement values.
         */
        public ShowMonarchPanelSwingTask(MonarchAction action, String... replace) {
            _action = action;
            _replace = replace;
        }

        /**
         * Show dialog and wait for selection.
         * 
         * @return true if OK, false if Cancel.
         */
        public boolean confirm() {
            try {
                Object result = invokeAndWait();
                return ((Boolean) result).booleanValue();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            boolean choice = canvas.showFreeColDialog(new MonarchPanel(canvas, _action, _replace));
            return Boolean.valueOf(choice);
        }


        private MonarchAction _action;

        private String[] _replace;
    }
}
