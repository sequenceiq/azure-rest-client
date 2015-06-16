package com.sequenceiq.cloud.azure.client

import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
import com.thoughtworks.xstream.io.xml.XppReader
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.*

import javax.xml.stream.XMLStreamException

import static groovyx.net.http.ContentType.*

/**
 * Azure cloud REST client - http://msdn.microsoft.com/library/azure/ee460799.aspx
 */
class AzureClient extends RESTClient {

    def subscriptionId = "id"
    def keyStorePath = "WindowsAzureKeyStore.jks"
    def keyStorePassword = "password"
    static final char[] letters = ["f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"]

    static final boolean debugEnabled = false;

    def JsonSlurper jsonSlurper = new JsonSlurper()

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
        setHeaders("x-ms-version": "2014-05-01")
        this.client.params.setParameter("http.socket.timeout", new Integer(20000))
        this.client.params.setParameter("http.connection.timeout", new Integer(20000))
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
        println "Listing all locations:"
        String jsonResponse = client.getLocations()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List virtual networks
        println "Listing all virtual networks:"
        jsonResponse = client.getVirtualNetworks()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List storage accounts
        println "Listing all storage accounts:"
        jsonResponse = client.getStorageAccounts()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List OS images
        println "Listing all OS images:"
        jsonResponse = client.getOsImages()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List VM Images
        println "Listing all VM images:"
        jsonResponse = client.getVmImages()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Disks
        println "Listing all disks:"
        jsonResponse = client.getDisks()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Affinity Groups
        println "Listing all affinity groups:"
        jsonResponse = client.getAffinityGroups()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

