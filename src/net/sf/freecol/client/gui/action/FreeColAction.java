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

package net.sf.freecol.client.gui.action;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.resources.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The super class of all actions in FreeCol. Subclasses of this object is
 * stored in an {@link ActionManager}.
 */
public abstract class FreeColAction extends AbstractAction implements Option {

    private static final Logger logger = Logger.getLogger(FreeColAction.class.getName());

    public static final String ACTION_ID = "ACTION_ID";
    public static final String BUTTON_IMAGE = "BUTTON_IMAGE";
    public static final String BUTTON_ROLLOVER_IMAGE = "BUTTON_ROLLOVER_IMAGE";
    public static final String BUTTON_PRESSED_IMAGE = "BUTTON_PRESSED_IMAGE";
    public static final String BUTTON_DISABLED_IMAGE = "BUTTON_DISABLED_IMAGE";
    public static final Integer NO_MNEMONIC = null;

    protected final FreeColClient freeColClient;


    /**
     * Creates a new <code>FreeColAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param id a <code>String</code> value
     */
    protected FreeColAction(FreeColClient freeColClient, String id) {
        super(Messages.message(id + ".name"));

        this.freeColClient = freeColClient;

        putValue(ACTION_ID, id);

        String descriptionKey = id + ".shortDescription";
        String shortDescription = Messages.message(descriptionKey);
        if (!shortDescription.equals(descriptionKey)) {
            putValue(SHORT_DESCRIPTION, descriptionKey);
        }

        String acceleratorKey = id + ".accelerator";
        String accelerator = Messages.message(acceleratorKey);
        if (!accelerator.equals(acceleratorKey)) {
            setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
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

    protected void addImageIcons(String key) {
        Image image = ResourceManager.getImage("orderButton.normal." + key);
        if (image != null) {
            putValue(BUTTON_IMAGE, new ImageIcon(image));
        }
        image = ResourceManager.getImage("orderButton.highlighted." + key);
        if (image != null) {
            putValue(BUTTON_ROLLOVER_IMAGE, new ImageIcon(image));
        }
        image = ResourceManager.getImage("orderButton.pressed." + key);
        if (image != null) {
            putValue(BUTTON_PRESSED_IMAGE, new ImageIcon(image));
        }
        image = ResourceManager.getImage("orderButton.disabled." + key);
        if (image != null) {
            putValue(BUTTON_DISABLED_IMAGE, new ImageIcon());
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
        return freeColClient.getCanvas() != null
                && !freeColClient.getCanvas().isClientOptionsDialogShowing();
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
     * Should this option be updated directly so that
     * changes may be previewes?
     *
     * @return <code>false</code>.
     */
    public boolean isPreviewEnabled() {
        return false;
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

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Document document) {
        try {
            StringWriter sw = new StringWriter();
            XMLOutputFactory xif = XMLOutputFactory.newInstance();
            XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
            toXML(xsw);
            xsw.close();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document tempDocument = null;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                tempDocument = builder.parse(new InputSource(new StringReader(sw.toString())));
                return (Element) document.importNode(tempDocument.getDocumentElement(), true);
            } catch (ParserConfigurationException pce) {
                // Parser with specified options can't be built
                StringWriter swe = new StringWriter();
                pce.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                throw new IllegalStateException("ParserConfigurationException");
            } catch (SAXException se) {
                StringWriter swe = new StringWriter();
                se.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                throw new IllegalStateException("SAXException");
            } catch (IOException ie) {
                StringWriter swe = new StringWriter();
                ie.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                throw new IllegalStateException("IOException");
            }
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
            throw new IllegalStateException("XMLStreamException");
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     */
    public void readFromXMLElement(Element element) {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer xmlTransformer = factory.newTransformer();
                StringWriter stringWriter = new StringWriter();
                xmlTransformer.transform(new DOMSource(element), new StreamResult(stringWriter));
                String xml = stringWriter.toString();
                XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
                xsr.nextTag();
                readFromXML(xsr);
            } catch (TransformerException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warning(sw.toString());
                throw new IllegalStateException("TransformerException");
            }
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
            throw new IllegalStateException("XMLStreamException");
        }
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
