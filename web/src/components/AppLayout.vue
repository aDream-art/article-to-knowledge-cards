<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BaseIcon from './BaseIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useFavoriteStore } from '@/stores/favorite'
import { useTagStore } from '@/stores/tag'

const auth = useAuthStore()
const chat = useChatStore()
const favorite = useFavoriteStore()
const tag = useTagStore()
const route = useRoute()
const router = useRouter()

const navs = [
  { name: 'chat', label: '聊天', icon: 'chat', to: '/chat' },
  { name: 'favorites', label: '收藏', icon: 'bookmark', to: '/favorites' },
  { name: 'stats', label: '统计', icon: 'chart', to: '/stats' },
]

const activeName = computed(() => String(route.name ?? ''))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '访客')
const initial = computed(() => displayName.value.slice(0, 1).toUpperCase())

async function handleLogout(): Promise<void> {
  await auth.logout()
  chat.reset()
  favorite.reset()
  tag.reset()
  ElMessage.success('已退出登录')
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" to="/chat" aria-label="轻问 AI 首页">
        <span class="brand-mark">问</span>
        <span class="brand-text">
          <strong>轻问 AI</strong>
          <small>轻量问答助手</small>
        </span>
      </RouterLink>

      <nav class="app-nav" aria-label="主导航">
        <RouterLink
          v-for="item in navs"
          :key="item.name"
          :to="item.to"
          class="nav-link"
          :class="{ active: activeName === item.name || (item.name === 'chat' && activeName === 'chat') }"
        >
          <BaseIcon :name="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="header-right">
        <span class="header-status">
          <i class="status-dot" />
          SSE 流式
        </span>
        <el-dropdown trigger="click" placement="bottom-end">
          <button class="user-chip" type="button">
            <span class="avatar">{{ initial }}</span>
            <span class="user-name">{{ displayName }}</span>
            <BaseIcon name="chevron-down" :size="14" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                <span class="dropdown-account">@{{ auth.user?.username ?? 'unknown' }}</span>
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <BaseIcon name="logout" :size="15" />
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--bg-app);
}

.app-header {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 60px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(12px);
  flex: none;
  z-index: 20;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-strong);
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  background: linear-gradient(135deg, #5b5bf0, #7c5cf5 55%, #9d5ff0);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.28);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-size: 15px;
  letter-spacing: 0.01em;
}

.brand-text small {
  font-size: 11px;
  color: var(--text-muted);
}

.app-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13.5px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: background 0.16s ease, color 0.16s ease;
}

.nav-link:hover {
  background: var(--bg-hover);
  color: var(--text-strong);
}

.nav-link.active {
  background: var(--primary-soft);
  color: var(--primary-strong);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
}

.header-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 5px 10px;
  background: var(--bg-subtle);
  border-radius: 999px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.16);
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 4px 8px 4px 4px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.user-chip:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow-sm);
}

.avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #eef0ff, #e7e2ff);
  color: var(--primary-strong);
  font-size: 13px;
  font-weight: 700;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.dropdown-account {
  font-size: 12px;
  color: var(--text-muted);
}

.app-main {
  flex: 1;
  min-height: 0;
  position: relative;
}

@media (max-width: 860px) {
  .app-header {
    gap: 12px;
    padding: 0 14px;
  }

  .brand-text small,
  .header-status {
    display: none;
  }

  .app-nav {
    gap: 2px;
  }

  .nav-link {
    padding: 8px 10px;
  }

  .nav-link span {
    display: none;
  }

  .user-name {
    display: none;
  }
}
</style>
