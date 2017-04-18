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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.AnimateAttackMessage;
import net.sf.freecol.common.networking.AnimateMoveMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.CloseMenusMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeleteTradeRouteMessage;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DOMMessageHandler;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.FeatureChangeMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.FountainOfYouthMessage;
import net.sf.freecol.common.networking.GameEndedMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NativeGiftMessage;
import net.sf.freecol.common.networking.NativeTradeMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.NewTradeRouteMessage;
import net.sf.freecol.common.networking.NewTurnMessage;
import net.sf.freecol.common.networking.ReconnectMessage;
import net.sf.freecol.common.networking.RemoveMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.SetAIMessage;
import net.sf.freecol.common.networking.SetCurrentPlayerMessage;
import net.sf.freecol.common.networking.SetDeadMessage;
import net.sf.freecol.common.networking.SetStanceMessage;
import net.sf.freecol.common.networking.StartGameMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.FreeColServerHolder;
import net.sf.freecol.server.model.ServerPlayer;
import static net.sf.freecol.common.util.CollectionUtils.*;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives while in the game.
 */
public final class AIInGameInputHandler extends FreeColServerHolder
    implements MessageHandler, DOMMessageHandler {

    private static final Logger logger = Logger.getLogger(AIInGameInputHandler.class.getName());

    /** The player for whom I work. */
    private final ServerPlayer serverPlayer;

    /** The main AI object. */
    private final AIMain aiMain;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main server.
     * @param serverPlayer The {@code ServerPlayer} that is being
     *     managed by this AIInGameInputHandler.
     * @param aiMain The main AI-object.
     */
    public AIInGameInputHandler(FreeColServer freeColServer,
                                ServerPlayer serverPlayer,
                                AIMain aiMain) {
        super(freeColServer);
        
        if (serverPlayer == null) {
            throw new NullPointerException("serverPlayer == null");
        } else if (!serverPlayer.isAI()) {
            throw new RuntimeException("Applying AIInGameInputHandler to a non-AI player!");
        } else if (aiMain == null) {
            throw new NullPointerException("aiMain == null");
        }

        this.serverPlayer = serverPlayer;
        this.aiMain = aiMain;
    }


    /**
     * Get player using this handler.
     *
     * @return The {@code ServerPlayer}.
     */
    private ServerPlayer getMyPlayer() {
        return this.serverPlayer;
    }

    /**
     * Get the AI player using this handler.
     *
     * @return The {@code AIPlayer}.
     */
    private AIPlayer getMyAIPlayer() {
        return this.aiMain.getAIPlayer(this.serverPlayer);
    }

    /**
     * Get the AI main in this handler.
     *
     * @return The {@code AIMain}.
     */
    private AIMain getAIMain() {
        return this.aiMain;
    }


    // Implement DOMMessageHandler

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Element handle(Connection connection, Element element) {
        if (element == null) return null;

        final Game game = getGame();
        final String tag = element.getTagName();
        try {
            switch (tag) {
            case ChooseFoundingFatherMessage.TAG:
                new ChooseFoundingFatherMessage(game, element)
                    .aiHandler(getFreeColServer(), getMyAIPlayer());
                break;
            case DiplomacyMessage.TAG:
                new DiplomacyMessage(game, element)
                    .aiHandler(getFreeColServer(), getMyAIPlayer());
                break;
            case FirstContactMessage.TAG:
                new FirstContactMessage(game, element)
                    .aiHandler(getFreeColServer(), getMyAIPlayer());
                break;
            case FountainOfYouthMessage.TAG:
                new FountainOfYouthMessage(game, element)
                    .aiHandler(getFreeColServer(), getMyAIPlayer());
                break;
            case IndianDemandMessage.TAG:
                indianDemand(new IndianDemandMessage(game, element));
                break;
            case LootCargoMessage.TAG:
                lootCargo(new LootCargoMessage(game, element));
                break;
            case MonarchActionMessage.TAG:
                monarchAction(new MonarchActionMessage(game, element));
                break;
            case MultipleMessage.TAG:
                multiple(connection, element);
                break;
            case NationSummaryMessage.TAG:
                nationSummary(new NationSummaryMessage(game, element));
                break;
            case NativeTradeMessage.TAG:
                nativeTrade(new NativeTradeMessage(game, element));
                break;
            case NewLandNameMessage.TAG:
                newLandName(new NewLandNameMessage(game, element));
                break;
            case NewRegionNameMessage.TAG:
                newRegionName(new NewRegionNameMessage(game, element));
                break;
            case ReconnectMessage.TAG:
                logger.info("Reconnect on illegal operation.");
                break;
            case SetAIMessage.TAG:
                setAI(new SetAIMessage(game, element));
                break;
            case SetCurrentPlayerMessage.TAG:
                setCurrentPlayer(new SetCurrentPlayerMessage(game, element));
                break;
                
            // Since we're the server, we can see everything.
            // Therefore most of these messages are useless.
            // This may change one day.
            case AddPlayerMessage.TAG:
            case AnimateAttackMessage.TAG:
            case AnimateMoveMessage.TAG:
            case AssignTradeRouteMessage.TAG:
            case ChatMessage.TAG:
            case CloseMenusMessage.TAG:
            case DeleteTradeRouteMessage.TAG:
            case DisconnectMessage.TAG:
            case ErrorMessage.TAG:
            case FeatureChangeMessage.TAG:
            case GameEndedMessage.TAG:
            case LogoutMessage.TAG: // Ignored, AIs do not log out
            case NativeGiftMessage.TAG:
            case NewTurnMessage.TAG:
            case NewTradeRouteMessage.TAG:
            case RemoveMessage.TAG:
            case ScoutSpeakToChiefMessage.TAG:
            case SetDeadMessage.TAG:
            case SetStanceMessage.TAG:
            case StartGameMessage.TAG:
            case UpdateMessage.TAG:
                break;
            default:
                logger.warning("Unknown message type: " + tag);
                break;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI input handler for " + getMyAIPlayer()
                + " caught error handling " + tag, e);
        }
        return null;
    }

    // Individual message handlers

    /**
     * Handles an "indianDemand"-message.
     *
     * @param message The {@code IndianDemandMessage} to process.
     */
    private void indianDemand(IndianDemandMessage message) {
        final Game game = getGame();
        final AIPlayer aiPlayer = getMyAIPlayer();
        final Unit unit = message.getUnit(game);
        final Colony colony = message.getColony(game);
        final GoodsType type = message.getType(game);
        final int amount = message.getAmount();
        final Boolean initialResult = message.getResult();

        Boolean result = aiPlayer.indianDemand(unit, colony, type, amount,
                                               initialResult);
        logger.finest("AI handling native demand by " + unit
            + " at " + colony + " result: " + initialResult + " -> " + result);
        if (result != null) {
            aiPlayer.invoke(() -> {
                    AIMessage.askIndianDemand(aiPlayer, unit, colony,
                                              type, amount, result);
                });
        }
    }

    /**
     * Replies to loot cargo offer.
     *
     * @param message The {@code LootCargoMessage} to process.
     */
    private void lootCargo(LootCargoMessage message) {
        final Game game = getGame();
        final Market market = getMyPlayer().getMarket();
        final Unit unit = message.getUnit(game);
        final List<Goods> initialGoods = message.getGoods();
        final String defenderId = message.getDefenderId();

        getMyAIPlayer().invoke(() -> {
                List<Goods> goods = sort(initialGoods,
                                         market.getSalePriceComparator());
                List<Goods> loot = new ArrayList<>();
                int space = unit.getSpaceLeft();
                while (!goods.isEmpty() && space > 0) {
                    Goods g = goods.remove(0);
                    if (g.getSpaceTaken() > space) continue; // Approximate
                    loot.add(g);
                    space -= g.getSpaceTaken();
                }
                AIMessage.askLoot(getAIMain().getAIUnit(unit), defenderId, loot);
            });
    }

    /**
     * Handles a "monarchAction"-message.
     *
     * @param message The {@code MonarchActionMessage} to process.
     */
    private void monarchAction(MonarchActionMessage message) {
        final AIPlayer aiPlayer = getMyAIPlayer();
        final MonarchAction action = message.getAction();

        boolean accept;
        switch (action) {
        case RAISE_TAX_WAR: case RAISE_TAX_ACT:
            accept = getMyAIPlayer().acceptTax(message.getTax());
            logger.finest("AI player monarch action " + action
                          + " = " + accept);
            break;

        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            accept = getMyAIPlayer().acceptMercenaries();
            logger.finest("AI player monarch action " + action
                          + " = " + accept);
            break;

        default:
            logger.finest("AI player ignoring monarch action " + action);
            return;
        }

        aiPlayer.invoke(() -> {
                AIMessage.askMonarchAction(aiPlayer, action, accept);
            });
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The {@code Connection} the element arrived on.
     * @param element The {@code Element} to process.
     */
    private void multiple(Connection connection, Element element) {
        new MultipleMessage(element).applyHandler(this, connection);
    }

    /**
     * Handle an incoming nation summary.
     *
     * @param message The {@code NationSummaryMessage} to process.
     */
    private void nationSummary(NationSummaryMessage message) {
        final AIPlayer aiPlayer = getMyAIPlayer();
        final Player player = aiPlayer.getPlayer();
        final Player other = message.getPlayer(getGame());
        final NationSummary ns = message.getNationSummary();

        player.putNationSummary(other, ns);
        logger.info("Updated nation summary of " + other.getSuffix()
            + " for AI " + player.getSuffix());
    }

    /**
     * Handle a native trade message.
     *
     * @param message The {@code NativeTradeMessage} to process.
     */
    private void nativeTrade(NativeTradeMessage message) {
        final AIPlayer aiPlayer = getMyAIPlayer();
        final NativeTrade nt = message.getNativeTrade();
        final NativeTradeAction action = message.getAction();

        NativeTradeAction result = aiPlayer.handleTrade(action, nt);
        aiPlayer.invoke(() -> {
                AIMessage.askNativeTrade(aiPlayer, result, nt);
            });
    }

    /**
     * Replies to offer to name the new land.
     *
     * @param message The {@code NewLandNameMessage} to process.
     */
    private void newLandName(NewLandNameMessage message) {
        final AIPlayer aiPlayer = getMyAIPlayer();
        final Unit unit = message.getUnit(aiPlayer.getPlayer());
        final String name = message.getNewLandName();

        aiPlayer.invoke(() -> {
                AIMessage.askNewLandName(aiPlayer, unit, name);
            });
    }

    /**
     * Replies to offer to name a new region name.
     *
     * @param message The {@code NewRegionNameMessage} to process.
     */
    private void newRegionName(NewRegionNameMessage message) {
        final AIPlayer aiPlayer = getMyAIPlayer();
        final Game game = getGame();
        final Region region = message.getRegion(game);
        final Tile tile = message.getTile(game);
        final Unit unit = message.getUnit(aiPlayer.getPlayer());
        final String name = message.getNewRegionName();

        aiPlayer.invoke(() -> {
                AIMessage.askNewRegionName(aiPlayer, region, tile, unit, name);
            });
    }

    /**
     * Handle a "setAI"-message.
     *
     * @param message The {@code SetAIMessage} to process.
     */
    private void setAI(SetAIMessage message) {
        final Player p = message.getPlayer(getGame());
        final boolean ai = message.getAI();

        if (p != null) p.setAI(ai);
    }

    /**
     * Handles a "setCurrentPlayer"-message.
     *
     * @param message The {@code SetCurrentPlayerMessage} to process.
     */
    private void setCurrentPlayer(SetCurrentPlayerMessage message) {
        final Player currentPlayer = message.getPlayer(getGame());

        if (currentPlayer != null
            && getMyPlayer().getId().equals(currentPlayer.getId())) {
            getMyAIPlayer().invoke(() -> {
                    getMyAIPlayer().startWorking();
                    AIMessage.askEndTurn(getMyAIPlayer());
                });
        }
    }

    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Message message) throws FreeColException {
        message.aiHandler(getFreeColServer(), getMyAIPlayer());
        return null; 
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), connection.getFreeColXMLReader());
    }
}
