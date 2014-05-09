package com.sequenceiq.cloud.azure.client

import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
import com.thoughtworks.xstream.io.xml.XppReader
import groovy.json.JsonOutput
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.*

import javax.xml.stream.XMLStreamException
import groovyx.net.http.AuthConfig
import groovyx.net.http.HttpResponseDecorator
/**
 * Azure cloud REST client - http://msdn.microsoft.com/library/azure/ee460799.aspx
 */
class AzureClient extends RESTClient {

    def subscriptionId = "id"
    def keyStorePath = "WindowsAzureKeyStore.jks"
    def keyStorePassword = "password"

    static final boolean debugEnabled = false;

    def AzureClient() {
        AzureClient(subscriptionId, keyStorePath, keyStorePassword)
    }

    def AzureClient(String _subscriptionId, String _keyStorePath, String _keyStorePassword) {
        super(String.format("https://management.core.windows.net/%s/", _subscriptionId))
        subscriptionId = _subscriptionId
        keyStorePath = "file://" + _keyStorePath
        keyStorePassword = _keyStorePassword

        def authConfig = new AuthConfig(this)
        println("keyStorePath=" + keyStorePath)
        authConfig.certificate(keyStorePath, keyStorePassword)
        setAuthConfig(authConfig)
        setHeaders("x-ms-version": "2014-04-01")
        // setting json as the desired format does not seem to work and always defaults to XML
        // "Content-Type": "application/json", "Accept": "application/json"
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

        def client

        if (args.length == 3) {
            client = new AzureClient(args[0], args[1], args[2])
        } else {
            println()
            println("Usage: AzureClient <subscription id> <absolute path for keystore> <password for keystore>")
            println()
            System.exit(1);
        }

        // List locations
        String jsonResponse = client.getLocations()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        try {
            client.createAffinityGroup(
                    name: 'new-affinity-group',
                    description: 'new-affinity-group-description',
                    location: 'East US')
            println("Created affinitity group successfully")
        } catch (ex) {
            // 409: Conflict
            println(ex.statusCode)
            println(ex.response.data.Code)
            println(ex.response.data.Message)
        }

        try {
            client.createStorageAccount(
                    name: 'newstorageaccount',
                    description: 'newstorageaccount-description',
                    affinityGroup: 'new-affinity-group'
            )
        } catch (ex) {
            println(ex.statusCode)
            println(ex.response.data.Code)
            println(ex.response.data.Message)
        }

        try {
            client.createCloudService(
                    name: 'newservice0123',
                    description: 'newservice0123-description',
                    affinityGroup: 'new-affinity-group'
            )
        } catch (ex) {
            println(ex.statusCode)
            println(ex.response.data.Code)
            println(ex.response.data.Message)
        }

        /*
        // List virtual networks
        jsonResponse = client.getVirtualNetworks()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List storage accounts
        jsonResponse = client.getStorageAccounts()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List OS images
        jsonResponse = client.getOsImages()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List VM Images
        jsonResponse = client.getVmImages()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Disks
        jsonResponse = client.getDisks()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Affinity Groups
        jsonResponse = client.getAffinityGroups()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Cloud Services
        jsonResponse = client.getCloudServices()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }
        */

    }

    def get(Map args) {
        args.contentType = TEXT
        return convert(super.get(args).data.text)
    }

    def getLocations() {
        return get(path: "services/hostedservices")
    }

    def getVirtualNetworks() {
        return get(path: "services/networking/virtualnetwork")
    }

    def getStorageAccounts() {
        return get(path: "services/storageservices")
    }

    /**
     * name: Storage account names must be between 3 and 24 characters in length and use numbers and lower-case letters only.
     *
     *<?xml version="1.0" encoding="utf-8"?>
     * <CreateStorageServiceInput xmlns="http://schemas.microsoft.com/windowsazure">
     *   <ServiceName>name-of-storage-account</ServiceName>
     *   <Description>description-of-storage-account</Description>
     *   <Label>base64-encoded-label</Label>
     *   <AffinityGroup>name-of-affinity-group</AffinityGroup>
     *   <Location>location-of-storage-account</Location>
     *   <GeoReplicationEnabled>geo-replication-indicator</GeoReplicationEnabled>
     *   <ExtendedProperties>
     *     <ExtendedProperty>
     *       <Name>property-name</Name>
     *       <Value>property-value</Value>
     *     </ExtendedProperty>
     *   </ExtendedProperties>
     *   <SecondaryReadEnabled>secondary-read-indicator</SecondaryReadEnabled>
     * </CreateStorageServiceInput>
     */
    def createStorageAccount(Map args) {
        return post(
            path: "services/storageservices",
            requestContentType: XML,
            body: {
                mkp.xmlDeclaration()
                CreateStorageServiceInput(xmlns: "http://schemas.microsoft.com/windowsazure") {
                    ServiceName(args.name)
                    Description(args.description)
                    Label(args.name.bytes.encodeBase64().toString())
                    AffinityGroup(args.affinityGroup)
                }
            }
        )
    }

    def getOsImages() {
        return get(path: "services/images")
    }

    def getVmImages() {
        return get(path: "services/vmimages")
    }

    def getDisks() {
        return get(path: "services/disks")
    }

    def getAffinityGroups() {
        return get(path: "affinitygroups")
    }

    /**
     * <?xml version="1.0" encoding="utf-8"?>
     * <CreateAffinityGroup xmlns="http://schemas.microsoft.com/windowsazure">
     *   <Name>affinity-group-name</Name>
     *   <Label>base64-encoded-affinity-group-label</Label>
     *   <Description>affinity-group-description</Description>
     *   <Location>location</Location>
     * </CreateAffinityGroup>
     * @return
     */
    def createAffinityGroup(Map args) {
        return post(
            path: "affinitygroups",
            requestContentType: XML,
            body: {
                mkp.xmlDeclaration()
                CreateAffinityGroup(xmlns: "http://schemas.microsoft.com/windowsazure") {
                    Name(args.name)
                    Label(args.name.bytes.encodeBase64().toString())
                    Description(args.description)
                    Location(args.location)
                }
            }
        )
    }

    def getCloudServices() {
        return get(path: "services/hostedservices")
    }

    /*
     * <?xml version="1.0" encoding="utf-8"?>
     * <CreateHostedService xmlns="http://schemas.microsoft.com/windowsazure">
     *   <ServiceName>name-of-cloud-service</ServiceName>
     *   <Label>base64-encoded-label-of-cloud-service</Label>
     *   <Description>description-of-cloud-service</Description>
     *   <Location>location-of-cloud-service</Location>
     *   <AffinityGroup>name-of-affinity-group</AffinityGroup>
     *   <ExtendedProperties>
     *     <ExtendedProperty>
     *       <Name>name-of-property</Name>
     *       <Value>value-of-property</Value>
     *     </ExtendedProperty>
     *   </ExtendedProperties>
     * </CreateHostedService>
     */
    def createCloudService(Map args) {
        return post(
            path: "services/hostedservices",
            requestContentType: XML,
            body: {
                mkp.xmlDeclaration()
                CreateHostedService(xmlns: "http://schemas.microsoft.com/windowsazure") {
                    ServiceName(args.name)
                    Label(args.name.bytes.encodeBase64().toString())
                    Description(args.description)
                    AffinityGroup(args.affinityGroup)
                }
            }
        )
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
            println(ex.getMessage())
            return null
        }

    }
}
