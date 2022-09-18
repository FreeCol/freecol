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

package net.sf.freecol.tools;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.sf.freecol.common.util.ImageUtils;


/**
 * Utility for making a seamless tilable 512x256 tile from a seamless tilable texture.
 * 
 * Recommended texture size (the input image) is either 512x512 or 256x128. Other sizes
 * will be seamlessly rescaled with interpolation.
 */
public class Texture2Tile {
    
    private static final int RESULT_WIDTH = 512;
    private static final int RESULT_HEIGHT = 256;

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printUsage();
            System.exit(0);
        }
        
        final File inputImageFile = new File(args[0]);
        final BufferedImage inputImage = ImageIO.read(inputImageFile);
        
        // Preferred input image of size 512x512:
        createRotatedResult(inputImage);
        
        // Preferred input image of size 256x128:
        createNonRotatedResult(inputImage);
    }


    private static void printUsage() {
        System.out.println("Creates an isometric base tile image from a tiling texture.");
        System.out.println();
        System.out.println("Usage: java -cp FreeCol.jar net.sf.freecol.tools.Texture2Tile IMAGE_FILE");
        System.out.println();
        System.out.println("IMAGE_FILE  - A seamlessly tiling texture to be used for creating a seamlessly");
        System.out.println("              tiling isometric base tile.");
        System.out.println();
        System.out.println("Two results are produced by this tool: \"generated.png\" and \"generated-rotated.png\"");
        System.out.println("The best one to use depends on the texture.");
    }


    private static void createRotatedResult(BufferedImage inputImage) throws IOException {
        final File resultImagefile = new File("generated-rotated.png");
        final BufferedImage tileMaskImage = ImageIO.read(new File("data/base/resources/images/masks/mask-center.size9.png"));
        
        BufferedImage tempImage = tileThreeByThreeAndRescaledTo(inputImage, RESULT_WIDTH, RESULT_HEIGHT * 2);
        tempImage = createRotatedImage(tempImage, 45);
        tempImage = ImageUtils.createResizedImage(tempImage, RESULT_WIDTH * 3, RESULT_HEIGHT * 3, true);
        tempImage = tempImage.getSubimage(
                (tempImage.getWidth() - RESULT_WIDTH) / 2,
                (tempImage.getHeight() - RESULT_HEIGHT) / 2,
                RESULT_WIDTH,
                RESULT_HEIGHT);
        tempImage = ImageUtils.imageWithAlphaFromMask(tempImage, tileMaskImage);
        
        ImageIO.write(tempImage, "png", resultImagefile);
        System.out.println("Image written to: " + resultImagefile.getAbsolutePath().toString());
    }


    private static void createNonRotatedResult(BufferedImage inputImage) throws Exception, IOException {
        final File resultImagefile = new File("generated.png");
        
        final int quarterTileWidth = RESULT_WIDTH / 2;
        final int quarterTileHeight = RESULT_HEIGHT / 2;
        
        inputImage = seamlessRescaleTextureIfNeeded(inputImage, quarterTileWidth, quarterTileHeight);
                
        final BufferedImage imageA = extractQuarterTileA(inputImage, quarterTileWidth, quarterTileHeight);
        final BufferedImage imageB = extractQuarterTileB(inputImage, quarterTileWidth, quarterTileHeight);
        final BufferedImage resultImage = combineQuarterTiles(imageA, imageB);
        
        ImageIO.write(resultImage, "png", resultImagefile);
        System.out.println("Image written to: " + resultImagefile.getAbsolutePath().toString());
    }


    private static BufferedImage seamlessRescaleTextureIfNeeded(BufferedImage inputImage, final int quarterTileWidth, final int quarterTileHeight) {
        if (inputImage.getWidth() == quarterTileWidth && inputImage.getHeight() == quarterTileHeight) {
            return inputImage;
        }
        
        if (closerToCorrectAspectWithoutDoublingHorizontally(inputImage)) {
            return seamlessRescaleTexture(inputImage, quarterTileWidth, quarterTileHeight);
        }
        
        final BufferedImage tempImage = createImageWithTextureFill(inputImage, inputImage.getWidth() * 2, inputImage.getHeight());       
        if (tempImage.getWidth() == quarterTileWidth && tempImage.getHeight() == quarterTileHeight) {
            return tempImage;
        }
        
        return seamlessRescaleTexture(tempImage, quarterTileWidth, quarterTileHeight);
    }


    private static boolean closerToCorrectAspectWithoutDoublingHorizontally(BufferedImage inputImage) {
        return Math.abs(1 - 2*inputImage.getHeight()/inputImage.getWidth()) <= Math.abs(1 - inputImage.getHeight()/inputImage.getWidth());
    }


    private static BufferedImage combineQuarterTiles(BufferedImage imageA, BufferedImage imageB) {
        final int quarterTileWidth = imageA.getWidth();
        final int quarterTileHeight = imageA.getHeight();
        final int resultWidth = quarterTileWidth * 2;
        final int resultHeight = quarterTileHeight * 2;
        
        final BufferedImage resultImage = new BufferedImage(resultWidth, resultHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gResult = resultImage.createGraphics();
        
        final int centerHorizontally = resultWidth / 2 - quarterTileWidth / 2;
        gResult.drawImage(imageA, centerHorizontally, 0, null);
        gResult.drawImage(imageA, centerHorizontally, resultHeight - quarterTileHeight, null);
        
        final int centerVertically = resultHeight / 2 - quarterTileHeight / 2;
        gResult.drawImage(imageB, resultWidth / 4 - quarterTileWidth / 2, centerVertically, null);
        gResult.drawImage(imageB, (3 * resultWidth) / 4 - quarterTileWidth / 2, centerVertically, null);
        gResult.dispose();
        
        return resultImage;
    }
    
    /**
     * Creates a new image that is the rotated input image.
     * 
     * Warning: This method has only been tested using 45 degrees and an image
     *          with equal width and height.
     * 
     * @param image The image to be rotated.
     * @param degrees The degrees. 
     * @return An image that is rescaled to cover the entire rotated image.
     */
    public static BufferedImage createRotatedImage(BufferedImage image, int degrees) {
        final int rotatedSize = (int) Math.sqrt(image.getWidth() * image.getWidth() + image.getHeight() * image.getHeight());
        final BufferedImage result = new BufferedImage(rotatedSize, rotatedSize, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = result.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.rotate(Math.toRadians(degrees), result.getWidth() / 2.0D, result.getHeight() / 2.0D);
        
        final int x = (result.getWidth() - image.getWidth()) / 2;
        final int y = (result.getHeight() - image.getHeight()) / 2;
        g2d.drawImage(image, x, y, null);
        g2d.dispose();
        return result;
    }
    
    private static BufferedImage seamlessRescaleTexture(BufferedImage im, int width, int height) {
        /*
         * We ensure seamless tiling after scaling by repeating the image before
         * rescaling, and then retriving the center part.
         * 
         * I am using the image three times in every direction although only a
         * few pixels around the center are really needed. This makes the code
         * easier to read :-)
         */
        final BufferedImage tempImage = tileThreeByThreeAndRescaledTo(im, width, height);
        return tempImage.getSubimage(width, height, width, height);
    }
    
    /**
     * Returns an image drawn in a 3x3 grid.
     * 
     * @param im The image to be drawn.
     * @param width The width of each image in the grid.
     * @param height The height of each image in the grid.
     * @return The provided image rescaled and drawn in a 3x3 grid so that
     *      the center image in the grid avoids having edge artifacts from
     *      the scaling.
     */
    private static BufferedImage tileThreeByThreeAndRescaledTo(BufferedImage im, int width, int height) {
        final BufferedImage tempImage = createImageWithTextureFill(im, im.getWidth()*3, im.getHeight()*3);
        final BufferedImage tempImage2 = ImageUtils.createResizedImage(tempImage, width*3, height*3, true);
        return tempImage2;
    }
    
    private static BufferedImage createImageWithTextureFill(BufferedImage im, int width, int height) {
        final BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = resultImage.createGraphics();
        ImageUtils.fillTexture(g, im, 0, 0, resultImage.getWidth(), resultImage.getHeight());
        g.dispose();
        
        return resultImage;
    }
    
    private static BufferedImage extractQuarterTileA(BufferedImage inputImage, int quarterTileWidth, int quarterTileHeight) throws Exception {
        final BufferedImage imN = imageWithMaskApplied(inputImage, "n");
        final BufferedImage imS = imageWithMaskApplied(inputImage, "s");
        final BufferedImage imageA = new BufferedImage(quarterTileWidth, quarterTileHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gA = imageA.createGraphics();
        gA.drawImage(imS.getSubimage(0, quarterTileHeight / 2, quarterTileWidth, quarterTileHeight / 2), 0, 0, null);
        gA.drawImage(imN.getSubimage(0, 0, quarterTileWidth, quarterTileHeight / 2), 0, quarterTileHeight / 2, null);
        gA.dispose();
        return imageA;
    }
    
    private static BufferedImage extractQuarterTileB(BufferedImage inputImage, int quarterTileWidth, int quarterTileHeight) throws Exception {
        final BufferedImage imE = imageWithMaskApplied(inputImage, "e");
        final BufferedImage imW = imageWithMaskApplied(inputImage, "w");
        final BufferedImage imageB = new BufferedImage(quarterTileWidth, quarterTileHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gB = imageB.createGraphics();
        gB.drawImage(imE.getSubimage(quarterTileWidth / 2, 0, quarterTileWidth / 2, quarterTileHeight), 0, 0, null);
        gB.drawImage(imW.getSubimage(0, 0, quarterTileWidth / 2, quarterTileHeight), quarterTileWidth / 2, 0, null);
        gB.dispose();
        return imageB;
    }
    

    private static BufferedImage imageWithMaskApplied(BufferedImage image, String variation) throws Exception {
        final String maskBaseName = "data/base/resources/images/masks/tools/mask-256x128-texture-";
        final BufferedImage mask = ImageIO.read(new File(maskBaseName + variation + ".png"));
        return ImageUtils.imageWithAlphaFromMask(image, mask); 
    }
}
