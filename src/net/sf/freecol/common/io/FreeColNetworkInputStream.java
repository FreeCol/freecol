/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

package net.sf.freecol.common.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sf.freecol.common.networking.Connection;


/**
 * Input stream for buffering the data from the network.
 * 
 * This is just an input stream that signals end-of-stream when a
 * given token {@link Connection#END_OF_STREAM} is encountered.
 * In order to continue receiving data, the method {@link #enable}
 * has to be called.  Calls to {@code close()} have no
 * effect, the underlying input stream has to be closed directly.
 */
public class FreeColNetworkInputStream extends BufferedInputStream {

    private static final int EOS_RESULT = -1;

    private final byte[] buffer = new byte[Connection.BUFFER_SIZE];

    private final byte[] bb = new byte[1];
        
    private int bStart = 0;

    private int bSize = 0;

    private int markStart = -1;

    private int markSize = -1;

    private boolean wait = false;


    /**
     * Creates a new {@code FreeColNetworkInputStream}.
     * 
     * @param in The input stream in which this object should get
     *     the data from.
     */
    public FreeColNetworkInputStream(InputStream in) {
        super(in);
    }


    /**
     * Really close this stream.
     */
    public void reallyClose() {
        try {
            super.close();
        } catch (IOException ioe) {}
    }

    /**
     * Prepares the input stream for a new message.
     *
     * Makes the subsequent calls to {@code read} return the data
     * instead of {@code EOS_RESULT}.
     */
    public void enable() {
        this.wait = false;
    }

    /**
     * Invalidate the mark.
     */
    private void unmark() {
        this.markStart = -1;
        this.markSize = -1;
    }

    /**
     * Fills the buffer with data.
     * 
     * @return True if a non-zero amount of data was read into the buffer.
     * @exception IOException is thrown by the underlying read.
     * @exception IllegalStateException if the buffer is not empty.
     */
    private boolean fill() throws IOException {
        if (this.bSize != 0) throw new IllegalStateException("Not empty.");

        int r = super.read(buffer, 0, buffer.length);
        if (r <= 0) return false;

        this.bStart = 0;
        this.bSize = r;
        return true;
    }

    /**
     * Reads a single byte.
     * 
     * @return The byte read, or EOS_RESULT on error or "end" of stream.
     * @see #read(byte[], int, int)
     * @exception IOException is thrown by the underlying read.
     */
    @Override
    public int read() throws IOException {
        return (read(bb, 0, 1) == 1) ? bb[0] : EOS_RESULT;
    }

    /**
     * Reads from the buffer and returns the data.
     * 
     * @param b The buffer to put the data in.
     * @param off The offset to use when writing the data.
     * @param len The maximum number of bytes to read.
     * @return The actual number of bytes read, or EOS_RESULT if
     *     the message has ended
     *     ({@link Connection#END_OF_STREAM} was encountered).
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.wait) return EOS_RESULT;

        int n = 0;
        for (; n < len; n++) {
            if (this.bSize == 0) {
                if (fill()) {
                    unmark();
                } else {
                    this.wait = true;
                    break;
                }
            }

            byte value = buffer[this.bStart];
            this.bStart++;
            this.bSize--;
            if (value == (byte)Connection.END_OF_STREAM) {
                this.wait = true;
                break;
            }
            b[n + off] = value;
        }

        return (n > 0 || !this.wait) ? n : EOS_RESULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws IOException {
        if (this.markStart < 0) {
            throw new IOException("reset of unmarked stream");
        }
        this.bStart = this.markStart;
        this.bSize = this.markSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mark(int readLimit) {
        if (this.bSize == 0) { // Make sure there is something to mark
            try {
                fill();
            } catch (IOException ioe) {}
        }
                
        this.markStart = this.bStart;
        this.markSize = this.bSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        ; // Do nothing
    }
}

