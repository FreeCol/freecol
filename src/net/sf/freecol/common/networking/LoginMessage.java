/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when logging in.
 */
public class LoginMessage extends DOMMessage {

    /** The Player that is logging in. */
    private Player player;

    /** The user name. */
    private String userName;

    /** The client FreeCol version. */
    private String version;

    /** Is the player an admin. */
    private boolean admin;

    /** Whether to start the game. */
    private boolean startGame;

    /** Is this a single player game. */
    private boolean singlePlayer;

    /** Is the client the current player. */
    private boolean currentPlayer;

    /** The optional id of the active unit. */
    private String activeUnitId;

    /** The game. */
    private Game game;

        
    /**
     * Create a new <code>LoginMessage</code> with the supplied name
     * and version.
     *
     * @param player The <code>Player</code> that is logging in.
     * @param userName The name of the user logging in.
     * @param version The version of FreeCol at the client.
     * @param startGame Whether to start the game.
     * @param singlePlayer True in single player games.
     * @param currentPlayer True if this player is the current player.
     * @param activeUnit The current active <code>Unit</code>, or null.
     * @param game The entire game.
     */
    public LoginMessage(Player player, String userName, String version,
                        boolean startGame, boolean singlePlayer,
                        boolean currentPlayer, Unit activeUnit,
                        Game game) {
        this.player = player;
        this.userName = userName;
        this.version = version;
        this.admin = player.isAdmin();
        this.startGame = startGame;
        this.singlePlayer = singlePlayer;
        this.currentPlayer = currentPlayer;
        this.activeUnitId = (activeUnit == null) ? null : activeUnit.getId();
        this.game = game;
    }

    /**
     * Create a new <code>LoginMessage</code> from a supplied element.
     *
     * @param game A <code>Game</code> (not used).
     * @param element The <code>Element</code> to use to create the message.
     */
    public LoginMessage(Game game, Element element) {
        String str;
        this.player = null; // Should not be used on client side
        this.userName = element.getAttribute("userName");
        this.version = element.getAttribute("version");
        str = element.getAttribute("admin");
        this.admin = Boolean.valueOf(str).booleanValue();
        str = element.getAttribute("startGame");
        this.startGame = Boolean.valueOf(str).booleanValue();
        str = element.getAttribute("singlePlayer");
        this.singlePlayer = Boolean.valueOf(str).booleanValue();
        str = element.getAttribute("currentPlayer");
        this.currentPlayer = Boolean.valueOf(str).booleanValue();
        this.activeUnitId = element.getAttribute("activeUnit");
        NodeList children = element.getChildNodes();
        this.game = (children.getLength() != 1) ? null
            : new Game((Element)children.item(0), userName);
    }

    // Simple public accessors.

    public String getUserName() {
        return userName;
    }

    public String getVersion() {
        return version;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean getStartGame() {
        return startGame;
    }

    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    public boolean isCurrentPlayer() {
        return currentPlayer;
    }

    public Unit getActiveUnit() {
        if (activeUnitId == null) return null;
        FreeColGameObject fcgo = game.getFreeColGameObjectSafely(activeUnitId);
        return (fcgo instanceof Unit) ? (Unit)fcgo : null;
    }

    public Game getGame() {
        return game;
    }

    /**
     * Handle a "login"-message.
     * This is actually done in PreGameController.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        return null;
    }

    /**
     * Convert this LoginMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        Document doc = result.getOwnerDocument();
        result.setAttribute("userName", userName);
        result.setAttribute("version", version);
        result.setAttribute("admin", Boolean.toString(admin));
        result.setAttribute("startGame", Boolean.toString(startGame));
        result.setAttribute("singlePlayer", Boolean.toString(singlePlayer));
        result.setAttribute("currentPlayer", Boolean.toString(currentPlayer));
        result.setAttribute("activeUnit", activeUnitId);
        result.appendChild(game.toXMLElement(player, doc, false, false));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "login".
     */
    public static String getXMLElementTagName() {
        return "login";
    }
}
