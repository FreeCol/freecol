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
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;

public class FreeColProgressBar extends JPanel {

    private static final Color PRIMARY_1 = new Color(122, 109, 82),
                               BG_COLOR_SELECT = new Color(255, 244, 195),
                               PRIMARY_3 = new Color(203, 182, 136),
                               SECONDARY_1 = new Color(10, 10, 10),
                               DISABLED_COLOR = new Color(166, 144, 95),
                               BG_COLOR = new Color(216, 194, 145);

    private int min = 0;
    private int max = 100;
    private int value = 0;
    private int step = 0;
    private int iconWidth;
    private int iconHeight = 16;

    /**
     * The type of goods this progress bar is for. The default value
     * of -1 indicates no goods type.
     */
    private int goodsType = -1;

    private final Canvas parent;
    private Image image;

    public FreeColProgressBar(Canvas parent, int goodsType) {
        this(parent, goodsType, 0, 100, 0, 0);
    }

    public FreeColProgressBar(Canvas parent, int goodsType, int min, int max) {
        this(parent, goodsType, min, max, 0, 0);
    }

    public FreeColProgressBar(Canvas parent, int goodsType, int min, int max, int value, int step) {
        this.parent = parent;
        this.goodsType = goodsType;
        this.min = min;
        this.max = max;
        this.value = value;
        this.step = step;

        setBorder(BorderFactory.createLineBorder(PRIMARY_1));
        ImageIcon icon = parent.getGUI().getImageLibrary().getGoodsImageIcon(goodsType);
        // scale to a height of 16px, preserving aspect ratio
        image = icon.getImage().getScaledInstance(-1, iconHeight, Image.SCALE_SMOOTH);
        iconWidth = image.getWidth(this);
        setPreferredSize(new Dimension(200, 20));
    }


    public void update(int value, int step) {
        update(min, max, value, step);
    }

    public void update(int min, int max, int value, int step) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.step = step;
    }

    protected void paintComponent(Graphics g) {

        Graphics2D g2d = (Graphics2D) g.create();
        int width = getWidth();
        int height = getHeight();

        if (iconWidth < 0) {
            iconWidth = image.getWidth(this);
        }

        if (isOpaque()) {
            Image tempImage = (Image) UIManager.get("BackgroundImage");
   
            if (tempImage != null) {
                for (int x = 0; x < width; x += tempImage.getWidth(null)) {
                    for (int y = 0; y < height; y += tempImage.getHeight(null)) {
                        g2d.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, width, height);
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
            g2d.setColor(PRIMARY_3);
            g2d.fillRect(0, 0, dvalue, height);
        }

        int dstep = 0;
        if (max > 0) {
            dstep = width * step / max;
            if (dstep > 0) {
                if (dstep + dvalue > width) {
                    dstep = width - dvalue;
                }
                g2d.setColor(BG_COLOR_SELECT);
                g2d.fillRect(dvalue, 0, dstep, height);
            }
        }

        String progressString = String.valueOf(value) + "+" + step + "/" + max;
        if (step > 0 && max > value) {
            progressString += " (" + (max - value) / step + " " + 
                              Messages.message("turns") + ")";
        }

        int stringWidth  = g2d.getFontMetrics().stringWidth(progressString);
        int stringHeight = g2d.getFontMetrics().getAscent() + g2d.getFontMetrics().getDescent();
        int restWidth = getWidth() - stringWidth;

        if (goodsType >= 0) {

            restWidth -= iconWidth;
            g2d.drawImage(image, restWidth/2, (getHeight() - iconHeight)/2 , null);
        }

        g2d.setColor(PRIMARY_1);
        g2d.drawString(progressString, restWidth/2 + iconWidth,
                       getHeight()/2 + stringHeight/4);
		


        g2d.dispose();
    }

}
