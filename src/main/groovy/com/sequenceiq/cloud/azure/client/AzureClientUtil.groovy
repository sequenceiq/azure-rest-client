package com.sequenceiq.cloud.azure.client
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException

import static groovyx.net.http.ContentType.TEXT

class AzureClientUtil {

    private static final Log log = LogFactory.getLog( getClass() );

    static def createBlobContainer(String authKey, String targetBlobContainerUri) {
        def RESTClient client = new RESTClient(targetBlobContainerUri)

        def msDateString = getMsDateString()

        def rawAuthString = getRawAuthStringForBlobApi(
                'PUT',
                'x-ms-date:' + msDateString + '\nx-ms-version:' + '2012-02-12',
                getCanonicalizedResource(targetBlobContainerUri) + '\nrestype:container'
        )

        def authHash = getAuthHashForBlobApi(authKey, rawAuthString)

        client.setHeaders(
                'x-ms-date': msDateString,
                'x-ms-version': '2012-02-12',
                'authorization': 'SharedKey ' + getStorageAccountNameFromUri(targetBlobContainerUri) + ':' + authHash,
                // 'Content-Length': 0
                // need to add 'Content-Length' since the underlying HTTP client does not add it for the HEAD request
                // but we assume so when we generate auth string
        )
        def HttpResponseDecorator response = client.put(
                path: targetBlobContainerUri,
                requestContentType: TEXT,
                query: [ restype: 'container' ]
        )
    }

    static def putBlob(String authKey, String targetBlobContainerUri, String containerName, String blobName, String putBody) {
        def RESTClient client = new RESTClient(targetBlobContainerUri)

        def msDateString = getMsDateString()

        def store = getStorageAccountNameFromUri(targetBlobContainerUri)
        def canonicalized = '/' + store + '/' + containerName

        def rawAuthString = getRawAuthStringForBlobApi(
                'PUT',
                'x-ms-date:' + msDateString + '\nx-ms-version:' + '2009-09-19',
                canonicalized + '\ncomp:metadata\nrestype:container\ntimeout:20'
        )

        def authHash = getAuthHashForBlobApi(authKey, rawAuthString)

        client.setHeaders(
                'x-ms-date': msDateString,
                'x-ms-version': '2015-02-21',
                'Content-Type': 'text/plain; charset=UTF-8',
                'Authorization': 'SharedKey ' + getStorageAccountNameFromUri(targetBlobContainerUri) + ':' + authHash,
                'x-ms-blob-type': 'BlockBlob',
                //'Content-Length': putBody.length()
                // need to add 'Content-Length' since the underlying HTTP client does not add it for the HEAD request
                // but we assume so when we generate auth string
        )
        def HttpResponseDecorator response = client.put(
                path: targetBlobContainerUri,
                requestContentType: TEXT,
                body: putBody
        )
        return response
    }

