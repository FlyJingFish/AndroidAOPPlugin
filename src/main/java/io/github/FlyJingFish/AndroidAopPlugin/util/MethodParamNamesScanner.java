package io.github.FlyJingFish.AndroidAopPlugin.util;


import io.github.FlyJingFish.AndroidAopPlugin.config.ASMPluginComponent;
import io.github.FlyJingFish.AndroidAopPlugin.config.ApplicationConfig;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public class MethodParamNamesScanner {
    private final List<MethodNode> methods = new ArrayList<>();
    private final ClassNode cn = new ClassNode();
    private int initCount;

    public MethodParamNamesScanner(ClassReader cr){
        ApplicationConfig applicationConfig = ASMPluginComponent.getApplicationConfig();
        cr.accept(cn, 0);
        this.methods.addAll(cn.methods);
        Iterator<MethodNode> iterator = methods.iterator();

        while (iterator.hasNext()){
            MethodNode methodNode = iterator.next();
            int access = methodNode.access;
            boolean isPublic =  (access & Opcodes.ACC_PUBLIC) != 0;
            boolean isProtected =  (access & Opcodes.ACC_PROTECTED) != 0;
            boolean isPrivate =  (access & Opcodes.ACC_PRIVATE) != 0;
            boolean isPackage;
            if (!isPublic && !isProtected && !isPrivate){
                isPackage = true;
            }else {
                isPackage = false;
            }

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