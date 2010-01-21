/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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

import java.io.File;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.util.test.FreeColTestCase;

import org.xml.sax.SAXException;



public class SerializationTest extends FreeColTestCase {

    private static Validator buildValidator(String path) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File schemaLocation = new File(path);
        Schema schema = factory.newSchema(schemaLocation);
        return schema.newValidator();
    }

    private Source buildSource(FreeColObject object, Player player, boolean showAll, boolean toSavedGame)
        throws Exception {
        StringWriter sw = new StringWriter();
        XMLOutputFactory xif = XMLOutputFactory.newInstance();
        XMLStreamWriter out = xif.createXMLStreamWriter(sw);
        object.toXML(out, player, showAll, toSavedGame);
        out.close();
        return new StreamSource(new StringReader(sw.toString()));
    }

    public void testValidation() throws Exception {

        Validator validator = buildValidator("schema/data/data-player.xsd");

        Game game = getGame();
        Map map = getTestMap(plainsType,true);
    	game.setMap(map);

        Colony colony = getStandardColony(6);
        Player player = game.getPlayer("model.nation.dutch");
        colony.newTurn();
        colony.newTurn();

        validator = buildValidator("schema/data/data-game.xsd");
        validator.validate(buildSource(game, player, true, true));

    }

    public void testMapAfrica() throws Exception {

        Validator mapValidator = buildValidator("schema/data/data-savedGame.xsd");

        FreeColSavegameFile mapFile =
            new FreeColSavegameFile(new File("data/maps/Africa.fsg"));

        mapValidator.validate(new StreamSource(mapFile.getSavegameInputStream()));

    }

    public void testMapAustralia() throws Exception {

        Validator mapValidator = buildValidator("schema/data/data-savedGame.xsd");

        FreeColSavegameFile mapFile =
            new FreeColSavegameFile(new File("data/maps/Australia.fsg"));

        mapValidator.validate(new StreamSource(mapFile.getSavegameInputStream()));

    }

    public void testMapAmerica() throws Exception {

        Validator mapValidator = buildValidator("schema/data/data-savedGame.xsd");

        FreeColSavegameFile mapFile =
            new FreeColSavegameFile(new File("data/maps/america-large.fsg"));

        mapValidator.validate(new StreamSource(mapFile.getSavegameInputStream()));

    }

    public void testMapCaribbean() throws Exception {

        Validator mapValidator = buildValidator("schema/data/data-savedGame.xsd");

        FreeColSavegameFile mapFile =
            new FreeColSavegameFile(new File("data/maps/caribbean-basin.fsg"));

        mapValidator.validate(new StreamSource(mapFile.getSavegameInputStream()));

    }

}