/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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


/**
 * Objects of this class hold the export data for a particular type of
 * goods.
 */
public class ExportData extends FreeColObject {

    public static final String TAG = "exportData";

    private static final int HIGH_LEVEL_DEFAULT = 90;
    private static final int LOW_LEVEL_DEFAULT = 10;
    private static final int EXPORT_LEVEL_DEFAULT = 50;
    
    /** The high water mark for the goods type. */
    private int highLevel = HIGH_LEVEL_DEFAULT;

    /** The low water mark for the goods type. */
    private int lowLevel = LOW_LEVEL_DEFAULT;

    /**
     * The amount of goods to import to, do not import when this is present.
     */
    private int importLevel = -1;

    /**
     * The amount of goods to retain, goods beyond this amount are exported.
     */
    private int exportLevel = EXPORT_LEVEL_DEFAULT;

    /** Whether to export or not. */
    private boolean exported = false;


    /**
     * Trivial constructor for Game.newInstance.
     */
    public ExportData() {}

    /**
     * Creates a new {@code ExportData} instance with default settings.
     *
     * @param goodsType The {@code GoodsType} this data refers to.
     * @param importLevel The import level to use.
     */
    public ExportData(GoodsType goodsType, int importLevel) {
        setId(goodsType.getId());
        setImportLevel(importLevel);
    }

    /**
     * Create a new {@code ExportData} by reading a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ExportData(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    /**
     * Get the high water mark for this data.
     *
     * @return The high water mark.
     */
    public final int getHighLevel() {
        return this.highLevel;
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
        return this.lowLevel;
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
     * Get the import level.
     *
     * @return The import level.
     */
    public final int getImportLevel() {
        return this.importLevel;
    }

    /**
     * Get the effective import level given the warehouse capacity to default
     * to when the actual import level is invalid.
     *
     * @param capacity The warehouse capacity.
     * @return The effective import level.
     */     
    public final int getEffectiveImportLevel(int capacity) {
        return (this.importLevel >= 0) ? this.importLevel : capacity;
    }

    /**
     * Set the import level.
     *
     * @param newImportLevel The new import level value.
     * @return This export data.
     */
    public final ExportData setImportLevel(final int newImportLevel) {
        this.importLevel = newImportLevel;
        return this;
    }

    /**
     * Get the export level.
     *
     * @return The export level.
     */
    public final int getExportLevel() {
        return this.exportLevel;
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
     * Can the goods type of this export data to be exported?
     *
     * @return True if this goods type is to be exported.
     */
    public final boolean getExported() {
        return this.exported;
    }

    /**
     * Set export status of the goods type of this export data.
     *
     * @param newExport The new export status.
     */
    public final void setExported(final boolean newExport) {
        this.exported = newExport;
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        ExportData o = copyInCast(other, ExportData.class);
        if (o == null || !super.copyIn(o)) return false;
        this.highLevel = o.getHighLevel();
        this.lowLevel = o.getLowLevel();
        this.importLevel = o.getImportLevel();
        this.exportLevel = o.getExportLevel();
        this.exported = o.getExported();
        return true;
    }


    // Serialization

    private static final String EXPORTED_TAG = "exported";
    private static final String EXPORT_LEVEL_TAG = "exportLevel";
    private static final String IMPORT_LEVEL_TAG = "importLevel";
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

        xw.writeAttribute(IMPORT_LEVEL_TAG, importLevel);

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

        importLevel = xr.getAttribute(IMPORT_LEVEL_TAG, -1);

        exportLevel = xr.getAttribute(EXPORT_LEVEL_TAG, EXPORT_LEVEL_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
