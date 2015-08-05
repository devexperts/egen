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

import com.devexperts.egen.processor.annotations.AutoSerializable;
import com.devexperts.egen.processor.annotations.field.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

@AutoSerializable()

public class ComplicatedClassResult implements ActionListener, java.io.Serializable {



    public ComplicatedClassResult() {

        super();

    }

    @Compact()

    int smallInt;

    @Compact()

    int[] smallInts;

    int day1;

    @Delta(value = "day1")

    int day2;

    @Delta(value = "4056")

    int day3;

    @Delta()

    int[] prices;

    LinkedBlockingDeque<Appendable> appendableList;

    @PresenceBit(value = "34.04")

    double price;

    @PresenceBit(value = "0")

    int hitCount;

    @PresenceBit(value = "7", groupId = 8)

    int maturity;

    @PresenceBit(value = "10.0", groupId = 8)

    double strikePrice;

    @PresenceBit(value = "5.5", groupId = 8)

    double timeInForce;

    @Ordinal()

    Enumerable a;

    ExternalizableClass test2;

    @Inline()

    ComplicatedClassResult test3;

    @Compact()

    TreeMap<ComplicatedClassResult, HashMap<ArrayList<Double>, Enumerable>> map1;

    @Compact()

    ArrayList<LinkedList<int[]>> list1;

    @Compact()

    Object[] objarr = new Object[10];

    @Compact()

    ComplicatedClassResult[] ComplicatedClassResultes = new ComplicatedClassResult[10];

    @Compact()

    ArrayList<Object> objectList;

    @Compact()

    HashMap<Object, Runnable> objectToRunnableMap;

    @AsInt()

    String htya = "532";

    @Compact()

    String s;



    @Override()

    public void actionPerformed(ActionEvent e) {

        System.out.println(hashCode());

    }



    public static void main(String[] args) throws IOException {

        Map<Integer, Double> m = new HashMap<>();

        System.out.println();

    }



    private void readExternall(ObjectInput in) throws IOException, ClassNotFoundException {

        {

            int objarrsize = com.devexperts.egen.processor.IOUtils.readCompactInt(in);

            if (objarrsize != -1) {

                objarr = new Object[objarrsize];

                for (int objarrindex = 0; objarrindex < objarrsize; objarrindex++) {

                    Object objarrelem = null;

                    objarrelem = (Object)in.readObject();

                    objarr[objarrindex] = objarrelem;

                }

            }

        }

    }



