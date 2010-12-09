package org.apache.servicemix.rmi;

public class EchoImpl implements Echo {

    public String echo(String message) {
        return message;
    }
    
}
