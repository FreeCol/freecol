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

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


/**
 * Class to contain the splash screen.
 */
public final class SplashScreen extends JFrame {

    /**
     * Initialize the splash screen.
     *
     * @param gd The {@code GraphicsDevice} to display on.
     * @param splashStream An {@code InputStream} to read content from.
     * @exception IOException on I/O error.
     */
    public SplashScreen(GraphicsDevice gd, InputStream splashStream)
        throws IOException {
        super(gd.getDefaultConfiguration());
        
        BufferedImage im = ImageIO.read(splashStream);
        this.getContentPane().add(new JLabel(new ImageIcon(im)));
        setUndecorated(true);
        this.pack();

        Point start = this.getLocation();
        DisplayMode dm = gd.getDisplayMode();
        int x = start.x + dm.getWidth()/2 - this.getWidth() / 2;
        int y = start.y + dm.getHeight()/2 - this.getHeight() / 2;
        this.setLocation(x, y);
    }
}
