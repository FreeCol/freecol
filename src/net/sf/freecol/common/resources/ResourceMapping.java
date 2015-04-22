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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Represents a mapping between identifiers and resources.
 *
 * @see Resource
 */
public final class ResourceMapping {

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


    /**
     * Adds a mapping between the given object identifier and a
     * <code>Resource</code>.
     *
     * @param id The identifier for the given resource in the mapping.
     * @param value The <code>Resource</code> identified by the
     *     identifier in the mapping,.
     */
    public void add(String id, ColorResource value) {
        colorResources.put(id, value);
    }

    public void add(String id, FontResource value) {
        fontResources.put(id, value);
    }

    public void add(String id, StringResource value) {
        stringResources.put(id, value);
    }

    public void add(String id, FAFileResource value) {
        fafResources.put(id, value);
    }

    public void add(String id, SZAResource value) {
        szaResources.put(id, value);
    }

    public void add(String id, AudioResource value) {
        audioResources.put(id, value);
    }

    public void add(String id, VideoResource value) {
        videoResources.put(id, value);
    }

    public void add(String id, ImageResource value) {
        imageResources.put(id, value);
    }

    public boolean duplicateResource(String id, String idNew) {
        ColorResource cr = colorResources.get(id);
        if(cr != null) {
            colorResources.put(idNew, cr);
            return true;
        }
        FontResource fr = fontResources.get(id);
        if(fr != null) {
            fontResources.put(idNew, fr);
            return true;
        }
        StringResource sr = stringResources.get(id);
        if(sr != null) {
            stringResources.put(idNew, sr);
            return true;
        }
        FAFileResource far = fafResources.get(id);
        if(far != null) {
            fafResources.put(idNew, far);
            return true;
        }
        SZAResource szr = szaResources.get(id);
        if(szr != null) {
            szaResources.put(idNew, szr);
            return true;
        }
        AudioResource ar = audioResources.get(id);
        if(ar != null) {
            audioResources.put(idNew, ar);
            return true;
        }
        VideoResource vr = videoResources.get(id);
        if(vr != null) {
            videoResources.put(idNew, vr);
            return true;
        }
        ImageResource ir = imageResources.get(id);
        if(ir != null) {
            imageResources.put(idNew, ir);
            return true;
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

    public boolean containsImageKey(String key) {
        return imageResources.containsKey(key);
    }

    /**
     * Gets the <code>Resource</code> by identifier.
     *
     * @param id The resource identifier.
     * @return The <code>Resource</code>.
     */
    public ColorResource getColorResource(String id) {
        return colorResources.get(id);
    }

    public FontResource getFontResource(String id) {
        return fontResources.get(id);
    }

    public StringResource getStringResource(String id) {
        return stringResources.get(id);
    }

    public FAFileResource getFAFileResource(String id) {
        return fafResources.get(id);
    }

    public SZAResource getSZAResource(String id) {
        return szaResources.get(id);
    }

    public AudioResource getAudioResource(String id) {
        return audioResources.get(id);
    }

    public VideoResource getVideoResource(String id) {
        return videoResources.get(id);
    }

    public ImageResource getImageResource(String id) {
        return imageResources.get(id);
    }

    /**
     * Get the image keys in this mapping with a given prefix.
     *
     * @param prefix The prefix to check for.
     * @return A list of keys.
     */
    public ArrayList<String> getImageKeys(String prefix) {
        ArrayList<String> result = new ArrayList<>();
        for (String key : imageResources.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Get the image keys in this mapping with a given prefix and suffix.
     *
     * @param prefix The prefix to check for.
     * @param suffix The suffix to check for.
     * @return A list of keys.
     */
    public ArrayList<String> getImageKeys(String prefix, String suffix) {
        ArrayList<String> result = new ArrayList<>();
        for (String key : imageResources.keySet()) {
            if (key.startsWith(prefix) && key.endsWith(suffix)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Get the image keys in this mapping with a given infix and suffix.
     *
     * @param infix The infix to check for.
     * @param suffix The suffix to check for.
     * @return The set of keys.
     */
    public Set<String> getImageKeySet(String infix, String suffix) {
        HashSet<String> result = new HashSet<>();
        for (String key : imageResources.keySet()) {
            if (key.endsWith(suffix) && key.contains(infix)) {
                result.add(key);
            }
        }
        return result;
    }
}
