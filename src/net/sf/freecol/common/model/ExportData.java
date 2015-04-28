/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * Objects of this class hold the export data for a particular type of
 * goods.
 */
public class ExportData extends FreeColObject {

    private static final int HIGH_LEVEL_DEFAULT = 90;
    private static final int LOW_LEVEL_DEFAULT = 10;
    private static final int EXPORT_LEVEL_DEFAULT = 50;

    /** The high water mark for the goods type. */
    private int highLevel = HIGH_LEVEL_DEFAULT;

    /** The low water mark for the goods type. */
    private int lowLevel = LOW_LEVEL_DEFAULT;

    /** The amount of goods to retain, goods beyond this amount are exported. */
    private int exportLevel = EXPORT_LEVEL_DEFAULT;

    /** Whether to export or not. */
    private boolean exported = false;


    /**
     * Creates a new <code>ExportData</code> instance with default settings.
     *
     * @param goodsType The <code>GoodsType</code> this data refers to.
     */
    public ExportData(GoodsType goodsType) {
        setId(goodsType.getId());
    }

    /**
     * Create a new <code>ExportData</code> by reading a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ExportData(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }

    /**
     * Create a new <code>ExportData</code> by reading an element.
     *
     * @param element The <code>Element</code> to read.
     */
    public ExportData(Element element) {
        readFromXMLElement(element);
    }


    /**
     * Get the high water mark for this data.
     *
     * @return The high water mark.
     */
    public final int getHighLevel() {
        return highLevel;
    }

    /**
     * Set the high water mark for this data.
     *
     * @param newHighLevel The new high water mark value.
     * @return This export data.
     */
    public final ExportData setHighLevel(final int newHighLevel) {
        this.highLevel = newHighLevel;
        return this;
    }

    /**
     * Get the low water mark for this data.
     *
     * @return The low water mark.
     */
    public final int getLowLevel() {
        return lowLevel;
    }

    /**
     * Set the low water mark for this data.
     *
     * @param newLowLevel The new low water mark value.
     * @return This export data.
     */
    public final ExportData setLowLevel(final int newLowLevel) {
        this.lowLevel = newLowLevel;
        return this;
    }

    /**
     * Get the export level.
     *
     * @return The export level.
     */
    public final int getExportLevel() {
        return exportLevel;
    }

    /**
     * Set the export level.
     *
     * @param newExportLevel The new export level value.
     * @return This export data.
     */
    public final ExportData setExportLevel(final int newExportLevel) {
        this.exportLevel = newExportLevel;
        return this;
    }

    /**
     * Is the goods type of this export data to be exported?
     *
     * @return True if this goods type is to be exported.
     */
    public final boolean getExported() {
        return exported;
    }

    public final void setExported(final boolean newExport) {
        this.exported = newExport;
    }


    // Serialization

    private static final String EXPORTED_TAG = "exported";
    private static final String EXPORT_LEVEL_TAG = "exportLevel";
    private static final String HIGH_LEVEL_TAG = "highLevel";
    private static final String LOW_LEVEL_TAG = "lowLevel";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(EXPORTED_TAG, exported);

        xw.writeAttribute(HIGH_LEVEL_TAG, highLevel);

        xw.writeAttribute(LOW_LEVEL_TAG, lowLevel);

        xw.writeAttribute(EXPORT_LEVEL_TAG, exportLevel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        exported = xr.getAttribute(EXPORTED_TAG, false);

        highLevel = xr.getAttribute(HIGH_LEVEL_TAG, HIGH_LEVEL_DEFAULT);

        lowLevel = xr.getAttribute(LOW_LEVEL_TAG, LOW_LEVEL_DEFAULT);

        exportLevel = xr.getAttribute(EXPORT_LEVEL_TAG, EXPORT_LEVEL_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "exportData".
     */
    public static String getXMLElementTagName() {
        return "exportData";
    }
}
