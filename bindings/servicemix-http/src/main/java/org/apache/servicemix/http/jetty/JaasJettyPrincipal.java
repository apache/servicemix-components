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
package org.apache.servicemix.http.jetty;

import org.eclipse.jetty.server.UserIdentity;

import javax.security.auth.Subject;
import java.security.Principal;

public class JaasJettyPrincipal implements UserIdentity, Principal {

    private final String _name;
    private final Subject _subject;
    private final String[] _roles;

    public JaasJettyPrincipal(Subject subject, String name, String[] roles)
    {
        _name = name;
        _subject=subject;
        _roles=roles;
    }

    public Subject getSubject()
    {
        return _subject;
    }

    public Principal getUserPrincipal()
    {
        return this;
    }

    public boolean isUserInRole(String role, Scope scope)
    {
        if (scope!=null && scope.getRoleRefMap()!=null)
            role=scope.getRoleRefMap().get(role);

        for (String r :_roles)
            if (r.equals(role))
                return true;
        return false;
    }

    @Override
    public String toString()
    {
        return JaasJettyPrincipal.class.getSimpleName()+"('"+_name+"')";
    }

    public String getName() {
        return _name;
    }
}
