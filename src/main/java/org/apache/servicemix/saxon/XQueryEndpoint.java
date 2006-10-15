package org.apache.servicemix.saxon;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;

import org.apache.servicemix.jbi.jaxp.BytesSource;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.springframework.core.io.Resource;

/**
 * @org.apache.xbean.XBean element="xquery"
 */
public class XQueryEndpoint extends SaxonEndpoint {

    private static final Properties EMPTY_PROPS = new Properties();
    
    private String query;
    private XQueryExpression exp;
    private StaticQueryContext staticEnv;
    private Properties outputProperties;
    
    // Properties
    // -------------------------------------------------------------------------

    /**
     * @return the outputProperties
     */
    public Properties getOutputProperties() {
        return outputProperties;
    }

    /**
     * @param outputProperties the outputProperties to set
     */
    public void setOutputProperties(Properties outputProperties) {
        this.outputProperties = outputProperties;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     * @org.apache.xbean.Flat
     */
    public void setQuery(String query) {
        this.query = query;
    }

    // Interface methods
    // -------------------------------------------------------------------------
    
    public void start() throws Exception {
        Configuration config = getConfiguration();
        if (config == null) {
            config = new Configuration();
        }
        config.setHostLanguage(Configuration.XQUERY);
        setConfiguration(config);
        staticEnv = new StaticQueryContext(config);
        if (getQuery() != null) {
            exp = staticEnv.compileQuery(getQuery());
            staticEnv = exp.getStaticContext();
        } else if (getResource() != null) {
            exp = staticEnv.compileQuery(getResource().getInputStream(), null);
            staticEnv = exp.getStaticContext();
        }
    }
    
    public void validate() throws DeploymentException {
        if (getQuery() == null && getResource() == null && getExpression() == null) {
            throw new DeploymentException("query, resource or expression should be specified");
        }
    }

    // Implementation methods
    // -------------------------------------------------------------------------
    
    protected void transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        XQueryExpression expression = createQuery(exchange, in);
        queryContent(expression, exchange, in, out);
    }
    
    protected void queryContent(XQueryExpression expression, MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        Configuration config = getConfiguration();
        DynamicQueryContext dynamicEnv = new DynamicQueryContext(config);
        DocumentInfo doc = staticEnv.buildDocument(in.getContent());
        dynamicEnv.setContextItem(doc);
        configureQuery(dynamicEnv, exchange, in);
        Properties props = outputProperties != null ? outputProperties : EMPTY_PROPS; 
        if (RESULT_BYTES.equalsIgnoreCase(getResult())) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Result result = new StreamResult(buffer);
            expression.pull(dynamicEnv, result, props);
            out.setContent(new BytesSource(buffer.toByteArray()));
        }
        else if (RESULT_STRING.equalsIgnoreCase(getResult())) {
            StringWriter buffer = new StringWriter();
            Result result = new StreamResult(buffer);
            expression.pull(dynamicEnv, result, props);
            out.setContent(new StringSource(buffer.toString()));
        } else {
            DOMResult result = new DOMResult();
            expression.pull(dynamicEnv, result, props);
            out.setContent(new DOMSource(result.getNode()));
        }
    }
    
    protected void configureQuery(DynamicQueryContext dynamicEnv, MessageExchange exchange, NormalizedMessage in) throws Exception {
        for (Iterator iter = exchange.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = exchange.getProperty(name);
            dynamicEnv.setParameter(name, value);
        }
        for (Iterator iter = in.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = in.getProperty(name);
            dynamicEnv.setParameter(name, value);
        }
        Map parameters = getParameters();
        if (parameters != null) {
            for (Iterator iter = parameters.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                Object value = parameters.get(name);
                dynamicEnv.setParameter(name, value);
            }
        }
        dynamicEnv.setParameter("exchange", exchange);
        dynamicEnv.setParameter("in", in);
        dynamicEnv.setParameter("component", this);
    }
    
    protected XQueryExpression createQuery(MessageExchange exchange, NormalizedMessage in) throws Exception {
        if (getExpression() != null) {
            Resource r = getDynamicResource(exchange, in);
            return staticEnv.compileQuery(r.getInputStream(), null);
        } else {
            return exp;
        }
    }
    
}
