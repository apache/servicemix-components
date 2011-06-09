package org.apache.servicemix.http.endpoints;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.xml.sax.InputSource;

public class SpringWSDLLocator implements javax.wsdl.xml.WSDLLocator {
    
    private Resource base;
    private Resource latest;
    private Map<String, Resource> history;
    
    public SpringWSDLLocator(Resource main) {
        this.base = main;
        
        //Init the history.
        history = new HashMap<String, Resource>();
        history.put(getBaseURI(), main);
    }
    

    @Override
    public InputSource getBaseInputSource() {
        try {
            return new InputSource(base.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    

    @Override
    public InputSource getImportInputSource(String parentLocation, String importLocation) {
        try {
            Resource parent = history.get(parentLocation);
            if (parent != null) {
                latest = parent.createRelative(importLocation);
                history.put(getLatestImportURI(), latest);
                return new InputSource(latest.getInputStream());
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (UnsupportedOperationException nie) {
            throw nie;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    

    @Override
    public String getBaseURI() {
        try {
            return base.getURI().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    

    @Override
    public String getLatestImportURI() {
        try {
            if (latest != null) {
                return latest.getURI().toString();
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    

    @Override
    public void close() {
        latest = null;
        history.clear();
    }
    
}
