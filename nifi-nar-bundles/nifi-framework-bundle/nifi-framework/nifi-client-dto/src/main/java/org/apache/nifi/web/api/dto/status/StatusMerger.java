/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nifi.web.api.dto.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.controller.status.RunStatus;
import org.apache.nifi.controller.status.TransmissionStatus;
import org.apache.nifi.util.FormatUtils;
import org.apache.nifi.web.api.dto.BulletinDTO;

public class StatusMerger {
    public static void merge(final ControllerStatusDTO target, final ControllerStatusDTO toMerge) {
        if (target == null || toMerge == null) {
            return;
        }

        target.setActiveRemotePortCount(target.getActiveRemotePortCount() + toMerge.getActiveRemotePortCount());
        target.setActiveThreadCount(target.getActiveThreadCount() + toMerge.getActiveThreadCount());
        target.setBytesQueued(target.getBytesQueued() + toMerge.getBytesQueued());
        target.setDisabledCount(target.getDisabledCount() + toMerge.getDisabledCount());
        target.setFlowFilesQueued(target.getFlowFilesQueued() + toMerge.getFlowFilesQueued());
        target.setInactiveRemotePortCount(target.getInactiveRemotePortCount() + toMerge.getInactiveRemotePortCount());
        target.setInvalidCount(target.getInvalidCount() + toMerge.getInvalidCount());
        target.setRunningCount(target.getRunningCount() + toMerge.getRunningCount());
        target.setStoppedCount(target.getStoppedCount() + toMerge.getStoppedCount());

        target.setBulletins(mergeBulletins(target.getBulletins(), toMerge.getBulletins()));
        target.setControllerServiceBulletins(mergeBulletins(target.getControllerServiceBulletins(), toMerge.getControllerServiceBulletins()));
        target.setReportingTaskBulletins(mergeBulletins(target.getReportingTaskBulletins(), toMerge.getReportingTaskBulletins()));

        updatePrettyPrintedFields(target);
    }

    public static void updatePrettyPrintedFields(final ControllerStatusDTO target) {
        target.setQueued(prettyPrint(target.getFlowFilesQueued(), target.getBytesQueued()));
        target.setConnectedNodes(formatCount(target.getConnectedNodeCount()) + " / " + formatCount(target.getTotalNodeCount()));
    }

    public static List<BulletinDTO> mergeBulletins(final List<BulletinDTO> targetBulletins, final List<BulletinDTO> toMerge) {
        final List<BulletinDTO> bulletins = new ArrayList<>();
        if (targetBulletins != null) {
            bulletins.addAll(targetBulletins);
        }

        if (toMerge != null) {
            bulletins.addAll(toMerge);
        }

        return bulletins;
    }


    public static void merge(final ProcessGroupStatusDTO target, final ProcessGroupStatusDTO toMerge, final String nodeId, final String nodeAddress, final Integer nodeApiPort) {
        merge(target.getAggregateStatus(), toMerge.getAggregateStatus());

        if (target.getNodeStatuses() != null) {
            final NodeProcessGroupStatusSnapshotDTO nodeSnapshot = new NodeProcessGroupStatusSnapshotDTO();
            nodeSnapshot.setStatusSnapshot(toMerge.getAggregateStatus());
            nodeSnapshot.setAddress(nodeAddress);
            nodeSnapshot.setApiPort(nodeApiPort);
            nodeSnapshot.setNodeId(nodeId);

            target.getNodeStatuses().add(nodeSnapshot);
        }
    }

