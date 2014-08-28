/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

import java.awt.Rectangle;
import java.awt.GraphicsDevice;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Logger;

import javax.swing.JFrame;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The frame that contains everything.  If full screen mode is
 * supported and chosen, then {@link FullScreenFrame} is used instead.
 */
public final class WindowedFrame extends FreeColFrame  {

    private static final Logger logger = Logger.getLogger(WindowedFrame.class.getName());

    /**
     * Create a windowed frame.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     * @param gd The <code>GraphicsDevice</code> to display on.
     * @param canvas The <code>Canvas</code> to extract the size from.
     */
    public WindowedFrame(final FreeColClient freeColClient,
                         GraphicsDevice gd, final Canvas canvas) {
        super(freeColClient, "FreeCol " + FreeCol.getVersion(), gd);

        // Disabled as this prevents the --windowed WIDTHxHEIGHT
        // command line parameter from working.
        //this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ResourceManager.preload(canvas.getSize());
            }
        });

        logger.info("WindowedFrame created with size: "
            + canvas.getSize());
    }


    /**
     * {@inheritDoc}
     */
    public void updateBounds(Rectangle rectangle) {
        if (rectangle != null) {
            setBounds(rectangle);
        } else {
            pack();
        }   
    }
}
