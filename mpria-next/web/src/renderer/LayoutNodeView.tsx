import type { ReactElement, ReactNode } from 'react';

import type { FieldState, GroupNode, LayoutNode, RegionNode, SlotNode } from '@/lib/pm/types';
import { UnsupportedComponent } from '@/renderer/components/fallbacks';
import { cellStyle, classNames, gridStyle } from '@/renderer/grid';
import type { ComponentRegistry } from '@/renderer/registry';
import styles from '@/renderer/renderer.module.css';

interface NodeProps {
  readonly node: LayoutNode;
  readonly fields: Readonly<Record<string, FieldState>>;
  readonly registry: ComponentRegistry;
}

/**
 * Walks the layout tree.
 *
 * <p>The `switch` is exhaustive over the discriminated union, mirroring the `sealed` hierarchy in
 * `pm-contract`: a new node kind fails to compile here rather than rendering as nothing.
 */
export function LayoutNodeView({ node, fields, registry }: NodeProps): ReactElement | null {
  switch (node.kind) {
    case 'REGION':
      return <RegionView node={node} fields={fields} registry={registry} />;
    case 'GROUP':
      return <GroupView node={node} fields={fields} registry={registry} />;
    case 'SLOT':
      return <SlotView node={node} fields={fields} registry={registry} />;
    default:
      return unhandled(node);
  }
}

function unhandled(node: never): never {
  throw new Error(`unhandled layout node kind: ${JSON.stringify(node)}`);
}

function childrenOf(
  children: readonly LayoutNode[],
  fields: Readonly<Record<string, FieldState>>,
  registry: ComponentRegistry,
): ReactNode {
  return children.map((child) => (
    <LayoutNodeView key={child.id} node={child} fields={fields} registry={registry} />
  ));
}

function RegionView({
  node,
  fields,
  registry,
}: NodeProps & { node: RegionNode }): ReactElement {
  return (
    <section
      className={styles.grid}
      style={gridStyle(node.grid)}
      data-node-kind="REGION"
      data-node-id={node.id}
    >
      {childrenOf(node.children, fields, registry)}
    </section>
  );
}

/**
 * A group is two things at once: a cell of its parent's grid (its `span`) and a grid container of
 * its own (its `grid`). Keeping those on separate elements is what lets a group be resized without
 * disturbing the arrangement inside it.
 */
function GroupView({ node, fields, registry }: NodeProps & { node: GroupNode }): ReactElement {
  const label = node.label ?? node.id;
  const body = (
    <div className={styles.grid} style={gridStyle(node.grid)} data-grid-of={node.id}>
      {childrenOf(node.children, fields, registry)}
    </div>
  );

  if (node.collapsible === true) {
    // `details` collapses with no JavaScript and is keyboard-operable and announced correctly out
    // of the box, so a collapsed group behaves identically before and after hydration.
    return (
      <details
        className={classNames(styles.cell, styles.group)}
        style={cellStyle(node.span)}
        data-node-kind="GROUP"
        data-node-id={node.id}
        open
      >
        <summary className={styles.groupSummary}>{label}</summary>
        {body}
      </details>
    );
  }

  return (
    <section
      className={classNames(styles.cell, styles.group)}
      style={cellStyle(node.span)}
      data-node-kind="GROUP"
      data-node-id={node.id}
    >
      {node.label === undefined ? null : <h3 className={styles.groupLabel}>{label}</h3>}
      {body}
    </section>
  );
}

function SlotView({ node, fields, registry }: NodeProps & { node: SlotNode }): ReactElement | null {
  const { component } = node;
  const field = component.fieldId === undefined ? undefined : fields[component.fieldId];

  if (field?.hidden === true) {
    // Not merely invisible: the cell is removed, so the surrounding grid closes up. Leaving an
    // empty cell would put a hole in a layout whose author never configured one.
    return null;
  }

  const Component = registry[component.type];

  return (
    <div
      className={styles.cell}
      style={cellStyle(node.span)}
      data-node-kind="SLOT"
      data-node-id={node.id}
      data-component-type={component.type}
    >
      {Component === undefined ? (
        <UnsupportedComponent component={component} />
      ) : (
        <Component slotId={node.id} component={component} field={field} />
      )}
    </div>
  );
}
