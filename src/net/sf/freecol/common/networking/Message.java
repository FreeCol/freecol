
package net.sf.freecol.common.networking;

import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



/**
* Class for parsing raw message data into an XML-tree and for creating new XML-trees.
*/
public final class Message {
    private static final Logger logger = Logger.getLogger(Message.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final String FREECOL_PROTOCOL_VERSION = "0.0.1";


    /** The actual Message data. */
    private final Document document;

    /**
    * Constructs a new Message with data from the given String. The constructor
    * to use if this is an INCOMING message.
    *
    * @param msg The raw message data.
    */
    public Message(String msg) {
        this(new InputSource(new StringReader(msg)));
    }


    /**
    * Constructs a new Message with data from the given InputStreamReader. The constructor
    * to use if this is an INCOMING message.
    */
    public Message(InputStreamReader inputStreamReader) {
        this(new InputSource(inputStreamReader));
    }


    /**
    * Constructs a new Message with data from the given InputSource. The constructor
    * to use if this is an INCOMING message.
    */
    public Message(InputSource inputSource) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document tempDocument = null;

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            tempDocument = builder.parse(inputSource);
        } catch (SAXException sxe) {
            // Error generated during parsing
            Exception  x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            x.printStackTrace();
            logger.warning("Invalid message received.");
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
        } catch (IOException ioe) {
            // I/O error
            ioe.printStackTrace();
        }
        document = tempDocument;
    }


    /**
    * Constructs a new Message with data from the given XML-document.
    * @param document The document representing an XML-message.
    */
    public Message(Document document) {
        this.document = document;
    }



    /**
    * Gets the current version of the FreeCol protocol.
    */
    public static String getFreeColProtocolVersion() {
        return FREECOL_PROTOCOL_VERSION;
    }


    /**
    * Creates and returns a new XML-document.
    * @return the new XML-document.
    */
    public static Document createNewDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.newDocument();
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
            return null;
        }
    }


    /**
    * Creates a new root element. This is done by creating a new document
    * and using this document to create the root element.
    *
    * @param tagName The tag name of the root element beeing created,
    * @return the new root element.
    */
    public static Element createNewRootElement(String tagName) {
        return createNewDocument().createElement(tagName);
    }


    /**
    * Creates an error message.
    *
    * @param messageID Identifies the "i18n"-keyname.
    *                  Not specified in the message if <i>null</i>.
    * @param message   The error in plain text.
    *                  Not specified in the message if <i>null</i>.
    * @return The root <code>Element</code> of the error message.
    */
    public static Element createError(String messageID, String message) {
        Element errorElement = createNewRootElement("error");

        if (messageID != null && !messageID.equals("")) {
            errorElement.setAttribute("messageID", messageID);
        }

        if (message != null && !message.equals("")) {
            errorElement.setAttribute("message", message);
        }

        return errorElement;
    }


    /**
    * Gets the <code>Document</code> holding the message data.
    * @return The <code>Document</code> holding the message data.
    */
    public Document getDocument() {
        return document;
    }


    /**
    * Gets the type of this Message.
    * @return The type of this Message.
    */
    public String getType() {
        if (document != null) {
            if (document.getDocumentElement() != null) {
                return document.getDocumentElement().getTagName();
            }
        }

        return new String("invalid");
    }


    /**
    * Checks if this message is of a given type.
    *
    * @param type The type you wish to test against.
    * @return <code>true</code> if the type of this message equals the given type
    *         and <code>false</code> otherwise.
    */
    public boolean isType(String type) {
        if (getType().equals(type)) {
            return true;
        } else {
            return false;
        }
    }


    /**
    * Sets an attribute on the root element.
    *
    * @param key The key of the attribute.
    * @param value The value of the attribute.
    */
    public void setAttribute(String key, String value) {
        document.getDocumentElement().setAttribute(key, value);
    }


    /**
    * Sets an attribute on the root element.
    *
    * @param key The key of the attribute.
    * @param value The value of the attribute.
    */
    public void setAttribute(String key, int value) {
        document.getDocumentElement().setAttribute(key, (new Integer(value)).toString());
    }


    /**
    * Gets an attribute from the root element.
    * @param key The key of the attribute.
    */
    public String getAttribute(String key) {
        return document.getDocumentElement().getAttribute(key);
    }


    /**
    * Checks if an attribute is set on the root element.
    * @param attribute The attribute in which to verify the existence of.
    */
    public boolean hasAttribute(String attribute) {
        return document.getDocumentElement().hasAttribute(attribute);
    }


    /**
    * Inserts <code>newRoot</code> as the new root element and appends
    * the old root element.
    */
    public void insertAsRoot(Element newRoot) {
        Element oldRoot = document.getDocumentElement();

        if (oldRoot != null) {
            document.removeChild(oldRoot);
            newRoot.appendChild(oldRoot);
        }

        document.appendChild(newRoot);
    }


    /**
    * Returns the <code>String</code> representation of the message.
    * This is what actually gets transmitted to the other peer.
    *
    * @return The <code>String</code> representation of the message.
    */
    public String toString() {
        return document.getDocumentElement().toString();
    }
}
