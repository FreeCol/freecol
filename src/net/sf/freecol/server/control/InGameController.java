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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

/**
 * TODO: write class comment.
 */
public final class InGameController extends Controller {
    private static Logger logger = Logger.getLogger(InGameController.class.getName());




    public int debugOnlyAITurns = 0;


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameController(FreeColServer freeColServer) {
        super(freeColServer);
    }

    /**
     * Ends the turn of the given player.
     * 
     * @param player The player to end the turn of.
     */
    public void endTurn(ServerPlayer player) {
        /* BEGIN FIX
         * 
         * TODO: Remove this temporary fix for bug:
         *       [ 1709196 ] Waiting for next turn (inifinite wait)
         *       
         *       This fix can be removed when FIFO ordering of
         *       of network messages is working correctly.
         *       (scheduled to be fixed as part of release 0.8.0)
         */
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        // END FIX
        
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer oldPlayer = (ServerPlayer) getGame().getCurrentPlayer();
        
        if (oldPlayer != player) {
            logger.warning("It is not " + player.getName() + "'s turn!");
            throw new IllegalArgumentException("It is not " + player.getName() + "'s turn!");
        }
        
        player.clearModelMessages();
        freeColServer.getModelController().clearTaskRegister();

        Player winner = checkForWinner();
        if (winner != null && (!freeColServer.isSingleplayer() || !winner.isAI())) {
            Element gameEndedElement = Message.createNewRootElement("gameEnded");
            gameEndedElement.setAttribute("winner", winner.getId());
            freeColServer.getServer().sendToAll(gameEndedElement, null);
            return;
        }
        
        ServerPlayer newPlayer = (ServerPlayer) nextPlayer();
        
        if (newPlayer != null 
                && !newPlayer.isAI()
                && (!newPlayer.isConnected() || debugOnlyAITurns > 0)) {
            endTurn(newPlayer);
            return;
        }
    }
    
    /**
     * Sets a new current player and notifies the clients.
     * @return The new current player.
     */
    private Player nextPlayer() {
        final FreeColServer freeColServer = getFreeColServer();
        
        if (!isHumanPlayersLeft()) {
            getGame().setCurrentPlayer(null);
            return null;
        }
        
        if (getGame().isNextPlayerInNewTurn()) {
            getGame().newTurn();
            if (debugOnlyAITurns > 0) {
                debugOnlyAITurns--;
            }
            Element newTurnElement = Message.createNewRootElement("newTurn");
            freeColServer.getServer().sendToAll(newTurnElement, null);
        }
        
        ServerPlayer newPlayer = (ServerPlayer) getGame().getNextPlayer();
        getGame().setCurrentPlayer(newPlayer);
        if (newPlayer == null) {
            getGame().setCurrentPlayer(null);
            return null;
        }
        
        synchronized (newPlayer) {
            if (checkForDeath(newPlayer)) {
                newPlayer.setDead(true);
                Element setDeadElement = Message.createNewRootElement("setDead");
                setDeadElement.setAttribute("player", newPlayer.getId());
                freeColServer.getServer().sendToAll(setDeadElement, null);
                return nextPlayer();
            }
        }
        
        if (newPlayer.isEuropean()) {

            try {        
                Market market = newPlayer.getMarket();
                // make random change to the market
                market.remove(getPseudoRandom().nextInt(Goods.NUMBER_OF_TYPES),
                           (getPseudoRandom().nextInt(21)));
                Element updateElement = Message.createNewRootElement("update");
                updateElement.appendChild(newPlayer.getMarket().toXMLElement(newPlayer, updateElement.getOwnerDocument()));
                newPlayer.getConnection().send(updateElement);
            } catch (IOException e) {
                logger.warning("Could not send message to: " + newPlayer.getName() + " with connection "
                        + newPlayer.getConnection());
            }

            if (newPlayer.getCurrentFather() == null && newPlayer.getSettlements().size() > 0) {
                chooseFoundingFather(newPlayer);
            }
            if (newPlayer.getMonarch() != null) {
                monarchAction(newPlayer);
            }
            bombardEnemyShips(newPlayer);
        }
        
        Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
        setCurrentPlayerElement.setAttribute("player", newPlayer.getId());
        freeColServer.getServer().sendToAll(setCurrentPlayerElement, null);
        
        return newPlayer;
    }
    
