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

package net.sf.freecol.server.model;

import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectListener;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.SimpleCombatModel;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerTurn;


/**
 * The server representation of the game.
 */
public class ServerGame extends Game implements ServerTurn {

    private static final Logger logger = Logger.getLogger(ServerGame.class.getName());

    /**
     * Creates a new game model.
     *
     * @param modelController A controller object the model can use to make
     *            actions not allowed from the model (generate random numbers
     *            etc).
     * @param specification The <code>Specification</code> to use in this game.
     * @see net.sf.freecol.server.FreeColServer#FreeColServer(boolean, boolean,
     *      int, String)
     */
    public ServerGame(ModelController modelController,
                      Specification specification) {
        super(specification);

        this.modelController = modelController;
        this.combatModel = new SimpleCombatModel();
        currentPlayer = null;
    }

    /**
     * Initiate a new <code>ServerGame</code> with information from a
     * saved game.
     *
     * @param freeColGameObjectListener A listener that should be monitoring
     *            this <code>Game</code>.
     * @param modelController A controller object the model can use to make
     *            actions not allowed from the model (generate random numbers
     *            etc).
     * @param in The input stream containing the XML.
     * @param fcgos A list of <code>FreeColGameObject</code>s to be added to
     *            this <code>Game</code>.
     * @param specification The <code>Specification</code> to use in this game.
     * @throws XMLStreamException if an error occurred during parsing.
     * @see net.sf.freecol.server.FreeColServer#loadGame
     */
    public ServerGame(FreeColGameObjectListener freeColGameObjectListener,
                      ModelController modelController, XMLStreamReader in,
                      FreeColGameObject[] fcgos, Specification specification)
        throws XMLStreamException {
        super(specification);

        setFreeColGameObjectListener(freeColGameObjectListener);
        this.modelController = modelController;
        if (modelController != null) {
            // no model controller when using the map editor
            this.combatModel = new SimpleCombatModel();
        }
        this.viewOwner = null;

        for (FreeColGameObject object : fcgos) {
            object.setGame(this);
            object.updateID();

            if (object instanceof Player) {
                players.add((Player) object);
            }
        }

        readFromXML(in);
    }


    /**
     * Get a unique ID to identify a <code>FreeColGameObject</code>.
     * 
     * @return A unique ID.
     */
    public String getNextID() {
        String id = Integer.toString(nextId);
        nextId++;
        return id;
    }


    /**
     * Is the next player in a new turn?
     */
    public boolean isNextPlayerInNewTurn() {
        Player nextPlayer = getNextPlayer();
        return players.indexOf(currentPlayer) > players.indexOf(nextPlayer)
            || currentPlayer == nextPlayer;
    }

    /**
     * New turn for this game.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        setTurn(getTurn().next());
        cs.addTrivial(See.all(), "newTurn", ChangePriority.CHANGE_NORMAL,
                      "turn", Integer.toString(getTurn().getNumber()));
        logger.info("ServerGame.csNewTurn, turn is " + getTurn().toString());

        for (Player player : getPlayers()) {
            player.newTurn();
            //((ServerPlayer) player).csNewTurn(random, cs);
        }

        if (getTurn().getAge() > 1 && !getSpanishSuccession()) {
            csSpanishSuccession(cs);
        }
    }

    /**
     * Checks for and if necessary performs the War of Spanish
     * Succession changes.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSpanishSuccession(ChangeSet cs) {
        Player weakestAIPlayer = null;
        Player strongestAIPlayer = null;
        int rebelPlayers = 0;
        for (Player player : getEuropeanPlayers()) {
            if (player.isREF()) continue;
            if (player.isAI()) {
                if (weakestAIPlayer == null
                    || weakestAIPlayer.getScore() > player.getScore()) {
                    weakestAIPlayer = player;
                }
                if (strongestAIPlayer == null
                    || strongestAIPlayer.getScore() < player.getScore()) {
                    strongestAIPlayer = player;
                }
            }
            if (player.getSoL() > 50) {
                rebelPlayers++;
            }
        }

        // Only eliminate the weakest AI if:
        // - there is at least one nation with >=50% rebels
        // - there is a distinct weakest nation
        // - it is not the sole nation with >=50% rebels
        if (rebelPlayers > 0
            && weakestAIPlayer != null && strongestAIPlayer != null
            && weakestAIPlayer != strongestAIPlayer
            && (weakestAIPlayer.getSoL() <= 50 || rebelPlayers > 1)) {
            for (Player player : getPlayers()) {
                for (IndianSettlement settlement
                         : player.getIndianSettlementsWithMission(weakestAIPlayer)) {
                    Unit missionary = settlement.getMissionary();
                    missionary.setOwner(strongestAIPlayer);
                    settlement.getTile().updatePlayerExploredTiles();
                    cs.add(See.perhaps().always((ServerPlayer)strongestAIPlayer),
                           settlement);
                }
            }
            for (Colony colony : weakestAIPlayer.getColonies()) {
                colony.changeOwner(strongestAIPlayer);
                for (Tile tile : colony.getOwnedTiles()) {
                    cs.add(See.perhaps(), tile);
                }
            }
            for (Unit unit : weakestAIPlayer.getUnits()) {
                unit.setOwner(strongestAIPlayer);
                cs.add(See.perhaps(), unit);
            }

            StringTemplate loser = weakestAIPlayer.getNationName();
            StringTemplate winner = strongestAIPlayer.getNationName();
            cs.addMessage(See.all(),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "model.diplomacy.spanishSuccession",
                                 strongestAIPlayer)
                    .addStringTemplate("%loserNation%", loser)
                    .addStringTemplate("%nation%", winner));
            for (Player p : getEuropeanPlayers()) {
                if (p != weakestAIPlayer) {
                    cs.addHistory((ServerPlayer) p,
                        new HistoryEvent(getTurn(),
                            HistoryEvent.EventType.SPANISH_SUCCESSION)
                            .addStringTemplate("%loserNation%", loser)
                            .addStringTemplate("%nation%", winner));
                }
            }
            weakestAIPlayer.setDead(true);
            cs.addDead((ServerPlayer) weakestAIPlayer);
            setSpanishSuccession(true);
            cs.addPartial(See.all(), this, "spanishSuccession");
        }
    }

}
