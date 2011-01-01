/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import net.sf.freecol.client.gui.panel.FreeColImageBorder;

import com.fluendo.player.Cortado;
// Disable the feature that seems to be missing from the stock Cortado
// import com.fluendo.player.StopListener;

/**
 * A component for playing video.
 */
public class VideoComponent extends JPanel {

    private final Cortado applet;
    private List<VideoListener> videoListeners = new LinkedList<VideoListener>();

    /**
     * Creates a component for displaying the given video.
     * @param video The <code>Video</code> to be displayed.
     */
    public VideoComponent(Video video, boolean mute) {
        final String url = video.getURL().toExternalForm();
        
        setBorder(createBorder());
        final Insets insets = getInsets();
        
        applet = new Cortado();
        applet.setSize(655, 480);
        // FIXME: -1 avoids transparent part of border.
        applet.setLocation(insets.left - 1, insets.top - 1);
        
        applet.setParam ("url", url);
        applet.setParam ("local", "false");
        applet.setParam ("framerate", "60");
        applet.setParam ("keepaspect", "true");
        applet.setParam ("video", "true");
        String withAudio = "true";
        if(mute){
            withAudio = "false";
        }
        applet.setParam ("audio", withAudio);
        applet.setParam ("kateIndex", "0");
        applet.setParam ("bufferSize", "200");
        applet.setParam ("showStatus", "hide");
        applet.setParam ("debug", "0");
        applet.init();

        // Disable the feature that seems to be missing from the stock Cortado
        //applet.setStopListener(new StopListener() {
        //    public void stopped() {
        //        SwingUtilities.invokeLater(new Runnable() {
        //            public void run() {
        //                for (VideoListener sl : videoListeners) {
        //                    sl.stopped();
        //                }
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
    
    /**
     * Adds a listener for video playback events.
     * @param videoListener A listener for video playback events.
     */
    public void addVideoListener(VideoListener videoListener) {
        videoListeners.add(videoListener);
    }
    
    /**
     * Removes the given listener.
     * @param videoListener The listener to be removed from this
     *      <code>VideoComponent</code>.
     */
    public void removeVideoListener(VideoListener videoListener) {
        videoListeners.remove(videoListener);
    }
    
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
    
    private Border createBorder() {
        return FreeColImageBorder.imageBorder;
    }
}
