package org.apache.servicemix.bean;

import javax.xml.namespace.QName;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @version $Revision$
 */
@Retention(RUNTIME)
@Target({ElementType.TYPE})
public @interface Endpoint {

    String name() default "";
    
    String uri() default "";

    String serviceName() default "";
    
    String targetNamespace() default "";

    boolean enabled() default true;
}
