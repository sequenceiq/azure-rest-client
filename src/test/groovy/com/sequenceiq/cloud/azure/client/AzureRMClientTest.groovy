package com.sequenceiq.cloud.azure.client

import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class AzureRMClientTest extends Specification {
    AzureRMClient azureClientV2 = new AzureRMClient("", "", "", "")

    void "test resourcegroup list"() {
        when:
        List<Map<String, String>> groups = azureClientV2.getResourceGroups()
        System.out.print(groups)
        then:
        noExceptionThrown()
    }

    void "test get resourcegroup"() {
        when:
        List<Map<String, String>> groups = azureClientV2.getResourceGroup("krisztian1")
        System.out.print(groups)
        then:
        noExceptionThrown()
    }

    void "test create resourcegroup"() {
        when:
        def group = azureClientV2.createResourceGroup("test123ere", "West US")
        System.out.print(group)
        then:
        noExceptionThrown()
    }

    void "list template deployments"() {
        when:
        def group = azureClientV2.getTemplateDeployments("krisztian1")
        System.out.print(group)
        then:
        noExceptionThrown()
    }

    void "get template deployment"() {
        when:
        def group = azureClientV2.getTemplateDeployment("azuretest6", "azuretest6")
        System.out.print(group)
        then:
        noExceptionThrown()
    }

    void "tets dsfsdfsd"() {
        when:
        def tesdfsdfsdfst = azureClientV2Classic.listDisks()
        System.out.print(tesdfsdfsdfst)
        then:
        noExceptionThrown()
    }

    void "tets getstorages"() {
        when:
        def result = azureClientV2.getStorageAccounts()
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "tets getstorages for resourcegroup"() {
        when:
        def result = azureClientV2.getStorageAccountsForResourceGroup("krisztian1")
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test create storage for resourcegroup"() {
        when:
        def result = azureClientV2.createStorageAccount("krisztian1", "sdfdsf24123123", "West US")
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test get storage keys for resourcegroup"() {
        when:
        Map<String, Object> result = azureClientV2.getStorageAccountKeys("krisztian1", "sdfdsf24123123")
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test create container for resourcegroup storage"() {
        when:
        def result = azureClientV2.createContainerInStorage("krisztian1", "sdfdsf24123123", "templates")
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test create blob for resourcegroup storage container"() {
        when:
        azureClientV2.createTemplateBlobInStorageContainer("krisztian1", "sdfdsf24123123", "azuredeploy2.json", new File('/Users/rdoktorics/prj/azuredeploy2.json').text)
        azureClientV2.createTemplateBlobInStorageContainer("krisztian1", "sdfdsf24123123", "azuredeploy-parameters2.json", new File('/Users/rdoktorics/prj/azuredeploy-parameters2.json').text)
        then:
        noExceptionThrown()
    }

    void "test public container"() {
        when:
        azureClientV2.setPublicPermissionOnContainer("krisztian1", "sdfdsf24123123", "templates")
        then:
        noExceptionThrown()
    }

    void "test deployment"() {
        when:
        Map<String, Object> result = azureClientV2.createTemplateDeployment("ricsitesztel", "ricsideployment", new File('/Users/rdoktorics/prj/ricsitest.json').text, new File('/Users/rdoktorics/prj/azuredeploy-parameters2.json').text)
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test delete deployment"() {
        when:
        azureClientV2.deleteTemplateDeployment("ricsitesztel", "ricsideployment")
        then:
        noExceptionThrown()
    }

    void "test cancel deployment"() {
        when:
        azureClientV2.cancelTemplateDeployments("ricsitesztel", "ricsideployment")
        then:
        noExceptionThrown()
    }

    void "test deployment operations"() {
        when:
        Map<String, Object> result = azureClientV2.getTemplateDeploymentOperations("azuretest6", "azuretest6")
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test deployment operation"() {
        when:
        Map<String, Object> result = azureClientV2.getTemplateDeploymentOperation("krisztian1", "ricsideployment", "AA75777F089331BC")
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test whole stack"() {
        when:
        Map<String, Object> result = azureClientV2.createWholeStack("West US", "ricsitesztel", "ricsideployment", new File('/Users/rdoktorics/prj/ricsitest.json').text, new File('/Users/rdoktorics/prj/azuredeploy-parameters2.json').text)
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "test list vms"() {
        when:
        Map<String, Object> result = azureClientV2.getVirtualMachines("ricsitesztel")
        System.out.print(result)
        then:
        noExceptionThrown()
    }

    void "stop vms"() {
        when:
        azureClientV2.stopVirtualMachines("ricsitesztel")
        then:
        noExceptionThrown()
    }

    void "start vms"() {
        when:
        azureClientV2.startVirtualMachines("ricsitesztel")
        then:
        noExceptionThrown()
    }

    void "get vms"() {
        when:
        def machine = azureClientV2.getVirtualMachine("azuretest6", "azuretest6cbgateway0")
        System.out.println(machine);
        then:
        noExceptionThrown()
    }

    void "get vms veiw"() {
      when:
      def machine = azureClientV2.getVirtualMachineInstanceView("azuretest6", "azuretest6cbgateway0")
      System.out.println(machine);
      then:
      noExceptionThrown()
    }

    void "deallocate vms"() {
        when:
        azureClientV2.deallocateVirtualMachine("ricsitesztel", "teststack1vmslave11")
        then:
        noExceptionThrown()
    }

    void "list vms"() {
      when:
      def machine = azureClientV2.getVirtualMachines("azuretest6")
      System.out.println(machine);
      then:
      noExceptionThrown()
    }

    void "delete vms"() {
        when:
        azureClientV2.deleteVirtualMachine("ricsitesztel", "teststack1vmslave11")
        then:
        noExceptionThrown()
    }

    void "delete ip"() {
        when:
        azureClientV2.deletePublicIpAddress("ricsitesztel", "teststack1ipslave11")
        then:
        noExceptionThrown()
    }

    void "delete networkinterface"() {
        when:
        azureClientV2.deleteNetworkInterface("ricsitesztel", "teststack1netinterfslave11")
        then:
        noExceptionThrown()
    }
}
