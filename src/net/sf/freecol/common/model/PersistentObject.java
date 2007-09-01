
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

public abstract class PersistentObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    protected static Logger logger = Logger.getLogger(PersistentObject.class.getName());


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
    public abstract void toXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException;

    
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
     * All attributes will be made visable.
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
    protected void  toArrayElement(String tagName, int[] array, XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(tagName);
        
        out.writeAttribute("xLength", Integer.toString(array.length));
        for (int x=0; x < array.length; x++) {
            out.writeAttribute("x" + Integer.toString(x), Integer.toString(array[x]));
        }
        
        out.writeEndElement();
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
    protected void  toArrayElement(String tagName, float[] array, XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(tagName);
        
        out.writeAttribute("xLength", Integer.toString(array.length));
        for (int x=0; x < array.length; x++) {
            out.writeAttribute("x" + Integer.toString(x), Float.toString(array[x]));
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
    protected float[] readFromArrayElement(String tagName, XMLStreamReader in, float[] arrayType) throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        
        float[] array = new float[Integer.parseInt(in.getAttributeValue(null, "xLength"))];
        
        for (int x=0; x<array.length; x++) {
            array[x] = Float.parseFloat(in.getAttributeValue(null, "x" + Integer.toString(x)));
        }
        
        in.nextTag();
        return array;
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
    protected int[] readFromArrayElement(String tagName, XMLStreamReader in, int[] arrayType) throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        
        int[] array = new int[Integer.parseInt(in.getAttributeValue(null, "xLength"))];
        
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
    protected void toArrayElement(String tagName, int[][] array, XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(tagName);
        
        out.writeAttribute("xLength", Integer.toString(array.length));
        out.writeAttribute("yLength", Integer.toString(array[0].length));
        for (int x=0; x < array.length; x++) {
            for (int y=0; y < array[0].length; y++) {
                out.writeAttribute("x" + Integer.toString(x) + "y" + Integer.toString(y), Integer.toString(array[x][y]));
            }
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
    protected int[][] readFromArrayElement(String tagName, XMLStreamReader in, int[][] arrayType) throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        
        int[][] array = new int[Integer.parseInt(in.getAttributeValue(null, "xLength"))][Integer.parseInt(in.getAttributeValue(null, "yLength"))];
        
        for (int x=0; x<array.length; x++) {
            for (int y=0; y<array[0].length; y++) {
                array[x][y] = Integer.parseInt(in.getAttributeValue(null, "x" + Integer.toString(x) + "y" + Integer.toString(y)));
            }
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
    protected void toArrayElement(String tagName, boolean[][] array, XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(tagName);
        
        out.writeAttribute("xLength", Integer.toString(array.length));
        out.writeAttribute("yLength", Integer.toString(array[0].length));
        
        StringBuffer sb = new StringBuffer(array.length * array[0].length);
        for (int x=0; x < array.length; x++) {
            for (int y=0; y < array[0].length; y++) {
                if (array[x][y]) {
                    sb.append("1");
                } else {
                    sb.append("0");
                }
            }
        }
        
        out.writeAttribute("data", sb.toString());

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
    protected boolean[][] readFromArrayElement(String tagName, XMLStreamReader in, boolean[][] arrayType) throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        
        boolean[][] array = new boolean[Integer.parseInt(in.getAttributeValue(null, "xLength"))][Integer.parseInt(in.getAttributeValue(null, "yLength"))];

        String data = in.getAttributeValue(null, "data");
        
        for (int x=0; x<array.length; x++) {
            for (int y=0; y<array[0].length; y++) {
                if (data != null) {
                    if (data.charAt(x*array[0].length+y) == '1') {
                        array[x][y] = true;
                    } else {
                        array[x][y] = false;
                    }
                } else { // Old type of storing booleans:
                    array[x][y] = Boolean.valueOf(in.getAttributeValue(null, "x" + Integer.toString(x) + "y" + Integer.toString(y))).booleanValue();
                }
            }
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
    protected void toArrayElement(String tagName, String[] array, XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(tagName);
        
        out.writeAttribute("xLength", Integer.toString(array.length));
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
    protected String[] readFromArrayElement(String tagName, XMLStreamReader in, String[] arrayType) throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        String[] array = new String[Integer.parseInt(in.getAttributeValue(null, "xLength"))];        
        for (int x=0; x<array.length; x++) {
            array[x] = in.getAttributeValue(null, "x" + Integer.toString(x));
        }
        
        in.nextTag();
        return array;
    }

    /**
    * Gets the tag name of the root element representing this object.
    * @return "goods".
    */
    public static String getXMLElementTagName() {
        return null;
    }

}

