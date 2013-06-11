/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.JFrame;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;


/**
 * The fullscreen frame that contains everything. If full screen mode is not
 * supported (or chosen), then the {@link WindowedFrame} will be used instead.
 */
public final class FullScreenFrame extends FreeColFrame {

    private static final Logger logger = Logger.getLogger(FullScreenFrame.class
                                                          .getName());


    /**
     * The constructor to use.
     * @param freeColClient 
     * 
     * @param gd
     *            The context of this <code>FullScreenFrame</code>.
     */
    public FullScreenFrame(FreeColClient freeColClient, GraphicsDevice gd) {
        super(freeColClient, "Freecol " + FreeCol.getVersion(), gd);
		
        logger.info("FullScreenFrame's JFrame created.");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);

        gd.setFullScreenWindow(this);

        logger.info("Switched to full screen mode.");

        //getContentPane().setLayout(null);

        logger.info("FullScreenFrame created.");
    }


    @Override
    public void updateBounds(Rectangle rectangle) {
    }


}

