package com.devexperts.egen.processor.annotations.field;

/*
 * #%L
 * EGEN - Externalizable implementation generator
 * %%
 * Copyright (C) 2014 - 2015 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * You can mark primitive type (including boxed) or string with this annotation to transmit one bit (presence bit)
 * if field contains its default value.<br>
 * Example: @PresenceBit(value = "NaN") double d; — serialization of d will cost one bit in case of d is NaN.<br>
 * Also, a whole group of fields may be transmitted in one bit:<br>
 * Example: @PresenceBit(value = "0", groupId = 1) int x; @PresenceBit(value = "0", groupId = 1) int y;<br>
 * Serialization of x and y will cost one bit in case of both x and y are zeros.<br>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface PresenceBit {
    String value();
    int groupId() default -1;
}
