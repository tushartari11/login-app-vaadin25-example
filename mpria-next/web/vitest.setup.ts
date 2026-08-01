import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

import '@testing-library/jest-dom/vitest';

// Testing Library only auto-cleans when `afterEach` is a global. It is not, because this project
// runs Vitest without globals, so unmounting is wired up explicitly here.
afterEach(cleanup);
