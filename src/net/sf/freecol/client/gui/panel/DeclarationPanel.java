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

package net.sf.freecol.client.gui.panel;

import static net.sf.freecol.common.util.StringUtils.join;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.Timer;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.resources.FAFile;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.ImageUtils;


/**
 * This panel displays the signing of the Declaration of Independence.
 */
public final class DeclarationPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DeclarationPanel.class.getName());

    private final int SIGNATURE_Y = 450;
    
    private final String ANIMATION_STOPPED = "AnimationStopped";

    private final int START_DELAY = 2000; // 2s before signing
    private final int ANIMATION_DELAY = 15; // 50ms between signature steps
    private final int FINISH_DELAY = 5000; // 5s before closing
    
    private final Runnable afterClosing;
    private boolean closed = false;
    private final Image image;


    /**
     * Creates a DeclarationPanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param afterClosing A callback that is executed after the panel closes.
     */
    public DeclarationPanel(FreeColClient freeColClient, Runnable afterClosing) {
        super(freeColClient);

        this.afterClosing = afterClosing;
        
        setLayout(null);
        image = getImageLibrary().getScaledImage("image.flavor.Declaration");
        setSize(image.getWidth(null), image.getHeight(null));
        setOpaque(false);
        setBorder(null);
        addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent k) {
                    closePanel();
                }
            });
        addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    closePanel();
                }
            });

        final int scaledSignatureY = (int) (SIGNATURE_Y * getImageLibrary().getScaleFactor());
        
        final SignaturePanel signaturePanel = new SignaturePanel();
        signaturePanel.initialize(getMyPlayer().getName());
        signaturePanel.setLocation((getWidth()-signaturePanel.getWidth()) / 2,
            (getHeight() + scaledSignatureY - signaturePanel.getHeight()) / 2 - 15);
        signaturePanel.addActionListener(this);

        add(signaturePanel);
    
        Timer t = new Timer(START_DELAY, (ActionEvent ae) -> {
                signaturePanel.startAnimation();
            });
        t.setRepeats(false);
        t.start();
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (ANIMATION_STOPPED.equals(command)) {
            Timer t = new Timer(FINISH_DELAY, (x) -> {
                    closePanel();
                });
            t.setRepeats(false);
            t.start();
        } else {
            super.actionPerformed(ae);
        }
    }
    
    private void closePanel() {
        final boolean oldClosed = closed;
        closed = true;
        
        if (!oldClosed) {
            getGUI().removeComponent(DeclarationPanel.this);
            afterClosing.run();
        }
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
    }


    /**
     * A panel for displaying an animated signature.
     * 
     * The panel should be {@link #initialize(String) initialized} with a name
     * before the animation is {@link #startAnimation() started}.
     */
    private class SignaturePanel extends JPanel {

        private final FAFile faFile;

        private final ArrayList<ActionListener> actionListeners
            = new ArrayList<>();

        private Point[] points = null;

        private int counter = 0;
        
        private BufferedImage imageBuffer;


        SignaturePanel() {
            this.faFile = ResourceManager.getFAFile("animatedfont.signature");
            setOpaque(false);
        }

        /**
         * Gets an abbreviated version of the given name.
         * 
         * @param name The name to be abbreviated
         * @return The abbreviated version of the given name.  The name
         *     is made small enough to fit within the bounds the
         *     {@code DeclarationPanel}.
         */
        private String getAbbreviatedName(String name) {
            if (!isTooLarge(name)) return name;

            String[] partNames = name.split(" ");

            // Abbreviate middle names:
            for (int i = 1; i < partNames.length - 1
                     && isTooLarge(join(" ", partNames)); i++) {
                partNames[i] = partNames[i].charAt(0) + ".";
            }

            // Remove middle names:
            while (partNames.length > 2
                && isTooLarge(join(" ", partNames))) {
                String[] newPartNames = new String[partNames.length - 1];
                newPartNames[0] = partNames[0];
                for (int i = 1; i < newPartNames.length; i++) {
                    newPartNames[i] = partNames[i + 1];
                }
                partNames = newPartNames;
            }

            String first = partNames[0], second = partNames[1];
            String s = join(" ", partNames);
            if (!isTooLarge(s)) return s;
            s = first.charAt(0) + ". " + second;
            if (!isTooLarge(s)) return s;
            s = first + " " + second.charAt(0) + ".";
            if (!isTooLarge(s)) return s;
            return first.charAt(0) + ". " + second.charAt(0) + ".";
        }

        /**
         * Checks if the given string is to large to be displayed within this
         * panel.
         * 
         * @param name The string to be tested.
         * @return {@code true} if the given string was to large to be
         *     fully displayed.
         */
        private boolean isTooLarge(String name) {
            final Dimension d = faFile.getDimension(name);
            final int scaledMargin = (int) (10 * getImageLibrary().getScaleFactor());
            final int  scaledSignatureWidth = (int) (d.width * getImageLibrary().getScaleFactor());
            return (scaledSignatureWidth > DeclarationPanel.this.getWidth() - scaledMargin);
        }

        /**
         * Initializes this panel with the given name.
         * 
         * @param name The name to be used when making the signature.
         */
        public void initialize(String name) {
            name = getAbbreviatedName(name);

            points = faFile.getPoints(name);
            counter = 0;
            
            final Dimension originalSize = faFile.getDimension(name);
            final Dimension size = getImageLibrary().scale(originalSize);
            imageBuffer = ImageUtils.createBufferedImage(size.width, size.height);
            setSize(size);
        }

        /**
         * Adds an {@code ActionListener} to this
         * {@code SignaturePanel}.  An event gets fired when the
         * animation is stopped.
         * 
         * @param al The {@code ActionListener}.
         * @see #startAnimation()
         */
        public void addActionListener(ActionListener al) {
            if (!actionListeners.contains(al)) {
                actionListeners.add(al);
            }
        }

        private void notifyStopped() {
            for (ActionListener actionListener : actionListeners) {
                actionListener.actionPerformed(new ActionEvent(this,
                        ActionEvent.ACTION_PERFORMED, ANIMATION_STOPPED));
            }
        }

        /**
         * Starts the animation of the signature.  An
         * {@code ActionEvent} gets sent to the registered
         * listeners when the animation has stopped.
         * 
         * @see #addActionListener(ActionListener)
         */
        public void startAnimation() {
            ActionListener taskPerformer = (ActionEvent ae) -> {
                if (counter >= points.length - 1) {
                    ((Timer)ae.getSource()).stop();
                    notifyStopped();
                }
                
                final int fromIndex = counter;
                counter += 3;
                if (counter > points.length) {
                    counter = points.length;
                    ((Timer)ae.getSource()).stop();
                    notifyStopped();
                }
                
                final float scaleFactor = getImageLibrary().getScaleFactor();
                final Graphics2D g2d = imageBuffer.createGraphics();
                g2d.setColor(Color.BLACK);
                
                final int brushSize = (scaleFactor <= 1 ? 1 : 2);
                
                for (int i = fromIndex; i < counter; i++) {
                    Point p = points[i];

                    g2d.fillRect(scaleToInt(p.getX(), scaleFactor), scaleToInt(p.getY(), scaleFactor), brushSize, brushSize);
                    g2d.fillRect(scaleToInt(p.getX(), scaleFactor) - 1, scaleToInt(p.getY(), scaleFactor) + 1, brushSize, brushSize);
                }
                
                g2d.dispose();
                
                validate();
                repaint();
            };
            new Timer(ANIMATION_DELAY, taskPerformer).start();
        }


        // Override JComponent
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void paintComponent(Graphics g) {
            if (points == null || points.length == 0) {
                return;
            }
            if (isOpaque()) {
                super.paintComponent(g);
            }
            
            final Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(imageBuffer, 0, 0, null);
        }
        
        private int scaleToInt(double d, float scaleFactor) {
            return (int) (d * scaleFactor);
        }
    }
}
