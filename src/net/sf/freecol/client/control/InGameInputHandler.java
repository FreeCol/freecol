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
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
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
import net.sf.freecol.common.networking.CloseMenusMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.FeatureChangeMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.FountainOfYouthMessage;
import net.sf.freecol.common.networking.GameEndedMessage;
import net.sf.freecol.common.networking.HighScoreMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
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

    private final Runnable displayModelMessagesRunnable = () -> {
        igc().displayModelMessages(false);
    };


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public InGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(AddPlayerMessage.TAG,
            (Connection c, Element e) ->
                addPlayer(new AddPlayerMessage(getGame(), e)));
        register(AnimateAttackMessage.TAG,
            (Connection c, Element e) ->
                animateAttack(new AnimateAttackMessage(getGame(), e)));
        register(AnimateMoveMessage.TAG,
            (Connection c, Element e) ->
                animateMove(new AnimateMoveMessage(getGame(), e)));
        register(ChatMessage.TAG,
            (Connection c, Element e) ->
                new ChatMessage(getGame(), e).clientHandler(freeColClient));
        register(ChooseFoundingFatherMessage.TAG,
            (Connection c, Element e) ->
                chooseFoundingFather(new ChooseFoundingFatherMessage(getGame(), e)));
        register(CloseMenusMessage.TAG,
            (Connection c, Element e) ->
                TrivialMessage.closeMenusMessage.clientHandler(freeColClient));
        register(DiplomacyMessage.TAG,
            (Connection c, Element e) ->
                diplomacy(new DiplomacyMessage(getGame(), e)));
        register(DisconnectMessage.TAG,
            (Connection c, Element e) ->
                TrivialMessage.disconnectMessage.clientHandler(freeColClient));
        register(ErrorMessage.TAG,
            (Connection c, Element e) ->
                error(new ErrorMessage(getGame(), e)));
        register(FeatureChangeMessage.TAG,
            (Connection c, Element e) ->
                featureChange(new FeatureChangeMessage(getGame(), e)));
        register(FirstContactMessage.TAG,
            (Connection c, Element e) ->
                new FirstContactMessage(getGame(), e).clientHandler(freeColClient));
        register(FountainOfYouthMessage.TAG,
            (Connection c, Element e) ->
                fountainOfYouth(new FountainOfYouthMessage(getGame(), e)));
        register(GameEndedMessage.TAG,
            (Connection c, Element e) ->
                new GameEndedMessage(getGame(), e).clientHandler(freeColClient));
        register(HighScoreMessage.TAG,
            (Connection c, Element e) ->
                highScore(new HighScoreMessage(getGame(), e)));
        register(InciteMessage.TAG,
            (Connection c, Element e) ->
                new InciteMessage(getGame(), e).clientHandler(freeColClient));
        register(IndianDemandMessage.TAG,
            (Connection c, Element e) ->
                indianDemand(new IndianDemandMessage(getGame(), e)));
        register(LogoutMessage.TAG,
            (Connection c, Element e) ->
                new LogoutMessage(getGame(), e).clientHandler(freeColClient));
        register(LootCargoMessage.TAG,
            (Connection c, Element e) ->
                lootCargo(new LootCargoMessage(getGame(), e)));
        register(MonarchActionMessage.TAG,
            (Connection c, Element e) ->
                monarchAction(new MonarchActionMessage(getGame(), e)));
        register(NationSummaryMessage.TAG,
            (Connection c, Element e) ->
                nationSummary(new NationSummaryMessage(getGame(), e)));
        register(NativeTradeMessage.TAG,
            (Connection c, Element e) ->
                nativeTrade(new NativeTradeMessage(getGame(), e)));
        register(NewLandNameMessage.TAG,
            (Connection c, Element e) ->
                new NewLandNameMessage(getGame(), e).clientHandler(freeColClient));
        register(NewRegionNameMessage.TAG,
            (Connection c, Element e) ->
                newRegionName(new NewRegionNameMessage(getGame(), e)));
        register(NewTurnMessage.TAG,
            (Connection c, Element e) ->
                newTurn(new NewTurnMessage(getGame(), e)));
        register(NewTradeRouteMessage.TAG,
            (Connection c, Element e) ->
                newTradeRoute(new NewTradeRouteMessage(getGame(), e)));
        register(ReconnectMessage.TAG,
            (Connection c, Element e) ->
                TrivialMessage.reconnectMessage.clientHandler(freeColClient));
        register(RemoveMessage.TAG,
            (Connection c, Element e) ->
                new RemoveMessage(getGame(), e).clientHandler(freeColClient));
        register(ScoutSpeakToChiefMessage.TAG,
            (Connection c, Element e) ->
                scoutSpeakToChief(new ScoutSpeakToChiefMessage(getGame(), e)));
        register(SetAIMessage.TAG,
            (Connection c, Element e) ->
                new SetAIMessage(getGame(), e).clientHandler(freeColClient));
        register(SetCurrentPlayerMessage.TAG,
            (Connection c, Element e) ->
                new SetCurrentPlayerMessage(getGame(), e).clientHandler(freeColClient));
        register(SetDeadMessage.TAG,
            (Connection c, Element e) ->
                setDead(new SetDeadMessage(getGame(), e)));
        register(SetStanceMessage.TAG,
            (Connection c, Element e) ->
                new SetStanceMessage(getGame(), e).clientHandler(freeColClient));
        register(SpySettlementMessage.TAG,
            (Connection c, Element e) ->
                spySettlement(new SpySettlementMessage(getGame(), e)));
        register(UpdateMessage.TAG,
            (Connection c, Element e) ->
                update(new UpdateMessage(getGame(), e)));
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
    public Element handle(Connection connection, Element element)
        throws FreeColException {
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
     * @param message The {@code AddPlayerMessage} to process.
     */
    private void addPlayer(AddPlayerMessage message) {
        // Do not need to do anything, reading the player in does
        // enough for now.
    }

    /**
     * Handle an "animateAttack"-message.
     *
     * @param message The {@code AnimateAttackMessage} to process.
     */
    private void animateAttack(AnimateAttackMessage message) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final Unit attacker = message.getAttacker(game);
        final Unit defender = message.getDefender(game);
        final Tile attackerTile = message.getAttackerTile(game);
        final Tile defenderTile = message.getDefenderTile(game);
        final boolean result = message.getResult();

        if (attacker == null) {
            logger.warning("Attack animation for: " + player.getId()
                + " missing attacker.");
        }
        if (defender == null) {
            logger.warning("Attack animation for: " + player.getId()
                + " omitted defender.");
        }
        if (attackerTile == null) {
            logger.warning("Attack animation for: " + player.getId()
                + " omitted attacker tile.");
        }
        if (defenderTile == null) {
            logger.warning("Attack animation for: " + player.getId()
                + " omitted defender tile.");
        }

        // This only performs animation, if required.  It does not
        // actually perform an attack.
        invokeAndWait(() ->
            igc().animateAttack(attacker, defender,
                                attackerTile, defenderTile, result));
    }

    /**
     * Handle an "animateMove"-message.
     *
     * @param message The {@code AnimateMoveMessage} to process.
     */
    private void animateMove(AnimateMoveMessage message) {
        final Game game = getGame();
        final Player player = getMyPlayer();
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

        // This only performs animation, if required.  It does not
        // actually change unit positions, which happens in an "update".
        invokeAndWait(() ->
            igc().animateMove(unit, oldTile, newTile));
    }

    /**
     * Handle an "chooseFoundingFather"-request.
     *
     * @param message The {@code ChooseFoundingFatherMessage} to process.
     */
    private void chooseFoundingFather(ChooseFoundingFatherMessage message) {
        final Game game = getGame();
        final List<FoundingFather> fathers = message.getFathers(game);
        
        invokeLater(() ->
            igc().chooseFoundingFather(fathers));
    }

    /**
     * Handle a "diplomacy"-message.
     *
     * @param message The {@code DiplomacyMessage} to process.
     */
    private void diplomacy(DiplomacyMessage message) {
        final Game game = getGame();
        final DiplomaticTrade agreement = message.getAgreement();
        final FreeColGameObject our = message.getOurFCGO(game);
        final FreeColGameObject other = message.getOtherFCGO(game);

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
     * @param message The {@code ErrorMessage} to process.
     */
    private void error(ErrorMessage message) {
        invokeLater(() ->
            igc().error(message.getTemplate(), message.getMessage()));
    }

    /**
     * Handle a "featureChange"-message.
     *
     * @param message The {@code FeatureChangeMessage} to process.
     */
    private void featureChange(FeatureChangeMessage message) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final FreeColGameObject parent = message.getParent(game);
        final List<FreeColObject> children = message.getChildren();
        final boolean add = message.getAdd();

        if (parent == null) {
            logger.warning("featureChange with null parent.");
            return;
        }
        if (children.isEmpty()) {
            logger.warning("featureChange with no children.");
            return;
        }

        // Add or remove a feature from a FreeColGameObject
        for (FreeColObject fco : children) {
            if (fco instanceof Ability) {
                if (add) {
                    parent.addAbility((Ability)fco);
                } else {
                    parent.removeAbility((Ability)fco);
                }
            } else if (fco instanceof Modifier) {
                if (add) {
                    parent.addModifier((Modifier)fco);
                } else {
                    parent.removeModifier((Modifier)fco);
                }
            } else if (fco instanceof HistoryEvent) {
                if (parent instanceof Player && add) {
                    Player player = (Player)parent;
                    player.addHistory((HistoryEvent)fco);
                } else {
                    logger.warning("Feature change NYI: "
                        + parent + "/" + add + "/" + fco);
                }
            } else if (fco instanceof LastSale) {
                if (parent instanceof Player && add) {
                    Player player = (Player)parent;
                    player.addLastSale((LastSale)fco);
                } else {
                    logger.warning("Feature change NYI: "
                        + parent + "/" + add + "/" + fco);
                }
            } else if (fco instanceof ModelMessage) {
                if (parent instanceof Player && add) {
                    Player player = (Player)parent;
                    player.addModelMessage((ModelMessage)fco);
                } else {
                    logger.warning("Feature change NYI: "
                        + parent + "/" + add + "/" + fco);
                }
            } else {        
                logger.warning("featureChange unrecognized: " + fco);
            }
        }
    }

    /**
     * Handle a "fountainOfYouth"-message.
     *
     * @param message The {@code FountainOfYouthMessage} to process.
     */
    private void fountainOfYouth(FountainOfYouthMessage message) {
        final Game game = getGame();
        final int n = message.getMigrants();

        if (n <= 0) {
            logger.warning("Invalid migrants attribute: " + n);
            return;
        }

        invokeLater(() ->
            igc().fountainOfYouth(n));
    }

    /**
     * Handle a "highScore"-message.
     *
     * @param message The {@code HighScoreMessage} to process.
     */
    private void highScore(HighScoreMessage message) {
        final Game game = getGame();

        invokeLater(() ->
            igc().displayHighScores(message.getKey(), message.getScores()));
    }
        
    /**
     * Handle an "indianDemand"-message.
     *
     * @param message The {@code IndianDemandMessage} to process.
     */
    private void indianDemand(IndianDemandMessage message) {
        final Game game = getGame();
        final Player player = getMyPlayer();
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
     * Handle a "lootCargo"-message.
     *
     * @param message The {@code LootCargoMessage} to process.
     */
    private void lootCargo(LootCargoMessage message) {
        final Game game = getGame();
        final Unit unit = message.getUnit(game);
        final String defenderId = message.getDefenderId();
        final List<Goods> goods = message.getGoods();

        if (unit == null || goods == null) return;

        invokeLater(() ->
            igc().loot(unit, goods, defenderId));
    }

    /**
     * Handle a "monarchAction"-message.
     *
     * @param message The {@code MonarchActionMessage} to process.
     */
    private void monarchAction(MonarchActionMessage message) {
        final Game game = getGame();
        final StringTemplate template = message.getTemplate();
        final String key = message.getMonarchKey();
        
        invokeLater(() ->
            igc().monarch(message.getAction(), template, key));
    }

    /**
     * Handle a "nationSummary"-message.
     *
     * @param message The {@code NationSummaryMessage} to process.
     */
    private void nationSummary(NationSummaryMessage message) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final Player other = message.getPlayer(game);
        final NationSummary ns = message.getNationSummary();

        player.putNationSummary(other, ns);
        logger.info("Updated nation summary of " + other.getSuffix()
            + " for " + player.getSuffix() + " with " + ns);
    }

    /**
     * Handle a "nativeTrade"-message.
     *
     * @param message The {@code NativeTradeMessage} to process.
     */
    private void nativeTrade(NativeTradeMessage message) {
        final Game game = getGame();
        final NativeTradeAction action = message.getAction();
        final NativeTrade nt = message.getNativeTrade();

        invokeLater(() ->
            igc().nativeTrade(action, nt));
    }

    /**
     * Handle a "newRegionName"-message.
     *
     * @param message The {@code NewRegionNameMessage} to process.
     */
    private void newRegionName(NewRegionNameMessage message) {
        final Game game = getGame();
        final Tile tile = message.getTile(game);
        final Unit unit = message.getUnit(getMyPlayer());
        final Region region = message.getRegion(game);
        final String defaultName = message.getNewRegionName();

        if (defaultName == null || region == null) return;

        invokeLater(() ->
            igc().newRegionName(region, defaultName, tile, unit));
    }

    /**
     * Handle a "newTradeRoute"-message.
     *
     * @param message The {@code NewTradeRouteMessage} to process.
     */
    private void newTradeRoute(NewTradeRouteMessage message) {
        final Game game = getGame();
        final Player player = getMyPlayer();
        final TradeRoute tr = message.getTradeRoute();

        if (player != null && tr != null) player.addTradeRoute(tr);
    }
        
    /**
     * Handle a "newTurn"-message.
     *
     * @param message The {@code NewTurnMessage} to process.
     */
    private void newTurn(NewTurnMessage message) {
        final Game game = getGame();
        final int n = message.getTurnNumber();

        if (n < 0) {
            logger.warning("Invalid turn for newTurn");
            return;
        }

        invokeLater(() ->
            igc().newTurn(n));
    }

    /**
     * Handle a "scoutSpeakToChief"-message.
     *
     * @param message The {@code ScoutSpeakToChiefMessage} to process.
     */
    private void scoutSpeakToChief(ScoutSpeakToChiefMessage message) {
        final Game game = getGame();
        final Unit unit = message.getUnit(game);
        final IndianSettlement is = message.getSettlement(game);
        final String result = message.getResult();

        invokeLater(() ->
            igc().scoutSpeakToChief(unit, is, result));
    }

    /**
     * Handle a "setDead"-message.
     *
     * @param message The {@code SetDeadMessage} to process.
     */
    private void setDead(SetDeadMessage message) {
        final Game game = getGame();
        final Player player = message.getPlayer(game);

        if (player == null) {
            logger.warning("Invalid player for setDead");
            return;
        }

        invokeLater(() ->
            igc().setDead(player));
    }

    /**
     * Handle a "spyResult" message.
     *
     * @param message The {@code SpySettlementMessage} to process.
     */
    private void spySettlement(SpySettlementMessage message) {
        final Game game = getGame();
        final Tile spyTile = message.getSpyTile();

        invokeLater(() ->
            igc().spyColony(spyTile));
    }

    /**
     * Handle an "update"-message.
     *
     * @param message The {@code UpdateMessage} to process.
     */
    private void update(UpdateMessage message) {
        final Player player = getMyPlayer();
        final Game game = getGame();

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
