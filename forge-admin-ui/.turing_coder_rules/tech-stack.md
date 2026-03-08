# Tech Stack Rules

## Core Framework
- **Vue 3** (Composition API) - Primary framework
- **Vite 5** - Build tool and dev server
- **Node.js** - Runtime environment

## State Management & Routing
- **Pinia 2.1.7** - State management (replaces Vuex)
- **Vue Router 4.2.5** - Client-side routing

## UI & Styling
- **UnoCSS 0.58.4** - Atomic CSS engine (Tailwind-compatible)
- **Vant 4.8.8** - Mobile UI component library
- **SCSS 1.70.0** - CSS preprocessor
- **@unocss/reset** - CSS reset utilities

## HTTP & API
- **Axios 1.6.5** - HTTP client for API requests

## Build & Development Tools
- **@vitejs/plugin-vue** - Vue 3 support for Vite
- **unplugin-auto-import** - Auto-import Vue/Pinia/Router APIs
- **unplugin-vue-components** - Auto-import Vue components
- **@vant/auto-import-resolver** - Auto-import Vant components

## Development Server
- Port: 3000
- API proxy: `/api` → `http://localhost:8080`
- Auto-open browser on start

## Package Manager
- **pnpm** (recommended, see pnpm-lock.yaml)
- Alternative: npm, yarn

## Constraints
- ✅ Use Vue 3 Composition API (no Options API)
- ✅ Use UnoCSS utilities for styling (not custom CSS when possible)
- ✅ Use Vant components for UI elements
- ✅ Use Pinia for state management
- ✅ Use auto-import features (no manual imports for Vue/Pinia/Router)
- ❌ Don't use Vuex (Pinia is the standard)
- ❌ Don't use Webpack (Vite is configured)
- ❌ Don't add new CSS frameworks (UnoCSS is sufficient)
