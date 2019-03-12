/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class SerializationTest extends FreeColTestCase {

    private static Validator buildValidator(String path) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File schemaLocation = new File(path);
        Schema schema = factory.newSchema(schemaLocation);
        return schema.newValidator();
    }

    private Source buildSource(FreeColObject object) throws Exception {
        return new StreamSource(new StringReader(object.serialize()));
    }


    private void validateMap(String name) throws Exception {
        try {
            Validator mapValidator = buildValidator("schema/data/data-savedGame.xsd");

            FreeColSavegameFile mapFile = new FreeColSavegameFile(new File(name));

            mapValidator.validate(new StreamSource(mapFile.getSavegameInputStream()));
        } catch (SAXParseException e) {
            String errMsg = e.getMessage()
                + " at line=" + e.getLineNumber()
                + " column=" + e.getColumnNumber();
            fail(errMsg);
        }
    }

    private void logParseFailure(SAXParseException e, String serialized) {
        int col = e.getColumnNumber();
        String errMsg = e.getMessage()
            + "\nAt line=" + e.getLineNumber()
            + ", column=" + col + ":\n"
            + serialized.substring(Math.max(0, col - 100),
                                   Math.min(col + 100, serialized.length()));
        fail(errMsg);
    }

    public void testValidation() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getStandardColony(6);
        Player player = game.getPlayerByNationId("model.nation.dutch");

        ServerTestHelper.newTurn();
        ServerTestHelper.newTurn();

        String serialized = null;
        try {
            Validator validator = buildValidator("schema/data/data-game.xsd");
            serialized = game.serialize();
            validator.validate(new StreamSource(new StringReader(serialized)));
        } catch (SAXParseException e) {
            int col = e.getColumnNumber();
            String errMsg = e.getMessage()
                + "\nAt line=" + e.getLineNumber()
                + ", column=" + col + ":\n";
            if (serialized != null) {
                errMsg += serialized.substring(Math.max(0, col - 100),
                                               Math.min(col + 100, serialized.length()));
            }
            fail(errMsg);
        }

        ServerTestHelper.stopServerGame();
    }

    public void testMapAfrica() throws Exception {
        validateMap("data/maps/Africa.fsm");
    }

    public void testMapAustralia() throws Exception {
        validateMap("data/maps/Australia.fsm");
    }

    public void testMapAmerica() throws Exception {
        validateMap("data/maps/America_large.fsm");
        validateMap("data/maps/South_America.fsm");
    }

    public void testMapCaribbean() throws Exception {
        validateMap("data/maps/Caribbean_basin.fsm");
        validateMap("data/maps/Caribbean_large.fsm");
    }

    public void testStringTemplate() throws Exception {

        StringTemplate t1 = StringTemplate.template("model.goods.goodsAmount")
            .add("%goods%", "model.goods.food.name")
            .addName("%amount%", "100");
        StringTemplate t2 = StringTemplate.template("model.goods.goodsAmount")
            .addAmount("%amount%", 50)
            .addStringTemplate("%goods%", t1);

        Game game = getGame();
        Player player = game.getPlayerByNationId("model.nation.dutch");

        try {
            Validator validator = buildValidator("schema/data/data-stringTemplate.xsd");
            validator.validate(buildSource(t2));
        } catch (SAXParseException e){
            String errMsg = e.getMessage()
                + " at line=" + e.getLineNumber()
                + " column=" + e.getColumnNumber();
            fail(errMsg);
        }
    }

    public void testSpecification() throws Exception {
        try {
            String filename = "test/data/specification.xml";
            Validator validator = buildValidator("schema/specification-schema.xsd");
            FileOutputStream fos = new FileOutputStream(filename);
            try (FreeColXMLWriter xw = new FreeColXMLWriter(fos, null, false)) {
                spec().toXML(xw);
            } catch (IOException ioe) {
                fail(ioe.getMessage());
            }

            validator.validate(new StreamSource(new FileReader(filename)));
        } catch (SAXParseException e) {
            String errMsg = e.getMessage()
                + " at line=" + e.getLineNumber()
                + " column=" + e.getColumnNumber();
            fail(errMsg);
        }

    }

    public void testDifficulty() throws Exception {
        Specification spec1 = null;
        Specification spec2 = null;
        spec1 = FreeColTcFile.getFreeColTcFile("classic").getSpecification();
        spec1.applyDifficultyLevel("model.difficulty.veryEasy");
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave())) {
            spec1.toXML(xw);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        spec2 = new Specification(new ByteArrayInputStream(sw.toString().getBytes()));

        assertNotNull(spec1);
        assertNotNull(spec2);

        OptionGroup level1 = spec1.getDifficultyOptionGroup();
        OptionGroup level2 = spec2.getDifficultyOptionGroup();
        assertNotNull(level1);
        assertNotNull(level2);
        assertEquals(level1.getId(), level2.getId());

        try {
            int increment1 = spec1.getInteger(GameOptions.CROSSES_INCREMENT);
            int increment2 = spec2.getInteger(GameOptions.CROSSES_INCREMENT);
            assertEquals(increment1, increment2);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testGeneratedLists() throws Exception {
        Specification spec1 = null;
        Specification spec2 = null;
        spec1 = FreeColTcFile.getFreeColTcFile("classic").getSpecification();
        spec1.applyDifficultyLevel("model.difficulty.veryEasy");
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            spec1.toXML(xw);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        spec2 = new Specification(new ByteArrayInputStream(sw.toString().getBytes()));

        List<GoodsType> food1 = spec1.getFoodGoodsTypeList();
        List<GoodsType> food2 = spec2.getFoodGoodsTypeList();
        assertEquals(food1.size(), food2.size());
        assertEquals(food1.get(0).getId(), food2.get(0).getId());

        List<GoodsType> farmed1 = spec1.getFarmedGoodsTypeList();
        List<GoodsType> farmed2 = spec2.getFarmedGoodsTypeList();
        assertEquals(farmed1.size(), farmed2.size());
        assertEquals(farmed1.get(0).getId(), farmed2.get(0).getId());
    }

}
