/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;


/**
 * Superclass for all dialogs in FreeCol.
 */
public class FreeColDialog<T> extends JDialog implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    public static enum DialogType {
        PLAIN,
        QUESTION,
    };

    private static final Border dialogBorder
        = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(10, 20, 10, 20));

    /** The enclosing client. */
    protected FreeColClient freeColClient;

    /** Is this dialog modal? */
    protected boolean modal;

    /** The options to choose from. */
    protected List<ChoiceItem<T>> options;

    /** The JOptionPane to embed in this dialog. */
    private JOptionPane pane;

    /** An optional ScrollPane if there are many options. */
    private JScrollPane scrollPane;


    /**
     * Protected constructor for the subclass panels.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    protected FreeColDialog(FreeColClient freeColClient) {
        super(freeColClient.getGUI().getFrame());

        this.freeColClient = freeColClient;
    }
        
    /**
     * Full constructor for canvas to build a dialog in one hit (supplying
     * the getResponse() implementation).
     *
     * Much of this was extracted from the source for
     * JOptionPane.createDialog.  We needed a way to control modality.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param type The <code>DialogType</code> to create.
     * @param modal Should this dialog be modal?
     * @param obj The main object that explains the choice for the user,
     *     usually just a string, but may be more complex.
     * @param icon An optional icon to display.
     * @param options A list of options to choose from.
     */
    public FreeColDialog(FreeColClient freeColClient, DialogType type,
                         boolean modal, Object obj, ImageIcon icon,
                         List<ChoiceItem<T>> options) {
        this(freeColClient);

        initialize(type, modal, obj, icon, options);
    }


    /**
     * Select the default option from the supplied options.
     *
     * @param options A list of options to choose from.
     * @return The option to select initially.
     */
    private int selectDefault(List<ChoiceItem<T>> options) {
        int def = -1, can = -1, ok = -1, i = 0;
        for (ChoiceItem<T> ci : options) {
            if (ci.isDefault()) def = i;
            else if (ci.isCancel()) can = i;
            else if (ci.isOK()) ok = i;
            i++;
        }
        return (def >= 0) ? def : (can >= 0) ? can : (ok >= 0) ? ok
            : options.size() - 1;
    }

    /**
     * Complete the initialization.  Useful for subclasses that need
     * to construct a non-trivial object to display in the JOptionPane.
     *
     * @param type The <code>DialogType</code> to create.
     * @param modal Should this dialog be modal?
     * @param obj The main object that explains the choice for the user,
     *     usually just a string, but may be more complex.
     * @param icon An optional icon to display.
     * @param options A list of options to choose from.
     */
    protected void initialize(DialogType type, boolean modal, Object obj, 
                              ImageIcon icon, List<ChoiceItem<T>> options) {
        this.modal = modal;
        this.options = options;
        int paneType = JOptionPane.QUESTION_MESSAGE;
        switch (type) {
        case PLAIN:    paneType = JOptionPane.PLAIN_MESSAGE; break;
        case QUESTION: paneType = JOptionPane.QUESTION_MESSAGE; break;
        }
        int def = selectDefault(options);
        ChoiceItem<T> ci = (def >= 0) ? options.get(def) : null;
        this.pane = new JOptionPane(obj, paneType, JOptionPane.YES_NO_OPTION,
                                    icon, options.toArray(), ci);
        this.pane.setBorder(dialogBorder);
        this.pane.setName("FreeColDialog");
        this.pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
        if (ci != null) this.pane.setInitialSelectionValue(ci);
        this.pane.addPropertyChangeListener(this);
        if (options.size() <= 20) {
            this.scrollPane = null;
        } else {
            this.scrollPane = new JScrollPane(this.pane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            this.scrollPane.setOpaque(false);
            this.scrollPane.setBorder(null);
            this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        }
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add((this.scrollPane != null) ? this.scrollPane
            : this.pane, BorderLayout.CENTER);

        setFocusCycleRoot(true);
        setComponentOrientation(this.pane.getComponentOrientation());
        setResizable(false);
        setUndecorated(true);
        setModal(modal);
        try { // Layout failures might not get logged.
            pack();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Packing failure", e);
        }
        setLocationRelativeTo(getGUI().getFrame());

        WindowAdapter adapter = new WindowAdapter() {
                private boolean gotFocus = false;

                public void windowClosing(WindowEvent we) {
                    if (!FreeColDialog.this.responded()) {
                        FreeColDialog.this.setValue(null);
                    }
                }
                public void windowGainedFocus(WindowEvent we) {
                    if (!gotFocus) { // Once window gets focus, initialize.
                        FreeColDialog.this.pane.selectInitialValue();
                        gotFocus = true;
                    }
                }
            };
        addWindowListener(adapter);
        addWindowFocusListener(adapter);

        addComponentListener(new ComponentAdapter() {
                public void componentShown(ComponentEvent ce) {
                    // Reset value to ensure closing works properly.
                    FreeColDialog.this.pane
                        .setValue(JOptionPane.UNINITIALIZED_VALUE);
                }
            });

        addMouseListener(new MouseAdapter() {
                private Point loc;

                @Override
                public void mouseDragged(MouseEvent e) {}

                @Override
                public void mouseMoved(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e) {
                    loc = SwingUtilities
                        .convertPoint((Component)e.getSource(),
                            e.getX(), e.getY(), null);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (loc == null) return;
                    Point now = SwingUtilities
                        .convertPoint((Component)e.getSource(),
                            e.getX(), e.getY(), null);
                    int dx = now.x - loc.x;
                    int dy = now.y - loc.y;
                    Point p = FreeColDialog.this.getLocation();
                    FreeColDialog.this.setLocation(p.x + dx, p.y + dy);
                    loc = null;
                }
            });
    }

    /**
     * Get the FreeColClient.
     *
     * @return The <code>FreeColClient</code>.
     */
    protected FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Get the GUI.
     *
     * @return The <code>GUI</code>.
     */
    protected GUI getGUI() {
        return freeColClient.getGUI();
    }

    /**
     * Get the controller.
     *
     * @return The <code>InGameController</code>.
     */
    protected InGameController getController() {
        return freeColClient.getInGameController();
    }

    /**
     * Get the Image library.
     *
     * @return The <code>ImageLibrary</code>.
     */
    protected ImageLibrary getImageLibrary() {
        return getGUI().getImageLibrary();
    }

    /**
     * Get the game.
     *
     * @return The <code>Game</code>.
     */
    protected Game getGame() {
        return freeColClient.getGame();
    }

    /**
     * Gets the specification.
     *
     * @return The specification from the game.
     */
    protected Specification getSpecification() {
        return getGame().getSpecification();
    }

    /**
     * Get the player.
     *
     * @return The current <code>Player</code>.
     */
    protected Player getMyPlayer() {
        return freeColClient.getMyPlayer();
    }

    /**
     * Get the response that was set by {@link JOptionPane#setValue} and
     * clean up the dialog.  Used by implementors of getResponse().
     *
     * @return The pane value.
     */
    protected Object getValue() {
        Object value = pane.getValue();
        dispose(); // Pane will now be null following removeNotify().
        return value;
    }

    /**
     * Set the value of this dialog.
     *
     * @param value The new value.
     */
    protected synchronized void setValue(Object value) {
        this.pane.setValue(value);
    }

    /**
     * Has this dialog been given a response.
     *
     * @return True if the dialog has a response.
     */
    public synchronized boolean responded() {
        return this.pane != null
            && this.pane.getValue() != JOptionPane.UNINITIALIZED_VALUE;
    }

    /**
     * Get the response from this dialog.
     *
     * @return The response from this dialog.
     */
    public T getResponse() {
        if (responded()) {
            Object value = getValue();
            for (ChoiceItem<T> ci : this.options) {
                if (ci.equals(value)) return ci.getObject();
            }
        }
        return null;
    }

    /**
     * Is this a modal dialog?
     *
     * @return True if this is a modal dialog.
     */
    public boolean isModal() {
        return modal;
    }

    /**
     * Create a list of choices.
     *
     * @return An empty list of choices.
     */
    public static <T> List<ChoiceItem<T>> choices() {
        return new ArrayList<ChoiceItem<T>>();
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    public void propertyChange(PropertyChangeEvent e) {
        // Let the defaultCloseOperation handle the closing if the
        // user closed the window without selecting a button (in which
        // case the new value will be null).  Otherwise, close the dialog.
        if (this.isVisible()
            && e.getSource() == pane
            && (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)
                || e.getPropertyName().equals(JOptionPane.INPUT_VALUE_PROPERTY))
            && e.getNewValue() != null
            && e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
            this.setVisible(false);
        }
    }


    // Override Dialog

    /**
     * {@inheritDoc}
     */
    public void setVisible(boolean val) {
        if (val) this.pane.selectInitialValue();
        super.setVisible(val); // This is where the thread blocks when modal.
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        freeColClient.getGUI().getCanvas()
            .dialogRemove(FreeColDialog.this);

        removeAll();
        if (this.pane != null) {
            this.pane.removePropertyChangeListener(this);
            this.pane = null;
        }
        this.scrollPane = null;

        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }
}