        // List Cloud Services
        println "Listing all cloud services:"
        jsonResponse = client.getCloudServices()
        if (jsonResponse != null) {
            println(JsonOutput.prettyPrint(jsonResponse))
        }

    }

    /**
     * Overrides the RESTClient's get method behavior so that we display the output in JSON by default (XML output
     * can be specified via the extra "format" parameter).
     * This was needed because the Azure API does not seem to return JSON even though the client sets appropriate
     * HTTP headers.
     *
     * @param args : the same as what's accepted by groovyx.net.http.RESTClient, with an additional format which
     *   can be JSON or XML.  If format is not supplied, JSON is assumed.
     */
    def get(Map args) {
        if (args.format == JSON || args.format == null) {
            args.contentType = TEXT
            args.remove('format')
            return convert(super.get(args).data.text)
        } else if (args.format == XML) {
            args.contentType = TEXT
            args.remove('format')
            return super.get(args).data.text
        } else {
            throw new IllegalArgumentException("Unrecognized format " + args.format)
        }
    }

    /**
     * Overrides the RESTClient's post method behavior so that temporary redirects cause automatic retries.
     */
    def post(Map args) {
        def argsClone = args.clone()
        log.info 'post original args=' + args
        def HttpResponseDecorator response = super.post(argsClone)
        // If the server responds with 307 (Temporary Redirect), POST again after a short wait
        while (response.getStatus() == 307) {
            sleep(1000)
            log.info 'post retry args=' + args
            argsClone = args.clone()
            response = super.post(argsClone)
        }
        return response
    }

    /**
     * Overrides the RESTClient's put method behavior so that temporary redirects cause automatic retries.
     */
    def put(Map args) {
        def argsClone = args.clone()
        log.info 'put original args=' + args
        def HttpResponseDecorator response = super.put(argsClone)
        // If the server responds with 307 (Temporary Redirect), POST again after a short wait
        while (response.getStatus() == 307) {
            sleep(1000)
            log.info 'put retry args=' + args
            argsClone = args.clone()
            response = super.put(argsClone)
        }
        return response
    }

    /**
     * Overrides the RESTClient's delete method behavior so that temporary redirects cause automatic retries.
     */
    def delete(Map args) throws HttpResponseException {
        def argsClone = args.clone()
        log.info 'delete original args=' + args
        def HttpResponseDecorator response = super.delete(argsClone)
        if (response.getStatus() == 404) {
            throw new HttpResponseException("Resource not found");
        }
        // If the server responds with 307 (Temporary Redirect), DELETE again after a short wait
        while (response.getStatus() == 307) {
            log.info 'delete retry args=' + args
            sleep(1000)
            argsClone = args.clone()
            response = super.delete(argsClone)
        }
        return response
    }

    /**
     * Gets the status of asynchronous requests.
     *
     * @param requestId
     * @return
     * Succeeded example: {"Operation":{"ID":"e9e74e80-5709-9cd2-8aaa-a5c9d238a12a","Status":"Succeeded","HttpStatusCode":200}}* In Progress example: {"Operation":{"ID":"e9e74e80-5709-9cd2-8aaa-a5c9d238a12a","Status":"InProgress"}}
     */
    def getRequestStatus(String requestId) {
        return get(path: "operations/" + requestId)
    }

    def getRequestId(HttpResponseDecorator response) {
        return response.headers.getAt('x-ms-request-id').value
    }

    /**
     * Blocks until the asynchronous request is complete.
     * @param requestId : the request ID for the asynchronous reqeust to wait on.
     * @param maxWaitMillis : maximum number of wait time in millis; defaults to 120000ms, or 2 mins.
     * @return JSON object of the completed operation status; null if the request did not complete in max wait time millis.
     */
    boolean waitUntilComplete(String requestId, int maxWaitMillis = 120000) {
        def start = new Date().getTime()
        while (true) {
            sleep(1000)
            def statusResponse = getRequestStatus(requestId)
            // println 'status response=' + statusResponse
            def status = jsonSlurper.parseText(statusResponse).Operation.Status
            if (status != 'InProgress') {
                statusResponse
                return true;
            }
            def end = new Date().getTime()
            if (end - start >= maxWaitMillis) {
                println 'Max wait time ' + maxWaitMillis + ' exceeded.  Exiting.'
                return false
            }
        }
        return false

    }

    /**
     * Gets all locations available, such as "West US", "East Asia", etc.
     * @param format : JSON or XML
     */
    def getLocations(ContentType format = ContentType.JSON) {
        return get(path: "locations", format: format)
    }

    /**
     * Gets all affinity groups under the subscription.
     * @param format : JSON or XML
     */
    def getAffinityGroups(ContentType format = ContentType.JSON) {
        return get(path: "affinitygroups", format: format)
    }

    /**
     * Gets one affinity group under the subscription.
     * @param format : JSON or XML
     * @param name : name of affinity group
     */
    def getAffinityGroup(String name, ContentType format = ContentType.JSON) {
        return get(path: "affinitygroups/" + name, format: format)
    }

    boolean isImageAvailable(String imageName) {
        return getOsImages().contains(imageName);
    }

    def uploadManagementCertificate(Map args) {
        return post(
                path: "certificates",
                requestContentType: XML,
                body: {
                    mkp.xmlDeclaration()
                    SubscriptionCertificate(xmlns: "http://schemas.microsoft.com/windowsazure") {
                        SubscriptionCertificatePublicKey(args.subscriptionCertificatePublicKey)
                        SubscriptionCertificateThumbprint(args.subscriptionCertificateThumbprint)
                        SubscriptionCertificateData(args.subscriptionCertificateData)
                    }
                }
        )
    }

    /**
     * Creates an affinity group.
     * This needs to be created before creating storage accounts, virtual networks, cloud services, virtual machines, and other resources.
     * @param
     * name: the name of the affinity group to create
     *   description
     *   location: pick one from the output of getLocations(); e.g., "East US"
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

    def createReservedIP(Map args) {
        return post(
                path: "services/networking/reservedips",
                requestContentType: XML,
                body: {
                    mkp.xmlDeclaration()
                    ReservedIP(xmlns: "http://schemas.microsoft.com/windowsazure") {
                        Name(args.name)
                        Label(args.name.bytes.encodeBase64().toString())
                        Location(args.location)
                    }
                }
        )
    }

    def getReservedIP(Map args, ContentType format = ContentType.JSON) throws HttpResponseException {
        return get(path: String.format('services/networking/reservedips/%s', args.name), format: format)
    }

    def deleteReservedIP(Map args) throws HttpResponseException {
        return delete(path: String.format('services/networking/reservedips/%s', args.name))
    }

    /**
     * Deletes an affinity group.
     * @param args
     *   name: the name of the affinity group to delete.
     */
    def deleteAffinityGroup(Map args) throws HttpResponseException {
        return delete(path: String.format('affinitygroups/%s', args.name))
    }

    /**
     * Gets all virtual network configurations for the subscription.
     * Used when creating a new virtual network.
     * @param format : JSON or XML
     */
    def getVirtualNetworkConfiguration(ContentType format = ContentType.JSON) {
        return get(path: "services/networking/media", format: format)
    }

    /**
     * Gets all virtual networks under the subscription.
     * @param format : JSON or XML
     */
    def getVirtualNetworks(ContentType format = ContentType.JSON) {
        return get(path: "services/networking/virtualnetwork", format: format)
    }

    /**
     * Creates a virtual network.
     * Note that this call is asynchronous.
     * If there are no validation errors, the server returns 202 (Accepted).
     * The request status can be checked via getRequestStatus(requestId).
     *
     * @param args
     *   name: required; name of the virtual network to create
     *   affinityGroup: required
     *   addressPrefix: required (e.g., 172.16.0.0/16)
     *   subnetName: required
     *   subnetAddressPrefix: required (e.g., 172.16.0.0/24)
     *   dnsServerAddress: optional
     *
     * @exception
     * Duplicate example: <Error xmlns="http://schemas.microsoft.com/windowsazure" xmlns:i="http://www.w3.org/2001/XMLSchema-instance"><Code>BadRequest</Code><Message>Multiple virtual network sites specified with the same name 'mynew123'.</Message></Error>
     */
    def createVirtualNetwork(Map args) {
        // There is no call to create a new virtual network, so we need to PUT the entire
        // virtual network configuration.

        // First, retrieve the current config.
        // Kill everything before < as the extra characters cause XML parsing errors.
        def currentConfig
        def root
        String configs
        try {
            configs = getVirtualNetworkConfiguration(XML)
        } catch (e) {
            println('Error in the request')
        }
        if (configs != null && configs != 'null') {
            currentConfig = configs.replaceFirst('^[^<]*', '')
            root = new XmlParser().parseText(currentConfig)
        }

        // Inject the new virtual network XML to the current config, or create a new config if none exists.

        // Case where no configs currently exist
        if (root == null) {
            def rootContent = {
                NetworkConfiguration("xmlns": "http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration") {
                    VirtualNetworkConfiguration {
                        if (args.dnsServerAddress) {
                            Dns {
                                DnsServers {
                                    DnsServer(name: args.name, IPAddress: args.dnsServerAddress)
                                }
                            }
                        }
                        VirtualNetworkSites {
                            VirtualNetworkSite(name: args.name, AffinityGroup: args.affinityGroup) {
                                AddressSpace {
                                    AddressPrefix(args.addressPrefix)
                                }
                                Subnets {
                                    Subnet(name: args.subnetName) {
                                        AddressPrefix(args.subnetAddressPrefix)
                                    }
                                }
                                if (args.dnsServerAddress) {
                                    DnsServersRef {
                                        DnsServerRef(name: args.name)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return put(
                    path: "services/networking/media",
                    requestContentType: TEXT,
                    body: closureToXmlString(rootContent)
            )
        }

        def newNodeContent

        // Case where a virtual network config already exists
        // Construct the new virtual network site XML node.
        newNodeContent = {
            VirtualNetworkSite(name: args.name, AffinityGroup: args.affinityGroup) {
                Subnets {
                    Subnet(name: args.subnetName) {
                        AddressPrefix(args.subnetAddressPrefix)
                    }
                }
                AddressSpace {
                    AddressPrefix(args.addressPrefix)
                }
            }
        }
        // Add VirtualNetworkSites tag if no virtual networks currently exist (this can happen if there's a DNS
        // defined but no virtual networks are defined, etc.)
        if (root.VirtualNetworkConfiguration.VirtualNetworkSites[0] == null) {
            root.VirtualNetworkConfiguration[0].appendNode("VirtualNetworkSites")
        }
        println 'current root=' + nodeToXmlString(root)
        // Add the new VirtualNetworkSite tag
        def Node newNode = root.VirtualNetworkConfiguration.VirtualNetworkSites[0].appendNode("")
        newNode.replaceNode(newNodeContent)
        return put(
                path: "services/networking/media",
                requestContentType: TEXT,
                body: nodeToXmlString(root)
        )
    }

    def closureToXmlString(def closure) {
        def writer = new StringWriter()
        def builder = new groovy.xml.MarkupBuilder(writer)

        builder.with {
            closure.delegate = delegate
            closure()
        }
        return writer.toString()
    }

    def nodeToXmlString(def root) {
        def writer = new StringWriter();
        def nodePrinter = new XmlNodePrinter(new PrintWriter(writer))
        // Must preserve whitespace.  Otherwise the printer adds extraneous spaces and the server barfs on it.
        nodePrinter.preserveWhitespace = true
        nodePrinter.print(root)
        return writer.toString()
    }

    /**
     * Deletes a virtual network.
     * @param args
     *   name: the name of the virtual network to delete.
     */
    def deleteVirtualNetwork(Map args) throws HttpResponseException {
        // There is no call to delete a new virtual network, so we need to PUT the entire
        // virtual network configuration.

        // First, retrieve the current config.
        // Kill everything before < as the extra characters cause XML parsing errors.
        def currentConfig = getVirtualNetworkConfiguration(XML).replaceFirst('^[^<]*', '')

        // println "current config=" + currentConfig

        def root = new XmlParser().parseText(currentConfig)

        // Remove the virtual network XML from the current config.
        def Node nodeToDelete = root.VirtualNetworkConfiguration.VirtualNetworkSites.VirtualNetworkSite.find { it.@name == args.name }
        if (nodeToDelete) {
            nodeToDelete.parent().remove(nodeToDelete)
        }

        def writer = new StringWriter();
        def nodePrinter = new XmlNodePrinter(new PrintWriter(writer))
        // Must preserve whitespace.  Otherwise the printer adds extraneous spaces and the server barfs on it.
        nodePrinter.preserveWhitespace = true
        nodePrinter.print(root)
        def requestXml = writer.toString()

        return put(
                path: "services/networking/media",
                requestContentType: TEXT,
                body: requestXml
        )
    }

    /**
     * Validates a storage account name to see if it can be used (not already taken and conforms to the rules).
     * @param args
     *   name: the storage account name to check validity for.
     */
    def isStorageAccountNameAvailable(Map args, ContentType format = ContentType.JSON) {
        return get(path: String.format('services/storageservices/operations/isavailable/%s', args.name),
                format: format)
    }

    /**
     * Creates a storage account.
     * Note that this call is asynchronous.
     * If there are no validation errors, the server returns 202 (Accepted).
     * The request status can be checked via getRequestStatus(requestId).
     *
     * @param args
     *   name: Required. A name for the storage account that is unique within Azure. Storage account names must be between
     *   3 and 24 characters in length and use numbers and lower-case letters only.
     *   This name is the DNS prefix name and can be used to access blobs, queues, and tables in the storage account.
     *   For example: http://ServiceName.blob.core.windows.net/mycontainer/
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

    /**
     * Deletes a Storage Account with all it's VHD files.
     *
     * @param name name of the Storage Account
     */
    def deleteStorageAccount(String name) {
        return delete(path: "services/storageservices/$name")
    }

    /**
     * Deletes a storage account.
     * Note that this call is asynchronous.
     * If there are no validation errors, the server returns 202 (Accepted).
     * The request status can be checked via getRequestStatus(requestId).
     *
     * @param args
     *   name: the name of the storage account to delete.
     */
    def deleteStorageAccount(Map args) throws HttpResponseException {
        return delete(path: String.format('services/storageservices/%s', args.name))
    }

    /**
     * Gets all storage accounts under the subscription.
     * @param format : JSON or XML
     */
    def getStorageAccounts(ContentType format = ContentType.JSON) {
        return get(path: "services/storageservices", format: format)
    }

    /**
     * Gets all storage accounts under the subscription.
     * @param format : JSON or XML
     * @param name : storage name
     */
    def getStorageAccount(String name, ContentType format = ContentType.JSON) {
        return get(path: "services/storageservices/" + name, format: format)
    }

    /**
     * Gets the key for the specified storage account.
     * The key can be used to make calls against the Azure Blob Management API.
     * @param args
     *   name: the name of the storage account to retrieve keys for.
     * @param format
     * @return
     */
    def getStorageAccountKeys(Map args, ContentType format = ContentType.JSON) {
        return get(path: String.format("services/storageservices/%s/keys", args.name))
    }

    /**
     * Gets all available OS images that can be used to create disks for new VMs.
     * @param format : JSON or XML
     */
    def getOsImages(ContentType format = ContentType.JSON) {
        return get(path: "services/images", format: format)
    }

    /**
     * Adds an operating system image to the image repository that is associated with the specified subscription.
     * @param args
     *   name: the name of the image.
     *   mediaLink: the URI of the .vhd file for the image.
     *   os: "Windows" or "Linux"
     */
    def addOsImage(Map args) {
        return post(
                path: "services/images",
                requestContentType: XML,
                body: {
                    mkp.xmlDeclaration()
                    OSImage(xmlns: "http://schemas.microsoft.com/windowsazure") {
                        Label(args.name)
                        MediaLink(args.mediaLink)
                        Name(args.name)
                        OS(args.os)
                    }
                }
        )
    }

    /**
     * Deletes an OS image. It does not delete the VHD file stored in a Storage Account.
     *
     * @param name name of the image
     */
    def deleteOsImage(String name) {
        return delete(path: "services/images/$name")
    }

    /**
     * Gets all disks under the subscription.
     * @param format : JSON or XML
     */
    def getDisks(ContentType format = ContentType.JSON) {
        return get(path: "services/disks", format: format)
    }

    /**
     * Deletes a disk.
     *
     * @param args
     *     name: the name of the disk to delete.
     */
    def deleteDisk(Map args) throws HttpResponseException {
        return delete(path: String.format('services/disks/%s?comp=media', args.name))
    }

    /**
     * Gets all image under the subscription.
     * @param format : JSON or XML
     */
    def getVmImages(ContentType format = ContentType.JSON) {
        return get(path: "services/vmimages", format: format)
    }

    /**
     * Gets all cloud services under the subscription.
     * @param format : JSON or XML
     */
    def getCloudServices(ContentType format = ContentType.JSON) {
        return get(path: "services/hostedservices", format: format)
    }

    /**
     * Gets one cloud service under the subscription.
     * @param format : JSON or XML
     * @param name : name of te cloud service
     */
    def getCloudService(String name, ContentType format = ContentType.JSON) {
        return get(path: "services/hostedservices/" + name, format: format)
    }

    /**
     * Gets all resource extensions under the subscription.
     * @param format : JSON or XML
     */
    def getResourceExtensions(ContentType format = ContentType.JSON) {
        return get(path: "services/resourceextensions", format: format)
    }

    /**
     * Creates a cloud service.
     * Before creating a VM, you need to create a cloud service.
     * @param
     * name: name of the cloud service to create
     *   description
     *   affinity group: affinity group to which this cloud service will belong
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

    /**
     * Deletes a cloud service.
     * @param args
     *   name: the name of the cloud service to delete
     */
    def deleteCloudService(Map args) throws HttpResponseException {
        return delete(path: String.format('services/hostedservices/%s', args.name))
    }

    /**
     * Gets all certificates that belong to a cloud service.
     * @param name : the name of the cloud service to get certificates for.
     * @param format
     * @return
     */
    def getServiceCertificates(Map args, ContentType format = ContentType.JSON) {
        return get(path: String.format('services/hostedservices/%s/certificates', args.name), format: format)
    }

    def deleteServiceCertificates(Map args) throws Exception {
        return delete(path: String.format('services/hostedservices/%s/certificates/%s-%s', args.name, args.algorithm, args.thumbprint))
    }

    /**
     * Creates a certificate for a cloud service.
     * Note that this call is asynchronous.
     * If there are no validation errors, the server returns 202 (Accepted).
     * The request status can be checked via getRequestStatus(requestId).

     * @param args
     *   name: the name of the cloud service to create a certificate for.
     *   data: the certificate data
     */
    def createServiceCertificate(Map args) {
        return post(
                path: String.format('services/hostedservices/%s/certificates', args.name),
                requestContentType: XML,
                body: {
                    mkp.xmlDeclaration()
                    CertificateFile(xmlns: "http://schemas.microsoft.com/windowsazure") {
                        Data(args.data)
                        CertificateFormat('pfx')
                    }
                }
        )
    }

    /**
     * Gets information on a virtual machine.
     * @param args
     *   name: the name of the virtual machine
     *   serviceName: the name of the cloud service under which the virtual machine exists
     */
    def getVirtualMachine(Map args, ContentType format = ContentType.JSON) {
        return get(path: String.format('services/hostedservices/%s/deployments/%s', args.serviceName, args.name))
    }

    /**
     * Returns the state of the virtual machine.
     *
     * @param args
     *   name: the name of the virtual machine
     *   serviceName: the name of the cloud service under which the virtual machine exists
     */
    String getVirtualMachineState(Map args) {
        jsonSlurper.parseText(getVirtualMachine(args)).Deployment.Status
    }

    /**
     * Stops the given virtual machine. It also deallocates the resources used by the vm, so Azure won't bill
     * for this vm while it is stopped.
     *
     * @param args
     *   name: the name of the virtual machine
     *   serviceName: the name of the cloud service under which the virtual machine exists
     */
    def stopVirtualMachine(Map args) {
        return post(
                path: String.format("services/hostedservices/%s/deployments/%s/roleinstances/%s/Operations", args.serviceName, args.name, args.name),
                requestContentType: 'application/atom+xml',
                body: {
                    ShutdownRoleOperation(xmlns: "http://schemas.microsoft.com/windowsazure", "xmlns:i": "http://www.w3.org/2001/XMLSchema-instance") {
                        OperationType("ShutdownRoleOperation")
                        PostShutdownAction("StoppedDeallocated")
                    }
                })
    }

    /**
     * Starts the given virtual machine.
     *
     * @param args
     *   name: the name of the virtual machine
     *   serviceName: the name of the cloud service under which the virtual machine exists
     */
    def startVirtualMachine(Map args) {
        return post(
                path: String.format("services/hostedservices/%s/deployments/%s/roleinstances/%s/Operations", args.serviceName, args.name, args.name),
                requestContentType: 'application/atom+xml',
                body: {
                    StartRoleOperation(xmlns: "http://schemas.microsoft.com/windowsazure", "xmlns:i": "http://www.w3.org/2001/XMLSchema-instance") {
                        OperationType("StartRoleOperation")
                    }
                })
    }

    /**
     * Creates a virtual machine.
     * Note that this call is asynchronous.
     * If there are no validation errors, the server returns 202 (Accepted).
     * The request status can be checked via getRequestStatus(requestId).
     *
     * @param args
     *   serviceName: the name of the cloud service under which the virtual machine will be created
     *   name: the name of the virtual machine to create
     *   deploymentSlot: "production" or "staging"
     *   label
     *   virtualNetworkName
     *   imageName: the name of the image from which the disk will be created.  Pick one from the output of getOsImages().
     *   imageStoreUri: the URI under the blob storage where the disk created from image will be stored (path to a new file)
     *   hostname
     *
     *   username: username of the account that gets SSH access
     *   For password-based SSH access, specify "password"; for certificate-based SSH access, specify "sshPublicKeyPath"
     *   and "sshPublicKeyFingerprint":
     *     password: password of the accounts that gets SSH access (do not specify this if you want SSH login)
     *     sshPublicKeyPath:  Specifies the full path of a file, on the Virtual Machine, where the SSH public key is
     *       stored. If the file already exists, the specified key is appended to the file.
     *       E.g., /home/azureuser/.ssh/authorized_keys
     *     sshPublicKeyFingerprint: Specifies the SHA1 fingerprint of an X509 certificate associated with the cloud
     *       service and includes the SSH public key.  Note that you need to upload the corresponding certificate via
     *       createServiceCertificate() first.
     *
     *   subnetName
     *   virtualNetworkName
     *   vmType: specifies the size of the VM.  Can be one of "ExtraSmall", "Small", "Medium", "Large", or "ExtraLarge".
     *
     *   ports: specifies the ports to open.  This is specified as an array of maps with the following keys:
     *     name, port, protocol
     *     For example, [
     *      [name: 'http', port: '80', localPort: '80', protocol: 'tcp', aclRules: [[action: 'deny', remoteSubNet: '0.0.0.0/0']]],
     *      [name: 'https', port: '443', localPort: '443', protocol: 'tcp', aclRules: []]
     *     ]
     */
    def createVirtualMachine(Map args) {
        return post(
                path: String.format("services/hostedservices/%s/deployments", args.serviceName),
                requestContentType: 'application/atom+xml',
                body: {
                    Deployment(xmlns: "http://schemas.microsoft.com/windowsazure", "xmlns:i": "http://www.w3.org/2001/XMLSchema-instance") {
                        Name(args.name)
                        DeploymentSlot(args.deploymentSlot)
                        Label(args.label)
                        RoleList {
                            Role {
                                RoleName(args.name)
                                RoleType('PersistentVMRole')
                                ConfigurationSets {
                                    ConfigurationSet {
                                        ConfigurationSetType('LinuxProvisioningConfiguration')
                                        HostName(args.hostname)
                                        UserName(args.username)
                                        if (args.password) {
                                            UserPassword(args.password)
                                            DisableSshPasswordAuthentication(false)
                                        } else {
                                            DisableSshPasswordAuthentication(true)
                                            SSH {
                                                PublicKeys {
                                                    for (sshKey in args.sshKeys) {
                                                        PublicKey {
                                                            Fingerprint(sshKey.fingerPrint)
                                                            Path(sshKey.publicKeyPath)
                                                        }
                                                    }
                                                }
                                            }
                                            if (args.customData) {
                                                CustomData(args.customData)
                                            }
                                        }
                                    }
                                    ConfigurationSet {
                                        ConfigurationSetType('NetworkConfiguration')
                                        InputEndpoints {
                                            for (port in args.ports) {
                                                InputEndpoint {
                                                    LocalPort(port.localPort)
                                                    Name(port.name)
                                                    Port(port.port)
                                                    Protocol(port.protocol)
                                                    if (port.aclRules.size > 0) {
                                                        EndpointAcl {
                                                            Rules {
                                                                for (int i = 0; i < port.aclRules.size; i++) {
                                                                    Rule {
                                                                        Order(i)
                                                                        Action(port.aclRules[i].action)
                                                                        RemoteSubnet(port.aclRules[i].remoteSubNet)
                                                                        Description(port.aclRules[i].description)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        SubnetNames {
                                            SubnetName(args.subnetName)
                                        }
                                        if (args.virtualNetworkIPAddress) {
                                            StaticVirtualNetworkIPAddress(args.virtualNetworkIPAddress)
                                        }
                                    }

                                }
                                if (args.disks) {
                                    DataVirtualHardDisks {
                                        for (int i = 0; i < args.disks.size; i++) {
                                            DataVirtualHardDisk {
                                                Lun(i)
                                                LogicalDiskSizeInGB(args.disks[i])
                                                MediaLink("http://${args.storageName}.blob.core.windows.net/vhd-store/${args.name}-0${i}.vhd")
                                            }
                                        }
                                    }
                                }
                                OSVirtualHardDisk {
                                    MediaLink(args.imageStoreUri)
                                    SourceImageName(args.imageName)
                                }
                                RoleSize(args.vmType)
                            }
                        }
                        VirtualNetworkName(args.virtualNetworkName)
                        if (args.reservedIpName) {
                            ReservedIPName(args.reservedIpName)
                        }
                    }
                }
        )
    }

    def updateEndpoints(Map args) {
        return put(
                path: String.format("services/hostedservices/%s/deployments/%s/roles/%s", args.name, args.name, args.name),
                requestContentType: 'application/atom+xml',
                body: {
                    PersistentVMRole(xmlns: "http://schemas.microsoft.com/windowsazure", "xmlns:i": "http://www.w3.org/2001/XMLSchema-instance") {
                        RoleName(args.name)
                        RoleType('PersistentVMRole')
                        ConfigurationSets {
                            ConfigurationSet {
                                ConfigurationSetType('NetworkConfiguration')
                                InputEndpoints {
                                    for (port in args.ports) {
                                        InputEndpoint {
                                            LocalPort(port.localPort)
                                            Name(port.name)
                                            Port(port.port)
                                            Protocol(port.protocol)
                                            if (port.aclRules.size > 0) {
                                                EndpointAcl {
                                                    Rules {
                                                        for (int i = 0; i < port.aclRules.size; i++) {
                                                            Rule {
                                                                Order(i)
                                                                Action(port.aclRules[i].action)
                                                                RemoteSubnet(port.aclRules[i].remoteSubNet)
                                                                Description(port.aclRules[i].description)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                SubnetNames {
                                    SubnetName(args.subnetName)
                                }
                                StaticVirtualNetworkIPAddress(args.virtualNetworkIPAddress)
                            }
                        }
                    }
                }
        )
    }

    def addRole(Map args) {
        return post(
                path: String.format("services/hostedservices/%s/deployments/%s/roles", args.serviceName, args.depname),
                requestContentType: 'application/xml',
                body: {
                    PersistentVMRole(xmlns: "http://schemas.microsoft.com/windowsazure", "xmlns:i": "http://www.w3.org/2001/XMLSchema-instance") {
                        RoleName(args.name)
                        RoleType('PersistentVMRole')
                        ConfigurationSets {
                            ConfigurationSet {
                                ConfigurationSetType('LinuxProvisioningConfiguration')
                                HostName(args.hostname)
                                UserName(args.username)
                                if (args.password) {
                                    UserPassword(args.password)
                                    DisableSshPasswordAuthentication(false)
                                } else {
                                    DisableSshPasswordAuthentication(true)
                                    SSH {
                                        PublicKeys {
                                            PublicKey {
                                                Fingerprint(args.sshPublicKeyFingerprint)
                                                Path(args.sshPublicKeyPath)
                                            }
                                        }
                                        /*
                            KeyPairs {
                                KeyPair {
                                    FingerPrint(args.sshKeyPairFingerPrint)
                                    Path(args.sshKeyPairPath)
                                }
                            }
                            */
                                    }
                                    if (args.customData) {
                                        CustomData(args.customData)
                                    }
                                }
                            }
                            ConfigurationSet {
                                ConfigurationSetType('NetworkConfiguration')

                                SubnetNames {
                                    SubnetName(args.subnetName)
                                }
                            }

                        }
                        OSVirtualHardDisk {
                            MediaLink(args.imageStoreUri)
                            SourceImageName(args.imageName)
                        }
                        RoleSize(args.vmType)
                    }
                }
        )

    }

    /**
     * Deletes a virtual machine.
     * Note that this call is asynchronous.
     * If there are no validation errors, the server returns 202 (Accepted).
     * The request status can be checked via getRequestStatus(requestId).
     *
     * @param args
     *   serviceName: the cloud service under which the virtual machine to delete resides.
     *   name: the name of the virtual machine to delete
     */
    def deleteVirtualMachine(Map args) throws HttpResponseException {
        return delete(path: String.format('services/hostedservices/%s/deployments/%s?comp=media', args.serviceName, args.name))
    }

    static String convert(String response) throws XMLStreamException, IOException {
        try {
            String xmlHeader = "xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\""
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
