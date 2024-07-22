/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.option.FullscreenDisplayModeOption;
import net.sf.freecol.common.option.FullscreenDisplayModeOption.FullscreenDisplayModeWrapper;


public final class FullscreenDisplayModeOptionUI extends OptionUI<FullscreenDisplayModeOption> {

    private final JComboBox<FullscreenDisplayModeWrapper> cbox;


    /**
     * Creates a new {@code FullscreenDisplayModeOptionUI} for the given {@code FullscreenDisplayModeOption}.
     *
     * @param gui The GUI.
     * @param option The {@code FullscreenDisplayModeOption} to make a user
     *      interface for.
     * @param editable boolean whether user can modify the setting
     */
    public FullscreenDisplayModeOptionUI(GUI gui, FullscreenDisplayModeOption option, boolean editable) {
        super(option, editable);

        cbox = new JComboBox<>();
        //panel.add(cbox, BorderLayout.LINE_START);

        cbox.setModel(new DefaultComboBoxModel<>(getChoices().toArray(new FullscreenDisplayModeWrapper[0])));
        cbox.setRenderer(new FreeColComboBoxRenderer<>());
        reset();
        cbox.setEnabled(editable);

        /*
        ActionListener aHandler = (ActionEvent ae) -> {
            FullscreenDisplayModeWrapper value = (FullscreenDisplayModeWrapper) cbox.getSelectedItem();
            getOption().setValue(value);
            gui.refreshGuiUsingClientOptions();
        };
        cbox.addActionListener(aHandler);
                */

        initialize();
    }
    
    private List<FullscreenDisplayModeWrapper> getChoices() {
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        
        final List<FullscreenDisplayModeWrapper> result = new ArrayList<>();
        result.add(new FullscreenDisplayModeWrapper(null));
        
        List.of(gd.getDisplayModes()).stream()
                .map(dm -> new FullscreenDisplayModeWrapper(dm))
                .sorted()
                .distinct()
                .forEach(dmw -> result.add(dmw));
        
        return result;
    }

    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent getComponent() {
        return cbox;
    }

    /**
     * Updates the value of the
     * {@link net.sf.freecol.common.option.Option} this object keeps.
     */
    @Override
    public void updateOption() {
        getOption().setValue((FullscreenDisplayModeWrapper) cbox.getSelectedItem());
    }

    /**
     * Reset with the value from the option.
     */
    @Override
    public void reset() {
        cbox.setSelectedItem(getOption().getValue());
    }
}
