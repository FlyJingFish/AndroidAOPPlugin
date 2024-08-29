package io.github.FlyJingFish.AndroidAopPlugin.util;


import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;
import io.github.FlyJingFish.AndroidAopPlugin.config.AOPPluginComponent;
import io.github.FlyJingFish.AndroidAopPlugin.config.ApplicationConfig;
import io.github.FlyJingFish.AndroidAopPlugin.config.CopyAnnotation;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class MethodParamNamesScanner {
    private final List<MethodNode> methods = new ArrayList<>();
    private final ClassNode cn = new ClassNode();
    private final ApplicationConfig applicationConfig;
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
        applicationConfig = AOPPluginComponent.getApplicationConfig();
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

    private List<String> getParamsTypes(String name,
                                String desc){
        List<String> paramsList = new ArrayList<>();
        CtMethod ctMethod = getCtMethod(name, desc);
        if (ctMethod != null){
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

    private String getReturnType(String name, String desc){
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
        return cn.name.replace("/",".");
    }

    public String getExtendsClassName(){
        return JavaToKotlinTypeConverter.removePackageNames(getExtendsLongClassName());
    }

    public String getExtendsLongClassName(){
        // 获取类签名
        try {
            SignatureAttribute signatureAttribute = (SignatureAttribute) ctClass.getClassFile().getAttribute(SignatureAttribute.tag);
            if (signatureAttribute != null) {
                // 解析签名
                SignatureAttribute.ClassSignature classSignature = SignatureAttribute.toClassSignature(signatureAttribute.getSignature());
                return classSignature.getSuperClass().toString();
            }
        } catch (BadBytecode ignore) {

        }
        return cn.superName.replace("/",".");
    }

    public String getSuperClassName(){
        return JavaToKotlinTypeConverter.removePackageNames(getSuperLongClassName());
    }

    public String getSuperLongClassName(){
        return cn.superName.replace("/",".");
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
                for (LocalVariableNode localVariable : localVariables) {
                    String varName = localVariable.name;
                    // index-记录了正确的方法本地变量索引。(方法本地变量顺序可能会被打乱。而index记录了原始的顺序)
                    int index = localVariable.index;
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

    private String[][] getParamsAnnotation(String name,
                                        String desc, FileTypeExtension extension){
        Type[] types = Type.getArgumentTypes(desc);
        CtMethod ctMethod = getCtMethod(name, desc);
        String[][] annoStr = new String[types.length][];
        if (ctMethod != null){
            if (desc.equals(ctMethod.getSignature()) && name.equals(ctMethod.getName())) {
                // 获取方法信息
                MethodInfo methodInfo = ctMethod.getMethodInfo();

                // 获取方法参数的注解
                ParameterAnnotationsAttribute paramAttr = (ParameterAnnotationsAttribute)
                        methodInfo.getAttribute(ParameterAnnotationsAttribute.visibleTag);

                if (paramAttr != null) {
                    Annotation[][] annotations = paramAttr.getAnnotations();
                    for (int i = 0; i < annotations.length; i++) {
                        Annotation[] annotations1 = annotations[i];
                        annoStr[i] = new String[annotations1.length];
                        String[] annoStr1= annoStr[i];
                        for (int j = 0; j < annotations1.length; j++) {
                            annoStr1[j] = getAnnotationShortString(annotations1[j],extension);
                        }
                    }
                    return annoStr;
                }
            }
        }
        Arrays.fill(annoStr, new String[0]);
        return null;
    }

    public String[][] getJavaParamsAnnotation(String name,
                                           String desc){
        return getParamsAnnotation(name, desc,FileTypeExtension.JAVA);
    }

    public String[][] getKotlinParamsAnnotation(String name,
                                              String desc){
        String[][] annoStr = getParamsAnnotation(name, desc,FileTypeExtension.KOTLIN);
        if (annoStr == null){
            return null;
        }
        for (String[] annotations : annoStr) {
            for (int j = 0; j < annotations.length; j++) {
                String anno = annotations[j];

                annotations[j] = anno;
            }
        }
        return annoStr;
    }

    private String[] getReturnAnnotation(String name,
                                        String desc, FileTypeExtension extension){
        CtMethod ctMethod = getCtMethod(name, desc);
        if (ctMethod != null){
            if (desc.equals(ctMethod.getSignature()) && name.equals(ctMethod.getName())) {
                // 获取方法信息
                MethodInfo methodInfo = ctMethod.getMethodInfo();

                // 获取方法参数的注解
                AnnotationsAttribute attr = (AnnotationsAttribute)
                        methodInfo.getAttribute(AnnotationsAttribute.visibleTag);

                if (attr != null) {
                    Annotation[] annotations = attr.getAnnotations();
                    String[] annoStr = new String[annotations.length];
                    for (int i = 0; i < annotations.length; i++) {
                        annoStr[i] = getAnnotationShortString(annotations[i],extension);
                    }
                    return annoStr;
                }
            }
        }
        return null;
    }

    public static String toAnnotationString(Annotation annotation, FileTypeExtension extension) {
        StringBuilder buf = new StringBuilder("@");
        buf.append(annotation.getTypeName());
        if (annotation.getMemberNames() != null) {
            buf.append("(");

            for (String name:annotation.getMemberNames()) {
                MemberValue annoItemValue = annotation.getMemberValue(name);
                String annoStr;
                if (annoItemValue instanceof ArrayMemberValue){
                    if (extension == FileTypeExtension.KOTLIN){
                        annoStr = annoItemValue.toString().replaceAll("^\\{","[").replaceAll("}$","]");
                    }else {
                        annoStr = annoItemValue.toString();
                    }
                }else if (annoItemValue instanceof EnumMemberValue){
                    annoStr = removeBeforeSecondLastDot(annoItemValue.toString());
                }else if (annoItemValue instanceof ClassMemberValue){
                    annoStr = JavaToKotlinTypeConverter.removePackageNames(annoItemValue.toString().replaceAll(".class$",""))+(extension == FileTypeExtension.KOTLIN?"::class.java":".class");
                }else if (annoItemValue instanceof AnnotationMemberValue){
                    Annotation annotationMember = ((AnnotationMemberValue) annoItemValue).getValue();
                    String value = getAnnotationShortString(annotationMember,extension);
                    if (extension == FileTypeExtension.KOTLIN){
                        annoStr = value.replaceFirst("^@","");
                    }else {
                        annoStr = value;
                    }
                }else {
                    annoStr = annoItemValue.toString();
                }

                buf.append(name).append("=")
                        .append(annoStr)
                        .append(", ");
            }
            buf.setLength(buf.length()-2);
            buf.append(")");
        }

        return buf.toString();
    }

//    public static void main(String[] args) {
//        String name = "com.CollectType.EXTENDS";
//        System.out.println("NAME="+removeBeforeSecondLastDot(name));
//    }

    public static String removeBeforeSecondLastDot(String input) {
        // 找到最后一个 '.' 的位置
        int lastDotIndex = input.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return input; // 如果没有找到 '.'，返回原始字符串
        }

        // 找到倒数第二个 '.' 的位置
        int secondLastDotIndex = input.lastIndexOf(".", lastDotIndex - 1);
        if (secondLastDotIndex == -1) {
            return input; // 如果只有一个 '.'，返回原始字符串
        }

        // 截取从倒数第二个 '.' 之后的字符串
        return input.substring(secondLastDotIndex + 1);
    }

    public static String getAnnotationShortString(Annotation annotation, FileTypeExtension extension){
        String longString = toAnnotationString(annotation,extension);
        String regex = "^@"+annotation.getTypeName();
        String replaceText = "@" +JavaToKotlinTypeConverter.removePackageNames(annotation.getTypeName());
        return longString.replaceFirst(regex,replaceText);
    }

    public String[] getJavaReturnAnnotation(String name,
                                              String desc){
        return getReturnAnnotation(name, desc,FileTypeExtension.JAVA);
    }

    public String[] getKotlinReturnAnnotation(String name,
                                                String desc){
        String[] annoStr = getReturnAnnotation(name, desc,FileTypeExtension.KOTLIN);
        if (annoStr == null){
            return null;
        }
        for (int j = 0; j < annoStr.length; j++) {
            String anno = annoStr[j];

            annoStr[j] = anno;
        }
        return annoStr;
    }

    public Set<String> getReplaceImportPackage(FileTypeExtension extension){
        Set<String> packageList = new HashSet<>();
        for (MethodNode method : methods) {
            Type[] argTypes = Type.getArgumentTypes(method.desc);
            Type returnType = Type.getReturnType(method.desc);
            if (!"kotlin.coroutines.Continuation".equals(returnType.getClassName())){
                addData(packageList,returnType.getClassName(),extension);
            }
            for (Type argType : argTypes) {
                if (!"kotlin.coroutines.Continuation".equals(argType.getClassName())){
                    addData(packageList,argType.getClassName(),extension);
                }
            }

            if (applicationConfig.getCopyAnnotation() == CopyAnnotation.Copy){
                CtMethod ctMethod = getCtMethod(method.name, method.desc);
                if (ctMethod != null){
                    if (method.desc.equals(ctMethod.getSignature()) && method.name.equals(ctMethod.getName())) {
                        // 获取方法信息
                        MethodInfo methodInfo = ctMethod.getMethodInfo();

                        // 获取方法参数的注解
                        ParameterAnnotationsAttribute paramAttr = (ParameterAnnotationsAttribute)
                                methodInfo.getAttribute(ParameterAnnotationsAttribute.visibleTag);

                        if (paramAttr != null) {
                            Annotation[][] annotations = paramAttr.getAnnotations();
                            for (Annotation[] annotations1 : annotations) {
                                for (Annotation annotation : annotations1) {
                                    Set<String> packageList2 = getPackage4Annotation(annotation, extension);
                                    packageList.addAll(packageList2);
                                }
                            }
                        }
                    }
                }
            }
        }
        return packageList;
    }

    private Set<String> getPackage4Annotation(Annotation annotation, FileTypeExtension extension){
        Set<String> packageList = new HashSet<>();

        addData(packageList,annotation.getTypeName(),extension);
        for (String name:annotation.getMemberNames()) {
            MemberValue annoItemValue = annotation.getMemberValue(name);
            Set<String> packageList2 = getPackage4Annotation(annoItemValue,extension);
            packageList.addAll(packageList2);
        }
        return packageList;
    }

    private Set<String> getPackage4Annotation(MemberValue annoItemValue, FileTypeExtension extension){
        Set<String> packageList = new HashSet<>();
        if (annoItemValue instanceof ArrayMemberValue){
            if (((ArrayMemberValue) annoItemValue).getValue() != null) {
                for (int i = 0; i < ((ArrayMemberValue) annoItemValue).getValue().length; i++) {
                    MemberValue annoItemValue2 = ((ArrayMemberValue) annoItemValue).getValue()[i];
                    Set<String> packageList2 = getPackage4Annotation(annoItemValue2,extension);
                    packageList.addAll(packageList2);
                }
            }
        }else if (annoItemValue instanceof EnumMemberValue){
            addData(packageList,((EnumMemberValue) annoItemValue).getType(),extension);
        }else if (annoItemValue instanceof ClassMemberValue){
            addData(packageList,((ClassMemberValue) annoItemValue).getValue(),extension);
        }else if (annoItemValue instanceof AnnotationMemberValue){
            Annotation annotationMember = ((AnnotationMemberValue) annoItemValue).getValue();

            Set<String> packageList2 = getPackage4Annotation(annotationMember,extension);
            packageList.addAll(packageList2);
        }
        return packageList;
    }

    private void addData(Set<String> packageList,String name,FileTypeExtension extension){
        if (!JavaToKotlinTypeConverter.isBaseType(name) && !"void".equals(name)){
            if (name.contains("[]")) {
                String type = name.replaceAll("\\[]", "");
                if (!JavaToKotlinTypeConverter.isBaseType(type) && !"void".equals(type)){
                    packageList.add("import "+type.replace("$",".")+(extension == FileTypeExtension.JAVA?";":""));
                }
            }else {
                packageList.add("import "+name.replace("$",".")+(extension == FileTypeExtension.JAVA?";":""));
            }
        }
    }

}