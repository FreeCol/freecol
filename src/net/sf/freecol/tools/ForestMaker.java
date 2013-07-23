/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

    private static int BASE_WIDTH = 128;
    private static int BASE_HEIGHT = 64;
    private static int MARGIN = 20;
    private static int TREES = 40;
    private static int RIVER_MARGIN = 4;

    private static int HALF_WIDTH = BASE_WIDTH / 2;
    private static int HALF_HEIGHT = BASE_HEIGHT / 2;

    private static int[] X = new int[] {
        BASE_WIDTH, BASE_WIDTH, 0, 0
    };

    private static double[] SLOPE = new double[] {
        -0.5, 0.5, -0.5, 0.5
    };

    private static final int[] POWERS_OF_TWO
        = new int[] { 1, 2, 4, 8 };


    private static boolean drawBorder = true;
    private static boolean drawRiver = true;


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
            File[] imageFiles = sourceDirectory.listFiles();
            if (imageFiles == null) {
                System.out.println("No images found in source directory " + arg + ".");
                continue;
            } else {
                System.out.println(imageFiles.length + " images found in source directory "
                                   + arg + ".");
            }
            List<BufferedImage> images = new ArrayList<BufferedImage>(imageFiles.length);
            int minimumHeight = Integer.MAX_VALUE;
            for (File imageFile : imageFiles) {
                if (imageFile.isFile() && imageFile.canRead()) {
                    try {
                        BufferedImage image = ImageIO.read(imageFile);
                        images.add(image);
                        if (image.getHeight() < minimumHeight) {
                            minimumHeight = image.getHeight();
                        }
                    } catch(Exception e) {
                        System.out.println("Unable to load image " + imageFile.getName() + ":\n");
                        e.printStackTrace();
                    }
                }
            }
            int numberOfImages = images.size();
            Random random = new Random(1492);

            // the tile itself
            int[] x_coords = new int[] {
                HALF_WIDTH, BASE_WIDTH, HALF_WIDTH, 0
            };
            int[] y_coords = new int[] {
                0, HALF_HEIGHT, BASE_HEIGHT, HALF_HEIGHT
            };
            Polygon diamond = new Polygon(x_coords, y_coords, 4);
            diamond.translate(0, MARGIN);

            Polygon[] polygons = getPolygons(RIVER_MARGIN);
            for (int index = 0; index < 16; index++) {
                BufferedImage base = new BufferedImage(BASE_WIDTH, BASE_HEIGHT + MARGIN,
                                                       BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = base.createGraphics();
                if (drawBorder) {
                    g.setColor(Color.RED);
                    g.draw(diamond);
                }

                List<ImageLocation> trees = new ArrayList<ImageLocation>(TREES);
                int count = 0;
                // reduce number of trees if branches are present
                int numberOfTrees = (8 - Integer.bitCount(index)) * TREES / 8;
                trees: while (count < numberOfTrees) {
                    int x = random.nextInt(BASE_WIDTH);
                    int y = random.nextInt(BASE_HEIGHT + MARGIN);
                    BufferedImage image = images.get(count % numberOfImages);
                    int width = image.getWidth();
                    int height = image.getHeight();
                    if (diamond.contains(x + width/2, y + height)
                        && x + width < BASE_WIDTH) {
                        for (int i = 0; i < POWERS_OF_TWO.length; i++) {
                            if ((index & POWERS_OF_TWO[i]) == POWERS_OF_TWO[i]
                                && polygons[i].intersects(x, y, width, height)) {
                                continue trees;
                            }
                        }
                        trees.add(new ImageLocation(image, x, y));
                        count++;
                    }
                }

                // sort by y, x coordinate
                Collections.sort(trees);
                for (ImageLocation imageLocation : trees) {
                    g.drawImage(imageLocation.image, imageLocation.x, imageLocation.y, null);
                }

                g.setPaint(texture);
                g.setStroke(new BasicStroke(6));
                String counter = "";
                if (index > 0) {
                    for (int i = 0; i < POWERS_OF_TWO.length; i++) {
                        if ((index & POWERS_OF_TWO[i]) == POWERS_OF_TWO[i]) {
                            counter += "1";
                            if (drawRiver) {
                                g.drawLine(HALF_WIDTH, HALF_HEIGHT + MARGIN, X[i],
                                           getY(HALF_WIDTH, HALF_HEIGHT + MARGIN, SLOPE[i], X[i]));
                            }
                        } else {
                            counter += "0";
                        }
                    }
                }
                ImageIO.write(base, "png", new File(sourceDirectory.getName() + counter + ".png"));

            }
        }
    }

    private static int getY(int x, int y, double slope, int newX) {
        return (int) (y + slope * (newX - x));
    }

    private static Polygon[] getPolygons(int height) {
        int width = 2 * height;
        Polygon[] result = new Polygon[4];
        int[] x = new int[4];
        int[] y = new int[4];
        // north-east
        x[0] = HALF_WIDTH - width;
        y[0] = HALF_HEIGHT;
        x[1] = HALF_WIDTH;
        y[1] = HALF_HEIGHT + height;
        x[2] = BASE_WIDTH;
        y[2] = getY(x[1], y[1], SLOPE[0], X[0]);
        x[3] = BASE_WIDTH;
        y[3] = getY(x[0], y[0], SLOPE[0], X[0]);
        result[0] = new Polygon(x, y, 4);
        result[0].translate(0, MARGIN);
        // south-east
        x[0] = HALF_WIDTH - width;
        y[0] = HALF_HEIGHT;
        x[1] = HALF_WIDTH;
        y[1] = HALF_HEIGHT - height;
        x[2] = BASE_WIDTH;
        y[2] = getY(x[1], y[1], SLOPE[1], X[1]);
        x[3] = BASE_WIDTH;
        y[3] = getY(x[0], y[0], SLOPE[1], X[1]);
        result[1] = new Polygon(x, y, 4);
        result[1].translate(0, MARGIN);
        // south-west
        x[0] = HALF_WIDTH;
        y[0] = HALF_HEIGHT - height;
        x[1] = HALF_WIDTH + width;
        y[1] = HALF_HEIGHT;
        x[2] = 0;
        y[2] = getY(x[1], y[1], SLOPE[2], X[2]);
        x[3] = 0;
        y[3] = getY(x[0], y[0], SLOPE[2], X[2]);
        result[2] = new Polygon(x, y, 4);
        result[2].translate(0, MARGIN);
        // north-west
        x[0] = HALF_WIDTH + width;
        y[0] = HALF_HEIGHT;
        x[1] = HALF_WIDTH;
        y[1] = HALF_HEIGHT + height;
        x[2] = 0;
        y[2] = getY(x[1], y[1], SLOPE[3], X[3]);
        x[3] = 0;
        y[3] = getY(x[0], y[0], SLOPE[3], X[3]);
        result[3] = new Polygon(x, y, 4);
        result[3].translate(0, MARGIN);

        return result;
    }
}

