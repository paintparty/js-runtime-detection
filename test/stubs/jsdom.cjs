// Preloaded with `node -r` to give Node the globals that global-jsdom and
// jest-environment-jsdom install: window, document, and a jsdom user agent.
globalThis.window = globalThis;
globalThis.document = {};
Object.defineProperty(globalThis, 'navigator', {
  configurable: true,
  value: {userAgent: 'Mozilla/5.0 (darwin) AppleWebKit/537.36 (KHTML, like Gecko) jsdom/26.1.0'}
});
