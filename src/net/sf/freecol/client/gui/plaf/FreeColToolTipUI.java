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

package net.sf.freecol.client.gui.plaf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

import net.sf.freecol.client.gui.ImageLibrary;


/**
 * Draw the "background.FreeColToolTip" resource as a tiled background
 * image on tool tip popups.
 */
public class FreeColToolTipUI extends BasicToolTipUI {

    private static FreeColToolTipUI sharedInstance = new FreeColToolTipUI();

    // TODO: find out why we can't use the FontRenderContext of the
    // component. And should we use fractional metrics?
    private static FontRenderContext frc = new FontRenderContext(null, true, false);

    private static int margin = 5;
    private static int maximumWidth = 300;


    private FreeColToolTipUI() {
        super();
    }

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    /**
     * Describe <code>setMaximumWidth</code> method here.
     *
     * @param width an <code>int</code> value
     */
    public static void setMaximumWidth(int width){
        maximumWidth = width;
    }

    public void paint(Graphics g, JComponent c) {
        Dimension size = c.getSize();
        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("background.FreeColToolTip", g, c, null);
        }

        // TODO: find out why this is necessary
        g.setColor(Color.BLACK);
        Graphics2D graphics = (Graphics2D) g;
        AttributedCharacterIterator styledText =
            new AttributedString(((JToolTip) c).getTipText()).getIterator();

        LineBreakMeasurer measurer = new LineBreakMeasurer(styledText, frc);

        float x = margin;
        float y = margin;
        while (measurer.getPosition() < styledText.getEndIndex()) {

            TextLayout layout = measurer.nextLayout(maximumWidth);

            y += (layout.getAscent());
            float dx = layout.isLeftToRight() ?
                0 : (maximumWidth - layout.getAdvance());

            layout.draw(graphics, x + dx, y);
            y += layout.getDescent() + layout.getLeading();
        }


     }

    public Dimension getPreferredSize(JComponent c) {
        String tipText = ((JToolTip)c).getTipText();
        if (tipText == null) {
            return new Dimension(0, 0);
        }

        AttributedCharacterIterator styledText =
            new AttributedString(((JToolTip) c).getTipText()).getIterator();
        LineBreakMeasurer measurer = new LineBreakMeasurer(styledText, frc);

        float x = 0f;
        float y = 0f;
        while (measurer.getPosition() < styledText.getEndIndex()) {

            TextLayout layout = measurer.nextLayout(maximumWidth);

            x = Math.max(x, layout.getVisibleAdvance());
            y += layout.getAscent() + layout.getDescent() + layout.getLeading();

        }
        return new Dimension((int) (x + 2 * margin),
                             (int) (y + 2 * margin));

    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
}
