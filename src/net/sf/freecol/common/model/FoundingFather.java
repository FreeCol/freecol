/*
 *  FoundingFather.java - Represents a Founding Father for a Player.
 *
 *  Copyright (C) 2004  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.common.model;

/**
* Represents one founding father to be contained in a Player object.
* Stateful information is in the Player object.
*/
public class FoundingFather {

    public static final int ADAM_SMITH = 0,
                            JACOB_FUGGER = 1,
                            PETER_MINUIT = 2,
                            PETER_STUYVESANT = 3,
                            JAN_DE_WITT = 4,
                            FERDINAND_MAGELLAN = 5,
                            FRANSICO_DE_CORONADO = 6,
                            HERNANDO_DE_SOTO = 7,
                            HENRY_HUDSON = 8,
                            LA_SALLE = 9,
                            HERNAN_CORTES = 10,
                            GEORGE_WASHINGTION = 11,
                            PAUL_REVERE = 12,
                            FRANCIS_DRAKE = 13,
                            JOHN_PAUL_JONES = 14,
                            THOMAS_JEFFERSON = 15,
                            POCAHONTAS = 16,
                            THOMAS_PAINE = 17,
                            SIMON_BOLIVAR = 18,
                            BENJAMIN_FRANKLIN = 19,
                            WILLIAM_BREWSTER = 20,
                            WILLIAM_PENN = 21,
                            FATHER_JEAN_DE_BREBEUF = 22,
                            JUAN_DE_SEPULVEDA = 23,
                            BARTOLOME_DE_LAS_CASAS = 24,

                            FATHER_COUNT = 25;

    
    public static String getDescription(int foundingFather) {
        return getPrefix(foundingFather) + ".description";
    }


    private static String getPrefix(int foundingFather) {
        switch (foundingFather) {
            case ADAM_SMITH: return "foundingFather.adamSmith";
            case JACOB_FUGGER: return "foundingFather.jacobFugger";
            case PETER_MINUIT: return "foundingFather.peterMinuit";
            case PETER_STUYVESANT: return "foundingFather.peterStuyvesant";
            case JAN_DE_WITT: return "foundingFather.janDeWitt";
            case FERDINAND_MAGELLAN: return "foundingFather.ferdinandMagellan";
            case FRANSICO_DE_CORONADO: return "foundingFather.fransicoDeCoronado";
            case HERNANDO_DE_SOTO: return "foundingFather.hernandoDeSoto";
            case HENRY_HUDSON: return "foundingFather.henryHudson";
            case LA_SALLE: return "foundingFather.laSalle";
            case HERNAN_CORTES: return "foundingFather.hernanCortes";
            case GEORGE_WASHINGTION: return "foundingFather.georgeWashingtion";
            case PAUL_REVERE: return "foundingFather.paulRevere";
            case FRANCIS_DRAKE: return "foundingFather.francisDrake";
            case JOHN_PAUL_JONES: return "foundingFather.johnPaulJones";
            case THOMAS_JEFFERSON: return "foundingFather.thomasJefferson";
            case POCAHONTAS: return "foundingFather.pocahontas";
            case THOMAS_PAINE: return "foundingFather.thomasPaine";
            case SIMON_BOLIVAR: return "foundingFather.simonBolivar";
            case BENJAMIN_FRANKLIN: return "foundingFather.benjaminFranklin";
            case WILLIAM_BREWSTER: return "foundingFather.williamBrewster";
            case WILLIAM_PENN: return "foundingFather.williamPenn";
            case FATHER_JEAN_DE_BREBEUF: return "foundingFather.fatherJeanDeBrebeuf";
            case JUAN_DE_SEPULVEDA: return "foundingFather.juanDeSepulveda";
            case BARTOLOME_DE_LAS_CASAS: return "foundingFather.bartolmeDeLasCasas";
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
        }
    }
}
