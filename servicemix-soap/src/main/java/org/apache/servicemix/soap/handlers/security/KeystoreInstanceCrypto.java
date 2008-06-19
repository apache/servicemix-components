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
package org.apache.servicemix.soap.handlers.security;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.servicemix.jbi.security.keystore.KeystoreInstance;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;

public class KeystoreInstanceCrypto extends BaseCrypto {

    private KeystoreInstance keystore;
    
    public KeystoreInstanceCrypto() {
    }
    
    public KeystoreInstanceCrypto(KeystoreInstance keystore) {
        this.keystore = keystore;
    }
    
    public KeystoreInstanceCrypto(KeystoreManager keystoreManager, String keystore) {
        this.keystore = keystoreManager.getKeystore(keystore);
    }
    
    /**
     * @return the keystore
     */
    public KeystoreInstance getKeystore() {
        return keystore;
    }

    /**
     * @param keystore the keystore to set
     */
    public void setKeystore(KeystoreInstance keystore) {
        this.keystore = keystore;
    }

    protected String[] getAliases() throws KeyStoreException {
        String[] pks = keystore.listPrivateKeys();
        String[] tcs = keystore.listTrustCertificates();
        List aliases = new ArrayList();
        aliases.addAll(Arrays.asList(pks));
        aliases.addAll(Arrays.asList(tcs));
        return (String[]) aliases.toArray(new String[aliases.size()]);
    }

    protected Certificate getCertificate(String alias) throws KeyStoreException {
        return keystore.getCertificate(alias);
    }

    protected String getCertificateAlias(Certificate cert) throws KeyStoreException {
        return keystore.getCertificateAlias(cert);
    }

    protected Certificate[] getCertificateChain(String alias) throws KeyStoreException {
        return keystore.getCertificateChain(alias);
    }

    public PrivateKey getPrivateKey(String alias, String password) throws Exception {
        return keystore.getPrivateKey(alias);
    }

    protected String[] getTrustCertificates() throws KeyStoreException {
        return keystore.listTrustCertificates();
    }

}
