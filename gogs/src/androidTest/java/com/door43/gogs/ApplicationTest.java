package com.door43.gogs;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    private static final String API_ROOT = "https://git.door43.org/api/v1/";
    // CAUTION: do not commit with username and password
    private static final String AUTH_USER = "";
    private static final String AUTH_PASS = "";
    private static final boolean ENABLE_AUTH_CHECK = false;

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
        if(ENABLE_AUTH_CHECK) {
            User user = new User(AUTH_USER, AUTH_PASS);
            List<Repository> repos = api.listRepos(user);
            assertTrue(repos.size() > 0);
        }

        User badUser = new User("fake", "fake");
        List<Repository> emptyRepos =  api.listRepos(badUser);
        assertEquals(emptyRepos.size(), 0);
    }

    public void test3CreateRepo() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        if(ENABLE_AUTH_CHECK) {
            User user = new User(AUTH_USER, AUTH_PASS);
            Repository repoSpec = new Repository("demo", "This is a demo repo", false);
            Repository repo = api.createRepo(repoSpec, user);
            assertNotNull(repo);
            assertEquals(repo.getFullName(), user.getUsername() + "/" + repoSpec.getName());
        }

        Repository badRepoSpec = new Repository("failed-demo", "This is a failed demo repo", false);
        User badUser = new User("fake", "fake");
        Repository badRepo = api.createRepo(badRepoSpec, badUser);
        assertNull(badRepo);
    }

    public void test4ListTokens() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        if(ENABLE_AUTH_CHECK) {
            User user = new User(AUTH_USER, AUTH_PASS);
            List<Token> tokens = api.listTokens(user);
            assertTrue(tokens.size() > 0);
        }

        User badUser = new User("fake", "fake");
        List<Token> badTokens = api.listTokens(badUser);
        assertEquals(badTokens.size(), 0);
    }

    public void test5SearchUsers() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        List<User> users = api.searchUsers("joel", 5, null);
        assertTrue(users.size() > 0);
        assertTrue(users.get(0).email.isEmpty());

        if(ENABLE_AUTH_CHECK) {
            User authUser = new User(AUTH_USER, AUTH_PASS);
            List<User> completeUsers = api.searchUsers("ef", 5, authUser);
            assertTrue(completeUsers.size() > 0);
            assertTrue(!completeUsers.get(0).email.isEmpty());
        }
    }

    public void test6GetUser() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        User findUser = new User("richmahn", "");

        if(ENABLE_AUTH_CHECK) {
            User authUser = new User(AUTH_USER, AUTH_PASS);
            User completeUser = api.getUser(findUser, authUser);
            assertNotNull(completeUser);
            assertTrue(!completeUser.email.isEmpty());
        }

        User user = api.getUser(findUser, null);
        assertNotNull(user);
        assertTrue(user.email.isEmpty());
    }

    public void test7CreateUser() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        User newUser = new User("joel-demo", "joel-demo");
        newUser.email = "da1nerd2@gmail.com";
        if(ENABLE_AUTH_CHECK) {
            User authUser = new User(AUTH_USER, AUTH_PASS);
            User createdUser = api.createUser(newUser, authUser, true);
            assertNotNull(createdUser);
            assertEquals(createdUser.getUsername(), newUser.getUsername());
        }

        User anotherNewUser = new User("joel-demo2", "joel-demo2");
        User badUser = api.createUser(anotherNewUser, null, false);
        assertNull(badUser);
    }

    public void test8CreateToken() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        Token newToken = new Token("demo-token");
        if(ENABLE_AUTH_CHECK) {
            User user = new User(AUTH_USER, AUTH_PASS);
            Token token = api.createToken(newToken, user);
            assertNotNull(token);
            assertEquals(token.getName(), token.getName());
        }

        Token token = api.createToken(newToken, null);
        assertNull(token);
    }

    public void test9DeleteUser() throws Exception {
        GogsAPI api = new GogsAPI(API_ROOT);
        User user = new User("joel-demo", "joel-demo");
        assertTrue(api.deleteUser(user));

        // unknown users is not an error on delete
        User fakeUser = new User("fake", "fake");
        assertTrue(api.deleteUser(fakeUser));
    }
}