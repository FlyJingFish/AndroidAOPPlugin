package io.github.FlyJingFish.AndroidAopPlugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.FlyJingFish.AndroidAopPlugin.action.ClassFileLocationKt;
import org.objectweb.asm.ClassReader;

public enum CurrentFileUtils {
    INSTANCE;
    private ClassReader reader;
    private VirtualFile file;
    private Project project;

    public void setReader(ClassReader reader) {
        this.reader = reader;
    }

    public ClassReader getReader() {
        return reader;
    }

    public VirtualFile getFile() {
        return file;
    }

    public void setFile(VirtualFile file) {
        this.file = file;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void showCode(){
        if (project != null && file != null && reader != null){
            ClassFileLocationKt.showCode(project,file,reader);
        }
    }
}
