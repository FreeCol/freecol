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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
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
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MigPanel;
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
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Superclass for all panels in FreeCol.
 */
public abstract class FreeColPanel extends MigPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());

    protected static final String CANCEL = "CANCEL";
    protected static final String OK = "OK";
    protected static final String HELP = "HELP";

    // The margin to use.
    protected static final int MARGIN = 3;

    // The borders to use for table cells
    public static final Border TOPCELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 1, GUI.BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    public static final Border CELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 1, GUI.BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    public static final Border LEFTCELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 1, 1, GUI.BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    public static final Border TOPLEFTCELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, GUI.BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));


    private FreeColClient freeColClient;

    protected boolean editable = true;

    protected JButton okButton = new JButton(Messages.message("ok"));


    /**
     * Constructor.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public FreeColPanel(FreeColClient freeColClient) {
        this(freeColClient, new FlowLayout());
    }

    /**
     * Default constructor.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param layout The <code>LayoutManager</code> to be used.
     */
    public FreeColPanel(FreeColClient freeColClient, LayoutManager layout) {
        super(layout);

        this.freeColClient = freeColClient;
        setFocusCycleRoot(true);

        setBorder(FreeColImageBorder.imageBorder);

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {});

        okButton.setActionCommand(OK);
        okButton.addActionListener(this);
        enterPressesWhenFocused(okButton);
        setCancelComponent(okButton);
    }


    /**
     * Get the FreeColClient.
     *
     * @return The current <code>FreeColClient</code>.
     */
    protected FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Is this panel editable?
     *
     * @return True if the panel is editable.
     */
    protected boolean isEditable() {
        return editable;
    }

    /**
     * Get the game.
     *
     * @return The current <code>Game</code>.
     */
    protected Game getGame() {
        return freeColClient.getGame();
    }

    /**
     * Get the GUI.
     *
     * @return The current <code>GUI</code>.
     */
    protected GUI getGUI() {
        return freeColClient.getGUI();
    }

    /**
     * Get the image library.
     *
     * @return The <code>ImageLibrary</code>.
     */
    protected ImageLibrary getLibrary() {
        return getGUI().getImageLibrary();
    }

    /**
     * Get the game specification.
     *
     * @return The <code>Specification</code>.
     */
    protected Specification getSpecification() {
        return freeColClient.getGame().getSpecification();
    }

    /**
     * Get the player.
     *
     * @return The client <code>Player</code>.
     */
    protected Player getMyPlayer() {
        return freeColClient.getMyPlayer();
    }

    /**
     * Get the client options.
     *
     * @return The <code>ClientOptions</code>.
     */
    protected ClientOptions getClientOptions() {
        return (freeColClient == null) ? null
            : freeColClient.getClientOptions();
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
     * Get a JLabel with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The <code>JLabel</code>.
     */
    protected JLabel localizedLabel(String key) {
        return new JLabel(Messages.message(key));
    }

    /**
     * Get a JLabel with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @return The <code>JLabel</code>.
     */
    protected JLabel localizedLabel(StringTemplate template) {
        return new JLabel(Messages.message(template));
    }

    /**
     * The OK button requests focus.
     */
    @Override
    public void requestFocus() {
        if (okButton != null) okButton.requestFocus();
    }

    /**
     * Create a button for a colony.
     *
     * @param colony The <code>Colony</code> to create a button for.
     * @return The new button.
     */
    public JButton createColonyButton(Colony colony) {
        JButton button = GUI.getLinkButton(colony.getName(), null,
                                           colony.getId());
        button.addActionListener(this);
        return button;
    }

    /**
     * Make the given button the CANCEL button.
     *
     * @param cancelButton an <code>AbstractButton</code> value
     */
    public void setCancelComponent(AbstractButton cancelButton) {
        if (cancelButton == null) throw new NullPointerException();

        InputMap inputMap
            = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
                     "release");

        Action cancelAction = cancelButton.getAction();
        getActionMap().put("release", cancelAction);
    }

    /**
     * Add a routine to be called when this panel closes.
     * Triggered by Canvas.notifyClose.
     *
     * @param runnable Some code to run on close.
     */
    public void addClosingCallback(final Runnable runnable) {
        addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    if ("closing".equals(e.getPropertyName())) {
                        runnable.run();
                        FreeColPanel.this.removePropertyChangeListener(this);
                    }
                }
            });
    }

    /**
     * Registers enter key for a JButton.
     *
     * @param button
     */
    public static void enterPressesWhenFocused(JButton button) {
        button.registerKeyboardAction(button.getActionForKeyStroke(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
            JComponent.WHEN_FOCUSED);

        button.registerKeyboardAction(button.getActionForKeyStroke(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true)),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true),
            JComponent.WHEN_FOCUSED);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        if (command.equals(OK)) {
            getGUI().removeFromCanvas(this);
        } else {
            logger.warning("Bad event: " + command);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        // removeNotify gets called when a JPanel has no parent any
        // more, that is the best opportunity available for JPanels
        // to be given a chance to remove leak generating references.

        if (okButton == null) return; // Been here before

        // We need to make sure the layout is cleared because some
        // versions of MigLayout are leaky.
        setLayout(null);

        okButton.removeActionListener(this);
        okButton = null;

        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }
}
