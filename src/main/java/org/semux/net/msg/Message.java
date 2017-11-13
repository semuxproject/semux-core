/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.net.msg;

import org.semux.util.Bytes;

/**
 * Abstract message class for all messages on the Semux network
 * 
 */
public abstract class Message {
    /**
     * message code.
     */
    protected MessageCode code;

    /**
     * Response message class.
     */
    protected Class<?> responseMessageClass;

    /**
     * encoded data.
     */
    protected byte[] encoded;

    /**
     * Create a message instance.
     * 
     * @param code
     * @param responseMessageClass
     */
    public Message(MessageCode code, Class<?> responseMessageClass) {
        super();
        this.code = code;
        this.responseMessageClass = responseMessageClass;
        this.encoded = Bytes.EMPY_BYTES;
    }

    /**
     * Get the encoded byte array of this message
     * 
     * @return
     */
    public byte[] getEncoded() {
        return encoded;
    }

    /**
     * Get the message code
     * 
     * @return
     */
    public MessageCode getCode() {
        return code;
    }

    /**
     * Get the response message class of this message.
     * 
     * @return the response message, or null if this message requires no response.
     */
    public Class<?> getResponseMessageClass() {
        return responseMessageClass;
    }

    /**
     * Return the message name.
     */
    public String toString() {
        return getClass().getName();
    }
}
