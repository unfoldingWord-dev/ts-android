package com.door43.translationstudio.git;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;

/**
 * Created by joel on 11/6/2015.
 */
public class SSHSession {

    private SSHSession() {

    }

    /**
     * Connections the session
     * @return
     * @throws JSchException
     * @throws UnsupportedEncodingException
     */
    public static Channel openSession(String user, String server, int port) throws JSchException, UnsupportedEncodingException {
        JSch jsch = new JSch();
        // configure keys
        File sshDir = AppContext.context().getKeysFolder();
        for (File file : sshDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                String[] pieces = s.split("\\.");
                if(pieces.length >= 2) {
                    String ext = pieces[pieces.length - 1];
                    return !ext.equals("pub");
                } else {
                    return true;
                }
            }
        })) {
            jsch.addIdentity(file.getAbsolutePath());
        }

        // configure session
        Session session = jsch.getSession(user, server);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPort(port);
        session.connect();

        // open channel
        Channel channelssh = session.openChannel("shell");
        channelssh.connect();
        return channelssh;
    }




}
