
package io.github.FlyJingFish.AndroidAopPlugin.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import io.github.FlyJingFish.AndroidAopPlugin.common.Constants;

@State(
        name = Constants.COMPONENT_NAME,
        storages = {
                @Storage(file = "$PROJECT_FILE$")
        }
)
public class AOPPluginComponent implements PersistentStateComponent<Element> {

    private static final ApplicationConfig applicationConfig = new ApplicationConfig();

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
    }


    // Property methods
    public static ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

}


