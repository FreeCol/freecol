/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.common.io;

import java.io.File;
import java.io.IOException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceFactory;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.ResourceMapping;

/**
 * A Total Conversion (TC).
 */
public class FreeColTcFile extends FreeColModFile {
    
    /**
     * Opens the given file for reading.
     * 
     * @param id The id of the TC to load.
     * @throws IOException if thrown while opening the file.
     */
    public FreeColTcFile(final String id) {
        super(id, new File(FreeCol.getDataDirectory(), id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceMapping getResourceMapping() {
        //Specification.createSpecification(getSpecificationInputStream());
        ResourceMapping result;
        try {
            final ModDescriptor info = getModDescriptor();
            if (info.getParent() != null) {
                final FreeColTcFile parentTcData = new FreeColTcFile(info.getParent());
                result = parentTcData.getResourceMapping();
                //result.addAll(Specification.getSpecification().getDefaultMapping());
            } else {
                result = new ResourceMapping();
            }
            result.addAll(createRiverMapping());
            result.addAll(createBeachMapping());
            result.addAll(super.getResourceMapping());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * File endings that are supported for this type of data file.
     * @return An array of: ".ftc" and ".zip".
     */
    @Override
    protected String[] getFileEndings() {
        return new String[] {".ftc", ".zip"};
    }


    private String getShortId(FreeColObject object) {
        return object.getId().substring(object.getId().lastIndexOf('.') + 1);
    }


    /*
      Don't attempt this before specification handling is sorted out.
    public ResourceMapping getDefaultResourceMapping() throws Exception {
        Specification.createSpecification(getSpecificationInputStream());
            
        ResourceMapping map = new ResourceMapping();
        String key, value, keyPrefix, urlPrefix, urlSuffix, roleId, shortId;

        // resources
        urlPrefix = "resources/images/bonus/";
        for (ResourceType resourceType : Specification.getSpecification().getResourceTypeList()) {
            key = resourceType.getId() + ".image";
            value = urlPrefix + getShortId(resourceType).toLowerCase() + ".png";
            map.add(key, ResourceFactory.createResource(getURI(value)));
        }

        // units
        String[][] attackAnimations = new String[][] {
            { ".attack.w.animation", "-attack-left.sza" },
            { ".attack.e.animation", "-attack-right.sza" }
        };

        urlPrefix = "resources/images/units/";
        for (UnitType unitType : Specification.getSpecification().getUnitTypeList()) {
            keyPrefix = unitType.getId() + ".";
            shortId = getShortId(unitType);
            urlSuffix = "/" + shortId + ".png";

            for (Role role : Role.values()) {
                // role images
                roleId = role.getId();
                key = keyPrefix + roleId + ".image";
                value = urlPrefix + roleId + urlSuffix;
                map.add(key, ResourceFactory.createResource(getURI(value)));
                // attack animations
                for (String[] animation : attackAnimations) {
                    key = keyPrefix + roleId + animation[0];
                    value = urlPrefix + roleId + "/" + shortId + animation[1];
                    map.add(key, ResourceFactory.createResource(getURI(value)));
                }
            }
        }
        return map;
    }
    */

    public ResourceMapping createRiverMapping() {
        ResourceMapping map = new ResourceMapping();
        String pathPrefix = "resources/images/river/";
        String key, path;
        for (int index = 0; index < ResourceManager.RIVER_STYLES; index++) {
            path = pathPrefix +"river" + index + ".png";
            map.add("river" + index, ResourceFactory.createResource(getURI(path)));
        }
        for (Direction d : Direction.longSides) {
            key = "delta_" + d + "_small";
            path = pathPrefix + key + ".png";
            map.add(key, ResourceFactory.createResource(getURI(path)));
            key = "delta_" + d + "_large";
            path = pathPrefix + key + ".png";
            map.add(key, ResourceFactory.createResource(getURI(path)));
        }
        return map;
    }

    public ResourceMapping createBeachMapping() {
        ResourceMapping map = new ResourceMapping();
        String pathPrefix = "resources/images/terrain/beach/";
        String key, path;
        // beach0 is never used
        for (int index = 1; index < ResourceManager.BEACH_STYLES; index++) {
            path = pathPrefix +"beach" + index + ".png";
            map.add("beach" + index, ResourceFactory.createResource(getURI(path)));
        }
        return map;
    }

}
