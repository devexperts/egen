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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.tree.JCTree.*;

public class MethodBlockFactory {
    TreeMaker maker;
    JavacElements utils;
    JCClassDecl classDecl;

    public MethodBlockFactory(TreeMaker maker, JavacElements utils, JCClassDecl classDecl) {
        this.maker = maker;
        this.utils = utils;
        this.classDecl = classDecl;
    }

    public JCBlock writeContentsBlock() {
        List<JCStatement> statements = List.nil();

        FieldGrouper fieldGrouper = new FieldGrouper(classDecl);
        java.util.List<java.util.List<JCVariableDecl>> fieldGroups = fieldGrouper.getFieldGroups();

        if (!fieldGroups.isEmpty()) {
            JCExpression flagsInit = maker.Apply(List.<JCExpression>nil(), ident("prepareFlags"), List.of((JCExpression) ident("self")));

            JCStatement flagsDef = maker.VarDef(maker.Modifiers(0), utils.getName("flags"), maker.TypeIdent(TypeTag.LONG), flagsInit);
            statements = statements.append(flagsDef);

            statements = statements.append(new StatementFactory(maker, utils, (JCVariableDecl) flagsDef, true).compactWriteStatement());
        }


        for (int i = 0; i < fieldGroups.size(); i++) {
            GroupStatementFactory groupStatementFactory = new GroupStatementFactory(fieldGroups.get(i), i, maker, utils);
            statements = statements.append(groupStatementFactory.groupWriteStatement());
        }

        for (JCTree tree : classDecl.defs) {
            if (tree instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) tree;
                if (FieldGrouper.PRESENCE.equals(AutoSerializableProcessor.getEgenAnnotationType(var)))
                    continue;

                if ((var.mods.flags & (Flags.TRANSIENT | Flags.STATIC)) != 0)
                    continue;

                StatementFactory statementFactory = new StatementFactory(maker, utils, var);
                statements = statements.append(statementFactory.writeStatement());
            }
        }

