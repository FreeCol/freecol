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

package net.sf.freecol.client.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.ImageIcon;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.InfoPanel;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Player;


/**
 * Custom component to paint turn progress.
 * <p>
 * Currently the component darken out background using alpha channel and
 * then paints the player's icon and wait message.
 */
public class GrayLayer extends Component {

    /** Color for graying out background component */
    private static final Color MASK_COLOR = new Color(0f, 0f, 0f, .6f);
    /** Default font size for message text */
    private static final int DEFAULT_FONT_SIZE = 18;
    /** Font size decrement for message text to reduce length */
    private static final int FONT_SIZE_DECREMENT = 2;
    /**
     * Maximum text width to show.  This is additional constraint to
     * the component's bounds
     */
    private static final int MAX_TEXT_WIDTH = 640;

    /** The client for this FreeCol game */
    private final FreeColClient freeColClient;
    /** Player object or <code>null</code> */
    private Player player;


    /**
     * Create a gray layer.
     *
     * @param freeColClient The client for the game.
     */
    public GrayLayer(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }


    /**
     * Executes painting.  The method shadows the background image, and
     * paints the message with icon (if available) and text.
     *
     * @param g The <code>Graphics</code> to paint on.
     */
    @Override
    public void paint(Graphics g) {
        Rectangle clipArea = g.getClipBounds();
        if (clipArea == null) {
            clipArea = getBounds();
            clipArea.x = clipArea.y = 0;
        }
        if (clipArea.isEmpty()) {
            // we are done - the picture is OK
            return;
        }
        
        if (!freeColClient.getClientOptions().getBoolean(ClientOptions.DISABLE_GRAY_LAYER)) {
            g.setColor(MASK_COLOR);
            g.fillRect(clipArea.x, clipArea.y, clipArea.width, clipArea.height);
        }

        ImageIcon coatOfArmsIcon = null;
        String message = null;
        Color colour = null;

        if (player == null) {
            message = Messages.message(freeColClient.getGame().getTurn()
                .getLabel());
            colour = Color.WHITE;

        } else {
            coatOfArmsIcon = new ImageIcon(
                ImageLibrary.getMiscIconImage(player.getNation(), 1f));
            message = Messages.message(player.getWaitingLabel());
            colour = player.getNationColor();
        }

        Font nameFont = getFont();
        FontMetrics nameFontMetrics = getFontMetrics(nameFont);
        Rectangle textBounds;
        int fontSize = DEFAULT_FONT_SIZE;
        int maxWidth = Math.min(MAX_TEXT_WIDTH, getSize().width);
        do {
            nameFont = nameFont.deriveFont(Font.BOLD, fontSize);
            nameFontMetrics = getFontMetrics(nameFont);
            textBounds = nameFontMetrics.getStringBounds(message, g)
                    .getBounds();
            fontSize -= FONT_SIZE_DECREMENT;
        } while (textBounds.width > maxWidth);

        Dimension size = getSize();
        textBounds.x = (size.width - textBounds.width) / 2;
        textBounds.y = size.height - InfoPanel.PANEL_HEIGHT
            - 2 * textBounds.height;

        if (textBounds.intersects(clipArea)) {
            // show message
            g.setFont(nameFont);
            g.setColor(colour);
            g.drawString(message, textBounds.x, textBounds.y
                    + textBounds.height);
        }
        if (coatOfArmsIcon != null) {
            Rectangle iconBounds = new Rectangle();
            iconBounds.width = coatOfArmsIcon.getIconWidth();
            iconBounds.height = coatOfArmsIcon.getIconHeight();
            iconBounds.x = (size.width - iconBounds.width) / 2;
            iconBounds.y = textBounds.y + 3 * textBounds.height / 2;
            if (iconBounds.intersects(clipArea)) {
                // show icon
                coatOfArmsIcon.paintIcon(this, g, iconBounds.x, iconBounds.y);
            }
        }
    }

    /**
     * Set the player for which we paint.  If the player is already
     * set, then nothing happens, otherwise a repaint event is sent.
     *
     * @param player The <code>Player</code> for status information.
     */
    public void setPlayer(Player player) {
        if (this.player == player) return;
        this.player = player;
        repaint();
    }
}
