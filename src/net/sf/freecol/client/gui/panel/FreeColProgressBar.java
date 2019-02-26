/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;


/**
 * Implements a simple progress bar suitable for use with
 * FreeCol. Unlike JProgressBar, it also displays the expected
 * increase next turn as well as the estimated time until completion.
 * Used in the colony panel for the building progress.
 */
public class FreeColProgressBar extends JPanel {

    /** The minimum value of the progress bar. */
    private int min = 0;

    /** The maximum value of the progress bar. */
    private int max = 100;

    /** The current value of the progress bar. */
    private int value = 0;

    /** The expected increase next turn. */
    private int step = 0;

    /**
     * The type of goods this progress bar is for.  The default value
     * of null indicates no goods type.
     */
    private GoodsType goodsType = null;


    /**
     * Creates a new {@code FreeColProgressBar} instance.
     *
     * @param goodsType the type of goods produced
     */
    public FreeColProgressBar(GoodsType goodsType) {
        this(goodsType, 0, 100, 0, 0);
    }

    /**
     * Creates a new {@code FreeColProgressBar} instance.
     *
     * @param goodsType the type of goods produced
     * @param min the minimum value of the progress bar
     * @param max the maximum value of the progress bar
     */
    public FreeColProgressBar(GoodsType goodsType, int min, int max) {
        this(goodsType, min, max, 0, 0);
    }

    /**
     * Creates a new {@code FreeColProgressBar} instance.
     *
     * @param goodsType the type of goods produced
     * @param min the minimum value of the progress bar
     * @param max the maximum value of the progress bar
     * @param value the current value of the progress bar
     * @param step the expected increase next turn
     */
    public FreeColProgressBar(GoodsType goodsType, int min, int max,
                              int value, int step) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.step = step;
        this.goodsType = goodsType;

        setBorder(Utility.PROGRESS_BORDER);
        setPreferredSize(new Dimension(200, 20));
    }


    /**
     * Update the data of the progress bar.
     *
     * @param value the current value of the progress bar
     * @param step the expected increase next turn
     */
    public void update(int value, int step) {
        update(min, max, value, step);
    }

    /**
     * Update the data of the progress bar.
     *
     * @param min the minimum value of the progress bar
     * @param max the maximum value of the progress bar
     * @param value the current value of the progress bar
     * @param step the expected increase next turn
     */
    private void update(int min, int max, int value, int step) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.step = step;
        repaint();
    }

    /**
     * Get an image to use for the goods type, if any.
     *
     * @return A suitable {@code Image}, or null if there is no
     *      goods type.
     */
    private Image getImage() {
        return (this.goodsType == null) ? null
            : ImageLibrary.getGoodsTypeImage(this.goodsType,
                new Dimension(-1, ImageLibrary.ICON_SIZE.height / 2));
    }


    // Override JComponent


    /**
     * Render the FreeColProgressBar
     *
     * @param g The instance of the Graphics Library FreeCol is using
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setFont(FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
                                           FontLibrary.FontSize.TINY));
        int width = getWidth() - getInsets().left - getInsets().right;
        int height = getHeight() - getInsets().top - getInsets().bottom;

        if (isOpaque()) {
            ImageLibrary.drawTiledImage("image.background.FreeColProgressBar",
                                        g, this, getInsets());
        }

        int dvalue = 0;
        if (value >= max) {
            dvalue = width;
        } else if (max > 0) {
            dvalue = (width * value) / max;
        }
        if (dvalue > 0) {
            if (dvalue > width) {
                dvalue = width;
            }
            g2d.setColor(new Color(0, 0, 0, 70));
            g2d.fillRect(getInsets().left, getInsets().top, dvalue, height);
        }
        int dstep = 0;
        if (max > 0) {
            dstep = (width * step) / max;
            if (dstep > 0) {
                if ((dstep + dvalue) > width) {
                    dstep = width - dvalue;
                }
                g2d.setColor(new Color(0, 0, 0, 40));
                g2d.fillRect(getInsets().left + dvalue, getInsets().top,
                             dstep, height);
            }
        }

        String stepSignal = (step < 0) ? "-" : "+";
        StringBuilder progress = new StringBuilder(32);
        progress.append(String.valueOf(value)).append(stepSignal).append(Math.abs(step)).append('/').append(max);
        String turnsString;
        if (max <= value) { // Already complete
            turnsString = "0";
        } else if (step > 0) { // There is progress, how many turns to go?
            int turns = (max - value) / step;
            if (((max - value) % step) > 0) {
                turns++;
            }
            turnsString = Integer.toString(turns);
        } else { // No progress
            turnsString = Messages.message("notApplicable");
        }
        progress.append(' ')
                .append(Messages.message(StringTemplate.template("freeColProgressBar.turnsToComplete")
                                                       .addName("%number%", turnsString))
                       );

        int stringWidth = g2d.getFontMetrics().stringWidth(progress.toString());
        int stringHeight = g2d.getFontMetrics().getAscent()
            + g2d.getFontMetrics().getDescent();
        int restWidth = getWidth() - stringWidth;

        Image image = null;
        int iconWidth = 0;
        if (goodsType != null && (image = getImage()) != null) {
            iconWidth = image.getWidth(this);
            g2d.drawImage(image, restWidth / 2,
                (getHeight() - ImageLibrary.ICON_SIZE.height / 2) / 2,
                null);
        }

        g2d.setColor(Color.BLACK);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawString(progress.toString(),
                       (restWidth - iconWidth) / 2 + (iconWidth + 8),
                       getHeight() / 2 + stringHeight / 4);
        g2d.dispose();
    }
}
