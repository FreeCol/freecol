/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;


/**
 * Generate forest tiles.
 */
public class ForestMaker {

    private static String DESTDIR = "data/rules/classic/resources/images/forest";

    private static int BASE_WIDTH = 128;
    private static int BASE_HEIGHT = 64;
    private static int MARGIN = 20;
    private static int TREES = 60;
    private static int RIVER_MARGIN = 4;

    private static int HALF_WIDTH = BASE_WIDTH / 2;
    private static int HALF_HEIGHT = BASE_HEIGHT / 2;

    private static int[] LIMIT = new int[] {
        HALF_WIDTH, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH
    };

    private static double[] SLOPE = new double[] {
        -0.5, 0.5, -0.5, 0.5
    };

    private static final Point TOP = new Point(HALF_WIDTH, -HALF_HEIGHT);
    private static final Point BOTTOM = new Point(HALF_WIDTH, HALF_HEIGHT);
    private static final Point LEFT = new Point(0, 0);
    private static final Point RIGHT = new Point(BASE_WIDTH, 0);

    private static final int[] POWERS_OF_TWO
        = new int[] { 1, 2, 4, 8 };


    private static boolean drawBorders = true;
    private static boolean drawRivers = true;
    private static boolean drawTrees = true;


    private static class ImageLocation implements Comparable<ImageLocation> {
        BufferedImage image;
        int x, y;

        public ImageLocation(BufferedImage image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }

        public int compareTo(ImageLocation other) {
            int dy = other.y - this.y;
            if (dy == 0) {
                return other.x - this.x;
            } else {
                return dy;
            }
        }

    }


