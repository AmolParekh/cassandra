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
package org.apache.cassandra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.SnapshotCommand;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.snapshot.SnapshotManager;
import org.apache.cassandra.service.snapshot.SnapshotOptions;
import org.apache.cassandra.service.snapshot.SnapshotType;
import org.apache.cassandra.utils.DiagnosticSnapshotService;


public class SnapshotVerbHandler implements IVerbHandler<SnapshotCommand>
{
    public static final SnapshotVerbHandler instance = new SnapshotVerbHandler();
    private static final Logger logger = LoggerFactory.getLogger(SnapshotVerbHandler.class);

    public void doVerb(Message<SnapshotCommand> message)
    {
        SnapshotCommand command = message.payload;
        if (command.clear_snapshot)
        {
            SnapshotManager.instance.clearSnapshots(command.snapshot_name, command.keyspace);
        }
        else if (DiagnosticSnapshotService.isDiagnosticSnapshotRequest(command))
        {
            DiagnosticSnapshotService.snapshot(command, message.from());
        }
        else
        {
            try
            {
                SnapshotOptions options = SnapshotOptions.systemSnapshot(command.snapshot_name, SnapshotType.DIAGNOSTICS, command.keyspace + '.' + command.column_family).build();
                SnapshotManager.instance.takeSnapshot(options);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }

        logger.debug("Enqueuing response to snapshot request {} to {}", command.snapshot_name, message.from());
        MessagingService.instance().send(message.emptyResponse(), message.from());
    }
}
