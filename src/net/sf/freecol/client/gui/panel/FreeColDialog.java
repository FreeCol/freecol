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
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Superclass for all panels in FreeCol.
 */
public class FreeColDialog<T> extends JDialog 
    implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    private static final Border dialogBorder
        = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(10, 20, 10, 20));
        

    /** The enclosing client. */
    private FreeColClient freeColClient;

    /** The options to choose from. */
    private String[] options;

    /** The values to return, corresponding to the options. */
    private T[] values;

    /** The JOptionPane to embed in this dialog. */
    private JOptionPane pane;


    /**
     * Constructor.
     *
     * Much of this was extracted from the source for
     * JOptionPane.createDialog.  We needed a way to control modality.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param obj The main object that explains the choice for the user,
     *     usually just a string, but may be more complex.
     * @param icon An optional icon to display.
     * @param modal Should this dialog be modal?
     * @param options The options to choose from.
     * @param values The corresponding values.
     */
    public FreeColDialog(FreeColClient freeColClient, boolean modal,
                         Object obj, ImageIcon icon,
                         String[] options, T... values) {
        super(freeColClient.getGUI().getFrame());

        this.freeColClient = freeColClient;
        this.options = options;
        this.values = values;
        this.pane = new JOptionPane(obj,
            JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,
            icon, options, options[0]);
        pane.setBorder(dialogBorder);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(pane, BorderLayout.CENTER);

        setFocusCycleRoot(true);
        setComponentOrientation(pane.getComponentOrientation());
        setResizable(false);
        setUndecorated(true);
        setModal(modal);
        pack();
        setLocationRelativeTo(freeColClient.getGUI().getFrame());

        WindowAdapter adapter = new WindowAdapter() {
                private boolean gotFocus = false;

                public void windowClosing(WindowEvent we) {
                    pane.setValue(null);
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
        pane.addPropertyChangeListener(this);
    }
    

    /**
     * Get the response when that was set by {@link JOptionPane#setValue}.
     *
     * @return The response from this dialog.
     */
    public T getResponse() {
        Object value = pane.getValue();
        dispose(); // Pane will now be null following removeNotify().
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) return values[i];
        }
        return null;
    }

    /**
     * Creates a new <code>FreeColDialog</code> with a text and a
     * ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param modal Should this dialog be modal?
     * @param text The text that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button.
     * @return The <code>FreeColDialog</code> created.
     */
    public static FreeColDialog<Boolean>
        createConfirmDialog(final FreeColClient freeColClient, boolean modal,
                            String text, ImageIcon icon,
                            String okText, String cancelText) {
        if (okText == null) okText = "ok";
        if (cancelText == null) cancelText = "cancel";
        String[] options = new String[] {
            Messages.message(okText),
            Messages.message(cancelText)
        };

        return new FreeColDialog<Boolean>(freeColClient, modal,
                                          GUI.getDefaultTextArea(text),
                                          icon, options,
                                          Boolean.TRUE, Boolean.FALSE);
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
        if (val) pane.selectInitialValue();
        super.setVisible(val); // This is where the thread blocks when modal.
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        pane.removePropertyChangeListener(this);
        pane = null;

        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }
}
