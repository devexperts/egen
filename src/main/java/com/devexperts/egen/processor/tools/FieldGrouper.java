package com.devexperts.egen.processor.tools;

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

import com.devexperts.egen.processor.AutoSerializableProcessor;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldGrouper {
    public static final String PRESENCE = "PresenceBit";

    private static final int ERROR = -2;
    private static final int SINGLETON_GROUP = -1;
    private static final String VALUE = "value";

    JCTree.JCClassDecl classDecl;

    public FieldGrouper(JCTree.JCClassDecl classDecl) {
        this.classDecl = classDecl;
    }

    public List<List<JCTree.JCVariableDecl>> getFieldGroups() {
        Map<Integer, List<JCTree.JCVariableDecl>> groupIdMap = new HashMap<>();

        for (JCTree tree : classDecl.defs) {
            if (tree instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl var = (JCTree.JCVariableDecl)tree;
                JCTree.JCAnnotation annotation = AutoSerializableProcessor.getEgenAnnotation(var);
                if (annotation != null && PRESENCE.equals(annotation.annotationType.toString())) {
                    if ((var.mods.flags & (Flags.TRANSIENT | Flags.STATIC)) != 0)
                        continue;

                    List<JCTree.JCExpression> arguments = annotation.getArguments();
                    int groupId = ERROR;
                    if (arguments.size() == 1 && arguments.get(0).toString().startsWith(VALUE)) {
                        groupId = SINGLETON_GROUP;
                    } else if (arguments.size() == 2 && arguments.get(0).toString().startsWith(VALUE)) {
                        JCTree.JCAssign assign = (JCTree.JCAssign)(arguments.get(1));
                        groupId = Integer.valueOf(assign.rhs.toString());
                    } else if (arguments.size() == 2 && arguments.get(1).toString().startsWith(VALUE)) {
                        JCTree.JCAssign assign = (JCTree.JCAssign)(arguments.get(0));
                        groupId = Integer.valueOf(assign.rhs.toString());
                    }
                    // TODO: Annotation args usage is hardcoded, needs to be refactored

                    if (groupId != ERROR && groupIdMap.get(groupId) == null)
                        groupIdMap.put(groupId, new ArrayList<JCTree.JCVariableDecl>());

                    groupIdMap.get(groupId).add(var);
                }
            }

        }

        List<List<JCTree.JCVariableDecl>> result = new ArrayList<>();

        for (Map.Entry<Integer, List<JCTree.JCVariableDecl>> e : groupIdMap.entrySet()) {
            if (e.getKey() == SINGLETON_GROUP) {
                for (JCTree.JCVariableDecl var : e.getValue()) {
                    List<JCTree.JCVariableDecl> varList = new ArrayList<>();
                    varList.add(var);
                    result.add(varList);
                }
            } else {
                result.add(e.getValue());
            }
        }

        return result;
    }
}
