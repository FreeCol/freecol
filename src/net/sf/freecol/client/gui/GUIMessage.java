
package net.sf.freecol.client.gui;

import java.awt.Color;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Represents a message that can be displayed in the GUI. It has message data
 * and a Color.
 */
public final class GUIMessage {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
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