    public void writeContents(java.io.ObjectOutputStream out) throws java.io.IOException {

        long flags = prepareFlags();

        com.devexperts.io.IOUtil.writeCompactLong(out, flags);

        if ((flags & 1L << 0) != 0) {

            com.devexperts.io.IOUtil.writeCompactInt(out, maturity);

            out.writeDouble(strikePrice);

            out.writeDouble(timeInForce);

        }

        if ((flags & 1L << 1) != 0) {

            out.writeDouble(price);

        }

        if ((flags & 1L << 2) != 0) {

            com.devexperts.io.IOUtil.writeCompactInt(out, hitCount);

        }

        com.devexperts.io.IOUtil.writeCompactInt(out, smallInt);

        if (smallInts != null) {

            com.devexperts.io.IOUtil.writeCompactInt(out, smallInts.length);

            for (int smallIntselem : smallInts) {

                com.devexperts.io.IOUtil.writeCompactInt(out, smallIntselem);

            }

        } else {

            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        }

        out.writeInt(day1);

        com.devexperts.egen.processor.IOUtils.writeDeltaInt(out, day2, Integer.valueOf(day1));

        com.devexperts.egen.processor.IOUtils.writeDeltaInt(out, day3, Integer.valueOf("4056"));

        com.devexperts.egen.processor.IOUtils.writeDeltaIntArray(out, prices);

        out.writeObject(appendableList);

        if (a != null) com.devexperts.io.IOUtil.writeCompactInt(out, a.code()); else com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        out.writeObject(test2);

        if (test3 != null) {

            out.writeByte(0);

            test3.writeInline(out, this, true);

        } else out.writeByte(-1);

        if (map1 != null) {

            com.devexperts.io.IOUtil.writeCompactInt(out, map1.size());

            for (java.util.Map.Entry<ComplicatedClassResult, HashMap<ArrayList<Double>, Enumerable>> map1entry : map1.entrySet()) {

                ComplicatedClassResult map1key = map1entry.getKey();

                HashMap<ArrayList<Double>, Enumerable> map1value = map1entry.getValue();

                if (map1key != null) {

                    out.writeByte(0);

                    map1key.writeInline(out, this, true);

                } else out.writeByte(-1);

                if (map1value != null) {

                    com.devexperts.io.IOUtil.writeCompactInt(out, map1value.size());

                    for (java.util.Map.Entry<ArrayList<Double>, Enumerable> map1valueentry : map1value.entrySet()) {

                        ArrayList<Double> map1valuekey = map1valueentry.getKey();

                        Enumerable map1valuevalue = map1valueentry.getValue();

                        if (map1valuekey != null) {

                            com.devexperts.io.IOUtil.writeCompactInt(out, map1valuekey.size());

                            for (Double map1valuekeyelem : map1valuekey) {

                                out.writeObject(map1valuekeyelem);

                            }

                        } else {

                            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

                        }

                        out.writeObject(map1valuevalue);

                    }

                } else {

                    com.devexperts.io.IOUtil.writeCompactInt(out, -1);

                }

            }

        } else {

            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        }

        if (list1 != null) {

            com.devexperts.io.IOUtil.writeCompactInt(out, list1.size());

            for (LinkedList<int[]> list1elem : list1) {

                if (list1elem != null) {

                    com.devexperts.io.IOUtil.writeCompactInt(out, list1elem.size());

                    for (int[] list1elemelem : list1elem) {

                        if (list1elemelem != null) {

                            com.devexperts.io.IOUtil.writeCompactInt(out, list1elemelem.length);

                            for (int list1elemelemelem : list1elemelem) {

                                com.devexperts.io.IOUtil.writeCompactInt(out, list1elemelemelem);

                            }

                        } else {

                            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

                        }

                    }

                } else {

                    com.devexperts.io.IOUtil.writeCompactInt(out, -1);

                }

            }

        } else {

            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        }

        if (objarr != null) {

            com.devexperts.io.IOUtil.writeCompactInt(out, objarr.length);

            for (Object objarrelem : objarr) {

                out.writeObject(objarrelem);

            }

        } else {

            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        }

        if (ComplicatedClassResultes != null) {

            com.devexperts.io.IOUtil.writeCompactInt(out, ComplicatedClassResultes.length);

            for (ComplicatedClassResult ComplicatedClassResulteselem : ComplicatedClassResultes) {

                if (ComplicatedClassResulteselem != null) {

                    out.writeByte(0);

                    ComplicatedClassResulteselem.writeInline(out, this, true);

                } else out.writeByte(-1);

            }

        } else {

            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        }

        if (objectList != null) {

            com.devexperts.io.IOUtil.writeCompactInt(out, objectList.size());

            for (Object objectListelem : objectList) {

                out.writeObject(objectListelem);

            }

        } else {

            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        }

        if (objectToRunnableMap != null) {

            com.devexperts.io.IOUtil.writeCompactInt(out, objectToRunnableMap.size());

            for (java.util.Map.Entry<Object, Runnable> objectToRunnableMapentry : objectToRunnableMap.entrySet()) {

                Object objectToRunnableMapkey = objectToRunnableMapentry.getKey();

                Runnable objectToRunnableMapvalue = objectToRunnableMapentry.getValue();

                out.writeObject(objectToRunnableMapkey);

                out.writeObject(objectToRunnableMapvalue);

            }

        } else {

            com.devexperts.io.IOUtil.writeCompactInt(out, -1);

        }

        com.devexperts.io.IOUtil.writeCompactInt(out, Integer.parseInt(htya));

        com.devexperts.io.IOUtil.writeUTFString(out, s);

    }



    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {

        writeContents(out);

    }



    public static void writeInline(java.io.ObjectOutputStream out, ComplicatedClassResult self, boolean checkClass) throws java.io.IOException {

        if (checkClass && ComplicatedClassResult.class != self.getClass()) throw new java.lang.UnsupportedOperationException("EGEN: Error: attempt of subclass inlining.");

        self.writeContents(out);

    }



