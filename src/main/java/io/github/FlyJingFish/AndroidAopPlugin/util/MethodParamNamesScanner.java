package io.github.FlyJingFish.AndroidAopPlugin.util;


import io.github.FlyJingFish.AndroidAopPlugin.config.AOPPluginComponent;
import io.github.FlyJingFish.AndroidAopPlugin.config.ApplicationConfig;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class MethodParamNamesScanner {
    private final List<MethodNode> methods = new ArrayList<>();
    private final ClassNode cn = new ClassNode();
    private CtClass ctClass;
    private int initCount;
    private static final Pattern pattern1 = Pattern.compile("^lambda\\$.*?\\$.+");
    private static final Pattern pattern2 = Pattern.compile("^access\\$\\d+");
    private static final Pattern pattern3 = Pattern.compile("\\$\\$.{32}\\$\\$AndroidAOP$");
    private static final Pattern pattern4 = Pattern.compile(".*?\\$lambda\\$.+");
    public static boolean isRemoveMethod(String methodName) {
        return pattern1.matcher(methodName).find()
                || pattern2.matcher(methodName).find()
                || pattern3.matcher(methodName).find()
                || pattern4.matcher(methodName).find();
    }

//    public static void main(String[] args) {
//        String name = "onCreate$lambda$7";
//        System.out.println(pattern4.matcher(name).find());
//    }

    public MethodParamNamesScanner(ClassReader cr){
        ApplicationConfig applicationConfig = AOPPluginComponent.getApplicationConfig();
        cr.accept(cn, 0);
        this.methods.addAll(cn.methods);
        Iterator<MethodNode> iterator = methods.iterator();

        while (iterator.hasNext()){
            MethodNode methodNode = iterator.next();
            if (isRemoveMethod(methodNode.name)){
                iterator.remove();
                continue;
            }

            int access = methodNode.access;
            boolean isPublic =  (access & Opcodes.ACC_PUBLIC) != 0;
            boolean isProtected =  (access & Opcodes.ACC_PROTECTED) != 0;
            boolean isPrivate =  (access & Opcodes.ACC_PRIVATE) != 0;
            boolean isPackage;
            isPackage = !isPublic && !isProtected && !isPrivate;

            if (applicationConfig.isPublic() && isPublic){
                continue;
            }
            if (applicationConfig.isProtected() && isProtected){
                continue;
            }
            if (applicationConfig.isPrivate() && isPrivate){
                continue;
            }
            if (applicationConfig.isPackage() && isPackage){
                continue;
            }
            iterator.remove();
        }

        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(Opcodes.ASM9,cw) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
            }
        },0);

        ClassPool cp = ClassPool.getDefault();
        InputStream byteArrayInputStream  =
                new ByteArrayInputStream(cw.toByteArray());
        try {
            ctClass = cp.makeClass(byteArrayInputStream);
        } catch (Throwable e) {
        }
    }

    private CtMethod getCtMethod(String methodName, String descriptor) {
        if (ctClass == null) return null;
        try {
            CtMethod[] ctMethods = ctClass.getDeclaredMethods(methodName);
            if (ctMethods != null) {
                for (CtMethod ctMethod : ctMethods) {
                    String allSignature = ctMethod.getSignature();
                    if (descriptor.equals(allSignature)) {
                        return ctMethod;
                    }
                }
            }
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    public boolean returnNull(String name,
                               String desc){
        boolean[] result = new boolean[]{false};
        for (MethodNode methodNode : methods) {
            if (desc.equals(methodNode.desc) && name.equals(methodNode.name)) {
                methodNode.accept(new MethodVisitor(Opcodes.ASM9) {

                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        String annotationClassName = Type.getType(descriptor).getClassName();
                        if (annotationClassName.equals("org.jetbrains.annotations.Nullable") || annotationClassName.equals("androidx.annotation.Nullable")) {
                            result[0] = true;
                        }
                        return super.visitAnnotation(descriptor, visible);
                    }
                });
            }
        }
        return result[0];
    }

    public boolean[] paramsNull(String name,
                              String desc){
        Type[] types = Type.getArgumentTypes(desc);
        boolean[] result = new boolean[types.length];
        for (MethodNode methodNode : methods) {
            if (desc.equals(methodNode.desc) && name.equals(methodNode.name)) {
                methodNode.accept(new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                        String annotationClassName = Type.getType(descriptor).getClassName();
//                        System.out.println(method.name+"==Parameter " + parameter + " has annotation: " + annotationClassName);
                        if (annotationClassName.equals("org.jetbrains.annotations.Nullable") || annotationClassName.equals("androidx.annotation.Nullable")) {
                            result[parameter] = true;
                        }
                        return super.visitParameterAnnotation(parameter, descriptor, visible);
                    }
                });
            }
        }
        return result;
    }

    public List<String> getParamsTypes(String name,
                                String desc){
        List<String> paramsList = new ArrayList<>();
        CtMethod ctMethod = getCtMethod(name, desc);
        if (ctMethod != null){
            String descCt = ctMethod.getSignature();
            System.out.println("desc="+desc+",descCt="+descCt);
            if (desc.equals(ctMethod.getSignature()) && name.equals(ctMethod.getName())) {
                try {
                    SignatureAttribute signatureAttribute = (SignatureAttribute) ctMethod.getMethodInfo().getAttribute(SignatureAttribute.tag);
                    if (signatureAttribute != null) {
                        // 解析签名
                        SignatureAttribute.MethodSignature methodSignature = SignatureAttribute.toMethodSignature(signatureAttribute.getSignature());

                        SignatureAttribute.Type[] params = methodSignature.getParameterTypes();
                        for (SignatureAttribute.Type paramType : params) {
                            paramsList.add(paramType.jvmTypeName());
                        }
                        return paramsList;
                    }
                } catch (BadBytecode e) {
                }
            }
        }
        Type[] types = Type.getArgumentTypes(desc);
        for (Type type : types) {
            paramsList.add(type.getClassName());
        }
        return paramsList;
    }

    public String getReturnType(String name, String desc){
        CtMethod ctMethod = getCtMethod(name, desc);
        if (ctMethod != null){
            if (desc.equals(ctMethod.getSignature()) && name.equals(ctMethod.getName())) {
                try {
                    SignatureAttribute signatureAttribute = (SignatureAttribute) ctMethod.getMethodInfo().getAttribute(SignatureAttribute.tag);
                    if (signatureAttribute != null) {
                        // 解析签名
                        SignatureAttribute.MethodSignature methodSignature = SignatureAttribute.toMethodSignature(signatureAttribute.getSignature());
                        return methodSignature.getReturnType().jvmTypeName();
                    }
                } catch (BadBytecode e) {
                }
            }
        }
        return Type.getReturnType(desc).getClassName();
    }
    public List<String> getJavaParamsTypes(String name,
                                             String desc){
        List<String> paramsList = getParamsTypes(name, desc);
        List<String> newList = new ArrayList<>();
        for (String s : paramsList) {
            newList.add(JavaToKotlinTypeConverter.removePackageNames(s));
        }
        return newList;
    }

    public String getJavaReturnType(String name,
                                      String desc){
        String returnType = getReturnType(name, desc);
        return JavaToKotlinTypeConverter.removePackageNames(returnType);
    }
    public List<String> getKotlinParamsTypes(String name,
                                       String desc){
        List<String> paramsList = getParamsTypes(name, desc);
        List<String> newList = new ArrayList<>();
        for (String s : paramsList) {

            String kotlinType = JavaToKotlinTypeConverter.convertJavaTypeToKotlin(s);
            newList.add(kotlinType);
        }
        return newList;
    }

    public String getKotlinReturnType(String name,
                                String desc){
        boolean isSuspendMethod = desc.endsWith("Lkotlin/coroutines/Continuation;)Ljava/lang/Object;");
        if (isSuspendMethod){
            for (MethodNode methodNode : methods) {
                if (desc.equals(methodNode.desc) && name.equals(methodNode.name)) {
                    return JavaToKotlinTypeConverter.convertJavaTypeToKotlin(AndroidAOPCode.getSuspendMethodType(methodNode.signature));
                }
            }
        }
        String returnType = getReturnType(name, desc);
        return JavaToKotlinTypeConverter.convertJavaTypeToKotlin(returnType);
    }

    public boolean isInterface(){
         return (cn.access & Opcodes.ACC_INTERFACE) != 0;
    }

    public String getClassName(){
        return cn.name.replaceAll("/",".");
    }

    public int getInitCount() {
        return initCount++;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    /**
     * 获取参数名列表辅助方法
     *
     * @param name
     * @param desc
     * @param size
     * @return
     */
    public List<String> getParamNames(
            String name,
            String desc ,
            int size
    ) {
        List<String> list = new ArrayList<>();
        for (MethodNode methodNode : methods) {
            Map<Integer, LocalVariable> varNames = new HashMap<>();
            if (desc.equals(methodNode.desc) && name.equals(methodNode.name)) {
                List<LocalVariableNode> localVariables = methodNode.localVariables;
                if (localVariables == null){
                    continue;
                }
                for (int i1 = 0; i1 < localVariables.size(); i1++) {
                    String varName = localVariables.get(i1).name;
                    // index-记录了正确的方法本地变量索引。(方法本地变量顺序可能会被打乱。而index记录了原始的顺序)
                    int index = localVariables.get(i1).index;
                    if (!"this".equals(varName)) {
                        varNames.put(index, new LocalVariable(index, varName));
                    }
                }


                List<LocalVariable> tmpArr = new ArrayList<>(varNames.values());
                Collections.sort(tmpArr);
                int len = Math.min(tmpArr.size(), size);

                for (int j = 0; j < len; j++) {
                    list.add(tmpArr.get(j).name);
                }
                break;
            }
        }
        if (size > 0 && list.isEmpty()){
            for (int i = 0; i < size; i++) {
                list.add("var"+i);
            }
        }
        return list;
    }

    /**
     * 方法本地变量索引和参数名封装
     * @author xby Administrator
     */
    private static class LocalVariable implements Comparable<LocalVariable> {
        private int index;
        private String name;

        public LocalVariable(int index, String name) {
            this.index = index;
            this.name = name;
        }

        @Override
        public int compareTo(LocalVariable other) {
            return index - other.index;
        }

        @Override
        public String toString() {
            return "LocalVariable{" +
                    "index=" + index +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}