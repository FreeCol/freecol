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

package net.sf.freecol.client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This class is responsible for drawing the Roads on a tile.
 */
public final class RoadPainter {

    /** Helper variables for displaying the map. */
    private int tileHeight, tileWidth, halfHeight, halfWidth;

    private final EnumMap<Direction, Point2D.Float> corners
        = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, List<Direction>> prohibitedRoads
        = new EnumMap<>(Direction.class);

    private Stroke roadStroke = new BasicStroke(2);


    /**
     * Create a new road painter for a given tile size.
     *
     * @param tileSize The tile size as a {@code Dimension}.
     */
    public RoadPainter(Dimension tileSize) {
        this.tileHeight = tileSize.height;
        this.tileWidth  = tileSize.width;
        this.halfHeight = this.tileHeight/2;
        this.halfWidth  = this.tileWidth/2;
        int dy = this.tileHeight/16;
        this.roadStroke = new BasicStroke(dy / 2.0f);

        // Corners
        corners.put(Direction.N,  new Point2D.Float(this.halfWidth, 0));
        corners.put(Direction.NE, new Point2D.Float(0.75f * this.tileWidth,
                                                    0.25f * this.tileHeight));
        corners.put(Direction.E,  new Point2D.Float(this.tileWidth,
                                                    this.halfHeight));
        corners.put(Direction.SE, new Point2D.Float(0.75f * this.tileWidth,
                                                    0.75f * this.tileHeight));
        corners.put(Direction.S,  new Point2D.Float(this.halfWidth,
                                                    this.tileHeight));
        corners.put(Direction.SW, new Point2D.Float(0.25f * this.tileWidth,
                                                    0.75f * this.tileHeight));
        corners.put(Direction.W,  new Point2D.Float(0, this.halfHeight));
        corners.put(Direction.NW, new Point2D.Float(0.25f * this.tileWidth,
                                                    0.25f * this.tileHeight));

        // Road pairs to skip drawing when doing 3 or 4 exit point tiles.
        // Don't put more than two directions in each list, otherwise
        // a 3-point tile may not draw any roads at all!
        prohibitedRoads.put(Direction.N,
                            Arrays.asList(Direction.NW, Direction.NE));
        prohibitedRoads.put(Direction.NE,
                            Arrays.asList(Direction.N,  Direction.E));
        prohibitedRoads.put(Direction.E,
                            Arrays.asList(Direction.NE, Direction.SE));
        prohibitedRoads.put(Direction.SE,
                            Arrays.asList(Direction.E,  Direction.S));
        prohibitedRoads.put(Direction.S,
                            Arrays.asList(Direction.SE, Direction.SW));
        prohibitedRoads.put(Direction.SW,
                            Arrays.asList(Direction.S,  Direction.W));
        prohibitedRoads.put(Direction.W,
                            Arrays.asList(Direction.SW, Direction.NW));
        prohibitedRoads.put(Direction.NW,
                            Arrays.asList(Direction.W,  Direction.N));
    }

    /**
     * Draws all roads on the given Tile.
     *
     * @param g The {@code Graphics} to draw the road upon.
     * @param tile The {@code Tile} with the road.
     */
    public void displayRoad(Graphics2D g, Tile tile) {
        Color oldColor = g.getColor();
        g.setColor(ImageLibrary.getRoadColor());
        g.setStroke(roadStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        final Map map = tile.getMap();
        final int x = tile.getX();
        final int y = tile.getY();
        final Predicate<Direction> borderPred = d -> {
            Tile borderingTile = map.getTile(d.step(x, y));
            TileImprovement r;
            return borderingTile != null
                && (r = borderingTile.getRoad()) != null
                && r.isComplete();
        };
        List<Direction> directions = transform(Direction.allDirections, borderPred);
        List<Point2D.Float> points = transform(directions, alwaysTrue(),
                                               d -> corners.get(d));
        GeneralPath path = new GeneralPath();
        switch (points.size()) {
        case 0:
            path.moveTo(0.35f * tileWidth, 0.35f * tileHeight);
            path.lineTo(0.65f * tileWidth, 0.65f * tileHeight);
            path.moveTo(0.35f * tileWidth, 0.65f * tileHeight);
            path.lineTo(0.65f * tileWidth, 0.35f * tileHeight);
            break;
        case 1:
            path.moveTo(halfWidth, halfHeight);
            path.lineTo(points.get(0).getX(), points.get(0).getY());
            break;
        case 2:
            path.moveTo(points.get(0).getX(), points.get(0).getY());
            path.quadTo(halfWidth, halfHeight,
                        points.get(1).getX(), points.get(1).getY());
            break;
        case 3: case 4:
            Direction pen = directions.get(directions.size() - 1);
            Point2D pt = corners.get(pen);
            path.moveTo(pt.getX(), pt.getY());
            for (Direction d : directions) {
                pt = corners.get(d);
                if (prohibitedRoads.get(pen).contains(d)) {
                    path.moveTo(pt.getX(), pt.getY());
                } else {
                    path.quadTo(halfWidth, halfHeight, pt.getX(), pt.getY());
                }
                pen = d;
            }
            break;
        default:
            for (Point2D p : points) {
                path.moveTo(halfWidth, halfHeight);
                path.lineTo(p.getX(), p.getY());
            }
            break;
        }
        g.draw(path);
        g.setColor(oldColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_OFF);
    }
}
