package com.zetcom.mpria.pm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A configured screen: the structural half of a {@link PresentationModel}.
 *
 * <p>Layout comes from configuration and field state comes from data plus permissions, and the two
 * are deliberately kept apart. An administrator can move a field without any data change, and a
 * permission change can disable a field without any layout change.
 */
public record Layout(String screenId, String recordType, List<LayoutNode> regions) {

    public Layout {
        screenId = Contract.requireId(screenId, "screenId");
        recordType = Contract.requireId(recordType, "recordType");
        regions = Contract.immutableList(regions);
        requireUniqueNodeIds(regions);
    }

    /** Every field id bound by a slot anywhere in the tree, in document order. */
    public List<String> boundFieldIds() {
        List<String> fieldIds = new ArrayList<>();
        walk(node -> {
            if (node instanceof LayoutNode.Slot slot && slot.component().isBound()) {
                fieldIds.add(slot.component().fieldId());
            }
        });
        return List.copyOf(fieldIds);
    }

    /** Depth-first traversal over every node in the tree. */
    public void walk(java.util.function.Consumer<LayoutNode> visitor) {
        Deque<LayoutNode> stack = new ArrayDeque<>();
        for (int i = regions.size() - 1; i >= 0; i--) {
            stack.push(regions.get(i));
        }
        while (!stack.isEmpty()) {
            LayoutNode node = stack.pop();
            visitor.accept(node);
            List<LayoutNode> children = childrenOf(node);
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
    }

    private static List<LayoutNode> childrenOf(LayoutNode node) {
        // Exhaustive over the sealed hierarchy: adding a node kind will not compile until handled.
        return switch (node) {
            case LayoutNode.Region region -> region.children();
            case LayoutNode.Group group -> group.children();
            case LayoutNode.Slot ignored -> List.of();
        };
    }

    private static void requireUniqueNodeIds(List<LayoutNode> roots) {
        Set<String> seen = new HashSet<>();
        Deque<LayoutNode> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            LayoutNode node = stack.pop();
            if (!seen.add(node.id())) {
                throw new IllegalArgumentException("duplicate layout node id '" + node.id() + "'");
            }
            stack.addAll(childrenOf(node));
        }
    }
}
