/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


/**
 * A <code>Resource</code> wrapping a <code>File</code> containing sounds.
 *
 * @see Resource
 */
public class AudioResource extends Resource {

    private static final Logger logger = Logger.getLogger(AudioResource.class.getName());

    private File file;


    public AudioResource(File file) {
        this.file = file;
    }

    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    AudioResource(URI resourceLocator) throws Exception {
        super(resourceLocator);
        File f = new File(resourceLocator);
        BufferedInputStream bis = null;
        AudioInputStream ais = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(f));
            bis.mark(1000); bis.skip(1); bis.reset();
            ais = AudioSystem.getAudioInputStream(bis);
            this.file = f;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Not an audio file: " + f.getPath(), e);
            this.file = null;
        } finally {
            try { // Close input streams
                if (ais != null) ais.close();
                if (bis != null) bis.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing audio stream", e);
            }
        }
    }

    /**
     * Preloading is a noop for this resource type.
     */
    public void preload() {}

    /**
     * Gets the file represented by this resource.
     *
     * @return The <code>File</code> for this resource.
     */
    public File getAudio() {
        return file;
    }
}
