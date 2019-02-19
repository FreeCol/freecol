/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;


/**
 * Superclass for all panels in FreeCol.
 */
public abstract class FreeColPanel extends MigPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());

    // Create some constants that the panels can use to for various states/actions.
    protected static final String CANCEL = "CANCEL";
    protected static final String OK = "OK";
    protected static final String HELP = "HELP";

    // Create some constants that can be used for layout contraints
    protected static final String SPAN_SPLIT_2 = "span, split 2";
    protected static final String NL_SPAN_SPLIT_2 = "newline, span, split 2";

    // The margin to use.
    protected static final int MARGIN = 3;

    private final FreeColClient freeColClient;

    protected boolean editable = true;

    protected JButton okButton = Utility.localizedButton("ok");


    /**
     * Constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    protected FreeColPanel(FreeColClient freeColClient) {
        this(freeColClient, null, new FlowLayout());
    }

    /**
     * Default constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param uiClassId An optional L+F class to render this component.
     * @param layout The {@code LayoutManager} to be used.
     */
    protected FreeColPanel(FreeColClient freeColClient, String uiClassId,
                           LayoutManager layout) {
        super(uiClassId, layout);

        this.freeColClient = freeColClient;

        setBorder(FreeColImageBorder.imageBorder);

        okButton.setActionCommand(OK);
        okButton.addActionListener(this);
        setCancelComponent(okButton);
    }


    /**
     * Get the FreeColClient.
     *
     * @return The current {@code FreeColClient}.
     */
    protected final FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Is this panel editable?
     *
     * @return True if the panel is editable.
     */
    protected final boolean isEditable() {
        return editable;
    }

    /**
     * Get the game.
     *
     * @return The current {@code Game}.
     */
    protected final Game getGame() {
        return freeColClient.getGame();
    }

    /**
     * Get the GUI.
     *
     * @return The current {@code GUI}.
     */
    protected final GUI getGUI() {
        return freeColClient.getGUI();
    }

    /**
     * Get the image library.
     *
     * @return The {@code ImageLibrary}.
     */
    protected final ImageLibrary getImageLibrary() {
        return getGUI().getImageLibrary();
    }

    /**
     * Get the game specification.
     *
     * @return The {@code Specification}.
     */
    protected Specification getSpecification() {
        return freeColClient.getGame().getSpecification();
    }

    /**
     * Get the player.
     *
     * @return The client {@code Player}.
     */
    protected final Player getMyPlayer() {
        return freeColClient.getMyPlayer();
    }

    /**
     * Get the client options.
     *
     * @return The {@code ClientOptions}.
     */
    protected final ClientOptions getClientOptions() {
        return (freeColClient == null) ? null
            : freeColClient.getClientOptions();
    }

    /**
     * Get the client controller.
     *
     * @return The client {@code InGameController}.
     */
    public final InGameController igc() {
        return freeColClient.getInGameController();
    }

    /**
     * Create a button for a colony.
     *
     * @param colony The {@code Colony} to create a button for.
     * @return The new button.
     */
    public JButton createColonyButton(Colony colony) {
        JButton button = Utility.getLinkButton(colony.getName(), null,
                                               colony.getId());
        button.addActionListener(this);
        return button;
    }

    /**
     * Make the given button the CANCEL button.
     *
     * @param cancelButton an {@code AbstractButton} value
     */
    public final void setCancelComponent(AbstractButton cancelButton) {
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
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    if ("closing".equals(e.getPropertyName())) {
                        runnable.run();
                        // Lambda unsuitable due to use of "this"
                        FreeColPanel.this.removePropertyChangeListener(this);
                    }
                }
            });
    }

    /**
     * Helper to get a small single abstract unit image.
     *
     * @param au The {@code AbstractUnit} to examine.
     * @return A suitable {@code BufferedImage}.
     */
    public BufferedImage getSmallAbstractUnitImage(AbstractUnit au) {
        final Specification spec = getSpecification();
        return getImageLibrary().getSmallUnitTypeImage(au.getType(spec),
                                                       au.getRoleId(), false);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (OK.equals(command)) {
            getGUI().removeComponent(this);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        // The OK button requests focus if it exists.
        if (okButton != null) okButton.requestFocus();
    }
}
