package io.github.FlyJingFish.AndroidAopPlugin.config;

public class ApplicationConfig {
    static final String APPLICATION_NAME = "AndroidAOP Code Viewer";
    private boolean isPublic = true;
    private boolean isProtected = true;
    private boolean isPackage = true;
    private boolean isPrivate = true;
    private ReplaceProxy replaceProxy = ReplaceProxy.NoneProxy;
    private CopyAnnotation copyAnnotation = CopyAnnotation.NoneCopy;
    private ImportPackage importPackage = ImportPackage.NoneImport;


    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean aProtected) {
        this.isProtected = aProtected;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        this.isPublic = aPublic;
    }

    public boolean isPackage() {
        return isPackage;
    }

    public void setPackage(boolean aPackage) {
        this.isPackage = aPackage;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        this.isPrivate = aPrivate;
    }

    public ReplaceProxy getReplaceProxy() {
        return replaceProxy;
    }

    public void setReplaceProxy(ReplaceProxy replaceProxy) {
        this.replaceProxy = replaceProxy;
    }

    public void setCopyAnnotation(CopyAnnotation copyAnnotation) {
        this.copyAnnotation = copyAnnotation;
    }

    public CopyAnnotation getCopyAnnotation() {
        return copyAnnotation;
    }

    public ImportPackage getImportPackage() {
        return importPackage;
    }

    public void setImportPackage(ImportPackage importPackage) {
        this.importPackage = importPackage;
    }
}
