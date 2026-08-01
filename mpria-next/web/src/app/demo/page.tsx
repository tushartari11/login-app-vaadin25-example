import Link from 'next/link';

import { parsePresentationModel } from '@/lib/pm/parse';
import { render } from '@/renderer/render';
import styles from '@/app/shell.module.css';

import demoModel from './demo-presentation-model.json';

/**
 * The renderer against a checked-in model, with nothing else running.
 *
 * <p>No BFF, no database, no identity provider — `npm run dev` and this page draws a complete
 * configured screen. That is what makes the skeleton reviewable by someone who is not set up to
 * run four services, and it is also the fastest way to see that the renderer really does work from
 * data alone.
 *
 * <p>Parsing happens at module scope, so a malformed fixture fails `next build` rather than
 * producing a broken page.
 */
const pm = parsePresentationModel(demoModel);

export const metadata = {
  title: 'MpRIA Next — rendered demo',
};

export default function DemoPage() {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.heading}>Rendered from a checked-in model</h1>
        <p className={styles.subheading}>
          screen <code>{pm.meta.screenId}</code> · revision <code>{pm.meta.revisionId}</code>
        </p>
      </header>

      <p className={styles.prose}>
        Nothing on this page is written in the client. Resize the window to watch the configured
        per-breakpoint spans take effect, and note the deliberately awkward cases: a read-only
        field, a field carrying a server-side validation message, a field the model marks hidden
        (its grid cell is gone, not merely invisible), an action disabled with a stated reason, an
        action that asks for confirmation, and a component type this client has never heard of.
      </p>

      <div className={styles.panel}>{render(pm)}</div>

      <p className={styles.prose}>
        <Link href="/">Back</Link>
      </p>
    </main>
  );
}
