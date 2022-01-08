/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.menu.FreeColMenuBar;


/**
 * The base frame for FreeCol.
 */
public class FreeColFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(FreeColFrame.class.getName());

    /** The FreeCol client controlling the frame. */
    protected final FreeColClient freeColClient;


    /**
     * Create a new main frame.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param gd The {@code GraphicsDevice} to use.
     * @param menuBar The menu bar to add to the frame.
     * @param windowed If the frame should be windowed.
     * @param bounds The optional size of the windowed frame.
     */
    public FreeColFrame(FreeColClient freeColClient, GraphicsDevice gd,
                        JMenuBar menuBar, boolean windowed, Rectangle bounds) {
        super(getFrameName(), gd.getDefaultConfiguration());

        this.freeColClient = freeColClient;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (windowed) {
            setResizable(true);
        } else {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        }
        setJMenuBar(menuBar);
        addWindowListener(windowed
            ? new WindowedFrameListener(freeColClient)
            : new FullScreenFrameListener(freeColClient, this));
        setIconImage(ImageLibrary.getUnscaledImage("image.miscicon.FrameIcon"));

        pack(); // necessary for getInsets
        Insets insets = getInsets();

        // numbers are taken from the size of the opening video
        setMinimumSize(new Dimension(656 + insets.left + insets.right,
                                     480 + insets.top + insets.bottom));

        // Use default bounds if not windowed or (possibly deliberately)
        // invalid bounds specified
        if (!windowed || bounds==null || bounds.width<=0 || bounds.height<=0) {
            bounds = gd.getDefaultConfiguration().getBounds();
            if (windowed) {
                Insets screenInsets = Toolkit.getDefaultToolkit()
                        .getScreenInsets(gd.getDefaultConfiguration());
                bounds = new Rectangle(bounds.x + screenInsets.left,
                                       bounds.y + screenInsets.top,
                                       bounds.width - screenInsets.right,
                                       bounds.height - screenInsets.bottom);
            }
        }
        setBounds(bounds);
        logger.info(((windowed) ? "Windowed" : "Full screen")
            + " frame created with size " + bounds.width
            + "x" + bounds.height);
        /* TODO: this should do something useful!
        if (windowed) {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    logger.info("Window size changes to " + getSize());
                }
            });
        }
        */
    }

    public void exitFullScreen() {
        GraphicsConfiguration GraphicsConf = getGraphicsConfiguration();
        GraphicsDevice gd = GraphicsConf.getDevice();
        gd.setFullScreenWindow(null);
    }

    public void setMenuBar(FreeColMenuBar bar) {
        setJMenuBar(bar);
        validate();
    }
    
    public void removeMenuBar() {
        setJMenuBar(null);
        validate();
    }

    public void resetMenuBar() {
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null) {
            ((FreeColMenuBar)menuBar).reset();
        }
    }

    public void updateMenuBar() {
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null) {
            ((FreeColMenuBar)menuBar).update();
        }
    }

    /**
     * Get the standard name for the main frame.
     *
     * @return The standard frame name.
     */
    private static String getFrameName() {
        return "FreeCol " + FreeCol.getVersion();
    }
}
