import { createSSRApp } from 'vue'
import uviewPlus from 'uview-plus'
import App from './App.vue'
import { setupStore } from './store'
import { setupDirectives } from './directives'

function patchTabBarStyleForNonTabBarPages() {
	if (typeof uni === 'undefined' || typeof uni.setTabBarStyle !== 'function') return
	if (uni.__upSafeSetTabBarStylePatched) return

	const rawSetTabBarStyle = uni.setTabBarStyle.bind(uni)
	const isNonTabBarPageError = (error) =>
		typeof error?.errMsg === 'string' && error.errMsg.includes('not TabBar page')

	uni.setTabBarStyle = (options) => {
		try {
			const result = rawSetTabBarStyle(options)
			if (result && typeof result.catch === 'function') {
				return result.catch((error) => {
					if (isNonTabBarPageError(error)) return error
					throw error
				})
			}
			return result
		} catch (error) {
			if (isNonTabBarPageError(error)) return Promise.resolve(error)
			throw error
		}
	}

	uni.__upSafeSetTabBarStylePatched = true
}

export function createApp() {
	const app = createSSRApp(App)

	// uView Plus 在非 tabBar 页面同步原生 tabBar 主题时会抛错，这里做一次兼容。
	patchTabBarStyleForNonTabBarPages()

	app.use(uviewPlus, () => ({
		options: {
			color: {
				primary: '#4266F7',
			},
		},
	}))

	// 注册 Pinia store
	setupStore(app)

	// 注册自定义指令（如 v-loading）
	setupDirectives(app)

	return {
		app,
	}
}
