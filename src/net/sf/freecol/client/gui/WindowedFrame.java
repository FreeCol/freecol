

package net.sf.freecol.client.gui;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.sf.freecol.FreeCol;

import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;


/**
* The frame that contains everything. If full screen mode is
* supported and choosen, then the {@link FullScreenFrame} will be used
* instead.
*/
public final class WindowedFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(WindowedFrame.class.getName());

    private Canvas canvas;
    
    
    
    

    /**
    * The constructor to use.
    */
    public WindowedFrame() {
        super("FreeCol " + FreeCol.getVersion());
        logger.info("WindowedFrame's JFrame created.");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize((int)bounds.getWidth() - 10, (int)bounds.getHeight() - 10); // allow room for frame handles

        logger.info("WindowedFrame created.");
    }

    
    
    

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        addWindowListener(new WindowedFrameListener(canvas));
    }


    /**
    * Adds a component to this WindowedFrame.
    * @param c The component to add to this WindowedFrame.
    */
    public void addComponent(JComponent c) {
        canvas.add(c);
    }
}
