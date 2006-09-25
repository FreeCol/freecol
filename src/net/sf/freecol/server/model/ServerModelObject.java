package net.sf.freecol.server.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Interface for server-side objects which needs to store
 * extra information to a save game.
 */
public interface ServerModelObject  {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public void toServerAdditionElement(XMLStreamWriter out) throws XMLStreamException;
    public void readFromServerAdditionElement(XMLStreamReader in) throws XMLStreamException;
} 
