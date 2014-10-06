package com.door43.translationstudio.git;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;

/**
 * Created by joel on 9/15/2014.
 */
public class TransportCallback implements TransportConfigCallback {
    private GitSessionFactory ssh;

    public TransportCallback() {
        ssh = new GitSessionFactory();
    }

    @Override
    public void configure(Transport tn) {
        if (tn instanceof SshTransport) {
            ((SshTransport) tn).setSshSessionFactory(ssh);
        }
    }
}
