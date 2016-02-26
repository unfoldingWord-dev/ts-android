package com.door43.gogs;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    // CAUTION: do not commit admin user with username and password
    private static final User adminUser = new User("", ""); // add a valid admin account for testing
    private static final String DEMO_USER_NAME = "demo-user-001";
    private static final User demoUser = new User(DEMO_USER_NAME, "demo-user-001");
    private static final User fakeUser = new User("fake-user-001", "fake-user-001");
    private static final Token demoToken = new Token("demo-token-001");
    private static final Repository demoRepo = new Repository("demo-repo-001", "This is a demo repo", false);
    private static final String API_ROOT = "https://git.door43.org/api/v1/";
    private static final GogsAPI api = new GogsAPI(API_ROOT);
    private static final PublicKey demoKey = new PublicKey("demo-public-key", "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDUbmwBOG5vI8qNCztby5LDc9ozwTuwsqf+1fpuHjT9iQ2Lu9nlKHQJcPSgdrYAcc+88K6o74ayhTAjfajKxkIHnbzZFjidoVZSQDhX5qvl93jvY/Uz390qky0sweW+fspm8pRJL+ofE3QEN5AXAuycq1tgsRT32XC+Ta82Xyv8b3xW+pWbsZzYCzUsZXDe/xWxg1rndXh2BIrmcYf9BMiv9ZJIojJXfuLCeRXl550tDzaMFC0rQ/T5pZjs/lQemtg92MnxnEDi5nhuvDwM4Q8eqCTOXc4BCE7iyIHv+B7rx+0x99ytMh5BSIIGyWTfgTot/AjGVm5aRKJSRFgPBm9N comment with whitespace");

    static {
        demoUser.email = "demo@example.com";
    }

    public ApplicationTest() {
        super(Application.class);
    }

    public void test01CreateUser() throws Exception {
        User createdUser = api.createUser(demoUser, adminUser, false);
        assertNotNull(createdUser);
        assertEquals(createdUser.getUsername(), demoUser.getUsername());

        User anotherNewUser = new User("demo-user-that-is-not-created", "bla bla bla");
        User badUser = api.createUser(anotherNewUser, null, false);
        assertNull(badUser);
    }

    public void test02SearchUsers() throws Exception {
        List<User> users = api.searchUsers(DEMO_USER_NAME, 5, null);
        assertTrue(users.size() > 0);
        assertTrue(users.get(0).email.isEmpty());

        List<User> completeUsers = api.searchUsers("ef", 5, adminUser);
        assertTrue(completeUsers.size() > 0);
        assertTrue(!completeUsers.get(0).email.isEmpty());
    }

    public void test03GetUser() throws Exception {
        User completeUser = api.getUser(demoUser, adminUser);
        assertNotNull(completeUser);
        assertTrue(!completeUser.email.isEmpty());

        User user = api.getUser(demoUser, null);
        assertNotNull(user);
        assertTrue(user.email.isEmpty());
    }

    public void test04SearchRepos() throws Exception {
        int limit = 2;
        List<Repository> repos = api.searchRepos("uw", 0, limit);
        assertTrue(repos.size() > 0);
        assertTrue(repos.size() <= limit);
    }

    public void test05CreateRepo() throws Exception {
        Repository repo = api.createRepo(demoRepo, demoUser);
        assertNotNull(repo);
        assertEquals(repo.getFullName(), demoUser.getUsername() + "/" + demoRepo.getName());

        Repository badRepoSpec = new Repository("repo-never-created", "This is never created", false);
        Repository badRepo = api.createRepo(badRepoSpec, fakeUser);
        assertNull(badRepo);
    }

    public void test06ListRepos() throws Exception {
        List<Repository> repos = api.listRepos(demoUser);
        assertTrue(repos.size() > 0);
        assertEquals(repos.get(0).getFullName(), demoUser.getUsername() + "/" + demoRepo.getName());

        // unknown user returns empty set
        List<Repository> emptyRepos =  api.listRepos(fakeUser);
        assertEquals(emptyRepos.size(), 0);
    }

    public void test07CreateToken() throws Exception {
        Token token = api.createToken(demoToken, demoUser);
        assertNotNull(token);
        assertEquals(token.getName(), demoToken.getName());

        Token badToken = api.createToken(demoToken, null);
        assertNull(badToken);
    }

    public void test08ListTokens() throws Exception {
        List<Token> tokens = api.listTokens(demoUser);
        assertTrue(tokens.size() > 0);
        assertEquals(tokens.get(0).getName(), demoToken.getName());

        List<Token> badTokens = api.listTokens(fakeUser);
        assertEquals(badTokens.size(), 0);
    }

    public void test09CreatePublicKey() throws Exception {
        PublicKey key = api.createPublicKey(demoKey, demoUser);
        assertNotNull(key);
        assertEquals(key.getTitle(), demoKey.getTitle());
    }

    public void test10ListPublicKeys() throws Exception {
        List<PublicKey> keys = api.listPublicKeys(demoUser);
        assertTrue(keys.size() > 0);
        assertEquals(keys.get(0).getTitle(), demoKey.getTitle());
    }

    public void test10GetPublicKey() throws Exception {
        // get key id first
        List<PublicKey> keys = api.listPublicKeys(demoUser);
        PublicKey key = new PublicKey(keys.get(0).getId());

        PublicKey fetchedKey = api.getPublicKey(key, demoUser);
        assertNotNull(fetchedKey);
        assertEquals(fetchedKey.getTitle(), demoKey.getTitle());
    }

    public void test11DeletePublicKey() throws Exception {
        // get key id first
        List<PublicKey> keys = api.listPublicKeys(demoUser);
        PublicKey key = new PublicKey(keys.get(0).getId());

        assertTrue(api.deletePublicKey(key, demoUser));
        assertEquals(api.listPublicKeys(demoUser).size(), 0);
    }

    public void test12DeleteRepo() throws Exception {
        assertTrue(api.deleteRepo(demoRepo, demoUser));
        assertEquals(api.listRepos(demoUser).size(), 0);

        // unknown user is not an error
        assertTrue(api.deleteRepo(demoRepo, fakeUser));

        // unknown repo is not an error
        Repository fakeRepo = new Repository("fake-repository", "", false);
        assertTrue(api.deleteRepo(fakeRepo, demoUser));
    }

    public void test13DeleteUser() throws Exception {
        assertTrue(api.deleteUser(demoUser, adminUser));
        assertNull(api.getUser(demoUser, adminUser));

        // unknown user is not an error on delete
        assertTrue(api.deleteUser(fakeUser, adminUser));

        // users cannot delete themselves
        assertFalse(api.deleteUser(fakeUser, fakeUser));
    }
}