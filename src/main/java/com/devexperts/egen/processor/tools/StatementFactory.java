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
import com.devexperts.egen.processor.CompactConfiguration;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;

import static com.sun.tools.javac.tree.JCTree.*;

public class StatementFactory {
    private static final int NULL_LITERAL = 17;
    
    TreeMaker maker;
    JavacElements utils;
    JCVariableDecl var;

    public StatementFactory(TreeMaker maker, JavacElements utils, JCVariableDecl var) {
        this.maker = maker;
        this.utils = utils;
        this.var = var;
    }

    public JCStatement writeStatement() {
        String annotationType = "";
        if (!var.mods.annotations.isEmpty()) {
            annotationType = var.mods.annotations.get(0).annotationType.toString();
        }

        switch (annotationType) {
            case "Compact":
                return compactWriteStatement();
            case "Delta":
                return deltaWriteStatement();
            case "Inline":
                return inlineWriteStatement();
            case "Ordinal":
                return ordinalWriteStatement();
            default:
                SerializationStrategyRecord strategyRecord = SerializationStrategyRecord.getByVariable(var);
                if (strategyRecord == null) {
                    return commonWriteStatement();
                } else {
                    return strategyWriteStatement(strategyRecord);
                }
        }
    }

    public JCStatement readStatement() {
        String annotationType = "";
        if (!var.mods.annotations.isEmpty()) {
            annotationType = var.mods.annotations.get(0).annotationType.toString();
        }
        switch (annotationType) {
            case "Compact":
                return compactReadStatement();
            case "Delta":
                return deltaReadStatement();
            case "Inline":
                return inlineReadStatement();
            case "Ordinal":
                return ordinalReadStatement();
            default:
                SerializationStrategyRecord strategyRecord = SerializationStrategyRecord.getByVariable(var);
                if (strategyRecord == null) {
                    return commonReadStatement();
                } else {
                    return strategyReadStatement(strategyRecord);
                }
        }
    }

    public JCStatement inlineWriteStatement() {
        JCExpression expression = ident(var.name.toString());

        JCExpression condition = maker.Binary(OpCode.NOT_EQUALS.value, expression, maker.Literal(NULL_LITERAL, null));

        JCExpression writeExpression = maker.Select(expression, utils.getName("writeInline"));
        writeExpression = maker.Apply(List.<JCExpression>nil(), writeExpression,
                List.of(ident("out"), ident(var.name.toString()), maker.Literal(true)));

        JCExpression writeNullByte = maker.Select(ident("out"), utils.getName("writeByte"));
        writeNullByte = maker.Apply(List.<JCExpression>nil(), writeNullByte, List.of((JCExpression) maker.Literal(-1)));

        JCExpression writeNotNullByte = maker.Select(ident("out"), utils.getName("writeByte"));
        writeNotNullByte = maker.Apply(List.<JCExpression>nil(), writeNotNullByte, List.of((JCExpression) maker.Literal(0)));

        return maker.If(condition,
                maker.Block(0, List.of((JCStatement) maker.Exec(writeNotNullByte), maker.Exec(writeExpression))),
                maker.Exec(writeNullByte));
    }

    public JCStatement inlineReadStatement() {
        JCExpression expression = maker.Select(ident("in"), utils.getName("readByte"));
        expression = maker.Apply(List.<JCExpression>nil(), expression, List.<JCExpression>nil());

        JCExpression condition = maker.Binary(OpCode.NOT_EQUALS.value, expression, maker.Literal(-1));

        JCExpression expression1 = maker.Assign(ident(var.name.toString()),
                maker.NewClass(null, List.<JCExpression>nil(), ident(var.vartype.toString()), List.<JCExpression>nil(), null));

        JCExpression expression2 = ident(var.name.toString());
        expression2 = maker.Select(expression2, utils.getName("readInline"));
        expression2 = maker.Apply(List.<JCExpression>nil(), expression2,
                List.of((JCExpression) ident("in"), ident(var.name.toString())));

        JCBlock block = maker.Block(0, List.of((JCStatement) maker.Exec(expression1), maker.Exec(expression2)));

        JCExpression elseExpr = maker.Assign(ident(var.name.toString()), maker.Literal(NULL_LITERAL, null));
        return maker.If(condition, block, maker.Exec(elseExpr));
    }

