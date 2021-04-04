/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.freecol.client.FreeColClient;
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

    /** The image library to use to draw with. */
    private final ImageLibrary lib;

    /** The font to write with. */
    private final Font font;
    
    /** The messages to display. */
    private final List<GUIMessage> messages;

    /** Cache the decoration images. */
    private final Image decorationS, decorationSW, decorationSE;


    /**
     * Create the chat display/ container.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     */
    public ChatDisplay(FreeColClient freeColClient) {
        this.lib = freeColClient.getGUI().getFixedImageLibrary();
        this.font = FontLibrary.getUnscaledFont("normal-plain-tiny");
        this.messages = new ArrayList<>(MESSAGE_COUNT);
        this.decorationS = ImageLibrary.getUnscaledImage("image.menuborder.shadow.s");
        this.decorationSW = ImageLibrary.getUnscaledImage("image.menuborder.shadow.sw");
        this.decorationSE = ImageLibrary.getUnscaledImage("image.menuborder.shadow.se");
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
     * Collect all messages to display, removing all older than MESSAGE_AGE.
     *
     * @return A copy of any messages found.
     */
    private synchronized List<GUIMessage> prepareMessages() {
        long currentTime = new Date().getTime();
        boolean result = false;
        List<GUIMessage> ret = new ArrayList<>();
        
        int i = 0;
        while (i < this.messages.size()) {
            GUIMessage m = this.messages.get(i);
            long creationTime = m.getCreationTime();
            if ((currentTime - creationTime) >= MESSAGE_AGE) {
                this.messages.remove(i);
            } else {
                ret.add(m);
                i++;
            }
        }
        return ret;
    }

    /**
     * Displays the list of messages.
     * 
     * @param g The Graphics2D the messages should be displayed on.
     * @param size The size of the space for displaying in.
     */
    public void display(Graphics2D g, Dimension size) {
        // Return quickly if there are no messages, which is always
        // true in single player games.
        List<GUIMessage> msgs = prepareMessages();
        if (msgs.isEmpty()) return;
        
        int yy = -1;
        final int xx = LEFT_MARGIN;
        for (GUIMessage m : msgs) {
            Image si = this.lib.getStringImage(g, m.getMessage(),
                                               m.getColor(), this.font);
            if (yy < 0) {
                yy = size.height - TOP_MARGIN - msgs.size() * si.getHeight(null);
            }
            g.drawImage(si, xx, yy, null);
            yy += si.getHeight(null);
        }
        int width = this.decorationS.getWidth(null);
        for (int index = 0; index < size.width; index += width) {
            g.drawImage(this.decorationS, index, 0, null);
        }
        g.drawImage(this.decorationSW, 0, 0, null);
        g.drawImage(this.decorationSE,
                    size.width - this.decorationSE.getWidth(null), 0, null);
    }
}
