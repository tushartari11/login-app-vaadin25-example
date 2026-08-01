import Link from 'next/link';

import styles from '@/app/shell.module.css';
import { bffBaseUrl, fetchScreen } from '@/lib/bff/client';

import { ScreenView } from './ScreenView';

/**
 * A live screen.
 *
 * <p>The route is generic — `screenId` is a path parameter, not a page. Configuring a new screen
 * therefore needs no frontend work at all, which is the property ARCHITECTURE.md §7.3 calls the
 * acceptance test for the whole design. `/screens/object-detail/<id>` is simply one value of it.
 *
 * <p>The fetch happens here, in a server component, so the access token stays on the server.
 */
export default async function ScreenPage({
  params,
}: {
  params: Promise<{ screenId: string; recordId: string }>;
}) {
  const { screenId, recordId } = await params;

  let content;
  try {
    content = <ScreenView pm={await fetchScreen(screenId, recordId)} />;
  } catch (error) {
    content = <Unavailable screenId={screenId} recordId={recordId} error={error} />;
  }

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.heading}>{screenId}</h1>
        <p className={styles.subheading}>
          record <code>{recordId}</code>
        </p>
      </header>
      <div className={styles.panel}>{content}</div>
    </main>
  );
}

/**
 * A readable failure rather than a stack trace. The BFF being down is an operational fact the
 * skeleton will hit constantly, and it should not look like a bug in the renderer.
 */
function Unavailable({
  screenId,
  recordId,
  error,
}: {
  screenId: string;
  recordId: string;
  error: unknown;
}) {
  return (
    <div className={styles.problem}>
      <p>
        Could not load <code>{screenId}</code> / <code>{recordId}</code> from the presentation BFF
        at <code>{bffBaseUrl()}</code>.
      </p>
      <p className={styles.problemDetail}>
        {error instanceof Error ? error.message : String(error)}
      </p>
      <p>
        The <Link href="/demo">rendered demo</Link> needs no backend, if you only want to see the
        renderer.
      </p>
    </div>
  );
}
