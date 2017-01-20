/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.AnimateAttackMessage;
import net.sf.freecol.common.networking.AnimateMoveMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.FountainOfYouthMessage;
import net.sf.freecol.common.networking.GameEndedMessage;
import net.sf.freecol.common.networking.HighScoreMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NativeTradeMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.NewTradeRouteMessage;
import net.sf.freecol.common.networking.NewTurnMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.SetAIMessage;
import net.sf.freecol.common.networking.SetCurrentPlayerMessage;
import net.sf.freecol.common.networking.SetDeadMessage;
import net.sf.freecol.common.networking.SetStanceMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateMessage;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


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
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public InGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(AddPlayerMessage.TAG,
            (Connection c, Element e) -> addPlayer(e));
        register(AnimateAttackMessage.TAG,
            (Connection c, Element e) -> animateAttack(e));
        register(AnimateMoveMessage.TAG,
            (Connection c, Element e) -> animateMove(e));
        register(ChatMessage.TAG,
            (Connection c, Element e) -> chat(e));
        register(ChooseFoundingFatherMessage.TAG,
            (Connection c, Element e) -> chooseFoundingFather(e));
        register(TrivialMessage.CLOSE_MENUS_TAG,
            (Connection c, Element e) -> closeMenus());
        register(DiplomacyMessage.TAG,
            (Connection c, Element e) -> diplomacy(e));
        register(TrivialMessage.DISCONNECT_TAG,
            (Connection c, Element e) -> disconnect(e));
        register(ErrorMessage.TAG,
            (Connection c, Element e) -> error(e));
        register("featureChange",
            (Connection c, Element e) -> featureChange(e));
        register(FirstContactMessage.TAG,
            (Connection c, Element e) -> firstContact(e));
        register(FountainOfYouthMessage.TAG,
            (Connection c, Element e) -> fountainOfYouth(e));
        register(GameEndedMessage.TAG,
            (Connection c, Element e) -> gameEnded(e));
        register(HighScoreMessage.TAG,
            (Connection c, Element e) -> highScore(e));
        register(InciteMessage.TAG,
            (Connection c, Element e) -> incite(e));
        register(IndianDemandMessage.TAG,
            (Connection c, Element e) -> indianDemand(e));
        register(LogoutMessage.TAG,
            (Connection c, Element e) -> logout(e));
        register(LootCargoMessage.TAG,
            (Connection c, Element e) -> lootCargo(e));
        register(MonarchActionMessage.TAG,
            (Connection c, Element e) -> monarchAction(e));
        register(MultipleMessage.TAG,
            (Connection c, Element e) -> multiple(c, e));
        register(NationSummaryMessage.TAG,
            (Connection c, Element e) -> nationSummary(e));
        register(NativeTradeMessage.TAG,
            (Connection c, Element e) -> nativeTrade(e));
        register(NewLandNameMessage.TAG,
            (Connection c, Element e) -> newLandName(e));
        register(NewRegionNameMessage.TAG,
            (Connection c, Element e) -> newRegionName(e));
        register(NewTurnMessage.TAG,
            (Connection c, Element e) -> newTurn(e));
        register(NewTradeRouteMessage.TAG,
            (Connection c, Element e) -> newTradeRoute(e));
        register(TrivialMessage.RECONNECT_TAG,
            (Connection c, Element e) -> reconnect());
        register("remove",
            (Connection c, Element e) -> remove(e));
        register(ScoutSpeakToChiefMessage.TAG,
            (Connection c, Element e) -> scoutSpeakToChief(e));
        register(SetAIMessage.TAG,
            (Connection c, Element e) -> setAI(e));
        register(SetCurrentPlayerMessage.TAG,
            (Connection c, Element e) -> setCurrentPlayer(e));
        register(SetDeadMessage.TAG,
            (Connection c, Element e) -> setDead(e));
        register(SetStanceMessage.TAG,
            (Connection c, Element e) -> setStance(e));
        register(SpySettlementMessage.TAG,
            (Connection c, Element e) -> spySettlement(e));
        register(UpdateMessage.TAG,
            (Connection c, Element e) -> update(e));
    }


    /**
     * Shorthand to run in the EDT and wait.
     *
     * @param runnable The {@code Runnable} to run.
     */
    private void invokeAndWait(Runnable runnable) {
        getGUI().invokeNowOrWait(runnable);
    }
    
    /**
     * Shorthand to run in the EDT eventually.
     *
     * @param runnable The {@code Runnable} to run.
     */
    private void invokeLater(Runnable runnable) {
        getGUI().invokeNowOrLater(runnable);
    }
    

    // Override ClientInputHandler

    /**
     * {@inheritDoc}
     */
    @Override
    public Element handle(Connection connection, Element element) {
        if (element == null) return null;
        Element reply = super.handle(connection, element);

        if (currentPlayerIsMyPlayer()) {
            // Play a sound if specified
            String sound = DOMUtils.getStringAttribute(element, "sound");
            if (sound != null && !sound.isEmpty()) {
                getGUI().playSound(sound);
            }
            // If there is a "flush" attribute present, encourage the
            // client to display any new messages.
            if (DOMUtils.getBooleanAttribute(element, "flush", false)) {
                invokeLater(displayModelMessagesRunnable);
            }
        }
        return reply;
    }


    // Individual message handlers

    /**
     * Handle an "addPlayer"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void addPlayer(Element element) {
        new AddPlayerMessage(getGame(), element);
    }

    /**
     * Handle an "animateAttack"-message.  This only performs animation, if
     * required.  It does not actually perform any attacks.
     *
     * @param element The {@code Element} to process.
     */
    private void animateAttack(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final AnimateAttackMessage message
            = new AnimateAttackMessage(game, element);
        final Unit attacker = message.getAttacker(game);
        final Unit defender = message.getDefender(game);
        final Tile attackerTile = message.getAttackerTile(game);
        final Tile defenderTile = message.getDefenderTile(game);
        final boolean result = message.getResult();

        if (attacker == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " missing attacker.");
        }
        if (defender == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " omitted defender.");
        }
        if (attackerTile == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " omitted attacker tile.");
        }
        if (defenderTile == null) {
            throw new IllegalStateException("Attack animation for: "
                + player.getId() + " omitted defender tile.");
        }

        invokeAndWait(() ->
            igc().animateAttack(attacker, defender,
                                attackerTile, defenderTile, result));
    }

    /**
     * Handle an "animateMove"-message.  This only performs
     * animation, if required.  It does not actually change unit
     * positions, which happens in an "update".
     *
     * @param element The {@code Element} to process.
     */
    private void animateMove(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final AnimateMoveMessage message
            = new AnimateMoveMessage(game, element);
        final Unit unit = message.getUnit(game);
        final Tile oldTile = message.getOldTile(game);
        final Tile newTile = message.getNewTile(game);

        if (unit == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing Unit.");
            return;
        }
        if (oldTile == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing old Tile.");
            return;
        }
        if (newTile == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing new Tile.");
            return;
        }

        invokeAndWait(() ->
            igc().animateMove(unit, oldTile, newTile));
    }

    /**
     * Handle a "chat"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void chat(Element element) {
        final Game game = getGame();
        final ChatMessage message = new ChatMessage(game, element);
        final Player player = message.getPlayer(game);
        final String text = message.getMessage();
        final boolean isPrivate = message.isPrivate();

        invokeLater(() ->
            igc().chat(player, text, isPrivate));
    }

    /**
     * Handle an "chooseFoundingFather"-request.
     *
     * @param element The {@code Element} to process.
     */
    private void chooseFoundingFather(Element element) {
        final Game game = getGame();
        final ChooseFoundingFatherMessage message
            = new ChooseFoundingFatherMessage(game, element);
        final List<FoundingFather> fathers = message.getFathers(game);
        
        invokeLater(() ->
            igc().chooseFoundingFather(fathers));
    }

    /**
     * Trivial handler to allow the server to signal to the client
     * that an offer that caused a popup (for example, a native demand
     * or diplomacy proposal) has not been answered quickly enough and
     * that the offering player has assumed this player has
     * refused-by-inaction, and therefore, the popup needs to be
     * closed.
     */
    private void closeMenus() {
        invokeAndWait(closeMenusRunnable);
    }

    /**
     * Handle a "diplomacy"-request.  If the message informs of an
     * acceptance or rejection then display the result and return
     * null.  If the message is a proposal, then ask the user about
     * it and return the response with appropriate response set.
     *
     * @param element The {@code Element} to process.
     */
    private void diplomacy(Element element) {
        final Game game = getGame();
        final DiplomacyMessage message
            = new DiplomacyMessage(getGame(), element);
        final DiplomaticTrade agreement = message.getAgreement();
        final FreeColGameObject our = message.getOtherFCGO(game);
        final FreeColGameObject other = message.getOurFCGO(game);

        // Note incoming message will have ownership transposed as it
        // is their proposal.
        if (our == null) {
            logger.warning("Our FCGO omitted from diplomacy message.");
            return;
        }
        if (other == null) {
            logger.warning("Other FCGO omitted from diplomacy message.");
            return;
        }

        invokeLater(() ->
            igc().diplomacy(our, other, agreement));
    }

    /**
     * Handle an "error"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void error(Element element) {
        final ErrorMessage errorMessage = new ErrorMessage(getGame(), element);

        invokeLater(() ->
            igc().error(errorMessage.getTemplate(),
                        errorMessage.getMessage()));
    }

    /**
     * Adds a feature to or removes a feature from a FreeColGameObject.
     *
     * @param element The {@code Element} to process.
     */
    private void featureChange(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final boolean add = DOMUtils.getBooleanAttribute(element, "add", false);
        final String id = DOMUtils.readId(element);
        final FreeColGameObject parent = game.getFreeColGameObject(id);
        if (parent == null) {
            logger.warning("featureChange with null object");
            return;
        }

        DOMUtils.mapChildren(element, (e) -> {
                final String tag = DOMUtils.getType(e);
                switch (tag) {
                case Ability.TAG:
                    Ability a = DOMUtils.readElement(game, e, false, Ability.class);
                    if (add) {
                        parent.addAbility(a);
                    } else {
                        parent.removeAbility(a);
                    }
                    break;
                case Modifier.TAG:
                    Modifier m = DOMUtils.readElement(game, e, false, Modifier.class);
                    if (add) {
                        parent.addModifier(m);
                    } else {
                        parent.removeModifier(m);
                    }
                    break;
                case HistoryEvent.TAG:
                    if (parent instanceof Player && add) {
                        HistoryEvent he = DOMUtils.readElement(game, e, true, HistoryEvent.class);
                        Player player = (Player)parent;
                        player.addHistory(he);
                    } else {
                        logger.warning(tag + " feature change failure: "
                            + parent + "/" + add);
                    }
                    break;
                case LastSale.TAG:
                    if (parent instanceof Player && add) {
                        LastSale ls = DOMUtils.readElement(game, e, true, LastSale.class);
                        Player player = (Player)parent;
                        player.addLastSale(ls);
                    } else {
                        logger.warning(tag + " feature change failure: "
                            + parent + "/" + add);
                    }
                    break;
                case ModelMessage.TAG:
                    if (parent instanceof Player && add) {
                        ModelMessage mm = DOMUtils.readElement(game, e, true, ModelMessage.class);
                        Player player = (Player)parent;
                        player.addModelMessage(mm);
                    } else {
                        logger.warning(tag + " feature change failure: "
                            + parent + "/" + add);
                    }
                    break;
                case TradeRoute.TAG:
                    if (parent instanceof Player) {
                        TradeRoute tr = DOMUtils.readElement(game, e, true, TradeRoute.class);
                        Player player = (Player)parent;
                        if (add) {
                            player.addTradeRoute(tr);
                        } else {
                            player.removeTradeRoute(tr);
                        }
                    } else {
                        logger.warning(tag + " feature change failure: "
                            + parent + "/" + add);
                    }
                    break;
                    
                default:
                    logger.warning("featureChange unrecognized: " + tag);
                    break;
                }
                return null;
            });
    }

    /**
     * Handle a first contact with a native nation.
     *
     * @param element The {@code Element} to process.
     */
    private void firstContact(Element element) {
        final Game game = getGame();
        final FirstContactMessage message
            = new FirstContactMessage(game, element);
        final Player player = message.getPlayer(game);
        final Player other = message.getOtherPlayer(game);
        final Tile tile = message.getTile(game);
        final int n = message.getSettlementCount();

        if (player == null || player != getMyPlayer()) {
            logger.warning("firstContact with bad player: " + player);
            return;
        }
        if (other == null || other == player || !other.isIndian()) {
            logger.warning("firstContact with bad other player: " + other);
            return;
        }
        if (tile != null && tile.getOwner() != other) {
            logger.warning("firstContact with bad tile: " + tile);
            return;
        }

        invokeLater(() ->
            igc().firstContact(player, other, tile, n));
    }

    /**
     * Ask the player to choose migrants from a fountain of youth event.
     *
     * @param element The {@code Element} to process.
     */
    private void fountainOfYouth(Element element) {
        final Game game = getGame();
        final FountainOfYouthMessage message
            = new FountainOfYouthMessage(game, element);
        final int n = message.getMigrants();

        if (n <= 0) {
            logger.warning("Invalid migrants attribute: " + n);
            return;
        }

        invokeLater(() ->
            igc().fountainOfYouth(n));
    }

    /**
     * Handle a "gameEnded"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void gameEnded(Element element) {
        final Game game = getGame();
        final GameEndedMessage message = new GameEndedMessage(game, element);
        final Player winner = message.getWinner(game);
        final String highScore = message.getScore();

        if (winner == null) {
            logger.warning("Invalid player for gameEnded");
            return;
        }
        FreeColDebugger.finishDebugRun(getFreeColClient(), true);
        if (winner != getMyPlayer()) return;
        
        invokeLater(() ->
            igc().victory(highScore));
    }

    /**
     * Handle a "highScore" message.
     *
     * @param element The {@code Element} to process.
     */
    private void highScore(Element element) {
        final Game game = getGame();
        final HighScoreMessage message
            = new HighScoreMessage(game, element);

        invokeLater(() ->
            igc().displayHighScores(message.getKey(), message.getScores()));
    }
        
    /**
     * Handle an "incite" message.
     *
     * @param element The {@code Element} to process.
     */
    private void incite(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final InciteMessage message = new InciteMessage(game, element);
        final Unit unit = message.getUnit(player);
        final IndianSettlement is = message.getSettlement(unit);
        final Player enemy = message.getEnemy(game);
        final int gold = message.getGold();
        
        invokeLater(() ->
            igc().incite(unit, is, enemy, gold));
    }
    
    /**
     * Handle an "indianDemand"-request.
     *
     * @param element The {@code Element} to process.
     */
    private void indianDemand(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final IndianDemandMessage message
            = new IndianDemandMessage(game, element);
        final Unit unit = message.getUnit(game);
        final Colony colony = message.getColony(game);
        final GoodsType goodsType = message.getType(game);
        final int amount = message.getAmount();
        
        if (unit == null) {
            logger.warning("IndianDemand with null unit.");
            return;
        }
        if (colony == null) {
            logger.warning("IndianDemand with null colony");
            return;
        } else if (!player.owns(colony)) {
            throw new IllegalArgumentException("Demand to anothers colony");
        }

        invokeLater(() ->
            igc().indianDemand(unit, colony, goodsType, amount));
    }

    /**
     * Handle a logout message.
     *
     * @param element The {@code Element} to process.
     */
    private void logout(Element element) {
        final Game game = getGame();
        final LogoutMessage message = new LogoutMessage(game, element);
        final Player player = message.getPlayer(game);
        final LogoutReason reason = message.getReason();
        if (player == null) return;

        invokeLater(() ->
            igc().logout(player, reason));
    }

    /**
     * Ask the player to choose something to loot.
     *
     * @param element The {@code Element} to process.
     */
    private void lootCargo(Element element) {
        final Game game = getGame();
        final LootCargoMessage message = new LootCargoMessage(game, element);
        final Unit unit = message.getUnit(game);
        final String defenderId = message.getDefenderId();
        final List<Goods> goods = message.getGoods();

        if (unit == null || goods == null) return;

        invokeLater(() ->
            igc().loot(unit, goods, defenderId));
    }

    /**
     * Handle a "monarchAction"-request.
     *
     * @param element The {@code Element} to process.
     */
    private void monarchAction(Element element) {
        final Game game = getGame();
        final MonarchActionMessage message
            = new MonarchActionMessage(game, element);
        final StringTemplate template = message.getTemplate();
        final String key = message.getMonarchKey();
        
        invokeLater(() ->
            igc().monarch(message.getAction(), template, key));
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The {@code Connection} the element arrived on.
     * @param element The {@code Element} to process.
     */
    private void multiple(Connection connection, Element element) {
        Element result = new MultipleMessage(element).applyHandler(this, connection);
        if (result != null) {
            logger.warning("Multiple message -> " + result.getTagName());
        }
    }

    /**
     * Handle an incoming nation summary.
     *
     * @param element The {@code Element} to process.
     */
    private void nationSummary(Element element) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final NationSummaryMessage message
            = new NationSummaryMessage(game, element);
        final Player other = message.getPlayer(game);
        final NationSummary ns = message.getNationSummary();

        player.putNationSummary(other, ns);
        logger.info("Updated nation summary of " + other.getSuffix()
            + " for " + player.getSuffix() + " with " + ns);
    }

    /**
     * Handle a native trade update.
     *
     * @param element The {@code Element} to process.
     */
    private void nativeTrade(Element element) {
        final Game game = getGame();
        final NativeTradeMessage message
            = new NativeTradeMessage(game, element);
        final NativeTradeAction action = message.getAction();
        final NativeTrade nt = message.getNativeTrade();

        invokeLater(() ->
            igc().nativeTrade(action, nt));
    }

    /**
     * Ask the player to name the new land.
     *
     * @param element The {@code Element} to process.
     */
    private void newLandName(Element element) {
        final Game game = getGame();
        NewLandNameMessage message = new NewLandNameMessage(game, element);
        final Unit unit = message.getUnit(getMyPlayer());
        final String defaultName = message.getNewLandName();

        if (unit == null || defaultName == null || !unit.hasTile()) return;

        invokeLater(() ->
            igc().newLandName(defaultName, unit));
    }

    /**
     * Ask the player to name a new region.
     *
     * @param element The {@code Element} to process.
     */
    private void newRegionName(Element element) {
        final Game game = getGame();
        NewRegionNameMessage message = new NewRegionNameMessage(game, element);
        final Tile tile = message.getTile(game);
        final Unit unit = message.getUnit(getMyPlayer());
        final Region region = message.getRegion(game);
        final String defaultName = message.getNewRegionName();

        if (defaultName == null || region == null) return;

        invokeLater(() ->
            igc().newRegionName(region, defaultName, tile, unit));
    }

    /**
     * Handle a "newTradeRoute" message.
     *
     * @param element The {@code Element} to process.
     */
    private void newTradeRoute(Element element) {
        final Game game = getGame();
        final NewTradeRouteMessage message
            = new NewTradeRouteMessage(game, element);
        final Player player = getMyPlayer();
        final TradeRoute tr = message.getTradeRoute();

        if (player != null && tr != null) player.addTradeRoute(tr);
    }
        
    /**
     * Handle a "newTurn"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void newTurn(Element element) {
        final Game game = getGame();
        final NewTurnMessage message = new NewTurnMessage(game, element);
        final int n = message.getTurnNumber();

        if (n < 0) {
            logger.warning("Invalid turn for newTurn");
            return;
        }

        invokeLater(() ->
            igc().newTurn(n));
    }

    /**
     * Handle an "reconnect"-message.
     */
    private void reconnect() {
        // "reconnect" is sent when the server finds a severe problem that
        // is likely to be localized to this client.  Call the controller
        // reconnect dialog that gives the user the option to reload itself
        // or quit.
        invokeLater(reconnectRunnable);
    }

    /**
     * Handle a "remove"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void remove(Element element) {
        final Game game = getGame();
        final FreeColGameObject divert
            = game.getFreeColGameObject(element.getAttribute("divert"));
        final List<FreeColGameObject> objects = new ArrayList<>();

        DOMUtils.mapChildren(element, (e) -> {
                final String id = DOMUtils.readId(e);
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
    }

    /**
     * Handle a "scoutSpeakToChief"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void scoutSpeakToChief(Element element) {
        final Game game = getGame();
        final ScoutSpeakToChiefMessage message
            = new ScoutSpeakToChiefMessage(game, element);
        final Unit unit = message.getUnit(game);
        final IndianSettlement is = message.getSettlement(game);
        final String result = message.getResult();

        invokeLater(() ->
            igc().scoutSpeakToChief(unit, is, result));
    }

    /**
     * Handle a "setAI"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void setAI(Element element) {
        final Game game = getGame();
        final SetAIMessage message = new SetAIMessage(game, element);
        final Player p = message.getPlayer(game);
        final boolean ai = message.getAI();

        if (p != null) p.setAI(ai);
    }

    /**
     * Handle a "setCurrentPlayer"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void setCurrentPlayer(Element element) {
        final Game game = getGame();
        final SetCurrentPlayerMessage message
            = new SetCurrentPlayerMessage(game, element);
        final Player player = message.getPlayer(game);

        if (player == null) {
            logger.warning("Invalid player for setCurrentPlayer");
            return;
        }

        igc().setCurrentPlayer(player); // It is safe to call this one directly
    }

    /**
     * Handle a "setDead"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void setDead(Element element) {
        final Game game = getGame();
        final SetDeadMessage message = new SetDeadMessage(game, element);
        final Player player = message.getPlayer(game);

        if (player == null) {
            logger.warning("Invalid player for setDead");
            return;
        }

        invokeLater(() ->
            igc().setDead(player));
    }

    /**
     * Handle a "setStance"-request.
     *
     * @param element The {@code Element} to process.
     */
    private void setStance(Element element) {
        final Game game = getGame();
        final SetStanceMessage message = new SetStanceMessage(game, element);
        final Stance stance = message.getStance();
        final Player p1 = message.getFirstPlayer(game);
        final Player p2 = message.getSecondPlayer(game);

        if (p1 == null) {
            logger.warning("Invalid player1 for setStance");
            return;
        }
        if (p2 == null) {
            logger.warning("Invalid player2 for setStance");
            return;
        }

        invokeLater(() ->
            igc().setStance(stance, p1, p2));
    }

    /**
     * Handle a "spyResult" message.
     *
     * @param element The {@code Element} to process.
     */
    private void spySettlement(Element element) {
        final Game game = getGame();
        final SpySettlementMessage message
            = new SpySettlementMessage(game, element);
        final Tile spyTile = message.getSpyTile();

        invokeLater(() ->
            igc().spyColony(spyTile));
    }

    /**
     * Handle an "update"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void update(Element element) {
        final Player player = getMyPlayer();
        final Game game = getGame();
        final UpdateMessage message = new UpdateMessage(game, element);

        boolean visibilityChange = false;
        for (FreeColGameObject fcgo : message.getObjects()) {
            if ((fcgo instanceof Player && (fcgo == player))
                || ((fcgo instanceof Ownable) && player.owns((Ownable)fcgo))) {
                visibilityChange = true;//-vis(player)
            }
        }
        if (visibilityChange) player.invalidateCanSeeTiles();//+vis(player)
    }
}
