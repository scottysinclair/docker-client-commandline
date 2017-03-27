package com.scott.docker.commandline;

import com.scott.docker.commandline.DockerHelper.AbnormalProgramTerminationException;

public class ExecuteCommand {

    public static void main(String args[]) {
    try {
            final String host = args[0];
            final String containerName = args[1];
            final String fullCommand = args[2];

            DockerHelper docker = DockerHelper.newDockerHelper( host );

            docker.executeCommand(containerName, fullCommand, System.out, System.err);
            return;
    }
    catch(AbnormalProgramTerminationException x) {
        x.printStackTrace(System.err);
        System.exit(-1);
    }
    catch(Exception x) {
        printUsage();
        x.printStackTrace(System.err);
        System.exit(-1);
    }
    }

    private static void printUsage() {
    System.out.println("Usage: <host> <container name> <full command>");
        System.out.println("EG: tcp:localhost:4243 batch java -jar batch.jar com.app.Main");
    }

}
