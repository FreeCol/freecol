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

package net.sf.freecol.client.gui;

import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Logger;

import javax.swing.JFrame;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The frame that contains everything. If full screen mode is
 * supported and choosen, then the {@link FullScreenFrame} will be used
 * instead.
 */
public final class WindowedFrame extends FreeColFrame  {

    private static final Logger logger = Logger.getLogger(WindowedFrame.class.getName());

    /**
    * The constructor to use.
     * @param canvas 
    */
    public WindowedFrame(final FreeColClient freeColClient, final Canvas canvas) {
        super(freeColClient, "FreeCol " + FreeCol.getVersion());
        logger.info("WindowedFrame's JFrame created.");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ResourceManager.preload(canvas.getSize());
            }
        });

        logger.info("WindowedFrame created.");
    }


    @Override
    public void updateBounds(Rectangle rectangle) {
        if (rectangle != null) {
            setBounds(rectangle);
        } else {
            pack();
        }   
    }
}
