/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Represents a mapping between identifiers and resources.
 *
 * @see Resource
 */
public final class ResourceMapping {

    private static final Logger logger = Logger.getLogger(ResourceMapping.class.getName());

    /** Mappings between an object identifier and a resource. */
    private final HashMap<String, ColorResource> colorResources;
    private final HashMap<String, FontResource> fontResources;
    private final HashMap<String, StringResource> stringResources;
    private final HashMap<String, FAFileResource> fafResources;
    private final HashMap<String, SZAResource> szaResources;
    private final HashMap<String, AudioResource> audioResources;
    private final HashMap<String, VideoResource> videoResources;
    private final HashMap<String, ImageResource> imageResources;


    /**
     * Creates a new empty <code>ResourceMapping</code>.
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

    /**
     * Adds a mapping between the given object identifier and a
     * <code>Resource</code>.
     *
     * @param key The identifier for the given resource in the mapping.
     * @param value The <code>Resource</code> identified by the
     *     identifier in the mapping,.
     * @return true on success
     */
    public boolean add(String key, ColorResource value) {
        if(!key.startsWith("color.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        colorResources.put(key, value);
        return true;
    }

    public boolean add(String key, FontResource value) {
        if(!key.startsWith("font.")) {
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

    public boolean add(String key, FAFileResource value) {
        if(!key.startsWith("animatedfont.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        fafResources.put(key, value);
        return true;
    }

    public boolean add(String key, SZAResource value) {
        if(!key.startsWith("animation.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        szaResources.put(key, value);
        return true;
    }

    public boolean add(String key, AudioResource value) {
        if(!key.startsWith("sound.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        audioResources.put(key, value);
        return true;
    }

    public boolean add(String key, VideoResource value) {
        if(!key.startsWith("video.")) {
            logger.warning("Rejecting malformed resource key: " + key);
            return false;
        }
        videoResources.put(key, value);
        return true;
    }

    public boolean add(String key, ImageResource value) {
        if(!key.startsWith("image.")) {
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
        if(cr != null) {
            return add(keyNew, cr);
        }
        FontResource fr = fontResources.get(key);
        if(fr != null) {
            return add(keyNew, fr);
        }
        StringResource sr = stringResources.get(key);
        if(sr != null) {
            return add(keyNew, sr);
        }
        FAFileResource far = fafResources.get(key);
        if(far != null) {
            return add(keyNew, far);
        }
        SZAResource szr = szaResources.get(key);
        if(szr != null) {
            return add(keyNew, szr);
        }
        AudioResource ar = audioResources.get(key);
        if(ar != null) {
            return add(keyNew, ar);
        }
        VideoResource vr = videoResources.get(key);
        if(vr != null) {
            return add(keyNew, vr);
        }
        ImageResource ir = imageResources.get(key);
        if(ir != null) {
            return add(keyNew, ir);
        }
        return false;
    }

    /**
     * Adds all mappings from the given <code>ResourceMapping</code> to
     * this object.
     *
     * @param rc The <code>ResourceMapping</code>.
     */
    public void addAll(ResourceMapping rc) {
        if (rc != null) {
            colorResources.putAll(rc.colorResources);
            fontResources.putAll(rc.fontResources);
            stringResources.putAll(rc.stringResources);
            fafResources.putAll(rc.fafResources);
            szaResources.putAll(rc.szaResources);
            audioResources.putAll(rc.audioResources);
            videoResources.putAll(rc.videoResources);
            imageResources.putAll(rc.imageResources);
        }
    }

    /**
     * Returns all the mappings between IDs and <code>Resource</code>s
     * that are kept by this object.
     *
     * @return An unmodifiable <code>Map</code>.
     */
    public Map<String, Resource> getResources() {
        HashMap<String, Resource> result = new HashMap<>();
            result.putAll(colorResources);
            result.putAll(fontResources);
            result.putAll(stringResources);
            result.putAll(fafResources);
            result.putAll(szaResources);
            result.putAll(audioResources);
            result.putAll(videoResources);
            result.putAll(imageResources);
        return result;
    }

    public Map<String, ImageResource> getImageResources() {
        return new HashMap<>(imageResources);
    }

    public boolean containsKey(String key) {
        return colorResources.containsKey(key)
            || fontResources.containsKey(key)
            || stringResources.containsKey(key)
            || fafResources.containsKey(key)
            || szaResources.containsKey(key)
            || audioResources.containsKey(key)
            || videoResources.containsKey(key)
            || imageResources.containsKey(key);
    }

    public boolean containsColorKey(String key) {
        return colorResources.containsKey(key);
    }

    public boolean containsImageKey(String key) {
        return imageResources.containsKey(key);
    }

    /**
     * Gets the <code>Resource</code> by identifier.
     *
     * @param key The resource identifier.
     * @return The <code>Resource</code>.
     */
    public ColorResource getColorResource(String key) {
        return colorResources.get(key);
    }

    public FontResource getFontResource(String key) {
        return fontResources.get(key);
    }

    public StringResource getStringResource(String key) {
        return stringResources.get(key);
    }

    public FAFileResource getFAFileResource(String key) {
        return fafResources.get(key);
    }

    public SZAResource getSZAResource(String key) {
        return szaResources.get(key);
    }

    public AudioResource getAudioResource(String key) {
        return audioResources.get(key);
    }

    public VideoResource getVideoResource(String key) {
        return videoResources.get(key);
    }

    public ImageResource getImageResource(String key) {
        return imageResources.get(key);
    }

    /**
     * Get the image keys in this mapping with a given prefix as a list.
     *
     * @param prefix The prefix to check for.
     * @return A list of keys.
     */
    public List<String> getImageKeys(String prefix) {
        return imageResources.keySet().stream()
            .filter(k -> k.startsWith(prefix)).collect(Collectors.toList());
    }

    /**
     * Get the image keys in this mapping with a given prefix as a set.
     *
     * @param prefix The prefix to check for.
     * @return The set of keys.
     */
    public Set<String> getImageKeySet(String prefix) {
        return imageResources.keySet().stream()
            .filter(k -> k.startsWith(prefix)).collect(Collectors.toSet());
    }

    /**
     * Get the image keys in this mapping with a given prefix and
     * suffix as a list.
     *
     * @param prefix The prefix to check for.
     * @param suffix The suffix to check for.
     * @return A list of keys.
     */
    public List<String> getImageKeys(String prefix, String suffix) {
        return imageResources.keySet().stream()
            .filter(k -> k.startsWith(prefix) && k.endsWith(suffix))
            .collect(Collectors.toList());
    }
}