    public void readContents(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {

        long flags = com.devexperts.io.IOUtil.readCompactLong(in);

        if ((flags & 1L << 0) != 0) {

            maturity = com.devexperts.io.IOUtil.readCompactInt(in);

            strikePrice = in.readDouble();

            timeInForce = in.readDouble();

        } else {

            maturity = Integer.valueOf("7");

            strikePrice = Double.valueOf("10.0");

            timeInForce = Double.valueOf("5.5");

        }

        if ((flags & 1L << 1) != 0) {

            price = in.readDouble();

        } else {

            price = Double.valueOf("34.04");

        }

        if ((flags & 1L << 2) != 0) {

            hitCount = com.devexperts.io.IOUtil.readCompactInt(in);

        } else {

            hitCount = Integer.valueOf("0");

        }

        smallInt = com.devexperts.io.IOUtil.readCompactInt(in);

        {

            int smallIntssize = com.devexperts.io.IOUtil.readCompactInt(in);

            if (smallIntssize != -1) {

                smallInts = new int[smallIntssize];

                for (int smallIntsindex = 0; smallIntsindex < smallIntssize; smallIntsindex++) {

                    int smallIntselem;

                    smallIntselem = com.devexperts.io.IOUtil.readCompactInt(in);

                    smallInts[smallIntsindex] = smallIntselem;

                }

            }

        }

        day1 = in.readInt();

        day2 = com.devexperts.egen.processor.IOUtils.readDeltaInt(in, Integer.valueOf(day1));

        day3 = com.devexperts.egen.processor.IOUtils.readDeltaInt(in, Integer.valueOf("4056"));

        prices = com.devexperts.egen.processor.IOUtils.readDeltaIntArray(in);

        appendableList = (LinkedBlockingDeque<Appendable>)in.readObject();

        {

            int aord = com.devexperts.io.IOUtil.readCompactInt(in);

            if (aord != -1) a = Enumerable.findByCode(Enumerable.class, aord);

        }

        test2 = (ExternalizableClass)in.readObject();

        if (in.readByte() != -1) {

            test3 = new ComplicatedClassResult();

            test3.readInline(in, this);

        } else test3 = null;

        {

            int map1size = com.devexperts.io.IOUtil.readCompactInt(in);

            if (map1size != -1) {

                map1 = new TreeMap<>();

                for (int map1index = 0; map1index < map1size; map1index++) {

                    ComplicatedClassResult map1key = null;

                    if (in.readByte() != -1) {

                        map1key = new ComplicatedClassResult();

                        map1key.readInline(in, this);

                    } else map1key = null;

                    HashMap<ArrayList<Double>, Enumerable> map1value = null;

                    {

                        int map1valuesize = com.devexperts.io.IOUtil.readCompactInt(in);

                        if (map1valuesize != -1) {

                            map1value = new HashMap<>();

                            for (int map1valueindex = 0; map1valueindex < map1valuesize; map1valueindex++) {

                                ArrayList<Double> map1valuekey = null;

                                {

                                    int map1valuekeysize = com.devexperts.io.IOUtil.readCompactInt(in);

                                    if (map1valuekeysize != -1) {

                                        map1valuekey = new ArrayList<>();

                                        for (int map1valuekeyindex = 0; map1valuekeyindex < map1valuekeysize; map1valuekeyindex++) {

                                            Double map1valuekeyelem = null;

                                            map1valuekeyelem = (Double)in.readObject();

                                            map1valuekey.add(map1valuekeyelem);

                                        }

                                    }

                                }

                                Enumerable map1valuevalue = null;

                                map1valuevalue = (Enumerable)in.readObject();

                                map1value.put(map1valuekey, map1valuevalue);

                            }

                        }

                    }

                    map1.put(map1key, map1value);

                }

            }

        }

        {

            int list1size = com.devexperts.io.IOUtil.readCompactInt(in);

            if (list1size != -1) {

                list1 = new ArrayList<>();

                for (int list1index = 0; list1index < list1size; list1index++) {

                    LinkedList<int[]> list1elem = null;

                    {

                        int list1elemsize = com.devexperts.io.IOUtil.readCompactInt(in);

                        if (list1elemsize != -1) {

                            list1elem = new LinkedList<>();

                            for (int list1elemindex = 0; list1elemindex < list1elemsize; list1elemindex++) {

                                int[] list1elemelem = null;

                                {

                                    int list1elemelemsize = com.devexperts.io.IOUtil.readCompactInt(in);

                                    if (list1elemelemsize != -1) {

                                        list1elemelem = new int[list1elemelemsize];

                                        for (int list1elemelemindex = 0; list1elemelemindex < list1elemelemsize; list1elemelemindex++) {

                                            int list1elemelemelem;

                                            list1elemelemelem = com.devexperts.io.IOUtil.readCompactInt(in);

                                            list1elemelem[list1elemelemindex] = list1elemelemelem;

                                        }

                                    }

                                }

                                list1elem.add(list1elemelem);

                            }

                        }

                    }

                    list1.add(list1elem);

                }

            }

        }

        {

            int objarrsize = com.devexperts.io.IOUtil.readCompactInt(in);

            if (objarrsize != -1) {

                objarr = new Object[objarrsize];

                for (int objarrindex = 0; objarrindex < objarrsize; objarrindex++) {

                    Object objarrelem;

                    objarrelem = (Object)in.readObject();

                    objarr[objarrindex] = objarrelem;

                }

            }

        }

        {

            int ComplicatedClassResultessize = com.devexperts.io.IOUtil.readCompactInt(in);

            if (ComplicatedClassResultessize != -1) {

                ComplicatedClassResultes = new ComplicatedClassResult[ComplicatedClassResultessize];

                for (int ComplicatedClassResultesindex = 0; ComplicatedClassResultesindex < ComplicatedClassResultessize; ComplicatedClassResultesindex++) {

                    ComplicatedClassResult ComplicatedClassResulteselem;

                    if (in.readByte() != -1) {

                        ComplicatedClassResulteselem = new ComplicatedClassResult();

                        ComplicatedClassResulteselem.readInline(in, this);

                    } else ComplicatedClassResulteselem = null;

                    ComplicatedClassResultes[ComplicatedClassResultesindex] = ComplicatedClassResulteselem;

                }

            }

        }

        {

            int objectListsize = com.devexperts.io.IOUtil.readCompactInt(in);

            if (objectListsize != -1) {

                objectList = new ArrayList<>();

                for (int objectListindex = 0; objectListindex < objectListsize; objectListindex++) {

                    Object objectListelem = null;

                    objectListelem = (Object)in.readObject();

                    objectList.add(objectListelem);

                }

            }

        }

        {

            int objectToRunnableMapsize = com.devexperts.io.IOUtil.readCompactInt(in);

            if (objectToRunnableMapsize != -1) {

                objectToRunnableMap = new HashMap<>();

                for (int objectToRunnableMapindex = 0; objectToRunnableMapindex < objectToRunnableMapsize; objectToRunnableMapindex++) {

                    Object objectToRunnableMapkey = null;

                    objectToRunnableMapkey = (Object)in.readObject();

                    Runnable objectToRunnableMapvalue = null;

                    objectToRunnableMapvalue = (Runnable)in.readObject();

                    objectToRunnableMap.put(objectToRunnableMapkey, objectToRunnableMapvalue);

                }

            }

        }

        htya = Integer.toString(com.devexperts.io.IOUtil.readCompactInt(in));

        s = com.devexperts.io.IOUtil.readUTFString(in);

    }



    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {

        readContents(in);

    }



    public static void readInline(java.io.ObjectInputStream in, ComplicatedClassResult self) throws java.io.IOException, java.lang.ClassNotFoundException {

        self.readContents(in);

    }



    private long prepareFlags() {

        long flags = 0;

        if (maturity != Integer.valueOf("7") || strikePrice != Double.valueOf("10.0") || timeInForce != Double.valueOf("5.5")) flags |= 1L << 0;

        if (price != Double.valueOf("34.04")) flags |= 1L << 1;

        if (hitCount != Integer.valueOf("0")) flags |= 1L << 2;

        return flags;

    }

}
