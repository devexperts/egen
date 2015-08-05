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

/**
 * Created by HP, 18.06.2014 7:53.
 */

@AutoSerializable
public class ComplicatedClass implements ActionListener {


    public ComplicatedClass() {

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

    ComplicatedClass test3;

    @Compact()

    TreeMap<ComplicatedClass, HashMap<ArrayList<Double>, Enumerable>> map1;

    @Compact()

    ArrayList<LinkedList<int[]>> list1;

    @Compact()

    Object[] objarr = new Object[10];

    @Compact()
    ComplicatedClass[] complicatedClasses = new ComplicatedClass[10];

    @Compact()
    ArrayList<Object> objectList;

    @Compact()
    HashMap<Object, Runnable> objectToRunnableMap;

    @AsInt()
    String htya = "532";

    @Compact String s;


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

}
