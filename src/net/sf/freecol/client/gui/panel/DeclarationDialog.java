
package net.sf.freecol.client.gui.panel;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FAFile;

/**
 * This panel displays the signing of the Declaration of Independence.
 */
public final class DeclarationDialog extends FreeColDialog {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DeclarationDialog.class.getName());
       
    private final FreeColClient freeColClient;
    @SuppressWarnings("unused")
    private final Canvas parent;
    private final SignaturePanel signaturePanel;
    
    final DeclarationDialog theDialog = this;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     * @param freeColClient The main controller object for the client.
     */
    public DeclarationDialog(final Canvas parent, final FreeColClient freeColClient) {       
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.signaturePanel = new SignaturePanel();

        setLayout(null);
        
        Image image = (Image) UIManager.get("Declaration.image");
        setSize(image.getWidth(null), image.getHeight(null));
        setOpaque(false);
        setBorder(null);        
                
        signaturePanel.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               ActionListener al = new ActionListener() {
                   public void actionPerformed(ActionEvent e) {
                       theDialog.setResponse(Boolean.TRUE);    
                   }
               };
               Timer t = new Timer(10000, al);
               t.setRepeats(false);
               t.start(); 
           }
        });
        add(signaturePanel);
        
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                theDialog.setResponse(Boolean.TRUE);
            }
        });
    }
    
    /**
     * Paints an image of the Declaration of Independence on this
     * panel.
     */
    public void paintComponent(Graphics g) {
        Image image = (Image) UIManager.get("Declaration.image");
        g.drawImage(image, 0, 0, null);
    }
    
    /**
     * Initializes this panel.
     */
    public void initialize() { 
        final int SIGNATURE_Y = 450;
        resetResponse();
        
        signaturePanel.initialize(freeColClient.getMyPlayer().getName());
        signaturePanel.setLocation((getWidth() - signaturePanel.getWidth())/2,
                (getHeight() + SIGNATURE_Y - signaturePanel.getHeight()) / 2 - 15);
        
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                signaturePanel.startAnimation();    
            }
        };
        Timer t = new Timer(3000, al);
        t.setRepeats(false);
        t.start();
    }
    
    
    /**
     * A panel for displaying an animated signature.
     * 
     * <br><br>
     * 
     * The panel should be {@link #initialize(String) initialized} 
     * with a name before the animation is 
     * {@link #startAnimation() started}.
     */
    private class SignaturePanel extends JPanel {
                
        private FAFile faFile;
        private List actionListeners = new ArrayList();        
        private Point[] points = null;
        private int counter = 0;
        
        SignaturePanel() {
            faFile = (FAFile) UIManager.get("Declaration.signature.font");
            setOpaque(false);
        }
        
        /**
         * Gets an abbreviated version of the given name.
         * @param name The name to be abbreviated
         * @return The abbreviated version of the given name. The
         *      name is made small enough to fit within the bounds
         *      the <code>DeclarationDialog</code>.
         */
        private String getAbbreviatedName(String name) {
            if (!isTooLarge(name)) {
                return name;
            }
            
            String[] partNames = name.split(" ");
            
            // Abbreviate middle names:
            for (int i=1; i<partNames.length-1 && isTooLarge(join(partNames, " ")); i++) {
                partNames[i] = partNames[i].charAt(0) + ".";                
            }
            
            // Remove middle names:
            while (partNames.length > 2 && isTooLarge(join(partNames, " "))) {
                String[] newPartNames = new String[partNames.length - 1];
                newPartNames[0] = partNames[0];
                for (int i=1; i<newPartNames.length; i++) {
                    newPartNames[i] = partNames[i+1];
                }
                partNames = newPartNames;
            }
            
             if (!isTooLarge(join(partNames, " "))) {
                 return join(partNames, " ");
             } else if (!isTooLarge(partNames[0].charAt(0) + ". " + partNames[1])) {
                 return partNames[0].charAt(0) + ". " + partNames[1];
             } else if (!isTooLarge(partNames[0] + " " + partNames[1].charAt(0) + ".")) {
                 return partNames[0] + " " + partNames[1].charAt(0) + ".";
             } else {
                 return partNames[0].charAt(0) + ". " + partNames[1].charAt(0) + ".";
             }                     
        }
        
        /**
         * Joins the given strings.
         * @param strings The strings to be joined.
         * @param delim The delimiter to place between the
         *      individual strings.
         * @return Each of the strings in the given array
         *      delimited by the given string.
         */
        private String join(String[] strings, String delim) {
            String s = strings[0];
            for (int i=1; i<strings.length; i++) {
                s += delim;
                s += strings[i];
            }
            return s;
        }
        
        /**
         * Checks if the given string is to large to be
         * displayed within this panel.
         * 
         * @param name The string to be tested.
         * @return <code>true</code> if the given string was
         *      to large to be fully displayed.
         */
        private boolean isTooLarge(String name) {
            Dimension d = faFile.getDimension(name);
            return (d.width > theDialog.getWidth() - 10);
        }
        
        /**
         * Initializes this panel with the given name.
         * 
         * @param name The name to be used when making the
         *      signature.
         */
        public void initialize(String name) {
            name = getAbbreviatedName(name);
            
            points = faFile.getPoints(name);
            counter = 0;
            setSize(faFile.getDimension(name));
        }
        
        /**
         * Adds an <code>ActionListener</code> to this
         * <code>SignaturePanel</code>. An event gets fired
         * when the animation is stopped.
         * 
         * @param al The <code>ActionListener</code>.
         * @see #startAnimation()
         */
        public void addActionListener(ActionListener al) {
            if (!actionListeners.contains(al)) {
                actionListeners.add(al);
            }
        }
        
        /**
         * Removes an <code>ActionListener</code>.
         * @param al The <code>ActionListener</code>.
         */
        public void removeActionListener(ActionListener al) {
            actionListeners.remove(al);
        }       
        
        private void notifyStopped() {
            for (int i=0; i<actionListeners.size(); i++) {
                ((ActionListener) actionListeners.get(i)).actionPerformed(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "AnimationStopped"));
            }
        }
        
        /**
         * Starts the animation of the signature. An <code>ActionEvent</code>
         * gets sent to the registered listeners when the animation has stopped.
         *
         * @see #addActionListener(ActionListener)
         */
        public void startAnimation() {
            int delay = 50; //milliseconds
            ActionListener taskPerformer = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (counter < points.length - 1) {                 
                        counter += 20;
                        if (counter > points.length) {
                            counter = points.length -1;
                            ((Timer) evt.getSource()).stop();
                            notifyStopped();
                        }                        
                        repaint();
                    } else {
                        ((Timer) evt.getSource()).stop();
                        notifyStopped();
                    }
                }
            };
            new Timer(delay, taskPerformer).start();
        }      
        
        /**
         * Paints the signature.
         */
        public void paintComponent(Graphics g) {
            if (points == null || points.length == 0) {
                return;
            }
            if (isOpaque()) {
                super.paintComponent(g);
            }
            
            g.setColor(Color.BLACK);
            ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            
            for (int i=0; i<counter; i++) {
                Point p = points[i];
                g.drawLine((int) p.getX(), (int) p.getY(), (int) p.getX(), (int) p.getY());
            }
        }        
    }    
}
