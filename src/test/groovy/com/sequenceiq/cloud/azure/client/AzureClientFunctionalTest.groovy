package com.sequenceiq.cloud.azure.client

import spock.lang.Specification

class AzureClientFunctionalTest extends Specification {

    // fill all data if you want to test your changes
    final String subscriptionId = "subscriptionId"
    final String keyStorePath = "keyStorePath"
    final String keyStorePassword = "keyStorePassword"
    final String clusterName = "azureRestTest123"
    AzureClient azureClient

    def setup() {
        azureClient = new AzureClient(subscriptionId, keyStorePath, keyStorePassword)
        deleteResources()
    }

    def cleanup() {
        deleteResources()
    }

    def deleteResources() {
        try {
            azureClient.deleteAffinityGroup(name: clusterName)
        } catch (e) {
            println('Resource not exist')
        }
    }


    void "T001: create affinity group test"() {
        when:
        def response = azureClient.createAffinityGroup(name: clusterName, description: 'test group', location: 'East US')
        then:
        assert response.isSuccess() == true
    }

}
