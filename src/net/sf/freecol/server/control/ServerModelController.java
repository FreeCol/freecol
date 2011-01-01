/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

/**
 * A server-side implementation of the <code>ModelController</code> interface.
 */
public class ServerModelController implements ModelController {

    private static final Logger logger = Logger.getLogger(ServerModelController.class.getName());

    private final FreeColServer freeColServer;

    private final HashMap<String, TaskEntry> taskRegister = new HashMap<String, TaskEntry>();


    /**
     * Creates a new <code>ServerModelController</code>.
     * 
     * @param freeColServer The main controller.
     */
    public ServerModelController(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }

    /**
     * Removes any entries older than {@link TaskEntry#TASK_ENTRY_TIME_OUT}.
     */
    public synchronized void clearTaskRegister() {
        int currentTurn = freeColServer.getGame().getTurn().getNumber();
        List<String> idsToRemove = new ArrayList<String>();
        for (TaskEntry te : taskRegister.values()) {
            if (te.hasExpired(currentTurn)) {
                if (!te.isSecure()) {
                    logger.warning("Possibly a cheating attempt.");
                }
                idsToRemove.add(te.taskID);
            }
        }
        if (!idsToRemove.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Clearing the task register. Removing the following items:");
            for (String id : idsToRemove) {
                taskRegister.remove(id);
                sb.append(" ");
                sb.append(id);
            }
            logger.info(sb.toString());
        }
    }

    /**
     * A single entry in the task register.
     */
    private static class TaskEntry {
        final String taskID;

        final int createdTurn;

        final Object entry;

        private boolean secure;


        TaskEntry(String taskID, int createdTurn, boolean secure, Object entry) {
            this.taskID = taskID;
            this.createdTurn = createdTurn;
            this.secure = secure;
            this.entry = entry;
        }

        synchronized boolean isSecure() {
            return this.secure;
        }

        boolean hasExpired(int currentTurn) {
            return createdTurn + TASK_ENTRY_TIME_OUT < currentTurn;
        }


        /** The number of turns before a <code>TaskEntry</code> has expired. */
        private static final int TASK_ENTRY_TIME_OUT = 5;
    }

}
