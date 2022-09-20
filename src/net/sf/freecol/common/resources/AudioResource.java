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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioInputStream;

import net.sf.freecol.common.sound.SoundPlayer;


/**
 * A {@code Resource} wrapping a {@code File} containing sounds.
 *
 * @see Resource
 */
public class AudioResource extends Resource {

    private final List<File> files = new ArrayList<>();;


    /**
     * Do not use directly.
     *
     * @param cachingKey The caching key.
     * @param resourceLocator The {@code URI} used when loading this
     *      resource.
     * @exception IOException if the URI does not point to recognizable audio.
     */
    public AudioResource(String cachingKey, URI resourceLocator) throws IOException {
        super(cachingKey, resourceLocator);
        
        final File file = new File(resourceLocator);
        if (!file.isDirectory()) {
            try (final AudioInputStream ais =SoundPlayer.getAudioInputStream(file)) {
                this.files.add(file);
            }
        } else {
            final File[] candidateFiles = file.listFiles((dir, name) -> name.endsWith(".ogg") || name.endsWith(".wav"));
            for (File f : candidateFiles) {
                try (final AudioInputStream ais = SoundPlayer.getAudioInputStream(f)) {
                    this.files.add(f);
                }
            }
        }
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
        if (files.isEmpty()) {
            return null;
        }
        return files.get(new Random().nextInt(files.size()));
    }
    
    public List<File> getAllAudio() {
        return files;
    }
}
