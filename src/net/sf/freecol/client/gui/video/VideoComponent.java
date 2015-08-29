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

package net.sf.freecol.client.gui.video;

import java.awt.Insets;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.border.Border;

import com.fluendo.player.Cortado;

import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.common.resources.Video;


/**
 * A component for playing video.
 */
public class VideoComponent extends JPanel {

    private static final Logger logger = Logger.getLogger(VideoComponent.class.getName());

    //private List<VideoListener> videoListeners
    //    = new LinkedList<VideoListener>();

    private final Cortado applet;


    /**
     * Creates a component for displaying the given video.
     * @param video The <code>Video</code> to be displayed.
     *
     * @param mute boolean silence
     */
    public VideoComponent(Video video, boolean mute) {
        final String url = video.getURL().toExternalForm();

        setOpaque(false);
        setBorder(createBorder());
        final Insets insets = getInsets();

        applet = new Cortado();
        applet.setSize(655, 480);
        // FIXME: -1 avoids transparent part of border.
        applet.setLocation(insets.left - 1, insets.top - 1);

        applet.setParam("url", url);
        applet.setParam("framerate", "60");
        applet.setParam("keepaspect", "true");
        applet.setParam("video", "true");
        applet.setParam("audio", mute ? "false" : "true");
        applet.setParam("kateIndex", "0");
        applet.setParam("bufferSize", "200");
        applet.setParam("showStatus", "hide");
        applet.setParam("debug", "0");
        applet.init();

        // Disable the feature that seems to be missing from the stock Cortado
        //applet.setStopListener(new StopListener() {
        //    public void stopped() {
        //        SwingUtilities.invokeLater(() -> {
        //            for (VideoListener sl : videoListeners) {
        //                sl.stopped();
        //            }
        //        });
        //    }
        //});

        setLayout(null);
        add(applet);

        // FIXME: -2 avoids transparent part of border.
        setSize(applet.getWidth() + insets.left + insets.right - 2,
                applet.getHeight() + insets.top + insets.bottom - 2);
    }


    private Border createBorder() {
        return FreeColImageBorder.imageBorder;
    }

    ///**
    // * Adds a listener for video playback events.
    // *
    // * @param videoListener A listener for video playback events.
    // */
    //public void addVideoListener(VideoListener videoListener) {
    //    videoListeners.add(videoListener);
    //}
    //
    ///**
    // * Removes the given listener.
    // *
    // * @param videoListener The listener to be removed from this
    // *     <code>VideoComponent</code>.
    // */
    //public void removeVideoListener(VideoListener videoListener) {
    //    videoListeners.remove(videoListener);
    //}

    @Override
    public void addMouseListener(MouseListener l) {
        super.addMouseListener(l);

        applet.addMouseListener(l);
    }

    @Override
    public void removeMouseListener(MouseListener l) {
        super.removeMouseListener(l);

        applet.removeMouseListener(l);
    }

    /**
     * Start playing the video.
     */
    public void play() {
        applet.start();
    }

    /**
     * Stop playing the video.
     */
    public void stop() {
        applet.stop();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        applet.stop();
        applet.destroy();

        // Java crashes here deep in the libraries, typically including:
        //   sun.awt.X11.XBaseMenuWindow.dispose(XBaseMenuWindow.java:907)
        // so it is probably X11-dependent.
        //
        // Sighted:
        //   (Fedora, 1.7.0_40, 24.0-b56)
        //   (Arch, 1.7.0_45, 24.45-b08)
        //
        // Switching windowed mode seems to hit is particularly badly on
        // arch, although not seeing that on Fedora (BR#2611).
        //
        // This routine was introduced to fix a different Java crash,
        // so disabling it and/or replacing it with a stub just moves
        // the problem around.  Even the following does not help in
        // all cases:
        try {
            super.removeNotify();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Video removal crash", e);
        }
    }
}
