
package io.github.FlyJingFish.AndroidAopPlugin.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import io.github.FlyJingFish.AndroidAopPlugin.common.Constants;

@State(
        name = Constants.COMPONENT_NAME,
        storages = {@Storage(
                value = "AOPPluginComponent.xml"
        )}
)
public class AOPPluginComponent implements PersistentStateComponent<Element> {

    private ApplicationConfig applicationConfig = new ApplicationConfig();
    private static AOPPluginComponent aopPluginComponent;

    public static AOPPluginComponent getInstance() {
        if (aopPluginComponent == null) {
            aopPluginComponent = ServiceManager.getService(AOPPluginComponent.class);
        }
        return aopPluginComponent;
    }

    public static AOPPluginComponent getInstanceForce() {
        return ServiceManager.getService(AOPPluginComponent.class);
    }

    @Override
    public Element getState() {
        Element root = new Element("state");
        Element modifiersType = new Element("modifiers");
        modifiersType.setAttribute("isPublic", String.valueOf(applicationConfig.isPublic()));
        modifiersType.setAttribute("isProtected", String.valueOf(applicationConfig.isProtected()));
        modifiersType.setAttribute("isPackage", String.valueOf(applicationConfig.isPackage()));
        modifiersType.setAttribute("isPrivate", String.valueOf(applicationConfig.isPrivate()));
        root.addContent(modifiersType);
        Element proxy = new Element("proxy");
        proxy.setAttribute("proxyType", applicationConfig.getReplaceProxy().toString());
        root.addContent(proxy);

        Element anno = new Element("anno");
        anno.setAttribute("annoType", applicationConfig.getCopyAnnotation().toString());
        root.addContent(anno);

        Element importPackage = new Element("import");
        importPackage.setAttribute("importPackage", applicationConfig.getImportPackage().toString());
        root.addContent(importPackage);
        return root;
    }

    @Override
    public void loadState(final Element state) {
        Element modifiersType = state.getChild("modifiers");
        if (modifiersType != null) {
            final String skipDebugStr = modifiersType.getAttributeValue("isPublic");
            if (skipDebugStr != null) applicationConfig.setPublic(Boolean.valueOf(skipDebugStr));
            final String skipFramesStr = modifiersType.getAttributeValue("isProtected");
            if (skipFramesStr != null) applicationConfig.setProtected(Boolean.valueOf(skipFramesStr));
            final String skipCodeStr = modifiersType.getAttributeValue("isPackage");
            if (skipCodeStr != null) applicationConfig.setPackage(Boolean.valueOf(skipCodeStr));
            final String expandFramesStr = modifiersType.getAttributeValue("isPrivate");
            if (expandFramesStr != null) applicationConfig.setPrivate(Boolean.valueOf(expandFramesStr));
        }
        Element proxy = state.getChild("proxy");
        if (proxy != null) {
            String codeStyleStr = proxy.getAttributeValue("proxyType");
            if (codeStyleStr != null) applicationConfig.setReplaceProxy(ReplaceProxy.valueOf(codeStyleStr));
        }

        Element anno = state.getChild("anno");
        if (anno != null) {
            String codeStyleStr = anno.getAttributeValue("annoType");
            if (codeStyleStr != null) applicationConfig.setCopyAnnotation(CopyAnnotation.valueOf(codeStyleStr));
        }

        Element importPackage = state.getChild("import");
        if (importPackage != null) {
            String codeStyleStr = importPackage.getAttributeValue("importPackage");
            if (codeStyleStr != null) applicationConfig.setImportPackage(ImportPackage.valueOf(codeStyleStr));
        }
    }


    // Property methods
    public static ApplicationConfig getApplicationConfig() {
        return getInstance().applicationConfig;
    }

}


