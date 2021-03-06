import time

from nose.plugins.attrib import attr

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.cloudstackAPI import (
    replaceNetworkACLList
)
from marvin.lib.base import (
    Account,
    VirtualMachine,
    PublicIPAddress,
    LoadBalancerRule,
    VPC,
    Network
)
from marvin.lib.common import (
    get_domain,
    get_zone,
    get_template,
    list_lb_rules,
    list_lb_instances,
    get_default_virtual_machine_offering,
    get_default_network_offering,
    get_default_vpc_offering,
    get_network_acl
)
from marvin.lib.utils import cleanup_resources
from marvin.utils.MarvinLog import MarvinLog
from marvin.utils.SshClient import SshClient


class TestLoadBalance(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.logger = MarvinLog(MarvinLog.LOGGER_TEST).get_logger()
        testClient = super(TestLoadBalance, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls.service_offering = get_default_virtual_machine_offering(cls.apiclient)

        cls.network_offering = get_default_network_offering(cls.apiclient)
        cls.logger.debug("Network Offering '%s' selected", cls.network_offering.name)

        cls.vpc_offering = get_default_vpc_offering(cls.apiclient)
        cls.logger.debug("VPC Offering '%s' selected", cls.vpc_offering.name)

        cls.vpc1 = VPC.create(cls.apiclient,
                               cls.services['vpcs']['vpc1'],
                               vpcofferingid=cls.vpc_offering.id,
                               zoneid=cls.zone.id,
                               domainid=cls.domain.id,
                               account=cls.account.name)
        cls.logger.debug("VPC '%s' created, CIDR: %s", cls.vpc1.name, cls.vpc1.cidr)

        cls.default_allow_acl = get_network_acl(cls.apiclient, 'default_allow')
        cls.logger.debug("ACL '%s' selected", cls.default_allow_acl.name)

        cls.network1 = Network.create(cls.apiclient,
                                       cls.services['networks']['network1'],
                                       networkofferingid=cls.network_offering.id,
                                       aclid=cls.default_allow_acl.id,
                                       vpcid=cls.vpc1.id,
                                       zoneid=cls.zone.id,
                                       domainid=cls.domain.id,
                                       accountid=cls.account.name)
        cls.logger.debug("Network '%s' created, CIDR: %s, Gateway: %s", cls.network1.name, cls.network1.cidr, cls.network1.gateway)

        cls.vm_1 = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[cls.network1.id]
        )
        cls.vm_2 = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[cls.network1.id]
        )
        cls.vm_3 = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=[cls.network1.id]
        )

        cls.non_src_nat_ip = PublicIPAddress.create(cls.apiclient,
            zoneid=cls.zone.id,
            domainid=cls.account.domainid,
            accountid=cls.account.name,
            vpcid=cls.vpc1.id,
            networkid=cls.network1.id)
        cls.logger.debug("Public IP '%s' acquired, VPC: %s, Network: %s", cls.non_src_nat_ip.ipaddress.ipaddress, cls.vpc1.name, cls.network1.name)

        command = replaceNetworkACLList.replaceNetworkACLListCmd()
        command.aclid = cls.default_allow_acl.id
        command.publicipid = cls.non_src_nat_ip.ipaddress.id
        cls.apiclient.replaceNetworkACLList(command)

        cls._cleanup = [
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        cleanup_resources(cls.apiclient, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=['advanced'])
    def test_01_create_lb_rule_src_nat(self):
        """Test to create Load balancing rule with source NAT"""

        # Validate the Following:
        # 1. listLoadBalancerRules should return the added rule
        # 2. attempt to ssh twice on the load balanced IP
        # 3. verify using the UNAME of the VM
        #   that round robin is indeed happening as expected
        src_nat_ip_addrs = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(src_nat_ip_addrs, list),
            True,
            "Check list response returns a valid list"
        )
        src_nat_ip_addr = src_nat_ip_addrs[0]

        # Check if VM is in Running state before creating LB rule
        vm_response = VirtualMachine.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM returns a valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        for vm in vm_response:
            self.assertEqual(
                vm.state,
                'Running',
                "VM state should be Running before creating a NAT rule."
            )

        # Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            src_nat_ip_addr.id,
            accountid=self.account.name,
            vpcid=self.vpc1.id,
            networkid=self.network1.id
        )
        self.cleanup.append(lb_rule)
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        lb_rules = list_lb_rules(
            self.apiclient,
            id=lb_rule.id
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "Check list response returns a valid list"
        )
        # verify listLoadBalancerRules lists the added load balancing rule
        self.assertNotEqual(
            len(lb_rules),
            0,
            "Check Load Balancer Rule in its List"
        )
        self.assertEqual(
            lb_rules[0].id,
            lb_rule.id,
            "Check List Load Balancer Rules returns valid Rule"
        )

        # listLoadBalancerRuleInstances should list all
        # instances associated with that LB rule
        lb_instance_rules = list_lb_instances(
            self.apiclient,
            id=lb_rule.id
        )
        self.assertEqual(
            isinstance(lb_instance_rules, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(lb_instance_rules),
            0,
            "Check Load Balancer instances Rule in its List"
        )
        self.logger.debug("lb_instance_rules Ids: %s, %s" % (
            lb_instance_rules[0].id,
            lb_instance_rules[1].id
        ))
        self.logger.debug("VM ids: %s, %s" % (self.vm_1.id, self.vm_2.id))

        self.assertIn(
            lb_instance_rules[0].id,
            [self.vm_1.id, self.vm_2.id],
            "Check List Load Balancer instances Rules returns valid VM ID"
        )

        self.assertIn(
            lb_instance_rules[1].id,
            [self.vm_1.id, self.vm_2.id],
            "Check List Load Balancer instances Rules returns valid VM ID"
        )

        unameResults = []
        self.try_ssh(src_nat_ip_addr.ipaddress, unameResults)
        self.try_ssh(src_nat_ip_addr.ipaddress, unameResults)
        self.try_ssh(src_nat_ip_addr.ipaddress, unameResults)
        self.try_ssh(src_nat_ip_addr.ipaddress, unameResults)
        self.try_ssh(src_nat_ip_addr.ipaddress, unameResults)

        self.logger.debug("UNAME: %s" % str(unameResults))
        self.assertIn(
            "Linux",
            unameResults,
            "Check if ssh succeeded for server1"
        )
        self.assertIn(
            "Linux",
            unameResults,
            "Check if ssh succeeded for server2"
        )

        # SSH should pass till there is a last VM associated with LB rule
        lb_rule.remove(self.apiclient, [self.vm_2])

        # making unameResultss list empty
        unameResults[:] = []

        try:
            self.logger.debug("SSHing into IP address: %s after removing VM (ID: %s)" %
                       (
                           src_nat_ip_addr.ipaddress,
                           self.vm_2.id
                       ))

            self.try_ssh(src_nat_ip_addr.ipaddress, unameResults)
            self.assertIn(
                "Linux",
                unameResults,
                "Check if ssh succeeded for server1"
            )
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                      (e, src_nat_ip_addr.ipaddress))

        lb_rule.remove(self.apiclient, [self.vm_1])

        with self.assertRaises(Exception):
            self.logger.debug("Removed all VMs, trying to SSH")
            self.try_ssh(src_nat_ip_addr.ipaddress, unameResults)
        return

    @attr(tags=['advanced'])
    def test_02_create_lb_rule_non_nat(self):
        """Test to create Load balancing rule with non source NAT"""

        # Validate the Following:
        # 1. listLoadBalancerRules should return the added rule
        # 2. attempt to ssh twice on the load balanced IP
        # 3. verify using the UNAME of the VM that
        #   round robin is indeed happening as expected

        # Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            self.non_src_nat_ip.ipaddress.id,
            accountid=self.account.name,
            vpcid=self.vpc1.id,
            networkid=self.network1.id
        )
        self.cleanup.append(lb_rule)
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        lb_rules = list_lb_rules(
            self.apiclient,
            id=lb_rule.id
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "Check list response returns a valid list"
        )
        # verify listLoadBalancerRules lists the added load balancing rule
        self.assertNotEqual(
            len(lb_rules),
            0,
            "Check Load Balancer Rule in its List"
        )
        self.assertEqual(
            lb_rules[0].id,
            lb_rule.id,
            "Check List Load Balancer Rules returns valid Rule"
        )
        # listLoadBalancerRuleInstances should list
        # all instances associated with that LB rule
        lb_instance_rules = list_lb_instances(
            self.apiclient,
            id=lb_rule.id
        )
        self.assertEqual(
            isinstance(lb_instance_rules, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(lb_instance_rules),
            0,
            "Check Load Balancer instances Rule in its List"
        )

        self.assertIn(
            lb_instance_rules[0].id,
            [self.vm_1.id, self.vm_2.id],
            "Check List Load Balancer instances Rules returns valid VM ID"
        )

        self.assertIn(
            lb_instance_rules[1].id,
            [self.vm_1.id, self.vm_2.id],
            "Check List Load Balancer instances Rules returns valid VM ID"
        )
        try:
            unameResults = []
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)

            self.logger.debug("UNAME: %s" % str(unameResults))
            self.assertIn(
                "Linux",
                unameResults,
                "Check if ssh succeeded for server1"
            )
            self.assertIn(
                "Linux",
                unameResults,
                "Check if ssh succeeded for server2"
            )

            # SSH should pass till there is a last VM associated with LB rule
            lb_rule.remove(self.apiclient, [self.vm_2])
            self.logger.debug("SSHing into IP address: %s after removing VM (ID: %s) from LB rule" %
                       (
                           self.non_src_nat_ip.ipaddress.ipaddress,
                           self.vm_2.id
                       ))
            # Making host list empty
            unameResults[:] = []

            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
            self.assertIn(
                "Linux",
                unameResults,
                "Check if ssh succeeded for server1"
            )
            self.logger.debug("UNAME after removing VM2: %s" % str(unameResults))
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                      (e, self.non_src_nat_ip.ipaddress.ipaddress))

        lb_rule.remove(self.apiclient, [self.vm_1])
        with self.assertRaises(Exception):
            self.logger.debug("SSHing into IP address: %s after removing VM (ID: %s) from LB rule" %
                       (
                           self.non_src_nat_ip.ipaddress.ipaddress,
                           self.vm_1.id
                       ))
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        return

    @attr(tags=['advanced'])
    def test_03_assign_and_removal_lb(self):
        """Test for assign & removing load balancing rule"""

        # Validate:
        # 1. Verify list API - listLoadBalancerRules lists
        #   all the rules with the relevant ports
        # 2. listLoadBalancerInstances will list
        #   the instances associated with the corresponding rule.
        # 3. verify ssh attempts should pass as long as there
        #   is at least one instance associated with the rule

        # Check if VM is in Running state before creating LB rule
        vm_response = VirtualMachine.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM returns a valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check Port Forwarding Rule is created"
        )
        for vm in vm_response:
            self.assertEqual(
                vm.state,
                'Running',
                "VM state should be Running before creating a NAT rule."
            )

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            self.non_src_nat_ip.ipaddress.id,
            self.account.name,
            vpcid=self.vpc1.id,
            networkid=self.network1.id
        )
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])

        unameResults = []
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)

        self.logger.debug("UNAME: %s" % str(unameResults))
        self.assertIn(
            "Linux",
            unameResults,
            "Check if ssh succeeded for server1"
        )
        self.assertIn(
            "Linux",
            unameResults,
            "Check if ssh succeeded for server2"
        )
        # Removing VM and assigning another VM to LB rule
        lb_rule.remove(self.apiclient, [self.vm_2])

        # making unameResults list empty
        unameResults[:] = []

        try:
            self.logger.debug("SSHing again into IP address: %s with VM (ID: %s) added to LB rule" %
                       (
                           self.non_src_nat_ip.ipaddress.ipaddress,
                           self.vm_1.id,
                       ))
            self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)

            self.assertIn(
                "Linux",
                unameResults,
                "Check if ssh succeeded for server1"
            )
        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                      self.non_src_nat_ip.ipaddress.ipaddress)

        lb_rule.assign(self.apiclient, [self.vm_3])

        # Making unameResults list empty
        unameResults[:] = []
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.try_ssh(self.non_src_nat_ip.ipaddress.ipaddress, unameResults)
        self.logger.debug("UNAME: %s" % str(unameResults))
        self.assertIn(
            "Linux",
            unameResults,
            "Check if ssh succeeded for server1"
        )
        self.assertIn(
            "Linux",
            unameResults,
            "Check if ssh succeeded for server3"
        )
        return

    def try_ssh(self, ip_addr, unameCmd):
        try:
            self.logger.debug(
                "SSH into VM (IPaddress: %s) & NAT Rule (Public IP: %s)" %
                (self.vm_1.ipaddress, ip_addr)
            )
            # If Round Robin Algorithm is chosen,
            # each ssh command should alternate between VMs

            ssh_1 = SshClient(
                ip_addr,
                self.services['lbrule']["publicport"],
                self.vm_1.username,
                self.vm_1.password,
                retries=10
            )
            unameCmd.append(ssh_1.execute("uname")[0])
            self.logger.debug(unameCmd)
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                      (e, ip_addr))
        time.sleep(10)
        return
