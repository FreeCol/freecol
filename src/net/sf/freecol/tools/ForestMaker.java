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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Polygon;
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
    private static int TREES = 60;

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

            // first, the base image
            BufferedImage base = new BufferedImage(BASE_WIDTH, BASE_HEIGHT + MARGIN,
                                                   BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = base.createGraphics();
            int[] x_coords = new int[] { 64, 127, 64, 0 };
            int[] y_coords = new int[] { 0, 32, 63, 32 };
            Polygon diamond = new Polygon(x_coords, y_coords, 4);
            diamond.translate(0, MARGIN);
            g.setColor(Color.RED);
            g.draw(diamond);

            int half = BASE_WIDTH / 2;

            List<ImageLocation> trees = new ArrayList<ImageLocation>(TREES);
            int count = 0;
            while (count < TREES) {
                int x = random.nextInt(BASE_WIDTH);
                int y = random.nextInt(BASE_HEIGHT + MARGIN);
                BufferedImage image = images.get(count % numberOfImages);
                if (diamond.contains(x, y + image.getHeight())
                    && diamond.contains(x + image.getWidth(), y + image.getHeight())) {
                    trees.add(new ImageLocation(image, x, y));
                    count++;
                }
            }

            // sort by y, x coordinate
            Collections.sort(trees);
            for (ImageLocation imageLocation : trees) {
                g.drawImage(imageLocation.image, imageLocation.x, imageLocation.y, null);
            }
            ImageIO.write(base, "png", new File(sourceDirectory.getName() + ".png"));

        }
    }



}

