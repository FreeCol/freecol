package net.sf.freecol.client.gui.images;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.resources.ImageCache;

/**
 * Lists the image creators that can be used by {@link ImageLibrary} to
 * dynamically generate images.
 */
public final class ImageCreators {

    private final BaseTileTransitionImageCreator baseTileTransitionImageCreator;
    private final BeachTileAnimationImageCreator beachTileAnimationImageCreator;
    private final RiverAnimationImageCreator riverAnimationImageCreator;
    private final DeltaAnimationImageCreator deltaAnimationImageCreator;
    

    public ImageCreators(ImageLibrary lib, ImageCache imageCache) {
        this.baseTileTransitionImageCreator = new BaseTileTransitionImageCreator(lib, imageCache);
        this.beachTileAnimationImageCreator = new BeachTileAnimationImageCreator(lib, imageCache);
        this.riverAnimationImageCreator = new RiverAnimationImageCreator(lib, imageCache);
        this.deltaAnimationImageCreator = new DeltaAnimationImageCreator(lib, imageCache);
    }
    
    
    public BaseTileTransitionImageCreator getBaseTileTransitionImageCreator() {
        return baseTileTransitionImageCreator;
    }
    
    public BeachTileAnimationImageCreator getBeachTileAnimationImageCreator() {
        return beachTileAnimationImageCreator;
    }
    
    public RiverAnimationImageCreator getRiverAnimationImageCreator() {
        return riverAnimationImageCreator;
    }
    
    public DeltaAnimationImageCreator getDeltaAnimationImageCreator() {
        return deltaAnimationImageCreator;
    }
}
