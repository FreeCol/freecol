/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.mapviewer.TileViewer;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.resources.ImageCache;
import net.sf.freecol.util.test.FreeColTestCase;

/** Tests the tile-yield overlay and its saved display setting. */
public class TileYieldsTest extends FreeColTestCase {

    /** Unexplored tiles must not expose potential production. */
    public void testUnexploredTile() {
        Tile tile = new Tile(getGame(), null, 0, 0);
        assertEquals(0, paintedPixels(render(tile, 1f)));
    }

    /** The overlay remains inside the tile and preserves the graphics state. */
    public void testExploredTileAtDifferentScales() {
        Tile tile = new Tile(getGame(), spec().getTileType("model.tile.plains"), 0, 0);
        for (float scale : new float[] { 0.5f, 1f, 2f }) {
            BufferedImage image = render(tile, scale);
            assertTrue(paintedPixels(image) > 0);
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0, image.getRGB(x, 0));
                assertEquals(0, image.getRGB(x, image.getHeight() - 1));
            }
            for (int y = 0; y < image.getHeight(); y++) {
                assertEquals(0, image.getRGB(0, y));
                assertEquals(0, image.getRGB(image.getWidth() - 1, y));
            }
        }
    }

    /** Old preferences acquire a disabled option; enabled values survive reload. */
    public void testPreferenceMigrationAndPersistence() throws Exception {
        ClientOptions options = new ClientOptions();
        assertTrue(options.load(new File("data/base/client-options.xml")));
        options.remove(ClientOptions.DISPLAY_TILE_YIELDS);
        options.fixClientOptions();
        assertFalse(options.getBoolean(ClientOptions.DISPLAY_TILE_YIELDS));
        options.setBoolean(ClientOptions.DISPLAY_TILE_YIELDS, true);
        options.fixClientOptions();
        assertTrue(options.getBoolean(ClientOptions.DISPLAY_TILE_YIELDS));
        File file = Files.createTempFile("freecol-tile-yields", ".xml").toFile();
        try {
            assertTrue(options.save(file));
            ClientOptions restored = new ClientOptions();
            assertTrue(restored.load(file));
            restored.fixClientOptions();
            assertTrue(restored.getBoolean(ClientOptions.DISPLAY_TILE_YIELDS));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    /** Render with in-memory icons so the test needs no graphics resources. */
    private BufferedImage render(Tile tile, float scale) {
        FontLibrary.createMainFont(Font.SANS_SERIF);
        ImageCache cache = new ImageCache() {
            @Override
            public BufferedImage getSizedImage(String key, Dimension size, boolean grayscale) {
                return new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
            }
        };
        ImageLibrary library = new ImageLibrary(scale, cache);
        Dimension size = library.getTileSize();
        BufferedImage image = new BufferedImage(size.width, size.height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            Font font = graphics.getFont();
            new TileViewer(null, library).displayTileYields(graphics, tile);
            assertEquals(Color.RED, graphics.getColor());
            assertEquals(font, graphics.getFont());
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** Count non-transparent pixels in the overlay. */
    private int paintedPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) count++;
            }
        }
        return count;
    }
}