    public static void merge(final ProcessGroupStatusSnapshotDTO target, final ProcessGroupStatusSnapshotDTO toMerge) {
        if (target == null || toMerge == null) {
            return;
        }

        target.setBytesIn(target.getBytesIn() + toMerge.getBytesIn());
        target.setFlowFilesIn(target.getFlowFilesIn() + toMerge.getFlowFilesIn());

        target.setBytesQueued(target.getBytesQueued() + toMerge.getBytesQueued());
        target.setFlowFilesQueued(target.getFlowFilesQueued() + toMerge.getFlowFilesQueued());

        target.setBytesRead(target.getBytesRead() + toMerge.getBytesRead());
        target.setBytesWritten(target.getBytesWritten() + toMerge.getBytesWritten());

        target.setBytesOut(target.getBytesOut() + toMerge.getBytesOut());
        target.setFlowFilesOut(target.getFlowFilesOut() + toMerge.getFlowFilesOut());

        target.setBytesTransferred(target.getBytesTransferred() + toMerge.getBytesTransferred());
        target.setFlowFilesTransferred(target.getFlowFilesTransferred() + toMerge.getFlowFilesTransferred());

        target.setBytesReceived(target.getBytesReceived() + toMerge.getBytesReceived());
        target.setFlowFilesReceived(target.getFlowFilesReceived() + toMerge.getFlowFilesReceived());

        target.setBytesSent(target.getBytesSent() + toMerge.getBytesSent());
        target.setFlowFilesSent(target.getFlowFilesSent() + toMerge.getFlowFilesSent());

        target.setActiveThreadCount(target.getActiveThreadCount() + toMerge.getActiveThreadCount());
        updatePrettyPrintedFields(target);

        // connection status
        // sort by id
        final Map<String, ConnectionStatusSnapshotDTO> mergedConnectionMap = new HashMap<>();
        for (final ConnectionStatusSnapshotDTO status : replaceNull(target.getConnectionStatusSnapshots())) {
            mergedConnectionMap.put(status.getId(), status);
        }

        for (final ConnectionStatusSnapshotDTO statusToMerge : replaceNull(toMerge.getConnectionStatusSnapshots())) {
            ConnectionStatusSnapshotDTO merged = mergedConnectionMap.get(statusToMerge.getId());
            if (merged == null) {
                mergedConnectionMap.put(statusToMerge.getId(), statusToMerge.clone());
                continue;
            }

            merge(merged, statusToMerge);
        }
        target.setConnectionStatusSnapshots(mergedConnectionMap.values());

        // processor status
        final Map<String, ProcessorStatusSnapshotDTO> mergedProcessorMap = new HashMap<>();
        for (final ProcessorStatusSnapshotDTO status : replaceNull(target.getProcessorStatusSnapshots())) {
            mergedProcessorMap.put(status.getId(), status);
        }

        for (final ProcessorStatusSnapshotDTO statusToMerge : replaceNull(toMerge.getProcessorStatusSnapshots())) {
            ProcessorStatusSnapshotDTO merged = mergedProcessorMap.get(statusToMerge.getId());
            if (merged == null) {
                mergedProcessorMap.put(statusToMerge.getId(), statusToMerge.clone());
                continue;
            }

            merge(merged, statusToMerge);
        }
        target.setProcessorStatusSnapshots(mergedProcessorMap.values());


        // input ports
        final Map<String, PortStatusSnapshotDTO> mergedInputPortMap = new HashMap<>();
        for (final PortStatusSnapshotDTO status : replaceNull(target.getInputPortStatusSnapshots())) {
            mergedInputPortMap.put(status.getId(), status);
        }

        for (final PortStatusSnapshotDTO statusToMerge : replaceNull(toMerge.getInputPortStatusSnapshots())) {
            PortStatusSnapshotDTO merged = mergedInputPortMap.get(statusToMerge.getId());
            if (merged == null) {
                mergedInputPortMap.put(statusToMerge.getId(), statusToMerge.clone());
                continue;
            }

            merge(merged, statusToMerge);
        }
        target.setInputPortStatusSnapshots(mergedInputPortMap.values());

        // output ports
        final Map<String, PortStatusSnapshotDTO> mergedOutputPortMap = new HashMap<>();
        for (final PortStatusSnapshotDTO status : replaceNull(target.getOutputPortStatusSnapshots())) {
            mergedOutputPortMap.put(status.getId(), status);
        }

        for (final PortStatusSnapshotDTO statusToMerge : replaceNull(toMerge.getOutputPortStatusSnapshots())) {
            PortStatusSnapshotDTO merged = mergedOutputPortMap.get(statusToMerge.getId());
            if (merged == null) {
                mergedOutputPortMap.put(statusToMerge.getId(), statusToMerge.clone());
                continue;
            }

            merge(merged, statusToMerge);
        }
        target.setOutputPortStatusSnapshots(mergedOutputPortMap.values());

        // child groups
        final Map<String, ProcessGroupStatusSnapshotDTO> mergedGroupMap = new HashMap<>();
        for (final ProcessGroupStatusSnapshotDTO status : replaceNull(target.getProcessGroupStatusSnapshots())) {
            mergedGroupMap.put(status.getId(), status);
        }

        for (final ProcessGroupStatusSnapshotDTO statusToMerge : replaceNull(toMerge.getProcessGroupStatusSnapshots())) {
            ProcessGroupStatusSnapshotDTO merged = mergedGroupMap.get(statusToMerge.getId());
            if (merged == null) {
                mergedGroupMap.put(statusToMerge.getId(), statusToMerge.clone());
                continue;
            }

            merge(merged, statusToMerge);
        }
        target.setOutputPortStatusSnapshots(mergedOutputPortMap.values());

        // remote groups
        final Map<String, RemoteProcessGroupStatusSnapshotDTO> mergedRemoteGroupMap = new HashMap<>();
        for (final RemoteProcessGroupStatusSnapshotDTO status : replaceNull(target.getRemoteProcessGroupStatusSnapshots())) {
            mergedRemoteGroupMap.put(status.getId(), status);
        }

        for (final RemoteProcessGroupStatusSnapshotDTO statusToMerge : replaceNull(toMerge.getRemoteProcessGroupStatusSnapshots())) {
            RemoteProcessGroupStatusSnapshotDTO merged = mergedRemoteGroupMap.get(statusToMerge.getId());
            if (merged == null) {
                mergedRemoteGroupMap.put(statusToMerge.getId(), statusToMerge.clone());
                continue;
            }

            merge(merged, statusToMerge);
        }
        target.setRemoteProcessGroupStatusSnapshots(mergedRemoteGroupMap.values());
    }

