package com.cloudops.optimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "optimizer")
public class OptimizerProperties {

    private Aws aws = new Aws();
    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Scan scan = new Scan();
    private Rules rules = new Rules();
    private Pricing pricing = new Pricing();
    private Reports reports = new Reports();

    public Aws getAws() {
        return aws;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Scan getScan() {
        return scan;
    }

    public Rules getRules() {
        return rules;
    }

    public Pricing getPricing() {
        return pricing;
    }

    public Reports getReports() {
        return reports;
    }

    public static class Aws {
        private String region = "ap-south-1";

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
        private String cookieName = "ACCESS_TOKEN";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }
    }

    public static class Admin {
        private String email;
        private String password;
        private String name = "Platform Admin";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Scan {
        private boolean enabled;
        private String cron = "0 0 2 * * *";
        private int s3ObjectSampleLimit = 200;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getS3ObjectSampleLimit() {
            return s3ObjectSampleLimit;
        }

        public void setS3ObjectSampleLimit(int s3ObjectSampleLimit) {
            this.s3ObjectSampleLimit = s3ObjectSampleLimit;
        }
    }

    public static class Rules {
        private double idleCpuPercent = 10.0;
        private int idleLookbackDays = 7;
        private int s3GlacierAgeDays = 90;
        private double s3StorageThresholdGb = 50.0;

        public double getIdleCpuPercent() {
            return idleCpuPercent;
        }

        public void setIdleCpuPercent(double idleCpuPercent) {
            this.idleCpuPercent = idleCpuPercent;
        }

        public int getIdleLookbackDays() {
            return idleLookbackDays;
        }

        public void setIdleLookbackDays(int idleLookbackDays) {
            this.idleLookbackDays = idleLookbackDays;
        }

        public int getS3GlacierAgeDays() {
            return s3GlacierAgeDays;
        }

        public void setS3GlacierAgeDays(int s3GlacierAgeDays) {
            this.s3GlacierAgeDays = s3GlacierAgeDays;
        }

        public double getS3StorageThresholdGb() {
            return s3StorageThresholdGb;
        }

        public void setS3StorageThresholdGb(double s3StorageThresholdGb) {
            this.s3StorageThresholdGb = s3StorageThresholdGb;
        }
    }

    public static class Pricing {
        private double unusedEipMonthlyUsd = 3.65;
        private double ebsGbMonthUsd = 0.08;
        private double s3StandardGbMonthUsd = 0.023;
        private double s3GlacierGbMonthUsd = 0.004;

        public double getUnusedEipMonthlyUsd() {
            return unusedEipMonthlyUsd;
        }

        public void setUnusedEipMonthlyUsd(double unusedEipMonthlyUsd) {
            this.unusedEipMonthlyUsd = unusedEipMonthlyUsd;
        }

        public double getEbsGbMonthUsd() {
            return ebsGbMonthUsd;
        }

        public void setEbsGbMonthUsd(double ebsGbMonthUsd) {
            this.ebsGbMonthUsd = ebsGbMonthUsd;
        }

        public double getS3StandardGbMonthUsd() {
            return s3StandardGbMonthUsd;
        }

        public void setS3StandardGbMonthUsd(double s3StandardGbMonthUsd) {
            this.s3StandardGbMonthUsd = s3StandardGbMonthUsd;
        }

        public double getS3GlacierGbMonthUsd() {
            return s3GlacierGbMonthUsd;
        }

        public void setS3GlacierGbMonthUsd(double s3GlacierGbMonthUsd) {
            this.s3GlacierGbMonthUsd = s3GlacierGbMonthUsd;
        }
    }

    public static class Reports {
        private String outputDir = "./data/reports";

        public String getOutputDir() {
            return outputDir;
        }

        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }
    }
}
