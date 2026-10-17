import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'virtual:uno.css'
import '@/styles/design-tokens.css'
import App from './VerificationApp.vue'

createApp(App).use(createPinia()).mount('#app')
