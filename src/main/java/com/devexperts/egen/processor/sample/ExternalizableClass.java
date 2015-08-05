package com.devexperts.egen.processor.sample;

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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ExternalizableClass implements Externalizable {
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // EMPTY
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // EMPTY
    }
}
