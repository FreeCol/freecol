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
                new AddPlayerMessage(getGame(), e).clientHandler(freeColClient));
        register(AnimateAttackMessage.TAG,
            (Connection c, Element e) ->
                new AnimateAttackMessage(getGame(), e).clientHandler(freeColClient));
        register(AnimateMoveMessage.TAG,
            (Connection c, Element e) ->
                new AnimateMoveMessage(getGame(), e).clientHandler(freeColClient));
        register(ChatMessage.TAG,
            (Connection c, Element e) ->
                new ChatMessage(getGame(), e).clientHandler(freeColClient));
        register(ChooseFoundingFatherMessage.TAG,
            (Connection c, Element e) ->
                new ChooseFoundingFatherMessage(getGame(), e).clientHandler(freeColClient));
        register(CloseMenusMessage.TAG,
            (Connection c, Element e) ->
                TrivialMessage.closeMenusMessage.clientHandler(freeColClient));
        register(DiplomacyMessage.TAG,
            (Connection c, Element e) ->
                new DiplomacyMessage(getGame(), e).clientHandler(freeColClient));
        register(DisconnectMessage.TAG,
            (Connection c, Element e) ->
                TrivialMessage.disconnectMessage.clientHandler(freeColClient));
        register(ErrorMessage.TAG,
            (Connection c, Element e) ->
                new ErrorMessage(getGame(), e).clientHandler(freeColClient));
        register(FeatureChangeMessage.TAG,
            (Connection c, Element e) ->
                new FeatureChangeMessage(getGame(), e).clientHandler(freeColClient));
        register(FirstContactMessage.TAG,
            (Connection c, Element e) ->
                new FirstContactMessage(getGame(), e).clientHandler(freeColClient));
        register(FountainOfYouthMessage.TAG,
            (Connection c, Element e) ->
                new FountainOfYouthMessage(getGame(), e).clientHandler(freeColClient));
        register(GameEndedMessage.TAG,
            (Connection c, Element e) ->
                new GameEndedMessage(getGame(), e).clientHandler(freeColClient));
        register(HighScoreMessage.TAG,
            (Connection c, Element e) ->
                new HighScoreMessage(getGame(), e).clientHandler(freeColClient));
        register(InciteMessage.TAG,
            (Connection c, Element e) ->
                new InciteMessage(getGame(), e).clientHandler(freeColClient));
        register(IndianDemandMessage.TAG,
            (Connection c, Element e) ->
                new IndianDemandMessage(getGame(), e).clientHandler(freeColClient));
        register(LogoutMessage.TAG,
            (Connection c, Element e) ->
                new LogoutMessage(getGame(), e).clientHandler(freeColClient));
        register(LootCargoMessage.TAG,
            (Connection c, Element e) ->
                new LootCargoMessage(getGame(), e).clientHandler(freeColClient));
        register(MonarchActionMessage.TAG,
            (Connection c, Element e) ->
                new MonarchActionMessage(getGame(), e).clientHandler(freeColClient));
        register(NationSummaryMessage.TAG,
            (Connection c, Element e) ->
                new NationSummaryMessage(getGame(), e).clientHandler(freeColClient));
        register(NativeTradeMessage.TAG,
            (Connection c, Element e) ->
                new NativeTradeMessage(getGame(), e).clientHandler(freeColClient));
        register(NewLandNameMessage.TAG,
            (Connection c, Element e) ->
                new NewLandNameMessage(getGame(), e).clientHandler(freeColClient));
        register(NewRegionNameMessage.TAG,
            (Connection c, Element e) ->
                new NewRegionNameMessage(getGame(), e).clientHandler(freeColClient));
        register(NewTurnMessage.TAG,
            (Connection c, Element e) ->
                new NewTurnMessage(getGame(), e).clientHandler(freeColClient));
        register(NewTradeRouteMessage.TAG,
            (Connection c, Element e) ->
                new NewTradeRouteMessage(getGame(), e).clientHandler(freeColClient));
        register(ReconnectMessage.TAG,
            (Connection c, Element e) ->
                TrivialMessage.reconnectMessage.clientHandler(freeColClient));
        register(RemoveMessage.TAG,
            (Connection c, Element e) ->
                new RemoveMessage(getGame(), e).clientHandler(freeColClient));
        register(ScoutSpeakToChiefMessage.TAG,
            (Connection c, Element e) ->
                new ScoutSpeakToChiefMessage(getGame(), e).clientHandler(freeColClient));
        register(SetAIMessage.TAG,
            (Connection c, Element e) ->
                new SetAIMessage(getGame(), e).clientHandler(freeColClient));
        register(SetCurrentPlayerMessage.TAG,
            (Connection c, Element e) ->
                new SetCurrentPlayerMessage(getGame(), e).clientHandler(freeColClient));
        register(SetDeadMessage.TAG,
            (Connection c, Element e) ->
                new SetDeadMessage(getGame(), e).clientHandler(freeColClient));
        register(SetStanceMessage.TAG,
            (Connection c, Element e) ->
                new SetStanceMessage(getGame(), e).clientHandler(freeColClient));
        register(SpySettlementMessage.TAG,
            (Connection c, Element e) ->
                new SpySettlementMessage(getGame(), e).clientHandler(freeColClient));
        register(UpdateMessage.TAG,
            (Connection c, Element e) ->
                new UpdateMessage(getGame(), e).clientHandler(freeColClient));
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
}
