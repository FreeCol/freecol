package net.sf.freecol.client.gui;

import java.awt.GraphicsDevice;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.sf.freecol.FreeCol;

/**
 * The fullscreen frame that contains everything. If full screen mode is not
 * supported (or choosen), then the {@link WindowedFrame} will be used instead.
 */
public final class FullScreenFrame extends JFrame {
	private static final Logger logger = Logger.getLogger(FullScreenFrame.class
			.getName());

	public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
	public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
	public static final String REVISION = "$Revision$";

	private Canvas canvas;

	/**
	 * The constructor to use.
	 * 
	 * @param gd
	 *            The context of this <code>FullScreenFrame</code>.
	 */
	public FullScreenFrame(GraphicsDevice gd) {
		super("Freecol " + FreeCol.getVersion(), gd.getDefaultConfiguration());
		
		// TODO: Add an icon for the taskbar.
		logger.info("FullScreenFrame's JFrame created.");

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setUndecorated(true);
		// setIgnoreRepaint(true);

		gd.setFullScreenWindow(this);

		logger.info("Switched to full screen mode.");

		// createBufferStrategy(2);
		// bufferStrategy = getBufferStrategy();

		getContentPane().setLayout(null);

		logger.info("FullScreenFrame created.");
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
		addWindowListener(new WindowedFrameListener(canvas));
	}

	/**
	 * Draws this frame on the screen. Should be called whenever something has
	 * changed visually and should be updated immediately.
	 */
	/*
	 * public void display() if (bufferStrategy != null) { Graphics2D g =
	 * (Graphics2D)bufferStrategy.getDrawGraphics();
	 * 
	 * if (!bufferStrategy.contentsLost()) { try { super.paint(g); } finally {
	 * g.dispose(); }
	 * 
	 * bufferStrategy.show(); } } }
	 */

	/**
	 * Draws this frame on the screen. Should never be called manually. It will
	 * be called by Swing whenever needed.
	 * 
	 * @param g
	 *            The Graphics context in which to paint this frame.
	 */
	/*
	 * public void paint(Graphics g) { display(); }
	 */

	/**
	 * Adds a component to this FullScreenFrame.
	 * 
	 * @param c
	 *            The component to add to this FullScreenFrame.
	 */
	public void addComponent(JComponent c) {
		canvas.add(c);
	}
}
