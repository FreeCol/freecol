/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.menu.FreeColMenuBar;
import net.sf.freecol.common.option.FullscreenDisplayModeOption;


/**
 * The base frame for FreeCol.
 */
public class FreeColFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(FreeColFrame.class.getName());

    /** The FreeCol client controlling the frame. */
    protected final FreeColClient freeColClient;

    private PropertyChangeListener displayModeChange = (event) -> {
                updateDisplayMode(getGraphicsConfiguration().getDevice());
            };

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
            
            final FullscreenDisplayModeOption fdmo = getDisplayModeOption(freeColClient);
            fdmo.addPropertyChangeListener(displayModeChange);
            updateDisplayMode(gd);
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

    private FullscreenDisplayModeOption getDisplayModeOption(FreeColClient freeColClient) {
        return freeColClient.getClientOptions().getOption(ClientOptions.DISPLAY_MODE_FULLSCREEN, FullscreenDisplayModeOption.class);
    }

    private void updateDisplayMode(GraphicsDevice gd) {
        final FullscreenDisplayModeOption fdmo = getDisplayModeOption(freeColClient);
        final DisplayMode displayMode = fdmo.getValue().getDisplayMode();
        if (displayMode == null) {
            final DisplayMode matchingDisplayMode = List.of(gd.getDisplayModes()).stream()
                    .sorted(Comparator.comparingInt((DisplayMode dm) -> dm.getWidth() * dm.getHeight())
                            .thenComparing(DisplayMode::getRefreshRate)
                            .thenComparing(DisplayMode::getBitDepth)
                            .reversed())
                    .findFirst()
                    .orElse(null);
            if (matchingDisplayMode == null) {
                return;
            }
            gd.setDisplayMode(matchingDisplayMode);
            return;
        }
        
        final DisplayMode matchingDisplayMode = List.of(gd.getDisplayModes()).stream()
                .filter(dm -> dm.getWidth() == displayMode.getWidth() && dm.getHeight() == displayMode.getHeight())
                .sorted(Comparator.comparingInt(DisplayMode::getRefreshRate).thenComparing(DisplayMode::getBitDepth).reversed())
                .findFirst()
                .orElse(null);
        if (matchingDisplayMode == null) {
            return;
        }
        
        gd.setDisplayMode(matchingDisplayMode);
    }

    public void exitFullScreen() {
        GraphicsConfiguration GraphicsConf = getGraphicsConfiguration();
        GraphicsDevice gd = GraphicsConf.getDevice();
        gd.setFullScreenWindow(null);
        getDisplayModeOption(freeColClient).removePropertyChangeListener(displayModeChange);
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