    /**
     * Pass the source directory as first argument.
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Usage: ForestMaker <directory>...");
            System.out.println("Directory name should match a directory in");
            System.out.println("   " + DESTDIR);
            System.exit(1);
        }

        String riverName = "data/rules/classic/resources/images/terrain/"
            + "ocean/center0.png";
        BufferedImage river = ImageIO.read(new File(riverName));
        // grab a rectangle completely filled with water
        river = river.getSubimage(44, 22, 40, 20);
        Rectangle2D rectangle = new Rectangle(0, 0, river.getWidth(), river.getHeight());
        TexturePaint texture = new TexturePaint(river, rectangle);
        // set up river points
        Point[] p = new Point[] {
            new Point(RIGHT), new Point(RIGHT),
            new Point(BOTTOM), new Point(TOP),
            null, null
        };
        p[0].translate(-8, 4);
        p[1].translate(-8, -4);
        p[2].translate(-8, 4);
        p[3].translate(-8, -4);
        p[4] = new Point((p[0].x + p[2].x) / 2,
                         (p[0].y + p[2].y) / 2);
        p[5] = new Point((p[1].x + p[3].x) / 2,
                         (p[1].y + p[3].y) / 2);

        for (String arg : args) {
            File sourceDirectory = new File(arg);
            if (!sourceDirectory.exists()) {
                System.out.println("Source directory " + arg + " does not exist.");
                continue;
            }
            String baseName = sourceDirectory.getName();
            File destinationDirectory = new File(DESTDIR, baseName);
            if (!destinationDirectory.exists()) {
                System.out.println("Destination directory " + destinationDirectory.getPath()
                                   + " does not exist.");
                continue;
            }
            File[] imageFiles = sourceDirectory.listFiles();
            if (imageFiles == null) {
                System.out.println("No images found in source directory " + arg + ".");
                continue;
            } else {
                System.out.println(imageFiles.length + " images found in source directory "
                                   + arg + ".");
            }
            List<BufferedImage> images = new ArrayList<BufferedImage>(imageFiles.length);
            int maximumHeight = 0;
            for (File imageFile : imageFiles) {
                if (imageFile.isFile() && imageFile.canRead()) {
                    try {
                        BufferedImage image = ImageIO.read(imageFile);
                        images.add(image);
                        if (image.getHeight() > maximumHeight) {
                            maximumHeight = image.getHeight();
                        }
                    } catch(Exception e) {
                        System.out.println("Unable to load image " + imageFile.getName() + ":\n");
                        e.printStackTrace();
                    }
                }
            }
            int numberOfImages = images.size();
            Random random = new Random(1492);

            /**
             * If we translate the diamond so that it is bisected by
             * the x-axis, we can describe any point within the
             * diamond as the addition of the two vectors that make up
             * the diamond's left sides, namely:
             *
             * { HALF_WIDTH,  HALF_HEIGHT }    and
             * { HALF_WIDTH, -HALF_HEIGHT }
             *
             * Thus, a random point can be found by scaling these two
             * vectors by the random numbers a and b, and adding the
             * result:
             *
             * x = (a + b) * HALF_WIDTH;
             * y = (a - b) * HALF_HEIGHT;
             */
            for (int index = 0; index < 16; index++) {
                BufferedImage base = new BufferedImage(BASE_WIDTH, BASE_HEIGHT + MARGIN,
                                                       BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = base.createGraphics();
                g.translate(0, HALF_HEIGHT + MARGIN);

                g.setPaint(texture);
                String counter = "";
                boolean[] branches = new boolean[4];
                if (index > 0) {
                    for (int i = 0; i < POWERS_OF_TWO.length; i++) {
                        if ((index & POWERS_OF_TWO[i]) == POWERS_OF_TWO[i]) {
                            branches[i] = true;
                            counter += "1";
                        } else {
                            counter += "0";
                        }
                    }
                }

                // the two vectors that describe the diamond
                Point v1 = new Point(HALF_WIDTH, HALF_HEIGHT);
                Point v2 = new Point(HALF_WIDTH, -HALF_HEIGHT);
                // make space for rivers
                if (branches[0] || branches[2]) {
                    v1.translate(-16, -8);
                }
                if (branches[1] || branches[3]) {
                    v2.translate(-16, 8);
                }

                if (drawBorders) {
                    int dx = v1.x + v2.x;
                    int dy = v1.y + v2.y;
                    g.setColor(Color.RED);
                    g.drawLine(0, 0, v1.x, v1.y);
                    g.drawLine(v1.x, v1.y, dx, dy);
                    g.drawLine(dx, dy, v2.x, v2.y);
                    g.drawLine(v2.x, v2.y, 0, 0);
                }

                if (drawRivers) {
                    g.setPaint(texture);
                    g.setStroke(new BasicStroke(6));
                    for (int r = 0; r < 2; r++) {
                        int i = r;
                        int j = r + 2;
                        if (branches[i] && branches[j]) {
                            // do nothing
                        } else if (branches[i]) {
                            j = r + 4;
                        } else if (branches[j]) {
                            i = r + 4;
                        } else {
                            continue;
                        }
                        g.drawLine(p[i].x, p[i].y, p[j].x, p[j].y);
                    }
                }

                if (drawTrees) {
                    List<ImageLocation> trees = new ArrayList<ImageLocation>(TREES);
                    // reduce number of trees if river branches are present
                    int numberOfTrees = (8 - Integer.bitCount(index)) * TREES / 8;

                    int count = 0;
                    while (count < numberOfTrees) {
                        BufferedImage image = images.get(random.nextInt(numberOfImages));
                        int width = image.getWidth();
                        int height = image.getHeight();
                        int halfWidth = width / 2;
                        /**
                         * Find a point for the root of the tree, that
                         * is the center of the lower edge of the tree
                         * image.
                         */
                        float a = random.nextFloat();
                        float b = random.nextFloat();
                        int x = (int) (a * v1.x + b * v2.x);
                        int y = (int) (a * v1.y + b * v2.y);
                        /**
                         * Additional constraint: the left and right
                         * edges of the tree image must be within the
                         * tile bounds (this will fail if the tree
                         * image is too large).
                         */
                        int dx = x - halfWidth;
                        if (dx < 0) {
                            x += -dx;
                        }
                        dx = x + halfWidth - BASE_WIDTH;
                        if (dx > 0) {
                            x += -dx;
                        }
                        /**
                         * Additional constraint: the top edge of the
                         * tree image must be within the tile bounds.
                         */
                        int crown = y - height;
                        int dy = (HALF_HEIGHT + MARGIN) - crown;
                        if (dy < 0) {
                            y += -dy;
                        }
                        /**
                         * Additional constraint: if there is a river
                         * in the Northwest or Southeast, then the
                         * crown of the tree, i.e. the center of the
                         * top edge of the tree image, must not be
                         * within the top right triangle of the tile
                         * image.
                         */
                        if (!((branches[1] || branches[3])
                              && crown < -BASE_HEIGHT + 8 + (x + halfWidth) / 2)) {
                            //System.out.println("x=" + x + ", y=" + (y - height));
                            trees.add(new ImageLocation(image, x - halfWidth, crown));
                            count++;
                        }
                    }

                    // sort by y, x coordinate
                    Collections.sort(trees);
                    for (ImageLocation imageLocation : trees) {
                        g.drawImage(imageLocation.image, imageLocation.x, imageLocation.y, null);
                    }

                }

                ImageIO.write(base, "png", new File(destinationDirectory,
                                                    sourceDirectory.getName() + counter + ".png"));

            }
        }
    }

    private static int getY(int x, int y, double slope, int newX) {
        return (int) (y + slope * (newX - x));
    }

    private static int getRandomY(Random random, int x) {
        int height = HALF_HEIGHT - Math.abs(x) / 2;
        return (height == 0) ? 0 : random.nextInt(2 * height) - height;
    }


}

