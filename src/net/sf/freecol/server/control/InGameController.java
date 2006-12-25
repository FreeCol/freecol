
package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 *
 */
public final class InGameController extends Controller {
    private static Logger logger = Logger.getLogger(InGameController.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private Random random = new Random();
    
    public int debugOnlyAITurns = 0;
    

    /**
     * The constructor to use.
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
            throw new IllegalArgumentException("It is not " + player.getName() + "'s turn!");
        }

        // Clean up server side model messages:
        game.clearModelMessages();

        ServerPlayer nextPlayer = (ServerPlayer) game.getNextPlayer();

        synchronized (nextPlayer) {
            while (nextPlayer != null && checkForDeath(nextPlayer)) {
                nextPlayer.setDead(true);
                Element setDeadElement = Message.createNewRootElement("setDead");
                setDeadElement.setAttribute("player", nextPlayer.getID());
                freeColServer.getServer().sendToAll(setDeadElement, null);

                nextPlayer = (ServerPlayer) game.getNextPlayer();
            }
        }

        while (nextPlayer != null && !nextPlayer.isConnected()) {
            game.setCurrentPlayer(nextPlayer);
            nextPlayer = (ServerPlayer) game.getPlayerAfter(nextPlayer);
        }
        
        while (nextPlayer != null && !nextPlayer.isAI() && debugOnlyAITurns > 0) {
            nextPlayer = (ServerPlayer) game.getPlayerAfter(nextPlayer);
        }

        if (nextPlayer == null) {
            game.setCurrentPlayer(null);
            return;
        }

        Player winner = checkForWinner();
        if (winner != null && 
                (!freeColServer.isSingleplayer() || !winner.isAI())) {
            Element gameEndedElement = Message.createNewRootElement("gameEnded");
            gameEndedElement.setAttribute("winner", winner.getID());
            freeColServer.getServer().sendToAll(gameEndedElement, null);

            return;
        }

        freeColServer.getModelController().clearTaskRegister();

        if (game.isNextPlayerInNewTurn()) {
            game.newTurn();
            if (debugOnlyAITurns > 0) {
                debugOnlyAITurns--;
            }
            
            Element newTurnElement = Message.createNewRootElement("newTurn");
            freeColServer.getServer().sendToAll(newTurnElement, null);
        }

        if (nextPlayer.isEuropean()) {
            try {
                Market market = game.getMarket();
                // make random change to the market
                market.add(random.nextInt(Goods.NUMBER_OF_TYPES), (50 - random.nextInt(71)));

                Element updateElement = Message.createNewRootElement("update");
                updateElement.appendChild(game.getMarket().toXMLElement(nextPlayer, updateElement.getOwnerDocument()));
                nextPlayer.getConnection().send(updateElement);
            } catch (IOException e) {
                logger.warning("Could not send message to: " + nextPlayer.getName() + " with connection " + nextPlayer.getConnection());
            }
        }        

        game.setCurrentPlayer(nextPlayer);

        // Ask the player to choose a founding father if none has been chosen:
        if (nextPlayer.isEuropean()) {
            if (nextPlayer.getCurrentFather() == -1) {
                chooseFoundingFather(nextPlayer);
            }
            if (nextPlayer.getMonarch() != null) {
                monarchAction(nextPlayer);
            }
        }
 
        Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
        setCurrentPlayerElement.setAttribute("player", nextPlayer.getID());
        freeColServer.getServer().sendToAll(setCurrentPlayerElement, null);
   }


    private void chooseFoundingFather(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        Thread t = new Thread() {
                public void run() {
                    int[] randomFoundingFathers = getRandomFoundingFathers(nextPlayer);

                    boolean atLeastOneChoice = false;
                    Element chooseFoundingFatherElement = Message.createNewRootElement("chooseFoundingFather");
                    for (int i=0; i<randomFoundingFathers.length; i++) {
                        chooseFoundingFatherElement.setAttribute("foundingFather" + Integer.toString(i), Integer.toString(randomFoundingFathers[i]));
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
                            for (int i=0; i<randomFoundingFathers.length; i++) {
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
     * Returns an <code>int[]</code> with the size of {@link FoundingFather#TYPE_COUNT},
     * containing random founding fathers (not including the founding fathers
     * the player has already) of each type.
     *
     * @param player The <code>Player</code> that should pick a founding father from this list.
     */
    private int[] getRandomFoundingFathers(Player player) {
        Game game = getFreeColServer().getGame();

        int[] randomFoundingFathers = new int[FoundingFather.TYPE_COUNT];
        for (int i=0; i<FoundingFather.TYPE_COUNT; i++) {
            int weightSum = 0;
            for (int j=0; j<FoundingFather.FATHER_COUNT; j++) {
                if (!player.hasFather(j) && FoundingFather.getType(j) == i) {
                    weightSum += FoundingFather.getWeight(j, game.getTurn().getAge());
                }
            }

            if (weightSum == 0) {
                randomFoundingFathers[i] = -1;
            } else {
                int r = random.nextInt(weightSum)+1;

                weightSum = 0;
                for (int j=0; j<FoundingFather.FATHER_COUNT; j++) {
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
     * @return The <code>Player</code> who have won the game or <i>null</i>
     *         if the game is not finished.
     */
    private Player checkForWinner() {
        Game game = getFreeColServer().getGame();
        GameOptions go = game.getGameOptions();
        
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            Iterator playerIterator = game.getPlayerIterator();
            while (playerIterator.hasNext()) {
                Player p = (Player) playerIterator.next();
                
                if (!p.isAI() && p.getRebellionState() == Player.REBELLION_POST_WAR) {                  
                    return p;
                }
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            Player winner = null;
            Iterator playerIterator = game.getPlayerIterator();
            while (playerIterator.hasNext()) {
                Player p = (Player) playerIterator.next();
                
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
            Iterator playerIterator = game.getPlayerIterator();
            while (playerIterator.hasNext()) {
                Player p = (Player) playerIterator.next();
                
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
        // Die if: (No colonies or units on map) && ((After 20 turns) || (Cannot get a unit from Europe))
        Game game = getFreeColServer().getGame();
        Map map = game.getMap();

        if (player.isREF()) {
            return false;
        }
        
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
            } else if (player.getEurope() == null) {
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
                                    removeGoodsElement.appendChild(goods.toXMLElement(nextPlayer,
                                                                                      removeGoodsElement.getOwnerDocument()));
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
                            logger.warning( "Declared war on nobody." );
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
                        newUnit = new Unit(game,
                                           nextPlayer.getEurope(),
                                           nextPlayer,
                                           Unit.FRIGATE,
                                           Unit.ACTIVE);
                        nextPlayer.getEurope().add(newUnit);
                        monarchActionElement.appendChild(newUnit.toXMLElement(nextPlayer,
                                                                              monarchActionElement.getOwnerDocument()));
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
                    newUnit = new Unit(game,
                                       nextPlayer.getEurope(),
                                       nextPlayer,
                                       Unit.ARTILLERY,
                                       Unit.ACTIVE);
                } else {
                    boolean mounted = false;
                    if (type == Monarch.DRAGOON) {
                        mounted = true;
                    }
                    newUnit = new Unit(game,
                                       nextPlayer.getEurope(),
                                       nextPlayer,
                                       Unit.VETERAN_SOLDIER,
                                       Unit.ACTIVE,
                                       true,
                                       mounted,
                                       0,
                                       false);
                }
                nextPlayer.getEurope().add(newUnit);
                if (element != null) {
                    element.appendChild(newUnit.toXMLElement(nextPlayer, element.getOwnerDocument()));
                }
            }
        }
    }
}
