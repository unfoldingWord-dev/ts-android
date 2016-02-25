package com.door43.gogs;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    public static final String API_ROOT = "https://git.door43.org/api/v1/";

    public ApplicationTest() {
        super(Application.class);
    }

    public void test1SearchRepos() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        int limit = 2;
        List<Repository> repos = api.searchRepos("uw", 0, limit);
        assertTrue(repos.size() > 0);
        assertTrue(repos.size() <= limit);
    }

    public void test2ListRepos() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        // CAUTION: do not commit with username and password
//        User user = new User("valid name", "valid password");
//        List<Repository> repos =  api.listRepos(user);
//        assertTrue(repos.size() > 0);

        User badUser = new User("fake", "fake");
        List<Repository> emptyRepos =  api.listRepos(badUser);
        assertEquals(emptyRepos.size(), 0);
    }

    public void test3CreateRepo() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        // CAUTION: do not commit with username and password
//        User user = new User("valid name", "valid password");
//        Repository repoSpec = new Repository("demo", "This is a demo repo", false);
//        Repository repo = api.createRepo(repoSpec, user);
//        assertNotNull(repo);
//        assertEquals(repo.getFullName(), user.getUsername() + "/" + repoSpec.getName());

        Repository badRepoSpec = new Repository("failed-demo", "This is a failed demo repo", false);
        User badUser = new User("fake", "fake");
        Repository badRepo = api.createRepo(badRepoSpec, badUser);
        assertNull(badRepo);
    }

    public void test4ListTokens() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        // CAUTION: do not commit with username and password
//        User user = new User("valid name", "valid password");
//        List<Token> tokens = api.listTokens(user);
//        assertTrue(tokens.size() > 0);

        User badUser = new User("fake", "fake");
        List<Token> badTokens = api.listTokens(badUser);
        assertEquals(badTokens.size(), 0);
    }
}