"""
@Desc     : This module defines all codes, constants maintained globally \
            and used across marvin and its test features.The main purpose \
            is to maintain readability, maintain one common place for \
            all codes used or reused across test features. It enhances \
            maintainability and readability. Users just import statement \
            to receive all the codes mentioned here. EX: Here, we define \
            a code viz., ENABLED  with value "Enabled",then using \
            this code in a sample feature say test_a.py as below. \

            from codes import *
            if obj.getvalue() == ENABLED

@DateAdded: 20th October 2013
"""

'''
VM STATES - START
'''
RUNNING = "Running"
STOPPED = "Stopped"
STOPPING = "Stopping"
STARTING = "Starting"
DESTROYED = "Destroyed"
EXPUNGING = "Expunging"
'''
VM STATES - END
'''

'''
Snapshot States - START
'''
BACKED_UP = "backedup"
BACKING_UP = "backingup"
'''
Snapshot States - END
'''

RECURRING = "RECURRING"
ENABLED = "Enabled"
DISABLED = "Disabled"
ENABLE = "Enable"
DISABLE = "Disable"
NETWORK_OFFERING = "network_offering"
ROOT = "ROOT"
INVALID_INPUT = "INVALID INPUT"
EMPTY_LIST = "EMPTY_LIST"
FAIL = 0
PASS = 1
MATCH_NOT_FOUND = "ELEMENT NOT FOUND IN THE INPUT"
SUCCESS = "SUCCESS"
EXCEPTION_OCCURRED = "Exception Occurred"
NO = "no"
YES = "yes"
FAILED = "FAILED"
UNKNOWN_ERROR = "Unknown Error"
EXCEPTION = "EXCEPTION"
INVALID_RESPONSE = "Invalid Response"
'''
Async Job Related Codes
'''
JOB_INPROGRESS = 0
JOB_SUCCEEDED = 1
JOB_FAILED = 2
JOB_CANCELLED = 3
'''
User Related Codes
'''
BASIC_ZONE = "basic"
ISOLATED_NETWORK = "ISOLATED"
SHARED_NETWORK = "SHARED"
VPC_NETWORK = "VPC"
ERROR_NO_HOST_FOR_MIGRATION = \
    "Could not find suitable host for migration, " \
    "please ensure setup has required no. of hosts"
NAT_RULE = "nat rule"
STATIC_NAT_RULE = "static nat rule"
LB_RULE = "Load Balancer Rule"
UNKNOWN = "UNKNOWN"
FAULT = "FAULT"
MASTER = "MASTER"
ADMIN = 1
DOMAIN_ADMIN = 2
USER = 0
XEN_SERVER = "XenServer"
ADMIN_ACCOUNT = 'ADMIN_ACCOUNT'
USER_ACCOUNT = 'USER_ACCOUNT'
RESOURCE_CPU = 8
RESOURCE_MEMORY = 9
RESOURCE_PRIMARY_STORAGE = 10
RESOURCE_SECONDARY_STORAGE = 11
KVM = "kvm"
ROOT_DOMAIN_ADMIN = "root domain admin"
CHILD_DOMAIN_ADMIN = "child domain admin"

'''
Network states
'''
ALLOCATED = "Allocated"

'''
Storage Tags
'''
ZONETAG1 = "zwps1"
ZONETAG2 = "zwps2"
CLUSTERTAG1 = "cwps1"
CLUSTERTAG2 = "cwps2"

'''
Traffic Types
'''
PUBLIC_TRAFFIC = "public"
GUEST_TRAFFIC = "guest"
MANAGEMENT_TRAFFIC = "management"
STORAGE_TRAFFIC = "storage"


'''
Storage Pools State
'''

UP = "up"

'''
Storage Pools Scope
'''

CLUSTER = "cluster"
DATA = "DATA"
