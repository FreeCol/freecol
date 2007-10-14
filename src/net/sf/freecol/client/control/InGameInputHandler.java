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

package net.sf.freecol.client.control;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import net.sf.freecol.FreeCol;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Handles the network messages that arrives while in the getGame().
 */
public final class InGameInputHandler extends InputHandler {
    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());





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
            } else if (type.equals("opponentMove")) {
                reply = opponentMove(element);
            } else if (type.equals("opponentAttack")) {
                reply = opponentAttack(element);
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
            } else if (type.equals("deliverGift")) {
                reply = deliverGift(element);
            } else if (type.equals("indianDemand")) {
                reply = indianDemand(element);
            } else if (type.equals("reconnect")) {
                reply = reconnect(element);
            } else if (type.equals("setAI")) {
                reply = setAI(element);
            } else if (type.equals("monarchAction")) {
                reply = monarchAction(element);
            } else if (type.equals("removeGoods")) {
                reply = removeGoods(element);
            } else if (type.equals("lostCityRumour")) {
                reply = lostCityRumour(element);
            } else if (type.equals("setStance")) {
                reply = setStance(element);
            } else if (type.equals("giveIndependence")) {
                reply = giveIndependence(element);
            } else if (type.equals("newConvert")) {
                reply = newConvert(element);
            } else if (type.equals("diplomaticTrade")) {
                reply = diplomaticTrade(element);
            } else {
                logger.warning("Message is of unsupported type \"" + type + "\".");
            }

            logger.log(Level.FINEST, "Handled message " + type);
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
        if (new ShowConfirmDialogSwingTask("reconnect.text", "reconnect.yes", "reconnect.no").confirm()) {
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
        NodeList nodeList = updateElement.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            FreeColGameObject fcgo = getGame().getFreeColGameObjectSafely(element.getAttribute("ID"));
            if (fcgo != null) {
                fcgo.readFromXMLElement(element);
            } else {
                logger.warning("Could not find 'FreeColGameObject' with ID: " + element.getAttribute("ID"));
            }
        }
        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }

    /**
     * Handles a "remove"-message.
     * 
     * @param removeElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     */
    private Element remove(Element removeElement) {

        NodeList nodeList = removeElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            FreeColGameObject fcgo = getGame().getFreeColGameObject(element.getAttribute("ID"));

            if (fcgo != null) {
                fcgo.dispose();
            } else {
                logger.warning("Could not find 'FreeColGameObject' with ID: " + element.getAttribute("ID"));
            }
        }

        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }

    /**
     * Handles an "opponentMove"-message.
     * 
     * @param opponentMoveElement The element (root element in a DOM-parsed XML
     *            tree) that holds all the information.
     */
    private Element opponentMove(Element opponentMoveElement) {
        Map map = getGame().getMap();

        int direction = Integer.parseInt(opponentMoveElement.getAttribute("direction"));

        if (!opponentMoveElement.hasAttribute("tile")) {
            final Unit unit = (Unit) getGame().getFreeColGameObjectSafely(opponentMoveElement.getAttribute("unit"));

            if (unit == null) {
                logger.warning("Could not find the 'unit' in 'opponentMove'. Unit ID: "
                        + opponentMoveElement.getAttribute("unit"));
                return null;
            }
            if (unit.getTile() == null) {
                logger.warning("Ignoring opponentMove, unit " + unit.getID() + " has no tile!");
                return null;
            }

            final Tile newTile = map.getNeighbourOrNull(direction, unit.getTile());
            if (getFreeColClient().getMyPlayer().canSee(newTile)) {
                final Tile oldTile = unit.getTile();
                unit.moveToTile(newTile);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getFreeColClient().getCanvas().refreshTile(oldTile);
                        getFreeColClient().getCanvas().refreshTile(newTile);
                        getFreeColClient().getGUI().setFocus(newTile.getPosition());
                    }
                });
            } else {
                final Tile oldTile = unit.getTile();
                unit.dispose();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getFreeColClient().getCanvas().refreshTile(oldTile);
                    }
                });
            }
        } else {
            String tileID = opponentMoveElement.getAttribute("tile");

            Element unitElement = Message.getChildElement(opponentMoveElement, Unit.getXMLElementTagName());
            if (unitElement == null) {
                logger.warning("unitElement == null");
                throw new NullPointerException("unitElement == null");
            }
            Unit u = (Unit) getGame().getFreeColGameObjectSafely(unitElement.getAttribute("ID"));
            if (u == null) {
                u = new Unit(getGame(), unitElement);
            } else {
                u.readFromXMLElement(unitElement);
            }
            final Unit unit = u;

            if (opponentMoveElement.hasAttribute("inUnit")) {
                String inUnitID = opponentMoveElement.getAttribute("inUnit");
                Unit inUnit = (Unit) getGame().getFreeColGameObjectSafely(inUnitID);

                NodeList units = opponentMoveElement.getElementsByTagName(Unit.getXMLElementTagName());
                Element locationElement = null;
                for (int i = 0; i < units.getLength() && locationElement == null; i++) {
                    Element element = (Element) units.item(i);
                    if (element.getAttribute("ID").equals(inUnitID))
                        locationElement = element;
                }
                if (locationElement != null) {
                    if (inUnit == null) {
                        inUnit = new Unit(getGame(), locationElement);
                    } else {
                        inUnit.readFromXMLElement(locationElement);
                    }
                }
            }

            if (getGame().getFreeColGameObject(tileID) == null) {
                logger.warning("Could not find tile with id: " + tileID);
            }
            unit.setLocation((Tile) getGame().getFreeColGameObject(tileID));

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getFreeColClient().getCanvas().refreshTile(unit.getTile());
                    getFreeColClient().getGUI().setFocus(unit.getTile().getPosition());
                }
            });
        }

        return null;
    }

    /**
     * Handles an "opponentAttack"-message.
     * 
     * @param opponentAttackElement The element (root element in a DOM-parsed
     *            XML tree) that holds all the information.
     */
    private Element opponentAttack(Element opponentAttackElement) {
        Unit unit = (Unit) getGame().getFreeColGameObject(opponentAttackElement.getAttribute("unit"));
        Colony colony = (Colony) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("colony"));
        Unit defender = (Unit) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("defender"));

        int result = Integer.parseInt(opponentAttackElement.getAttribute("result"));
        int plunderGold = Integer.parseInt(opponentAttackElement.getAttribute("plunderGold"));

        if (opponentAttackElement.hasAttribute("update")) {
            String updateAttribute = opponentAttackElement.getAttribute("update");
            if (updateAttribute.equals("unit")) {
                Element unitElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                if (unitElement == null) {
                    logger.warning("unitElement == null");
                    throw new NullPointerException("unitElement == null");
                }
                unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                if (unit == null) {
                    unit = new Unit(getGame(), unitElement);
                } else {
                    unit.readFromXMLElement(unitElement);
                }
                unit.setLocation(unit.getTile());
                if (unit.getTile() == null) {
                    logger.warning("unit.getTile() == null");
                    throw new NullPointerException("unit.getTile() == null");
                }
            } else if (updateAttribute.equals("defender")) {
                Element defenderTileElement = Message.getChildElement(opponentAttackElement, Tile
                        .getXMLElementTagName());
                if (defenderTileElement != null) {
                    Tile defenderTile = (Tile) getGame().getFreeColGameObject(defenderTileElement.getAttribute("ID"));
                    defenderTile.readFromXMLElement(defenderTileElement);
                }

                Element defenderElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                if (defenderElement == null) {
                    logger.warning("defenderElement == null");
                    throw new NullPointerException("defenderElement == null");
                }
                defender = (Unit) getGame().getFreeColGameObject(defenderElement.getAttribute("ID"));
                if (defender == null) {
                    defender = new Unit(getGame(), defenderElement);
                } else {
                    defender.readFromXMLElement(defenderElement);
                }
                defender.setLocation(defender.getTile());

                if (defender.getTile() == null) {
                    logger.warning("defender.getTile() == null");
                    throw new NullPointerException();
                }
            } else if (updateAttribute.equals("tile")) {
                Element tileElement = Message.getChildElement(opponentAttackElement, Tile
                        .getXMLElementTagName());
                Tile tile = (Tile) getGame().getFreeColGameObject(tileElement.getAttribute("ID"));
                if (tile == null) {
                    tile = new Tile(getGame(), tileElement);
                } else {
                    tile.readFromXMLElement(tileElement);
                }
                colony = tile.getColony();
            } else {
                logger.warning("Unknown update: " + updateAttribute);
                throw new IllegalStateException("Unknown update " + updateAttribute);
            }
        }

        if (unit == null && colony == null) {
            logger.warning("unit == null && colony == null");
            throw new NullPointerException("unit == null && colony == null");
        }

        if (defender == null) {
            logger.warning("defender == null");
            throw new NullPointerException("defender == null");
        }

        if (colony != null) {
            colony.bombard(defender, result);
        } else {
            unit.attack(defender, result, plunderGold);
            if (!unit.isDisposed() && (unit.getLocation() == null || !unit.isVisibleTo(getFreeColClient().getMyPlayer()))) {
                unit.dispose();
            }
        }

        if (!defender.isDisposed()
                && (defender.getLocation() == null || !defender.isVisibleTo(getFreeColClient().getMyPlayer()))) {
            if (result == Unit.ATTACK_DONE_SETTLEMENT && defender.getColony() != null
                    && !defender.getColony().isDisposed()) {
                defender.getColony().setUnitCount(defender.getColony().getUnitCount());
            }
            defender.dispose();
        }

        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }

    /**
     * Handles a "setCurrentPlayer"-message.
     * 
     * @param setCurrentPlayerElement The element (root element in a DOM-parsed
     *            XML tree) that holds all the information.
     */
    private Element setCurrentPlayer(Element setCurrentPlayerElement) {

        final Player currentPlayer = (Player) getGame().getFreeColGameObject(setCurrentPlayerElement.getAttribute("player"));

        logger.finest("About to set currentPlayer to " + currentPlayer.getName());
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    getFreeColClient().getInGameController().setCurrentPlayer(currentPlayer);
                    getFreeColClient().getActionManager().update();
                }
            });
        } catch (InterruptedException e) {
            // Ignore
        } catch (InvocationTargetException e) {
            // Ignore
        }
        logger.finest("Succeeded in setting currentPlayer to " + currentPlayer.getName());

        new RefreshCanvasSwingTask(true).invokeLater();
        return null;
    }

    /**
     * Handles a "newTurn"-message.
     * 
     * @param newTurnElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     */
    private Element newTurn(Element newTurnElement) {
        getGame().newTurn();
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
        player.setDead(true);

        if (player == freeColClient.getMyPlayer()) {
            if (freeColClient.isSingleplayer()) {
                if (!new ShowConfirmDialogSwingTask("defeatedSingleplayer.text", "defeatedSingleplayer.yes",
                        "defeatedSingleplayer.no").confirm()) {
                    freeColClient.quit();
                } else {
                    freeColClient.getFreeColServer().enterRevengeMode(player.getUsername());
                }
            } else {
                if (!new ShowConfirmDialogSwingTask("defeated.text", "defeated.yes", "defeated.no").confirm()) {
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
        final Player sender = (Player) getGame().getFreeColGameObjectSafely(element.getAttribute("sender"));
        final String message = element.getAttribute("message");
        final boolean privateChat = Boolean.valueOf(element.getAttribute("privateChat")).booleanValue();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getFreeColClient().getCanvas().displayChatMessage(sender, message, privateChat);
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
        new ShowErrorMessageSwingTask(element.hasAttribute("messageID") ? element.getAttribute("messageID") : null,
                element.getAttribute("message")).show();
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
        final FoundingFather[] possibleFoundingFathers = new FoundingFather[FoundingFather.TYPE_COUNT];
        for (int i = 0; i < FoundingFather.TYPE_COUNT; i++) {
            String id = element.getAttribute("foundingFather" + Integer.toString(i));
            if ("".equals(id)) {
                possibleFoundingFathers[i] = null;
            } else {
                possibleFoundingFathers[i] = FreeCol.getSpecification().getFoundingFather(id);
            }
        }

        int selected = new ShowSelectFoundingFatherSwingTask(possibleFoundingFathers).select();
        FoundingFather foundingFather = possibleFoundingFathers[selected];

        Element reply = Message.createNewRootElement("chosenFoundingFather");
        reply.setAttribute("foundingFather", foundingFather.getID());
        getFreeColClient().getMyPlayer().setCurrentFather(foundingFather);
        return reply;
    }

    /**
     * Handles a "newConvert"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML
     * tree) that holds all the information.
     */
    private Element newConvert(Element element) {
        Tile tile = (Tile) getGame().getFreeColGameObject(element.getAttribute("colony"));
        Colony colony = tile.getColony();
        String nation = colony.getOwner().getNationAsString();
        ModelMessage message = new ModelMessage(colony,
                                                "model.colony.newConvert",
                                                new String[][] {
                                                    {"%nation%", nation},
                                                    {"%colony%", colony.getName()}},
                                                ModelMessage.UNIT_ADDED);
        getFreeColClient().getMyPlayer().addModelMessage(message);
        return null;
    }


    /**
     * Handles a "diplomaticTrade"-request. Returns either an "accept"
     * element, if the offer was accepted, or "null", if it was not.
     * 
     * @param element The element (root element in a DOM-parsed XML
     * tree) that holds all the information.
     */
    private Element diplomaticTrade(Element element) {
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                                               + element.getAttribute("unit"));
        }
        int direction = Integer.parseInt(element.getAttribute("direction"));
        Tile tile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
        if (tile == null) {
            throw new IllegalArgumentException("Could not find 'Tile' in direction " +
                                               direction);
        }
        Settlement settlement = tile.getSettlement();
        if (settlement == null) {
            throw new IllegalArgumentException("No settlement on 'Tile' " +
                                               tile.getID());
        }
        
        NodeList childElements = element.getChildNodes();
        Element childElement = (Element) childElements.item(0);
        DiplomaticTrade proposal = new DiplomaticTrade(getGame(), childElement);
        
        if (proposal.isAccept()) {
            new ShowInformationMessageSwingTask("negotiationDialog.offerAccepted",
                    new String[][] {{"%nation%", unit.getOwner().getNationAsString()}}).show();
            proposal.makeTrade();
        } else {
            DiplomaticTrade agreement = new ShowNegotiationDialogSwingTask(unit, settlement, proposal).select();
            if (agreement != null) {
                Element diplomaticElement = Message.createNewRootElement("diplomaticTrade");
                if (agreement.isAccept()) {
                    diplomaticElement.setAttribute("accept", "accept");
                } else {
                    diplomaticElement.setAttribute("unit", unit.getID());
                    diplomaticElement.setAttribute("direction", String.valueOf(direction));
                    diplomaticElement.appendChild(agreement.toXMLElement(null, diplomaticElement.getOwnerDocument()));
                }
                return diplomaticElement;
            }
        }
        return null;
    }


    /**
     * Handles an "deliverGift"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element deliverGift(Element element) {
        Element unitElement = Message.getChildElement(element, Unit.getXMLElementTagName());

        Unit unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
        unit.readFromXMLElement(unitElement);

        Settlement settlement = (Settlement) getGame().getFreeColGameObject(element.getAttribute("settlement"));
        Goods goods = new Goods(getGame(), Message.getChildElement(element, Goods.getXMLElementTagName()));

        unit.deliverGift(settlement, goods);

        return null;
    }

    /**
     * Handles an "indianDemand"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element indianDemand(Element element) {
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Colony colony = (Colony) getGame().getFreeColGameObject(element.getAttribute("colony"));
        int gold = 0;
        Goods goods = null;
        boolean accepted;

        Element unitElement = Message.getChildElement(element, Unit.getXMLElementTagName());
        if (unitElement != null) {
            if (unit == null) {
                unit = new Unit(getGame(), unitElement);
            } else {
                unit.readFromXMLElement(unitElement);
            }
        }

        Element goodsElement = Message.getChildElement(element, Goods.getXMLElementTagName());
        if (goodsElement == null) {
            gold = Integer.parseInt(element.getAttribute("gold"));
            accepted = new ShowConfirmDialogSwingTask("indianDemand.gold.text", "indianDemand.gold.yes",
                    "indianDemand.gold.no", new String[][] { { "%nation%", unit.getOwner().getNationAsString() },
                            { "%colony%", colony.getName() }, { "%amount%", String.valueOf(gold) } }).confirm();
            if (accepted) {
                colony.getOwner().modifyGold(-gold);
            }
        } else {
            goods = new Goods(getGame(), goodsElement);

            if (goods.getType() == Goods.FOOD) {
                accepted = new ShowConfirmDialogSwingTask("indianDemand.food.text", "indianDemand.food.yes",
                        "indianDemand.food.no", new String[][] { { "%nation%", unit.getOwner().getNationAsString() },
                                { "%colony%", colony.getName() } }).confirm();
            } else {
                accepted = new ShowConfirmDialogSwingTask("indianDemand.other.text", "indianDemand.other.yes",
                        "indianDemand.other.no", new String[][] { { "%nation%", unit.getOwner().getNationAsString() },
                                { "%colony%", colony.getName() }, { "%amount%", String.valueOf(goods.getAmount()) },
                                { "%goods%", goods.getName() } }).confirm();
            }

            if (accepted) {
                colony.getGoodsContainer().removeGoods(goods);
            }
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
        Player player = freeColClient.getMyPlayer();
        Monarch monarch = player.getMonarch();
        final int action = Integer.parseInt(element.getAttribute("action"));
        Element reply;

        switch (action) {
        case Monarch.RAISE_TAX:
            boolean force = Boolean.parseBoolean(element.getAttribute("force"));
            final int amount = new Integer(element.getAttribute("amount")).intValue();
            if (force) {
                freeColClient.getMyPlayer().setTax(amount);
                player.addModelMessage(new ModelMessage(player, "model.monarch.forceTaxRaise",
                                                        new String[][] {
                                                            {"%replace%", String.valueOf(amount) }},
                                                        ModelMessage.WARNING));
                reply = null;
            } else {
                reply = Message.createNewRootElement("acceptTax");
                String[][] replace = new String[][] {
                    { "%replace%", element.getAttribute("amount") },
                    { "%goods%", element.getAttribute("goods") }, };
                if (new ShowMonarchPanelSwingTask(action, replace).confirm()) {
                    freeColClient.getMyPlayer().setTax(amount);
                    reply.setAttribute("accepted", String.valueOf(true));
                    new UpdateMenuBarSwingTask().invokeLater();
                } else {
                    reply.setAttribute("accepted", String.valueOf(false));
                }
            }
            return reply;
        case Monarch.ADD_TO_REF:
            Element arrayElement = Message.getChildElement(element, "addition");
            int[] units = new int[Integer.parseInt(arrayElement.getAttribute("xLength"))];
            for (int x = 0; x < units.length; x++) {
                units[x] = Integer.parseInt(arrayElement.getAttribute("x" + Integer.toString(x)));
            }
            monarch.addToREF(units);
            player.addModelMessage(new ModelMessage(player, "model.monarch.addToREF",
                                                    new String[][] {
                                                        { "%addition%", monarch.getName(units) }},
                                                    ModelMessage.WARNING));
            break;
        case Monarch.DECLARE_WAR:
            Player enemy = (Player) getGame().getFreeColGameObject(element.getAttribute("nation"));
            player.setStance(enemy, Player.WAR);
            player.addModelMessage(new ModelMessage(player, "model.monarch.declareWar",
                                                    new String[][] {
                                                        {"%nation%", enemy.getName()}},
                                                    ModelMessage.WARNING));
            break;
        case Monarch.SUPPORT_LAND:
        case Monarch.SUPPORT_SEA:
        case Monarch.ADD_UNITS:
            NodeList unitList = element.getChildNodes();
            for (int i = 0; i < unitList.getLength(); i++) {
                Element unitElement = (Element) unitList.item(i);
                Unit newUnit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                if (newUnit == null) {
                    newUnit = new Unit(getGame(), unitElement);
                } else {
                    newUnit.readFromXMLElement(unitElement);
                }
                player.getEurope().add(newUnit);
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Canvas canvas = getFreeColClient().getCanvas();
                    if (!canvas.isShowingSubPanel()
                            && (action == Monarch.ADD_UNITS || !canvas.showMonarchPanel(action, null))) {
                        canvas.showEuropePanel();
                    }
                }
            });
            break;
        case Monarch.OFFER_MERCENARIES:
            reply = Message.createNewRootElement("hireMercenaries");
            Element mercenaryElement = Message.getChildElement(element, "mercenaries");
            int[] mercenaries = new int[Integer.parseInt(mercenaryElement.getAttribute("xLength"))];
            for (int x = 0; x < mercenaries.length; x++) {
                mercenaries[x] = Integer.parseInt(mercenaryElement.getAttribute("x" + Integer.toString(x)));
            }
            if (new ShowMonarchPanelSwingTask(action, new String[][] { { "%gold%", element.getAttribute("price") },
                    { "%mercenaries%", monarch.getName(mercenaries) } }).confirm()) {
                int price = new Integer(element.getAttribute("price")).intValue();
                freeColClient.getMyPlayer().modifyGold(-price);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        freeColClient.getCanvas().updateGoldLabel();
                    }
                });
                reply.setAttribute("accepted", String.valueOf(true));
            } else {
                reply.setAttribute("accepted", String.valueOf(false));
            }
            return reply;
        }
        return null;
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
        int stance = Integer.parseInt(element.getAttribute("stance"));
        Player first = (Player) getGame().getFreeColGameObject(element.getAttribute("first"));
        Player second = (Player) getGame().getFreeColGameObject(element.getAttribute("second"));

        /*
         * War declared messages are sometimes not shown, because opponentAttack
         * message arrives before and when setStance message arrives the player
         * has the new stance. So not check stance is going to change.
         */
        /*
         * if (first.getStance(second) == stance) { return null; }
         */

        first.setStance(second, stance);

        if (stance == Player.WAR) {
            if (player.equals(second)) {
                player.addModelMessage(new ModelMessage(first, "model.diplomacy.war.declared",
                                                        new String[][] {
                                                            {"%nation%", first.getNationAsString()}},
                                                        ModelMessage.FOREIGN_DIPLOMACY));
            } else {
                player.addModelMessage(new ModelMessage(first, "model.diplomacy.war.others",
                                                        new String[][] {
                                                            { "%attacker%", first.getNationAsString() },
                                                            { "%defender%", second.getNationAsString() } },
                                                        ModelMessage.FOREIGN_DIPLOMACY));
            }
        }

        return null;
    }

    /**
     * Handles a "giveIndependence"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element giveIndependence(Element element) {
        Player player = (Player) getGame().getFreeColGameObject(element.getAttribute("player"));
        player.giveIndependence();
        return null;
    }

    /**
     * Handles a "removeGoods"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element removeGoods(Element element) {
        final FreeColClient freeColClient = getFreeColClient();

        NodeList nodeList = element.getChildNodes();
        Element goodsElement = (Element) nodeList.item(0);

        if (goodsElement == null) {
            // player has no colony or nothing to trade
            new ShowMonarchPanelSwingTask(Monarch.WAIVE_TAX, null).confirm();
        } else {
            final Goods goods = new Goods(getGame(), goodsElement);
            final Colony colony = (Colony) goods.getLocation();
            colony.removeGoods(goods);

            // JACOB_FUGGER does not protect against new boycotts
            freeColClient.getMyPlayer().setArrears(goods);

            final String message;
            if (goods.getType() == Goods.HORSES) {
                message = "model.monarch.bostonTeaParty.horses";
            } else if (colony.isLandLocked()) {
                message = "model.monarch.bostonTeaParty.landLocked";
            } else {
                message = "model.monarch.bostonTeaParty.harbour";
            }
            new ShowModelMessageSwingTask(new ModelMessage(colony, message,
                                             new String[][] { { "%colony%", colony.getName() },
                                                 { "%amount%", String.valueOf(goods.getAmount()) },
                                                 { "%goods%", goods.getName() } },
                                             ModelMessage.WARNING)).invokeLater();
        }

        return null;
    }

    /**
     * Handles a "lostCityRumour"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element lostCityRumour(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        final Player player = freeColClient.getMyPlayer();
        int type = Integer.parseInt(element.getAttribute("type"));
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));

        if (unit == null) {
            throw new IllegalArgumentException("Unit is null.");
        }
        Tile tile = unit.getTile();
        tile.setLostCityRumour(false);

        Unit newUnit;
        NodeList unitList;
        ModelMessage m;
        switch (type) {
        case LostCityRumour.BURIAL_GROUND:
            Player indianPlayer = tile.getOwner();
            indianPlayer.modifyTension(player, Tension.TENSION_HATEFUL);
            m = new ModelMessage(tile, "lostCityRumour.BurialGround", new String[][] { { "%nation%",
                    indianPlayer.getNationAsString() } }, ModelMessage.LOST_CITY_RUMOUR);
            break;
        case LostCityRumour.EXPEDITION_VANISHES:
            m = new ModelMessage(tile, "lostCityRumour.ExpeditionVanishes", null, ModelMessage.LOST_CITY_RUMOUR);
            unit.dispose();
            break;
        case LostCityRumour.NOTHING:
            m = new ModelMessage(tile, "lostCityRumour.Nothing", null, ModelMessage.LOST_CITY_RUMOUR);
            break;
        case LostCityRumour.LEARN:
            m = new ModelMessage(tile, "lostCityRumour.SeasonedScout", new String[][] { { "%unit%", unit.getName() } },
                    ModelMessage.LOST_CITY_RUMOUR);
            unit.setType(FreeCol.getSpecification().getUnitType(element.getAttribute("unitType")));
            break;
        case LostCityRumour.TRIBAL_CHIEF:
            String amount = element.getAttribute("amount");
            m = new ModelMessage(tile, "lostCityRumour.TribalChief", new String[][] { { "%money%", amount } },
                    ModelMessage.LOST_CITY_RUMOUR);
            player.modifyGold(Integer.parseInt(amount));
            break;
        case LostCityRumour.COLONIST:
            m = new ModelMessage(tile, "lostCityRumour.Colonist", null, ModelMessage.LOST_CITY_RUMOUR);
            unitList = element.getChildNodes();
            for (int i = 0; i < unitList.getLength(); i++) {
                Element unitElement = (Element) unitList.item(i);
                newUnit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                if (newUnit == null) {
                    newUnit = new Unit(getGame(), unitElement);
                } else {
                    newUnit.readFromXMLElement(unitElement);
                }
                tile.add(newUnit);
            }
            break;
        case LostCityRumour.TREASURE:
            String treasure = element.getAttribute("amount");
            m = new ModelMessage(tile, "lostCityRumour.TreasureTrain", new String[][] { { "%money%", treasure } },
                    ModelMessage.LOST_CITY_RUMOUR);
            unitList = element.getChildNodes();
            for (int i = 0; i < unitList.getLength(); i++) {
                Element unitElement = (Element) unitList.item(i);
                newUnit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                if (newUnit == null) {
                    newUnit = new Unit(getGame(), unitElement);
                } else {
                    newUnit.readFromXMLElement(unitElement);
                }
                tile.add(newUnit);
            }
            break;
        case LostCityRumour.FOUNTAIN_OF_YOUTH:
            if (player.getEurope() == null) {
                m = new ModelMessage(player, "lostCityRumour.FountainOfYouthWithoutEurope", null,
                                     ModelMessage.LOST_CITY_RUMOUR);
            } else {
                m = new ModelMessage(player.getEurope(), "lostCityRumour.FountainOfYouth", null,
                                     ModelMessage.LOST_CITY_RUMOUR);
                if (player.hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.williamBrewster"))) {
                    final int emigrants = Integer.parseInt(element.getAttribute("emigrants"));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            for (int i = 0; i < emigrants; i++) {
                                int slot = getFreeColClient().getCanvas().showEmigrationPanel();
                                Element selectElement = Message.createNewRootElement("selectFromFountainYouth");
                                selectElement.setAttribute("slot", Integer.toString(slot));
                                Element reply = freeColClient.getClient().ask(selectElement);

                                Element unitElement = (Element) reply.getChildNodes().item(0);
                                Unit unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                                if (unit == null) {
                                    unit = new Unit(getGame(), unitElement);
                                } else {
                                    unit.readFromXMLElement(unitElement);
                                }
                                player.getEurope().add(unit);

                                String newRecruitableStr = reply.getAttribute("newRecruitable");
                                UnitType newRecruitable = FreeCol.getSpecification().getUnitType(newRecruitableStr);
                                player.getEurope().setRecruitable(slot, newRecruitable);
                            }
                        }
                    });
               } else {
                    unitList = element.getChildNodes();
                    for (int i = 0; i < unitList.getLength(); i++) {
                        Element unitElement = (Element) unitList.item(i);
                        newUnit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                        if (newUnit == null) {
                            newUnit = new Unit(getGame(), unitElement);
                        } else {
                            newUnit.readFromXMLElement(unitElement);
                        }
                        player.getEurope().add(newUnit);
                    }
               }
            }
            break;
        default:
            throw new IllegalStateException("No such rumour.");
        }
        
        player.addModelMessage(m);
        return null;
    }


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
            canvas.updateJMenuBar();
        }
    }

    /**
     * This task shows the victory panel.
     */
    class ShowVictoryPanelSwingTask extends NoResultCanvasSwingTask {
        protected void doWork(Canvas canvas) {
            canvas.showVictoryPanel();
        }
    }

    /**
     * This class shows a dialog and saves the answer (ok/cancel).
     */
    class ShowConfirmDialogSwingTask extends SwingTask {

        /**
         * Constructor.
         * 
         * @param text The key for the question.
         * @param okText The key for the OK button.
         * @param cancelText The key for the Cancel button.
         */
        public ShowConfirmDialogSwingTask(String text, String okText, String cancelText) {
            this(text, okText, cancelText, null);
        }

        /**
         * Constructor.
         * 
         * @param text The key for the question.
         * @param okText The key for the OK button.
         * @param cancelText The key for the Cancel button.
         * @param replace The replacement values.
         */
        public ShowConfirmDialogSwingTask(String text, String okText, String cancelText, String[][] replace) {
            _text = text;
            _okText = okText;
            _cancelText = cancelText;
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
            boolean choice = getFreeColClient().getCanvas().showConfirmDialog(_text, _okText, _cancelText, _replace);
            return Boolean.valueOf(choice);
        }


        private String _text;

        private String _okText;

        private String _cancelText;

        private String[][] _replace;
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
            getFreeColClient().getCanvas().showModelMessage(_modelMessage);
            return null;
        }


        private ModelMessage _modelMessage;
    }

    /**
     * This class shows an informational dialog.
     */
    class ShowInformationMessageSwingTask extends ShowMessageSwingTask {

        /**
         * Constructor.
         * 
         * @param messageId The key for the message.
         * @param replace The values to replace text with.
         */
        public ShowInformationMessageSwingTask(String messageId, String[][] replace) {
            _messageId = messageId;
            _replace = replace;
        }

        protected Object doWork() {
            getFreeColClient().getCanvas().showInformationMessage(_messageId, _replace);
            return null;
        }


        private String _messageId;

        private String[][] _replace;
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
    class ShowSelectFoundingFatherSwingTask extends ShowSelectSwingTask {

        /**
         * Constructor.
         * 
         * @param choices The possible founding fathers.
         */
        public ShowSelectFoundingFatherSwingTask(FoundingFather[] choices) {
            this.choices = choices;
        }

        protected Object doWork() {
            return Integer.valueOf(getFreeColClient().getCanvas().showChooseFoundingFatherDialog(choices));
        }


        private FoundingFather[] choices;
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
         * @param unit The proposal made by unit's owner.
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
        public ShowMonarchPanelSwingTask(int action, String[][] replace) {
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
            boolean choice = getFreeColClient().getCanvas().showMonarchPanel(_action, _replace);
            return Boolean.valueOf(choice);
        }


        private int _action;

        private String[][] _replace;
    }
}
