/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.FreeColActionUI;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;

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
    public InGameInputHandler(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui);
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
        if (element == null) {
            throw new RuntimeException("Received empty (null) message!");
        }

        Element reply;
        String type = element.getTagName();
        logger.log(Level.FINEST, "Received message: " + type);

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
        } else if (type.equals("spyResult")) {
            reply = spyResult(element);
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
        } else if (type.equals("featureChange")) {
            reply = featureChange(element);
        } else if (type.equals("newLandName")) {
            reply = newLandName(element);
        } else if (type.equals("newRegionName")) {
            reply = newRegionName(element);
        } else if (type.equals("fountainOfYouth")) {
            reply = fountainOfYouth(element);
        } else if (type.equals("lootCargo")) {
            reply = lootCargo(element);
        } else if (type.equals("closeMenus")) {
            reply = closeMenus();
        } else if (type.equals("multiple")) {
            reply = multiple(connection, element);
        } else {
            logger.warning("Unsupported message type: " + type);
            return null;
        }
        logger.log(Level.FINEST, "Handled message: " + type
            + " replying with: "
            + ((reply == null) ? "null" : reply.getTagName()));

        final FreeColClient fcc = getFreeColClient();
        if (Boolean.TRUE.toString().equals(element.getAttribute("flush"))
            && fcc.currentPlayerIsMyPlayer()) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fcc.getInGameController().displayModelMessages(false);
                    }
                });
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
        if (new ShowConfirmDialogSwingTask(null,
                                           StringTemplate.key("reconnect.text"),
                                           "reconnect.yes", "reconnect.no").confirm()) {
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
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = FreeColObject.readId(element);
            FreeColGameObject fcgo = getGame().getFreeColGameObject(id);
            if (fcgo == null) {
                logger.warning("Update object not present in client: " + id);
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
        String ds = removeElement.getAttribute("divert");
        FreeColGameObject divert = game.getFreeColGameObject(ds);
        Player player = getFreeColClient().getMyPlayer();
        NodeList nodeList = removeElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String idString = FreeColObject.readId(element);
            FreeColGameObject fcgo = game.getFreeColGameObject(idString);
            if (fcgo == null) {
                logger.warning("Could not find FreeColGameObject: "
                               + idString);
            } else {
                if (divert != null) {
                    player.divertModelMessages(fcgo, divert);
                }
                // Deselect the object if it is the current active unit.
                if (fcgo instanceof Unit) {
                    Unit u = (Unit) fcgo;
                    player.invalidateCanSeeTiles();
                    if (u == gui.getActiveUnit())
                        gui.setActiveUnit(null);
                    // Temporary hack until we have real containers.
                    player.removeUnit(u);
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
     * Select a child element with the given object identifier from a
     * parent element.
     *
     * @param parent The parent <code>Element</code>.
     * @param key The key to search for.
     * @return An <code>Element</code> with matching key, or null if none found.
     */
    private static Element selectElement(Element parent, String key) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element)nodes.item(i);
            if (key.equals(FreeColObject.readId(e))) return e;
        }
        return null;
    }

    /**
     * Sometimes units appear which the client does not know about,
     * and are passed in as the children of the parent element.
     * Worse, if their location is a Unit, that unit has to be passed in too.
     *
     * @param game The <code>Game</code> to add the unit to.
     * @param element The <code>Element</code> to find a unit in.
     * @param id The object identifier of the unit to find.
     * @return A unit or null if none found.
     */
    private static Unit selectUnitFromElement(Game game, Element element,
                                              String id) {
        Element e = selectElement(element, id);
        Unit u = null;
        if (e != null) {
            u = new Unit(game, e);
            if (u.getLocation() == null) {
                throw new IllegalStateException("NULL LOC: " + u);
                /*String locId = e.getAttribute("location");
                  if (locId.startsWith("unit:")) {
                  selectUnitFromElement(game, element, locId);
                  }*/
            }
        }
        return u;
    }

    /**
     * Handles an "animateMove"-message.  This only performs
     * animation, if required.  It does not actually change unit
     * positions, which happens in an "update".
     *
     * @param element An element (root element in a DOM-parsed XML
     *     tree) that holds attributes for the old and new tiles and
     *     an element for the unit that is moving (which are used
     *     solely to operate the animation).
     * @return Null.
     */
    private Element animateMove(Element element) {
        FreeColClient client = getFreeColClient();
        if (client.isHeadless()) return null;

        final Game game = getGame();
        String unitId = element.getAttribute("unit");
        Unit unit;
        if (unitId == null
            || ((unit = game.getFreeColGameObject(unitId, Unit.class)) == null
                && (unit = selectUnitFromElement(game, element, unitId)) == null)) {
            throw new IllegalStateException("Animation"
                + " for: " + client.getMyPlayer().getId()
                + " missing unit:" + unitId);
        }

        Player player = client.getMyPlayer();
        String oldTileId = element.getAttribute("oldTile");
        String newTileId = element.getAttribute("newTile");
        Tile oldTile, newTile;
        if (oldTileId == null
            || (oldTile = game.getFreeColGameObject(oldTileId, Tile.class)) == null) {
            throw new IllegalStateException("Animation for: " + player.getId()
                + " missing oldTile: " + oldTileId);
        }
        if (newTileId == null
            || (newTile = game.getFreeColGameObject(newTileId, Tile.class)) == null) {
            throw new IllegalStateException("Animation for: " + player.getId()
                + " missing newTile: " + newTileId);
        }

        if (gui.getAnimationSpeed(unit) > 0) {
            // All is well, queue the animation.
            // Use lastAnimatedUnit as a filter to avoid excessive refocussing.
            try {
                new UnitMoveAnimationCanvasSwingTask(unit, oldTile, newTile,
                    unit != lastAnimatedUnit).invokeSpecial();
            } catch (Exception e) {
                logger.log(Level.WARNING, "UnitMoveAnimation", e);
            }
        } else {
            // Not animating, but if the centering option is enabled at least
            // refocus so we can see the move happen.
            if (!gui.onScreen(oldTile) && client.getClientOptions()
                .getBoolean(ClientOptions.ALWAYS_CENTER)) {
                gui.setFocus(oldTile);
            }
        }
        lastAnimatedUnit = unit;
        return null;
    }

    /**
     * Handles an "animateAttack"-message.  This only performs animation, if
     * required.  It does not actually perform any attacks.
     *
     * @param element An element (root element in a DOM-parsed XML
     *     tree) that holds attributes for the old and new tiles and
     *     an element for the unit that is moving (which are used
     *     solely to operate the animation).
     * @return Null.
     */
    private Element animateAttack(Element element) {
        FreeColClient client = getFreeColClient();
        if (client.isHeadless()) return null;
        final Game game = getGame();
        String str;
        Unit attacker, defender;
        Tile attackerTile, defenderTile;

        if ((str = element.getAttribute("attacker")) == null) {
            logger.warning("animateAttack null attacker");
            return null;
        } else {
            if ((attacker = game.getFreeColGameObject(str, Unit.class)) == null
                && (attacker = selectUnitFromElement(game, element, str)) == null) {
                logger.warning("Attack animation"
                    + " for: " + client.getMyPlayer().getId()
                    + " incorrectly omitted attacker: " + str);
                return null;
            }
        }

        if ((str = element.getAttribute("defender")) == null) {
            logger.warning("animateAttack null defender");
            return null;
        } else {
            if ((defender = game.getFreeColGameObject(str, Unit.class)) == null
                && (defender = selectUnitFromElement(game, element, str)) == null) {
                logger.warning("Attack animation"
                    + " for: " + client.getMyPlayer().getId()
                    + " incorrectly omitted defender: " + str);
                return null;
            }
        }

        if ((str = element.getAttribute("attackerTile")) == null) {
            logger.warning("animateAttack null attackerTile");
            return null;
        } else {
            attackerTile = game.getFreeColGameObject(str, Tile.class);
            if (attackerTile == null) {
                logger.warning("Attack animation"
                    + " for: " + client.getMyPlayer().getId()
                    + " can not find attacker tile: " + str);
                return null;
            }
        }

        if ((str = element.getAttribute("defenderTile")) == null) {
            logger.warning("animateAttack null defenderTile");
            return null;
        } else {
            defenderTile = game.getFreeColGameObject(str, Tile.class);
            if (defenderTile == null) {
                logger.warning("Attack animation"
                    + " for: " + client.getMyPlayer().getId()
                    + " can not find defender tile: " + str);
                return null;
            }
        }

        boolean success = Boolean.parseBoolean(element.getAttribute("success"));
        // All is well, queue the animation.
        // Use lastAnimatedUnit as a filter to avoid excessive refocussing.
        try {
            new UnitAttackAnimationCanvasSwingTask(attacker, defender,
                attackerTile, defenderTile, success,
                attacker != lastAnimatedUnit).invokeSpecial();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Attack animation fail", e);
        }
        lastAnimatedUnit = attacker;
        return null;
    }

    private void takeTurn(Player player, boolean newTurn) {
        final FreeColClient fcc = getFreeColClient();
    }

    /**
     * Handles a "setCurrentPlayer"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *            tree) that holds all the information.
     * @return an <code>Element</code> value
     */
    private Element setCurrentPlayer(Element element) {
        final FreeColClient fcc = getFreeColClient();
        final Player player = fcc.getMyPlayer();
        final Player newPlayer = getGame()
            .getFreeColGameObject(element.getAttribute("player"), Player.class);
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && fcc.currentPlayerIsMyPlayer()) closeMenus();
        FreeColDebugger.finishDebugRun(fcc, false);
        
        fcc.getInGameController().setCurrentPlayer(newPlayer);
        if (player == newPlayer) { // Prepare client for our player turn.
            try {
                List<Settlement> settlements = player.getSettlements();
                Tile defTile = ((settlements.isEmpty())
                    ? player.getEntryLocation().getTile()
                    : settlements.get(0).getTile()).getSafeTile(null, null);
                player.resetIterators();
                fcc.getInGameController().nextActiveUnit(defTile);
            } catch (Exception e) {
                // We end up here if there is a crash in things like the
                // turn report.  These were hard to track down because we
                // used to fail silently.  We now complain louder.
                logger.log(Level.WARNING, "Client new turn failure for " + player, e);
            }
        }
        fcc.updateActions();

        new RefreshCanvasSwingTask(true).invokeLater();
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

        // plays an alert sound on each new turn if the option for it is turned on
        if (getFreeColClient().getClientOptions().getBoolean("model.option.audioAlerts")) {
            gui.playSound("sound.event.alertSound");
        }


        if (currTurn.isFirstSeasonTurn()) {
            new ShowInformationMessageSwingTask(
                StringTemplate.template("twoTurnsPerYear")
                    .addName("%year%", Integer.toString(currTurn.getYear())))
                .invokeLater();
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
        Player player = getGame().getFreeColGameObject(element.getAttribute("player"),
                                                       Player.class);
        Player myPlayer = freeColClient.getMyPlayer();
        if (player == myPlayer) {
            FreeColDebugger.finishDebugRun(freeColClient, true);
            if (freeColClient.isSinglePlayer()) {
                if (myPlayer.getPlayerType() == Player.PlayerType.RETIRED) {
                    ; // Do nothing, retire routine will quit
                } else if (myPlayer.getPlayerType() != Player.PlayerType.UNDEAD
                    && new ShowConfirmDialogSwingTask(null,
                        StringTemplate.key("defeatedSinglePlayer.text"),
                        "defeatedSinglePlayer.yes",
                        "defeatedSinglePlayer.no").confirm()) {
                    freeColClient.askServer().enterRevengeMode();
                } else {
                    freeColClient.quit();
                }
            } else {
                if (!new ShowConfirmDialogSwingTask(null, StringTemplate.key("defeated.text"),
                        "defeated.yes", "defeated.no").confirm()) {
                    freeColClient.quit();
                }
            }
        } else {
            myPlayer.setStance(player, null);
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
        FreeColDebugger.finishDebugRun(freeColClient, true);
        String str = element.getAttribute("winner");
        Player winner = (str == null) ? null
            : getGame().getFreeColGameObject(str, Player.class);
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
        final Game game = getGame();
        final ChatMessage chatMessage = new ChatMessage(game, element);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.displayChatMessage(chatMessage.getPlayer(game),
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
        } catch (Exception e) {
            logger.log(Level.WARNING, "error() raised exception", e);
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

        Player p = getGame().getFreeColGameObject(element.getAttribute("player"),
                                                  Player.class);
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
        ChooseFoundingFatherMessage message
            = new ChooseFoundingFatherMessage(getGame(), element);
        List<FoundingFather> ffs = message.getFathers();
        FoundingFather ff = gui.showChooseFoundingFatherDialog(ffs);
        if (ff != null) {
            message.setResult(ff);
            getFreeColClient().getMyPlayer().setCurrentFather(ff);
        }
        return message.toXMLElement();
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
        Player player = getFreeColClient().getMyPlayer();
        DiplomacyMessage message = new DiplomacyMessage(getGame(), element);
        Unit unit = message.getUnit();
        if (unit == null) {
            logger.warning("Unit omitted from diplomacy message.");
            return null;
        }
        Settlement settlement = message.getSettlement();
        if (settlement == null) {
            logger.warning("Settlement omitted from diplomacy message.");
            return null;
        }
        Player other = (player.owns(unit)) ? settlement.getOwner()
            : unit.getOwner();
        String nation = Messages.message(other.getNationName());
        DiplomaticTrade agreement = message.getAgreement();

        switch (agreement.getStatus()) {
        case ACCEPT_TRADE:
            new ShowInformationMessageSwingTask(StringTemplate
                .template("negotiationDialog.offerAccepted")
                    .addName("%nation%", nation)).show();
            new UpdateMenuBarSwingTask().invokeLater();
            break;
        case REJECT_TRADE:
            new ShowInformationMessageSwingTask(StringTemplate
                .template("negotiationDialog.offerRejected")
                    .addName("%nation%", nation)).show();
            new UpdateMenuBarSwingTask().invokeLater();
            break;
        case PROPOSE_TRADE:
            DiplomaticTrade ourAgreement
                = gui.showNegotiationDialog(unit, settlement, agreement);
            if (ourAgreement == null) {
                agreement.setStatus(TradeStatus.REJECT_TRADE);
            } else {
                agreement = ourAgreement;
            }
            return new DiplomacyMessage(unit, settlement, agreement)
                .toXMLElement();
        default:
            logger.warning("Bogus trade status: " + agreement.getStatus());
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
        } else if (!player.owns(colony)) {
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
                    StringTemplate.template("indianDemand.gold.text")
                    .addName("%nation%", nation)
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", message.getGold()),
                    "indianDemand.gold.yes",
                    "indianDemand.gold.no").confirm();
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
                        StringTemplate.template("indianDemand.food.text")
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", goods.getAmount()),
                        "indianDemand.food.yes",
                        "indianDemand.food.no").confirm();
                } else {
                    accepted = new ShowConfirmDialogSwingTask(colony.getTile(),
                        StringTemplate.template("indianDemand.other.text")
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", goods.getAmount())
                        .add("%goods%", goods.getType().getNameKey()),
                        "indianDemand.other.yes",
                        "indianDemand.other.no").confirm();
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
            getFreeColClient().getInGameController().nextModelMessage();
        }
        message.setResult(accepted);
        return message.toXMLElement();
    }

    /**
     * Handles a "spyResult" message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element spyResult(Element element) {
        // The element contains two children, being the full and
        // normal versions of the settlement-being-spied-upon's tile.
        // It has to be the tile, as otherwise we do not see the units
        // defending the settlement.  So, we have to unpack, update
        // with the first, display, then update with the second.  This
        // is hacky as the client could retain the settlement
        // information, but the potential abuses are limited.
        NodeList nodeList = element.getChildNodes();
        if (nodeList.getLength() != 2) {
            logger.warning("spyResult length = " + nodeList.getLength());
            return null;
        }
        Game game = getGame();
        final Element fullTile = (Element) nodeList.item(0);
        final Element normalTile = (Element) nodeList.item(1);
        String tileId = element.getAttribute("tile");
        final Tile tile = game.getFreeColGameObject(tileId, Tile.class);
        if (tile == null) {
            logger.warning("spyResult bad tile = " + tileId);
            return null;
        }
        tile.readFromXMLElement(fullTile);
        Colony colony = tile.getColony();
        if (colony == null) {
            tile.readFromXMLElement(normalTile);
        } else {
            new SpyColonySwingTask(colony, normalTile).invokeLater();
        }
        return null;
    }


    /**
     * Handles a "monarchAction"-request.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element monarchAction(Element element) {
        Game game = getGame();
        MonarchActionMessage message = new MonarchActionMessage(game, element);
        MonarchAction action = message.getAction();
        boolean accept = new ShowMonarchPanelSwingTask(action,
            message.getTemplate()).confirm();
        message.setResult(accept);

        Element reply; // Some actions require an answer.
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR: case OFFER_MERCENARIES:
            reply = message.toXMLElement();
            break;
        default:
            reply = null;
            break;
        }

        new UpdateMenuBarSwingTask().invokeLater();
        return reply;
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
        Player first = game.getFreeColGameObject(element.getAttribute("first"),
                                                 Player.class);
        Player second = game.getFreeColGameObject(element.getAttribute("second"),
                                                  Player.class);

        Stance old = first.getStance(second);
        try {
            first.setStance(second, stance);
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Illegal stance transition", e);
            return null;
        }
        logger.info("Stance transition: " + old.toString()
            + " -> " + stance.toString());
        if (player == first && old == Stance.UNCONTACTED) {
            gui.playSound("sound.event.meet."
                + second.getNationId());
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
        Game game = getGame();
        String id = FreeColObject.readId(playerElement);
        if (game.getFreeColGameObject(id, Player.class) == null) {
            game.addPlayer(new Player(game, playerElement));
        } else {
            game.getFreeColGameObject(id).readFromXMLElement(playerElement);
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
            String id = FreeColObject.readId(e);
            Unit u = game.getFreeColGameObject(id, Unit.class);
            if (u == null) {
                logger.warning("Object is not a unit");
            } else {
                u.dispose();
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
            Player player = game.getFreeColGameObject(owner, Player.class);
            if (player == null) {
                logger.warning("addObject with broken owner: " + owner);
                continue;
            }

            final String tag = e.getTagName();
            if (FoundingFather.getXMLElementTagName() == tag) {
                FoundingFather father = spec.getFoundingFather(FreeColObject.readId(e));
                if (father != null) player.addFather(father);
                
            } else if (HistoryEvent.getXMLElementTagName() == tag) {
                player.getHistory().add(new HistoryEvent(e));

            } else if (LastSale.getXMLElementTagName() == tag) {
                player.addLastSale(new LastSale(e));

            } else if (ModelMessage.getXMLElementTagName() == tag) {
                player.addModelMessage(new ModelMessage(e));

            } else if (TradeRoute.getXMLElementTagName() == tag) {
                player.getTradeRoutes().add(new TradeRoute(game, e));

            } else {
                logger.warning("addObject unrecognized: " + tag);
            }
        }
        return null;
    }

    /**
     * Adds a feature to or removes a feature from a FreeColGameObject.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element featureChange(Element element) {
        Game game = getGame();
        Specification spec = game.getSpecification();
        boolean add = "add".equalsIgnoreCase(element.getAttribute("add"));
        String id = FreeColObject.readId(element);
        FreeColGameObject object = game.getFreeColGameObject(id);
        if (object == null) {
            logger.warning("featureChange with null object");
            return null;
        }

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);

            final String tag = e.getTagName();
            if (Ability.getXMLElementTagName() == tag) {
                if (add) {
                    object.addAbility(new Ability(e, spec));
                } else {
                    object.removeAbility(new Ability(e, spec));
                }

            } else if (Modifier.getXMLElementTagName() == tag) {
                if (add) {
                    object.addModifier(new Modifier(e, spec));
                } else {
                    object.removeModifier(new Modifier(e, spec));
                }

            } else {
                logger.warning("featureChange unrecognized: " + tag);
            }
        }
        return null;
    }



    /**
     * Ask the player to name the new land.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element newLandName(Element element) {
        Game game = getGame();
        NewLandNameMessage message = new NewLandNameMessage(game, element);
        Unit unit = message.getUnit(game);
        String defaultName = message.getNewLandName();
        if (unit == null || defaultName == null
            || unit.getTile() == null) return null;

        // Offer to name the land.
        new NewLandNameSwingTask(unit, defaultName, message.getWelcomer(game),
            message.getCamps()).invokeLater();
        return null;
    }

    /**
     * Ask the player to name a new region.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element newRegionName(Element element) {
        Game game = getGame();
        NewRegionNameMessage message = new NewRegionNameMessage(game, element);
        String name = message.getNewRegionName();
        Region region = message.getRegion(game);
        Tile tile = message.getTile(game);
        if (name == null || region == null) return null;

        // Offer to name the region.
        new NewRegionNameSwingTask(tile, region, message.getNewRegionName())
            .invokeLater();
        return null;
    }

    /**
     * Ask the player to choose migrants from a fountain of youth event.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element fountainOfYouth(Element element) {
        String migrants = element.getAttribute("migrants");
        int n;
        try {
            n = Integer.parseInt(migrants);
        } catch (NumberFormatException e) {
            n = -1;
        }
        if (n > 0) {
            // Without Brewster, the migrants have already been selected
            // and were updated to the European docks by the server.
            final int m = n;
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (int i = 0; i < m; i++) {
                            int index = gui.showEmigrationPanel(true);
                            getFreeColClient().askServer().emigrate(index + 1);
                        }
                    }
                });
        }
        return null;
    }

    /**
     * Ask the player to choose something to loot.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element lootCargo(Element element) {
        Game game = getGame();
        LootCargoMessage message = new LootCargoMessage(game, element);
        Unit unit = message.getUnit(game);
        List<Goods> goods = message.getGoods();
        if (unit == null || goods == null) return null;
        new LootCargoSwingTask(unit, message.getDefenderId(), goods)
            .invokeLater();
        return null;
    }

    /**
     * Trivial handler to allow the server to signal to the client
     * that an offer that caused a popup (for example, a native demand
     * or diplomacy proposal) has not been answered quickly enough and
     * that the offering player has assumed this player has
     * refused-by-inaction, and therefore, the popup needs to be
     * closed.
     *
     * @return Null.
     */
    public Element closeMenus() {
        gui.closeMenus();
        return null;
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The <code>Connection</code> the element arrived on.
     * @param element The <code>Element</code> to process.
     * @return An <code>Element</code> containing the response/s.
     */
    public Element multiple(Connection connection, Element element) {
        NodeList nodes = element.getChildNodes();
        List<Element> results = new ArrayList<Element>();

        for (int i = 0; i < nodes.getLength(); i++) {
            try {
                Element reply = handle(connection, (Element) nodes.item(i));
                if (reply != null) results.add(reply);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Caught crash in multiple item " + i
                    + ", continuing.", e);
            }
        }
        return DOMMessage.collapseElements(results);
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
                taskLogger.log(Level.FINEST, "Running Swing task "
                               + getClass().getName() + "...");

                setResult(doWork());

                taskLogger.log(Level.FINEST, "Swing task "
                               + getClass().getName()
                               + " returned " + _result);
            } catch (RuntimeException e) {
                taskLogger.log(Level.WARNING, "Swing task "
                               + getClass().getName() + " failed!", e);
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
     * Base class for Swing tasks that need to do a simple update
     * without return value, and use the canvas.
     */
    abstract class NoResultCanvasSwingTask extends SwingTask {

        protected Object doWork() {
            doNoResultWork();
            return null;
        }

        abstract void doNoResultWork();

    }

    /**
     * This task refreshes the entire canvas.
     */
    class RefreshCanvasSwingTask extends NoResultCanvasSwingTask {

        private final boolean requestFocus;


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
            this.requestFocus = requestFocus;
        }

        protected void doNoResultWork() {
            gui.refresh();

            if (requestFocus && !gui.isShowingSubPanel()) {
                gui.requestFocusInWindow();
            }
        }
    }

    /**
     * This class displays a dialog that lets the player choose goods to loot.
     */
    class LootCargoSwingTask extends NoResultCanvasSwingTask {

        private Unit unit;
        private String defenderId;
        private List<Goods> goods;


        /**
         * Constructor.
         *
         * @param goods A list of <code>Goods</code> to choose from.
         */
        public LootCargoSwingTask(Unit unit, String defenderId,
                                  List<Goods> goods) {
            this.unit = unit;
            this.defenderId = defenderId;
            this.goods = goods;
        }

        protected void doNoResultWork() {
            goods = gui.showCaptureGoodsDialog(unit, goods);
            if (!goods.isEmpty()) {
                getFreeColClient().askServer().loot(unit, defenderId, goods);
            }
        }
    }

    class NewLandNameSwingTask extends NoResultCanvasSwingTask {

        private Unit unit;
        private String defaultName;
        private Player welcomer;
        private String camps;


        /**
         * Constructor.
         *
         * @param unit The <code>Unit</code> that has come ashore.
         * @param defaultName The default new land name.
         * @param welcomer An optional <code>Player</code> that is welcoming
         *     this player to the new world.
         * @param camps The number of camps of the welcomer.
         */
        public NewLandNameSwingTask(Unit unit, String defaultName,
            Player welcomer, String camps) {
            this.unit = unit;
            this.defaultName = defaultName;
            this.welcomer = welcomer;
            this.camps = camps;
        }

        protected void doNoResultWork() {
            // Player names the land.
            Tile tile = unit.getTile();
            String name = gui.showInputDialog(tile,
                StringTemplate.template("newLand.text"), defaultName,
                "newLand.yes", null, true);

            // Check if there is a welcoming native offering land.
            boolean accept = false;
            if (welcomer != null) {
                String messageId = (welcomer.owns(tile))
                    ? "welcomeOffer.text" : "welcomeSimple.text";
                String type = ((IndianNationType) welcomer
                    .getNationType()).getSettlementTypeKey(true);
                accept = gui.showConfirmDialog(tile,
                    StringTemplate.template(messageId)
                        .addStringTemplate("%nation%", welcomer.getNationName())
                        .addName("%camps%", camps)
                        .add("%settlementType%", type),
                    "welcome.yes", "welcome.no");
            }

            // Respond to the server.
            FreeColClient fcc = getFreeColClient();
            fcc.askServer().newLandName(unit, name, welcomer, accept);

            // Add tutorial message.
            Player player = unit.getOwner();
            String key = FreeColActionUI.getHumanKeyStrokeText(fcc
                .getActionManager().getFreeColAction("buildColonyAction")
                .getAccelerator());
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                    "tutorial.buildColony", player)
                .addName("%build_colony_key%", key)
                .add("%build_colony_menu_item%", "buildColonyAction.name")
                .add("%orders_menu_item%", "menuBar.orders"));
            fcc.getInGameController().nextModelMessage();
        }
    }

    class NewRegionNameSwingTask extends NoResultCanvasSwingTask {

        private Tile tile;
        private Region region;
        private String defaultName;


        /**
         * Constructor.
         *
         * @param tile The <code>Tile</code> where the region is discovered.
         * @param region The <code>Region</code> that is discovered.
         * @param defaultName The default name of the new region.
         */
        public NewRegionNameSwingTask(Tile tile, Region region,
                                      String defaultName) {
            this.tile = tile;
            this.region = region;
            this.defaultName = defaultName;
        }

        protected void doNoResultWork() {
            String name = gui.showInputDialog(tile,
                StringTemplate.template("nameRegion.text")
                    .addName("%type%", Messages.message(region.getLabel())),
                defaultName, "ok", null, false);
            if (name == null || "".equals(name)) name = defaultName;
            getFreeColClient().askServer().newRegionName(region, tile, name);
        }
    }

    class RefreshTilesSwingTask extends NoResultCanvasSwingTask {

        public RefreshTilesSwingTask(Tile oldTile, Tile newTile) {
            super();
            _oldTile = oldTile;
            _newTile = newTile;
        }

        void doNoResultWork() {
            gui.refreshTile(_oldTile);
            gui.refreshTile(_newTile);
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
         * Constructor.
         * Play the unit movement animation, optionally focusing on
         * the source tile.
         *
         * @param unit The <code>Unit</code> that is moving.
         * @param sourceTile The <code>Tile</code> from which to move.
         * @param destinationTile The <code>Tile</code> to move to.
         * @param focus Focus on the source tile before the animation.
         */
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Tile sourceTile,
                                                Tile destinationTile,
                                                boolean focus) {
            this.unit = unit;
            this.sourceTile = sourceTile;
            this.destinationTile = destinationTile;
            this.focus = focus;
        }

        protected void doNoResultWork() {
            if (focus || !gui.onScreen(sourceTile)) {
                gui.setFocusImmediately(sourceTile);
            }

            gui.animateUnitMove(unit, sourceTile, destinationTile);
            gui.refresh();
        }
    }

    /**
     * This task plays an unit attack animation in the Canvas.
     */
    class UnitAttackAnimationCanvasSwingTask extends NoResultCanvasSwingTask {

        private final Unit attacker;
        private final Unit defender;
        private final Tile attackerTile;
        private final Tile defenderTile;
        private final boolean success;

        private boolean focus;


        /**
         * Constructor - Play the unit attack animation, optionally focusing on
         * the source tile.
         *
         * @param attacker The <code>Unit</code> that is attacking.
         * @param defender The <code>Unit</code> that is defending.
         * @param attackerTile The <code>Tile</code> the attack comes from.
         * @param defenderTile The <code>Tile</code> the attack goes to.
         * @param success Did the attack succeed?
         * @param focus Focus on the source tile before the animation.
         */
        public UnitAttackAnimationCanvasSwingTask(Unit attacker, Unit defender,
            Tile attackerTile, Tile defenderTile, boolean success,
            boolean focus) {
            this.attacker = attacker;
            this.defender = defender;
            this.attackerTile = attackerTile;
            this.defenderTile = defenderTile;
            this.success = success;
            this.focus = focus;
        }

        protected void doNoResultWork() {
            if (focus || !gui.onScreen(attackerTile)
                || !gui.onScreen(defenderTile)) {
                gui.setFocusImmediately(attackerTile);
            }

            gui.animateUnitAttack(attacker, defender,
                                  attackerTile, defenderTile, success);
            gui.refresh();
        }
    }

    /**
     * This task shows an enhanced colony panel, then restores the
     * normal information when it closes.
     */
    class SpyColonySwingTask extends NoResultCanvasSwingTask {
        private Colony colony;
        private Element normalTile;

        public SpyColonySwingTask(Colony colony, Element normalTile) {
            this.colony = colony;
            this.normalTile = normalTile;
        }

        protected void doNoResultWork() {
            final Tile tile = colony.getTile();
            gui.showColonyPanel(colony)
                .addClosingCallback(new Runnable() {
                        public void run() {
                            tile.readFromXMLElement(normalTile);
                        }
                    });
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
        protected void doNoResultWork() {
            gui.updateMenuBar();
        }
    }

    /**
     * This task shows the victory panel.
     */
    class ShowVictoryPanelSwingTask extends NoResultCanvasSwingTask {
        protected void doNoResultWork() {
            gui.showVictoryPanel();
        }
    }

    /**
     * This class shows a dialog and saves the answer (ok/cancel).
     */
    class ShowConfirmDialogSwingTask extends SwingTask {

        private Tile tile;

        private StringTemplate text;

        private String okText;

        private String cancelText;


        /**
         * Constructor.
         *
         * @param tile An optional tile to make visible.
         * @param text The key for the question.
         * @param okText The key for the OK button.
         * @param cancelText The key for the Cancel button.
         */
        public ShowConfirmDialogSwingTask(Tile tile, StringTemplate text,
                                          String okText, String cancelText) {
            this.tile = tile;
            this.text = text;
            this.okText = okText;
            this.cancelText = cancelText;
        }

        /**
         * Show dialog and wait for selection.
         *
         * @return true if OK, false if Cancel.
         */
        public boolean confirm() {
            try {
                Object result = invokeSpecial();
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
            boolean choice = gui.showConfirmDialog(tile, text, okText,
                cancelText);
            return Boolean.valueOf(choice);
        }
    }

    /**
     * This class shows a an input dialog and saves the answer (ok/cancel).
     */
    class ShowInputDialogSwingTask extends SwingTask {

        private Tile tile;

        private StringTemplate text;

        private String defaultValue;

        private String okText;

        private String cancelText;

        private boolean rejectEmpty;


        /**
         * Constructor.
         *
         * @param tile An optional tile to make visible.
         * @param text A <code>StringTemplate</code> for the question.
         * @param defaultValue The default value.
         * @param okText The key for the OK button.
         * @param cancelText The key for the Cancel button.
         * @param rejectEmpty Reject the empty response.
         */
        public ShowInputDialogSwingTask(Tile tile, StringTemplate text,
                                        String defaultValue,
                                        String okText, String cancelText,
                                        boolean rejectEmpty) {
            this.tile = tile;
            this.text = text;
            this.defaultValue = defaultValue;
            this.okText = okText;
            this.cancelText = cancelText;
            this.rejectEmpty = rejectEmpty;
        }

        /**
         * Show dialog and wait for selection.
         *
         * @return The result string.
         */
        public String show() {
            try {
                Object result = invokeSpecial();
                return (result instanceof String) ? (String) result : null;
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        protected Object doWork() {
            String choice = gui.showInputDialog(tile, text, defaultValue,
                okText, cancelText, rejectEmpty);
            return choice;
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
                invokeSpecial();
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
            gui.showModelMessages(_modelMessage);
            return null;
        }


        private ModelMessage _modelMessage;
    }

    /**
     * This class shows an informational dialog.
     */
    class ShowInformationMessageSwingTask extends ShowMessageSwingTask {

        private StringTemplate message;


        /**
         * Constructor.
         *
         * @param message the StringTemplate
         */
        public ShowInformationMessageSwingTask(StringTemplate message) {
            this.message = message;
        }

        protected Object doWork() {
            gui.showInformationMessage(null, message);
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
            gui.errorMessage(_messageId, _message);
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
     * This class shows the monarch panel.
     */
    class ShowMonarchPanelSwingTask extends SwingTask {

        private MonarchAction action;

        private StringTemplate replace;


        /**
         * Constructor.
         *
         * @param action The action key.
         * @param replace The replacement values.
         */
        public ShowMonarchPanelSwingTask(MonarchAction action,
                                         StringTemplate replace) {
            this.action = action;
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
            boolean choice = gui.showMonarchPanelDialog(action, replace);
            return Boolean.valueOf(choice);
        }
    }
}
