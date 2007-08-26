package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;

/**
 * Implements a simple progress bar suitable for use with FreeCol. Unlike
 * JProgressBar, it also displays the expected increase next turn as well as the
 * estimated time until completion.
 */
public class FreeColProgressBar extends JPanel {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final Color PRIMARY_1 = new Color(122, 109, 82), BG_COLOR_SELECT = new Color(255, 244, 195),
            PRIMARY_3 = new Color(203, 182, 136)
            // ,SECONDARY_1 = new Color(10, 10, 10),
            // DISABLED_COLOR = new Color(166, 144, 95),
            // BG_COLOR = new Color(216, 194, 145)
            ;

    // The minimum value of the progress bar
    private int min = 0;

    // The minimum value of the progress bar
    private int max = 100;

    // The current value of the progress bar
    private int value = 0;

    // The expected increase next turn
    private int step = 0;

    private int iconWidth;

    private int iconHeight = 16;

    /**
     * The type of goods this progress bar is for. The default value of null
     * indicates no goods type.
     */
    private GoodsType goodsType = null;

    @SuppressWarnings("unused")
    private final Canvas parent;

    private Image image;


    /**
     * Creates a new <code>FreeColProgressBar</code> instance.
     * 
     * @param parent a <code>Canvas</code> value
     * @param goodsType the type of goods produced
     */
    public FreeColProgressBar(Canvas parent, GoodsType goodsType) {
        this(parent, goodsType, 0, 100, 0, 0);
    }

    /**
     * Creates a new <code>FreeColProgressBar</code> instance.
     * 
     * @param parent a <code>Canvas</code> value
     * @param goodsType the type of goods produced
     * @param min the minimum value of the progress bar
     * @param max the maximum value of the progress bar
     */
    public FreeColProgressBar(Canvas parent, GoodsType goodsType, int min, int max) {
        this(parent, goodsType, min, max, 0, 0);
    }

    /**
     * Creates a new <code>FreeColProgressBar</code> instance.
     * 
     * @param parent a <code>Canvas</code> value
     * @param goodsType the type of goods produced
     * @param min the minimum value of the progress bar
     * @param max the maximum value of the progress bar
     * @param value the current value of the progress bar
     * @param step the expected increase next turn
     */
    public FreeColProgressBar(Canvas parent, GoodsType goodsType, int min, int max, int value, int step) {
        this.parent = parent;
        this.goodsType = goodsType;
        this.min = min;
        this.max = max;
        this.value = value;
        this.step = step;

        setBorder(BorderFactory.createLineBorder(PRIMARY_1));
        ImageIcon icon = parent.getGUI().getImageLibrary().getGoodsImageIcon(goodsType.getIndex());
        // scale to a height of 16px, preserving aspect ratio
        image = icon.getImage().getScaledInstance(-1, iconHeight, Image.SCALE_SMOOTH);
        iconWidth = image.getWidth(this);
        setPreferredSize(new Dimension(200, 20));
    }

    /**
     * Upate the data of the progress bar.
     * 
     * @param value the current value of the progress bar
     * @param step the expected increase next turn
     */
    public void update(int value, int step) {
        update(min, max, value, step);
    }

    /**
     * Upate the data of the progress bar.
     * 
     * @param min the minimum value of the progress bar
     * @param max the maximum value of the progress bar
     * @param value the current value of the progress bar
     * @param step the expected increase next turn
     */
    public void update(int min, int max, int value, int step) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.step = step;
        repaint();
    }

    protected void paintComponent(Graphics g) {

        Graphics2D g2d = (Graphics2D) g.create();
        int width = getWidth() - getInsets().left - getInsets().right;
        int height = getHeight() - getInsets().top - getInsets().bottom;

        if (iconWidth < 0) {
            iconWidth = image.getWidth(this);
        }

        if (isOpaque()) {
            Image tempImage = (Image) UIManager.get("BackgroundImage");

            if (tempImage != null) {
                for (int x = getInsets().left; x < width + getInsets().left; x += tempImage.getWidth(null)) {
                    for (int y = getInsets().top; y < height + getInsets().top; y += tempImage.getHeight(null)) {
                        g2d.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g2d.setColor(getBackground());
                g2d.fillRect(getInsets().left, getInsets().top, width, height);
            }
        }

        int dvalue = 0;
        if (value >= max) {
            dvalue = width;
        } else if (max > 0) {
            dvalue = width * value / max;
        }
        if (dvalue > 0) {
            if (dvalue > width) {
                dvalue = width;
            }
            //g2d.setColor(PRIMARY_3);
            g2d.setColor(new Color(0, 0, 0, 70));
            g2d.fillRect(getInsets().left, getInsets().top, dvalue, height);
        }

        int dstep = 0;
        if (max > 0) {
            dstep = width * step / max;
            if (dstep > 0) {
                if (dstep + dvalue > width) {
                    dstep = width - dvalue;
                }
                //g2d.setColor(BG_COLOR_SELECT);
                g2d.setColor(new Color(0, 0, 0, 40));
                g2d.fillRect(getInsets().left + dvalue, getInsets().top, dstep, height);
            }
        }

        String progressString = String.valueOf(value) + "+" + step + "/" + max;
        if (step > 0 && max > value) {
            int turns = (max - value) / step;
            if ((max - value) % step > 0) {
                turns++;
            }
            progressString += " (" + turns + " " + Messages.message("turns") + ")";
        }

        int stringWidth = g2d.getFontMetrics().stringWidth(progressString);
        int stringHeight = g2d.getFontMetrics().getAscent() + g2d.getFontMetrics().getDescent();
        int restWidth = getWidth() - stringWidth;

        if (goodsType != null) {
            restWidth -= iconWidth;
            g2d.drawImage(image, restWidth / 2, (getHeight() - iconHeight) / 2, null);
        }

        //g2d.setColor(PRIMARY_1);
        g2d.setColor(Color.BLACK);
        g2d.drawString(progressString, restWidth / 2 + iconWidth, getHeight() / 2 + stringHeight / 4);

        g2d.dispose();
    }

}
