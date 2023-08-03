package net.sf.freecol.client.gui.mapviewer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.util.Utils;

/**
 * Supports asynchronous painting to a buffer. This allows frames to be rendered
 * in parallel with the GUI thread (that is, the "EDT" - Event Dispatching Thread).
 * 
 * Asynchronous painting is needed for smooth scrolling, but it can in the future
 * also be used if we start getting lots of animated content on the map.
 */
public final class MapAsyncPainter {

    /**
     * Activates debug output to STDOUT explaining how prerendered images
     * are produced and consumed.
     */
    private static final boolean DEBUG_ASYNC_EVENTS_TO_STDOUT = false;
    
    /**
     * The {@code MapViewer} this painter is servicing.
     */
    private MapViewer mapViewer;

    /**
     * The current direction scrolling direction.
     * 
     * Volatile since it's modified from the EDT and read from the MapRendererThread
     */
    private volatile Direction scrollDirection = null;
    
    private final MapRendererThread mapRendererThread;
    
    private final Object consumptionLock = new Object();
    private volatile boolean aborted = false;    
    private volatile boolean consumed = true;
    private volatile boolean consumerIgnoresOldBuffer = true;
    private volatile long lastRenderTimestamp = System.currentTimeMillis();
    private boolean used = false;
    
    private Point mapFocusPointBackBuffer;
    private BufferedImage backBufferImage;
    private BufferedImage nextBackBufferImage;
       
    private final Dimension size;
    private final Dimension origSize;  
    
    private final int scrollBufferSizeInPixels;
    private final int keepScrollPixelsForChangingDirection;
    
