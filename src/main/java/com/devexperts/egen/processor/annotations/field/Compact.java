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
 * Mark field with this annotation to make its serialized form more compact.<br>
 * For primitive types (including boxed) and strings appropriate method from dxlib's IOUtil will be used.<br>
 * For arrays, collections and maps: their content will be serialized without their own identity.
 * Field must be non-abstract: you can't write @Compact java.util.Map&lt;K, V&gt; m;<br>
 * For other @AutoSerializable classes @Compact is just equal to @Inline.<br>
 * Note that annotation is recursive: @Compact HashMap&lt;K, V&gt; causes applying @Compact to K and V.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Compact {
}
