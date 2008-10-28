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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.BuyLandMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.networking.StatisticsMessage;
import net.sf.freecol.common.networking.StealLandMessage;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Handles the network messages that arrives while
 * {@link FreeColServer#IN_GAME in game}.
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
        register("createUnit", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return createUnit(connection, element);
            }
        });
        register("createBuilding", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return createBuilding(connection, element);
            }
        });
        register("getRandom", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getRandom(connection, element);
            }
        });
        register("getVacantEntryLocation", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getVacantEntryLocation(connection, element);
            }
        });
        register("setDestination", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return setDestination(connection, element);
            }
        });
        register("move", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return move(connection, element);
            }
        });
        register("askSkill", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return askSkill(connection, element);
            }
        });
        register("attack", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return attack(connection, element);
            }
        });
        register("embark", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return embark(connection, element);
            }
        });
        register("boardShip", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return boardShip(connection, element);
            }
        });
        register("learnSkillAtSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return learnSkillAtSettlement(connection, element);
            }
        });
        register("scoutIndianSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return scoutIndianSettlement(connection, element);
            }
        });
        register("missionaryAtSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return missionaryAtSettlement(connection, element);
            }
        });
        register("inciteAtSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return inciteAtSettlement(connection, element);
            }
        });
        register("armedUnitDemandTribute", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return armedUnitDemandTribute(connection, element);
            }
        });
        register("leaveShip", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return leaveShip(connection, element);
            }
        });
        register("loadCargo", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return loadCargo(connection, element);
            }
        });
        register("unloadCargo", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return unloadCargo(connection, element);
            }
        });
        register("buyGoods", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return buyGoods(connection, element);
            }
        });
        register("sellGoods", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return sellGoods(connection, element);
            }
        });
        register("moveToEurope", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return moveToEurope(connection, element);
            }
        });
        register("moveToAmerica", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return moveToAmerica(connection, element);
            }
        });
        register("buildColony", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return buildColony(connection, element);
            }
        });
        register("recruitUnitInEurope", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return recruitUnitInEurope(connection, element);
            }
        });
        register("emigrateUnitInEurope", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return emigrateUnitInEurope(connection, element);
            }
        });
        register("trainUnitInEurope", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return trainUnitInEurope(connection, element);
            }
        });
        register("equipUnit", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return equipUnit(connection, element);
            }
        });
        register("work", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return work(connection, element);
            }
        });
        register("changeWorkType", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return changeWorkType(connection, element);
            }
        });
        register("workImprovement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return workImprovement(connection, element);
            }
        });
        register("setCurrentlyBuilding", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return setCurrentlyBuilding(connection, element);
            }
        });
        register("changeState", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return changeState(connection, element);
            }
        });
        register("putOutsideColony", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return putOutsideColony(connection, element);
            }
        });
        register("clearSpeciality", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return clearSpeciality(connection, element);
            }
        });
        register("setNewLandName", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return setNewLandName(connection, element);
            }
        });
        register("endTurn", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return endTurn(connection, element);
            }
        });
        register("disbandUnit", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return disbandUnit(connection, element);
            }
        });
        register("cashInTreasureTrain", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return cashInTreasureTrain(connection, element);
            }
        });
        register("tradeProposition", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return tradeProposition(connection, element);
            }
        });
        register("trade", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return trade(connection, element);
            }
        });
        register("buyProposition", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return buyProposition(connection, element);
            }
        });
        register("buy", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return buy(connection, element);
            }
        });
        register("deliverGift", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return deliverGift(connection, element);
            }
        });
        register("indianDemand", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return indianDemand(connection, element);
            }
        });
        register(BuyLandMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                BuyLandMessage message = new BuyLandMessage(getGame(), element);
                return message.handle(freeColServer, player, connection);
            }
        });
        register(StealLandMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                StealLandMessage message = new StealLandMessage(getGame(), element);
                return message.handle(freeColServer, player, connection);
            }
        });
        register("payForBuilding", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return payForBuilding(connection, element);
            }
        });
        register("payArrears", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return payArrears(connection, element);
            }
        });
        register("setGoodsLevels", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return setGoodsLevels(connection, element);
            }
        });
        register("declareIndependence", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return declareIndependence(connection, element);
            }
        });
        register("giveIndependence", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return giveIndependence(connection, element);
            }
        });
        register("foreignAffairs", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return foreignAffairs(connection, element);
            }
        });
        register("getREFUnits", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return getREFUnits(connection, element);
            }
        });
        register("rename", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return rename(connection, element);
            }
        });
        register("getNewTradeRoute", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getNewTradeRoute(connection, element);
            }
        });
        register("updateTradeRoute", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return updateTradeRoute(connection, element);
            }
        });
        register("setTradeRoutes", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return setTradeRoutes(connection, element);
            }
        });
        register("assignTradeRoute", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return assignTradeRoute(connection, element);
            }
        });
        register("updateCurrentStop", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return updateCurrentStop(connection, element);
            }
        });
        register("diplomaticTrade", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return diplomaticTrade(connection, element);
            }
        });
        register("selectFromFountainYouth", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return selectFromFountainYouth(connection, element);
            }
        });
        register("spySettlement", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return spySettlement(connection, element);
            }
        });
        register("abandonColony", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return abandonColony(connection, element);
            }
        });
        register("continuePlaying", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return continuePlaying(connection, element);
            }
        });
        register("assignTeacher", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return assignTeacher(connection, element);
            }
        });
        register(StatisticsMessage.getXMLElementTagName(), new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getServerStatistics(connection, element);
            }
        });
    }

    private List<ServerPlayer> getOtherPlayers(Player player) {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        for (Player otherPlayer : getGame().getPlayers()) {
            ServerPlayer enemyPlayer = (ServerPlayer) otherPlayer;
            if (player.equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
                continue;
            }
            result.add(enemyPlayer);
        }
        return result;
    }


    private void sendRemoveUnitToAll(Unit unit, Player player) {
        Element removeElement = Message.createNewRootElement("remove");
        Element removeUnit = removeElement.getOwnerDocument().createElement("removeObject");
        removeUnit.setAttribute("ID", unit.getId());
        removeElement.appendChild(removeUnit);
        for (ServerPlayer enemyPlayer : getOtherPlayers(player)) {
            if (unit.isVisibleTo(enemyPlayer)) {
                try {
                    enemyPlayer.getConnection().send(removeElement);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                                   + enemyPlayer.getConnection());
                }
            }
        }
    }

    /**
     * Handles a "setNewLandName"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element setNewLandName(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        player.setNewLandName(element.getAttribute("newLandName"));
        // TODO: Send name to all other players.
        return null;
    }

    /**
     * Handles a "createUnit"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element createUnit(Connection connection, Element element) {
        logger.info("Receiving \"createUnit\"-request.");
        String taskID = element.getAttribute("taskID");
        Location location = (Location) getGame().getFreeColGameObject(element.getAttribute("location"));
        Player owner = (Player) getGame().getFreeColGameObject(element.getAttribute("owner"));
        UnitType type = FreeCol.getSpecification().getUnitType(element.getAttribute("type"));
        if (location == null) {
            throw new NullPointerException();
        }
        if (owner == null) {
            throw new NullPointerException();
        }
        Unit unit = getFreeColServer().getModelController()
                .createUnit(taskID, location, owner, type, false, connection);
        Element reply = Message.createNewRootElement("createUnitConfirmed");
        reply.appendChild(unit.toXMLElement(owner, reply.getOwnerDocument()));
        return reply;
    }

    /**
     * Handles a "createBuilding"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element createBuilding(Connection connection, Element element) {
        logger.info("Receiving \"createBuilding\"-request.");
        String taskID = element.getAttribute("taskID");
        Colony colony = (Colony) getGame().getFreeColGameObject(element.getAttribute("colony"));
        BuildingType type = FreeCol.getSpecification().getBuildingType(element.getAttribute("type"));
        if (colony == null) {
            throw new NullPointerException();
        }
        Building building = getFreeColServer().getModelController()
                .createBuilding(taskID, colony, type, false, connection);
        Element reply = Message.createNewRootElement("createBuildingConfirmed");
        reply.appendChild(building.toXMLElement(colony.getOwner(), reply.getOwnerDocument()));
        return reply;
    }

    /**
     * Handles a "getRandom"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element getRandom(Connection connection, Element element) {
        logger.info("Receiving \"getRandom\"-request.");
        String taskID = element.getAttribute("taskID");
        int n = Integer.parseInt(element.getAttribute("n"));
        int result = getFreeColServer().getModelController().getRandom(taskID, n);
        Element reply = Message.createNewRootElement("getRandomConfirmed");
        reply.setAttribute("result", Integer.toString(result));
        logger.info("Result: " + result);
        return reply;
    }

    /**
     * Handles a "getVacantEntryLocation"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element getVacantEntryLocation(Connection connection, Element element) {
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Player owner = unit.getOwner();
        ServerPlayer askingPlayer = getFreeColServer().getPlayer(connection);
        if (owner != askingPlayer) {
            throw new IllegalStateException("Unit " + unit + " with owner " + owner + " not owned by " + askingPlayer
                    + ", refusing to get vacant location!");
        }
        Location entryLocation = getFreeColServer().getModelController().setToVacantEntryLocation(unit);
        Element reply = Message.createNewRootElement("getVacantEntryLocationConfirmed");
        reply.setAttribute("location", entryLocation.getId());
        return reply;
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
        TradeRoute clientTradeRoute = new TradeRoute(null, childElement);
        TradeRoute serverTradeRoute = (TradeRoute) getGame().getFreeColGameObject(clientTradeRoute.getId());
        if (serverTradeRoute == null) {
            throw new IllegalArgumentException("Could not find 'TradeRoute' with specified ID: "
                    + clientTradeRoute.getId());
        }
        if (serverTradeRoute.getOwner() != player) {
            throw new IllegalStateException("Not your trade route!");
        }
        serverTradeRoute.updateFrom(clientTradeRoute);
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
     * Handles a "updateCurrentStop"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element updateCurrentStop(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        } else if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        
        unit.nextStop();
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
     * Handles a "diplomaticTrade"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element diplomaticTrade(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                                               + element.getAttribute("unit"));
        }
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        Tile tile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
        if (tile == null) {
            throw new IllegalArgumentException("Could not find 'Tile' in direction " +
                                               direction);
        }
        Settlement settlement = tile.getSettlement();
        if (settlement == null) {
            throw new IllegalArgumentException("No settlement on 'Tile' " +
                                               tile.getId());
        }
        unit.setMovesLeft(0);
        
        NodeList childElements = element.getChildNodes();
        Element childElement = (Element) childElements.item(0);
        DiplomaticTrade agreement = new DiplomaticTrade(getGame(), childElement);
        if (agreement.getSender() != player) {
            throw new IllegalArgumentException("Sender of 'DiplomaticTrade' message is not " +
                                               "player " + player.getName());
        }
        if (agreement.isAccept()) {
            agreement.makeTrade();
        }
        ServerPlayer enemyPlayer = (ServerPlayer) agreement.getRecipient();
        Element reply = null;
        try {
            reply = enemyPlayer.getConnection().ask(element);
        } catch (IOException e) {
            logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                           + enemyPlayer.getConnection());
        }
        if (reply != null) {
            String accept = reply.getAttribute("accept");
            if (accept != null && accept.equals("accept")) {
                agreement.makeTrade();
            }
        }
        return reply;
    }

    /**
     * Handles an "spySettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param spyElement The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element spySettlement(Connection connection, Element spyElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        // Get parameters:
        Unit unit = (Unit) getGame().getFreeColGameObject(spyElement.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, spyElement.getAttribute("direction"));
        // Test the parameters:
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + spyElement.getAttribute("unit"));
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' is not on the map: " + unit.toString());
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
        if (newTile == null) {
            throw new IllegalArgumentException("Could not find tile in direction " + direction + " from unit with ID "
                    + spyElement.getAttribute("unit"));
        }
        Settlement settlement = newTile.getSettlement();
        if (settlement == null) {
            throw new IllegalArgumentException("There is no settlement in direction " + direction + " from unit with ID "
                    + spyElement.getAttribute("unit"));
        }
        
        Element reply = Message.createNewRootElement("foreignColony");
        if (settlement instanceof Colony) {
            reply.appendChild(((Colony) settlement).toXMLElement(player, reply.getOwnerDocument(), true, false));
        } else if (settlement instanceof IndianSettlement) {
            reply.appendChild(((IndianSettlement) settlement).toXMLElement(player, reply.getOwnerDocument(), true, false));
        }
        for(Unit foreignUnit : newTile.getUnitList()) {
            reply.appendChild(foreignUnit.toXMLElement(player, reply.getOwnerDocument(), true, false));
        }
        return reply;
    }

    /**
     * Handles an "abandonColony"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param abandonElement The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element abandonColony(Connection connection, Element abandonElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        // Get parameters:
        Colony colony = (Colony) getGame().getFreeColGameObject(abandonElement.getAttribute("colony"));
        // Test the parameters:
        if (colony == null) {
            throw new IllegalArgumentException("Could not find 'Colony' with specified ID: "
                    + abandonElement.getAttribute("colony"));
        }
        if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your colony!");
        }
        if (colony.hasAbility("model.ability.preventAbandonColony")) {
            logger.warning("Ability prevents abandoning colony.");
            return null;
        }
        Tile tile = colony.getTile();
        // TODO: modify/abort trade routes?
        colony.dispose();
        sendUpdatedTileToAll(tile, player);
        return null;
    }

    /**
     * Handles a "move"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param moveElement The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element move(Connection connection, Element moveElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        String unitID = moveElement.getAttribute("unit");
        Unit unit = (Unit) getGame().getFreeColGameObject(unitID);
        Direction direction = Enum.valueOf(Direction.class, moveElement.getAttribute("direction"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + unitID);
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' not on map: ID: " + unitID + " ("
                    + unit.getName() + ")");
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());

        for (ServerPlayer enemyPlayer : getOtherPlayers(player)) {
            try {
                if (unit.isVisibleTo(enemyPlayer)) { // && !disembark
                    // the unit is already visible
                    Element opponentMoveElement = Message.createNewRootElement("opponentMove");
                    opponentMoveElement.setAttribute("fromTile", unit.getTile().getId());
                    opponentMoveElement.setAttribute("direction", direction.toString());
                    opponentMoveElement.setAttribute("unit", unit.getId());
                    enemyPlayer.getConnection().send(opponentMoveElement);
                } else if (enemyPlayer.canSee(newTile)
                        && (newTile.getSettlement() == null || 
                            !getGame().getGameOptions().getBoolean(GameOptions.UNIT_HIDING))) {
                    // the unit reveals itself, after leaving a settlement or carrier
                    Element opponentMoveElement = Message.createNewRootElement("opponentMove");
                    opponentMoveElement.setAttribute("direction", direction.toString());
                    opponentMoveElement.setAttribute("toTile", newTile.getId());
                    opponentMoveElement.appendChild(unit.toXMLElement(enemyPlayer, opponentMoveElement
                            .getOwnerDocument()));
                    if (unit.getLocation() instanceof Unit && !((Unit) unit.getLocation()).isVisibleTo(enemyPlayer)) {
                        Unit location = (Unit) unit.getLocation();
                        opponentMoveElement.setAttribute("inUnit", location.getId());
                        opponentMoveElement.appendChild(location.toXMLElement(enemyPlayer, opponentMoveElement
                                .getOwnerDocument()));
                    }
                    enemyPlayer.getConnection().send(opponentMoveElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                        + enemyPlayer.getConnection());
            }
        }
        
        unit.move(direction);

        Element reply = Message.createNewRootElement("update");
        CombatModel combatModel = unit.getGame().getCombatModel();        
        // Check if ship is slowed
        if (unit.isNaval() && unit.getMovesLeft() > 0) {
            Iterator<Position> tileIterator = getGame().getMap().getAdjacentIterator(unit.getTile().getPosition());
            float attackPower = 0;
            Unit attacker = null;
            
            while (tileIterator.hasNext()) {
                Tile tile = getGame().getMap().getTile(tileIterator.next());
                Colony colony = tile.getColony();
                // ships in settlements don't slow enemy ships
                if (colony != null) {
                    Player enemy = colony.getOwner();
                    if (player != enemy && (player.getStance(enemy) == Stance.WAR
                            || unit.hasAbility("model.ability.piracy"))
                            && colony.hasAbility("model.ability.bombardShips")) {
                        float bombardingPower = combatModel.getOffencePower(colony, unit);
                    }
                } else if (!tile.isLand() && tile.getFirstUnit() != null) {
                    Player enemy = tile.getFirstUnit().getOwner();
                    if (player == enemy) { // own units, check another tile
                        continue;
                    }
                    
                    for (Unit enemyUnit : tile.getUnitList()) {
                        if (enemyUnit.isOffensiveUnit() && (player.getStance(enemy) == Stance.WAR
                                || enemyUnit.hasAbility("model.ability.piracy")
                                || unit.hasAbility("model.ability.piracy"))) {
                            attackPower += combatModel.getOffencePower(enemyUnit, unit);
                            if (attacker == null) {
                                attacker = enemyUnit;
                            }
                        }
                    }
                }
            }
            
            if (attackPower > 0) {
                float defencePower = combatModel.getDefencePower(attacker, unit);
                float totalProbability = attackPower + defencePower;
                int r = getPseudoRandom().nextInt(Math.round(totalProbability) + 1);
                if (r < attackPower) {
                    int diff = Math.max(0, Math.round(attackPower - defencePower));
                    int moves = Math.min(9, 3 + diff / 3);
                    unit.setMovesLeft(unit.getMovesLeft() - moves);
                    reply.setAttribute("movesSlowed", Integer.toString(moves));
                    reply.setAttribute("slowedBy", attacker.getId());
                }
            }
        }
        

        if (player.isEuropean()) {
            Region region = newTile.getDiscoverableRegion();
            if (region != null &&
                (region.isPacific() ||
                 getGame().getGameOptions().getBoolean(GameOptions.EXPLORATION_POINTS))) {
                String name;
                if (region.isPacific()) {
                    name = region.getDisplayName();
                } else {
                    name = moveElement.getAttribute("regionName");
                    if (name == null || "".equals(name)) {
                        name = player.getDefaultRegionName(region.getType());
                    }
                }
                region.discover(player, getGame().getTurn(), name);
                reply.appendChild(region.toXMLElement(player, reply.getOwnerDocument()));

                Element updateElement = Message.createNewRootElement("update");
                updateElement.appendChild(region.toXMLElement(player, updateElement.getOwnerDocument()));
                freeColServer.getServer().sendToAll(updateElement, player.getConnection());
            }
        }


        List<Tile> surroundingTiles = getGame().getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());
        for (int i = 0; i < surroundingTiles.size(); i++) {
            Tile t = surroundingTiles.get(i);
            reply.appendChild(t.toXMLElement(player, reply.getOwnerDocument()));
        }
        if (newTile.hasLostCityRumour() && player.isEuropean()) {
            newTile.setLostCityRumour(false);
            exploreLostCityRumour(unit, player);
        }
        return reply;
    }

    /**
     * Returns a type of Lost City Rumour. The type of rumour depends on the
     * exploring unit, as well as player settings.
     * 
     * @param unit The <code>Unit</code> exploring the lost city rumour.
     * @param player The <code>ServerPlayer</code> to send a message to.
     */
    private void exploreLostCityRumour(Unit unit, ServerPlayer player) {
        //TODO: Move this magic values to the specification file
        
        //The following arrays contain percentage values for "good"
        //and "bad" events when scouting with a non-expert at the various
        //difficulty levels [0..4]
        //exact values TODO, but generally "bad" should increase, "good" decrease
        final int BAD_EVENT_PERCENTAGE[]  = { 11, 17, 23, 30, 37};
        final int GOOD_EVENT_PERCENTAGE[] = { 75, 62, 48, 33, 17};
        //remaining to 100, event "NOTHING":  14, 21, 29, 37, 46
        
        //The following arrays contain the modifiers applied when expert scout is at work
        //exact values TODO; modifiers may look slightly "better" on harder levels
        //since we're starting from a "worse" percentage
        final int BAD_EVENT_MOD[]  = {-6, -7, -7, -8, -9};
        final int GOOD_EVENT_MOD[] = {14, 15, 16, 18, 20};

        Tile tile = unit.getTile();        
        Specification specification = FreeCol.getSpecification();
        List<UnitType> learntUnitTypes = unit.getType().getUnitTypesLearntInLostCity();
        List<UnitType> newUnitTypes = specification.getUnitTypesWithAbility("model.ability.foundInLostCity");
        List<UnitType> treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");

        //the scouting outcome is based on three factors: level, expert scout or not, DeSoto or not
        int level = Specification.getSpecification().getRangeOption("model.option.difficulty").getValue();
        boolean isExpertScout = unit.hasAbility("model.ability.expertScout") && 
                                unit.hasAbility("model.ability.scoutIndianSettlement");
        boolean hasDeSoto = player.hasAbility("model.ability.rumoursAlwaysPositive");

        //based on that, we're going to calculate probabilites for neutral, bad and good events
        int percentNeutral = 0;
        int percentBad;
        int percentGood;
        
        if (hasDeSoto) {
            percentBad     = 0;
            percentGood    = 100;
        } else {
            //First, get "basic" percentages
            percentBad  = BAD_EVENT_PERCENTAGE[level];
            percentGood = GOOD_EVENT_PERCENTAGE[level];
        
            //Second, apply ExpertScout bonus if necessary
            if (isExpertScout) {
                percentBad += BAD_EVENT_MOD[level];
                percentGood += GOOD_EVENT_MOD[level];
            }

            //Third, get a value for the "neutral" percentage,
            //unless the other values exceed 100 already
            if (percentBad+percentGood<100) {
                percentNeutral = 100-percentBad-percentGood;
            }
        }

        //Now, the individual events; each section should add up to 100
        //The NEUTRAL
        int eventNothing = 100;

        //The BAD
        int eventVanish = 100;
        int eventBurialGround = 0;
        //if the tile is native-owned, allow burial grounds rumour
        if (tile.getOwner() != null && !tile.getOwner().isEuropean()) {
            eventVanish = 75;
            eventBurialGround = 25;
        }

        //The GOOD
        int eventLearn    = 30;
        int eventTrinkets = 30;
        int eventColonist = 20;
        //or, if the unit can't learn
        if (learntUnitTypes.isEmpty()) {
            eventLearn    =  0;
            eventTrinkets = 50;
            eventColonist = 30;
        }

        //The SPECIAL
        //TODO: Right now, these are considered "good" events that happen randomly
        //Change this to have them appear a fixed (per specifications) amount of times throughout the game
        int eventDorado   = 13;
        int eventFountain =  7;

        //Finally, apply the Good/Bad/Neutral modifiers from above,
        //so that we end up with a ton of values, some of them zero,
        //the sum of which should be 10000.
        eventNothing      *= percentNeutral;
        eventVanish       *= percentBad;
        eventBurialGround *= percentBad;
        eventLearn        *= percentGood;
        eventTrinkets     *= percentGood;
        eventColonist     *= percentGood;
        eventDorado       *= percentGood;
        eventFountain     *= percentGood;

        //Add all possible events to a RandomChoice List
        List<RandomChoice<RumourType>> choices = new ArrayList<RandomChoice<RumourType>>();
        if (eventNothing>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.NOTHING, eventNothing));
        }
        if (eventVanish>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.EXPEDITION_VANISHES, eventVanish));
        }
        if (eventBurialGround>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.BURIAL_GROUND, eventBurialGround));
        }
        if (eventLearn>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.LEARN, eventLearn));
        }
        if (eventTrinkets>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.TRIBAL_CHIEF, eventTrinkets));
        }
        if (eventColonist>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.COLONIST, eventColonist));
        }
        if (eventFountain>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.FOUNTAIN_OF_YOUTH, eventFountain));
        }
        if (eventDorado>0) {
            choices.add(new RandomChoice<RumourType>(RumourType.TREASURE, eventDorado));
        }
        RumourType rumour = RandomChoice.getWeightedRandom(getPseudoRandom(), choices);

        Element rumourElement = Message.createNewRootElement("lostCityRumour");
        rumourElement.setAttribute("type", rumour.toString());
        rumourElement.setAttribute("unit", unit.getId());
        Unit newUnit;
        int random;
        int dx = 10 - level;
        switch (rumour) {
        case BURIAL_GROUND:
            Player indianPlayer = tile.getOwner();
            indianPlayer.modifyTension(player, Tension.Level.HATEFUL.getLimit());
            break;
        case EXPEDITION_VANISHES:
            unit.dispose();
            break;
        case NOTHING:
            break;
        case LEARN:
            random = getPseudoRandom().nextInt(learntUnitTypes.size());
            unit.setType(learntUnitTypes.get(random));
            rumourElement.setAttribute("unitType", learntUnitTypes.get(random).getId());
            break;
        case TRIBAL_CHIEF:
            int amount = getPseudoRandom().nextInt(dx * 10) + dx * 5;
            player.modifyGold(amount);
            rumourElement.setAttribute("amount", Integer.toString(amount));
            break;
        case COLONIST:
            random = getPseudoRandom().nextInt(newUnitTypes.size());
            newUnit = new Unit(getGame(), tile, player, newUnitTypes.get(random), UnitState.ACTIVE);
            rumourElement.appendChild(newUnit.toXMLElement(player, rumourElement.getOwnerDocument()));
            break;
        case TREASURE:
            int treasure = getPseudoRandom().nextInt(dx * 600) + dx * 300;
            random = getPseudoRandom().nextInt(treasureUnitTypes.size());
            newUnit = new Unit(getGame(), tile, player, treasureUnitTypes.get(random), UnitState.ACTIVE);
            newUnit.setTreasureAmount(treasure);
            rumourElement.setAttribute("amount", Integer.toString(treasure));
            rumourElement.appendChild(newUnit.toXMLElement(player, rumourElement.getOwnerDocument()));
            break;
        case FOUNTAIN_OF_YOUTH:
            if (player.getEurope() != null) {
                if (player.hasAbility("model.ability.selectRecruit")) {
                    player.setRemainingEmigrants(dx);
                    rumourElement.setAttribute("emigrants", Integer.toString(dx));
                } else {
                    for (int k = 0; k < dx; k++) {
                        newUnit = new Unit(getGame(), player.getEurope(), player,
                                           player.generateRecruitable(String.valueOf(k)), UnitState.ACTIVE);
                        rumourElement.appendChild(newUnit.toXMLElement(player, rumourElement.getOwnerDocument()));
                    }
                }
            }
            break;
        default:
            throw new IllegalStateException("No such rumour.");
        }
        // tell the player about the result, sendAndWait to avoid a block
        try {
            player.getConnection().sendAndWait(rumourElement);
        } catch (IOException e) {
            logger.warning("Could not send rumour message to: " + player.getName() + " with connection "
                    + player.getConnection());
        }
        // tell everyone the rumour has been explored
        for (ServerPlayer updatePlayer : getOtherPlayers(player)) {
            if (updatePlayer.canSee(tile)) {
                try {
                    Element rumourUpdate = Message.createNewRootElement("update");
                    rumourUpdate.appendChild(tile.toXMLElement(updatePlayer, rumourUpdate.getOwnerDocument()));
                    updatePlayer.getConnection().send(rumourUpdate);
                } catch (IOException e) {
                    logger.warning("Could not send update message to: " + updatePlayer.getName() + " with connection "
                                   + updatePlayer.getConnection());
                }
            }
        }
    }
    
    /**
     * Handles an "selectFromFountainYouth"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element selectFromFountainYouth(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        int remaining = player.getRemainingEmigrants();
        if (remaining == 0) {
            throw new IllegalStateException("There is no remaining emigrants for this player.");
        }
        player.setRemainingEmigrants(remaining-1);
        
        Europe europe = player.getEurope();
        int slot = Integer.parseInt(element.getAttribute("slot"));
        UnitType recruitable = europe.getRecruitable(slot);
        UnitType newRecruitable = player.generateRecruitable(String.valueOf(remaining));
        europe.setRecruitable(slot, newRecruitable);
        
        Unit unit = new Unit(getGame(), europe, player, recruitable, UnitState.ACTIVE);
        Element reply = Message.createNewRootElement("selectFromFountainYouthConfirmed");
        reply.setAttribute("newRecruitable", newRecruitable.getId());
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));
        return reply;
    }

    /**
     * Handles an "askSkill"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element askSkill(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getMovesLeft() == 0) {
            throw new IllegalArgumentException("Unit has no moves left.");
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' not on map: ID: " + element.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile())
                .getSettlement();
        
        unit.setMovesLeft(0);
        Element reply = Message.createNewRootElement("provideSkill");
        if (settlement.getLearnableSkill() != null) {
            reply.setAttribute("skill", settlement.getLearnableSkill().getId());
        }
        // Set the Tile.PlayerExploredTile attribute.
        settlement.getTile().updateIndianSettlementSkill(player);
        return reply;
    }

    /**
     * Handles an "attack"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param attackElement The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element attack(Connection connection, Element attackElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        // Get parameters:
        String unitID = attackElement.getAttribute("unit");
        Unit unit = (Unit) getGame().getFreeColGameObject(unitID);
        Direction direction = Enum.valueOf(Direction.class, attackElement.getAttribute("direction"));
        // Test the parameters:
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + unitID);
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' is not on the map: " + unit.toString());
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
        if (newTile == null) {
            throw new IllegalArgumentException("Could not find tile in direction " + direction + " from unit with ID "
                    + unitID);
        }
        CombatResult result;
        int plunderGold = -1;
        Unit defender = newTile.getDefendingUnit(unit);
        if (defender == null) {
            if (newTile.getSettlement() != null) {
                result = new CombatResult(CombatResultType.DONE_SETTLEMENT, 0);
            } else {
                throw new IllegalStateException("Nothing to attack in direction " + direction + " from unit with ID "
                        + unitID);
            }
        } else {
            result = unit.getGame().getCombatModel().generateAttackResult(unit, defender); 
        }
        if (result.type == CombatResultType.DONE_SETTLEMENT) {
            // 10% of their gold
            plunderGold = newTile.getSettlement().getOwner().getGold() / 10;
        }
        // Inform the players (other then the player attacking) about
        // the attack:
        for (ServerPlayer enemyPlayer : getOtherPlayers(player)) {
            Element opponentAttackElement = Message.createNewRootElement("opponentAttack");
            if (unit.isVisibleTo(enemyPlayer) || defender.isVisibleTo(enemyPlayer)) {
                opponentAttackElement.setAttribute("direction", direction.toString());
                opponentAttackElement.setAttribute("result", result.type.toString());
                opponentAttackElement.setAttribute("damage", String.valueOf(result.damage));
                opponentAttackElement.setAttribute("plunderGold", Integer.toString(plunderGold));
                opponentAttackElement.setAttribute("unit", unit.getId());
                opponentAttackElement.setAttribute("defender", defender.getId());
                if (defender.getOwner() == enemyPlayer) {
                    // always update the attacker, defender needs its location
                    opponentAttackElement.setAttribute("update", "unit");
                    opponentAttackElement.appendChild(unit.toXMLElement(enemyPlayer,
                            opponentAttackElement.getOwnerDocument()));
                } else if (!defender.isVisibleTo(enemyPlayer)) {
                    opponentAttackElement.setAttribute("update", "defender");
                    if (!enemyPlayer.canSee(defender.getTile())) {
                        enemyPlayer.setExplored(defender.getTile());
                        opponentAttackElement.appendChild(defender.getTile()
                            .toXMLElement(enemyPlayer, opponentAttackElement.getOwnerDocument()));
                    }
                    opponentAttackElement.appendChild(defender.toXMLElement(enemyPlayer,
                            opponentAttackElement.getOwnerDocument()));
                } else if (!unit.isVisibleTo(enemyPlayer)) {
                    opponentAttackElement.setAttribute("update", "unit");
                    opponentAttackElement.appendChild(unit.toXMLElement(enemyPlayer,
                            opponentAttackElement.getOwnerDocument()));
                }
                try {
                    enemyPlayer.getConnection().send(opponentAttackElement);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + enemyPlayer.getName()
                                   + " with connection " + enemyPlayer.getConnection());
                }
            }
        }
        // Create the reply for the attacking player:
        Element reply = Message.createNewRootElement("attackResult");
        reply.setAttribute("result", result.type.toString());
        reply.setAttribute("damage", String.valueOf(result.damage));
        reply.setAttribute("plunderGold", Integer.toString(plunderGold));
        
        if (result.type == CombatResultType.DONE_SETTLEMENT && newTile.getColony() != null) {
            // If a colony will been won, send an updated tile:
            reply.appendChild(newTile.toXMLElement(newTile.getColony().getOwner(), reply.getOwnerDocument()));
            reply.appendChild(defender.toXMLElement(newTile.getColony().getOwner(), reply.getOwnerDocument()));
        } else {
            reply.appendChild(defender.toXMLElement(player, reply.getOwnerDocument()));
        }
        
        Hashtable<String, Integer> oldGoodsCounts = new Hashtable<String, Integer>();
        if (unit.canCaptureGoods() && getGame().getGameOptions().getBoolean(GameOptions.UNIT_HIDING)) {
            List<Goods> goodsInUnit = unit.getGoodsContainer().getFullGoods();
            for (Goods goods : goodsInUnit) {
                oldGoodsCounts.put(goods.getType().getId(), goods.getAmount());
            }
        }
        int oldUnits = unit.getTile().getUnitCount();
        unit.getGame().getCombatModel().attack(unit, defender, result, plunderGold);
        
        if (result.type.compareTo(CombatResultType.WIN) >= 0 
            && unit.getTile() != newTile
            && oldUnits < unit.getTile().getUnitCount()) {
            // If unit won, didn't move, there are more units,
            // then if the last one is not European it must be a convert
            // (not a combat captive), so send it
            Unit lastUnit = unit.getTile().getLastUnit();
            if (!lastUnit.getOwner().isEuropean()) {
                Element convertElement = reply.getOwnerDocument().createElement("convert");
                convertElement.appendChild(lastUnit.toXMLElement(unit.getOwner(), reply.getOwnerDocument()));
                reply.appendChild(convertElement);
            }
        }
        
        // Send capturedGoods if UNIT_HIDING is true because when it's
        // false unit is already sent with carried goods and units
        if (unit.canCaptureGoods() && getGame().getGameOptions().getBoolean(GameOptions.UNIT_HIDING)) {
            List<Goods> goodsInUnit = unit.getGoodsContainer().getCompactGoods();
            for (Goods newGoods : goodsInUnit) {
                Integer oldGoodsAmount = oldGoodsCounts.get(newGoods.getType().getId());
                int capturedGoods = newGoods.getAmount() - (oldGoodsAmount!=null?oldGoodsAmount.intValue():0);
                if (capturedGoods > 0) {
                    Element captured = reply.getOwnerDocument().createElement("capturedGoods");
                    captured.setAttribute("type", newGoods.getType().getId());
                    captured.setAttribute("amount", Integer.toString(capturedGoods));
                    reply.appendChild(captured);
                }
            }
        }

        if (result.type.compareTo(CombatResultType.EVADES) >= 0 && unit.getTile().equals(newTile)) {
            // In other words, we moved...
            Element update = reply.getOwnerDocument().createElement("update");
            int lineOfSight = unit.getLineOfSight();
            if (result.type == CombatResultType.DONE_SETTLEMENT && newTile.getSettlement() != null) {
                lineOfSight = Math.max(lineOfSight, newTile.getSettlement().getLineOfSight());
            }
            List<Tile> surroundingTiles = getGame().getMap().getSurroundingTiles(unit.getTile(), lineOfSight);
            for (int i = 0; i < surroundingTiles.size(); i++) {
                Tile t = surroundingTiles.get(i);
                update.appendChild(t.toXMLElement(player, update.getOwnerDocument()));
            }
            update.appendChild(unit.getTile().toXMLElement(player, update.getOwnerDocument()));
            reply.appendChild(update);
        }
        return reply;
    }

    /**
     * Handles an "embark"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param embarkElement The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     */
    private Element embark(Connection connection, Element embarkElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(embarkElement.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, embarkElement.getAttribute("direction"));
        Unit destinationUnit = (Unit) getGame().getFreeColGameObject(embarkElement.getAttribute("embarkOnto"));
        if (unit == null || destinationUnit == null
                || getGame().getMap().getNeighbourOrNull(direction, unit.getTile()) != destinationUnit.getTile()) {
            throw new IllegalArgumentException("Invalid data format in client message.");
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile oldTile = unit.getTile();
        unit.embark(destinationUnit);
        sendRemoveUnitToAll(unit, player);
        return null;
    }

    /**
     * Handles an "boardShip"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param boardShipElement The element containing the request.
     */
    private Element boardShip(Connection connection, Element boardShipElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(boardShipElement.getAttribute("unit"));
        Unit carrier = (Unit) getGame().getFreeColGameObject(boardShipElement.getAttribute("carrier"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile oldTile = unit.getTile();
        boolean tellEnemyPlayers = true;
        if (oldTile == null || oldTile.getSettlement() != null) {
            tellEnemyPlayers = false;
        }
        if (unit.isCarrier()) {
            logger.warning("Tried to load a carrier onto another carrier.");
            return null;
        }
        unit.boardShip(carrier);
        if (tellEnemyPlayers) {
            sendRemoveUnitToAll(unit, player);
        }
        // For updating the number of colonist:
        sendUpdatedTileToAll(unit.getTile(), player);
        return null;
    }

    /**
     * Handles a "learnSkillAtSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element learnSkillAtSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        boolean cancelAction = false;
        if (element.getAttribute("action").equals("cancel")) {
            cancelAction = true;
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' not on map: ID: " + element.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        IndianSettlement settlement = (IndianSettlement) tile.getSettlement();
        if (settlement == null) {
            throw new IllegalStateException("No settlement to learn skill from.");
        }
        if (!unit.getType().canBeUpgraded(settlement.getLearnableSkill(),
                                          UnitType.UpgradeType.NATIVES)) {
            throw new IllegalStateException("Unit can't learn that skill from settlement!");
        }
        
        Element reply = Message.createNewRootElement("learnSkillResult");
        if (!cancelAction) {
            Tension tension = settlement.getAlarm(player);
            if (tension == null) {
                tension = new Tension(0);
            }
            switch (tension.getLevel()) {
            case HATEFUL:
                reply.setAttribute("result", "die");
                unit.dispose();
                break;
            case ANGRY:
                reply.setAttribute("result", "leave");
                break;
            default:
                unit.setType(settlement.getLearnableSkill());
                if (!settlement.isCapital())
                    settlement.setLearnableSkill(null);
                // Set the Tile.PlayerExploredTile attribute.
                settlement.getTile().updateIndianSettlementSkill(player);
                reply.setAttribute("result", "success");
            }
        } else {
            reply.setAttribute("result", "cancelled");
        }
        return reply;
    }

    /**
     * Handles a "scoutIndianSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element scoutIndianSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        String action = element.getAttribute("action");
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();
        Element reply = Message.createNewRootElement("scoutIndianSettlementResult");
        if (action.equals("basic")) {
            unit.contactAdjacent(settlement.getTile());
            unit.setMovesLeft(0);
            // Just return the skill and wanted goods.
            UnitType skill = settlement.getLearnableSkill();
            if (skill != null) {
                reply.setAttribute("skill", skill.getId());
            }
            settlement.updateWantedGoods();
            GoodsType[] wantedGoods = settlement.getWantedGoods();
            reply.setAttribute("highlyWantedGoods", wantedGoods[0].getId());
            reply.setAttribute("wantedGoods1", wantedGoods[1].getId());
            reply.setAttribute("wantedGoods2", wantedGoods[2].getId());
            reply.setAttribute("numberOfCamps", String.valueOf(settlement.getOwner().getSettlements().size()));
            for (Tile tile : getGame().getMap().getSurroundingTiles(settlement.getTile(), unit.getLineOfSight())) {
                reply.appendChild(tile.toXMLElement(player, reply.getOwnerDocument()));
            }
            // Set the Tile.PlayerExploredTile attribute.
            settlement.getTile().updateIndianSettlementInformation(player);
        } else if (action.equals("cancel")) {
            return null;
        } else if (action.equals("attack")) {
            // The movesLeft has been set to 0 when the scout
            // initiated its action.  If it wants to attack then it
            // can and it will need some moves to do it.
            unit.setMovesLeft(1);
            return null;
        } else if (settlement.getAlarm(player) != null &&
                   settlement.getAlarm(player).getLevel() == Tension.Level.HATEFUL) {
            reply.setAttribute("result", "die");
            unit.dispose();
        } else if (action.equals("speak")) {
            if (!settlement.hasBeenVisited()) {
                // This can probably be randomized, I don't think the AI needs
                // to do anything here.
                double random = Math.random();
                if (random < 0.33) {
                    reply.setAttribute("result", "tales");
                    Element update = reply.getOwnerDocument().createElement("update");
                    Position center = new Position(settlement.getTile().getX(), settlement.getTile().getY());
                    Iterator<Position> circleIterator = map.getCircleIterator(center, true, 6);
                    while (circleIterator.hasNext()) {
                        Position position = circleIterator.next();
                        if ((!position.equals(center))
                                && (map.getTile(position).isLand() || map.getTile(position).getLandCount() > 0)) {
                            Tile t = map.getTile(position);
                            player.setExplored(t);
                            update.appendChild(t.toXMLElement(player, update.getOwnerDocument(), true, false));
                        }
                    }
                    reply.appendChild(update);
                } else {
                    int beadsGold = (int) (Math.random() * (400 * settlement.getBonusMultiplier())) + 50;
                    if (unit.hasAbility("model.ability.expertScout")) {
                        beadsGold = (beadsGold * 11) / 10;
                    }
                    reply.setAttribute("result", "beads");
                    reply.setAttribute("amount", Integer.toString(beadsGold));
                    player.modifyGold(beadsGold);
                }
                settlement.setVisited(player);
            } else {
                reply.setAttribute("result", "nothing");
            }
        } else if (action.equals("tribute")) {
            demandTribute(settlement, player, reply);
        }
        return reply;
    }

    /**
     * Handles a "armedUnitDemandTribute"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element armedUnitDemandTribute(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        // Get parameters:
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        // Test the parameters:
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' is not on the map: " + unit.toString());
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
        if (newTile == null) {
            throw new IllegalArgumentException("Could not find tile in direction " + direction + " from unit with ID "
                    + element.getAttribute("unit"));
        }
        unit.setMovesLeft(0);
        IndianSettlement settlement = (IndianSettlement) newTile.getSettlement();
        Element reply = Message.createNewRootElement("armedUnitDemandTributeResult");
        demandTribute(settlement, player, reply);
        return reply;
    }


    /**
     * Demands a tribute to an <code>IndianSettlement</code>
     * 
     * @param settlement The <code>IndianSettlement</code> whom demand the
     *            tribute to
     * @param player The <code>Player</code> which demands the tribute
     * @param reply The element to add the result
     */
    private void demandTribute(IndianSettlement settlement, Player player, Element reply) {
        int gold = settlement.getTribute(player);
        if (gold > 0) {
            reply.setAttribute("result", "agree");
            reply.setAttribute("amount", String.valueOf(gold));
            player.modifyGold(gold);
        } else {
            reply.setAttribute("result", "disagree");
        }
    }


    /**
     * Handles a "missionaryAtSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element missionaryAtSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        InGameController inGameController = freeColServer.getInGameController();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        String action = element.getAttribute("action");
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile())
                .getSettlement();
        unit.setMovesLeft(0);
        if (action.equals("cancel")) {
            return null;
        } else if (action.equals("establish")) {
            sendRemoveUnitToAll(unit, player);
            
            boolean success = inGameController.createMission(settlement,unit);
            
        	Element reply = Message.createNewRootElement("missionaryReply");
        	reply.setAttribute("success", String.valueOf(success));
        	reply.setAttribute("tension", settlement.getAlarm(unit.getOwner()).getLevel().toString());
            return reply;
        } else if (action.equals("heresy")) {
            Element reply = Message.createNewRootElement("missionaryReply");
            sendRemoveUnitToAll(unit, player);
            double random = Math.random() * settlement.getMissionary().getOwner().getCrosses() /
                (unit.getOwner().getCrosses() + 1);
            if (settlement.getMissionary().hasAbility("model.ability.expertMissionary")) {
                random += 0.2;
            }
            if (unit.hasAbility("model.ability.expertMissionary")) {
                random -= 0.2;
            }
            if (random < 0.5) {
            	boolean success = inGameController.createMission(settlement,unit);
            	reply.setAttribute("success", String.valueOf(success));
            	reply.setAttribute("tension", settlement.getAlarm(unit.getOwner()).getLevel().toString());    
            } else {
                reply.setAttribute("success", "false");
                unit.dispose();
            }
            return reply;
        } else if (action.equals("incite")) {
            Element reply = Message.createNewRootElement("missionaryReply");
            Player enemy = (Player) getGame().getFreeColGameObject(element.getAttribute("incite"));
            reply.setAttribute("amount", String.valueOf(Game.getInciteAmount(player, enemy, settlement.getOwner())));
            // Move the unit into the settlement while we wait for the client's
            // response.
            unit.setLocation(settlement);
            return reply;
        } else {
            return null;
        }
    }

    /**
     * Handles a "inciteAtSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element inciteAtSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        String confirmed = element.getAttribute("confirmed");
        IndianSettlement settlement = (IndianSettlement) unit.getTile().getSettlement();
        // Move the unit back to its original Tile.
        unit.setLocation(map.getNeighbourOrNull(direction.getReverseDirection(), unit.getTile()));
        if (confirmed.equals("true")) {
            Player enemy = (Player) getGame().getFreeColGameObject(element.getAttribute("enemy"));
            int amount = Game.getInciteAmount(player, enemy, settlement.getOwner());
            if (player.getGold() < amount) {
                throw new IllegalStateException("Not enough gold to incite indians!");
            } else {
                player.modifyGold(-amount);
            }
            // Set the indian player at war with the european player (and vice
            // versa).
            settlement.getOwner().changeRelationWithPlayer(enemy, Stance.WAR);
            // Increase tension levels:
            settlement.modifyAlarm(enemy, 1000); // let propagation works
            enemy.modifyTension(settlement.getOwner(), 500);
            enemy.modifyTension(player, 250);
        }
        // else: no need to do anything: unit's moves are already zero.
        return null;
    }

    /**
     * Handles a "leaveShip"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param leaveShipElement The element containing the request.
     */
    private Element leaveShip(Connection connection, Element leaveShipElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(leaveShipElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        unit.leaveShip();
        Tile newTile = unit.getTile();
        if (newTile != null) {
            sendUpdatedTileToAll(newTile, player);
        }
        return null;
    }

    /**
     * Handles a "loadCargo"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param loadCargoElement The element containing the request.
     */
    private Element loadCargo(Connection connection, Element loadCargoElement) {
        Unit carrier = (Unit) getGame().getFreeColGameObject(loadCargoElement.getAttribute("carrier"));
        Goods goods = new Goods(getGame(), (Element) loadCargoElement.getChildNodes().item(0));
        goods.loadOnto(carrier);
        return null;
    }

    /**
     * Handles an "unloadCargo"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param unloadCargoElement The element containing the request.
     */
    private Element unloadCargo(Connection connection, Element unloadCargoElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Goods goods = new Goods(getGame(), (Element) unloadCargoElement.getChildNodes().item(0));
        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getColony() != null) {
            goods.unload();
        } else {
            goods.setLocation(null);
        }
        return null;
    }

    /**
     * Handles a "buyGoods"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param buyGoodsElement The element containing the request.
     */
    private Element buyGoods(Connection connection, Element buyGoodsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit carrier = (Unit) getGame().getFreeColGameObject(buyGoodsElement.getAttribute("carrier"));
        GoodsType type = FreeCol.getSpecification().getGoodsType(buyGoodsElement.getAttribute("type"));
        int amount = Integer.parseInt(buyGoodsElement.getAttribute("amount"));
        if (carrier.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (carrier.getOwner() != player) {
            throw new IllegalStateException();
        }
        carrier.buyGoods(type, amount);
       
        Element marketElement = Message.createNewRootElement("marketElement");
        marketElement.setAttribute("type", type.getId());
        marketElement.setAttribute("amount", String.valueOf(-amount/4));
        getFreeColServer().getServer().sendToAll(marketElement, player.getConnection());
        return null;
    }

    /**
     * Handles a "sellGoods"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param sellGoodsElement The element containing the request.
     */
    private Element sellGoods(Connection connection, Element sellGoodsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Goods goods = new Goods(getGame(), (Element) sellGoodsElement.getChildNodes().item(0));
        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        player.getMarket().sell(goods, player);

        Element marketElement = Message.createNewRootElement("marketElement");
        marketElement.setAttribute("type", goods.getType().getId());
        marketElement.setAttribute("amount", String.valueOf(goods.getAmount()/4));
        getFreeColServer().getServer().sendToAll(marketElement, player.getConnection());
        return null;
    }

    /**
     * Handles a "moveToEurope"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param moveToEuropeElement The element containing the request.
     */
    private Element moveToEurope(Connection connection, Element moveToEuropeElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(moveToEuropeElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        // Inform other players the unit is moving off the map
        sendRemoveUnitToAll(unit, player);
        
        Tile oldTile = unit.getTile();
        unit.moveToEurope();
        return null;
    }

    /**
     * Handles a "moveToAmerica"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param moveToAmericaElement The element containing the request.
     */
    private Element moveToAmerica(Connection connection, Element moveToAmericaElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(moveToAmericaElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        unit.moveToAmerica();
        return null;
    }

    /**
     * Handles a "buildColony"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param buildColonyElement The element containing the request.
     */
    private Element buildColony(Connection connection, Element buildColonyElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        String name = buildColonyElement.getAttribute("name");
        Unit unit = (Unit) getGame().getFreeColGameObject(buildColonyElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.canBuildColony()) {
            Colony colony = new Colony(getGame(), player, name, unit.getTile());
            Element reply = Message.createNewRootElement("buildColonyConfirmed");
            reply.appendChild(colony.toXMLElement(player, reply.getOwnerDocument()));
            Element updateElement = reply.getOwnerDocument().createElement("update");
            int range = colony.getLineOfSight();
            if (range > unit.getLineOfSight()) {
                for (Tile t : getGame().getMap().getSurroundingTiles(unit.getTile(), range)) {
                    updateElement.appendChild(t.toXMLElement(player, reply.getOwnerDocument()));
                }
            }
            updateElement.appendChild(unit.getTile().toXMLElement(player, reply.getOwnerDocument()));
            reply.appendChild(updateElement);
            unit.buildColony(colony);
            sendUpdatedTileToAll(unit.getTile(), player);
            return reply;
        } else {
            logger.warning("A client is requesting to build a colony, but the operation is not permitted! (unsynchronized?)");
            return null;
        }
    }

    /**
     * Handles a "recruitUnitInEurope"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param recruitUnitInEuropeElement The element containing the request.
     */
    private Element recruitUnitInEurope(Connection connection, Element recruitUnitInEuropeElement) {
        Player player = getFreeColServer().getPlayer(connection);
        Europe europe = player.getEurope();
        int slot = Integer.parseInt(recruitUnitInEuropeElement.getAttribute("slot"));
        UnitType recruitable = europe.getRecruitable(slot);
        UnitType newRecruitable = player.generateRecruitable("abc" + getPseudoRandom().nextInt(10000));
        Unit unit = new Unit(getGame(), europe, player, recruitable, UnitState.ACTIVE, recruitable.getDefaultEquipment());
        Element reply = Message.createNewRootElement("recruitUnitInEuropeConfirmed");
        reply.setAttribute("newRecruitable", newRecruitable.getId());
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));
        europe.recruit(slot, unit, newRecruitable);
        return reply;
    }

    /**
     * Handles an "emigrateUnitInEurope"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param emigrateUnitInEuropeElement The element containing the request.
     */
    private Element emigrateUnitInEurope(Connection connection, Element emigrateUnitInEuropeElement) {
        Player player = getFreeColServer().getPlayer(connection);
        Europe europe = player.getEurope();
        int slot;
        if (player.hasAbility("model.ability.selectRecruit")) {
            slot = Integer.parseInt(emigrateUnitInEuropeElement.getAttribute("slot"));
        } else {
            slot = (int) (Math.random() * 3);
        }
        UnitType recruitable = europe.getRecruitable(slot);
        UnitType newRecruitable = player.generateRecruitable("xyzzy" + getPseudoRandom().nextInt(10000));
        Unit unit = new Unit(getGame(), europe, player, recruitable, UnitState.ACTIVE, recruitable.getDefaultEquipment());
        Element reply = Message.createNewRootElement("emigrateUnitInEuropeConfirmed");
        if (!player.hasAbility("model.ability.selectRecruit")) {
            reply.setAttribute("slot", Integer.toString(slot));
        }
        reply.setAttribute("newRecruitable", newRecruitable.getId());
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));
        europe.emigrate(slot, unit, newRecruitable);
        return reply;
    }

    /**
     * Handles a "trainUnitInEurope"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param trainUnitInEuropeElement The element containing the request.
     */
    private Element trainUnitInEurope(Connection connection, Element trainUnitInEuropeElement) {
        Player player = getFreeColServer().getPlayer(connection);
        Europe europe = player.getEurope();
        String unitId = trainUnitInEuropeElement.getAttribute("unitType");
        UnitType unitType = FreeCol.getSpecification().getUnitType(unitId);
        Unit unit = new Unit(getGame(), europe, player, unitType, UnitState.ACTIVE, unitType.getDefaultEquipment());
        Element reply = Message.createNewRootElement("trainUnitInEuropeConfirmed");
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));
        europe.train(unit);
        return reply;
    }

    /**
     * Handles a "equipUnit"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element equipUnit(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        String typeString = workElement.getAttribute("type");
        EquipmentType type = FreeCol.getSpecification().getEquipmentType(typeString);
        int amount = Integer.parseInt(workElement.getAttribute("amount"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        if (amount > 0) {
            for (int count = 0; count < amount; count++) {
                unit.equipWith(type);
            }
        } else {
            for (int count = 0; count > amount; count--) {
                unit.removeEquipment(type);
            }
        }

        if (unit.getLocation() instanceof Tile) {
            sendUpdatedTileToAll(unit.getTile(), player);
        }
        return null;
    }

    /**
     * Handles a "work"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element work(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        WorkLocation workLocation = (WorkLocation) getGame().getFreeColGameObject(workElement.getAttribute("workLocation"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (workLocation == null) {
            throw new NullPointerException();
        }
        Location oldLocation = unit.getLocation();
        unit.work(workLocation);
        // For updating the number of colonist:
        sendUpdatedTileToAll(unit.getTile(), player);
        // oldLocation is empty now
        if (oldLocation instanceof ColonyTile) {
            sendUpdatedTileToAll(((ColonyTile) oldLocation).getWorkTile(), player);
        }
        // workLocation is occupied now
        if (workLocation instanceof ColonyTile) {
            sendUpdatedTileToAll(((ColonyTile) workLocation).getWorkTile(), player);
        }
        return null;
    }

    /**
     * Handles a "changeWorkType"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element changeWorkType(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        String workTypeString = workElement.getAttribute("workType");
        if (workTypeString != null) {
            GoodsType workType = FreeCol.getSpecification().getGoodsType(workTypeString);
            // No reason to send an update to other players: this is always hidden.
            unit.setWorkType(workType);

        }
        return null;

    }

    /**
     * Handles a "changeWorkType"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element workImprovement(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile tile = unit.getTile();

        String improvementTypeString = workElement.getAttribute("improvementType");
        if (improvementTypeString != null) {
            Element reply = Message.createNewRootElement("workImprovementConfirmed");

            if (tile.getTileItemContainer() == null) {
                tile.setTileItemContainer(new TileItemContainer(tile.getGame(), tile));
                reply.appendChild(tile.getTileItemContainer().toXMLElement(player, reply.getOwnerDocument()));
            }

            TileImprovementType type = FreeCol.getSpecification().getTileImprovementType(improvementTypeString);
            TileImprovement improvement = unit.getTile().findTileImprovementType(type);
            if (improvement == null) {
                // create new improvement
                improvement = new TileImprovement(getGame(), unit.getTile(), type);
                unit.getTile().add(improvement);
            }
            reply.appendChild(improvement.toXMLElement(player, reply.getOwnerDocument()));
            unit.work(improvement);
            return reply;
        } else {
            return null;
        }

    }

    /**
     * Handles a "assignTeacher"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element assignTeacher(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit student = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("student"));
        Unit teacher = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("teacher"));

        if (!student.canBeStudent(teacher)) {
            throw new IllegalStateException("Unit can not be student!");
        }
        if (!teacher.getColony().canTrain(teacher)) {
            throw new IllegalStateException("Unit can not be teacher!");
        }
        if (student.getOwner() != player) {
            throw new IllegalStateException("Student is not your unit!");
        }
        if (teacher.getOwner() != player) {
            throw new IllegalStateException("Teacher is not your unit!");
        }
        if (student.getColony() != teacher.getColony()) {
            throw new IllegalStateException("Student and teacher are not in the same colony!");
        }
        if (!(student.getLocation() instanceof WorkLocation)) {
            throw new IllegalStateException("Student is not in a WorkLocation!");
        }
        // No reason to send an update to other players: this is always hidden.
        if (student.getTeacher() != null) {
            student.getTeacher().setStudent(null);
        }
        student.setTeacher(teacher);
        if (teacher.getStudent() != null) {
            teacher.getStudent().setTeacher(null);
        }
        teacher.setStudent(student);
        return null;
    }

    /**
     * Handles a "setCurrentlyBuilding"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param setCurrentlyBuildingElement The element containing the request.
     */
    private Element setCurrentlyBuilding(Connection connection, Element setCurrentlyBuildingElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Colony colony = (Colony) getGame().getFreeColGameObject(setCurrentlyBuildingElement.getAttribute("colony"));
        if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your colony!");
        }
        String typeString = setCurrentlyBuildingElement.getAttribute("type");
        BuildableType type = null;
        if (typeString.equals("model.buildableType.nothing"))
            type = BuildableType.NOTHING;
        else
            type = (BuildableType) FreeCol.getSpecification().getType(typeString);
        colony.setCurrentlyBuilding(type);
        sendUpdatedTileToAll(colony.getTile(), player);
        return null;
    }

    /**
     * Handles a "changeState"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param changeStateElement The element containing the request.
     * @return null (always).
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element changeState(Connection connection, Element changeStateElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObjectSafely(changeStateElement.getAttribute("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + changeStateElement.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        UnitState state = Enum.valueOf(UnitState.class, changeStateElement.getAttribute("state"));
        Tile oldTile = unit.getTile();
        if (unit.checkSetState(state)) {
            unit.setState(state);
        } else {
            logger.warning("Can't set state " + state + " for unit " + unit + " with current state " + unit.getState()
                    + " and " + unit.getMovesLeft() + " moves left belonging to " + player
                    + ". Possible cheating attempt (or bug)?");
        }
        // Send the updated tile anyway, we may have a synchronization issue
        sendUpdatedTileToAll(oldTile, player);
        return null;
    }

    /**
     * Handles a "putOutsideColony"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param putOutsideColonyElement The element containing the request.
     */
    private Element putOutsideColony(Connection connection, Element putOutsideColonyElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(putOutsideColonyElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        unit.putOutsideColony();
        sendUpdatedTileToAll(unit.getTile(), player);
        return null;
    }

    /**
     * Handles a "payForBuilding"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param payForBuildingElement The element containing the request.
     */
    private Element payForBuilding(Connection connection, Element payForBuildingElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Colony colony = (Colony) getGame().getFreeColGameObject(payForBuildingElement.getAttribute("colony"));
        if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        colony.payForBuilding();
        return null;
    }

    /**
     * Handles a "payArrears"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param payArrearsElement The element containing the request.
     */
    private Element payArrears(Connection connection, Element payArrearsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        GoodsType goodsType = FreeCol.getSpecification().getGoodsType(payArrearsElement.getAttribute("goodsType"));
        int arrears = player.getArrears(goodsType);
        if (player.getGold() < arrears) {
            throw new IllegalStateException("Not enough gold to pay tax arrears!");
        } else {
            player.modifyGold(-arrears);
            player.resetArrears(goodsType);
        }
        return null;
    }

    /**
     * Handles a "setGoodsLevels"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param setGoodsLevelsElement The element containing the request.
     */
    private Element setGoodsLevels(Connection connection, Element setGoodsLevelsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Colony colony = (Colony) getGame().getFreeColGameObject(setGoodsLevelsElement.getAttribute("colony"));
        if (colony == null) {
            throw new IllegalArgumentException("Found no colony with ID " + setGoodsLevelsElement.getAttribute("colony"));
        } else if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your colony!");
            /**
             * we don't really care whether the colony has a custom house } else
             * if (!colony.getBuilding(Building.CUSTOM_HOUSE).isBuilt()) { throw
             * new IllegalStateException("Colony has no custom house!");
             */
        }
        ExportData exportData = new ExportData();
        exportData.readFromXMLElement((Element) setGoodsLevelsElement.getChildNodes().item(0));
        colony.setExportData(exportData);
        return null;
    }

    /**
     * Handles a "clearSpeciality"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param clearSpecialityElement The element containing the request.
     */
    private Element clearSpeciality(Connection connection, Element clearSpecialityElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(clearSpecialityElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        unit.clearSpeciality();
        if (unit.getLocation() instanceof Tile) {
            sendUpdatedTileToAll(unit.getTile(), player);
        }
        return null;
    }

    /**
     * Handles an "endTurn" notification from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element endTurn(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        getFreeColServer().getInGameController().endTurn(player);
        return null;
    }

    /**
     * Handles a "disbandUnit"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element disbandUnit(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile oldTile = unit.getTile();
        unit.dispose();
        sendUpdatedTileToAll(oldTile, player);
        return null;
    }

    /**
     * Handles a "cashInTreasureTrain"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element cashInTreasureTrain(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile oldTile = unit.getTile();
        unit.cashInTreasureTrain();
        sendUpdatedTileToAll(oldTile, player);
        return null;
    }

    /**
     * Handles a "declareIndependence"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element declareIndependence(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Nation refNation = Specification.getSpecification().getNation(player.getNation().getRefId());
        ServerPlayer refPlayer = getFreeColServer().addAIPlayer(refNation);
        refPlayer.setEntryLocation(player.getEntryLocation());
        Element reply = Message.createNewRootElement("update");
        reply.appendChild(refPlayer.toXMLElement(null, reply.getOwnerDocument()));
        List<Unit> navalUnits = new ArrayList<Unit>();
        for (AbstractUnit unit : player.getMonarch().getNavalUnits()) {
            for (int index = 0; index < unit.getNumber(); index++) {
                Unit newUnit = new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                        unit.getUnitType(), UnitState.TO_AMERICA);
                navalUnits.add(newUnit);
            }
        }
        List<Unit> safeUnits = new ArrayList<Unit>(navalUnits);
        EquipmentType muskets = Specification.getSpecification().getEquipmentType("model.equipment.muskets");
        EquipmentType horses = Specification.getSpecification().getEquipmentType("model.equipment.horses");
        Iterator<Unit> unitIterator = navalUnits.iterator();
        for (AbstractUnit unit : player.getMonarch().getLandUnits()) {
            EquipmentType[] equipment = EquipmentType.NO_EQUIPMENT;
            switch(unit.getRole()) {
            case SOLDIER:
                equipment = new EquipmentType[] { muskets };
                break;
            case DRAGOON:
                equipment = new EquipmentType[] { horses, muskets };
                break;
            default:
            }
            for (int index = 0; index < unit.getNumber(); index++) {
                Unit newUnit = new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                        unit.getUnitType(), UnitState.ACTIVE, equipment);
                while (newUnit != null) {
                    if (!unitIterator.hasNext()) {
                        unitIterator = navalUnits.iterator();
                    }
                    Unit carrier = unitIterator.next();
                    if (newUnit.getSpaceTaken() < carrier.getSpaceLeft()) {
                        newUnit.setLocation(carrier);
                        safeUnits.add(newUnit);
                        newUnit = null;
                    }
                }
            }
        }
        for (Unit unit : safeUnits) {
            reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));
        }
        player.declareIndependence();
        return reply;
    }

    /**
     * Handles a "giveIndependence"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element giveIndependence(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Player independent = (Player) getGame().getFreeColGameObject(element.getAttribute("player"));
        if (independent.getREFPlayer() != player) {
            throw new IllegalStateException("Cannot give independence to a country we do not own.");
        }
        independent.giveIndependence();
        Element giveIndependenceElement = Message.createNewRootElement("giveIndependence");
        giveIndependenceElement.setAttribute("player", independent.getId());
        getFreeColServer().getServer().sendToAll(giveIndependenceElement, connection);
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
            if (enemyPlayer.getConnection() == null || enemyPlayer.isIndian() || enemyPlayer.isREF()) {
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
                    navalStrength += unit.getGame().getCombatModel().getOffencePower(unit, unit);
                } else {
                    militaryStrength += unit.getGame().getCombatModel().getOffencePower(unit, unit);
                }
            }
            Stance stance = enemyPlayer.getStance(player);
            if (stance == null) {
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
     * Handles a "getREFUnits"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element getREFUnits(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        UnitType defaultType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
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
            reply.appendChild(unit.toXMLElement(reply.getOwnerDocument()));
        }
        return reply;
    }

    /**
     * Handles a "setDestination"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element setDestination(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not the owner of the unit.");
        }
        Location destination = null;
        if (element.hasAttribute("destination")) {
            destination = (Location) getGame().getFreeColGameObject(element.getAttribute("destination"));
        }
        unit.setDestination(destination);
        return null;
    }

    /**
     * Handles a "rename"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element rename(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Nameable object = (Nameable) getGame().getFreeColGameObject(element.getAttribute("nameable"));
        if (!(object instanceof Ownable) || ((Ownable) object).getOwner() != player) {
            throw new IllegalStateException("Not the owner of the nameable.");
        }
        object.setName(element.getAttribute("name"));
        return null;
    }

    /**
     * Handles a "tradeProposition"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element tradeProposition(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Settlement settlement = (Settlement) getGame().getFreeColGameObject(element.getAttribute("settlement"));
        Goods goods = new Goods(getGame(), Message.getChildElement(element, Goods.getXMLElementTagName()));
        int gold = -1;
        if (element.hasAttribute("gold")) {
            gold = Integer.parseInt(element.getAttribute("gold"));
        }
        if (goods.getAmount() > 100) {
            throw new IllegalArgumentException("Amount of goods exceeds 100: " + goods.getAmount());
        }
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getMovesLeft() <= 0) {
            throw new IllegalStateException("No moves left!");
        }
        if (settlement == null) {
            throw new IllegalArgumentException("Could not find 'Settlement' with specified ID: "
                    + element.getAttribute("settlement"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Not adjacent to settlemen!");
        }
        int returnGold = ((AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner()))
                .tradeProposition(unit, settlement, goods, gold);
        Element tpaElement = Message.createNewRootElement("tradePropositionAnswer");
        tpaElement.setAttribute("gold", Integer.toString(returnGold));
        return tpaElement;
    }

    /**
     * Handles a "trade"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element trade(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Settlement settlement = (Settlement) getGame().getFreeColGameObject(element.getAttribute("settlement"));
        Goods goods = new Goods(getGame(), Message.getChildElement(element, Goods.getXMLElementTagName()));
        int gold = Integer.parseInt(element.getAttribute("gold"));
        if (gold <= 0) {
            throw new IllegalArgumentException();
        }
        if (goods.getAmount() > 100) {
            throw new IllegalArgumentException();
        }
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getMovesLeft() <= 0) {
            throw new IllegalStateException("No moves left!");
        }
        if (settlement == null) {
            throw new IllegalArgumentException("Could not find 'Settlement' with specified ID: "
                    + element.getAttribute("settlement"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Not adjacent to settlemen!");
        }
        AIPlayer aiPlayer = (AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner());
        int returnGold = aiPlayer.tradeProposition(unit, settlement, goods, gold);
        if (returnGold != gold) {
            throw new IllegalArgumentException("This was not the price we agreed upon! Cheater?");
        }
        unit.trade(settlement, goods, gold);
        
        Element reply = null;
        if (settlement instanceof IndianSettlement) { // offer goods to buy
            List<Goods> sellGoods = ((IndianSettlement) settlement).getSellGoods();
            if (!sellGoods.isEmpty()) {
                Element sellProposition = Message.createNewRootElement("sellProposition");
                for (Goods goodsToSell : sellGoods) {
                    aiPlayer.registerSellGoods(goodsToSell);
                    sellProposition.appendChild(goodsToSell.toXMLElement(null, sellProposition.getOwnerDocument()));
                }
                reply = sellProposition;
            }
        }
        
        return reply;
    }

    /**
     * Handles a "buyProposition"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element buyProposition(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Goods goods = new Goods(getGame(), Message.getChildElement(element, Goods.getXMLElementTagName()));
        IndianSettlement settlement = (IndianSettlement) goods.getLocation();
        int gold = -1;
        if (element.hasAttribute("gold")) {
            gold = Integer.parseInt(element.getAttribute("gold"));
        }
        if (goods.getAmount() > 100) {
            throw new IllegalArgumentException();
        }
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (settlement == null) {
            throw new IllegalArgumentException("Goods are not in a settlement");
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Not adjacent to settlemen!");
        }
        int returnGold = ((AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner()))
                .buyProposition(unit, goods, gold);
        Element tpaElement = Message.createNewRootElement("buyPropositionAnswer");
        tpaElement.setAttribute("gold", Integer.toString(returnGold));
        return tpaElement;
    }

    /**
     * Handles a "buy"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element buy(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Goods goods = new Goods(getGame(), Message.getChildElement(element, Goods.getXMLElementTagName()));
        IndianSettlement settlement = (IndianSettlement) goods.getLocation();
        int gold = Integer.parseInt(element.getAttribute("gold"));
        if (gold <= 0) {
            throw new IllegalArgumentException();
        }
        if (goods.getAmount() > 100) {
            throw new IllegalArgumentException();
        }
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (settlement == null) {
            throw new IllegalArgumentException("Goods are not in a settlement");
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Not adjacent to settlemen!");
        }
        int returnGold = ((AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner()))
                .buyProposition(unit, goods, gold);
        if (returnGold != gold) {
            throw new IllegalArgumentException("This was not the price we agreed upon! Cheater?");
        }
        unit.buy(settlement, goods, gold);
        
        return null;
    }

    /**
     * Handles a "deliverGift"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element deliverGift(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Settlement settlement = (Settlement) getGame().getFreeColGameObject(element.getAttribute("settlement"));
        Goods goods = new Goods(getGame(), Message.getChildElement(element, Goods.getXMLElementTagName()));
        if (goods.getAmount() > 100) {
            throw new IllegalArgumentException();
        }
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getMovesLeft() <= 0) {
            throw new IllegalStateException("No moves left!");
        }
        if (settlement == null) {
            throw new IllegalArgumentException("Could not find 'Settlement' with specified ID: "
                    + element.getAttribute("settlement"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Not adjacent to settlemen!");
        }
        ServerPlayer receiver = (ServerPlayer) settlement.getOwner();
        if (!receiver.isAI() && receiver.isConnected()) {
            Element deliverGiftElement = Message.createNewRootElement("deliverGift");
            Element unitElement = unit.toXMLElement(receiver, deliverGiftElement.getOwnerDocument());
            Element goodsContainerElement = unit.getGoodsContainer().toXMLElement(receiver,
                    deliverGiftElement.getOwnerDocument());
            unitElement.replaceChild(goodsContainerElement, unitElement.getElementsByTagName(
                    GoodsContainer.getXMLElementTagName()).item(0));
            deliverGiftElement.appendChild(unitElement);
            deliverGiftElement.setAttribute("settlement", settlement.getId());
            deliverGiftElement.appendChild(goods.toXMLElement(receiver, deliverGiftElement.getOwnerDocument()));
            try {
                receiver.getConnection().send(deliverGiftElement);
            } catch (IOException e) {
                logger.warning("Could not send \"deliverGift\"-message!");
            }
        }
        unit.deliverGift(settlement, goods);
        return null;
    }

    /**
     * Handles an "indianDemand"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element indianDemand(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Colony colony = (Colony) getGame().getFreeColGameObject(element.getAttribute("colony"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getMovesLeft() <= 0) {
            throw new IllegalStateException("No moves left!");
        }
        if (colony == null) {
            throw new IllegalArgumentException("Could not find 'Colony' with specified ID: "
                    + element.getAttribute("colony"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.getTile().getDistanceTo(colony.getTile()) > 1) {
            throw new IllegalStateException("Not adjacent to colony!");
        }
        ServerPlayer receiver = (ServerPlayer) colony.getOwner();
        if (receiver.isConnected()) {
            int gold = 0;
            Goods goods = null;
            Element goodsElement = Message.getChildElement(element, Goods.getXMLElementTagName());
            if (goodsElement == null) {
                gold = Integer.parseInt(element.getAttribute("gold"));
            } else {
                goods = new Goods(getGame(), goodsElement);
            }
            try {
                Element reply = receiver.getConnection().ask(element);
                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                if (accepted) {
                    if (goods == null) {
                        receiver.modifyGold(-gold);
                    } else {
                        colony.getGoodsContainer().removeGoods(goods);
                    }
                }
                return reply;
            } catch (IOException e) {
                logger.warning("Could not send \"demand\"-message!");
            }
        }
        return null;
    }

    /**
     * Handles an "continuePlaying"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element continuePlaying(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (!getFreeColServer().isSingleplayer()) {
            throw new IllegalStateException("Can't continue playing in multiplayer!");
        }
        if (player != getFreeColServer().getInGameController().checkForWinner()) {
            throw new IllegalStateException("Can't continue playing! Player "
                    + player.getName() + " hasn't won the game");
        }
        GameOptions go = getGame().getGameOptions();
        ((BooleanOption) go.getObject(GameOptions.VICTORY_DEFEAT_REF)).setValue(false);
        ((BooleanOption) go.getObject(GameOptions.VICTORY_DEFEAT_EUROPEANS)).setValue(false);
        ((BooleanOption) go.getObject(GameOptions.VICTORY_DEFEAT_HUMANS)).setValue(false);
        
        // victory panel is shown after end turn, end turn again to start turn of next player
        final ServerPlayer currentPlayer = (ServerPlayer) getFreeColServer().getGame().getCurrentPlayer();
        getFreeColServer().getInGameController().endTurn(currentPlayer);
        return null;
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

    private void sendUpdatedTileToAll(Tile newTile, Player player) {
        // TODO: can Player be null?
        Iterator<Player> enemyPlayerIterator = getGame().getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
            if (player != null && player.equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
                continue;
            }
            try {
                if (enemyPlayer.canSee(newTile)) {
                    Element updateElement = Message.createNewRootElement("update");
                    updateElement.appendChild(newTile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));
                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                        + enemyPlayer.getConnection());
            }
        }
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
