/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.client.gui.animation;

import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.io.sza.AnimationEvent;
import net.sf.freecol.common.io.sza.ImageAnimationEvent;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Class for in-place animation of units.
 */
public final class UnitImageAnimation {
    
    private final SwingGUI gui;
    private final Unit unit;
    private final Tile tile;
    private final SimpleZippedAnimation animation;
    private final boolean mirror;

    /**
     * Constructor
     *
     * @param gui The gui.
     * @param unit The <code>Unit</code> to be animated. 
     * @param tile The <code>Tile</code> where the animation occurs.
     * @param animation The animation to show.
     */
    public UnitImageAnimation(SwingGUI gui, Unit unit, Tile tile,
                              SimpleZippedAnimation animation, boolean mirror) {
        this.gui = gui;
        this.unit = unit;
        this.tile = tile;
        this.animation = animation;
        this.mirror = mirror;
    }

    /**
     * Do the animation.
     */
    public void animate() {
        if (gui.getTilePosition(tile) == null) return;

        // Painting the whole screen once to get rid of disposed dialog-boxes.
        gui.paintImmediatelyCanvasInItsBounds();
        gui.executeWithUnitOutForAnimation(unit, tile, (JLabel unitLabel) -> {
                for (AnimationEvent event : animation) {
                    long time = System.nanoTime();
                    if (event instanceof ImageAnimationEvent) {
                        final ImageAnimationEvent ievent = (ImageAnimationEvent) event;
                        final ImageIcon icon = (ImageIcon)unitLabel.getIcon();
                        Image image = ievent.getImage();
                        if(mirror) {
                            // FIXME: Add mirroring functionality to SimpleZippedAnimation
                            image = ImageLibrary.createMirroredImage(image);
                        }
                        icon.setImage(image);
                        gui.paintImmediatelyCanvasIn(getDirtyAnimationArea());
                        
                        time = ievent.getDurationInMs()
                            - (System.nanoTime() - time) / 1000000;
                        if (time > 0) {
                            try {
                                Thread.sleep(time);
                            } catch (InterruptedException ex) {
                                //ignore
                            }
                        }
                    }
                }
            });
    }

    protected Rectangle getDirtyAnimationArea() {
        return gui.getTileBounds(tile);
    }
}
