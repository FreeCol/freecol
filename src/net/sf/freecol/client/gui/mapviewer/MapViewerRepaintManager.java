package net.sf.freecol.client.gui.mapviewer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.util.Utils;

/**
 * Internal class that handles buffers and dirty state of {@link MapViewer}.
 * 
 * Methods in this class should only be used by {@link SwingGUI}, {@link Canvas}
 * or {@link MapViewer}.
 */
public class MapViewerRepaintManager {

    private VolatileImage backBufferImage = null;
    private BufferedImage nonAnimationBufferImage = null;

    private Rectangle dirtyRegion = new Rectangle(0, 0, 0, 0);
    private Tile focus = null;
    private Point focusPoint = null;
    private boolean repaintsBlocked = false;
    private final Set<Tile> dirtyTiles = new HashSet<>();

    MapViewerRepaintManager() {

    }


    /**
     * Prepares buffers for rendering.
     * 
     * @param mapViewerBounds The bounds used when determining the size of the
     *      buffers, and also for checking if the focus has changed.
     * @param focus A new {@code Tile} to focus on.
     * @return {@code true} if the buffers have been reset -- meaning
     *      prior content has been lost.
     */
    boolean prepareBuffers(MapViewerBounds mapViewerBounds, Tile focus) {
        assert SwingUtilities.isEventDispatchThread();
        
        final Dimension size = mapViewerBounds.getSize();

        final Tile oldFocus = this.focus;
        final Point oldFocusPoint = this.focusPoint;

        this.focus = focus;
        this.focusPoint = mapViewerBounds.tileToPoint(focus);
        this.repaintsBlocked = false;

        if (isBuffersUninitialized(size)) {
            initializeBuffers(size);
            markAsDirty();
            dirtyTiles.clear();
            return true;
        }

        if (isAllDirty()) {
            dirtyTiles.clear();
            return false;
        }

        if (oldFocus == null) {
            markAsDirty();
            dirtyTiles.clear();
            return false;
        }
        
        updateDirtyRegionWithDirtyTiles(mapViewerBounds);
        
        reuseNonDirtyAreasIfPossible(mapViewerBounds, oldFocus, oldFocusPoint);
        
        return false;
    }


    private void initializeBuffers(final Dimension size) {
        backBufferImage = Utils.getGoodGraphicsDevice()
                .getDefaultConfiguration()
                .createCompatibleVolatileImage(size.width, size.height, Transparency.OPAQUE);
        nonAnimationBufferImage = Utils.getGoodGraphicsDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(size.width, size.height, Transparency.TRANSLUCENT);
    }

    /**
     * Gets the clip bounds that should be fully redrawn.
     *
     * @return The region to redraw.
     */
    Rectangle getDirtyClipBounds() {
        assert SwingUtilities.isEventDispatchThread();
        
        return dirtyRegion;
    }

    /**
     * Check if the buffers for the entire map viewer is dirty.
     * 
     * @return {@code true} if no part of the buffer can be reused the next
     *      time the map gets painted.
     */
    public boolean isAllDirty() {
        assert SwingUtilities.isEventDispatchThread();
        
        return backBufferImage == null
                || dirtyRegion != null
                && dirtyRegion.x == 0
                && dirtyRegion.y == 0
                && dirtyRegion.width == backBufferImage.getWidth()
                && dirtyRegion.height == backBufferImage.getHeight();
    }

    /**
     * Marks the given area as dirty for all layers.
     * 
     * This method should only be used if {@link paintImmediately}
     * is called both before and after.
     * 
     * @param bounds The bounds that should be marked dirty.
     */
    public void markAsDirty(Rectangle bounds) {
        assert SwingUtilities.isEventDispatchThread();
        
        if (dirtyRegion.isEmpty()) {
            this.dirtyRegion = bounds;
        } else {
            this.dirtyRegion = this.dirtyRegion.union(bounds);
        }
    }
    
    /**
     * Marks the given {@code Tile} as dirty for all layers.
     * 
     * @param dirtyTile The {@code Tile} that should be repainted.
     */
    public void markAsDirty(Tile dirtyTile) {
        assert SwingUtilities.isEventDispatchThread();
        
        Objects.requireNonNull(dirtyTile, "dirtyTile");
        this.dirtyTiles.add(dirtyTile);
    }
    
    /**
     * Marks the given {@code Tile}s as dirty for all layers.
     * 
     * @param dirtyTiles The {@code Tile}s that should be repainted.
     */
    public void markAsDirty(Collection<Tile> dirtyTiles) {
        assert SwingUtilities.isEventDispatchThread();
        
        this.dirtyTiles.addAll(dirtyTiles);
    }

    /**
     * Marks the entire area of all buffers as dirty. Please avoid calling this
     * method as it causes full repaints.
     */
    public void markAsDirty() {
        assert SwingUtilities.isEventDispatchThread();
        
        if (backBufferImage == null) {
            /* The dirtyRegion will be defined by the next call to prepareBuffers */
            this.dirtyRegion = null;
            return;
        }
        this.dirtyRegion = new Rectangle(0, 0, backBufferImage.getWidth(), backBufferImage.getHeight());
    }

    /**
     * Marks every buffer as clean.
     * 
     * WARNING: This method should only be called from {@code MapViewer} after painting
     *          has completed.
     */
    void markAsClean() {
        assert SwingUtilities.isEventDispatchThread();
        
        this.dirtyRegion = new Rectangle(0, 0, 0 ,0);
    }

