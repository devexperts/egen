package com.devexperts.egen.processor.tools;

/*
 * #%L
 * EGEN - Externalizable implementation generator
 * %%
 * Copyright (C) 2014 - 2020 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

public enum OpCode {
    UNARY_POST_INC(54), // a++
    BINARY_OR(57), // a || b
    BINARY_AND(58), // a && b
    BITWISE_AND(61), // a & b
    NOT_EQUALS(63), // a != b
    LESSER(64), // a < b
    BITWISE_SHIFT_LEFT(68), // a << b
    ASSIGN_OR(76); // a |= b

    int value;

    OpCode(int value) {
        this.value = value;
    }
}
