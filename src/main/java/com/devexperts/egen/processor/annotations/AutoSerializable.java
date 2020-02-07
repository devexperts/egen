package com.devexperts.egen.processor.annotations;

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
 * Serialization of class marked with this annotation will be optimized during compilation.
 * writeObject/readObject methods' implementation will be generated automatically.
 * It will contain lines for serialization/deserialization of all non-static and non-transient fields.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface AutoSerializable {

}
