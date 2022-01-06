package net.sf.freecol.client.gui.mapviewer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

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
    
    MapViewerRepaintManager() {
        
    }
    
    
    /**
     * Prepares buffers for rendering.
     * 
     * @param size The size of the buffers, that is the current size of {@code MapViewer}.
     * @return {@code true} if the buffers have been reset -- meaning prior content has
     *      been lost.
     */
    boolean prepareBuffers(Dimension size, Tile focus) {
        final boolean focusHasChanged = (this.focus == null) || !this.focus.equals(focus);
        if (focusHasChanged) {
            markAsDirty();
        }
        
        this.focus = focus;
        
        if (isBuffersUninitialized(size)) {
            backBufferImage = Utils.getGoodGraphicsDevice()
                    .getDefaultConfiguration()
                    .createCompatibleVolatileImage(size.width, size.height, Transparency.OPAQUE);
            nonAnimationBufferImage = Utils.getGoodGraphicsDevice()
                    .getDefaultConfiguration()
                    .createCompatibleImage(size.width, size.height, Transparency.TRANSLUCENT);
            
            markAsDirty();
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the clip bounds that should be fully redrawn.
     */
    Rectangle getDirtyClipBounds() {
        return dirtyRegion;
    }
    
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
     * method as it caused full repaints.
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
    
    
    private boolean isBuffersUninitialized(Dimension size) {
        return backBufferImage == null
                || backBufferImage.getWidth() != size.width
                || backBufferImage.getHeight() != size.height;
    }
}
