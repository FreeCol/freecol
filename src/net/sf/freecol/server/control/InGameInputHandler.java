
package net.sf.freecol.server.control;

import java.awt.Color;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Random;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.Connection;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
* Handles the network messages that arrives while 
* {@link FreeColServer#IN_GAME in game}.
*/
public final class InGameInputHandler implements MessageHandler {
    private static Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    private FreeColServer freeColServer;
                     
    public static Random attackCalculator;

    /**
    * The constructor to use.
    * @param freeColServer The main control object.
    */
    public InGameInputHandler(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
        attackCalculator = new Random();
    }





    /**
    * Handles a network message.
    *
    * @param connection The <code>Connection</code> the message came from.
    * @param element The message to be processed.
    */
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        String type = element.getTagName();

        try {
            if (element != null) {
                if (freeColServer.getGame().getCurrentPlayer().equals(freeColServer.getPlayer(connection))) {
                    if (type.equals("chat")) {
                        reply = chat(connection, element);
                    } else if (type.equals("move")) {
                        reply = move(connection, element);
                    } else if (type.equals("attack")) {
                        reply = attack(connection, element);
                    } else if (type.equals("embark")) {
                        reply = embark(connection, element);
                    } else if (type.equals("boardShip")) {
                        reply = boardShip(connection, element);
                    } else if (type.equals("leaveShip")) {
                        reply = leaveShip(connection, element);
                    } else if (type.equals("loadCargo")) {
                        reply = loadCargo(connection, element);
                    } else if (type.equals("unloadCargo")) {
                        reply = unloadCargo(connection, element);
                    } else if (type.equals("buyGoods")) {
                        reply = buyGoods(connection, element);
                    } else if (type.equals("sellGoods")) {
                        reply = sellGoods(connection, element);
                    } else if (type.equals("moveToEurope")) {
                        reply = moveToEurope(connection, element);
                    } else if (type.equals("moveToAmerica")) {
                        reply = moveToAmerica(connection, element);
                    } else if (type.equals("buildColony")) {
                        reply = buildColony(connection, element);
                    } else if (type.equals("recruitUnitInEurope")) {
                        reply = recruitUnitInEurope(connection, element);
                    } else if (type.equals("emigrateUnitInEurope")) {
                        reply = emigrateUnitInEurope(connection, element);
                    } else if (type.equals("trainUnitInEurope")) {
                        reply = trainUnitInEurope(connection, element);
                    } else if (type.equals("equipunit")) {
                        reply = equipUnit(connection, element);
                    } else if (type.equals("work")) {
                        reply = work(connection, element);
                    } else if (type.equals("changeWorkType")) {
                        reply = changeWorkType(connection, element);
                    } else if (type.equals("setCurrentlyBuilding")) {
                        reply = setCurrentlyBuilding(connection, element);
                    } else if (type.equals("changeState")) {
                        reply = changeState(connection, element);
                    } else if (type.equals("putOutsideColony")) {
                        reply = putOutsideColony(connection, element);
                    } else if (type.equals("clearSpeciality")) {
                        reply = clearSpeciality(connection, element);
                    } else if (type.equals("endTurn")) {
                        reply = endTurn(connection, element);
                    } else {
                        logger.warning("Unknown request from client " + element.getTagName());
                    }
                } else {
                    reply = Message.createNewRootElement("error");
                    reply.setAttribute("message", "Not your turn.");
                }
            }
        } catch (Exception e) {
            // TODO: Force the client to reconnect.
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.warning(sw.toString());
            
            return null;
        }

        return reply;
    }
    
    
    /**
    * Handles a "chat"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    *
    */
    private Element chat(Connection connection, Element element) {
        // TODO: Add support for private chat.
        element.setAttribute("sender", freeColServer.getPlayer(connection).getID());
        freeColServer.getServer().sendToAll(element, connection);
        return null;
    }


    /**
    * Handles a "move"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param moveElement The element containing the request.
    * @exception IllegalArgumentException If the data format of the message is invalid.
    * @exception IllegalStateException If the request is not accepted by the model.
    *
    */
    private Element move(Connection connection, Element moveElement) {
        Game game = freeColServer.getGame();

        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(moveElement.getAttribute("unit"));
        int direction = Integer.parseInt(moveElement.getAttribute("direction"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + moveElement.getAttribute("unit"));
        }
        
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' not on map: ID: " + moveElement.getAttribute("unit"));
        }

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        
        boolean disembark = !unit.getTile().isLand() && game.getMap().getNeighbourOrNull(direction, unit.getTile()).isLand();

        Tile oldTile = unit.getTile();

        unit.move(direction);

        Tile newTile = unit.getTile();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(oldTile) && !disembark) {
                    Element opponentMoveElement = Message.createNewRootElement("opponentMove");
                    opponentMoveElement.setAttribute("direction", Integer.toString(direction));
                    opponentMoveElement.setAttribute("unit", unit.getID());
                    enemyPlayer.getConnection().send(opponentMoveElement);
                } else if (enemyPlayer.canSee(newTile)) {
                    Element opponentMoveElement = Message.createNewRootElement("opponentMove");
                    opponentMoveElement.setAttribute("direction", Integer.toString(direction));
                    opponentMoveElement.setAttribute("tile", unit.getTile().getID());
                    opponentMoveElement.appendChild(unit.toXMLElement(enemyPlayer, opponentMoveElement.getOwnerDocument()));
                    enemyPlayer.getConnection().send(opponentMoveElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }


        Element reply = Message.createNewRootElement("update");
        Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

        for (int i=0; i<surroundingTiles.size(); i++) {
            Tile t = (Tile) surroundingTiles.get(i);
            player.setExplored(t);
            reply.appendChild(t.toXMLElement(player, reply.getOwnerDocument()));
        }

        return reply;
    }

    /**
    * Handles an "attack"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param moveElement The element containing the request.
    * @exception IllegalArgumentException If the data format of the message is invalid.
    * @exception IllegalStateException If the request is not accepted by the model.
    *
    */
    private Element attack(Connection connection, Element attackElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(attackElement.getAttribute("unit"));
        int direction = Integer.parseInt(attackElement.getAttribute("direction"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + attackElement.getAttribute("unit"));
        }

        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' is not on the map: " + unit.toString());
        }

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        Tile oldTile = unit.getTile();
        Tile newTile = game.getMap().getNeighbourOrNull(direction, unit.getTile());

        if (newTile == null) {
            throw new IllegalArgumentException("Could not find tile in direction " + direction + " from unit with ID " + attackElement.getAttribute("unit"));
        }

        Unit defender = newTile.getDefendingUnit(unit);
        if (defender == null) {
            throw new IllegalStateException("Nothing to attack in direction " + direction + " from unit with ID " + attackElement.getAttribute("unit"));
        }

        // Calculate the result:
        int attack_power = unit.getOffensePower(defender);
        int total_probability = attack_power + defender.getDefensePower(unit);
        int result = Unit.ATTACKER_LOSS; // Assume this until otherwise calculated.

        if ((attackCalculator.nextInt(total_probability+1)) <= attack_power) {
            result = Unit.ATTACKER_WIN;
        }

        // Inform the other players (other then the player attacking) about the attack:
        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            // Send
            if (enemyPlayer.canSee(oldTile) || enemyPlayer.canSee(newTile)) {
                Element opponentAttackElement = Message.createNewRootElement("opponentAttack");
                opponentAttackElement.setAttribute("direction", Integer.toString(direction));
                opponentAttackElement.setAttribute("result", Integer.toString(result));
                opponentAttackElement.setAttribute("unit", unit.getID());

                if (enemyPlayer != unit.getOwner() && enemyPlayer != defender.getOwner()) {
                    if (!enemyPlayer.canSee(oldTile)) {
                        Element updateElement = Message.createNewRootElement("update");
                        updateElement.appendChild(oldTile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));
                        try {
                            enemyPlayer.getConnection().send(updateElement);
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
                        }
                    } else if (!enemyPlayer.canSee(newTile)) {
                        Element updateElement = Message.createNewRootElement("update");
                        updateElement.appendChild(newTile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));
                        try {
                            enemyPlayer.getConnection().send(updateElement);
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
                        }
                    }
                }

                try {
                    enemyPlayer.getConnection().send(opponentAttackElement);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
                }
            }
        }

        // Create the reply:
        Element reply = Message.createNewRootElement("attackResult");
        reply.setAttribute("result", Integer.toString(result));

        // If a colony has been won, send an updated tile:
        if (result == Unit.ATTACKER_WIN && newTile.getColony() != null && defender.getLocation() != defender.getTile()) {
            reply.appendChild(newTile.toXMLElement(newTile.getColony().getOwner(), reply.getOwnerDocument()));
        }

        if (result == Unit.ATTACKER_LOSS) {
            unit.loseAttack();
        } else {
            unit.winAttack(defender);
        }

        if (unit.getTile().equals(newTile)) { // In other words, we moved...
            Element update = reply.getOwnerDocument().createElement("update");
            Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

            for (int i=0; i<surroundingTiles.size(); i++) {
                Tile t = (Tile) surroundingTiles.get(i);
                player.setExplored(t);
                update.appendChild(t.toXMLElement(player, update.getOwnerDocument()));
            }

            reply.appendChild(update);
        }

        return reply;
    }

    /**
    * Handles an "embark"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param embarkElement The element containing the request.
    * @exception IllegalArgumentException If the data format of the message is invalid.
    */
    private Element embark(Connection connection, Element embarkElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(embarkElement.getAttribute("unit"));
        int direction = Integer.parseInt(embarkElement.getAttribute("direction"));
        Unit destinationUnit = (Unit) game.getFreeColGameObject(embarkElement.getAttribute("embarkOnto"));

        if (unit == null || destinationUnit == null || game.getMap().getNeighbourOrNull(direction, unit.getTile()) != destinationUnit.getTile()) {
            throw new IllegalArgumentException("Invalid data format in client message.");
        }

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        Tile oldTile = unit.getTile();

        unit.embark(destinationUnit);

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(oldTile)) {
                    Element removeElement = Message.createNewRootElement("remove");
                    
                    Element removeUnit = removeElement.getOwnerDocument().createElement("removeObject");
                    removeUnit.setAttribute("ID", unit.getID());
                    removeElement.appendChild(removeUnit);

                    enemyPlayer.getConnection().send(removeElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }

        return null;
    }


    /**
    * Handles an "boardShip"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param boardShipElement The element containing the request.
    */
    private Element boardShip(Connection connection, Element boardShipElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(boardShipElement.getAttribute("unit"));
        Unit carrier = (Unit) game.getFreeColGameObject(boardShipElement.getAttribute("carrier"));

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
            Iterator enemyPlayerIterator = game.getPlayerIterator();
            while (enemyPlayerIterator.hasNext()) {
                ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

                if (player.equals(enemyPlayer)) {
                    continue;
                }

                try {
                    if (enemyPlayer.canSee(oldTile)) {
                        Element removeElement = Message.createNewRootElement("remove");

                        Element removeUnit = removeElement.getOwnerDocument().createElement("removeObject");
                        removeUnit.setAttribute("ID", unit.getID());
                        removeElement.appendChild(removeUnit);

                        enemyPlayer.getConnection().send(removeElement);
                    }
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
                }
            }
        }

        return null;
    }


    /**
    * Handles a "leaveShip"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param leaveShipElement The element containing the request.
    */
    private Element leaveShip(Connection connection, Element leaveShipElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(leaveShipElement.getAttribute("unit"));

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
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit carrier = (Unit) game.getFreeColGameObject(loadCargoElement.getAttribute("carrier"));
        Goods goods = new Goods(game, (Element) loadCargoElement.getChildNodes().item(0));

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
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Goods goods = new Goods(game, (Element) unloadCargoElement.getChildNodes().item(0));
        
        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        goods.unload();

        return null;
    }

    /**
    * Handles a "buyGoods"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param boardShipElement The element containing the request.
    */
    private Element buyGoods(Connection connection, Element buyGoodsElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit carrier = (Unit) game.getFreeColGameObject(buyGoodsElement.getAttribute("carrier"));
        int type = Integer.parseInt(buyGoodsElement.getAttribute("type"));
        int amount = Integer.parseInt(buyGoodsElement.getAttribute("amount"));

        if (carrier.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        if (carrier.getOwner() != player) {
            throw new IllegalStateException();
        }

        carrier.buyGoods(type, amount);

        return null;
    }


    /**
    * Handles a "sellGoods"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param leaveShipElement The element containing the request.
    */
    private Element sellGoods(Connection connection, Element sellGoodsElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Goods goods = new Goods(game, (Element) sellGoodsElement.getChildNodes().item(0));

        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        game.getMarket().sell(goods, player);

        return null;
    }

    /**
    * Handles a "moveToEurope"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param moveToEuropeElement The element containing the request.
    */
    private Element moveToEurope(Connection connection, Element moveToEuropeElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(moveToEuropeElement.getAttribute("unit"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        Tile oldTile = unit.getTile();
        unit.moveToEurope();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(oldTile)) {
                    Element removeElement = Message.createNewRootElement("remove");

                    Element removeUnit = removeElement.getOwnerDocument().createElement("removeObject");
                    removeUnit.setAttribute("ID", unit.getID());
                    removeElement.appendChild(removeUnit);

                    enemyPlayer.getConnection().send(removeElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }

        return null;
    }


    /**
    * Handles a "moveToAmerica"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element moveToAmerica(Connection connection, Element moveToAmericaElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(moveToAmericaElement.getAttribute("unit"));

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
    * @param element The element containing the request.
    */
    private Element buildColony(Connection connection, Element buildColonyElement) {
        Game game = freeColServer.getGame();
        Player player = freeColServer.getPlayer(connection);

        String name = buildColonyElement.getAttribute("name");
        Unit unit = (Unit) freeColServer.getGame().getFreeColGameObject(buildColonyElement.getAttribute("unit"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        if (unit.canBuildColony()) {
            Colony colony = new Colony(game, player, name, unit.getTile());

            Element reply = Message.createNewRootElement("buildColonyConfirmed");
            reply.appendChild(colony.toXMLElement(player, reply.getOwnerDocument()));

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
    * @param element The element containing the request.
    */
    private Element recruitUnitInEurope(Connection connection, Element recruitUnitInEuropeElement) {
        Game game = freeColServer.getGame();
        Player player = freeColServer.getPlayer(connection);
        Europe europe = player.getEurope();

        int slot = Integer.parseInt(recruitUnitInEuropeElement.getAttribute("slot"));
        int recruitable = europe.getRecruitable(slot);
        int newRecruitable = Unit.generateRecruitable();

        Unit unit = new Unit(game, player, recruitable);

        Element reply = Message.createNewRootElement("recruitUnitInEuropeConfirmed");
        reply.setAttribute("newRecruitable", Integer.toString(newRecruitable));
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));

        europe.recruit(slot, unit, newRecruitable);

        return reply;
    }

    /**
    * Handles an "emigrateUnitInEurope"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element emigrateUnitInEurope(Connection connection, Element emigrateUnitInEuropeElement) {
        Game game = freeColServer.getGame();
        Player player = freeColServer.getPlayer(connection);
        Europe europe = player.getEurope();

        int slot = Integer.parseInt(emigrateUnitInEuropeElement.getAttribute("slot"));
        int recruitable = europe.getRecruitable(slot);
        int newRecruitable = Unit.generateRecruitable();

        Unit unit = new Unit(game, player, recruitable);

        Element reply = Message.createNewRootElement("emigrateUnitInEuropeConfirmed");
        reply.setAttribute("newRecruitable", Integer.toString(newRecruitable));
        reply.setAttribute("slot", Integer.toString(slot));
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));

        europe.emigrate(slot, unit, newRecruitable);

        try {
            connection.send(reply);
        } catch (IOException e) {
            logger.warning("Can't send the player an emigrateUnitInEuropeConfirmed element.");
        }
        return null;
    }

    /**
    * Handles a "trainUnitInEurope"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element trainUnitInEurope(Connection connection, Element trainUnitInEuropeElement) {
        Game game = freeColServer.getGame();
        Player player = freeColServer.getPlayer(connection);
        Europe europe = player.getEurope();

        int unitType = Integer.parseInt(trainUnitInEuropeElement.getAttribute("unitType"));

        Unit unit = new Unit(game, player, unitType);

        Element reply = Message.createNewRootElement("trainUnitInEuropeConfirmed");
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));

        europe.train(unit);

        return reply;
    }

    /**
    * Handles a "equipunit"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param workElement The element containing the request.
    */
    private Element equipUnit(Connection connection, Element workElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(workElement.getAttribute("unit"));
        int type = Integer.parseInt(workElement.getAttribute("type"));
        int amount = Integer.parseInt(workElement.getAttribute("amount"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        switch(type) {
            case Goods.CROSSES:
                unit.setMissionary((amount > 0));
                break;
            case Goods.MUSKETS:
                unit.setArmed((amount > 0)); // So give them muskets if the amount we want is greater than zero.
                break;
            case Goods.HORSES:
                unit.setMounted((amount > 0)); // As above.
                break;
            case Goods.TOOLS:
                int actualAmount = amount;
                if ((actualAmount % 20) > 0) {
                    logger.warning("Trying to set a number of tools that is not a multiple of 20.");
                    actualAmount -= (actualAmount % 20);
                }
                unit.setNumberOfTools(actualAmount);
                break;
            default:
                logger.warning("Invalid type of goods to equip.");
                return null;
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
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(workElement.getAttribute("unit"));
        WorkLocation workLocation = (WorkLocation) game.getFreeColGameObject(workElement.getAttribute("workLocation"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        if (workLocation == null) {
            throw new NullPointerException();
        }

        // No reason to send an update to other players: this is always hidden.

        unit.work(workLocation);

        return null;
    }


    /**
    * Handles a "changeWorkType"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param workElement The element containing the request.
    */
    private Element changeWorkType(Connection connection, Element workElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(workElement.getAttribute("unit"));
        int workType = Integer.parseInt(workElement.getAttribute("workType"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        // No reason to send an update to other players: this is always hidden.

        unit.setWorkType(workType);

        return null;
    }

    /**
    * Handles a "setCurrentlyBuilding"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param setCurrentlyBuildingElement The element containing the request.
    */
    private Element setCurrentlyBuilding(Connection connection, Element setCurrentlyBuildingElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Colony colony = (Colony) game.getFreeColGameObject(setCurrentlyBuildingElement.getAttribute("colony"));
        int type = Integer.parseInt(setCurrentlyBuildingElement.getAttribute("type"));

        if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your colony!");
        }

        colony.setCurrentlyBuilding(type);

        sendUpdatedTileToAll(colony.getTile(), player);

        return null;
    }


    /**
    * Handles a "changeState"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param moveElement The element containing the request.
    * @exception IllegalArgumentException If the data format of the message is invalid.
    * @exception IllegalStateException If the request is not accepted by the model.
    *
    */
    private Element changeState(Connection connection, Element changeStateElement) {
        Game game = freeColServer.getGame();

        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(changeStateElement.getAttribute("unit"));
        int state = Integer.parseInt(changeStateElement.getAttribute("state"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + changeStateElement.getAttribute("unit"));
        }
        
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        Tile oldTile = unit.getTile();

        if (!unit.checkSetState(state)) {
            // Oh, really, Mr. Client? I'll show YOU!
            // kickPlayer(player);
            logger.warning("Can't set state " + state + ". Possible cheating attempt?");
            return null;
        }
        unit.setState(state);

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
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(putOutsideColonyElement.getAttribute("unit"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        unit.putOutsideColony();
        
        sendUpdatedTileToAll(unit.getTile(), player);

        return null;
    }


    /**
    * Handles a "clearSpeciality"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param clearSpecialityElement The element containing the request.
    */
    private Element clearSpeciality(Connection connection, Element clearSpecialityElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(clearSpecialityElement.getAttribute("unit"));

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
    private Element endTurn(Connection connection, Element moveElement) {
        Game game = freeColServer.getGame();

        ServerPlayer nextPlayer = (ServerPlayer) game.getNextPlayer();

        while (checkForDeath(nextPlayer)) {
            nextPlayer.setDead(true);
            Element setDeadElement = Message.createNewRootElement("setDead");
            setDeadElement.setAttribute("player", nextPlayer.getID());
            freeColServer.getServer().sendToAll(setDeadElement, null);

            nextPlayer = (ServerPlayer) game.getNextPlayer();
        }

        Player winner = checkForWinner();
        if (winner != null) {
            Element gameEndedElement = Message.createNewRootElement("gameEnded");
            gameEndedElement.setAttribute("winner", winner.getID());
            freeColServer.getServer().sendToAll(gameEndedElement, null);
            
            return null;
        }

        if (game.isNextPlayerInNewTurn()) {
            game.newTurn();

            Element newTurnElement = Message.createNewRootElement("newTurn");
            freeColServer.getServer().sendToAll(newTurnElement, null);

            createUnitsInColonies();
        }

        try {
            Element updateElement = Message.createNewRootElement("update");
            updateElement.appendChild(game.getMarket().toXMLElement(nextPlayer, updateElement.getOwnerDocument()));
            nextPlayer.getConnection().send(updateElement);
        } catch (IOException e) {
            logger.warning("Could not send message to: " + nextPlayer.getName() + " with connection " + nextPlayer.getConnection());
        }

        game.setCurrentPlayer(nextPlayer);

        Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
        setCurrentPlayerElement.setAttribute("player", nextPlayer.getID());
        freeColServer.getServer().sendToAll(setCurrentPlayerElement, null);

        return null;
    }


    /**
    * Checks if anybody has won the game and returns that player.
    * @return The <code>Player</code> who have won the game or <i>null</i>
    *         if the game is not finished.
    */
    private Player checkForWinner() {
        Game game = freeColServer.getGame();

        Iterator playerIterator = game.getPlayerIterator();
        Player winner = null;
        
        if (freeColServer.isSingleplayer()) {
            // TODO
        } else {
            while (playerIterator.hasNext()) {
                Player p = (Player) playerIterator.next();

                if (!p.isDead() && !p.isAI()) {
                    if (winner != null) {
                        return null;
                    } else {
                        winner = p;
                    }
                }
            }
        }

        return winner;
    }


    /**
    * Creates a colonist in every colony having more than 300 food
    * and builds a unit if choosen and completed.
    */
    private void createUnitsInColonies() {
        Game game = freeColServer.getGame();
        Map map = game.getMap();

        Iterator wmi = map.getWholeMapIterator();
        while (wmi.hasNext()) {
            Tile tile = map.getTile((Position) wmi.next());
            if (tile.getColony() != null && tile.getColony().getGoodsCount(Goods.FOOD) >= 300) {
                Colony colony = tile.getColony();
                Unit unit = new Unit(game, null, colony.getOwner(), Unit.FREE_COLONIST, Unit.ACTIVE);

                Element createUnitElement = Message.createNewRootElement("createUnit");
                createUnitElement.setAttribute("location", colony.getID());
                createUnitElement.appendChild(unit.toXMLElement(colony.getOwner(), createUnitElement.getOwnerDocument()));

                colony.createUnit(unit);

                ServerPlayer player = ((ServerPlayer) colony.getOwner());

                try {
                    player.getConnection().send(createUnitElement);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + player.getName() + " with connection " + player.getConnection());
                }
            }

            if (tile.getColony() != null && tile.getColony().getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) {
                int unitType = tile.getColony().getCurrentlyBuilding() - Colony.BUILDING_UNIT_ADDITION;

                if (Unit.getNextHammers(unitType) <= tile.getColony().getHammers() &&
                    Unit.getNextTools(unitType) <= tile.getColony().getGoodsCount(Goods.TOOLS)) {

                    Colony colony = tile.getColony();

                    // Check for cheating:
                    if (!colony.canBuildUnit(unitType)) {
                        logger.warning("Trying to build: " + unitType + " in colony: " + colony);
                        return;
                    }

                    Unit unit = new Unit(game, null, colony.getOwner(), unitType, Unit.ACTIVE);

                    Element createUnitElement = Message.createNewRootElement("createUnit");
                    createUnitElement.setAttribute("location", colony.getID());
                    createUnitElement.appendChild(unit.toXMLElement(colony.getOwner(), createUnitElement.getOwnerDocument()));

                    colony.createUnit(unit);

                    ServerPlayer player = ((ServerPlayer) colony.getOwner());

                    try {
                        player.getConnection().send(createUnitElement);
                    } catch (IOException e) {
                        logger.warning("Could not send message to: " + player.getName() + " with connection " + player.getConnection());
                    }
                }
            }
        }
    }


    /**
    * Checks if this player has died.
    * @return <i>true</i> if this player should die.
    */
    private boolean checkForDeath(Player player) {
        // Die if: (No colonies or units on map) && ((After 20 turns) || (Cannot get a unit from Europe))
        Game game = freeColServer.getGame();
        Map map = game.getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && ((t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(player))
                            || t.getSettlement() != null && t.getSettlement().getOwner().equals(player))) {
                return false;
            }
        }

        // At this point we know the player does not have any units or settlements on the map.

        if (player.getNation() >= 0 && player.getNation() <= 3) {
            /*if (game.getTurn().getNumber() > 20 || player.getEurope().getFirstUnit() == null
                    && player.getGold() < 600 && player.getGold() < player.getRecruitPrice()) {
            */
            if (game.getTurn().getNumber() > 20) {
                return true;
            } else if (player.getGold() < 1000) {
                Iterator unitIterator = player.getEurope().getUnitIterator();
                while (unitIterator.hasNext()) {
                    if (((Unit) unitIterator.next()).isCarrier()) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }


    private void sendUpdatedTileToAll(Tile newTile, Player player) {
        Game game = freeColServer.getGame();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player != null && player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(newTile)) {
                    Element updateElement = Message.createNewRootElement("update");
                    updateElement.appendChild(newTile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));

                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }
    }

    private void sendErrorToAll(String message, Player player) {
        Game game = freeColServer.getGame();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if ((player != null) && (player.equals(enemyPlayer))) {
                continue;
            }

            try {
                Element errorElement = Message.createNewRootElement("error");
                errorElement.setAttribute("message", message);

                enemyPlayer.getConnection().send(errorElement);
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }
    }

}
