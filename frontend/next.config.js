/** @type {import('next').NextConfig} */

const nextConfig = {
  // 生产环境优化
  output: 'standalone',
  
  // 实验性功能
  experimental: {
    // 服务端组件优化
    serverComponentsExternalPackages: ['@prisma/client'],
  },

  // 环境变量配置
  env: {
    CUSTOM_KEY: process.env.CUSTOM_KEY,
  },
  
  // 公共环境变量
  publicRuntimeConfig: {
    apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
    wsUrl: process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080',
    minioEndpoint: process.env.NEXT_PUBLIC_MINIO_ENDPOINT || 'http://localhost:9000',
  },

  // 服务端配置
  serverRuntimeConfig: {
    // 仅在服务端可用的配置
    internalApiUrl: process.env.INTERNAL_API_URL || 'http://api-gateway:80',
  },

  // 图片优化配置
  images: {
    domains: [
      'localhost',
      'minio',
      process.env.MINIO_DOMAIN,
      process.env.CDN_DOMAIN,
    ].filter(Boolean),
    formats: ['image/webp', 'image/avif'],
  },

  // 重写规则
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/:path*`,
      },
    ];
  },

  // 重定向规则
  async redirects() {
    return [
      {
        source: '/admin',
        destination: '/admin/dashboard',
        permanent: true,
      },
    ];
  },

  // 头部配置
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          {
            key: 'X-XSS-Protection',
            value: '1; mode=block',
          },
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
        ],
      },
      {
        source: '/api/(.*)',
        headers: [
          {
            key: 'Cache-Control',
            value: 'no-store, must-revalidate',
          },
        ],
      },
    ];
  },

  // Webpack 配置
  webpack: (config, { buildId, dev, isServer, defaultLoaders, webpack }) => {
    // 生产环境优化
    if (!dev) {
      config.optimization.splitChunks = {
        chunks: 'all',
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendors',
            chunks: 'all',
          },
        },
      };
    }

    // 添加别名
    config.resolve.alias = {
      ...config.resolve.alias,
      '@': require('path').resolve(__dirname, './'),
      '@/components': require('path').resolve(__dirname, './components'),
      '@/lib': require('path').resolve(__dirname, './lib'),
      '@/utils': require('path').resolve(__dirname, './utils'),
    };

    return config;
  },

  // TypeScript 配置
  typescript: {
    // 生产构建时忽略类型错误（谨慎使用）
    ignoreBuildErrors: false,
  },

  // ESLint 配置
  eslint: {
    // 生产构建时忽略 ESLint 错误（谨慎使用）
    ignoreDuringBuilds: false,
  },

  // 压缩配置
  compress: true,

  // 电源相关配置
  poweredByHeader: false,

  // 严格模式
  reactStrictMode: true,

  // SWC minifier
  swcMinify: true,

  // 国际化配置
  i18n: {
    locales: ['zh-CN', 'en', 'zh-TW'],
    defaultLocale: 'zh-CN',
    localeDetection: true,
  },

  // 跟踪配置
  productionBrowserSourceMaps: false,

  // 构建指示器
  devIndicators: {
    buildActivity: true,
  },

  // 页面扩展名
  pageExtensions: ['ts', 'tsx', 'js', 'jsx', 'md', 'mdx'],

  // 尾斜杠配置
  trailingSlash: false,

  // 静态优化指示器
  onDemandEntries: {
    // 页面在内存中保存的时间（毫秒）
    maxInactiveAge: 25 * 1000,
    // 同时保存在内存中的页面数
    pagesBufferLength: 2,
  },
};

module.exports = nextConfig;