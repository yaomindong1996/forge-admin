# Common Rules & Best Practices

## File Organization

```
src/
├── api/              # API request functions (axios)
├── components/       # Reusable Vue components
├── router/          # Vue Router configuration
├── stores/          # Pinia stores (one file per store)
├── styles/          # Global SCSS files
├── utils/           # Utility functions
├── views/           # Page components (routed)
├── App.vue          # Root component
└── main.js          # Entry point
```

## Vue 3 Composition API Rules

### Component Structure
```vue
<template>
  <!-- Template first -->
</template>

<script setup>
// Imports
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

// Reactive state
const count = ref(0)

// Computed properties
const doubled = computed(() => count.value * 2)

// Methods
const increment = () => count.value++

// Lifecycle hooks
onMounted(() => {
  // Setup logic
})
</script>

<style scoped lang="scss">
// Component styles
</style>
```

### Rules
- ✅ Use `<script setup>` syntax (modern, cleaner)
- ✅ Use `ref()` for primitive values, `reactive()` for objects
- ✅ Use `computed()` for derived state
- ✅ Use `onMounted()`, `onUnmounted()` for lifecycle
- ✅ Always use `scoped` attribute on `<style>` tags
- ❌ Don't use `data()`, `methods`, `computed` options (Options API)
- ❌ Don't use `this` keyword

## Styling Rules

### UnoCSS Usage
- Use UnoCSS utility classes for layout, spacing, colors
- Example: `class="flex items-center justify-between gap-4 p-6 bg-white rounded-lg"`
- Responsive: `class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3"`
- Dark mode: `class="dark:bg-slate-900 dark:text-white"`

### SCSS Usage
- Use SCSS only for:
  - Complex animations
  - Mixins and functions
  - Component-specific styling that can't be done with utilities
  - Global theme variables (in `styles/variables.scss`)

### Variables (SCSS)
```scss
// styles/variables.scss
$primary-color: #1890ff;
$border-radius: 4px;
$transition-duration: 0.3s;
```

### Rules
- ✅ Prefer UnoCSS utilities over custom CSS
- ✅ Use `scoped` styles in components
- ✅ Use SCSS variables for theme colors
- ✅ Use `lang="scss"` in style tags
- ❌ Don't use inline styles (use classes)
- ❌ Don't use global CSS (use scoped or utilities)
- ❌ Don't use CSS-in-JS libraries

## Pinia Store Rules

### Store Structure
```javascript
// stores/user.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // State
  const user = ref(null)
  const isLoading = ref(false)

  // Computed
  const isLoggedIn = computed(() => user.value !== null)

  // Actions
  const fetchUser = async () => {
    isLoading.value = true
    try {
      const response = await api.getUser()
      user.value = response.data
    } finally {
      isLoading.value = false
    }
  }

  const logout = () => {
    user.value = null
  }

  return {
    user,
    isLoading,
    isLoggedIn,
    fetchUser,
    logout
  }
})
```

### Rules
- ✅ Use Composition API style (setup function)
- ✅ One store per domain (user, notes, settings, etc.)
- ✅ Export store with `useXxxStore` naming
- ✅ Keep stores focused and single-responsibility
- ✅ Use `ref()` for state, `computed()` for derived state
- ❌ Don't use Options API style stores
- ❌ Don't mix multiple domains in one store

## API & Axios Rules

### Request Setup
```javascript
// utils/request.js
import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// Request interceptor
instance.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor
instance.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export default instance
```

### API Functions
```javascript
// api/user.js
import request from '@/utils/request'

export const getUser = () => request.get('/user')
export const updateUser = (data) => request.put('/user', data)
export const deleteUser = () => request.delete('/user')
```

### Usage in Components
```vue
<script setup>
import { ref, onMounted } from 'vue'
import { getUser } from '@/api/user'

const user = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    user.value = await getUser()
  } catch (error) {
    console.error('Failed to fetch user:', error)
  } finally {
    loading.value = false
  }
})
</script>
```

### Rules
- ✅ Centralize API calls in `api/` folder
- ✅ Use request interceptors for auth tokens
- ✅ Handle errors gracefully
- ✅ Use async/await pattern
- ✅ Return response.data directly from interceptor
- ❌ Don't make API calls directly in components
- ❌ Don't hardcode URLs

## Router Rules

### Route Definition
```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

### Rules
- ✅ Use lazy loading with `() => import()`
- ✅ Use named routes
- ✅ Use kebab-case for paths
- ✅ Add 404 catch-all route
- ✅ Use `createWebHistory()` for clean URLs
- ❌ Don't use hash mode (#/)
- ❌ Don't hardcode component imports

## Component Rules

### Naming
- Files: PascalCase (e.g., `UserCard.vue`)
- Components: PascalCase (e.g., `<UserCard />`)
- Props: camelCase (e.g., `userName`)
- Events: kebab-case (e.g., `@user-updated`)

### Props & Emits
```vue
<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  count: {
    type: Number,
    default: 0
  }
})

defineEmits(['update', 'delete'])

const handleUpdate = () => {
  emit('update', newValue)
}
</script>
```

### Rules
- ✅ Use `defineProps()` and `defineEmits()`
- ✅ Always type props
- ✅ Provide default values
- ✅ Use descriptive prop names
- ✅ Keep components focused (single responsibility)
- ❌ Don't mutate props directly
- ❌ Don't use `v-model` for complex logic

## Vant Component Rules

### Common Components
- `<van-button>` - Buttons
- `<van-cell>` - List items
- `<van-form>` - Forms
- `<van-field>` - Form inputs
- `<van-popup>` - Modals/Popups
- `<van-tab>` - Tabs
- `<van-list>` - Infinite scroll lists
- `<van-loading>` - Loading indicator
- `<van-toast>` - Notifications

### Usage
```vue
<template>
  <van-button type="primary" @click="handleClick">
    Click me
  </van-button>
  
  <van-field
    v-model="form.name"
    label="Name"
    placeholder="Enter name"
  />
  
  <van-toast message="Success!" />
