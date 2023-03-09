/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.toolkit.encryptconfig.util

import groovy.xml.XmlUtil
import org.apache.nifi.properties.SensitivePropertyProvider
import org.apache.nifi.properties.SensitivePropertyProviderFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException

class NiFiRegistryAuthorizersXmlEncryptor extends XmlEncryptor {

    private static final Logger logger = LoggerFactory.getLogger(NiFiRegistryAuthorizersXmlEncryptor.class)

    static final String LDAP_USER_GROUP_PROVIDER_CLASS = "org.apache.nifi.registry.security.ldap.tenants.LdapUserGroupProvider"
    static final String AZURE_GRAPH_USER_GROUP_PROVIDER_CLASS = "org.apache.nifi.authorization.azure.AzureGraphUserGroupProvider"
    static final String FILE_USER_GROUP_PROVIDER_CLASS = "org.apache.nifi.registry.security.authorization.file.FileUserGroupProvider"
    private static final String LDAP_USER_GROUP_PROVIDER_REGEX =
            /(?s)<userGroupProvider>(?:(?!<userGroupProvider>).)*?<class>\s*org\.apache\.nifi\.registry\.security\.ldap\.tenants\.LdapUserGroupProvider.*?<\/userGroupProvider>/
    private static final String AZURE_GRAPH_USER_GROUP_PROVIDER_REGEX =
                /(?s)<userGroupProvider>(?:(?!<userGroupProvider>).)*?<class>\s*org\.apache\.nifi\.authorization\.azure\.AzureGraphUserGroupProvider.*?<\/userGroupProvider>/
    private static final String FILE_USER_GROUP_PROVIDER_REGEX =
                /(?s)<userGroupProvider>(?:(?!<userGroupProvider>).)*?<class>\s*org\.apache\.nifi\.registry\.security\.authorization\.file\.FileUserGroupProvider.*?<\/userGroupProvider>/
    /* Explanation of LDAP_USER_GROUP_PROVIDER_REGEX:
     *   (?s)                             -> single-line mode (i.e., `.` in regex matches newlines)
     *   <userGroupProvider>              -> find occurrence of `<userGroupProvider>` literally (case-sensitive)
     *   (?: ... )                        -> group but do not capture submatch
     *   (?! ... )                        -> negative lookahead
     *   (?:(?!<userGroupProvider>).)*?   -> find everything until a new `<userGroupProvider>` starts. This is for not selecting multiple userGroupProviders in one match
     *   <class>                          -> find occurrence of `<class>` literally (case-sensitive)
     *   \s*                              -> find any whitespace
     *   org\.apache\.nifi\.registry\.security\.ldap\.tenants\.LdapUserGroupProvider
     *                                    -> find occurrence of `org.apache.nifi.registry.security.ldap.tenants.LdapUserGroupProvider` literally (case-sensitive)
     *   .*?</userGroupProvider>          -> find everything as needed up until and including occurrence of '</userGroupProvider>'
     */

    NiFiRegistryAuthorizersXmlEncryptor(final SensitivePropertyProvider encryptionProvider, final SensitivePropertyProvider decryptionProvider,
            final SensitivePropertyProviderFactory providerFactory) {
        super(encryptionProvider, decryptionProvider, providerFactory)
    }

