import { createSSRApp } from 'vue'
import App from './App.vue'
import { setupStore } from './store'
import { setupDirectives } from './directives'
import { markEmbeddedHost } from './utils/embedded-host'

export function createApp() {
	// #ifdef H5
	markEmbeddedHost()
	// #endif

	const app = createSSRApp(App)

	// 注册 Pinia store
	setupStore(app)

	// 注册自定义指令（如 v-loading）
	setupDirectives(app)

	return {
		app,
	}
}
