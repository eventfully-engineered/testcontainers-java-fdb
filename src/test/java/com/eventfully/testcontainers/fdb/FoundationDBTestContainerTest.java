package com.eventfully.testcontainers.fdb;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Testcontainers
class FoundationDBTestContainerTest {

    @Container
    private static final FoundationDBTestContainer FDB_CONTAINER = new FoundationDBTestContainer();

    private FDB fdb = FDB.selectAPIVersion(620);

    // TODO: run test?
    // FDB_CONTAINER.isRunning()?

    @Test
    void shouldSuccessfullyConnect() throws InterruptedException, ExecutionException, TimeoutException {

        try (Database database = fdb.open(FDB_CONTAINER.getClusterFile().getAbsolutePath())) {
            byte[] status = database.read(rt -> {
                rt.options().setTimeout(2_000);
                return rt.get("\\xff\\xff/status/json".getBytes());
            }).get(1, TimeUnit.SECONDS);

            System.out.println(new String(status));
        }

    }

}
