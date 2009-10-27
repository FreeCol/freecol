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
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * TODO: write class comment.
 */
public final class InGameController extends Controller {

    private static Logger logger = Logger.getLogger(InGameController.class.getName());

    public int debugOnlyAITurns = 0;

    private java.util.Map<String,java.util.Map<String, java.util.Map<String,Object>>> transactionSessions;
    
    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameController(FreeColServer freeColServer) {
        super(freeColServer);
        
        transactionSessions = new HashMap<String,java.util.Map<String, java.util.Map<String,Object>>>();
    }


    /**
     * Get a list of all server players, optionally excluding the supplied one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude (may be null).
     * @return A list of all connected server players except one.
     */
    public List<ServerPlayer> getOtherPlayers(ServerPlayer serverPlayer) {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        for (Player otherPlayer : getGame().getPlayers()) {
            ServerPlayer enemyPlayer = (ServerPlayer) otherPlayer;
            if (!enemyPlayer.equals(serverPlayer)
                && enemyPlayer.isConnected()) {
                result.add(enemyPlayer);
            }
        }
        return result;
    }

    /**
     * Tell all players to remove a unit, optionally excluding one player.
     * Only send if the unit is visible to the player.
     *
     * @param unit The <code>Unit</code> to remove.
     * @param serverPlayer A <code>ServerPlayer</code> to exclude (may be null).
     */
    public void sendRemoveUnitToAll(Unit unit, ServerPlayer serverPlayer) {
        Element remove = Message.createNewRootElement("remove");
        unit.addToRemoveElement(remove);
        for (ServerPlayer enemyPlayer : getOtherPlayers(serverPlayer)) {
            if (unit.isVisibleTo(enemyPlayer)) {
                try {
                    enemyPlayer.getConnection().sendAndWait(remove);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    /**
     * Unconditionally tell all players to update an object,
     * optionally excluding one player.
     *
     * @param obj The <code>FreeColGameObject</code> to update.
     * @param serverPlayer A <code>ServerPlayer</code> to exclude (may be null).
     */
    public void sendUpdateToAll(FreeColGameObject obj, ServerPlayer serverPlayer) {
        for (ServerPlayer enemyPlayer : getOtherPlayers(serverPlayer)) {
            Element update = Message.createNewRootElement("update");
            Document doc = update.getOwnerDocument();
            update.appendChild(obj.toXMLElement(enemyPlayer, doc));
            try {
                enemyPlayer.getConnection().sendAndWait(update);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }
    }

    /**
     * Tell all players to update a tile, optionally excluding one player.
     * Only send if the tile is visible.
     *
     * @param newTile The <code>Tile</code> to update.
     * @param serverPlayer A <code>ServerPlayer</code> to exclude (may be null).
     */
    public void sendUpdatedTileToAll(Tile newTile, ServerPlayer serverPlayer) {
        for (ServerPlayer enemyPlayer : getOtherPlayers(serverPlayer)) {
            if (enemyPlayer.canSee(newTile)) {
                Element update = Message.createNewRootElement("update");
                Document doc = update.getOwnerDocument();
                update.appendChild(newTile.toXMLElement(enemyPlayer, doc));
                try {
                    enemyPlayer.getConnection().sendAndWait(update);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
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
            throw new IllegalArgumentException("It is not "
                + player.getName() + "'s turn, it is "
                + ((oldPlayer == null) ? "noone" : oldPlayer.getName()) + "'s!");
        }
        
        player.clearModelMessages();
        freeColServer.getModelController().clearTaskRegister();

        Player winner = checkForWinner();
        if (winner != null && (!freeColServer.isSingleplayer() || !winner.isAI())) {
            Element gameEndedElement = Message.createNewRootElement("gameEnded");
            gameEndedElement.setAttribute("winner", winner.getId());
            freeColServer.getServer().sendToAll(gameEndedElement, null);
            
            // TODO: Remove when the server can properly revert to a pre-game state:
            if (FreeCol.getFreeColClient() == null) {
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 20000);
            }
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
     * Remove a standard yearly amount of storable goods, and
     * a random extra amount of a random type.
     * Send the market and change messages to the player.
     * This method is public so it can be use in the Market test code.
     *
     * @param player The player whose market is to be updated.
     */
    public void yearlyGoodsRemoval(ServerPlayer player) {
        List<ModelMessage> messages = new ArrayList<ModelMessage>();
        List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
        Market market = player.getMarket();

        // Pick a random type of goods to remove an extra amount of.
        GoodsType removeType;
        do {
            removeType = goodsTypes.get(getPseudoRandom().nextInt(goodsTypes.size()));
        } while (!removeType.isStorable());

        // Remove standard amount, and the extra amount.
        for (GoodsType type : goodsTypes) {
            if (type.isStorable()) {
                int amount = 10;
                if (type == removeType) {
                    amount += getPseudoRandom().nextInt(21);
                }
                if (market.addGoodsToMarket(type, -amount)) {
                    messages.add(market.makePriceMessage(type));
                }
            }
        }

        // Update the client
        Element element;
        if (messages.isEmpty()) {
            element = Message.createNewRootElement("update");
            Document doc = element.getOwnerDocument();
            element.appendChild(market.toXMLElement(player, doc));
        } else {
            element = Message.createNewRootElement("multiple");
            Document doc = element.getOwnerDocument();
            Element update = doc.createElement("update");
            element.appendChild(update);
            update.appendChild(market.toXMLElement(player, doc));
            Element mess = doc.createElement("addMessages");
            element.appendChild(mess);
            for (ModelMessage m : messages) {
                mess.appendChild(m.toXMLElement(player, doc));
            }
        }
        try {
            player.getConnection().send(element);
        } catch (Exception e) {
            logger.warning("Error sending yearly market update to "
                           + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Create an element to mark a player as dead, inform other players, and
     * remove any leftover units.
     *
     * @param serverPlayer The player to kill.
     * @return An element to kill the player.
     */
    private Element killPlayerElement(ServerPlayer serverPlayer) {
        Element element = Message.createNewRootElement("multiple");
        Document doc = element.getOwnerDocument();

        Element update = doc.createElement("update");
        element.appendChild(update);
        Player player = (Player) serverPlayer;
        player.setDead(true);
        update.appendChild(player.toXMLElementPartial(doc, "dead"));

        if (!serverPlayer.getUnits().isEmpty()) {
            Element remove = doc.createElement("remove");
            element.appendChild(remove);
            List<Unit> unitList = new ArrayList<Unit>(serverPlayer.getUnits());
            for (Unit unit : unitList) {
                serverPlayer.removeUnit(unit);
                unit.addToRemoveElement(remove);
                unit.dispose();
            }
        }

        Element messages = doc.createElement("addMessages");
        element.appendChild(messages);
        ModelMessage m = new ModelMessage(serverPlayer,
                                          ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                          serverPlayer, "model.diplomacy.dead",
                                          "%nation%", serverPlayer.getNationAsString());
        messages.appendChild(m.toXMLElement(doc));

        Element setDeadElement = doc.createElement("setDead");
        element.appendChild(setDeadElement);
        setDeadElement.setAttribute("player", serverPlayer.getId());

        return element;
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
            if (getGame().getTurn().getAge() > 1
                && !getGame().getSpanishSuccession()) {
                checkSpanishSuccession();
            }
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
            if (Player.checkForDeath(newPlayer)) {
                Element element = killPlayerElement(newPlayer);
                freeColServer.getServer().sendToAll(element, null);
                logger.info(newPlayer.getNationAsString() + " is dead.");
                return nextPlayer();
            }
        }
        
        if (newPlayer.isEuropean()) {
            yearlyGoodsRemoval(newPlayer);

            if (newPlayer.getCurrentFather() == null && newPlayer.getSettlements().size() > 0) {
                chooseFoundingFather(newPlayer);
            }
            if (newPlayer.getMonarch() != null) {
                monarchAction(newPlayer);
            }
            bombardEnemyShips(newPlayer);
        }
        else if (newPlayer.isIndian()) {
            
            for (IndianSettlement indianSettlement: newPlayer.getIndianSettlements()) {
                if (indianSettlement.checkForNewMissionaryConvert()) {
                    // an Indian brave gets converted by missionary
                    Unit missionary = indianSettlement.getMissionary();
                    ServerPlayer european = (ServerPlayer) missionary.getOwner();
                    // search for a nearby colony
                    Tile settlementTile = indianSettlement.getTile();
                    Tile targetTile = null;
                    Iterator<Position> ffi = getGame().getMap().getFloodFillIterator(settlementTile.getPosition());
                    while (ffi.hasNext()) {
                        Tile t = getGame().getMap().getTile(ffi.next());
                        if (settlementTile.getDistanceTo(t) > IndianSettlement.MAX_CONVERT_DISTANCE) {
                            break;
                        }
                        if (t.getSettlement() != null && t.getSettlement().getOwner() == european) {
                            targetTile = t;
                            break;
                        }
                    }
        
                    if (targetTile != null) {
                        
                        List<UnitType> converts = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.convert");
                        if (converts.size() > 0) {
                            // perform the conversion from brave to convert in the server
                            Unit brave = indianSettlement.getUnitIterator().next();
                            String nationId = brave.getOwner().getNationID();
                            brave.dispose();
                            ModelController modelController = getGame().getModelController();
                            int random = modelController.getRandom(indianSettlement.getId() + "getNewConvertType", converts.size());
                            UnitType unitType = converts.get(random);
                            Unit unit = modelController.createUnit(indianSettlement.getId() + "newTurn100missionary", targetTile,
                                    european, unitType);
                            // and send update information to the client
                            try {
                                Element updateElement = Message.createNewRootElement("newConvert");
                                updateElement.setAttribute("nation", nationId);
                                updateElement.setAttribute("colonyTile", targetTile.getId());
                                updateElement.appendChild(unit.toXMLElement(european,updateElement.getOwnerDocument()));
                                european.getConnection().send(updateElement);
                                logger.info("New convert created for " + european.getName() + " with ID=" + unit.getId());
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + european.getName());
                            }
                        }
                    }
                }
            }
        }
        
        Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
        setCurrentPlayerElement.setAttribute("player", newPlayer.getId());
        freeColServer.getServer().sendToAll(setCurrentPlayerElement, null);
        
        return newPlayer;
    }

    private void checkSpanishSuccession() {
        boolean rebelMajority = false;
        Player weakestAIPlayer = null;
        Player strongestAIPlayer = null;
        java.util.Map<Player, Element> documentMap = new HashMap<Player, Element>();
        for (Player player : getGame().getPlayers()) {
            documentMap.put(player, Message.createNewRootElement("spanishSuccession"));
            if (player.isEuropean()) {
                if (player.isAI() && !player.isREF()) {
                    if (weakestAIPlayer == null
                        || weakestAIPlayer.getScore() > player.getScore()) {
                        weakestAIPlayer = player;
                    }
                    if (strongestAIPlayer == null
                        || strongestAIPlayer.getScore() < player.getScore()) {
                        strongestAIPlayer = player;
                    }
                } else if (player.getSoL() > 50) {
                    rebelMajority = true;
                }
            }
        }

        if (rebelMajority
            && weakestAIPlayer != null
            && strongestAIPlayer != null
            && weakestAIPlayer != strongestAIPlayer) {
            documentMap.remove(weakestAIPlayer);
            for (Element element : documentMap.values()) {
                element.setAttribute("loser", weakestAIPlayer.getId());
                element.setAttribute("winner", strongestAIPlayer.getId());
            }
            for (Colony colony : weakestAIPlayer.getColonies()) {
                colony.setOwner(strongestAIPlayer);
                for (Entry<Player, Element> entry : documentMap.entrySet()) {
                    if (entry.getKey().canSee(colony.getTile())) {
                        entry.getValue().appendChild(colony.toXMLElement(entry.getKey(),
                                                                         entry.getValue().getOwnerDocument()));
                    }
                }
            }
            for (Unit unit : weakestAIPlayer.getUnits()) {
                unit.setOwner(strongestAIPlayer);
                for (Entry<Player, Element> entry : documentMap.entrySet()) {
                    if (entry.getKey().canSee(unit.getTile())) {
                        entry.getValue().appendChild(unit.toXMLElement(entry.getKey(),
                                                                       entry.getValue().getOwnerDocument()));
                    }
                }
            }
            for (Entry<Player, Element> entry : documentMap.entrySet()) {
                try {
                    ((ServerPlayer) entry.getKey()).getConnection().send(entry.getValue());
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + entry.getKey().getName());
                }
            }
            weakestAIPlayer.setDead(true);
            getGame().setSpanishSuccession(true);
        }
    }
    
    private boolean isHumanPlayersLeft() {
        for (Player player : getFreeColServer().getGame().getPlayers()) {
            if (!player.isDead() && !player.isAI() && ((ServerPlayer) player).isConnected()) {
                return true;
            }
        }
        return false;
    }

    private void chooseFoundingFather(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        Thread t = new Thread(FreeCol.SERVER_THREAD+"FoundingFather-thread") {
                public void run() {
                    List<FoundingFather> randomFoundingFathers = getRandomFoundingFathers(nextPlayer);
                    boolean atLeastOneChoice = false;
                    Element chooseFoundingFatherElement = Message.createNewRootElement("chooseFoundingFather");
                    for (FoundingFather father : randomFoundingFathers) {
                        chooseFoundingFatherElement.setAttribute(father.getType().toString(),
                                                                 father.getId());
                        atLeastOneChoice = true;
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
        EnumMap<FoundingFatherType, Integer> weightSums = new
            EnumMap<FoundingFatherType, Integer>(FoundingFatherType.class);
        for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
            if (!player.hasFather(father) && father.isAvailableTo(player)) {
                Integer weightSum = weightSums.get(father.getType());
                if (weightSum == null) {
                    weightSum = new Integer(0);
                }
                weightSums.put(father.getType(), weightSum + father.getWeight(age));
            }
        }
        for (java.util.Map.Entry<FoundingFatherType, Integer> entry : weightSums.entrySet()) {
            if (entry.getValue() != 0) {
                int r = getPseudoRandom().nextInt(entry.getValue()) + 1;
                int weightSum = 0;
                for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                    if (!player.hasFather(father) && father.getType() == entry.getKey()) {
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
        List<Player> players = getGame().getPlayers();
        GameOptions go = getGame().getGameOptions();
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            for (Player player : players) {
                if (!player.isAI() && player.getPlayerType() == PlayerType.INDEPENDENT) {
                    return player;
                }
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            Player winner = null;
            for (Player player : players) {
                if (!player.isDead() && player.isEuropean() && !player.isREF()) {
                    if (winner != null) {
                        // There is more than one european player alive:
                        winner = null;
                        break;
                    } else {
                        winner = player;
                    }
                }
            }
            if (winner != null) {
                return winner;
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
            Player winner = null;
            for (Player player : players) {
                if (!player.isDead() && !player.isAI()) {
                    if (winner != null) {
                        // There is more than one human player alive:
                        winner = null;
                        break;
                    } else {
                        winner = player;
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
     * Checks for monarch actions.
     * 
     * @param player The server player.
     */
    private void monarchAction(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        Thread t = new Thread("monarchAction") {
                public void run() {
                    try {
                        Monarch monarch = nextPlayer.getMonarch();
                        MonarchAction action = monarch.getAction();
                        Element monarchActionElement = Message.createNewRootElement("monarchAction");
                        monarchActionElement.setAttribute("action", String.valueOf(action));
                        switch (action) {
                        case RAISE_TAX:
                            int oldTax = nextPlayer.getTax();
                            int newTax = monarch.getNewTax(MonarchAction.RAISE_TAX);
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
                                        colony.removeGoods(goods);
                                        nextPlayer.setArrears(goods);
                                        colony.getFeatureContainer().addModifier(Modifier
                                            .createTeaPartyModifier(getGame().getTurn()));
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
                        case LOWER_TAX:
                            int taxLowered = monarch.getNewTax(MonarchAction.LOWER_TAX);
                            if (taxLowered < 0) {
                                logger.warning("Tax rate less than 0 percent.");
                                return;
                            }
                            monarchActionElement.setAttribute("amount", String.valueOf(taxLowered));
                            try {
                                nextPlayer.setTax(taxLowered); // to avoid cheating
                                nextPlayer.getConnection().send(monarchActionElement); 
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                        case ADD_TO_REF:
                            List<AbstractUnit> unitsToAdd = monarch.addToREF();
                            monarch.addToREF(unitsToAdd);
                            Element additionElement = monarchActionElement.getOwnerDocument().createElement("addition");
                            for (AbstractUnit unit : unitsToAdd) {
                                additionElement.appendChild(unit.toXMLElement(nextPlayer,additionElement.getOwnerDocument()));
                            }
                            monarchActionElement.appendChild(additionElement);
                            try {
                                nextPlayer.getConnection().send(monarchActionElement);
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                        case DECLARE_WAR:
                            Player enemy = monarch.declareWar();
                            if (enemy == null) {
                                // this should not happen
                                logger.warning("Declared war on nobody.");
                                return;
                            }
                            // We also need to change the tension to avoid the AI player declaring cease-fire right away
                            if(nextPlayer.isAI()){
                                nextPlayer.modifyTension(enemy, Tension.TENSION_ADD_DECLARE_WAR_FROM_PEACE);
                            }
                            nextPlayer.changeRelationWithPlayer(enemy, Stance.WAR);
                            monarchActionElement.setAttribute("enemy", enemy.getId());
                            try {
                                nextPlayer.getConnection().send(monarchActionElement);
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                            /** TODO: restore
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
                                newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer, unitType, UnitState.ACTIVE);
                                //nextPlayer.getEurope().add(newUnit);
                                monarchActionElement.appendChild(newUnit.toXMLElement(nextPlayer, monarchActionElement
                                .getOwnerDocument()));
                                try {
                                nextPlayer.getConnection().send(monarchActionElement);
                                } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                                }
                                break;
                            */
                        case OFFER_MERCENARIES:
                            Element mercenaryElement = monarchActionElement.getOwnerDocument().createElement("mercenaries");
                            List<AbstractUnit> units = monarch.getMercenaries();
                            int price = monarch.getPrice(units, true);
                            monarchActionElement.setAttribute("price", String.valueOf(price));
                            for (AbstractUnit unit : units) {
                                mercenaryElement.appendChild(unit.toXMLElement(monarchActionElement.getOwnerDocument()));
                            }
                            monarchActionElement.appendChild(mercenaryElement);
                            try {
                                Element reply = nextPlayer.getConnection().ask(monarchActionElement);
                                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                                if (accepted) {
                                    Element updateElement = Message.createNewRootElement("monarchAction");
                                    updateElement.setAttribute("action", String.valueOf(MonarchAction.ADD_UNITS));
                                    nextPlayer.modifyGold(-price);
                                    createUnits(units, updateElement, nextPlayer);
                                    nextPlayer.getConnection().send(updateElement);
                                }
                            } catch (IOException e) {
                                logger.warning("Could not send message to: " + nextPlayer.getName());
                            }
                            break;
                        case NO_ACTION:
                            // nothing to do here. :-)
                            break;
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Monarch action failed!", e);
                    }
                }
            };
        t.start();
    }

    public ServerPlayer createREFPlayer(ServerPlayer player){
        Nation refNation = player.getNation().getRefNation();
        ServerPlayer refPlayer = getFreeColServer().addAIPlayer(refNation);
        refPlayer.setEntryLocation(player.getEntryLocation());
        // This will change later, just for setup
        player.setStance(refPlayer, Stance.PEACE);
        refPlayer.setTension(player, new Tension(Tension.Level.CONTENT.getLimit()));
        player.setTension(refPlayer, new Tension(Tension.Level.CONTENT.getLimit()));
        
        return refPlayer;
    }
    
    private void createUnits(List<AbstractUnit> units, Element element, ServerPlayer nextPlayer) {
        String musketsTypeStr = null;
        String horsesTypeStr = null;
        if(nextPlayer.isIndian()){
                musketsTypeStr = "model.equipment.indian.muskets";
            horsesTypeStr = "model.equipment.indian.horses";
        } else {
                musketsTypeStr = "model.equipment.muskets";
            horsesTypeStr = "model.equipment.horses";
        }
        
        final EquipmentType muskets = FreeCol.getSpecification().getEquipmentType(musketsTypeStr);
        final EquipmentType horses = FreeCol.getSpecification().getEquipmentType(horsesTypeStr);
        
        EquipmentType[] soldier = new EquipmentType[] { muskets };
        EquipmentType[] dragoon = new EquipmentType[] { horses, muskets };
        for (AbstractUnit unit : units) {
            EquipmentType[] equipment = EquipmentType.NO_EQUIPMENT;
            for (int count = 0; count < unit.getNumber(); count++) {
                switch(unit.getRole()) {
                case SOLDIER:
                    equipment = soldier;
                    break;
                case DRAGOON:
                    equipment = dragoon;
                    break;
                default:
                }
                Unit newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer,
                                        unit.getUnitType(), UnitState.ACTIVE, equipment);
                //nextPlayer.getEurope().add(newUnit);
                if (element != null) {
                    element.appendChild(newUnit.toXMLElement(nextPlayer, element.getOwnerDocument()));
                }
            }
        }
    }
    
    public List<Unit> createREFUnits(ServerPlayer player, ServerPlayer refPlayer){
        EquipmentType muskets = Specification.getSpecification().getEquipmentType("model.equipment.muskets");
        EquipmentType horses = Specification.getSpecification().getEquipmentType("model.equipment.horses");
        
        List<Unit> unitsList = new ArrayList<Unit>();
        List<Unit> navalUnits = new ArrayList<Unit>();
        List<Unit> landUnits = new ArrayList<Unit>();
        
        // Create naval units
        for (AbstractUnit unit : player.getMonarch().getNavalUnits()) {
            for (int index = 0; index < unit.getNumber(); index++) {
                Unit newUnit = new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                        unit.getUnitType(), UnitState.TO_AMERICA);
                navalUnits.add(newUnit);
            }
        }
        unitsList.addAll(navalUnits);
        
        // Create land units
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
                landUnits.add(new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                        unit.getUnitType(), UnitState.ACTIVE, equipment));
            }
        }
        unitsList.addAll(landUnits);
            
        // Board land units
        Iterator<Unit> carriers = navalUnits.iterator();
        for(Unit unit : landUnits){
            //cycle through the naval units to find a carrier for this unit
            
            // check if there is space for this unit
            boolean noSpaceForUnit=true;
            for(Unit carrier : navalUnits){
                if (unit.getSpaceTaken() <= carrier.getSpaceLeft()) {
                    noSpaceForUnit=false;
                    break;
                }
            }
            // There is no space for this unit, stays in Europe
            if(noSpaceForUnit){
                continue;
            }
            // Find carrier
            Unit carrier = null;
            while (carrier == null){
                // got to the end of the list, restart
                if (!carriers.hasNext()) {
                    carriers = navalUnits.iterator();
                }
                carrier = carriers.next();
                // this carrier cant carry this unit
                if (unit.getSpaceTaken() > carrier.getSpaceLeft()) {
                    carrier = null;
                }
            }
            // set unit aboard carrier
            unit.setLocation(carrier);
            //XXX: why only the units that can be aboard are sent to the player?
            //unitsList.add(unit);
        }
        return unitsList;
    }
    
    public boolean createMission(IndianSettlement settlement, Unit missionary) {
        Tension tension = settlement.getAlarm(missionary.getOwner());
        if (tension != null) {
            switch (tension.getLevel()) {
            case HAPPY: case CONTENT: case DISPLEASED:
                settlement.setMissionary(missionary);
                return true;
            case ANGRY: case HATEFUL:
                missionary.dispose();
                return false;
            }
        }
        return false;
    }

    private void bombardEnemyShips(ServerPlayer currentPlayer) {
        logger.finest("Entering method bombardEnemyShips.");
        Map map = getFreeColServer().getGame().getMap();
        CombatModel combatModel = getFreeColServer().getGame().getCombatModel();
        for (Settlement settlement : currentPlayer.getSettlements()) {
            Colony colony = (Colony) settlement;
            if (colony.canBombardEnemyShip()){
                logger.fine("Colony " + colony.getName() + " can bombard enemy ships.");
                Position colonyPosition = colony.getTile().getPosition();
                for (Direction direction : Direction.values()) {
                    Tile tile = map.getTile(Map.getAdjacent(colonyPosition, direction));
                    
                    // ignore land tiles and borders
                    if(tile == null || tile.isLand()){
                        continue;
                    }
                    
                    // Go through the units in the tile
                    // a new list must be created, since the original may be changed while iterating
                    List<Unit> unitList = new ArrayList<Unit>(tile.getUnitList());
                    Iterator<Unit> unitIterator = unitList.iterator();
                    while (unitIterator.hasNext()) {
                        Unit unit = unitIterator.next();
                        Player player = unit.getOwner();
                    
                        // ignore own units
                        if(player == currentPlayer){
                                continue;
                        }
                        
                        // ignore friendly units
                        if(currentPlayer.getStance(player) != Stance.WAR &&
                                                !unit.hasAbility("model.ability.piracy")){
                                continue;
                        }

                        logger.info(colony.getName() + " found enemy unit to bombard: " + unit.getName() + "(" + unit.getOwner().getNationAsString() + ")");
                        // generate bombardment result
                        CombatModel.CombatResult result = combatModel.generateAttackResult(colony, unit);

                        // ship was damaged, get repair location
                        Location repairLocation = null;
                        if(result.type == CombatModel.CombatResultType.WIN){
                                        repairLocation = player.getRepairLocation(unit);
                        }
                                        
                        // update server data
                        getGame().getCombatModel().bombard(colony, unit, result, repairLocation);

                        // Inform the players (other then the player
                        // attacking) about the attack:
                        int plunderGold = -1;
                        Iterator<Player> enemyPlayerIterator = getFreeColServer().getGame().getPlayerIterator();
                        while (enemyPlayerIterator.hasNext()) {
                                ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

                                if (//currentPlayer.equals(enemyPlayer) ||
                                                enemyPlayer.getConnection() == null) {
                                        continue;
                                }
                                
                                // unit not visible to player, move to next player
                                if(!unit.isVisibleTo(enemyPlayer)){
                                        continue;
                                }
                                
                                Element opponentAttackElement = Message.createNewRootElement("opponentAttack");                                 
                                opponentAttackElement.setAttribute("direction", direction.toString());
                                opponentAttackElement.setAttribute("result", result.type.toString());
                                opponentAttackElement.setAttribute("plunderGold", Integer.toString(plunderGold));
                            opponentAttackElement.setAttribute("colony", colony.getId());
                                opponentAttackElement.setAttribute("defender", unit.getId());
                                opponentAttackElement.setAttribute("damage", String.valueOf(result.damage));

                                // Add repair location to defending player
                                if(enemyPlayer == player && repairLocation != null){
                                        opponentAttackElement.setAttribute("repairIn", repairLocation.getId());
                                }
                                
                                // Every player who witness the confrontation needs to know about the attacker
                                if (!enemyPlayer.canSee(colony.getTile())) {
                                        opponentAttackElement.setAttribute("update", "tile");
                                        enemyPlayer.setExplored(colony.getTile());
                                        opponentAttackElement.appendChild(colony.getTile().toXMLElement(
                                                        enemyPlayer, opponentAttackElement.getOwnerDocument()));
                                }
                                
                                // update players view of the unit
                                if (enemyPlayer.canSee(unit.getTile())) { 
                            opponentAttackElement.setAttribute("update", "unit");
                            opponentAttackElement.appendChild(unit.toXMLElement(enemyPlayer, opponentAttackElement.getOwnerDocument()));
                                }
                                
                                // Send response
                                try {
                                        enemyPlayer.getConnection().send(opponentAttackElement);
                                } catch (IOException e) {
                                        logger.warning("Could not send message to: " + enemyPlayer.getName()
                                                        + " with connection " + enemyPlayer.getConnection());
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
    
    public java.util.Map<String,Object> getTransactionSession(Unit unit, Settlement settlement){
        java.util.Map<String, java.util.Map<String,Object>> unitTransactions = null;

        if(transactionSessions.containsKey(unit.getId())){
            unitTransactions = transactionSessions.get(unit.getId());
            if(unitTransactions.containsKey(settlement.getId())){
                return unitTransactions.get(settlement.getId());
            }
        }
        // Session does not exist, create, store, and return it
        java.util.Map<String,Object> session = new HashMap<String,Object>();
        // default values
        session.put("canGift", true);
        session.put("canSell", true);
        session.put("canBuy", true);
        session.put("actionTaken", false);
        session.put("hasSpaceLeft", unit.getSpaceLeft() != 0);
        session.put("unitMoves", unit.getMovesLeft());
        if(settlement.getOwner().getStance(unit.getOwner()) == Stance.WAR){
            session.put("canSell", false);
            session.put("canBuy", false);
        }
        else{
        	// the unit took nothing to sell, so nothing should be in this session
            if(unit.getSpaceTaken() == 0){
                session.put("canSell", false);
            }
        }
        // only keep track of human player sessions
        if(unit.getOwner().isAI()){
            return session;
        }
        
        // Save session for tracking
        
        // unit has no open transactions
        if(unitTransactions == null){
            unitTransactions = new HashMap<String,java.util.Map<String, Object>>();
            transactionSessions.put(unit.getId(), unitTransactions);
        }
        unitTransactions.put(settlement.getId(), session);
        return session;
    }

    public void closeTransactionSession(Unit unit, Settlement settlement){
        java.util.Map<String, java.util.Map<String,Object>> unitTransactions;
        
        // only keep track of human player sessions
        if(unit.getOwner().isAI()){
          return;  
        }
        
        if(!transactionSessions.containsKey(unit.getId())){
            throw new IllegalStateException("Trying to close a non-existing session");
        }
        
        unitTransactions = transactionSessions.get(unit.getId());   
        if(!unitTransactions.containsKey(settlement.getId())){
            throw new IllegalStateException("Trying to close a non-existing session");
        }
        
        unitTransactions.remove(settlement.getId());
        if(unitTransactions.isEmpty()){
            transactionSessions.remove(unit.getId());
        }
    }
    
    public boolean isTransactionSessionOpen(Unit unit, Settlement settlement){
        // AI does not need to send a message to open a session
        if(unit.getOwner().isAI()){
            return true;
        }
        
        if(!transactionSessions.containsKey(unit.getId())){
            return false;
        }
        if(settlement != null &&
           !transactionSessions.get(unit.getId()).containsKey(settlement.getId())){
                return false;
        }
        return true;
    }

    /**
     * A unit migrates from Europe.
     *
     * @param player The <code>ServerPlayer</code> whose unit it will be.
     * @param slot The slot within <code>Europe</code> to select the unit from.
     * @param fountain True if this occurs as a result of a Fountain of Youth.
     */
    public ModelMessage emigrate(ServerPlayer player, int slot, boolean fountain) {
        // Valid slots are in [1,3], recruitable indices are in [0,2].
        // An invalid slot is normal when the player has no control over
        // recruit type.
        boolean validSlot = 1 <= slot && slot <= Europe.RECRUIT_COUNT;
        int index = (validSlot) ? slot-1
            : getPseudoRandom().nextInt(Europe.RECRUIT_COUNT);

        // Create the recruit, move it to the docks.
        Europe europe = player.getEurope();
        UnitType recruitType = europe.getRecruitable(index);
        Unit unit = new Unit(getGame(), europe, player, recruitType,
                             UnitState.ACTIVE, recruitType.getDefaultEquipment());
        unit.setLocation(europe);

        // Update immigration counters if this was an ordinary decision to migrate.
        if (!fountain) {
            player.updateImmigrationRequired();
            player.reduceImmigration();
        }

        // Replace the recruit we used.
        europe.setRecruitable(index, player.generateRecruitable(player.getId() + "slot." + Integer.toString(slot)));

        // Return an informative message only if this was an ordinary migration
        // where we did not select the unit type.
        // Fountain of Youth migrants have already been announced in bulk.
        return (fountain || validSlot) ? null
            : new ModelMessage(player, ModelMessage.MessageType.UNIT_ADDED,
                               unit, "model.europe.emigrate",
                               "%europe%", europe.getName(),
                               "%unit%", unit.getName());
    }


    /**
     * If a unit moves, check if an opposing naval unit slows it down.
     * Note that the unit moves are reduced here.
     *
     * @param unit The <code>Unit</code> that is moving.
     * @param tile The <code>Tile</code> the unit is moving to.
     * @return Either an enemy unit that causes a slowdown, or null if none.
     */
    public Unit getSlowedBy(Unit unit, Tile newTile) {
        Player player = unit.getOwner();
        Game game = unit.getGame();
        CombatModel combatModel = game.getCombatModel();
        Unit attacker = null;
        boolean pirate = unit.hasAbility("model.ability.piracy");
        float attackPower = 0;

        if (!unit.isNaval() || unit.getMovesLeft() <= 0) return null;
        for (Tile tile : game.getMap().getSurroundingTiles(newTile, 1)) {
            // Ships in settlements do not slow enemy ships, but:
            // TODO should a fortress slow a ship?
            Player enemy;
            if (tile.isLand()
                || tile.getColony() != null
                || tile.getFirstUnit() == null
                || (enemy = tile.getFirstUnit().getOwner()) == player) continue;
            for (Unit enemyUnit : tile.getUnitList()) {
                if (pirate || enemyUnit.hasAbility("model.ability.piracy")
                    || (enemyUnit.isOffensiveUnit()
                        && player.getStance(enemy) == Stance.WAR)) {
                    attackPower += combatModel.getOffencePower(enemyUnit, unit);
                    if (attacker == null) {
                        attacker = enemyUnit;
                    }
                }
            }
        }
        if (attackPower > 0) {
            float defencePower = combatModel.getDefencePower(attacker, unit);
            float totalProbability = attackPower + defencePower;
            if (getPseudoRandom().nextInt(Math.round(totalProbability) + 1)
                < attackPower) {
                int diff = Math.max(0, Math.round(attackPower - defencePower));
                int moves = Math.min(9, 3 + diff / 3);
                unit.setMovesLeft(unit.getMovesLeft() - moves);
                logger.info(unit.getId()
                            + " slowed by " + attacker.getId()
                            + " by " + Integer.toString(moves) + " moves.");
            } else {
                attacker = null;
            }
        }
        return attacker;
    }


    /**
     * Returns a type of Lost City Rumour. The type of rumour depends on the
     * exploring unit, as well as player settings.
     *
     * @param lostCity The <code>LostCityRumour</code> to investigate.
     * @param unit The <code>Unit</code> exploring the lost city rumour.
     * @param difficulty The difficulty level.
     * @return The type of rumour.
     * @todo Move all the magic numbers in here to the specification.
     *       Also change the logic so that the special events appear a fixed number
     *       of times throughout the game, according to the specification.
     *       Names for the cities of gold is also on the wishlist.
     */
    private RumourType getLostCityRumourType(LostCityRumour lostCity,
                                             Unit unit, int difficulty) {
        Tile tile = unit.getTile();
        Player player = unit.getOwner();
        RumourType rumour = lostCity.getType();

        if (rumour != null) {
            // Filter out failing cases that could only occur if the
            // type was explicitly set in debug mode.
            switch (rumour) {
            case BURIAL_GROUND:
                if (tile.getOwner() == null || !tile.getOwner().isIndian()) {
                    rumour = RumourType.NOTHING;
                }
                break;
            case LEARN:
                if (unit.getType().getUnitTypesLearntInLostCity().isEmpty()) {
                    rumour = RumourType.NOTHING;
                }
                break;
            default:
                break;
            }
            return rumour;
        }

        // The following arrays contain percentage values for
        // "good" and "bad" events when scouting with a non-expert
        // at the various difficulty levels [0..4] exact values
        // but generally "bad" should increase, "good" decrease
        final int BAD_EVENT_PERCENTAGE[]  = { 11, 17, 23, 30, 37 };
        final int GOOD_EVENT_PERCENTAGE[] = { 75, 62, 48, 33, 17 };
        // remaining to 100, event NOTHING:   14, 21, 29, 37, 46

        // The following arrays contain the modifiers applied when
        // expert scout is at work exact values; modifiers may
        // look slightly "better" on harder levels since we're
        // starting from a "worse" percentage.
        final int BAD_EVENT_MOD[]  = { -6, -7, -7, -8, -9 };
        final int GOOD_EVENT_MOD[] = { 14, 15, 16, 18, 20 };

        // The scouting outcome is based on three factors: level,
        // expert scout or not, DeSoto or not.  Based on this, we
        // are going to calculate probabilites for neutral, bad
        // and good events.
        boolean isExpertScout = unit.hasAbility("model.ability.expertScout")
            && unit.hasAbility("model.ability.scoutIndianSettlement");
        boolean hasDeSoto = player.hasAbility("model.ability.rumoursAlwaysPositive");
        int percentNeutral;
        int percentBad;
        int percentGood;

        if (hasDeSoto) {
            percentBad  = 0;
            percentGood = 100;
            percentNeutral = 0;
        } else { // First, get "basic" percentages
            percentBad  = BAD_EVENT_PERCENTAGE[difficulty];
            percentGood = GOOD_EVENT_PERCENTAGE[difficulty];

            // Second, apply ExpertScout bonus if necessary
            if (isExpertScout) {
                percentBad  += BAD_EVENT_MOD[difficulty];
                percentGood += GOOD_EVENT_MOD[difficulty];
            }

            // Third, get a value for the "neutral" percentage,
            // unless the other values exceed 100 already
            if (percentBad + percentGood < 100) {
                percentNeutral = 100 - percentBad - percentGood;
            } else {
                percentNeutral = 0;
            }
        }

        // Now, the individual events; each section should add up to 100
        // The NEUTRAL
        int eventNothing = 100;

        // The BAD
        int eventVanish = 100;
        int eventBurialGround = 0;
        // If the tile not is European-owned, allow burial grounds rumour.
        if (tile.getOwner() != null && tile.getOwner().isIndian()) {
            eventVanish = 75;
            eventBurialGround = 25;
        }

        // The GOOD
        int eventLearn    = 30;
        int eventTrinkets = 30;
        int eventColonist = 20;
        // or, if the unit can't learn
        if (unit.getType().getUnitTypesLearntInLostCity().isEmpty()) {
            eventLearn    =  0;
            eventTrinkets = 50;
            eventColonist = 30;
        }

        // The SPECIAL
        // Right now, these are considered "good" events that happen randomly.
        int eventDorado   = 13;
        int eventFountain = 7;

        // Finally, apply the Good/Bad/Neutral modifiers from
        // above, so that we end up with a ton of values, some of
        // them zero, the sum of which should be 10000.
        eventNothing      *= percentNeutral;
        eventVanish       *= percentBad;
        eventBurialGround *= percentBad;
        eventLearn        *= percentGood;
        eventTrinkets     *= percentGood;
        eventColonist     *= percentGood;
        eventDorado       *= percentGood;
        eventFountain     *= percentGood;

        // Add all possible events to a RandomChoice List
        List<RandomChoice<RumourType>> choices = new ArrayList<RandomChoice<RumourType>>();
        if (eventNothing > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.NOTHING, eventNothing));
        }
        if (eventVanish > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.EXPEDITION_VANISHES, eventVanish));
        }
        if (eventBurialGround > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.BURIAL_GROUND, eventBurialGround));
        }
        if (eventLearn > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.LEARN, eventLearn));
        }
        if (eventTrinkets > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.TRIBAL_CHIEF, eventTrinkets));
        }
        if (eventColonist > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.COLONIST, eventColonist));
        }
        if (eventFountain > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.FOUNTAIN_OF_YOUTH, eventFountain));
        }
        if (eventDorado > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.TREASURE, eventDorado));
        }
        return RandomChoice.getWeightedRandom(getPseudoRandom(), choices);
    }

    /**
     * Explore a lost city.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @return A list of FreeColObjects to send to the client as a result.
     */
    public List<FreeColObject> exploreLostCityRumour(ServerPlayer serverPlayer, Unit unit) {
        List<FreeColObject> result = new ArrayList<FreeColObject>();
        Specification specification = FreeCol.getSpecification();
        int difficulty = specification.getRangeOption("model.option.difficulty").getValue();
        int dx = 10 - difficulty;
        Game game = unit.getGame();
        Tile tile = unit.getTile();
        LostCityRumour lostCity = tile.getLostCityRumour();

        if (lostCity == null) return result;
        switch (getLostCityRumourType(lostCity, unit, difficulty)) {
        case BURIAL_GROUND:
            Player indianPlayer = tile.getOwner();
            indianPlayer.modifyTension(serverPlayer, Tension.Level.HATEFUL.getLimit());
            result.add(indianPlayer);
            result.add(new ModelMessage(serverPlayer,
                                        ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        unit,
                                        "lostCityRumour.BurialGround",
                                        "%nation%", indianPlayer.getNationAsString()));
            break;
        case EXPEDITION_VANISHES:
            unit.dispose();
            result.add(new ModelMessage(serverPlayer,
                                        ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        null,
                                        "lostCityRumour.ExpeditionVanishes"));
            break;
        case NOTHING:
            result.add(new ModelMessage(serverPlayer,
                                        ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        unit,
                                        "lostCityRumour.Nothing"));
            break;
        case LEARN:
            List<UnitType> learntUnitTypes = unit.getType().getUnitTypesLearntInLostCity();
            String oldName = unit.getName();
            unit.setType(learntUnitTypes.get(getPseudoRandom().nextInt(learntUnitTypes.size())));
            result.add(new ModelMessage(serverPlayer,
                                        ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        unit,
                                        "lostCityRumour.Learn",
                                        "%unit%", oldName,
                                        "%type%", unit.getType().getName()));
            break;
        case TRIBAL_CHIEF:
            int chiefAmount = getPseudoRandom().nextInt(dx * 10) + dx * 5;
            serverPlayer.modifyGold(chiefAmount);
            result.add(serverPlayer);
            result.add(new ModelMessage(serverPlayer,
                                        ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        unit,
                                        "lostCityRumour.TribalChief",
                                        "%money%", Integer.toString(chiefAmount)));
            break;
        case COLONIST:
            List<UnitType> newUnitTypes = specification.getUnitTypesWithAbility("model.ability.foundInLostCity");
            Unit newUnit = new Unit(game, tile, serverPlayer,
                                    newUnitTypes.get(getPseudoRandom().nextInt(newUnitTypes.size())),
                                    UnitState.ACTIVE);
            result.add(new ModelMessage(serverPlayer,
                                        ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        newUnit,
                                        "lostCityRumour.Colonist"));
            break;
        case TREASURE:
            List<UnitType> treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");
            int treasureAmount = getPseudoRandom().nextInt(dx * 600) + dx * 300;
            String treasureString = String.valueOf(treasureAmount);
            UnitType unitType = treasureUnitTypes.get(getPseudoRandom().nextInt(treasureUnitTypes.size()));
            newUnit = new Unit(game, tile, serverPlayer, unitType, UnitState.ACTIVE);
            newUnit.setTreasureAmount(treasureAmount);
            result.add(new ModelMessage(serverPlayer,
                                        ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                        newUnit,
                                        "lostCityRumour.TreasureTrain",
                                        "%money%", treasureString));
            result.add(new HistoryEvent(game.getTurn().getNumber(),
                                        HistoryEvent.Type.CITY_OF_GOLD,
                                        "%treasure%", treasureString));
            break;
        case FOUNTAIN_OF_YOUTH:
            Europe europe = serverPlayer.getEurope();
            if (europe == null) {
                result.add(new ModelMessage(serverPlayer,
                                            ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                            unit,
                                            "lostCityRumour.FountainOfYouthWithoutEurope"));
            } else {
                if (serverPlayer.hasAbility("model.ability.selectRecruit")
                    && !serverPlayer.isAI() // TODO: let the AI select
                    ) {
                    // Remember, and ask player to select
                    serverPlayer.setRemainingEmigrants(dx);
                } else {
                    for (int k = 0; k < dx; k++) {
                        new Unit(game, europe, serverPlayer, serverPlayer.generateRecruitable(serverPlayer.getId() + "fountain." + Integer.toString(k)),
                                 UnitState.ACTIVE);
                    }
                    result.add(europe);
                }
                result.add(new ModelMessage(serverPlayer,
                                            ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                            unit,
                                            "lostCityRumour.FountainOfYouth"));
            }
            break;
        case NO_SUCH_RUMOUR:
        default:
            throw new IllegalStateException("No such rumour.");
        }
        tile.removeLostCityRumour();
        result.add(tile);
        return result;
    }

    /**
     * Find uncontacted players with units or settlements on
     * surrounding tiles.
     * Removed the restriction that the unit must not be naval which
     * avoids the special case where a scout, student, missionary, or
     * military unit can arrive by ship at an uncontacted settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is moving.
     * @param tile The <code>Tile</code> to check.
     * @return A list of <code>ServerPlayer</code>s newly contacted.
     */
    public List<ServerPlayer> findAdjacentUncontacted(ServerPlayer serverPlayer,
                                                      Tile tile) {
        List<ServerPlayer> players = new ArrayList<ServerPlayer>();
        for (Tile t : getGame().getMap().getSurroundingTiles(tile, 1)) {
            if (t == null || !t.isLand()) {
                continue; // Invalid tile for contact
            }

            ServerPlayer otherPlayer = null;
            if (t.getSettlement() != null) {
                otherPlayer = (ServerPlayer) t.getSettlement().getOwner();
            } else if (t.getFirstUnit() != null) {
                otherPlayer = (ServerPlayer) t.getFirstUnit().getOwner();
            }

            // Ignore ourself and previously contacted nations.
            if (otherPlayer != null && otherPlayer != serverPlayer
                && !serverPlayer.hasContacted(otherPlayer)) {
                players.add(otherPlayer);
            }
        }
        return players;
    }

    /**
     * Move a unit.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is moving.
     * @param unit The <code>Unit</code> to move.
     * @param newTile The <code>Tile</code> to move to.
     * @return A list of newly contacted <code>ServerPlayer</code>s as
     *         a result of this move.
     */
    public List<FreeColObject> move(ServerPlayer serverPlayer, Unit unit,
                                    Tile newTile) {
        unit.setState(UnitState.ACTIVE);
        unit.setStateToAllChildren(UnitState.SENTRY);
        unit.setMovesLeft(unit.getMovesLeft() - unit.getMoveCost(newTile));
        unit.setLocation(newTile);
        unit.activeAdjacentSentryUnits(newTile);

        // Clear the alreadyOnHighSea flag if we move onto a non-highsea tile.
        unit.setAlreadyOnHighSea(newTile.canMoveToEurope());

        // Explore a rumour if present.
        List<FreeColObject> objects
            = (newTile.hasLostCityRumour() && serverPlayer.isEuropean())
            ? exploreLostCityRumour(serverPlayer, unit)
            : new ArrayList<FreeColObject>();

        // If the unit died we do not get to see the formerly unseen
        // objects or make new contacts.  However we do not ignore the
        // rumour objects as the only way the unit can die is the
        // burial ground rumour, for which the rumour object is the
        // now aggravated Indian player which we do want to update.
        if (!unit.isDisposed()) {
            List<ServerPlayer> contacts = findAdjacentUncontacted(serverPlayer, newTile);
            for (ServerPlayer other : contacts) {
                serverPlayer.setContacted(other);
                other.setContacted(serverPlayer);
                objects.add(other);
            }

            // Also check for arriving next to an IndianSettlement
            // without alarm set, in which case it should be initialized.
            if (serverPlayer.isEuropean()) {
                for (Tile t : getGame().getMap().getSurroundingTiles(newTile, 1)) {
                    Settlement settlement = t.getSettlement();
                    if (settlement != null
                        && settlement instanceof IndianSettlement) {
                        IndianSettlement indians = (IndianSettlement) settlement;
                        if (indians.getAlarm(serverPlayer) == null) {
                            Player indianPlayer = indians.getOwner();
                            indians.setAlarm(serverPlayer,
                                             indianPlayer.getTension(serverPlayer));
                            objects.add(indians);
                        }
                    }
                }
            }
        }

        return objects;
    }

    /**
     * Demand a tribute from a native settlement.
     *
     * @param player The <code>Player</code> demanding the tribute.
     * @param settlement The <code>IndianSettlement</code> demanded of.
     * @return The amount of gold offered as tribute.
     * @todo Move TURNS_PER_TRIBUTE magic number to the spec.
     */
    public int demandTribute(Player player, IndianSettlement settlement) {
        final int TURNS_PER_TRIBUTE = 5;
        Player indianPlayer = settlement.getOwner();
        int gold = 0;
        int year = getGame().getTurn().getNumber();
        if (settlement.getLastTribute() + TURNS_PER_TRIBUTE < year) {
            switch (indianPlayer.getTension(player).getLevel()) {
            case HAPPY:
            case CONTENT:
                gold = Math.min(indianPlayer.getGold() / 10, 100);
                break;
            case DISPLEASED:
                gold = Math.min(indianPlayer.getGold() / 20, 100);
                break;
            case ANGRY:
            case HATEFUL:
            default:
                break; // do nothing
            }
        }

        // Increase tension whether we paid or not.
        // Apply tension directly to the settlement and let propagation work.
        settlement.modifyAlarm(player, Tension.TENSION_ADD_NORMAL);
        settlement.setLastTribute(year);
        indianPlayer.modifyGold(-gold);
        player.modifyGold(gold);
        return gold;
    }


    /**
     * Embark a unit onto a carrier.
     * Checking that the locations are appropriate is not done here.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose unit is embarking.
     * @param unit The <code>Unit</code> that is embarking.
     * @param carrier The <code>Unit</code> to embark onto.
     * @return True if the the embarkation succeeds.
     */
    public boolean embarkUnit(ServerPlayer serverPlayer, Unit unit, Unit carrier) {
        if (unit.isNaval() || carrier.getSpaceLeft() < unit.getSpaceTaken()) {
            return false;
        }

        Location sourceLocation = unit.getLocation();
        unit.setLocation(carrier);
        unit.setMovesLeft(0); // unit.getMovesLeft() -  3
        unit.setState(UnitState.SENTRY);

        // Update others
        if (sourceLocation instanceof Tile) {
            sendRemoveUnitToAll(unit, serverPlayer);
        }
        return true;
    }
}
