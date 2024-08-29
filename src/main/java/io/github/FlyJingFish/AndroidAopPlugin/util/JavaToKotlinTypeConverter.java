package io.github.FlyJingFish.AndroidAopPlugin.util;


import java.util.*;

public class JavaToKotlinTypeConverter {

    private static final Map<String,String> basejavaKotlinMap = new HashMap<>();
    private static final Map<String, String> javaToKotlinMap = new HashMap<>();
    private static final Set<String> baseKotlinSet = new HashSet<>();
    private static final Map<String,String> baseKotlinArrayMap = new HashMap<>();

    static {
        baseKotlinArrayMap.put("Int","IntArray");
        baseKotlinArrayMap.put("Short","ShortArray");
        baseKotlinArrayMap.put("Byte","ByteArray");
        baseKotlinArrayMap.put("Char","CharArray");
        baseKotlinArrayMap.put("Long","LongArray");
        baseKotlinArrayMap.put("Float","FloatArray");
        baseKotlinArrayMap.put("Double","DoubleArray");
        baseKotlinArrayMap.put("Boolean","BooleanArray");

        basejavaKotlinMap.put("int","Int");
        basejavaKotlinMap.put("short","Short");
        basejavaKotlinMap.put("byte","Byte");
        basejavaKotlinMap.put("char","Char");
        basejavaKotlinMap.put("long","Long");
        basejavaKotlinMap.put("float","Float");
        basejavaKotlinMap.put("double","Double");
        basejavaKotlinMap.put("boolean","Boolean");

        baseKotlinSet.add("Int");
        baseKotlinSet.add("Short");
        baseKotlinSet.add("Byte");
        baseKotlinSet.add("Char");
        baseKotlinSet.add("Long");
        baseKotlinSet.add("Float");
        baseKotlinSet.add("Double");
        baseKotlinSet.add("Boolean");

        javaToKotlinMap.putAll(basejavaKotlinMap);

        javaToKotlinMap.put("java.lang.Integer", "Int");
        javaToKotlinMap.put("java.lang.Boolean", "Boolean");
        javaToKotlinMap.put("java.lang.Double", "Double");
        javaToKotlinMap.put("java.lang.Float", "Float");
        javaToKotlinMap.put("java.lang.Long", "Long");
        javaToKotlinMap.put("java.lang.Short", "Short");
        javaToKotlinMap.put("java.lang.Byte", "Byte");
        javaToKotlinMap.put("java.lang.Character", "Char");

        javaToKotlinMap.put("java.lang.String", "String");
        javaToKotlinMap.put("java.util.List", "List");
        javaToKotlinMap.put("java.util.ArrayList", "MutableList");
        javaToKotlinMap.put("java.util.Map", "Map");
        javaToKotlinMap.put("java.util.HashMap", "MutableMap");
        javaToKotlinMap.put("java.util.Set", "Set");
        javaToKotlinMap.put("java.util.HashSet", "MutableSet");
        javaToKotlinMap.put("java.util.Iterator", "Iterator");
        javaToKotlinMap.put("java.util.Collection", "Collection");

        javaToKotlinMap.put("void","");
        javaToKotlinMap.put("java.lang.Void","Unit?");
        javaToKotlinMap.put("java.lang.Object","Any");

        javaToKotlinMap.put("java.util.concurrent.Future", "kotlin.concurrent.Future");
        javaToKotlinMap.put("java.util.concurrent.CompletableFuture", "kotlin.concurrent.CompletableFuture");
    }

    private static String getShowMethodClassName(String className){
        if (className.contains(".")){
            String newName = className.replaceAll("\\$",".");
            return newName.substring(newName.lastIndexOf(".")+1);
        }else {
            return className;
        }
    }

    public static String getShowMethodKotlinClassName(String className){
        String showName = getShowMethodClassName(className);
        String kotlinName = javaToKotlinMap.get(showName);
        return Objects.requireNonNullElse(kotlinName, showName);

    }

    public static String convertJavaTypeToKotlin(String javaType) {

        // 替换为 Kotlin 类型
        for (Map.Entry<String, String> entry : javaToKotlinMap.entrySet()) {
            javaType = javaType.replace(entry.getKey(), entry.getValue());
        }

        // 处理数组类型 (e.g., String[] -> Array<String>)
//        javaType = javaType.replaceAll("(\\w+)\\[\\]", "Array<$1>");
        javaType = getKotlinArray(javaType);

        return removePackageNames(javaType).replace("? extends","out")
                .replace("? super","in");
    }

    private static String getKotlinArray(String showName){
        if (showName.contains("[]")){
            String type = showName.replaceAll("\\[]","");
            boolean isBaseType = baseKotlinSet.contains(type);
            String subStr = "[]";
            int count = 0;
            int index = 0;
            while ((index = showName.indexOf(subStr, index)) != -1) {
                index += subStr.length();
                count++;
            }
            if (isBaseType){
                if (count == 1){
                    return baseKotlinArrayMap.get(type);
                }else {
                    return "Array<".repeat(Math.max(0, count - 1)) +
                            baseKotlinArrayMap.get(type) +
                            ">".repeat(Math.max(0, count - 1));
                }
            }else {
                return "Array<".repeat(Math.max(0, count)) +
                        type +
                        ">".repeat(Math.max(0, count));
            }
        }else {
            return showName;
        }

    }

    public static String removePackageNames(String input) {
        // 使用正则表达式匹配包名，并将其替换为空字符串
        return input.replaceAll("\\b[a-zA-Z_][a-zA-Z0-9_]*\\.", "")
                .replaceAll("\\b[a-zA-Z_][a-zA-Z0-9_]*\\$","");
    }

//    public static void main(String[] args) {
//        String javaType = "java.util.List<java.lang.String>";
//        String kotlinType = removePackageNames(javaType);
//        System.out.println(kotlinType);  // 输出 "List<List<Array<String>>>"
//
////        String javaType = "java.util.regex.Pattern";
////        String kotlinType = removePackageNames(javaType);
////        System.out.println(kotlinType);  // 输出 "List<List<Array<String>>>"
//    }

    public static boolean isBaseType(String name){
        return basejavaKotlinMap.containsKey(name) && !name.contains(".");
    }

    public static boolean isImport(String name){
        return !isBaseType(name) && !"void".equals(name) && !name.startsWith("java.lang.");
    }
}

