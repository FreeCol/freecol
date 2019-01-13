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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when doing a monarch action.
 */
public class MonarchActionMessage extends ObjectMessage {

    public static final String TAG = "monarchAction";
    private static final String ACTION_TAG = "action";
    private static final String MONARCH_TAG = "monarch";
    private static final String RESULT_TAG = "result";
    private static final String TAX_TAG = "tax";


    /**
     * Create a new {@code MonarchActionMessage} with the given action
     * to be sent to the client to solicit a response.
     *
     * @param action The {@code MonarchAction} to do.
     * @param template A {@code StringTemplate} describing the action.
     * @param monarchKey The resource key for the monarch image.
     */
    public MonarchActionMessage(MonarchAction action,
                                StringTemplate template, String monarchKey) {
        super(TAG, ACTION_TAG, action.toString(), MONARCH_TAG, monarchKey);

        appendChild(template);
    }

    /**
     * Create a new {@code MonarchActionMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public MonarchActionMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, ACTION_TAG, MONARCH_TAG, TAX_TAG, RESULT_TAG);

        StringTemplate template = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (StringTemplate.TAG.equals(tag)) {
                if (template == null) {
                    template = xr.readFreeColObject(game, StringTemplate.class);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected(StringTemplate.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
        appendChild(template);
    }


    /**
     * Gets the monarch action type of this message.
     *
     * @return The monarch action type.
     */
    private MonarchAction getAction() {
        return getEnumAttribute(ACTION_TAG, MonarchAction.class,
                                (MonarchAction)null);
    }

    /**
     * Gets the template of this message.
     *
     * @return The template.
     */
    private StringTemplate getTemplate() {
        return getChild(0, StringTemplate.class);
    }

    /**
     * Gets the monarch key.
     *
     * @return The monarch key.
     */
    private String getMonarchKey() {
        return getStringAttribute(MONARCH_TAG);
    }

    /**
     * Gets the tax amount attached to this message.
     *
     * @return The tax amount, or negative if none present.
     */
    private int getTax() {
        return getIntegerAttribute(TAX_TAG, -1);
    }

    /**
     * Gets the result.
     *
     * @return The result.
     */
    private Boolean getResult() {
        return getBooleanAttribute(RESULT_TAG, (Boolean)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        MessagePriority mp = null;
        MonarchAction action = getAction();
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            mp = Message.MessagePriority.EARLY;
            break;
        case NO_ACTION: case FORCE_TAX: case WAIVE_TAX: case DISPLEASURE:
            mp = Message.MessagePriority.NORMAL;
            break;
        case LOWER_TAX_OTHER: case LOWER_TAX_WAR: case ADD_TO_REF:
        case DECLARE_PEACE: case DECLARE_WAR:
        case SUPPORT_LAND: case SUPPORT_SEA:
            mp = Message.MessagePriority.LATE;
            break;
        }
        if (mp == null) {
            throw new RuntimeException("Missing priority for action: "
                + action);
        }
        return mp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final MonarchAction action = getAction();
        final int tax = getTax();

        aiPlayer.monarchActionHandler(action, tax);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final MonarchAction action = getAction();
        final StringTemplate template = getTemplate();
        final String key = getMonarchKey();
        
        igc(freeColClient).monarchActionHandler(action, template, key);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return igc(freeColServer)
            .monarchAction(serverPlayer, getAction(), getResult());
    }


    // Public modifiers

    /**
     * Sets the tax amount attached to this message.
     *
     * @param tax The tax amount.
     * @return This message.
     */
    public MonarchActionMessage setTax(int tax) {
        setStringAttribute(TAX_TAG, Integer.toString(tax));
        return this;
    }

    /**
     * Sets the result.
     *
     * @param accept The new result.
     * @return This message.
     */
    public MonarchActionMessage setResult(boolean accept) {
        setStringAttribute(RESULT_TAG, Boolean.toString(accept));
        return this;
    }
}
