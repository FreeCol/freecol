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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.panel.MonarchPanel;
import net.sf.freecol.client.gui.panel.VictoryPanel;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.util.Utils;

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
            } else if (type.equals("diplomacy")) {
                reply = diplomacy(element);
            } else if (type.equals("marketElement")) {
                reply = marketElement(element);
            } else if (type.equals("addPlayer")) {
                reply = addPlayer(element);
            } else if (type.equals("spanishSuccession")) {
                reply = spanishSuccession(element);
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
        
        updateGameObjects(updateElement.getChildNodes());
        
        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }
    
    /**
     * Updates all FreeColGameObjects from the childNodes of the message
     * @param nodeList The list of nodes from the message
     */
    private void updateGameObjects(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            FreeColGameObject fcgo = getGame().getFreeColGameObjectSafely(element.getAttribute("ID"));
            if (fcgo != null) {
                fcgo.readFromXMLElement(element);
            } else {
                logger.warning("Could not find 'FreeColGameObject' with ID: " + element.getAttribute("ID"));
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

        Direction direction = Enum.valueOf(Direction.class, opponentMoveElement.getAttribute("direction"));

        if (opponentMoveElement.hasAttribute("fromTile")) {
            // The unit moving should be already visible
            final Unit unit = (Unit) getGame().getFreeColGameObjectSafely(opponentMoveElement.getAttribute("unit"));
            if (unit == null) {
                /*
                logger.warning("Could not find the 'unit' in 'opponentMove'. Unit ID: "
                        + opponentMoveElement.getAttribute("unit"));
                return null;
                */
                throw new IllegalStateException("Could not find the 'unit' in 'opponentMove'. Unit ID: "
                                                + opponentMoveElement.getAttribute("unit"));
            }
            
            final Tile fromTile = (Tile) getGame().getFreeColGameObjectSafely(opponentMoveElement.getAttribute("fromTile"));
            if (fromTile == null) {
                /*
                logger.warning("Ignoring opponentMove, unit " + unit.getId() + " has no tile!");
                return null;
                */
                throw new IllegalStateException("Ignoring opponentMove, unit " + unit.getId()
                                                + " has no tile!");
            }

            final Tile toTile = map.getNeighbourOrNull(direction, fromTile);
            if (toTile==null) {
                // logger.warning("Destination tile is null!");
                // TODO: find out why this can happen
                throw new IllegalStateException("Destination tile is null!");
            } else {
                final String key = (getFreeColClient().getMyPlayer() == unit.getOwner()) ?
                        ClientOptions.MOVE_ANIMATION_SPEED
                        : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
                if (getFreeColClient().getClientOptions().getInteger(key) > 0) {
                    //Playing the animation before actually moving the unit
                    try {
                        new UnitMoveAnimationCanvasSwingTask(unit, toTile).invokeAndWait();
                    } catch (InvocationTargetException exception) {
                        logger.warning("UnitMoveAnimationCanvasSwingTask raised " + exception.toString());
                    }
                } else {
                    // Just refresh both Tiles
                    new RefreshTilesSwingTask(unit.getTile(), toTile).invokeLater();
                }
            }
            if (getFreeColClient().getMyPlayer().canSee(toTile)) {
                unit.setLocation(toTile);
            } else {
                unit.dispose();
            }
        } else {
            // the unit reveals itself, after leaving a settlement or carrier
            String tileID = opponentMoveElement.getAttribute("toTile");

            Element unitElement = Message.getChildElement(opponentMoveElement, Unit.getXMLElementTagName());
            if (unitElement == null) {
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
                /*
                logger.warning("Could not find tile with id: " + tileID);
                unit.setLocation(null);
                // Can't go on without the tile
                return null;
                */
                throw new IllegalStateException("Could not find tile with id: " + tileID);
            }
            
            final Tile newTile = (Tile) getGame().getFreeColGameObject(tileID);
            
            if (unit.getLocation() == null) {
                // Getting the previous tile so we can animate the movement properly
                final Tile oldTile = map.getNeighbourOrNull(direction.getReverseDirection(), newTile);
                unit.setLocationNoUpdate(oldTile); 
            }
            
            final String key = (getFreeColClient().getMyPlayer() == unit.getOwner()) ?
                    ClientOptions.MOVE_ANIMATION_SPEED
                    : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
            if (getFreeColClient().getClientOptions().getInteger(key) > 0) {
                //Playing the animation before actually moving the unit
                try {
                    new UnitMoveAnimationCanvasSwingTask(unit, newTile).invokeAndWait();
                } catch (InvocationTargetException exception) {
                    logger.warning("UnitMoveAnimationCanvasSwingTask raised " + exception.toString());
                }
            } else {
                // Just refresh both Tiles
                new RefreshTilesSwingTask(unit.getTile(), newTile).invokeLater();
            }

            unit.setLocation(newTile);

        }

        return null;
    }

    /**
     * Handles an "opponentAttack"-message.
     * 
     * @param opponentAttackElement The element (root element in a DOM-parsed
     *            XML tree) that holds all the information.
     */
    private Element opponentAttack(final Element opponentAttackElement) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Unit unit = (Unit) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("unit"));
                    Colony colony = (Colony) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("colony"));
                    Unit defender = (Unit) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("defender"));

                    CombatResultType result = Enum.valueOf(CombatResultType.class, opponentAttackElement.getAttribute("result"));
                    int damage = Integer.parseInt(opponentAttackElement.getAttribute("damage"));
                    int plunderGold = Integer.parseInt(opponentAttackElement.getAttribute("plunderGold"));
                    Location repairLocation = (Location) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("repairIn"));

                    if (opponentAttackElement.hasAttribute("update")) {
                        String updateAttribute = opponentAttackElement.getAttribute("update");
                        if (updateAttribute.equals("unit")) {
                            Element unitElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                            unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                            if (unit == null) {
                                unit = new Unit(getGame(), unitElement);
                            } else {
                                unit.readFromXMLElement(unitElement);
                            }
                            if (unit.getTile() == null) {
                                throw new NullPointerException("unit.getTile() == null");
                            }
                            unit.setLocation(unit.getTile());
                        } else if (updateAttribute.equals("defender")) {
                            final Tile defenderTile = (Tile) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("defenderTile"));
                            final Element defenderTileElement = Message.getChildElement(opponentAttackElement, Tile
                                    .getXMLElementTagName());
                            if (defenderTileElement != null) {
                                final Tile checkTile = (Tile) getGame().getFreeColGameObject(defenderTileElement.getAttribute("ID"));
                                if (checkTile != defenderTile) {
                                    throw new IllegalStateException("Trying to update another tile than the defending unit's tile.");
                                }
                                defenderTile.readFromXMLElement(defenderTileElement);
                            }
                            Element defenderElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                            defender = (Unit) getGame().getFreeColGameObject(defenderElement.getAttribute("ID"));
                            if (defender == null) {
                                defender = new Unit(getGame(), defenderElement);
                            } else {
                                defender.readFromXMLElement(defenderElement);
                            }
                            defender.setLocationNoUpdate(defenderTile);
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
                            throw new IllegalStateException("Unknown update " + updateAttribute);
                        }
                    }

                    if (unit == null && colony == null) {
                        throw new NullPointerException("unit == null && colony == null");
                    }

                    if (defender == null) {
                        throw new NullPointerException("defender == null");
                    }
                    
                    Animations.unitAttack(getFreeColClient().getCanvas(), unit, defender, result);

                    if (colony != null) {
                        getGame().getCombatModel().bombard(colony, defender, new CombatResult(result, damage), repairLocation);
                    } else {            
                        unit.getGame().getCombatModel().attack(unit, defender, new CombatResult(result, damage), plunderGold, repairLocation);
                        if (!unit.isDisposed() &&
                                (unit.getLocation() == null ||
                                        !unit.isVisibleTo(getFreeColClient().getMyPlayer()))) {
                            unit.dispose();
                        }
                    }

                    if (!defender.isDisposed()
                            && (defender.getLocation() == null || !defender.isVisibleTo(getFreeColClient().getMyPlayer()))) {
                        if (result == CombatResultType.DONE_SETTLEMENT && defender.getColony() != null
                                && !defender.getColony().isDisposed()) {
                            defender.getColony().setUnitCount(defender.getColony().getUnitCount());
                        }
                        defender.dispose();
                    }
                }
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while handling opponentAttack message.", e);
        }
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
        //getGame().newTurn();
        getGame().getTurn().increase();
        getFreeColClient().getMyPlayer().newTurn();
        new UpdateMenuBarSwingTask().invokeLater();
        
        // Show info message for change to two turns per year
        Turn currTurn = getGame().getTurn(); 
        if(currTurn.getYear() == 1600 &&
        		Turn.getYear(currTurn.getNumber()-1) == 1599 ){
        	new ShowInformationMessageSwingTask("twoTurnsPerYear").invokeLater(); 
        }
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
                    freeColClient.getFreeColServer().enterRevengeMode(player.getName());
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
        final List<FoundingFather> possibleFoundingFathers = new ArrayList<FoundingFather>();
        for (FoundingFatherType type : FoundingFatherType.values()) {
            String id = element.getAttribute(type.toString());
            if (id != null && !id.equals("")) {
                possibleFoundingFathers.add(FreeCol.getSpecification().getFoundingFather(id));
            }
        }

        FoundingFather foundingFather = new ShowSelectFoundingFatherSwingTask(possibleFoundingFathers).select();

        Element reply = Message.createNewRootElement("chosenFoundingFather");
        reply.setAttribute("foundingFather", foundingFather.getId());
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
        Tile tile = (Tile) getGame().getFreeColGameObject(element.getAttribute("colonyTile"));
        Colony colony = tile.getColony();
        String nation = Specification.getSpecification().getNation(element.getAttribute("nation")).getName();
        
        Element unitElement = (Element) element.getFirstChild();
        Unit convert = new Unit(getGame(), unitElement);
        tile.add(convert);
        
        ModelMessage message = new ModelMessage(convert,
                                                ModelMessage.MessageType.UNIT_ADDED,
                                                convert,
                                                "model.colony.newConvert",
                                                "%nation%", nation,
                                                "%colony%", colony.getName());

        getFreeColClient().getMyPlayer().addModelMessage(message);
        return null;
    }

    /**
     * Handles a "diplomacy"-request.  If the message informs of an
     * acceptance or rejection then display the result and return
     * null.  If the message is a proposal, then ask the user about
     * it and return the response with appropriate response set.
     *
     * @param element The element (root element in a DOM-parsed XML tree)
     *        containing a "diplomacy"-message.
     * @return A diplomacy response, or null if none required.
     */
    private Element diplomacy(Element element) {
        Player player = getFreeColClient().getMyPlayer();
        DiplomacyMessage message = new DiplomacyMessage(getGame(), element);
        DiplomaticTrade agreement;

        if (message.isReject()) {
            String nation = message.getOtherNationName(player);
            new ShowInformationMessageSwingTask("negotiationDialog.offerRejected",
                                                "%nation%", nation).show();
            return null;
        }
        if (message.isAccept()) {
            String nation = message.getOtherNationName(player);
            new ShowInformationMessageSwingTask("negotiationDialog.offerAccepted",
                                                "%nation%", nation).show();
            return null;
        }
        agreement = new ShowNegotiationDialogSwingTask(message.getUnit(),
                                                       message.getSettlement(),
                                                       message.getAgreement()).select();
        if (agreement == null) {
            message.setReject();
        } else {
            message.setAgreement(agreement);
            if (agreement.isAccept()) message.setAccept();
        }
        return message.toXMLElement();
    }

    /**
     * Handles an "deliverGift"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element deliverGift(Element element) {
        Element unitElement = Message.getChildElement(element, Unit.getXMLElementTagName());
        Unit unit = null;

        if (unitElement != null) {
            unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit == null) {
                unit = new Unit(getGame(), unitElement);
            } else {
                unit.readFromXMLElement(unitElement);
            }
        }
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
                                                      "indianDemand.gold.no",
                                                      "%nation%", unit.getOwner().getNationAsString(),
                                                      "%colony%", colony.getName(),
                                                      "%amount%", String.valueOf(gold)).confirm();
            if (accepted) {
                colony.getOwner().modifyGold(-gold);
            }
        } else {
            goods = new Goods(getGame(), goodsElement);

            if (goods.getType() == Goods.FOOD) {
                accepted = new ShowConfirmDialogSwingTask("indianDemand.food.text", "indianDemand.food.yes",
                                                          "indianDemand.food.no",
                                                          "%nation%", unit.getOwner().getNationAsString(),
                                                          "%colony%", colony.getName(),
                                                          "%amount%", String.valueOf(goods.getAmount())).confirm();
            } else {
                accepted = new ShowConfirmDialogSwingTask("indianDemand.other.text", "indianDemand.other.yes",
                                                          "indianDemand.other.no",
                                                          "%nation%", unit.getOwner().getNationAsString(),
                                                          "%colony%", colony.getName(),
                                                          "%amount%", String.valueOf(goods.getAmount()),
                                                          "%goods%", goods.getName()).confirm();
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
        final MonarchAction action = Enum.valueOf(MonarchAction.class, element.getAttribute("action"));
        Element reply;

        switch (action) {
        case RAISE_TAX:
            boolean force = Boolean.parseBoolean(element.getAttribute("force"));
            final int amount = new Integer(element.getAttribute("amount")).intValue();
            if (force) {
                freeColClient.getMyPlayer().setTax(amount);
                player.addModelMessage(new ModelMessage(player, "model.monarch.forceTaxRaise",
                                                        new String[][] {
                                                            {"%replace%", String.valueOf(amount) }},
                                                        ModelMessage.MessageType.WARNING));
                reply = null;
            } else {
                reply = Message.createNewRootElement("acceptTax");
                if (new ShowMonarchPanelSwingTask(action,
                                                  "%replace%", element.getAttribute("amount"),
                                                  "%goods%", element.getAttribute("goods")).confirm()) {
                    freeColClient.getMyPlayer().setTax(amount);
                    reply.setAttribute("accepted", String.valueOf(true));
                    new UpdateMenuBarSwingTask().invokeLater();
                } else {
                    reply.setAttribute("accepted", String.valueOf(false));
                }
            }
            return reply;
        case LOWER_TAX:
	
            final int newTax = new Integer(element.getAttribute("amount")).intValue();
            final int difference = freeColClient.getMyPlayer().getTax() - newTax;
                    
            freeColClient.getMyPlayer().setTax(newTax);
            player.addModelMessage(new ModelMessage(player, ModelMessage.MessageType.WARNING, null,
                                                    "model.monarch.lowerTax",
                                                    "%difference%",String.valueOf(difference),
                                                    "%newTax%",
                                                    String.valueOf(newTax)));
            break;
        case ADD_TO_REF:
            Element additionElement = Message.getChildElement(element, "addition");
            NodeList childElements = additionElement.getChildNodes();
            ArrayList<AbstractUnit> units = new ArrayList<AbstractUnit>();
            ArrayList<String> unitNames = new ArrayList<String>();
            for (int index = 0; index < childElements.getLength(); index++) {
                AbstractUnit unit = new AbstractUnit();
                unit.readFromXMLElement((Element) childElements.item(index));
                units.add(unit);
                unitNames.add(unit.getNumber() + " " + Unit.getName(unit.getUnitType(), unit.getRole()));
            }
            monarch.addToREF(units);
            player.addModelMessage(new ModelMessage(player, ModelMessage.MessageType.WARNING, null,
                                                    "model.monarch.addToREF",
                                                    "%addition%", Utils.join(" " + Messages.message("and") + " ",
                                                                             unitNames)));
            break;
        case DECLARE_WAR:
            Player enemy = (Player) getGame().getFreeColGameObject(element.getAttribute("enemy"));
            player.changeRelationWithPlayer(enemy, Stance.WAR);
            player.addModelMessage(new ModelMessage(player, "model.monarch.declareWar",
                                                    new String[][] {
                                                        {"%nation%", enemy.getNationAsString()}},
                                                    ModelMessage.MessageType.WARNING));
            break;
        case SUPPORT_LAND:
        case SUPPORT_SEA:
        case ADD_UNITS:
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
                            && (action == MonarchAction.ADD_UNITS ||
                                !canvas.showFreeColDialog(new MonarchPanel(canvas, action)))) {
                            canvas.showEuropePanel();
                        }
                    }
                });
            break;
        case OFFER_MERCENARIES:
            reply = Message.createNewRootElement("hireMercenaries");
            Element mercenaryElement = Message.getChildElement(element, "mercenaries");
            childElements = mercenaryElement.getChildNodes();
            ArrayList<String> mercenaries = new ArrayList<String>();
            for (int index = 0; index < childElements.getLength(); index++) {
                AbstractUnit unit = new AbstractUnit();
                unit.readFromXMLElement((Element) childElements.item(index));
                mercenaries.add(unit.getNumber() + " " + Unit.getName(unit.getUnitType(), unit.getRole()));
            }
            if (new ShowMonarchPanelSwingTask(action,
                                              "%gold%", element.getAttribute("price"),
                                              "%mercenaries%", Utils.join(" " + Messages.message("and") + " ",
                                                                          mercenaries)).confirm()) {
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
        case NO_ACTION:
            // Nothing to do here, obviously.
            break;
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
        Stance stance = Enum.valueOf(Stance.class, element.getAttribute("stance"));
        Player first = (Player) getGame().getFreeColGameObject(element.getAttribute("first"));
        Player second = (Player) getGame().getFreeColGameObject(element.getAttribute("second"));

        /*
         * Diplomacy messages were sometimes not shown because opponentAttack
         * messages arrive before setStance messages, so when the setStance
         * message arrived the player already had the new stance.
         * So, do not filter like this:
         *   if (first.getStance(second) == stance) { return null; }
         */
        first.setStance(second, stance);
        if (second.getStance(first) != stance) second.setStance(first, stance);
        if (player.equals(second)) {
            player.addModelMessage(new ModelMessage(first,
                    "model.diplomacy." + stance.toString().toLowerCase() + ".declared",
                    new String[][] {
                        {"%nation%", first.getNationAsString()}},
                    ModelMessage.MessageType.FOREIGN_DIPLOMACY));
        } else if (stance == Stance.WAR
                   || player.hasAbility("model.ability.betterForeignAffairsReport")
                   || player.hasContacted(first)
                   || player.hasContacted(second)) {
            // Always inform about wars, always inform post-deWitt,
            // generally inform if have met one of the nations involved
            player.addModelMessage(new ModelMessage(first,
                    "model.diplomacy." + stance.toString().toLowerCase() + ".others",
                    new String[][] {
                        {"%attacker%", first.getNationAsString()},
                        {"%defender%", second.getNationAsString()}},
                    ModelMessage.MessageType.FOREIGN_DIPLOMACY));
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
     * Handles an "addPlayer"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
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
     * Handles a "marketElement"-request.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *        holds all the information.
     */
    private Element marketElement(Element element) {
        final Player player = getFreeColClient().getMyPlayer();
        GoodsType type = FreeCol.getSpecification().getGoodsType(element.getAttribute("type"));
        int amount = Integer.parseInt(element.getAttribute("amount"));
        if (amount > 0) {
            player.getMarket().add(type, amount);
        } else {
            player.getMarket().remove(type, -amount);
        }
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
            new ShowMonarchPanelSwingTask(MonarchAction.WAIVE_TAX).confirm();
        } else {
            final Goods goods = new Goods(getGame(), goodsElement);
            final Colony colony = (Colony) goods.getLocation();
            colony.removeGoods(goods);

            // JACOB_FUGGER does not protect against new boycotts
            freeColClient.getMyPlayer().setArrears(goods);

            String messageID = goods.getType().getId() + ".destroyed";
            if (!Messages.containsKey(messageID)) {
                if (colony.isLandLocked()) {
                    messageID = "model.monarch.colonyGoodsParty.landLocked";
                } else {
                    messageID = "model.monarch.colonyGoodsParty.harbour";
                }
            }
            colony.getFeatureContainer().addModifier(Modifier
               .createTeaPartyModifier(getGame().getTurn()));
            new ShowModelMessageSwingTask(new ModelMessage(colony, ModelMessage.MessageType.WARNING,
                                                           null, messageID,    
                                                           "%colony%", colony.getName(),
                                                           "%amount%", String.valueOf(goods.getAmount()),
                                                           "%goods%", goods.getName())).invokeLater();
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
        RumourType type = Enum.valueOf(RumourType.class, element.getAttribute("type"));
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));

        if (unit == null) {
            throw new IllegalArgumentException("Unit is null.");
        }
        Tile tile = unit.getTile();
        tile.removeLostCityRumour();
        
        // center on the explorer
        freeColClient.getGUI().setFocusImmediately(tile.getPosition());

        Unit newUnit = null;
        NodeList unitList;
        ModelMessage m;
        switch (type) {
        case BURIAL_GROUND:
            Player indianPlayer = tile.getOwner();
            indianPlayer.modifyTension(player, Tension.Level.HATEFUL.getLimit());
            m = new ModelMessage(unit, "lostCityRumour.BurialGround", new String[][] { { "%nation%",
                    indianPlayer.getNationAsString() } }, ModelMessage.MessageType.LOST_CITY_RUMOUR);
            break;
        case EXPEDITION_VANISHES:
            m = new ModelMessage(unit, "lostCityRumour.ExpeditionVanishes", null, ModelMessage.MessageType.LOST_CITY_RUMOUR);
            unit.dispose();
            break;
        case NOTHING:
            m = new ModelMessage(unit, "lostCityRumour.Nothing", null, ModelMessage.MessageType.LOST_CITY_RUMOUR);
            break;
        case LEARN:
            m = new ModelMessage(unit, "lostCityRumour.SeasonedScout", new String[][] { { "%unit%", unit.getName() } },
                    ModelMessage.MessageType.LOST_CITY_RUMOUR);
            unit.setType(FreeCol.getSpecification().getUnitType(element.getAttribute("unitType")));
            break;
        case TRIBAL_CHIEF:
            String amount = element.getAttribute("amount");
            m = new ModelMessage(unit, "lostCityRumour.TribalChief", new String[][] { { "%money%", amount } },
                    ModelMessage.MessageType.LOST_CITY_RUMOUR);
            player.modifyGold(Integer.parseInt(amount));
            break;
        case COLONIST:
            m = new ModelMessage(unit, ModelMessage.MessageType.LOST_CITY_RUMOUR, null, "lostCityRumour.Colonist");
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
        case TREASURE:
            String treasure = element.getAttribute("amount");
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
            m = new ModelMessage(unit, ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 newUnit, "lostCityRumour.TreasureTrain",
                                 "%money%", treasure);
            player.getHistory().add(new HistoryEvent(player.getGame().getTurn().getNumber(),
                                                     HistoryEvent.Type.CITY_OF_GOLD,
                                                     "%treasure%", String.valueOf(treasure)));
            break;
        case FOUNTAIN_OF_YOUTH:
            if (player.getEurope() == null) {
                m = new ModelMessage(player, "lostCityRumour.FountainOfYouthWithoutEurope", null,
                                     ModelMessage.MessageType.LOST_CITY_RUMOUR);
            } else {
                freeColClient.playMusicOnce("fountain");
                m = new ModelMessage(player.getEurope(), "lostCityRumour.FountainOfYouth", null,
                                     ModelMessage.MessageType.LOST_CITY_RUMOUR);
                if (player.hasAbility("model.ability.selectRecruit")) {
                    final int emigrants = Integer.parseInt(element.getAttribute("emigrants"));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            for (int i = 0; i < emigrants; i++) {
                                int slot = getFreeColClient().getCanvas().showEmigrationPanel(true);
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
     * Handles a "spanishSuccession" message
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element spanishSuccession(Element element) {
        final Player player = getFreeColClient().getMyPlayer();
        final Player loser = (Player) getGame().getFreeColGameObject(element.getAttribute("loser"));
        final Player winner = (Player) getGame().getFreeColGameObject(element.getAttribute("winner"));
        player.addModelMessage(new ModelMessage(winner, ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                                null, "model.diplomacy.spanishSuccession",
                                                "%loserNation%", loser.getNationAsString(),
                                                "%nation%", winner.getNationAsString()));
        loser.setDead(true);
        update(element);
        return null;
    }

    /**
     *
     *    Handler methods end here.
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
                
        /**
         * Constructor - Play the unit movement animation, focusing the unit
         * @param unit The unit that is moving
         * @param destinationTile The Tile where the unit will be moving to.
         */
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Tile destinationTile) {
            this(unit, destinationTile, true);
        }
        
        /**
         * Constructor - Play the unit movement animation, focusing the unit
         * @param unit The unit that is moving
         * @param direction The Direction in which the Unit will be moving.
         */
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Direction direction) {
            this(unit, unit.getGame().getMap().getNeighbourOrNull(direction, unit.getTile()), true);
        }
        
        /**
         * Constructor
         * @param unit The unit that is moving
         * @param destinationTile The Tile where the unit will be moving to.
         * @param focus If before the animation the screen should focus the unit
         */
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Tile destinationTile, boolean focus) {
            _unit = unit;
            _destinationTile = destinationTile;
            _focus = focus;
        }

        protected void doWork(Canvas canvas) {
            
            if (_focus)
                canvas.getGUI().setFocusImmediately(_unit.getTile().getPosition());
                        
            Animations.unitMove(canvas, _unit, _destinationTile);
            canvas.refresh();
        }
        
        private final Unit _unit;
        private final Tile _destinationTile;
        private boolean _focus;
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
            canvas.showPanel(new VictoryPanel(canvas));
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
         * @param replace The replacement values.
         */
        public ShowConfirmDialogSwingTask(String text, String okText, String cancelText, String... replace) {
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

        private String[] _replace;
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

        /**
         * Constructor.
         * 
         * @param messageId The key for the message.
         * @param replace The values to replace text with.
         */
        public ShowInformationMessageSwingTask(String messageId, String... replace) {
            _messageId = messageId;
            _replace = replace;
        }

        protected Object doWork() {
            getFreeColClient().getCanvas().showInformationMessage(_messageId, _replace);
            return null;
        }


        private String _messageId;

        private String[] _replace;
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


        private MonarchAction  _action;

        private String[] _replace;
    }
}
