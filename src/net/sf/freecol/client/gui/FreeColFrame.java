package net.sf.freecol.client.gui;

import java.awt.GraphicsDevice;
import java.awt.Rectangle;

import javax.swing.JFrame;

import net.sf.freecol.client.FreeColClient;

public abstract class FreeColFrame extends JFrame {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision: 2763 $";
    
    private FreeColClient freeColClient;
    
    
    public static FreeColFrame createFreeColFrame(FreeColClient freeColClient, GraphicsDevice gd, boolean windowed) {
        if (windowed) 
            return new WindowedFrame(freeColClient);
        return new FullScreenFrame(freeColClient, gd);
    }
    
    public FreeColFrame(FreeColClient freeColClient, String title) {
        super(title);
        this.freeColClient = freeColClient;
    }

    public FreeColFrame(FreeColClient freeColClient, String title, GraphicsDevice gd) {
        super(title, gd.getDefaultConfiguration());
        this.freeColClient = freeColClient;
    }

    public void setCanvas(Canvas canvas) {
        addWindowListener(new WindowedFrameListener(freeColClient));
        getContentPane().add(canvas);
        
    }
   
    public abstract void updateBounds(Rectangle rectangle);
    
    
}
