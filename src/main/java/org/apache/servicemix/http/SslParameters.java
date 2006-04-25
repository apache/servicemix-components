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
package org.apache.servicemix.http;

/**
 * This class contains all parameters needed to create an SSL server or client
 * socket.
 *  
 * @author gnodet
 * @org.apache.xbean.XBean 
 */
public class SslParameters {

    private String keyPassword;
    private String keyStore;
    private String keyStorePassword;
    private String keyStoreType = "JKS"; // type of the key store
    private String trustStore;
    private String trustStorePassword;
    private String trustStoreType = "JKS";
    private String protocol = "TLS";
    private String algorithm = "SunX509"; // cert algorithm
    private boolean wantClientAuth = false;
    private boolean needClientAuth = false;
    
    /**
     * @return Returns the algorithm.
     */
    public String getAlgorithm() {
        return algorithm;
    }
    /**
     * @param algorithm The algorithm to set.
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    /**
     * @return Returns the keyPassword.
     */
    public String getKeyPassword() {
        return keyPassword;
    }
    /**
     * @param keyPassword The keyPassword to set.
     */
    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }
    /**
     * @return Returns the keyStore.
     */
    public String getKeyStore() {
        return keyStore;
    }
    /**
     * @param keyStore The keyStore to set.
     */
    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }
    /**
     * @return Returns the keyStorePassword.
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }
    /**
     * @param keyStorePassword The keyStorePassword to set.
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }
    /**
     * @return Returns the keyStoreType.
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }
    /**
     * @param keyStoreType The keyStoreType to set.
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }
    /**
     * @return Returns the needClientAuth.
     */
    public boolean isNeedClientAuth() {
        return needClientAuth;
    }
    /**
     * @param needClientAuth The needClientAuth to set.
     */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }
    /**
     * @return Returns the protocol.
     */
    public String getProtocol() {
        return protocol;
    }
    /**
     * @param protocol The protocol to set.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    /**
     * @return Returns the wantClientAuth.
     */
    public boolean isWantClientAuth() {
        return wantClientAuth;
    }
    /**
     * @param wantClientAuth The wantClientAuth to set.
     */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }
    /**
     * @return Returns the trustStore.
     */
    public String getTrustStore() {
        return trustStore;
    }
    /**
     * @param trustStore The trustStore to set.
     */
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }
    /**
     * @return Returns the trustStorePassword.
     */
    public String getTrustStorePassword() {
        return trustStorePassword;
    }
    /**
     * @param trustStorePassword The trustStorePassword to set.
     */
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
    /**
     * @return Returns the trustStoreType.
     */
    public String getTrustStoreType() {
        return trustStoreType;
    }
    /**
     * @param trustStoreType The trustStoreType to set.
     */
    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof SslParameters == false) {
            return false;
        }
        SslParameters s = (SslParameters) o;
        return eq(algorithm, s.algorithm) &&
               eq(keyPassword, s.keyPassword) &&
               eq(keyStore, s.keyStore) &&
               eq(keyStorePassword, s.keyStorePassword) &&
               eq(keyStoreType, s.keyStoreType) &&
               needClientAuth == s.needClientAuth &&
               eq(protocol, s.protocol) &&
               eq(trustStore, s.trustStore) &&
               eq(trustStorePassword, s.trustStorePassword) &&
               eq(trustStoreType, s.trustStoreType) &&
               wantClientAuth == s.wantClientAuth;
               
    }
    
    public int hashCode() {
        return hash(algorithm) ^
               hash(keyPassword) ^
               hash(keyStore) ^
               hash(keyStorePassword) ^
               hash(keyStoreType) ^
               Boolean.valueOf(needClientAuth).hashCode() ^
               hash(protocol) ^
               hash(trustStore) ^
               hash(trustStorePassword) ^
               hash(trustStoreType) ^
               Boolean.valueOf(wantClientAuth).hashCode();
    }
    
    private static boolean eq(String s1, String s2) {
        return (s1 == null) ? s2 == null : s1.equals(s2);
    }
    
    private static int hash(String s) {
        return s != null ? s.hashCode() : 0;
    }


}