    static {
        if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
            System.out.println();
            System.out.println("Map Async Debug enabled:");
            System.out.println("p\tPrerendering started");
            System.out.println("123\tTime taken prerendering");
            System.out.println("P\tPrerendered image delivered for consumption");
            System.out.println("#\tPrerendered image invalidated and dropped");
            System.out.println(".\tPrerendered image used");
            System.out.println(".\tPrerendered image used");
            System.out.println("!\tNew prerendered image needed immediately");
            System.out.println();
        }
    }
    
    public MapAsyncPainter(MapViewer mapViewer) {
        final MapViewerBounds mvb = mapViewer.getMapViewerBounds();
        final TileBounds tb = mvb.getTileBounds();
        this.mapViewer = mapViewer;
        this.origSize = mvb.getSize();
        this.scrollBufferSizeInPixels = tb.getWidth() * 2;
        this.size = new Dimension(origSize.width + scrollBufferSizeInPixels * 2, origSize.height + scrollBufferSizeInPixels * 2);
        
        this.keepScrollPixelsForChangingDirection = tb.getWidth();
        this.mapFocusPointBackBuffer = mvb.getFocusMapPoint();
        
        this.backBufferImage = Utils.getGoodGraphicsDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(size.width, size.height, Transparency.OPAQUE);
        this.nextBackBufferImage = Utils.getGoodGraphicsDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(size.width, size.height, Transparency.OPAQUE);
        
        mapRendererThread = new MapRendererThread(size, tb, mvb.getFocus(), this.mapFocusPointBackBuffer);
    }
    
    /**
     * Gets the next image to be rendered. This operation blocks until the image is ready.
     * 
     * @return A {@code BufferedImage} containing the entire map view to be displayed.
     */
    public BufferedImage getBackBufferImage() {
        while (true) {
            synchronized (consumptionLock) {
                try {
                    while (consumed) {
                        if (aborted) {
                            return null;
                        }
                        consumptionLock.wait();
                    }
                } catch (InterruptedException e) {}
            }
            
            if (aborted) {
                return null;
            }

            final long now = System.currentTimeMillis();
            final Point newFocus = getNewFocusPoint(now);
            
            int xDiff = newFocus.x - mapFocusPointBackBuffer.x;
            int yDiff = newFocus.y - mapFocusPointBackBuffer.y;
            int focusInBufferX = size.width / 2 + xDiff;
            int focusInBufferY = size.height / 2 + yDiff;
            int x = focusInBufferX - origSize.width / 2;
            int y = focusInBufferY - origSize.height / 2;
            
            if (x >= 0
                    && y >= 0
                    && x + origSize.width <= size.width
                    && y + origSize.height <= size.height) {
                used = true;
                lastRenderTimestamp = now;
                mapViewer.getMapViewerBounds().setFocusMapPoint(newFocus);
                if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
                    System.out.print(".");
                }
                consumerIgnoresOldBuffer = false;
                
                return backBufferImage.getSubimage(x, y, origSize.width, origSize.height);
            }
            
            if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
                System.out.print("!");
            }
            if (!used) {
                if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
                    System.out.println("\n\nWARNING: PRERENDERED IMAGE COMPLETELY UNUSED\nCoordinates: x1="
                            + x + ", y1=" + y + ", x2" + (x + origSize.width) + ", y2" + (y + origSize.height)
                            + " \nOut-of-bounds in buffer: " + size + "\n");
                }
                consumerIgnoresOldBuffer = true;
            }
            
            consumed = true;
            LockSupport.unpark(mapRendererThread);
        }
    }
    
    /**
     * Updates the scrolling direction on the map.
     * 
     * @param scrollDirection The direction the map should be scrolled in.
     */
    public void setScrollDirection(Direction scrollDirection) {
        Objects.requireNonNull(scrollDirection, "scrollDirection");
        
        final boolean initialized = (this.scrollDirection != null);
        this.scrollDirection = scrollDirection;
        
        if (!initialized) {
            mapRendererThread.start();
        }
        
        LockSupport.unpark(mapRendererThread);
    }

    /**
     * Stops this async painter.
     */
    public void stop() {
        aborted = true;
        LockSupport.unpark(mapRendererThread);
        synchronized (consumptionLock) {
            consumptionLock.notifyAll();
        }
    }
    
    /**
     * Checks if this async painter has been stopped.
     * @return {@code true} if this painter has been stopped.
     */
    public boolean isStopped() {
        return aborted;
    }


    private Point getNewFocusPoint(final long now) {
        final Point newFocus;
        if (consumerIgnoresOldBuffer) {
            newFocus = mapViewer.getMapViewerBounds().getFocusMapPoint();
        } else {
            newFocus = determineMapFocusPointOnRender(now);
        }
        return newFocus;
    }
    
    
    private static Point plusOffsets(Point point, Direction direction, int offsetX, int offsetY) {
        switch (direction) {
        case N: return new Point(point.x, point.y - offsetY);
        case S: return new Point(point.x, point.y + offsetY);
        case E: return new Point(point.x + offsetX, point.y);
        case W: return new Point(point.x - offsetX, point.y);
        case NW: return new Point(point.x - offsetX, point.y - offsetY);
        case NE: return new Point(point.x + offsetX, point.y - offsetY);
        case SW: return new Point(point.x - offsetX, point.y + offsetY);
        case SE: return new Point(point.x + offsetX, point.y + offsetY);
        default: throw new IllegalStateException("Unknown direction: " + direction);
        }
    }
    
    private Point scrollFocusOnOriginalSize(Point point, final Direction direction) {
        final int offset = scrollBufferSizeInPixels - 1 - keepScrollPixelsForChangingDirection / 2;
        return plusOffsets(point, direction, offset, offset);
    }
    
    private Point scrollFocusOnBufferSize(Point point, final Direction direction) {
        final int offset = scrollBufferSizeInPixels * 2 - 2 - keepScrollPixelsForChangingDirection;
        return plusOffsets(point, direction, offset, offset);
    }
    
    private Point determineMapFocusPointOnRender(long now) {
        final long timeElapsed = now - lastRenderTimestamp;
        final int divisor = 2;
        final int pixelsMoved = (int) (timeElapsed / divisor) + ((timeElapsed % divisor >= divisor / 2) ? 1 : 0);
        final Point newFocus = plusOffsets(mapViewer.getMapViewerBounds().getFocusMapPoint(), scrollDirection, pixelsMoved, pixelsMoved);
        
        return newFocus;
    }
    
    /**
     * The thread producing images for {@link #getBackBufferImage}.
     */
    private final class MapRendererThread extends Thread {
        
        private final MapViewerBounds internalRenderMapViewerBounds;
        private boolean ignoreOldBuffer = false;
        
        
        private MapRendererThread(Dimension size, TileBounds tileBounds, Tile focusTile, Point initialFocus) {
            this.internalRenderMapViewerBounds = new MapViewerBounds();
            this.internalRenderMapViewerBounds.changeSize(size, tileBounds);
            this.internalRenderMapViewerBounds.setFocus(focusTile);
            this.internalRenderMapViewerBounds.setFocusMapPoint(initialFocus);
        }

        
        @Override
        public void run() {
            while (!aborted) {
                paint();
            }
        }
        
        
        private void paint() {
            if (aborted) {
                return;
            }
            
            if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
                System.out.print("p");
            }
            
            final long now = System.currentTimeMillis();
            final Point newFocus = getNewFocusPoint(now);
            final Direction direction = scrollDirection;
            final Point nextMapFocusPointBackBuffer = produceRenderedImage(newFocus, direction);
            
            ignoreOldBuffer = false;
            
            if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
                System.out.print(" " + (System.currentTimeMillis() - now) + " ");
            }
            
            while (!consumed && !aborted) {
                if (direction != scrollDirection) {
                    // TODO: Reuse parts of the image.
                    if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
                        System.out.print("#");
                    }
                    ignoreOldBuffer = true;
                    return;
                }
                LockSupport.park();
            }
            
            if (aborted) {
                return;
            }
            
            deliverRenderedImage(nextMapFocusPointBackBuffer);
        }


        private Point produceRenderedImage(final Point newFocus, final Direction direction) {
            Point nextMapFocusPointBackBuffer;
            final Graphics2D g2d = nextBackBufferImage.createGraphics();
            try {
                if (consumerIgnoresOldBuffer || ignoreOldBuffer || consumed) {
                    nextMapFocusPointBackBuffer = scrollFocusOnOriginalSize(newFocus, direction);
                } else {
                    nextMapFocusPointBackBuffer = scrollFocusOnBufferSize(mapFocusPointBackBuffer, direction);
                }
                
                internalRenderMapViewerBounds.setFocusMapPoint(nextMapFocusPointBackBuffer);
                mapViewer.paintMap(g2d, size, internalRenderMapViewerBounds);
            } finally {
                g2d.dispose();
            }
            return nextMapFocusPointBackBuffer;
        }


        private void deliverRenderedImage(Point nextMapFocusPointBackBuffer) {
            final BufferedImage next = backBufferImage;
            backBufferImage = nextBackBufferImage;
            nextBackBufferImage = next;
            mapFocusPointBackBuffer = nextMapFocusPointBackBuffer;
            
            if (DEBUG_ASYNC_EVENTS_TO_STDOUT) {
                System.out.println("P");
            }

            synchronized (consumptionLock) {
                ignoreOldBuffer = false;
                used = false;
                consumed = false;
                consumptionLock.notifyAll();
            }
        }
    }
}
