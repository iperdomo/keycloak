/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.keys;

import org.jboss.logging.Logger;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JavaKeystoreKeyProvider extends AbstractRsaKeyProvider {

    private static final Logger logger = Logger.getLogger(JavaKeystoreKeyProvider.class);

    public JavaKeystoreKeyProvider(RealmModel realm, ComponentModel model) {
        super(realm, model);
    }

    @Override
    protected Keys loadKeys(RealmModel realm, ComponentModel model) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_KEY)), model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_PASSWORD_KEY).toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(model.get(JavaKeystoreKeyProviderFactory.KEY_ALIAS_KEY), model.get(JavaKeystoreKeyProviderFactory.KEY_PASSWORD_KEY).toCharArray());
            PublicKey publicKey = KeyUtils.extractPublicKey(privateKey);

            KeyPair keyPair = new KeyPair(publicKey, privateKey);

            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(model.get(JavaKeystoreKeyProviderFactory.KEY_ALIAS_KEY));
            if (certificate == null) {
                certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, realm.getName());
            }

            String kid = KeyUtils.createKeyId(keyPair.getPublic());

            return new Keys(kid, keyPair, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load keys", e);
        }
    }

}
