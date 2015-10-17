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

import java.io.File;
import java.net.URI;

import net.sf.freecol.common.sound.SoundPlayer;


/**
 * A <code>Resource</code> wrapping a <code>File</code> containing sounds.
 *
 * @see Resource
 */
public class AudioResource extends Resource {

    private File file;


    public AudioResource(File file) {
        this.file = file;
    }

    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @throws Exception of assorted types from the underlying audio components
     */
    public AudioResource(URI resourceLocator) throws Exception {
        super(resourceLocator);
        File f = new File(resourceLocator);
        if (SoundPlayer.getAudioInputStream(f) != null) this.file = f;
    }


    /**
     * Gets the file represented by this resource.
     *
     * @return The <code>File</code> for this resource.
     */
    public File getAudio() {
        return file;
    }
}
