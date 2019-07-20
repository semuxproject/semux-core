/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import static java.util.Arrays.stream;

import static org.semux.core.Unit.SEM;
import static org.semux.gui.TextContextMenuItem.COPY;
import static org.semux.gui.TextContextMenuItem.CUT;
import static org.semux.gui.TextContextMenuItem.PASTE;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.ethereum.vm.util.HashUtil;
import org.semux.core.Amount;
import org.semux.core.Transaction;
import org.semux.core.Unit;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.gui.model.WalletAccount;
import org.semux.message.GuiMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import io.netty.util.internal.StringUtil;

public class SwingUtil {

    private static final Logger logger = LoggerFactory.getLogger(SwingUtil.class);

    private static int fractionDigits = 3;

    private static Unit unit = SEM;

    private SwingUtil() {
    }

    /**
     * Put a JFrame in the center of screen.
     *
     * @param frame
     * @param width
     * @param height
     */
    public static void alignFrameToMiddle(JFrame frame, int width, int height) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = ((int) d.getWidth() - width) / 2;
        int y = ((int) d.getHeight() - height) / 2;
        frame.setLocation(x, y);
        frame.setBounds(x, y, width, height);
    }

    /**
     * Load an ImageIcon from resource, and rescale it.
     *
     * @param imageName
     *            image name
     * @return an image icon if exists, otherwise null
     */
    public static ImageIcon loadImage(String imageName, int width, int height) {
        String imgLocation = imageName + ".png";
        URL imageURL = SwingUtil.class.getResource(imgLocation);

        if (imageURL == null) {
            logger.warn("Resource not found: {}", imgLocation);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(image);
        } else {
            ImageIcon icon = new ImageIcon(imageURL);
            Image img = icon.getImage();
            Image img2 = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img2);
        }
    }

    /**
     * Generate an empty image icon.
     *
     * @param width
     * @param height
     * @return
     */
    public static ImageIcon emptyImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(new Color(255, 255, 255));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        return new ImageIcon(image);
    }

    /**
     * Set the preferred width of table columns.
     *
     * @param table
     * @param total
     * @param widths
     */
    public static void setColumnWidths(JTable table, int total, double... widths) {
        TableColumnModel model = table.getColumnModel();
        for (int i = 0; i < widths.length; i++) {
            model.getColumn(i).setPreferredWidth((int) (total * widths[i]));
        }
    }

    /**
     * Set the alignments of table columns.
     *
     * @param table
     * @param right
     */
    public static void setColumnAlignments(JTable table, boolean... right) {
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        TableColumnModel model = table.getColumnModel();
        for (int i = 0; i < right.length; i++) {
            if (right[i]) {
                model.getColumn(i).setCellRenderer(rightRenderer);
            }
        }
    }

    /**
     * Generate an QR image for the given text.
     *
     * @param text
     * @param width
     * @param height
     * @return
     * @throws WriterException
     */
    public static BufferedImage createQrImage(String text, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hintMap.put(EncodeHintType.MARGIN, 2);
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hintMap);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                if (matrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }

        return image;
    }

    /**
     * Adds a copy-paste-cut popup to the given component.
     *
     * @param comp
     */
    private static void addTextContextMenu(JComponent comp, List<TextContextMenuItem> textContextMenuItems) {
        JPopupMenu popup = new JPopupMenu();

        textContextMenuItems.forEach(i -> {
            JMenuItem menuItem = new JMenuItem(i.toAction());
            menuItem.setText(i.toString());
            popup.add(menuItem);
        });

        comp.setComponentPopupMenu(popup);
    }

    /**
     * Ensures that a text field gets focused when it's clicked. Credits to:
     * https://stackoverflow.com/a/41965891/670662
     *
     * @param textField
     */
    private static void addTextMouseClickFocusListener(final JComponent textField) {
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                textField.requestFocusInWindow();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                textField.requestFocusInWindow();
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                textField.requestFocusInWindow();
            }
        });
    }

    /**
     * Generates a text field with copy-paste-cut popup menu.
     *
     * @return
     */
    public static JTextField textFieldWithCopyPastePopup() {
        JTextField textField = new JTextField();
        textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        addTextContextMenu(textField, Arrays.asList(COPY, PASTE, CUT));
        addTextMouseClickFocusListener(textField);
        return textField;
    }

    /**
     * Generates a text field with copy-paste-cut popup menu.
     *
     * @return
     */
    public static <T> JComboBox<T> comboBoxWithCopyPastePopup() {
        JComboBox<T> textField = new JComboBox<>();
        textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        addTextContextMenu(textField, Arrays.asList(COPY, PASTE, CUT));
        addTextMouseClickFocusListener(textField);
        return textField;
    }

    /**
     * Generates a readonly selectable text area.
     *
     * @param txt
     * @return
     */
    public static JTextArea textAreaWithCopyPopup(String txt) {
        JTextArea c = new JTextArea(txt);
        c.setBackground(null);
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        c.setEditable(false);

        addTextContextMenu(c, Collections.singletonList(COPY));
        return c;
    }

    /**
     * Convenience factory method for creating buttons
     *
     * @param text
     * @param listener
     * @param action
     * @return
     */
    public static JButton createDefaultButton(String text, ActionListener listener, Action action) {
        JButton button = new JButton(text);
        button.setActionCommand(action.name());
        button.addActionListener(listener);
        return button;
    }

    /**
     * Parses a number from a localized string.
     *
     * @param str
     * @return
     * @throws ParseException
     */
    public static BigDecimal parseNumber(String str) throws ParseException {
        DecimalFormat format = new DecimalFormat();
        format.setParseBigDecimal(true);
        ParsePosition position = new ParsePosition(0);
        BigDecimal number = (BigDecimal) format.parse(str, position);
        if (position.getIndex() != str.length() || number == null) {
            throw new ParseException("Failed to parse number: " + str, position.getIndex());
        }
        return number;
    }

    /**
     * Formats a number as a localized string.
     *
     * @param number
     * @param decimals
     * @return
     */
    public static String formatNumber(Number number, int decimals) {
        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(decimals);
        format.setRoundingMode(RoundingMode.FLOOR);

        return format.format(number);
    }

    /**
     * Format a number with zero decimals.
     *
     * @param number
     * @return
     */
    public static String formatNumber(Number number) {
        return formatNumber(number, 0);
    }

    /**
     * Formats a Semux value.
     *
     * @param a
     * @return
     */
    public static String formatAmount(Amount a) {
        return formatAmount(a, unit, fractionDigits, true);
    }

    /**
     * Formats a Semux value.
     *
     * @param a
     * @return
     */
    public static String formatAmountNoUnit(Amount a) {
        return formatAmount(a, unit, fractionDigits, false);
    }

    /**
     * Formats a Semux value without truncation.
     *
     * @param a
     * @return
     */
    public static String formatAmountFull(Amount a) {
        return formatAmount(a, unit, 9, true);
    }

    /**
     * Formats a Semux value.
     *
     * @param a
     * @param unit
     * @param fractionDigits
     * @param withUnit
     * @return
     */
    private static String formatAmount(Amount a, Unit unit, int fractionDigits, boolean withUnit) {
        return formatNumber(unit.toDecimal(a, fractionDigits), fractionDigits)
                + (withUnit ? (" " + unit.symbol) : "");
    }

    /**
     * Set the default unit for {@link SwingUtil#formatAmount(Amount)}
     *
     * @param unit
     */
    public static void setDefaultUnit(String unit) {
        SwingUtil.unit = Unit.ofSymbol(unit);
    }

    /**
     * Set the default fraction digits for {@link SwingUtil#formatAmount(Amount)}
     *
     * @param fractionDigits
     */
    public static void setDefaultFractionDigits(int fractionDigits) {
        SwingUtil.fractionDigits = fractionDigits;
    }

    /**
     * Parses a Semux value.
     *
     * @param str
     * @return
     * @throws ParseException
     */
    public static Amount parseAmount(String str) throws ParseException {
        Unit theUnit = stream(Unit.values())
                .filter(u -> str.endsWith(" " + u.symbol))
                .findAny()
                .orElse(unit);
        String strNoUnit = str.replace(" " + theUnit.symbol, "");

        return theUnit.fromDecimal(parseNumber(strNoUnit));
    }

    /**
     * Formats a percentage
     *
     * @param percentage
     * @return
     */
    public static String formatPercentage(double percentage) {
        return formatPercentage(percentage, 1);
    }

    /**
     * Formats a percentage
     *
     * @param percentage
     * @return
     */
    public static String formatPercentage(double percentage, int decimals) {
        return formatNumber(percentage, decimals) + " %";
    }

    /**
     * Format a timestamp into date string.
     *
     * @param timestamp
     * @return
     */
    public static String formatTimestamp(long timestamp) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        return format.format(new Date(timestamp));
    }

    /**
     * Parse timestamp from its string representation.
     *
     * @param timestamp
     * @return
     * @throws ParseException
     */
    public static long parseTimestamp(String timestamp) throws ParseException {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        return format.parse(timestamp).getTime();
    }

    /**
     * Formats a vote
     *
     * @param vote
     * @return
     */
    public static String formatVote(Amount vote) {
        return formatAmountNoUnit(vote);
    }

    /**
     * Parses a percentage.
     *
     * @param str
     * @return
     * @throws ParseException
     */
    public static double parsePercentage(String str) throws ParseException {
        return parseNumber(str.substring(0, str.length() - 2)).doubleValue();
    }

    /**
     * Number string comparator based on its value.
     *
     * @exception
     */
    public static final Comparator<String> NUMBER_COMPARATOR = (o1, o2) -> {
        try {
            return parseNumber(o1).compareTo(parseNumber(o2));
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Value string comparator based on its value.
     *
     * @exception
     */
    public static final Comparator<String> VALUE_COMPARATOR = (o1, o2) -> {
        try {
            return parseAmount(o1).compareTo(parseAmount(o2));
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Percentage string comparator based on its value.
     *
     * @exception
     */
    public static final Comparator<String> PERCENTAGE_COMPARATOR = (o1, o2) -> {
        try {
            return Double.compare(parsePercentage(o1), parsePercentage(o2));
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Timestamp/date string comparator based on its value.
     *
     * @exception
     */
    public static final Comparator<String> TIMESTAMP_COMPARATOR = (o1, o2) -> {
        try {
            return Long.compare(parseTimestamp(o1), parseTimestamp(o2));
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Returns an description of an account.
     *
     * @param tx
     * @return
     */
    public static String getTransactionDescription(SemuxGui gui, Transaction tx) {
        switch (tx.getType()) {
        case COINBASE:
            return GuiMessages.get("BlockReward") + " => " + describeAddress(gui, tx.getTo());
        case VOTE:
        case UNVOTE:
        case TRANSFER:
        case CALL:
            return describeAddress(gui, tx.getFrom()) + " => " + describeAddress(gui, tx.getTo());
        case DELEGATE:
            return GuiMessages.get("DelegateRegistration");
        case CREATE:
            return GuiMessages.get("CreateContract") + " => "
                    + describeAddress(gui, HashUtil.calcNewAddress(tx.getFrom(), tx.getNonce()));
        default:
            return StringUtil.EMPTY_STRING;
        }
    }

    /**
     * Returns the alias, or delegate name, or abbreviation of an address.
     *
     * @param gui
     * @param address
     * @return
     */
    public static String describeAddress(SemuxGui gui, byte[] address) {
        return getAddressAlias(gui, address)
                .orElse(getAddressDelegateName(gui, address).orElse(getAddressAbbr(address)));
    }

    /**
     * Returns the alias of an address.
     *
     * @param gui
     * @param address
     * @return
     */
    public static Optional<String> getAddressAlias(SemuxGui gui, byte[] address) {
        WalletAccount account = gui.getModel().getAccount(address);
        if (account != null) {
            return account.getName();
        }

        // use address book for lookup
        return gui.getKernel().getWallet().getAddressAlias(address);
    }

    /**
     * Returns the name of the delegate that corresponds to the given address.
     *
     * @param address
     * @return
     */
    public static Optional<String> getAddressDelegateName(SemuxGui gui, byte[] address) {
        DelegateState ds = gui.getKernel().getBlockchain().getDelegateState();
        Delegate d = ds.getDelegateByAddress(address);

        return d == null ? Optional.empty() : Optional.of(d.getNameString());
    }

    /**
     * Returns the abbreviation of the given address.
     *
     * @param address
     * @return
     */
    public static String getAddressAbbr(byte[] address) {
        return Hex.PREF + Hex.encode(Arrays.copyOfRange(address, 0, 2)) + "..."
                + Hex.encode(Arrays.copyOfRange(address, address.length - 2, address.length));
    }

    public static JProgressBar createMetalProgressBar() {
        JProgressBar progressBar;

        try {
            LookAndFeel defaultLookAndFeel = UIManager.getLookAndFeel();
            UIManager.setLookAndFeel(MetalLookAndFeel.class.getCanonicalName());
            progressBar = new JProgressBar();
            UIManager.setLookAndFeel(defaultLookAndFeel);
        } catch (Exception e) {
            progressBar = new JProgressBar();
        }

        return progressBar;
    }
}
