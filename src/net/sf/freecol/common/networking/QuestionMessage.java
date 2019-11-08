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

package net.sf.freecol.common.networking;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;


/**
 * The basic question message wrapper.
 */
public class QuestionMessage extends WrapperMessage {

    public static final String TAG = "question";


    /**
     * Create a new {@code QuestionMessage} of a given type.
     *
     * @param replyId The reply id.
     * @param message The {@code Message} to encapsulate.
     */
    public QuestionMessage(int replyId, Message message) {
        super(TAG, replyId, message);
    }

    /**
     * Create a new {@code QuestionMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     * @exception FreeColException if the internal message can not be read.
     */
    public QuestionMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException, FreeColException {
        super(TAG, game, xr);
    }
}
