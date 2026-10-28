package com.dreamuniverse_server.image.service;
import com.dreamuniverse_server.global.config.R2Properties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AccountImageDeletionTest {
    @Test void listsOnlyOwnedDreamPrefixAndDeletesAllPages() {
        var client=mock(S3Client.class);
        var request=ListObjectsV2Request.builder().bucket("test").prefix("dreams/7/").build();
        when(client.listObjectsV2Paginator(request)).thenReturn(new ListObjectsV2Iterable(client,request));
        when(client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(
            ListObjectsV2Response.builder().contents(S3Object.builder().key("dreams/7/old.png").build()).isTruncated(true).nextContinuationToken("next").build(),
            ListObjectsV2Response.builder().contents(S3Object.builder().key("dreams/7/new.png").build()).isTruncated(false).build());
        new ImageStorageService(client,mock(S3Presigner.class),new R2Properties("account","key","secret","test")).deleteDreamPrefix(7L);
        verify(client).deleteObject(DeleteObjectRequest.builder().bucket("test").key("dreams/7/old.png").build());
        verify(client).deleteObject(DeleteObjectRequest.builder().bucket("test").key("dreams/7/new.png").build());
        verify(client,times(2)).listObjectsV2(any(ListObjectsV2Request.class));
    }
    @Test void failedDeleteIsPropagatedInsteadOfReportingSuccess() {
        var client=mock(S3Client.class);
        when(client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(503).build());
        var service=new ImageStorageService(client,mock(S3Presigner.class),new R2Properties("account","key","secret","test"));
        assertThrows(IllegalStateException.class,()->service.delete("dreams/7/test.png"));
    }
}
