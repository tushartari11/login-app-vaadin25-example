import { render as mount, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { render } from '@/renderer/render';

import { action, boundTo, field, model, region, slot } from './fixtures';

function screenWith(actions: ReturnType<typeof action>[]) {
  return model([region('region-1', [slot('slot-1', boundTo('fieldA'))])], { fieldA: field() }, actions);
}

describe('actions render from the model only', () => {
  it('renders one control per action and no others', () => {
    mount(render(screenWith([action(), action({ id: 'act-2', label: 'label.act2' })])));

    expect(screen.getByRole('button', { name: 'label.act1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'label.act2' })).toBeInTheDocument();
  });

  it('hands the server-supplied method and href straight back, never a constructed URL', async () => {
    const onAction = vi.fn();
    mount(render(screenWith([action()]), { onAction }));

    await userEvent.click(screen.getByRole('button', { name: 'label.act1' }));

    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onAction.mock.calls[0]?.[0]?.action).toMatchObject({
      method: 'PUT',
      href: '/api/v1/screens/screen-1/rec-1',
    });
  });

  it('passes the current control values alongside the action', async () => {
    const onAction = vi.fn();
    const pm = model([region('region-1', [slot('slot-1', boundTo('fieldA'))])], { fieldA: field() }, [
      action(),
    ]);
    mount(render(pm, { onAction }));

    await userEvent.type(screen.getByLabelText('label.a'), 'v1');
    await userEvent.click(screen.getByRole('button', { name: 'label.act1' }));

    expect(onAction.mock.calls[0]?.[0]?.values).toEqual({ fieldA: 'v1' });
  });
});

describe('a disabled action', () => {
  const disabled = action({
    id: 'act-3',
    label: 'label.act3',
    enabled: false,
    disabledReason: 'reason.notAllowedHere',
  });

  it('is not clickable', async () => {
    const onAction = vi.fn();
    mount(render(screenWith([disabled]), { onAction }));

    await userEvent.click(screen.getByRole('button', { name: /label\.act3/ }));

    expect(onAction).not.toHaveBeenCalled();
  });

  it('reports itself as disabled to assistive technology', () => {
    mount(render(screenWith([disabled]), {}));

    expect(screen.getByRole('button', { name: /label\.act3/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });

  it('surfaces its reason, and links it to the control so it is announced', () => {
    // aria-disabled rather than the `disabled` attribute, deliberately: a `disabled` button is
    // removed from the tab order, so a keyboard user could never reach the explanation.
    mount(render(screenWith([disabled]), {}));
    const button = screen.getByRole('button', { name: /label\.act3/ });

    expect(screen.getByText('reason.notAllowedHere')).toBeVisible();
    expect(button.getAttribute('aria-describedby')).toBe(
      screen.getByText('reason.notAllowedHere').getAttribute('id'),
    );
  });
});

describe('an action requiring confirmation', () => {
  const confirmed = action({ id: 'act-4', label: 'label.act4', confirm: true });

  it('does not fire on the first click', async () => {
    const onAction = vi.fn();
    mount(render(screenWith([confirmed]), { onAction }));

    await userEvent.click(screen.getByRole('button', { name: 'label.act4' }));

    expect(onAction).not.toHaveBeenCalled();
  });

  it('fires once confirmed', async () => {
    const onAction = vi.fn();
    mount(render(screenWith([confirmed]), { onAction }));

    await userEvent.click(screen.getByRole('button', { name: 'label.act4' }));
    await userEvent.click(screen.getByRole('button', { name: /^Confirm/ }));

    expect(onAction).toHaveBeenCalledTimes(1);
  });

  it('can be abandoned', async () => {
    const onAction = vi.fn();
    mount(render(screenWith([confirmed]), { onAction }));

    await userEvent.click(screen.getByRole('button', { name: 'label.act4' }));
    await userEvent.click(screen.getByRole('button', { name: /^Cancel/ }));

    expect(onAction).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'label.act4' })).toBeInTheDocument();
  });

  it('leaves an action that needs no confirmation firing immediately', async () => {
    const onAction = vi.fn();
    mount(render(screenWith([action()]), { onAction }));

    await userEvent.click(screen.getByRole('button', { name: 'label.act1' }));

    expect(onAction).toHaveBeenCalledTimes(1);
  });
});