    private static <T> Collection<T> replaceNull(final Collection<T> collection) {
        return (collection == null) ? Collections.<T> emptyList() : collection;
    }


    /**
     * Updates the fields that are "pretty printed" based on the raw values currently set. For example,
     * {@link ProcessGroupStatusDTO#setInput(String)} will be called with the pretty-printed form of the
     * FlowFile counts and sizes retrieved via {@link ProcessGroupStatusDTO#getFlowFilesIn()} and
     * {@link ProcessGroupStatusDTO#getBytesIn()}.
     *
     * This logic is performed here, rather than in the DTO itself because the DTO needs to be kept purely
     * getters & setters - otherwise the automatic marshalling and unmarshalling to/from JSON becomes very
     * complicated.
     *
     * @param target the DTO to update
     */
    public static void updatePrettyPrintedFields(final ProcessGroupStatusSnapshotDTO target) {
        target.setQueued(prettyPrint(target.getFlowFilesQueued(), target.getBytesQueued()));
        target.setQueuedCount(formatCount(target.getFlowFilesQueued()));
        target.setQueuedSize(formatDataSize(target.getBytesQueued()));
        target.setInput(prettyPrint(target.getFlowFilesIn(), target.getBytesIn()));
        target.setRead(formatDataSize(target.getBytesRead()));
        target.setWritten(formatDataSize(target.getBytesWritten()));
        target.setOutput(prettyPrint(target.getFlowFilesOut(), target.getBytesOut()));
        target.setTransferred(prettyPrint(target.getFlowFilesTransferred(), target.getBytesTransferred()));
        target.setReceived(prettyPrint(target.getFlowFilesReceived(), target.getBytesReceived()));
        target.setSent(prettyPrint(target.getFlowFilesSent(), target.getBytesSent()));
    }


