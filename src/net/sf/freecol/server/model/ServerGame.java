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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectListener;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.SimpleCombatModel;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;

/**
 * The main component of the game model.
 * 
 * <br>
 * <br>
 * 
 * The server representation of the game
 */
public class ServerGame extends Game {

    private static final Logger logger = Logger.getLogger(ServerGame.class.getName());

    /**
     * Creates a new game model.
     * 
     * @param modelController A controller object the model can use to make
     *            actions not allowed from the model (generate random numbers
     *            etc).
     * @see net.sf.freecol.server.FreeColServer#FreeColServer(boolean, boolean,
     *      int, String)
     */
    public ServerGame(ModelController modelController) {
        super(null);

        this.modelController = modelController;
        this.combatModel = new SimpleCombatModel();
        //this.viewOwner = null;

        gameOptions = new GameOptions();

        currentPlayer = null;
        //market = new Market(this);
    }

    /**
     * Initiate a new <code>ServerGame</code> with information from a saved game.
     * 
     * @param freeColGameObjectListener A listener that should be monitoring
     *            this <code>Game</code>.
     * @param modelController A controller object the model can use to make
     *            actions not allowed from the model (generate random numbers
     *            etc).
     * @param in The input stream containing the XML.
     * @param fcgos A list of <code>FreeColGameObject</code>s to be added to
     *            this <code>Game</code>.
     * @throws XMLStreamException if an error occurred during parsing.
     * @see net.sf.freecol.server.FreeColServer#loadGame
     */
    public ServerGame(FreeColGameObjectListener freeColGameObjectListener, ModelController modelController,
                XMLStreamReader in, FreeColGameObject[] fcgos) throws XMLStreamException {
        super(null, in);

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

    /*
     * Initiate a new <code>Game</code> with information from a saved game.
     * 
     * Currently not used, commented.
     * 
     * @param freeColGameObjectListener A listener that should be monitoring
     * this <code>Game</code>. @param modelController A controller object the
     * model can use to make actions not allowed from the model (generate random
     * numbers etc). @param fcgos A list of <code>FreeColGameObject</code>s
     * to be added to this <code>Game</code>. @param e An XML-element that
     * will be used to initialize this object.
     * 
     * public Game(FreeColGameObjectListener freeColGameObjectListener,
     * ModelController modelController, Element e, FreeColGameObject[] fcgos){
     * super(null, e);
     * 
     * setFreeColGameObjectListener(freeColGameObjectListener);
     * this.modelController = modelController; this.viewOwner = null;
     * 
     * canGiveID = true;
     * 
     * for (int i=0; i<fcgos.length; i++) { fcgos[i].setGame(this);
     * fcgos[i].updateID();
     * 
     * if (fcgos[i] instanceof Player) { players.add((Player) fcgos[i]); } }
     * 
     * readFromXMLElement(e); }
     */

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
     * Prepares this <code>Game</code> for a new turn.
     * 
     * Invokes <code>newTurn()</code> for every registered
     * <code>FreeColGameObject</code>.
     * 
     * @see #setFreeColGameObject
     */
    public void newTurn() {
        setTurn(getTurn().next());
        logger.info("Turn is now " + getTurn().toString());

        for (Player player : players) {
            logger.info("Calling newTurn for player " + player.getName());
            player.newTurn();
        }
    }
}
