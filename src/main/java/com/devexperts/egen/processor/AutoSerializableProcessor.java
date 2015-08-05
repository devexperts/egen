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

import com.devexperts.egen.processor.tools.MethodBlockFactory;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Set;

import static com.sun.tools.javac.tree.JCTree.*;


@SupportedAnnotationTypes(value = {AutoSerializableProcessor.ANNOTATION_TYPE})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions({"ordinals", "maps", "collections"})
public class AutoSerializableProcessor extends AbstractProcessor {
    public static final String ANNOTATION_TYPE = "com.devexperts.egen.processor.annotations.AutoSerializable";
    private JavacProcessingEnvironment javacProcessingEnv;
    private TreeMaker maker;

    private static Set<? extends Element> classes; // list of processing classes
    private static String ordinals; // list of enumerable clases that should be marshaled as their .code() integer
    private static String maps; // list of classes that should be considered as maps
    private static String collections; // list of classes that should be considered as collections
    private JavacElements utils;
    private JCClassDecl classDecl;

    @Override
    public void init(ProcessingEnvironment procEnv) {
        super.init(procEnv);
        this.javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
        this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        final TypeElement annotation = javacProcessingEnv.getElementUtils().getTypeElement(ANNOTATION_TYPE);

        if (annotation != null) {
            classes = roundEnv.getElementsAnnotatedWith(annotation);

            fetchCompilerOptions();

            utils = javacProcessingEnv.getElementUtils();
            PrintWriter logPw = null; // TODO: sane logging
            try {
                logPw = new PrintWriter("egen_output.txt");
            } catch (FileNotFoundException ignored) {
            }
            for (final Element e : classes) {
                JCTree classNode = utils.getTree(e);

                classDecl = (JCClassDecl) classNode;
                filterClass(classDecl);

                JCExpression serializableInterface = makeSelectExpr("java.io.Serializable");
                JCExpression ioExceptionClass = makeSelectExpr("java.io.IOException");
                JCExpression classNotFoundExceptionClass = makeSelectExpr("java.lang.ClassNotFoundException");
                JCExpression objectOutputClass = makeSelectExpr("java.io.ObjectOutputStream");
                JCExpression objectInputClass = makeSelectExpr("java.io.ObjectInputStream");

                classDecl.implementing = classDecl.implementing.append(serializableInterface);

                JCModifiers publicModifiers = maker.Modifiers(Flags.PUBLIC, List.<JCAnnotation>nil());
                JCModifiers privateModifiers = maker.Modifiers(Flags.PRIVATE, List.<JCAnnotation>nil());
                JCModifiers publicStaticModifiers = maker.Modifiers(Flags.PUBLIC | Flags.STATIC, List.<JCAnnotation>nil());

                MethodBlockFactory methodBlockFactory = new MethodBlockFactory(maker, utils, classDecl);

                JCBlock writeContentsBlock = methodBlockFactory.writeContentsBlock();
                JCMethodDecl writeContentsMethod = getWriteContentsMethod(utils, ioExceptionClass,
                        publicModifiers, objectOutputClass, writeContentsBlock);

                JCBlock readContentsBlock = methodBlockFactory.readContentsBlock();
                JCMethodDecl readContentsMethod = getReadContentsMethod(utils, ioExceptionClass, classNotFoundExceptionClass, publicModifiers, objectInputClass, readContentsBlock);

                JCBlock writeObjectBlock = methodBlockFactory.writeObjectBlock();
                JCMethodDecl writeObjectMethod = getWriteObjectMethod(utils, ioExceptionClass, privateModifiers,
                        objectOutputClass, writeObjectBlock);

                JCBlock readObjectBlock = methodBlockFactory.readObjectBlock();
                JCMethodDecl readObjectMethod = getReadObjectMethod(utils, ioExceptionClass, classNotFoundExceptionClass,
                        privateModifiers, objectInputClass, readObjectBlock);

                JCBlock writeInlineBlock = methodBlockFactory.writeInlineBlock();
                JCMethodDecl writeInlineMethod = getWriteInlineMethod(utils, ioExceptionClass, publicStaticModifiers,
                        objectOutputClass, writeInlineBlock);

                JCBlock readInlineBlock = methodBlockFactory.readInlineBlock();
                JCMethodDecl readInlineMethod = getReadInlineMethod(utils, ioExceptionClass, classNotFoundExceptionClass,
                        publicStaticModifiers, objectInputClass, readInlineBlock);

                JCBlock prepareFlagsBlock = methodBlockFactory.prepareFlagsBlock();
                JCMethodDecl prepareFlagsMethod = getPrepareFlagsMethod(utils, prepareFlagsBlock);

                classDecl.defs = classDecl.defs.append(writeContentsMethod);
                classDecl.defs = classDecl.defs.append(writeObjectMethod);
                classDecl.defs = classDecl.defs.append(writeInlineMethod);

                classDecl.defs = classDecl.defs.append(readContentsMethod);
                classDecl.defs = classDecl.defs.append(readObjectMethod);
                classDecl.defs = classDecl.defs.append(readInlineMethod);

                classDecl.defs = classDecl.defs.append(prepareFlagsMethod);

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "EGEN: Class " + classDecl.name +
                        " custom serialization protocol is being automatically implemented.");

                if (logPw != null) {
                    logPw.println("EGEN: Class " + classDecl.name +
                            " custom serialization protocol is being automatically implemented.\n" + classDecl.toString());
                    logPw.flush();
                }
            }

            return true;
        }