    public JCStatement strategyWriteStatement(SerializationStrategyRecord strategyRecord) {
        JCExpression expression = makeDxlibIOUtilsSelect();
        expression = maker.Select(expression, utils.getName("write" + strategyRecord.targetStrategy));

        String[] toTargetSelects = strategyRecord.toTarget.split("\\.");
        JCExpression toTarget = ident(toTargetSelects[0]);
        for (int i = 1; i < toTargetSelects.length; i++) {
            toTarget = maker.Select(toTarget, utils.getName(toTargetSelects[i]));
        }
        JCExpression toTargetApply = maker.Apply(List.<JCExpression>nil(), toTarget, List.of((JCExpression)ident(var.name.toString())));
        expression = maker.Apply(List.<JCExpression>nil(), expression,
                List.of(ident("out"), toTargetApply));
        return maker.Exec(expression);
    }

    public JCStatement strategyReadStatement(SerializationStrategyRecord strategyRecord) {
        JCExpression expression = makeDxlibIOUtilsSelect();
        expression = maker.Select(expression, utils.getName("read" + strategyRecord.targetStrategy));

        String[] fromTargetSelects = strategyRecord.fromTarget.split("\\.");
        JCExpression fromTarget = ident(fromTargetSelects[0]);
        for (int i = 1; i < fromTargetSelects.length; i++) {
            fromTarget = maker.Select(fromTarget, utils.getName(fromTargetSelects[i]));
        }
        expression = maker.Apply(List.<JCExpression>nil(), expression, List.of((JCExpression) ident("in")));
        expression = maker.Apply(List.<JCExpression>nil(), fromTarget, List.of(expression));
        return maker.Exec(maker.Assign(ident(var.name.toString()), expression));
    }

