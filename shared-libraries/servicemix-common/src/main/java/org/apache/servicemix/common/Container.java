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
package org.apache.servicemix.common;

import java.lang.reflect.Method;
import javax.jbi.component.ComponentContext;

import org.apache.servicemix.common.osgi.Configuration;
import org.apache.servicemix.executors.ExecutorFactory;
import org.apache.servicemix.executors.impl.ExecutorFactoryImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

public abstract class Container {

    public enum Type {
        ServiceMix3,
        ServiceMix4,
        Unknown
    }

    protected final ComponentContext context;

    protected Container(ComponentContext context) {
        this.context = context;
    }

    public String toString() {
        return getType().toString();
    }

    public abstract Type getType();

    public abstract boolean handleTransactions();

    public ExecutorFactory getExecutorFactory() {
        return new ExecutorFactoryImpl();
    }

    public ClassLoader getSharedLibraryClassLoader(String name) {
        throw new UnsupportedOperationException("Can not access shared libraries");
    }

    public ClassLoader getComponentClassLoader(String name) {
        throw new UnsupportedOperationException("Can not access components");
    }

    public static Container detect(ComponentContext context) {
        try {
            String clName = context.getClass().getName();
            if ("org.apache.servicemix.jbi.framework.ComponentContextImpl".equals(clName)) {
                return new Smx3Container(context);
            }
            if ("org.apache.servicemix.jbi.runtime.impl.ComponentContextImpl".equals(clName)) {
                return new Smx4Container(context);
            }
        } catch (Throwable t) {
        }
        return new UnknownContainer(context);
    }

    public static class Smx3Container extends Container {
        private boolean handleTransactions;
        private Object container;

        public Smx3Container(ComponentContext context) {
            super(context);
            try {
                Method getContainerMth = context.getClass().getMethod("getContainer");
                container = getContainerMth.invoke(context, new Object[0]);
            } catch (Throwable t) {
            }
            try {
                Method isUseNewTransactionModelMth = container.getClass().getMethod("isUseNewTransactionModel");
                Boolean b = (Boolean) isUseNewTransactionModelMth.invoke(container);
                handleTransactions = !b;
            } catch (Throwable t) {
                handleTransactions = true;
            }
        }

        public Type getType() {
            return Type.ServiceMix3;
        }

        public boolean handleTransactions() {
            return handleTransactions;
        }

        @Override
        public ExecutorFactory getExecutorFactory() {
            try {
                if (container != null) {
                    Method getWorkManagerMth = container.getClass().getMethod("getExecutorFactory", new Class[0]);
                    return (ExecutorFactory) getWorkManagerMth.invoke(container, new Object[0]);
                }
            } catch (Throwable t) {
                // ignore this
            }
            return null;
        }

        public Object getSmx3Container() {
            return container;
        }

        public ClassLoader getSharedLibraryClassLoader(String name) {
            try {
                Object registry = container.getClass().getMethod("getRegistry").invoke(container);
                Object sl = registry.getClass().getMethod("getSharedLibrary", String.class).invoke(registry, name);
                return (ClassLoader) sl.getClass().getMethod("getClassLoader").invoke(sl);
            } catch (Throwable t) {
                return null;
            }
        }

        public ClassLoader getComponentClassLoader(String name) {
            try {
                Object registry = container.getClass().getMethod("getRegistry").invoke(container);
                Object mbean = registry.getClass().getMethod("getComponent", String.class).invoke(registry, name);
                Object cmp = mbean.getClass().getMethod("getComponent").invoke(mbean);
                return cmp.getClass().getClassLoader();
            } catch (Throwable t) {
                return null;
            }
        }
    }

    public static class Smx4Container extends Container {

        public Smx4Container(ComponentContext context) {
            super(context);
        }

        public Type getType() {
            return Type.ServiceMix4;
        }

        public boolean handleTransactions() {
            return false;
        }

        @Override
        public ExecutorFactory getExecutorFactory() {
            final ExecutorFactoryImpl factory = (ExecutorFactoryImpl) super.getExecutorFactory();
            factory.setDefaultConfig(Configuration.getInstance().getExecutorConfig());
            return factory;
        }

        public ClassLoader getSharedLibraryClassLoader(String name) {
            try {
                BundleContext context = FrameworkUtil.getBundle(this.context.getClass()).getBundleContext();
                if (name.startsWith("osgi:")) {
                    name = name.substring("osgi:".length());
                    String symbolicName;
                    Version version;
                    if (name.indexOf('/') > 0) {
                        symbolicName = name.substring(0, name.indexOf('/'));
                        version = new Version(name.substring(name.indexOf('/') + 1));
                    } else {
                        symbolicName = name;
                        version = null;
                    }
                    for (Bundle b : context.getBundles()) {
                        if (symbolicName.equals(b.getSymbolicName()) && (version == null || version.equals(b.getVersion()))) {
                            return BundleDelegatingClassLoader.createBundleClassLoaderFor(b);
                        }
                    }
                    return null;
                } else {
                    ServiceReference[] references = context.getAllServiceReferences("org.apache.servicemix.jbi.deployer.SharedLibrary",
                            "(NAME=" + name + ")");
                    Object sl = context.getService(references[0]);
                    return (ClassLoader) sl.getClass().getMethod("getClassLoader").invoke(sl);
                }
            } catch (Throwable t) {
                return null;
            }
        }

        public ClassLoader getComponentClassLoader(String name) {
            try {
                BundleContext context = FrameworkUtil.getBundle(this.context.getClass()).getBundleContext();
                ServiceReference[] references = context.getAllServiceReferences("javax.jbi.component.Component",
                        "(NAME=" + name + ")");
                Object cmp = context.getService(references[0]);
                return cmp.getClass().getClassLoader();
            } catch (Throwable t) {
                return null;
            }
        }
    }

    public static class UnknownContainer extends Container {

        public UnknownContainer(ComponentContext context) {
            super(context);
        }

        public Type getType() {
            return Type.Unknown;
        }

        public boolean handleTransactions() {
            return false;
        }
    }

}
