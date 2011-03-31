/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


package net.sf.freecol.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;

import javax.imageio.ImageIO;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;


public class MapConverter {

    public static void main(String[] args) throws Exception {

        Specification specification = new FreeColTcFile("freecol").getSpecification();

        for (String filename : args) {
            File out = new File(filename);
            if (out.exists()) {
                try {
                    String newName = filename + ".old";
                    File in = new File(newName);
                    out.renameTo(in);
                    System.out.println("Renamed " + filename + " to " + newName + ".");
                    FreeColSavegameFile savegame = new FreeColSavegameFile(in);
                    BufferedImage thumbnail = null;
                    try {
                        thumbnail = ImageIO.read(savegame.getInputStream("thumbnail.png"));
                        System.out.println("Loaded thumbnail.");
                    } catch (FileNotFoundException e) {
                        System.out.println("No thumbnail present.");
                    }
                    FreeColServer server = new FreeColServer(savegame, FreeCol.DEFAULT_PORT,
                                                             "mapTransformer", specification);
                    System.out.println("Started server.");
                    server.saveGame(out, server.getOwner(), thumbnail);
                    System.out.println("Saved updated savegame.");
                    server.shutdown();
                    System.out.println("Shut down server.");
                } catch(Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

}

