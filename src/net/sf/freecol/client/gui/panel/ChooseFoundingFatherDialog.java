/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import net.sf.freecol.FreeCol;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;

/**
 * This panel displays the different founding fathers the player can work
 * towards recruiting.
 * 
 * @see FoundingFather
 */
public final class ChooseFoundingFatherDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(ChooseFoundingFatherDialog.class.getName());




    @SuppressWarnings("unused")
    private final Canvas parent;

    private FoundingFatherPanel[] foundingFatherPanels = new FoundingFatherPanel[FoundingFather.TYPE_COUNT];

    private final JTabbedPane tb;

    private final ChooseFoundingFatherDialog chooseFoundingFatherDialog;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ChooseFoundingFatherDialog(Canvas parent) {
        this.parent = parent;
        chooseFoundingFatherDialog = this;

        setFocusCycleRoot(false);
        setBorder(null);
        setOpaque(false);

        tb = new JTabbedPane(JTabbedPane.TOP);

        for (int i = 0; i < foundingFatherPanels.length; i++) {
            foundingFatherPanels[i] = new FoundingFatherPanel(i);
            tb.addTab(FoundingFather.getTypeAsString(i), null, foundingFatherPanels[i], null);
        }

        add(tb);
    }

    /**
     * Prepares this panel to be displayed.
     * 
     * @param possibleFoundingFathers The founding fathers which can be
     *            selected. The length of the array is the same as the number of
     *            <code>FoundingFather</code> categories and the values
     *            identifies a <code>FoundingFather</code> to be picked in
     *            each of those categories.
     */
    public void initialize(FoundingFather[] possibleFoundingFathers) {
        boolean hasSelectedTab = false;
        for (int i = 0; i < possibleFoundingFathers.length; i++) {
            foundingFatherPanels[i].initialize(possibleFoundingFathers[i], i);
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
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
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

        private FoundingFather foundingFather = null;

        private JLabel header;

        private JTextArea description;

        private JTextArea text;

        private JButton ok;

        private JPanel p1;


        /**
         * Creates a <code>FoundingFatherPanel</code> for a given type of
         * founding fathers.
         * 
         * @param type The type of founding fathers to be displayed in this
         *            <code>FoundingFatherPanel</code>.
         */
        public FoundingFatherPanel(int type) {
            // this.type = type;

            setLayout(new BorderLayout());

            header = getDefaultHeader("");

            add(header, BorderLayout.NORTH);

            p1 = new JPanel();
            p1.setLayout(new BorderLayout(20, 20));
            p1.setOpaque(false);
            p1.setBorder(new EmptyBorder(20, 20, 20, 20));

            Image image = null;
            switch (type) {
            case 0:
                image = (Image) UIManager.get("FoundingFather.trade");
                break;
            case 1:
                image = (Image) UIManager.get("FoundingFather.exploration");
                break;
            case 2:
                image = (Image) UIManager.get("FoundingFather.military");
                break;
            case 3:
                image = (Image) UIManager.get("FoundingFather.political");
                break;
            case 4:
                image = (Image) UIManager.get("FoundingFather.religious");
                break;
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
            enterPressesWhenFocused(ok);
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
         * 
         * @param father The founding father to be displayed or
         *            <code>-1</code> if there is none.
         */
        public void initialize(FoundingFather father, int index) {
            this.foundingFather = father;

            if (father != null) {
                header.setText(father.getName());
                description.setText(father.getDescription());
                text.setText("\n" + "[" + father.getBirthAndDeath() + "] "
                        + father.getText());
                ok.setActionCommand(Integer.toString(index));
            }
        }

        public boolean isEnabled() {
            return (foundingFather != null);
        }
    }
}
