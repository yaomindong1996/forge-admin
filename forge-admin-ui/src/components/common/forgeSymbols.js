/** Forge 统一符号：24px 网格、1.7px 描边；路径只来自此静态白名单。 */
export const forgeSymbols = Object.freeze({
  home: ['M3 10.5 12 3l9 7.5', 'M5 9v11h5v-6h4v6h5V9'],
  apps: ['M3 3h7v7H3z', 'M14 3h7v7h-7z', 'M3 14h7v7H3z', 'M14 17.5h7', 'M17.5 14v7'],
  builder: ['M3 4h18v16H3z', 'M3 9h18', 'M9 9v11', 'M13 13h4', 'M13 16h3'],
  workflow: ['M9 3h6v5H9z', 'M3 16h6v5H3z', 'M15 16h6v5h-6z', 'M12 8v4', 'M6 16v-4h12v4'],
  analytics: ['M4 3v18h17', 'M8 16v-4', 'M13 16V7', 'M18 16V4'],
  ai: ['M7 7h10v10H7z', 'M9 3v4m6-4v4M9 17v4m6-4v4M3 9h4m-4 6h4m10-6h4m-4 6h4', 'M10 12h4'],
  platform: ['M4 4h16v6H4z', 'M4 14h16v6H4z', 'M7 7h.01M7 17h.01', 'M16 7h1m-1 10h1'],
  collaboration: [
    'M12 3a3 3 0 1 0 0 6 3 3 0 0 0 0-6',
    'M7 21v-3a5 5 0 0 1 10 0v3',
    'M4 6a3 3 0 0 0 0 6m16-6a3 3 0 0 1 0 6',
    'M2 20v-2a4 4 0 0 1 3-4m17 6v-2a4 4 0 0 0-3-4',
  ],
  open: ['M8 8 3 12l5 4', 'm16-8 5 4-5 4', 'm14 4-4 16'],
  plugins: ['M4 4h6V3a2 2 0 0 1 4 0v1h6v6h1a2 2 0 0 1 0 4h-1v6h-6v-1a2 2 0 0 0-4 0v1H4v-6H3a2 2 0 0 1 0-4h1z'],
  monitor: ['M3 4h18v13H3z', 'M8 21h8m-4-4v4', 'M6 11h3l2-4 3 7 2-3h2'],
  message: ['M4 4h16v13H9l-5 4z', 'M8 8h8m-8 5h5'],
  print: ['M7 8V3h10v5', 'M6 17H3V8h18v9h-3', 'M7 14h10v7H7z', 'M17 11h.01'],
  schedule: ['M8 2v4m8-4v4', 'M3 10V4h18v6', 'M3 10v11h18V10z', 'M12 13v3h3'],
  database: ['M4 6a8 3 0 1 0 16 0 8 3 0 1 0-16 0', 'M4 6v12a8 3 0 0 0 16 0V6', 'M4 12a8 3 0 0 0 16 0'],
  shield: ['M12 2 3 6v6c0 5 9 10 9 10s9-5 9-10V6z', 'm8 12 3 3 5-6'],
  approval: ['M9 3H4v18h16V3h-5', 'M9 2h6v4H9z', 'm8 13 3 3 5-6'],
  actions: ['M4 3h16v18H4z', 'm7 8 2 2 3-3', 'M14 9h3', 'm7 16 2 2 3-3', 'M14 17h3'],
  layers: ['m12 2 10 5-10 5L2 7z', 'm2 12 10 5 10-5', 'm2 17 10 5 10-5'],
  connection: ['M7 9V3m10 6V3', 'M5 9h14v3a7 7 0 0 1-14 0z', 'M12 19v3'],
  module: ['M12 2 3 7v10l9 5 9-5V7z', 'm3 7 9 5 9-5', 'M12 12v10', 'm7.5 4.5 9 5'],
})

export function resolveForgeSymbol(name) {
  return Object.hasOwn(forgeSymbols, name) ? name : 'module'
}
