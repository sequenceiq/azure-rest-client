package com.sequenceiq.cloud.azure.client

import spock.lang.Specification

class AzureClientFunctionalTest extends Specification {

    // fill all data if you want to test your changes
    final String subscriptionId = "subscriptionId"
    final String keyStorePath = "keyStorePath"
    final String keyStorePassword = "keyStorePassword"
    final Random rand = new Random()
    final String clusterName = "test" + rand.nextInt(Integer.MAX_VALUE)
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
        def label = vmName.bytes.encodeBase64().toString()
        def virtualMachineResponse = azureClient.createVirtualMachine(
                name: vmName,
                serviceName: vmName,
                deploymentSlot: 'production',
                label: label,
                imageName: 'c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6',
                imageStoreUri: String.format('http://%s.blob.core.windows.net/vhd-store/%s.vhd', clusterName, vmName),
                hostname: vmName,
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
