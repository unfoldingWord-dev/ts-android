package com.door43.translationstudio.git;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by joel on 9/15/2014.
 */
public class GitSessionFactory extends JschConfigSessionFactory {
    @Override
    protected void configure(Host arg0, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey,password");
        session.setPort(Integer.parseInt(App.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER_PORT, App.context().getResources().getString(R.string.pref_default_git_server_port))));
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        JSch jsch = new JSch();
        File sshDir = App.getKeysFolder();
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
        return jsch;
    }
}
