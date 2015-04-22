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

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A factory class for creating <code>Resource</code> instances.
 * @see Resource
 */
public class ResourceFactory {

    private static final Logger logger = Logger.getLogger(ResourceFactory.class.getName());

    /**
     * Takes a newly produced Resource.
     */
    public interface ResourceSink {

        void add(ColorResource r);
        void add(FontResource r);
        void add(StringResource r);
        void add(FAFileResource r);
        void add(SZAResource r);
        void add(AudioResource r);
        void add(VideoResource r);
        void add(ImageResource r);

    }

    /**
     * <code>WeakHashMap</code>s to ensure that only one
     * <code>Resource</code> is created given the same
     * <code>URI</code>.
     */
    private static final Map<URI, WeakReference<ColorResource>> colorResources
        = new WeakHashMap<>();
    private static final Map<URI, WeakReference<FontResource>> fontResources
        = new WeakHashMap<>();
    private static final Map<URI, WeakReference<StringResource>> stringResources
        = new WeakHashMap<>();
    private static final Map<URI, WeakReference<FAFileResource>> fafResources
        = new WeakHashMap<>();
    private static final Map<URI, WeakReference<SZAResource>> szaResources
        = new WeakHashMap<>();
    private static final Map<URI, WeakReference<AudioResource>> audioResources
        = new WeakHashMap<>();
    private static final Map<URI, WeakReference<VideoResource>> videoResources
        = new WeakHashMap<>();
    private static final Map<URI, WeakReference<ImageResource>> imageResources
        = new WeakHashMap<>();

    /**
     * Check for previously created resources.
     *
     * @param uri The <code>URI</code> used when creating the instance.
     * @param output Where a previously created instance of <code>Resource</code>
     *      with the given <code>URI</code> is put if such an object has
     *      already been created.
     * @return If a Resource is found.
     */
    private static boolean findResource(URI uri, ResourceSink output) {
        final WeakReference<ColorResource> crwr = colorResources.get(uri);
        if(crwr != null) {
            final ColorResource cr = crwr.get();
            if (cr != null) {
                output.add(cr);
                return true;
            }
        }
        final WeakReference<FontResource> frwr = fontResources.get(uri);
        if(frwr != null) {
            final FontResource fr = frwr.get();
            if (fr != null) {
                output.add(fr);
                return true;
            }
        }
        final WeakReference<StringResource> srwr = stringResources.get(uri);
        if(srwr != null) {
            final StringResource sr = srwr.get();
            if (sr != null) {
                output.add(sr);
                return true;
            }
        }
        final WeakReference<FAFileResource> farwr = fafResources.get(uri);
        if(farwr != null) {
            final FAFileResource far = farwr.get();
            if (far != null) {
                output.add(far);
                return true;
            }
        }
        final WeakReference<SZAResource> szrwr = szaResources.get(uri);
        if(szrwr != null) {
            final SZAResource szr = szrwr.get();
            if (szr != null) {
                output.add(szr);
                return true;
            }
        }
        final WeakReference<AudioResource> arwr = audioResources.get(uri);
        if(arwr != null) {
            final AudioResource ar = arwr.get();
            if (ar != null) {
                output.add(ar);
                return true;
            }
        }
        final WeakReference<VideoResource> vrwr = videoResources.get(uri);
        if(vrwr != null) {
            final VideoResource vr = vrwr.get();
            if (vr != null) {
                output.add(vr);
                return true;
            }
        }
        final WeakReference<ImageResource> irwr = imageResources.get(uri);
        if(irwr != null) {
            final ImageResource ir = irwr.get();
            if (ir != null) {
                output.add(ir);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an instance of <code>Resource</code> with the
     * given <code>URI</code> as the parameter.
     *
     * @param uri The <code>URI</code> used when creating the
     *      instance.
     * @param output Where a previously created instance of <code>Resource</code>
     *      with the given <code>URI</code> is put if such an object has
     *      already been created, or a new instance if not.
     */
    public static void createResource(URI uri, ResourceSink output) {
        if(findResource(uri, output))
            return;

        try {
            if ("urn".equals(uri.getScheme())) {
                if (uri.getSchemeSpecificPart().startsWith(ColorResource.SCHEME)) {
                    ColorResource cr = new ColorResource(uri);
                    output.add(cr);
                    colorResources.put(uri, new WeakReference<>(cr));
                } else if (uri.getSchemeSpecificPart().startsWith(FontResource.SCHEME)) {
                    FontResource fr = new FontResource(uri);
                    output.add(fr);
                    fontResources.put(uri, new WeakReference<>(fr));
                }
            } else if (uri.getPath().endsWith("\"")
                    && uri.getPath().lastIndexOf('"',
                            uri.getPath().length()-1) >= 0) {
                StringResource sr = new StringResource(uri);
                output.add(sr);
                stringResources.put(uri, new WeakReference<>(sr));
            } else if (uri.getPath().endsWith(".faf")) {
                FAFileResource far = new FAFileResource(uri);
                output.add(far);
                fafResources.put(uri, new WeakReference<>(far));
            } else if (uri.getPath().endsWith(".sza")) {
                SZAResource szr = new SZAResource(uri);
                output.add(szr);
                szaResources.put(uri, new WeakReference<>(szr));
            } else if (uri.getPath().endsWith(".ttf")) {
                FontResource fr = new FontResource(uri);
                output.add(fr);
                fontResources.put(uri, new WeakReference<>(fr));
            } else if (uri.getPath().endsWith(".wav")) {
                AudioResource ar = new AudioResource(uri);
                output.add(ar);
                audioResources.put(uri, new WeakReference<>(ar));
            } else if (uri.getPath().endsWith(".ogg")) {
                if (uri.getPath().endsWith(".video.ogg")) {
                    VideoResource vr = new VideoResource(uri);
                    output.add(vr);
                    videoResources.put(uri, new WeakReference<>(vr));
                } else {
                    AudioResource ar = new AudioResource(uri);
                    output.add(ar);
                    audioResources.put(uri, new WeakReference<>(ar));
                }
            } else {
                ImageResource ir = new ImageResource(uri);
                output.add(ir);
                imageResources.put(uri, new WeakReference<>(ir));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create resource with URI: " + uri, e);
        }
    }

}
