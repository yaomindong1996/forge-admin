import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as naive from 'naive-ui'
import 'virtual:uno.css'
import App from './VerificationApp.vue'

const app = createApp(App)
app.use(createPinia())
for (const [name, component] of Object.entries(naive)) {
  if (/^N[A-Z]/.test(name))
    app.component(name, component)
}
app.mount('#app')
