package net.sf.freecol.common.option;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

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

import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The super class of all options.
 */
abstract public class AbstractOption implements Option {
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String NO_ID = "NO_ID";

    private static Logger logger = Logger.getLogger(AbstractOption.class.getName());

    private ArrayList<PropertyChangeListener> propertyChangeListeners = new ArrayList<PropertyChangeListener>();

    private String id;

    private String name;

    private String shortDescription;


    /**
     * Creates a new <code>AbstractOption</code>.
     * 
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     * @param name The name of the <code>AbstractOption</code>. This text is
     *            used for identifying the option for a user. Example: The text
     *            related to a checkbox.
     * @param shortDescription Should give a short description of the
     *            <code>Option</code>. This might be used as a tooltip text.
     */
    public AbstractOption(String id, String name, String shortDescription) {
        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
    }

    /**
     * Adds a new <code>PropertyChangeListener</code> for monitoring state
     * changes. Events are generated when variables are changed.
     * 
     * @param pcl The <code>PropertyChangeListener</code> to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeListeners.add(pcl);
    }

    /**
     * Remove the given <code>PropertyChangeListener</code>.
     * 
     * @param pcl The <code>PropertyChangeListener</code> to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeListeners.remove(pcl);
    }

    /**
     * Fires a <code>PropertyChangeEvent</code> to all listeners.
     * 
     * @param name The name of the changed variable.
     * @param oldValue The old value.
     * @param newValue The new value.
     */
    protected void firePropertyChange(String name, Object oldValue, Object newValue) {
        Iterator<PropertyChangeListener> it = propertyChangeListeners.iterator();
        while (it.hasNext()) {
            it.next().propertyChange(new PropertyChangeEvent(this, name, oldValue, newValue));
        }
    }

    /**
     * Gives a short description of this <code>Option</code>. Can for
     * instance be used as a tooltip text.
     * 
     * @return A short description of this <code>Option</code>.
     */
    public String getShortDescription() {
        return Messages.message(shortDescription);
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
     * @return The unique identifier as provided in the constructor.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of this <code>Option</code>.
     * 
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return Messages.message(name);
    }

    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out);
    }

    /**
     * Initializes this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }

    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    abstract protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException;

    /**
     * Initializes this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    abstract protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException;

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
            IllegalStateException ex = new IllegalStateException("XMLStreamException");
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "option".
     */
    public static String getXMLElementTagName() {
        return "option";
    }

}
