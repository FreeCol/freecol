/*
 *        ServerPlayer.java - A Player as one is known by the server.
 *
 *  Copyright (C) 2003  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.server.model;

import java.net.Socket;

import java.util.Iterator;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.model.Map;

import net.sf.freecol.common.networking.Connection;


/**
* A <code>Player</code> with additional (server specific) information.
*
* <br><br>
*
* Contains an array of the player's explored tiles. This is
* neccessary because there is only one <code>Map</code> for
* each <code>Game</code>. At the client-side 
* <code>tile.isExplored()</code> may be used instead.
*
* <br><br>
*
* In addition, there are pointers to this player's
* {@link Connection} and {@link Socket}
*/
public class ServerPlayer extends Player {

    /** The network socket to the player's client. */
    private Socket socket;

    /** The connection for this player. */
    private Connection connection;

    /** Stores information about which tiles this player has explored. */
    private boolean[][] exploredTiles;




    /**
    * Creates a new <code>ServerPlayer</code>.
    *
    * @param name The player name.
    * @param admin Whether the player is the game administrator or not.
    * @param socket The socket to the player's client.
    * @param connection The <code>Connection</code> for the above mentioned socket.
    */
    public ServerPlayer(Game game, String name, boolean admin, Socket socket, Connection connection) {
        super(game, name, admin);

        this.socket = socket;
        this.connection = connection;
        //ready = false;

        resetExploredTiles(getGame().getMap());
    }




    /**
    * Resets this player's explored tiles. This is done by setting
    * all the tiles within a {@link Unit}s line of sight visible.
    * The other tiles are made unvisible.
    *
    * @param map The <code>Map</code> to reset the explored tiles on.
    * @see #hasExplored
    */
    public void resetExploredTiles(Map map) {
        if (map != null) {
            exploredTiles = new boolean[map.getWidth()][map.getHeight()];

            Iterator unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = (Unit) unitIterator.next();

                Map.Position position = unit.getTile().getPosition();
                exploredTiles[position.getX()][position.getY()] = true;

                Iterator positionIterator = map.getCircleIterator(position, true, unit.getLineOfSight());
                while (positionIterator.hasNext()) {
                    Map.Position p = (Map.Position) positionIterator.next();
                    exploredTiles[p.getX()][p.getY()] = true;
                }
            }

        }

    }


    /**
    * (DEBUG ONLY) Makes the entire map visible.
    */
    public void revealMap() {
        for (int i=0; i<exploredTiles.length; i++) {
            for (int j=0; j<exploredTiles[0].length; j++) {
                exploredTiles[i][j] = true;
            }
        }
    }


    /**
    * Checks if this player has explored the given <code>Tile</code>.
    *
    * @param  The <code>Tile</code> to check the visibillity of.
    * @return <i>true</i> if the <code>Tile</code> has been explored and
    *         <i>false</i> otherwise.
    * @see #setExplored(Tile)
    * @see #setExplored(Unit)
    */
    public boolean hasExplored(Tile tile) {
        return exploredTiles[tile.getX()][tile.getY()];
    }


    /**
    * Sets the given <code>Tile</code> to be explored by this player.
    *
    * @param tile The <code>Tile</code> to set as explored.
    * @see #hasExplored
    * @see #setExplored(Unit)
    */
    public void setExplored(Tile tile) {
        exploredTiles[tile.getX()][tile.getY()] = true;
    }


    /**
    * Sets the tiles within the given <code>Unit</code>'s line of
    * sight to be explored by this player.
    *
    * @param unit The <code>Unit</code>.
    * @see #setExplored(Tile)
    * @see #hasExplored
    */
    public void setExplored(Unit unit) {
        if (unit.getTile() == null) {
            return;
        }

        Iterator positionIterator = getGame().getMap().getCircleIterator(unit.getTile().getPosition(), true, unit.getLineOfSight());
        while (positionIterator.hasNext()) {
            Map.Position p = (Map.Position) positionIterator.next();
            exploredTiles[p.getX()][p.getY()] = true;
        }
    }


    /**
     * Gets the socket of this player.
     * @return The <code>Socket</code>.
     */
    public Socket getSocket() {
        return socket;
    }


    /**
     * Gets the connection of this player.
     * @return The <code>Connection</code>.
     */
    public Connection getConnection() {
        return connection;
    }
}
