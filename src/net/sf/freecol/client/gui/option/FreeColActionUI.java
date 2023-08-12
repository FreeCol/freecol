/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.util.ImageUtils;


/**
 * User interface for displaying/changing a keyboard accelerator for a
 * {@code FreeColAction}.
 */
public final class FreeColActionUI extends OptionUI<FreeColAction>
    implements ActionListener {

    private OptionGroupUI optionGroupUI;
    private KeyStroke keyStroke;
    private final JButton recordButton;
    private final JButton removeButton;
    private final BlinkingLabel bl;
    private final JPanel panel = new JPanel();


    /**
     * Creates a new {@code FreeColActionUI} for the
     * given {@code FreeColAction}.
     *
     * @param option The {@code FreeColAction} to make a user
     *       interface for.
     * @param editable boolean whether user can modify the setting
     */
    public FreeColActionUI(FreeColAction option, boolean editable) {
        super(option, editable);

        this.optionGroupUI = null;

        keyStroke = option.getAccelerator();

        panel.add(getJLabel());

        bl = new BlinkingLabel();
        panel.add(bl);

        recordButton = new JButton(getRecordImage());
        recordButton.addActionListener(this);
        panel.add(recordButton);

        removeButton = new JButton(getRemoveImage());
        removeButton.addActionListener(this);
        panel.add(removeButton);

        initialize();
    }

    /**
    * Creates an icon for symbolizing the recording of a {@code KeyStroke}.
    * @return The {@code ImageIcon}.
    */
    public static ImageIcon getRecordImage() {
        final int imageSize = (int) (9 * FontLibrary.getFontScaling());
        final int strokeSize = (int) (1 * FontLibrary.getFontScaling());
        final int circleSize = imageSize - strokeSize*2;
        final BufferedImage bi = ImageUtils.createBufferedImage(imageSize, imageSize);
        final Graphics2D g = bi.createGraphics();
        g.setColor(Color.RED);
        g.fillOval(strokeSize, strokeSize, circleSize, circleSize);
        g.setColor(Color.BLACK);
        g.drawOval(strokeSize, strokeSize, circleSize, circleSize);
        g.dispose();
        return new ImageIcon(bi);
    }


    /**
    * Creates an icon to be used on the button that removes a keyboard accelerator.
    * @return The {@code ImageIcon}.
    */
    public static ImageIcon getRemoveImage() {
        final int imageSize = (int) (9 * FontLibrary.getFontScaling());
        final int strokeSize = (int) (2 * FontLibrary.getFontScaling());
        final BufferedImage bi = ImageUtils.createBufferedImage(imageSize, imageSize);
        final Graphics2D g = bi.createGraphics();
        
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(strokeSize + 1));
        g.drawLine(strokeSize, strokeSize, imageSize - strokeSize, imageSize - strokeSize);
        g.drawLine(imageSize - strokeSize, strokeSize, strokeSize, imageSize - strokeSize);
        
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(strokeSize));
        g.drawLine(strokeSize, strokeSize, imageSize - strokeSize, imageSize - strokeSize);
        g.drawLine(imageSize - strokeSize, strokeSize, strokeSize, imageSize - strokeSize);

        return new ImageIcon(bi);
    }

    /**
    * Gets a string to represent the given {@code KeyStroke} to the user.
    *
    * @param keyStroke {@code java.awt.event.KeyStroke}
    * @return String
    */
    public static String getHumanKeyStrokeText(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return " ";
        }

        String s = InputEvent.getModifiersExText(keyStroke.getModifiers());
        if (!s.isEmpty()) s += "+";
        return s + KeyEvent.getKeyText(keyStroke.getKeyCode());
    }


    /**
    * Removes the given {@code KeyStroke}. That is:
    * This action's {@code KeyStroke} is set to
    * {@code null} if it is the same as the given
    * {@code KeyStroke}.
    *
    * @param k The {@code KeyStroke} to be removed.
    */
    public void removeKeyStroke(KeyStroke k) {
        if (k != null && keyStroke != null
            && k.getKeyCode() == keyStroke.getKeyCode()
            && k.getModifiers() == keyStroke.getModifiers()) {
            keyStroke = null;
            bl.setText(" ");
        }
    }

    public void setOptionGroupUI(OptionGroupUI ui) {
        this.optionGroupUI = ui;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == recordButton) {
            bl.startBlinking();
            bl.requestFocus();
        } else if (ae.getSource() == removeButton) {
            bl.stopBlinking();
            bl.setText(" ");
            keyStroke = null;
        }
    }


    /**
     * Label for displaying a {@code KeyStroke}.
     */
    class BlinkingLabel extends JLabel implements ActionListener, KeyListener, MouseListener {

        private final Timer blinkingTimer = new Timer(500, this);
        private boolean blinkOn = false;

        BlinkingLabel() {
            super(getHumanKeyStrokeText(keyStroke), JLabel.CENTER);

            setOpaque(false);
            setBorder(Utility.getTrivialLineBorder());
            addKeyListener(this);
            addMouseListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1) {
                startBlinking();
                requestFocus();
            }
        }


        @Override
        public void mouseEntered(MouseEvent e) { /* No such event */ }
        @Override
        public void mouseExited(MouseEvent e) { /* No such event */ }
        @Override
        public void mousePressed(MouseEvent e) { /* No such event */ }
        @Override
        public void mouseReleased(MouseEvent e) { /* No such event */ }


        @Override
        public Dimension getMinimumSize() {
            return new Dimension((int) (80 * FontLibrary.getFontScaling()), super.getMinimumSize().height);
        }

        @Override
        public Dimension getPreferredSize() {
            return getMinimumSize();
        }


        public void startBlinking() {
            blinkingTimer.start();
        }


        public void stopBlinking() {
            blinkingTimer.stop();
            setOpaque(false);
            repaint();
        }

        @Override
        public void keyPressed(KeyEvent e) { /* No such event */ }

        @Override
        public void keyTyped(KeyEvent e) { /* No such event */ }

        @Override
        public void keyReleased(KeyEvent e) {
            KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(),
                                                  e.getModifiersEx());
            if (FreeColActionUI.this.optionGroupUI != null) {
                FreeColActionUI.this.optionGroupUI.removeKeyStroke(ks);
            }
            keyStroke = ks;
            //keyStroke = KeyStroke.getKeyStroke(new Character(e.getKeyChar()), e.getModifiers());
            stopBlinking();
            setText(getHumanKeyStrokeText(keyStroke));
            recordButton.requestFocus();
        }


        // Interface ActionListener

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent ae) {
            if (!hasFocus()) stopBlinking();

            if (blinkOn) {
                setOpaque(false);
                blinkOn = false;
                repaint();
            } else {
                setOpaque(true);
                setBackground(Color.RED);
                blinkOn = true;
                repaint();
            }
        }
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getComponent() {
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setAccelerator(keyStroke);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        keyStroke = getOption().getAccelerator();
        bl.setText(getHumanKeyStrokeText(keyStroke));
    }
}
