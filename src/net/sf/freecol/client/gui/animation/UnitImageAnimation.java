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

package net.sf.freecol.client.gui.animation;

import java.awt.Image;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.io.sza.AnimationEvent;
import net.sf.freecol.common.io.sza.ImageAnimationEvent;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.ImageUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * Class for in-place animation of units.
 */
public final class UnitImageAnimation extends Animation {
    
    private static final Logger logger = Logger.getLogger(UnitImageAnimation.class.getName());

    /**
       Alternate directions to check when a directional animation is
       not found.
    */
    public static final Map<Direction, List<Direction>> alternatives
        = new HashMap<>();

    /** The animation image series. */
    private final SimpleZippedAnimation animation;

    /** Reflect the images? */
    private boolean mirror;


    /**
     * Build a new image animation.
     *
     * @param unit The {@code Unit} to be animated.
     * @param tile The {@code Tile} where the animation occurs.
     * @param animation The {@code SimpleZippedAnimation} to show.
     */
    public UnitImageAnimation(Unit unit, Tile tile,
                              SimpleZippedAnimation animation) {
        super(unit, makeUnmodifiableList(tile));
        
        this.animation = animation;
        this.mirror = false;
    }

    /**
     * Set the mirror state.
     *
     * @param mirror The new mirror state.
     */
    public void setMirrored(boolean mirror) {
        this.mirror = mirror;
    }
    

    /**
     * Get a list of directions to try when looking for a
     * direction-keyed animation given a preferred direction which has
     * failed.
     *
     * @param direction The preferred {@code Direction}.
     * @return A list of {@code Direction}s.
     */
    private synchronized static List<Direction> trialDirections(Direction direction) {
        if (alternatives.isEmpty()) { // Populate first time
            // Favour the closest E-W cases
            for (Direction d : Direction.allDirections) {
                List<Direction> a = new ArrayList<>();
                a.add(d);
                switch (d) {
                case N: case S:
                    a.add(Direction.E); a.add(Direction.W);
                    a.add(d.rotate(1)); a.add(d.rotate(-1));
                    a.add(d.rotate(3)); a.add(d.rotate(-3));
                    break;
                case NE: case SW:
                    a.add(d.rotate(1));
                    a.add(d.rotate(2));
                    a.add(d.rotate(-1)); a.add(d.rotate(3));
                    break;
                case NW: case SE:
                    a.add(d.rotate(-1));
                    a.add(d.rotate(-2));
                    a.add(d.rotate(1)); a.add(d.rotate(-3));
                    break;
                case E: case W:
                    a.add(d.rotate(1)); a.add(d.rotate(-1));
                    a.add(d.rotate(2)); a.add(d.rotate(-2));
                    break;
                }
                alternatives.put(d, a);
            }
        }
        return alternatives.get(direction);
    }

    /**
     * Static quasi-constructor that can fail harmlessly.
     *
     * @param unit The {@code Unit} to be animated.
     * @param tile The {@code Tile} where the animation occurs.
     * @param dirn The {@code Direction} of the attack.
     * @param base The base prefix for the animation resource.
     * @param scale The gui scale.
     * @return The animation found or null if none present.
     */
    public static UnitImageAnimation build(Unit unit, Tile tile,
                                           Direction dirn,
                                           String base, float scale) {
        for (Direction d : trialDirections(dirn)) {
            String szaId = base + downCase(d.toString());
            SimpleZippedAnimation sza = ImageLibrary.getSZA(szaId, scale);
            if (sza != null) {
                return new UnitImageAnimation(unit, tile, sza);
            }
            // Try the mirrored case
            szaId = base + downCase(d.getEWMirroredDirection().toString());
            sza = ImageLibrary.getSZA(szaId, scale);
            if (sza != null) {
                UnitImageAnimation ret = new UnitImageAnimation(unit, tile, sza);
                ret.setMirrored(true);
                return ret;
            }
        }
        return null;
    }


    // Implement Animation

    /**
     * {@inheritDoc}
     */
    public void executeWithLabel(JLabel unitLabel,
                                 Animations.Procedure paintCallback) {
        final ImageIcon icon = (ImageIcon)unitLabel.getIcon();

        // Step through the animation, chaning the image
        for (AnimationEvent event : animation) {
            long time = System.nanoTime();
            if (event instanceof ImageAnimationEvent) {
                final ImageAnimationEvent ievent = (ImageAnimationEvent)event;
                Image image = ievent.getImage();
                if (mirror) {
                    // FIXME: Add mirroring functionality to SimpleZippedAnimation
                    image = createMirroredImage(image);
                }
                icon.setImage(image);
                paintCallback.execute(); // paint now

                // Time accounting
                time = ievent.getDurationInMs()
                    - (System.nanoTime() - time) / 1000000;
                if (time > 0) Utils.delay(time, "Animation delayed.");
            }
        }
    }
}
