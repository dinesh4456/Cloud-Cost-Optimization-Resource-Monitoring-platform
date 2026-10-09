package com.cloudops.optimizer.aws.scanner;

import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import java.util.List;

public interface ResourceScanner {

    List<ResourceSnapshot> scan(ScanJob job) throws Exception;
}
