package com.cloud.vpc;

import com.cloud.legacymodel.configuration.Resource.ResourceType;
import com.cloud.legacymodel.configuration.ResourceCount;
import com.cloud.legacymodel.configuration.ResourceLimit;
import com.cloud.legacymodel.domain.Domain;
import com.cloud.legacymodel.exceptions.ResourceAllocationException;
import com.cloud.legacymodel.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.component.ManagerBase;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MockResourceLimitManagerImpl extends ManagerBase implements ResourceLimitService {

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#updateResourceLimit(java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Long)
     */
    @Override
    public ResourceLimit updateResourceLimit(final Long accountId, final Long domainId, final Integer resourceType, final Long max) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#recalculateResourceCount(java.lang.Long, java.lang.Long, java.lang.Integer)
     */
    @Override
    public List<? extends ResourceCount> recalculateResourceCount(final Long accountId, final Long domainId, final Integer typeId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#searchForLimits(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Long, java.lang.Long)
     */
    @Override
    public List<? extends ResourceLimit> searchForLimits(final Long id, final Long accountId, final Long domainId, final Integer type, final Long startIndex, final Long
            pageSizeVal) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#findCorrectResourceLimitForAccount(com.cloud.legacymodel.user.Account, com.cloud.legacymodel.configuration.Resource.ResourceType)
     */
    @Override
    public long findCorrectResourceLimitForAccount(final Account account, final ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long findCorrectResourceLimitForAccount(final long accountId, final Long limit, final ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#findCorrectResourceLimitForDomain(com.cloud.legacymodel.domain.Domain, com.cloud.legacymodel.configuration.Resource.ResourceType)
     */
    @Override
    public long findCorrectResourceLimitForDomain(final Domain domain, final ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#incrementResourceCount(long, com.cloud.legacymodel.configuration.Resource.ResourceType, java.lang.Long[])
     */
    @Override
    public void incrementResourceCount(final long accountId, final ResourceType type, final Long... delta) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#decrementResourceCount(long, com.cloud.legacymodel.configuration.Resource.ResourceType, java.lang.Long[])
     */
    @Override
    public void decrementResourceCount(final long accountId, final ResourceType type, final Long... delta) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#checkResourceLimit(com.cloud.legacymodel.user.Account, com.cloud.legacymodel.configuration.Resource.ResourceType, long[])
     */
    @Override
    public void checkResourceLimit(final Account account, final ResourceType type, final long... count) throws ResourceAllocationException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#getResourceCount(com.cloud.legacymodel.user.Account, com.cloud.legacymodel.configuration.Resource.ResourceType)
     */
    @Override
    public long getResourceCount(final Account account, final ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void checkResourceLimit(final Account account, final ResourceType type, final Boolean displayResource, final long... count) throws ResourceAllocationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void incrementResourceCount(final long accountId, final ResourceType type, final Boolean displayResource, final Long... delta) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void changeResourceCount(final long accountId, final ResourceType type, final Boolean displayResource, final Long... delta) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void decrementResourceCount(final long accountId, final ResourceType type, final Boolean displayResource, final Long... delta) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#countCpusForAccount(long)
     */
    public long countCpusForAccount(final long accountId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#calculateRAMForAccount(long)
     */
    public long calculateMemoryForAccount(final long accountId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#calculateSecondaryStorageForAccount(long)
     */
    public long calculateSecondaryStorageForAccount(final long accountId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }
}
