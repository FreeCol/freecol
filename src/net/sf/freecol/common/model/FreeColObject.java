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

package net.sf.freecol.common.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class FreeColObject {

    protected static Logger logger = Logger.getLogger(FreeColObject.class.getName());

    /**
     * XML tag name for ID attribute.
     */
    protected static final String ID_ATTRIBUTE = "ID";

    /**
     * XML tag name for array elements.
     */
    protected static final String ARRAY_SIZE = "xLength";

    /**
     * Unique identifier of an object
     */
    private String id;

    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    protected void setId(final String newId) {
        this.id = newId;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */    
    public Element toXMLElement(Player player, Document document) {
        return toXMLElement(player, document, false, false);
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @return An XML-representation of this object.
     */    
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        try {
            StringWriter sw = new StringWriter();
            XMLOutputFactory xif = XMLOutputFactory.newInstance();
            XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
            toXML(xsw, player, showAll, toSavedGame);
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
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */    
    public Element toXMLElement(Document document) {
        return toXMLElement(null, document, false, false);
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public void toXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        toXMLImpl(out);
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
     * This method writes an XML-representation of this object to
     * the given stream. Only attributes visible to the given
     * <code>Player</code> will be added to that representation.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation is
     *               made for.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     * @see #toXML(XMLStreamWriter, Player, boolean, boolean)
     */
    public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
        toXML(out, player, false, false);
    }   
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * All attributes will be made visible.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     * @see #toXML(XMLStreamWriter, Player, boolean, boolean)
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, null, true, false);
    }        

    /**
     * Initialize this object from an XML-representation of this object.
     * @param element An XML-element that will be used to initialize
     *      this object.
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

    /**
     * Initializes this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected abstract void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException;
    

    /**
     * Creates an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param array The array to represent.
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toArrayElement(String tagName, int[] array, XMLStreamWriter out)
        throws XMLStreamException {
        out.writeStartElement(tagName);
        
        out.writeAttribute(ARRAY_SIZE, Integer.toString(array.length));
        for (int x=0; x < array.length; x++) {
            out.writeAttribute("x" + Integer.toString(x), Integer.toString(array[x]));
        }
        
        out.writeEndElement();
    }
     
    /**
     * Reads an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param arrayType The type of array to be read.
     * @return The array.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */               
    protected int[] readFromArrayElement(String tagName, XMLStreamReader in, int[] arrayType)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        
        int[] array = new int[Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE))];
        
        for (int x=0; x<array.length; x++) {
            array[x] = Integer.parseInt(in.getAttributeValue(null, "x" + Integer.toString(x)));
        }
        
        in.nextTag();
        return array;
    }

    /**
     * Creates an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param array The array to represent.
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toArrayElement(String tagName, String[] array, XMLStreamWriter out)
        throws XMLStreamException {
        out.writeStartElement(tagName);
        
        out.writeAttribute(ARRAY_SIZE, Integer.toString(array.length));
        for (int x=0; x < array.length; x++) {
            out.writeAttribute("x" + Integer.toString(x), array[x]);
        }
        
        out.writeEndElement();
    }
    

    /**
     * Reads an XML-representation of an array.
     * 
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param arrayType The type of array to be read.
     * @return The array.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */               
    protected String[] readFromArrayElement(String tagName, XMLStreamReader in, String[] arrayType)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        String[] array = new String[Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE))];        
        for (int x=0; x<array.length; x++) {
            array[x] = in.getAttributeValue(null, "x" + Integer.toString(x));
        }
        
        in.nextTag();
        return array;
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @return an <code>int</code> value
     */
    public boolean hasAttribute(XMLStreamReader in, String attributeName) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        return attributeString != null;
    }
    
    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue an <code>int</code> value
     * @return an <code>int</code> value
     */
    public int getAttribute(XMLStreamReader in, String attributeName, int defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return Integer.parseInt(attributeString);
        } else {
            return defaultValue;
        }
    }
    
    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue a <code>float</code> value
     * @return an <code>int</code> value
     */
    public float getAttribute(XMLStreamReader in, String attributeName, float defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return Float.parseFloat(attributeString);
        } else {
            return defaultValue;
        }
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue a <code>boolean</code> value
     * @return an <code>boolean</code> value
     */
    public boolean getAttribute(XMLStreamReader in, String attributeName, boolean defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return Boolean.parseBoolean(attributeString);
        } else {
            return defaultValue;
        }
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue an <code>String</code> value
     * @return an <code>String</code> value
     */
    public String getAttribute(XMLStreamReader in, String attributeName, String defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return attributeString;
        } else {
            return defaultValue;
        }
    }

    /**
     * Write an ID attribute if object is not null.
     *
     * @param out a <code>XMLStreamWriter</code> value
     * @param attributeName a <code>String</code> value
     * @param object a <code>FreeColObject</code> value
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName, FreeColObject object)
        throws XMLStreamException {
        if (object != null) {
            out.writeAttribute(attributeName, object.getId());
        }
    }

    public void writeFreeColGameObject(FreeColGameObject object, XMLStreamWriter out, Player player,
                                   boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        if (object != null) {
            object.toXMLImpl(out, player, showAll, toSavedGame);
        }
    }

    /**
     * Gets the tag name used to serialize this object, generally the
     * class name starting with a lower case letter. This method
     * should be overridden by all subclasses that need to be
     * serialized.
     *
     * @return <code>null</code>.
     */
    public static String getXMLElementTagName() {
        return null;
    }

}

