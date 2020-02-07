package com.devexperts.egen.processor.annotations.field;

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark integer/long field with this annotation to use delta-encoding during serialization.<br>
 * You can do @Delta("42") to transmit difference to 42 or @Delta("b") to transmit difference to integer/long field b.<br>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Delta {
    String value() default "";
}
