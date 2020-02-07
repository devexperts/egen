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

import com.devexperts.egen.processor.tools.MethodBlockFactory;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
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
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.tree.JCTree.*;


@SupportedAnnotationTypes(value = {AutoSerializableProcessor.ANNOTATION_TYPE})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"ordinals", "maps", "collections"})
public class AutoSerializableProcessor extends AbstractProcessor {
    public static final String ANNOTATION_TYPE = "com.devexperts.egen.processor.annotations.AutoSerializable";
    private static final HashSet<String> VAR_ANNOTATION_LIST = new HashSet<>(Arrays.asList("Compact", "Delta", "Inline", "Ordinal", "PresenceBit"));
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
        // in incremental mode gradle would pass us a wrapped instance
        boolean isIncrementalCompilation = !(procEnv instanceof JavacProcessingEnvironment);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "EGEN: isIncrementalCompilation=" + isIncrementalCompilation);

        this.javacProcessingEnv = getJavacProcessingEnvironment(procEnv);
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

                for (final Element e : classes) {
                    JCTree classNode = utils.getTree(e);

                    classDecl = (JCClassDecl) classNode;
                    filterClass(classDecl);

                    /* This a dirty hack: for unknown reason, compiler's
                    com.sun.tools.javac.comp.Flow.AssignAnalyzer.visitMethodDef() internal check fails without this.
                    Intervening in the unsupported API sometimes hurts. */
                    maker.pos = Math.max(maker.pos, classDecl.getModifiers().getStartPosition());

                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "EGEN: Class " + classDecl.name +
                            " custom serialization protocol is being automatically implemented.");

                    logPw.println("EGEN: Class " + classDecl.name +
                            " custom serialization protocol is being automatically implemented.");

                    JCExpression ioExceptionClass = makeSelectExpr("java.io.IOException");
                    JCExpression classNotFoundExceptionClass = makeSelectExpr("java.lang.ClassNotFoundException");
                    JCExpression objectOutputClass = makeSelectExpr("java.io.ObjectOutputStream");
                    JCExpression objectInputClass = makeSelectExpr("java.io.ObjectInputStream");

                    classDecl.implementing = classDecl.implementing.append(makeSelectExpr("java.io.Serializable"));

                    JCModifiers privateModifiers = maker.Modifiers(Flags.PRIVATE , List.<JCAnnotation>nil());
                    JCModifiers publicStaticModifiers = maker.Modifiers(Flags.PUBLIC | Flags.STATIC, List.<JCAnnotation>nil());

                    MethodBlockFactory methodBlockFactory = new MethodBlockFactory(maker, utils, classDecl);

                    JCBlock writeContentsBlock = methodBlockFactory.writeContentsBlock();
                    JCMethodDecl writeContentsMethod = getWriteContentsMethod(utils, ioExceptionClass,
                            publicStaticModifiers, objectOutputClass, writeContentsBlock);

                    JCBlock readContentsBlock = methodBlockFactory.readContentsBlock();
                    JCMethodDecl readContentsMethod = getReadContentsMethod(utils, ioExceptionClass,
                            classNotFoundExceptionClass, publicStaticModifiers, objectInputClass, readContentsBlock);

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

                    makeAllFieldsTransient(classDecl);
                    logPw.println("EGEN: Class " + classDecl.name + " - success.\n" + classDecl.toString());
                }
            } catch (Throwable t) {
                if (logPw != null) {
                    logPw.println("EGEN: Processing of " + classDecl.name + " resulted an exception:");
                    logPw.println(t);
                    t.printStackTrace(logPw);
                }
            } finally {
                if (logPw != null) {
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
                maker.TypeIdent(TypeTag.VOID),
                List.<JCTypeParameter>nil(),
                List.of(
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("out"), objectOutputClass, null),
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("self"), makeSelectExpr(classDecl.sym.type.toString()), null)
                ),
                List.of(ioExceptionClass),
                writeContentsBlock,
                null
        );
    }

    private JCMethodDecl getReadContentsMethod(JavacElements utils, JCExpression ioExceptionClass, JCExpression classNotFoundExceptionClass, JCModifiers methodModifiers, JCExpression objectInputClass, JCBlock readContentsBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("readContents"),
                maker.TypeIdent(TypeTag.VOID),
                List.<JCTypeParameter>nil(),
                List.of(
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("in"), objectInputClass, null),
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("self"), makeSelectExpr(classDecl.sym.type.toString()), null)
                ),
                List.of(ioExceptionClass, classNotFoundExceptionClass),
                readContentsBlock,
                null
        );
    }

    private JCMethodDecl getWriteObjectMethod(JavacElements utils, JCExpression ioExceptionClass, JCModifiers methodModifiers, JCExpression objectOutputClass, JCBlock writeObjectBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("writeObject"),
                maker.TypeIdent(TypeTag.VOID),
                List.<JCTypeParameter>nil(),
                List.of(maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("out"), objectOutputClass, null)),
                List.of(ioExceptionClass),
                writeObjectBlock,
                null
        );
    }

    private JCMethodDecl getReadObjectMethod(JavacElements utils, JCExpression ioExceptionClass, JCExpression classNotFoundExceptionClass, JCModifiers methodModifiers, JCExpression objectInputClass, JCBlock readObjectBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("readObject"),
                maker.TypeIdent(TypeTag.VOID),
                List.<JCTypeParameter>nil(),
                List.of(maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("in"), objectInputClass, null)),
                List.of(ioExceptionClass, classNotFoundExceptionClass),
                readObjectBlock,
                null
        );
    }

    private JCMethodDecl getWriteInlineMethod(JavacElements utils, JCExpression ioExceptionClass, JCModifiers methodModifiers, JCExpression objectOutputClass, JCBlock writeInlineBlock) {
        return maker.MethodDef(
                methodModifiers,
                utils.getName("writeInline"),
                maker.TypeIdent(TypeTag.VOID),
                List.<JCTypeParameter>nil(),
                List.of(
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("out"), objectOutputClass, null),
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("self"), makeSelectExpr(classDecl.sym.type.toString()), null),
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("checkClass"), maker.TypeIdent(TypeTag.BOOLEAN), null)
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
                maker.TypeIdent(TypeTag.VOID),
                List.<JCTypeParameter>nil(),
                List.of(
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("in"), objectInputClass, null),
                        maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("self"), makeSelectExpr(classDecl.sym.type.toString()), null)
                ),
                List.of(ioExceptionClass, classNotFoundExceptionClass),
                readInlineBlock,
                null
        );
    }

    private JCMethodDecl getPrepareFlagsMethod(JavacElements utils, JCBlock prepareFlagsBlock) {
        return maker.MethodDef(
                maker.Modifiers(Flags.PRIVATE | Flags.STATIC),
                utils.getName("prepareFlags"),
                maker.TypeIdent(TypeTag.LONG),
                List.<JCTypeParameter>nil(),
                List.of(maker.VarDef(maker.Modifiers(Flags.PARAMETER), utils.getName("self"), makeSelectExpr(classDecl.sym.type.toString()), null)),
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

    /**
     * @return EGEN-related annotation expression (or null, if var is not annotated)
     */
    public static JCAnnotation getEgenAnnotation(JCVariableDecl var) {
        for (JCAnnotation annotation : var.mods.annotations) {
            if (VAR_ANNOTATION_LIST.contains(annotation.getAnnotationType().toString()))
                return annotation;
        }
        return null;
    }

    /**
     * @return string representation of EGEN-related annotation (or "", if var is not annotated)
     */
    public static String getEgenAnnotationType(JCVariableDecl var) {
        for (JCAnnotation annotation : var.mods.annotations) {
            if (VAR_ANNOTATION_LIST.contains(annotation.getAnnotationType().toString()))
                return annotation.annotationType.toString();
        }
        return "";
    }

    private static void filterClass(JCClassDecl classDecl) {
        List<JCExpression> newImplementing = List.nil();
        for (JCExpression expr : classDecl.implementing) {
            if (!expr.toString().endsWith("Externalizable") && !expr.toString().endsWith("Serializable"))
                newImplementing = newImplementing.append(expr);
        }
        classDecl.implementing = newImplementing;
    }

    /**
     * Info about fields is redundant in the class descriptor if custom serialization methods are present.
     */
    private static void makeAllFieldsTransient(JCClassDecl classDecl) {
        for (JCTree tree : classDecl.defs) {
            if (tree instanceof JCVariableDecl)
                ((JCVariableDecl) tree).mods.flags |= Flags.TRANSIENT;
        }
    }

    /**
     * This class casts the given processing environment to a JavacProcessingEnvironment. In case of
     * gradle incremental compilation, the delegate ProcessingEnvironment of the gradle wrapper is returned.
     */
    private static JavacProcessingEnvironment getJavacProcessingEnvironment(ProcessingEnvironment procEnv) {
        final Class<?> procEnvClass = procEnv.getClass();
        if (procEnv.getClass().getName().equals("org.gradle.api.internal.tasks.compile.processing.IncrementalProcessingEnvironment")) {
            try {
                Field field = procEnvClass.getDeclaredField("delegate");
                field.setAccessible(true);
                Object delegate = field.get(procEnv);
                return getJavacProcessingEnvironment((ProcessingEnvironment) delegate);
            } catch (Exception e) {
                e.printStackTrace();
                procEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Can't get the delegate of the gradle IncrementalProcessingEnvironment.");
                throw new IllegalStateException(e);
            }
        }
        return (JavacProcessingEnvironment) procEnv;
    }
}
