package net.sf.freecol.server.model;

import org.w3c.dom.*;

public interface ServerModelObject {

    public Element toServerAdditionElement(Document document);
    public void readFromServerAdditionElement(Element element);
} 
