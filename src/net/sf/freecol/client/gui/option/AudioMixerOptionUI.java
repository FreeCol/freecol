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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.AudioMixerOption.MixerWrapper;


/**
 * This class provides visualization for an
 * {@link net.sf.freecol.common.option.AudioMixerOption}
 * in order to enable values to be both seen and changed.
 */
public final class AudioMixerOptionUI extends OptionUI<AudioMixerOption> {

    private final GUI gui;
    private final JPanel panel = new JPanel();
    private final JComboBox<MixerWrapper> cbox;
    private final JButton button1;
    private final JButton button2;
    private final JLabel currentMixerLabel;


    /**
     * Creates a new <code>AudioMixerOptionUI</code> for the given
     * <code>AudioMixerOption</code>.
     *
     * @param gui The GUI.
     * @param option The <code>AudioMixerOption</code> to make a user
     *      interface for.
     * @param editable boolean whether user can modify the setting
     */
    public AudioMixerOptionUI(GUI gui, final AudioMixerOption option,
                              boolean editable) {
        super(option, editable);

        this.gui = gui;

        BorderLayout layout = new BorderLayout();
        layout.setHgap(15);
        panel.setLayout(layout);

        cbox = new JComboBox<>();
        panel.add(cbox, BorderLayout.WEST);

        currentMixerLabel = new JLabel();
        panel.add(currentMixerLabel, BorderLayout.EAST);
        updateMixerLabel();

        button1 = Utility.localizedButton("test");
        panel.add(button1);

        button2 = Utility.localizedButton("music");
        panel.add(button2);

        cbox.add(super.getJLabel());
        cbox.setModel(new DefaultComboBoxModel<>(getOption().getChoices()
                .toArray(new MixerWrapper[0])));
        reset();
        cbox.setEnabled(editable);

        ActionListener aHandler = (ActionEvent ae) -> {
            if (ae.getSource() == button1) {
                gui.playSound("sound.event.buildingComplete");
            } else if (ae.getSource() == button2) {
                gui.playSound("sound.intro.general");
            } else if (ae.getSource() == cbox) {
                MixerWrapper value = (MixerWrapper) cbox.getSelectedItem();
                if (getOption().getValue() != value) {
                    getOption().setValue(value);
                    updateMixerLabel();
                }
            }
        };
        button1.addActionListener(aHandler);
        button2.addActionListener(aHandler);
        cbox.addActionListener(aHandler);

        initialize();
    }

    private void updateMixerLabel() {
        currentMixerLabel.setText(gui.getSoundMixerLabelText());
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public final JLabel getJLabel() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getComponent() {
        return panel;
    }

    /**
     * Updates the value of the
     * {@link net.sf.freecol.common.option.Option} this object keeps.
     */
    @Override
    public void updateOption() {
        getOption().setValue((MixerWrapper)cbox.getSelectedItem());
    }

    /**
     * Reset with the value from the option.
     */
    @Override
    public void reset() {
        cbox.setSelectedItem(getOption().getValue());
    }
}
