package com.devexperts.egen.processor.tools;

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

import com.devexperts.egen.processor.AutoSerializableProcessor;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;

import com.sun.tools.javac.util.List;

import java.util.Collections;

import static com.sun.tools.javac.tree.JCTree.*;

public class GroupStatementFactory {
    java.util.List<JCTree.JCVariableDecl> fieldGroup;
    int groupOrdinal;
    TreeMaker maker;
    JavacElements utils;

    public GroupStatementFactory(java.util.List<JCTree.JCVariableDecl> fieldGroup, int groupOrdinal, TreeMaker maker, JavacElements utils) {
        this.fieldGroup = fieldGroup;
        this.groupOrdinal = groupOrdinal;
        this.maker = maker;
        this.utils = utils;
    }

    public JCStatement groupFlagsStatement() {
        return maker.If(groupDefaultCheckCond(fieldGroup), maker.Exec(flagAssignExpr(groupOrdinal)), null);
    }

    public JCStatement groupWriteStatement() {
        return maker.If(flagsCheckCond(groupOrdinal), maker.Block(0, writeStatements(fieldGroup)), null);
    }

    public JCStatement groupReadStatement() {
        return maker.If(flagsCheckCond(groupOrdinal), maker.Block(0, readStatements(fieldGroup)),
                maker.Block(0, defaultReadStatements(fieldGroup)));
    }

    private JCExpression ident(String complexIdent) {
        String[] parts = complexIdent.split("\\.");
        JCExpression expression = maker.Ident(utils.getName(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            expression = maker.Select(expression, utils.getName(parts[i]));
        }
        return expression;
    }

    private JCExpression flagsCheckCond(int groupOrdinal) {
        return maker.Binary(
                Tag.NE,
                maker.Parens(maker.Binary(
                        Tag.BITAND,
                        ident("flags"),
                        maker.Binary(JCTree.Tag.SL, maker.Literal(1L), maker.Literal(groupOrdinal))
                )),
                maker.Literal(0)
        );
    }

    private List<JCStatement> writeStatements(java.util.List<JCTree.JCVariableDecl> vars) {
        List<JCStatement> result = List.nil();
        for (JCVariableDecl var : vars) {
//            String typeIdentString = Character.toUpperCase(variableDecl.vartype.toString().charAt(0)) +
//                    variableDecl.vartype.toString().substring(1);
//            JCExpression expression = ident("out");
//            expression = maker.Select(expression, utils.getName("write" + typeIdentString));
//            expression = maker.Apply(List.<JCExpression>nil(), expression,
//                    List.of((JCExpression) ident(variableDecl.name.toString())));
            result = result.append(new StatementFactory(maker, utils, var).compactWriteStatement());
        }
        return result;
    }

    private List<JCStatement> defaultReadStatements(java.util.List<JCTree.JCVariableDecl> vars) {
        List<JCStatement> result = List.nil();
        for (JCVariableDecl var : vars) {
            JCExpression expression = varDefaultValue(var);
            expression = maker.Assign(ident("self." + var.name.toString()), expression);
            result = result.append(maker.Exec(expression));
        }
        return result;
    }

    private List<JCStatement> readStatements(java.util.List<JCTree.JCVariableDecl> vars) {
        List<JCStatement> result = List.nil();
        for (JCVariableDecl var : vars) {
//            String typeIdentString = Character.toUpperCase(variableDecl.vartype.toString().charAt(0)) +
//                    variableDecl.vartype.toString().substring(1);
//            JCExpression expression = ident("in");
//            expression = maker.Select(expression, utils.getName("read" + typeIdentString));
//            expression = maker.Apply(List.<JCExpression>nil(), expression, List.<JCExpression>nil());
//            expression = maker.Assign(ident(variableDecl.name.toString()), expression);
            result = result.append(new StatementFactory(maker, utils, var).compactReadStatement());
        }
        return result;
    }

    private JCExpression groupDefaultCheckCond(java.util.List<JCTree.JCVariableDecl> vars) {
        JCVariableDecl var = vars.get(0);
        int size = vars.size();
        String varName = var.name.toString();
        if (vars.size() == 1) {
            return maker.Binary(Tag.NE, ident("self." + varName), varDefaultValue(var));
        } else {
            return maker.Binary(Tag.OR, groupDefaultCheckCond(vars.subList(0, size - 1)),
                    groupDefaultCheckCond(Collections.singletonList(vars.get(size - 1))));
        }
    }

    private JCExpression flagAssignExpr(int groupOrdinal) {
        return maker.Assignop(Tag.BITOR_ASG, ident("flags"),
                maker.Binary(Tag.SL, maker.Literal(1L), maker.Literal(groupOrdinal)));
    }

    private JCExpression varDefaultValue(JCVariableDecl var) {
        String typeIdentString = Character.toUpperCase(var.vartype.toString().charAt(0)) + var.vartype.toString().substring(1);
        if ("Int".equals(typeIdentString)) {
            typeIdentString = "Integer";
        } else if ("Char".equals(typeIdentString)) {
            typeIdentString = "Character";
        }
        JCExpression resultExpr = maker.Ident(utils.getName(typeIdentString));
        resultExpr = maker.Select(resultExpr, utils.getName("valueOf"));

        String value = "0";
        JCAnnotation annotation = AutoSerializableProcessor.getEgenAnnotation(var);
        if (annotation == null)
            throw new IllegalStateException("EGEN annotation not found");

        for (JCExpression expression : annotation.args) {
            JCAssign assign = (JCAssign)expression;
            if ("value".equals(assign.lhs.toString())) {
                value = assign.rhs.toString().substring(1, assign.rhs.toString().length() - 1);
            }
        }

        return maker.Apply(List.<JCExpression>nil(), resultExpr, List.of((JCExpression)maker.Literal(value)));
    }
}
