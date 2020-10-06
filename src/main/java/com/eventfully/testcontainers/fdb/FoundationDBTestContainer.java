package com.eventfully.testcontainers.fdb;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;

// TODO: Fixed port? How to avoid?
public class FoundationDBTestContainer extends FixedHostPortGenericContainer<FoundationDBTestContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FoundationDBTestContainer.class);
    private static final String FDB_NETWORKING_MODE_KEY = "FDB_NETWORKING_MODE";
    public static final int FDB_PORT = 4500;

    private static final String FDB_VERSION = "6.2.22";
    private static final String FDB_IMAGE = "foundationdb/foundationdb";
    private File clusterFile;

    public FoundationDBTestContainer() {
        this(FDB_VERSION);
    }

    public FoundationDBTestContainer(String fdbVersion) {
        super(FDB_IMAGE + ":" + fdbVersion);
        // withExposedPorts(FDB_PORT);
        withFixedExposedPort(FDB_PORT, FDB_PORT);
        withEnv(FDB_NETWORKING_MODE_KEY, "host");
        withFileSystemBind("./etc", "/etc/foundationdb");
        // waitingFor(Wait.forListeningPort());
        waitingFor(Wait.forLogMessage(".*FDBD joined cluster.*\\n", 1));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        try {
            Container.ExecResult initResult = execInContainer("fdbcli", "--exec", "configure new single memory");
            String stdout = initResult.getStdout();
            LOGGER.info("init FDB stdout: " + stdout);
            int exitCode = initResult.getExitCode();
            LOGGER.info("init FDB exit code: " + exitCode);

            boolean fdbReady = false;
            LOGGER.info("waiting for FDB to be healthy");

            // waiting for fdb to be up and healthy
            while (!fdbReady) {
                Container.ExecResult statusResult = execInContainer("fdbcli", "--exec", "status minimal");
                stdout = statusResult.getStdout();

                if (stdout.contains("database is available")) {
                    fdbReady = true;
                    LOGGER.info("fdb is healthy");
                } else {
                    LOGGER.debug("fdb is unhealthy");
                    Thread.sleep(1_000);
                }
            }

            clusterFile = File.createTempFile("fdb", ".cluster");
//            File internal = File.createTempFile("fdb", ".cluster");
//            String content = "docker:docker@127.0.0.1:" + getMappedPort(4500);
//            Files.write(clusterFile.toPath(), content.getBytes());

            // copyFileFromContainer("/var/fdb/fdb.cluster", internal.getAbsolutePath());
            copyFileFromContainer("/var/fdb/fdb.cluster", clusterFile.getAbsolutePath());

            // System.out.println(new String(Files.readAllBytes(internal.toPath())));
            //System.out.println(new String(Files.readAllBytes(clusterFile.toPath())));
        } catch (InterruptedException | IOException e) {
            LOGGER.error("error starting FoundationDB test container", e);
        }
    }

    /**
     * A hook that is executed after the container is stopped with {@link #stop()}.
     * Warning! This hook won't be executed if the container is terminated during
     * the JVM's shutdown hook or by Ryuk.
     *
     * @param containerInfo
     */
    @Override
    protected void containerIsStopped(InspectContainerResponse containerInfo) {
        super.containerIsStopped(containerInfo);
        clusterFile.delete();
    }

    public File getClusterFile() {
        return clusterFile;
    }


}
