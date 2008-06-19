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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.X509NameTokenizer;

public abstract class BaseCrypto implements Crypto {

    private static final String SKI_OID = "2.5.29.14";
    
    private String provider;
    private CertificateFactory certFact;
    private String defaultX509Alias;

    /**
     * @param defaultX509Alias the defaultX509Alias to set
     */
    public void setDefaultX509Alias(String defaultX509Alias) {
        this.defaultX509Alias = defaultX509Alias;
    }

    /**
     * @return the provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param provider the provider to set
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Return a X509 Certificate alias in the keystore according to a given Certificate
     * <p/>
     *
     * @param cert The certificate to lookup
     * @return alias name of the certificate that matches the given certificate
     *         or null if no such certificate was found.
     */
    public String getAliasForX509Cert(Certificate cert) throws WSSecurityException {
        try {
            String alias = getCertificateAlias(cert);
            if (alias != null)
                return alias;
            // Use brute force search
            String[] allAliases = getAliases();
            for (int i = 0; i < allAliases.length; i++) {
                Certificate cert2 = getCertificate(alias);
                if (cert2.equals(cert)) {
                    return alias;
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE,
                    "keystore");
        }
        return null;
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given
     * the issuer of a Certficate.
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate chain
     * for each alias. Then the Issuer fo each certificate of the chain
     * is compared with the parameters.
     *
     * @param issuer The issuer's name for the certificate
     * @return alias name of the certificate that matches the issuer name
     *         or null if no such certificate was found.
     */
    public String getAliasForX509Cert(String issuer) throws WSSecurityException {
        return getAliasForX509Cert(issuer, null, false);
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given
     * SubjectKeyIdentifier.
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate chain
     * or certificate for each alias. Then the SKI for each user certificate
     * is compared with the SKI parameter.
     *
     * @param skiBytes The SKI info bytes
     * @return alias name of the certificate that matches serialNumber and issuer name
     *         or null if no such certificate was found.
     * @throws org.apache.ws.security.WSSecurityException
     *          if problems during keystore handling or wrong certificate (no SKI data)
     */
    public String getAliasForX509Cert(byte[] skiBytes) throws WSSecurityException {
        Certificate cert = null;
        try {
            String[] allAliases = getAliases();
            for (int i = 0; i < allAliases.length; i++) {
                String alias = allAliases[i];
                cert = getCertificateChainOrCertificate(alias);
                if (cert instanceof X509Certificate) {
                    byte[] data = getSKIBytesFromCert((X509Certificate) cert);
                    if (Arrays.equals(data, skiBytes)) {
                        return alias;
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        return null;
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given serial number and
     * the issuer of a Certficate.
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate chain
     * for each alias. Then the SerialNumber and Issuer fo each certificate of the chain
     * is compared with the parameters.
     *
     * @param issuer       The issuer's name for the certificate
     * @param serialNumber The serial number of the certificate from the named issuer
     * @return alias name of the certificate that matches serialNumber and issuer name
     *         or null if no such certificate was found.
     */
    public String getAliasForX509Cert(String issuer, BigInteger serialNumber) throws WSSecurityException {
        return getAliasForX509Cert(issuer, serialNumber, true);
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given
     * Thumbprint.
     * <p/>
     * The search gets all alias names of the keystore, then reads the certificate chain
     * or certificate for each alias. Then the thumbprint for each user certificate
     * is compared with the thumbprint parameter.
     *
     * @param thumb The SHA1 thumbprint info bytes
     * @return alias name of the certificate that matches the thumbprint
     *         or null if no such certificate was found.
     * @throws org.apache.ws.security.WSSecurityException
     *          if problems during keystore handling or wrong certificate
     */
    public String getAliasForX509CertThumb(byte[] thumb) throws WSSecurityException {
        Certificate cert = null;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            throw new WSSecurityException(0, "noSHA1availabe");
        }
        try {
            String[] allAliases = getAliases();
            for (int i = 0; i < allAliases.length; i++) {
                String alias = allAliases[i];
                cert = getCertificateChainOrCertificate(alias);
                if (cert instanceof X509Certificate) {
                    sha.reset();
                    try {
                        sha.update(cert.getEncoded());
                    } catch (CertificateEncodingException e1) {
                        throw new WSSecurityException(
                                WSSecurityException.SECURITY_TOKEN_UNAVAILABLE,
                                "encodeError");
                    }
                    byte[] data = sha.digest();
                    if (Arrays.equals(data, thumb)) {
                        return alias;
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE,
                    "keystore");
        }
        return null;
    }

    /**
     * Lookup X509 Certificates in the keystore according to a given DN of the subject of the certificate
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate (chain)
     * for each alias. Then the DN of the certificate is compared with the parameters.
     *
     * @param subjectDN The DN of subject to look for in the keystore
     * @return Vector with all alias of certificates with the same DN as given in the parameters
     * @throws org.apache.ws.security.WSSecurityException
     *
     */
    public String[] getAliasesForDN(String subjectDN) throws WSSecurityException {
        // Store the aliases found
        Vector aliases = new Vector();
        Certificate cert = null;
        // The DN to search the keystore for
        Vector subjectRDN = splitAndTrim(subjectDN);
        // Look at every certificate in the keystore
        try {
            String[] allAliases = getAliases();
            for (int i = 0; i < allAliases.length; i++) {
                String alias = allAliases[i];
                cert = getCertificateChainOrCertificate(alias);
                if (cert instanceof X509Certificate) {
                    Vector foundRDN = splitAndTrim(((X509Certificate) cert).getSubjectDN().getName());
                    if (subjectRDN.equals(foundRDN)) {
                        aliases.add(alias);
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        // Convert the vector into an array
        return (String[]) aliases.toArray(new String[aliases.size()]);
    }

    /**
     * get a byte array given an array of X509 certificates.
     * <p/>
     *
     * @param reverse If set the first certificate in the array data will
     *                the last in the byte array
     * @param certs   The certificates to convert
     * @return The byte array for the certficates ordered according
     *         to the reverse flag
     * @throws WSSecurityException
     */
    public byte[] getCertificateData(boolean reverse, X509Certificate[] certs) throws WSSecurityException {
        Vector list = new Vector();
        for (int i = 0; i < certs.length; i++) {
            if (reverse) {
                list.insertElementAt(certs[i], 0);
            } else {
                list.add(certs[i]);
            }
        }
        try {
            CertPath path = getCertificateFactory().generateCertPath(list);
            return path.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE,
                    "encodeError");
        } catch (CertificateException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE,
                    "parseError");
        }
    }

    /**
     * Singleton certificate factory for this Crypto instance.
     * <p/>
     *
     * @return Returns a <code>CertificateFactory</code> to construct
     *         X509 certficates
     * @throws org.apache.ws.security.WSSecurityException
     *
     */
    public synchronized CertificateFactory getCertificateFactory() throws WSSecurityException {
        if (certFact == null) {
            try {
                if (provider == null || provider.length() == 0) {
                    certFact = CertificateFactory.getInstance("X.509");
                } else {
                    certFact = CertificateFactory.getInstance("X.509", provider);
                }
            } catch (CertificateException e) {
                throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE,
                        "unsupportedCertType");
            } catch (NoSuchProviderException e) {
                throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE,
                        "noSecProvider");
            }
        }
        return certFact;
    }

    /**
     * Gets the list of certificates for a given alias.
     * <p/>
     *
     * @param alias Lookup certificate chain for this alias
     * @return Array of X509 certificates for this alias name, or
     *         null if this alias does not exist in the keystore
     */
    public X509Certificate[] getCertificates(String alias) throws WSSecurityException {
        try {
            Certificate[] certs = getCertificateChain(alias);
            if (certs != null && certs.length > 0) {
                List x509certs = new ArrayList();
                for (int i = 0; i < certs.length; i++) {
                    if (certs[i] instanceof X509Certificate) {
                        x509certs.add(certs[i]);
                    }
                }
                return (X509Certificate[]) x509certs.toArray(new X509Certificate[x509certs.size()]);
            }
            // no cert chain, so lets check if getCertificate gives us a  result.
            Certificate cert = getCertificate(alias);
            if (cert instanceof X509Certificate) {
                return new X509Certificate[] { (X509Certificate) cert };
            }
            return null;
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
    }

    public String getDefaultX509Alias() {
        return defaultX509Alias;
    }

    public KeyStore getKeyStore() {
        return null;
    }

    /**
     * Gets the private key identified by <code>alias</> and <code>password</code>.
     * <p/>
     *
     * @param alias    The alias (<code>KeyStore</code>) of the key owner
     * @param password The password needed to access the private key
     * @return The private key
     * @throws Exception
     */
    public abstract PrivateKey getPrivateKey(String alias, String password) throws Exception;

    /**
     * Reads the SubjectKeyIdentifier information from the certificate.
     * <p/>
     * If the the certificate does not contain a SKI extension then
     * try to compute the SKI according to RFC3280 using the
     * SHA-1 hash value of the public key. The second method described
     * in RFC3280 is not support. Also only RSA public keys are supported.
     * If we cannot compute the SKI throw a WSSecurityException.
     *
     * @param cert The certificate to read SKI
     * @return The byte array conating the binary SKI data
     */
    public byte[] getSKIBytesFromCert(X509Certificate cert) throws WSSecurityException {
        /*
         * Gets the DER-encoded OCTET string for the extension value (extnValue)
         * identified by the passed-in oid String. The oid string is represented
         * by a set of positive whole numbers separated by periods.
         */
        byte[] derEncodedValue = cert.getExtensionValue(SKI_OID);
        if (cert.getVersion() < 3 || derEncodedValue == null) {
            PublicKey key = cert.getPublicKey();
            if (!(key instanceof RSAPublicKey)) {
                throw new WSSecurityException(1, "noSKIHandling", new Object[] { "Support for RSA key only" });
            }
            byte[] encoded = key.getEncoded();
            // remove 22-byte algorithm ID and header
            byte[] value = new byte[encoded.length - 22];
            System.arraycopy(encoded, 22, value, 0, value.length);
            MessageDigest sha;
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException ex) {
                throw new WSSecurityException(1, "noSKIHandling", new Object[] { "Wrong certificate version (<3) and no SHA1 message digest availabe" });
            }
            sha.reset();
            sha.update(value);
            return sha.digest();
        }
        /*
         * Strip away first four bytes from the DerValue (tag and length of
         * ExtensionValue OCTET STRING and KeyIdentifier OCTET STRING)
         */
        byte abyte0[] = new byte[derEncodedValue.length - 4];
        System.arraycopy(derEncodedValue, 4, abyte0, 0, abyte0.length);
        return abyte0;
    }

    public X509Certificate[] getX509Certificates(byte[] data, boolean reverse) throws WSSecurityException {
        InputStream in = new ByteArrayInputStream(data);
        CertPath path = null;
        try {
            path = getCertificateFactory().generateCertPath(in);
        } catch (CertificateException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE,
                    "parseError");
        }
        List l = path.getCertificates();
        X509Certificate[] certs = new X509Certificate[l.size()];
        Iterator iterator = l.iterator();
        for (int i = 0; i < l.size(); i++) {
            certs[(reverse) ? (l.size() - 1 - i) : i] = (X509Certificate) iterator.next();
        }
        return certs;
    }

    /**
     * load a X509Certificate from the input stream.
     * <p/>
     *
     * @param in The <code>InputStream</code> array containg the X509 data
     * @return An X509 certificate
     * @throws WSSecurityException
     */
    public X509Certificate loadCertificate(InputStream in) throws WSSecurityException {
        X509Certificate cert = null;
        try {
            cert = (X509Certificate) getCertificateFactory().generateCertificate(in);
        } catch (CertificateException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE,
                    "parseError");
        }
        return cert;
    }

    /**
     * Uses the CertPath API to validate a given certificate chain
     * <p/>
     *
     * @param certs Certificate chain to validate
     * @return true if the certificate chain is valid, false otherwise
     * @throws WSSecurityException
     */
    public boolean validateCertPath(X509Certificate[] certs) throws WSSecurityException {
        try {
            // Generate cert path
            java.util.List certList = java.util.Arrays.asList(certs);
            CertPath path = this.getCertificateFactory().generateCertPath(certList);

            // Use the certificates in the keystore as TrustAnchors
            Set hashSet = new HashSet();
            String[] aliases = getTrustCertificates();
            for (int i = 0; i < aliases.length; i++) {
                Certificate cert = getCertificate(aliases[i]);
                if (cert instanceof X509Certificate) {
                    hashSet.add(new TrustAnchor((X509Certificate) cert, null));
                }
            }
            PKIXParameters param = new PKIXParameters(hashSet);
            // Do not check a revocation list
            param.setRevocationEnabled(false);
            // Verify the trust path using the above settings
            CertPathValidator certPathValidator;
            if (provider == null || provider.length() == 0) {
                certPathValidator = CertPathValidator.getInstance("PKIX");
            } else {
                certPathValidator = CertPathValidator.getInstance("PKIX", provider);
            }
            certPathValidator.validate(path, param);
        } catch (NoSuchProviderException ex) {
                throw new WSSecurityException(WSSecurityException.FAILURE,
                                "certpath", new Object[] { ex.getMessage() },
                                (Throwable) ex);
        } catch (NoSuchAlgorithmException ex) {
                throw new WSSecurityException(WSSecurityException.FAILURE,
                                "certpath", new Object[] { ex.getMessage() },
                                (Throwable) ex);
        } catch (CertificateException ex) {
                throw new WSSecurityException(WSSecurityException.FAILURE,
                                "certpath", new Object[] { ex.getMessage() },
                                (Throwable) ex);
        } catch (InvalidAlgorithmParameterException ex) {
                throw new WSSecurityException(WSSecurityException.FAILURE,
                                "certpath", new Object[] { ex.getMessage() },
                                (Throwable) ex);
        } catch (CertPathValidatorException ex) {
                throw new WSSecurityException(WSSecurityException.FAILURE,
                                "certpath", new Object[] { ex.getMessage() },
                                (Throwable) ex);
        } catch (KeyStoreException ex) {
                throw new WSSecurityException(WSSecurityException.FAILURE,
                                "certpath", new Object[] { ex.getMessage() },
                                (Throwable) ex);
        }

        return true;
    }

    protected Vector splitAndTrim(String inString) {
        X509NameTokenizer nmTokens = new X509NameTokenizer(inString);
        Vector vr = new Vector();

        while (nmTokens.hasMoreTokens()) {
            vr.add(nmTokens.nextToken());
        }
        java.util.Collections.sort(vr);
        return vr;
    }
    
    protected Certificate getCertificateChainOrCertificate(String alias) throws KeyStoreException {
        Certificate[] certs = getCertificateChain(alias);
        Certificate cert = null;
        if (certs == null || certs.length == 0) {
            // no cert chain, so lets check if getCertificate gives us a  result.
            cert = getCertificate(alias);
            if (cert == null) {
                return null;
            }
        } else {
            cert = certs[0];
        }
        return cert;
    }
    
    /*
     * need to check if "getCertificateChain" also finds certificates that are
     * used for enryption only, i.e. they may not be signed by a CA
     * Otherwise we must define a restriction how to use certificate:
     * each certificate must be signed by a CA or is a self signed Certificate
     * (this should work as well).
     * --- remains to be tested in several ways --
     */
     private String getAliasForX509Cert(String issuer, BigInteger serialNumber,
                                        boolean useSerialNumber)
             throws WSSecurityException {
         Vector issuerRDN = splitAndTrim(issuer);
         X509Certificate x509cert = null;
         Vector certRDN = null;
         Certificate cert = null;

         try {
             String[] allAliases = getAliases();
             for (int i = 0; i < allAliases.length; i++) {
                 String alias = allAliases[i];
                 cert = getCertificateChainOrCertificate(alias);
                 if (cert instanceof X509Certificate) {
                     x509cert = (X509Certificate) cert;
                     if (!useSerialNumber ||
                             useSerialNumber && x509cert.getSerialNumber().compareTo(serialNumber) == 0) {
                         certRDN = splitAndTrim(x509cert.getIssuerDN().getName());
                         if (certRDN.equals(issuerRDN)) {
                             return alias;
                         }
                     }
                 }
             }
         } catch (KeyStoreException e) {
             throw new WSSecurityException(WSSecurityException.FAILURE,
                     "keystore");
         }
         return null;
     }

    protected abstract String[] getAliases() throws KeyStoreException;
    
    protected abstract Certificate[] getCertificateChain(String alias) throws KeyStoreException;
    
    protected abstract Certificate getCertificate(String alias) throws KeyStoreException;

    protected abstract String getCertificateAlias(Certificate cert) throws KeyStoreException;
    
    protected abstract String[] getTrustCertificates() throws KeyStoreException;

}
