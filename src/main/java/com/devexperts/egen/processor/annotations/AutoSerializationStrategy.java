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
 * It is meta-annotation.<br>
 * You can mark other annotation (e. g. @Annotation) with this annotation, and later mark fields with @Annotation.<br>
 * For their serialization the following code will be generated:<br>
 * com.devexperts.io.IOUtil.write&lt;targetStrategy&gt;(out, &lt;toTarget&gt;(field));<br>
 * &lt;fromTarget&gt; will be used thereafter during deserialization.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface AutoSerializationStrategy {
    String targetStrategy();
    String toTarget() default "";
    String fromTarget() default "";

}
