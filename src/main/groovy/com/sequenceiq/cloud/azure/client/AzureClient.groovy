package com.sequenceiq.cloud.azure.client

import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
/**
 * Azure cloud REST client - http://msdn.microsoft.com/library/azure/ee460799.aspx
 */
class AzureClient {

	def slurper = new JsonSlurper();
	def RESTClient azure
	def subscriptionId
	boolean debugEnabled = false;
	
	def String getSubscriptionId() {
		subscriptionId
	}
	
	def setSubscriptionId(subscriptionId) {
		this.subscriptionId = subscriptionId
	}
	public static void main(String[] args) {

		if (args.size == 1) {
			setSubscriptionId( args[0])
			
		}

		AzureClient client = new AzureClient()
		

	}
}