    public static void merge(final ProcessorStatusDTO target, final ProcessorStatusDTO toMerge, final String nodeId, final String nodeAddress, final Integer nodeApiPort) {
        merge(target.getAggregateStatus(), toMerge.getAggregateStatus());

        if (target.getNodeStatuses() != null) {
            final NodeProcessorStatusSnapshotDTO nodeSnapshot = new NodeProcessorStatusSnapshotDTO();
            nodeSnapshot.setStatusSnapshot(toMerge.getAggregateStatus());
            nodeSnapshot.setAddress(nodeAddress);
            nodeSnapshot.setApiPort(nodeApiPort);
            nodeSnapshot.setNodeId(nodeId);

            target.getNodeStatuses().add(nodeSnapshot);
        }
    }

    public static void merge(final ProcessorStatusSnapshotDTO target, final ProcessorStatusSnapshotDTO toMerge) {
        if (target == null || toMerge == null) {
            return;
        }

        // if the status to merge is invalid allow it to take precedence. whether the
        // processor run status is disabled/stopped/running is part of the flow configuration
        // and should not differ amongst nodes. however, whether a processor is invalid
        // can be driven by environmental conditions. this check allows any of those to
        // take precedence over the configured run status.
        if (RunStatus.Invalid.name().equals(toMerge.getRunStatus())) {
            target.setRunStatus(RunStatus.Invalid.name());
        }

        target.setBytesRead(target.getBytesRead() + toMerge.getBytesRead());
        target.setBytesWritten(target.getBytesWritten() + toMerge.getBytesWritten());
        target.setFlowFilesIn(target.getFlowFilesIn() + toMerge.getFlowFilesIn());
        target.setBytesIn(target.getBytesIn() + toMerge.getBytesIn());
        target.setFlowFilesOut(target.getFlowFilesOut() + toMerge.getFlowFilesOut());
        target.setBytesOut(target.getBytesOut() + toMerge.getBytesOut());
        target.setTaskCount(target.getTaskCount() + toMerge.getTaskCount());
        target.setTaskDuration(target.getTaskDuration() + toMerge.getTaskDuration());
        target.setActiveThreadCount(target.getActiveThreadCount() + toMerge.getActiveThreadCount());
        updatePrettyPrintedFields(target);
    }

    public static void updatePrettyPrintedFields(final ProcessorStatusSnapshotDTO target) {
        target.setInput(prettyPrint(target.getFlowFilesIn(), target.getBytesIn()));
        target.setRead(formatDataSize(target.getBytesRead()));
        target.setWritten(formatDataSize(target.getBytesWritten()));
        target.setOutput(prettyPrint(target.getFlowFilesOut(), target.getBytesOut()));

        final Integer taskCount = target.getTaskCount();
        final String tasks = (taskCount == null) ? "-" : formatCount(taskCount);
        target.setTasks(tasks);

        target.setTasksDuration(FormatUtils.formatHoursMinutesSeconds(target.getTaskDuration(), TimeUnit.NANOSECONDS));
    }


    public static void merge(final ConnectionStatusSnapshotDTO target, final ConnectionStatusSnapshotDTO toMerge) {
        if (target == null || toMerge == null) {
            return;
        }

        target.setFlowFilesIn(target.getFlowFilesIn() + toMerge.getFlowFilesIn());
        target.setBytesIn(target.getBytesIn() + toMerge.getBytesIn());
        target.setFlowFilesOut(target.getFlowFilesOut() + toMerge.getFlowFilesOut());
        target.setBytesOut(target.getBytesOut() + toMerge.getBytesOut());
        target.setFlowFilesQueued(target.getFlowFilesQueued() + toMerge.getFlowFilesQueued());
        target.setBytesQueued(target.getBytesQueued() + toMerge.getBytesQueued());
        updatePrettyPrintedFields(target);
    }

