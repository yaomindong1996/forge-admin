import { getPagePathes } from '..'

const PLUGIN_PAGE_PATHES_ID = 'isme:page-pathes'
export function pluginPagePathes() {
  return {
    name: 'isme:page-pathes',
    resolveId(id) {
      if (id === PLUGIN_PAGE_PATHES_ID)
        return `\0${PLUGIN_PAGE_PATHES_ID}`
    },
    load(id) {
      if (id === `\0${PLUGIN_PAGE_PATHES_ID}`) {
        return `export default ${JSON.stringify(getPagePathes())}`
      }
    },
  }
}
