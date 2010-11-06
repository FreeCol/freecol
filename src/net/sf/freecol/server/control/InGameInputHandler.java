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

package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.AbandonColonyMessage;
import net.sf.freecol.common.networking.AskSkillMessage;
import net.sf.freecol.common.networking.AssignTeacherMessage;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.BuyGoodsMessage;
import net.sf.freecol.common.networking.BuyMessage;
import net.sf.freecol.common.networking.BuyPropositionMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisbandUnitMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipUnitMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.GiveIndependenceMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.LearnSkillMessage;
import net.sf.freecol.common.networking.LoadCargoMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToAmericaMessage;
import net.sf.freecol.common.networking.MoveToEuropeMessage;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.networking.PayArrearsMessage;
import net.sf.freecol.common.networking.PayForBuildingMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SellGoodsMessage;
import net.sf.freecol.common.networking.SellMessage;
import net.sf.freecol.common.networking.SellPropositionMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SetGoodsLevelsMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.StatisticsMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.UnloadCargoMessage;
import net.sf.freecol.common.networking.UpdateCurrentStopMessage;
import net.sf.freecol.common.networking.WorkMessage;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Handles the network messages that arrives while
 * {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME in game}.
 */
public final class InGameInputHandler extends InputHandler implements NetworkConstants {

    private static Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
        // TODO: move and simplify methods later, for now just delegate
        // TODO: check that NRHs and CPNRHs are sensibly chosen