    public static void updatePrettyPrintedFields(final ConnectionStatusSnapshotDTO target) {
        target.setQueued(prettyPrint(target.getFlowFilesQueued(), target.getBytesQueued()));
        target.setQueuedCount(formatCount(target.getFlowFilesQueued()));
        target.setQueuedSize(formatDataSize(target.getBytesQueued()));
        target.setInput(prettyPrint(target.getFlowFilesIn(), target.getBytesIn()));
        target.setOutput(prettyPrint(target.getFlowFilesOut(), target.getBytesOut()));
    }



    public static void merge(final RemoteProcessGroupStatusSnapshotDTO target, final RemoteProcessGroupStatusSnapshotDTO toMerge) {
        final String transmittingValue = TransmissionStatus.Transmitting.name();
        if (transmittingValue.equals(target.getTransmissionStatus()) || transmittingValue.equals(toMerge.getTransmissionStatus())) {
            target.setTransmissionStatus(transmittingValue);
        }

        target.setActiveThreadCount(target.getActiveThreadCount() + toMerge.getActiveThreadCount());

        final List<String> authIssues = new ArrayList<>();
        if (target.getAuthorizationIssues() != null) {
            authIssues.addAll(target.getAuthorizationIssues());
        }
        if (toMerge.getAuthorizationIssues() != null) {
            authIssues.addAll(toMerge.getAuthorizationIssues());
        }
        target.setAuthorizationIssues(authIssues);

        target.setFlowFilesSent(target.getFlowFilesSent() + toMerge.getFlowFilesSent());
        target.setBytesSent(target.getBytesSent() + toMerge.getBytesSent());
        target.setFlowFilesReceived(target.getFlowFilesReceived() + toMerge.getFlowFilesReceived());
        target.setBytesReceived(target.getBytesReceived() + toMerge.getBytesReceived());
        updatePrettyPrintedFields(target);
    }

    public static void updatePrettyPrintedFields(final RemoteProcessGroupStatusSnapshotDTO target) {
        target.setReceived(prettyPrint(target.getFlowFilesReceived(), target.getBytesReceived()));
        target.setSent(prettyPrint(target.getFlowFilesSent(), target.getBytesSent()));
    }



    public static void merge(final PortStatusSnapshotDTO target, final PortStatusSnapshotDTO toMerge) {
        if (target == null || toMerge == null) {
            return;
        }

        target.setActiveThreadCount(target.getActiveThreadCount() + toMerge.getActiveThreadCount());
        target.setFlowFilesIn(target.getFlowFilesIn() + toMerge.getFlowFilesIn());
        target.setBytesIn(target.getBytesIn() + toMerge.getBytesIn());
        target.setFlowFilesOut(target.getFlowFilesOut() + toMerge.getFlowFilesOut());
        target.setBytesOut(target.getBytesOut() + toMerge.getBytesOut());
        target.setTransmitting(Boolean.TRUE.equals(target.isTransmitting()) || Boolean.TRUE.equals(toMerge.isTransmitting()));

        // should be unnecessary here since ports run status not should be affected by
        // environmental conditions but doing so in case that changes
        if (RunStatus.Invalid.name().equals(toMerge.getRunStatus())) {
            target.setRunStatus(RunStatus.Invalid.name());
        }

        updatePrettyPrintedFields(target);
    }

    public static void updatePrettyPrintedFields(final PortStatusSnapshotDTO target) {
        target.setInput(prettyPrint(target.getFlowFilesIn(), target.getBytesIn()));
        target.setOutput(prettyPrint(target.getFlowFilesOut(), target.getBytesOut()));
    }


    public static String formatCount(final Integer intStatus) {
        return intStatus == null ? "-" : FormatUtils.formatCount(intStatus);
    }

    public static String formatDataSize(final Long longStatus) {
        return longStatus == null ? "-" : FormatUtils.formatDataSize(longStatus);
    }

    public static String prettyPrint(final Integer count, final Long bytes) {
        return formatCount(count) + " / " + formatDataSize(bytes);
    }

}