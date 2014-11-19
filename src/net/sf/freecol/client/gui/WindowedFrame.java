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

import java.awt.GraphicsDevice;
import java.awt.Rectangle;

import javax.swing.JFrame;

import net.sf.freecol.client.FreeColClient;


/**
 * The frame that contains everything.  If full screen mode is
 * supported and chosen, then {@link FullScreenFrame} is used instead.
 */
public final class WindowedFrame extends FreeColFrame  {


    /**
     * Create a windowed frame.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gd The <code>GraphicsDevice</code> to display on.
     */
    public WindowedFrame(final FreeColClient freeColClient,
                         GraphicsDevice gd) {
        super(freeColClient, gd);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setCanvas(Canvas canvas) {
        addWindowListener(new WindowedFrameListener(freeColClient));
        super.setCanvas(canvas);
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
