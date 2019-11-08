/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.OutForAnimationCallback;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.io.sza.AnimationEvent;
import net.sf.freecol.common.io.sza.ImageAnimationEvent;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.Utils;


/**
 * Class for in-place animation of units.
 */
public final class UnitImageAnimation extends FreeColClientHolder
    implements OutForAnimationCallback {
    
    private static final Logger logger = Logger.getLogger(UnitImageAnimation.class.getName());

    private final Unit unit;
    private final Tile tile;
    private final SimpleZippedAnimation animation;
    private final boolean mirror;

    /**
     * Constructor
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param unit The {@code Unit} to be animated.
     * @param tile The {@code Tile} where the animation occurs.
     * @param animation The animation to show.
     * @param mirror Mirror image the base animation.
     */
    public UnitImageAnimation(FreeColClient freeColClient, Unit unit,
                              Tile tile, SimpleZippedAnimation animation,
                              boolean mirror) {
        super(freeColClient);

        this.unit = unit;
        this.tile = tile;
        this.animation = animation;
        this.mirror = mirror;
    }


    /**
     * Do the animation.
     */
    public void animate() {
        getGUI().executeWithUnitOutForAnimation(this.unit, this.tile, this);
    }


    // Interface OutForAnimationCallback

    /**
     * {@inheritDoc}
     */
    public void executeWithUnitOutForAnimation(JLabel unitLabel) {
        final GUI gui = getGUI();

        // Tile position should now be valid.
        if (gui.getAnimationTilePosition(this.tile) == null) {
            logger.warning("Failed attack animation for " + this.unit
                + " at tile: " + this.tile);
            return;
        }

        final Rectangle rect = gui.getAnimationTileBounds(this.tile);
        final ImageIcon icon = (ImageIcon)unitLabel.getIcon();
        for (AnimationEvent event : animation) {
            long time = System.nanoTime();
            if (event instanceof ImageAnimationEvent) {
                final ImageAnimationEvent ievent = (ImageAnimationEvent)event;
                Image image = ievent.getImage();
                if (mirror) {
                    // FIXME: Add mirroring functionality to SimpleZippedAnimation
                    image = ImageLibrary.createMirroredImage(image);
                }
                icon.setImage(image);
                gui.paintImmediately(rect);
                time = ievent.getDurationInMs()
                    - (System.nanoTime() - time) / 1000000;
                if (time > 0) Utils.delay(time, "Animation delayed.");
            }
        }
        gui.refresh();
    }
}
