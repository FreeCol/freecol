package net.sf.freecol.server.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Interface for server-side objects which needs to store
 * extra information to a save game.
 */
public interface ServerModelObject  {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public Element toServerAdditionElement(Document document);
    public void readFromServerAdditionElement(Element element);
} 
