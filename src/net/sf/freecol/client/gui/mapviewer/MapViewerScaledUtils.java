package net.sf.freecol.client.gui.mapviewer;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.EnumMap;

import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;

final class MapViewerScaledUtils {
    
    /*
     * These variables are directly dependent on the map scale, which
     * changes in changeScale().
     * Their consistency is maintained by calling updateScaledVariables().
     */

    /** Fog of war area. */
    private final GeneralPath fog = new GeneralPath();

    /** Fonts (scaled). */
    private Font fontNormal, fontItalic, fontProduction, fontTiny;
        
    /** Points to use to draw the borders. */
    private final EnumMap<Direction, Point2D.Float> borderPoints
        = new EnumMap<>(Direction.class);
    /** Support points for quadTo calls when drawing borders. */
    private final EnumMap<Direction, Point2D.Float> controlPoints
        = new EnumMap<>(Direction.class);

    /** Stroke to draw the borders with. */
    private Stroke borderStroke = new BasicStroke(4);

    /** Stroke to draw the grid with. */
    private Stroke gridStroke = new BasicStroke(1);
    
    
    MapViewerScaledUtils() {
    }
    
    
    void updateScaledVariables(ImageLibrary lib) {
        final TileBounds tileBounds = new TileBounds(lib.getTileSize(), lib.getScaleFactor());
        // ATTENTION: we assume that all base tiles have the same size
        this.fog.reset();
        this.fog.moveTo(tileBounds.getHalfWidth(), 0);
        this.fog.lineTo(tileBounds.getWidth(), tileBounds.getHalfHeight());
        this.fog.lineTo(tileBounds.getHalfWidth(), tileBounds.getHeight());
        this.fog.lineTo(0, tileBounds.getHalfHeight());
        this.fog.closePath();

        // Update fonts, make sure font{Normal,Italic} are non-null but
        // allow the others to disappear if they get too small
        this.fontNormal = lib.getScaledFont("normal-bold-smaller", null);
        this.fontItalic = lib.getScaledFont("normal-bold+italic-smaller", null);
        this.fontProduction = lib.getScaledFont("normal-bold-tiny", null);
        this.fontTiny = lib.getScaledFont("normal-plain-tiny", null);
        if (this.fontNormal == null) {
            this.fontNormal = (lib.getScaleFactor() < 1f)
                ? FontLibrary.getUnscaledFont("normal-bold-tiny", null)
                : FontLibrary.getUnscaledFont("normal-bold-max", null);
        }
        if (this.fontItalic == null) this.fontItalic = this.fontNormal;
        
        final int dx = tileBounds.getWidth() / 16;
        final int dy = tileBounds.getHeight() / 16;
        final int ddx = dx + dx / 2;
        final int ddy = dy + dy / 2;

        // small corners
        this.controlPoints.put(Direction.N,
            new Point2D.Float(tileBounds.getHalfWidth(), dy));
        this.controlPoints.put(Direction.E,
            new Point2D.Float(tileBounds.getWidth() - dx, tileBounds.getHalfHeight()));
        this.controlPoints.put(Direction.S,
            new Point2D.Float(tileBounds.getHalfWidth(), tileBounds.getHeight() - dy));
        this.controlPoints.put(Direction.W,
            new Point2D.Float(dx, tileBounds.getHalfHeight()));
        // big corners
        this.controlPoints.put(Direction.SE,
            new Point2D.Float(tileBounds.getHalfWidth(), tileBounds.getHeight()));
        this.controlPoints.put(Direction.NE,
            new Point2D.Float(tileBounds.getWidth(), tileBounds.getHalfHeight()));
        this.controlPoints.put(Direction.SW,
            new Point2D.Float(0, tileBounds.getHalfHeight()));
        this.controlPoints.put(Direction.NW,
            new Point2D.Float(tileBounds.getHalfWidth(), 0));
        // small corners
        this.borderPoints.put(Direction.NW,
            new Point2D.Float(dx + ddx, tileBounds.getHalfHeight() - ddy));
        this.borderPoints.put(Direction.N,
            new Point2D.Float(tileBounds.getHalfWidth() - ddx, dy + ddy));
        this.borderPoints.put(Direction.NE,
            new Point2D.Float(tileBounds.getHalfWidth() + ddx, dy + ddy));
        this.borderPoints.put(Direction.E,
            new Point2D.Float(tileBounds.getWidth() - dx - ddx, tileBounds.getHalfHeight() - ddy));
        this.borderPoints.put(Direction.SE,
            new Point2D.Float(tileBounds.getWidth() - dx - ddx, tileBounds.getHalfHeight() + ddy));
        this.borderPoints.put(Direction.S,
            new Point2D.Float(tileBounds.getHalfWidth() + ddx, tileBounds.getHeight() - dy - ddy));
        this.borderPoints.put(Direction.SW,
            new Point2D.Float(tileBounds.getHalfWidth() - ddx, tileBounds.getHeight() - dy - ddy));
        this.borderPoints.put(Direction.W,
            new Point2D.Float(dx + ddx, tileBounds.getHalfHeight() + ddy));

        this.borderStroke = new BasicStroke(dy);
        this.gridStroke = new BasicStroke(lib.getScaleFactor());
    }
    
    GeneralPath getFog() {
        return fog;
    }
    
    Stroke getGridStroke() {
        return gridStroke;
    }
    
    Stroke getBorderStroke() {
        return borderStroke;
    }
    
    EnumMap<Direction, Point2D.Float> getBorderPoints() {
        return borderPoints;
    }
    
    EnumMap<Direction, Point2D.Float> getControlPoints() {
        return controlPoints;
    }
    
    Font getFontNormal() {
        return fontNormal;
    }
    
    Font getFontProduction() {
        return fontProduction;
    }
    
    Font getFontItalic() {
        return fontItalic;
    }
    
    Font getFontTiny() {
        return fontTiny;
    }
}
