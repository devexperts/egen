package com.devexperts.egen.processor.sample;

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

import com.devexperts.egen.processor.annotations.AutoSerializationStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: It's only for test. Should be removed from EGEN source folders
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
@AutoSerializationStrategy(targetStrategy="CompactInt", toTarget="Integer.parseInt", fromTarget="Integer.toString")
public @interface AsInt {
}
