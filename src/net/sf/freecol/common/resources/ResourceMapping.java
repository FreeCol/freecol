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

package net.sf.freecol.common.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * Represents a mapping between identifiers and resources.
 *
 * @see Resource
 */
public final class ResourceMapping {

    private static final Logger logger = Logger.getLogger(ResourceMapping.class.getName());

    /* Mappings between an object identifier and a resource. */
    private final Map<String, ColorResource> colorResources;
    private final Map<String, FontResource> fontResources;
    private final Map<String, StringResource> stringResources;
    private final Map<String, FAFileResource> fafResources;
    private final Map<String, SZAResource> szaResources;
    private final Map<String, AudioResource> audioResources;
    private final Map<String, VideoResource> videoResources;
    private final Map<String, ImageResource> imageResources;
    private final Map<Class<?>, ResourceType<?>> resourceTypes;


    /**
     * Creates a new empty {@code ResourceMapping}.
     */
    public ResourceMapping() {
        colorResources = new HashMap<>();
        fontResources = new HashMap<>();
        stringResources = new HashMap<>();
        fafResources = new HashMap<>();
        szaResources = new HashMap<>();
        audioResources = new HashMap<>();
        videoResources = new HashMap<>();
        imageResources = new HashMap<>();
        resourceTypes = mapFrom(
                new ResourceType<ColorResource>(ColorResource.class, "color.", colorResources),
                new ResourceType<FontResource>(FontResource.class, "font.", fontResources),
                new ResourceType<StringResource>(StringResource.class, null, stringResources),
                new ResourceType<FAFileResource>(FAFileResource.class, "animatedfont.", fafResources),
                new ResourceType<SZAResource>(SZAResource.class, "animation.", szaResources),
                new ResourceType<AudioResource>(AudioResource.class, "sound.", audioResources),
                new ResourceType<VideoResource>(VideoResource.class, "video.", videoResources),
                new ResourceType<ImageResource>(ImageResource.class, "image.", imageResources));
    }
    
    public boolean add(String key, Resource resource) {
		final ResourceType<?> resourceType = resourceTypes.get(resource.getClass());
		return resourceType.put(key, resource);
    }

    /**
     * Create another mapping for a Resource under a different key.
     *
     * @param key The key to find the existing Resource.
     * @param keyNew The new key for the duplicate.
     * @return true on success
     */
    public boolean duplicateResource(String key, String keyNew) {
        ColorResource cr = colorResources.get(key);
        if (cr != null) {
            return add(keyNew, cr);
        }
        FontResource fr = fontResources.get(key);
        if (fr != null) {
            return add(keyNew, fr);
        }
        StringResource sr = stringResources.get(key);
        if (sr != null) {
            return add(keyNew, sr);
        }
        FAFileResource far = fafResources.get(key);
        if (far != null) {
            return add(keyNew, far);
        }
        SZAResource szr = szaResources.get(key);
        if (szr != null) {
            return add(keyNew, szr);
        }
        AudioResource ar = audioResources.get(key);
        if (ar != null) {
            return add(keyNew, ar);
        }
        VideoResource vr = videoResources.get(key);
        if (vr != null) {
            return add(keyNew, vr);
        }
        ImageResource ir = imageResources.get(key);
        if (ir != null) {
            return add(keyNew, ir);
        }
        return false;
    }

    /**
     * Adds all mappings from the given {@code ResourceMapping} to
     * this object.
     *
     * @param rc The {@code ResourceMapping}.
     */
    public void addAll(ResourceMapping rc) {
        if (rc == null) {
            return;
        }
        colorResources.putAll(rc.colorResources);
        fontResources.putAll(rc.fontResources);
        stringResources.putAll(rc.stringResources);
        fafResources.putAll(rc.fafResources);
        szaResources.putAll(rc.szaResources);
        audioResources.putAll(rc.audioResources);
        videoResources.putAll(rc.videoResources);
        imageResources.putAll(rc.imageResources);
    }

    /**
     * Get an {@code AudioResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code AudioResource} found.
     */
    public AudioResource getAudioResource(String key) {
        return audioResources.get(key);
    }

    /**
     * Get an {@code ColorResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code ColorResource} found.
     */
    public ColorResource getColorResource(String key) {
        return colorResources.get(key);
    }

    /**
     * Get an {@code FAFileResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code FAFileResource} found.
     */
    public FAFileResource getFAFileResource(String key) {
        return fafResources.get(key);
    }

    /**
     * Get an {@code FontResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code FontResource} found.
     */
    public FontResource getFontResource(String key) {
        return fontResources.get(key);
    }

    /**
     * Get an {@code ImageResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code ImageResource} found.
     */
    public ImageResource getImageResource(String key) {
        return imageResources.get(key);
    }

    /**
     * Get an {@code StringResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code StringResource} found.
     */
    public StringResource getStringResource(String key) {
        return stringResources.get(key);
    }

    /**
     * Get an {@code SZAResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code SZAResource} found.
     */
    public SZAResource getSZAResource(String key) {
        return szaResources.get(key);
    }

    /**
     * Get an {@code VideoResource} by identifier.
     *
     * @param key The resource identifier.
     * @return The {@code VideoResource} found.
     */
    public VideoResource getVideoResource(String key) {
        return videoResources.get(key);
    }

    /**
     * Get the image keys in this mapping.
     *
     * @return A set of keys.
     */
    public Set<String> getImageKeySet() {
        return imageResources.keySet();
    }

    /**
     * Preload all resources in this mapping.
     *
     * @param preloadController The {@code PreloadController} handling
     *     this preload.
     * @return The number of resources loaded.
     */
    public int preload(PreloadController preloadController) {
        int ret = 0;
        for (Resource r : colorResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        for (Resource r : fontResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        for (Resource r : stringResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        for (Resource r : videoResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        for (Resource r : imageResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        for (Resource r : fafResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        for (Resource r : szaResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        for (Resource r : audioResources.values()) {
            if (!preloadController.shouldContinue()) {
                return ret;
            }
            r.preload();
            ret++;
        }
        return ret;
    }
    
    public void clearCaches() {
        imageResources.values().stream().forEach(r -> r.clean());
    }
    
    public interface PreloadController {
        boolean shouldContinue();
    }
    
    private static final Map<Class<?>, ResourceType<?>> mapFrom(ResourceType<?>... resourceTypes) {
        final Map<Class<?>, ResourceType<?>> map = new HashMap<>();
        for (ResourceType<?> r : resourceTypes) {
            map.put(r.clazz, r);
        }
        return  map;
    }
    
    private static class ResourceType<T> {
        private final Class<T> clazz;
        private final String requiredKeyPrefix;
        private final Map<String, T> resources;
        
        public ResourceType(Class<T> clazz, String requiredKeyPrefix, Map<String, T> resources) {
            this.clazz = clazz;
            this.requiredKeyPrefix = requiredKeyPrefix;
            this.resources = resources;
        }
        
        boolean put(String key, Object resource) {
            if (requiredKeyPrefix != null && !key.startsWith(requiredKeyPrefix)) {
                logger.warning("Rejecting malformed resource key: \""
                        + key + "\". The key should have started with: \"" + requiredKeyPrefix + "\".");
                return false;
            }
            resources.put(key, clazz.cast(resource));
            return true;
        }
    }
}
