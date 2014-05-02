package com.sequenceiq.cloud.azure.client
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
import com.thoughtworks.xstream.io.xml.XppReader
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import javax.xml.stream.XMLStreamException
/**
 * Azure cloud REST client - http://msdn.microsoft.com/library/azure/ee460799.aspx
 */
class AzureClient {
    static String subscriptionId = "subscriptionId";
    static String keyStorePath = "WindowsAzureKeyStore.jks";
    static String keyStorePassword = "password";
    def slurper = new JsonSlurper();

    boolean debugEnabled = false;

    def String getSubscriptionId() {
        subscriptionId
    }

    def setSubscriptionId(subscriptionId) {
        this.subscriptionId = subscriptionId
    }

    static void main(String[] args) {
        def slurper = new JsonSlurper();
        if (args.size == 1) {
            setSubscriptionId(args[0]);
        }

        //List locations
        String jsonResponse = convert(getLocations())
        if(jsonResponse != null) {
            System.out.println(JsonOutput.prettyPrint(jsonResponse));
        }

    }

    static String getLocations() {
        String url = String.format("https://management.core.windows.net/%s/locations", subscriptionId);
        String response = ServiceManagementHelper.processGetRequest(new URL(url), keyStorePath, keyStorePassword);
        return response;
    }

    static String convert(String response) throws XMLStreamException, IOException {
        try {
            String xmlHeader= "xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"";
            HierarchicalStreamReader sourceReader = new XppReader(new StringReader(response.toString().replaceAll(xmlHeader, "")));

            StringWriter buffer = new StringWriter();
            JettisonMappedXmlDriver jettisonDriver = new JettisonMappedXmlDriver();
            jettisonDriver.createWriter(buffer);
            HierarchicalStreamWriter destinationWriter = jettisonDriver.createWriter(buffer);

            HierarchicalStreamCopier copier = new HierarchicalStreamCopier();
            copier.copy(sourceReader, destinationWriter);
           return buffer.toString();
        } catch (Exception ex) {
            System.out.println(ex.getMessage())
            return null;
        }

    }
}
