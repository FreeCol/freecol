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

package net.sf.freecol.client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This class is responsible for drawing the Roads on a tile.
 */
public final class RoadPainter {
    // Helper variables for displaying the map.
    private int tileHeight, tileWidth, halfHeight, halfWidth;

    // roads
    private final EnumMap<Direction, Point2D.Float> corners =
        new EnumMap<>(Direction.class);
    private final EnumMap<Direction, List<Direction>> prohibitedRoads =
        new EnumMap<>(Direction.class);
    private Stroke roadStroke = new BasicStroke(2);

    public RoadPainter(Dimension tileSize) {
        tileHeight = tileSize.height;
        tileWidth = tileSize.width;
        halfHeight = tileHeight/2;
        halfWidth = tileWidth/2;

        int dy = tileHeight/16;

        // corners
        corners.put(Direction.N,  new Point2D.Float(halfWidth, 0));
        corners.put(Direction.NE, new Point2D.Float(0.75f * tileWidth, 0.25f * tileHeight));
        corners.put(Direction.E,  new Point2D.Float(tileWidth, halfHeight));
        corners.put(Direction.SE, new Point2D.Float(0.75f * tileWidth, 0.75f * tileHeight));
        corners.put(Direction.S,  new Point2D.Float(halfWidth, tileHeight));
        corners.put(Direction.SW, new Point2D.Float(0.25f * tileWidth, 0.75f * tileHeight));
        corners.put(Direction.W,  new Point2D.Float(0, halfHeight));
        corners.put(Direction.NW, new Point2D.Float(0.25f * tileWidth, 0.25f * tileHeight));

        // road pairs to skip drawing when doing 3 or 4 exit point tiles
        //  don't put more than two directions in each list,
        //  otherwise a 3-point tile may not draw any roads at all!
        prohibitedRoads.put(Direction.N,  Arrays.asList(Direction.NW, Direction.NE));
        prohibitedRoads.put(Direction.NE, Arrays.asList(Direction.N, Direction.E));
        prohibitedRoads.put(Direction.E,  Arrays.asList(Direction.NE, Direction.SE));
        prohibitedRoads.put(Direction.SE, Arrays.asList(Direction.E, Direction.S));
        prohibitedRoads.put(Direction.S,  Arrays.asList(Direction.SE, Direction.SW));
        prohibitedRoads.put(Direction.SW, Arrays.asList(Direction.S, Direction.W));
        prohibitedRoads.put(Direction.W,  Arrays.asList(Direction.SW, Direction.NW));
        prohibitedRoads.put(Direction.NW, Arrays.asList(Direction.W, Direction.N));

        roadStroke = new BasicStroke(dy / 2.0f);
    }

    /**
     * Draws all roads on the given Tile.
     *
     * @param g The <code>Graphics</code> to draw the road upon.
     * @param tile a <code>Tile</code>
     */
    public void displayRoad(Graphics2D g, Tile tile) {
        Color oldColor = g.getColor();
        g.setColor(ResourceManager.getColor("color.map.road"));
        g.setStroke(roadStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        GeneralPath path = new GeneralPath();
        Map map = tile.getMap();
        int x = tile.getX();
        int y = tile.getY();
        List<Point2D.Float> points = new ArrayList<>(8);
        List<Direction> directions = Direction.allDirections.stream()
            .filter((Direction direction) -> {
                    Tile borderingTile = map.getTile(direction.step(x, y));
                    TileImprovement r;
                    return (borderingTile != null
                        && (r = borderingTile.getRoad()) != null
                        && r.isComplete());
                })
            .peek((Direction direction) -> points.add(corners.get(direction)))
            .collect(Collectors.toList());

        switch(points.size()) {
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
            path.quadTo(halfWidth, halfHeight, points.get(1).getX(), points.get(1).getY());
            break;
        case 3:
        case 4: {
            Direction pen = directions.get(directions.size() - 1);
            Point2D p = corners.get(pen);
            path.moveTo(p.getX(), p.getY());
            for (Direction d : directions) {
                p = corners.get(d);
                if(prohibitedRoads.get(pen).contains(d)) {
                    path.moveTo(p.getX(), p.getY());
                } else {
                    path.quadTo(halfWidth, halfHeight, p.getX(), p.getY());
                }
                pen = d;
            }
            break;
        }
        default:
            for (Point2D p : points) {
                path.moveTo(halfWidth, halfHeight);
                path.lineTo(p.getX(), p.getY());
            }
        }
        g.draw(path);
        g.setColor(oldColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

}
