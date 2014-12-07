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

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;


/**
 * The base frame for FreeCol panels.
 */
public abstract class FreeColFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(FreeColFrame.class.getName());

    /** The FreeCol client controlling the frame. */
    protected final FreeColClient freeColClient;


    /**
     * Create a new main frame.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     */
    public FreeColFrame(FreeColClient freeColClient) {
        super(getFrameName());

        this.freeColClient = freeColClient;
    }

    /**
     * Create a new main frame.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     * @param gd The <code>GraphicsDevice</code> to use.
     */
    public FreeColFrame(FreeColClient freeColClient, GraphicsDevice gd) {
        super(getFrameName(), gd.getDefaultConfiguration());

        this.freeColClient = freeColClient;
    }


    /**
     * Get the standard name for the main frame.
     *
     * @return The standard frame name.
     */
    private static String getFrameName() {
        return "FreeCol " + FreeCol.getVersion();
    } 


    /**
     * Set the canvas for this frame.
     *
     * @param canvas The <code>Canvas</code> to use.
     */
    public void setCanvas(Canvas canvas) {
        // This crashes deep in the Java libraries when changing full screen
        // mode during the opening video
        //   Java version: 1.7.0_45
        //   Java WM name: OpenJDK 64-Bit Server VM
        //   Java WM version: 24.45-b08
        // arch linux, reported by Lone Wolf
        try {
            getContentPane().add(canvas);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Java crash", e);
        }        
    }

    /**
     * Update the bounds of the main FreeCol frame.
     *
     * @param rectangle A <code>Rectangle</code> to specify the bounds.
     */
    public abstract void updateBounds(Rectangle rectangle);
}
