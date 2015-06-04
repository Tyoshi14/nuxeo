/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.jsf;

import java.io.IOException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.sun.faces.config.WebConfiguration;
import com.sun.faces.renderkit.html_basic.ScriptStyleBaseRenderer;

/**
 * Base class for web resources resolution.
 *
 * @since 7.3
 */
public abstract class AbstractResourceRenderer extends ScriptStyleBaseRenderer {

    protected abstract void startElement(ResponseWriter writer, UIComponent component) throws IOException;

    protected abstract void endElement(ResponseWriter writer) throws IOException;

    protected abstract void encodeEnd(FacesContext context, UIComponent component, String src) throws IOException;

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        Map<String, Object> attributes = component.getAttributes();
        String src = (String) attributes.get("src");
        String url;
        if (src != null) {
            String value = context.getApplication().getViewHandler().getResourceURL(context, src);
            url = context.getExternalContext().encodeResourceURL(value);
        } else {
            String name = (String) attributes.get("name");
            String library = (String) attributes.get("library");
            url = resolveResource(context, component, library, name);
        }
        encodeEnd(context, component, url);
        super.encodeEnd(context, component);
    }

    protected String resolveResource(FacesContext context, UIComponent component, String library, String name) {
        Map<Object, Object> contextMap = context.getAttributes();

        String key = name + library;

        if (null == name) {
            return null;
        }

        // Ensure this import is not rendered more than once per request
        if (contextMap.containsKey(key)) {
            return null;
        }
        contextMap.put(key, Boolean.TRUE);

        // Special case of scripts that have query strings
        // These scripts actually use their query strings internally, not externally
        // so we don't need the resource to know about them
        int queryPos = name.indexOf("?");
        String query = null;
        if (queryPos > -1 && name.length() > queryPos) {
            query = name.substring(queryPos + 1);
            name = name.substring(0, queryPos);
        }

        Resource resource = context.getApplication().getResourceHandler().createResource(name, library);

        String resourceSrc = "RES_NOT_FOUND";

        WebConfiguration webConfig = WebConfiguration.getInstance();

        if (library == null
                && name != null
                && name.startsWith(webConfig.getOptionValue(WebConfiguration.WebContextInitParameter.WebAppContractsDirectory))) {

            if (context.isProjectStage(ProjectStage.Development)) {

                String msg = "Illegal path, direct contract references are not allowed: " + name;
                context.addMessage(component.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR, msg,
                        msg));
            }
            resource = null;
        }

        if (resource == null) {

            if (context.isProjectStage(ProjectStage.Development)) {
                String msg = "Unable to find resource " + (library == null ? "" : library + ", ") + name;
                context.addMessage(component.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR, msg,
                        msg));
            }

        } else {
            resourceSrc = resource.getRequestPath();
            if (query != null) {
                resourceSrc = resourceSrc + ((resourceSrc.indexOf("?") > -1) ? "&amp;" : "?") + query;
            }
            resourceSrc = context.getExternalContext().encodeResourceURL(resourceSrc);
        }

        return resourceSrc;
    }

}
