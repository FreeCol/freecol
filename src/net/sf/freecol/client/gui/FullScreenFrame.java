/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.GraphicsDevice;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.sf.freecol.FreeCol;

/**
 * The fullscreen frame that contains everything. If full screen mode is not
 * supported (or chosen), then the {@link WindowedFrame} will be used instead.
 */
public final class FullScreenFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(FullScreenFrame.class
                                                          .getName());


    private Canvas canvas;

    /**
     * The constructor to use.
     * 
     * @param gd
     *            The context of this <code>FullScreenFrame</code>.
     */
    public FullScreenFrame(GraphicsDevice gd) {
        super("Freecol " + FreeCol.getVersion(), gd.getDefaultConfiguration());
		
        logger.info("FullScreenFrame's JFrame created.");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);

        gd.setFullScreenWindow(this);

        logger.info("Switched to full screen mode.");

        //getContentPane().setLayout(null);

        logger.info("FullScreenFrame created.");
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        addWindowListener(new WindowedFrameListener(canvas));
    }

    /**
     * Adds a component to this FullScreenFrame.
     * 
     * @param c
     *            The component to add to this FullScreenFrame.
     */
    public void addComponent(JComponent c) {
        canvas.add(c);
    }
}

