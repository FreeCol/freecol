package net.sf.freecol.client.gui.mapviewer;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

/**
 * Handles taking units of of the map rendering in order to be animated using {@code JLabel}s.
 */
public class UnitAnimator {
    /** The units that are being animated and an associated reference count. */
    private final java.util.Map<Unit, Integer> unitsOutForAnimation
        = new HashMap<>();
    /** The labels being used in animation for a unit. */
    private final java.util.Map<Unit, JLabel> unitsOutForAnimationLabels
        = new HashMap<>();
    
    private final MapViewer mapViewer;
    private final ImageLibrary lib;
    
    
    UnitAnimator(FreeColClient freeColClient, MapViewer mapViewer, ImageLibrary lib) {
        this.mapViewer = mapViewer;
        this.lib = lib;
    }

    
    /**
     * Make an animation label for the unit, and reference count it.
     *
     * @param unit The {@code Unit} to animate.
     * @return A {@code JLabel} for the animation.
     */
    public JLabel enterUnitOutForAnimation(final Unit unit) {
        Integer i = this.unitsOutForAnimation.get(unit);
        if (i == null) {
            final JLabel unitLabel = createUnitAnimationLabel(unit);
            this.unitsOutForAnimationLabels.put(unit, unitLabel);
            i = 1;
        } else {
            i++;
        }
        this.unitsOutForAnimation.put(unit, i);
        return this.unitsOutForAnimationLabels.get(unit);
    }

    /**
     * Release an animation label for a unit, maintain the reference count.
     *
     * @param unit The {@code Unit} to animate.
     */
    public void releaseUnitOutForAnimation(final Unit unit) {
        Integer i = this.unitsOutForAnimation.get(unit);
        if (i == null) {
            throw new RuntimeException("Unit not out for animation: " + unit);
        }
        if (i == 1) {
            this.unitsOutForAnimation.remove(unit);
        } else {
            i--;
            this.unitsOutForAnimation.put(unit, i);
        }
    }
    
    /**
     * Are any units being animated?
     *
     * @return True if unit animation underway.
     */
    boolean isUnitsOutForAnimation() {
        return !unitsOutForAnimation.isEmpty();
    }

    /**
     * Is a given unit being animated?
     *
     * @param unit The {@code Unit} to check.
     * @return True if the unit is being animated.
     */
    public boolean isOutForAnimation(final Unit unit) {
        return this.unitsOutForAnimation.containsKey(unit);
    }

    /**
     * Get the position a unit label should be positioned in a tile.
     *
     * @param unitLabel The unit {@code JLabel}.
     * @param tile The {@code Tile} to position in.
     * @return The {@code Point} to position the label.
     */
    public Point getAnimationPosition(JLabel unitLabel, Tile tile) {
        final Point tilePosition = mapViewer.getMapViewerBounds().calculateTilePosition(tile, false);
        return mapViewer.calculateUnitLabelPositionInTile(unitLabel, tilePosition);
    }
    
    /**
     * Create a label to use for animating a unit.
     *
     * @param unit The {@code Unit} to animate.
     * @return A {@code JLabel} to use in animation.
     */
    private JLabel createUnitAnimationLabel(Unit unit) {
        final BufferedImage unitImg = this.lib.getScaledUnitImage(unit);

        final JLabel label = new JLabel(new ImageIcon(unitImg));
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);
        label.setSize(unitImg.getWidth(), unitImg.getHeight());

        return label;
    }
}
