/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.AbandonColonyMessage;
import net.sf.freecol.common.networking.AskSkillMessage;
import net.sf.freecol.common.networking.AssignTeacherMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.BuyMessage;
import net.sf.freecol.common.networking.BuyPropositionMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.CurrentPlayerNetworkRequestHandler;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeclineMoundsMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisbandUnitMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipForRoleMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.GetNationSummaryMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.LearnSkillMessage;
import net.sf.freecol.common.networking.LoadGoodsMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToMessage;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.PayArrearsMessage;
import net.sf.freecol.common.networking.PayForBuildingMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.RearrangeColonyMessage;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SellMessage;
import net.sf.freecol.common.networking.SellPropositionMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.SetCurrentStopMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SetGoodsLevelsMessage;
import net.sf.freecol.common.networking.SetTradeRoutesMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.UnloadGoodsMessage;
import net.sf.freecol.common.networking.UpdateTradeRouteMessage;
import net.sf.freecol.common.networking.WorkMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Handles the network messages that arrives while
 * {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME in game}.
 */
public final class InGameInputHandler extends InputHandler
    implements NetworkConstants {

    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
        // FIXME: move and simplify methods later, for now just delegate
        // FIXME: check that NRHs and CPNRHs are sensibly chosen

        // Messages that are not specialized are trivial elements identified
        // by tag name only.
        register(AbandonColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AbandonColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(AskSkillMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AskSkillMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(AssignTeacherMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AssignTeacherMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(AttackMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AttackMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(BuildColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new BuildColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(BuyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new BuyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(BuyPropositionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new BuyPropositionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(CashInTreasureTrainMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new CashInTreasureTrainMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ChangeStateMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ChangeStateMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ChangeWorkImprovementTypeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ChangeWorkImprovementTypeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ChangeWorkTypeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ChangeWorkTypeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ChooseFoundingFatherMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ChooseFoundingFatherMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ClaimLandMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ClaimLandMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ClearSpecialityMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ClearSpecialityMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(CloseTransactionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new CloseTransactionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DeclareIndependenceMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DeclareIndependenceMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DeclineMoundsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DeclineMoundsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DeliverGiftMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DeliverGiftMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DemandTributeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DemandTributeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DisbandUnitMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DisbandUnitMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DisembarkMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DisembarkMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(EmbarkMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new EmbarkMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(EmigrateUnitMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new EmigrateUnitMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register("endTurn",
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return freeColServer.getInGameController()
                    .endTurn(freeColServer.getPlayer(connection));
            }});
        register(EquipForRoleMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new EquipForRoleMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register("getREFUnits",
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return freeColServer.getInGameController()
                    .getREFUnits(freeColServer.getPlayer(connection));
            }});
        register(GetTransactionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new GetTransactionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(GoodsForSaleMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new GoodsForSaleMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(InciteMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new InciteMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(IndianDemandMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new IndianDemandMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(JoinColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new JoinColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(LearnSkillMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new LearnSkillMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(LoadGoodsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new LoadGoodsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(LootCargoMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new LootCargoMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MissionaryMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MissionaryMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MonarchActionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MonarchActionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MoveMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MoveMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MoveToMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MoveToMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(NewLandNameMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new NewLandNameMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(NewRegionNameMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new NewRegionNameMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(PayArrearsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new PayArrearsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(PayForBuildingMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new PayForBuildingMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(PutOutsideColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new PutOutsideColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(RearrangeColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new RearrangeColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(RenameMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new RenameMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ScoutIndianSettlementMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ScoutIndianSettlementMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ScoutSpeakToChiefMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ScoutSpeakToChiefMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SellMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SellMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SellPropositionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SellPropositionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SetBuildQueueMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SetBuildQueueMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SetGoodsLevelsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SetGoodsLevelsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(TrainUnitInEuropeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new TrainUnitInEuropeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(UnloadGoodsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new UnloadGoodsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(WorkMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new WorkMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});

        register("multiple",
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                final NodeList nodes = element.getChildNodes();
                List<Element> results = new ArrayList<>();

                for (int i = 0; i < nodes.getLength(); i++) {
                    final Element node = (Element)nodes.item(i);
                    final String tag = node.getTagName();
                    try {
                        Element reply = InGameInputHandler.this
                            .handle(connection, node);
                        if (reply != null) results.add(reply);
                        logger.log(Level.FINEST, "multiple(" + i + "): " + tag
                                + " -> " + ((reply == null) ? "null"
                                    : reply.getTagName()));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Crash in multiple " + i
                            + ", tag " + tag + ", continuing.", e);
                    }
                }
                return DOMMessage.collapseElements(results);
            }});

        // NetworkRequestHandlers
        register(AssignTradeRouteMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new AssignTradeRouteMessage(getGame(), element)
                .handle(freeColServer, connection));
        register("continuePlaying",
            (Connection connection, Element element) ->
            freeColServer.getInGameController()
                .continuePlaying(freeColServer.getPlayer(connection)));
        register(DiplomacyMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new DiplomacyMessage(getGame(), element)
                .handle(freeColServer, connection));
        register("enterRevengeMode",
            (Connection connection, Element element) ->
            freeColServer.getInGameController()
                .enterRevengeMode(freeColServer.getPlayer(connection)));
        register(FirstContactMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new FirstContactMessage(getGame(), element)
                .handle(freeColServer, connection));
        register("getHighScores",
            (Connection connection, Element element) ->
            freeColServer.getInGameController()
                .getHighScores(freeColServer.getPlayer(connection)));
        register(GetNationSummaryMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new GetNationSummaryMessage(element)
                .handle(freeColServer, connection));
        register("getNewTradeRoute",
            (Connection connection, Element element) ->
            freeColServer.getInGameController()
                .getNewTradeRoute(freeColServer.getPlayer(connection)));
        register("retire",
            (Connection connection, Element element) ->
            freeColServer.getInGameController()
                .retire(freeColServer.getPlayer(connection)));
        register(SetCurrentStopMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new SetCurrentStopMessage(getGame(), element)
                .handle(freeColServer, connection));
        register(SetDestinationMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new SetDestinationMessage(getGame(), element)
                .handle(freeColServer, connection));
        register(SetTradeRoutesMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new SetTradeRoutesMessage(getGame(), element)
                .handle(freeColServer, connection));
        register(SpySettlementMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new SpySettlementMessage(getGame(), element)
                .handle(freeColServer, connection));
        register("getStatistics",
            (Connection connection, Element element) ->
            freeColServer.getInGameController()
                .getStatistics(freeColServer.getPlayer(connection)));
        register(UpdateTradeRouteMessage.getXMLElementTagName(),
            (Connection connection, Element element) ->
            new UpdateTradeRouteMessage(getGame(), element)
                .handle(freeColServer, connection));
    }

    /**
     * Handles a "logout"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param logoutElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     * @return The reply.
     */
    @Override
    protected Element logout(Connection connection, Element logoutElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        logger.info("Logout by: " + connection
                    + ((player != null) ? " (" + player.getName() + ") " : ""));
        if (player == null) {
            return null;
        }
        // FIXME:
        // Remove the player's units/colonies from the map and send map updates
        // to the
        // players that can see such units or colonies.
        // SHOULDN'T THIS WAIT UNTIL THE CURRENT PLAYER HAS FINISHED HIS TURN?
        /*
         * player.setDead(true); Element setDeadElement =
         * Message.createNewRootElement("setDead");
         * setDeadElement.setAttribute("player", player.getId());
         * freeColServer.getServer().sendToAll(setDeadElement, connection);
         */

        /*
         * FIXME: Setting the player dead directly should be a server
         * option, but for now - allow the player to reconnect:
         */
        Element reply = null;
        player.setConnected(false);
        if (getFreeColServer().getGame().getCurrentPlayer() == player
                && !getFreeColServer().getSinglePlayer()) {
            reply = getFreeColServer().getInGameController().endTurn(player);
        }
        getFreeColServer().updateMetaServer();
        return reply;
    }
}
