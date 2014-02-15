/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

package net.sf.freecol.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.plaf.ColorUIResource;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.panel.Flag;
import net.sf.freecol.client.gui.panel.Flag.Background;
import net.sf.freecol.client.gui.panel.Flag.Decoration;
import net.sf.freecol.client.gui.panel.Flag.UnionPosition;


public class FlagTest extends JFrame implements ActionListener, ItemListener {


    public class ColorButton extends JButton {
        private Color color = null;

        public ColorButton(Color color) {
            setColor(color);
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
            setBackground(color);
            setText(color == null ? "X" : " ");
        }

    }

    // based on the flag of Venezuela (Colombia and Ecuador are
    // similar)
    public static final Flag SPANISH_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.MIDDLE)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0xcf, 0x14, 0x2b),
                             new Color(0, 0x24, 0x7d),
                             new Color(255, 204, 0));

    // based on the flag of Brazil, particularly the Provisional
    // Flag of Republic of the United States of Brazil (November
    // 15–19, 1889)
    public static final Flag PORTUGUESE_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.CANTON)
        .setUnionColor(new Color(62, 64, 149))
        .setBackgroundColors(new Color(0, 168, 89),
                             new Color(255, 204, 41));

    // based on the current flag of the United States and its
    // various predecessors
    public static final Flag ENGLISH_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.CANTON)
        .setUnionColor(new Color(.234f, .233f, .430f))
        .setBackgroundColors(new Color(.698f, .132f, .203f),
                             Color.WHITE);

    // based on the flag of Louisiana in 1861 and other similar
    // French colonial flags
    public static final Flag FRENCH_FLAG =
        new Flag(Background.PALES, Decoration.NONE, UnionPosition.LEFT)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0, 0x23, 0x95),
                             Color.WHITE,
                             new Color(0xed, 0x29, 0x39));

    // Dutch flag
    public static final Flag DUTCH_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.TOP)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(new Color(0xae, 0x1c, 0x28),
                             Color.WHITE,
                             new Color(0x21, 0x46, 0x6b));

    // Swedish flag
    public static final Flag SWEDISH_FLAG =
        new Flag(Background.QUARTERLY, Decoration.SCANDINAVIAN_CROSS, UnionPosition.CANTON)
        .setUnionColor(null)
        .setDecorationColor(new Color(0xFE, 0xCB, 0))
        .setBackgroundColors(new Color(0, 0x52, 0x93));

    // Danish flag
    public static final Flag DANISH_FLAG =
        new Flag(Background.QUARTERLY, Decoration.SCANDINAVIAN_CROSS, UnionPosition.CANTON)
        .setUnionColor(null)
        .setDecorationColor(Color.WHITE)
        .setBackgroundColors(new Color(0xC6, 0x0C, 0x30));

    // Russian flag
    public static final Flag RUSSIAN_FLAG =
        new Flag(Background.FESSES, Decoration.NONE, UnionPosition.MIDDLE)
        .setStripes(3)
        .setUnionColor(null)
        .setBackgroundColors(Color.WHITE,
                             new Color(0, 0x39, 0xa6),
                             new Color(0xd5, 0x2b, 0x1e));



    private final Flag[] FLAGS = new Flag[] {
        ENGLISH_FLAG, SPANISH_FLAG, FRENCH_FLAG, DUTCH_FLAG,
        PORTUGUESE_FLAG, SWEDISH_FLAG, DANISH_FLAG, RUSSIAN_FLAG, null
    };

    private final String[] FLAG_NAMES = new String[] {
        "England", "Spain", "France", "Netherlands",
        "Portugal", "Sweden", "Denmark", "Russia", "Custom"
    };

    private Flag flag;

    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox flags = new JComboBox(FLAG_NAMES);
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox decoration = new JComboBox(Flag.Decoration.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox background = new JComboBox(Flag.Background.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox union = new JComboBox(Flag.UnionPosition.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox stars = new JComboBox(getNumbers(50));
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox stripes = new JComboBox(getNumbers(13));

    private ColorButton unionColor = new ColorButton(Color.BLUE);
    private ColorButton starColor = new ColorButton(Color.WHITE);
    private ColorButton decorationColor = new ColorButton(Color.WHITE);
    private ColorButton[] backgroundColors = new ColorButton[] {
        new ColorButton(null), new ColorButton(null), new ColorButton(null),
        new ColorButton(null), new ColorButton(null), new ColorButton(null)
    };

    private Component[] customComponents = new Component[] {
        background, decoration, union, stripes,
        unionColor, starColor, decorationColor,
        backgroundColors[0], backgroundColors[1],
        backgroundColors[2], backgroundColors[3],
        backgroundColors[4], backgroundColors[5]
    };

    private JComboBox[] typeBoxes = new JComboBox[] {
        decoration, union
    };

    final JLabel label = new JLabel();

    @SuppressWarnings("unchecked")
    public FlagTest() {

        super("FlagTest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("wrap 2", "[][fill]"));
        flags.addItemListener(this);
        add(new JLabel("predefined flags"));
        add(flags);

        background.addItemListener(this);
        add(new JLabel("background"));
        add(background);

        decoration.addItemListener(this);
        add(new JLabel("decoration"));
        add(decoration);

        union.addItemListener(this);
        add(new JLabel("union"));
        add(union);

        stars.setSelectedIndex(12);
        stars.addItemListener(this);
        add(new JLabel("number of stars"));
        add(stars);

        stripes.setSelectedIndex(12);
        stripes.addItemListener(this);
        add(new JLabel("number of stripes"));
        add(stripes);

        unionColor.addActionListener(this);
        add(new JLabel("union color"));
        add(unionColor);

        decorationColor.addActionListener(this);
        add(new JLabel("decoration color"));
        add(decorationColor);

        starColor.addActionListener(this);
        add(new JLabel("star color"));
        add(starColor);

        add(new JLabel("background colors"));
        for (int index = 0; index < backgroundColors.length; index++) {
            ColorButton button = backgroundColors[index];
            button.addActionListener(this);
            if (index == 0) {
                add(button, "split 3");
            } else if (index % 3 == 0) {
                add(button, "skip, split 3");
            } else {
                add(button);
            }
        }

        add(label, "width 200, height 100");

        itemStateChanged(null);
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    public void itemStateChanged(ItemEvent e) {
        Flag.Background newBackground = (Flag.Background) background.getSelectedItem();
        Flag.Decoration newDecoration = (Flag.Decoration) decoration.getSelectedItem();
        Flag.UnionPosition newPosition = (Flag.UnionPosition) union.getSelectedItem();
        Flag newFlag = FLAGS[flags.getSelectedIndex()];
        if (e == null || e.getSource() == flags) {
            if (newFlag == null) {
                // custom
                enable(customComponents, true);
            } else {
                enable(customComponents, false);
                flag = newFlag;
                unionColor.setColor(flag.getUnionColor());
                starColor.setColor(flag.getStarColor());
                decorationColor.setColor(flag.getDecorationColor());
                List<Color> colors = flag.getBackgroundColors();
                for (int index = 0; index < backgroundColors.length; index++) {
                    Color color = (index < colors.size())
                        ? colors.get(index) : null;
                    backgroundColors[index].setColor(color);
                }
            }
        } else {
            if (newFlag == null) {
                if (e.getSource() == decoration) {
                    UnionPosition oldPosition = (UnionPosition) union.getSelectedItem();
                    union.removeAllItems();
                    for (UnionPosition position : newDecoration.unionPositions) {
                        union.addItem(position);
                    }
                    union.setSelectedItem(oldPosition);
                }
                flag = new Flag(newBackground, newDecoration, newPosition);
                flag.setStripes(stripes.getSelectedIndex() + 1);
                setColors();
            }
            flag.setStars(stars.getSelectedIndex() + 1);
        }

        label.setIcon(new ImageIcon(flag.getImage()));
    }

    public void actionPerformed(ActionEvent e) {
        ColorButton button = (ColorButton) e.getSource();
        Color color = JColorChooser
            .showDialog(FlagTest.this,
                        label.getText(),
                        button.getBackground());
        button.setColor(color);
        setColors();
        label.setIcon(new ImageIcon(flag.getImage()));
    }

    private void setColors() {
        flag.setUnionColor(unionColor.getBackground());
        flag.setStarColor(starColor.getBackground());
        List<Color> colors = new ArrayList<Color>();
        for (ColorButton button : backgroundColors) {
            Color color = button.getColor();
            if (color != null) {
                colors.add(color);
            }
            flag.setBackgroundColors(colors);
        }
    }

    public String[] getNumbers(int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = Integer.toString(index + 1);
        }
        return result;
    }

    private void enable(Component[] components, boolean value) {
        for (Component component : components) {
            component.setEnabled(value);
        }
    }


    public static void main(String[] args) {

        FlagTest frame = new FlagTest();

        // display the window
        frame.pack();
        frame.setVisible(true);


    }

}
