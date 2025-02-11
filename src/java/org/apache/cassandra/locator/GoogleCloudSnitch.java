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

import java.io.IOException;

/**
 * A snitch that assumes an GCE region is a DC and an GCE availability_zone
 * is a rack. This information is available in the config for the node.
 * @deprecated See CASSANDRA-19488
 */
@Deprecated(since = "CEP-21")
public class GoogleCloudSnitch extends AbstractCloudMetadataServiceSnitch
{
    public GoogleCloudSnitch() throws IOException
    {
        this(new SnitchProperties());
    }

    public GoogleCloudSnitch(SnitchProperties properties) throws IOException
    {
        super(new GoogleCloudLocationProvider(properties));
    }

    public GoogleCloudSnitch(AbstractCloudMetadataServiceConnector connector) throws IOException
    {
        super(new GoogleCloudLocationProvider(connector));
    }
}
