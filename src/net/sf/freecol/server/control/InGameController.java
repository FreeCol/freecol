package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
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

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

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
        FreeColServer freeColServer = getFreeColServer();
        Game game = freeColServer.getGame();
        ServerPlayer oldPlayer = (ServerPlayer) game.getCurrentPlayer();
        
        if (oldPlayer != player) {
            logger.warning("It is not " + player.getName() + "'s turn!");
            throw new IllegalArgumentException("It is not " + player.getName() + "'s turn!");
        }
        
        player.clearModelMessages();
        freeColServer.getModelController().clearTaskRegister();

        Player winner = checkForWinner();
        if (winner != null && (!freeColServer.isSingleplayer() || !winner.isAI())) {
            Element gameEndedElement = Message.createNewRootElement("gameEnded");
            gameEndedElement.setAttribute("winner", winner.getID());
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
        final Game game = freeColServer.getGame();
        
        if (!isHumanPlayersLeft()) {
            game.setCurrentPlayer(null);
            return null;
        }
        
        if (game.isNextPlayerInNewTurn()) {
            game.newTurn();
            if (debugOnlyAITurns > 0) {
                debugOnlyAITurns--;
            }
            Element newTurnElement = Message.createNewRootElement("newTurn");
            freeColServer.getServer().sendToAll(newTurnElement, null);
        }
        
        ServerPlayer newPlayer = (ServerPlayer) game.getNextPlayer();
        game.setCurrentPlayer(newPlayer);
        if (newPlayer == null) {
            game.setCurrentPlayer(null);
            return null;
        }
        
        synchronized (newPlayer) {
            if (checkForDeath(newPlayer)) {
                newPlayer.setDead(true);
                Element setDeadElement = Message.createNewRootElement("setDead");
                setDeadElement.setAttribute("player", newPlayer.getID());
                freeColServer.getServer().sendToAll(setDeadElement, null);
                return nextPlayer();
            }
        }
        
        if (newPlayer.isEuropean()) {
            try {
                Market market = newPlayer.getMarket();
                // make random change to the market
                market.add(getPseudoRandom().nextInt(Goods.NUMBER_OF_TYPES), (50 - getPseudoRandom().nextInt(71)));
                Element updateElement = Message.createNewRootElement("update");
                updateElement.appendChild(newPlayer.getMarket().toXMLElement(newPlayer, updateElement.getOwnerDocument()));
                newPlayer.getConnection().send(updateElement);
            } catch (IOException e) {
                logger.warning("Could not send message to: " + newPlayer.getName() + " with connection "
                        + newPlayer.getConnection());
            }

            if (newPlayer.getCurrentFather() == -1 && newPlayer.getSettlements().size() > 0) {
                chooseFoundingFather(newPlayer);
            }
            if (newPlayer.getMonarch() != null) {
                monarchAction(newPlayer);
            }
            bombardEnemyShips(newPlayer);
        }
        
        Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
        setCurrentPlayerElement.setAttribute("player", newPlayer.getID());
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
        Thread t = new Thread() {
            public void run() {
                int[] randomFoundingFathers = getRandomFoundingFathers(nextPlayer);
                boolean atLeastOneChoice = false;
                Element chooseFoundingFatherElement = Message.createNewRootElement("chooseFoundingFather");
                for (int i = 0; i < randomFoundingFathers.length; i++) {
                    chooseFoundingFatherElement.setAttribute("foundingFather" + Integer.toString(i), Integer
                            .toString(randomFoundingFathers[i]));
                    if (randomFoundingFathers[i] != -1) {
                        atLeastOneChoice = true;
                    }
                }
                if (!atLeastOneChoice) {
                    nextPlayer.setCurrentFather(-1);
                } else {
                    try {
                        Element reply = nextPlayer.getConnection().ask(chooseFoundingFatherElement);
                        int foundingFather = Integer.parseInt(reply.getAttribute("foundingFather"));
                        boolean foundIt = false;
                        for (int i = 0; i < randomFoundingFathers.length; i++) {
                            if (randomFoundingFathers[i] == foundingFather) {
                                foundIt = true;
                                break;
                            }
                        }
                        if (!foundIt) {
                            throw new IllegalArgumentException();
                        }
                        nextPlayer.setCurrentFather(foundingFather);
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
     * Returns an <code>int[]</code> with the size of
     * {@link FoundingFather#TYPE_COUNT}, containing random founding fathers
     * (not including the founding fathers the player has already) of each type.
     * 
     * @param player The <code>Player</code> that should pick a founding
     *            father from this list.
     */
    private int[] getRandomFoundingFathers(Player player) {
        Game game = getFreeColServer().getGame();
        int[] randomFoundingFathers = new int[FoundingFather.TYPE_COUNT];
        for (int i = 0; i < FoundingFather.TYPE_COUNT; i++) {
            int weightSum = 0;
            for (int j = 0; j < FoundingFather.FATHER_COUNT; j++) {
                if (!player.hasFather(j) && FoundingFather.getType(j) == i) {
                    weightSum += FoundingFather.getWeight(j, game.getTurn().getAge());
                }
            }
            if (weightSum == 0) {
                randomFoundingFathers[i] = -1;
            } else {
                int r = getPseudoRandom().nextInt(weightSum) + 1;
                weightSum = 0;
                for (int j = 0; j < FoundingFather.FATHER_COUNT; j++) {
                    if (!player.hasFather(j) && FoundingFather.getType(j) == i) {
                        weightSum += FoundingFather.getWeight(j, game.getTurn().getAge());
                        if (weightSum >= r) {
                            randomFoundingFathers[i] = j;
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
    private Player checkForWinner() {
        Game game = getFreeColServer().getGame();
        GameOptions go = game.getGameOptions();
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            Iterator<Player> playerIterator = game.getPlayerIterator();
            while (playerIterator.hasNext()) {
                Player p = playerIterator.next();
                if (!p.isAI() && p.getRebellionState() == Player.REBELLION_POST_WAR) {
                    return p;
                }
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            Player winner = null;
            Iterator<Player> playerIterator = game.getPlayerIterator();
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
            Iterator<Player> playerIterator = game.getPlayerIterator();
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
        // Die if: (No colonies or units on map) && ((After 20 turns) || (Cannot
        // get a unit from Europe))
        Game game = getFreeColServer().getGame();
        Map map = game.getMap();
        if (player.isREF()) {
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
        // At this point we know the player does not have any units or
        // settlements on the map.
        if (player.getNation() >= 0 && player.getNation() <= 3) {
            /*
             * if (game.getTurn().getNumber() > 20 ||
             * player.getEurope().getFirstUnit() == null && player.getGold() <
             * 600 && player.getGold() < player.getRecruitPrice()) {
             * 
             */
            if (game.getTurn().getNumber() > 20) {
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
        final Game game = getFreeColServer().getGame();
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
                        try {
                            Element reply = nextPlayer.getConnection().ask(monarchActionElement);
                            boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                            if (accepted) {
                                nextPlayer.setTax(newTax);
                            } else {
                                Element removeGoodsElement = Message.createNewRootElement("removeGoods");
                                if (goods != null) {
                                    ((Colony) goods.getLocation()).removeGoods(goods);
                                    nextPlayer.setArrears(goods);
                                    removeGoodsElement.appendChild(goods.toXMLElement(nextPlayer, removeGoodsElement
                                            .getOwnerDocument()));
                                }
                                nextPlayer.getConnection().send(removeGoodsElement);
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
                        int nation = monarch.declareWar();
                        if (nation == Player.NO_NATION) {
                            // this should not happen
                            logger.warning("Declared war on nobody.");
                            return;
                        }
                        nextPlayer.setStance(game.getPlayer(nation), Player.WAR);
                        monarchActionElement.setAttribute("nation", String.valueOf(nation));
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
                        newUnit = new Unit(game, nextPlayer.getEurope(), nextPlayer, Unit.FRIGATE, Unit.ACTIVE);
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
        final Game game = getFreeColServer().getGame();
        Unit newUnit;
        for (int type = 0; type < units.length; type++) {
            for (int i = 0; i < units[type]; i++) {
                if (type == Monarch.ARTILLERY) {
                    newUnit = new Unit(game, nextPlayer.getEurope(), nextPlayer, Unit.ARTILLERY, Unit.ACTIVE);
                } else {
                    boolean mounted = false;
                    if (type == Monarch.DRAGOON) {
                        mounted = true;
                    }
                    newUnit = new Unit(game, nextPlayer.getEurope(), nextPlayer, Unit.VETERAN_SOLDIER, Unit.ACTIVE,
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
            Building stockade = colony.getBuilding(Building.STOCKADE);
            if (stockade.getLevel() > Building.HOUSE && !colony.isLandLocked()) {
                logger.finest("Colony has harbour and fort.");
                float attackPower = 0;
                Unit attacker = null;
                Iterator<Unit> unitIterator = colony.getTile().getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    logger.finest("Unit is " + unit.getName());
                    switch (unit.getType()) {
                    case Unit.ARTILLERY:
                        attacker = unit;
                        attackPower += unit.getOffensePower(unit);
                        break;
                    case Unit.DAMAGED_ARTILLERY:
                        if (attacker == null) {
                            attacker = unit;
                        }
                        attackPower += unit.getOffensePower(unit);
                        break;
                    default:
                    }
                }
                if (attackPower <= 0) {
                    continue;
                } else if (attackPower > 48) {
                    attackPower = 48;
                }
                logger.finest("Colony has attack power " + attackPower);
                Position colonyPosition = colony.getTile().getPosition();
                for (int direction = 0; direction < Map.NUMBER_OF_DIRECTIONS; direction++) {
                    Tile tile = map.getTile(map.getAdjacent(colonyPosition, direction));
                    if (!tile.isLand()) {
                        unitIterator = tile.getUnitIterator();
                        while (unitIterator.hasNext()) {
                            Unit unit = unitIterator.next();
                            Player player = unit.getOwner();
                            if (player != currentPlayer
                                    && (currentPlayer.getStance(player) == Player.WAR || unit.getType() == Unit.PRIVATEER)) {
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
                                        opponentAttackElement.setAttribute("colony", colony.getID());
                                        opponentAttackElement.setAttribute("building", stockade.getID());
                                        opponentAttackElement.setAttribute("unit", unit.getID());

                                        if (!unit.isVisibleTo(enemyPlayer)) {
                                            opponentAttackElement.setAttribute("update", "unit");
                                            if (!enemyPlayer.canSee(unit.getTile())) {
                                                enemyPlayer.setExplored(unit.getTile());
                                                opponentAttackElement.appendChild(unit.getTile().toXMLElement(
                                                        enemyPlayer, opponentAttackElement.getOwnerDocument()));
                                            }
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
                                /*
                                 * Element bombardElement =
                                 * Message.createNewRootElement("bombardResult");
                                 * bombardElement.setAttribute("result",
                                 * Integer.toString(result));
                                 * bombardElement.setAttribute("colony",
                                 * colony.getID());
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
