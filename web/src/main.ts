import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'highlight.js/styles/github-dark.css'
import '@/styles/main.css'

import App from './App.vue'
import router from './router'
import { onUnauthorized } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useFavoriteStore } from '@/stores/favorite'
import { useTagStore } from '@/stores/tag'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
// 全量引入 Element Plus（个人项目优先简单直接）
app.use(ElementPlus, { locale: zhCn })
app.use(router)

/** 401：清空本地登录态与各 store，回到登录页并保留原地址。 */
onUnauthorized(() => {
  useAuthStore(pinia).clear()
  useChatStore(pinia).reset()
  useFavoriteStore(pinia).reset()
  useTagStore(pinia).reset()

  const current = router.currentRoute.value
  if (current.name === 'login') return
  const redirect = current.fullPath && current.fullPath !== '/' ? current.fullPath : undefined
  void router.replace({ name: 'login', query: redirect ? { redirect } : undefined })
})

app.mount('#app')
