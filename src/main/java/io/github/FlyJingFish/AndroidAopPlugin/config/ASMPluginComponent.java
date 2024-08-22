/*
 *
 *  Copyright 2011 Cédric Champeau
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package io.github.FlyJingFish.AndroidAopPlugin.config;
/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 18/01/11
 * Time: 19:51
 * Updated by: Kamiel
 */

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import io.github.FlyJingFish.AndroidAopPlugin.common.Constants;

/**
 * A component created just to be able to configure the plugin.
 */
@State(
        name = Constants.COMPONENT_NAME,
        storages = {
                @Storage(file = "$PROJECT_FILE$")
        }
)
public class ASMPluginComponent implements PersistentStateComponent<Element> {

    private static ApplicationConfig applicationConfig = new ApplicationConfig();

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