    /**
     * If available, a buffer that contains the entire map.
     *
     * @return The map buffer.
     */
    VolatileImage getBackBufferImage() {
        assert SwingUtilities.isEventDispatchThread();
        
        return backBufferImage;
    }

    /**
     * If available, a buffer that contains the non-animated parts
     * of the map.
     *
     * @return The static map buffer.
     */
    BufferedImage getNonAnimationBufferImage() {
        assert SwingUtilities.isEventDispatchThread();
        
        return nonAnimationBufferImage;
    }

    /**
     * Stops the map viewer from being updated in the user interface
     * by using the backbuffer for repaint requests.
     * 
     * WARNING: Always secure that this value is set back to
     *          {@code false}, in reasonable time, and repaint the
     *          entire map after doing so.
     * 
     * Please note that a resized frame will still force a repaint.
     * 
     * @param repaintsBlocked {@code true} blocks new repaints while
     *      {@code false} allows repaints to be made again. 
     */
    public void setRepaintsBlocked(boolean repaintsBlocked) {
        assert SwingUtilities.isEventDispatchThread();
        
        this.repaintsBlocked = repaintsBlocked;
    }

    /**
     * Checks if repaints are temporarily served using only the
     * back buffer.
     * 
     * @see #setRepaintsBlocked(boolean)
     *
     * @param size A size to check for initialized buffers.
     * @return True if repaints are blocked.
     */
    boolean isRepaintsBlocked(Dimension size) {
        assert SwingUtilities.isEventDispatchThread();
        
        return repaintsBlocked && !isBuffersUninitialized(size);
    }

    private boolean isBuffersUninitialized(Dimension size) {
        return backBufferImage == null
                || backBufferImage.getWidth() != size.width
                || backBufferImage.getHeight() != size.height;
    }
    
    /**
     * Moves the contents of the buffers to a new location
     * after changing focus tile.
     *
     * @param mapViewerBounds The bounds defining the focus.
     * @param oldFocus The previous focus {@code Tile}.
     * @param oldFocusPoint The previous focus {@code Point}.
     */
    private void reuseNonDirtyAreasIfPossible(
            final MapViewerBounds mapViewerBounds,
            final Tile oldFocus,
            final Point oldFocusPoint) {
        final Point repositionedOldFocusPoint = mapViewerBounds.tileToPoint(oldFocus);

        final int dx = repositionedOldFocusPoint.x - oldFocusPoint.x;
        final int dy = repositionedOldFocusPoint.y - oldFocusPoint.y;
        updateDirtyRegion(mapViewerBounds, dx, dy);

        if (!isAllDirty()) {
            moveContents(backBufferImage, dx, dy);
            nonAnimationBufferImage = moveContentsAndRecreateImage(nonAnimationBufferImage, dx, dy);
        }
    }

    /**
     * Updates the dirty region by moving it in the specified direction.
     * 
     * @param mapViewerBounds The bounds defining the focus.
     * @param dx The number of pixels to move the contents of the buffers
     *      for the x coordinate. The value can be negative.
     * @param dy The number of pixels to move the contents of the buffers
     *      for the y coordinate. The value can be negative.
     */
    private void updateDirtyRegion(final MapViewerBounds mapViewerBounds,
                                   final int dx, final int dy) {
        final Dimension size = mapViewerBounds.getSize();
        
        final Rectangle alreadyPaintedBounds = new Rectangle(0, 0, size.width, size.height);
        alreadyPaintedBounds.translate(dx, dy);

        final Area dirtyArea = new Area(new Rectangle(0, 0, size.width, size.height));
        dirtyArea.subtract(new Area(alreadyPaintedBounds));
        final Rectangle newDirtyBounds = dirtyArea.getBounds();
        if (dirtyRegion != null && !dirtyRegion.isEmpty()) {
            dirtyRegion = dirtyRegion.union(newDirtyBounds);
        } else {
            dirtyRegion = newDirtyBounds;
        }
        
        dirtyRegion = dirtyRegion.intersection(new Rectangle(0, 0, size.width, size.height));
    }

    /**
     * Move the content of an opaque {@code Image}.
     *
     * @param image The {@code Image} to move.
     * @param dx The x-coordinate change.
     * @param dy The y-coordinate change.
     */
    private static void moveContents(Image image, final int dx, final int dy) {
        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.copyArea(0, 0, image.getWidth(null), image.getHeight(null), dx, dy);
        g2d.dispose();
    }

    /**
     * Move the content of a transparent {@code BufferedImage} by creating a
     * new image.
     * 
     * Please use the more efficient {@link #moveContents(Image, int, int)}
     * for opaque images.
     *
     * @param image The {@code Image} to move.
     * @param dx The x-coordinate change.
     * @param dy The y-coordinate change.
     * @return The new image.
     */
    private static BufferedImage moveContentsAndRecreateImage(BufferedImage image, final int dx, final int dy) {
        final BufferedImage result = Utils.getGoodGraphicsDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
        final Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, dx, dy, null);
        g2d.dispose();
        return result;
    }
    
    private void updateDirtyRegionWithDirtyTiles(MapViewerBounds mapViewerBounds) {
        for (Tile dirtyTile : dirtyTiles) {
            final Rectangle dirtyTileBounds = mapViewerBounds.calculateDrawnTileBounds(dirtyTile);
            if (dirtyRegion.isEmpty()) {
                dirtyRegion = dirtyTileBounds;
            } else {
                dirtyRegion = dirtyRegion.union(dirtyTileBounds);
            }
        }
        dirtyTiles.clear();
    }
}
