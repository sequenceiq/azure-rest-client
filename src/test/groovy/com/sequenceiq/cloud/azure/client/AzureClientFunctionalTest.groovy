package com.sequenceiq.cloud.azure.client

import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import spock.lang.Ignore
import spock.lang.Specification

class AzureClientFunctionalTest extends Specification {

    // fill all data if you want to test your changes
    final String subscriptionId = "sdsdfdfs"
    final String keyStorePath = "sdfsdf"
    final String keyStorePassword = "ssdfsdfsdf"
    final Random rand = new Random()
    final JsonSlurper jsonSlurper = new JsonSlurper()
    final String clusterName = "test" + rand.nextInt(9999)
    AzureClient azureClient

    def setup() {
        azureClient = new AzureClient(subscriptionId, keyStorePath, keyStorePassword)
    }

    def cleanup() {
        deleteResources()
    }

    def deleteResources() {
        try {
            azureClient.deleteVirtualMachine(serviceName: String.format("%s-1", clusterName), name: String.format("%s-1", clusterName))
        } catch (e) {
            println('Resource virtual machine not exist: ' + String.format("%s-1", clusterName))
        }
        try {
            azureClient.deleteCloudService(name: String.format("%s-1", clusterName))
        } catch (e) {
            println('Resource Cloud service not exist: ' + String.format("%s-1", clusterName))
        }
        try {
            azureClient.deleteVirtualNetwork(name: clusterName)
        } catch (e) {
            println('Resource Virtual network not exist:' + clusterName)
        }
        try {
            azureClient.deleteStorageAccount(name: clusterName)
        } catch (e) {
            println('Resource Storage account not exist: ' + clusterName)
        }
        try {
            azureClient.deleteAffinityGroup(name: clusterName)
        } catch (e) {
            println('Resource affinity group not exist: ' + clusterName)
        }
    }

    @Ignore
    void "test image available"() {
        when:
        boolean result = azureClient.isImageAvailable('seqambdocker')
        then:
        result == true
    }

    @Ignore
    void "test instance stopping with resource deallocation"() {
        when:
        def vm = "start-stop-test-11412153225036-11412153269963"
        def context = ["serviceName": vm,
                       "name"       : vm]
        azureClient.stopVirtualMachine(context)

        then:
        noExceptionThrown()
    }

    @Ignore
    void "test instance start"() {
        when:
        def vm = "start-stop-test-11412153225036-01412153233868"
        def context = ["serviceName": vm,
                       "name"       : vm]
        azureClient.startVirtualMachine(context)

        then:
        noExceptionThrown()
    }

    @Ignore
    void "test image creation"() {
        when:
        def baseImageUri = 'http://vmdepoteastus.blob.core.windows.net/linux-community-store/community-62091-a59dcdc1-d82d-4e76-9094-27b8c018a4a1-1.vhd'
        def osImageName = 'seqambdocker'
        def nameI = clusterName
        azureClient.createAffinityGroup(name: nameI, description: nameI, location: 'East US')
        def HttpResponseDecorator response = azureClient.createStorageAccount(name: nameI, description: 'Created by ' + nameI, affinityGroup: nameI)
        azureClient.waitUntilComplete(azureClient.getRequestId(response))
        def targetBlobContainerUri = 'http://' + nameI + '.blob.core.windows.net/vm-images'
        def targetImageUri = targetBlobContainerUri + '/' + nameI + '.vhd'
        def keyJson = azureClient.getStorageAccountKeys(name: nameI)
        def storageAccountKey = jsonSlurper.parseText(keyJson).StorageService.StorageServiceKeys.Primary

        //Create the blob container to hold the image copy
        AzureClientUtil.createBlobContainer(storageAccountKey, targetBlobContainerUri)

        // Copy the public os image to the storage account
        AzureClientUtil.copyOsImage(storageAccountKey, baseImageUri, targetImageUri)
        AzureClientUtil.imageCopyProgress(storageAccountKey, targetImageUri)
        azureClient.addOsImage('name': osImageName, 'mediaLink': targetImageUri, 'os': 'Linux')
        then:
        assert true == true
    }

    @Ignore
    void "T001: create all resource end to end"() {
        when:
        println("The cluster name will be:" + clusterName)
        def affinityResponse = azureClient.createAffinityGroup(name: clusterName, description: 'test group', location: 'East US')
        def requestId = azureClient.getRequestId(affinityResponse)
        azureClient.waitUntilComplete(requestId)
        def storageResponse = azureClient.createStorageAccount(name: clusterName, description: 'Created by test porpuses', affinityGroup: clusterName)
        requestId = azureClient.getRequestId(storageResponse)
        azureClient.waitUntilComplete(requestId)
        def virtualNetworkResponse = azureClient.createVirtualNetwork(
                name: clusterName,
                affinityGroup: clusterName,
                addressPrefix: '172.16.0.0/16',
                subnetName: clusterName,
                subnetAddressPrefix: '172.16.0.0/24'
        )
        requestId = azureClient.getRequestId(virtualNetworkResponse)
        azureClient.waitUntilComplete(requestId)
        String vmName = String.format("%s-1", clusterName)
        def cloudServiceResponse = azureClient.createCloudService(name: vmName, description: 'Created by ' + clusterName, affinityGroup: clusterName)
        requestId = azureClient.getRequestId(cloudServiceResponse)
        azureClient.waitUntilComplete(requestId)
        def label = (vmName + "12").bytes.encodeBase64().toString()
        def virtualMachineResponse = azureClient.createVirtualMachine(
                name: vmName + "12",
                serviceName: vmName,
                deploymentSlot: 'production',
                label: label,
                imageName: 'c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6',
                imageStoreUri: String.format("http://" + clusterName + ".blob.core.windows.net/vhd-store/" + vmName + ".vhd"),
                hostname: vmName + "12",
                username: 'azureuser',
                password: 'Password!@#$',
                disableSshPasswordAuthentication: false,
                subnetName: clusterName,
                virtualNetworkName: clusterName,
                vmType: 'Small'
        )
        requestId = azureClient.getRequestId(virtualMachineResponse)
        azureClient.waitUntilComplete(requestId)
        then:
        assert storageResponse.isSuccess() == true
        assert affinityResponse.isSuccess() == true
        assert virtualNetworkResponse.isSuccess() == true
        assert cloudServiceResponse.isSuccess() == true
        assert virtualMachineResponse.isSuccess() == true
    }
}
