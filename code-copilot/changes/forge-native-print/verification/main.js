import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './VerificationApp.vue'
import DesignerApp from './DesignerVerificationApp.vue'

createApp(location.search.includes('designer') ? DesignerApp : App).use(createPinia()).mount('#app')
