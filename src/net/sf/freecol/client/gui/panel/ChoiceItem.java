/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Player;


/**
 * Can be used as a single choice for the
 * {@link net.sf.freecol.client.gui.panel.FreeColDialog#createChoiceDialog(FreeColClient, GUI, String, String, List) choice dialog}.
 */
public class ChoiceItem<T> {

    private String text;
    private T object;
    private boolean enabled;


    /**
     * Creates a new <code>ChoiceItem</code> with the
     * given object.
     *
     * @param text The text that should be used to represent this choice.
     * @param object The object contained by this choice.
     * @param enable Sets if the option should be enabled or not       
     */
    public ChoiceItem(String text, T object, boolean enable) {
        this.text = text;
        this.object = object;
        this.enabled = enable;
    }


    /**
     * Creates a new <code>ChoiceItem</code> with the
     * given object.
     *
     * @param text The text that should be used to represent this choice.
     * @param object The object contained by this choice.
     */
    public ChoiceItem(String text, T object) {
        this(text, object, true);
    }


    /**
     * Creates a new <code>ChoiceItem</code> with the
     * given object.
     *
     * @param object The object contained by this choice.
     */
    public ChoiceItem(T object) {
        this(Messages.message(object.toString()), object, true);
        
        // Check to see if we can improve upon object.toString()
        if(object instanceof AbstractGoods) {
            this.text = ((AbstractGoods)object).getAmount() + " " + Messages.message(((AbstractGoods)object).getNameKey());
        } else if (object instanceof AbstractUnit) {
            this.text = Messages.message(((AbstractUnit)object).getId());
        } else if (object instanceof Player) {
            this.text = Messages.message(((Player)object).getRulerNameKey()) + " (" + Messages.message(((Player)object).getNationName()) + ")";
        }
    }


    /**
     * Gets the <code>Object</code> contained by this choice.
     * @return The <code>Object</code>.
     */
    public T getObject() {
        return object;
    }


    /**
     * Gets the choice as an <code>int</code>.
     *
     * @return The number representing this object.
     * @exception ClassCastException if the {@link #getObject object} is
     *            not an <code>Integer</code>.
     */
    public int getChoice() {
        return ((Integer) object).intValue();
    }
    
    /**
     * Checks if the option should be enabled or not
     * @return enable status
     */
    public boolean isEnabled(){
    	return this.enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return text;
    }
}
