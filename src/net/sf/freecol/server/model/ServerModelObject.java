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

package net.sf.freecol.server.model;

import java.util.Random;

import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.control.ChangeSet;


/**
 * Interface for server-side objects which needs to store
 * extra information to a save game.
 */
public interface ServerModelObject {

    /*
      All ServerModelObjects must also implement a trivial constructor
      (ServerGame does not but it is special, being the Game itself)
      of the form:

      public <constructor>(Game game, String id) {
          super(game, id);
      }
    */

    /**
     * Get the object identifier.
     *
     * @return The object identifier.
     */
    public String getId();

    /**
     * Gets the tag to use when saving this server object.
     *
     * @return The server object tag.
     */
    public String getServerXMLElementTagName();

    /**
     * Executes new-turn actions for this server object.
     *
     * @param random A pseudo-random number source.
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs);
} 
