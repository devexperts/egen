package com.devexperts.egen.processor;

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

/**
 * This class determines whether @Inline and @Ordinal annotations apply to contents of @Compact container.
 */
public class CompactConfiguration {
    private static boolean recursiveInlineEnabled = true;
    private static boolean recursiveOrdinalEnabled = true;
    // TODO: Configuring from file or JVM properties

    private CompactConfiguration() {}

    public static boolean isRecursiveInlineEnabled() {
        return recursiveInlineEnabled;
    }

    public static void setRecursiveInlineEnabled(boolean recursiveInlineEnabled) {
        CompactConfiguration.recursiveInlineEnabled = recursiveInlineEnabled;
    }

    public static boolean isRecursiveOrdinalEnabled() {
        return recursiveOrdinalEnabled;
    }

    public static void setRecursiveOrdinalEnabled(boolean recursiveOrdinalEnabled) {
        CompactConfiguration.recursiveOrdinalEnabled = recursiveOrdinalEnabled;
    }
}
