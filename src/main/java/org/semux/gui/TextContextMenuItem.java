/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.TextAction;

import org.semux.message.GuiMessages;
import org.semux.util.exception.UnreachableException;

/**
 * This enum maintains mappings of text context menu.
 */
enum TextContextMenuItem {
    CUT, COPY, PASTE;

    public TextAction toAction() {
        switch (this) {
        case CUT:
            return new DefaultEditorKit.CutAction();
        case COPY:
            return new DefaultEditorKit.CopyAction();
        case PASTE:
            return new DefaultEditorKit.PasteAction();
        }
        throw new UnreachableException();
    }

    @Override
    public String toString() {
        switch (this) {
        case CUT:
            return GuiMessages.get("Cut");
        case COPY:
            return GuiMessages.get("Copy");
        case PASTE:
            return GuiMessages.get("Paste");
        }
        throw new UnreachableException();
    }
}