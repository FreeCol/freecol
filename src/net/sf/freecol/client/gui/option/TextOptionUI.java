/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.client.gui.option;

import javax.swing.JTextField;

import net.sf.freecol.common.option.TextOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.TextOption} in order to enable
 * values to be both seen and changed.
 */
public final class TextOptionUI extends OptionUI<TextOption>  {

    private final JTextField box = new JTextField(16);


    /**
     * Creates a new <code>TextOptionUI</code> for the given
     * <code>TextOption</code>.
     *
     * @param option The <code>TextOption</code> to make a user interface for.
     * @param editable boolean whether user can modify the setting.
     */
    public TextOptionUI(final TextOption option, boolean editable) {
        super(option, editable);

        box.setText(option.getValue());
        initialize();
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public JTextField getComponent() {
        return box;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setValue(box.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        box.setText(getOption().getValue());
    }
}