    /**
     * Overrides the super class implementation to marking xml nodes that should be encrypted.
     * This is done using logic specific to the authorizers.xml file type targeted by this subclass,
     * leveraging knowledge of the XML file structure and which elements are sensitive.
     * Sensitive nodes are marked by adding the encryption="none" attribute.
     * When all the sensitive values are found and marked, the base class implementation
     * is invoked to encrypt them.
     *
     * @param plainXmlContent the plaintext content of an authorizers.xml file
     * @param propertyLocation The property location
     * @return the comment with sensitive values encrypted and marked with the cipher.
     */
    @Override
    String encrypt(final String plainXmlContent) {
        String encryptingProperties = "Password,Application ID"
        String[] propertyNames = encryptingProperties.split(",")
        // First, mark the XML nodes to encrypt that are specific to authorizers.xml by adding an attribute encryption="none"
        String markedXmlContent = markXmlNodesForEncryption(plainXmlContent, "userGroupProvider", {
            it.find { it.'class' as String == LDAP_USER_GROUP_PROVIDER_CLASS }
            .property.findAll { propertyNames.any { name -> it.@name =~ name } && it.text() }
        });
        markedXmlContent = markXmlNodesForEncryption(markedXmlContent, "userGroupProvider", {
            it.find { it.'class' as String == AZURE_GRAPH_USER_GROUP_PROVIDER_CLASS }
            .property.findAll { propertyNames.any { name -> it.@name =~ name } && it.text() }
        });
        markedXmlContent = markXmlNodesForEncryption(markedXmlContent, "userGroupProvider", {
            it.find { it.'class' as String == FILE_USER_GROUP_PROVIDER_CLASS }
            .property.findAll { propertyNames.any { name -> it.@name =~ name } && it.text() }
        });

        // Now, return the results of the base implementation, which encrypts any node with an encryption="none" attribute
        return super.encrypt(markedXmlContent)
    }

    List<String> serializeXmlContentAndPreserveFormat(String updatedXmlContent, String originalXmlContent) {
        if (updatedXmlContent == originalXmlContent) {
            // If nothing was encrypted, e.g., the sensitive properties are commented out or empty,
            // then the best thing to do to preserve formatting perspective is to do nothing.
            return originalXmlContent.split("\n")
        }

        // Find & replace the userGroupProvider element of the updated content in the original contents
        try {
            def parsedXml = new XmlSlurper().parseText(updatedXmlContent)
            def ldapUserGroupProvider = parsedXml.userGroupProvider.find { it.'class' as String == LDAP_USER_GROUP_PROVIDER_CLASS }
            if (ldapUserGroupProvider) {
                def serializedProvider = new XmlUtil().serialize(ldapUserGroupProvider)
                // Remove XML declaration from top
                serializedProvider = serializedProvider.replaceFirst(XML_DECLARATION_REGEX, "")
                originalXmlContent = originalXmlContent.replaceFirst(LDAP_USER_GROUP_PROVIDER_REGEX, serializedProvider)
                // return originalXmlContent.split("\n")
            }
            def azureGraphUserGroupProvider = parsedXml.userGroupProvider.find { it.'class' as String == AZURE_GRAPH_USER_GROUP_PROVIDER_CLASS }
            if (azureGraphUserGroupProvider) {
                def serializedProvider = new XmlUtil().serialize(azureGraphUserGroupProvider)
                serializedProvider = serializedProvider.replaceFirst(XML_DECLARATION_REGEX, "")
                originalXmlContent = originalXmlContent.replaceFirst(AZURE_GRAPH_USER_GROUP_PROVIDER_REGEX, serializedProvider)
            }
            def fileUserGroupProvider = parsedXml.userGroupProvider.find { it.'class' as String == FILE_USER_GROUP_PROVIDER_CLASS }
            if (fileUserGroupProvider) {
                def serializedProvider = new XmlUtil().serialize(fileUserGroupProvider)
                serializedProvider = serializedProvider.replaceFirst(XML_DECLARATION_REGEX, "")
                originalXmlContent = originalXmlContent.replaceFirst(FILE_USER_GROUP_PROVIDER_REGEX, serializedProvider)
            }
        } catch (SAXException e) {
            logger.warn("No userGroupProvider with class ${LDAP_USER_GROUP_PROVIDER_CLASS}/${AZURE_GRAPH_USER_GROUP_PROVIDER_CLASS}/${FILE_USER_GROUP_PROVIDER_CLASS} found in XML content. " +
                    "The file could be empty or the element may be missing or commented out")
        }
        return originalXmlContent.split("\n")
    }

}
