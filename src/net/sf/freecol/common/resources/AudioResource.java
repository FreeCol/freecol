/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.io.IOException;
import java.net.URI;

import net.sf.freecol.common.sound.SoundPlayer;


/**
 * A {@code Resource} wrapping a {@code File} containing sounds.
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
     * @param resourceLocator The {@code URI} used when loading this
     *      resource.
     * @exception IOException if the URI does not point to recognizable audio.
     */
    public AudioResource(URI resourceLocator) throws IOException {
        super(resourceLocator);
        File f = new File(resourceLocator);
        this.file = (SoundPlayer.getAudioInputStream(f) != null) ? f : null;
    }


    /**
     * {@inheritDoc}
     */
    public void preload() {}

    /**
     * Gets the file represented by this resource.
     *
     * @return The {@code File} for this resource.
     */
    public File getAudio() {
        return this.file;
    }
}
