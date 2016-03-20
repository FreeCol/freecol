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

package net.sf.freecol.client.control;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeleteTradeRouteMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.HighScoreMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.NewTradeRouteMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.UpdateMessage;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Handle the network messages that arrives while in the game.
 *
 * Delegate to the real handlers in InGameController which are allowed
 * to touch the GUI.  Call IGC through invokeLater except for the messages
 * that demand a response, which requires invokeAndWait.
 *
 * Note that the EDT often calls the controller, which queries the
 * server, which results in handling the reply here, still within the
 * EDT.  invokeAndWait is illegal within the EDT, but none of messages
 * that require a response are client-initiated so the problem does
 * not arise...
 *
 * ...except for the special case of the animations.  These have to be
 * done in series but are sometimes in the EDT (our unit moves) and
 * sometimes not (other nation unit moves).  Hence the hack
 * GUI.invokeNowOrWait.
 */
public final class InGameInputHandler extends ClientInputHandler {

    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    // A bunch of predefined non-closure runnables.
    private final Runnable closeMenusRunnable = () -> {
        igc().closeMenus();
    };
    private final Runnable displayModelMessagesRunnable = () -> {
        igc().displayModelMessages(false);
    };
    private final Runnable reconnectRunnable = () -> {
        igc().reconnect();
    };


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public InGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(Connection.DISCONNECT_TAG,
            (Connection c, Element e) -> disconnect(e));
        register("addObject",
            (Connection c, Element e) -> addObject(e));
        register(AddPlayerMessage.TAG,
            (Connection c, Element e) -> addPlayer(e));
        register("animateAttack",
            (Connection c, Element e) -> animateAttack(e));
        register("animateMove",
            (Connection c, Element e) -> animateMove(e));
        register("chat",
            (Connection c, Element e) -> chat(e));
        register("chooseFoundingFather",
            (Connection c, Element e) -> chooseFoundingFather(e));
        register("closeMenus",
            (Connection c, Element e) -> closeMenus());
        register("diplomacy",
            (Connection c, Element e) -> diplomacy(e));
        register(ErrorMessage.TAG,
            (Connection c, Element e) -> error(e));
        register("featureChange",
            (Connection c, Element e) -> featureChange(e));
        register("firstContact",
            (Connection c, Element e) -> firstContact(e));
        register("fountainOfYouth",
            (Connection c, Element e) -> fountainOfYouth(e));
        register("gameEnded",
            (Connection c, Element e) -> gameEnded(e));
        register(GoodsForSaleMessage.TAG,
            (Connection c, Element e) -> goodsForSale(e));
        register(HighScoreMessage.TAG,
            (Connection c, Element e) -> highScore(e));
        register(InciteMessage.TAG,
            (Connection c, Element e) -> incite(e));
        register("indianDemand",
            (Connection c, Element e) -> indianDemand(e));
        register("lootCargo",
            (Connection c, Element e) -> lootCargo(e));
        register("monarchAction",
            (Connection c, Element e) -> monarchAction(e));
        register(MultipleMessage.TAG,
            (Connection c, Element e) -> multiple(c, e));
        register(NationSummaryMessage.TAG,
            (Connection c, Element e) -> nationSummary(e));
        register("newLandName",
            (Connection c, Element e) -> newLandName(e));
        register("newRegionName",
            (Connection c, Element e) -> newRegionName(e));
        register("newTurn",
            (Connection c, Element e) -> newTurn(e));
        register("newTradeRoute",
            (Connection c, Element e) -> newTradeRoute(e));
        register(Connection.RECONNECT_TAG,
            (Connection c, Element e) -> reconnect());
        register("remove",
            (Connection c, Element e) -> remove(e));
        register(ScoutSpeakToChiefMessage.TAG,
            (Connection c, Element e) -> scoutSpeakToChief(e));
        register("setAI",
            (Connection c, Element e) -> setAI(e));
        register("setCurrentPlayer",
            (Connection c, Element e) -> setCurrentPlayer(e));
        register("setDead",
            (Connection c, Element e) -> setDead(e));
        register("setStance",
            (Connection c, Element e) -> setStance(e));
        register(SpySettlementMessage.TAG,
            (Connection c, Element e) -> spySettlement(e));
        register(UpdateMessage.TAG,
            (Connection c, Element e) -> update(e));
    }


    /**
     * Shorthand to run in the EDT and wait.
     *
     * @param runnable The <code>Runnable</code> to run.
     */
    private void invokeAndWait(Runnable runnable) {
        getGUI().invokeNowOrWait(runnable);
    }
    
    /**
     * Shorthand to run in the EDT eventually.
     *
     * @param runnable The <code>Runnable</code> to run.
     */
    private void invokeLater(Runnable runnable) {
        getGUI().invokeNowOrLater(runnable);
    }
    
    /**
     * Get the integer value of an element attribute.
     *
     * @param element The <code>Element</code> to query.
     * @param attrib The attribute to use.
     * @return The integer value of the attribute, or
     *     Integer.MIN_VALUE on failure.
     */
    private static int getIntegerAttribute(Element element, String attrib) {
        int n;
        try {
            n = Integer.parseInt(element.getAttribute(attrib));
        } catch (NumberFormatException e) {
            n = Integer.MIN_VALUE;
        }
        return n;
    }

    /**
     * Select a child element with the given object identifier from a
     * parent element.
     *
     * @param parent The parent <code>Element</code>.
     * @param key The key to search for.
     * @return An <code>Element</code> with matching key,
     *     or null if none found.
     */
    private static Element selectElement(Element parent, String key) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element)nodes.item(i);
            if (key.equals(DOMMessage.readId(e))) return e;
        }
        return null;
    }

    /**
     * Sometimes units appear which the client does not know about,
     * and are passed in as the children of the parent element.
     * Worse, if their location is a Unit, that unit has to be passed in too.
     * Pull a unit out of the children by id.
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
                throw new RuntimeException("Null location: " + u);
            }
        }
        return u;
    }


    // Override ClientInputHandler

    /**
     * {@inheritDoc}
     */
    @Override
    public Element handle(Connection connection, Element element) {
        if (element == null) return null;
        Element reply = super.handle(connection, element);

        // If there is a "flush" attribute present, encourage the client
        // to display any new messages.
        if (Boolean.TRUE.toString().equals(element.getAttribute("flush"))
            && currentPlayerIsMyPlayer()) {
            invokeLater(displayModelMessagesRunnable);
        }
        return reply;
    }


    // Individual message handlers

    /**
     * Add the objects which are the children of this Element.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element addObject(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        DOMMessage.mapChildren(element, (e) -> {
                String owner = DOMMessage.getStringAttribute(e, "owner");
                Player player = game.getFreeColGameObject(owner, Player.class);
                if (player == null) {
                    logger.warning("addObject with broken owner: " + owner);
                } else {
                    final String tag = e.getTagName();
                    if (FoundingFather.getTagName().equals(tag)) {
                        FoundingFather father
                            = spec.getFoundingFather(DOMMessage.readId(e));
                        if (father != null) player.addFather(father);
                        player.invalidateCanSeeTiles();// Might be coronado?
                        
                    } else if (HistoryEvent.getTagName().equals(tag)) {
                        player.addHistory(DOMMessage.readElement(game, e, HistoryEvent.class));
                        
                    } else if (LastSale.getTagName().equals(tag)) {
                        player.addLastSale(DOMMessage.readElement(game, e, LastSale.class));
                        
                    } else if (ModelMessage.getTagName().equals(tag)) {
                        player.addModelMessage(DOMMessage.readElement(game, e, ModelMessage.class));
                        
                    } else {
                        logger.warning("addObject unrecognized: " + tag);
                    }
                }
                return player;
            });
        return null;
    }

    /**
     * Handle an "addPlayer"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element addPlayer(Element element) {
        new AddPlayerMessage(getGame(), element);
        return null;
    }

    /**
     * Handle an "animateAttack"-message.  This only performs animation, if
     * required.  It does not actually perform any attacks.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element animateAttack(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        String str;
        Unit u;

        if ((str = element.getAttribute("attacker")).isEmpty()) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " missing attacker attribute.");
        }
        if ((u = game.getFreeColGameObject(str, Unit.class)) == null
            && (u = selectUnitFromElement(game, element, str)) == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " omitted attacker: " + str);
        }
        final Unit attacker = u;

        if ((str = element.getAttribute("defender")).isEmpty()) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " missing defender attribute.");
        }
        if ((u = game.getFreeColGameObject(str, Unit.class)) == null
            && (u = selectUnitFromElement(game, element, str)) == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " omitted defender: " + str);
        }
        final Unit defender = u;

        if ((str = element.getAttribute("attackerTile")).isEmpty()) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " missing attacker tile attribute.");
        }
        final Tile attackerTile = game.getFreeColGameObject(str, Tile.class);
        if (attackerTile == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " omitted attacker tile: " + str);
        }

        if ((str = element.getAttribute("defenderTile")).isEmpty()) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " missing defender tile attribute.");
        }
        final Tile defenderTile = game.getFreeColGameObject(str, Tile.class);
        if (defenderTile == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " omitted defender tile: " + str);
        }

        final boolean success
            = Boolean.parseBoolean(element.getAttribute("success"));

        // All is well, do the animation.
        invokeAndWait(() -> {
                igc().animateAttack(attacker, defender,
                                    attackerTile, defenderTile, success);
            });
        return null;
    }

    /**
     * Handle an "animateMove"-message.  This only performs
     * animation, if required.  It does not actually change unit
     * positions, which happens in an "update".
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element animateMove(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();

        String unitId = element.getAttribute("unit");
        if (unitId.isEmpty()) {
            logger.warning("Animation for: " + player.getId()
                + " missing unitId.");
            return null;
        }
        Unit u = game.getFreeColGameObject(unitId, Unit.class);
        if (u == null) {
            u = selectUnitFromElement(game, element, unitId);
            //if (u != null) logger.info("Added unit from element: " + unitId);
        }
        if (u == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing unit:" + unitId);
            return null;
        }
        final Unit unit = u;

        String oldTileId = element.getAttribute("oldTile");
        if (oldTileId.isEmpty()) {
            logger.warning("Animation for: " + player.getId()
                + " missing oldTileId");
            return null;
        }
        final Tile oldTile = game.getFreeColGameObject(oldTileId, Tile.class);
        if (oldTile == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing oldTile: " + oldTileId);
            return null;
        }

        String newTileId = element.getAttribute("newTile");
        if (newTileId.isEmpty()) {
            logger.warning("Animation for: " + player.getId()
                + " missing newTileId");
            return null;
        }
        final Tile newTile = game.getFreeColGameObject(newTileId, Tile.class);
        if (newTile == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing newTile: " + newTileId);
            return null;
        }

        invokeAndWait(() -> { igc().animateMove(unit, oldTile, newTile); });
        return null;
    }

    /**
     * Handle a "chat"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element chat(Element element) {
        final Game game = getGame();
        final ChatMessage chatMessage = new ChatMessage(game, element);

        invokeLater(() ->
            igc().chat(chatMessage.getPlayer(game), chatMessage.getMessage(),
                       chatMessage.isPrivate()));
        return null;
    }

    /**
     * Handle an "chooseFoundingFather"-request.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.  The choice is returned asynchronously.
     */
    private Element chooseFoundingFather(Element element) {
        final ChooseFoundingFatherMessage message
            = new ChooseFoundingFatherMessage(getGame(), element);
        final List<FoundingFather> ffs = message.getFathers();

        invokeLater(() -> igc().chooseFoundingFather(ffs));
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
    private Element closeMenus() {
        invokeAndWait(closeMenusRunnable);
        return null;
    }

    /**
     * Handle a "diplomacy"-request.  If the message informs of an
     * acceptance or rejection then display the result and return
     * null.  If the message is a proposal, then ask the user about
     * it and return the response with appropriate response set.
     *
     * @param element The <code>Element</code> to process.
     * @return A diplomacy response, or null if none required.
     */
    private Element diplomacy(Element element) {
        final Game game = getGame();
        final DiplomacyMessage message
            = new DiplomacyMessage(getGame(), element);
        final DiplomaticTrade agreement = message.getAgreement();

        final FreeColGameObject our = message.getOurFCGO(game);
        if (our == null) {
            logger.warning("Our FCGO omitted from diplomacy message.");
            return null;
        }

        final FreeColGameObject other = message.getOtherFCGO(game);
        if (other == null) {
            logger.warning("Other FCGO omitted from diplomacy message.");
            return null;
        }

        invokeAndWait(() -> {
                message.setAgreement(igc().diplomacy(our, other, agreement));
            });
        return (message.getAgreement() == null) ? null
            : message.toXMLElement();
    }

    /**
     * Disposes of the <code>Unit</code>s which are the children of this
     * Element.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element disposeUnits(Element element) {
        final Game game = getGame();
        DOMMessage.mapChildren(element, (e) -> {
                // Do not read the whole unit out of the element as we
                // are only going to dispose of it, not forgetting
                // that the server may have already done so and its
                // view will only mislead us here in the client.
                final String id = DOMMessage.readId(e);
                Unit u = game.getFreeColGameObject(id, Unit.class);
                if (u == null) {
                    logger.warning("Object is not a unit");
                } else {
                    u.dispose();
                }
                return u;
            });
        return null;
    }

    /**
     * Handle an "error"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element error(Element element) {
        final ErrorMessage errorMessage = new ErrorMessage(getGame(), element);
        invokeLater(() -> igc().error(errorMessage.getMessageId(),
                                      errorMessage.getMessage()));
        return null;
    }

    /**
     * Adds a feature to or removes a feature from a FreeColGameObject.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element featureChange(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final boolean add = "add".equalsIgnoreCase(element.getAttribute("add"));
        final String id = DOMMessage.readId(element);
        final FreeColGameObject object = game.getFreeColGameObject(id);
        if (object == null) {
            logger.warning("featureChange with null object");
            return null;
        }

        DOMMessage.mapChildren(element, (e) -> {
                final String tag = DOMMessage.readId(e);
                if (Ability.getTagName().equals(tag)) {
                    if (add) {
                        object.addAbility(new Ability(e, spec));
                    } else {
                        object.removeAbility(new Ability(e, spec));
                    }
                    
                } else if (Modifier.getTagName().equals(tag)) {
                    if (add) {
                        object.addModifier(new Modifier(e, spec));
                    } else {
                        object.removeModifier(new Modifier(e, spec));
                    }
                    
                } else {
                    logger.warning("featureChange unrecognized: " + tag);
                }
                return object;
            });
        return null;
    }

    /**
     * Handle a first contact with a native nation.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element firstContact(Element element) {
        final Game game = getGame();
        final FirstContactMessage message
            = new FirstContactMessage(game, element);

        final Player player = message.getPlayer(game);
        if (player == null || player != getMyPlayer()) {
            logger.warning("firstContact with bad player: " + player);
            return null;
        }
        final Player other = message.getOtherPlayer(game);
        if (other == null || other == player || !other.isIndian()) {
            logger.warning("firstContact with bad other player: " + other);
            return null;
        }
        final Tile tile = message.getTile(game);
        if (tile != null && tile.getOwner() != other) {
            logger.warning("firstContact with bad tile: " + tile);
            return null;
        }
        final int n = message.getSettlementCount();

        invokeLater(() -> { igc().firstContact(player, other, tile, n); });
        return null;
    }

    /**
     * Ask the player to choose migrants from a fountain of youth event.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element fountainOfYouth(Element element) {
        final int n = getIntegerAttribute(element, "migrants");
        if (n <= 0) {
            logger.warning("Invalid migrants attribute: "
                + element.getAttribute("migrants"));
            return null;
        }

        invokeLater(() -> { igc().fountainOfYouth(n); });
        return null;
    }

    /**
     * Handle a "gameEnded"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element gameEnded(Element element) {
        FreeColDebugger.finishDebugRun(getFreeColClient(), true);
        final Player winner
            = getGame().getFreeColGameObject(element.getAttribute("winner"),
                                             Player.class);
        if (winner == null) {
            logger.warning("Invalid player for gameEnded");
            return null;
        }
        final String highScore = element.getAttribute("highScore");

        if (winner == getMyPlayer()) {
            invokeLater(() -> { igc().victory(highScore); });
        }
        return null;
    }

    /**
     * Handle a "goodsForSale"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element goodsForSale(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final GoodsForSaleMessage message
            = new GoodsForSaleMessage(game, element);
        final Unit unit = message.getUnit(player);
        final IndianSettlement is = message.getSettlement(unit); 

        is.setGoodsForSale(message.getGoods());
        return null;
    }

    /**
     * Handle an "incite" message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element incite(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final InciteMessage message = new InciteMessage(game, element);
        final Unit unit = message.getUnit(player);
        final IndianSettlement is = message.getSettlement(unit);
        final Player enemy = message.getEnemy(game);
        final int gold = message.getGold();
        
        invokeLater(() -> { igc().incite(unit, is, enemy, gold); });
        return null;
    }
    
    /**
     * Handle an incoming nation summary.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element nationSummary(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();

        NationSummaryMessage message = new NationSummaryMessage(game, element);
        Player other = message.getPlayer(game);
        NationSummary ns = message.getNationSummary();
        player.putNationSummary(other, ns);
        logger.info("Updated nation summary of " + other.getSuffix()
            + " for " + player.getSuffix() + " with " + ns);
        return null;
    }
        
    /**
     * Handle a "highScore" message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element highScore(Element element) {
        final Game game = getGame();
        final HighScoreMessage message
            = new HighScoreMessage(game, element);
        invokeLater(() -> { igc().displayHighScores(message.getKey(),
                                                    message.getScores()); });
        return null;
    }
        
    /**
     * Handle an "indianDemand"-request.
     *
     * @param element The <code>Element</code> to process.
     * @return An <code>IndianDemand</code> message containing the response,
     *     or null on error.
     */
    private Element indianDemand(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final IndianDemandMessage message
            = new IndianDemandMessage(game, element);
        final Unit unit = message.getUnit(game);
        if (unit == null) {
            logger.warning("IndianDemand with null unit: "
                + element.getAttribute("unit"));
            return null;
        }
        final Colony colony = message.getColony(game);
        if (colony == null) {
            logger.warning("IndianDemand with null colony: "
                + element.getAttribute("colony"));
            return null;
        } else if (!player.owns(colony)) {
            throw new IllegalArgumentException("Demand to anothers colony");
        }

        invokeAndWait(() -> message.setResult(igc().indianDemand(unit, colony,
                    message.getType(game), message.getAmount())));
        return message.toXMLElement();
    }

    /**
     * Ask the player to choose something to loot.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element lootCargo(Element element) {
        final Game game = getGame();
        final LootCargoMessage message = new LootCargoMessage(game, element);
        final Unit unit = message.getUnit(game);
        final String defenderId = message.getDefenderId();
        final List<Goods> goods = message.getGoods();
        if (unit == null || goods == null) return null;

        invokeLater(() -> igc().loot(unit, goods, defenderId));
        return null;
    }

    /**
     * Handle a "monarchAction"-request.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element monarchAction(Element element) {
        final Game game = getGame();
        final MonarchActionMessage message
            = new MonarchActionMessage(game, element);

        invokeLater(() -> igc().monarch(message.getAction(),
                message.getTemplate(), message.getMonarchKey()));
        return null;
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The <code>Connection</code> the element arrived on.
     * @param element The <code>Element</code> to process.
     * @return An <code>Element</code> containing the response/s.
     */
    private Element multiple(Connection connection, Element element) {
        return new MultipleMessage(element).applyHandler(this, connection);
    }

    /**
     * Ask the player to name the new land.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element newLandName(Element element) {
        final Game game = getGame();
        NewLandNameMessage message = new NewLandNameMessage(game, element);
        final Unit unit = message.getUnit(getMyPlayer());
        final String defaultName = message.getNewLandName();
        if (unit == null || defaultName == null 
            || !unit.hasTile()) return null;

        invokeLater(() -> igc().newLandName(defaultName, unit));
        return null;
    }

    /**
     * Ask the player to name a new region.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element newRegionName(Element element) {
        final Game game = getGame();
        NewRegionNameMessage message = new NewRegionNameMessage(game, element);
        final Tile tile = message.getTile(game);
        final Unit unit = message.getUnit(getMyPlayer());
        final Region region = message.getRegion(game);
        final String defaultName = message.getNewRegionName();
        if (defaultName == null || region == null) return null;

        invokeLater(() -> igc().newRegionName(region, defaultName, tile, unit));
        return null;
    }

    /**
     * Handle a "newTradeRoute" message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element newTradeRoute(Element element) {
        final Game game = getGame();
        NewTradeRouteMessage message = new NewTradeRouteMessage(game, element);
        if (message != null) {
            final Player player = getMyPlayer();
            TradeRoute tr = message.getTradeRoute();
            if (tr != null) player.addTradeRoute(tr);
        }
        return null;
    }
        
    /**
     * Handle a "newTurn"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element newTurn(Element element) {
        final int n = getIntegerAttribute(element, "turn");
        if (n < 0) {
            logger.warning("Invalid turn for newTurn");
            return null;
        }

        invokeLater(() -> igc().newTurn(n));
        return null;
    }

    /**
     * Handle an "reconnect"-message.
     *
     * @return Null.
     */
    private Element reconnect() {
        invokeLater(reconnectRunnable);
        return null;
    }

    /**
     * Handle a "remove"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element remove(Element element) {
        final Game game = getGame();
        final FreeColGameObject divert
            = game.getFreeColGameObject(element.getAttribute("divert"));
        final List<FreeColGameObject> objects = new ArrayList<>();
        DOMMessage.mapChildren(element, (e) -> {
                final String id = DOMMessage.readId(e);
                FreeColGameObject fcgo = game.getFreeColGameObject(id);
                if (fcgo != null) {
                    // Null fcgo can happen legitimately when an
                    // update that removes pointers to a disappearing
                    // unit happens, then a gc which drops the weak
                    // reference in freeColGameObjects, before this
                    // remove is processed.
                    objects.add(fcgo);
                }
                return fcgo;
            });
        if (!objects.isEmpty()) {
            invokeLater(() -> igc().remove(objects, divert));
        }
        return null;
    }

    /**
     * Handle a "scoutSpeakToChief"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element scoutSpeakToChief(Element element) {
        final Game game = getGame();
        final ScoutSpeakToChiefMessage message
            = new ScoutSpeakToChiefMessage(game, element);
        if (message != null) {
            final Unit unit = message.getUnit(game);
            final IndianSettlement settlement = message.getSettlement(game);
            final String result = message.getResult();
            invokeLater(() ->
                igc().scoutSpeakToChief(unit, settlement, result));
        }
        return null;
    }

    /**
     * Handle a "setAI"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element setAI(Element element) {
        final Game game = getGame();
        Player p = game.getFreeColGameObject(element.getAttribute("player"),
                                             Player.class);
        p.setAI(Boolean.parseBoolean(element.getAttribute("ai")));

        return null;
    }

    /**
     * Handle a "setCurrentPlayer"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element setCurrentPlayer(Element element) {
        final Player player
            = getGame().getFreeColGameObject(element.getAttribute("player"),
                                             Player.class);
        if (player == null) {
            logger.warning("Invalid player for setCurrentPlayer");
            return null;
        }

        igc().setCurrentPlayer(player); // It is safe to call this one directly
        return null;
    }

    /**
     * Handle a "setDead"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element setDead(Element element) {
        final Player player = getGame()
            .getFreeColGameObject(element.getAttribute("player"),Player.class);
        if (player == null) {
            logger.warning("Invalid player for setDead");
            return null;
        }

        invokeLater(() -> igc().setDead(player));
        return null;
    }

    /**
     * Handle a "setStance"-request.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element setStance(Element element) {
        final Game game = getGame();
        final Stance stance = Enum.valueOf(Stance.class,
                                           element.getAttribute("stance"));
        if (stance == null) {
            logger.warning("Invalid stance for setStance");
            return null;
        }
        final Player p1 = game
            .getFreeColGameObject(element.getAttribute("first"), Player.class);
        if (p1 == null) {
            logger.warning("Invalid player1 for setStance");
            return null;
        }
        final Player p2 = game
            .getFreeColGameObject(element.getAttribute("second"),Player.class);
        if (p2 == null) {
            logger.warning("Invalid player2 for setStance");
            return null;
        }

        invokeLater(() -> igc().setStance(stance, p1, p2));
        return null;
    }

    /**
     * Handle a "spyResult" message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element spySettlement(Element element) {
        final Game game = getGame();
        final SpySettlementMessage message
            = new SpySettlementMessage(game, element);
        final Tile spyTile = message.getSpyTile();
        invokeLater(() -> igc().spyColony(spyTile));
        return null;
    }

    /**
     * Handle an "update"-message.
     *
     * @param element The <code>Element</code> to process.
     * @return Null.
     */
    private Element update(Element element) {
        final Player player = getMyPlayer();
        final Game game = getGame();
        final UpdateMessage message = new UpdateMessage(game, element);
        boolean visibilityChange = false;
        
        for (FreeColGameObject fcgo : message.getObjects()) {
            if ((fcgo instanceof Player && (fcgo == player))
                || ((fcgo instanceof Settlement || fcgo instanceof Unit)
                    && player.owns((Ownable)fcgo))) {
                visibilityChange = true;//-vis(player)
            }
        }
        if (visibilityChange) player.invalidateCanSeeTiles();//+vis(player)

        return null;
    }
}
