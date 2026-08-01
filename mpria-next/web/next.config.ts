import type { NextConfig } from 'next';

/**
 * Deliberately close to empty. The renderer is plain React plus CSS Modules, so there is nothing
 * to configure — and a build that needs no configuration is one less thing that can drift between
 * a developer's machine and CI.
 */
const nextConfig: NextConfig = {
  reactStrictMode: true,
};

export default nextConfig;
