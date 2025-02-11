/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.locator;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.distributed.test.log.ClusterMetadataTestHelper;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.tcm.ClusterMetadataService;
import org.apache.cassandra.tcm.Epoch;
import org.apache.cassandra.tcm.StubClusterMetadataService;

import static org.apache.cassandra.locator.Replica.fullReplica;
import static org.apache.cassandra.locator.ReplicaUtils.EP1;
import static org.apache.cassandra.locator.ReplicaUtils.EP2;
import static org.apache.cassandra.locator.ReplicaUtils.EP3;
import static org.apache.cassandra.locator.ReplicaUtils.EP4;
import static org.apache.cassandra.locator.ReplicaUtils.EP5;
import static org.apache.cassandra.locator.ReplicaUtils.EP6;
import static org.apache.cassandra.locator.ReplicaUtils.R1;
import static org.apache.cassandra.locator.ReplicaUtils.assertEquals;
import static org.apache.cassandra.locator.ReplicaUtils.tk;
import static org.apache.cassandra.locator.ReplicaUtils.trans;

public class ReplicaPlansTest
{
    static Keyspace ks;
    static
    {
        DatabaseDescriptor.daemonInitialization();
    }

    private static Keyspace ks(Set<InetAddressAndPort> dc1, Map<String, String> replication)
    {
        replication = ImmutableMap.<String, String>builder().putAll(replication).put("class", "NetworkTopologyStrategy").build();
        Keyspace keyspace = Keyspace.mockKS(KeyspaceMetadata.create("blah", KeyspaceParams.create(false, replication)));
        return keyspace;
    }

    @Before
    public void setup()
    {
        ClusterMetadataService.unsetInstance();
        ClusterMetadataService.setInstance(StubClusterMetadataService.forTesting());
        ClusterMetadataTestHelper.register(EP1, "DC1", "R1");
        ClusterMetadataTestHelper.register(EP2, "DC1", "R1");
        ClusterMetadataTestHelper.register(EP3, "DC1", "R1");
        ClusterMetadataTestHelper.register(EP4, "DC2", "R2");
        ClusterMetadataTestHelper.register(EP5, "DC2", "R2");
        ClusterMetadataTestHelper.register(EP6, "DC2", "R2");
    }

    private static Replica full(InetAddressAndPort ep) { return fullReplica(ep, R1); }

    @Test
    public void testWriteEachQuorum()
    {
        final Token token = tk(1L);
        {
            // all full natural
            Keyspace ks = ks(ImmutableSet.of(EP1, EP2, EP3), ImmutableMap.of("DC1", "3", "DC2", "3"));
            EndpointsForToken natural = EndpointsForToken.of(token, full(EP1), full(EP2), full(EP3), full(EP4), full(EP5), full(EP6));
            EndpointsForToken pending = EndpointsForToken.empty(token);
            ReplicaPlan.ForWrite plan = ReplicaPlans.forWrite(ks, ConsistencyLevel.EACH_QUORUM, (cm) -> natural, (cm) -> pending, null, Predicates.alwaysTrue(), ReplicaPlans.writeNormal);
            assertEquals(natural, plan.liveAndDown);
            assertEquals(natural, plan.live);
            assertEquals(natural, plan.contacts());
        }
        {
            // all natural and up, one transient in each DC
            // Note: this is confusing because it looks misconfigured as the Keyspace has never been setup with any
            // transient replicas in its replication params.
            Keyspace ks = ks(ImmutableSet.of(EP1, EP2, EP3), ImmutableMap.of("DC1", "3", "DC2", "3"));
            EndpointsForToken natural = EndpointsForToken.of(token, full(EP1), full(EP2), trans(EP3), full(EP4), full(EP5), trans(EP6));
            EndpointsForToken pending = EndpointsForToken.empty(token);
            ReplicaPlan.ForWrite plan = ReplicaPlans.forWrite(ks, ConsistencyLevel.EACH_QUORUM, (cm) -> natural, (cm) -> pending, Epoch.FIRST, Predicates.alwaysTrue(), ReplicaPlans.writeNormal);
            assertEquals(natural, plan.liveAndDown);
            assertEquals(natural, plan.live);
            EndpointsForToken expectContacts = EndpointsForToken.of(token, full(EP1), full(EP2), full(EP4), full(EP5));
            assertEquals(expectContacts, plan.contacts());
        }
    }
}
