/*
 *
 *  Copyright 2017 Kamiel Ahmadpour
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
 * @author Kamiel Ahmadpour - 2017
 */
public class ApplicationConfig {
    static final String APPLICATION_NAME = "AndroidAOP Code Viewer";
    private boolean isPublic = true;
    private boolean isProtected = true;
    private boolean isPackage = true;
    private boolean isPrivate = true;
    private ReplaceProxy replaceProxy = ReplaceProxy.NoneProxy;


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

}
