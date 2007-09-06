

package net.sf.freecol.client.gui;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.sf.freecol.FreeCol;


/**
* The frame that contains everything. If full screen mode is
* supported and choosen, then the {@link FullScreenFrame} will be used
* instead.
*/
public final class WindowedFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(WindowedFrame.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private Canvas canvas;
    
    
    
    

    /**
    * The constructor to use.
    */
    public WindowedFrame() {
        super("FreeCol " + FreeCol.getVersion());
        logger.info("WindowedFrame's JFrame created.");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);

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
