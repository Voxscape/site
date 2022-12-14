/* eslint @typescript-eslint/no-var-requires: 0 */
const { PHASE_DEVELOPMENT_SERVER, PHASE_PRODUCTION_BUILD } = require('next/constants');

const NUTHATCH_API_ORIGIN = process.env.NUTHATCH_API_ORIGIN || 'http://127.0.0.1:8080';
/**
 * when in problem, try to sync with {@link https://github.com/vercel/next.js/tree/canary/packages/create-next-app/templates/typescript}
 * @type {import('next').NextConfig}
 */
const nextConf = {
  poweredByHeader: false,

  /**
   * runtime server-only configuration
   */
  serverRuntimeConfig: {
    // becomes process.env.SOME_CONSTANT : boolean
    serverStartedAt: new Date().toISOString(),
    apiServerOrigin: NUTHATCH_API_ORIGIN,
  },
  /**
   * build-time configuration
   */
  env: {
    // becomes process.env.SOME_CONSTANT : boolean
    builtAt: new Date().toISOString(),
  },
  // see https://nextjs.org/docs/#customizing-webpack-config
  webpack(config, { buildId, dev, isServer, webpack }) {
    config.plugins.push(
      new webpack.DefinePlugin({
        /**
         * build-time configuration
         */
        'process.env.NEXT_DEV': JSON.stringify(!!dev),
      }),
    );

    config.node = {
      // allow use of __file / __dirname
      ...config.node,
      __filename: true,
    };
    return config;
  },

  transpilePackages: ['lodash-es', '@jokester/ts-commonutil'],

  images: {},

  productionBrowserSourceMaps: true,
  reactStrictMode: true,
  async rewrites() {
    return {
      beforeFiles: [
        {
          source: '/api/nuthatch_v1/:path*',
          destination: `${NUTHATCH_API_ORIGIN}/api/nuthatch_v1/:path*`,
        },
      ],
    };
  },
};

module.exports = (phase, { defaultConfig }) => {
  /**
   * @type {import('next').NextConfig}
   */
  let merged = { ...nextConf };

  if (phase === PHASE_PRODUCTION_BUILD) {
    merged = require('@next/bundle-analyzer')({ enabled: true, openAnalyzer: false })(merged);
  }

  return merged;
};
