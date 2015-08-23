package com.sequenceiq.cloud.azure.client

import com.microsoft.aad.adal4j.AuthenticationCallback
import com.microsoft.aad.adal4j.AuthenticationContext
import com.microsoft.aad.adal4j.AuthenticationResult
import com.microsoft.aad.adal4j.ClientCredential
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.BlobContainerPermissions
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.microsoft.azure.storage.blob.CloudPageBlob
import com.microsoft.azure.storage.blob.CopyState
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class AzureRMClient extends RESTClient {

    def JsonSlurper jsonSlurper = new JsonSlurper()

    def tenant = "tenant"
    def accessKey = "accesKey"
    def secretKey = "secretKey"
    def subscriptionId = "subscriptionId"

    def AzureRMClient(String _tenant, String _accessKey, String _secretKey, String _subscriptionId) {
        super(String.format("https://management.azure.com/subscriptions/%s/", _subscriptionId))
        this.tenant = _tenant;
        this.accessKey = _accessKey;
        this.secretKey = _secretKey;
        this.subscriptionId = _subscriptionId;

        this.client.params.setParameter("http.socket.timeout", new Integer(20000))
        this.client.params.setParameter("http.connection.timeout", new Integer(20000))
    }

    def get(Map args) {
        String token = getToken();
        this.headers['Authorization'] = 'Bearer ' + token

        def argsClone = args.clone()
        argsClone.path = args.route
        argsClone.query = ['api-version': argsClone.apiversion]

        argsClone.remove('route')
        argsClone.remove('apiversion')

        setContentType(ContentType.JSON)
        return super.get(argsClone)
    }

    /**
     * Overrides the RESTClient's post method behavior so that temporary redirects cause automatic retries.
     */
    def post(Map args) {
        String token = getToken();
        this.headers['Authorization'] = 'Bearer ' + token

        def argsClone = args.clone()
        argsClone.path = args.route
        argsClone.query = ['api-version': argsClone.apiversion]

        argsClone.remove('route')
        argsClone.remove('apiversion')

        if (argsClone.body != null) {
            argsClone.body = new JsonBuilder(argsClone.body).toPrettyString()
        }
        def HttpResponseDecorator response = super.post(argsClone)
        while (response.getStatus() == 307) {
            sleep(1000)
            argsClone = args.clone()
            response = super.post(argsClone)
        }
        return response
    }

    /**
     * Overrides the RESTClient's put method behavior so that temporary redirects cause automatic retries.
     */
    def put(Map args) {
        String token = getToken();
        this.headers['Authorization'] = 'Bearer ' + token

        def argsClone = args.clone()
        argsClone.path = args.route
        argsClone.query = ['api-version': argsClone.apiversion]

        argsClone.remove('route')
        argsClone.remove('apiversion')

        if (argsClone.body != null) {
            argsClone.body = new JsonBuilder(argsClone.body).toPrettyString()
        }
        this.defaultRequestContentType = ContentType.JSON
        def response = super.put(argsClone)
        return response
    }

    def rawput(Map args) {
        String token = getToken();
        this.headers['Authorization'] = 'Bearer ' + token

        def argsClone = args.clone()
        argsClone.path = args.route
        argsClone.query = ['api-version': argsClone.apiversion]

        argsClone.remove('route')
        argsClone.remove('apiversion')
        this.defaultRequestContentType = ContentType.JSON
        def response = super.put(argsClone)
        return response
    }

    /**
     * Overrides the RESTClient's delete method behavior so that temporary redirects cause automatic retries.
     */
    def delete(Map args) throws HttpResponseException {
        String token = getToken();
        this.headers['Authorization'] = 'Bearer ' + token

        def argsClone = args.clone()
        argsClone.path = args.route
        argsClone.query = ['api-version': argsClone.apiversion]

        argsClone.remove('route')
        argsClone.remove('apiversion')

        def HttpResponseDecorator response = super.delete(argsClone)
        if (response.getStatus() == 404) {
            throw new HttpResponseException("Resource not found");
        }
        while (response.getStatus() == 307) {
            sleep(1000)
            argsClone = args.clone()
            response = super.delete(argsClone)
        }
        return response
    }

    def getToken() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        AuthenticationContext authenticationContext = new AuthenticationContext("https://login.windows.net/" + tenant, true, executorService);
        ClientCredential credential = new ClientCredential(accessKey, secretKey);
        Future<AuthenticationResult> authenticationResultFuture = authenticationContext.acquireToken("https://management.azure.com/", credential, new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                result.getAccessToken();
            }

            @Override
            public void onFailure(Throwable exc) {
            }
        });
        def result = authenticationResultFuture.get()
        executorService.shutdown();
        return result.getAccessToken();
    }

    Map<String, Object> getResourceGroup(String name) throws Exception {
        Map<String, Object> result = get(route: String.format("resourcegroups/%s", name), apiversion: '2015-01-01').responseData;
        return result;
    }

    List<Map<String, Object>> getResourceGroups() throws Exception {
        List<Map<String, Object>> result = get(route: "resourcegroups", apiversion: '2015-01-01').responseData.value;
        return result;
    }

    def deleteResourceGroup(String name) throws Exception {
        return delete(route: String.format("resourcegroups/%s", name), apiversion: '2015-01-01')
    }

    Map<String, Object> createResourceGroup(String name, String region) throws Exception {
        Map<String, Object> result = put(route: String.format("resourcegroups/%s", name), body: {location(region)}, apiversion: '2015-01-01').responseData;
        return result;
    }

    Map<String, Object> createTemplateDeployment(String resourceGroupName, String deploymentName, String rmTemplateContent, String rmParameterContent) throws Exception {
        Map<String, Object> result = rawput(route: String.format("resourcegroups/%s/providers/microsoft.resources/deployments/%s", resourceGroupName, deploymentName), body: "{ \"properties\": { \"template\": " + rmTemplateContent + ",\"mode\":\"Incremental\", \"parameters\": " + rmParameterContent + "}}", apiversion: '2015-01-01').responseData
        return result;
    }

    Map<String, Object> createWholeStack(String location, String resourceGroupName, String deploymentName, String rmTemplateContent, String rmParameterContent) throws Exception {
        createResourceGroup(resourceGroupName, location);
        Map<String, Object> result = createTemplateDeployment(resourceGroupName, deploymentName, rmTemplateContent, rmParameterContent);
        return result;
    }

    def deleteTemplateDeployment(String resourceGroupName, String deploymentName) throws Exception {
        return delete(route: String.format("resourcegroups/%s/providers/microsoft.resources/deployments/%s", resourceGroupName, deploymentName), apiversion: '2015-01-01')
    }

    Map<String, Object> getTemplateDeployment(String resourceGroupName, String deploymentName) throws Exception {
        Map<String, Object> result = get(route: String.format("resourcegroups/%s/providers/microsoft.resources/deployments/%s", resourceGroupName, deploymentName), apiversion: '2015-01-01').responseData;
        return result;
    }

    Map<String, Object> getTemplateDeploymentOperations(String resourceGroupName, String deploymentName) throws Exception {
        Map<String, Object> result = get(route: String.format("resourcegroups/%s/providers/microsoft.resources/deployments/%s/operations", resourceGroupName, deploymentName), apiversion: '2015-01-01').responseData;
        return result;
    }

    Map<String, Object> getTemplateDeploymentOperation(String resourceGroupName, String deploymentName, String operation) throws Exception {
        Map<String, Object> result = get(route: String.format("resourcegroups/%s/providers/microsoft.resources/deployments/%s/operations/%s", resourceGroupName, deploymentName, operation), apiversion: '2015-01-01').responseData;
        return result;
    }

    def cancelTemplateDeployments(String resourceGroupName, String deploymentName) throws Exception {
        return post(route: String.format("resourcegroups/%s/providers/microsoft.resources/deployments/%s/cancel", resourceGroupName, deploymentName), apiversion: '2015-01-01')
    }

    Map<String, Object> getTemplateDeployments(String resourceGroupName) throws Exception {
        Map<String, Object> result = get(route: String.format("resourcegroups/%s/providers/microsoft.resources/deployments", resourceGroupName), apiversion: '2015-01-01').responseData;
        return result;
    }

    ArrayList<Map<String, Object>> getStorageAccounts() throws Exception {
        Map<String, Object> result = get(route: String.format("providers/Microsoft.Storage/storageAccounts"), apiversion: '2015-05-01-preview').responseData;
        return result.get("value");
    }

    ArrayList<Map<String, Object>> getStorageAccountsForResourceGroup(String resourceGroup) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/Microsoft.Storage/storageAccounts", resourceGroup), apiversion: '2015-05-01-preview').responseData;
        return result.get("value");
    }

    def deleteStorageAccount(String resourceGroup, String storageName) throws Exception {
        return delete(route: String.format("resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s", resourceGroup, storageName), apiversion: '2015-05-01-preview');
    }

    def createStorageAccount(String resourceGroup, String storageName, String storageLocation) throws Exception {
        return put(route: String.format("resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s", resourceGroup, storageName), apiversion: '2015-05-01-preview', body: {
            location(storageLocation)
            properties {
                accountType("Standard_GRS")
            }
        });
    }

    Map<String, Object> getStorageAccountKeys(String resourceGroup, String storageName) throws Exception {
        Map<String, Object> result = post(route: String.format("resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s/listKeys", resourceGroup, storageName), apiversion: '2015-05-01-preview').responseData;
        return result;
    }

    def createContainerInStorage(String resourceGroup, String storageName, String containerName) throws Exception {
        def keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get("key1"));
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.createIfNotExists();
        setPublicPermissionOnContainer(resourceGroup, storageName, containerName);
    }

    def setPublicPermissionOnContainer(String resourceGroup, String storageName, String containerName) throws Exception {
        def keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get("key1"));
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
        containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
        container.uploadPermissions(containerPermissions);
    }

    def copyImageBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String sourceBlob) throws Exception{
        def keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get("key1"));
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.split("/").last());
        cloudPageBlob.startCopyFromBlob(sourceBlob);
    }

    CopyState getCopyStatus(String resourceGroup, String storageName, String containerName, String sourceBlob) throws Exception{
        def keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get("key1"));
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.split("/").last());
        cloudPageBlob.getCopyState();
    }

    /*def createTemplateBlobInStorageContainer(String resourceGroup, String storageName, String blobName, String blobContent) throws Exception{
        def keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get("key1"));
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("templates");
        container.createIfNotExists();
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        blob.upload(new ByteArrayInputStream(blobContent.getBytes()), blobContent.length());
    }*/

    def deleteTemplateBlobInStorageContainer(String resourceGroup, String storageName, String blobName) throws Exception {
        deleteBlobInStorageContainer(resourceGroup, storageName, "templates", blobName);
    }

    def deleteBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String blobName) throws Exception {
        def keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get("key1"));
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        blob.deleteIfExists();
    }

    Map<String, Object> getVirtualMachines(String resourceGroup) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/Microsoft.Compute/virtualmachines", resourceGroup), apiversion: '2015-05-01-preview').responseData;
        return result;
    }

    Map<String, Object> getVirtualMachine(String resourceGroup, String vmName) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s", resourceGroup, vmName), apiversion: '2015-05-01-preview').responseData;
        return result;
    }

    Map<String, Object> getVirtualMachineInstanceView(String resourceGroup, String vmName) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/InstanceView", resourceGroup, vmName), apiversion: '2015-05-01-preview').responseData;
        return result;
    }

    def deallocateVirtualMachine(String resourceGroup, String vmName) throws Exception {
        def result = post(route: String.format("resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/deallocate", resourceGroup, vmName), apiversion: '2014-12-01-preview');
        return result;
    }

    def deleteVirtualMachine(String resourceGroup, String vmName) throws Exception {
        def result = delete(route: String.format("resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s", resourceGroup, vmName), apiversion: '2014-12-01-preview');
        return result;
    }

    def startVirtualMachine(String resourceGroup, String vmName) throws Exception {
        def result = post(route: String.format("resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/start", resourceGroup, vmName), apiversion: '2014-12-01-preview');
        return result;
    }

    def startVirtualMachines(String resourceGroup) throws Exception {
        Map<String, Object> result = getVirtualMachines(resourceGroup);
        List<Map<String, Object>> list = result.get("value");
        for (Map<String, Object> map : list) {
            startVirtualMachine(resourceGroup, map.get("name"));
        }
    }

    def stopVirtualMachine(String resourceGroup, String vmName) throws Exception {
        def result = post(route: String.format("resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/stop", resourceGroup, vmName), apiversion: '2014-12-01-preview');
        return result;
    }

    def stopVirtualMachines(String resourceGroup) {
        Map<String, Object> result = getVirtualMachines(resourceGroup);
        List<Map<String, Object>> list = result.get("value");
        for (Map<String, Object> map : list) {
            stopVirtualMachine(resourceGroup, map.get("name"));
        }
    }

    def deletePublicIpAddress(String resourceGroup, String ipName) throws Exception {
        def result = delete(route: String.format("resourceGroups/%s/providers/microsoft.network/publicIPAddresses/%s", resourceGroup, ipName), apiversion: '2014-12-01-preview');
        return result;
    }

    def getPublicIpAddress(String resourceGroup, String ipName) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/microsoft.network/publicIPAddresses/%s", resourceGroup, ipName), apiversion: '2014-12-01-preview').responseData;
        return result;
    }

    def getPublicIpAddresses(String resourceGroup) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/microsoft.network/publicIPAddresses", resourceGroup), apiversion: '2014-12-01-preview').responseData;
        List<Map<String, Object>> list = result.get("value");
        return list;
    }

    def deleteNetworkInterface(String resourceGroup, String networkInterfaceName) throws Exception {
        def result = delete(route: String.format("resourceGroups/%s/providers/microsoft.network/networkInterfaces/%s", resourceGroup, networkInterfaceName), apiversion: '2014-12-01-preview');
        return result;
    }

    def getNetworkInterface(String resourceGroup, String networkInterfaceName) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/microsoft.network/networkInterfaces/%s", resourceGroup, networkInterfaceName), apiversion: '2014-12-01-preview').responseData;
        return result;
    }

    def getNetworkInterfaces(String resourceGroup) throws Exception {
        Map<String, Object> result = get(route: String.format("resourceGroups/%s/providers/microsoft.network/networkInterfaces", resourceGroup), apiversion: '2014-12-01-preview').responseData;
        List<Map<String, Object>> list = result.get("value");
        return list;
    }


}
