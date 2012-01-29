/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The super class of all actions in FreeCol. Subclasses of this object is
 * stored in an {@link ActionManager}.
 */
public abstract class FreeColAction extends AbstractAction implements Option<FreeColAction> {

    private static final Logger logger = Logger.getLogger(FreeColAction.class.getName());

    public static final String ACTION_ID = "ACTION_ID";
    public static final String BUTTON_IMAGE = "BUTTON_IMAGE";
    public static final String BUTTON_ROLLOVER_IMAGE = "BUTTON_ROLLOVER_IMAGE";
    public static final String BUTTON_PRESSED_IMAGE = "BUTTON_PRESSED_IMAGE";
    public static final String BUTTON_DISABLED_IMAGE = "BUTTON_DISABLED_IMAGE";
    public static final Integer NO_MNEMONIC = null;

    protected final FreeColClient freeColClient;

    private int orderButtonImageCount = 0;

    protected GUI gui;


    /**
     * Creates a new <code>FreeColAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param id a <code>String</code> value
     */
    protected FreeColAction(FreeColClient freeColClient, GUI gui, String id) {
        super(Messages.message(id + ".name"));

        this.freeColClient = freeColClient;

        this.gui = gui;

        putValue(ACTION_ID, id);

        String descriptionKey = id + ".shortDescription";
        String shortDescription = Messages.message(descriptionKey);
        if (!shortDescription.equals(descriptionKey)) {
            putValue(SHORT_DESCRIPTION, shortDescription);
        }

        String acceleratorKey = id + ".accelerator";
        String accelerator = Messages.message(acceleratorKey);
        if (!accelerator.equals(acceleratorKey)) {
            setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
    }

    /**
     * Don't use this method.
     */
    public FreeColAction clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("FreeColAction can not be cloned.");
    }

    /**
     * Gets the mnemonic to be used for selecting this action
     *
     * @return The mnemonic of the action
     */
    public Integer getMnemonic() {
        return (Integer) getValue(MNEMONIC_KEY);
    }

