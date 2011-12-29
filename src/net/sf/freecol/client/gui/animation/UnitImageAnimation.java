/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.OutForAnimationCallback;
import net.sf.freecol.common.io.sza.AnimationEvent;
import net.sf.freecol.common.io.sza.ImageAnimationEvent;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;


/**
 * Class for in-place animation of units.
 */
public final class UnitImageAnimation {
    
    private final Unit unit;
    private final SimpleZippedAnimation animation;
    private final Location currentLocation;
    private GUI gui;
    
    /**
     * Constructor
     * @param unit The unit to be animated. 
     * @param animation The animation.
     */
    public UnitImageAnimation(GUI gui, Unit unit,
                              SimpleZippedAnimation animation) {
        this.gui = gui;
        this.unit = unit;
        this.currentLocation = unit.getLocation();
        this.animation = animation;
    }
    

    /**
     * Do the animation.
     */
    public void animate() {
        if (gui.getTilePosition(unit.getTile()) == null) {
            return;
        } 
        // Painting the whole screen once to get rid of disposed dialog-boxes.
        gui.paintImmediatelyCanvasInItsBounds();
        gui.executeWithUnitOutForAnimation(unit, unit.getTile(), new OutForAnimationCallback() {
            public void executeWithUnitOutForAnimation(final JLabel unitLabel) {
                for (AnimationEvent event : animation) {
                    long time = System.nanoTime();
                    if (event instanceof ImageAnimationEvent) {
                        final ImageAnimationEvent ievent = (ImageAnimationEvent) event;
                        final ImageIcon icon = (ImageIcon) unitLabel.getIcon();
                        icon.setImage(ievent.getImage());
                        gui.getCanvas().paintImmediately(getDirtyAnimationArea());

                        time = ievent.getDurationInMs() - (System.nanoTime() - time) / 1000000;
                        if (time > 0) {
                            try {
                                Thread.sleep(time);
                            } catch (InterruptedException ex) {
                                //ignore
                            }
                        }
                    }
                }             
            }
        });
    }
    
    protected Rectangle getDirtyAnimationArea() {
        return gui.getTileBounds(currentLocation.getTile());
    }
}