    static private getMsDateString() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.format("EEE, dd MMM yyyy HH:mm:ss z")
    }

    static def copyOsImage(String authKey, String sourceImageUri, String targetImageUri) {
        def RESTClient client = new RESTClient(targetImageUri)

        def msDateString = getMsDateString()

        def rawAuthString = getRawAuthStringForBlobApi(
                'PUT',
                'x-ms-copy-source:' + sourceImageUri + '\nx-ms-date:' + msDateString + '\nx-ms-version:' + '2012-02-12',
                getCanonicalizedResource(targetImageUri)
        )

        def authHash = getAuthHashForBlobApi(authKey, rawAuthString)

        client.setHeaders(
                'x-ms-copy-source': sourceImageUri,
                'x-ms-date': msDateString,
                'x-ms-version': '2012-02-12',
                'authorization': 'SharedKey ' + getStorageAccountNameFromUri(targetImageUri) + ':' + authHash
        )
        client.put(
                path: targetImageUri,
                requestContentType: TEXT,
                contentType: TEXT
                // can't use XML here, because Azure REST Server returns some extra characters (the BOM)
                // and causes the XML parser to throw "org.xml.sax.SAXParseException: Content is not allowed in prolog"
        )
    }

    static def imageCopyProgress(storageAccountKey, targetImageUri) {
        def copyStatus = 'pending'
        while (copyStatus == 'pending') {
            def copyStatusFromServer = getCopyOsImageProgress(storageAccountKey, targetImageUri)
            copyStatus = copyStatusFromServer.status
            log.info 'copy status=' + copyStatusFromServer.status
            log.info 'copy progress=' + copyStatusFromServer.copiedBytes + ' / ' + copyStatusFromServer.totalBytes + ' percentage: ' + (copyStatusFromServer.copiedBytes.toLong() / copyStatusFromServer.totalBytes.toLong() * 100).intValue() + '%'
            sleep(5000)
        }
        if (copyStatus != 'success') {
            throw new Exception('Copy OS image failed with status: ' + copyStatus)
        } else {
            return "DONE"
        }
    }

    static private getAuthHashForBlobApi(String authKey, String rawAuthString) {
        hmacSha256(authKey.decodeBase64(), rawAuthString)
    }

    static def hmacSha256(byte[] key, String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256")
            Mac mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            byte[] digest = mac.doFinal(data.getBytes())
            return digest.encodeBase64().toString()
        } catch (InvalidKeyException ex) {
            throw new RuntimeException("Invalid key exception while converting to HmacSHA256")
        }
    }

    static def getCopyOsImageProgress(String authKey, String targetImageUri) {
        def RESTClient client = new RESTClient(targetImageUri)

        def msDateString = getMsDateString()

        def rawAuthString = getRawAuthStringForBlobApi(
                'HEAD',
                'x-ms-date:' + msDateString + '\nx-ms-version:' + '2012-02-12',
                getCanonicalizedResource(targetImageUri)
        )

        def authHash = getAuthHashForBlobApi(authKey, rawAuthString)

        client.setHeaders(
                'x-ms-date': msDateString,
                'x-ms-version': '2012-02-12',
                'authorization': 'SharedKey ' + getStorageAccountNameFromUri(targetImageUri) + ':' + authHash,
                'Content-Length': 0
                // need to add 'Content-Length' since the underlying HTTP client does not add it for the HEAD request
                // but we assume so when we generate auth string
        )

        def HttpResponseDecorator response = client.head(
                path: targetImageUri,
                requestContentType: TEXT
                // can't use XML here, because Azure REST Server returns some extra characters
                // and causes the XML parser to throw "org.xml.sax.SAXParseException: Content is not allowed in prolog"
        )
        def progress = response.headers.'x-ms-copy-progress'.tokenize('/')

        [ status: response.headers.'x-ms-copy-status', copiedBytes: progress[0], totalBytes: progress[1]  ]
    }

    static private getRawAuthStringForBlobApi(String verb, String canonicalizedHeaders, String canonicalizedResource) {
        def contentEncoding = ''
        def contentLanguage = ''
        def contentLength = 0
        def contentMd5 = ''
        def contentType = ''
        def date = ''
        def ifModifiedSince = ''
        def ifMatch = ''
        def ifNoneMatch = ''
        def ifUnmodifiedSince = ''
        def range = ''

        def rawAuthString =
                verb + '\n' +
                        contentEncoding + '\n' +
                        contentLanguage + '\n' +
                        contentLength + '\n' +
                        contentMd5 + '\n' +
                        contentType + '\n' +
                        date + '\n' +
                        ifModifiedSince + '\n' +
                        ifMatch + '\n' +
                        ifNoneMatch + '\n' +
                        ifUnmodifiedSince + '\n' +
                        range + '\n' +
                        canonicalizedHeaders + '\n' +
                        canonicalizedResource
        rawAuthString
    }

    static private getCanonicalizedResource(String resourceUri) {
        def url = new URL(resourceUri)
        def store = getStorageAccountNameFromUri(resourceUri)
        def path = url.getPath()
        def canonicalized = '/' + store + path
        return canonicalized
    }

    static private getStorageAccountNameFromUri(String uri) {
        new URL(uri).getHost().tokenize('.')[0]
    }
}
