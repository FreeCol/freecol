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


package net.sf.freecol.client.gui;

import java.awt.Color;
import java.util.Date;
import java.util.logging.Logger;


/**
 * Represents a message that can be displayed in the GUI. It has message data
 * and a Color.
 */
public final class GUIMessage {
    
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GUIMessage.class.getName());
    
    private final String    message;
    private final Color     color;
    private final Date      creationTime;
    
    /**
    * The constructor to use.
    * @param message The actual message.
    * @param color The color in which to display this message.
    */
    public GUIMessage(String message, Color color) {
        this.message = message;
        this.color = color;
        this.creationTime = new Date();
    }
    
    /**
    * Returns the actual message data.
    * @return The actual message data.
    */
    public String getMessage() {
        return message;
    }
    
    /**
    * Returns the message's Color.
    * @return The message's Color.
    */
    public Color getColor() {
        return color;
    }
    
    /**
    * Returns the time at which this message was created.
    * @return The time at which this message was created.
    */
    public Date getCreationTime() {
        return creationTime;
    }
}
