package com.qiankubx.common.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "qianku.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String cdnDomain;
    private Dirs dirs = new Dirs();

    @Data
    public static class Dirs {
        private String invoice = "invoice/";
        private String reimbursement = "reimbursement/";
        private String merged = "merged/";
        private String zip = "zip/";
        private String poster = "poster/";
    }

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
