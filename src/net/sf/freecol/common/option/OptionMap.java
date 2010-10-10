/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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


package net.sf.freecol.common.option;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Specification;

import org.w3c.dom.Element;


/**
* Used for grouping objects of {@link Option}.
*/
public abstract class OptionMap extends OptionGroup {

    private static Logger logger = Logger.getLogger(OptionMap.class.getName());

    private String xmlTagName;
   
    /**
     * Describe specification here.
     */
    private Specification specification;


    /**
     * Creates a new <code>OptionMap</code>.
     *
     * @param xmlTagName The tag name that should be used for the parent XML-element
     *           returned by {@link Option#toXMLElement}.
     */
    public OptionMap(String xmlTagName, Specification specification) {
        super(xmlTagName);
        this.xmlTagName = xmlTagName;
        this.specification = specification;
        
        addDefaultOptions();
    }

    /**
     * Creates an <code>OptionMap</code> from an XML representation.
     *
     * <br><br>
     *
     * @param in The XML stream to read the data from.
     * @param xmlTagName The tag name that should be used for the parent XML-element
     *           returned by {@link Option#toXMLElement}.
     * @exception XMLStreamException if an error occured during parsing.
     */
    public OptionMap(XMLStreamReader in, String xmlTagName, Specification spec) throws XMLStreamException {
        this(xmlTagName, spec);
        readFromXML(in);
    }

    /**
     * Creates an <code>OptionMap</code> from an XML representation.
     *
     * <br><br>
     *
     * @param element The XML <code>Element</code> from which this object
     *                should be constructed.
     * @param xmlTagName The tag name that should be used for the parent XML-element
     *           returned by {@link Option#toXMLElement}.
     */
    public OptionMap(Element element, String xmlTagName, Specification spec) {
        this(xmlTagName, spec);
        readFromXMLElement(element);
    }


    /**
     * Get the <code>Specification</code> value.
     *
     * @return a <code>Specification</code> value
     */
    public final Specification getSpecification() {
        return specification;
    }

    /**
     * Set the <code>Specification</code> value.
     *
     * @param newSpecification The new Specification value.
     */
    public final void setSpecification(final Specification newSpecification) {
        this.specification = newSpecification;
    }


    /**
     * Adds the default options to this <code>OptionMap</code>.
     * Needs to be implemented by subclasses.
     */
    protected abstract void addDefaultOptions();


    /**
     * Gets the <code>File</code> specified by an option.
     *
     * @param id The id of the option.
     * @return The value.
     * @exception IllegalArgumentException If there is no <code>File</code>
     *            associated with the specified option.
     * @exception NullPointerException if the given <code>Option</code> does not exist.
     */
    public File getFile(String id) {
        try {
            return ((FileOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No File associated with the specified option.");
        }
    }

    /**
     * Creates a <code>XMLStreamReader</code> for reading the given file.
     * Compression is automatically detected.
     * 
     * @param file The file to be read.
     * @return The <code>XMLStreamReader</code>.
     * @exception IOException if thrown while loading the game or if a
     *                <code>XMLStreamException</code> have been thrown by the
     *                parser.
     */
    private static XMLStreamReader createXMLStreamReader(File file) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        // Automatically detect compression:
        in.mark(10);
        byte[] buf = new byte[5];
        in.read(buf, 0, 5);
        in.reset();
        if (!(new String(buf)).equals("<?xml")) {
            in = new BufferedInputStream(new InflaterInputStream(in));
        }
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            return xif.createXMLStreamReader(in, "UTF-8");
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException("XMLStreamException.");
        }
    }
    
    abstract protected boolean isCorrectTagName(String tagName);

    /**
     * Reads the options from the given file.
     * 
     * @param loadFile The <code>File</code> to read the
     *            options from.
     */
    public void load(File loadFile) {
        load(loadFile, true);
    }

    /**
     * Reads the options from the given file.
     * 
     * @param loadFile The <code>File</code> to read the
     *            options from.
     * @param update a <code>boolean</code> value
     */
    public void load(File loadFile, boolean update) {
        if (loadFile == null || !loadFile.exists()) {
            logger.warning("Could not find the client options file.");
            return;
        }
        
        XMLStreamReader in = null;
        try {
            in = createXMLStreamReader(loadFile);
            in.nextTag();
            while (!isCorrectTagName(in.getLocalName())) {
                in.nextTag();
            }
            if (update) {
                logger.finest("Updating " + getId() + " from " + loadFile.getPath());
                updateFromXML(in);
            } else {
                logger.finest("Loading " + getId() + " from " + loadFile.getPath());
                readFromXML(in);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while loading options.", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception while closing stream.", e);
            }
        }
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *  
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        // This method is identical to the superclass method,
        // EXCEPT FOR the tag name
        // Start element:
        out.writeStartElement(xmlTagName);

        Iterator<Option> it = iterator();
        while (it.hasNext()) {
            (it.next()).toXML(out);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    private void updateFromXML(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(OptionGroup.getXMLElementTagName())) {
                updateFromXML(in);
            } else {
                final String idStr = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                if (idStr != null) {
                    // old protocols:
                    Option o = getOption(idStr);

                    if (o != null) {
                        o.readFromXML(in);
                    } else {
                        // Normal only if this option is from an old save game:
                        logger.info("Option \"" + idStr + "\" (" + in.getLocalName() + ") could not be found.");
                        
                        // Ignore the option:
                        final String ignoredTag = in.getLocalName();
                        while (in.nextTag() != XMLStreamConstants.END_ELEMENT
                                || !in.getLocalName().equals(ignoredTag));
                    }
                } else {
                    Option o = getOption(in.getLocalName());
                    if (o != null) {
                        o.readFromXML(in);
                    } else {
                        // Normal only if this option is from an old save game:
                        logger.info("Option \"" + in.getLocalName() + " not found.");

                        // Ignore the option:
                        final String ignoredTag = in.getLocalName();
                        while (in.nextTag() != XMLStreamConstants.END_ELEMENT
                                || !in.getLocalName().equals(ignoredTag));
                    }
                }
            }
        }
        // DONE BY while-loop: in.nextTag();
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @exception UnsupportedOperationException at any time, since this
    *            class should get it's XML tag name in the
    *            {@link #OptionMap(String) constructor}
    */
    public static String getXMLElementTagName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Allow a File to be set in other ways, as from the user using the Client.
     * 
     * @param id
     * @param newFileValue
     */
    public void setFile(String id, File newFileValue) {
    	if( id == null || id.trim().length() == 0 )
    		throw new IllegalArgumentException("Requires an ID");
    	if( newFileValue == null )
    		throw new IllegalArgumentException("Requires a File parameter");
    	if( getOption(id) == null )
    		throw new IllegalArgumentException("No option with ID=["+ id +"]");
    	
        try {
            ((FileOption) getOption(id)).setValue(newFileValue);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No File associated with option ["+ id +"].");
        }
    }
}
