/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Objects of this class hold the export data for a particular type of
 * goods.
 */
public class ExportData extends FreeColObject {

    /**
     * Describe highLevel here.
     */
    private int highLevel = 90;

    /**
     * Describe lowLevel here.
     */
    private int lowLevel = 10;

    /**
     * Describe exportLevel here.
     */
    private int exportLevel = 50;

    /**
     * Describe export here.
     */
    private boolean exported = false;

    /**
     * Package constructor: This class is only supposed to be
     * constructed by {@link Colony}.
     *
     */
    public ExportData() {}

    /**
     * Creates a new <code>ExportData</code> instance.
     *
     * @param goodsType a <code>GoodsType</code> value
     */
    public ExportData(GoodsType goodsType) {
        setId(goodsType.getId());
    }

    /**
     * Creates a new <code>ExportData</code> instance.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param exported a <code>boolean</code> value
     * @param lowLevel an <code>int</code> value
     * @param highLevel an <code>int</code> value
     * @param exportLevel an <code>int</code> value
     */
    public ExportData(GoodsType goodsType, boolean exported, int lowLevel, int highLevel, int exportLevel) {
        setId(goodsType.getId());
        this.exported = exported;
        this.lowLevel = lowLevel;
        this.highLevel = highLevel;
        this.exportLevel = exportLevel;
    }

    /**
     * Creates a new <code>ExportData</code> instance.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param exported a <code>boolean</code> value
     * @param exportLevel an <code>int</code> value
     */
    public ExportData(GoodsType goodsType, boolean exported, int exportLevel) {
        this(goodsType, exported, 0, 100, exportLevel);
    }

    /**
     * Creates a new <code>ExportData</code> instance.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param template an <code>ExportData</code> value
     */
    public ExportData(GoodsType goodsType, ExportData template) {
        setId(goodsType.getId());
        this.exported = template.exported;
        this.lowLevel = template.lowLevel;
        this.highLevel = template.highLevel;
        this.exportLevel = template.exportLevel;
    }

    /**
     * Get the <code>HighLevel</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getHighLevel() {
        return highLevel;
    }

    /**
     * Set the <code>HighLevel</code> value.
     *
     * @param newHighLevel The new HighLevel value.
     */
    public final void setHighLevel(final int newHighLevel) {
        this.highLevel = newHighLevel;
    }

    /**
     * Get the <code>LowLevel</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getLowLevel() {
        return lowLevel;
    }

    /**
     * Set the <code>LowLevel</code> value.
     *
     * @param newLowLevel The new LowLevel value.
     */
    public final void setLowLevel(final int newLowLevel) {
        this.lowLevel = newLowLevel;
    }

    /**
     * Get the <code>ExportLevel</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getExportLevel() {
        return exportLevel;
    }

    /**
     * Set the <code>ExportLevel</code> value.
     *
     * @param newExportLevel The new ExportLevel value.
     */
    public final void setExportLevel(final int newExportLevel) {
        this.exportLevel = newExportLevel;
    }

    /**
     * Get the <code>Export</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isExported() {
        return exported;
    }

    /**
     * Set the <code>Export</code> value.
     *
     * @param newExport The new Export value.
     */
    public final void setExported(final boolean newExport) {
        this.exported = newExport;
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("exported", Boolean.toString(exported));
        out.writeAttribute("highLevel", Integer.toString(highLevel));
        out.writeAttribute("lowLevel", Integer.toString(lowLevel));
        out.writeAttribute("exportLevel", Integer.toString(exportLevel));
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        exported = Boolean.parseBoolean(in.getAttributeValue(null,
                "exported"));
        highLevel = Integer.parseInt(in.getAttributeValue(null, "highLevel"));
        lowLevel = Integer.parseInt(in.getAttributeValue(null, "lowLevel"));
        exportLevel = Integer.parseInt(in.getAttributeValue(null,
                "exportLevel"));
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "exportData".
     */
    public static String getXMLElementTagName() {
        return "exportData";
    }
}
