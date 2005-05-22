
package net.sf.freecol.client.gui.option;

import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.common.option.*;

import java.awt.event.KeyEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.BorderFactory;


/**
* User interface for displaying/changing a keyboard accelerator for a <code>FreeColAction</code>.
*/
public final class FreeColActionUI extends JPanel implements OptionUpdater, ActionListener {
    private static final Logger logger = Logger.getLogger(FreeColActionUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final FreeColAction option;
    private final OptionGroupUI optionGroupUI;
    private KeyStroke keyStroke;
    private JButton recordButton;
    private BlinkingLabel bl;


    /**
    * Creates a new <code>FreeColActionUI</code> for the given <code>FreeColAction</code>.
    * @param option The <code>FreeColAction</code> to make a user interface for.
    */
    public FreeColActionUI(FreeColAction option, OptionGroupUI optionGroupUI) {
        super(new BorderLayout());

        this.option = option;
        this.optionGroupUI = optionGroupUI;
        keyStroke = option.getAccelerator();

        JLabel label = new JLabel(option.getName(), JLabel.LEFT);
        label.setToolTipText(option.getShortDescription());
        add(label, BorderLayout.CENTER);

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bl = new BlinkingLabel();
        p1.add(bl);

        recordButton = new JButton("O");
        recordButton.addActionListener(this);
        p1.add(recordButton);
        
        add(p1, BorderLayout.EAST);

        setOpaque(false);
    }


    public Dimension getPreferredSize() {
        return new Dimension(getParent().getWidth()/2 - getParent().getInsets().left - getParent().getInsets().right - (OptionGroupUI.H_GAP*3)/2, super.getPreferredSize().height);
    }
    
    
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    

    /**
    * Gets a string to represent the given <code>KeyStroke</code> to the user.
    */
    private static String getHumanKeyStrokeText(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return " ";
        }

        String s = KeyEvent.getKeyModifiersText(keyStroke.getModifiers());
        if (!s.equals("")) {
            s += "+";
        }
        return s + KeyEvent.getKeyText(keyStroke.getKeyCode());
    }

    
    /**
    * Removes the given <code>KeyStroke</code>.
    */
    public void removeKeyStroke(KeyStroke k) {
        if (k != null && keyStroke != null && k.getKeyCode() == keyStroke.getKeyCode() && k.getModifiers() == keyStroke.getModifiers()) {
            keyStroke = null;
            bl.setText(" ");
        }
    }


    /**
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        option.setAccelerator(keyStroke);
    }

    
    public void actionPerformed(ActionEvent evt) {
        bl.startBlinking();
        bl.requestFocus();
    }



    /**
    * Label for displaying a <code>KeyStroke</code>.
    */
    class BlinkingLabel extends JLabel implements ActionListener, KeyListener {

        private Timer blinkingTimer = new Timer(1000, this);
        private boolean blinkOn = false;

        BlinkingLabel() {
            super(getHumanKeyStrokeText(keyStroke), JLabel.CENTER);

            setOpaque(false);
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            addKeyListener(this);
        }

        public Dimension getMinimumSize() {
            return new Dimension(80, super.getMinimumSize().height);
        }

        public Dimension getPreferredSize() {
            return getMinimumSize();
        }


        public void startBlinking() {
            blinkingTimer.start();
        }


        public void stopBlinking() {
            blinkingTimer.stop();
            setOpaque(false);
            repaint();
        }

        public void actionPerformed(ActionEvent evt) {
            if (blinkOn) {
                setOpaque(false);
                blinkOn = false;
                repaint();
            } else {
                setOpaque(true);
                setBackground(Color.RED);
                blinkOn = true;
                repaint();
            }
        }


        public void keyPressed(KeyEvent e) {}

        public void keyTyped(KeyEvent e) {}

        public void keyReleased(KeyEvent e) {
            KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
            optionGroupUI.removeKeyStroke(ks);
            keyStroke = ks;
            //keyStroke = KeyStroke.getKeyStroke(new Character(e.getKeyChar()), e.getModifiers());
            stopBlinking();
            setText(getHumanKeyStrokeText(keyStroke));
            recordButton.requestFocus();
        }
    }
}
