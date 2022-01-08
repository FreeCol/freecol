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

    private Rectangle dirtyRegion = null;
    private Tile focus = null;
    private Point focusPoint = null;
    private boolean repaintsBlocked = false;


    MapViewerRepaintManager() {

    }


    /**
     * Prepares buffers for rendering.
     * 
     * @param mapViewerBounds The bounds used when determining the size of the
     *      buffers, and also for checking if the focus has changed.
     * @return {@code true} if the buffers have been reset -- meaning prior content has
     *      been lost.
     */
    boolean prepareBuffers(MapViewerBounds mapViewerBounds, Tile focus) {
        final Dimension size = mapViewerBounds.getSize();

        final Tile oldFocus = this.focus;
        final Point oldFocusPoint = this.focusPoint;

        this.focus = focus;
        this.focusPoint = mapViewerBounds.tileToPoint(focus);
        this.repaintsBlocked = false;

        if (isBuffersUninitialized(size)) {
            initializeBuffers(size);
            markAsDirty();
            return true;
        }

        if (isAllDirty()) {
            return false;
        }

        if (oldFocus == null) {
            markAsDirty();
            return false;
        }
        
        if (!oldFocus.equals(focus)) {
            reuseNonDirtyAreasIfPossible(mapViewerBounds, oldFocus, oldFocusPoint);
        }
        
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
     */
    Rectangle getDirtyClipBounds() {
        return dirtyRegion;
    }

    /**
     * Check if the buffers for the entire map viewer is dirty.
     * 
     * @return {@code true} if no part of the buffer can be reused the next
     *      time the map gets painted.
     */
    boolean isAllDirty() {
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
     * @param bounds The bounds that should be marked dirty.
     */
    public void markAsDirty(Rectangle bounds) {
        if (dirtyRegion == null) {
            this.dirtyRegion = new Rectangle(bounds);
            return;
        }
        this.dirtyRegion = this.dirtyRegion.union(bounds);
    }

    /**
     * Marks the entire area of all buffers as dirty. Please avoid calling this
     * method as it causes full repaints.
     */
    public void markAsDirty() {
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
        this.dirtyRegion = null;
    }

    /**
     * If available, a buffer that contains the entire map.
     */
    VolatileImage getBackBufferImage() {
        return backBufferImage;
    }

    /**
     * If available, a buffer that contains the non-animated parts
     * of the map.
     */
    BufferedImage getNonAnimationBufferImage() {
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
        this.repaintsBlocked = repaintsBlocked;
    }

    /**
     * Checks if repaints are temporarily served using only the
     * back buffer.
     * 
     * @see #setRepaintsBlocked(boolean)
     */
    boolean isRepaintsBlocked(Dimension size) {
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
     */
    private void reuseNonDirtyAreasIfPossible(
            final MapViewerBounds mapViewerBounds,
            final Tile oldFocus,
            final Point oldFocusPoint) {
        final Dimension size = mapViewerBounds.getSize();
        final Point repositionedOldFocusPoint = mapViewerBounds.tileToPoint(oldFocus);

        final int dx = repositionedOldFocusPoint.x - oldFocusPoint.x;
        final int dy = repositionedOldFocusPoint.y - oldFocusPoint.y;
        updateDirtyRegion(size, dx, dy);

        if (!isAllDirty()) {
            moveContents(backBufferImage, dx, dy);
            nonAnimationBufferImage = moveContentsAndRecreateImage(nonAnimationBufferImage, dx, dy);
        }
    }

    /**
     * Updates the dirty region by moving it in the specified direction.
     * 
     * @param size The size of the MapViewer/buffers.
     * @param dx The number of pixels to move the contents of the buffers
     *      for the x coordinate. The value can be negative.
     * @param dy The number of pixels to move the contents of the buffers
     *      for the y coordinate. The value can be negative.
     */
    private void updateDirtyRegion(final Dimension size, final int dx, final int dy) {
        if (dirtyRegion != null) {
            dirtyRegion.translate(dx, dy);
        }
        
        final Rectangle newBounds = new Rectangle(0, 0, size.width, size.height);
        newBounds.translate(dx, dy);
        
        final Area dirtyArea = new Area(new Rectangle(0, 0, size.width, size.height));
        dirtyArea.subtract(new Area(newBounds));
        
        if (dirtyRegion != null) {
            dirtyRegion = dirtyRegion.union(dirtyArea.getBounds());
        } else {
            dirtyRegion = dirtyArea.getBounds();
        }
    }

    /**
     * Move the content of an opaque {@code Image}.
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
}
