package com.door43.translationstudio.translations;

import android.util.Log;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * This class provides funtionality for syncing a local repository to a remote server as well as interacting with repositories on nearby devices.
 * TODO: this class is super ugly. what we should do is create a list of repositories with labels so you can easily manager repositories. Or perhaps just create an instance for each repository.
 */
public class GitSync {
    private final String TAG = "GitSync";
    private String mRepoPath;
    Git mGit;

//    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Opens a git repository and initializes it if nessesary.
     * @param repoDir the absolute path to the repository
     */
    public void openRepo(File repoDir) {
        mRepoPath = repoDir.getPath();
        if(!repoDir.exists()) {
            repoDir.mkdir();
        }

        // initialize git repo
        InitCommand init = Git.init();
        File initFile = new File(mRepoPath);
        init.setDirectory(initFile);
        try {
            init.call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

    /**
     * Pushes a local repository to the remote server
     * @param remote
     */
    public void pushToRemote(String remote) {
        Log.d(TAG, "need to push the repo to the server");
    }

    /**
     * Saves the translated text to a file within the repository.
     * @param translation the translated text
     * @param path the relative path to the file within the repository
     */
    public void updateFile(String translation, String path) {
        File newFile = new File(mRepoPath+"/"+path);

        // create new folder structure
        if(!newFile.exists()) {
            newFile.getParentFile().mkdir();
        }

        // TODO: check if file exists. if not create the directories and the file.
        try {
            newFile.createNewFile();
            PrintStream ps = new PrintStream(newFile);
            ps.print(translation);
            ps.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // add to git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File f = new File(mRepoPath+"/.git");
        Repository db = null;
        try {
            db = builder.setGitDir(f)
                    .findGitDir() // scan up the file system tree
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Git git = new Git(db);
        AddCommand add = git.add();
        try {
            add.addFilepattern(".").call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        // commit the change
        CommitCommand commit = git.commit();
        commit.setAll(true);
        commit.setMessage("a fancy git commit message");
        try {
            commit.call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        // check the log
//        try {
//            for (RevCommit c : git.log().call()) {
//                Log.d(TAG, c.getId() + "/" + c.getAuthorIdent().getName() + "/"
//                        + c.getShortMessage());
//            }
//        } catch (GitAPIException e) {
//            e.printStackTrace();
//        }
    }


//    @Before
//    public void setUp() throws GitAPIException {
//        mGit = Git.init().setDirectory( tempFolder.getRoot() ).call();
//        File folder = new File( mGit.getRepository().getWorkTree(), "folder" );
//        folder.mkdir();
//        file = new File( folder, "file" );
//    }

//    @After
//    public void tearDown() {
//        mGit.getRepository().close();
//    }

//    private void writeFile( String content ) throws IOException {
//        FileOutputStream outputStream = new FileOutputStream( file );
//        outputStream.write( content.getBytes( "UTF-8" ) );
//        outputStream.close();
//    }
//
//    private void commitAll( String message ) throws GitAPIException {
//        mGit.add().addFilepattern( "." ).call();
//        mGit.commit().setMessage( message ).call();
//    }

}
