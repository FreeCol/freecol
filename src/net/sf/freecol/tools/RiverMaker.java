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

package net.sf.freecol.tools;

import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.io.File;
import javax.imageio.ImageIO;



public class RiverMaker {

    private static final int BASE_WIDTH = 128;
    private static final int BASE_HEIGHT = 64;
    private static final int HALF_WIDTH = BASE_WIDTH / 2;
    private static final int HALF_HEIGHT = BASE_HEIGHT / 2;

    private static final float EDGE_LENGTH = (float) Math.sqrt(HALF_WIDTH * HALF_WIDTH + HALF_HEIGHT * HALF_HEIGHT);

    private static final int DY = 4;
    private static final int DX = 2 * DY;

    private static final Point2D.Float NE
        = new Point2D.Float(HALF_WIDTH + DX, DY);
    private static final Point2D.Float SE
        = new Point2D.Float(BASE_WIDTH - DX, HALF_HEIGHT + DY);
    private static final Point2D.Float SW
        = new Point2D.Float(DX, HALF_HEIGHT + DY);
    private static final Point2D.Float NW
        = new Point2D.Float(HALF_WIDTH - DX, DY);
    private static final Point2D.Float CENTER
        = new Point2D.Float(HALF_WIDTH, 2 * DY);

    private static final Point2D.Float[] POINTS
        = { NE, SE, SW, NW };


    public static void main(String[] args) throws Exception {

        String riverName = "data/rules/classic/resources/images/terrain/"
            + "ocean/center0.png";
        String riverDir = "data/rules/classic/resources/images/river";
        BufferedImage river = ImageIO.read(new File(riverName));
        // grab a rectangle completely filled with water
        river = river.getSubimage(44, 22, 40, 20);
        Rectangle2D rectangle = new Rectangle(0, 0, river.getWidth(), river.getHeight());
        TexturePaint texture = new TexturePaint(river, rectangle);
        Stroke minor = new BasicStroke(4);
        Stroke major = new BasicStroke(6);

        //         float seg = EDGE_LENGTH / 6;

        // Path2D.Float straight = new Path2D.Float();
        // straight.moveTo(0, 0);
        // straight.quadTo(seg, 0, 2 * seg, -4);
        // straight.quadTo(3 * seg, -8, 4 * seg, -4);
        // straight.quadTo(5 * seg, 0, 6 * seg, 0);

        // Path2D.Float bend = new Path2D.Float();
        // bend.moveTo(0, 0);
        // bend.lineTo(5 * seg, 0);
        // bend.quadTo(EDGE_LENGTH, 0, EDGE_LENGTH, seg);
        // bend.lineTo(EDGE_LENGTH, EDGE_LENGTH);

        int[] branches = { 1, 0, 0, 0 };
        for (int index = 1; index < 81; index++) {
            BufferedImage result = new BufferedImage(128, 64,
                                                     BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = result.createGraphics();
            g.setPaint(texture);
            String name = getName(branches);
            int count = branchCount(branches);
            for (int branch = 0; branch < branches.length; branch++) {
                int size = branches[branch];
                if (size > 0) {
                    g.setStroke(size == 1 ? minor : major);
                    int next = (branch + 1) % 4;
                    int other = (branch + 2) % 4;
                    Path2D.Float bend = new Path2D.Float();
                    float px = (POINTS[branch].x + POINTS[other].x) / 2;
                    float py = (POINTS[branch].y + POINTS[other].y) / 2;
                    bend.moveTo(POINTS[branch].x, POINTS[branch].y);
                    if (count == 1) {
                        // single
                        bend.lineTo(px, py);
                        g.draw(bend);
                        break;
                    } else if (branches[other] > 0) {
                        // or straight line
                        bend.lineTo(px, py);
                        bend.moveTo(POINTS[branch].x, POINTS[branch].y);
                    }
                    if (branches[next] > 0) {
                        // bend, possibly around start
                        bend.quadTo(CENTER.x, CENTER.y,
                                    (POINTS[next].x + CENTER.x) / 2,
                                    (POINTS[next].y + CENTER.y) / 2);
                        bend.lineTo(POINTS[next].x, POINTS[next].y);
                    }
                    g.draw(bend);
                }
            }

            /*
            g.setStroke(stroke);
            //bend.transform(AffineTransform.getRotateInstance(Math.PI/6));
            //bend.transform(AffineTransform.getTranslateInstance(HALF_WIDTH, 0));
            Path2D.Float path = new Path2D.Float();
            float dx = (NW.x - SE.x)/6;
            float dy = (NW.y - SE.y)/6;
            path.moveTo(NW.x, NW.y);
            //path.lineTo(SE.x, SE.y);
            path.lineTo(SE.x - 16, SE.y - 8);
            path.quadTo(BASE_WIDTH - 16, HALF_HEIGHT, SE.x - 16, SE.y + 8);
            path.lineTo(SW.x, SW.y);
            //path.moveTo(NE.x, NE.y);
            //path.lineTo(SW.x, SW.y);
            g.draw(path);
            */
            g.dispose();
            ImageIO.write(result, "png", new File(riverDir, "river" + name + ".png"));
            branches = nextBranch(branches);
        }

    }


    private static int[] nextBranch(int[] branches) {
        for (int index = 0; index < branches.length; index++) {
            if (branches[index] == 2) {
                branches[index] = 0;
            } else {
                branches[index]++;
                break;
            }
        }
        return branches;
    }

    private static int branchCount(int[] branches) {
        int result = 0;
        for (int branche : branches) {
            if (branche > 0) {
                result++;
            }
        }
        return result;
    }

    private static String getName(int[] branches) {
        String name = "";
        for (int branche : branches) {
            name += Integer.toString(branche);
        }
        return name;
    }
}