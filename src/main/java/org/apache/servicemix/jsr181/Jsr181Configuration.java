/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jsr181;

import org.apache.servicemix.common.PersistentConfiguration;

public class Jsr181Configuration extends PersistentConfiguration implements Jsr181ConfigurationMBean {

    private boolean printStackTraceInFaults;
    
    public boolean isPrintStackTraceInFaults() {
        return this.printStackTraceInFaults;
    }

    public void setPrintStackTraceInFaults(boolean printStackTraceInFaults) {
        this.printStackTraceInFaults = printStackTraceInFaults;
        save();
    }

    public void save() {
        properties.setProperty("printStackTraceInFaults", Boolean.toString(printStackTraceInFaults));
        super.save();
    }
    
    public boolean load() {
        if (super.load()) {
            if (properties.getProperty("printStackTraceInFaults") != null) {
                printStackTraceInFaults = Boolean.getBoolean(properties.getProperty("printStackTraceInFaults"));
            }
            return true;
        } else {
            return false;
        }
    }
    
}
