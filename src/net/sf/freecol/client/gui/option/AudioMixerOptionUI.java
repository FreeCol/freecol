/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.AudioMixerOption.MixerWrapper;


/**
 * This class provides visualization for an {@link AudioMixerOption}. In order to
 * enable values to be both seen and changed.
 */
public final class AudioMixerOptionUI extends JComboBox implements OptionUpdater, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(AudioMixerOptionUI.class.getName());

    private final AudioMixerOption option;
    private MixerWrapper originalValue;
    private JLabel label;


    /**
    * Creates a new <code>AudioMixerOptionUI</code> for the given <code>AudioMixerOption</code>.
    * @param option The <code>AudioMixerOption</code> to make a user interface for.
    */
    public AudioMixerOptionUI(final AudioMixerOption option, boolean editable) {

        this.option = option;
        this.originalValue = option.getValue();

        String name = Messages.getName(option);
        String description = Messages.getShortDescription(option);
        label = new JLabel(name, JLabel.LEFT);
        label.setToolTipText((description != null) ? description : name);
        add(label);

        setModel(new DefaultComboBoxModel(option.getOptions()));
        reset();
        
        setEnabled(editable);
        addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                if (option.isPreviewEnabled()) {
                    MixerWrapper value = (MixerWrapper) getSelectedItem();
                    if (option.getValue() != value) {
                        option.setValue(value);
                    }
                }
            }
        });

        option.addPropertyChangeListener(this);
        setOpaque(false);
    }

    /**
     * Get the <code>Label</code> value.
     *
     * @return a <code>JLabel</code> value
     */
    public JLabel getLabel() {
        return label;
    }

    /**
     * Set the <code>Label</code> value.
     *
     * @param newLabel The new Label value.
     */
    public void setLabel(final JLabel newLabel) {
        this.label = newLabel;
    }

    /**
     * Rollback to the original value.
     * 
     * This method gets called so that changes made to options with
     * {@link Option#isPreviewEnabled()} is rolled back
     * when an option dialoag has been cancelled.
     */
    public void rollback() {
        option.setValue(originalValue);
    }
    
    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        option.removePropertyChangeListener(this);    
    }
    
    /**
     * Updates this UI with the new data from the option.
     * @param event The event.
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("value")) {
            MixerWrapper value = (MixerWrapper) event.getNewValue();
            if (value != getSelectedItem()) {
                setSelectedItem(value);
                originalValue = value;
            }
        }
    }
    
    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValue((MixerWrapper) getSelectedItem());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        setSelectedItem(option.getValue());
    }
}
