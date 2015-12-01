/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when logging in.
 */
public class LoginMessage extends DOMMessage {

    /** The Player that is logging in. */
    private final Player player;

    /** The user name. */
    private final String userName;

    /** The client FreeCol version. */
    private final String version;

    /** Is the player an admin. */
    private final boolean admin;

    /** Whether to start the game. */
    private final boolean startGame;

    /** Is this a single player game. */
    private final boolean singlePlayer;

    /** Is the client the current player. */
    private final boolean currentPlayer;

    /** The game. */
    private final Game game;

        
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
     * @param game The entire game.
     */
    public LoginMessage(Player player, String userName, String version,
                        boolean startGame, boolean singlePlayer,
                        boolean currentPlayer, Game game) {
        super(getXMLElementTagName());

        this.player = player;
        this.userName = userName;
        this.version = version;
        this.admin = player.isAdmin();
        this.startGame = startGame;
        this.singlePlayer = singlePlayer;
        this.currentPlayer = currentPlayer;
        this.game = game;
    }

    /**
     * Create a new <code>LoginMessage</code> from a supplied element.
     *
     * @param game A <code>Game</code> (not used).
     * @param element The <code>Element</code> to use to create the message.
     */
    public LoginMessage(Game game, Element element) {
        super(getXMLElementTagName());

        String str;
        this.player = null; // Should not be used on client side
        this.userName = element.getAttribute("userName");
        this.version = element.getAttribute("version");
        str = element.getAttribute("admin");
        this.admin = Boolean.parseBoolean(str);
        str = element.getAttribute("startGame");
        this.startGame = Boolean.parseBoolean(str);
        str = element.getAttribute("singlePlayer");
        this.singlePlayer = Boolean.parseBoolean(str);
        str = element.getAttribute("currentPlayer");
        this.currentPlayer = Boolean.parseBoolean(str);
        NodeList children = element.getChildNodes();
        this.game = (children.getLength() != 1) ? null
            : new Game((Element)children.item(0), this.userName);
    }


    // Public interface

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
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "userName", userName,
            "version", version,
            "admin", Boolean.toString(admin),
            "startGame", Boolean.toString(startGame),
            "singlePlayer", Boolean.toString(singlePlayer),
            "currentPlayer", Boolean.toString(currentPlayer));
        result.appendChild(game.toXMLElement(result.getOwnerDocument(), 
                                             player));
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
