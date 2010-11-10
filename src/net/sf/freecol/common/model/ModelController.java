/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.util.ArrayList;

import net.sf.freecol.common.model.Player.Stance;

/**
* The <code>ModelController</code> is used by the model to perform
* tasks which cannot be done by the model.
*
* <br><br>
*
* The tasks might not be allowed to perform within the model (like generating
* random numbers or creating new {@link FreeColGameObject FreeColGameObjects}),
* or the model might have insufficient data.
*
* <br><br>
*
* Any {@link FreeColGameObject} may get access to the <code>ModelController</code>
* by using {@link Game#getModelController getGame().getModelController()}.
*/
public interface ModelController {

}
