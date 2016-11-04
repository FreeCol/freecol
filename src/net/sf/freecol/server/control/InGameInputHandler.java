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

package net.sf.freecol.server.control;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.AbandonColonyMessage;
import net.sf.freecol.common.networking.AskSkillMessage;
import net.sf.freecol.common.networking.AssignTeacherMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.CurrentPlayerNetworkRequestHandler;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeclineMoundsMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DeleteTradeRouteMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisbandUnitMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipForRoleMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.HighScoreMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.LearnSkillMessage;
import net.sf.freecol.common.networking.LoadGoodsMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NativeGiftMessage;
import net.sf.freecol.common.networking.NativeTradeMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.NewTradeRouteMessage;
import net.sf.freecol.common.networking.PayArrearsMessage;
import net.sf.freecol.common.networking.PayForBuildingMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.RearrangeColonyMessage;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.SetCurrentStopMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SetGoodsLevelsMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UnloadGoodsMessage;
import net.sf.freecol.common.networking.UpdateTradeRouteMessage;
import net.sf.freecol.common.networking.WorkMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives while
 * {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME in game}.
 */
public final class InGameInputHandler extends ServerInputHandler {

    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());


    /**
     * Create a new server in-game input handler.
     *
     * Note: all the handler lamdbas call getGame() because it is not
     * necessarily available when the constructor is called.
     * 
     * @param freeColServer The main server object.
     */
    public InGameInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);

        register(AbandonColonyMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new AbandonColonyMessage(getGame(), e)));
        register(AskSkillMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new AskSkillMessage(getGame(), e)));
        register(AssignTeacherMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new AssignTeacherMessage(getGame(), e)));
        register(AssignTradeRouteMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new AssignTradeRouteMessage(getGame(), e)));
        register(AttackMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new AttackMessage(getGame(), e)));
        register(BuildColonyMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new BuildColonyMessage(getGame(), e)));
        register(CashInTreasureTrainMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new CashInTreasureTrainMessage(getGame(), e)));
        register(ChangeStateMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ChangeStateMessage(getGame(), e)));
        register(ChangeWorkImprovementTypeMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ChangeWorkImprovementTypeMessage(getGame(), e)));
        register(ChangeWorkTypeMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ChangeWorkTypeMessage(getGame(), e)));
        register(ChooseFoundingFatherMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ChooseFoundingFatherMessage(getGame(), e)));
        register(ClaimLandMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ClaimLandMessage(getGame(), e)));
        register(ClearSpecialityMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ClearSpecialityMessage(getGame(), e)));
        register(TrivialMessage.CONTINUE_TAG,
            (Connection conn, Element e) -> handler(false, conn,
                TrivialMessage.CONTINUE_MESSAGE));
        register(DeclareIndependenceMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new DeclareIndependenceMessage(getGame(), e)));
        register(DeclineMoundsMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new DeclineMoundsMessage(getGame(), e)));
        register(DeliverGiftMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new DeliverGiftMessage(getGame(), e)));
        register(DeleteTradeRouteMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new DeleteTradeRouteMessage(getGame(), e)));
        register(DemandTributeMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new DemandTributeMessage(getGame(), e)));
        register(DiplomacyMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new DiplomacyMessage(getGame(), e)));
        register(DisbandUnitMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new DisbandUnitMessage(getGame(), e)));
        register(DisembarkMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new DisembarkMessage(getGame(), e)));
        register(EmbarkMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new EmbarkMessage(getGame(), e)));
        register(EmigrateUnitMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new EmigrateUnitMessage(getGame(), e)));
        register(TrivialMessage.END_TURN_TAG,
            (Connection conn, Element e) -> handler(true, conn,
                TrivialMessage.END_TURN_MESSAGE));
        register(TrivialMessage.ENTER_REVENGE_MODE_TAG,
            (Connection conn, Element e) -> handler(false, conn,
                TrivialMessage.ENTER_REVENGE_MODE_MESSAGE));
        register(EquipForRoleMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new EquipForRoleMessage(getGame(), e)));
        register(FirstContactMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new FirstContactMessage(getGame(), e)));
        register(InciteMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new InciteMessage(getGame(), e)));
        register(IndianDemandMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new IndianDemandMessage(getGame(), e)));
        register(HighScoreMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new HighScoreMessage(getGame(), e)));
        register(JoinColonyMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new JoinColonyMessage(getGame(), e)));
        register(LearnSkillMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new LearnSkillMessage(getGame(), e)));
        register(LoadGoodsMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new LoadGoodsMessage(getGame(), e)));
        register(LootCargoMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new LootCargoMessage(getGame(), e)));
        register(MissionaryMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new MissionaryMessage(getGame(), e)));
        register(MonarchActionMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new MonarchActionMessage(getGame(), e)));
        register(MoveMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new MoveMessage(getGame(), e)));
        register(MoveToMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new MoveToMessage(getGame(), e)));
        //register(MultipleMessage.TAG,
        //    (Connection conn, Element e) -> handler(false, conn,
        //        new MultipleMessage(getGame(), e)));
        register(NationSummaryMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new NationSummaryMessage(getGame(), e)));
        register(NativeGiftMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new NativeGiftMessage(getGame(), e)));
        register(NativeTradeMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new NativeTradeMessage(getGame(), e)));
        register(NewLandNameMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new NewLandNameMessage(getGame(), e)));
        register(NewRegionNameMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new NewRegionNameMessage(getGame(), e)));
        register(NewTradeRouteMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new NewTradeRouteMessage(getGame(), e)));
        register(PayArrearsMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new PayArrearsMessage(getGame(), e)));
        register(PayForBuildingMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new PayForBuildingMessage(getGame(), e)));
        register(PutOutsideColonyMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new PutOutsideColonyMessage(getGame(), e)));
        register(RearrangeColonyMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new RearrangeColonyMessage(getGame(), e)));
        register(RenameMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new RenameMessage(getGame(), e)));
        register(TrivialMessage.RETIRE_TAG,
            (Connection conn, Element e) -> handler(false, conn,
                TrivialMessage.RETIRE_MESSAGE));
        register(ScoutIndianSettlementMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ScoutIndianSettlementMessage(getGame(), e)));
        register(ScoutSpeakToChiefMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new ScoutSpeakToChiefMessage(getGame(), e)));
        register(SetBuildQueueMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new SetBuildQueueMessage(getGame(), e)));
        register(SetCurrentStopMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new SetCurrentStopMessage(getGame(), e)));
        register(SetDestinationMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new SetDestinationMessage(getGame(), e)));
        register(SetGoodsLevelsMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new SetGoodsLevelsMessage(getGame(), e)));
        register(SpySettlementMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new SpySettlementMessage(getGame(), e)));
        register(TrainUnitInEuropeMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new TrainUnitInEuropeMessage(getGame(), e)));
        register(UnloadGoodsMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new UnloadGoodsMessage(getGame(), e)));
        register(UpdateTradeRouteMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new UpdateTradeRouteMessage(getGame(), e)));
        register(WorkMessage.TAG,
            (Connection conn, Element e) -> handler(true, conn,
                new WorkMessage(getGame(), e)));

        register(MultipleMessage.TAG,
            (Connection connection, Element element) ->
            new MultipleMessage(getGame(), element)
                .handle(freeColServer, connection));
    }
}
