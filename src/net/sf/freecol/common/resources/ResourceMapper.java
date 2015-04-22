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

/**
 * Puts the Resource into the ResourceMapping.
 */
public final class ResourceMapper implements ResourceFactory.ResourceSink {

    private final ResourceMapping resourceMapping;
    private String key;

    public ResourceMapper(ResourceMapping resourceMapping) {
        this.resourceMapping = resourceMapping;
        key = null;
    }

    public void setKey(String key) {
        this.key = key;
    }


    @Override
    public void add(ColorResource r) {
        resourceMapping.add(key, r);
    }

    @Override
    public void add(FontResource r) {
        resourceMapping.add(key, r);
    }

    @Override
    public void add(StringResource r) {
        resourceMapping.add(key, r);
    }

    @Override
    public void add(FAFileResource r) {
        resourceMapping.add(key, r);
    }

    @Override
    public void add(SZAResource r) {
        resourceMapping.add(key, r);
    }

    @Override
    public void add(AudioResource r) {
        resourceMapping.add(key, r);
    }

    @Override
    public void add(VideoResource r) {
        resourceMapping.add(key, r);
    }

    @Override
    public void add(ImageResource r) {
        resourceMapping.add(key, r);
    }

}