    /**
     * Describe <code>setMnemonic</code> method here.
     *
     * @param mnemonic an <code>int</code> value
     */
    public void setMnemonic(int mnemonic) {
        putValue(MNEMONIC_KEY, mnemonic);
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
     * Are all the order button images present?
     *
     * @return True if all the order button images are present.
     */
    public boolean hasOrderButtons() {
        return orderButtonImageCount == 4;
    }

    /**
     * Adds icons for the order buttons.
     *
     * @param key The id of the action.
     */
    protected void addImageIcons(String key) {
        Image normal = ResourceManager.getImage("orderButton.normal." + key);
        Image highlighted = ResourceManager.getImage("orderButton.highlighted." + key);
        Image pressed = ResourceManager.getImage("orderButton.pressed." + key);
        Image disabled = ResourceManager.getImage("orderButton.disabled." + key);
        orderButtonImageCount = ((normal == null) ? 0 : 1)
            + ((highlighted == null) ? 0 : 1)
            + ((pressed == null) ? 0 : 1)
            + ((disabled == null) ? 0 : 1);
        if (hasOrderButtons()) {
            putValue(BUTTON_IMAGE, new ImageIcon(normal));
            putValue(BUTTON_ROLLOVER_IMAGE, new ImageIcon(highlighted));
            putValue(BUTTON_PRESSED_IMAGE, new ImageIcon(pressed));
            putValue(BUTTON_DISABLED_IMAGE, new ImageIcon(disabled));
        } else {
            logger.warning("Missing " + (4-orderButtonImageCount)
                + " orderButton images for " + getId());
        }
    }

    /**
     * Updates the "enabled"-status with the value returned by
     * {@link #shouldBeEnabled}.
     */
    public void update() {
        boolean b = shouldBeEnabled();
        if (isEnabled() != b) {
            setEnabled(b);
        }
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>false</code> if the
     *         {@link net.sf.freecol.client.gui.panel.ClientOptionsDialog} is
     *         visible and <code>true</code> otherwise. This method should be
     *         extended by subclasses if the action should be disabled in other
     *         cases.
     */
    protected boolean shouldBeEnabled() {
        return gui.isClientOptionsDialogShowing();
    }

    /**
     * Sets a keyboard accelerator.
     *
     * @param accelerator The <code>KeyStroke</code>. Using <code>null</code>
     *            is the same as disabling the keyboard accelerator.
     */
    public void setAccelerator(KeyStroke accelerator) {
        putValue(ACCELERATOR_KEY, accelerator);
    }

    /**
     * Gets the keyboard accelerator for this option.
     *
     * @return The <code>KeyStroke</code> or <code>null</code> if the
     *         keyboard accelerator is disabled.
     */
    public KeyStroke getAccelerator() {
        return (KeyStroke) getValue(ACCELERATOR_KEY);
    }

    /**
     * Gives a short description of this <code>Option</code>. Can for
     * instance be used as a tooltip text.
     *
     * @return A short description of this action.
     */
    public String getShortDescription() {
        return (String) getValue(SHORT_DESCRIPTION);
    }

    /**
     * Returns a textual representation of this object.
     *
     * @return The name of this <code>Option</code>.
     * @see #getName
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the id of this <code>Option</code>.
     *
     * @return An unique identifier for this action.
     */
    public String getId() {
        return (String) getValue(ACTION_ID);
    }

    /**
     * Returns the name of this <code>Option</code>.
     *
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return (String) getValue(NAME);
    }

    /**
     * Creates a <code>String</code> that keeps the attributes given
     * <code>KeyStroke</code>. This <code>String</code> can be used to
     * store the key stroke in an XML-file.
     *
     * @param keyStroke The <code>KeyStroke</code>.
     * @return A <code>String</code> that produces a key stroke equal to the
     *         given <code>KeyStroke</code> if passed as a parameter to
     *         <code>getAWTKeyStroke(String)</code>.
     */
    public static String getKeyStrokeText(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return "";
        } else
            return keyStroke.toString();
    }


    /**
     * Returns the action itself. TODO: at the moment, this is only
     * necessary in order to implement Option.
     *
     * @return an <code>FreeColAction</code> value
     */
    public FreeColAction getValue() {
        return this;
    }

    /**
     * Does nothing except log a warning. TODO: at the moment, this is
     * only necessary in order to implement Option.
     *
     * @param value a <code>FreeColAction</code> value
     */
    public void setValue(FreeColAction value) {
        logger.warning("Calling unsupported method setValue.");
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("id", getId());
        out.writeAttribute("accelerator", getKeyStrokeText(getAccelerator()));

        out.writeEndElement();
   }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        String id = in.getAttributeValue(null, "id");
        String acc = in.getAttributeValue(null, "accelerator");

        if (id == null){
            // Old syntax
            id = in.getLocalName();
        }

        if (!acc.equals("")) {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(acc));
        } else {
            putValue(ACCELERATOR_KEY, null);
        }
        in.nextTag();
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }

    public MenuKeyListener getMenuKeyListener() {
        return new InnerMenuKeyListener();
    }


    /**
     * A class used by Actions which have a mnemonic. Those Actions should
     * assign this listener to the JMenuItem they are a part of. This captures
     * the mnemonic key press and keeps other menus from processing keys meant
     * for other actions.
     *
     * @author johnathanj
     */
    public class InnerMenuKeyListener implements MenuKeyListener {

        int mnemonic;


        public InnerMenuKeyListener() {
            mnemonic = ((Integer) getValue(MNEMONIC_KEY)).intValue();
        }

        public void menuKeyPressed(MenuKeyEvent e) {

            if (e.getKeyCode() == mnemonic) {
                ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), (String) getValue(Action.NAME),
                                                 e.getModifiers());
                actionPerformed(ae);

                e.consume();
            }
        }

        public void menuKeyReleased(MenuKeyEvent e) {
            // do nothing
        }

        public void menuKeyTyped(MenuKeyEvent e) {
            // do nothing
        }

    }

    /**
     * Gets the tag name of the root element representing this object.
     * @return "integerOption".
     */
     public static String getXMLElementTagName() {
         return "action";
     }

}