        return maker.Block(0, statements);
    }

    public JCBlock readContentsBlock() {
        List<JCStatement> statements = List.nil();

        FieldGrouper fieldGrouper = new FieldGrouper(classDecl);
        java.util.List<java.util.List<JCVariableDecl>> fieldGroups = fieldGrouper.getFieldGroups();

        if (!fieldGroups.isEmpty()) {
            JCExpression flagsInit = makeDxlibIOUtilsSelect();
            flagsInit = maker.Select(flagsInit, utils.getName("readCompactLong"));
            flagsInit = maker.Apply(List.<JCExpression>nil(), flagsInit, List.of((JCExpression) ident("in")));

            JCStatement flagsDef = maker.VarDef(maker.Modifiers(0), utils.getName("flags"), maker.TypeIdent(TypeTag.LONG), flagsInit);
            statements = statements.append(flagsDef);
        }

        for (int i = 0; i < fieldGroups.size(); i++) {
            GroupStatementFactory groupStatementFactory = new GroupStatementFactory(fieldGroups.get(i), i, maker, utils);
            statements = statements.append(groupStatementFactory.groupReadStatement());
        }

        for (JCTree tree : classDecl.defs) {
            if (tree instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) tree;
                if (FieldGrouper.PRESENCE.equals(AutoSerializableProcessor.getEgenAnnotationType(var)))
                    continue;

                if ((var.mods.flags & (Flags.TRANSIENT | Flags.STATIC)) != 0)
                    continue;

                StatementFactory statementFactory = new StatementFactory(maker, utils, var);
                statements = statements.append(statementFactory.readStatement());
            }
        }

        return maker.Block(0, statements);
    }

    public JCBlock writeObjectBlock() {
        List<JCStatement> statements = List.nil();
        JCExpression expression = maker.Apply(List.<JCExpression>nil(), ident("writeContents"),
                List.of(ident("out"), ident("this")));
        statements = statements.append(maker.Exec(expression));
        return maker.Block(0, statements);
    }

    public JCBlock readObjectBlock() {
        List<JCStatement> statements = List.nil();
        JCExpression expression = maker.Apply(List.<JCExpression>nil(), ident("readContents"),
                List.of(ident("in"), ident("this")));
        statements = statements.append(maker.Exec(expression));
        return maker.Block(0, statements);
    }

    public JCBlock writeInlineBlock() {
        List<JCStatement> statements = List.nil();

        //JCExpression currentClassTypeToken = makeSelectExpr(classDecl.sym.type.toString() + ".class");
        JCExpression currentClassTypeToken = makeSelectExpr(classDecl.sym.type.toString() + ".class");

        JCExpression selfGetClass = maker.Apply(List.<JCExpression>nil(), makeSelectExpr("self.getClass"), List.<JCExpression>nil());
        JCExpression sameClassCond = maker.Binary(Tag.NE, currentClassTypeToken, selfGetClass);
        JCExpression classCheckCond = maker.Binary(Tag.AND, ident("checkClass"), sameClassCond);

        JCStatement throwException = maker.Throw(maker.NewClass(null, List.<JCExpression>nil(),
                makeSelectExpr("java.lang.UnsupportedOperationException"),
                List.of((JCExpression) maker.Literal("EGEN: Error: attempt of subclass inlining.")), null));

        JCStatement classCheck = maker.If(classCheckCond, throwException, null);
        statements = statements.append(classCheck);

        JCStatement writeContentsCall = maker.Exec(maker.Apply(List.<JCExpression>nil(),
                makeSelectExpr("writeContents"), List.of(ident("out"), ident("self"))));
        statements = statements.append(writeContentsCall);

        if (classDecl.extending != null) {
            String superTypeName = classDecl.extending.toString().split("<")[0];
            JCExpression superMethodName = makeSelectExpr(superTypeName + ".writeInline");
            JCExpression superInlineCall = maker.Apply(List.<JCExpression>nil(), superMethodName,
                    List.of(ident("out"), ident("self"), maker.Literal(false)));
            statements = statements.append(maker.Exec(superInlineCall));
        }

        return maker.Block(0, statements);
    }

    public JCBlock readInlineBlock() {
        List<JCStatement> statements = List.nil();

        JCStatement readContentsCall = maker.Exec(maker.Apply(List.<JCExpression>nil(),
                makeSelectExpr("readContents"), List.of(ident("in"), ident("self"))));
        statements = statements.append(readContentsCall);

        if (classDecl.extending != null) {
            String superTypeName = classDecl.extending.toString().split("<")[0];
            JCExpression superMethodName = makeSelectExpr(superTypeName + ".readInline");
            JCExpression superInlineCall = maker.Apply(List.<JCExpression>nil(), superMethodName,
                    List.of(ident("in"), (JCExpression)ident("self")));
            statements = statements.append(maker.Exec(superInlineCall));
        }

        return maker.Block(0, statements);
    }

    public JCBlock prepareFlagsBlock() {
        List<JCStatement> statements = List.nil();

        JCStatement flagsDef = maker.VarDef(maker.Modifiers(0), utils.getName("flags"), maker.TypeIdent(TypeTag.LONG), maker.Literal(0));
        statements = statements.append(flagsDef);

        FieldGrouper fieldGrouper = new FieldGrouper(classDecl);
        java.util.List<java.util.List<JCVariableDecl>> fieldGroups = fieldGrouper.getFieldGroups();
        for (int i = 0; i < fieldGroups.size(); i++) {
            GroupStatementFactory groupStatementFactory = new GroupStatementFactory(fieldGroups.get(i), i, maker, utils);
            statements = statements.append(groupStatementFactory.groupFlagsStatement());
        }

        statements = statements.append(maker.Return(ident("flags")));

        return maker.Block(0, statements);
    }

    private JCExpression makeDxlibIOUtilsSelect() {
        return makeSelectExpr("com.devexperts.io.IOUtil");
    }

    private JCExpression makeSelectExpr(String select) {
        String[] parts = select.split("\\.");
        JCExpression expression = ident(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            expression = maker.Select(expression, utils.getName(parts[i]));
        }
        return expression;
    }

    private JCIdent ident(String name) {
        return maker.Ident(utils.getName(name));
    }
}
