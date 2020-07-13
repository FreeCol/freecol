/**
 *  Copyright (C) 2002-2020   The FreeCol Team
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import net.sf.freecol.client.gui.ImageLibrary;


/**
 * ChatDisplay manages use of {@code GUIMessage}.
 */
public class ChatDisplay {

    /** The number of messages getting remembered. */
    private static final int MESSAGE_COUNT = 3;

    /** Left margin for the text. */
    private static final int LEFT_MARGIN = 40;

    /** Top margin for the text. */
    private static final int TOP_MARGIN = 300;

    /** The amount of time before a message gets deleted (in milliseconds). */
    private static final int MESSAGE_AGE = 30000;

    /** The messages to display. */
    private final List<GUIMessage> messages;


    /**
     * Create the chat display/ container.
     */
    public ChatDisplay() {
        this.messages = new ArrayList<>(MESSAGE_COUNT);
    }

    /**
     * Adds a message to the list of messages that need to be displayed
     * on the GUI.
     *
     * @param message The message to add.
     */
    public synchronized void addMessage(GUIMessage message) {
        if (this.messages.size() >= MESSAGE_COUNT) {
            this.messages.remove(0);
        }
        this.messages.add(message);
    }

    /**
     * Removes all the message that are older than MESSAGE_AGE.
     *
     * This can be useful to see if it is necessary to refresh the screen.
     *
     * @return True if all messages are gone.
     */
    private boolean removeOldMessages() {
        long currentTime = new Date().getTime();
        boolean result = false;

        int i = 0;
        while (i < this.messages.size()) {
            long creationTime = this.messages.get(i).getCreationTime();
            if ((currentTime - creationTime) >= MESSAGE_AGE) {
                this.messages.remove(i);
            } else {
                i++;
            }
        }
        return this.messages.isEmpty();
    }

    /**
     * Displays the list of messages.
     * 
     * @param g The Graphics2D the messages should be displayed on.
     * @param lib The imageLibrary to use.
     * @param size The size of the space for displaying in.
     */
    public synchronized void display(Graphics2D g, ImageLibrary lib,
                                     Dimension size) {
        // Return quickly if there are no messages, which is always
        // true in single player games.
        if (this.messages.isEmpty() || removeOldMessages()) return;
        
        final Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, lib.getScaleFactor());
        int yy = -1;
        final int xx = LEFT_MARGIN;
        for (GUIMessage m : this.messages) {
            Image si = lib.getStringImage(g, m.getMessage(),
                                          m.getColor(), font);
            if (yy < 0) {
                yy = size.height - TOP_MARGIN
                    - this.messages.size() * si.getHeight(null);
            }
            g.drawImage(si, xx, yy, null);
            yy += si.getHeight(null);
        }
        Image decoration = ImageLibrary
            .getUnscaledImage("image.menuborder.shadow.s");
        int width = decoration.getWidth(null);
        for (int index = 0; index < size.width; index += width) {
            g.drawImage(decoration, index, 0, null);
        }
        decoration = ImageLibrary
            .getUnscaledImage("image.menuborder.shadow.sw");
        g.drawImage(decoration, 0, 0, null);
        decoration = ImageLibrary
            .getUnscaledImage("image.menuborder.shadow.se");
        g.drawImage(decoration, size.width - decoration.getWidth(null),
            0, null);
    }
}
