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

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;


/**
 * Generate forest tiles.
 */
public class ForestMaker {

    private static final String DESTDIR = "data/rules/classic/resources/images/forest";

    private static final int BASE_WIDTH = 128;
    private static final int BASE_HEIGHT = 64;
    private static final int MARGIN = 20;
    private static final int TREES = 60;
    private static final int RIVER_HEIGHT = 8;
    private static final int RIVER_WIDTH = 2 * RIVER_HEIGHT;

    private static final int HALF_WIDTH = BASE_WIDTH / 2;
    private static final int HALF_HEIGHT = BASE_HEIGHT / 2;

    private static final int[] LIMIT = {
        HALF_WIDTH, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH
    };

    private static final double[] SLOPE = {
        -0.5, 0.5, -0.5, 0.5
    };

    private static final int[] POWERS_OF_TWO
        = { 1, 2, 4, 8 };


    private static final boolean drawBorders = true;
    private static final boolean drawTrees = true;


    private static class ImageLocation implements Comparable<ImageLocation> {

        final BufferedImage image;
        final int x;
        final int y;


        public ImageLocation(BufferedImage image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }

        // Implement Comparable<ImageLocation>

        @Override
        public int compareTo(ImageLocation other) {
            int dy = other.y - this.y;
            return (dy == 0) ? other.x - this.x : dy;
        }

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object other) {
            if (other instanceof ImageLocation) {
                return this.compareTo((ImageLocation)other) == 0;
            }
            return super.equals(other);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 37 * hash + x;
            hash = 37 * hash + y;
            return 37 * hash + image.hashCode();
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
            List<BufferedImage> images = new ArrayList<>(imageFiles.length);
            int maximumHeight = 0;
            for (File imageFile : imageFiles) {
                if (imageFile.isFile() && imageFile.canRead()) {
                    try {
                        BufferedImage image = ImageIO.read(imageFile);
                        images.add(image);
                        if (image.getHeight() > maximumHeight) {
                            maximumHeight = image.getHeight();
                        }
                    } catch(IOException e) {
                        System.out.println("Unable to load image " + imageFile.getName() + ":\n");
                        e.printStackTrace();
                    }
                }
            }
            int numberOfImages = images.size();
            Random random = new Random(1492);

            /**
             * In order to ensure that trees do not occlude the rivers
             * on other tiles, we must move the rivers to the top NE
             * and NW edges of the tile.
             *
             * If we consider two adjoining edges of the diamond to be
             * vectors, any point within the diamond can be generated
             * as the addition of these two vectors, suitably scaled.
             * For the sake of convenience, we choose the two edges
             * that will be shortened if a river is present, i.e. the
             * SE and SW edges, and move the origin to their
             * intersection.
             */
            for (int index = 0; index < 16; index++) {
                BufferedImage base = new BufferedImage(BASE_WIDTH, BASE_HEIGHT + MARGIN,
                                                       BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = base.createGraphics();
                g.translate(HALF_WIDTH, BASE_HEIGHT + MARGIN);

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
                Point right = new Point(HALF_WIDTH, -HALF_HEIGHT);
                Point left = new Point(-HALF_WIDTH, -HALF_HEIGHT);
                int treeCount = 0;
                // make space for rivers
                if (branches[0] || branches[2]) {
                    left.translate(RIVER_WIDTH, RIVER_HEIGHT);
                    treeCount++;
                }
                if (branches[1] || branches[3]) {
                    right.translate(-RIVER_WIDTH, RIVER_HEIGHT);
                    treeCount++;
                }

                if (drawBorders) {
                    int dx = right.x + left.x;
                    int dy = right.y + left.y;
                    g.setColor(Color.RED);
                    g.drawLine(0, 0, right.x, right.y);
                    g.drawLine(right.x, right.y, dx, dy);
                    g.drawLine(dx, dy, left.x, left.y);
                    g.drawLine(left.x, left.y, 0, 0);
                }

                if (drawTrees) {
                    List<ImageLocation> trees = new ArrayList<>(TREES);
                    // reduce number of trees if river branches are present
                    int numberOfTrees = (6 - treeCount) * TREES / 6;

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
                        int x = (int) (a * right.x + b * left.x);
                        int y = (int) (a * right.y + b * left.y);
                        /**
                         * Additional constraint: the left and right
                         * edges of the tree image must be within the
                         * tile bounds (this will fail if the tree
                         * image is too large).
                         */
                        if (x - halfWidth < - HALF_WIDTH) {
                            x = -HALF_WIDTH + halfWidth; // left
                        }
                        if (x + halfWidth > HALF_WIDTH) {
                            x = HALF_WIDTH - halfWidth; // right
                        }
                        /**
                         * Additional constraint: the top edge of the
                         * tree image must be within the tile bounds.
                         */
                        int crown = Math.max(y - height, -(BASE_HEIGHT + MARGIN));
                        /**
                         * Additional constraint: if there is a river
                         * along the top right edge of the diamond,
                         * the top right corner of the tree most not be
                         * "above" the line defined by that edge.
                         */
                        if ((branches[1] || branches[3])
                            && crown < -BASE_HEIGHT + RIVER_HEIGHT + (x + halfWidth) / 2) {
                            continue;
                        }
                        /**
                         * Additional constraint: if there is a river
                         * along the top left edge of the diamond,
                         * the top left corner of the tree most not be
                         * "above" the line defined by that edge.
                         */
                        if ((branches[0] || branches[2])
                            && crown < -BASE_HEIGHT + RIVER_HEIGHT - (x - halfWidth) / 2) {
                            continue;
                        }
                        //System.out.println("x=" + x + ", y=" + (y - height));
                        trees.add(new ImageLocation(image, x - halfWidth, crown));
                        count++;
                    }

                    // sort by y, x coordinate
                    Collections.sort(trees);
                    for (ImageLocation imageLocation : trees) {
                        g.drawImage(imageLocation.image, imageLocation.x, imageLocation.y, null);
                    }

                }
                g.dispose();

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

