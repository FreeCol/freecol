package net.sf.freecol.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A wrapper for <code>XMLStreamReader</code> and the underlying stream.
 * 
 * The close method on <code>XMLStreamReader</code> doesn't close
 * the underlying stream. This class is a wrapper for the
 * <code>XMLStreamReader</code> and the underlying stream with
 * a {@link #close()} method which close them both. 
 */
public class XMLStream implements Closeable {

    private static final Logger logger = Logger.getLogger(XMLStream.class.getName());
    
    private InputStream inputStream;
    private XMLStreamReader xmlStreamReader;
    
    /**
     * Creates a new <code>XMLStream</code>.
     * 
     * @param inputStream The <code>InputStream</code> to create
     *      a <code>XMLStreamReader</code> for.
     * @throws IOException if thrown while creating the
     *      <code>XMLStreamReader</code>. 
     */
    public XMLStream(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.xmlStreamReader = createXMLStreamReader(inputStream);
    }

    /**
     * Get the <code>XMLStreamReader</code>.
     * 
     * @return The <code>XMLStreamReader</code> created using
     *      the underlying stream provided by the contructor
     *      on this object.
     */
    public XMLStreamReader getXMLStreamReader() {
        return xmlStreamReader;
    }
    
    /**
     * Closes both the <code>XMLStreamReader</code> and
     * the underlying stream.
     */
    public void close() {
        try {
            xmlStreamReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private XMLStreamReader createXMLStreamReader(InputStream inputStream) throws IOException{
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();        
            return xif.createXMLStreamReader(inputStream, "UTF-8");
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException("XMLStreamException.");
        } catch (NullPointerException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new NullPointerException("NullPointerException.");
        }
    }
}
