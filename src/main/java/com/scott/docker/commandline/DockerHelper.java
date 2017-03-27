package com.scott.docker.commandline;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

public class DockerHelper implements Closeable {

    public static class AbnormalProgramTerminationException extends Exception {
    public AbnormalProgramTerminationException(String message){
        super(message);
    }
    }

    public static DockerHelper newDockerHelper(String hostUrl) throws IOException {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(hostUrl)
        .build();

    JerseyDockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory();
    dockerCmdExecFactory/*.withReadTimeout(60 * 1000)*/.withConnectTimeout(1000).withMaxTotalConnections(100)
        .withMaxPerRouteConnections(10);

    DockerClient docker = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(dockerCmdExecFactory)
        .build();

    return new DockerHelper(docker);
    }

    private DockerClient docker;

    private DockerHelper(DockerClient docker) {
    this.docker = docker;
    }

    @Override
    public void close() throws IOException {
    docker.close();
    }

    public Container getContainer(String containerName) {
    ListContainersCmd listContainers = docker.listContainersCmd();
    for (Container container : listContainers.exec()) {
        for (String name : container.getNames()) {
        if (name.equals('/' + containerName)) {
            return container;
        }
        }

    }
    throw new IllegalStateException("Could not find batch container");
    }

    public void executeCommand(String containerName, String command, PrintStream out, PrintStream err) throws AbnormalProgramTerminationException {

    Container container = getContainer(containerName);

    ExecCreateCmdResponse execCreateResponse = docker.execCreateCmd(container.getId()).withAttachStdout(true)
        .withAttachStderr(true).withTty(false).withCmd(command).exec();

    try {
        docker.execStartCmd(execCreateResponse.getId())
                .withDetach(false)
                .exec(new ExecStartResultCallback(out, err))
            .awaitCompletion()
            .onError(new IllegalStateException("Error on executing command"));


        InspectExecResponse inspect = docker.inspectExecCmd(execCreateResponse.getId())
            .exec();
        if (inspect.isRunning()) {
        throw new IllegalStateException("The command is still running, even though we waited for completion.");
        }
        if (inspect.getExitCode() != 0) {
        throw new AbnormalProgramTerminationException("The command '" + command + "' on container '" + containerName + "' did not exit normally.");
        }



    } catch (InterruptedException e) {
        throw new IllegalStateException("Wait interrupted while executing integration job");
    }
    finally {
        out.flush();
        err.flush();
    }

    }

}
