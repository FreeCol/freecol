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

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.dialog.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.option.Option;


/**
 * The super class of all actions in FreeCol.  Subclasses of this
 * object is stored in an {@link ActionManager}.
 */
public abstract class FreeColAction extends AbstractAction
    implements Option<FreeColAction> {

    /** Protected to congregate the subclasses here. */
    protected static final Logger logger = Logger.getLogger(FreeColAction.class.getName());

    public static final String TAG = "action";

    /**
     * There are four versions of the order buttons: normal, rollover,
     * pressed, disabled.
     */
    private static final int ORDER_BUTTON_COUNT = 4;
    
    /**
     * A class used by Actions which have a mnemonic. Those Actions should
     * assign this listener to the JMenuItem they are a part of. This captures
     * the mnemonic key press and keeps other menus from processing keys meant
     * for other actions.
     *
     * @author johnathanj
     */
    public class InnerMenuKeyListener implements MenuKeyListener {

        final int mnemonic;

        public InnerMenuKeyListener() {
            mnemonic = ((Integer) getValue(MNEMONIC_KEY));
        }

        @Override
        public void menuKeyPressed(MenuKeyEvent e) {

            if (e.getKeyCode() == mnemonic) {
                ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), (String) getValue(Action.NAME),
                                                 e.getModifiers());
                actionPerformed(ae);

                e.consume();
            }
        }

        @Override
        public void menuKeyReleased(MenuKeyEvent e) {
            // do nothing
        }

        @Override
        public void menuKeyTyped(MenuKeyEvent e) {
            // do nothing
        }
    }

    public static final String ACTION_ID = "ACTION_ID";
    public static final String BUTTON_IMAGE = "BUTTON_IMAGE";
    public static final String BUTTON_ROLLOVER_IMAGE = "BUTTON_ROLLOVER_IMAGE";
    public static final String BUTTON_PRESSED_IMAGE = "BUTTON_PRESSED_IMAGE";
    public static final String BUTTON_DISABLED_IMAGE = "BUTTON_DISABLED_IMAGE";

    protected final FreeColClient freeColClient;

    private int orderButtonImageCount = 0;


    /**
     * Creates a new {@code FreeColAction}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param id The object identifier for this action.
     */
    protected FreeColAction(FreeColClient freeColClient, String id) {
        super(Messages.getName(id));

        this.freeColClient = freeColClient;

        putValue(ACTION_ID, id);

        String shortDescription = Messages.getDescription(id);
        if (!shortDescription.equals(Messages.descriptionKey(id))) {
            putValue(SHORT_DESCRIPTION, shortDescription);
        }

        String acceleratorKey = id + ".accelerator";
        String accelerator = Messages.message(acceleratorKey);
        if (!accelerator.equals(acceleratorKey)) {
            setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
    }


    /**
     * Gets the main controller object for the client.
     *
     * @return The main controller object for the client.
     */
    protected FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Gets the game.
     *
     * @return The {@code Game}.
     */
    protected Game getGame() {
        return freeColClient.getGame();
    }

    /**
     * Get the GUI.
     *
     * @return The GUI.
     */
    protected GUI getGUI() {
        return freeColClient.getGUI();
    }

    /**
     * Get the controller.
     *
     * @return The {@code InGameController}.
     */
    protected InGameController igc() {
        return freeColClient.getInGameController();
    }

    /**
     * Get the connect controller.
     *
     * @return The {@code ConnectController}.
     */
    protected ConnectController getConnectController() {
        return freeColClient.getConnectController();
    }

    /**
     * Get the action manager.
     *
     * @return The {@code ActionManager}.
     */
    protected ActionManager getActionManager() {
        return freeColClient.getActionManager();
    }

    /**
     * Get the client options
     *
     * @return The {@code ClientOptions}.
     */
    protected ClientOptions getClientOptions() {
        return freeColClient.getClientOptions();
    }


    /**
     * Don't use this method.
     */
    @Override
    public FreeColAction cloneOption() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("FreeColAction can not be cloned:" + this);
    }

    /**
     * Gets the mnemonic to be used for selecting this action
     *
     * @return The mnemonic of the action
     */
    public Integer getMnemonic() {
        return (Integer)getValue(MNEMONIC_KEY);
    }

    public void setMnemonic(int mnemonic) {
        putValue(MNEMONIC_KEY, mnemonic);
    }

    /**
     * Are all the order button images present?
     *
     * @return True if all the order button images are present.
     */
    public boolean hasOrderButtons() {
        return orderButtonImageCount == ORDER_BUTTON_COUNT;
    }

    /**
     * Adds icons for the order buttons.
     *
     * @param key The identifier of the action.
     */
    protected void addImageIcons(String key) {
        List<BufferedImage> images = ImageLibrary.getButtonImages(key);
        orderButtonImageCount = images.size();
        if (hasOrderButtons()) {
            putValue(BUTTON_IMAGE, new ImageIcon(images.remove(0)));
            putValue(BUTTON_ROLLOVER_IMAGE, new ImageIcon(images.remove(0)));
            putValue(BUTTON_PRESSED_IMAGE, new ImageIcon(images.remove(0)));
            putValue(BUTTON_DISABLED_IMAGE, new ImageIcon(images.remove(0)));
        } else {
            logger.warning("Found only " + orderButtonImageCount
                + " order button images for " + getId() + "/" + key);
        }
    }

    /**
     * Sets a keyboard accelerator.
     *
     * @param accelerator The {@code KeyStroke}. Using {@code null}
     *            is the same as disabling the keyboard accelerator.
     */
    public final void setAccelerator(KeyStroke accelerator) {
        putValue(ACCELERATOR_KEY, accelerator);
    }

    /**
     * Gets the keyboard accelerator for this option.
     *
     * @return The {@code KeyStroke} or {@code null} if the
     *         keyboard accelerator is disabled.
     */
    public final KeyStroke getAccelerator() {
        return (KeyStroke) getValue(ACCELERATOR_KEY);
    }

    /**
     * Gives a short description of this {@code Option}. Can for
     * instance be used as a tooltip text.
     *
     * @return A short description of this action.
     */
    public final String getShortDescription() {
        return (String) getValue(SHORT_DESCRIPTION);
    }

    /**
     * Get the identifier of this {@code Option}.
     *
     * @return An unique identifier for this action.
     */
    @Override
    public final String getId() {
        return (String) getValue(ACTION_ID);
    }

    /**
     * Returns the name of this {@code Option}.
     *
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return (String) getValue(NAME);
    }

    /**
     * Creates a {@code String} that keeps the attributes given
     * {@code KeyStroke}. This {@code String} can be used to
     * store the key stroke in an XML-file.
     *
     * @param keyStroke The {@code KeyStroke}.
     * @return A {@code String} that produces a key stroke equal to the
     *         given {@code KeyStroke} if passed as a parameter to
     *         {@code getAWTKeyStroke(String)}.
     */
    private static String getKeyStrokeText(KeyStroke keyStroke) {
        return (keyStroke == null) ? "" : keyStroke.toString();
    }


    public MenuKeyListener getMenuKeyListener() {
        return new InnerMenuKeyListener();
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return True if the {@link ClientOptionsDialog}
     *     is not visible.
     */
    protected boolean shouldBeEnabled() {
        return !getGUI().isClientOptionsDialogShowing();
    }

    /**
     * Updates the "enabled"-status with the value returned by
     * {@link #shouldBeEnabled}.
     */
    public void update() {
        boolean b = shouldBeEnabled();
        if (isEnabled() != b) setEnabled(b);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGroup() {
        return getActionManager().getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGroup(String group) {
        logger.warning("Unsupported method FreeColAction.setGroup.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColAction getValue() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(FreeColAction value) {
        logger.warning("Unsupported method FreeColAction.setValue.");
    }


    // Serialization
    // This is not actually a FreeColObject, so the serialization is
    // less elaborate.
    
    private static final String ACCELERATOR_TAG = "accelerator";


    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @throws XMLStreamException if there is a problem writing to the stream.
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeStartElement(TAG);

        xw.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG, getId());

        xw.writeAttribute(ACCELERATOR_TAG, getKeyStrokeText(getAccelerator()));

        xw.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    @Override
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        // id is hard-wired
        String acc = xr.getAttribute(ACCELERATOR_TAG, "");
        putValue(ACCELERATOR_KEY, (acc == null || acc.isEmpty()) ? null
            : KeyStroke.getKeyStroke(acc));
        xr.closeTag(TAG);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }
}
