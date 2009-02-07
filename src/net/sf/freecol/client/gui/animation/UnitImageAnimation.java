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

package net.sf.freecol.client.gui.animation;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.io.sza.AnimationEvent;
import net.sf.freecol.common.io.sza.ImageAnimationEvent;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;

/**
 * Class for the animation of units movement.
 */
public final class UnitImageAnimation {
    
    private static final Logger logger = Logger.getLogger(UnitImageAnimation.class.getName());
    
    private final Canvas canvas;
    private final Unit unit;
    private final SimpleZippedAnimation animation;
    private final Direction direction;
    
    private final Location currentLocation;
    
    private JLabel unitLabel;
    private static final Integer UNIT_LABEL_LAYER = JLayeredPane.DEFAULT_LAYER;
    
    
    /**
     * Constructor
     * @param canvas The canvas where the animation will be drawn
     * @param unit The unit to be animated. 
     * @param animation The animation.
     * @param direction The Direction of the animation (if applicable).
     */
    public UnitImageAnimation(Canvas canvas, Unit unit, SimpleZippedAnimation animation, Direction direction) {
        this.canvas = canvas;
        
        this.unit = unit;
        this.direction = direction;
        this.currentLocation = unit.getLocation();
        this.animation = animation;
                
        final GUI gui = canvas.getGUI();
        
        Point currP = gui.getTilePosition(unit.getTile());
        if (currP != null) {
            unitLabel = gui.getUnitLabel(unit);
            unitLabel.setSize(gui.getTileWidth(), gui.getTileHeight());
            unitLabel.setLocation(gui.getUnitLabelPositionInTile(unitLabel, currP));
        } else {
            // Unit is offscreen  - no need to animate
        }
    }
    
    public void animate() {
        // Painting the whole screen once to get rid of disposed dialog-boxes.
        canvas.paintImmediately(canvas.getBounds());
        canvas.getGUI().executeWithUnitOutForAnimation(unit, new Runnable() {
            public void run() {
                try {
                    canvas.add(unitLabel, UNIT_LABEL_LAYER, false);
                    for (AnimationEvent event : animation) {
                        long time = System.nanoTime();
                        if (event instanceof ImageAnimationEvent) {
                            final ImageAnimationEvent ievent = (ImageAnimationEvent) event;
                            final ImageIcon icon = (ImageIcon) unitLabel.getIcon();
                            icon.setImage(ievent.getImage());
                            canvas.paintImmediately(getDirtyAnimationArea());
                            
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
                } finally {
                    canvas.remove(unitLabel, false);
                }                
            }
        });
    }
    
    protected Rectangle getDirtyAnimationArea() {
        return canvas.getGUI().getTileBounds(currentLocation.getTile());
    }
}
