package com.cloud.user;

import com.cloud.api.command.admin.account.UpdateAccountCmd;
import com.cloud.api.command.admin.user.DeleteUserCmd;
import com.cloud.api.command.admin.user.UpdateUserCmd;
import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.legacymodel.acl.ControlledEntity;
import com.cloud.legacymodel.exceptions.ConcurrentOperationException;
import com.cloud.legacymodel.exceptions.ResourceUnavailableException;
import com.cloud.legacymodel.user.Account;
import com.cloud.legacymodel.user.User;
import com.cloud.legacymodel.user.UserAccount;
import com.cloud.legacymodel.utils.Pair;
import com.cloud.legacymodel.utils.Ternary;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * AccountManager includes logic that deals with accounts, domains, and users.
 */
public interface AccountManager extends AccountService {
    public static final String MESSAGE_ADD_ACCOUNT_EVENT = "Message.AddAccount.Event";
    public static final String MESSAGE_REMOVE_ACCOUNT_EVENT = "Message.RemoveAccount.Event";

    /**
     * Disables an account by accountId
     *
     * @param accountId
     * @return true if disable was successful, false otherwise
     */
    boolean disableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException;

    boolean deleteAccount(AccountVO account, long callerUserId, Account caller);

    Long checkAccessAndSpecifyAuthority(Account caller, Long zoneId);

    Account createAccount(String accountName, short accountType, Long domainId, String networkDomain, Map<String, String> details, String uuid);

    /**
     * Logs out a user
     *
     * @param userId
     */
    void logoutUser(long userId);

    /**
     * Authenticates a user when s/he logs in.
     *
     * @param username          required username for authentication
     * @param password          password to use for authentication, can be null for single sign-on case
     * @param domainId          id of domain where user with username resides
     * @param requestParameters the request parameters of the login request, which should contain timestamp of when the request signature is
     *                          made, and the signature itself in the single sign-on case
     * @return a user object, null if the user failed to authenticate
     */
    UserAccount authenticateUser(String username, String password, Long domainId, InetAddress loginIpAddress, Map<String, Object[]> requestParameters);

    /**
     * Locate a user by their apiKey
     *
     * @param apiKey that was created for a particular user
     * @return the user/account pair if one exact match was found, null otherwise
     */
    Pair<User, Account> findUserByApiKey(String apiKey);

    boolean enableAccount(long accountId);

    void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> sb, Long domainId,
                               boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

    void buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> sb, Long domainId,
                                   boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

    void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc,
                                Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

    void buildACLSearchParameters(Account caller, Long id,
                                  String accountName, Long projectId, List<Long> permittedAccounts, Ternary<Long, Boolean, ListProjectResourcesCriteria>
                                          domainIdRecursiveListProject, boolean listAll,
                                  boolean forProjectInvitation);

    void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> sc,
                                    Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

    /**
     * Deletes a user by userId
     *
     * @param accountId - id of the account do delete
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUserAccount(long accountId);

    /**
     * Updates an account
     *
     * @param cmd - the parameter containing accountId or account nameand domainId
     * @return updated account object
     */
    Account updateAccount(UpdateAccountCmd cmd);

    /**
     * Disables an account by accountName and domainId
     *
     * @param accountName
     * @param domainId
     * @param accountId
     * @param disabled    account if success
     * @return true if disable was successful, false otherwise
     */
    Account disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * Enables an account by accountId
     *
     * @param accountName - the enableAccount command defining the accountId to be deleted.
     * @param domainId    TODO
     * @param accountId
     * @return account object
     */
    Account enableAccount(String accountName, Long domainId, Long accountId);

    /**
     * Deletes user by Id
     *
     * @param deleteUserCmd
     * @return
     */
    boolean deleteUser(DeleteUserCmd deleteUserCmd);

    /**
     * Update a user by userId
     *
     * @param userId
     * @return UserAccount object
     */
    UserAccount updateUser(UpdateUserCmd cmd);

    /**
     * Disables a user by userId
     *
     * @param userId - the userId
     * @return UserAccount object
     */
    UserAccount disableUser(long userId);

    /**
     * Enables a user
     *
     * @param userId - the userId
     * @return UserAccount object
     */
    UserAccount enableUser(long userId);

    /**
     * Locks an account by accountId. A locked account cannot access the API, but will still have running VMs/IP
     * addresses
     * allocated/etc.
     *
     * @param accountName - the LockAccount command defining the accountId to be locked.
     * @param domainId    TODO
     * @param accountId
     * @return account object
     */
    Account lockAccount(String accountName, Long domainId, Long accountId);
}
