
package net.sf.freecol.server.ai;

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

import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
* An <code>AIObject</code> contains AI-related information and methods.
* Each <code>FreeColGameObject</code>, that is owned by an AI-controlled
* player, can have a single <code>AIObject</code> attached to it.
*/
public abstract class AIObject {
    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final AIMain aiMain;
    protected boolean uninitialized = false;
    protected String id = null;

    
    /**
     * Creates a new <code>AIObject</code>.
     * @param aiMain The main AI-object.
     */
    public AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
    }

    /**
     * Creates a new <code>AIObject</code> and registers
     * this object with <code>AIMain</code>.
     * 
     * @param aiMain The main AI-object.
     * @param id The unique identifier.
     * @see AIMain#addAIObject(String, AIObject)
     */
    public AIObject(AIMain aiMain, String id) {
        this.aiMain = aiMain;
        this.id = id;
        aiMain.addAIObject(id, this);
    }

    
    /**
     * Returns the main AI-object.
     * @return The <code>AIMain</code>.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Checks if this <code>AIObject</code> 
     * is uninitialized. That is: it has been referenced
     * by another object, but has not yet been updated with
     * {@link #readFromXML}.
     * 
     * @return <code>true</code> if this object is not initialized.
     */
    public boolean isUninitialized() {
        return uninitialized;
    }
    
    /**
    * Returns the ID of this <code>AIObject</code>.
    * @return The ID of this <code>AIObject</code>. This is normally
    *         the ID of the {@link FreeColGameObject} this object
    *         represents.
    */
    public abstract String getID();

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
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
                tempDocument = builder.parse(new InputSource(new StringReader(sw.toString())));;
                return (Element) document.importNode(tempDocument.getDocumentElement(), true);
            } catch (ParserConfigurationException pce) {
                // Parser with specified options can't be built
                StringWriter swe = new StringWriter();
                pce.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                IllegalStateException ex = new IllegalStateException("ParserConfigurationException");
                ex.initCause(pce);
                throw ex;
            } catch (SAXException se) {
                StringWriter swe = new StringWriter();
                se.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                IllegalStateException ex = new IllegalStateException("SAXException");
                ex.initCause(se);
                throw ex;
            } catch (IOException ie) {
                StringWriter swe = new StringWriter();
                ie.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                IllegalStateException ex = new IllegalStateException("IOException");
                ex.initCause(ie);
                throw ex;
            }                                    
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
            IllegalStateException ex = new IllegalStateException("XMLStreamException");
            ex.initCause(e);
            throw ex;
        }
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
                // TODO: FIX:
                xsr = xif.createXMLStreamReader(new StringReader(xml));
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
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    abstract protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException;


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    abstract protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException;

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public final void toXML(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out);
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXML(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
        uninitialized = false;
    }
    
    /**
    * Returns an instance of <code>PseudoRandom</code>. It that can be
    * used to generate random numbers.
    * 
    * @return An instance of <code>PseudoRandom</code>.
    */
    protected PseudoRandom getRandom() {
        return aiMain.getRandom();
    }

    
    /**
     * Disposes this <code>AIObject</code> by removing
     * any referances to this object.
     */
    public void dispose() {
        getAIMain().removeAIObject(getID());
    }
    
        
    /**
    * Returns the game.
    * @return The <code>Game</code>.
    */
    public Game getGame() {
        return aiMain.getGame();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unknown".
    */
    public static String getXMLElementTagName() {
        return "unknown";
    }
}