</template>

<script setup>
import { ref } from 'vue'
import { showToast } from 'vant'

const form = ref({ name: '' })

const handleClick = () => {
  showToast('Button clicked!')
}
</script>
```

### Rules
- ✅ Use Vant components for UI
- ✅ Use Vant utilities (showToast, showDialog, etc.)
- ✅ Customize with UnoCSS classes
- ✅ Check Vant docs for component props
- ❌ Don't create custom buttons/inputs (use Vant)
- ❌ Don't override Vant styles globally

## Performance Rules

### Code Splitting
- ✅ Use lazy loading for routes: `() => import('@/views/Page.vue')`
- ✅ Use lazy loading for heavy components
- ✅ Use `<Suspense>` for async components

### Optimization
- ✅ Use `computed()` instead of methods for derived state
- ✅ Use `v-show` for frequently toggled elements
- ✅ Use `v-if` for rarely rendered elements
- ✅ Use `key` binding for list items
- ✅ Avoid unnecessary re-renders with `ref` and `reactive`

### Bundle Size
- ✅ Tree-shake unused imports
- ✅ Use dynamic imports for large libraries
- ✅ Monitor bundle size with `vite build --analyze`

## Error Handling

### Try-Catch Pattern
```javascript
const fetchData = async () => {
  try {
    const data = await api.getData()
    return data
  } catch (error) {
    console.error('Error fetching data:', error)
    showToast('Failed to load data')
    throw error
  }
}
```

### Rules
- ✅ Always wrap async operations in try-catch
- ✅ Show user-friendly error messages
- ✅ Log errors for debugging
- ✅ Handle network errors gracefully
- ❌ Don't silently fail
- ❌ Don't expose technical error details to users

## Testing Rules

### Unit Tests (if applicable)
- Use Vitest for unit tests
- Test stores, utilities, and API functions
- Aim for 80%+ coverage on critical paths

### E2E Tests (if applicable)
- Use Playwright or Cypress
- Test user workflows
- Test critical features

## Git & Commit Rules

### Commit Messages
- Use conventional commits: `feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`, `chore:`
- Example: `feat: add user authentication`
- Keep messages concise and descriptive

### Branch Naming
- Feature: `feature/user-auth`
- Bug fix: `fix/login-error`
- Docs: `docs/readme-update`

## Environment Variables

### .env Files
```
# .env (shared)
VITE_API_BASE_URL=/api

# .env.development (dev only)
VITE_DEBUG=true

# .env.production (prod only)
VITE_DEBUG=false
```

### Usage in Code
```javascript
const apiUrl = import.meta.env.VITE_API_BASE_URL
```

### Rules
- ✅ Use `VITE_` prefix for client-side variables
- ✅ Keep sensitive data in `.env.local` (not committed)
- ✅ Document all env variables
- ❌ Don't commit `.env.local`
- ❌ Don't expose API keys in client code

## Accessibility Rules

### HTML Semantics
- ✅ Use semantic HTML: `<button>`, `<nav>`, `<main>`, `<article>`
- ✅ Use `<label>` for form inputs
- ✅ Use `alt` text for images
- ✅ Use ARIA attributes when needed

### Keyboard Navigation
- ✅ All interactive elements must be keyboard accessible
- ✅ Use `tabindex` carefully (usually not needed)
- ✅ Provide focus indicators

### Color & Contrast
- ✅ Don't rely on color alone to convey information
- ✅ Ensure 4.5:1 contrast ratio for text
- ✅ Test with accessibility tools

## Code Quality

### Linting & Formatting
- Use ESLint for code quality
- Use Prettier for code formatting
- Run before committing

### Naming Conventions
- Variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Classes/Components: PascalCase
- Files: kebab-case (except components)

### Comments
- ✅ Use comments for complex logic
- ✅ Use JSDoc for functions
- ❌ Don't over-comment obvious code
- ❌ Don't leave commented-out code

## Security Rules

### XSS Prevention
- ✅ Vue auto-escapes template content
- ✅ Use `v-text` instead of `{{ }}` for user input
- ✅ Use `v-html` only for trusted content
- ❌ Don't use `innerHTML` directly

### CSRF Protection
- ✅ Use CSRF tokens in forms
- ✅ Validate tokens on backend
- ✅ Use SameSite cookies

### Authentication
- ✅ Store tokens securely (httpOnly cookies preferred)
- ✅ Validate tokens on every request
- ✅ Implement token refresh logic
- ❌ Don't store sensitive data in localStorage

## Debugging

### Vue DevTools
- Install Vue DevTools browser extension
- Inspect component state and props
- Track store mutations

### Console Logging
```javascript
// Development only
if (import.meta.env.DEV) {
  console.log('Debug info:', data)
}
```

### Network Debugging
- Use browser DevTools Network tab
- Check API requests and responses
- Monitor performance

## Documentation

### README
- Include setup instructions
- Document environment variables
- List available scripts
- Provide examples

### Code Comments
- Document complex algorithms
- Explain non-obvious decisions
- Use JSDoc for functions

### Commit Messages
- Reference issues: `fixes #123`
- Explain why, not what
- Keep concise
