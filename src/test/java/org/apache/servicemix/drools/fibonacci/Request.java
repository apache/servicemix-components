package org.apache.servicemix.drools.fibonacci;

public class Request {

    private final int value;
    
    public Request(int value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
    }
    
}
