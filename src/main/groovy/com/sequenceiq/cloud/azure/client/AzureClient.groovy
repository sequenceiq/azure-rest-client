package com.sequenceiq.cloud.azure.client
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
import com.thoughtworks.xstream.io.xml.XppReader
import groovy.json.JsonOutput

import javax.xml.stream.XMLStreamException
/**
 * Azure cloud REST client - http://msdn.microsoft.com/library/azure/ee460799.aspx
 */
class AzureClient {
    def subscriptionId = "id"
    def keyStorePath = "WindowsAzureKeyStore.jks"
    def keyStorePassword = "password"

    static final boolean debugEnabled = false;

    def AzureClient() {
    }

    def AzureClient(String _subscriptionId, String _keyStorePath, String _keyStorePassword) {
        subscriptionId = _subscriptionId
        keyStorePath = _keyStorePath
        keyStorePassword = _keyStorePassword
    }

    def getSubscriptionId() {
        subscriptionId
    }

    def setSubscriptionId(_subscriptionId) {
        subscriptionId = _subscriptionId
    }

    def getKeyStorePath() {
        keyStorePath
    }

    def setKeyStorePath(_keyStorePath) {
        keyStorePath = _keyStorePath
    }

    def getKeyStorePassword() {
        keyStorePassword
    }

    def setKeyStorePassword(_keyStorePassword) {
        keyStorePassword = _keyStorePassword
    }

    static void main(String[] args) {

        def client = new AzureClient()

        if (args.length == 3) {
            client.setSubscriptionId(args[0])
            client.setKeyStorePath(args[1])
            client.setKeyStorePassword(args[2])
        }

        // List locations
        String jsonResponse = convert(client.getLocations())
        if (jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List virtual networks
        jsonResponse = convert(client.getVirtualNetworks())
        if (jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List storage accounts
        jsonResponse = convert(client.getStorageAccounts())
        if (jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List OS images
        jsonResponse = convert(client.getOsImages())
        if (jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List VM Images
        jsonResponse = convert(client.getVmImages())
        if (jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Disks
        jsonResponse = convert(client.getDisks())
        if (jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Affinity Groups
        jsonResponse = convert(client.getAffinityGroups())
        if (jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse))
        }
    }

    String get(String url) {
        String fullUrl = String.format("https://management.core.windows.net/%s/%s", subscriptionId, url)
        return ServiceManagementHelper.processGetRequest(new URL(fullUrl), keyStorePath, keyStorePassword)
    }

    def getLocations() {
        return get("services/hostedservices")
    }

    def getVirtualNetworks() {
        return get("services/networking/virtualnetwork")
    }

    def getStorageAccounts() {
        return get("services/storageservices")
    }

    def getOsImages() {
        return get("services/images")
    }

    def getVmImages() {
        return get("services/vmimages")
    }

    def getDisks() {
        return get("services/disks")
    }

    def getAffinityGroups() {
        return get("affinitygroups")
    }

    static String convert(String response) throws XMLStreamException, IOException {
        try {
            String xmlHeader= "xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\""
            HierarchicalStreamReader sourceReader = new XppReader(new StringReader(response.toString().replaceAll(xmlHeader, "")))

            StringWriter buffer = new StringWriter()
            JettisonMappedXmlDriver jettisonDriver = new JettisonMappedXmlDriver()
            jettisonDriver.createWriter(buffer)
            HierarchicalStreamWriter destinationWriter = jettisonDriver.createWriter(buffer)

            HierarchicalStreamCopier copier = new HierarchicalStreamCopier()
            copier.copy(sourceReader, destinationWriter)
            return buffer.toString()
        } catch (Exception ex) {
            System.out.println(ex.getMessage())
            return null
        }

    }
}
