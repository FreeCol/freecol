
package net.sf.freecol.client.gui.panel;

import net.sf.freecol.common.model.*;
import net.sf.freecol.client.gui.i18n.Messages;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.logging.Logger;

import javax.swing.border.*;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.*;

import net.sf.freecol.client.gui.Canvas;

/**
* This panel displays the different founding fathers the player can work towards
* recruiting.
*
* @see FoundingFather
*/
public final class ChooseFoundingFatherDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(ChooseFoundingFatherDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final Canvas    parent;
    private FoundingFatherPanel[] foundingFatherPanels = new FoundingFatherPanel[FoundingFather.TYPE_COUNT];
    private final JTabbedPane tb;
    private final ChooseFoundingFatherDialog chooseFoundingFatherDialog;
    
    
    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public ChooseFoundingFatherDialog(Canvas parent) {
        this.parent = parent;
        chooseFoundingFatherDialog = this;

        setBorder(null);
        setOpaque(false);

        tb = new JTabbedPane(JTabbedPane.TOP);

        for (int i=0; i<foundingFatherPanels.length; i++) {
            foundingFatherPanels[i] = new FoundingFatherPanel(i);
            tb.addTab(FoundingFather.getTypeAsString(i), null, foundingFatherPanels[i], null);
        }

        add(tb);
    }
    
    
    /**
    * Prepares this panel to be displayed.
    */
    public void initialize(int[] possibleFoundingFathers) {
        boolean hasSelectedTab = false;
        for (int i=0; i<possibleFoundingFathers.length; i++) {
            foundingFatherPanels[i].initialize(possibleFoundingFathers[i]);
            tb.setEnabledAt(i, foundingFatherPanels[i].isEnabled());
            if (!hasSelectedTab && foundingFatherPanels[i].isEnabled()) {
                tb.setSelectedIndex(i);
                hasSelectedTab = true;
            }
        }
        
        setSize(tb.getPreferredSize());
    }
    

    public void requestFocus() {
        tb.requestFocus();
    }

    
    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            setResponse(Integer.valueOf(command));
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
            setResponse(null);
        }
    }
    
    
    /**
    * This is the panel that is displayed in each tab.
    */
    protected class FoundingFatherPanel extends JPanel {
    
        private final int type;
        private int foundingFather = -1;
        
        private JLabel header;
        private JTextArea description;
        private JTextArea text;
        private JButton ok;
        private JPanel p1;
        
        
        /**
        * Creates a <code>FoundingFatherPanel</code> for a given type
        * of founding fathers.
        * @param type The type of founding fathers to be displayed in this
        *             <code>FoundingFatherPanel</code>.
        */
        public FoundingFatherPanel(int type) {
            this.type = type;

            setLayout(new BorderLayout());
            
            header = new JLabel("", JLabel.CENTER);
            header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
            header.setBorder(new EmptyBorder(20, 0, 0, 0));

            add(header, BorderLayout.NORTH);

            p1 = new JPanel();
            p1.setLayout(new BorderLayout(20, 20));
            p1.setOpaque(false);
            p1.setBorder(new EmptyBorder(20, 20, 20, 20));

            Image image = null;
            switch (type) {
                case 0: image = (Image) UIManager.get("FoundingFather.trade"); break;
                case 1: image = (Image) UIManager.get("FoundingFather.exploration"); break;
                case 2: image = (Image) UIManager.get("FoundingFather.military"); break;
                case 3: image = (Image) UIManager.get("FoundingFather.political"); break;
                case 4: image = (Image) UIManager.get("FoundingFather.religious"); break;
            }

            JLabel imageLabel;
            if (image != null) {
                imageLabel = new JLabel(new ImageIcon(image));
            } else {
                imageLabel = new JLabel();
            }

            p1.add(imageLabel, BorderLayout.WEST);

            JPanel p2 = new JPanel(new BorderLayout());
            p2.setOpaque(false);

            description = new JTextArea();
            description.setBorder(null);
            description.setOpaque(false);
            description.setLineWrap(true);
            description.setEditable(false);
            description.setWrapStyleWord(true);
            description.setFocusable(false);
            //description.setFont(description.getFont().deriveFont(Font.BOLD+Font.ITALIC));
            p2.add(description, BorderLayout.NORTH);

            text = new JTextArea();
            text.setBorder(null);
            text.setOpaque(false);
            text.setLineWrap(true);
            text.setEditable(false);
            text.setWrapStyleWord(true);
            text.setFocusable(false);
            p2.add(text, BorderLayout.CENTER);

            JPanel p3 = new JPanel(new BorderLayout());
            p3.setOpaque(false);
            p3.setBorder(new EmptyBorder(0, 160, 20, 160));
            ok = new JButton(Messages.message("chooseThisFoundingFather"));
            ok.addActionListener(chooseFoundingFatherDialog);
            ok.setSize(ok.getPreferredSize());
            p3.add(ok, BorderLayout.CENTER);

            p1.add(p2, BorderLayout.CENTER);
            add(p1, BorderLayout.CENTER);
            add(p3, BorderLayout.SOUTH);
        }


        public void requestFocus() {
            ok.requestFocus();
        }

        public Dimension getPreferredSize() {
            return new Dimension(570, super.getPreferredSize().height);
        }


        /**
        * Prepares this panel to be displayed.
        */
        public void initialize(int foundingFather) {
            this.foundingFather = foundingFather;

            if (foundingFather != -1) {
                header.setText(Messages.message(FoundingFather.getName(foundingFather)));
                description.setText(Messages.message(FoundingFather.getDescription(foundingFather)));
                text.setText("\n" + "[" + Messages.message(FoundingFather.getBirthAndDeath(foundingFather)) + "] " + Messages.message(FoundingFather.getText(foundingFather)));
                ok.setActionCommand(Integer.toString(foundingFather));
            }
        }


        public boolean isEnabled() {
            if (foundingFather != -1) {
                return true;
            } else {
                return false;
            }
        }
    }
}

