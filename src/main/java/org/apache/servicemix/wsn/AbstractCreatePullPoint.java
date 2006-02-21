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
package org.apache.servicemix.wsn;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.apache.activemq.util.IdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.wsn.jaxws.CreatePullPoint;
import org.apache.servicemix.wsn.jaxws.UnableToCreatePullPointFault;
import org.apache.servicemix.wsn.jaxws.UnableToDestroyPullPointFault;
import org.oasis_open.docs.wsn.b_2.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType;
import org.w3c.dom.Element;

@WebService(endpointInterface = "org.apache.servicemix.wsn.jaxws.CreatePullPoint")
public abstract class AbstractCreatePullPoint extends AbstractEndpoint implements CreatePullPoint {

    private static Log log = LogFactory.getLog(AbstractCreatePullPoint.class);

    private IdGenerator idGenerator;

    private Map<String, AbstractPullPoint> pullPoints;

    public AbstractCreatePullPoint(String name) {
        super(name);
        idGenerator = new IdGenerator();
        pullPoints = new ConcurrentHashMap<String, AbstractPullPoint>();
    }

    public void init() throws Exception {
        register();
    }
    
    @Override
    protected String createAddress() {
        return "http://servicemix.org/wsnotification/CreatePullPoint/" + getName();
    }

    @WebMethod(operationName = "CreatePullPoint")
    @WebResult(name = "CreatePullPointResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "CreatePullPointResponse")
    public CreatePullPointResponse createPullPoint(
        @WebParam(name = "CreatePullPoint", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "CreatePullPointRequest")
        org.oasis_open.docs.wsn.b_2.CreatePullPoint createPullPointRequest)
            throws UnableToCreatePullPointFault {

        log.debug("CreatePullEndpoint");
        return handleCreatePullPoint(createPullPointRequest, null);
    }

    public CreatePullPointResponse handleCreatePullPoint(org.oasis_open.docs.wsn.b_2.CreatePullPoint createPullPointRequest, EndpointManager manager)
            throws UnableToCreatePullPointFault {
        AbstractPullPoint pullPoint = null;
        boolean success = false;
        try {
            pullPoint = createPullPoint(idGenerator.generateSanitizedId());
            for (Iterator it = createPullPointRequest.getAny().iterator(); it.hasNext();) {
                Element el = (Element) it.next();
                if ("address".equals(el.getLocalName())
                        && "http://servicemix.apache.org/wsn2005/1.0".equals(el.getNamespaceURI())) {
                    String address = DOMUtil.getElementText(el).trim();
                    pullPoint.setAddress(address);
                }
            }
            pullPoint.setCreatePullPoint(this);
            pullPoints.put(pullPoint.getAddress(), pullPoint);
            pullPoint.create(createPullPointRequest);
            if (manager != null) {
                pullPoint.setManager(manager);
            }
            pullPoint.register();
            CreatePullPointResponse response = new CreatePullPointResponse();
            response.setPullPoint(createEndpointReference(pullPoint.getAddress()));
            success = true;
            return response;
        } catch (EndpointRegistrationException e) {
            UnableToCreatePullPointFaultType fault = new UnableToCreatePullPointFaultType();
            throw new UnableToCreatePullPointFault("Unable to register new endpoint", fault, e);
        } finally {
            if (!success && pullPoint != null) {
                pullPoints.remove(pullPoint.getAddress());
                try {
                    pullPoint.destroy();
                } catch (UnableToDestroyPullPointFault e) {
                    log.info("Error destroying pullPoint", e);
                }
            }
        }
    }

    public void destroyPullPoint(String address) throws UnableToDestroyPullPointFault {
        AbstractPullPoint pullPoint = pullPoints.remove(address);
        if (pullPoint != null) {
            pullPoint.destroy();
        }
    }

    protected abstract AbstractPullPoint createPullPoint(String name);

}
