
package net.sf.freecol.server.control;

import java.util.Vector;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Random;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
* Handles the network messages that arrives while
* {@link FreeColServer#IN_GAME in game}.
*/
public final class InGameInputHandler extends InputHandler {
    private static Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static Random attackCalculator;


    /**
    * The constructor to use.
    * @param freeColServer The main server object.
    */
    public InGameInputHandler(FreeColServer freeColServer) {
        super(freeColServer);
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
        FreeColServer freeColServer = getFreeColServer();

        String type = element.getTagName();

        try {
            if (element != null) {
                // The first messages you see here are the ones that are supported even
                // though it is NOT the sender's turn:
                if (type.equals("logout")) {
                    reply = logout(connection, element);
                } else if (type.equals("createUnit")) {
                    reply = createUnit(connection, element);
                } else if (type.equals("getRandom")) {
                    reply = getRandom(connection, element);
                } else if (type.equals("getVacantEntryLocation")) {
                    reply = getVacantEntryLocation(connection, element);
                } else if (type.equals("disconnect")) {
                    reply = disconnect(connection, element);
                } else if (freeColServer.getGame().getCurrentPlayer().equals(freeColServer.getPlayer(connection))) {
                    if (type.equals("chat")) {
                        reply = chat(connection, element);
                    } else if (type.equals("move")) {
                        reply = move(connection, element);
                    } else if (type.equals("askSkill")) {
                        reply = askSkill(connection, element);
                    } else if (type.equals("attack")) {
                        reply = attack(connection, element);
                    } else if (type.equals("embark")) {
                        reply = embark(connection, element);
                    } else if (type.equals("boardShip")) {
                        reply = boardShip(connection, element);
                    } else if (type.equals("learnSkillAtSettlement")) {
                        reply = learnSkillAtSettlement(connection, element);
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
                    } else if (type.equals("setNewLandName")) {
                        reply = setNewLandName(connection, element);
                    } else if (type.equals("endTurn")) {
                        reply = endTurn(connection, element);
                    } else if (type.equals("disbandUnit")) {
                        reply = disbandUnit(connection, element);
                    } else {
                        logger.warning("Unknown request from client " + element.getTagName());
                    }
                } else {
                    // The message we've received is probably a good one, but
                    // it was sent when it was not the sender's turn.
                    reply = Message.createNewRootElement("error");
                    reply.setAttribute("message", "Not your turn.");
                    
                    logger.warning("Received message when not in turn: " + element.getTagName());
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.warning(sw.toString());

            Element reconnect = Message.createNewRootElement("reconnect");

            try {
                connection.send(reconnect);
            } catch (IOException ex) {
                logger.warning("Could not send reconnect message!");
            }

            return null;
        }

        return reply;
    }


    /**
    * Handles a "setNewLandName"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    *
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
    *
    */
    private Element createUnit(Connection connection, Element element) {
        Game game = getFreeColServer().getGame();

        logger.info("Receiving \"createUnit\"-request.");

        String taskID = element.getAttribute("taskID");
        Location location = (Location) game.getFreeColGameObject(element.getAttribute("location"));
        Player owner = (Player) game.getFreeColGameObject(element.getAttribute("owner"));
        int type = Integer.parseInt(element.getAttribute("type"));

        if (owner != getFreeColServer().getPlayer(connection)) {
            throw new IllegalStateException();
        }

        Unit unit = getFreeColServer().getModelController().createUnit(taskID, location, owner, type);

        Element reply = Message.createNewRootElement("createUnitConfirmed");
        reply.appendChild(unit.toXMLElement(owner, reply.getOwnerDocument()));

        return reply;
    }


    /**
    * Handles a "getRandom"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    *
    */
    private Element getRandom(Connection connection, Element element) {
        Game game = getFreeColServer().getGame();

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
    *
    */
    private Element getVacantEntryLocation(Connection connection, Element element) {
        Game game = getFreeColServer().getGame();
        Unit unit = (Unit) game.getFreeColGameObject(element.getAttribute("unit"));
        Player owner = unit.getOwner();

        if (owner != getFreeColServer().getPlayer(connection)) {
            throw new IllegalStateException();
        }

        Location entryLocation = getFreeColServer().getModelController().setToVacantEntryLocation(unit);

        Element reply = Message.createNewRootElement("getVacantEntryLocationConfirmed");
        reply.setAttribute("location", entryLocation.getID());

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
        element.setAttribute("sender", getFreeColServer().getPlayer(connection).getID());
        getFreeColServer().getServer().sendToAll(element, connection);
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
        FreeColServer freeColServer = getFreeColServer();
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
        Tile newTile = game.getMap().getNeighbourOrNull(direction, unit.getTile());

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
                continue;
            }

            try {
                if (unit.isVisibleTo(enemyPlayer) && !disembark) {
                    Element opponentMoveElement = Message.createNewRootElement("opponentMove");
                    opponentMoveElement.setAttribute("direction", Integer.toString(direction));
                    opponentMoveElement.setAttribute("unit", unit.getID());
                    enemyPlayer.getConnection().send(opponentMoveElement);
                } else if (enemyPlayer.canSee(newTile) && newTile.getSettlement() == null) {
                    Element opponentMoveElement = Message.createNewRootElement("opponentMove");
                    opponentMoveElement.setAttribute("direction", Integer.toString(direction));
                    opponentMoveElement.setAttribute("tile", newTile.getID());
                    opponentMoveElement.appendChild(unit.toXMLElement(enemyPlayer, opponentMoveElement.getOwnerDocument()));
                    enemyPlayer.getConnection().send(opponentMoveElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }

        unit.move(direction);

        Element reply = Message.createNewRootElement("update");
        Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

        for (int i=0; i<surroundingTiles.size(); i++) {
            Tile t = (Tile) surroundingTiles.get(i);
            reply.appendChild(t.toXMLElement(player, reply.getOwnerDocument()));
        }

        return reply;
    }


    /**
    * Handles an "askSkill"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    *
    * @exception IllegalArgumentException If the data format of the message is invalid.
    * @exception IllegalStateException If the request is not accepted by the model.
    */
    private Element askSkill(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Game game = freeColServer.getGame();
        Map map = game.getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(element.getAttribute("unit"));
        int direction = Integer.parseInt(element.getAttribute("direction"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + element.getAttribute("unit"));
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

        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();

        if (settlement.getLearnableSkill() != IndianSettlement.UNKNOWN) {
            unit.setMovesLeft(0);

            if (settlement.getLearnableSkill() != IndianSettlement.NONE) {
                // We now put the unit on the indian settlement. Normally we shouldn't have
                // to this, but the movesLeft are set to 0 for unit and if the player decides
                // to learn a skill with a learnSkillAtSettlement message then we have to be
                // able to check if the unit can learn the skill.
                unit.setLocation(settlement);
            }

            Element reply = Message.createNewRootElement("provideSkill");
            reply.setAttribute("skill", Integer.toString(settlement.getLearnableSkill()));

            return reply;
        } else {
            throw new IllegalStateException("Learnable skill from Indian settlement is unknown at server.");
        }
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
        FreeColServer freeColServer = getFreeColServer();
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);


        // Get parameters:
        Unit unit = (Unit) game.getFreeColGameObject(attackElement.getAttribute("unit"));
        int direction = Integer.parseInt(attackElement.getAttribute("direction"));


        // Test the parameters:
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


        int result = generateAttackResult(unit, defender);
        int plunderGold = -1;

        if (result == Unit.ATTACK_DONE_SETTLEMENT) {
            plunderGold = newTile.getSettlement().getOwner().getGold()/10; // 10% of their gold
        }

        // Inform the other players (other then the player attacking) about the attack:
        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
                continue;
            }

            if (unit.isVisibleTo(enemyPlayer) || defender.isVisibleTo(enemyPlayer)) {
                Element opponentAttackElement = Message.createNewRootElement("opponentAttack");
                opponentAttackElement.setAttribute("direction", Integer.toString(direction));
                opponentAttackElement.setAttribute("result", Integer.toString(result));
                opponentAttackElement.setAttribute("plunderGold", Integer.toString(plunderGold));
                opponentAttackElement.setAttribute("unit", unit.getID());
                opponentAttackElement.setAttribute("defender", defender.getID());

                if (!defender.isVisibleTo(enemyPlayer)) {
                    opponentAttackElement.appendChild(defender.toXMLElement(enemyPlayer, opponentAttackElement.getOwnerDocument()));
                } else if (!unit.isVisibleTo(enemyPlayer)) {
                    opponentAttackElement.appendChild(unit.toXMLElement(enemyPlayer, opponentAttackElement.getOwnerDocument()));
                }

                try {
                    enemyPlayer.getConnection().send(opponentAttackElement);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
                }
            }
        }


        // Create the reply for the attacking player:
        Element reply = Message.createNewRootElement("attackResult");
        reply.setAttribute("result", Integer.toString(result));
        reply.setAttribute("plunderGold", Integer.toString(plunderGold));

        if (result == Unit.ATTACK_DONE_SETTLEMENT && newTile.getColony() != null) { // If a colony will been won, send an updated tile:
            reply.appendChild(newTile.toXMLElement(newTile.getColony().getOwner(), reply.getOwnerDocument()));
        } else if (!defender.isVisibleTo(player)) {
            reply.appendChild(defender.toXMLElement(player, reply.getOwnerDocument()));
        }

        unit.attack(defender, result, plunderGold);

        if (unit.getTile().equals(newTile)) { // In other words, we moved...
            Element update = reply.getOwnerDocument().createElement("update");
            Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

            for (int i=0; i<surroundingTiles.size(); i++) {
                Tile t = (Tile) surroundingTiles.get(i);
                update.appendChild(t.toXMLElement(player, update.getOwnerDocument()));
            }

            reply.appendChild(update);
        }

        return reply;
    }
    
    
    /**
    * Generates a result of an attack.
    */
    private int generateAttackResult(Unit unit, Unit defender) {
        int attackPower = unit.getOffensePower(defender);
        int totalProbability = attackPower + defender.getDefensePower(unit);
        int result;
        int r = attackCalculator.nextInt(totalProbability+1);
        if (r > attackPower) {
            result = Unit.ATTACK_LOSS;
        } else if(r == attackPower) {
            if (defender.isNaval()) {
                result = Unit.ATTACK_EVADES;
            } else {
                result = Unit.ATTACK_WIN;
            }
        } else { // (r < attackPower)
            result = Unit.ATTACK_WIN;
        }

        if (result == Unit.ATTACK_WIN) {
            int diff = defender.getDefensePower(unit)*2-attackPower;
            int r2 = attackCalculator.nextInt((diff<3) ? 3 : diff);

            if (r2 == 0) {
                result = Unit.ATTACK_GREAT_WIN;
            } else {
                result = Unit.ATTACK_WIN;
            }
        }
        
        if (result == Unit.ATTACK_LOSS) {
            int diff = attackPower*2-defender.getDefensePower(unit);
            int r2 = attackCalculator.nextInt((diff<3) ? 3 : diff);

            if (r2 == 0) {
                result = Unit.ATTACK_GREAT_LOSS;
            } else {
                result = Unit.ATTACK_LOSS;
            }
        }

        if ((result == Unit.ATTACK_WIN || result == Unit.ATTACK_GREAT_WIN) && (
                defender.getTile().getSettlement() != null && defender.getTile().getSettlement() instanceof IndianSettlement
                && ((IndianSettlement) defender.getTile().getSettlement()).getUnitCount()+defender.getTile().getUnitCount() <= 1
                || defender.getTile().getColony() != null && !defender.isArmed() && !defender.isMounted() && defender.getType() != Unit.ARTILLERY
                && defender.getType() != Unit.DAMAGED_ARTILLERY && !defender.isMounted())) {
            result = Unit.ATTACK_DONE_SETTLEMENT;
        }

        return result;
    }


    /**
    * Handles an "embark"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param embarkElement The element containing the request.
    * @exception IllegalArgumentException If the data format of the message is invalid.
    */
    private Element embark(Connection connection, Element embarkElement) {
        FreeColServer freeColServer = getFreeColServer();
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

            if (player.equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
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
        FreeColServer freeColServer = getFreeColServer();
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

                if (player.equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
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
    * Handles a "learnSkillAtSettlement"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element learnSkillAtSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Game game = freeColServer.getGame();
        Map map = game.getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(element.getAttribute("unit"));
        int direction = Integer.parseInt(element.getAttribute("direction"));
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

        // The unit was relocated to the indian settlement. See askSkill for more info.
        IndianSettlement settlement = (IndianSettlement) unit.getLocation();
        Tile tile = map.getNeighbourOrNull(Map.getReverseDirection(direction), unit.getTile());
        unit.setLocation(tile);

        if (!cancelAction) {
            unit.setType(settlement.getLearnableSkill());
            settlement.setLearnableSkill(IndianSettlement.NONE);
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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(moveToEuropeElement.getAttribute("unit"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        Tile oldTile = unit.getTile();
        unit.moveToEurope();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

        String name = buildColonyElement.getAttribute("name");
        Unit unit = (Unit) game.getFreeColGameObject(buildColonyElement.getAttribute("unit"));

        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        if (unit.canBuildColony()) {
            Colony colony = new Colony(game, player, name, unit.getTile());

            Element reply = Message.createNewRootElement("buildColonyConfirmed");
            reply.appendChild(colony.toXMLElement(player, reply.getOwnerDocument()));

            if (colony.getLineOfSight() > unit.getLineOfSight()) {
                Element updateElement = reply.getOwnerDocument().createElement("update");
                Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), colony.getLineOfSight());

                for (int i=0; i<surroundingTiles.size(); i++) {
                    Tile t = (Tile) surroundingTiles.get(i);
                    if (t != unit.getTile()) {
                        updateElement.appendChild(t.toXMLElement(player, reply.getOwnerDocument()));
                    }
                }

                reply.appendChild(updateElement);
            }

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
        Game game = getFreeColServer().getGame();
        Player player = getFreeColServer().getPlayer(connection);
        Europe europe = player.getEurope();

        int slot = Integer.parseInt(recruitUnitInEuropeElement.getAttribute("slot"));
        int recruitable = europe.getRecruitable(slot);
        int newRecruitable = player.generateRecruitable();

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
        Game game = getFreeColServer().getGame();
        Player player = getFreeColServer().getPlayer(connection);
        Europe europe = player.getEurope();

        int slot;
        if (player.hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            slot = Integer.parseInt(emigrateUnitInEuropeElement.getAttribute("slot"));
        }
        else {
            slot = (int) ((Math.random() * 3) + 1);
        }

        int recruitable = europe.getRecruitable(slot);
        int newRecruitable = player.generateRecruitable();

        Unit unit = new Unit(game, player, recruitable);

        Element reply = Message.createNewRootElement("emigrateUnitInEuropeConfirmed");
        if (!player.hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            reply.setAttribute("slot", Integer.toString(slot));
        }
        reply.setAttribute("newRecruitable", Integer.toString(newRecruitable));
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));

        europe.emigrate(slot, unit, newRecruitable);

        return reply;
    }

    /**
    * Handles a "trainUnitInEurope"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element trainUnitInEurope(Connection connection, Element trainUnitInEuropeElement) {
        Game game = getFreeColServer().getGame();
        Player player = getFreeColServer().getPlayer(connection);
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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();

        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);

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
    private Element endTurn(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        getFreeColServer().getInGameController().endTurn(player);

        return null;
    }

    /**
     * Handles a "disbandUnit"-message.
     *
     * @param connection The <code>Connection</code> the message was received on.
     * @param element The element containing the request.
     */
    private Element disbandUnit(Connection connection, Element element) {
        Game game = getFreeColServer().getGame();
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) game.getFreeColGameObject(element.getAttribute("unit"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + element.getAttribute("unit"));
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
    * Handles a "logout"-message.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param logoutElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    protected Element logout(Connection connection, Element logoutElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);

        logger.info("Logout by: " + connection + ((player != null) ? " (" + player.getName() + ") " : ""));

        // TODO

        // Remove the player's units/colonies from the map and send map updates to the
        // players that can see such units or colonies.
        // SHOULDN'T THIS WAIT UNTIL THE CURRENT PLAYER HAS FINISHED HIS TURN?

        /*
        player.setDead(true);

        Element setDeadElement = Message.createNewRootElement("setDead");
        setDeadElement.setAttribute("player", player.getID());
        freeColServer.getServer().sendToAll(setDeadElement, connection);
        */

        /*
        TODO: Setting the player dead directly should be a server option,
            but for now - allow the player to reconnect:
        */
        player.setConnected(false);


        if (getFreeColServer().getGame().getCurrentPlayer() == player && !getFreeColServer().isSingleplayer() && isHumanPlayersLeft()) {
            getFreeColServer().getInGameController().endTurn(player);
        }

        return null;
    }


    private boolean isHumanPlayersLeft() {
        Iterator playerIterator = getFreeColServer().getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = (Player) playerIterator.next();

            if (!p.isDead() && !p.isAI()) {
                return true;
            }
        }

        return false;
    }


    private void sendUpdatedTileToAll(Tile newTile, Player player) {
        Game game = getFreeColServer().getGame();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
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
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }
    }


    private void sendErrorToAll(String message, Player player) {
        Game game = getFreeColServer().getGame();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if ((player != null) && (player.equals(enemyPlayer)) || enemyPlayer.getConnection() == null) {
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
