import Link from 'next/link';

import styles from '@/app/shell.module.css';

export default function HomePage() {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.heading}>MpRIA Next — operator client</h1>
        <p className={styles.subheading}>Phase 0 walking skeleton</p>
      </header>

      <p className={styles.prose}>
        This client asks the presentation BFF for <em>a screen</em>, never for a record of a
        particular kind, and draws whatever tree it is handed. It contains no field names, no record
        types and no business rules, so adding a screen needs no frontend work at all.
      </p>

      <ul className={styles.links}>
        <li>
          <Link href="/demo">Rendered demo — a checked-in model, no backend required</Link>
        </li>
      </ul>

      <p className={styles.prose}>
        A live screen is at <code>/screens/&lt;screenId&gt;/&lt;recordId&gt;</code>, which loads{' '}
        <code>/api/v1/screens/&lt;screenId&gt;/&lt;recordId&gt;</code> from the BFF.
      </p>
    </main>
  );
}
