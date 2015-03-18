package com.sequenceiq.cloud.azure.main;

import static com.sequenceiq.cloud.azure.main.VersionedApplication.versionedApplication;

import java.io.IOException;

public class AzureClientMain {

    public static void main(String[] args) throws IOException {
        versionedApplication().showVersionInfo(args);
    }
}