        return false;
    }

    private void fetchCompilerOptions() {
        ordinals = processingEnv.getOptions().get("ordinals");
        if (ordinals == null)
            ordinals = "";

        maps = processingEnv.getOptions().get("maps");
        if (maps == null)
            maps = "";

        collections = processingEnv.getOptions().get("collections");
        if (collections == null)
            collections = "";
    }

    private JCMethodDecl getWriteContentsMethod(JavacElements utils, JCExpression ioExceptionClass, JCModifiers methodModifiers, JCExpression objectOutputClass, JCBlock writeContentsBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("writeContents"),
                maker.TypeIdent(TypeTags.VOID),
                List.<JCTypeParameter>nil(),
                List.of(maker.VarDef(maker.Modifiers(0), utils.getName("out"), objectOutputClass, null)),
                List.of(ioExceptionClass),
                writeContentsBlock,
                null
        );
    }

    private JCMethodDecl getReadContentsMethod(JavacElements utils, JCExpression ioExceptionClass, JCExpression classNotFoundExceptionClass, JCModifiers methodModifiers, JCExpression objectInputClass, JCBlock readContentsBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("readContents"),
                maker.TypeIdent(TypeTags.VOID),
                List.<JCTypeParameter>nil(),
                List.of(maker.VarDef(maker.Modifiers(0), utils.getName("in"), objectInputClass, null)),
                List.of(ioExceptionClass, classNotFoundExceptionClass),
                readContentsBlock,
                null
        );
    }

    private JCMethodDecl getWriteObjectMethod(JavacElements utils, JCExpression ioExceptionClass, JCModifiers methodModifiers, JCExpression objectOutputClass, JCBlock writeObjectBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("writeObject"),
                maker.TypeIdent(TypeTags.VOID),
                List.<JCTypeParameter>nil(),
                List.of(maker.VarDef(maker.Modifiers(0), utils.getName("out"), objectOutputClass, null)),
                List.of(ioExceptionClass),
                writeObjectBlock,
                null
        );
    }

    private JCMethodDecl getReadObjectMethod(JavacElements utils, JCExpression ioExceptionClass, JCExpression classNotFoundExceptionClass, JCModifiers methodModifiers, JCExpression objectInputClass, JCBlock readObjectBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("readObject"),
                maker.TypeIdent(TypeTags.VOID),
                List.<JCTypeParameter>nil(),
                List.of(maker.VarDef(maker.Modifiers(0), utils.getName("in"), objectInputClass, null)),
                List.of(ioExceptionClass, classNotFoundExceptionClass),
                readObjectBlock,
                null
        );
    }

    private JCMethodDecl getWriteInlineMethod(JavacElements utils, JCExpression ioExceptionClass, JCModifiers methodModifiers, JCExpression objectOutputClass, JCBlock writeInlineBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("writeInline"),
                maker.TypeIdent(TypeTags.VOID),
                List.<JCTypeParameter>nil(),
                List.of(
                        maker.VarDef(maker.Modifiers(0), utils.getName("out"), objectOutputClass, null),
                        maker.VarDef(maker.Modifiers(0), utils.getName("self"), ident(classDecl.name.toString()), null),
                        maker.VarDef(maker.Modifiers(0), utils.getName("checkClass"), maker.TypeIdent(TypeTags.BOOLEAN), null)
                ),
                List.of(ioExceptionClass),
                writeInlineBlock,
                null
        );
    }

    private JCMethodDecl getReadInlineMethod(JavacElements utils, JCExpression ioExceptionClass, JCExpression classNotFoundExceptionClass, JCModifiers methodModifiers, JCExpression objectInputClass, JCBlock readInlineBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("readInline"),
                maker.TypeIdent(TypeTags.VOID),
                List.<JCTypeParameter>nil(),
                List.of(
                        maker.VarDef(maker.Modifiers(0), utils.getName("in"), objectInputClass, null),
                        maker.VarDef(maker.Modifiers(0), utils.getName("self"), makeSelectExpr(classDecl.name.toString()), null)
                ),
                List.of(ioExceptionClass, classNotFoundExceptionClass),
                readInlineBlock,
                null
        );
    }

    private JCMethodDecl getPrepareFlagsMethod(JavacElements utils, JCBlock prepareFlagsBlock) {
        return maker.MethodDef(
                maker.Modifiers(Flags.PRIVATE),
                utils.getName("prepareFlags"),
                maker.TypeIdent(TypeTags.LONG),
                List.<JCTypeParameter>nil(),
                List.<JCVariableDecl>nil(),
                List.<JCExpression>nil(),
                prepareFlagsBlock,
                null
        );
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

    public static Set<? extends Element> getClasses() {
        return classes;
    }

    public static String getOrdinals() {
        return ordinals;
    }

    public static String getMaps() {
        return maps;
    }

    public static String getCollections() {
        return collections;
    }

    private static void filterClass(JCClassDecl classDecl) {
        List<JCExpression> newImplementing = List.nil();
        for (JCExpression expr : classDecl.implementing) {
            if (!expr.toString().endsWith("Externalizable") && !expr.toString().endsWith("Serializable"))
                newImplementing = newImplementing.append(expr);
        }
        classDecl.implementing = newImplementing;

//        List<JCTree> newDefs = List.nil();
//        for (JCTree tree : classDecl.defs) {
//            if (tree instanceof JCMethodDecl) {
//                JCMethodDecl methodDecl = (JCMethodDecl) tree;
//                if (!methodDecl.name.toString().equals("readExternal") && !methodDecl.name.toString().equals("writeExternal"))
//                    newDefs = newDefs.append(tree);
//            } else {
//                newDefs = newDefs.append(tree);
//            }
//
//        }
//        classDecl.defs = newDefs;
    }
}
