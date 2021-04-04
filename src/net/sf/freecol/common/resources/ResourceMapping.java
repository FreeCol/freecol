/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Represents a mapping between identifiers and resources.
 *
 * @see Resource
 */
public final class ResourceMapping {

    private static final Logger logger = Logger.getLogger(ResourceMapping.class.getName());

    /* Mappings between an object identifier and a resource. */
    private final HashMap<String, ColorResource> colorResources;
    private final HashMap<String, FontResource> fontResources;
    private final HashMap<String, StringResource> stringResources;
    private final HashMap<String, FAFileResource> fafResources;
    private final HashMap<String, SZAResource> szaResources;
    private final HashMap<String, AudioResource> audioResources;
    private final HashMap<String, VideoResource> videoResources;
    private final HashMap<String, ImageResource> imageResources;


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
    }


    // TODO: Consider cutting off the type prefixes after validation,
    //       to reduce processing time and memory use for strings.

    public boolean add(String key, AudioResource value) {
        if (!key.startsWith("sound.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        audioResources.put(key, value);
        return true;
    }

    /**
     * Adds a mapping between the given object identifier and a
     * {@code ColorResource}.
     *
     * @param key The identifier for the given resource in the mapping.
     * @param value The {@code ColorResource} identified by the
     *     identifier in the mapping,.
     * @return true on success
     */
    public boolean add(String key, ColorResource value) {
        if (!key.startsWith("color.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        colorResources.put(key, value);
        return true;
    }

    public boolean add(String key, FAFileResource value) {
        if (!key.startsWith("animatedfont.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        fafResources.put(key, value);
        return true;
    }

    public boolean add(String key, FontResource value) {
        if (!key.startsWith("font.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        fontResources.put(key, value);
        return true;
    }

    public boolean add(String key, StringResource value) {
        stringResources.put(key, value);
        return true;
    }

    public boolean add(String key, SZAResource value) {
        if (!key.startsWith("animation.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        szaResources.put(key, value);
        return true;
    }

    public boolean add(String key, VideoResource value) {
        if (!key.startsWith("video.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        videoResources.put(key, value);
        return true;
    }

    public boolean add(String key, ImageResource value) {
        if (!key.startsWith("image.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        imageResources.put(key, value);
        return true;
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
        if (rc == null) return;
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
     * @return The number of resources loaded.
     */
    public int preload() {
        int ret = 0;
        for (Resource r : colorResources.values()) {
            r.preload();
            ret++;
        }
        for (Resource r : fontResources.values()) {
            r.preload();
            ret++;
        }
        for (Resource r : stringResources.values()) {
            r.preload();
            ret++;
        }
        for (Resource r : fafResources.values()) {
            r.preload();
            ret++;
        }
        for (Resource r : szaResources.values()) {
            r.preload();
            ret++;
        }
        for (Resource r : audioResources.values()) {
            r.preload();
            ret++;
        }
        for (Resource r : videoResources.values()) {
            r.preload();
            ret++;
        }
        for (Resource r : imageResources.values()) {
            r.preload();
            ret++;
        }
        return ret;
    }
}
