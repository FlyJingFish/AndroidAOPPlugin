package io.github.FlyJingFish.AndroidAopPlugin.util;

import io.github.FlyJingFish.AndroidAopPlugin.common.FileTypeExtension;
import io.github.FlyJingFish.AndroidAopPlugin.config.ASMPluginComponent;
import io.github.FlyJingFish.AndroidAopPlugin.config.ApplicationConfig;
import io.github.FlyJingFish.AndroidAopPlugin.config.ReplaceProxy;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AndroidAOPCode {
    private final MethodParamNamesScanner scanner;
    private final boolean useProxyMethod;
//    public static void main(String[] args) throws Exception {
//        ClassReader cr = new ClassReader("com.testdemo1.Demo");
//        AndroidAOPCode androidAOPCode = new AndroidAOPCode(cr);
//        StringWriter replaceJavaCode = androidAOPCode.getReplaceContent(cr,CodeStyle.JavaCode);
//        System.out.println("replaceJavaCode=\n"+replaceJavaCode);
//        StringWriter replaceKotlinCode = androidAOPCode.getReplaceContent(cr,CodeStyle.KotlinCode);
//        System.out.println("replaceKotlinCode=\n"+replaceKotlinCode);
//        StringWriter matchJavaCode = androidAOPCode.getMatchContent(cr);
//        System.out.println("replaceKotlinCode=\n"+matchJavaCode);
//    }

    public AndroidAOPCode(ClassReader cr) {
        scanner = new MethodParamNamesScanner(cr);
        ApplicationConfig applicationConfig = ASMPluginComponent.getApplicationConfig();
        useProxyMethod = applicationConfig.getReplaceProxy() == ReplaceProxy.Proxy;
    }

    private static final Map<String,String> javaKotlinMap = new HashMap<>();
    private static final Map<String,String> basejavaKotlinMap = new HashMap<>();
    private static final Map<String,String> basejavaKotlinArrayMap = new HashMap<>();

    static {
        basejavaKotlinArrayMap.put("int","IntArray");
        basejavaKotlinArrayMap.put("short","ShortArray");
        basejavaKotlinArrayMap.put("byte","ByteArray");
        basejavaKotlinArrayMap.put("char","CharArray");
        basejavaKotlinArrayMap.put("long","LongArray");
        basejavaKotlinArrayMap.put("float","FloatArray");
        basejavaKotlinArrayMap.put("double","DoubleArray");
        basejavaKotlinArrayMap.put("boolean","BooleanArray");

        basejavaKotlinMap.put("int","Int");
        basejavaKotlinMap.put("short","Short");
        basejavaKotlinMap.put("byte","Byte");
        basejavaKotlinMap.put("char","Char");
        basejavaKotlinMap.put("long","Long");
        basejavaKotlinMap.put("float","Float");
        basejavaKotlinMap.put("double","Double");
        basejavaKotlinMap.put("boolean","Boolean");

        javaKotlinMap.putAll(basejavaKotlinMap);
        javaKotlinMap.put("Integer","Int?");
        javaKotlinMap.put("Short","Short?");
        javaKotlinMap.put("Byte","Byte?");
        javaKotlinMap.put("Character","Char?");
        javaKotlinMap.put("Long","Long?");
        javaKotlinMap.put("Float","Float?");
        javaKotlinMap.put("Double","Double?");
        javaKotlinMap.put("Boolean","Boolean?");

        javaKotlinMap.put("void","");
        javaKotlinMap.put("Void","Unit?");
        javaKotlinMap.put("Object","Any");
    }

    public StringWriter getMatchContent(FileTypeExtension codeStyle,boolean allMethod,boolean useProxyMethod) {

        StringWriter stringWriter = new StringWriter();
        stringWriter.append("@AndroidAopMatchClassMethod(\n")
                .append("   targetClassName = \"").append(scanner.getClassName()).append("\",\n")
                .append("   type = MatchType.SELF,\n");
        if (codeStyle == FileTypeExtension.KOTLIN){
            stringWriter.append("   methodName = [");
        }else {
            stringWriter.append("   methodName = {");
        }

        if (allMethod){
            stringWriter.append("\"*\"");
        }else {
            List<String> methodName = new ArrayList<>();
            for (MethodNode method : scanner.getMethods()) {
                String name = getMatchJavaMethod(method.access,method.name,method.desc,method.signature,scanner);
                if (name != null){
                    methodName.add(name);
                }
            }

            for (int i = 0; i < methodName.size(); i++) {
                stringWriter.append(methodName.get(i));
                if (i != methodName.size() -1){
                    stringWriter.append(",");
                }
                if (i % 3 == 0){
                    stringWriter.append("\n");
                }
            }
        }



        if (codeStyle == FileTypeExtension.KOTLIN){
            stringWriter.append("]\n)\n");
            stringWriter.append("class Match")
                    .append(getShowMethodClassName(scanner.getClassName()))
                    .append(" : ").append(useProxyMethod?"MatchClassMethodProxy()":"MatchClassMethod").append("{\n\n");
        }else {
            stringWriter.append("}\n)\n");
            stringWriter.append("public class Match")
                    .append(getShowMethodClassName(scanner.getClassName()))
                    .append(useProxyMethod?" extends MatchClassMethodProxy":" implements MatchClassMethod").append("{\n\n");
        }



        stringWriter.append("}");

        return stringWriter;
    }


    public static String getMatchJavaMethod(int methodAccess, String methodName, String methodDescriptor,String signature,
                                            MethodParamNamesScanner scanner) {
        if (!"<clinit>".equals(methodName) && !"<init>".equals(methodName)){
            StringWriter stringWriter = new StringWriter();
            boolean isSuspendMethod = methodDescriptor.endsWith("Lkotlin/coroutines/Continuation;)Ljava/lang/Object;");

            String returnTypeClassName = Type.getReturnType(methodDescriptor).getClassName();

            Type[] types= Type.getArgumentTypes(methodDescriptor);

            stringWriter.append("\"");
            if (isSuspendMethod){
                stringWriter.append("suspend").append(" ");
            }else {
                stringWriter.append(returnTypeClassName).append(" ");
            }
            stringWriter.append(methodName).append("(");

            for (int i = 0; i < types.length; i++) {
                Type type = types[i];
                if (i == types.length - 1 && isSuspendMethod){
                    break;
                }
                stringWriter.append(type.getClassName());
                if ((isSuspendMethod && i < types.length - 2) || (!isSuspendMethod && i != types.length -1)){
                    stringWriter.append(",");
                }
            }
            stringWriter.append(")\"");
            return stringWriter.toString();
        }else {
            return null;
        }
    }

    public StringWriter getReplaceContent(FileTypeExtension codeStyle) {

        StringWriter stringWriter = new StringWriter();

        if (useProxyMethod){
            StringWriter matchContent = getMatchContent(codeStyle,true,true);
            stringWriter.append(matchContent.toString()).append("\n");
        }

        stringWriter.append("@AndroidAopReplaceClass(\"")
                .append(scanner.getClassName())
                .append("\")\n");
        if (codeStyle == FileTypeExtension.JAVA){
            stringWriter.append("public class Replace")
                    .append(getShowMethodClassName(scanner.getClassName()))
                    .append("{\n\n");
        }else {
            stringWriter.append("object Replace")
                    .append(getShowMethodClassName(scanner.getClassName()))
                    .append("{\n\n");
        }

        for (MethodNode method : scanner.getMethods()) {
            boolean isSuspendMethod = method.desc.endsWith("Lkotlin/coroutines/Continuation;)Ljava/lang/Object;");
            if (codeStyle == FileTypeExtension.JAVA){
                if (!isSuspendMethod){
                    getReplaceJavaMethod(method.access,method.name,method.desc,stringWriter,scanner);
                }
            }else {
                getReplaceKotlinMethod(method.access,method.name,method.desc,method.signature,stringWriter,scanner);
            }
        }

        stringWriter.append("}");

        return stringWriter;
    }


    public void getReplaceKotlinMethod(int methodAccess, String methodName, String methodDescriptor,String signature,
                                              StringWriter stringWriter, MethodParamNamesScanner scanner) {
        if (!"<clinit>".equals(methodName)){

            boolean isSuspendMethod = methodDescriptor.endsWith("Lkotlin/coroutines/Continuation;)Ljava/lang/Object;");
            boolean isInit = "<init>".equals(methodName);
            String returnTypeClassName = Type.getReturnType(methodDescriptor).getClassName();
            if (isSuspendMethod){
                returnTypeClassName = getSuspendMethodType(signature);
            }else if (isInit){
                returnTypeClassName = scanner.getClassName();
            }


            Type[] types= Type.getArgumentTypes(methodDescriptor);
            List<String> argNameList = scanner.getParamNames(
                    methodName,
                    methodDescriptor,
                    types.length
            );

            boolean isStatic = (methodAccess & Opcodes.ACC_STATIC) != 0;

            stringWriter.append("@JvmStatic\n");
            if (useProxyMethod){
                stringWriter.append("@ProxyMethod(proxyClass = ")
                        .append(getShowMethodClassName(scanner.getClassName()))
                        .append("::class,type = ")
                        .append(isStatic?"ProxyType.STATIC_METHOD":(isInit?"ProxyType.INIT":"ProxyType.METHOD"))
                        .append(")\n");
            }
            stringWriter.append("@AndroidAopReplaceMethod(\"");
            if (isSuspendMethod){
                stringWriter.append("suspend").append(" ");
            }else if (!isInit){
                stringWriter.append(returnTypeClassName).append(" ");
            }
            stringWriter.append(methodName).append("(");

            for (int i = 0; i < types.length; i++) {
                Type type = types[i];
                if (i == types.length - 1 && isSuspendMethod){
                    break;
                }
                stringWriter.append(type.getClassName().replaceAll("\\$","."));
                if ((isSuspendMethod && i < types.length - 2) || (!isSuspendMethod && i != types.length -1)){
                    stringWriter.append(",");
                }
            }
            stringWriter.append(")");

            stringWriter.append("\")");

            if (isSuspendMethod){
                stringWriter.append("\nsuspend fun ");
            }else {
                stringWriter.append("\nfun ");
            }
            if (isInit){
                stringWriter.append("get").append(getShowMethodKotlinClassName(scanner.getClassName())).append(scanner.getInitCount()+"");
            }else {
                stringWriter.append(methodName);
            }
            stringWriter.append("(");
            if (!isStatic){
                stringWriter.append("thisObj: ");
                stringWriter.append(getShowMethodKotlinClassName(scanner.getClassName()));
                if (types.length > 0 && !isInit){
                    if (isSuspendMethod){
                        if (types.length > 1){
                            stringWriter.append(",");
                        }
                    }else {
                        stringWriter.append(",");
                    }
                }
            }
            if (!isInit){
                for (int i = 0; i < types.length; i++) {
                    Type type = types[i];
                    if (isSuspendMethod && i == types.length - 1){
                        break;
                    }
                    stringWriter.append(argNameList.get(i)).append(": ");
                    stringWriter.append(getShowMethodKotlinClassName(type.getClassName()));
                    if ((isSuspendMethod && i < types.length - 2) || (!isSuspendMethod && i != types.length -1)){
                        stringWriter.append(",");
                    }
                }
            }
            String returnStr = getShowMethodKotlinClassName(returnTypeClassName);
            if ("".equals(returnStr)){
                stringWriter.append(")");
            }else {
                stringWriter.append("):");
            }

            stringWriter.append(returnStr);
            stringWriter.append("{\n");

            StringBuffer args = new StringBuffer();
            for (int i = 0; i < argNameList.size(); i++) {
                if (i == types.length - 1 && isSuspendMethod){
                    break;
                }
                args.append(argNameList.get(i));
                if ((isSuspendMethod && i < types.length - 2) || (!isSuspendMethod && i != types.length -1)){
                    args.append(",");
                }
            }

            if (!"void".equals(returnTypeClassName) || isInit){
                stringWriter.append("   return ");

            }else {
                stringWriter.append("   ");
            }
            if (isStatic){
                stringWriter
                        .append(getShowMethodKotlinClassName(scanner.getClassName()));
            }else {
                stringWriter.append("thisObj");
            }
            if (!isInit){
                stringWriter.append(".")
                        .append(methodName)
                        .append("(")
                        .append(args)
                        .append(")");
            }

            stringWriter.append("\n}\n\n");
        }
    }

    public void getReplaceJavaMethod(int methodAccess, String methodName, String methodDescriptor,
                                            StringWriter stringWriter, MethodParamNamesScanner scanner) {
        if (!"<clinit>".equals(methodName)){
            boolean isInit = "<init>".equals(methodName);
            Type returnType = Type.getReturnType(methodDescriptor);
            Type[] types= Type.getArgumentTypes(methodDescriptor);
            List<String> argNameList = scanner.getParamNames(
                    methodName,
                    methodDescriptor,
                    types.length
            );

            boolean isStatic = (methodAccess & Opcodes.ACC_STATIC) != 0;
            boolean isSuspendMethod = methodDescriptor.endsWith("Lkotlin/coroutines/Continuation;)Ljava/lang/Object;");
            if (useProxyMethod){
                stringWriter.append("@ProxyMethod(proxyClass = ")
                        .append(getShowMethodClassName(scanner.getClassName()))
                        .append(".class,type = ")
                        .append(isStatic?"ProxyType.STATIC_METHOD":(isInit?"ProxyType.INIT":"ProxyType.METHOD"))
                        .append(")\n");
            }
            stringWriter.append("@AndroidAopReplaceMethod(\"");
            if (isSuspendMethod){
                stringWriter.append("suspend").append(" ");
            }else if (!isInit){
                stringWriter.append(returnType.getClassName()).append(" ");
            }
            stringWriter.append(methodName).append("(");

            for (int i = 0; i < types.length; i++) {
                Type type = types[i];
                stringWriter.append(type.getClassName().replaceAll("\\$","."));
                if (i != types.length -1){
                    stringWriter.append(",");
                }
            }
            stringWriter.append(")");

            stringWriter.append("\")");

            stringWriter.append("\npublic static ");
            if (isInit){
                stringWriter.append(getShowMethodClassName(scanner.getClassName()));
            }else {
                stringWriter.append(getShowMethodClassName(returnType.getClassName()));
            }
            stringWriter.append(" ");
            if (isInit){
                stringWriter.append("get").append(getShowMethodClassName(scanner.getClassName())).append(scanner.getInitCount()+"");
            }else {
                stringWriter.append(methodName);
            }
            stringWriter.append("(");
            if (!isStatic){
                stringWriter.append(getShowMethodClassName(scanner.getClassName()));
                stringWriter.append(" thisObj");
                if (types.length > 0 && !isInit){
                    stringWriter.append(",");
                }
            }
            if (!isInit){
                for (int i = 0; i < types.length; i++) {
                    Type type = types[i];
                    stringWriter.append(getShowMethodClassName(type.getClassName()));
                    stringWriter.append(" ").append(argNameList.get(i));
                    if (i != types.length -1){
                        stringWriter.append(",");
                    }
                }
            }
            stringWriter.append(")");
            stringWriter.append("{\n");

            StringBuffer args = new StringBuffer();
            for (int i = 0; i < argNameList.size(); i++) {
                args.append(argNameList.get(i));
                if (i != argNameList.size() - 1){
                    args.append(",");
                }
            }

            if (!"void".equals(returnType.getClassName()) || isInit){
                stringWriter.append("   return ");

            }else {
                stringWriter.append("   ");
            }
            if (isStatic){
                stringWriter
                        .append(getShowMethodClassName(scanner.getClassName()));
            }else {
                stringWriter.append("thisObj");
            }
            if (!isInit){
                stringWriter.append(".")
                        .append(methodName)
                        .append("(")
                        .append(args)
                        .append(")");
            }

            stringWriter.append(";\n}\n\n");
        }
    }


    private static String getShowMethodClassName(String className){
        if (className.contains(".")){
            return className.replaceAll("\\$",".").substring(className.lastIndexOf(".")+1);
        }else {
            return className;
        }
    }

    private static String getShowMethodKotlinClassName(String className){
        String showName = getShowMethodClassName(className);
        String kotlinName = javaKotlinMap.get(showName);
        if (kotlinName == null){
            if (showName.contains("[]")){
                String type = showName.replaceAll("\\[]","");
                boolean isBaseType = basejavaKotlinMap.containsKey(type);
                String subStr = "[]";
                int count = 0;
                int index = 0;
                while ((index = showName.indexOf(subStr, index)) != -1) {
                    index += subStr.length();
                    count++;
                }
                if (isBaseType){
                    if (count == 1){
                        return basejavaKotlinArrayMap.get(type);
                    }else {
                        StringBuilder stringBuffer = new StringBuilder();
                        for (int i = 0; i < count - 1; i++) {
                            stringBuffer.append("Array<");
                        }
                        stringBuffer.append(basejavaKotlinArrayMap.get(type));
                        for (int i = 0; i < count - 1; i++) {
                            stringBuffer.append(">");
                        }
                        return stringBuffer.toString();
                    }
                }else {
                    StringBuilder stringBuffer = new StringBuilder();
                    for (int i = 0; i < count; i++) {
                        stringBuffer.append("Array<");
                    }
                    stringBuffer.append(type);
                    for (int i = 0; i < count; i++) {
                        stringBuffer.append(">");
                    }
                    return stringBuffer.toString();
                }
            }else {
                return showName;
            }
        }else {
            return kotlinName;
        }

    }


    private static final Pattern signatureClassnamePattern = Pattern.compile("\\(.*?kotlin/coroutines/Continuation<-.*?>;\\)Ljava/lang/Object;");
    private static final Pattern signatureClassnamePattern1 = Pattern.compile("\\(.*?kotlin/coroutines/Continuation<-.*?");
    static String getSuspendMethodType(String type) {
        return getType(type,signatureClassnamePattern,signatureClassnamePattern1,">;)Ljava/lang/Object;");
    }

    private static String getType(String type, Pattern classnamePattern, Pattern classnamePattern1, String replaceText) {
        if (type == null){
            return null;
        }
        String className = null;
        Matcher matcher = classnamePattern.matcher(type);
        if (matcher.find()) {
            String type2 = matcher.group();
            Matcher matcher1 = classnamePattern1.matcher(type2);
            if (matcher1.find()) {
                String realType = matcher1.replaceFirst("");
                Matcher realMatcher = classnamePattern.matcher(realType);
                String realTypeClass;
                if (realMatcher.find()) {
                    realTypeClass = realMatcher.replaceFirst("");
                } else {
                    realTypeClass = realType.replace(replaceText, "");
                }
                className = realTypeClass;
            }
        }
        if (className != null){
            className = getSeeClassName(className);
            Matcher fanMatcher = fanClassnamePattern.matcher(className);
            if (fanMatcher.find()) {
                className = fanMatcher.replaceAll("");
            }
        }
        return className;
    }
    private static final Pattern classnameArrayPattern = Pattern.compile("\\[");
    private static final Pattern fanClassnamePattern = Pattern.compile("<.*?>$");
    private static final Pattern fanClassnamePattern2 = Pattern.compile("<.*?>;$");
    private static String getSeeClassName(String className) {
        if (classnameArrayPattern.matcher(className).find()) {
            return getArrayClazzName(className);
        } else {
            return className.substring(1).replace("/",".").replace(";","");
        }
    }

    private static String getArrayClazzName(String classname) {
        String subStr = "[";
        int count = 0;
        int index = 0;
        while ((index = classname.indexOf(subStr, index)) != -1) {
            index += subStr.length();
            count++;
        }
        String realClassName = classnameArrayPattern.matcher(classname).replaceAll("");
        Matcher matcher = fanClassnamePattern2.matcher(realClassName);
        String clazzName ;
        if (matcher.find()) {
            clazzName = matcher.replaceAll("");
        }else{
            clazzName = realClassName;
        }
        StringBuilder result = new StringBuilder(getTypeInternal(clazzName));
        for (int i = 0; i < count; i++) {
            result.append("[]");
        }
        return result.toString();
    }


    private static String getTypeInternal(String classname) {
        if ("Z".equals(classname)){
            return "boolean";
        }else if ("C".equals(classname)){
            return "char";
        }else if ("B".equals(classname)){
            return "byte";
        }else if ("S".equals(classname)){
            return "short";
        }else if ("I".equals(classname)){
            return "int";
        }else if ("F".equals(classname)){
            return "float";
        }else if ("J".equals(classname)){
            return "long";
        }else if ("D".equals(classname)){
            return "double";
        }else {
            return classname.substring(1).replace("/",".").replace(";","");
        }
    }

}
