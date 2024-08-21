package io.github.FlyJingFish.AndroidAopPlugin.util;


import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public class MethodParamNamesScanner {
    private final List<MethodNode> methods;
    private final ClassNode cn = new ClassNode();
    private int initCount;

    public MethodParamNamesScanner(ClassReader cr){
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(cn, 0);
        this.methods = cn.methods;
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