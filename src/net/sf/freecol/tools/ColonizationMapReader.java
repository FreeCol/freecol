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


package net.sf.freecol.tools;

import java.io.RandomAccessFile;
import java.util.Arrays;


/**
 * Just pass the name of a Colonization map file (with extension ".MP").
 */
public class ColonizationMapReader {

    public static final int WIDTH = 0;
    public static final int HEIGHT = 2;

    private static final char[] tiletypes = new char[256];

    /**
     * It seems that the least significant three bits encodes the
     * terrain type, and the more significant bits encodes overlays
     * such as forests, hills, mountains and rivers. Ice, oceans and
     * sea lanes seem to be handled differently.
     *
     * bits 0-2: tile type
     * bit 3 (8): forest
     * bit 4 (16): mountain
     * bits 3+4 (24): special (since forest + mountain is not supported)
     * bit 5 (32): hill
     * bit 6 (64): minor river
     * bit 7 (128): unknown
     * bits 6+7 (192): major river
     */
    static {
        Arrays.fill(tiletypes, '?');
        tiletypes[0]  = 't'; // tundra
        tiletypes[1]  = 'd'; // desert
        tiletypes[2]  = 'p'; // plains
        tiletypes[3]  = 'r'; // prairie
        tiletypes[4]  = 'g'; // grassland
        tiletypes[5]  = 'v'; // savannah
        tiletypes[6]  = 'm'; // marsh
        tiletypes[7]  = 's'; // swamp

        tiletypes[8]  = 'T'; // boreal
        tiletypes[9]  = 'D'; // scrub
        tiletypes[10] = 'P'; // mixed
        tiletypes[11] = 'R'; // broadleaf
        tiletypes[12] = 'G'; // conifer
        tiletypes[13] = 'V'; // tropical
        tiletypes[14] = 'M'; // wetland
        tiletypes[15] = 'S'; // rain

        tiletypes[16] = '*'; // tundra with mountain
        tiletypes[17] = '*'; // desert with mountain
        tiletypes[18] = '*'; // plains with mountain
        tiletypes[19] = '*'; // prairie with mountain
        tiletypes[20] = '*'; // grassland with mountain
        tiletypes[21] = '*'; // savannah with mountain
        tiletypes[22] = '*'; // marsh with mountain
        tiletypes[23] = '*'; // swamp with mountain

        tiletypes[24] = '_'; // ice
        tiletypes[25] = '.'; // ocean
        tiletypes[26] = ':'; // sea lane

        tiletypes[32] = '^'; // tundra with hill
        tiletypes[33] = '^'; // desert with hill
        tiletypes[34] = '^'; // plains with hill
        tiletypes[35] = '^'; // prairie with hill
        tiletypes[36] = '^'; // grassland with hill
        tiletypes[37] = '^'; // savannah with hill
        tiletypes[38] = '^'; // marsh with hill
        tiletypes[39] = '^'; // swamp with hill

        tiletypes[40] = '^'; // tundra with hill
        tiletypes[41] = '^'; // desert with hill
        tiletypes[42] = '^'; // plains with hill
        tiletypes[43] = '^'; // prairie with hill
        tiletypes[44] = '^'; // grassland with hill
        tiletypes[45] = '^'; // savannah with hill
        tiletypes[46] = '^'; // marsh with hill
        tiletypes[47] = '^'; // swamp with hill

        tiletypes[48] = '^'; // tundra with hill
        tiletypes[49] = '^'; // desert with hill
        tiletypes[50] = '^'; // plains with hill
        tiletypes[51] = '^'; // prairie with hill
        tiletypes[52] = '^'; // grassland with hill
        tiletypes[53] = '^'; // savannah with hill
        tiletypes[54] = '^'; // marsh with hill
        tiletypes[55] = '^'; // swamp with hill

        tiletypes[64] = '~'; // tundra with minor river
        tiletypes[65] = '~'; // desert with minor river
        tiletypes[66] = '~'; // plains with minor river
        tiletypes[67] = '~'; // prairie with minor river
        tiletypes[68] = '~'; // grassland with minor river
        tiletypes[69] = '~'; // savannah with minor river
        tiletypes[70] = '~'; // marsh with minor river
        tiletypes[71] = '~'; // swamp with minor river

        tiletypes[72] = '~'; // boreal forest with minor river
        tiletypes[73] = '~'; // scrub forest with minor river
        tiletypes[74] = '~'; // mixed forest with minor river
        tiletypes[75] = '~'; // broadleaf forest with minor river
        tiletypes[76] = '~'; // conifer forest with minor river
        tiletypes[77] = '~'; // tropical forest with minor river
        tiletypes[78] = '~'; // wetland forest with minor river
        tiletypes[79] = '~'; // rain forest with minor river

        tiletypes[80] = '~'; // boreal forest with minor river
        tiletypes[81] = '~'; // scrub forest with minor river
        tiletypes[82] = '~'; // mixed forest with minor river
        tiletypes[83] = '~'; // broadleaf forest with minor river
        tiletypes[84] = '~'; // conifer forest with minor river
        tiletypes[85] = '~'; // tropical forest with minor river
        tiletypes[86] = '~'; // wetland forest with minor river
        tiletypes[87] = '~'; // rain forest with minor river

        tiletypes[89] = '~'; // ocean with minor river

        tiletypes[112]  = 'x'; // tundra with hill and minor river
        tiletypes[113]  = 'x'; // desert with hill and minor river
        tiletypes[114]  = 'x'; // plains with hill and minor river
        tiletypes[115]  = 'x'; // prairie with hill and minor river
        tiletypes[116]  = 'x'; // grassland with hill and minor river
        tiletypes[117]  = 'x'; // savannah with hill and minor river
        tiletypes[118]  = 'x'; // marsh with hill and minor river
        tiletypes[119]  = 'x'; // swamp with hill and minor river

        tiletypes[160] = '*'; // tundra with mountain
        tiletypes[161] = '*'; // desert with mountain
        tiletypes[162] = '*'; // plains with mountain
        tiletypes[163] = '*'; // prairie with mountain
        tiletypes[164] = '*'; // grassland with mountain
        tiletypes[165] = '*'; // savannah with mountain
        tiletypes[166] = '*'; // marsh with mountain
        tiletypes[167] = '*'; // swamp with mountain

        tiletypes[168] = '*'; // tundra with mountain
        tiletypes[169] = '*'; // desert with mountain
        tiletypes[170] = '*'; // plains with mountain
        tiletypes[171] = '*'; // prairie with mountain
        tiletypes[172] = '*'; // grassland with mountain
        tiletypes[173] = '*'; // savannah with mountain
        tiletypes[174] = '*'; // marsh with mountain
        tiletypes[175] = '*'; // swamp with mountain

        tiletypes[176] = '*'; // tundra with mountain
        tiletypes[177] = '*'; // desert with mountain
        tiletypes[178] = '*'; // plains with mountain
        tiletypes[179] = '*'; // prairie with mountain
        tiletypes[180] = '*'; // grassland with mountain
        tiletypes[181] = '*'; // savannah with mountain
        tiletypes[182] = '*'; // marsh with mountain
        tiletypes[183] = '*'; // swamp with mountain

        tiletypes[192] = '='; // tundra with major river
        tiletypes[193] = '='; // desert with major river
        tiletypes[194] = '='; // plains with major river
        tiletypes[195] = '='; // prairie with major river
        tiletypes[196] = '='; // grassland with major river
        tiletypes[197] = '='; // savannah with major river
        tiletypes[198] = '='; // marsh with major river
        tiletypes[199] = '='; // swamp with major river

        tiletypes[200] = '='; // boreal forest with major river
        tiletypes[201] = '='; // scrub forest with major river
        tiletypes[202] = '='; // mixed forest with major river
        tiletypes[203] = '='; // broadleaf forest with major river
        tiletypes[204] = '='; // conifer forest with major river
        tiletypes[205] = '='; // tropical forest with major river
        tiletypes[206] = '='; // wetland forest with major river
        tiletypes[207] = '='; // rain forest with major river

        tiletypes[208] = '='; // boreal forest with major river
        tiletypes[209] = '='; // scrub forest with major river
        tiletypes[210] = '='; // mixed forest with major river
        tiletypes[211] = '='; // broadleaf forest with major river
        tiletypes[212] = '='; // conifer forest with major river
        tiletypes[213] = '='; // tropical forest with major river
        tiletypes[214] = '='; // wetland forest with major river
        tiletypes[215] = '='; // rain forest with major river

        tiletypes[217] = '='; // ocean with major river

    }

    private static final byte[] header = new byte[6];
    private static byte[] layer1;

    public static void main(String[] args) throws Exception {

        StringBuilder unknown = new StringBuilder();
        RandomAccessFile reader = new RandomAccessFile(args[0], "r");
        reader.read(header);

        System.out.println(String.format("Map width:  %02d", (int) header[WIDTH]));
        System.out.println(String.format("Map height: %02d", (int) header[HEIGHT]));

        int size = header[WIDTH] * header[HEIGHT];
        layer1 = new byte[size];
        reader.read(layer1);

        int index = 0;
        for (int y = 0; y < header[HEIGHT]; y++) {
            for (int x = 0; x < header[WIDTH]; x++) {
                int decimal = layer1[index] & 0xff;
                char terrain = tiletypes[decimal];
                System.out.print(terrain);
                if (terrain == '?') {
                    unknown.append(x + ", " + y + ": " + decimal + "\n");
                }
                index++;
            }
            System.out.println("\n");
        }
        System.out.println("\n");
        System.out.println(unknown.toString());
    }

}