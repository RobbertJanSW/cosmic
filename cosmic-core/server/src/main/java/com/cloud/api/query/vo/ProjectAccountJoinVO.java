package com.cloud.api.query.vo;

import com.cloud.legacymodel.InternalIdentity;
import com.cloud.projects.ProjectAccount.Role;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "project_account_view")
public class ProjectAccountJoinVO extends BaseViewVO implements InternalIdentity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "account_type")
    private short accountType;

    @Column(name = "account_role")
    @Enumerated(value = EnumType.STRING)
    private Role accountRole;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_path")
    private String domainPath;

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    public ProjectAccountJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    public long getDomainId() {
        return domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public String getAccountName() {
        return accountName;
    }

    public short getAccountType() {
        return accountType;
    }

    public Role getAccountRole() {
        return accountRole;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getProjectName() {
        return projectName;
    }
}
