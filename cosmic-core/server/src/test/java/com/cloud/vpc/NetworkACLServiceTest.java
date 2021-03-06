package com.cloud.vpc;

import com.cloud.api.command.user.network.CreateNetworkACLCmd;
import com.cloud.context.CallContext;
import com.cloud.dao.EntityManager;
import com.cloud.engine.orchestration.service.NetworkOrchestrationService;
import com.cloud.legacymodel.exceptions.InvalidParameterValueException;
import com.cloud.legacymodel.network.vpc.NetworkACLItem;
import com.cloud.legacymodel.network.vpc.Vpc;
import com.cloud.legacymodel.user.Account;
import com.cloud.legacymodel.user.User;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.NetworkACLService;
import com.cloud.network.vpc.NetworkACLServiceImpl;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.test.utils.SpringUtils;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.UUID;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class NetworkACLServiceTest extends TestCase {
    private static final Logger s_logger = LoggerFactory.getLogger(NetworkACLServiceTest.class);
    @Inject
    NetworkACLService _aclService;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    NetworkACLManager _networkAclMgr;
    @Inject
    NetworkACLDao _networkACLDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    EntityManager _entityMgr;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VpcService _vpcSrv;
    private CreateNetworkACLCmd createACLItemCmd;
    private NetworkACLVO acl;
    private NetworkACLItemVO aclItem;

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        final Account account = new AccountVO("testaccount", 1, "testdomain", (short) 0, UUID.randomUUID().toString());
        final UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);

        CallContext.register(user, account);

        createACLItemCmd = new CreateNetworkACLCmd() {
            @Override
            public Integer getNumber() {
                return 1;
            }

            @Override
            public String getProtocol() {
                return "TCP";
            }

            @Override
            public Long getACLId() {
                return 3L;
            }
        };

        acl = new NetworkACLVO() {
            @Override
            public Long getVpcId() {
                return 1L;
            }

            @Override
            public long getId() {
                return 1L;
            }
        };

        aclItem = new NetworkACLItemVO() {
            @Override
            public long getAclId() {
                return 4L;
            }
        };
    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateACL() throws Exception {
        Mockito.when(_entityMgr.findById(Matchers.eq(Vpc.class), Matchers.anyLong())).thenReturn(new VpcVO());
        Mockito.when(_networkAclMgr.createNetworkACL("acl_new", "acl desc", 1L, true)).thenReturn(acl);
        assertNotNull(_aclService.createNetworkACL("acl_new", "acl desc", 1L, true));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteDefaultACL() throws Exception {
        Mockito.when(_networkACLDao.findById(Matchers.anyLong())).thenReturn(acl);
        Mockito.when(_networkAclMgr.deleteNetworkACL(acl)).thenReturn(true);
        _aclService.deleteNetworkACL(1L);
    }

    @Test
    public void testCreateACLItem() throws Exception {
        Mockito.when(_entityMgr.findById(Matchers.eq(Vpc.class), Matchers.anyLong())).thenReturn(new VpcVO());
        Mockito.when(_networkAclMgr.getNetworkACL(Matchers.anyLong())).thenReturn(acl);
        Mockito.when(
                _networkAclMgr.createNetworkACLItem(Matchers.anyInt(), Matchers.anyInt(), Matchers.anyString(), Matchers.anyList(), Matchers.anyInt(), Matchers.anyInt(),
                        Matchers.any(NetworkACLItem.TrafficType.class), Matchers.anyLong(), Matchers.anyString(), Matchers.anyInt(), Matchers.anyBoolean())).thenReturn(
                new NetworkACLItemVO());
        Mockito.when(_networkACLItemDao.findByAclAndNumber(Matchers.anyLong(), Matchers.anyInt())).thenReturn(null);
        assertNotNull(_aclService.createNetworkACLItem(createACLItemCmd));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateACLItemDuplicateNumber() throws Exception {
        Mockito.when(_entityMgr.findById(Matchers.eq(Vpc.class), Matchers.anyLong())).thenReturn(new VpcVO());
        Mockito.when(_networkAclMgr.getNetworkACL(Matchers.anyLong())).thenReturn(acl);
        Mockito.when(_networkACLItemDao.findByAclAndNumber(Matchers.anyLong(), Matchers.anyInt())).thenReturn(new NetworkACLItemVO());
        _aclService.createNetworkACLItem(createACLItemCmd);
    }

    @Test
    public void testDeleteACLItem() throws Exception {
        Mockito.when(_networkACLItemDao.findById(Matchers.anyLong())).thenReturn(aclItem);
        Mockito.when(_networkAclMgr.getNetworkACL(Matchers.anyLong())).thenReturn(acl);
        Mockito.when(_networkAclMgr.revokeNetworkACLItem(Matchers.anyLong())).thenReturn(true);
        Mockito.when(_entityMgr.findById(Mockito.eq(Vpc.class), Mockito.anyLong())).thenReturn(new VpcVO());
        assertTrue(_aclService.revokeNetworkACLItem(1L));
    }

    @Configuration
    @ComponentScan(basePackageClasses = {NetworkACLServiceImpl.class}, includeFilters = {@ComponentScan.Filter(value = NetworkACLTestConfiguration.Library.class,
            type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class NetworkACLTestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public EntityManager entityManager() {
            return Mockito.mock(EntityManager.class);
        }

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public IPAddressDao ipAddressDao() {
            return Mockito.mock(IPAddressDao.class);
        }

        @Bean
        public NetworkOrchestrationService networkManager() {
            return Mockito.mock(NetworkOrchestrationService.class);
        }

        @Bean
        public NetworkModel networkModel() {
            return Mockito.mock(NetworkModel.class);
        }

        @Bean
        public VpcManager vpcManager() {
            return Mockito.mock(VpcManager.class);
        }

        @Bean
        public ResourceTagDao resourceTagDao() {
            return Mockito.mock(ResourceTagDao.class);
        }

        @Bean
        public NetworkACLDao networkACLDao() {
            return Mockito.mock(NetworkACLDao.class);
        }

        @Bean
        public NetworkACLItemDao networkACLItemDao() {
            return Mockito.mock(NetworkACLItemDao.class);
        }

        @Bean
        public NetworkDao networkDao() {
            return Mockito.mock(NetworkDao.class);
        }

        @Bean
        public NetworkACLManager networkACLManager() {
            return Mockito.mock(NetworkACLManager.class);
        }

        @Bean
        public VpcGatewayDao vpcGatewayDao() {
            return Mockito.mock(VpcGatewayDao.class);
        }

        @Bean
        public VpcDao vpcDao() {
            return Mockito.mock(VpcDao.class);
        }

        @Bean
        public VpcService vpcService() {
            return Mockito.mock(VpcService.class);
        }

        public static class Library implements TypeFilter {
            @Override
            public boolean match(final MetadataReader mdr, final MetadataReaderFactory arg1) throws IOException {
                mdr.getClassMetadata().getClassName();
                final ComponentScan cs = NetworkACLTestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }
}
