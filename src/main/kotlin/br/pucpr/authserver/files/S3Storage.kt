package br.pucpr.authserver.files

import br.pucpr.authserver.users.User
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class S3Storage : FileStorage {
    private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
        .withRegion(Regions.US_EAST_1)
        .withCredentials(EnvironmentVariableCredentialsProvider())
        .build()

    override fun save(user: User, path: String, file: MultipartFile) {
        val contentType = file.contentType!!

        val meta = ObjectMetadata()
        meta.contentType = contentType
        meta.contentLength = file.size
        meta.userMetadata["userId"] = "${user.id}"
        meta.userMetadata["originalFilename"] = file.originalFilename

        val transferManager = TransferManagerBuilder.standard()
            .withS3Client(s3)
            .build()
        transferManager
            .upload(THUMB, path, file.inputStream, meta)
            .waitForUploadResult()
    }

    override fun load(path: String): Resource? = InputStreamResource(
        s3.getObject(PUBLIC, path.replace("--", "/")).objectContent
    )

    override fun urlFor(name: String) = "$PREFIX/$name"

    companion object {
        private const val THUMB = "vinigodoy-authserver-thumb"
        private const val PUBLIC = "vinigodoy-authserver-public"
        private const val PREFIX = "https://vinigodoy-authserver-public2.s3.us-east-1.amazonaws.com"
    }
}