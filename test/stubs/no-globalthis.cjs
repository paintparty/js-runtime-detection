// Preloaded with `node -r` to delete globalThis before the library loads,
// imitating engines without it (Node before 12, IE11, Safari before 12.1).
delete globalThis.globalThis;
