/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.protolayout.expression;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a virtually unique fingerprint for a proto message.
 *
 * <p>Note that this actually represents the way a message was built and not necessarily its
 * contents. In other words, 2 messages with the same contents may have different fingerprints if
 * their setters were called in a different order.
 *
 * <p>A value of -1 for {@code selfPropsValue} means the self part should be considered different
 * when compared with other instances of this class. A value of -1 for {@code childNodesValue} means
 * the children part should be considered different when compared with other instances of this
 * class.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class Fingerprint {
    private static final int DEFAULT_VALUE = 0;
    private final int selfTypeValue;
    private int selfPropsValue;
    private int childNodesValue;
    private @Nullable List<Fingerprint> childNodes;

    public Fingerprint(int selfTypeValue) {
        this.selfTypeValue = selfTypeValue;
        this.selfPropsValue = DEFAULT_VALUE;
        this.childNodesValue = DEFAULT_VALUE;
        this.childNodes = null;
    }

    public Fingerprint(@NonNull NodeFingerprint proto) {
        this.selfTypeValue = proto.getSelfTypeValue();
        this.selfPropsValue = proto.getSelfPropsValue();
        this.childNodesValue = proto.getChildNodesValue();
        for (NodeFingerprint childNode : proto.getChildNodesList()) {
            addChildNode(new Fingerprint(childNode));
        }
    }

    /**
     * Get the aggregate numeric fingerprint, representing the message itself as well as all its
     * child nodes.
     */
    public int aggregateValueAsInt() {
        int aggregateValue = selfTypeValue;
        aggregateValue = (31 * aggregateValue) + selfPropsValue;
        aggregateValue = (31 * aggregateValue) + childNodesValue;
        return aggregateValue;
    }

    /** Get the numeric fingerprint for the message's type. */
    public int selfTypeValue() {
        return selfTypeValue;
    }

    /**
     * Get the numeric fingerprint for the message's properties only, excluding its type and child
     * nodes.
     */
    public int selfPropsValue() {
        return selfPropsValue;
    }

    /** Get the numeric fingerprint for the child nodes. */
    public int childNodesValue() {
        return childNodesValue;
    }

    /** Get the child nodes. Returns empty list if the node has no children. */
    public @NonNull List<Fingerprint> childNodes() {
        return childNodes == null ? Collections.emptyList() : childNodes;
    }

    /** Add a child node to this fingerprint. */
    public void addChildNode(@NonNull Fingerprint childNode) {
        if (childNodes == null) {
            childNodes = new ArrayList<>();
        }
        childNodes.add(childNode);
        childNodesValue = (31 * childNodesValue) + childNode.aggregateValueAsInt();
    }

    /** Record a property value being updated. */
    public void recordPropertyUpdate(int fieldNumber, int valueHash) {
        recordEntry(fieldNumber);
        recordEntry(valueHash);
    }

    private void recordEntry(int entry) {
        selfPropsValue = (31 * selfPropsValue) + entry;
    }

    NodeFingerprint toProto() {
        NodeFingerprint.Builder builder = NodeFingerprint.newBuilder();
        if (selfTypeValue() != 0) {
            builder.setSelfTypeValue(selfTypeValue());
        }
        if (selfPropsValue() != 0) {
            builder.setSelfPropsValue(selfPropsValue());
        }
        if (childNodesValue() != 0) {
            builder.setChildNodesValue(childNodesValue());
        }
        for (Fingerprint childNode : childNodes()) {
            builder.addChildNodes(childNode.toProto());
        }
        return builder.build();
    }
}
