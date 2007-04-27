/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.http;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.framework.ComponentMBeanImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * This servlet is meant to be used when embedding ServiceMix in a web application so that so servicemix-http component
 * will reuse the web container.
 * 
 * @author gnodet
 */
public class HttpManagedServlet extends javax.servlet.http.HttpServlet {

    public static final String CONTAINER_PROPERTY = "container";
    public static final String CONTAINER_DEFAULT = "jbi";

    public static final String COMPONENT_PROPERTY = "component";
    public static final String COMPONENT_DEFAULT = "servicemix-http";

    public static final String MAPPING_PROPERTY = "mapping";

    private HttpProcessor processor;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Retrieve spring application context
        ApplicationContext applicationContext = WebApplicationContextUtils
                        .getRequiredWebApplicationContext(getServletContext());

        // Retrieve
        String containerName = config.getInitParameter(CONTAINER_PROPERTY);
        if (containerName == null) {
            containerName = CONTAINER_DEFAULT;
        }
        JBIContainer container = (JBIContainer) applicationContext.getBean(containerName);
        if (container == null) {
            throw new IllegalStateException("Unable to find jbi container " + containerName);
        }
        String componentName = config.getInitParameter(COMPONENT_PROPERTY);
        if (componentName == null) {
            componentName = COMPONENT_DEFAULT;
        }
        ComponentMBeanImpl componentMBean = container.getComponent(componentName);
        if (componentMBean == null) {
            throw new IllegalStateException("Unable to find component " + componentName);
        }
        HttpComponent component = (HttpComponent) componentMBean.getComponent();
        String mapping = config.getInitParameter(MAPPING_PROPERTY);
        if (mapping != null) {
            component.getConfiguration().setMapping(mapping);
        }
        processor = component.getMainProcessor();
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                    IOException {
        try {
            processor.process(request, response);
        } catch (IOException e) {
            throw e;
        } catch (ServletException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Failed to process request: " + e, e);
        }
    }
}