    public JCStatement deltaWriteStatement() {
        String typeName = var.vartype.toString();
        switch (typeName) {
            case "int[]":
            case "long[]": {
                String capitalizedElementName = Character.toUpperCase(typeName.charAt(0)) +
                        typeName.substring(1, typeName.length() - 2);
                JCExpression expression = makeEgenIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("writeDelta" + capitalizedElementName + "Array"));
                expression = maker.Apply(List.<JCExpression>nil(), expression,
                        List.of(ident("out"), (JCExpression) ident(var.name.toString())));
                return maker.Exec(expression);
            }
            case "int":
            case "long": {
                String capitalizedElementName = Character.toUpperCase(typeName.charAt(0)) +
                        typeName.substring(1, typeName.length());
                JCExpression expression = makeEgenIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("writeDelta" + capitalizedElementName));
                expression = maker.Apply(List.<JCExpression>nil(), expression,
                        List.of(ident("out"), ident(var.name.toString()), varDeltaFrom(var)));
                return maker.Exec(expression);
            }
            default:
                return commonWriteStatement();
        }
    }

    public JCStatement deltaReadStatement() {
        String typeName = var.vartype.toString();
        switch (typeName) {
            case "int[]":
            case "long[]": {
                String capitalizedElementName = Character.toUpperCase(typeName.charAt(0)) +
                        typeName.substring(1, typeName.length() - 2);
                JCExpression expression = makeEgenIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("readDelta" + capitalizedElementName + "Array"));
                expression = maker.Apply(List.<JCExpression>nil(), expression, List.of((JCExpression) ident("in")));
                return maker.Exec(maker.Assign(ident(var.name.toString()), expression));
            }
            case "int":
            case "long": {
                String capitalizedElementName = Character.toUpperCase(typeName.charAt(0)) +
                        typeName.substring(1, typeName.length());
                JCExpression expression = makeEgenIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("readDelta" + capitalizedElementName));
                expression = maker.Apply(List.<JCExpression>nil(), expression, List.of(ident("in"), varDeltaFrom(var)));
                return maker.Exec(maker.Assign(ident(var.name.toString()), expression));
            }
            default:
                return commonReadStatement();
        }
    }

    public JCStatement compactWriteStatement() {
        String typeName = var.vartype.toString();
        switch (typeName) {
            case "int":
            case "long":
            case "Integer":
            case "Long": {
                String capitalizedTypeName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
                JCExpression expression = makeDxlibIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("writeCompact" + capitalizedTypeName));
                expression = maker.Apply(List.<JCExpression>nil(), expression,
                        List.of(ident("out"), (JCExpression) ident(var.name.toString())));
                return maker.Exec(expression);
            }
            case "String": {
                JCExpression expression = makeDxlibIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("writeUTFString"));
                expression = maker.Apply(List.<JCExpression>nil(), expression,
                        List.of(ident("out"), (JCExpression) ident(var.name.toString())));
                return maker.Exec(expression);
            }
            default:
                if (CompactConfiguration.isRecursiveInlineEnabled() && isInlineApplicable(var))
                    return inlineWriteStatement();

                if (CompactConfiguration.isRecursiveOrdinalEnabled() && isOrdinalApplicable(var))
                    return ordinalWriteStatement();

                if (isCollectionApplicable(var))
                    return collectionWriteStatement();

                if (isArrayApplicable(var))
                    return arrayWriteStatement();

                if (isMapApplicable(var))
                    return mapWriteStatement();

                return commonWriteStatement();
        }
    }

    public JCStatement compactReadStatement() {
        String typeName = var.vartype.toString();
        switch (typeName) {
            case "int":
            case "long":
            case "Integer":
            case "Long": {
                String capitalizedTypeName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
                JCExpression expression = makeDxlibIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("readCompact" + capitalizedTypeName));
                expression = maker.Apply(List.<JCExpression>nil(), expression, List.of((JCExpression) ident("in")));
                return maker.Exec(maker.Assign(ident(var.name.toString()), expression));
            }
            case "String": {
                JCExpression expression = makeDxlibIOUtilsSelect();
                expression = maker.Select(expression, utils.getName("readUTFString"));
                expression = maker.Apply(List.<JCExpression>nil(), expression, List.of((JCExpression) ident("in")));
                return maker.Exec(maker.Assign(ident(var.name.toString()), expression));
            }
            default:
                if (CompactConfiguration.isRecursiveInlineEnabled() && isInlineApplicable(var))
                    return inlineReadStatement();

                if (CompactConfiguration.isRecursiveOrdinalEnabled() && isOrdinalApplicable(var))
                    return ordinalReadStatement();

                if (isCollectionApplicable(var))
                    return collectionReadStatement();

                if (isArrayApplicable(var))
                    return arrayReadStatement();

                if (isMapApplicable(var))
                    return mapReadStatement();

                return commonReadStatement();
        }
    }

    public JCStatement ordinalWriteStatement() {
        JCExpression notNullExpr = makeDxlibIOUtilsSelect();
        notNullExpr = maker.Select(notNullExpr, utils.getName("writeCompactInt"));
        JCExpression select = maker.Select(ident(var.name.toString()), utils.getName("code"));
        notNullExpr = maker.Apply(List.<JCExpression>nil(), notNullExpr,
                List.of(ident("out"), maker.Apply(List.<JCExpression>nil(), select, List.<JCExpression>nil())));

        JCExpression nullExpr = makeDxlibIOUtilsSelect();
        nullExpr = maker.Select(nullExpr, utils.getName("writeCompactInt"));
        nullExpr = maker.Apply(List.<JCExpression>nil(), nullExpr, List.of(ident("out"), maker.Literal(-1)));

        JCExpression cond = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString()), maker.Literal(NULL_LITERAL, null));
        return maker.If(cond, maker.Exec(notNullExpr), maker.Exec(nullExpr));
    }

    public JCStatement ordinalReadStatement() {
        JCExpression classExpr = ident(var.vartype.toString());
        classExpr = maker.Select(classExpr, utils.getName("class"));

        JCExpression readIntExpr = makeDxlibIOUtilsSelect();
        readIntExpr = maker.Select(readIntExpr, utils.getName("readCompactInt"));
        readIntExpr = maker.Apply(List.<JCExpression>nil(), readIntExpr, List.of((JCExpression) ident("in")));
        JCStatement intVarDef = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "ord"), maker.TypeIdent(TypeTags.INT), readIntExpr);

        JCExpression expression = ident(var.vartype.toString());
        expression = maker.Select(expression, utils.getName("findByCode"));
        expression = maker.Apply(List.<JCExpression>nil(), expression, List.of(classExpr, ident(var.name.toString() + "ord")));

        JCBinary cond = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString() + "ord"), maker.Literal(-1));
        JCIf jcIf = maker.If(cond, maker.Exec(maker.Assign(ident(var.name.toString()), expression)), null);

        return maker.Block(0, List.of(intVarDef, jcIf));
    }

    public JCStatement commonWriteStatement() {
        String typeName = var.vartype.toString();
        if (isPrimitive(typeName)) {
            String capitalizedTypeName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
            JCExpression expression = ident("out");
            expression = maker.Select(expression, utils.getName("write" + capitalizedTypeName));
            expression = maker.Apply(List.<JCExpression>nil(), expression,
                    List.of((JCExpression) ident(var.name.toString())));
            return maker.Exec(expression);
        } else {
            JCExpression expression = ident("out");
            expression = maker.Select(expression, utils.getName("writeObject"));
            expression = maker.Apply(List.<JCExpression>nil(), expression,
                    List.of((JCExpression) ident(var.name.toString())));
            return maker.Exec(expression);
        }
    }

    public JCStatement commonReadStatement() {
        String typeName = var.vartype.toString();
        if (isPrimitive(typeName)) {
            String capitalizedTypeName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
            JCExpression expression = ident("in");
            expression = maker.Select(expression, utils.getName("read" + capitalizedTypeName));
            expression = maker.Apply(List.<JCExpression>nil(), expression, List.<JCExpression>nil());
            return maker.Exec(maker.Assign(ident(var.name.toString()), expression));
        } else {
            JCExpression expression = ident("in");
            expression = maker.Select(expression, utils.getName("readObject"));
            expression = maker.Apply(List.<JCExpression>nil(), expression, List.<JCExpression>nil());
            return maker.Exec(maker.Assign(ident(var.name.toString()), maker.TypeCast(var.vartype, expression)));
        }
    }

    public JCStatement collectionWriteStatement() {
        JCExpression expression1 = makeDxlibIOUtilsSelect();
        expression1 = maker.Select(expression1, utils.getName("writeCompactInt"));
        JCExpression select = maker.Select(ident(var.name.toString()), utils.getName("size"));
        expression1 = maker.Apply(List.<JCExpression>nil(), expression1,
                List.of(ident("out"), maker.Apply(List.<JCExpression>nil(), select, List.<JCExpression>nil())));
        JCStatement statement1 = maker.Exec(expression1);

        JCVariableDecl forEachLoopVar = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "elem"),
                ((JCTypeApply) var.vartype).arguments.get(0), null);
        JCStatement statement2 = maker.ForeachLoop(forEachLoopVar, ident(var.name.toString()),
                maker.Block(0, List.of(new StatementFactory(maker, utils, forEachLoopVar).compactWriteStatement())));

        JCExpression writeMinusOne = makeDxlibIOUtilsSelect();
        writeMinusOne = maker.Select(writeMinusOne, utils.getName("writeCompactInt"));
        writeMinusOne = maker.Apply(List.<JCExpression>nil(), writeMinusOne,
                List.of(ident("out"), maker.Literal(-1)));
        JCBinary binary = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString()), maker.Literal(NULL_LITERAL, null));
        return maker.If(binary, maker.Block(0, List.of(statement1, statement2)), maker.Block(0, List.of((JCStatement) maker.Exec(writeMinusOne))));
    }

    /**
     * TODO: get rid of copypaste: merge with collectionWriteStatement()
     */
    public JCStatement arrayWriteStatement() {
        JCExpression expression1 = makeDxlibIOUtilsSelect();
        expression1 = maker.Select(expression1, utils.getName("writeCompactInt"));
        JCExpression select = maker.Select(ident(var.name.toString()), utils.getName("length"));
        expression1 = maker.Apply(List.<JCExpression>nil(), expression1, List.of(ident("out"), select));
        JCStatement statement1 = maker.Exec(expression1);

        JCVariableDecl forEachLoopVar = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "elem"),
                ((JCArrayTypeTree)var.vartype).elemtype, null);
        JCStatement statement2 = maker.ForeachLoop(forEachLoopVar, ident(var.name.toString()),
                maker.Block(0, List.of(new StatementFactory(maker, utils, forEachLoopVar).compactWriteStatement())));

        JCExpression writeMinusOne = makeDxlibIOUtilsSelect();
        writeMinusOne = maker.Select(writeMinusOne, utils.getName("writeCompactInt"));
        writeMinusOne = maker.Apply(List.<JCExpression>nil(), writeMinusOne,
                List.of(ident("out"), maker.Literal(-1)));
        JCBinary binary = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString()), maker.Literal(NULL_LITERAL, null));
        return maker.If(binary, maker.Block(0, List.of(statement1, statement2)), maker.Block(0, List.of((JCStatement) maker.Exec(writeMinusOne))));
    }

    public JCStatement mapWriteStatement() {
        JCExpression expression1 = makeDxlibIOUtilsSelect();
        expression1 = maker.Select(expression1, utils.getName("writeCompactInt"));
        JCExpression select = maker.Select(ident(var.name.toString()), utils.getName("size"));
        expression1 = maker.Apply(List.<JCExpression>nil(), expression1,
                List.of(ident("out"), maker.Apply(List.<JCExpression>nil(), select, List.<JCExpression>nil())));
        JCStatement statement1 = maker.Exec(expression1);

        JCExpression mapEntryType = ident("java");
        mapEntryType = maker.Select(mapEntryType, utils.getName("util"));
        mapEntryType = maker.Select(mapEntryType, utils.getName("Map"));
        mapEntryType = maker.Select(mapEntryType, utils.getName("Entry"));
        mapEntryType = maker.TypeApply(mapEntryType, ((JCTypeApply) var.vartype).arguments);
        JCVariableDecl forEachLoopVar = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "entry"), mapEntryType, null);

        JCExpression apply21 = maker.Select(ident(forEachLoopVar.name.toString()), utils.getName("getKey"));
        apply21 = maker.Apply(List.<JCExpression>nil(), apply21, List.<JCExpression>nil());
        JCVariableDecl statement21 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "key"), ((JCTypeApply) var.vartype).arguments.get(0), apply21);

        JCExpression apply22 = maker.Select(ident(forEachLoopVar.name.toString()), utils.getName("getValue"));
        apply22 = maker.Apply(List.<JCExpression>nil(), apply22, List.<JCExpression>nil());
        JCVariableDecl statement22 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "value"), ((JCTypeApply) var.vartype).arguments.get(1), apply22);

        JCStatement write23 = new StatementFactory(maker, utils, statement21).compactWriteStatement();

        JCStatement write24 = new StatementFactory(maker, utils, statement22).compactWriteStatement();

        JCExpression entrySetExpr = maker.Apply(List.<JCExpression>nil(),
                maker.Select(ident(var.name.toString()), utils.getName("entrySet")), List.<JCExpression>nil());
        JCStatement statement2 = maker.ForeachLoop(forEachLoopVar, entrySetExpr,
                maker.Block(0, List.of(statement21, statement22, write23, write24)));

        JCExpression writeMinusOne = makeDxlibIOUtilsSelect();
        writeMinusOne = maker.Select(writeMinusOne, utils.getName("writeCompactInt"));
        writeMinusOne = maker.Apply(List.<JCExpression>nil(), writeMinusOne,
                List.of(ident("out"), maker.Literal(-1)));
        JCBinary binary = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString()), maker.Literal(NULL_LITERAL, null));
        return maker.If(binary, maker.Block(0, List.of(statement1, statement2)), maker.Block(0, List.of((JCStatement) maker.Exec(writeMinusOne))));
    }

    public JCStatement collectionReadStatement() {
        JCExpression expression1 = makeDxlibIOUtilsSelect();
        expression1 = maker.Select(expression1, utils.getName("readCompactInt"));
        expression1 = maker.Apply(List.<JCExpression>nil(), expression1, List.of((JCExpression) ident("in")));
        JCVariableDecl statement1 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "size"), maker.TypeIdent(TypeTags.INT), expression1);

        JCTypeApply typeApply = (JCTypeApply) (var.vartype);
        JCExpression expression2 = maker.Assign(ident(var.name.toString()),
                maker.NewClass(null, List.<JCExpression>nil(), maker.TypeApply(typeApply.clazz, List.<JCExpression>nil()), List.<JCExpression>nil(), null));
        JCStatement statement2 = maker.Exec(expression2);

        JCVariableDecl statement31 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "elem"), typeApply.arguments.get(0), maker.Literal(NULL_LITERAL, null));

        JCStatement statement32 = new StatementFactory(maker, utils, statement31).compactReadStatement();

        JCExpression select33 = maker.Select(ident(var.name.toString()), utils.getName("add"));
        JCStatement statement33 = maker.Exec(maker.Apply(List.<JCExpression>nil(), select33,
                List.of((JCExpression) ident(statement31.name.toString()))));

        JCStatement init = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "index"), maker.TypeIdent(TypeTags.INT), maker.Literal(0));
        JCExpression cond = maker.Binary(OpCode.LESSER.value, ident(var.name.toString() + "index"), ident(var.name.toString() + "size"));
        JCExpressionStatement step = maker.Exec(maker.Unary(OpCode.UNARY_POST_INC.value, ident(var.name.toString() + "index")));
        JCStatement statement3 = maker.ForLoop(List.of(init), cond, List.of(step), maker.Block(0, List.of(statement31, statement32, statement33)));

        JCBinary binary = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString() + "size"), maker.Literal(-1));
        return maker.Block(0, List.of(statement1, maker.If(binary, maker.Block(0, List.of(statement2, statement3)), null)));
    }

    /**
     * TODO: get rid of copypaste: merge with collectionReadStatement()
     */
    public JCStatement arrayReadStatement() {
        JCExpression expression1 = makeDxlibIOUtilsSelect();
        expression1 = maker.Select(expression1, utils.getName("readCompactInt"));
        expression1 = maker.Apply(List.<JCExpression>nil(), expression1, List.of((JCExpression) ident("in")));
        JCVariableDecl statement1 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "size"), maker.TypeIdent(TypeTags.INT), expression1);

        JCArrayTypeTree arrayTypeTree = (JCArrayTypeTree) (var.vartype);
        JCExpression expression2 = maker.Assign(ident(var.name.toString()),
                maker.NewArray(arrayTypeTree.elemtype, List.of((JCExpression) ident(var.name.toString() + "size")), null));
        JCStatement statement2 = maker.Exec(expression2);

        JCVariableDecl statement31 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "elem"), arrayTypeTree.elemtype, null);

        JCStatement statement32 = new StatementFactory(maker, utils, statement31).compactReadStatement();

        JCStatement statement33 = maker.Exec(maker.Assign(maker.Indexed(ident(var.name.toString()),
                ident(var.name.toString() + "index")), ident(var.name.toString() + "elem")));

        JCStatement init = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "index"), maker.TypeIdent(TypeTags.INT), maker.Literal(0));
        JCExpression cond = maker.Binary(OpCode.LESSER.value, ident(var.name.toString() + "index"), ident(var.name.toString() + "size"));
        JCExpressionStatement step = maker.Exec(maker.Unary(OpCode.UNARY_POST_INC.value, ident(var.name.toString() + "index")));
        JCStatement statement3 = maker.ForLoop(List.of(init), cond, List.of(step), maker.Block(0, List.of(statement31, statement32, statement33)));

        JCBinary binary = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString() + "size"), maker.Literal(-1));
        return maker.Block(0, List.of(statement1, maker.If(binary, maker.Block(0, List.of(statement2, statement3)), null)));
    }

    public JCStatement mapReadStatement() {
        JCExpression expression1 = makeDxlibIOUtilsSelect();
        expression1 = maker.Select(expression1, utils.getName("readCompactInt"));
        expression1 = maker.Apply(List.<JCExpression>nil(), expression1, List.of((JCExpression) ident("in")));
        JCVariableDecl statement1 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "size"), maker.TypeIdent(TypeTags.INT), expression1);

        JCTypeApply typeApply = (JCTypeApply) (var.vartype);
        JCExpression expression2 = maker.Assign(ident(var.name.toString()),
                maker.NewClass(null, List.<JCExpression>nil(), maker.TypeApply(typeApply.clazz, List.<JCExpression>nil()), List.<JCExpression>nil(), null));
        JCStatement statement2 = maker.Exec(expression2);

        JCVariableDecl statement31 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "key"), typeApply.arguments.get(0), maker.Literal(NULL_LITERAL, null));

        JCStatement statement32 = new StatementFactory(maker, utils, statement31).compactReadStatement();

        JCVariableDecl statement33 = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "value"), typeApply.arguments.get(1), maker.Literal(NULL_LITERAL, null));

        JCStatement statement34 = new StatementFactory(maker, utils, statement33).compactReadStatement();

        JCExpression select35 = maker.Select(ident(var.name.toString()), utils.getName("put"));
        JCStatement statement35 = maker.Exec(maker.Apply(List.<JCExpression>nil(), select35,
                List.of((JCExpression) ident(statement31.name.toString()), ident(statement33.name.toString()))));

        JCStatement init = maker.VarDef(maker.Modifiers(0), utils.getName(var.name.toString() + "index"), maker.TypeIdent(TypeTags.INT), maker.Literal(0));
        JCExpression cond = maker.Binary(OpCode.LESSER.value, ident(var.name.toString() + "index"), ident(var.name.toString() + "size"));
        JCExpressionStatement step = maker.Exec(maker.Unary(OpCode.UNARY_POST_INC.value, ident(var.name.toString() + "index")));
        JCStatement statement3 = maker.ForLoop(List.of(init), cond, List.of(step), maker.Block(0, List.of(statement31, statement32, statement33, statement34, statement35)));

        JCBinary binary = maker.Binary(OpCode.NOT_EQUALS.value, ident(var.name.toString() + "size"), maker.Literal(-1));
        return maker.Block(0, List.of(statement1, maker.If(binary, maker.Block(0, List.of(statement2, statement3)), null)));
    }

    public static boolean isInlineApplicable(JCVariableDecl var) {
        for (javax.lang.model.element.Element e : AutoSerializableProcessor.getClasses()) {
            if (e.toString().equals(var.vartype.type.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOrdinalApplicable(JCVariableDecl var) {
        return AutoSerializableProcessor.getOrdinals().contains(resolveFullClassName(var));
    }

    public static boolean isCollectionApplicable(JCVariableDecl var) {
        switch (resolveFullClassName(var)) {
            case "java.util.ArrayList":
            case "java.util.LinkedList":
            case "java.util.HashSet":
            case "java.util.TreeSet":
                return true;
            default:
                return AutoSerializableProcessor.getCollections().contains(resolveFullClassName(var));
        }
    }

    public static boolean isArrayApplicable(JCVariableDecl var) {
        return var.vartype.toString().endsWith("[]");
    }

    public static boolean isMapApplicable(JCVariableDecl var) {
        switch (resolveFullClassName(var)) {
            case "java.util.HashMap":
            case "java.util.TreeMap":
                return true;
            default:
                return AutoSerializableProcessor.getMaps().contains(resolveFullClassName(var));
        }
    }

    private JCExpression makeEgenIOUtilsSelect() {
        JCExpression expression = ident("com");
        expression = maker.Select(expression, utils.getName("devexperts"));
        expression = maker.Select(expression, utils.getName("egen"));
        expression = maker.Select(expression, utils.getName("processor"));
        expression = maker.Select(expression, utils.getName("IOUtils"));
        return expression;
    }

    private JCExpression makeDxlibIOUtilsSelect() {
        JCExpression expression = ident("com");
        expression = maker.Select(expression, utils.getName("devexperts"));
        expression = maker.Select(expression, utils.getName("io"));
        expression = maker.Select(expression, utils.getName("IOUtil"));
        return expression;
    }


    private boolean isPrimitive(String typeName) {
        return "boolean".equals(typeName) ||
                "char".equals(typeName) ||
                "byte".equals(typeName) ||
                "short".equals(typeName) ||
                "int".equals(typeName) ||
                "long".equals(typeName) ||
                "float".equals(typeName) ||
                "double".equals(typeName);
    }

    private JCIdent ident(String name) {
        return maker.Ident(utils.getName(name));
    }

    private JCExpression varDeltaFrom(JCVariableDecl var) {
        String typeIdentString = Character.toUpperCase(var.vartype.toString().charAt(0)) + var.vartype.toString().substring(1);
        if ("Int".equals(typeIdentString)) {
            typeIdentString = "Integer";
        }
        JCExpression resultExpr = maker.Ident(utils.getName(typeIdentString));
        resultExpr = maker.Select(resultExpr, utils.getName("valueOf"));

        String value = "0";
        for (JCExpression expression : var.mods.annotations.get(0).args) {
            JCAssign assign = (JCAssign) expression;
            if ("value".equals(assign.lhs.toString())) {
                value = assign.rhs.toString().substring(1, assign.rhs.toString().length() - 1);
            }
        }

        if (value.matches("[a-zA-Z].*")) {
            return maker.Apply(List.<JCExpression>nil(), resultExpr, List.of((JCExpression) ident(value)));
        } else {
            return maker.Apply(List.<JCExpression>nil(), resultExpr, List.of((JCExpression) maker.Literal(value)));
        }
    }
    private static String resolveFullClassName(JCVariableDecl var) {
        JCExpression expression = var.vartype;
        if (expression instanceof JCIdent) {
            return ((JCIdent) expression).sym.toString();
        } else if (expression instanceof JCTypeApply) {
            return ((JCIdent)((JCTypeApply)expression).clazz).sym.toString();
        } else if (expression instanceof JCArrayTypeTree) {
            return ((JCArrayTypeTree)expression).type.toString();
        } else if (expression instanceof JCPrimitiveTypeTree) {
            return ((JCPrimitiveTypeTree)expression).type.toString();
        } else {
            return null;
        }
    }

    static class SerializationStrategyRecord {
        String targetStrategy;
        String toTarget;
        String fromTarget;

        private SerializationStrategyRecord() {}

        public static SerializationStrategyRecord getByVariable(JCVariableDecl var) {
            try {
                List<Attribute.Compound> annotationAttributes = var.mods.annotations.get(0).type.tsym.attributes_field;
                Attribute.Compound strategyCompound = null;
                for (Attribute.Compound ac : annotationAttributes) {
                    if (ac.type.toString().endsWith("AutoSerializationStrategy")) {
                        strategyCompound = ac;
                    }
                }

                if (strategyCompound == null)
                    return null;

                SerializationStrategyRecord result = new SerializationStrategyRecord();
                for (Pair<com.sun.tools.javac.code.Symbol.MethodSymbol, Attribute> pair : strategyCompound.values) {
                    String annotationArgument = pair.fst.toString();
                    if (annotationArgument.startsWith("targetStrategy")) {
                        result.targetStrategy = ((Attribute.Constant)pair.snd).value.toString();
                    } else if (annotationArgument.startsWith("toTarget")) {
                        result.toTarget = ((Attribute.Constant)pair.snd).value.toString();
                    } else if (annotationArgument.startsWith("fromTarget")) {
                        result.fromTarget = ((Attribute.Constant)pair.snd).value.toString();
                    }
                }
                return result;
            } catch (NullPointerException | ClassCastException | IndexOutOfBoundsException e) {
                return null;
            }
        }

    }

}
