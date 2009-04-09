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

package net.sf.freecol.client.gui.video;

import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.border.Border;

import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.common.resources.ResourceManager;

import com.fluendo.player.Cortado;

/**
 * A component for playing video.
 */
public class VideoComponent extends JPanel {

    private final Cortado applet;

    /**
     * Creates a component for displaying the given video.
     * @param video The <code>Video</code> to be displayed.
     */
    public VideoComponent(Video video) {
        final String url = video.getURL().toExternalForm();
        
        setBorder(createBorder());
        final Insets insets = getInsets();
        
        applet = new Cortado();
        applet.setSize(655, 480);
        // FIXME: -1 avoids transparent part of border.
        applet.setLocation(insets.left - 1, insets.top - 1);

        applet.setParam ("url", url);
        applet.setParam ("local", "false");
        applet.setParam ("framerate", "60");
        applet.setParam ("keepaspect", "true");
        applet.setParam ("video", "true");
        applet.setParam ("audio", "true");
        applet.setParam ("kateIndex", "0");
        applet.setParam ("bufferSize", "200");
        applet.setParam ("showStatus", "hide");
        applet.init();

        setLayout(null);
        add(applet);

        // FIXME: -2 avoids transparent part of border.
        setSize(applet.getWidth() + insets.left + insets.right - 2,
                applet.getHeight() + insets.top + insets.bottom - 2);
    }
    
    /**
     * Start playing the video.
     */
    public void play() {
        applet.start();
    }
    
    /**
     * Stop playing the video.
     */
    public void stop() {
        applet.stop();
    }
    
    public void addMouseListener(MouseListener l) {
        applet.addMouseListener(l);
    }
    
    private Border createBorder() {
        final Image menuborderN = ResourceManager.getImage("menuborder.n.image");
        final Image menuborderNW = ResourceManager.getImage("menuborder.nw.image");
        final Image menuborderNE = ResourceManager.getImage("menuborder.ne.image");
        final Image menuborderW = ResourceManager.getImage("menuborder.w.image");
        final Image menuborderE = ResourceManager.getImage("menuborder.e.image");
        final Image menuborderS = ResourceManager.getImage("menuborder.s.image");
        final Image menuborderSW = ResourceManager.getImage("menuborder.sw.image");
        final Image menuborderSE = ResourceManager.getImage("menuborder.se.image");
        final FreeColImageBorder imageBorder = new FreeColImageBorder(menuborderN, menuborderW, menuborderS,
                menuborderE, menuborderNW, menuborderNE, menuborderSW, menuborderSE);
        return imageBorder;
    }
}
