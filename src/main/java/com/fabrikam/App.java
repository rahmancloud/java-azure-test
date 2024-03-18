package com.fabrikam;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.AzureAuthorityHosts;
import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.storage.models.PublicEndpoints;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.common.StorageSharedKeyCredential;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {

        try {
            // TokenCredential tokenCredential = new DefaultAzureCredentialBuilder()
            // .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
            // .build();

            AzureCliCredential cliCredential = new AzureCliCredentialBuilder().build();
            // Use the Azure CLI credential to authenticate.

            // ManagedIdentityCredential managedIdentityCredential = new
            // ManagedIdentityCredentialBuilder()
            // .clientId("<user-assigned managed identity client ID>") // required only for
            // user-assigned
            // .build();

            // If you don't set the tenant ID and subscription ID via environment variables,
            // change to create the Azure profile with tenantId, subscriptionId, and Azure
            // environment.
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(cliCredential, profile)
                    .withDefaultSubscription();

            // Create a new storage account.
            Map<String, String> env = System.getenv();
            String storageAccountName = env.get("AZURE_STORAGE_ACCOUNT_NAME");
            String storageResourceGroup = env.get("AZURE_STORAGE_RESOURCE_GROUP");
            StorageAccount storage = azureResourceManager.storageAccounts().define(storageAccountName)
                    .withRegion(Region.US_WEST2)
                    .withNewResourceGroup(storageResourceGroup)
                    .create();

            // Create a storage container to hold the file.
            List<StorageAccountKey> keys = storage.getKeys();
            PublicEndpoints endpoints = storage.endPoints();
            String accountName = storage.name();
            String accountKey = keys.get(0).value();
            String endpoint = endpoints.primary().blob();

            StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

            BlobServiceClient storageClient = new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .credential(credential)
                    .buildClient();

            // Container name must be lowercase.
            BlobContainerClient blobContainerClient = storageClient.getBlobContainerClient("helloazure");
            // blobContainerClient.create();

            // Make the container public.
            blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER, null);

            // Write a blob to the container.
            // String fileName = "helloazure6.txt";
            // String textNew = "Hello Azure";
            // BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
            // InputStream is = new ByteArrayInputStream(textNew.getBytes());
            // blobClient.upload(is, textNew.length());

            // Generate a SAS token for the container.
            BlobContainerSasPermission permissions = new BlobContainerSasPermission().setReadPermission(true)
                    .setWritePermission(true);
            OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(1); // SAS token expires in 1 day
            BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime,
                    permissions);
            String sasToken = blobContainerClient.generateSas(sasSignatureValues);
            System.out.println("SAS Token: " + sasToken);

            // Create a new BlobServiceClient with the SAS token
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .sasToken(sasToken)
                    .buildClient();

            // List the blobs in the container
            blobContainerClient.listBlobs().forEach(blobItem -> System.out.println(blobItem.getName()));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
