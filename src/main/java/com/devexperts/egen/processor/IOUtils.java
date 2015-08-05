package com.devexperts.egen.processor;

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

import java.io.*;

import static com.devexperts.io.IOUtil.readUTFString;
import static com.devexperts.io.IOUtil.writeUTFString;

public class IOUtils {
    public static void writeCompactInt(DataOutput out, int v) throws IOException {
        com.devexperts.io.IOUtil.writeCompactInt(out, v);
    }

    public static void writeCompactIntArray(DataOutput out, int[] v) throws IOException {
        if (v == null) {
            writeCompactInt(out, -1);
            return;
        }

        writeCompactInt(out, v.length);
        for (int aV : v) {
            writeCompactInt(out, aV);
        }
    }

    public static void writeDeltaIntArray(DataOutput out, int[] v) throws IOException {
        if (v == null) {
            writeCompactInt(out, -1);
            return;
        }

        writeCompactInt(out, v.length);
        for (int i = 0; i < v.length; i++) {
            if (i == 0) {
                out.writeInt(v[i]);
            } else {
                writeCompactInt(out, v[i] - v[i - 1]);
            }
        }
    }

    public static void writeCompactLong(DataOutput out, long v) throws IOException {
        com.devexperts.io.IOUtil.writeCompactLong(out, v);
    }

    public static void writeCompactLongArray(DataOutput out, long[] v) throws IOException {
        if (v == null) {
            writeCompactInt(out, -1);
            return;
        }

        writeCompactInt(out, v.length);
        for (long aV : v) {
            writeCompactLong(out, aV);
        }
    }

    public static void writeDeltaLongArray(DataOutput out, long[] v) throws IOException {
        if (v == null) {
            writeCompactInt(out, -1);
            return;
        }

        writeCompactInt(out, v.length);
        for (int i = 0; i < v.length; i++) {
            if (i == 0) {
                out.writeLong(v[i]);
            } else {
                writeCompactLong(out, v[i] - v[i - 1]);
            }
        }
    }

    public static int readCompactInt(DataInput in) throws IOException {
        return com.devexperts.io.IOUtil.readCompactInt(in);
    }

    public static int[] readCompactIntArray(DataInput in) throws IOException {
        int length = readCompactInt(in);
        if (length == -1)
            return null;

        int[] v = new int[length];
        for (int i = 0; i < v.length; i++) {
            v[i] = readCompactInt(in);
        }
        return v;
    }

    public static int[] readDeltaIntArray(DataInput in) throws IOException {
        int length = readCompactInt(in);
        if (length == -1)
            return null;

        int[] v = new int[length];
        for (int i = 0; i < v.length; i++) {
            if (i == 0) {
                v[i] = in.readInt();
            } else {
                v[i] = v[i - 1] + readCompactInt(in);
            }
        }
        return v;
    }

    public static long readCompactLong(DataInput in) throws IOException {
        return com.devexperts.io.IOUtil.readCompactLong(in);
    }

    public static long[] readCompactLongArray(DataInput in) throws IOException {
        int length = readCompactInt(in);
        if (length == -1)
            return null;

        long[] v = new long[length];
        for (int i = 0; i < v.length; i++) {
            v[i] = readCompactLong(in);
        }
        return v;
    }

    public static long[] readDeltaLongArray(DataInput in) throws IOException {
        int length = readCompactInt(in);
        if (length == -1)
            return null;

        long[] v = new long[length];
        for (int i = 0; i < v.length; i++) {
            if (i == 0) {
                v[i] = in.readLong();
            } else {
                v[i] = v[i - 1] + readCompactLong(in);
            }
        }
        return v;
    }

    public static void writeCompactString(DataOutput out, String s) throws IOException {
        writeUTFString(out, s);
    }

    public static String readCompactString(DataInput in) throws IOException {
        return readUTFString(in);
    }

//    public static void writeDecimalDouble(DataOutput out, double d) throws IOException {
//        writeCompactInt(out, compose(d));
//    }
//
//    public static double readDecimalDouble(DataInput in) throws IOException {
//        return toDouble(readCompactInt(in));
//    }
//
//    public static void writeDeltaDouble(DataOutput out, double d, double from) throws IOException {
//        writeCompactInt(out, compose(d) - compose(from));
//    }
//
//    public static double readDeltaDouble(DataInput in, double from) throws IOException {
//        return toDouble(readCompactInt(in) + compose(from));
//    }

    public static void writeDeltaInt(DataOutput out, int v, int from) throws IOException {
        writeCompactInt(out, v - from);
    }

    public static int readDeltaInt(DataInput in, int from) throws IOException {
        return readCompactInt(in) + from;
    }

    public static void writeDeltaLong(DataOutput out, long v, long from) throws IOException {
        writeCompactLong(out, v - from);
    }

    public static long readDeltaLong(DataInput in, long from) throws IOException {
        return readCompactLong(in) + from;
    }

//    public static void writeDeltaDoubleArray(DataOutput out, double[] v) throws IOException {
//        if (v == null) {
//            writeCompactInt(out, -1);
//            return;
//        }
//
//        writeCompactInt(out, v.length);
//        for (int i = 0; i < v.length; i++) {
//            if (i == 0) {
//                out.writeInt(compose(v[i]));
//            } else {
//                writeCompactInt(out, compose(v[i]) - compose(v[i - 1]));
//            }
//        }
//    }

//    public static double[] readDeltaDoubleArray(DataInput in) throws IOException {
//        int length = readCompactInt(in);
//        if (length == -1)
//            return null;
//
//        int[] v = new int[length];
//        double[] result = new double[length];
//        for (int i = 0; i < v.length; i++) {
//            if (i == 0) {
//                v[i] = in.readInt();
//            } else {
//                v[i] = v[i - 1] + readCompactInt(in);
//            }
//            result[i] = toDouble(v[i]);
//        }
//        return result;
//    }

//    public static void writeInlineObject(ObjectOutput out, Externalizable o) throws IOException {
//        out.writeByte(o == null ? -1 : 0);
//        if (o != null)
//            o.writeExternal(out);
//    }
}