        register(AbandonColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AbandonColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(AskSkillMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AskSkillMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(AssignTeacherMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AssignTeacherMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(AttackMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new AttackMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(BuildColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new BuildColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(BuyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new BuyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(BuyGoodsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new BuyGoodsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(BuyPropositionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new BuyPropositionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(CashInTreasureTrainMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new CashInTreasureTrainMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ChangeStateMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ChangeStateMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ChangeWorkImprovementTypeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ChangeWorkImprovementTypeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ChangeWorkTypeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ChangeWorkTypeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ClaimLandMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ClaimLandMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ClearSpecialityMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ClearSpecialityMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(CloseTransactionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new CloseTransactionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DeclareIndependenceMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DeclareIndependenceMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DeliverGiftMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DeliverGiftMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DemandTributeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DemandTributeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DisbandUnitMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DisbandUnitMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(DisembarkMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new DisembarkMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(EmbarkMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new EmbarkMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(EmigrateUnitMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new EmigrateUnitMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(EquipUnitMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new EquipUnitMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(GetTransactionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new GetTransactionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(GiveIndependenceMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new GiveIndependenceMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(GoodsForSaleMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new GoodsForSaleMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(InciteMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new InciteMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(IndianDemandMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new IndianDemandMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(JoinColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new JoinColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(LearnSkillMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new LearnSkillMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(LoadCargoMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new LoadCargoMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(LootCargoMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new LootCargoMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MissionaryMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MissionaryMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MoveMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MoveMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MoveToAmericaMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MoveToAmericaMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(MoveToEuropeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new MoveToEuropeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(NewLandNameMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new NewLandNameMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(NewRegionNameMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new NewRegionNameMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(PayArrearsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new PayArrearsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(PayForBuildingMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new PayForBuildingMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(PutOutsideColonyMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new PutOutsideColonyMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(RenameMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new RenameMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(ScoutIndianSettlementMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new ScoutIndianSettlementMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SellMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SellMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SellGoodsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SellGoodsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SellPropositionMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SellPropositionMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SetBuildQueueMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SetBuildQueueMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(SetGoodsLevelsMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new SetGoodsLevelsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(TrainUnitInEuropeMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new TrainUnitInEuropeMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(UnloadCargoMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new UnloadCargoMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(WorkMessage.getXMLElementTagName(),
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new WorkMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});

        // NetworkRequestHandlers
        register(DiplomacyMessage.getXMLElementTagName(),
                 new NetworkRequestHandler() {
            @Override
            public Element handle(Connection connection, Element element) {
                return new DiplomacyMessage(getGame(), element)
                    .handle(freeColServer, connection);
            }});
        register(SetDestinationMessage.getXMLElementTagName(),
                 new NetworkRequestHandler() {
            @Override
            public Element handle(Connection connection, Element element) {
                return new SetDestinationMessage(getGame(), element)
                    .handle(freeColServer, connection);
            }});
        register(SpySettlementMessage.getXMLElementTagName(),
                 new NetworkRequestHandler() {
            @Override
            public Element handle(Connection connection, Element element) {
                return new SpySettlementMessage(getGame(), element)
                    .handle(freeColServer, connection);
            }});
        register(UpdateCurrentStopMessage.getXMLElementTagName(),
                 new NetworkRequestHandler() {
            @Override
            public Element handle(Connection connection, Element element) {
                return new UpdateCurrentStopMessage(getGame(), element)
                    .handle(freeColServer, connection);
            }});

        register("getREFUnits",
                 new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return getREFUnits(connection, element);
            }
        });

        register("foreignAffairs",
                 new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return foreignAffairs(connection, element);
            }});
        register("highScores",
                 new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return highScores(connection, element);
            }});
        register("getNewTradeRoute",
                 new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getNewTradeRoute(connection, element);
            }});
        register("updateTradeRoute",
                 new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return updateTradeRoute(connection, element);
            }});
        register("setTradeRoutes",
                 new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return setTradeRoutes(connection, element);
            }});
        register("assignTradeRoute",
                 new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return assignTradeRoute(connection, element);
            }});
        register(StatisticsMessage.getXMLElementTagName(),
                 new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getServerStatistics(connection, element);
            }
        });

        // Trivial comms that do not merit their own Message-type
        register("endTurn", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                FreeColServer server = getFreeColServer();
                ServerPlayer serverPlayer = server.getPlayer(connection);
                server.getInGameController().endTurn(serverPlayer);
                return null;
            }
        });
        register("continuePlaying", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                FreeColServer server = getFreeColServer();
                ServerPlayer serverPlayer = server.getPlayer(connection);
                return server.getInGameController()
                    .continuePlaying(serverPlayer);
            }
        });
        register("retire", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                FreeColServer server = getFreeColServer();
                ServerPlayer serverPlayer = server.getPlayer(connection);
                return server.getInGameController().retire(serverPlayer);
            }
        });
    }


    /**
     * Handles a "getNewTradeRoute"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element getNewTradeRoute(Connection connection, Element element) {
        Player player = getFreeColServer().getPlayer(connection);
        TradeRoute tradeRoute = getFreeColServer().getModelController().getNewTradeRoute(player);
        Element reply = Message.createNewRootElement("getNewTradeRouteConfirmed");
        reply.appendChild(tradeRoute.toXMLElement(player, reply.getOwnerDocument()));
        return reply;
    }

    /**
     * Handles a "setTradeRoutes"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element updateTradeRoute(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Element childElement = (Element) element.getChildNodes().item(0);
        String id = childElement.getAttribute("ID");
        //TradeRoute clientTradeRoute = new TradeRoute(null, childElement);
        TradeRoute serverTradeRoute = (TradeRoute) getGame().getFreeColGameObject(id);
        if (serverTradeRoute == null) {
            throw new IllegalArgumentException("Could not find 'TradeRoute' with specified ID: "
                    + id);
        }
        if (serverTradeRoute.getOwner() != player) {
            throw new IllegalStateException("Not your trade route!");
        }
        //serverTradeRoute.updateFrom(clientTradeRoute);
        serverTradeRoute.readFromXMLElement(childElement);
        return null;
    }

    /**
     * Handles a "updateTradeRoute"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element setTradeRoutes(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        ArrayList<TradeRoute> routes = new ArrayList<TradeRoute>();
        
        NodeList childElements = element.getChildNodes();
        for(int i = 0; i < childElements.getLength(); i++) {
            Element childElement = (Element) childElements.item(i);
            String id = childElement.getAttribute("id");
            TradeRoute serverTradeRoute = (TradeRoute) getGame().getFreeColGameObject(id);
            if (serverTradeRoute == null) {
                throw new IllegalArgumentException("Could not find 'TradeRoute' with specified ID: " + id);
            }
            if (serverTradeRoute.getOwner() != player) {
                throw new IllegalStateException("Not your trade route!");
            }
            routes.add(serverTradeRoute);
        }
        player.setTradeRoutes(routes);
        return null;
    }

    /**
     * Handles a "assignTradeRoute"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element assignTradeRoute(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        } else if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        String tradeRouteString = element.getAttribute("tradeRoute");

        if (tradeRouteString == null || tradeRouteString == "") {
            unit.setTradeRoute(null);
        } else {
            TradeRoute tradeRoute = (TradeRoute) getGame().getFreeColGameObject(tradeRouteString);

            if (tradeRoute == null) {
                throw new IllegalArgumentException("Could not find 'TradeRoute' with specified ID: "
                                                   + element.getAttribute("tradeRoute"));
            }
            if (tradeRoute.getOwner() != player) {
                throw new IllegalStateException("Not your trade route!");
            }
            unit.setTradeRoute(tradeRoute);
        }
        return null;
    }

    /**
     * Handles a "foreignAffairs"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element foreignAffairs(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Element reply = Message.createNewRootElement("foreignAffairsReport");
        Iterator<Player> enemyPlayerIterator = getGame().getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
            if (enemyPlayer.getConnection() == null || enemyPlayer.isIndian()
                || enemyPlayer.isDead()) {
                continue;
            }
            Element enemyElement = reply.getOwnerDocument().createElement("opponent");
            enemyElement.setAttribute("player", enemyPlayer.getId());
            int numberOfColonies = enemyPlayer.getSettlements().size();
            int numberOfUnits = 0;
            int militaryStrength = 0;
            int navalStrength = 0;
            Iterator<Unit> unitIterator = enemyPlayer.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();
                numberOfUnits++;
                if (unit.isNaval()) {
                    navalStrength += unit.getGame().getCombatModel().getOffencePower(unit, null);
                } else {
                    militaryStrength += unit.getGame().getCombatModel().getOffencePower(unit, null);
                }
            }
            Stance stance = enemyPlayer.getStance(player);
            if (stance == Stance.UNCONTACTED) {
                stance = Stance.PEACE;
            }
            enemyElement.setAttribute("numberOfColonies", String.valueOf(numberOfColonies));
            enemyElement.setAttribute("numberOfUnits", String.valueOf(numberOfUnits));
            enemyElement.setAttribute("militaryStrength", String.valueOf(militaryStrength));
            enemyElement.setAttribute("navalStrength", String.valueOf(navalStrength));
            enemyElement.setAttribute("stance", String.valueOf(stance));
            enemyElement.setAttribute("gold", String.valueOf(enemyPlayer.getGold()));
            if (player.equals(enemyPlayer) ||
                player.hasAbility("model.ability.betterForeignAffairsReport")) {
                enemyElement.setAttribute("SoL", String.valueOf(enemyPlayer.getSoL()));
                enemyElement.setAttribute("foundingFathers", String.valueOf(enemyPlayer.getFatherCount()));
                enemyElement.setAttribute("tax", String.valueOf(enemyPlayer.getTax()));
            }
            reply.appendChild(enemyElement);
        }
        return reply;
    }


    /**
     * Handles a "highScores"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element highScores(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Element reply = Message.createNewRootElement("highScoresReport");
        for (HighScore score : getFreeColServer().getHighScores()) {
            reply.appendChild(score.toXMLElement(player, reply.getOwnerDocument()));
        }
        return reply;
    }


    /**
     * Handles a "getREFUnits"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element getREFUnits(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        UnitType defaultType = getGame().getSpecification().getUnitType("model.unit.freeColonist");
        if (player.getMonarch() == null) {
            ServerPlayer enemyPlayer = (ServerPlayer) player.getREFPlayer();
            java.util.Map<UnitType, EnumMap<Role, Integer>> unitHash =
                new HashMap<UnitType, EnumMap<Role, Integer>>();
            for (Unit unit : enemyPlayer.getUnits()) {
                if (unit.isOffensiveUnit()) {
                    UnitType unitType = defaultType;
                    if (unit.getType().getOffence() > 0 ||
                        unit.hasAbility("model.ability.expertSoldier")) {
                        unitType = unit.getType();
                    }
                    EnumMap<Role, Integer> roleMap = unitHash.get(unitType);
                    if (roleMap == null) {
                        roleMap = new EnumMap<Role, Integer>(Role.class);
                    }
                    Role role = unit.getRole();
                    Integer count = roleMap.get(role);
                    if (count == null) {
                        roleMap.put(role, new Integer(1));
                    } else {
                        roleMap.put(role, new Integer(count.intValue() + 1));
                    }
                    unitHash.put(unitType, roleMap);
                }
            }
            for (java.util.Map.Entry<UnitType, EnumMap<Role, Integer>> typeEntry : unitHash.entrySet()) {
                for (java.util.Map.Entry<Role, Integer> roleEntry : typeEntry.getValue().entrySet()) {
                    units.add(new AbstractUnit(typeEntry.getKey(), roleEntry.getKey(), roleEntry.getValue()));
                }
            }
        } else {
            units = player.getMonarch().getREF();
        }

        Element reply = Message.createNewRootElement("REFUnits");
        for (AbstractUnit unit : units) {
            reply.appendChild(unit.toXMLElement(player,reply.getOwnerDocument()));
        }
        return reply;
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
        logger.info("Logout by: " + connection + ((player != null) ? " (" + player.getName() + ") " : ""));
        if (player == null) {
            return null;
        }
        // TODO
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
         * TODO: Setting the player dead directly should be a server option, but
         * for now - allow the player to reconnect:
         */
        player.setConnected(false);
        if (getFreeColServer().getGame().getCurrentPlayer() == player
                && !getFreeColServer().isSingleplayer()) {
            getFreeColServer().getInGameController().endTurn(player);
        }
        try {
            getFreeColServer().updateMetaServer();
        } catch (NoRouteToServerException e) {}
        
        return null;
    }

    /*
     * Method not used, keep in comments. private void sendErrorToAll(String
     * message, Player player) { Game game = getFreeColServer().getGame();
     * Iterator enemyPlayerIterator = getGame().getPlayerIterator(); while
     * (enemyPlayerIterator.hasNext()) { ServerPlayer enemyPlayer =
     * (ServerPlayer) enemyPlayerIterator.next(); if ((player != null) &&
     * (player.equals(enemyPlayer)) || enemyPlayer.getConnection() == null) {
     * continue; } try { Element errorElement = createErrorReply(message);
     * enemyPlayer.getConnection().send(errorElement); } catch (IOException e) {
     * logger.warning("Could not send message to: " + enemyPlayer.getName() + "
     * with connection " + enemyPlayer.getConnection()); } } }
     */

    private Element getServerStatistics(Connection connection, Element request) {
        StatisticsMessage m = new StatisticsMessage(getGame(), getFreeColServer().getAIMain());
        Element reply = m.toXMLElement();
        return reply;
    }
}