    private boolean isHumanPlayersLeft() {
        Iterator<Player> playerIterator = getFreeColServer().getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer p = (ServerPlayer) playerIterator.next();
            if (!p.isDead() && !p.isAI() && p.isConnected()) {
                return true;
            }
        }
        return false;
    }

    private void chooseFoundingFather(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        Thread t = new Thread("foundingfather-thread") {
            public void run() {
                List<FoundingFather> randomFoundingFathers = getRandomFoundingFathers(nextPlayer);
                boolean atLeastOneChoice = false;
                Element chooseFoundingFatherElement = Message.createNewRootElement("chooseFoundingFather");
                for (int i = 0; i < randomFoundingFathers.size(); i++) {
                    if (randomFoundingFathers.get(i) != null) {
                        chooseFoundingFatherElement.setAttribute("foundingFather" + Integer.toString(i), 
                                                                 randomFoundingFathers.get(i).getId());
                        atLeastOneChoice = true;
                    } else {
                        chooseFoundingFatherElement.setAttribute("foundingFather" + Integer.toString(i), "");
                    }

                }
                if (!atLeastOneChoice) {
                    nextPlayer.setCurrentFather(null);
                } else {
                    try {
                        Element reply = nextPlayer.getConnection().ask(chooseFoundingFatherElement);
                        FoundingFather father = FreeCol.getSpecification().
                            getFoundingFather(reply.getAttribute("foundingFather"));
                        if (!randomFoundingFathers.contains(father)) {
                            throw new IllegalArgumentException();
                        }
                        nextPlayer.setCurrentFather(father);
                    } catch (IOException e) {
                        logger.warning("Could not send message to: " + nextPlayer.getName());
                    }
                }
            }
        };
        t.start();
    }

    /**
     * 
     * Returns a List of FoundingFathers, not including the founding
     * fathers the player already has, one of each type, or null if no
     * FoundingFather of that type is available.
     * 
     * @param player The <code>Player</code> that should pick a founding
     *            father from this list.
     */
    private List<FoundingFather> getRandomFoundingFathers(Player player) {
        int age = getGame().getTurn().getAge();
        List<FoundingFather> randomFoundingFathers = new ArrayList<FoundingFather>();
        for (int type = 0; type < FoundingFather.TYPE_COUNT; type++) {
            int weightSum = 0;
            for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                if (!player.hasFather(father) && father.getType() == type &&
                    father.isAvailableTo(player)) {
                    weightSum += father.getWeight(age);
                }
            }
            if (weightSum == 0) {
                randomFoundingFathers.add(null);
            } else {
                int r = getPseudoRandom().nextInt(weightSum) + 1;
                weightSum = 0;
                for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                    if (!player.hasFather(father) && father.getType() == type) {
                        weightSum += father.getWeight(age);
                        if (weightSum >= r) {
                            randomFoundingFathers.add(father);
                            break;
                        }
                    }
                }
            }
        }
        return randomFoundingFathers;
    }

    /**
     * Checks if anybody has won the game and returns that player.
     * 
     * @return The <code>Player</code> who have won the game or <i>null</i>
     *         if the game is not finished.
     */
    public Player checkForWinner() {
        GameOptions go = getGame().getGameOptions();
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            Iterator<Player> playerIterator = getGame().getPlayerIterator();
            while (playerIterator.hasNext()) {
                Player p = playerIterator.next();
                if (!p.isAI() && p.getRebellionState() == Player.REBELLION_POST_WAR) {
                    return p;
                }
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            Player winner = null;
            Iterator<Player> playerIterator = getGame().getPlayerIterator();
            while (playerIterator.hasNext()) {
                Player p = playerIterator.next();
                if (!p.isDead() && p.isEuropean() && !p.isREF()) {
                    if (winner != null) {
                        // There is more than one european player alive:
                        winner = null;
                        break;
                    } else {
                        winner = p;
                    }
                }
            }
            if (winner != null) {
                return winner;
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
            Player winner = null;
            Iterator<Player> playerIterator = getGame().getPlayerIterator();
            while (playerIterator.hasNext()) {
                Player p = playerIterator.next();
                if (!p.isDead() && !p.isAI()) {
                    if (winner != null) {
                        // There is more than one human player alive:
                        winner = null;
                        break;
                    } else {
                        winner = p;
                    }
                }
            }
            if (winner != null) {
                return winner;
            }
        }
        return null;
    }

    /**
     * Checks if the given player has died.
     * 
     * @param player The <code>Player</code>.
     * @return <i>true</i> if this player should die.
     */
    private boolean checkForDeath(Player player) {
        /*
         * Die if: (No colonies or units on map)
         *         && ((After 20 turns) || (Cannot get a unit from Europe))
         */
        
        if (player.isREF()) {
            /*
             * The REF never dies. I can grant independence to
             * dominions, see: AIPlayer.checkForREFDefeat
             */
            return false;
        }
        
        Map map = getGame().getMap();

        // Quick check to avoid long processing time:
        if (!player.getSettlements().isEmpty()) {
            return false;
        }

        Iterator<Position> tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile(tileIterator.next());
            if (t != null
                    && ((t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(player)) || t.getSettlement() != null
                            && t.getSettlement().getOwner().equals(player))) {
                return false;
            }
        }
        
        /*
         * At this point we know the player does not have any units or
         * settlements on the map.
         */
        
        if (player.isEuropean()) {
            /*
             * if (getGame().getTurn().getNumber() > 20 ||
             * player.getEurope().getFirstUnit() == null && player.getGold() <
             * 600 && player.getGold() < player.getRecruitPrice()) {
             * 
             */
            if (getGame().getTurn().getNumber() > 20) {
                return true;
            } else if (player.getEurope() == null) {
                return true;
            } else if (player.getGold() < 1000) {
                Iterator<Unit> unitIterator = player.getEurope().getUnitIterator();
                while (unitIterator.hasNext()) {
                    if (unitIterator.next().isCarrier()) {
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

    /**
     * Checks for monarch actions.
     * 
     * @param player The server player.
     */
    private void monarchAction(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        Thread t = new Thread() {
            public void run() {
                try {
                    Monarch monarch = nextPlayer.getMonarch();
                    int action = monarch.getAction();
                    Unit newUnit;
                    Element monarchActionElement = Message.createNewRootElement("monarchAction");
                    monarchActionElement.setAttribute("action", String.valueOf(action));
                    switch (action) {
                    case Monarch.RAISE_TAX:
                        int oldTax = nextPlayer.getTax();
                        int newTax = monarch.getNewTax();
                        if (newTax > 100) {
                            logger.warning("Tax rate exceeds 100 percent.");
                            return;
                        }
                        Goods goods = nextPlayer.getMostValuableGoods();
                        if (goods == null) {
                            return;
                        }
                        monarchActionElement.setAttribute("amount", String.valueOf(newTax));
                        monarchActionElement.setAttribute("goods", goods.getName());
                        monarchActionElement.setAttribute("force", String.valueOf(false));
                        try {
                            nextPlayer.setTax(newTax); // to avoid cheating
                            Element reply = nextPlayer.getConnection().ask(monarchActionElement);
                            boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                            
                            if (!accepted) {
                                Colony colony = (Colony) goods.getLocation();
                                if (colony.getGoodsCount(goods.getType()) >= goods.getAmount()) {
                                    nextPlayer.setTax(oldTax); // player hasn't accepted, restoring tax
                                    Element removeGoodsElement = Message.createNewRootElement("removeGoods");
                                    ((Colony) goods.getLocation()).removeGoods(goods);
                                    nextPlayer.setArrears(goods);
                                    removeGoodsElement.appendChild(goods.toXMLElement(nextPlayer, removeGoodsElement
                                                                                      .getOwnerDocument()));
                                    nextPlayer.getConnection().send(removeGoodsElement);
                                } else {
                                    // player has cheated and removed goods from colony, don't restore tax
                                    monarchActionElement.setAttribute("force", String.valueOf(true));
                                    nextPlayer.getConnection().send(monarchActionElement);
                                }
                            }
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + nextPlayer.getName());
                        }
                        break;
                    case Monarch.ADD_TO_REF:
                        int[] addition = monarch.addToREF();
                        Element additionElement = monarchActionElement.getOwnerDocument().createElement("addition");
                        additionElement.setAttribute("xLength", Integer.toString(addition.length));
                        for (int x = 0; x < addition.length; x++) {
                            additionElement.setAttribute("x" + Integer.toString(x), Integer.toString(addition[x]));
                        }
                        monarchActionElement.appendChild(additionElement);
                        try {
                            nextPlayer.getConnection().send(monarchActionElement);
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + nextPlayer.getName());
                        }
                        break;
                    case Monarch.DECLARE_WAR:
                        Player enemy = monarch.declareWar();
                        if (enemy == null) {
                            // this should not happen
                            logger.warning("Declared war on nobody.");
                            return;
                        }
                        nextPlayer.setStance(enemy, Player.WAR);
                        monarchActionElement.setAttribute("enemy", enemy.getId());
                        try {
                            nextPlayer.getConnection().send(monarchActionElement);
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + nextPlayer.getName());
                        }
                        break;
                    case Monarch.SUPPORT_LAND:
                        int[] additions = monarch.supportLand();
                        createUnits(additions, monarchActionElement, nextPlayer);
                        try {
                            nextPlayer.getConnection().send(monarchActionElement);
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + nextPlayer.getName());
                        }
                        break;
                    case Monarch.SUPPORT_SEA:
                        // TODO: make this generic
                        UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.frigate");
                        newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer, unitType, Unit.ACTIVE);
                        nextPlayer.getEurope().add(newUnit);
                        monarchActionElement.appendChild(newUnit.toXMLElement(nextPlayer, monarchActionElement
                                .getOwnerDocument()));
                        try {
                            nextPlayer.getConnection().send(monarchActionElement);
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + nextPlayer.getName());
                        }
                        break;
                    case Monarch.OFFER_MERCENARIES:
                        int[] units = monarch.getMercenaries();
                        int price = monarch.getPrice(units, true);
                        Element mercenaryElement = monarchActionElement.getOwnerDocument().createElement("mercenaries");
                        monarchActionElement.setAttribute("price", String.valueOf(price));
                        mercenaryElement.setAttribute("xLength", Integer.toString(units.length));
                        monarchActionElement.appendChild(mercenaryElement);
                        for (int x = 0; x < units.length; x++) {
                            mercenaryElement.setAttribute("x" + Integer.toString(x), Integer.toString(units[x]));
                        }
                        try {
                            Element reply = nextPlayer.getConnection().ask(monarchActionElement);
                            boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                            if (accepted) {
                                Element updateElement = Message.createNewRootElement("monarchAction");
                                updateElement.setAttribute("action", String.valueOf(Monarch.ADD_UNITS));
                                nextPlayer.modifyGold(-price);
                                createUnits(units, updateElement, nextPlayer);
                                nextPlayer.getConnection().send(updateElement);
                            }
                        } catch (IOException e) {
                            logger.warning("Could not send message to: " + nextPlayer.getName());
                        }
                        break;
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Monarch action failed!", e);
                }
            }
        };
        t.start();
    }

    private void createUnits(int[] units, Element element, ServerPlayer nextPlayer) {
        Unit newUnit;
        for (int type = 0; type < units.length; type++) {
            for (int i = 0; i < units[type]; i++) {
                if (type == Monarch.ARTILLERY) {
                    UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.artillery");
                    newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer, unitType, Unit.ACTIVE);
                } else {
                    boolean mounted = false;
                    if (type == Monarch.DRAGOON) {
                        mounted = true;
                    }
                    UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier");
                    newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer, unitType, Unit.ACTIVE,
                            true, mounted, 0, false);
                }
                nextPlayer.getEurope().add(newUnit);
                if (element != null) {
                    element.appendChild(newUnit.toXMLElement(nextPlayer, element.getOwnerDocument()));
                }
            }
        }
    }

    private void bombardEnemyShips(ServerPlayer currentPlayer) {
        logger.finest("Entering method bombardEnemyShips.");
        Map map = getFreeColServer().getGame().getMap();
        for (Settlement settlement : currentPlayer.getSettlements()) {
            Colony colony = (Colony) settlement;
            logger.finest("Colony is " + colony.getName());
            if (colony.hasAbility("model.ability.bombardShips") && !colony.isLandLocked()) {
                logger.finest("Colony has harbour and fort.");
                float attackPower = colony.getBombardingPower();
                if (attackPower <= 0) {
                    continue;
                }
                Unit attacker = colony.getBombardingAttacker();
                logger.finest("Colony has attack power " + attackPower);
                Position colonyPosition = colony.getTile().getPosition();
                for (int direction = 0; direction < Map.NUMBER_OF_DIRECTIONS; direction++) {
                    Tile tile = map.getTile(Map.getAdjacent(colonyPosition, direction));
                    if (!tile.isLand()) {
                        Iterator<Unit> unitIterator = tile.getUnitIterator();
                        while (unitIterator.hasNext()) {
                            Unit unit = unitIterator.next();
                            Player player = unit.getOwner();
                            if (player != currentPlayer
                                    && (currentPlayer.getStance(player) == Player.WAR || unit.hasAbility("model.ability.piracy"))) {
                                logger.finest("Found enemy unit " + unit.getOwner().getNationAsString() + " "
                                        + unit.getName());
                                // generate bombardment result
                                float totalProbability = attackPower + unit.getDefensePower(attacker);
                                int result;
                                int r = getPseudoRandom().nextInt(Math.round(totalProbability) + 1);
                                if (r < attackPower) {
                                    int diff = Math.round(unit.getDefensePower(attacker) * 2 - attackPower);
                                    int r2 = getPseudoRandom().nextInt((diff < 3) ? 3 : diff);
                                    if (r2 == 0) {
                                        result = Unit.ATTACK_GREAT_WIN;
                                    } else {
                                        result = Unit.ATTACK_WIN;
                                    }
                                } else {
                                    result = Unit.ATTACK_EVADES;
                                }

                                // Inform the players (other then the player
                                // attacking) about the attack:
                                int plunderGold = -1;
                                Iterator<Player> enemyPlayerIterator = getFreeColServer().getGame().getPlayerIterator();
                                while (enemyPlayerIterator.hasNext()) {
                                    ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

                                    if (// currentPlayer.equals(enemyPlayer) ||
                                    enemyPlayer.getConnection() == null) {
                                        continue;
                                    }

                                    Element opponentAttackElement = Message.createNewRootElement("opponentAttack");
                                    if (unit.isVisibleTo(enemyPlayer)) {
                                        opponentAttackElement.setAttribute("direction", Integer.toString(direction));
                                        opponentAttackElement.setAttribute("result", Integer.toString(result));
                                        opponentAttackElement
                                                .setAttribute("plunderGold", Integer.toString(plunderGold));
                                        opponentAttackElement.setAttribute("colony", colony.getId());
                                        opponentAttackElement.setAttribute("unit", attacker.getId());
                                        opponentAttackElement.setAttribute("defender", unit.getId());

                                        if (!enemyPlayer.canSee(attacker.getTile())) {
                                            opponentAttackElement.setAttribute("update", "tile");
                                            enemyPlayer.setExplored(attacker.getTile());
                                            opponentAttackElement.appendChild(attacker.getTile().toXMLElement(
                                                        enemyPlayer, opponentAttackElement.getOwnerDocument()));
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
                                /*
                                 * Element bombardElement =
                                 * Message.createNewRootElement("bombardResult");
                                 * bombardElement.setAttribute("result",
                                 * Integer.toString(result));
                                 * bombardElement.setAttribute("colony",
                                 * colony.getId());
                                 * 
                                 * if (!unit.isVisibleTo(player)) {
                                 * bombardElement.appendChild(unit.toXMLElement(player,
                                 * bombardElement.getOwnerDocument())); }
                                 * colony.bombard(unit, result); try {
                                 * currentPlayer.getConnection().send(bombardElement); }
                                 * catch (IOException e) { logger.warning("Could
                                 * not send message to: " +
                                 * currentPlayer.getName() + " with connection " +
                                 * currentPlayer.getConnection()); }
                                 */
                            }
                        }
                    }
                }
            }
        }
    }
}
