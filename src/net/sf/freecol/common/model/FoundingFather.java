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
        switch (foundingFather) {
            case ADAM_SMITH: return "Factories produce 1.5 manufactured goods per 1 raw material";
            case JACOB_FUGGER: return "All boycotts currently in effect are dropped";
            case PETER_MINUIT: return "Indians no longer demand payment for their land";
            case PETER_STUYVESANT: return "The construction of custom houses becomes possible";
            case JAN_DE_WITT: return "Trade with foreign colonies becomes possible";
            case FERDINAND_MAGELLAN: return "Naval vessels movement is increased by 1 and the time to sail to/from Europe/America is shortened";
            case FRANSICO_DE_CORONADO: return "All existing colonies become visible on the map";
            case HERNANDO_DE_SOTO: return "Exploration of Lost City Rumors always yields a positive result and all units have an extended sight radius";
            case HENRY_HUDSON: return "Increases output of all Fur trappers by 100%";
            case LA_SALLE: return "Gives all existing and future colonies a stockade when they population reaches 3";
            case HERNAN_CORTES: return "Conquered native settlements always yeild treasure (and in greater abundance) and the King\'s galleons transport it free of charge";
            case GEORGE_WASHINGTION: return "Any soldier or dragoon who wins a combat is automatically upgraded to the next possible level";
            case PAUL_REVERE: return "When a colony with no standing soldiers is attacked, a colonist automatically takes up any stockpiled muskets and defends";
            case FRANCIS_DRAKE: return "Increases the combat strength of all Privateers by 50%";
            case JOHN_PAUL_JONES: return "A Frigate is added to your colonial navy (free)";
            case THOMAS_JEFFERSON: return "Increases Liberty Bell production in colonies by 50%";
            case POCAHONTAS: return "All tension levels between you and natives are removed and Indian alarm is generated half as fast";
            case THOMAS_PAINE: return "Increases Liberty Bell production in colonies by the value of the current tax rate";
            case SIMON_BOLIVAR: return "Sons of Liberty membership in all existing colonies is increased by 20%";
            case BENJAMIN_FRANKLIN: return "The King\'s foreign wars no longer have effect on relationships in the New World and Europeans in the New World always offer peace";
            case WILLIAM_BREWSTER: return "No more criminals or servants appear on the docks and you can select which immigrant in the recruitment pool to move to the docks";
            case WILLIAM_PENN: return "Cross production in all colonies in increased by 50%";
            case FATHER_JEAN_DE_BREBEUF: return "All missionaries function as experts";
            case JUAN_DE_SEPULVEDA: return "Increases chance that a subjugated Indian settlement will \"convert\" and join a colony";
            case BARTOLOME_DE_LAS_CASAS: return "All existing Indian converts are converted to free colonists";
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
        }
    }
}
