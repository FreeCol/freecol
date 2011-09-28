/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import javax.sound.sampled.Mixer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.AudioMixerOption.MixerWrapper;


/**
 * This class provides visualization for an {@link
 * net.sf.freecol.common.option.AudioMixerOption}. In order to enable
 * values to be both seen and changed.
 */
public final class AudioMixerOptionUI extends OptionUI<AudioMixerOption> {

    JPanel panel = new JPanel();

    private final FreeColClient client = FreeColClient.get();
    private JComboBox cbox;
    private JButton button1, button2;
    private JLabel currentMixerLabel;

    private ActionListener aHandler = new ActionListener () {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == button1) {
                client.playSound("sound.event.buildingComplete");
            } else if (e.getSource() == button2) {
                client.playSound("sound.intro.general");
            } else if (e.getSource() == cbox) {
                MixerWrapper value = (MixerWrapper) cbox.getSelectedItem();
                if (getOption().getValue() != value) {
                    getOption().setValue(value);
                    updateMixerLabel();
                }
            }
        }
    };

    /**
     * Creates a new <code>AudioMixerOptionUI</code> for the given
     * <code>AudioMixerOption</code>.
     *
     * @param option The <code>AudioMixerOption</code> to make a user
     *      interface for.
     * @param editable boolean whether user can modify the setting
     */
    public AudioMixerOptionUI(final AudioMixerOption option, boolean editable) {
        super(option, editable);

        BorderLayout layout = new BorderLayout();
        layout.setHgap(15);
        panel.setLayout(layout);

        cbox = new JComboBox();
        panel.add(cbox, BorderLayout.WEST);

        currentMixerLabel = new JLabel();
        panel.add(currentMixerLabel, BorderLayout.EAST);
        updateMixerLabel();

        button1 = new JButton(Messages.message("Test"));
        panel.add(button1);
        button1.addActionListener(aHandler);

        button2 = new JButton(Messages.message("Music"));
        panel.add(button2);
        button2.addActionListener(aHandler);

        cbox.add(super.getLabel());
        cbox.setModel(new DefaultComboBoxModel(getOption().getOptions()));
        reset();

        cbox.setEnabled(editable);
        cbox.addActionListener(aHandler);

        initialize();
    }

    private void updateMixerLabel() {
        SoundPlayer soundPlayer = FreeColClient.get().getSoundPlayer();
        Mixer mixer;
        String text = (soundPlayer == null)
            ? Messages.message("nothing")
            : ((mixer = soundPlayer.getMixer()) == null)
            ? Messages.message("none")
            : mixer.getMixerInfo().getName();
        currentMixerLabel.setText(Messages.message("Current") + ":  " + text);
    }


    /**
     * Returns <code>null</code>, since this OptionUI does not require
     * an external label.
     *
     * @return null
     */
    @Override
    public final JLabel getLabel() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JPanel getComponent() {
        return panel;
    }

    /**
     * Updates the value of the {@link
     * net.sf.freecol.common.getOption().Option} this object keeps.
     */
    public void updateOption() {
        getOption().setValue((MixerWrapper) cbox.getSelectedItem());
    }

    /**
     * Reset with the value from the getOption().
     */
    public void reset() {
        cbox.setSelectedItem(getOption().getValue());
    }
}
